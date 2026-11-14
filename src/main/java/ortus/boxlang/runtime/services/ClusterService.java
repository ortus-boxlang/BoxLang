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
package ortus.boxlang.runtime.services;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.ReentrantLock;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.cache.providers.ICacheProvider;
import ortus.boxlang.runtime.config.segments.ClusterConfig;
import ortus.boxlang.runtime.dynamic.Attempt;
import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.dynamic.casters.DoubleCaster;
import ortus.boxlang.runtime.logging.BoxLangLogger;
import ortus.boxlang.runtime.net.NetworkUtil;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.async.tasks.TaskClusterClaim;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.util.DataNavigator;
import ortus.boxlang.runtime.util.DataNavigator.Navigator;

/**
 * The cluster service owns cluster membership and this node's record in the shared coordination
 * cache. It is deliberately not part of the scheduler: scheduled tasks are one consumer, and cache
 * invalidation, distributed locking, or per-node health aggregation can be others.
 * <p>
 * <h2>What it owns</h2>
 * <ul>
 * <li>node identity (see {@link ClusterConfig#nodeId})</li>
 * <li>this node's record, rewritten on the heartbeat cadence</li>
 * <li>this node's attributes, held in memory and written out with every record write</li>
 * <li>a lazily refreshed, debounced snapshot of the other nodes</li>
 * </ul>
 * <p>
 * <h2>What it does not do</h2>
 * It publishes and hands data back, and it <b>never decides anything</b>: no leader election, no
 * "who is busiest", no claim arbitration. Every semantic judgment belongs to the consumer. That is
 * what keeps this from growing into a distributed consensus framework.
 * <p>
 * <h2>Keys</h2>
 *
 * <pre>
 * bxcluster : {cluster} : {subsystem} : ...
 * bxcluster:mycluster:nodes                        the member list
 * bxcluster:mycluster:node:{nodeId}                a node's record
 * bxcluster:mycluster:bxtasks:...                  the scheduler's records (owned by the scheduler)
 * </pre>
 *
 * <h2>Attributes</h2>
 * The record's {@code attributes} key is an open struct. Any contributor may add any keys of any
 * type, and the semantics belong to whoever added them - see {@link #setAttribute}, {@link #mergeAttributes}
 * and {@link #removeAttribute} for the push door, and the {@code onClusterNodeAttributes}
 * announcement for the pull door.
 */
public class ClusterService extends BaseService {

	/**
	 * The service name
	 */
	public static final Key												NAME						= Key.of( "ClusterService" );

	/**
	 * The announcement fired when contributors should add their data to this node's attributes.
	 */
	public static final Key												ON_NODE_ATTRIBUTES			= Key.of( "onClusterNodeAttributes" );

	/**
	 * The root of every key this feature writes, and the scheduler subsystem segment inside it.
	 */
	public static final String											CLUSTER_ROOT				= "bxcluster";
	public static final String											SCHEDULER_SUBSYSTEM			= "bxtasks";

	/**
	 * How long a single peer snapshot refresh may take before the read fails.
	 */
	private static final long											SNAPSHOT_TIMEOUT_MILLIS		= 5_000;

	/**
	 * The member list is written with the same optimistic pattern as a claim: write, settle, read back, and
	 * retry a few times before giving up.
	 */
	private static final int											MEMBER_LIST_WRITE_ATTEMPTS	= 3;
	private static final long											MEMBER_LIST_SETTLE_MILLIS	= 250;

	/**
	 * Label used for a task record that belongs to no application. It sits in the same slot as
	 * {@code application:{name}}, so there is no placeholder pretending to be a real name.
	 */
	public static final String											GLOBAL_SCOPE				= "global";

	/**
	 * Label used for the application scope segment.
	 */
	public static final String											APPLICATION_SCOPE			= "application";

	private static final String											KEY_NODES					= "nodes";
	private static final String											KEY_NODE					= "node";

	/**
	 * This node's resolved configuration
	 */
	private ClusterConfig												config;

	/**
	 * Logger
	 */
	private BoxLangLogger												logger;

	/**
	 * The resolved coordination cache. Null until startup, or when the cache is missing.
	 */
	private volatile ICacheProvider										cache;

	/**
	 * This node's identity, unique per running process
	 */
	private String														nodeId;

	/**
	 * This node's attributes, held in memory and written with every record write. The live reference
	 * is handed out by {@link #getLocalAttributes()}.
	 */
	private final IStruct												localAttributes				= new Struct();

	/**
	 * When this node's attributes were last collected from contributors
	 */
	private volatile Instant											attributesUpdatedAt			= Instant.now();

	/**
	 * When this node started
	 */
	private Instant														startedAt					= Instant.now();

