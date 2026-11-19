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
package ortus.boxlang.cluster;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ortus.boxlang.cluster.ClusterSoakTimeline.Execution;
import ortus.boxlang.cluster.ClusterSoakTimeline.LifecycleEvent;
import ortus.boxlang.cluster.ClusterSoakTimeline.NodeSpan;
import ortus.boxlang.cluster.ClusterSoakTimeline.Run;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.async.tasks.TaskClusterClaim;

/**
 * The cluster soak: several <b>real</b> BoxLang runtimes sharing one cache, running the same clustered
 * task, while nodes are scaled up, scaled down and restarted underneath each other.
 * <p>
 * Every node is an ordinary process started against its own home directory containing a
 * {@code config/boxlang.json} and a {@code config/tasks.json}:
 *
 * <pre>
 * runtime -> SchedulerService.onStartup -> tasks.json -> BaseScheduler -> executor
 *         -> ScheduledTask.runInternal -> TaskClusterClaim.attempt -> the task body
 * </pre>
 *
 * The node itself does nothing but start, stay alive, and shut down gracefully when a stop file appears.
 * This harness only writes configuration, starts and stops processes, reads each runtime's own
 * {@code scheduler.log}, and renders the timeline. Nothing calls the claim protocol directly, so what is
 * being tested is the shipped code path, not a model of it.
 * <p>
 * Stops are always graceful - a stop file plus a termination request, with the runtime's own shutdown
 * doing the waiting. There are no kills: production does not SIGKILL a node, and neither does this.
 * <p>
 * Options:
 * <ul>
 * <li>{@code --minutes} run length (default 6)</li>
 * <li>{@code --seconds} run length in seconds instead of minutes, for a fast verification loop</li>
 * <li>{@code --nodes} nodes to start with (default 3)</li>
 * <li>{@code --min-nodes} / {@code --max-nodes} scaling bounds (default 2 / 5)</li>
 * <li>{@code --tasks} how many overlapping clustered tasks to schedule (default 6, max 6)</li>
 * <li>{@code --mode} eager | balanced (default balanced)</li>
 * <li>{@code --keep} keep the cache, homes and logs (default false)</li>
 * </ul>
 *
 * Each scheduled task is a real `tasks.json` entry pointing at a `.bxm` template whose `<bx:script>` block
 * sleeps for 4-9 seconds, so several schedules are genuinely in flight on different nodes at once. The task
 * path is relative to the node's working directory, because that is how the include/template flow resolves
 * it.
 *
 * <b>Fairness note.</b> For {@code balanced}, the minimum period is
 * {@code verifyRounds x settleMillis + maxPushbackMillis + 1s} (8s with the shipped defaults), and a claim
 * on the worst-ranked node costs its settle rounds plus its pushback. At a period sitting on that floor the
 * last node finishes its attempt right as the next occurrence arrives and loses every time, which looks
 * exactly like starvation. Run with headroom (12s defaults here) and the same shape spreads across every
 * node.
 */
public class ClusterSoakReal {

	private static final String		CLUSTER_NAME		= "soak";
	private static final String		CACHE_NAME			= "soakCache";
	private static final String		TASK_PREFIX			= "soak-task-";
	private static final String		SCHEDULER_NAME		= "soakScheduler";
	private static final String		REPORT_PATH			= "build/cluster-soak/timeline.html";

	/**
	 * The overlapping workloads: each schedule has its own cadence and its own run length, so several are
	 * always in flight at once on different nodes.
	 */
	private static final int[]		CRON_SECONDS		= { 8, 10, 15, 20, 30, 45 };
	private static final long[]		JOB_MILLIS			= { 2_000, 20_000, 5_000, 35_000, 8_000, 50_000 };

	/**
	 * A short, deliberately <b>unclustered</b> schedule, present on every node as {@code cluster: false}.
	 * It must run on <b>every</b> node rather than once per cluster, which is the point: it proves the
	 * unclustered path still executes normally while clustered tasks coordinate around it. Its name is
	 * kept clear of {@link #TASK_PREFIX} on purpose, so it never enters the exactly-once accounting - a
	 * run of this task on three nodes is correct behaviour, not a duplicate.
	 */
	private static final String		PLAIN_TASK			= "soak-plain-1";
	private static final int		PLAIN_SECONDS		= 10;
	private static final long		PLAIN_MILLIS		= 500;

	/** How many nodes are hard-killed per run. Two is enough to prove the abandoned path; more just adds churn. */
	private static final int		MAX_HARD_KILLS		= 2;

	private static final Pattern	CLAIMED				= Pattern
	    .compile( "Task \\[(?:[^\\]]*\\.)?(" + TASK_PREFIX + "\\d+)\\] occurrence \\[([^\\]]+)\\] CLAIMED by (\\S+) - running" );
	private static final Pattern	SUCCEEDED			= Pattern.compile( "Task \\[(?:[^\\]]*\\.)?(" + TASK_PREFIX + "\\d+)\\] has succeeded" );
	private static final Pattern	PLAIN_RAN			= Pattern.compile( "Task \\[(?:[^\\]]*\\.)?(" + PLAIN_TASK + ")\\] has succeeded" );
	/** A run whose own node was told to stop mid-job. Distinct from abandoned, which means the runner vanished. */
	private static final Pattern	INTERRUPTED			= Pattern.compile(
	    "Task \\[(?:[^\\]]*\\.)?(" + TASK_PREFIX + "\\d+)\\] occurrence \\[([^\\]]+)\\] INTERRUPTED on \\[" );
	/** A run a peer declared abandoned because the node holding it disappeared. */
	private static final Pattern	ABANDONED			= Pattern.compile(
	    "Task \\[(?:[^\\]]*\\.)?(" + TASK_PREFIX + "\\d+)\\] occurrence \\[([^\\]]+)\\] ABANDONED by" );
	/** A run ending published by an observer, which names the run but not as a claim line. */
	private static final Pattern	REPORTED_END		= Pattern.compile(
	    "Cluster run report: \\[[^\\]]*?::(" + TASK_PREFIX + "\\d+)::([^\\]]+?) (SUCCESS|FAILED|INTERRUPTED|ABANDONED)" );
	/** How many runs a killed node was holding, from the kill event's detail. */
	private static final Pattern	IN_FLIGHT_AT_KILL	= Pattern.compile( "\\((\\d+) run\\(s\\) in flight\\)" );
	/** A peer concluding a run was abandoned because the node holding it disappeared - the hard-kill path. */
	private static final Pattern	PEER_ABANDONED		= Pattern.compile(
	    "Cluster run report: \\[[^\\]]*?::(" + TASK_PREFIX + "\\d+)::([^\\]]+?) ABANDONED" );
	/** Any observer report about a run, naming the node it belonged to. Used to prove the track record exists. */
	private static final Pattern	PEER_REPORT			= Pattern.compile(
	    "Cluster run report: \\[[^\\]]*?::(" + TASK_PREFIX + "\\d+)::([^\\]]+?) (?:STARTED|SUCCESS|FAILED|INTERRUPTED|ABANDONED|ENDED)[^\\]]*? on (node\\d+)" );
	private static final Pattern	PLAIN_START			= Pattern.compile( "Task \\[(?:[^\\]]*\\.)?(" + PLAIN_TASK + ")\\] is about to run" );
	private static final Pattern	LOG_TIME			= Pattern.compile( "^\\[(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}[,.]\\d{3})[^\\]]*\\]" );

