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
import java.util.Optional;

import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;

/**
 * Every cache provider in BoxLang must adhere to this interface.
 * Every provider can have its own implementation of the cache entry or
 * use the default one provided by BoxLang.
 *
 * The Cache Entry provides uniformity when storing and retrieving cache entries.
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
	 * The timeout of this cache entry in seconds
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
	 * @return The optional that represents the value
	 */
	public Optional<Object> value();

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
	 * Set the value of the cache entry
	 */
	public ICacheEntry setValue( Object value );

	/**
	 * Set the metadata of the cache entry
	 *
	 * @param metadata The metadata
	 */
	public ICacheEntry setMetadata( Struct metadata );

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

}