	/**
	 * The last peer snapshot, and when it was taken
	 */
	private volatile IStruct											snapshot					= null;
	private volatile long												snapshotAt					= 0;

	/**
	 * Guards a snapshot refresh so concurrent callers collapse onto one cache read
	 */
	private final ReentrantLock											refreshLock					= new ReentrantLock();

	/**
	 * Extra attribute contributors. The scheduler registers one so its rolling task-seconds metric is
	 * published with the node record; anything else can register too. Contributors are additive and
	 * run before the record write.
	 */
	private final java.util.List<java.util.function.Supplier<IStruct>>	attributeContributors		= new java.util.concurrent.CopyOnWriteArrayList<>();

	/**
	 * The service loops
	 */
	private volatile Thread												heartbeatThread;
	private volatile Thread												attributesThread;
	private volatile boolean											loopsRunning				= false;

	/**
	 * The shutdown latch the loops watch
	 */
	private volatile boolean											stopRequested				= false;

	/**
	 * Once true, the service has been shut down and must not start its loops again
	 */
	private volatile boolean											closed						= false;

	/**
	 * How long shutdown waits for a loop to exit
	 */
	private static final long											LOOP_JOIN_MILLIS			= 500;

	/**
	 * Constructor
	 *
	 * @param runtime The runtime singleton
	 */
	public ClusterService( BoxRuntime runtime ) {
		super( runtime, NAME );
	}