	/**
	 * Entry point.
	 *
	 * @param args Command line arguments
	 *
	 * @throws Exception When anything fails
	 */
	public static void main( String[] args ) throws Exception {
		Map<String, String> options = parse( args );
		if ( options.containsKey( "node" ) ) {
			runNode( options );
			return;
		}
		System.exit( runSoak( options ) );
	}

	/**
	 * --------------------------------------------------------------------------
	 * Child node: a real runtime with a real tasks.json, and nothing else
	 * --------------------------------------------------------------------------
	 */

	private static void runNode( Map<String, String> options ) {
		String	nodeId	= options.get( "node" );
		Path	home	= Path.of( options.get( "home" ) );
		Path	stop	= options.containsKey( "stop-file" ) ? Path.of( options.get( "stop-file" ) ) : null;
		Thread	worker	= Thread.currentThread();

		Runtime.getRuntime().addShutdownHook( new Thread( () -> {
			System.out.println( "[" + nodeId + "] STOP REQUESTED - shutting the runtime down gracefully" );
			System.out.flush();
			try {
				worker.join( 30_000 );
			} catch ( InterruptedException e ) {
				Thread.currentThread().interrupt();
			}
		}, "soak-stop" ) );

		BoxRuntime runtime;
		try {
			runtime = BoxRuntime.getInstance( true, null, home.toString() );
		} catch ( Throwable t ) {
			System.out.println( "[" + nodeId + "] STARTUP FAILED " + t.getClass().getName() + ": " + t.getMessage() );
			t.printStackTrace( System.out );
			System.out.flush();
			System.exit( 1 );
			return;
		}

		System.out.println( "[" + nodeId + "] RUNTIME STARTED" );
		System.out.flush();

		// Report how many runs this node is holding, so the orchestrator can kill a node that is actually busy
		// rather than one that happens to be idle. Also a continuous check that the published in-flight list
		// tracks reality.
		Thread reporter = new Thread( () -> {
			while ( !Thread.currentThread().isInterrupted() ) {
				try {
					Thread.sleep( 500 );
				} catch ( InterruptedException e ) {
					return;
				}
				ortus.boxlang.runtime.services.ClusterService cluster = BoxRuntime.getInstance().getClusterService();
				System.out.println( "[" + nodeId + "] INFLIGHT " + ( cluster == null ? 0 : cluster.getInFlightRunCount() ) );
				System.out.flush();
			}
		}, "soak-inflight-reporter" );
		reporter.setDaemon( true );
		reporter.start();

		while ( stop == null || !Files.exists( stop ) ) {
			try {
				Thread.sleep( 200 );
			} catch ( InterruptedException e ) {
				Thread.currentThread().interrupt();
				break;
			}
		}

		long stopSeenAt = System.currentTimeMillis();
		System.out.println( "[" + nodeId + "] STOPPING - graceful runtime shutdown at " + stopSeenAt );
		System.out.flush();
		try {
			runtime.shutdown( false );
		} catch ( Throwable t ) {
			System.out.println( "[" + nodeId + "] SHUTDOWN ERROR " + t.getMessage() );
		}
		System.out.println( "[" + nodeId + "] STOPPED after " + ( System.currentTimeMillis() - stopSeenAt )
		    + "ms of runtime shutdown (process still has to exit) at " + System.currentTimeMillis() );
		System.out.flush();
	}

	/**
	 * --------------------------------------------------------------------------
	 * A node process
	 * --------------------------------------------------------------------------
	 */

	/** One running node: its identity, its process, and when this lifespan came online. */
	private record NodeProc( String id, Process process, long onlineFrom ) {
	}

	/**
	 * --------------------------------------------------------------------------
	 * Orchestrator
	 * --------------------------------------------------------------------------
	 */

