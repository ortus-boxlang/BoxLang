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
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Struct;

/**
 * The base implementation of a cache entry in BoxLang.
 * This entry can be stored inside the cache provider if needed.
 * You can also inherit from this class to create your own cache entry implementation.
 */
public class BoxCacheEntry implements ICacheEntry {

	// Add a serialVersionUID to avoid warning
	private static final long	serialVersionUID	= 1L;

	/**
	 * --------------------------------------------------------------------------
	 * Private Properties
	 * --------------------------------------------------------------------------
	 */

	protected Key				cacheName;
	protected AtomicLong		hits				= new AtomicLong( 0 );
	protected long				timeout;
	protected long				lastAccessTimeout;
	protected Instant			created				= Instant.now();
	protected Instant			lastAccessed;
	protected Key				key;
	protected Object			value;
	protected Struct			metadata			= new Struct();
	protected AtomicBoolean		isExpired			= new AtomicBoolean( false );

	/**
	 * Constructor with metadata
	 *
	 * @param cacheName         The name of the cache associated with this entry
	 * @param timeout           The timeout in seconds
	 * @param lastAccessTimeout The last access timeout in seconds
	 * @param created           The creation date
	 * @param key               The key
	 * @param value             The value
	 * @param metadata          The metadata
	 */
	protected BoxCacheEntry(
	    Key cacheName,
	    long timeout,
	    long lastAccessTimeout,
	    Instant created,
	    Key key,
	    Object value,
	    Struct metadata ) {
		// Prep the entry
		this.cacheName			= cacheName;
		this.timeout			= timeout;
		this.lastAccessTimeout	= lastAccessTimeout;
		this.created			= created;
		this.key				= key;
		this.value				= value;
		this.metadata			= metadata;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Helper Methods
	 * --------------------------------------------------------------------------
	 */

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
	 * Set the last accessed date
	 */
	public ICacheEntry setLastAccessed( Instant lastAccessed ) {
		this.lastAccessed = lastAccessed;
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
	 * Mark the entry as expired
	 */
	public ICacheEntry expire() {
		this.isExpired.set( true );
		return this;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Interface Methods
	 * --------------------------------------------------------------------------
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
	public boolean isExpired() {
		return this.isExpired.get();
	}

	@Override
	public Key key() {
		return this.key;
	}

	@Override
	public Optional<?> value() {
		return Optional.ofNullable( this.value );
	}

	@Override
	public Struct metadata() {
		return this.metadata;
	}

}
