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
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import ortus.boxlang.runtime.dynamic.Attempt;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.util.conversion.ObjectMarshaller;

/**
 * BoxCacheEntry represents a single entry in the BoxLang cache system.
 *
 * This class implements the ICacheEntry interface and provides a complete cache entry
 * implementation with support for timeouts, hit counting, metadata, and expiration logic.
 *
 * Key features:
 * - Thread-safe hit counting using AtomicLong
 * - Configurable timeout and last access timeout
 * - Metadata support for additional entry information
 * - Automatic expiration based on creation time and last access time
 * - Immutable entry properties (except for hits, created, and lastAccessed timestamps)
 * - Efficient hash code calculation excluding the cached value
 *
 * The entry supports two types of timeouts:
 * 1. Absolute timeout: Entry expires after a fixed time from creation
 * 2. Idle timeout (lastAccessTimeout): Entry expires if not accessed within the specified time
 *
 * Setting timeout to 0 makes the entry eternal (never expires by time).
 *
 * Thread Safety:
 * This class is thread-safe for concurrent access. The hits counter, creation time,
 * and last accessed time are managed using atomic operations.
 *
 * @author Ortus Solutions, Corp
 *
 * @since 1.0.0
 */
public class BoxCacheEntry implements ICacheEntry {

	/**
	 * Empty Cache Entry
	 */
	public static final ICacheEntry		EMPTY				= new BoxCacheEntry(
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

	/**
	 * Serial Version UID for serialization
	 */
	private static final long			serialVersionUID	= 1L;

	/**
	 * The name of the cache this entry belongs to
	 */
	private final Key					cacheName;

	/**
	 * The number of hits this entry has received
	 */
	private final AtomicLong			hits				= new AtomicLong( 0 );

	/**
	 * The timeout in seconds for this entry
	 * If 0, it is eternal and never expires
	 */
	private final long					timeout;

	/**
	 * The last access timeout in seconds for this entry
	 * If 0, then it's not checked for last access timeout
	 */
	private final long					lastAccessTimeout;

	/**
	 * The time this entry was created
	 */
	private AtomicReference<Instant>	created				= new AtomicReference<>( Instant.now() );

	/**
	 * The last time this entry was accessed
	 * This is used to determine if the entry is expired based on the last access timeout
	 */
	private AtomicReference<Instant>	lastAccessed		= new AtomicReference<>( Instant.now() );

	/**
	 * The key of the entry
	 */
	private final Key					key;

	/**
	 * The value of the entry
	 * This can be any object, but it is not serialized in the hash code
	 */
	private final Object				value;

	/**
	 * The metadata associated with this entry
	 * This is a struct that can hold additional information about the entry
	 */
	private final IStruct				metadata;

	/**
	 * The hash code for this entry
	 * This is built based on the key, timeout, lastAccessTimeout, and cacheName
	 * It does not include the value since it's a cache entry
	 */
	private final int					hashCode;

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
		this.cacheName			= Objects.requireNonNull( cacheName, "Cache name cannot be null" );
		this.key				= Objects.requireNonNull( key, "Key cannot be null" );
		this.timeout			= timeout;
		this.lastAccessTimeout	= lastAccessTimeout;
		this.value				= value;
		this.metadata			= metadata == null ? new Struct() : metadata;

		// Check that timeouts cannot be negative
		if ( timeout < 0 ) {
			throw new BoxRuntimeException( "Timeout cannot be negative" );
		}
		if ( lastAccessTimeout < 0 ) {
			throw new BoxRuntimeException( "Last access timeout cannot be negative" );
		}

		// Build out the hash code which doesn't use the object value since it's a cache entry
		final int	prime				= 31;
		int			calculatedHashCode	= 1;
		calculatedHashCode	= prime * calculatedHashCode + key.hashCode();
		calculatedHashCode	= prime * calculatedHashCode + cacheName.hashCode();
		calculatedHashCode	= prime * calculatedHashCode + Long.hashCode( timeout );
		calculatedHashCode	= prime * calculatedHashCode + Long.hashCode( lastAccessTimeout );
		this.hashCode		= calculatedHashCode;
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
		if ( this == obj )
			return true;
		if ( obj == null || getClass() != obj.getClass() )
			return false;

		BoxCacheEntry other = ( BoxCacheEntry ) obj;
		return this.key.equals( other.key )
		    && this.cacheName.equals( other.cacheName )
		    && this.timeout == other.timeout
		    && this.lastAccessTimeout == other.lastAccessTimeout;
	}

	/**
	 * Reset the last accessed date
	 */
	public ICacheEntry touchLastAccessed() {
		this.lastAccessed.set( Instant.now() );
		return this;
	}

	/**
	 * Reset the created date
	 */
	public ICacheEntry resetCreated() {
		this.created.set( Instant.now() );
		this.lastAccessed.set( Instant.now() );
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
	 * Reset the hit count
	 **/
	public ICacheEntry setHits( long hits ) {
		this.hits.set( hits );
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
		return this.created.get();
	}

	@Override
	public Instant lastAccessed() {
		return this.lastAccessed.get();
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
		    "created", this.created.get(),
		    "lastAccessed", this.lastAccessed.get(),
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

	/**
	 * Verifies if the cache entry is expired.
	 * The rules are as follows:
	 * - If the timeout is 0, it is eternal and never expires.
	 * - If the last access timeout is set and the last accessed time plus that timeout is before now, it is expired.
	 * - If the created time plus the timeout is before now, it is expired.
	 */
	public boolean isExpired() {

		// If the timeout is 0, it's eternal
		if ( this.isEternal() ) {
			return false;
		}

		var now = Instant.now();

		// Verify first if the last access timeout is expired
		if ( this.lastAccessTimeout > 0 && lastAccessed().plusSeconds( this.lastAccessTimeout ).isBefore( now ) ) {
			return true;
		}

		// Verify if the timeout is expired
		return created().plusSeconds( this.timeout ).isBefore( now );
	}

	/**
	 * Get the size in bytes of this cache entry.
	 *
	 * @return The size in bytes
	 */
	public long sizeInBytes() {
		return ObjectMarshaller.estimateSerializedSize( this.getMemento() );
	}

}
