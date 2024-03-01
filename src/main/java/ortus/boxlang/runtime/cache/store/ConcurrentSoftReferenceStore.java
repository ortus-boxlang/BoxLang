// /**
// * [BoxLang]
// *
// * Copyright [2023] [Ortus Solutions, Corp]
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// * http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
// package ortus.boxlang.runtime.cache.store;

// import java.lang.ref.ReferenceQueue;
// import java.lang.ref.SoftReference;
// import java.util.Map;
// import java.util.concurrent.ConcurrentHashMap;

// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;

// import ortus.boxlang.runtime.cache.ICacheEntry;
// import ortus.boxlang.runtime.cache.filters.ICacheKeyFilter;
// import ortus.boxlang.runtime.cache.providers.ICacheProvider;
// import ortus.boxlang.runtime.scopes.Key;
// import ortus.boxlang.runtime.types.IStruct;
// import ortus.boxlang.runtime.types.Struct;

// /**
// * This object store keeps all objects in heap using Concurrent classes.
// * Naturally the store is ordered by {@code created} timestamp and can be used for concurrent access.
// */
// public class ConcurrentSoftReferenceStore extends ConcurrentStore implements IObjectStore {

// /**
// * Logger
// */
// private static final Logger logger = LoggerFactory.getLogger( ConcurrentSoftReferenceStore.class );

// /**
// * Reverse lookup map for soft references
// */
// private Map<Integer, Key> softRefKeyMap;

// /**
// * The concurrent pool of objects
// * The value can be a ICacheEntry, or a soft reference
// */
// private Map<Key, Object> pool;

// /**
// * Reference queue for soft references
// */
// private ReferenceQueue<ICacheEntry> referenceQueue;

// /**
// * Constructor
// */
// public ConcurrentSoftReferenceStore() {
// // Empty constructor
// }

// /**
// * Some storages require a method to initialize the storage or do
// * object loading. This method is called when the cache provider is started.
// *
// * @param provider The cache provider associated with this store
// * @param config The configuration for the store
// */
// @Override
// public void init( ICacheProvider provider, IStruct config ) {
// super.init( provider, config );
// this.pool = new ConcurrentHashMap<>( config.getAsInteger( Key.maxObjects ) / 4 );
// this.softRefKeyMap = new ConcurrentHashMap<>( config.getAsInteger( Key.maxObjects ) / 4 );
// this.referenceQueue = new ReferenceQueue<>();
// }

// /**
// * --------------------------------------------------------------------------
// * Interface Methods
// * --------------------------------------------------------------------------
// */

// /**
// * Reap the storage for expired objects by running the eviction policy and count.
// */
// @Override
// public void reap() {

// // this.pool.entrySet()
// // // Stream it
// // .parallelStream()
// // // Sort using the policy comparator
// // .sorted( Map.Entry.comparingByValue( getPolicy().getComparator() ) )
// // // Get only the non-expired entries or non-eternal entries
// // .filter( entry -> !entry.getValue().isExpired() && !entry.getValue().isEternal() )
// // // Check how many to expire according to the config count
// // .limit( this.config.getAsInteger( Key.evictCount ) )
// // // Evict it & Log Stats
// // .forEach( entry -> {
// // this.pool.remove( entry.getKey() );
// // getProvider().getStats().recordEviction();
// // } );

// }

// /**
// * Clear all the elements in the store
// */
// @Override
// public void clearAll() {
// this.pool.clear();
// this.softRefKeyMap.clear();
// this.referenceQueue = new ReferenceQueue<>();
// }

// /**
// * Clear all the elements in the store with a ${@link ICacheKeyFilter}.
// * This can be a lambda or method reference since it's a functional interface.
// *
// * @param filter The filter that determines which keys to clear
// */
// public void clearAll( ICacheKeyFilter filter ) {
// this.pool.keySet().removeIf( filter );
// // TODO: ref key map clear
// }

// /**
// * Clears an object from the storage
// *
// * @param key The object key to clear
// *
// * @return True if the object was cleared, false otherwise (if the object was not found in the store)
// */
// @Override
// public boolean clear( Key key ) {
// var softReference = this.pool.remove( key );
// if ( softReference != null ) {
// this.softRefKeyMap.remove( softReference.hashCode() );
// return true;
// }
// }

// /**
// * Check if an object is in the store and not expired
// *
// * @param key The key to lookup in the store
// *
// * @return True if the object is in the store, false otherwise
// */
// public boolean lookup( Key key ) {
// // Key is in the store and not expired
// return this.pool.computeIfPresent(
// key,
// ( k, cacheEntry ) -> cacheEntry.isExpired() ? null : cacheEntry
// ) != null;
// }

