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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.async.tasks.ScheduledTask;
import ortus.boxlang.runtime.async.tasks.TaskClusterClaim;
import ortus.boxlang.runtime.cache.providers.ICacheProvider;
import ortus.boxlang.runtime.config.segments.CacheConfig;
import ortus.boxlang.runtime.config.segments.ClusterConfig;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.services.ClusterService;
import ortus.boxlang.runtime.types.IStruct;

/**
 * Multi-process cluster harness: its own entry point, deliberately outside the unit test run.
 * <p>
 * It launches several real JVM processes, each running a {@link ClusterService} against one shared
 * on-disk cache, lets them discover and heartbeat each other, kills one outright, and then checks
 * that the survivors converge on the new membership. That is the tier a single JVM cannot prove.
 * <p>
 * Everything is plain CLI processes - no web context, no servlet container - so the whole scenario is
 * files and processes.
 * <p>
 * Run the orchestrator:
 *
 * <pre>
 * java -cp ... ortus.boxlang.cluster.ClusterHarness --nodes 3 --run-seconds 90
 * </pre>
 *
 * A child process is launched with {@code --node <id>} and handled by the same main.
 */
public class ClusterHarness {

	private static final String	CACHE_NAME			= "harnessCache";
	private static final String	DEFAULT_CLUSTER		= "harness";
	private static final String	PEERS_PREFIX		= "PEERS ";

	private static final long	HEARTBEAT_SECONDS	= 2;
	private static final long	ATTRIBUTES_SECONDS	= 4;

	/**
	 * Entry point. Runs a child node when {@code --node} is present, otherwise orchestrates a run.
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

		System.exit( orchestrate( options ) );
	}

	/**
	 * --------------------------------------------------------------------------
	 * Child node
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Run one cluster node inside this process until told to stop. Invoked as a child process.
	 *
	 * @param options The parsed options: node, dir, cluster, seconds
	 *
	 * @throws Exception When the node cannot start
	 */
	private static void runNode( Map<String, String> options ) throws Exception {
		String	nodeId		= options.get( "node" );
		String	cluster		= options.getOrDefault( "cluster", DEFAULT_CLUSTER );
		Path	cacheDir	= Path.of( options.get( "dir" ) );
		long	seconds		= Long.parseLong( options.getOrDefault( "seconds", "300" ) );

		// Prove whether the JVM exits on its own or is killed from outside
		Runtime.getRuntime().addShutdownHook( new Thread( () -> System.out.println( "[" + nodeId + "] JVM SHUTDOWN" ), "harness-jvm-watch" ) );

		try {
			runNodeBody( nodeId, cluster, cacheDir, seconds );
		} catch ( Throwable t ) {
			System.out.println( "[" + nodeId + "] FATAL " + t.getClass().getName() + ": " + t.getMessage() );
			t.printStackTrace( System.out );
			System.out.flush();
			System.exit( 1 );
		}

		// The runtime keeps non-daemon threads of its own, so a CLI child must exit explicitly or the
		// process lingers forever. Our own cluster threads are stopped first, above.
		System.out.flush();
		System.exit( 0 );
	}

