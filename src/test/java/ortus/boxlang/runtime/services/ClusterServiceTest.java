/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ortus.boxlang.runtime.services;

import static com.google.common.truth.Truth.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.async.tasks.TaskClusterClaim;
import ortus.boxlang.runtime.cache.providers.ICacheProvider;
import ortus.boxlang.runtime.config.Configuration;
import ortus.boxlang.runtime.config.segments.CacheConfig;
import ortus.boxlang.runtime.config.segments.ClusterConfig;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * Unit tests for the cluster service.
 * <p>
 * Several simulated nodes run inside this one JVM, each with its own {@link ClusterService} and node
 * id, all pointed at the same coordination cache. That is the enabling trick for testing cluster
 * behavior without real processes: everything below the process boundary is the real code.
 */
class ClusterServiceTest {

	static BoxRuntime		runtime;
	static Path				cacheDirectory;
	static ICacheProvider	sharedCache;

	@BeforeAll
	static void setUp() throws Exception {
		runtime			= BoxRuntime.getInstance( true );
		cacheDirectory	= Files.createTempDirectory( "cluster-service-test" );

		// One cache for the whole cluster, backed by the filesystem so several providers can share it
		CacheConfig cacheConfig = new CacheConfig( "clusterTest" );
		cacheConfig.properties.put( Key.objectStore, "FileSystemStore" );
		cacheConfig.properties.put( Key.directory, cacheDirectory.toString() );
		cacheConfig.properties.put( Key.defaultTimeout, 0 );
		cacheConfig.properties.put( Key.defaultLastAccessTimeout, 0 );
		cacheConfig.properties.put( Key.useLastAccessTimeouts, false );

		sharedCache = runtime.getCacheService().createDefaultCache( "clusterTest", cacheConfig );
	}

	@AfterAll
	static void tearDown() {
		runtime.getCacheService().shutdownCache( Key.of( "clusterTest" ) );
	}

	/**
	 * Build a node: an explicit config with its own name and node id, over the shared cache.
	 */
	private ClusterService newNode( String clusterName, String nodeId ) {
		ClusterConfig config = new ClusterConfig();
		config.enabled		= true;
		config.name			= clusterName;
		config.nodeId		= nodeId;
		config.cacheName	= "clusterTest";
		return new ClusterService( runtime, config, sharedCache );
	}

	@Nested
	@DisplayName( "Keys" )
	class Keys {

		@DisplayName( "keys carry the outer root, the cluster name and the subsystem" )
		@Test
		void testKeyShape() {
			ClusterService node = newNode( "mycluster", "web01" );

			assertThat( node.getKeyPrefix() ).isEqualTo( "bxcluster:mycluster:" );
			assertThat( node.getTaskKeyPrefix() ).isEqualTo( "bxcluster:mycluster:bxtasks:" );
		}

		@DisplayName( "a global task uses the global scope slot" )
		@Test
		void testGlobalTaskKey() {
			ClusterService node = newNode( "mycluster", "web01" );

			assertThat( node.buildTaskKey( null, "ops", "cleanup" ) )
			    .isEqualTo( "bxcluster:mycluster:bxtasks:global:scheduler:ops:task:cleanup" );
			assertThat( node.buildTaskKey( "", "ops", "cleanup" ) )
			    .isEqualTo( "bxcluster:mycluster:bxtasks:global:scheduler:ops:task:cleanup" );
		}

		@DisplayName( "an application task uses the application scope slot" )
		@Test
		void testApplicationTaskKey() {
			ClusterService node = newNode( "mycluster", "web01" );

			assertThat( node.buildTaskKey( "myApp", "ops", "cleanup" ) )
			    .isEqualTo( "bxcluster:mycluster:bxtasks:application:myApp:scheduler:ops:task:cleanup" );
		}

		@DisplayName( "an application named global cannot shadow the global scope" )
		@Test
		void testApplicationNamedGlobalIsDistinct() {
			ClusterService node = newNode( "mycluster", "web01" );

			assertThat( node.buildTaskKey( "global", "ops", "cleanup" ) )
			    .isNotEqualTo( node.buildTaskKey( null, "ops", "cleanup" ) );
		}