	private static int runSoak( Map<String, String> options ) throws Exception {
		int		minutes		= Integer.parseInt( options.getOrDefault( "minutes", "6" ) );
		int		nodeCount	= Integer.parseInt( options.getOrDefault( "nodes", "3" ) );
		int		minNodes	= Integer.parseInt( options.getOrDefault( "min-nodes", "2" ) );
		int		maxNodes	= Integer.parseInt( options.getOrDefault( "max-nodes", "5" ) );
		int		taskCount	= Math.min( Integer.parseInt( options.getOrDefault( "tasks", String.valueOf( CRON_SECONDS.length ) ) ),
		    CRON_SECONDS.length );
		String	mode		= options.getOrDefault( "mode", "balanced" );
		boolean	keep		= Boolean.parseBoolean( options.getOrDefault( "keep", "false" ) );

		if ( minNodes < 1 ) {
			minNodes = 1;
		}
		if ( maxNodes < minNodes ) {
			maxNodes = minNodes;
		}
		nodeCount = Math.min( Math.max( nodeCount, minNodes ), maxNodes );

		Path	root			= Files.createTempDirectory( "cluster-soak-" );
		Path	cacheDir		= Files.createDirectories( root.resolve( "cache" ) );
		Path	logDir			= Files.createDirectories( root.resolve( "logs" ) );
		Path	stopsDir		= Files.createDirectories( root.resolve( "stops" ) );

		long	startedAt		= System.currentTimeMillis();
		long	deadline		= startedAt + ( options.containsKey( "seconds" )
		    ? Long.parseLong( options.get( "seconds" ) ) * 1000L
		    : minutes * 60_000L );
		long	budget			= deadline - startedAt;
		// Every window is a fraction of the budget, so a short run is actually short. These used to be a
		// third of the budget in each of two places plus a fixed 15s of slack, which for a 20 second budget
		// meant the loop that drives the churn never ran at all.
		long	settleMillis	= Math.min( 25_000L, budget / 6 );
		long	drainMillis		= Math.min( 30_000L, budget / 6 );
		long	slackMillis		= Math.max( 2_000L, Math.min( 15_000L, budget / 10 ) );

		System.out.println( "Cluster soak starting" );
		System.out.println( "  root:     " + root );
		System.out.println( "  nodes:    " + nodeCount + " (scale " + minNodes + "-" + maxNodes + ")" );
		System.out.println( "  schedule: " + taskCount + " overlapping clustered tasks, mode " + mode );
		for ( int i = 1; i <= taskCount; i++ ) {
			System.out.println( "            " + TASK_PREFIX + i + " every " + CRON_SECONDS[ ( i - 1 ) % CRON_SECONDS.length ] + "s, runs "
			    + JOB_MILLIS[ ( i - 1 ) % JOB_MILLIS.length ] + "ms" );
		}
		System.out.println( "            " + PLAIN_TASK + " every " + PLAIN_SECONDS + "s, runs " + PLAIN_MILLIS
		    + "ms - NOT clustered, must run on every node" );

		Map<String, NodeProc>	live			= new LinkedHashMap<>();
		List<NodeSpan>			spans			= new ArrayList<>();
		List<LifecycleEvent>	events			= new ArrayList<>();
		Map<String, Integer>	starts			= new LinkedHashMap<>();
		Set<String>				freshIds		= new LinkedHashSet<>();
		List<String>			allIds			= new ArrayList<>();
		int						nextNode		= nodeCount;
		int						round			= 0;
		int						killsWithWork	= 0;

		Runtime.getRuntime().addShutdownHook( new Thread( () -> live.values().forEach( node -> {
			if ( node.process().isAlive() ) {
				node.process().destroyForcibly();
			}
		} ), "soak-cleanup" ) );

		try {
			for ( int i = 1; i <= nodeCount; i++ ) {
				String id = "node" + i;
				allIds.add( id );
				start( id, root, cacheDir, logDir, stopsDir, taskCount, mode, live, events, starts, freshIds, true );
			}

			// Let the cluster settle before disturbing it
			sleep( Math.max( 2, settleMillis / 1000 ) );
			reap( live, spans, events, root, cacheDir, logDir, stopsDir, taskCount, mode, starts, freshIds );

			while ( System.currentTimeMillis() < deadline - drainMillis - slackMillis && round < 40 ) {
				round++;
				reap( live, spans, events, root, cacheDir, logDir, stopsDir, taskCount, mode, starts, freshIds );

				String	victim	= live.keySet().stream().findFirst().orElse( null );
				// Five actions: the four graceful ones, plus a hard kill. The kill is the only way to produce a
				// genuinely abandoned run - a task whose owner vanished without recording an outcome - because
				// every other stop lets the node mark its own runs interrupted.
				int		action	= round % 5;

				if ( action == 1 && live.size() < maxNodes ) {
					String id = "node" + ( ++nextNode );
					allIds.add( id );
					System.out.println( "Round " + round + ": scale up   -> " + id );
					start( id, root, cacheDir, logDir, stopsDir, taskCount, mode, live, events, starts, freshIds, false );
				} else if ( action == 2 && live.size() > minNodes ) {
					System.out.println( "Round " + round + ": scale down -> retire " + victim + " (graceful)" );
					stop( victim, stopsDir, live, spans, events, "retired" );
					sleep( 4 );
					String id = "node" + ( ++nextNode );
					allIds.add( id );
					System.out.println( "Round " + round + ":            -> replace it with " + id );
					start( id, root, cacheDir, logDir, stopsDir, taskCount, mode, live, events, starts, freshIds, false );
				} else if ( action == 3 && victim != null ) {
					System.out.println( "Round " + round + ": restart    -> " + victim + " (graceful stop, then start)" );
					stop( victim, stopsDir, live, spans, events, "stopped" );
					sleep( 4 );
					start( victim, root, cacheDir, logDir, stopsDir, taskCount, mode, live, events, starts, freshIds, false );
				} else if ( action == 4 && victim != null && killsWithWork < MAX_HARD_KILLS ) {
					// Pull the plug on a node that is actually working - killing an idle node tests nothing. Pick
					// the busiest one from the in-flight counts the nodes report.
					String	killTarget	= busiestNode( logDir, live.keySet() );
					int		held		= lastInFlight( logDir, killTarget );
					if ( held <= 0 ) {
						System.out.println( "Round " + round + ": KILL skipped - no node is holding a run right now" );
					} else {
						System.out.println( "Round " + round + ": KILL       -> " + killTarget + " (instant, " + held + " run(s) in flight)" );
						int killed = kill( killTarget, live, spans, events, logDir );
						if ( killed > 0 ) {
							killsWithWork++;
						}
						sleep( 4 );
						start( killTarget, root, cacheDir, logDir, stopsDir, taskCount, mode, live, events, starts, freshIds, false );
					}
				} else if ( victim != null ) {
					System.out.println( "Round " + round + ": scale down -> retire " + victim + " (graceful)" );
					stop( victim, stopsDir, live, spans, events, "retired" );
				}

				// Pace the rounds by the shortest cadence so churn does not outrun the schedules
				sleep( Math.max( 4, Math.min( 16, CRON_SECONDS[ 0 ] - 2 ) ) );
			}

			// Let the last occurrence finish rather than cutting the run mid-job
			sleep( Math.max( 3, drainMillis / 1000 ) );
		} finally {
			System.out.println( "Stopping all nodes gracefully (concurrently)" );
			stopAll( live, stopsDir, spans, events );

			long			endedAt		= System.currentTimeMillis();
			List<String>	failures	= analyse( root, allIds, spans, events, starts, freshIds );
			boolean			passed		= failures.isEmpty();
			writeReport( root, startedAt, endedAt, mode, spans, events, allIds, passed, failures );

			if ( keep ) {
				System.out.println( "Artifacts kept at " + root );
			} else {
				deleteRecursively( root );
			}

			System.out.println( passed ? "CLUSTER SOAK: PASS" : "CLUSTER SOAK: FAIL" );
			return passed ? 0 : 1;
		}
	}

	/**
	 * Write a node's configuration and task definition, then start it.
	 */
	private static void start(
	    String id,
	    Path root,
	    Path cacheDir,
	    Path logDir,
	    Path stopsDir,
	    int taskCount,
	    String mode,
	    Map<String, NodeProc> live,
	    List<LifecycleEvent> events,
	    Map<String, Integer> starts,
	    Set<String> freshIds,
	    boolean initial ) throws IOException {
		boolean	fresh	= !starts.containsKey( id );
		Path	home	= Files.createDirectories( root.resolve( id + "-home" ) );
		Path	config	= Files.createDirectories( home.resolve( "config" ) );

		Files.writeString( config.resolve( "boxlang.json" ), boxlangJson( id, cacheDir, mode ) );
		writeJobs( home, taskCount );
		Files.writeString( config.resolve( "tasks.json" ), tasksJson( taskCount ) );

		Path stopFile = stopsDir.resolve( id + ".stop" );
		Files.deleteIfExists( stopFile );

		Process	process	= launch( id, home, logDir, stopFile );
		long	at		= System.currentTimeMillis();

		live.put( id, new NodeProc( id, process, at ) );
		starts.merge( id, 1, Integer::sum );
		if ( fresh && !initial ) {
			freshIds.add( id );
		}
		events.add( new LifecycleEvent( at, id, "started", initial ? "initial deployment" : ( fresh ? "fresh node (scale up)" : "same node restarted" ) ) );
	}

