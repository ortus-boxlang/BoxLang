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
package ortus.boxlang.runtime.cache.util;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;

public class BoxCacheStats implements ICacheStats {

	/**
	 * --------------------------------------------------------------------------
	 * Private Properties
	 * --------------------------------------------------------------------------
	 */
	private AtomicLong	garbageCollections;
	private AtomicLong	evictionCount;
	private AtomicLong	hits;
	private AtomicLong	misses;
	private Instant		lastReapDatetime;
	private AtomicLong	reapCount;
	private Instant		started;
	private long		size;

	/**
	 * Constructor
	 */
	BoxCacheStats() {
		reset();
	}

	/**
	 * --------------------------------------------------------------------------
	 * Public Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Record an eviction hit
	 */
	public ICacheStats recordEviction() {
		this.evictionCount.incrementAndGet();
		return this;
	}

	/**
	 * Record a cache hit
	 */
	public ICacheStats recordHit() {
		this.hits.incrementAndGet();
		return this;
	}

	/**
	 * Record a cache miss
	 */
	public ICacheStats recordMiss() {
		this.misses.incrementAndGet();
		return this;
	}

	/**
	 * Record a cache garbage collection
	 */
	public ICacheStats recordGCHit() {
		this.garbageCollections.incrementAndGet();
		return this;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Interface Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Get the cache's hit rate = hits / (hits + misses)
	 *
	 * @return The hit ratio
	 */
	public int hitRate() {
		return ( int ) ( ( this.hits.get() / ( this.hits.get() + this.misses.get() ) ) * 100 );
	}

	/**
	 * How many entries are in the cache. This should take a live snapshot.
	 *
	 * @return The live object count regardless of expiration
	 */
	public int objectCount() {
		// todo: implement
		return 0;
	}

	/**
	 * How many entries are expired in the cache. This should take a live snapshot.
	 *
	 * @return The expired count
	 */
	public int expiredCount() {
		// todo: implement
		return 0;
	}

	/**
	 * Reset the cache's statistics
	 *
	 * @return The stats object
	 */
	public ICacheStats reset() {
		this.garbageCollections	= new AtomicLong( 0 );
		this.evictionCount		= new AtomicLong( 0 );
		this.hits				= new AtomicLong( 0 );
		this.misses				= new AtomicLong( 0 );
		this.lastReapDatetime	= Instant.now();
		this.reapCount			= new AtomicLong( 0 );
		this.started			= Instant.now();
		this.size				= 0;
		return this;
	}

	/**
	 * Get the total cache's garbage collections of Soft/Weak references
	 *
	 * @return The garbage collections
	 */
	public long garbageCollections() {
		return this.garbageCollections.get();
	}

	/**
	 * Get the total cache's evictions due to cache constraints (e.g. size, memory, etc)
	 *
	 * @return The eviction count
	 */
	public long evictionCount() {
		return this.evictionCount.get();
	}

	/**
	 * Get the total cache's hits
	 *
	 * @return The hits
	 */
	public long hits() {
		return this.hits.get();
	}

	/**
	 * Get the total cache's misses
	 *
	 * @return The misses
	 */
	public long misses() {
		return this.misses.get();
	}

	/**
	 * Get the date/time of the last reap the cache did
	 *
	 * @return date/time or null if never reaped
	 */
	public Instant lastReapDatetime() {
		return this.lastReapDatetime;
	}

	/**
	 * How many times the cache has been reaped
	 *
	 * @return The reap count
	 */
	public long reapCount() {
		return this.reapCount.get();
	}

	/**
	 * When the cache was started
	 */
	public Instant started() {
		return this.started;
	}

	/**
	 * Get the total cache's size in bytes
	 *
	 * @return The size in bytes
	 */
	public long size() {
		return this.size;
	}

	/**
	 * Get a Struct representation of the cache's statistics
	 */
	public IStruct toStruct() {
		return Struct.of(
		    "garbageCollections", this.garbageCollections.get(),
		    "evictionCount", this.evictionCount.get(),
		    "hits", this.hits.get(),
		    "misses", this.misses.get(),
		    "lastReapDatetime", this.lastReapDatetime,
		    "reapCount", this.reapCount.get(),
		    "started", this.started,
		    // Dynamic stats
		    "size", size(),
		    "objectCount", objectCount(),
		    "expiredCount", expiredCount()
		);
	}
}