		@DisplayName( "names containing a colon are legal and produce distinct keys" )
		@Test
		void testNamesWithColons() {
			ClusterService	node			= newNode( "mycluster", "web01" );

			// Application schedulers are already named "{appName}:{path}", so this must be accepted
			String			appScheduler	= node.buildTaskKey( "myApp", "myApp:/app/config/Scheduler.bx", "cleanup" );
			assertThat( appScheduler ).contains( "scheduler:myApp:/app/config/Scheduler.bx" );

			assertThat( node.buildTaskKey( "a:b", "c", "t" ) )
			    .isNotEqualTo( node.buildTaskKey( "a", "b:c", "t" ) );
		}

		@DisplayName( "different cluster names do not collide" )
		@Test
		void testDifferentClustersDoNotCollide() {
			ClusterService	one	= newNode( "clusterA", "web01" );
			ClusterService	two	= newNode( "clusterB", "web01" );

			assertThat( one.buildTaskKey( null, "ops", "cleanup" ) )
			    .isNotEqualTo( two.buildTaskKey( null, "ops", "cleanup" ) );
		}
	}

	@Nested
	@DisplayName( "Node identity and records" )
	class Records {

		@DisplayName( "a configured node name is used verbatim" )
		@Test
		void testConfiguredNodeNameIsVerbatim() {
			ClusterService node = newNode( "mycluster", "web01" );
			assertThat( node.getNodeId() ).isEqualTo( "web01" );
		}

		@DisplayName( "an unconfigured node name defaults to hostname-pid" )
		@Test
		void testDefaultNodeNameHasPid() {
			ClusterConfig	config		= new ClusterConfig();
			String			generated	= ClusterConfig.defaultNodeId();

			assertThat( generated ).startsWith( ortus.boxlang.runtime.net.NetworkUtil.getLocalHostname() + "-" );
			assertThat( generated ).contains( String.valueOf( ProcessHandle.current().pid() ) );
		}

		@DisplayName( "this node's record is answered from memory, with no cache read" )
		@Test
		void testOwnNodeComesFromMemory() {
			ClusterService node = newNode( "mycluster", "web01" );

			// Nothing has been written to the cache at all
			assertThat( node.peekSnapshot() ).isEmpty();

			IStruct record = node.getNode( "web01" );
			assertThat( record.getAsString( Key.nodeId ) ).isEqualTo( "web01" );
			assertThat( record.containsKey( Key.heartbeatAt ) ).isTrue();
			assertThat( record.containsKey( Key.attributes ) ).isTrue();
		}

		@DisplayName( "an unknown node returns an empty record" )
		@Test
		void testUnknownNodeIsEmpty() {
			ClusterService node = newNode( "mycluster", "web01" );
			assertThat( node.getNode( "nope" ) ).isEmpty();
		}

		@DisplayName( "a written record is visible to a peer after a refresh" )
		@Test
		void testPeerSeesWrittenRecord() {
			ClusterService	one	= newNode( "peers", "nodeOne" );
			ClusterService	two	= newNode( "peers", "nodeTwo" );

			one.writeLocalRecord();
			one.registerInMemberList();
			two.writeLocalRecord();
			two.registerInMemberList();

			IStruct peers = two.getPeers( 0 );
			assertThat( peers.containsKey( Key.of( "nodeOne" ) ) ).isTrue();
			// getPeers excludes self
			assertThat( peers.containsKey( Key.of( "nodeTwo" ) ) ).isFalse();
		}

		@DisplayName( "getNodes includes this node, getPeers does not" )
		@Test
		void testNodesIncludeSelf() {
			ClusterService node = newNode( "selfcheck", "solo" );
			node.writeLocalRecord();
			node.registerInMemberList();

			assertThat( node.getNodes( 0 ).containsKey( Key.of( "solo" ) ) ).isTrue();
			assertThat( node.getPeers( 0 ).containsKey( Key.of( "solo" ) ) ).isFalse();
		}

