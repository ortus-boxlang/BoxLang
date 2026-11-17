/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http: //www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ortus.boxlang.runtime.async.tasks;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.cache.providers.ICacheProvider;
import ortus.boxlang.runtime.config.segments.ClusterConfig;
import ortus.boxlang.runtime.dynamic.Attempt;
import ortus.boxlang.runtime.logging.BoxLangLogger;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.services.ClusterService;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;

/**
 * Claim arbitration for clustered scheduled tasks.
 * <p>
 * This is deliberately part of the <b>scheduler</b>, not the cluster service: claiming an occurrence so
 * a job runs exactly once is scheduler domain, while membership and node records belong to the cluster
 * service. The cluster service supplies the cache, the cluster name, and the peer snapshot; everything
 * here is policy.
 * <p>
 * The protocol, for one occurrence:
 *
 * <pre>
 * 1. read the task record
 * 2. decide from the occurrence id - already running? already handled? mine to try?
 * 3. delay - eager: random; balanced: rank-based pushback + tie jitter
 * 4. re-read - someone else claimed it while we waited? stop without writing
 * 5. write our claim
 * 6. settle, then read back (repeat verifyRounds times) - if the record is no longer ours, stop
 * 7. run, heartbeating the record so a long job keeps its claim
 * 8. finish - re-read first, because a record we no longer own means a duplicate ran
 * </pre>
 *
 * There is no atomic create in the cache API, so steps 4-6 are what stand in for one: the loser of a
 * race either sees the winner's record on the re-read, or sees it on a verification read.
 */
public class TaskClusterClaim {

	/**
	 * The outcome of attempting to claim an occurrence.
	 */
	public enum Decision {
		/** Not clustered: the caller should proceed exactly as it always has. */
		UNCLUSTERED,
		/** This node owns the occurrence and should run it. */
		RUN,
		/** Another node owns it, or it is already handled: do not run, and log why. */
		SKIP
	}

	private static final DateTimeFormatter	OCCURRENCE_FORMAT			= DateTimeFormatter.ofPattern( "yyyy-MM-dd'T'HH:mm:ss" );

	/**
	 * What a task does when its previous occurrence is still running, when neither the task nor the scheduler
	 * config says otherwise.
	 */
	public static final String				DEFAULT_TASK_OVERLAP_POLICY	= "skip";

	/**
	 * The claim safety budget: wait this long for a write to be visible, read it back, and retry the whole
	 * claim this many times. These are how the protocol works, not settings.
	 */
	public static final long				SETTLE_MILLIS				= 1_000;
	public static final int					VERIFY_ROUNDS				= 2;
	public static final int					ATTEMPTS					= 3;

	/**
	 * eager: maximum random delay before claiming. balanced: maximum pushback for the busiest node, plus
	 * jitter so equally loaded nodes do not stay in lockstep.
	 */
	public static final long				RANDOM_DELAY_MILLIS			= 1_000;
	public static final long				MAX_PUSHBACK_MILLIS			= 5_000;
	public static final long				TIE_JITTER_MILLIS			= 250;

	/**
	 * How often an in-flight claim attempt re-checks whether its scheduler has begun shutting down, while it is
	 * sleeping through its delay and settle rounds.
	 */
	private static final long				SHUTDOWN_POLL_MILLIS		= 50;

	/**
	 * Is this deployment sharing occurrences out by published load?
	 *
	 * @return true when the scheduler is configured for balanced sharing
	 */
	public static String modeName() {
		return isBalanced() ? "balanced" : "eager";
	}

	public static boolean isBalanced() {
		return "balanced".equalsIgnoreCase( BoxRuntime.getInstance().getConfiguration().scheduler.clusterMode );
	}

	/**
	 * The shortest occurrence period this claim protocol can service: the settle rounds plus the largest
	 * delay the mode can apply, plus a second of slack.
	 *
	 * @return The minimum period in seconds
	 */
	public static long minimumPeriodSeconds() {
		long delayMillis = isBalanced() ? MAX_PUSHBACK_MILLIS : RANDOM_DELAY_MILLIS;
		return ( ( VERIFY_ROUNDS * SETTLE_MILLIS ) + delayMillis + 1_000 ) / 1000;
	}

	private static final BoxLangLogger							logger			= BoxRuntime.getInstance().getLoggingService().SCHEDULER_LOGGER;

	/**
	 * --------------------------------------------------------------------------
	 * Monitoring, and the shared heartbeat
	 *
	 * Two things replaced the per-run thread and the peer sweep:
	 *
	 * - <b>One</b> thread beats every in-flight claim this node holds, instead of one thread per running job.
	 * - A node that <i>loses</i> a claim adds that occurrence's key to a watch list. The same thread reads each
	 * watched key until it reaches a terminal status (then it is simply dropped) or its heartbeat goes stale,
	 * in which case the occurrence is written `abandoned` immediately after the read that found it stale, and
	 * dropped. After that no node ever considers that occurrence again.
	 * --------------------------------------------------------------------------
	 */

	/** Runs this node currently holds, keyed by cache key. */
	private static final java.util.Map<String, Claim>			HELD_RUNS		= new java.util.concurrent.ConcurrentHashMap<>();

	/** Occurrences this node lost, and is watching until they resolve. Key to the occurrence it belongs to. */
	private static final java.util.Map<String, MonitoredClaim>	WATCHED_CLAIMS	= new java.util.concurrent.ConcurrentHashMap<>();

	/** The single thread that beats held runs and resolves watched ones. Created on demand, daemon. */
	private static Thread										maintenanceThread;

	/** Set when the last held run finishes, so the thread can exit instead of idling forever. */
	private static volatile boolean								maintenanceStop	= true;

	/** A claim this node lost and is watching. */
	private static final class MonitoredClaim {

		final ScheduledTask		task;
		final ClusterService	cluster;
		final ICacheProvider	cache;
		final String			key;
		final String			occurrence;

		MonitoredClaim( ScheduledTask task, ClusterService cluster, ICacheProvider cache, String key, String occurrence ) {
			this.task		= task;
			this.cluster	= cluster;
			this.cache		= cache;
			this.key		= key;
			this.occurrence	= occurrence;
		}