	private static void runNodeBody( String nodeId, String cluster, Path cacheDir, long seconds ) throws Exception {
		// Each node gets its own runtime home, like a separate installation would
		Path nodeHome = cacheDir.getParent().resolve( nodeId + "-home" );
		Files.createDirectories( nodeHome );

		BoxRuntime		runtime	= BoxRuntime.getInstance( true, null, nodeHome.toString() );
		ICacheProvider	cache	= createFileSystemCache( runtime, cacheDir );

		ClusterConfig	config	= new ClusterConfig();
		config.enabled			= true;
		config.name				= cluster;
		config.nodeId			= nodeId;
		config.cacheName		= CACHE_NAME;
		config.heartbeatSeconds	= HEARTBEAT_SECONDS;

		ClusterService service = new ClusterService( runtime, config, cache );
		service.setAttribute( "harness.node", nodeId );
		service.setAttribute( "harness.startedAt", Instant.now().toString() );

		// A clean stop on SIGTERM proves shutdown releases the threads
		Runtime.getRuntime().addShutdownHook( new Thread( service::stopLoops, "harness-shutdown" ) );

		service.startLoops();
		log( nodeId, "STARTED cluster=" + cluster + " cacheDir=" + cacheDir );

		// A synthetic clustered task, so the run exercises claim arbitration end to end. Each tick
		// attempts the claim for a deterministic 3-second occurrence bucket; exactly one node in the
		// cluster may win any given bucket, which the orchestrator verifies from the logs.
		ScheduledTask	task		= new ScheduledTask( "harness-task" ).every( 3, TimeUnit.SECONDS ).setCluster( true );
		long			periodMs	= 3_000;

		long			deadline	= System.currentTimeMillis() + ( seconds * 1000 );
		while ( System.currentTimeMillis() < deadline ) {
			log( nodeId, "TICK" );
			try {
				reportPeers( service, nodeId );
				runClaimTick( service, task, nodeId, periodMs );
			} catch ( Throwable t ) {
				log( nodeId, "LOOP-ERROR " + t.getClass().getSimpleName() + ": " + t.getMessage() );
			}
			// Two seconds between reports: a shared filesystem store makes every read and write
			// expensive, and this cadence is still far faster than any staleness window
			Thread.sleep( 2_000 );
		}

		service.stopLoops();
		log( nodeId, "STOPPED" );
	}

	/**
	 * Attempt to claim the current occurrence bucket, and record the run if this node won it.
	 * <p>
	 * The due instant is aligned to the bucket so every node computes the same occurrence id - that
	 * alignment is what makes the claim comparable across processes.
	 */
	private static void runClaimTick( ClusterService service, ScheduledTask task, String nodeId, long periodMs ) {
		long					bucket		= System.currentTimeMillis() / periodMs;
		Instant					due			= Instant.ofEpochMilli( bucket * periodMs );
		String					occurrence	= TaskClusterClaim.occurrenceId( task, due );

		TaskClusterClaim.Claim	claim		= TaskClusterClaim.attempt( task, due, service );
		if ( claim == null ) {
			return;
		}

		try {
			log( nodeId, "RUN " + occurrence );
			// Pretend to do a little work
			Thread.sleep( 250 );
			claim.finish( "success", null );
		} catch ( InterruptedException e ) {
			Thread.currentThread().interrupt();
		}
	}

	/**
	 * Print this node's current view of its peers. The orchestrator joins on these lines.
	 */
	private static void reportPeers( ClusterService service, String nodeId ) {
		try {
			IStruct		peers	= service.getPeers();
			Set<String>	names	= new TreeSet<>();
			peers.keySet().forEach( key -> names.add( key.getName() ) );

			StringBuilder line = new StringBuilder( PEERS_PREFIX );
			line.append( String.join( ",", names ) );
			line.append( " attrs=" ).append( ClusterService.attributesOf( service.getNode( nodeId ) ).size() );
			log( nodeId, line.toString() );
		} catch ( Throwable t ) {
			log( nodeId, "ERROR " + t.getMessage() );
		}
	}

	private static void log( String nodeId, String message ) {
		System.out.println( "[" + nodeId + "] " + message );
		// Flush every line: the orchestrator reads these files live, and a crashed (killed) node never
		// gets to flush anything, so an unflushed log hides exactly the evidence we need
		System.out.flush();
	}