		@DisplayName( "two clusters in one cache never see each other" )
		@Test
		void testClusterIsolation() {
			ClusterService	inA	= newNode( "isoA", "aNode" );
			ClusterService	inB	= newNode( "isoB", "bNode" );

			inA.writeLocalRecord();
			inA.registerInMemberList();
			inB.writeLocalRecord();
			inB.registerInMemberList();

			assertThat( inA.getPeers( 0 ).containsKey( Key.of( "bNode" ) ) ).isFalse();
			assertThat( inB.getPeers( 0 ).containsKey( Key.of( "aNode" ) ) ).isFalse();
			assertThat( inA.getPeers( 0 ).containsKey( Key.of( "aNode" ) ) ).isFalse();
		}

		@DisplayName( "a node whose heartbeat is stale drops out of the snapshot" )
		@Test
		void testStaleHeartbeatDropsNode() {
			ClusterService	node	= newNode( "stale", "watcher" );

			// Hand-write a peer record with an old heartbeat
			IStruct			dead	= Struct.ofNonConcurrent(
			    Key.nodeId, "deadNode",
			    Key.hostname, "somewhere",
			    Key.ip, "127.0.0.1",
			    Key.startedAt, Instant.now().minusSeconds( 600 ),
			    Key.heartbeatAt, Instant.now().minusSeconds( 600 ),
			    Key.attributesUpdatedAt, Instant.now().minusSeconds( 600 ),
			    Key.attributes, new Struct()
			);
			sharedCache.set( "bxcluster:stale:node:deadNode", dead, 600 );
			sharedCache.set( "bxcluster:stale:nodes", "deadNode", 600 );

			node.invalidateSnapshot();
			assertThat( node.getPeers( 0 ).containsKey( Key.of( "deadNode" ) ) ).isFalse();
		}
	}

	@Nested
	@DisplayName( "Attributes" )
	class Attributes {

		@DisplayName( "setAttribute builds nested paths" )
		@Test
		void testSetAttributeNested() {
			ClusterService node = newNode( "attrs", "a1" );

			node.setAttribute( "perf.stats.cpu.avgLoad", 0.72 );

			Object value = node.getLocalAttributes()
			    .getAsStruct( Key.of( "perf" ) )
			    .getAsStruct( Key.of( "stats" ) )
			    .getAsStruct( Key.of( "cpu" ) )
			    .get( Key.of( "avgLoad" ) );

			assertThat( value ).isEqualTo( 0.72 );
		}

		@DisplayName( "removeAttribute drops the key" )
		@Test
		void testRemoveAttribute() {
			ClusterService node = newNode( "attrs", "a2" );

			node.setAttribute( "perf.disk.freeGb", 128 );
			assertThat( node.getLocalAttributes().getAsStruct( Key.of( "perf" ) ).getAsStruct( Key.of( "disk" ) ) ).isNotNull();

			node.removeAttribute( "perf.disk.freeGb" );
			assertThat( node.getLocalAttributes().getAsStruct( Key.of( "perf" ) ).getAsStruct( Key.of( "disk" ) ).containsKey( Key.of( "freeGb" ) ) )
			    .isFalse();
		}

		@DisplayName( "mergeAttributes merges key by key" )
		@Test
		void testMergeAttributes() {
			ClusterService node = newNode( "attrs", "a3" );

			node.mergeAttributes( Struct.of( "one", 1, "two", 2 ) );
			node.mergeAttributes( Struct.of( "three", 3 ) );

			assertThat( node.getLocalAttributes().getAsInteger( Key.of( "one" ) ) ).isEqualTo( 1 );
			assertThat( node.getLocalAttributes().getAsInteger( Key.of( "three" ) ) ).isEqualTo( 3 );
		}

		@DisplayName( "the live struct is held by reference" )
		@Test
		void testLiveReference() {
			ClusterService	node	= newNode( "attrs", "a4" );

			IStruct			live	= node.getLocalAttributes();
			live.put( Key.of( "scheduler" ), Struct.of( "taskSeconds", 400 ) );

			assertThat( node.getLocalAttributes().getAsStruct( Key.of( "scheduler" ) ).getAsInteger( Key.of( "taskSeconds" ) ) ).isEqualTo( 400 );
		}

