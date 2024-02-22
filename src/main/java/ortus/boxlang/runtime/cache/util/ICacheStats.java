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

import ortus.boxlang.runtime.types.IStruct;

/**
 * Every cache provider in BoxLang must adhere to this interface in
 * order to provide statistics about the cache it manages.
 *
 * If your cache doesn't implement a method, then make sure it returns
 * a default value.
 */
public interface ICacheStats {

	/**
	 * Get the cache's hit rate = hits / (hits + misses)
	 *
	 * @return The hit ratio
	 */
	public int hitRate();

	/**
	 * How many entries are in the cache. This should take a live snapshot.
	 *
	 * @return The live object count regardless of expiration
	 */
	public int objectCount();

	/**
	 * How many entries are expired in the cache. This should take a live snapshot.
	 *
	 * @return The expired count
	 */
	public int expiredCount();

	/**
	 * Reset the cache's statistics
	 *
	 * @return The stats object
	 */
	public ICacheStats reset();

	/**
	 * Get the total cache's garbage collections of Soft/Weak references
	 *
	 * @return The garbage collections
	 */
	public long garbageCollections();

	/**
	 * Get the total cache's evictions due to cache constraints (e.g. size, memory, etc)
	 *
	 * @return The eviction count
	 */
	public long evictionCount();

	/**
	 * Get the total cache's hits
	 *
	 * @return The hits
	 */
	public long hits();

	/**
	 * Get the total cache's misses
	 *
	 * @return The misses
	 */
	public long misses();

	/**
	 * Get the date/time of the last reap the cache did
	 *
	 * @return date/time or null if never reaped
	 */
	public Instant lastReapDatetime();

	/**
	 * How many times the cache has been reaped
	 *
	 * @return The reap count
	 */
	public long reapCount();

	/**
	 * When the cache was started
	 */
	public Instant started();

	/**
	 * Get the total cache's size in bytes
	 *
	 * @return The size in bytes
	 */
	public long size();

	/**
	 * Get a Struct representation of the cache's statistics
	 */
	public IStruct toStruct();

}