	/**
	 * Constructor for tests and advanced embedding: run this instance against an explicit
	 * configuration and coordination cache instead of the runtime's global ones. This is what lets a
	 * single JVM host several simulated cluster nodes.
	 *
	 * @param runtime The runtime singleton
	 * @param config  The cluster configuration for this node
	 * @param cache   The coordination cache this node writes to
	 */
	public ClusterService( BoxRuntime runtime, ClusterConfig config, ICacheProvider cache ) {
		super( runtime, NAME );
		this.config	= config;
		this.cache	= cache;
		this.nodeId	= this.config.nodeId != null ? this.config.nodeId : ClusterConfig.defaultNodeId();
		this.logger	= runtime.getLoggingService().RUNTIME_LOGGER;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Lifecycle
	 * --------------------------------------------------------------------------
	 */

	/**
	 * @inheritDoc
	 */
	@Override
	public void onConfigurationLoad() {
		this.logger	= runtime.getLoggingService().RUNTIME_LOGGER;
		this.config	= runtime.getConfiguration().cluster;
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public void onStartup() {
		this.closed	= false;
		this.config	= runtime.getConfiguration().cluster;
		this.nodeId	= this.config.nodeId != null ? this.config.nodeId : ClusterConfig.defaultNodeId();
		this.logger	= runtime.getLoggingService().RUNTIME_LOGGER;

		this.logger.info(
		    "+ Cluster service [{}] starting as node [{}] in cluster [{}], cache [{}], mode [{}]",
		    this.getName().getName(),
		    this.nodeId,
		    this.config.name,
		    this.config.cacheName,
		    TaskClusterClaim.modeName()
		);

		if ( !this.config.enabled ) {
			this.logger.debug( "+ Cluster service is disabled, no loops started" );
			return;
		}

		// Fail loudly at startup, not on the first claim: the cache has to exist *and* work
		verifyCoordinationCache( resolveCache( true ) );
		startLoops();
	}

	/**
	 * Prove the coordination cache can actually be used, so a misconfiguration surfaces at startup instead
	 * of as a failed claim minutes later. Write, read back, remove - the same operations the claim protocol
	 * needs from the store.
	 *
	 * @param cache The resolved coordination cache
	 */
	private void verifyCoordinationCache( ICacheProvider cache ) {
		String	probeKey	= getKeyPrefix() + "health:" + this.nodeId;
		String	token		= UUID.randomUUID().toString();

		try {
			cache.set( probeKey, token, 60 );
			Object readBack = cache.get( probeKey ).orElse( null );
			cache.clear( probeKey );

			if ( readBack == null || !token.equals( readBack.toString() ) ) {
				throw new BoxRuntimeException(
				    "The cluster coordination cache [" + this.config.cacheName + "] did not return what was written to it. "
				        + "A cache for clustering must be backed by a store every node can see (a shared FileSystemStore "
				        + "directory, a JDBC store, or a distributed provider such as Redis)."
				);
			}
		} catch ( BoxRuntimeException e ) {
			throw e;
		} catch ( Throwable t ) {
			throw new BoxRuntimeException(
			    "The cluster coordination cache [" + this.config.cacheName + "] could not be used: " + t.getMessage()
			        + ". Every node must point at a cache backed by a shared store.",
			    t
			);
		}
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public void onShutdown( Boolean force ) {
		// Set the gate first, so nothing can restart the loops while we tear them down
		this.closed = true;
		stopLoops();
		this.cache = null;
	}

	/**
	 * Start the service loops. Called at startup when the cluster is enabled, and public so the
	 * scheduler can bring the service up precisely when a clustered task needs it.
	 */
	public synchronized void startLoops() {
		if ( this.closed ) {
			// Once the service has shut down, its loops must never come back
			this.logger.warn( "+ Cluster service is shut down, refusing to start its loops again" );
			return;
		}
		if ( this.loopsRunning || !this.config.enabled ) {
			return;
		}
		this.stopRequested		= false;
		this.startedAt			= Instant.now();

		this.heartbeatThread	= newLoopThread( "boxlang-cluster-heartbeat", this::heartbeatLoop );
		this.attributesThread	= newLoopThread( "boxlang-cluster-attributes", this::attributesLoop );

		this.heartbeatThread.start();
		this.attributesThread.start();
		this.loopsRunning = true;

		// Announce ourselves immediately so peers show up without waiting a full cadence
		writeLocalRecord();
		registerInMemberList();
		this.logger.info( "+ Cluster service loops started for node [{}]", this.nodeId );
	}

	/**
	 * Stop the service loops and wait for them to exit, so no thread outlives the service.
	 * Idempotent, and safe when the loops never started.
	 */
	public synchronized void stopLoops() {
		if ( !this.loopsRunning ) {
			return;
		}

		this.stopRequested = true;

		Thread	heartbeat	= this.heartbeatThread;
		Thread	attributes	= this.attributesThread;

		if ( heartbeat != null ) {
			heartbeat.interrupt();
		}
		if ( attributes != null ) {
			attributes.interrupt();
		}

		joinQuietly( heartbeat );
		joinQuietly( attributes );

		this.heartbeatThread	= null;
		this.attributesThread	= null;
		this.loopsRunning		= false;
		this.stopRequested		= false;

		this.logger.info( "+ Cluster service loops stopped for node [{}]", this.nodeId );
	}

	/**
	 * Create a named daemon loop thread. Daemon so a stuck loop can never keep the JVM alive; named
	 * so it is identifiable in a thread dump.
	 */
	private Thread newLoopThread( String name, Runnable body ) {
		Thread thread = new Thread( body, name );
		thread.setDaemon( true );
		return thread;
	}

	private void joinQuietly( Thread thread ) {
		if ( thread == null ) {
			return;
		}
		try {
			thread.join( LOOP_JOIN_MILLIS );
			// The loops are daemons, so a straggler can never hold the JVM open. Waiting longer than this buys
			// nothing and just delays shutdown, so say so and move on.
			if ( thread.isAlive() ) {
				this.logger.warn(
				    "+ Cluster loop [{}] was still alive {}ms after the shutdown request; it will exit on its next check",
				    thread.getName(),
				    LOOP_JOIN_MILLIS
				);
			}
		} catch ( InterruptedException e ) {
			Thread.currentThread().interrupt();
		}
	}

	/**
	 * Is the service running its loops?
	 *
	 * @return true when the loops are running
	 */
	public boolean isRunning() {
		return this.loopsRunning;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Loops
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Liveness loop: rewrite this node's record so peers see a fresh heartbeat.
	 */
	private void heartbeatLoop() {
		while ( !stopRequested ) {
			try {
				if ( !sleepSeconds( this.config.heartbeatSeconds ) ) {
					break;
				}
				// Never start a record write once shutdown has been requested: the interrupt cannot abort a
				// cache write in progress, so starting one here is what made an idle node sit on the shutdown.
				if ( stopRequested ) {
					break;
				}
				writeLocalRecord();
			} catch ( Throwable t ) {
				this.logger.error( "+ Cluster heartbeat error on node [{}]: {}", this.nodeId, t.getMessage() );
			}
		}
	}

	/**
	 * Attributes loop: ask contributors for their data, merge it, and write the record.
	 * <p>
	 * A contributor that throws is logged and skipped; the others still contribute and the record is
	 * still written, so no contributor can make a node look dead.
	 */
	private void attributesLoop() {
		while ( !stopRequested ) {
			try {
				if ( !sleepSeconds( this.config.heartbeatSeconds ) ) {
					break;
				}
				if ( stopRequested ) {
					break;
				}
				IStruct payload = Struct.ofNonConcurrent(
				    Key.nodeId, this.nodeId,
				    Key.attributes, this.localAttributes
				);
				announce( ON_NODE_ATTRIBUTES, payload );

				// Registered contributors publish without needing an interceptor, so core components
				// (the scheduler) do not have to depend on interceptor plumbing
				mergeContributors();

				this.attributesUpdatedAt = Instant.now();
				writeLocalRecord();
			} catch ( Throwable t ) {
				this.logger.error( "+ Cluster attributes error on node [{}]: {}", this.nodeId, t.getMessage() );
			}
		}
	}

	/**
	 * Sleep for the given number of seconds, waking early on interrupt.
	 *
	 * @return false when the loop should stop
	 */
	private boolean sleepSeconds( long seconds ) {
		if ( seconds <= 0 ) {
			return !stopRequested;
		}
		try {
			Thread.sleep( seconds * 1000L );
			return !stopRequested;
		} catch ( InterruptedException e ) {
			Thread.currentThread().interrupt();
			return false;
		}
	}

	/**
	 * --------------------------------------------------------------------------
	 * Cache and identity
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Resolve the coordination cache. When failFast is on and the cache is missing, this throws
	 * rather than silently falling back to a per-node cache, which would coordinate nothing.
	 */
	private ICacheProvider resolveCache( boolean strict ) {
		if ( this.cache != null ) {
			return this.cache;
		}
		CacheService cacheService = runtime.getCacheService();
		if ( cacheService.hasCache( this.config.cacheName ) ) {
			this.cache = cacheService.getCache( this.config.cacheName );
		} else if ( strict ) {
			throw new BoxRuntimeException(
			    "The cluster coordination cache [" + this.config.cacheName + "] does not exist. "
			        + "Declare it under [caches] and point every node at the same data."
			);
		}
		return this.cache;
	}

	/**
	 * The one coordination cache, exposed so consumers (the scheduler, for its task records) do not
	 * have to read the config themselves.
	 *
	 * @return The cache provider
	 */
	public ICacheProvider getCache() {
		ICacheProvider resolved = resolveCache( false );
		if ( resolved == null ) {
			throw new BoxRuntimeException( "The cluster coordination cache [" + this.config.cacheName + "] is not available." );
		}
		return resolved;
	}

	/**
	 * The name of the coordination cache.
	 *
	 * @return The cache name
	 */
	public String getCacheName() {
		return this.config.cacheName;
	}

	/**
	 * The cluster name. Every key lives under it, so a consumer composing its own keys puts this
	 * right after the cluster root.
	 *
	 * @return The cluster name
	 */
	public String getClusterName() {
		return this.config.name;
	}

	/**
	 * This node's identity.
	 *
	 * @return The node id
	 */
	public String getNodeId() {
		return this.nodeId;
	}

	/**
	 * The resolved cluster configuration.
	 *
	 * @return The configuration
	 */
	public ClusterConfig getConfig() {
		return this.config;
	}

	/**
	 * The cluster root prefix every key in the coordination cache starts with.
	 *
	 * @return e.g. {@code bxcluster:mycluster:}
	 */
	public String getKeyPrefix() {
		return CLUSTER_ROOT + ":" + this.config.name + ":";
	}

	/**
	 * The key prefix for the scheduler's records under this cluster.
	 *
	 * @return e.g. {@code bxcluster:mycluster:bxtasks:}
	 */
	public String getTaskKeyPrefix() {
		return getKeyPrefix() + SCHEDULER_SUBSYSTEM + ":";
	}

	private String nodesKey() {
		return getKeyPrefix() + KEY_NODES;
	}

	private String nodeKey( String forNodeId ) {
		return getKeyPrefix() + KEY_NODE + ":" + forNodeId;
	}

	/**
	 * Register a contributor of extra attributes. Its struct is merged into this node's attributes
	 * before each record write, so a component can publish data without any interceptor machinery.
	 *
	 * @param contributor A supplier of attributes to merge
	 *
	 * @return This service
	 */
	public ClusterService addAttributeContributor( java.util.function.Supplier<IStruct> contributor ) {
		if ( contributor != null ) {
			this.attributeContributors.add( contributor );
		}
		return this;
	}

	/**
	 * Remove a previously registered contributor.
	 *
	 * @param contributor The contributor to remove
	 *
	 * @return This service
	 */
	public ClusterService removeAttributeContributor( java.util.function.Supplier<IStruct> contributor ) {
		this.attributeContributors.remove( contributor );
		return this;
	}

	/**
	 * Merge every registered contributor's attributes into this node's attributes. A contributor that
	 * throws is logged and skipped, so one bad contributor cannot stop the record being written.
	 */
	public void mergeContributors() {
		for ( java.util.function.Supplier<IStruct> contributor : this.attributeContributors ) {
			try {
				IStruct contributed = contributor.get();
				if ( contributed != null ) {
					mergeAttributes( contributed );
				}
			} catch ( Throwable t ) {
				this.logger.error( "+ Cluster attribute contributor failed: {}", t.getMessage() );
			}
		}
	}

	/**
	 * --------------------------------------------------------------------------
	 * Local attributes - the push door
	 * --------------------------------------------------------------------------
	 */

	/**
	 * This node's live attributes struct, <b>by reference</b>. Hold it and mutate it whenever you
	 * like; the next record write picks up whatever is there.
	 *
	 * @return The live attributes struct
	 */
	public IStruct getLocalAttributes() {
		return this.localAttributes;
	}

	/**
	 * Set one attribute by dotted path, creating intermediate structs as needed.
	 *
	 * @param path  The dotted path, e.g. {@code perf.stats.cpu.avgLoad}
	 * @param value The value, of any type
	 *
	 * @return This service
	 */
	public ClusterService setAttribute( String path, Object value ) {
		String[]	segments	= splitPath( path );
		IStruct		current		= this.localAttributes;

		for ( int i = 0; i < segments.length - 1; i++ ) {
			Object next = current.get( Key.of( segments[ i ] ) );
			if ( ! ( next instanceof IStruct ) ) {
				IStruct created = new Struct();
				current.put( Key.of( segments[ i ] ), created );
				current = created;
			} else {
				current = ( IStruct ) next;
			}
		}

		current.put( Key.of( segments[ segments.length - 1 ] ), value );
		return this;
	}

	/**
	 * Merge a struct of attributes into this node's attributes, shallowly, key by key.
	 *
	 * @param attributes The attributes to merge
	 *
	 * @return This service
	 */
	public ClusterService mergeAttributes( IStruct attributes ) {
		if ( attributes != null ) {
			attributes.forEach( ( key, value ) -> this.localAttributes.put( key, value ) );
		}
		return this;
	}

	/**
	 * Remove one attribute by dotted path. A contributor that stops reporting a metric should drop
	 * the key rather than leave a stale value that consumers keep reading.
	 *
	 * @param path The dotted path
	 *
	 * @return This service
	 */
	public ClusterService removeAttribute( String path ) {
		String[]	segments	= splitPath( path );
		IStruct		current		= this.localAttributes;

		for ( int i = 0; i < segments.length - 1; i++ ) {
			Object next = current.get( Key.of( segments[ i ] ) );
			if ( ! ( next instanceof IStruct ) ) {
				return this;
			}
			current = ( IStruct ) next;
		}

		current.remove( Key.of( segments[ segments.length - 1 ] ) );
		return this;
	}

	private static String[] splitPath( String path ) {
		return path.split( "\\." );
	}

	/**
	 * --------------------------------------------------------------------------
	 * Records
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Build a fresh record for this node. The heartbeat is always current; the attributes are
	 * whatever is in memory.
	 *
	 * @return The record
	 */
	public IStruct buildLocalRecord() {
		return Struct.ofNonConcurrent(
		    Key.nodeId, this.nodeId,
		    Key.hostname, NetworkUtil.getLocalHostname(),
		    Key.ip, NetworkUtil.getLocalIPAddress(),
		    Key.startedAt, this.startedAt,
		    Key.heartbeatAt, Instant.now(),
		    Key.attributesUpdatedAt, this.attributesUpdatedAt,
		    Key.attributes, this.localAttributes
		);
	}

	/**
	 * Write this node's record to the coordination cache.
	 * <p>
	 * The whole record is rewritten, including attributes, because the cache has no partial update:
	 * {@code set} replaces the entry. That is why the attributes are held in memory.
	 */
	public void writeLocalRecord() {
		ICacheProvider resolved = resolveCache( false );
		if ( resolved == null ) {
			return;
		}
		try {
			resolved.set( nodeKey( this.nodeId ), buildLocalRecord(), nodeTtlSeconds() );
		} catch ( Throwable t ) {
			this.logger.error( "+ Cluster failed to write node record [{}]: {}", this.nodeId, t.getMessage() );
		}
	}

	private long nodeTtlSeconds() {
		return Math.max( this.config.heartbeatSeconds * 3 * 2, this.config.heartbeatSeconds * 2 );
	}

	/**
	 * --------------------------------------------------------------------------
	 * Consumer API
	 * --------------------------------------------------------------------------
	 */

	/**
	 * The record for one node. <b>This node is answered from memory</b> and never read back from the
	 * cache.
	 *
	 * @param forNodeId The node id
	 *
	 * @return The record, or an empty struct when the node is unknown
	 */
	public IStruct getNode( String forNodeId ) {
		if ( forNodeId != null && forNodeId.equals( this.nodeId ) ) {
			return buildLocalRecord();
		}
		IStruct fromSnapshot = getSnapshot( -1 ).getAsStruct( Key.of( forNodeId ) );
		return fromSnapshot == null ? new Struct() : fromSnapshot;
	}

	/**
	 * The same data as {@link #getNode(String)}, wrapped in a {@link Navigator} for chaining.
	 *
	 * @param forNodeId The node id
	 *
	 * @return A navigator over the record
	 */
	public Navigator getNodeNavigator( String forNodeId ) {
		return DataNavigator.of( getNode( forNodeId ) );
	}

	/**
	 * Every known node, including this one.
	 *
	 * @return A struct of nodeId to record
	 */
	public IStruct getNodes() {
		return getNodes( -1 );
	}

	/**
	 * Every known node, including this one, honoring a per-call freshness window.
	 *
	 * @param freshnessMillis Milliseconds the snapshot may be old; {@code 0} forces a re-read
	 *
	 * @return A struct of nodeId to record
	 */
	public IStruct getNodes( long freshnessMillis ) {
		IStruct	result	= new Struct();
		IStruct	snap	= getSnapshot( freshnessMillis );

		result.put( Key.of( this.nodeId ), buildLocalRecord() );
		snap.forEach( ( key, value ) -> result.put( key, value ) );
		return result;
	}

	/**
	 * Every known node <b>except this one</b>. One ranking other nodes uses this; one building a full
	 * picture uses {@link #getNodes()}.
	 *
	 * @return A struct of nodeId to record
	 */
	public IStruct getPeers() {
		return getPeers( -1 );
	}

	/**
	 * Every known node except this one, honoring a per-call freshness window.
	 *
	 * @param freshnessMillis Milliseconds the snapshot may be old; {@code 0} forces a re-read
	 *
	 * @return A struct of nodeId to record
	 */
	public IStruct getPeers( long freshnessMillis ) {
		IStruct	peers	= new Struct();
		IStruct	snap	= getSnapshot( freshnessMillis );
		snap.forEach( ( key, value ) -> {
			if ( !key.getName().equals( this.nodeId ) ) {
				peers.put( key, value );
			}
		} );
		return peers;
	}

	/**
	 * Read one attribute from one node by dotted path, with a default.
	 * <p>
	 * Uses attempt-style casting rather than a plain cast, so a contributor that stored a
	 * non-numeric value where a number was expected yields the caller's default instead of throwing.
	 * That is what stops one contributor's bad value from breaking another component's read.
	 *
	 * @param forNodeId    The node id
	 * @param path         The dotted path into the record, e.g.
	 *                     {@code attributes.perf.stats.cpu.avgLoad}
	 * @param defaultValue Returned when the node, the path, or the value is unusable
	 *
	 * @return The value, or the default
	 */
	public Object getNodeAttribute( String forNodeId, String path, Object defaultValue ) {
		IStruct record = getNode( forNodeId );
		if ( record.isEmpty() ) {
			return defaultValue;
		}

		// A node that is alive but whose attributes are too old has no trustworthy contributed data
		if ( !isAttributeDataFresh( record ) && path != null && path.startsWith( Key.attributes.getName() ) ) {
			return defaultValue;
		}

		Object value = DataNavigator.of( record ).get( path, defaultValue );
		return coerce( value, defaultValue );
	}

	/**
	 * Coerce a stored value towards the type the caller asked for, falling back to the caller's
	 * default when the stored value cannot be interpreted.
	 */
	private Object coerce( Object value, Object defaultValue ) {
		if ( value == null || ( value instanceof String s && s.isBlank() ) ) {
			return defaultValue;
		}
		if ( defaultValue instanceof Number ) {
			Attempt<Double> asDouble = DoubleCaster.attempt( value );
			return asDouble.isPresent() ? asDouble.get() : defaultValue;
		}
		if ( defaultValue instanceof Boolean ) {
			Attempt<Boolean> asBoolean = BooleanCaster.attempt( value );
			return asBoolean.isPresent() ? asBoolean.get() : defaultValue;
		}
		return value;
	}

	/**
	 * Is this record's contributed data still trustworthy?
	 */
	private boolean isAttributeDataFresh( IStruct record ) {
		if ( this.config.heartbeatSeconds * 3 <= 0 ) {
			return true;
		}
		Object updated = record.get( Key.attributesUpdatedAt );
		if ( ! ( updated instanceof Instant updatedAt ) ) {
			return true;
		}
		return updatedAt.plusSeconds( this.config.heartbeatSeconds * 3 ).isAfter( Instant.now() );
	}

	/**
	 * --------------------------------------------------------------------------
	 * Snapshot
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Get the peer snapshot, refreshing it when it is missing or older than the freshness window.
	 * <p>
	 * Peer reads are lazy: nothing polls peers on a timer, so a deployment where nothing asks for
	 * them does no peer reads at all. A stale snapshot triggers exactly one refresh no matter how many
	 * callers are asking, and callers block on that refresh rather than receiving a half-populated
	 * snapshot.
	 *
	 * @param freshnessMillis Milliseconds the cached snapshot may be old; a negative value uses the
	 *                        configured window, and {@code 0} forces a re-read
	 *
	 * @return A struct of nodeId to record, excluding this node
	 */
	public IStruct getSnapshot( long freshnessMillis ) {
		long	window	= freshnessMillis < 0 ? this.config.heartbeatSeconds * 1000 : freshnessMillis;

		IStruct	current	= this.snapshot;
		long	age		= System.currentTimeMillis() - this.snapshotAt;

		// A zero (or negative) window means "always re-read"
		if ( current != null && window > 0 && age <= window && age >= 0 ) {
			return current;
		}

		return refreshSnapshot();
	}

	/**
	 * Force a snapshot refresh, blocking until it completes or the configured timeout elapses.
	 * <p>
	 * Concurrent callers collapse onto a single refresh: whoever wins the lock does the read, and the
	 * others see the result rather than each starting their own cache read.
	 *
	 * @return The fresh snapshot
	 */
	public IStruct refreshSnapshot() {
		// Remember what we saw, so we can tell whether someone else refreshed while we waited
		long	seenAt	= this.snapshotAt;

		boolean	acquired;
		try {
			acquired = this.refreshLock.tryLock( this.SNAPSHOT_TIMEOUT_MILLIS, java.util.concurrent.TimeUnit.MILLISECONDS );
		} catch ( InterruptedException e ) {
			Thread.currentThread().interrupt();
			throw new BoxRuntimeException( "Interrupted while waiting for a cluster snapshot refresh" );
		}

		if ( !acquired ) {
			throw new BoxRuntimeException(
			    "Timed out after [" + this.SNAPSHOT_TIMEOUT_MILLIS + "ms] waiting for a cluster snapshot refresh. "
			        + "Raise cluster.snapshotTimeoutMillis, or read with a longer freshness window."
			);
		}

		try {
			// Someone else refreshed while we waited for the lock: use theirs instead of reading again
			IStruct current = this.snapshot;
			if ( current != null && this.snapshotAt != seenAt ) {
				return current;
			}

			IStruct fresh = readSnapshotFromCache();
			this.snapshot	= fresh;
			this.snapshotAt	= System.currentTimeMillis();
			return fresh;
		} finally {
			this.refreshLock.unlock();
		}
	}

	/**
	 * Read every peer record named by the member list, dropping nodes whose heartbeat is stale, and
	 * rewrite the member list to match.
	 * <p>
	 * The member list is only rewritten when the live set actually changed (or when this node is not
	 * yet in it). It is a shared cache write, so rewriting it on every refresh would double the write
	 * traffic for no benefit.
	 */
	private IStruct readSnapshotFromCache() {
		IStruct			fresh		= new Struct();
		ICacheProvider	resolved	= resolveCache( false );
		if ( resolved == null ) {
			return fresh;
		}

		Object	rawList	= resolved.get( nodesKey() ).orElse( null );
		String	list	= rawList == null ? "" : rawList.toString();
		Instant	now		= Instant.now();

		for ( String candidate : list.split( "\\R" ) ) {
			String candidateId = candidate.trim();
			if ( candidateId.isEmpty() || candidateId.equals( this.nodeId ) ) {
				continue;
			}

			Object record = resolved.get( nodeKey( candidateId ) ).orElse( null );
			if ( ! ( record instanceof IStruct recordStruct ) ) {
				// The record expired or was never written: the node is gone
				continue;
			}

			if ( isHeartbeatStale( recordStruct, now ) ) {
				continue;
			}

			fresh.put( Key.of( candidateId ), recordStruct );
		}

		// Rewrite the member list so a departure needs no explicit leave protocol, but only when
		// something actually changed
		IStruct	previous	= this.snapshot;
		boolean	changed		= previous == null || !previous.keySet().equals( fresh.keySet() ) || !list.contains( this.nodeId );
		if ( changed ) {
			writeMemberList( fresh.keySet().stream().map( Key::getName ).toList(), true );
		}

		return fresh;
	}

	private boolean isHeartbeatStale( IStruct record, Instant now ) {
		if ( this.config.heartbeatSeconds * 3 <= 0 ) {
			return false;
		}
		Object heartbeat = record.get( Key.heartbeatAt );
		if ( ! ( heartbeat instanceof Instant heartbeatAt ) ) {
			return true;
		}
		return !heartbeatAt.plusSeconds( this.config.heartbeatSeconds * 3 ).isAfter( now );
	}

	/**
	 * Rewrite the shared member list. The list is multi-writer, so it gets the same
	 * write-settle-verify treatment as a task claim.
	 *
	 * @param peerIds The peer ids to record
	 * @param replace When true the list becomes exactly these peers plus this node (used by a refresh,
	 *                which has just established who is alive); when false the entries already present
	 *                are preserved (used at registration, which must not evict anyone)
	 */
	private void writeMemberList( java.util.List<String> peerIds, boolean replace ) {
		ICacheProvider resolved = resolveCache( false );
		if ( resolved == null ) {
			return;
		}

		java.util.LinkedHashSet<String> desired = new java.util.LinkedHashSet<>();

		if ( !replace ) {
			Object existing = resolved.get( nodesKey() ).orElse( null );
			if ( existing != null ) {
				for ( String entry : existing.toString().split( "\\R" ) ) {
					String trimmed = entry.trim();
					if ( !trimmed.isEmpty() ) {
						desired.add( trimmed );
					}
				}
			}
		}

		desired.addAll( peerIds );
		desired.add( this.nodeId );

		String payload = String.join( System.lineSeparator(), desired );

		try {
			for ( int attempt = 1; attempt <= MEMBER_LIST_WRITE_ATTEMPTS; attempt++ ) {
				resolved.set( nodesKey(), payload, nodeTtlSeconds() );
				if ( MEMBER_LIST_SETTLE_MILLIS > 0 ) {
					Thread.sleep( MEMBER_LIST_SETTLE_MILLIS );
				}
				Object confirmed = resolved.get( nodesKey() ).orElse( null );
				if ( confirmed != null && confirmed.toString().contains( this.nodeId ) ) {
					return;
				}
			}
			this.logger.warn( "+ Cluster could not confirm membership of node [{}] in the member list", this.nodeId );
		} catch ( InterruptedException e ) {
			Thread.currentThread().interrupt();
		} catch ( Throwable t ) {
			this.logger.error( "+ Cluster failed to write the member list: {}", t.getMessage() );
		}
	}

	/**
	 * --------------------------------------------------------------------------
	 * Convenience
	 * --------------------------------------------------------------------------
	 */

	/**
	 * A random delay within the given bound, plus a little jitter. Used by consumers that need the
	 * same shaping the scheduler uses.
	 *
	 * @param boundMillis The upper bound
	 *
	 * @return A random value in [0, boundMillis)
	 */
	public static long randomDelay( long boundMillis ) {
		return boundMillis <= 0 ? 0 : ThreadLocalRandom.current().nextLong( boundMillis );
	}

	/**
	 * Drop any cached snapshot. Visible for tests.
	 */
	public void invalidateSnapshot() {
		this.snapshot	= null;
		this.snapshotAt	= 0;
	}

	/**
	 * Register this node in the shared member list. Called at startup so peers can discover us
	 * without waiting for their next refresh.
	 */
	public void registerInMemberList() {
		writeMemberList( java.util.List.of(), false );
	}

	/**
	 * All node records currently held in memory, for diagnostics. Never triggers a cache read.
	 *
	 * @return The last snapshot, or an empty struct
	 */
	public IStruct peekSnapshot() {
		IStruct current = this.snapshot;
		return current == null ? new Struct() : current;
	}

	/**
	 * Convenience for tests and diagnostics: the {@code attributes} struct of a record.
	 *
	 * @param record The record
	 *
	 * @return The attributes struct, or an empty struct
	 */
	public static IStruct attributesOf( IStruct record ) {
		Object attributes = record == null ? null : record.get( Key.attributes );
		return attributes instanceof IStruct struct ? struct : new Struct();
	}

	/**
	 * Build the key for a scheduler task record. Lives here so both subsystems agree on the shape:
	 * the scheduler owns the segment, but the root and cluster name come from here.
	 *
	 * @param application The application name, or null/blank for a global task
	 * @param scheduler   The scheduler name
	 * @param task        The task name
	 *
	 * @return The full cache key
	 */
	public String buildTaskKey( String application, String scheduler, String task ) {
		String scope = ( application == null || application.isBlank() )
		    ? GLOBAL_SCOPE
		    : APPLICATION_SCOPE + ":" + application;

		return getTaskKeyPrefix()
		    + scope + ":"
		    + Key.scheduler.getName() + ":" + scheduler + ":"
		    + Key.task.getName() + ":" + task;
	}

	/**
	 * A diagnostic view of every record this service owns.
	 *
	 * @return A struct of keys to records
	 */
	public IStruct getClusterReport() {
		IStruct report = new Struct();
		report.put( Key.nodeId, this.nodeId );
		report.put( Key._name, this.config.name );
		report.put( Key.cacheName, this.config.cacheName );
		report.put( Key.mode, TaskClusterClaim.modeName() );
		report.put( Key.attributes, this.localAttributes );
		report.put( Key.nodes, getNodes() );
		return report;
	}

}