		/**
		 * Resolve this watched occurrence if it has gone stale, then stop watching it.
		 * <p>
		 * Read, decide, write - with no gap in between that this code controls. If the record has already
		 * reached a terminal status, or has disappeared, there is nothing to resolve: drop it and never look
		 * again.
		 */
		void resolveIfStale() {
			IStruct record;
			try {
				record = asStruct( this.cache.get( this.key ).orElse( null ) );
			} catch ( Throwable t ) {
				logger.warn( "Could not read watched claim [{}]: {}", this.key, t.getMessage() );
				return;
			}

			// Gone, or already resolved by its owner (or by a previous pass): stop watching
			if ( record == null || !isInFlight( getString( record, Key.status ) ) ) {
				WATCHED_CLAIMS.remove( this.key );
				return;
			}

			// Still held by its owner, but the heartbeat has stopped: the owner is gone
			if ( isFresh( record, this.cluster.getConfig() ) ) {
				return;
			}

			String	owner		= getString( record, Key.nodeId );
			IStruct	abandoned	= Struct.ofNonConcurrent(
			    Key.claimId, getString( record, Key.claimId ),
			    Key.occurrenceId, this.occurrence,
			    Key.nodeId, owner,
			    Key.status, "abandoned",
			    Key.claimedAt, record.get( Key.claimedAt ),
			    Key.heartbeatAt, record.get( Key.heartbeatAt ),
			    Key.finishedAt, Instant.now(),
			    Key.lastResult, "heartbeat stopped - reported abandoned by " + this.cluster.getNodeId()
			);
			try {
				// Written immediately after the read that judged it stale, so the decision-to-write gap is as
				// short as the cache allows. The key belongs to this occurrence alone, so this cannot clobber a
				// different run.
				this.cache.set( this.key, abandoned, claimTtlSeconds( this.cluster.getConfig() ) );
			} catch ( Throwable t ) {
				logger.warn( "Could not mark occurrence [{}] abandoned: {}", this.occurrence, t.getMessage() );
				return;
			}

			logAbandonedOnce( this.task, this.occurrence, owner );
			WATCHED_CLAIMS.remove( this.key );
		}
	}

	/**
	 * Watch an occurrence this node just lost, so its record is resolved instead of being looked at again and
	 * again by every node's own tick.
	 */
	private static void monitor( ScheduledTask task, ClusterService cluster, String key, String occurrence ) {
		if ( !cluster.getConfig().enabled ) {
			return;
		}
		WATCHED_CLAIMS.put( key, new MonitoredClaim( task, cluster, cluster.getCache(), key, occurrence ) );
		startMaintenanceThread();
	}

	/**
	 * Start the single maintenance thread if it is not already running.
	 */
	private static synchronized void startMaintenanceThread() {
		maintenanceStop = false;
		if ( maintenanceThread != null && maintenanceThread.isAlive() ) {
			return;
		}
		maintenanceThread = new Thread( TaskClusterClaim::maintenanceLoop, "boxlang-cluster-task-heartbeat" );
		maintenanceThread.setDaemon( true );
		maintenanceThread.start();
	}

	/**
	 * Beat every held claim and resolve every watched one, once, on the cluster heartbeat cadence.
	 * <p>
	 * Deliberately one thread for all of them: a thread per running job meant twenty concurrent runs cost twenty
	 * platform threads. Neither this nor the claim path uses an executor, so none of it can compete with task
	 * runs for a pool slot.
	 */
	static void maintenanceLoop() {
		while ( !maintenanceStop ) {
			// Exit as soon as there is nothing to do, rather than idling for the life of the JVM. The next claim
			// or watch restarts it.
			if ( HELD_RUNS.isEmpty() && WATCHED_CLAIMS.isEmpty() ) {
				maintenanceStop = true;
				return;
			}
			try {
				long			intervalSeconds	= 5;
				ClusterService	cluster			= getClusterServiceOrNull();
				if ( cluster != null && cluster.getConfig() != null ) {
					intervalSeconds = Math.max( 1, Math.min( cluster.getConfig().heartbeatSeconds, 30 ) );
				}
				// Sleep in short slices so an emptied queue is noticed promptly instead of after a full interval
				long deadline = System.currentTimeMillis() + ( intervalSeconds * 1000L );
				while ( !maintenanceStop && System.currentTimeMillis() < deadline ) {
					if ( HELD_RUNS.isEmpty() && WATCHED_CLAIMS.isEmpty() ) {
						maintenanceStop = true;
						return;
					}
					sleepQuietly( 100 );
				}
				// Held claims: rewrite so peers see them alive, and detect a lost claim
				for ( Claim claim : java.util.List.copyOf( HELD_RUNS.values() ) ) {
					claim.beat();
				}
				// Watched claims: resolve, then forget
				for ( MonitoredClaim watched : java.util.List.copyOf( WATCHED_CLAIMS.values() ) ) {
					watched.resolveIfStale();
				}
			} catch ( Throwable t ) {
				logger.error( "Cluster task maintenance error: {}", t.getMessage(), t );
			}
		}
	}

	/**
	 * Stop the maintenance thread and clear all state.
	 * <p>
	 * Interrupts first and then joins, so a shutdown is not delayed: the thread sleeps in 100ms slices and
	 * checks its stop flag, so it exits on the next slice at the latest. Called from the scheduler service's
	 * shutdown paths - without that call the thread would keep beating claims into a cache that is about to
	 * close.
	 */
	public static synchronized void shutdownMaintenance() {
		maintenanceStop = true;
		Thread thread = maintenanceThread;
		maintenanceThread = null;
		HELD_RUNS.clear();
		WATCHED_CLAIMS.clear();
		if ( thread != null && thread != Thread.currentThread() ) {
			thread.interrupt();
			try {
				thread.join( 500 );
			} catch ( InterruptedException e ) {
				Thread.currentThread().interrupt();
			}
			if ( thread.isAlive() ) {
				logger.warn( "Cluster task maintenance thread was still alive after the shutdown request; it is a daemon and will exit on its next check" );
			}
		}
	}

	/** Visible for tests: how many occurrences are currently being watched. */
	public static int watchedClaimCount() {
		return WATCHED_CLAIMS.size();
	}

	/** Visible for tests: how many runs this node is currently beating. */
	public static int heldClaimCount() {
		return HELD_RUNS.size();
	}