		@DisplayName( "a contributor attribute named nodeId does not disturb the core field" )
		@Test
		void testAttributeCannotShadowCoreFields() {
			ClusterService node = newNode( "attrs", "a5" );
			node.setAttribute( "nodeId", "impostor" );

			IStruct record = node.getNode( "a5" );
			assertThat( record.getAsString( Key.nodeId ) ).isEqualTo( "a5" );
			assertThat( ClusterService.attributesOf( record ).getAsString( Key.of( "nodeId" ) ) ).isEqualTo( "impostor" );
		}

		@DisplayName( "arbitrary attribute types round-trip through the cache" )
		@Test
		void testArbitraryTypesRoundTrip() {
			ClusterService	writer	= newNode( "types", "writer" );
			ClusterService	reader	= newNode( "types", "reader" );

			writer.setAttribute( "aString", "hello" );
			writer.setAttribute( "aStruct", Struct.of( "nested", true ) );
			writer.setAttribute( "anArray", ortus.boxlang.runtime.types.Array.of( 1, 2, 3 ) );
			writer.writeLocalRecord();
			writer.registerInMemberList();

			IStruct attributes = ClusterService.attributesOf( reader.getNode( "writer" ) );
			assertThat( attributes.getAsString( Key.of( "aString" ) ) ).isEqualTo( "hello" );
			assertThat( attributes.getAsStruct( Key.of( "aStruct" ) ).getAsBoolean( Key.of( "nested" ) ) ).isTrue();
			assertThat( attributes.getAsArray( Key.of( "anArray" ) ).size() ).isEqualTo( 3 );
		}
	}

	@Nested
	@DisplayName( "Snapshot" )
	class Snapshot {

		@DisplayName( "repeated reads inside the freshness window reuse one snapshot" )
		@Test
		void testDebounce() {
			ClusterService node = newNode( "debounce", "d1" );

			node.getPeers( -1 );
			IStruct first = node.peekSnapshot();

			for ( int i = 0; i < 36; i++ ) {
				node.getPeers( -1 );
			}

			// Same instance: no refresh happened for any of those 36 calls
			assertThat( node.peekSnapshot() ).isSameInstanceAs( first );
		}

		@DisplayName( "nothing asked means nothing read" )
		@Test
		void testLazyPeers() {
			ClusterService node = newNode( "lazy", "l1" );

			assertThat( node.peekSnapshot() ).isEmpty();
			assertThat( node.getNode( "l1" ).getAsString( Key.nodeId ) ).isEqualTo( "l1" );
			// Still no peer read, because only this node's own data was asked for
			assertThat( node.peekSnapshot() ).isEmpty();
		}

		@DisplayName( "a zero freshness forces a re-read" )
		@Test
		void testForceRefresh() {
			ClusterService node = newNode( "force", "f1" );

			node.getPeers( -1 );
			IStruct first = node.peekSnapshot();

			node.getPeers( 0 );
			assertThat( node.peekSnapshot() ).isNotSameInstanceAs( first );
		}
	}

	@Nested
	@DisplayName( "Node attributes read with defaults" )
	class NodeAttributeReads {

		@DisplayName( "a missing node, a missing path and a junk value all give the default" )
		@Test
		void testDefaults() {
			ClusterService node = newNode( "readattrs", "r1" );

			assertThat( node.getNodeAttribute( "missing", "attributes.x", 7 ) ).isEqualTo( 7 );

			node.setAttribute( "real.thing", 5 );
			assertThat( node.getNodeAttribute( "r1", "attributes.real.thing", 7 ) ).isEqualTo( 5 );
			assertThat( node.getNodeAttribute( "r1", "attributes.real.absent", 7 ) ).isEqualTo( 7 );

			// A non-numeric value where a number was expected yields the default rather than throwing
			node.setAttribute( "junk", "not-a-number" );
			assertThat( node.getNodeAttribute( "r1", "attributes.junk", 7 ) ).isEqualTo( 7 );
		}