	/**
	 * --------------------------------------------------------------------------
	 * Orchestrator
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Launch the nodes, wait for convergence, kill one, and verify the survivors converge again.
	 *
	 * @param options The parsed options
	 *
	 * @return 0 on success, 1 on failure
	 *
	 * @throws Exception When the harness itself fails
	 */
	private static int orchestrate( Map<String, String> options ) throws Exception {
		int		nodeCount	= Integer.parseInt( options.getOrDefault( "nodes", "3" ) );
		long	runSeconds	= Long.parseLong( options.getOrDefault( "run-seconds", "90" ) );
		boolean	keep		= Boolean.parseBoolean( options.getOrDefault( "keep", "false" ) );

		Path	root		= Files.createTempDirectory( "cluster-harness-" );
		Path	cacheDir	= Files.createDirectories( root.resolve( "cache" ) );
		Path	logDir		= Files.createDirectories( root.resolve( "logs" ) );

		System.out.println( "Cluster harness starting" );
		System.out.println( "  root:  " + root );
		System.out.println( "  nodes: " + nodeCount );
		System.out.println( "  cache: FileSystemStore at " + cacheDir );

		List<String>			nodeIds		= new ArrayList<>();
		Map<String, Process>	processes	= new LinkedHashMap<>();

		for ( int i = 1; i <= nodeCount; i++ ) {
			String nodeId = "node" + i;
			nodeIds.add( nodeId );
			processes.put( nodeId, launchChild( nodeId, cacheDir, logDir, runSeconds + 60 ) );
			// Start nodes a beat apart, like a rolling deployment rather than a synchronized thundering herd
			Thread.sleep( 1_500 );
		}

		boolean passed = true;

		try {
			// 1. Everyone should see everyone
			passed &= awaitPeers( logDir, nodeIds, Set.copyOf( nodeIds ), 180_000 );

			// 2. Kill one node outright, like a process crash
			String victim = nodeIds.get( nodeIds.size() - 1 );
			System.out.println( "Killing " + victim + " (simulated crash)" );

			// Remember how much each survivor had written, so only evidence produced *after* the kill
			// counts when we check the shrunken membership
			Map<String, Integer> lineMarks = new LinkedHashMap<>();
			for ( String survivor : nodeIds ) {
				lineMarks.put( survivor, ( int ) lineCount( logDir.resolve( survivor + ".log" ) ) );
			}

			processes.get( victim ).destroyForcibly();
			processes.get( victim ).waitFor( 10, TimeUnit.SECONDS );

			// 3. The survivors should converge on the smaller membership, after the staleness window
			Set<String>	survivors		= nodeIds.stream().filter( id -> !id.equals( victim ) ).collect( Collectors.toCollection( TreeSet::new ) );
			long		stalenessWindow	= HEARTBEAT_SECONDS * 3 * 1000;
			passed	&= awaitVictimGone( logDir, List.copyOf( survivors ), victim, lineMarks, stalenessWindow + 120_000 );

			// 4. Every surviving node must still be producing heartbeats (no wedged threads)
			passed	&= allStillReporting( logDir, survivors );

			// 5. Exactly once per occurrence: no bucket may appear as RUN in two nodes' logs
			passed	&= assertExactlyOnce( logDir, nodeIds );
		} finally {
			System.out.println( "Stopping remaining nodes" );
			for ( Process process : processes.values() ) {
				if ( process.isAlive() ) {
					process.destroy();
				}
			}
			for ( Map.Entry<String, Process> entry : processes.entrySet() ) {
				Process process = entry.getValue();
				if ( !process.waitFor( 15, TimeUnit.SECONDS ) ) {
					System.out.println( "  " + entry.getKey() + " did not stop, killing" );
					process.destroyForcibly();
				} else {
					System.out.println( "  " + entry.getKey() + " exited with " + process.exitValue() );
				}
			}

			summary( logDir, nodeIds );

			if ( !keep ) {
				deleteRecursively( root );
			} else {
				System.out.println( "Artifacts kept at " + root );
			}
		}

		System.out.println( passed ? "CLUSTER HARNESS: PASS" : "CLUSTER HARNESS: FAIL" );

		if ( !passed ) {
			System.out.println();
			System.out.println( "Diagnostics:" );
			System.out.println( "  - If nodes logged only a handful of reports, the shared store is too slow for" );
			System.out.println( "    cross-process coordination. FileSystemStore on Windows is known to block under" );
			System.out.println( "    concurrent readers/writers; use a POSIX-safe shared volume or a database store." );
			System.out.println( "  - If no node records exist at all, the cache directory is not actually shared." );
		}

		return passed ? 0 : 1;
	}