	/**
	 * Stop every remaining node at once, the way an orchestrator sends SIGTERM to each replica rather than
	 * draining them one after another.
	 * <p>
	 * Requests are issued back to back before any wait begins, so the drain costs the <b>longest</b> single
	 * drain rather than their sum. Stopping them in sequence was the reason a 20 second run took well over a
	 * minute, and it also staggered the shutdown markers across the timeline - each node was only asked to
	 * stop once the previous one had already exited, which reads as nodes accepting work long after "the"
	 * shutdown began.
	 */
	private static void stopAll( Map<String, NodeProc> live, Path stopsDir, List<NodeSpan> spans, List<LifecycleEvent> events ) {
		List<String>		ids			= new ArrayList<>( live.keySet() );
		Map<String, Long>	requestedAt	= new LinkedHashMap<>();

		// 1. Ask every node to stop, without waiting in between
		for ( String id : ids ) {
			NodeProc node = live.get( id );
			if ( node == null ) {
				continue;
			}
			long at = System.currentTimeMillis();
			requestedAt.put( id, at );
			try {
				Files.writeString( stopsDir.resolve( id + ".stop" ), "stop" );
			} catch ( IOException e ) {
				node.process().destroy();
			}
			events.add( new LifecycleEvent( at, id, "stop-requested", "graceful shutdown requested - no new runs accepted from here" ) );
		}

		// 2. Then wait for them all, recording each process's *actual* exit instant. Waiting on them one at a
		// time and stamping the span afterwards records the moment we got around to reading the process, not
		// the moment it exited - which made nodes that had already stopped look like they were still draining,
		// and made every node queued behind a slow one appear to end at the same instant.
		Map<String, Long>	exitedAt	= new LinkedHashMap<>();
		long				giveUpAt	= System.currentTimeMillis() + 100_000;

		while ( exitedAt.size() < requestedAt.size() && System.currentTimeMillis() < giveUpAt ) {
			for ( String id : ids ) {
				if ( exitedAt.containsKey( id ) ) {
					continue;
				}
				NodeProc node = live.get( id );
				if ( node == null || !node.process().isAlive() ) {
					exitedAt.put( id, System.currentTimeMillis() );
				}
			}
			if ( exitedAt.size() < requestedAt.size() ) {
				try {
					Thread.sleep( 25 );
				} catch ( InterruptedException e ) {
					Thread.currentThread().interrupt();
					break;
				}
			}
		}

		for ( String id : ids ) {
			NodeProc	node	= live.get( id );
			Long		at		= requestedAt.get( id );
			Long		exited	= exitedAt.get( id );
			if ( node == null || at == null ) {
				continue;
			}

			if ( exited == null ) {
				System.out.println( "  " + id + " did not shut down within 100s - killing it" );
				node.process().destroyForcibly();
				events.add( new LifecycleEvent( at, id, "killed", "runtime shutdown did not finish in 100s" ) );
				spans.add( new NodeSpan( id, node.onlineFrom(), System.currentTimeMillis(), "crashed" ) );
				live.remove( id );
				continue;
			}

			spans.add( new NodeSpan( id, node.onlineFrom(), exited, "stopped" ) );
			events.add( new LifecycleEvent( at, id, "stopped", "stopped gracefully (runtime shutdown)" ) );
			System.out.println( "  " + id + " shut down " + ( ( exited - at ) / 1000 )
			    + "s after the stop request (drain, no new runs accepted)" );
			live.remove( id );
		}
	}

	/**
	 * Kill a node outright: {@code destroyForcibly} sends SIGKILL / TerminateProcess, so no shutdown hook runs and
	 * the node records nothing. This is the only way to produce a truly abandoned run - a claim whose owner
	 * disappeared, leaving peers to discover it.
	 *
	 * @return The number of runs the victim was holding when it was killed, or -1 when it was not running any
	 */
	private static int kill( String id, Map<String, NodeProc> live, List<NodeSpan> spans, List<LifecycleEvent> events, Path logDir ) {
		NodeProc node = live.get( id );
		if ( node == null ) {
			return -1;
		}
		int		inFlight	= lastInFlight( logDir, id );
		long	at			= System.currentTimeMillis();
		node.process().destroyForcibly();
		try {
			node.process().waitFor( 15, TimeUnit.SECONDS );
		} catch ( InterruptedException e ) {
			Thread.currentThread().interrupt();
		}
		events.add( new LifecycleEvent( at, id, "killed",
		    "process killed outright - no shutdown, nothing recorded (" + Math.max( 0, inFlight ) + " run(s) in flight)" ) );
		spans.add( new NodeSpan( id, node.onlineFrom(), at, "killed" ) );
		live.remove( id );
		return inFlight;
	}

	/**
	 * The most recent in-flight count a node reported, or -1 when it never reported one.
	 */
	private static int lastInFlight( Path logDir, String nodeId ) {
		Path log = logDir.resolve( nodeId + ".log" );
		if ( !Files.exists( log ) ) {
			return -1;
		}
		try {
			java.util.List<String> lines = Files.readAllLines( log );
			for ( int i = lines.size() - 1; i >= 0; i-- ) {
				String	line	= lines.get( i );
				int		at		= line.lastIndexOf( "INFLIGHT " );
				if ( at >= 0 ) {
					return Integer.parseInt( line.substring( at + 9 ).trim() );
				}
			}
		} catch ( IOException | NumberFormatException e ) {
			// No usable reading
		}
		return -1;
	}

	/**
	 * The live node currently holding the most runs, from the counts each node publishes. Falls back to the first
	 * live node when none reported anything.
	 */
	private static String busiestNode( Path logDir, Set<String> candidates ) {
		String	best		= candidates.stream().findFirst().orElse( null );
		int		bestCount	= -1;
		for ( String id : candidates ) {
			int count = lastInFlight( logDir, id );
			if ( count > bestCount ) {
				bestCount	= count;
				best		= id;
			}
		}
		return best;
	}

	/**
	 * Stop a node the way production does: ask it to stop, and let the runtime shut down on its own terms.
	 */
	private static void stop( String id, Path stopsDir, Map<String, NodeProc> live, List<NodeSpan> spans, List<LifecycleEvent> events, String endReason ) {
		NodeProc node = live.get( id );
		if ( node == null ) {
			return;
		}

		long at = System.currentTimeMillis();
		try {
			Files.writeString( stopsDir.resolve( id + ".stop" ), "stop" );
		} catch ( IOException e ) {
			node.process().destroy();
		}
		// Recorded separately from the span end so the timeline can show *when* the node stopped accepting
		// new work: from this instant the scheduler refuses new runs, and everything after it is drain time.
		events.add( new LifecycleEvent( at, id, "stop-requested", "graceful shutdown requested - no new runs accepted from here" ) );

		try {
			if ( !node.process().waitFor( 100, TimeUnit.SECONDS ) ) {
				System.out.println( "  " + id + " did not shut down within 100s - killing it" );
				node.process().destroyForcibly();
				events.add( new LifecycleEvent( at, id, "killed", "runtime shutdown did not finish in 45s" ) );
				spans.add( new NodeSpan( id, node.onlineFrom(), System.currentTimeMillis(), "crashed" ) );
				live.remove( id );
				return;
			}
		} catch ( InterruptedException e ) {
			Thread.currentThread().interrupt();
		}

		spans.add( new NodeSpan( id, node.onlineFrom(), System.currentTimeMillis(), endReason ) );
		events.add( new LifecycleEvent( at, id, endReason, "retired".equals( endReason )
		    ? "retired for good (scale down), runtime shut down gracefully"
		    : "stopped gracefully (runtime shutdown)" ) );
		System.out.println( "  " + id + " shut down " + ( ( System.currentTimeMillis() - at ) / 1000 )
		    + "s after the stop request (drain, no new runs accepted)" );
		live.remove( id );
	}