		@DisplayName( "numeric strings are coerced" )
		@Test
		void testNumericStringsCoerce() {
			ClusterService node = newNode( "readattrs", "r2" );
			node.setAttribute( "sandbox", "42" );

			assertThat( node.getNodeAttribute( "r2", "attributes.sandbox", 0 ) ).isEqualTo( 42.0 );
		}

		@DisplayName( "a navigator is available for chaining" )
		@Test
		void testNavigator() {
			ClusterService node = newNode( "readattrs", "r3" );
			node.setAttribute( "perf.stats.cpu.avgLoad", 0.5 );

			assertThat( node.getNodeNavigator( "r3" ).getAsDouble( "attributes.perf.stats.cpu.avgLoad", 0d ) ).isEqualTo( 0.5 );
		}
	}

	@Nested
	@DisplayName( "Threads" )
	class Threads {

		@DisplayName( "start then stop leaves no cluster threads behind" )
		@Test
		void testThreadsAreStopped() throws Exception {
			ClusterService node = newNode( "threads", "t1" );
			node.getConfig().heartbeatSeconds = 3600;

			node.startLoops();
			assertThat( node.isRunning() ).isTrue();
			// One heartbeat loop now: attributes ride the same beat, so there is no second thread
			assertThat( clusterThreadCount() ).isAtLeast( 1 );

			node.stopLoops();

			assertThat( node.isRunning() ).isFalse();
			assertThat( awaitNoClusterThreads( 10_000 ) ).isTrue();
		}

		@DisplayName( "stop is idempotent and safe when never started" )
		@Test
		void testStopIsIdempotent() {
			ClusterService node = newNode( "threads", "t2" );

			node.stopLoops();
			node.stopLoops();
			assertThat( node.isRunning() ).isFalse();

			node.startLoops();
			node.stopLoops();
			node.stopLoops();
			assertThat( node.isRunning() ).isFalse();
		}

		@DisplayName( "repeated start and stop leaves no threads behind" )
		@Test
		void testRepeatedCycles() throws Exception {
			ClusterService node = newNode( "threads", "t3" );
			node.getConfig().heartbeatSeconds = 3600;

			for ( int i = 0; i < 5; i++ ) {
				node.startLoops();
				node.stopLoops();
			}

			assertThat( awaitNoClusterThreads( 10_000 ) ).isTrue();
		}

		@DisplayName( "the loops are daemon threads" )
		@Test
		void testLoopsAreDaemon() {
			ClusterService node = newNode( "threads", "t4" );
			node.getConfig().heartbeatSeconds = 3600;

			node.startLoops();
			try {
				assertThat( Thread.getAllStackTraces().keySet().stream()
				    .filter( t -> t.getName().startsWith( "boxlang-cluster-" ) )
				    .allMatch( Thread::isDaemon ) ).isTrue();
			} finally {
				node.stopLoops();
			}
		}

		/**
		 * Count live cluster service loop threads.
		 */
		/**
		 * Wait for every cluster loop to retire. Shutdown joins with a bound, so a loop can still be finishing
		 * its last write when shutdown returns; the contract is that it is gone shortly after, not instantly.
		 */
		private boolean awaitNoClusterThreads( long timeoutMillis ) throws InterruptedException {
			long deadline = System.currentTimeMillis() + timeoutMillis;
			while ( System.currentTimeMillis() < deadline ) {
				if ( clusterThreadCount() == 0 ) {
					return true;
				}
				Thread.sleep( 50 );
			}
			return clusterThreadCount() == 0;
		}

		private long clusterThreadCount() {
			return Thread.getAllStackTraces().keySet().stream()
			    .filter( Thread::isAlive )
			    .filter( t -> t.getName().startsWith( "boxlang-cluster-" ) )
			    .count();
		}
	}

	@Nested
	@DisplayName( "Minimum period" )
	class MinimumPeriod {

		@DisplayName( "the eager floor comes from the settle rounds plus the random delay" )
		@Test
		void testEagerFloor() {
			ClusterConfig config = new ClusterConfig();

			// 2 x 1000 + 1000 + 1000 margin = 4000ms
			assertThat( TaskClusterClaim.minimumPeriodSeconds() ).isEqualTo( 4 );
		}