	/**
	 * Verify the core guarantee from the logs: each occurrence id was run by exactly one node.
	 * <p>
	 * Occurrence ids are the join key, so this needs no shared writer - every node owns its own log
	 * and the harness aggregates them. Any occurrence that appears as RUN twice fails the run, and the
	 * distribution across nodes is reported.
	 *
	 * @return true when every occurrence was executed by exactly one node
	 */
	private static boolean assertExactlyOnce( Path logDir, List<String> nodeIds ) {
		Map<String, Set<String>> runners = new java.util.TreeMap<>();

		for ( String nodeId : nodeIds ) {
			for ( String line : readLines( logDir.resolve( nodeId + ".log" ) ) ) {
				int at = line.indexOf( "RUN " );
				if ( at < 0 ) {
					continue;
				}
				String occurrence = line.substring( at + 4 ).trim();
				runners.computeIfAbsent( occurrence, key -> new TreeSet<>() ).add( nodeId );
			}
		}

		boolean					ok		= true;
		Map<String, Integer>	perNode	= new LinkedHashMap<>();
		nodeIds.forEach( id -> perNode.put( id, 0 ) );

		for ( Map.Entry<String, Set<String>> entry : runners.entrySet() ) {
			if ( entry.getValue().size() > 1 ) {
				System.out.println( "DUPLICATE occurrence " + entry.getKey() + " run by " + entry.getValue() );
				ok = false;
			} else {
				perNode.merge( entry.getValue().iterator().next(), 1, Integer::sum );
			}
		}

		System.out.println( "Occurrences executed: " + runners.size() + " across " + perNode );
		if ( runners.isEmpty() ) {
			System.out.println( "No occurrences were executed at all - the claim path did not run" );
			ok = false;
		}

		return ok;
	}

	private static List<String> readLines( Path file ) {
		if ( !Files.exists( file ) ) {
			return List.of();
		}
		try {
			return Files.readAllLines( file );
		} catch ( IOException e ) {
			return List.of();
		}
	}

	/**
	 * Launch one child node process, redirecting its output to its own log file.
	 */
	private static Process launchChild( String nodeId, Path cacheDir, Path logDir, long seconds ) throws IOException {
		Path			javaBin	= Path.of( System.getProperty( "java.home" ), "bin", isWindows() ? "java.exe" : "java" );

		List<String>	command	= new ArrayList<>();
		command.add( javaBin.toString() );
		command.add( "-cp" );
		command.add( System.getProperty( "java.class.path" ) );
		command.add( ClusterHarness.class.getName() );
		command.add( "--node" );
		command.add( nodeId );
		command.add( "--dir" );
		command.add( cacheDir.toString() );
		command.add( "--cluster" );
		command.add( DEFAULT_CLUSTER );
		command.add( "--seconds" );
		command.add( String.valueOf( seconds ) );

		return new ProcessBuilder( command )
		    .redirectOutput( logDir.resolve( nodeId + ".log" ).toFile() )
		    .redirectError( logDir.resolve( nodeId + ".err.log" ).toFile() )
		    .start();
	}

	/**
	 * Wait until every watched node has <b>ever</b> reported containing all of its expected peers.
	 * <p>
	 * Membership containment rather than set equality on purpose: a node's view can legitimately
	 * include a peer that has just been removed from the member list, or be read mid-report. What is
	 * being proved is that this node discovered its peers, which one containing report demonstrates.
	 */
	private static boolean awaitPeers( Path logDir, List<String> watchers, Set<String> expected, long timeoutMillis )
	    throws InterruptedException {
		long		deadline	= System.currentTimeMillis() + timeoutMillis;
		Set<String>	remaining	= new TreeSet<>( watchers );

		while ( System.currentTimeMillis() < deadline && !remaining.isEmpty() ) {
			remaining.removeIf( nodeId -> sawAll( logDir, nodeId, peersOf( expected, nodeId ), 0 ) );

			if ( !remaining.isEmpty() ) {
				Thread.sleep( 500 );
			}
		}

		for ( String nodeId : watchers ) {
			System.out.println(
			    "  " + nodeId
			        + " expectedPeers=" + peersOf( expected, nodeId )
			        + " last=" + lastPeers( logDir, nodeId )
			        + " sawAll=" + sawAll( logDir, nodeId, peersOf( expected, nodeId ), 0 )
			);
		}

		if ( !remaining.isEmpty() ) {
			System.out.println( "TIMEOUT waiting for " + remaining );
			return false;
		}
		return true;
	}