	/**
	 * Resolve whether this task participates in cluster coordination.
	 * <p>
	 * {@code task.cluster} is a tri-state: absent inherits {@code cluster.enabled} from the deployment
	 * configuration.
	 *
	 * @param task The task
	 *
	 * @return true when this task should coordinate through the cluster
	 */
	public static boolean isClustered( ScheduledTask task ) {
		return isClustered( task, getClusterServiceOrNull() );
	}

	/**
	 * Resolve participation against an explicit cluster service, so tests and embedders can drive this
	 * without the runtime's global one.
	 *
	 * @param task    The task
	 * @param cluster The cluster service, may be null
	 *
	 * @return true when this task should coordinate through the cluster
	 */
	public static boolean isClustered( ScheduledTask task, ClusterService cluster ) {
		Boolean override = task.getCluster();
		if ( override != null ) {
			return override;
		}
		return cluster != null && cluster.getConfig() != null && cluster.getConfig().enabled;
	}

	/**
	 * Attempt to claim one occurrence of a task.
	 *
	 * @param task       The task
	 * @param dueInstant When the tick was due, or null when the runtime has no due instant
	 *
	 * @return A claim to finish after the run, or null when the task is not clustered or this node
	 *         should not run this occurrence
	 */
	public static Claim attempt( ScheduledTask task, Instant dueInstant ) {
		return attempt( task, dueInstant, getClusterServiceOrNull() );
	}

	/**
	 * Attempt to claim one occurrence of a task against an explicit cluster service.
	 *
	 * @param task       The task
	 * @param dueInstant When the tick was due, or null
	 * @param cluster    The cluster service
	 *
	 * @return A claim to finish after the run, or null when it should not run
	 */
	public static Claim attempt( ScheduledTask task, Instant dueInstant, ClusterService cluster ) {
		if ( !isClustered( task, cluster ) || cluster == null ) {
			return null;
		}

		ClusterConfig	config		= cluster.getConfig();
		ICacheProvider	cache		= cluster.getCache();
		String			occurrence	= occurrenceId( task, dueInstant );
		// This occurrence has its own key: nothing else contends for it, so a write here cannot destroy
		// another occurrence's guard.
		String			key			= buildTaskKey( cluster, task, occurrence );

		// 1. read
		IStruct			record		= asStruct( cache.get( key ).orElse( null ) );

		// 2. decide
		if ( record != null ) {
			String	existingStatus	= getString( record, Key.status );
			String	existingOwner	= getString( record, Key.nodeId );
			boolean	live			= isFresh( record, config );

			if ( isInFlight( existingStatus ) ) {
				if ( live ) {
					log( task, occurrence, "in progress on " + existingOwner + " - skipping" );
					// Watch this occurrence until it resolves: if the owner dies, this node marks it abandoned
					monitor( task, cluster, key, occurrence );
				} else {
					logAbandonedOnce( task, occurrence, existingOwner );
					monitor( task, cluster, key, occurrence );
				}
				// An occurrence is never re-run, fresh or stale: that is the whole guarantee
				return null;
			}
			if ( "abandoned".equals( existingStatus ) ) {
				logAbandonedOnce( task, occurrence, existingOwner );
				return null;
			}
			// success / failed: already handled, nothing to do and nothing to watch
			log( task, occurrence, "already handled by " + existingOwner + " (" + existingStatus + ") - skipping" );
			return null;
		}

		// 3. delay
		if ( !pause( task, computeDelayMillis( task, cluster, config ) ) ) {
			log( task, occurrence, "abandoning the claim - the scheduler is shutting down" );
			return null;
		}

		// 3b. Overlap policy. Different occurrences have different keys now, so "is the previous one still
		// running" has to be asked explicitly rather than falling out of a shared slot.
		String overlap = overlappingClaimReason( task, cluster, cache, occurrence );
		if ( overlap != null ) {
			log( task, occurrence, "skipped - " + overlap );
			return null;
		}

		// 4. re-read: someone else may have claimed this occurrence while we waited
		String blockedBy = blockingClaimReason( asStruct( cache.get( key ).orElse( null ) ), cluster, config );
		if ( blockedBy != null ) {
			log( task, occurrence, "backing off - " + blockedBy );
			monitor( task, cluster, key, occurrence );
			return null;
		}

		// 5-6. write, settle, verify
		int attempts = Math.max( 1, ATTEMPTS );
		for ( int attempt = 1; attempt <= attempts; attempt++ ) {
			// Re-check immediately before every write, including retries. This occurrence has its own key, so
			// the only thing that can appear here is someone else claiming this same occurrence.
			blockedBy = blockingClaimReason( asStruct( cache.get( key ).orElse( null ) ), cluster, config );
			if ( blockedBy != null ) {
				log( task, occurrence, "backing off - " + blockedBy );
				monitor( task, cluster, key, occurrence );
				return null;
			}

			String	claimId	= UUID.randomUUID().toString();
			IStruct	claim	= buildRecord( cluster, task, occurrence, claimId, "claimed" );

			cache.set( key, claim, claimTtlSeconds( config ) );

			boolean owned = true;
			for ( int round = 0; round < Math.max( 1, VERIFY_ROUNDS ); round++ ) {
				if ( !settleAndVerify( cache, key, cluster, occurrence, claimId ) ) {
					owned = false;
					break;
				}
			}

			if ( owned ) {
				IStruct running = buildRecord( cluster, task, occurrence, claimId, "running" );
				cache.set( key, running, claimTtlSeconds( config ) );
				// Advisory pointer so the next occurrence can tell this one is still in flight (overlap=skip)
				writeTaskPointer( cluster, cache, task, occurrence );
				// Publish the run in this node's record so peers can see it without reading this task key, and so
				// they can read the authoritative outcome from this occurrence's key if this node disappears.
				cluster.trackRunStarted( schedulerNameOf( task ), task.getName(), occurrence, key );
				log( task, occurrence, "CLAIMED by " + cluster.getNodeId() + " - running" );
				return new Claim( task, cluster, cache, key, occurrence, claimId );
			}

			// A claim that cannot be confirmed is normally retried, but not once shutdown has begun: there is
			// no point, and sleeping through the retries is what kept an idle node from exiting.
			if ( isShuttingDown( task ) ) {
				log( task, occurrence, "abandoning the claim - the scheduler is shutting down" );
				return null;
			}

			if ( attempt < attempts ) {
				sleepQuietly( randomBetween( 0, Math.max( 1, TIE_JITTER_MILLIS ) ) );
			}
		}

		log( task, occurrence, "could not confirm a claim after " + attempts + " attempts - skipping" );
		return null;
	}

