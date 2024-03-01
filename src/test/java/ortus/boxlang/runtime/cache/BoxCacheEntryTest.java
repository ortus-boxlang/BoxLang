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

import static com.google.common.truth.Truth.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Struct;

public class BoxCacheEntryTest {

	BoxCacheEntry cacheEntry;

	@BeforeEach
	void setUp() {
		// Create a test fixture
		cacheEntry = new BoxCacheEntry(
		    Key.of( "default" ),
		    60,
		    0,
		    Key.of( "myCacheKey" ),
		    "myValue",
		    new Struct()
		);
	}

	@Test
	@DisplayName( "When creating a BoxCacheEntry, it should have initial properties set" )
	void testBoxCacheEntryInitialization() {
		Key		cacheName	= cacheEntry.cacheName();
		Instant	created		= cacheEntry.created();
		long	initialHits	= cacheEntry.hits();

		// Then
		assertThat( cacheName ).isEqualTo( Key.of( "default" ) );
		assertThat( cacheEntry.key() ).isEqualTo( Key.of( "myCacheKey" ) );
		assertThat( cacheEntry.value().get() ).isEqualTo( "myValue" );
		assertThat( cacheEntry.metadata() ).isNotNull();
		assertThat( cacheEntry.timeout() ).isEqualTo( 60 );
		assertThat( cacheEntry.lastAccessTimeout() ).isEqualTo( 0 );
		assertThat( cacheEntry.isExpired() ).isFalse();
		assertThat( created ).isNotNull();
		assertThat( initialHits ).isEqualTo( 0L );
	}

	@Test
	@DisplayName( "When updating BoxCacheEntry properties, they should reflect the changes" )
	void testBoxCacheEntryPropertiesUpdate() throws InterruptedException {
		// Given
		Instant newLastAccessed = Instant.now();

		// When
		cacheEntry.touchLastAccessed();
		cacheEntry.setValue( newLastAccessed );
		cacheEntry.incrementHits();
		Instant originalCreated = cacheEntry.created();

		Thread.sleep( 1000 ); // Sleep for 1 second to ensure the created time is different

		cacheEntry.resetCreated();

		// Then
		assertThat( cacheEntry.value().get() ).isEqualTo( newLastAccessed );
		assertThat( cacheEntry.lastAccessed() ).isEqualTo( newLastAccessed );
		assertThat( cacheEntry.hits() ).isEqualTo( 1L );
		assertThat( cacheEntry.created() ).isNotEqualTo( originalCreated );
	}

	// Add more test cases as needed

	// Example of additional test case:
	@Test
	@DisplayName( "When marking BoxCacheEntry as expired, it should be reflected" )
	void testBoxCacheEntryExpiration() {
		assertThat( cacheEntry.isExpired() ).isFalse();

		// When
		cacheEntry.expire();

		// Then
		assertThat( cacheEntry.isExpired() ).isTrue();
	}

	@Test
	@DisplayName( "It can serialize the BoxCacheEntry to binary" )
	void testBoxCacheEntrySerialization() {

		byte[] cacheEntryBytes = null;

		// Serialize the BoxCacheEntry
		try ( ByteArrayOutputStream bos = new ByteArrayOutputStream();
		    ObjectOutputStream oos = new ObjectOutputStream( bos ) ) {
			oos.writeObject( cacheEntry );
			oos.flush();
			cacheEntryBytes = bos.toByteArray();
		} catch ( IOException e ) {
			e.printStackTrace();
		}

		// Then
		assertThat( cacheEntryBytes ).isNotNull();

		// Deserialize the cacheEntryBytes
		BoxCacheEntry inflatedEntry = null;
		try ( ByteArrayInputStream bis = new ByteArrayInputStream( cacheEntryBytes );
		    ObjectInputStream ois = new ObjectInputStream( bis ) ) {
			inflatedEntry = ( BoxCacheEntry ) ois.readObject();
		} catch ( IOException | ClassNotFoundException e ) {
			e.printStackTrace();
		}

		assertThat( inflatedEntry.cacheName() ).isEqualTo( Key.of( "default" ) );
		assertThat( inflatedEntry.key() ).isEqualTo( Key.of( "myCacheKey" ) );
		assertThat( inflatedEntry.value().get() ).isEqualTo( "myValue" );
		assertThat( inflatedEntry.metadata() ).isNotNull();
		assertThat( inflatedEntry.timeout() ).isEqualTo( 60 );
		assertThat( inflatedEntry.lastAccessTimeout() ).isEqualTo( 0 );
		assertThat( inflatedEntry.isExpired() ).isFalse();
		assertThat( inflatedEntry.created() ).isNotNull();
		assertThat( inflatedEntry.hits() ).isEqualTo( 0L );

	}
}