	/**
	 * Same as {@link #awaitPeers}, but only evidence written after the recorded line marks counts, and
	 * the assertion is that the victim is <b>gone</b> from the node's view. That is what makes the
	 * post-kill check meaningful: a node having seen a smaller set earlier (before its peers even
	 * joined) must not satisfy it.
	 */
	private static boolean awaitVictimGone( Path logDir, List<String> watchers, String victim,
	    Map<String, Integer> lineMarks, long timeoutMillis ) throws InterruptedException {
		long		deadline	= System.currentTimeMillis() + timeoutMillis;
		Set<String>	remaining	= new TreeSet<>( watchers );

		while ( System.currentTimeMillis() < deadline && !remaining.isEmpty() ) {
			remaining.removeIf( nodeId -> reportedVictimGone( logDir, nodeId, victim, lineMarks.getOrDefault( nodeId, 0 ) ) );

			if ( !remaining.isEmpty() ) {
				Thread.sleep( 500 );
			}
		}

		for ( String nodeId : watchers ) {
			System.out.println(
			    "  " + nodeId
			        + " last=" + lastPeers( logDir, nodeId )
			        + " victimGoneAfterKill=" + reportedVictimGone( logDir, nodeId, victim, lineMarks.getOrDefault( nodeId, 0 ) )
			);
		}

		if ( !remaining.isEmpty() ) {
			System.out.println( "TIMEOUT waiting for " + remaining + " to stop seeing " + victim );
			return false;
		}
		return true;
	}

	/**
	 * Has this node produced at least one report after the mark, and does its <b>latest</b> post-mark
	 * report no longer contain the victim?
	 * <p>
	 * Both halves matter. Requiring a post-mark report stops "no evidence yet" from counting as
	 * success - which is what made an earlier version of this check pass instantly and vacuously.
	 * Requiring the latest one (rather than any) means the node really has converged, not just
	 * glimpsed a moment where the victim was missing.
	 */
	private static boolean reportedVictimGone( Path logDir, String nodeId, String victim, int afterLine ) {
		List<Set<String>> reports = peerReports( logDir, nodeId, afterLine );
		if ( reports.isEmpty() ) {
			return false;
		}
		return !reports.get( reports.size() - 1 ).contains( victim );
	}