	/**
	 * Why this node must not write a claim for this occurrence, given the record it is looking at, or null when
	 * it may.
	 * <p>
	 * With one key per occurrence the record can only ever describe <b>this</b> occurrence, so all that is left
	 * to decide is whether someone else holds it in an in-flight state. There is deliberately no freshness
	 * escape here: a stale in-flight record still blocks, and the monitor that lost the claim resolves it to
	 * {@code abandoned} within the staleness window. Overwriting a stale record instead would mean running an
	 * occurrence a dead node may have already half-executed.
	 *
	 * @param record  The record as this node sees it, or null
	 * @param cluster The cluster service
	 *
	 * @return A human-readable reason to stand down, or null to proceed
	 */
	private static String blockingClaimReason( IStruct record, ClusterService cluster, ClusterConfig config ) {
		if ( record == null ) {
			return null;
		}
		String	existingStatus	= getString( record, Key.status );
		String	existingOwner	= getString( record, Key.nodeId );
		boolean	mine			= cluster.getNodeId().equals( existingOwner );

		if ( isInFlight( existingStatus ) ) {
			if ( mine ) {
				// Our own candidate write from an earlier attempt: allowed through so a retry can proceed
				return null;
			}
			return "already claimed by " + existingOwner + " (" + existingStatus + ")";
		}

		// Terminal. This occurrence is resolved and must never be run again - including by a node whose first
		// read happened before the owner wrote its outcome. Without this, the re-read before the write lets a
		// delayed node claim an occurrence that already finished.
		return "already handled by " + existingOwner + " (" + existingStatus + ")";
	}

	/**
	 * Why this occurrence must be skipped because a <b>different</b> occurrence of the same task is still
	 * running here or elsewhere, under {@code overlap=skip}.
	 * <p>
	 * Each occurrence has its own key, so there is nothing to contend with - which is exactly why this lookup
	 * exists. It reads the task-level pointer that every successful claim updates, and blocks only when that
	 * pointer names a different occurrence that is in flight and fresh. The pointer is advisory: if it is
	 * clobbered or missing the task simply overlaps, and no claim safety depends on it.
	 *
	 * @param task       The task
	 * @param cluster    The cluster service
	 * @param cache      The coordination cache
	 * @param occurrence The occurrence this node wants to claim
	 *
	 * @return A human-readable reason to skip, or null
	 */
	private static String overlappingClaimReason( ScheduledTask task, ClusterService cluster, ICacheProvider cache, String occurrence ) {
		if ( !"skip".equals( effectiveOverlap( task, cluster ) ) ) {
			return null;
		}
		IStruct pointer;
		try {
			pointer = asStruct( cache.get( buildTaskKey( cluster, task ) ).orElse( null ) );
		} catch ( Throwable t ) {
			// A failed read must not block work: treat it as no overlap
			return null;
		}
		if ( pointer == null ) {
			return null;
		}
		String	pointerOccurrence	= getString( pointer, Key.occurrenceId );
		String	pointerStatus		= getString( pointer, Key.status );
		if ( pointerOccurrence == null || occurrence.equals( pointerOccurrence ) ) {
			return null;
		}
		if ( !isInFlight( pointerStatus ) || !isFresh( pointer, cluster.getConfig() ) ) {
			return null;
		}
		return "previous occurrence " + pointerOccurrence + " still in flight on " + getString( pointer, Key.nodeId );
	}

	/**
	 * Write the advisory task-level pointer to the occurrence this node is now running.
	 * <p>
	 * Used only by the overlap check above, never for claim safety - each occurrence's real state lives in its
	 * own key. A clobbered pointer can only cause a task to overlap that should have skipped.
	 */
	private static void writeTaskPointer( ClusterService cluster, ICacheProvider cache, ScheduledTask task, String occurrence ) {
		try {
			cache.set(
			    buildTaskKey( cluster, task ),
			    Struct.ofNonConcurrent(
			        Key.occurrenceId, occurrence,
			        Key.nodeId, cluster.getNodeId(),
			        Key.status, "running",
			        Key.heartbeatAt, Instant.now()
			    ),
			    claimTtlSeconds( cluster.getConfig() )
			);
		} catch ( Throwable t ) {
			logger.warn( "Could not write task pointer for [{}]: {}", task.getName(), t.getMessage() );
		}
	}

	/**
	 * Whether a claim is currently being worked or about to be. {@code claimed} is the candidate write,
	 * written before verification and promoted to {@code running} afterwards; both mean "hands off".
	 *
	 * @param status The recorded status
	 *
	 * @return True when the occurrence is in flight
	 */
	private static boolean isInFlight( String status ) {
		return "running".equals( status ) || "claimed".equals( status );
	}

	/**
	 * Is this record's heartbeat stale - that is, has its owner stopped beating it for long enough that it must be
	 * presumed gone?
	 *
	 * @param record The claim record
	 * @param config The cluster config
	 *
	 * @return true when the record is in flight and its heartbeat is past the staleness threshold
	 */
	public static boolean isStale( IStruct record, ClusterConfig config ) {
		if ( record == null || !isInFlight( getString( record, Key.status ) ) ) {
			return false;
		}
		return !isFresh( record, config );
	}