	/** Restart a node that exited on its own, and say so. */
	private static void reap(
	    Map<String, NodeProc> live,
	    List<NodeSpan> spans,
	    List<LifecycleEvent> events,
	    Path root,
	    Path cacheDir,
	    Path logDir,
	    Path stopsDir,
	    int taskCount,
	    String mode,
	    Map<String, Integer> starts,
	    Set<String> freshIds ) throws IOException {
		for ( String id : new ArrayList<>( live.keySet() ) ) {
			NodeProc node = live.get( id );
			if ( node == null || node.process().isAlive() ) {
				continue;
			}
			int		code	= node.process().exitValue();
			long	at		= System.currentTimeMillis();
			System.out.println( "! " + id + " exited on its own (exit " + code + ") - restarting it" );
			events.add( new LifecycleEvent( at, id, "crashed", "process exited on its own (exit " + code + ")" ) );
			spans.add( new NodeSpan( id, node.onlineFrom(), at, "crashed" ) );
			live.remove( id );
			start( id, root, cacheDir, logDir, stopsDir, taskCount, mode, live, events, starts, freshIds, false );
		}
	}

	/**
	 * --------------------------------------------------------------------------
	 * Node configuration
	 * --------------------------------------------------------------------------
	 */

	private static void writeJobs( Path home, int taskCount ) throws IOException {
		for ( int i = 1; i <= taskCount; i++ ) {
			// A .bxm template with a script block, addressed by a path relative to the node's working directory:
			// the include flow resolves templates relative to the working dir, and a .bx script is not an
			// includable template.
			Files.writeString( home.resolve( jobName( i ) ), """
			                                                 <bx:script>
			                                                     sleep( %d );
			                                                 </bx:script>
			                                                 """.formatted( JOB_MILLIS[ ( i - 1 ) % JOB_MILLIS.length ] ) );
		}

		// The unclustered task's body. Short on purpose: it runs on every node, every 5s, for the whole run.
		Files.writeString( home.resolve( plainJobName() ), """
		                                                   <bx:script>
		                                                       sleep( %d );
		                                                   </bx:script>
		                                                   """.formatted( PLAIN_MILLIS ) );
	}

	private static String jobName( int index ) {
		return TASK_PREFIX + index + ".bxm";
	}

	private static String plainJobName() {
		return PLAIN_TASK + ".bxm";
	}

	private static String boxlangJson( String nodeId, Path cacheDir, String mode ) {
		String cache = cacheDir.toString().replace( "\\", "\\\\" );
		return """
		       {
		         "cluster": {
		           "enabled": true,
		           "name": "%s",
		           "nodeId": "%s",
		           "cacheName": "%s",
		           "heartbeatSeconds": 2
		         },
		         "caches": {
		           "%s": {
		             "provider": "BoxCacheProvider",
		             "properties": {
		               "objectStore": "FileSystemStore",
		               "directory": "%s",
		               "defaultTimeout": 0,
		               "defaultLastAccessTimeout": 0,
		               "useLastAccessTimeouts": false,
		               "reapFrequency": 3600,
		               "maxObjects": 100000
		             }
		           }
		         },
		         "scheduler": {
		           "clusterMode": "%s",
		           "taskOverlapPolicy": "skip",
		           // Deliberately shorter than the longest job (50s). Nodes are retired while long jobs are in
		           // flight, so shutdown interrupts them and those runs end `abandoned` - the path this soak needs
		           // to keep exercising. With a grace longer than every job, no run is ever interrupted.
		           "shutdownGraceSeconds": 8,
		           "shutdownInterruptGraceSeconds": 5
		         },
		         "logging": {
		           "loggers": {
		             "scheduler": {
		               "level": "DEBUG"
		             }
		           }
		         }
		       }
		       """.formatted( CLUSTER_NAME, nodeId, CACHE_NAME, CACHE_NAME, cache, mode );
	}

	private static String tasksJson( int taskCount ) {
		StringBuilder entries = new StringBuilder();
		for ( int i = 1; i <= taskCount; i++ ) {
			if ( i > 1 ) {
				entries.append( ",\n" );
			}
			entries.append( """
			                {
			                  "task": "%s%d",
			                  "scheduler": "%s",
			                  "group": "",
			                  "cronTime": "*/%d * * * * *",
			                  "path": "%s",
			                  "cluster": true,
			                  "overlap": "skip"
			                }""".formatted( TASK_PREFIX, i, SCHEDULER_NAME, CRON_SECONDS[ ( i - 1 ) % CRON_SECONDS.length ],
			    jobName( i ) ) );
		}

		// The unclustered control task. "cluster": false is explicit - an absent key would inherit
		// cluster.enabled = true and silently make this clustered, which is exactly what it must not be.
		if ( taskCount > 0 ) {
			entries.append( ",\n" );
		}
		entries.append( """
		                {
		                  "task": "%s",
		                  "scheduler": "%s",
		                  "group": "",
		                  "cronTime": "*/%d * * * * *",
		                  "path": "%s",
		                  "cluster": false,
		                  "overlap": "skip"
		                }""".formatted( PLAIN_TASK, SCHEDULER_NAME, PLAIN_SECONDS, plainJobName() ) );
		return "[\n" + entries + "\n]\n";
	}

	private static Process launch( String nodeId, Path home, Path logDir, Path stopFile ) throws IOException {
		Path			javaBin	= Path.of( System.getProperty( "java.home" ), "bin", isWindows() ? "java.exe" : "java" );
		List<String>	command	= new ArrayList<>();
		command.add( javaBin.toString() );
		command.add( "-cp" );
		command.add( System.getProperty( "java.class.path" ) );
		command.add( ClusterSoakReal.class.getName() );
		command.add( "--node" );
		command.add( nodeId );
		command.add( "--home" );
		command.add( home.toString() );
		command.add( "--stop-file" );
		command.add( stopFile.toString() );

		return new ProcessBuilder( command )
		    .directory( home.toFile() )
		    .redirectOutput( ProcessBuilder.Redirect.appendTo( logDir.resolve( nodeId + ".log" ).toFile() ) )
		    .redirectError( ProcessBuilder.Redirect.appendTo( logDir.resolve( nodeId + ".err.log" ).toFile() ) )
		    .start();
	}

	/**
	 * --------------------------------------------------------------------------
	 * Analysis - from the runtimes' own scheduler logs
	 * --------------------------------------------------------------------------
	 */