	/**
	 * Has this node ever reported containing every one of these peers, in a line at or after
	 * {@code afterLine}?
	 */
	private static boolean sawAll( Path logDir, String nodeId, Set<String> expectedPeers, int afterLine ) {
		if ( expectedPeers.isEmpty() ) {
			return true;
		}
		for ( Set<String> reported : peerReports( logDir, nodeId, afterLine ) ) {
			if ( reported.containsAll( expectedPeers ) ) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Has this node reported containing this peer, in a line at or after {@code afterLine}?
	 */
	private static boolean sawPeer( Path logDir, String nodeId, String peer, int afterLine ) {
		for ( Set<String> reported : peerReports( logDir, nodeId, afterLine ) ) {
			if ( reported.contains( peer ) ) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Every peer report this node has written, optionally only those after a line index.
	 */
	private static List<Set<String>> peerReports( Path logDir, String nodeId, int afterLine ) {
		Path				logFile	= logDir.resolve( nodeId + ".log" );
		List<Set<String>>	reports	= new ArrayList<>();
		if ( !Files.exists( logFile ) ) {
			return reports;
		}
		try {
			List<String> lines = Files.readAllLines( logFile );
			for ( int i = Math.max( 0, afterLine ); i < lines.size(); i++ ) {
				Set<String> reported = parsePeers( lines.get( i ) );
				if ( reported != null ) {
					reports.add( reported );
				}
			}
		} catch ( IOException ignored ) {
			// a half-written file simply yields no new evidence this round
		}
		return reports;
	}

	/**
	 * Parse a peer report out of one log line, or null when the line is not a report.
	 */
	private static Set<String> parsePeers( String line ) {
		int at = line.indexOf( PEERS_PREFIX );
		if ( at < 0 ) {
			return null;
		}
		String		rest	= line.substring( at + PEERS_PREFIX.length() );
		int			attrsAt	= rest.indexOf( " attrs=" );
		String		list	= ( attrsAt >= 0 ? rest.substring( 0, attrsAt ) : rest ).trim();

		Set<String>	peers	= new TreeSet<>();
		if ( !list.isEmpty() ) {
			Arrays.stream( list.split( "," ) ).map( String::trim ).filter( s -> !s.isEmpty() ).forEach( peers::add );
		}
		return peers;
	}

	/**
	 * The peer set a node should see: everyone in the cluster except itself, since {@code getPeers()}
	 * excludes the local node by design.
	 */
	private static Set<String> peersOf( Set<String> clusterNodes, String forNodeId ) {
		return clusterNodes.stream()
		    .filter( id -> !id.equals( forNodeId ) )
		    .collect( Collectors.toCollection( TreeSet::new ) );
	}

	/**
	 * A node is still reporting if its log was written recently.
	 */
	private static boolean allStillReporting( Path logDir, Set<String> nodeIds ) throws IOException {
		long	now		= System.currentTimeMillis();
		boolean	allGood	= true;

		for ( String nodeId : nodeIds ) {
			long	logAge	= now - Files.getLastModifiedTime( logDir.resolve( nodeId + ".log" ) ).toMillis();
			boolean	fresh	= logAge < 15_000;
			System.out.println( "  " + nodeId + " last log write " + logAge + "ms ago" + ( fresh ? "" : " (STALLED)" ) );
			allGood &= fresh;
		}

		return allGood;
	}

	/**
	 * Print the last peer report from every node's log.
	 */
	private static void summary( Path logDir, List<String> nodeIds ) {
		System.out.println( "Per-node final view:" );
		for ( String nodeId : nodeIds ) {
			Set<String>	peers	= lastPeers( logDir, nodeId );
			String		state	= peers == null ? "<no report>" : peers.toString();
			System.out.println( "  " + nodeId + " -> " + state + " (" + lineCount( logDir.resolve( nodeId + ".log" ) ) + " log lines)" );

			Path err = logDir.resolve( nodeId + ".err.log" );
			if ( Files.exists( err ) && lineCount( err ) > 0 ) {
				System.out.println( "     stderr:" );
				try {
					Files.readAllLines( err ).stream().limit( 25 ).forEach( line -> System.out.println( "       " + line ) );
				} catch ( IOException e ) {
					System.out.println( "       <unreadable: " + e.getMessage() + ">" );
				}
			}
		}
	}

	private static long lineCount( Path file ) {
		try {
			return Files.exists( file ) ? Files.lines( file ).count() : 0;
		} catch ( IOException e ) {
			return -1;
		}
	}

	/**
	 * Read the last peer report line from a node's log.
	 *
	 * @return The peer set, or null when the node never reported
	 */
	private static Set<String> lastPeers( Path logDir, String nodeId ) {
		Path logFile = logDir.resolve( nodeId + ".log" );
		if ( !Files.exists( logFile ) ) {
			return null;
		}
		try {
			List<String> lines = Files.readAllLines( logFile );
			for ( int i = lines.size() - 1; i >= 0; i-- ) {
				Set<String> reported = parsePeers( lines.get( i ) );
				if ( reported != null ) {
					return reported;
				}
			}
		} catch ( IOException e ) {
			return null;
		}
		return null;
	}

	/**
	 * Build a cache provider over the shared filesystem directory.
	 */
	private static ICacheProvider createFileSystemCache( BoxRuntime runtime, Path cacheDir ) {
		CacheConfig cacheConfig = new CacheConfig( CACHE_NAME );
		cacheConfig.properties.put( Key.objectStore, "FileSystemStore" );
		cacheConfig.properties.put( Key.directory, cacheDir.toString() );
		cacheConfig.properties.put( Key.defaultTimeout, 0 );
		cacheConfig.properties.put( Key.defaultLastAccessTimeout, 0 );
		cacheConfig.properties.put( Key.useLastAccessTimeouts, false );
		// The harness writes a handful of keys; a frequent reaper scan of the cache directory costs
		// far more than it saves and would distort the timings we are measuring
		cacheConfig.properties.put( Key.reapFrequency, 3600 );
		cacheConfig.properties.put( Key.maxObjects, 10000 );
		return runtime.getCacheService().createDefaultCache( CACHE_NAME, cacheConfig );
	}

	/**
	 * --------------------------------------------------------------------------
	 * Helpers
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Parse {@code --key value} pairs.
	 */
	private static Map<String, String> parse( String[] args ) {
		Map<String, String> options = new LinkedHashMap<>();
		for ( int i = 0; i < args.length; i++ ) {
			String arg = args[ i ];
			if ( !arg.startsWith( "--" ) ) {
				continue;
			}
			String key = arg.substring( 2 );
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