	/**
	 * Mark a stale claim abandoned on behalf of a node that has disappeared, and log it once.
	 * <p>
	 * Used when a node dies holding a run no peer ever observed - claimed and killed inside one heartbeat, for
	 * instance. Those runs appear in nobody's running list, so without this their occurrence record simply expires
	 * after its TTL and nobody ever reports them: per-occurrence keys are only read by the claim path and by the
	 * node that lost the claim.
	 *
	 * @param cluster    The cluster service
	 * @param taskName   The task name, for the log line
	 * @param key        The occurrence cache key
	 * @param occurrence The occurrence id
	 * @param record     The stale record
	 *
	 * @return true when it was marked abandoned
	 */
	public static boolean markAbandoned( ClusterService cluster, String taskName, String key, String occurrence, IStruct record ) {
		String	owner		= getString( record, Key.nodeId );
		IStruct	abandoned	= Struct.ofNonConcurrent(
		    Key.claimId, getString( record, Key.claimId ),
		    Key.occurrenceId, occurrence,
		    Key.nodeId, owner,
		    Key.status, "abandoned",
		    Key.claimedAt, record.get( Key.claimedAt ),
		    Key.heartbeatAt, record.get( Key.heartbeatAt ),
		    Key.finishedAt, Instant.now(),
		    Key.lastResult, "its node disappeared - reported by " + cluster.getNodeId()
		);
		try {
			cluster.getCache().set( key, abandoned, claimTtlSeconds( cluster.getConfig() ) );
		} catch ( Throwable t ) {
			logger.warn( "Could not mark occurrence [{}] abandoned: {}", occurrence, t.getMessage() );
			return false;
		}
		logger.warn(
		    "Task [{}] occurrence [{}] ABANDONED by [{}] - its node disappeared, and it will not be re-run",
		    taskName,
		    occurrence,
		    owner
		);
		return true;
	}

	/**
	 * Log that an occurrence was abandoned, once per occurrence - not on every poll, which would flood the log for
	 * a task that runs every few seconds.
	 *
	 * @param task       The task
	 * @param occurrence The abandoned occurrence
	 * @param owner      The node that had claimed it
	 */
	private static void logAbandonedOnce( ScheduledTask task, String occurrence, String owner ) {
		Object alreadyLogged = task.getMetaKey( "abandonedLogged" );
		if ( occurrence != null && occurrence.equals( alreadyLogged ) ) {
			return;
		}
		task.setMetaKey( "abandonedLogged", occurrence );
		logger.warn(
		    "Task [{}] occurrence [{}] ABANDONED by [{}] - its heartbeat stopped, and it will not be re-run",
		    task.getName(),
		    occurrence,
		    owner == null ? "unknown" : owner
		);
	}

	/**
	 * The shortest gap between two consecutive fires of a cron expression, in whole seconds.
	 * <p>
	 * A clustered task can only be claimed once per settle-and-verify cycle, so a cron that fires faster than that
	 * cannot be serviced - `* * * * * *` will fire sixty times a minute no matter what the cluster does. This is
	 * measured rather than pattern-matched: the expression is asked for its next few fire times and the smallest
	 * gap between them is taken, so any way of writing "every second" is caught.
	 * <p>
	 * Returns 0 when the interval cannot be determined (an unparseable or one-shot expression), which leaves the
	 * task exempt rather than rejecting something we do not understand.
	 *
	 * @param expression The cron expression
	 *
	 * @return The shortest interval in seconds, or 0 when unknown
	 */
	public static long cronIntervalSeconds( String expression ) {
		if ( expression == null || expression.isBlank() ) {
			return 0;
		}
		try {
			var				cron	= ortus.boxlang.runtime.async.CronExpression.parse( expression );
			LocalDateTime	from	= LocalDateTime.now();
			// Average the fires over ten minutes rather than measuring adjacent gaps: a `*/8` cron is not uniform
			// across the minute boundary (…:56 then :00 is a 4s gap), so a min-gap reading would call it a 4s task.
			// The average is what the schedule actually delivers.
			var				window	= cron.fireTimesBetween( from, from.plusMinutes( 10 ) );
			if ( window.isEmpty() || window.get().count() < 2 ) {
				// Fewer than two fires in ten minutes: far above any floor, so nothing to enforce
				return 0;
			}
			return Math.max( 1, Math.round( 600.0 / window.get().count() ) );
		} catch ( Throwable t ) {
			return 0;
		}
	}

