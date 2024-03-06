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
package ortus.boxlang.runtime.cache.store;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ortus.boxlang.runtime.cache.ICacheEntry;
import ortus.boxlang.runtime.cache.filters.ICacheKeyFilter;
import ortus.boxlang.runtime.cache.providers.ICacheProvider;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;

/**
 * This object store keeps all objects in heap using Concurrent classes.
 * Naturally the store is ordered by {@code created} timestamp and can be used for concurrent access.
 */
public class ConcurrentSoftReferenceStore extends ConcurrentStore implements IObjectStore {

	/**
	 * Logger
	 */
	private static final Logger										logger	= LoggerFactory.getLogger( ConcurrentSoftReferenceStore.class );

	/**
	 * The concurrent pool of objects based on a soft reference
	 */
	protected ConcurrentHashMap<Key, SoftReference<ICacheEntry>>	pool;

	/**
	 * Reverse lookup map for soft references
	 */
	private ConcurrentHashMap<Integer, Key>							softRefKeyMap;

	/**
	 * Reference queue for soft references
	 */
	private ReferenceQueue<ICacheEntry>								referenceQueue;

	/**
	 * Constructor
	 */
	public ConcurrentSoftReferenceStore() {
		// Empty constructor
	}

	/**
	 * Some storages require a method to initialize the storage or do
	 * object loading. This method is called when the cache provider is started.
	 *
	 * @param provider The cache provider associated with this store
	 * @param config   The configuration for the store
	 */
	@Override
	public IObjectStore init( ICacheProvider provider, IStruct config ) {
		this.provider		= provider;
		this.config			= config;
		this.pool			= new ConcurrentHashMap<>( config.getAsInteger( Key.maxObjects ) / 4 );
		this.softRefKeyMap	= new ConcurrentHashMap<>( config.getAsInteger( Key.maxObjects ) / 4 );
		this.referenceQueue	= new ReferenceQueue<>();

		logger.atDebug().log(
		    "ConcurrentSoftReferenceStore({}) initialized with a max size of {}",
		    provider.getName(),
		    config.getAsInteger( Key.maxObjects )
		);

		return this;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Interface Methods
	 * --------------------------------------------------------------------------
	 */

	@Override
	public void clearAll() {
		super.clearAll();
		this.softRefKeyMap.clear();
	}

	@Override
	public void clearAll( ICacheKeyFilter filter ) {
		super.clearAll( filter );
		this.softRefKeyMap.values().removeIf( filter );
	}

	@Override
	public boolean clear( Key key ) {
		// Remove the soft reference from the pool
		SoftReference<ICacheEntry> reference = this.pool.remove( key );
		if ( reference != null ) {
			// Remove the soft reference from the reverse lookup map
			this.softRefKeyMap.remove( reference.hashCode() );
			return true;
		}
		return false;
	}

	@Override
	public void evict() {
		super.evict();
		// Evict all garbage collected soft references
		evictSoftReferences();
	}

	@Override
	public boolean lookup( Key key ) {
		var entry = this.pool.get( key );

		if ( entry != null ) {
			ICacheEntry cacheEntry = entry.get();
			// If the entry is null, it was collected by the GC
			if ( cacheEntry == null ) {
				clear( key );
				getProvider().getStats().recordGCHit();
				return false;
			}
			return true;
		}

		return false;
	}

	@Override
	public ICacheEntry getQuiet( Key key ) {
		var reference = this.pool.get( key );

		if ( reference != null ) {
			ICacheEntry cacheEntry = reference.get();
			// If the entry is null, it was collected by the GC
			if ( cacheEntry == null ) {
				clear( key );
				getProvider().getStats().recordGCHit();
			} else {
				return cacheEntry;
			}
		}

		return null;
	}

	@Override
	public void set( Key key, ICacheEntry entry ) {
		// Create Soft Reference Wrapper and register with Queue
		SoftReference<ICacheEntry> softReference = createSoftReference( key, entry );
		// Store the soft reference in the pool
		this.pool.put( key, softReference );
	}

	/**
	 * --------------------------------------------------------------------------
	 * Helper Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Evict soft references from the store that have been collected
	 */
	@SuppressWarnings( "unchecked" )
	void evictSoftReferences() {
		SoftReference<ICacheEntry> collected;
		while ( ( collected = ( SoftReference<ICacheEntry> ) this.referenceQueue.poll() ) != null ) {
			if ( verifySoftReference( collected ) ) {
				clear( getSoftReferenceKey( collected ) );
				this.softRefKeyMap.remove( collected.hashCode() );
				getProvider().getStats().recordGCHit();
			}
		}
	}

	/**
	 * --------------------------------------------------------------------------
	 * Private Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Create a soft reference for an incoming entry
	 *
	 * @param key   The key to store the object under
	 * @param entry The cache entry to store
	 *
	 * @return The soft reference created
	 */
	private SoftReference<ICacheEntry> createSoftReference( Key key, ICacheEntry entry ) {
		// Create Soft Reference Wrapper and register with Queue
		SoftReference<ICacheEntry> softReference = new SoftReference<>( entry, this.referenceQueue );
		// Create a reverse lookup map for the soft reference key
		this.softRefKeyMap.put( softReference.hashCode(), key );
		return softReference;
	}

	/**
	 * Verify if the soft reference is in the key map
	 *
	 * @param softReference The soft reference to verify
	 *
	 * @return True if the soft reference is in the key map via the hash code
	 */
	private boolean verifySoftReference( SoftReference<ICacheEntry> softReference ) {
		return this.softRefKeyMap.containsKey( softReference.hashCode() );
	}

	/**
	 * Get the soft reference key from the key map
	 *
	 * @param softReference The soft reference to get the key for
	 *
	 * @return The key for the soft reference
	 */
	private Key getSoftReferenceKey( SoftReference<ICacheEntry> softReference ) {
		return this.softRefKeyMap.get( softReference.hashCode() );
	}

}