		@DisplayName( "the balanced floor includes the pushback budget" )
		@Test
		void testBalancedFloor() {
			ClusterConfig config = new ClusterConfig();

			// 2 x 1000 + 5000 + 1000 margin = 8000ms, but only when the scheduler says balanced
			ortus.boxlang.runtime.BoxRuntime.getInstance().getConfiguration().scheduler.clusterMode = "balanced";
			try {
				assertThat( TaskClusterClaim.minimumPeriodSeconds() ).isEqualTo( 8 );
			} finally {
				ortus.boxlang.runtime.BoxRuntime.getInstance().getConfiguration().scheduler.clusterMode = "eager";
			}
		}
	}

	@Nested
	@DisplayName( "Configuration" )
	class Config {

		@DisplayName( "scheduler.cacheName is a deprecated bridge: it enables clustering and becomes its cache" )
		@Test
		void testLegacySchedulerCacheNameBridge() {
			Configuration config = new Configuration();
			config.process( Struct.of(
			    Key.scheduler, Struct.of( Key.cacheName, "legacyCoordination" )
			) );

			assertThat( config.cluster.enabled ).isTrue();
			assertThat( config.cluster.cacheName ).isEqualTo( "legacyCoordination" );

			// The cluster block still wins when it says otherwise
			Configuration explicit = new Configuration();
			explicit.process( Struct.of(
			    Key.scheduler, Struct.of( Key.cacheName, "legacyCoordination" ),
			    Key.cluster, Struct.of( Key.cacheName, "explicitCache", Key.enabled, false )
			) );

			assertThat( explicit.cluster.enabled ).isFalse();
			assertThat( explicit.cluster.cacheName ).isEqualTo( "explicitCache" );

			// The old shipped default was "default", so every legacy config has this key. It must NOT opt in,
			// otherwise upgrading silently enables clustering pointed at an in-memory cache.
			Configuration legacyDefault = new Configuration();
			legacyDefault.process( Struct.of(
			    Key.scheduler, Struct.of( Key.cacheName, "default" )
			) );

			assertThat( legacyDefault.cluster.enabled ).isFalse();
			assertThat( legacyDefault.cluster.cacheName ).isEqualTo( "bxcluster" );
		}

		@DisplayName( "enabling clustering with a cache that does not exist fails at startup with a useful message" )
		@Test
		void testMissingCoordinationCacheFailsAtStartup() {
			ClusterConfig	config			= BoxRuntime.getInstance().getConfiguration().cluster;
			String			originalCache	= config.cacheName;
			boolean			originalEnabled	= config.enabled;

			config.enabled		= true;
			config.cacheName	= "no-such-cluster-cache";
			try {
				ClusterService		service	= new ClusterService( BoxRuntime.getInstance(), config, null );
				BoxRuntimeException	e		= org.junit.jupiter.api.Assertions.assertThrows( BoxRuntimeException.class, service::onStartup );
				assertThat( e.getMessage() ).contains( "no-such-cluster-cache" );
				assertThat( e.getMessage() ).contains( "caches" );
			} finally {
				config.cacheName	= originalCache;
				config.enabled		= originalEnabled;
			}
		}

		@DisplayName( "the cluster name defaults to default" )
		@Test
		void testClusterNameDefault() {
			ClusterConfig config = new ClusterConfig();
			assertThat( config.name ).isEqualTo( "default" );

			assertThat( config.enabled ).isFalse();
		}
	}

	@Nested
	@DisplayName( "Capability exposure" )
	class Capability {

		@DisplayName( "the service exposes the cache, cache name and cluster name" )
		@Test
		void testAccessors() {
			ClusterService node = newNode( "capability", "c1" );

			assertThat( node.getCache() ).isNotNull();
			assertThat( node.getCacheName() ).isEqualTo( "clusterTest" );
			assertThat( node.getClusterName() ).isEqualTo( "capability" );
			assertThat( node.getNodeId() ).isEqualTo( "c1" );
		}
	}

}
