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
package ortus.boxlang.runtime.cache;

import java.io.Serializable;
import java.time.Instant;

import ortus.boxlang.runtime.dynamic.Attempt;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;

/**
 * Interface that defines a cache entry contract for BoxLang cache implementations and its object storages (@see IObjectStore)
 *
 * This interface serves as the primary communication mechanism between BoxCaches and their
 * underlying object pools. Each cache entry represents a cached object with associated
 * metadata including creation time, access patterns, expiration information, and custom
 * metadata.
 *
 * Cache entries maintain state information such as hit counts, timestamps, and timeout
 * configurations to support various caching strategies including LRU, time-based expiration,
 * and access-based eviction policies.
 *
 * Implementations of this interface should be thread-safe as cache entries may be accessed
 * concurrently by multiple threads in a multi-threaded environment.
 *
 * @since 1.0.0
 */
public interface ICacheEntry extends Serializable {

	/**
	 * The name of the cache this entry belongs to
	 *
	 * @return The cache name
	 */
	public Key cacheName();

	/**
	 * The number of hits this cache entry has had
	 *
	 * @return The number of hits
	 */
	public long hits();

	/**
	 * The timeout of this cache entry in seconds since creation.
	 *
	 * @return The timeout in seconds
	 */
	public long timeout();

	/**
	 * The last access timeout of this cache entry in seconds
	 *
	 * @return The last access timeout in seconds
	 */
	public long lastAccessTimeout();

	/**
	 * When this cache entry was created
	 *
	 * @return The creation timestamp
	 */
	public Instant created();

	/**
	 * When this cache entry was last accessed
	 *
	 * @return The last access timestamp
	 */
	public Instant lastAccessed();

	/**
	 * Is this an eternal object
	 *
	 * @return True if the object is eternal, false otherwise
	 */
	public boolean isEternal();

	/**
	 * The key associated with this entry
	 *
	 * @return The key
	 */
	public Key key();

	/**
	 * The value associated with this entry, if any.
	 *
	 * @return The attempt that represents the value from the cache
	 */
	public Attempt<Object> value();

	/**
	 * The raw value of the cache entry
	 *
	 * @return The raw value or null
	 */
	public Object rawValue();

	/**
	 * An entry can store custom information to the entry
	 *
	 * @return A struct with the metadata
	 */
	public IStruct metadata();

	/**
	 * Resets the last accessed date
	 */
	public ICacheEntry touchLastAccessed();

	/**
	 * Reset the created date
	 */
	public ICacheEntry resetCreated();

	/**
	 * Increment the hits
	 */
	public ICacheEntry incrementHits();

	/**
	 * Get the state of the entry as a struct
	 */
	public IStruct toStruct();

	/**
	 * Verifies if this cache entry has expired
	 */
	public default boolean isExpired() {
		return false;
	}

	/**
	 * If available, it will get the size of the cache entry in bytes.
	 * This is not required, but if the cache provider can provide this information,
	 * it will be used to calculate the size of the cache.
	 */
	public default long sizeInBytes() {
		return 0;
	}

}