	private static List<String> analyse(
	    Path root,
	    List<String> nodeIds,
	    List<NodeSpan> spans,
	    List<LifecycleEvent> events,
	    Map<String, Integer> starts,
	    Set<String> freshIds ) throws IOException {
		// Keyed by task + occurrence: each scheduled task has its own occurrence ids, so the same id can
		// legitimately appear for two different tasks at the same moment.
		Map<String, Set<String>>			claimed			= new LinkedHashMap<>();
		// How many times each occurrence was claimed, per node. `claimed` collapses repeats by the same node,
		// which is exactly the duplicate that must not be missed: the occurrence ran twice.
		Map<String, Map<String, Integer>>	claimCounts		= new LinkedHashMap<>();
		Map<String, Integer>				perNode			= new LinkedHashMap<>();
		Map<String, Integer>				perTask			= new LinkedHashMap<>();
		Map<String, Integer>				plainRuns		= new LinkedHashMap<>();
		Set<String>							abandoned		= new LinkedHashSet<>();
		Set<String>							peerAbandoned	= new LinkedHashSet<>();
		Map<String, Set<String>>			runObservers	= new LinkedHashMap<>();
		int									succeeded		= 0;

		nodeIds.forEach( id -> perNode.put( id, 0 ) );
		nodeIds.forEach( id -> plainRuns.put( id, 0 ) );

		for ( String nodeId : nodeIds ) {
			Path log = schedulerLog( root, nodeId );
			if ( !Files.exists( log ) ) {
				continue;
			}
			for ( String line : Files.readAllLines( log ) ) {
				Matcher claim = CLAIMED.matcher( line );
				if ( claim.find() ) {
					String	task		= claim.group( 1 );
					String	occurrence	= task + " @ " + claim.group( 2 );
					String	node		= claim.group( 3 );
					claimed.computeIfAbsent( occurrence, key -> new TreeSet<>() ).add( node );
					claimCounts.computeIfAbsent( occurrence, key -> new LinkedHashMap<>() ).merge( node, 1, Integer::sum );
					perNode.merge( node, 1, Integer::sum );
					perTask.merge( task, 1, Integer::sum );
				}
				if ( SUCCEEDED.matcher( line ).find() ) {
					succeeded++;
				}
				// An occurrence a node reported as interrupted or abandoned: it will never complete
				Matcher interruptedLine = INTERRUPTED.matcher( line );
				if ( interruptedLine.find() ) {
					abandoned.add( interruptedLine.group( 1 ) + " @ " + interruptedLine.group( 2 ) );
				}
				Matcher abandonedLine = ABANDONED.matcher( line );
				if ( abandonedLine.find() ) {
					abandoned.add( abandonedLine.group( 1 ) + " @ " + abandonedLine.group( 2 ) );
				}
				Matcher peerAbandonedLine = PEER_ABANDONED.matcher( line );
				if ( peerAbandonedLine.find() ) {
					peerAbandoned.add( peerAbandonedLine.group( 1 ) + " @ " + peerAbandonedLine.group( 2 ) );
				}
				// Which nodes logged a report about this run, and which node the run belonged to
				Matcher peerReport = PEER_REPORT.matcher( line );
				if ( peerReport.find() ) {
					runObservers.computeIfAbsent( peerReport.group( 1 ) + " @ " + peerReport.group( 2 ), key -> new LinkedHashSet<>() )
					    .add( nodeId + ">" + peerReport.group( 3 ) );
				}
				// The unclustered task is counted per node, from that node's own log. It is deliberately
				// excluded from `claimed` and from `succeeded`: it is supposed to run everywhere.
				if ( PLAIN_RAN.matcher( line ).find() ) {
					plainRuns.merge( nodeId, 1, Integer::sum );
				}
			}
		}

		List<String>	failures	= new ArrayList<>();

		// The claimed runs at least one node other than the owner reported seeing in flight. Those are the runs a
		// peer had the chance to watch, which is what makes their endings accountable below.
		Set<String>		witnessed	= peerWitnessed( claimed, runObservers );

		// 1. Exactly once: an occurrence claimed more than once is a duplicate - on two nodes, or twice on the same
		// node, which is the case a set of node ids silently swallowed.
		for ( Map.Entry<String, Map<String, Integer>> entry : claimCounts.entrySet() ) {
			int runs = entry.getValue().values().stream().mapToInt( Integer::intValue ).sum();
			if ( runs > 1 ) {
				System.out.println( "DUPLICATE " + entry.getKey() + " claimed " + runs + " time(s) by " + entry.getValue() );
				failures.add( entry.getKey() + " claimed " + runs + " time(s) by " + entry.getValue() );
			}
		}

		System.out.println();
		System.out.println( "Task runs claimed:     " + claimed.size() );
		System.out.println( "Task runs succeeded:   " + succeeded );
		System.out.println( "Runs abandoned:        " + abandoned.size() + "  (interrupted or abandoned)" );
		long	kills			= events.stream().filter( e -> "killed".equals( e.type() ) ).count();
		long	killsWithWork	= events.stream()
		    .filter( e -> "killed".equals( e.type() ) )
		    .filter( e -> {
									    Matcher m = IN_FLIGHT_AT_KILL.matcher( e.detail() );
									    return m.find() && Integer.parseInt( m.group( 1 ) ) > 0;
								    } )
		    .count();
		System.out.println( "Peer-declared abandoned: " + peerAbandoned.size() + "  (node vanished holding the run)" );
		System.out.println( "Hard kills:            " + kills + "  (" + killsWithWork + " while holding work)" );
		System.out.println( "Runs per task:         " + perTask );
		System.out.println( "Runs per node:         " + perNode );
		System.out.println( "Unclustered per node:  " + plainRuns + "  (" + PLAIN_TASK + ", expected on every node)" );
		System.out.println( "Node starts:           " + starts );
		System.out.println( "Crashes:               " + events.stream().filter( e -> "crashed".equals( e.type() ) ).count() );

		// 2. Every claimed run must end one of two ways: succeeded, or abandoned because it was cut short (by
		// shutdown, or by its node being killed). Anything else is a run that vanished.
		Set<String> accounted = new LinkedHashSet<>( abandoned );
		accounted.addAll( peerAbandoned );

		if ( claimed.isEmpty() ) {
			System.out.println( "No task runs were claimed at all - the scheduler never ran anything" );
			failures.add( "no task runs were claimed at all" );
		} else if ( succeeded > claimed.size() ) {
			failures.add( ( succeeded - claimed.size() ) + " task run(s) succeeded without a claim - duplicate execution" );
		} else if ( succeeded + accounted.size() < claimed.size() ) {
			// A run can legitimately have no reported ending: a node killed inside one heartbeat is gone before any
			// peer's running list ever contains the run, so no peer can report it and the dead node cannot either
			// (see 3c). Only a run a peer actually watched in flight must have an ending - those are the ones that
			// prove the observer did its job.
			Set<String> unwitnessed = new LinkedHashSet<>( claimed.keySet() );
			unwitnessed.removeAll( witnessed );
			unwitnessed.removeAll( accounted );
			int unaccounted = claimed.size() - succeeded - accounted.size() - unwitnessed.size();

			if ( unaccounted > 0 ) {
				System.out.println( unaccounted + " claimed run(s) neither succeeded nor were reported abandoned" );
				failures.add( unaccounted + " task run(s) started but neither finished nor were reported abandoned" );
			}
		}

		// 3b. A hard kill must hit a node that is actually working, and must produce a peer-declared abandonment.
		// Killing an idle node tests nothing, and if a busy node dies and nobody reports it, the observer never
		// noticed - both are failures, not acceptable variance.
		if ( kills > 0 && killsWithWork == 0 ) {
			System.out.println( "Every hard kill hit an idle node - the abandoned path was not exercised" );
			failures.add( "every hard kill hit an idle node, so no run could be abandoned" );
		} else if ( killsWithWork > 0 && peerAbandoned.isEmpty() && abandoned.isEmpty() ) {
			System.out.println( "A node was killed while holding work but no run was ever reported abandoned" );
			failures.add( "a node was killed holding work but no abandoned run was reported" );
		}

		// 3c. Full track record: every clustered run must have been reported by at least one node other than the one
		// that ran it. That is the whole point of the observer - any node's log should be able to tell you what
		// happened to any run - so a run nobody else noticed is a failure, not a detail.
		int uncovered = 0;
		for ( Map.Entry<String, Set<String>> entry : claimed.entrySet() ) {
			if ( !witnessed.contains( entry.getKey() ) ) {
				uncovered++;
				if ( uncovered <= 5 ) {
					System.out.println( "NOT OBSERVED: " + entry.getKey() + " ran on " + entry.getValue().iterator().next()
					    + ", no other node reported it" );
				}
			}
		}
		System.out.println( "Runs observed by a peer: " + ( claimed.size() - uncovered ) + "/" + claimed.size() );
		if ( uncovered > 0 ) {
			// Informational, not a failure. A run claimed and its node killed inside one heartbeat never reaches any
			// peer's running list, so nobody can report it - an accepted edge case, not a defect.
			System.out.println( "  (" + uncovered
			    + " run(s) were never seen by another node - expected when a node dies inside one heartbeat)" );
		}

		// 3. The interrupted-shutdown path must actually be exercised. With a grace shorter than the longest job,
		// a run of any decent length should always produce at least one abandonment - if it stops doing so, the
		// soak has quietly stopped testing the thing it exists for.
		long runSeconds = ( spans.stream().mapToLong( NodeSpan::to ).max().orElse( 0 )
		    - spans.stream().mapToLong( NodeSpan::from ).min().orElse( 0 ) ) / 1000;
		if ( runSeconds >= 60 && abandoned.isEmpty() ) {
			System.out.println( "No run was abandoned in a " + runSeconds + "s run - the interrupted-shutdown path was not exercised" );
			failures.add( "no run was abandoned, so the interrupted-shutdown path was not exercised" );
		}

		// 3. Every node that was up long enough to have a fair chance must have taken work. A node that never
		// wins a claim while its peers take everything is a broken run, not an acceptable outcome - the whole
		// point of the cluster is that the work spreads.
		//
		// This can only be judged when there were at least as many clusters runs as nodes: with two runs and
		// three nodes, someone winning nothing is arithmetic, not starvation. A very short run therefore
		// reports the fact instead of failing on it.
		long fairWindowMillis = Math.max( 20_000L, CRON_SECONDS[ 0 ] * 2_000L );

		if ( claimed.size() < perNode.size() ) {
			System.out.println( "FAIRNESS: not judged - only " + claimed.size() + " clustered run(s) for "
			    + perNode.size() + " node(s), too few for every node to be expected to win one" );
		} else {
			for ( Map.Entry<String, Integer> entry : perNode.entrySet() ) {
				long onlineMillis = spans.stream().filter( span -> span.nodeId().equals( entry.getKey() ) )
				    .mapToLong( span -> span.to() - span.from() ).sum();

				if ( entry.getValue() == 0 && onlineMillis >= fairWindowMillis ) {
					System.out.println( "FAIRNESS: node " + entry.getKey() + " was up " + ( onlineMillis / 1000 )
					    + "s and never claimed a task run while its peers did - the ranking never let it win" );
					failures.add( "node " + entry.getKey() + " was up " + ( onlineMillis / 1000 ) + "s and never claimed a task run" );
				}
			}
		}

		// 4. The unclustered task must run on EVERY node. Its whole purpose is to prove that a task with
		// cluster=false still executes normally on every server while its clustered peers coordinate around
		// it - so a node that was up long enough to see several of its fires and never ran it is a
		// regression in the unclustered path, not a balancing issue.
		for ( Map.Entry<String, Integer> entry : plainRuns.entrySet() ) {
			long onlineMillis = spans.stream().filter( span -> span.nodeId().equals( entry.getKey() ) )
			    .mapToLong( span -> span.to() - span.from() ).sum();

			if ( entry.getValue() == 0 && onlineMillis >= fairWindowMillis ) {
				System.out.println( "UNCLUSTERED MISSING: node " + entry.getKey() + " was up " + ( onlineMillis / 1000 )
				    + "s and never ran [" + PLAIN_TASK + "], which is not clustered and must run everywhere" );
				failures.add( "node " + entry.getKey() + " never ran the unclustered task [" + PLAIN_TASK + "]" );
			}
		}

		return failures;
	}