	/**
	 * The effective overlap policy: the task's own setting, else the scheduler default, else "skip".
	 *
	 * @param task    The task
	 * @param cluster The cluster service
	 *
	 * @return "skip" or "run"
	 */
	public static String effectiveOverlap( ScheduledTask task, ClusterService cluster ) {
		if ( task.getOverlap() != null && !task.getOverlap().isBlank() ) {
			return task.getOverlap();
		}
		// Scheduler-owned default: overlapping runs is a scheduling policy, not a cluster concern
		String schedulerDefault = BoxRuntime.getInstance().getConfiguration().scheduler.taskOverlapPolicy;
		return ( schedulerDefault == null || schedulerDefault.isBlank() ) ? DEFAULT_TASK_OVERLAP_POLICY : schedulerDefault;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Occurrence identity
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Compute the occurrence id for this tick.
	 * <p>
	 * Every node must compute the same value from local information alone, and a late tick must map to
	 * the occurrence it was due for rather than the one after it. That is why this uses the <b>due
	 * instant</b> and not the wall clock.
	 *
	 * @param task       The task
	 * @param dueInstant When the tick was due, or null
	 *
	 * @return The occurrence id
	 */
	public static String occurrenceId( ScheduledTask task, Instant dueInstant ) {
		Instant			anchor		= dueInstant != null ? dueInstant : Instant.now();
		long			period		= task.getPeriod();
		ZoneId			zone		= task.getTimezone() != null ? task.getTimezone() : ZoneId.systemDefault();
		LocalDateTime	dueLocal	= LocalDateTime.ofInstant( anchor, zone );

		// Cron and calendar tasks: the occurrence is the calendar second it fired for
		if ( period <= 0 || task.getCronExpression() != null ) {
			return occurrenceId( anchor, zone );
		}

		// Interval tasks: bucket the due instant, so a late tick still lands in its own bucket
		long periodMillis = task.getPeriodMillis();
		if ( periodMillis <= 0 ) {
			return dueLocal.format( OCCURRENCE_FORMAT );
		}
		return String.valueOf( anchor.toEpochMilli() / periodMillis );
	}

	/**
	 * The occurrence id for a cron-style fire: the calendar second it was due for.
	 * <p>
	 * Exposed separately from {@link #occurrenceId(ScheduledTask, Instant)} so anything that has to name a
	 * fire the same way - a harness reporting a task that never goes through the claim path, for instance -
	 * produces an identical, sortable id instead of inventing its own format.
	 *
	 * @param dueInstant When the tick was due
	 * @param zone       The zone to render it in, or null for the system default
	 *
	 * @return The occurrence id
	 */
	public static String occurrenceId( Instant dueInstant, ZoneId zone ) {
		ZoneId effective = zone == null ? ZoneId.systemDefault() : zone;
		return LocalDateTime.ofInstant( dueInstant, effective ).format( OCCURRENCE_FORMAT );
	}

	/**
	 * --------------------------------------------------------------------------
	 * Delay
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Compute the delay before writing a claim.
	 * <p>
	 * Eager mode is a plain random delay. Balanced mode ranks the live nodes by their contributed
	 * {@code scheduler.taskSeconds}, giving the least busy node 0 and the most busy
	 * {@code maxPushbackMillis}, evenly spaced - plus tie jitter, because task-seconds are integers and
	 * ties are common. With no peers visible it falls back to the eager random delay, so a single-node
	 * deployment pays nothing for balanced mode.
	 *
	 * @return The delay in milliseconds
	 */
	public static long computeDelayMillis( ScheduledTask task, ClusterService cluster, ClusterConfig config ) {
		long randomDelay = randomBetween( 0, Math.max( 1, RANDOM_DELAY_MILLIS ) );

		if ( !isBalanced() ) {
			return randomDelay;
		}

		IStruct peers = cluster.getPeers();
		if ( peers.isEmpty() ) {
			// No peers: nothing to balance against
			return randomDelay;
		}

		// Rank every live node by its contributed load. Rank rather than a ratio: one outlier would
		// otherwise peg everyone else to zero and they would all tie.
		String					selfId	= cluster.getNodeId();
		java.util.List<Double>	loads	= new java.util.ArrayList<>();
		double					myLoad	= taskSecondsOf( cluster.getNodeId(), cluster );
		loads.add( myLoad );

		for ( Key peer : peers.keySet() ) {
			loads.add( taskSecondsOf( peer.getName(), cluster ) );
		}
		loads.sort( Comparator.naturalOrder() );

		int	myRank	= loads.indexOf( myLoad );   // first index of an equal value
		int	last	= loads.size() - 1;

		if ( last <= 0 ) {
			return randomDelay;
		}

		// Least busy waits 0, most busy waits maxPushbackMillis, evenly spaced in between
		long pushback = Math.round( ( double ) MAX_PUSHBACK_MILLIS * myRank / last );
		return pushback + randomBetween( 0, Math.max( 1, TIE_JITTER_MILLIS ) );
	}

	private static double taskSecondsOf( String nodeId, ClusterService cluster ) {
		// Contributed by the scheduler as attributes.scheduler.taskSeconds; missing means idle
		return toDouble( cluster.getNodeAttribute(
		    nodeId,
		    Key.attributes.getName() + "." + Key.scheduler.getName() + "." + Key.taskSeconds.getName(),
		    0
		) );
	}

	/**
	 * --------------------------------------------------------------------------
	 * Claim record
	 * --------------------------------------------------------------------------
	 */

	private static IStruct buildRecord( ClusterService cluster, ScheduledTask task, String occurrence, String claimId, String status ) {
		return Struct.ofNonConcurrent(
		    Key.claimId, claimId,
		    Key.occurrenceId, occurrence,
		    Key.nodeId, cluster.getNodeId(),
		    Key.hostname, ortus.boxlang.runtime.net.NetworkUtil.getLocalHostname(),
		    Key.ip, ortus.boxlang.runtime.net.NetworkUtil.getLocalIPAddress(),
		    Key.status, status,
		    Key.claimedAt, Instant.now(),
		    Key.heartbeatAt, Instant.now()
		);
	}

	/**
	 * The claim TTL.
	 * <p>
	 * Deliberately long. The record is the only place a peer can learn what another node is working on, and a
	 * short TTL meant it could expire before any peer looked - for a monthly task, that is always. A day costs
	 * one small entry per task, keeps the last known state visible, and is still refreshed by the run heartbeat
	 * so a long job keeps its claim alive.
	 */
	public static final long CLAIM_RECORD_TTL_SECONDS = 86_400;

	private static long claimTtlSeconds( ClusterConfig config ) {
		return Math.max( CLAIM_RECORD_TTL_SECONDS, config.heartbeatSeconds * 4 );
	}

	private static boolean settleAndVerify( ICacheProvider cache, String key, ClusterService cluster, String occurrence,
	    String claimId ) {
		// Deliberately NOT abortable once the claim is written. Bailing here would leave a `claimed` record that
		// other nodes must respect until its heartbeat goes stale, and the occurrence would then be skipped by
		// everyone - worse than taking the couple of seconds needed to finish the attempt we already started.
		sleepQuietly( SETTLE_MILLIS + randomBetween( 0, ( long ) Math.max( 0, SETTLE_MILLIS * 0.25 ) ) );

		IStruct confirm = asStruct( cache.get( key ).orElse( null ) );
		if ( confirm == null ) {
			return false;
		}
		return claimId.equals( getString( confirm, Key.claimId ) )
		    && cluster.getNodeId().equals( getString( confirm, Key.nodeId ) )
		    && occurrence.equals( getString( confirm, Key.occurrenceId ) );
	}

	/**
	 * Sleep for a while, but give up immediately once the scheduler begins shutting down.
	 * <p>
	 * This is only safe <b>before</b> a claim record is written: at that point nothing is recorded, so another
	 * node can take the occurrence with no consequence. Once the record exists, an attempt must be carried to
	 * a conclusion - see {@link #settleAndVerify}.
	 *
	 * @param task   The task being claimed
	 * @param millis How long to pause
	 *
	 * @return false when shutdown began and the caller should abandon the attempt
	 */
	private static boolean pause( ScheduledTask task, long millis ) {
		long deadline = System.currentTimeMillis() + millis;
		while ( System.currentTimeMillis() < deadline ) {
			if ( isShuttingDown( task ) ) {
				return false;
			}
			sleepQuietly( Math.min( SHUTDOWN_POLL_MILLIS, Math.max( 1, deadline - System.currentTimeMillis() ) ) );
		}
		return !isShuttingDown( task );
	}

	private static boolean isShuttingDown( ScheduledTask task ) {
		if ( !task.hasScheduler() || task.getScheduler() == null ) {
			return false;
		}
		return task.getScheduler().isShuttingDown();
	}

	private static boolean isFresh( IStruct record, ClusterConfig config ) {
		Object heartbeat = record.get( Key.heartbeatAt );
		if ( ! ( heartbeat instanceof Instant heartbeatAt ) ) {
			return false;
		}
		return heartbeatAt.plusSeconds( config.heartbeatSeconds * 3 ).isAfter( Instant.now() );
	}

	/**
	 * Build the cache key for a task, using the cluster service so both subsystems agree on the shape.
	 */
	public static String buildTaskKey( ClusterService cluster, ScheduledTask task ) {
		return buildTaskKey( cluster, task, null );
	}

	/**
	 * The cache key for <b>one occurrence</b> of a task.
	 * <p>
	 * One key per occurrence, not one slot per task. A single slot meant an older occurrence's write could
	 * destroy a newer occurrence's guard - which is what produced duplicate runs, and it is why the claim
	 * protocol had to carry rules like "an older occurrence may not displace a newer one". With a key per
	 * occurrence, two occurrences never contend for the same entry, and an observer marking a stale entry
	 * abandoned can only ever touch that one occurrence.
	 *
	 * @param cluster    The cluster service
	 * @param task       The task
	 * @param occurrence The occurrence id, or null for the task-level prefix
	 *
	 * @return The cache key
	 */
	public static String buildTaskKey( ClusterService cluster, ScheduledTask task, String occurrence ) {
		String	schedulerName	= task.hasScheduler() ? task.getScheduler().getSchedulerName() : "";
		String	application		= "";
		if ( task.hasScheduler() && task.getScheduler().getContext() != null ) {
			application = Optional.ofNullable( task.getScheduler().getContext().getApplicationName() ).orElse( "" );
		}
		String base = cluster.buildTaskKey( application, schedulerName, task.getName() );
		return occurrence == null ? base : base + ":occurrence:" + occurrence;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Run lifecycle
	 * --------------------------------------------------------------------------
	 */

	/**
	 * A held claim. Finishing it writes the outcome, and heartbeating it keeps a long run's claim alive.
	 * <p>
	 * The heartbeat doubles as the fence: if two nodes ever both get past verification, each heartbeat
	 * overwrites the other, so each reads back a record that is not its own. That node logs a duplicate
	 * alert while the job is still running instead of only at the end.
	 */
	public static class Claim {

		private final ScheduledTask		task;
		private final ClusterService	cluster;
		private final ICacheProvider	cache;
		private final String			key;
		private final String			occurrence;
		private final String			claimId;
		private final AtomicBoolean		finished	= new AtomicBoolean( false );
		private volatile boolean		lostClaim	= false;

		Claim( ScheduledTask task, ClusterService cluster, ICacheProvider cache, String key, String occurrence, String claimId ) {
			this.task		= task;
			this.cluster	= cluster;
			this.cache		= cache;
			this.key		= key;
			this.occurrence	= occurrence;
			this.claimId	= claimId;
		}

		/**
		 * Register this claim with the single maintenance thread so the record keeps a fresh heartbeat. No
		 * thread is started here: one thread beats every claim this node holds.
		 *
		 * @return This claim
		 */
		public Claim startHeartbeat() {
			HELD_RUNS.put( this.key, this );
			startMaintenanceThread();
			return this;
		}

		/**
		 * One heartbeat pass for this claim, run by the maintenance thread.
		 */
		void beat() {
			// Never resurrect a finished run: finish() writes the outcome, and a beat that landed after it would
			// put the record back to `running` so every peer then reports the completed occurrence abandoned.
			if ( this.finished.get() ) {
				HELD_RUNS.remove( this.key );
				return;
			}
			try {
				IStruct current = asStruct( this.cache.get( this.key ).orElse( null ) );
				if ( !isMine( current ) ) {
					this.lostClaim = true;
					logger.error(
					    "Task [{}] occurrence [{}] lost its claim while running (now held by [{}]) - duplicate execution suspected",
					    this.task.getName(),
					    this.occurrence,
					    current == null ? "nobody" : getString( current, Key.nodeId )
					);
					HELD_RUNS.remove( this.key );
					return;
				}
				IStruct running = buildRecord( this.cluster, this.task, this.occurrence, this.claimId, "running" );
				this.cache.set( this.key, running, claimTtlSeconds( this.cluster.getConfig() ) );
				// Refresh the advisory pointer too. It drives cross-node `overlap=skip`, so leaving it unbeaten meant
				// the block expired partway through a long run and a peer would start the next occurrence anyway.
				writeTaskPointer( this.cluster, this.cache, this.task, this.occurrence );
			} catch ( Throwable t ) {
				logger.warn( "Could not heartbeat occurrence [{}]: {}", this.occurrence, t.getMessage() );
			}
		}

		private boolean isMine( IStruct record ) {
			return record != null
			    && claimId.equals( getString( record, Key.claimId ) )
			    && cluster.getNodeId().equals( getString( record, Key.nodeId ) );
		}

		/**
		 * Did this run lose its claim while executing?
		 *
		 * @return true when the claim was observed to be held by someone else
		 */
		public boolean lostClaim() {
			return lostClaim;
		}

		/**
		 * The cache key this claim owns: one per occurrence. Exposed so callers and tests can read the record
		 * without re-deriving the occurrence id.
		 *
		 * @return The cache key
		 */
		public String cacheKey() {
			return this.key;
		}

		/**
		 * Finish the claim: fence-check, then record the outcome.
		 *
		 * @param status  "success" or "failed"
		 * @param message Optional detail, may be null
		 */
		public void finish( String status, String message ) {
			if ( !finished.compareAndSet( false, true ) ) {
				return;
			}

			// Unregister first, so the maintenance thread can never beat this record again - the ordering that
			// a per-claim thread had to be interrupted and joined to achieve.
			HELD_RUNS.remove( this.key );

			IStruct current = asStruct( cache.get( key ).orElse( null ) );
			if ( !isMine( current ) ) {
				logger.error(
				    "Task [{}] occurrence [{}] was no longer owned at completion (now [{}]) - duplicate execution suspected",
				    task.getName(),
				    occurrence,
				    current == null ? "nobody" : getString( current, Key.nodeId )
				);
				return;
			}

			IStruct done = Struct.ofNonConcurrent(
			    Key.claimId, claimId,
			    Key.occurrenceId, occurrence,
			    Key.nodeId, cluster.getNodeId(),
			    Key.status, status,
			    Key.claimedAt, current.get( Key.claimedAt ),
			    Key.heartbeatAt, Instant.now(),
			    Key.finishedAt, Instant.now(),
			    Key.lastResult, message
			);
			try {
				cache.set( key, done, claimTtlSeconds( cluster.getConfig() ) );
			} catch ( Throwable t ) {
				logger.error( "Task [{}] could not record its outcome: {}", task.getName(), t.getMessage() );
			}

			// A cut-short run is the one outcome nothing else will ever report: with one key per occurrence no
			// peer reads this occurrence again, so silence here would make an interrupted run invisible. Log it
			// in the same shape a peer uses, so operators (and the soak) see it either way.
			if ( "interrupted".equals( status ) ) {
				logger.warn(
				    "Task [{}] occurrence [{}] INTERRUPTED on [{}] - shutdown stopped the run, and it will not be re-run",
				    task.getName(),
				    occurrence,
				    cluster.getNodeId()
				);
			} else if ( "abandoned".equals( status ) ) {
				logger.warn(
				    "Task [{}] occurrence [{}] ABANDONED by [{}] - it will not be re-run",
				    task.getName(),
				    occurrence,
				    cluster.getNodeId()
				);
			}
			// Tell the node record this run has ended, so peers report it on their next sweep rather than
			// waiting for this task to fire again.
			cluster.trackRunFinished( schedulerNameOf( task ), task.getName(), occurrence, status );
		}
	}

	/**
	 * --------------------------------------------------------------------------
	 * Helpers
	 * --------------------------------------------------------------------------
	 */

	private static ClusterService getClusterServiceOrNull() {
		return BoxRuntime.getInstance().getClusterService();
	}

	/** The scheduler name a task belongs to, or an empty string when it has none. */
	private static String schedulerNameOf( ScheduledTask task ) {
		return ( task.hasScheduler() && task.getScheduler() != null ) ? task.getScheduler().getSchedulerName() : "";
	}

	private static IStruct asStruct( Object value ) {
		if ( value instanceof IStruct struct ) {
			return struct;
		}
		return null;
	}

	private static String getString( IStruct struct, Key key ) {
		Object value = struct == null ? null : struct.get( key );
		return value == null ? null : value.toString();
	}

	private static double toDouble( Object value ) {
		if ( value instanceof Number number ) {
			return number.doubleValue();
		}
		if ( value != null ) {
			Attempt<Double> attempt = ortus.boxlang.runtime.dynamic.casters.DoubleCaster.attempt( value );
			if ( attempt.isPresent() ) {
				return attempt.get();
			}
		}
		return 0;
	}

	private static long randomBetween( long minInclusive, long maxExclusive ) {
		if ( maxExclusive <= minInclusive ) {
			return minInclusive;
		}
		return ThreadLocalRandom.current().nextLong( minInclusive, maxExclusive );
	}

	private static void sleepQuietly( long millis ) {
		if ( millis <= 0 ) {
			return;
		}
		try {
			Thread.sleep( millis );
		} catch ( InterruptedException e ) {
			Thread.currentThread().interrupt();
		}
	}

	private static void log( ScheduledTask task, String occurrence, String message ) {
		logger.info( "Task [{}] occurrence [{}] {}", task.getName(), occurrence, message );
	}

	/**
	 * Convenience: an {@link Instant} for a task's local "now", used by callers that need a due anchor.
	 *
	 * @param task The task
	 *
	 * @return The current instant in the task's timezone
	 */
	public static Instant nowFor( ScheduledTask task ) {
		ZoneId zone = task.getTimezone() != null ? task.getTimezone() : ZoneId.systemDefault();
		return ZonedDateTime.now( zone ).toInstant();
	}

	/**
	 * Convenience for callers that need the configured minimum period.
	 *
	 * @param config The cluster configuration
	 *
	 * @return The minimum period in seconds
	 */
	public static long minimumPeriodSeconds( ClusterConfig config ) {
		return minimumPeriodSeconds();
	}

	/**
	 * Convenience: does this task's period satisfy the cluster floor?
	 *
	 * @param task   The task
	 * @param config The cluster configuration
	 *
	 * @return true when the period is at least the floor
	 */
	public static boolean meetsMinimumPeriod( ScheduledTask task, ClusterConfig config ) {
		long periodSeconds = periodSeconds( task );
		return periodSeconds <= 0 || periodSeconds >= minimumPeriodSeconds();
	}

	/**
	 * A task's period in whole seconds, or 0 when it has no fixed period (cron, one-shot).
	 *
	 * @param task The task
	 *
	 * @return The period in seconds, or 0
	 */
	public static long periodSeconds( ScheduledTask task ) {
		// A cron task's period is its *poll* cadence - one second for a second-level cron - not the cadence of its
		// occurrences, so reading it directly made every clustered cron task look like a 1s task and rejected it
		// against the minimum period floor. Instead, derive the cron's own shortest interval: a task firing every
		// second really cannot service a claim, while a nightly task obviously can.
		if ( task.getMetaKey( "cronExpression" ) != null ) {
			return cronIntervalSeconds( task.getCronExpression() );
		}

		// A fixed-delay ("spaced") schedule repeats just like a period one, it simply measures the gap from the
		// end of the previous run rather than from its start. Counting it as "no cadence" let a one-second
		// spaced-delay clustered task pass the floor check even though it can never complete a claim in time.
		long spacedMillis = task.getTimeUnit() != null
		    ? task.getTimeUnit().toMillis( task.getSpacedDelay() )
		    : 0;
		if ( spacedMillis > 0 ) {
			return Math.max( 1, Duration.ofMillis( spacedMillis ).toSeconds() );
		}

		long periodMillis = task.getPeriodMillis();
		if ( periodMillis <= 0 ) {
			return 0;
		}
		return Math.max( 1, Duration.ofMillis( periodMillis ).toSeconds() );
	}
}