// /**
// * Check if multiple objects are in the store
// *
// * @param key A varargs of keys to lookup in the store
// *
// * @return A struct of keys and their lookup status
// */
// public IStruct lookup( Key... keys ) {
// IStruct results = new Struct();
// for ( Key key : keys ) {
// results.put( key, lookup( key ) );
// }
// return results;
// }

// /**
// * Check if multiple objects are in the store using a filter
// *
// * @param filter The filter that determines which keys to return
// *
// * @return A struct of keys and their lookup status
// */
// public IStruct lookup( ICacheKeyFilter filter ) {
// IStruct results = new Struct();
// this.pool.keySet()
// .parallelStream()
// .filter( filter )
// .forEach( key -> results.put( key, true ) );
// return results;
// }

// /**
// * Get an object from the store with metadata tracking
// *
// * @param key The key to retrieve
// *
// * @return The cache entry retrieved or null if not found
// */
// public ICacheEntry get( Key key ) {
// var results = this.pool.getOrDefault( key, null );

// if ( results != null ) {
// // Update Stats
// results
// .incrementHits()
// .touchLastAccessed();
// // Is resetTimeoutOnAccess enabled? If so, jump up the creation time to increase the timeout
// if ( this.config.getAsBoolean( Key.resetTimeoutOnAccess ) ) {
// results.resetCreated();
// }
// }

// return results;
// }

// /**
// * Get multiple objects from the store with metadata tracking
// *
// * @param key The keys to retrieve
// *
// * @return A struct of keys and their cache entries
// */
// public IStruct get( Key... keys ) {
// IStruct results = new Struct();
// for ( Key key : keys ) {
// results.put( key, get( key ) );
// }
// return results;
// }

// /**
// * Get multiple objects from the store with metadata tracking using a filter
// *
// * @param filter The filter that determines which keys to return
// *
// * @return A struct of keys and their cache entries
// */
// public IStruct get( ICacheKeyFilter filter ) {
// IStruct results = new Struct();
// this.pool.keySet()
// .parallelStream()
// .filter( filter )
// .forEach( key -> results.put( key, get( key ) ) );
// return results;
// }

// /**
// * Get an object from cache with no metadata tracking
// *
// * @param key The key to retrieve
// *
// * @return The cache entry retrieved or null if not found
// */
// @SuppressWarnings( "unchecked" )
// @Override
// public ICacheEntry getQuiet( Key key ) {
// var results = this.pool.getOrDefault( key, null );

// if ( results instanceof SoftReference ) {
// results = ( ( SoftReference<?> ) results ).get();
// }

// // We do null checks, since the soft reference could have been cleared
// return results == null ? null : ( ICacheEntry ) results;
// }

// /**
// * Expire an object from the store
// *
// * @param key The key to expire
// *
// * @return True if the object was expired, false otherwise (if the object was not found in the store)
// */
// public boolean expire( Key key ) {
// var results = getQuiet( key );
// if ( results != null ) {
// results.expire();
// return true;
// }
// return false;
// }

// /**
// * Expire check for an object in the store
// *
// * @param key The key to check
// *
// * @return True if the object is expired, false otherwise (could be not found in the store or not expired yet)
// */
// @Override
// public boolean isExpired( Key key ) {
// var results = getQuiet( key );
// return results == null ? false : results.isExpired();
// }

// /**
// * Sets an object in the storage
// *
// * @param key The key to store the object under
// * @param entry The cache entry to store
// */
// @Override
// public void set( Key key, ICacheEntry entry ) {
// // Check if the entry is eternal, else do a soft reference
// if ( entry.isEternal() ) {
// this.pool.put( key, entry );
// } else {
// this.pool.put( key, createSoftReference( key, entry ) );
// }
// }

// /**
// * --------------------------------------------------------------------------
// * Private Methods
// * --------------------------------------------------------------------------
// */

// /**
// * Create a soft reference for the entry
// *
// * @param key The key to store the object under
// * @param entry The cache entry to store
// *
// * @return The soft reference created
// */
// private SoftReference<ICacheEntry> createSoftReference( Key key, ICacheEntry entry ) {
// // Create Soft Reference Wrapper and register with Queue
// SoftReference<ICacheEntry> softReference = new SoftReference<>( entry, this.referenceQueue );
// // Create a reverse lookup map for the soft reference key
// this.softRefKeyMap.put( softReference.hashCode(), key );
// return softReference;
// }

// /**
// * Verify if the soft reference is in the key map
// *
// * @param softReference The soft reference to verify
// */
// private boolean verifySoftReference( SoftReference<ICacheEntry> softReference ) {
// return this.softRefKeyMap.containsKey( softReference.hashCode() );
// }

// /**
// * Get the soft reference key from the key map
// *
// * @param softReference The soft reference to get the key for
// *
// * @return The key for the soft reference
// */
// private Key getSoftReferenceKey( SoftReference<ICacheEntry> softReference ) {
// return this.softRefKeyMap.get( softReference.hashCode() );
// }

// }