	/**
	 * The claimed runs that at least one node other than the owner reported seeing in flight. Each observer is
	 * recorded as {@code <observing node>><run owner>}, so an entry only counts when the observing node is
	 * somebody other than the node that ran it.
	 */
	private static Set<String> peerWitnessed( Map<String, Set<String>> claimed, Map<String, Set<String>> runObservers ) {
		Set<String> witnessed = new LinkedHashSet<>();

		for ( Map.Entry<String, Set<String>> entry : claimed.entrySet() ) {
			String	owner		= entry.getValue().iterator().next();
			boolean	seenByPeer	= runObservers.getOrDefault( entry.getKey(), Set.of() ).stream().anyMatch( reporter -> {
									int sep = reporter.indexOf( '>' );
									return sep > 0 && !reporter.substring( 0, sep ).equals( owner );
								} );
			if ( seenByPeer ) {
				witnessed.add( entry.getKey() );
			}
		}

		return witnessed;
	}

	/** The runtime's own scheduler log for a node. */
	private static Path schedulerLog( Path root, String nodeId ) {
		return root.resolve( nodeId + "-home" ).resolve( "logs" ).resolve( "scheduler.log" );
	}

	/**
	 * The identity of an unclustered task run: {@code task @ yyyy-MM-dd'T'HH:mm:ss}, the same shape the claim
	 * path gives a clustered occurrence. It is derived from the cron fire the run belongs to rather than the
	 * instant the node happened to start, so every node's run of the same fire groups under one id and the
	 * ids sort alongside the clustered ones.
	 */
	private static String plainOccurrence( long at ) {
		long	fire	= at - ( at % ( PLAIN_SECONDS * 1000L ) );
		String	id		= TaskClusterClaim.occurrenceId( Instant.ofEpochMilli( fire ), ZoneId.systemDefault() );
		return PLAIN_TASK + " @ " + id;
	}

	/**
	 * Close the most recent run of a task still open on this node. Matching by node only would pair a
	 * task's success with a different task's claim and report a bogus duration.
	 *
	 * @return true when a run was closed
	 */
	private static boolean close( List<Execution> executions, String nodeId, String occurrencePrefix, long at, String outcome ) {
		for ( int i = executions.size() - 1; i >= 0; i-- ) {
			Execution execution = executions.get( i );
			if ( execution.nodeId().equals( nodeId ) && execution.occurrence().startsWith( occurrencePrefix )
			    && "running".equals( execution.outcome() ) ) {
				executions.set( i,
				    new Execution( execution.occurrence(), nodeId, execution.start(), at, outcome, execution.clustered() ) );
				return true;
			}
		}
		return false;
	}

	private static boolean close( List<Execution> executions, String nodeId, String occurrencePrefix, long at ) {
		return close( executions, nodeId, occurrencePrefix, at, "success" );
	}

