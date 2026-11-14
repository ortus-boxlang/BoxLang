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

	private static final BoxLangLogger logger = BoxRuntime.getInstance().getLoggingService().SCHEDULER_LOGGER;

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
		String			key			= buildTaskKey( cluster, task );
		String			occurrence	= occurrenceId( task, dueInstant );

		// 1. read
		IStruct			record		= asStruct( cache.get( key ).orElse( null ) );

		// 2. decide
		if ( record != null ) {
			String	existingOccurrence	= getString( record, Key.occurrenceId );
			String	existingStatus		= getString( record, Key.status );
			String	existingOwner		= getString( record, Key.nodeId );
			boolean	live				= isFresh( record, config );

			if ( occurrence.equals( existingOccurrence ) ) {
				if ( isInFlight( existingStatus ) ) {
					if ( live ) {
						log( task, occurrence, "in progress on " + existingOwner + " - skipping" );
					} else {
						logAbandonedOnce( task, occurrence, existingOwner );
					}
					// An occurrence is never re-run, fresh or stale: that is the whole guarantee
					return null;
				}
				if ( "abandoned".equals( existingStatus ) ) {
					logAbandonedOnce( task, occurrence, existingOwner );
					return null;
				}
				if ( "success".equals( existingStatus ) || "failed".equals( existingStatus ) ) {
					log( task, occurrence, "already handled by " + existingOwner + " (" + existingStatus + ") - skipping" );
					return null;
				}
			} else {
				// A different occurrence is in the slot. Everything that should stop us is decided in one
				// place, so the read here, the re-read after the delay and the write all agree.
				String blocked = blockingClaimReason( task, record, cluster, occurrence, dueInstant );
				if ( blocked != null ) {
					log( task, occurrence, "skipped - " + blocked );
					return null;
				}
				if ( isInFlight( existingStatus ) && !live ) {
					// The previous occurrence's runner is gone. Report it, then let this occurrence run.
					logAbandonedOnce( task, existingOccurrence, existingOwner );
				}
			}
		}

		// 3. delay
		if ( !pause( task, computeDelayMillis( task, cluster, config ) ) ) {
			log( task, occurrence, "abandoning the claim - the scheduler is shutting down" );
			return null;
		}

		// 4. re-read: someone else may have claimed it while we waited
		String blockedBy = blockingClaimReason( task, asStruct( cache.get( key ).orElse( null ) ), cluster, occurrence, dueInstant );
		if ( blockedBy != null ) {
			log( task, occurrence, "backing off - " + blockedBy );
			return null;
		}

		// 5-6. write, settle, verify
		int attempts = Math.max( 1, ATTEMPTS );
		for ( int attempt = 1; attempt <= attempts; attempt++ ) {
			// Re-check immediately before every write, including retries. The record has room for one
			// occurrence, so a write here can destroy a newer occurrence's guard - which is how a third
			// node ends up running that newer occurrence a second time.
			blockedBy = blockingClaimReason( task, asStruct( cache.get( key ).orElse( null ) ), cluster, occurrence, dueInstant );
			if ( blockedBy != null ) {
				log( task, occurrence, "backing off - " + blockedBy );
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
	 * Why this node must not write a claim, given the record it is looking at, or null when it may.
	 * <p>
	 * The record is keyed per task and holds exactly one occurrence, so there are two ways a write here
	 * would break exactly-once:
	 * <ul>
	 * <li>A node holds the same occurrence in either in-flight state. {@code claimed} is the candidate
	 * write before it is promoted to {@code running}, and it is just as much a claim.</li>
	 * <li>Two occurrences can legitimately be in flight at once (a job longer than its period, or a node
	 * evaluating a bucket that is already behind), so an <b>older</b> occurrence must never overwrite a
	 * <b>newer</b> one. Doing so deletes the newer occurrence's guard, and the next node to evaluate that
	 * newer occurrence finds nothing in its way and runs it a second time.</li>
	 * </ul>
	 * Overlap governs whether a node may run its own occurrences concurrently; it never licenses two nodes
	 * running the same occurrence, which is why the in-flight check applies whatever the overlap policy is.
	 *
	 * @param task       The task
	 * @param record     The claim record as this node sees it, or null
	 * @param cluster    The cluster service
	 * @param occurrence The occurrence this node wants to claim
	 * @param dueInstant When that occurrence was due
	 *
	 * @return A human-readable reason to stand down, or null to proceed
	 */
	private static String blockingClaimReason( ScheduledTask task, IStruct record, ClusterService cluster, String occurrence, Instant dueInstant ) {
		if ( record == null ) {
			return null;
		}

		String	existingOccurrence	= getString( record, Key.occurrenceId );
		String	existingStatus		= getString( record, Key.status );
		String	existingOwner		= getString( record, Key.nodeId );
		boolean	mine				= cluster.getNodeId().equals( existingOwner );
		boolean	inFlight			= isInFlight( existingStatus );
		boolean	sameOccurrence		= occurrence.equals( existingOccurrence );

		// Someone else already holds this occurrence, in either in-flight state. Not "mine" because a node
		// retrying its own candidate write has to be allowed through.
		if ( !mine && inFlight && sameOccurrence ) {
			return "already claimed by " + existingOwner + " (" + existingStatus + ")";
		}

		// A live occurrence in flight blocks the next one, for this node as much as for any other - which is
		// what overlap=skip means on a single node. Keeping this here as well as in the read above means a
		// claim that appeared during the delay is still honoured.
		if ( inFlight && !sameOccurrence && isFresh( record, cluster.getConfig() ) && "skip".equals( effectiveOverlap( task, cluster ) ) ) {
			return "previous occurrence " + existingOccurrence + " still in flight on " + existingOwner;
		}

		// An older occurrence must never displace a newer one
		if ( !mine && recordIsNewerThan( record, dueInstant ) ) {
			return "superseded by " + existingStatus + " " + existingOccurrence + " on " + existingOwner;
		}

		return null;
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
	 * Whether the record belongs to a different occurrence that is newer than the one being evaluated,
	 * decided from the claim timestamp rather than by parsing occurrence ids.
	 *
	 * @param record     The claim record
	 * @param dueInstant When the occurrence being evaluated was due
	 *
	 * @return True when the record describes a newer occurrence
	 */
	private static boolean recordIsNewerThan( IStruct record, Instant dueInstant ) {
		if ( dueInstant == null ) {
			return false;
		}
		Object claimedAt = record.get( Key.claimedAt );
		if ( ! ( claimedAt instanceof Instant claimed ) ) {
			return false;
		}
		return claimed.isAfter( dueInstant );
	}

	/**
	 * Log that an occurrence was abandoned, once per occurrence - not on every poll, which would flood
	 * the log for a task that runs every few seconds.
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
	 * The claim TTL: long enough that the record outlives the window in which this occurrence could
	 * still be evaluated, and refreshed by the run heartbeat so a long job keeps its claim.
	 */
	private static long claimTtlSeconds( ClusterConfig config ) {
		long heartbeat = Math.max( config.heartbeatSeconds, 5 );
		return Math.max( heartbeat * 4, config.heartbeatSeconds * 3 );
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
		String	schedulerName	= task.hasScheduler() ? task.getScheduler().getSchedulerName() : "";
		String	application		= "";
		if ( task.hasScheduler() && task.getScheduler().getContext() != null ) {
			application = Optional.ofNullable( task.getScheduler().getContext().getApplicationName() ).orElse( "" );
		}
		return cluster.buildTaskKey( application, schedulerName, task.getName() );
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
		private Thread					heartbeatThread;
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
		 * Start heartbeating the record so a job of any length keeps its claim.
		 *
		 * @return This claim
		 */
		public Claim startHeartbeat() {
			long	intervalSeconds	= Math.max( 1, Math.min( cluster.getConfig().heartbeatSeconds, 30 ) );
			Thread	thread			= new Thread( () -> {
										while ( !finished.get() ) {
											try {
												Thread.sleep( intervalSeconds * 1000L );
											} catch ( InterruptedException e ) {
												Thread.currentThread().interrupt();
												return;
											}
											if ( finished.get() ) {
												return;
											}
											heartbeat();
										}
									}, "boxlang-cluster-task-heartbeat-" + task.getName() );
			thread.setDaemon( true );
			this.heartbeatThread = thread;
			thread.start();
			return this;
		}

		private void heartbeat() {
			IStruct current = asStruct( cache.get( key ).orElse( null ) );
			if ( !isMine( current ) ) {
				lostClaim = true;
				logger.error(
				    "Task [{}] occurrence [{}] lost its claim while running (now held by [{}]) - duplicate execution suspected",
				    task.getName(),
				    occurrence,
				    current == null ? "nobody" : getString( current, Key.nodeId )
				);
				return;
			}
			IStruct running = buildRecord( cluster, task, occurrence, claimId, "running" );
			cache.set( key, running, claimTtlSeconds( cluster.getConfig() ) );
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
		 * Finish the claim: fence-check, then record the outcome.
		 *
		 * @param status  "success" or "failed"
		 * @param message Optional detail, may be null
		 */
		public void finish( String status, String message ) {
			if ( !finished.compareAndSet( false, true ) ) {
				return;
			}

			Thread thread = this.heartbeatThread;
			if ( thread != null ) {
				thread.interrupt();
			}

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
		// A cron task's period is its *poll* cadence - one second for a second-level cron - not the cadence of
		// its occurrences, so it is not a period-bound task at all. Reading the poll cadence as the period made
		// every clustered cron task look like a 1s task and rejected it against the minimum period floor.
		if ( task.getMetaKey( "cronExpression" ) != null ) {
			return 0;
		}

		long periodMillis = task.getPeriodMillis();
		if ( periodMillis <= 0 ) {
			return 0;
		}
		return Math.max( 1, Duration.ofMillis( periodMillis ).toSeconds() );
	}
}
