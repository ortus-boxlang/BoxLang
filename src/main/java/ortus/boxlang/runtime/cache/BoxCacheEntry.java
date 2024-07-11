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

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

import ortus.boxlang.runtime.dynamic.Attempt;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;

/**
 * The base implementation of a cache entry in BoxLang.
 * This entry can be stored inside the cache provider if needed.
 * You can also inherit from this class to create your own cache entry implementation.
 */
public class BoxCacheEntry implements ICacheEntry {

	/**
	 * Empty Cache Entry
	 */
	public static final ICacheEntry	EMPTY				= new BoxCacheEntry(
	    Key._EMPTY,
	    0,
	    0,
	    Key._EMPTY,
	    null,
	    new Struct()
	);

	/**
	 * --------------------------------------------------------------------------
	 * Private Properties
	 * --------------------------------------------------------------------------
	 */

	// Add a serialVersionUID to avoid warning
	private static final long		serialVersionUID	= 1L;
	private Key						cacheName;
	private AtomicLong				hits				= new AtomicLong( 0 );
	private long					timeout;
	private long					lastAccessTimeout;
	private Instant					created				= Instant.now();
	private Instant					lastAccessed		= Instant.now();
	private Key						key;
	private Object					value;
	private IStruct					metadata			= new Struct();
	// Calculated hashcode
	private int						hashCode;

	/**
	 * Constructor with metadata
	 *
	 * @param cacheName         The name of the cache associated with this entry
	 * @param timeout           The timeout in seconds
	 * @param lastAccessTimeout The last access timeout in seconds
	 * @param key               The key
	 * @param value             The value
	 * @param metadata          The metadata
	 */
	public BoxCacheEntry(
	    Key cacheName,
	    long timeout,
	    long lastAccessTimeout,
	    Key key,
	    Object value,
	    IStruct metadata ) {
		// Prep the entry
		this.cacheName			= cacheName;
		this.timeout			= timeout;
		this.lastAccessTimeout	= lastAccessTimeout;
		this.key				= key;
		this.value				= value;
		this.metadata			= metadata;

		// Build out the hash code which doesn't use the object value since it's a cache entry
		final int prime = 31;
		this.hashCode	= 1;
		this.hashCode	= prime * this.hashCode + key.hashCode();
		this.hashCode	= prime * this.hashCode + cacheName.hashCode();
		this.hashCode	= prime * this.hashCode + Long.hashCode( timeout );
		this.hashCode	= prime * this.hashCode + Long.hashCode( lastAccessTimeout );
	}

	/**
	 * --------------------------------------------------------------------------
	 * Helper Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Build out a hashcode for this entry based
	 * on the key, timeout, lastAccessTimeout, and cacheName
	 */
	@Override
	public int hashCode() {
		return this.hashCode;
	}

	/**
	 * Check if the entry is equal to another entry
	 * Remember this doesn't account for object values
	 */
	@Override
	public boolean equals( Object obj ) {
		if ( obj == null ) {
			return false;
		}
		if ( this == obj ) {
			return true;
		}
		if ( getClass() != obj.getClass() ) {
			return false;
		}
		BoxCacheEntry other = ( BoxCacheEntry ) obj;
		return other.hashCode() == this.hashCode();
	}

	/**
	 * Set the value of the cache entry
	 */
	public ICacheEntry setValue( Object value ) {
		this.value = value;
		return this;
	}

	/**
	 * Set the metadata of the cache entry
	 */
	public ICacheEntry setMetadata( Struct metadata ) {
		this.metadata = metadata;
		return this;
	}

	/**
	 * Reset the last accessed date
	 */
	public ICacheEntry touchLastAccessed() {
		this.lastAccessed = Instant.now();
		return this;
	}

	/**
	 * Reset the created date
	 */
	public ICacheEntry resetCreated() {
		this.created = Instant.now();
		return this;
	}

	/**
	 * Increment the hits
	 */
	public ICacheEntry incrementHits() {
		this.hits.incrementAndGet();
		return this;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Interface Methods
	 * --------------------------------------------------------------------------
	 * Mostly Getters
	 */

	@Override
	public Key cacheName() {
		return this.cacheName;
	}

	@Override
	public long hits() {
		return this.hits.get();
	}

	@Override
	public long timeout() {
		return this.timeout;
	}

	@Override
	public long lastAccessTimeout() {
		return this.lastAccessTimeout;
	}

	@Override
	public Instant created() {
		return this.created;
	}

	@Override
	public Instant lastAccessed() {
		return this.lastAccessed;
	}

	@Override
	public boolean isEternal() {
		return this.timeout == 0;
	}

	@Override
	public Key key() {
		return this.key;
	}

	@Override
	public Attempt<Object> value() {
		return Attempt.of( this.value );
	}

	@Override
	public Object rawValue() {
		return this.value;
	}

	@Override
	public IStruct metadata() {
		return this.metadata;
	}

	@Override
	public IStruct toStruct() {
		return Struct.of(
		    "cacheName", this.cacheName,
		    "hits", this.hits.get(),
		    "timeout", this.timeout,
		    "lastAccessTimeout", this.lastAccessTimeout,
		    "created", this.created,
		    "lastAccessed", this.lastAccessed,
		    "key", this.key,
		    "metadata", this.metadata,
		    "isEternal", this.isEternal()
		);
	}

	/**
	 * Get the memento of the cache entry
	 *
	 * @return The memento
	 */
	public IStruct getMemento() {
		var results = toStruct();
		results.put( "value", value );
		return results;
	}

}