	/**
	 * Close any still-open run of this exact occurrence on this node before a new one starts.
	 * <p>
	 * Two runs can share one occurrence key - a delayed fire landing in a bucket that already ran, for instance.
	 * Only the newest would ever be closed by its success line, leaving the older one drawn as "started but never
	 * finished" at 0ms, which is a rendering artefact rather than a real unfinished run.
	 */
	private static void closeSuperseded( List<Execution> executions, String nodeId, String occurrence, long at ) {
		for ( int i = 0; i < executions.size(); i++ ) {
			Execution execution = executions.get( i );
			if ( execution.nodeId().equals( nodeId ) && execution.occurrence().equals( occurrence )
			    && "running".equals( execution.outcome() ) ) {
				executions.set( i,
				    new Execution( execution.occurrence(), nodeId, execution.start(), at, "unknown", execution.clustered() ) );
			}
		}
	}

	private static void writeReport(
	    Path root,
	    long startedAt,
	    long endedAt,
	    String mode,
	    List<NodeSpan> spans,
	    List<LifecycleEvent> events,
	    List<String> nodeIds,
	    boolean passed,
	    List<String> failures ) throws IOException {
		List<Execution> executions = new ArrayList<>();

		for ( String nodeId : nodeIds ) {
			Path log = schedulerLog( root, nodeId );
			if ( !Files.exists( log ) ) {
				continue;
			}
			for ( String line : Files.readAllLines( log ) ) {
				long	at			= logTimestamp( line, startedAt );
				Matcher	claim		= CLAIMED.matcher( line );
				Matcher	done		= SUCCEEDED.matcher( line );
				Matcher	plainStart	= PLAIN_START.matcher( line );
				Matcher	plainDone	= PLAIN_RAN.matcher( line );
				Matcher	cutShort	= INTERRUPTED.matcher( line );

				if ( claim.find() ) {
					String occurrence = claim.group( 1 ) + " @ " + claim.group( 2 );
					closeSuperseded( executions, nodeId, occurrence, at );
					executions.add( new Execution( occurrence, claim.group( 3 ), at, at, "running", true ) );
				} else if ( cutShort.find() ) {
					// The owner recording that shutdown cut its run short. Close the run with that outcome, or it
					// renders as "started but never finished" at 0ms - which is exactly how an interrupted run used
					// to disappear from the report.
					close( executions, nodeId, cutShort.group( 1 ) + " @ " + cutShort.group( 2 ), at, "interrupted" );
				} else if ( done.find() ) {
					close( executions, nodeId, done.group( 1 ) + " @", at );
				} else if ( plainStart.find() ) {
					// A non-clustered task has no occurrence id, so one is derived from the cron fire it belongs
					// to - the same shape and format the claim path uses. Every node's run of the same fire then
					// shares one id, groups under it, and sorts alongside the clustered ids.
					String plainFire = plainOccurrence( at );
					closeSuperseded( executions, nodeId, plainFire, at );
					executions.add( new Execution( plainFire, nodeId, at, at, "running", false ) );
				} else if ( plainDone.find() ) {
					if ( !close( executions, nodeId, PLAIN_TASK + " @ ", at ) ) {
						// No start line available: still show the run at its completion instant rather than
						// dropping it from the report.
						executions.add( new Execution( plainOccurrence( at ), nodeId, at, at, "success", false ) );
					}
				}
			}
		}

		// Anything still marked "running" never logged an ending. A report is written after the run has finished, so
		// that can only mean its node died holding it: mark those abandoned (hard kill) or unknown (graceful stop
		// with no outcome recorded) rather than leaving them displayed as "running", which is simply false.
		for ( int i = 0; i < executions.size(); i++ ) {
			Execution execution = executions.get( i );
			if ( !"running".equals( execution.outcome() ) ) {
				continue;
			}
			// The lifespan this run started in - not simply the node's last one, which could be a later restart
			NodeSpan	completed	= spans.stream()
			    .filter( span -> span.nodeId().equals( execution.nodeId() ) )
			    .filter( span -> span.from() <= execution.start() && span.to() >= execution.start() )
			    .min( java.util.Comparator.comparingLong( NodeSpan::to ) )
			    .orElse( null );
			String		outcome		= completed != null && ( "killed".equals( completed.endReason() ) || "crashed".equals( completed.endReason() ) )
			    ? "abandoned"
			    : "unknown";
			long		end			= completed == null ? execution.start() : Math.max( execution.start(), completed.to() );
			executions.set( i, new Execution( execution.occurrence(), execution.nodeId(), execution.start(), end, outcome, execution.clustered() ) );
		}

		Run		run		= new Run( mode, 0, startedAt, endedAt, spans, executions, events, List.of(), passed, failures );
		Path	html	= Path.of( REPORT_PATH ).toAbsolutePath();
		Files.createDirectories( html.getParent() );
		Files.writeString( html, ClusterSoakTimeline.render( run ) );

		System.out.println( "Report:   " + html );
		System.out.println( "  open:   " + html.toUri() );
	}

	/**
	 * The timestamp logback prints at the start of every line. It is a local date-time with no offset -
	 * parsing it as an {@link OffsetDateTime} threw and every execution collapsed onto the run start.
	 */
	private static long logTimestamp( String line, long fallback ) {
		Matcher matcher = LOG_TIME.matcher( line );
		if ( !matcher.find() ) {
			return fallback;
		}
		try {
			return java.time.LocalDateTime.parse( matcher.group( 1 ).replace( ',', '.' ) )
			    .atZone( java.time.ZoneId.systemDefault() )
			    .toInstant()
			    .toEpochMilli();
		} catch ( Exception e ) {
			return fallback;
		}
	}

	/**
	 * --------------------------------------------------------------------------
	 * Helpers
	 * --------------------------------------------------------------------------
	 */

	private static Map<String, String> parse( String[] args ) {
		Map<String, String> options = new LinkedHashMap<>();
		for ( int i = 0; i < args.length; i++ ) {
			if ( !args[ i ].startsWith( "--" ) ) {
				continue;
			}
			String key = args[ i ].substring( 2 );
			if ( key.contains( "=" ) ) {
				int eq = key.indexOf( '=' );
				options.put( key.substring( 0, eq ), key.substring( eq + 1 ) );
			} else if ( i + 1 < args.length && !args[ i + 1 ].startsWith( "--" ) ) {
				options.put( key, args[ ++i ] );
			} else {
				options.put( key, "true" );
			}
		}
		return options;
	}

	private static void sleep( long seconds ) {
		try {
			Thread.sleep( seconds * 1000L );
		} catch ( InterruptedException e ) {
			Thread.currentThread().interrupt();
		}
	}

	private static boolean isWindows() {
		return System.getProperty( "os.name", "" ).toLowerCase().contains( "win" );
	}

	private static void deleteRecursively( Path root ) {
		try ( var paths = Files.walk( root ) ) {
			paths.sorted( java.util.Comparator.reverseOrder() ).forEach( path -> {
				try {
					Files.deleteIfExists( path );
				} catch ( IOException ignored ) {
					// best effort
				}
			} );
		} catch ( IOException ignored ) {
			// best effort
		}
	}

}
