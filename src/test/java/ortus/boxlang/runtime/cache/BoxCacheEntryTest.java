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
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

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
		assertThat( created ).isNotNull();
		assertThat( initialHits ).isEqualTo( 0L );
	}

	@Test
	@DisplayName( "When updating BoxCacheEntry properties, they should reflect the changes" )
	void testBoxCacheEntryPropertiesUpdate() throws InterruptedException {
		cacheEntry.touchLastAccessed();
		cacheEntry.incrementHits();
		Instant originalCreated = cacheEntry.created();

		Thread.sleep( 1000 ); // Sleep for 1 second to ensure the created time is different

		cacheEntry.resetCreated();

		// Then
		// The following test is not consistent as there may be nanoseconds of difference between the two times
		// assertThat( cacheEntry.lastAccessed() ).isEqualTo( newLastAccessed );
		assertThat( cacheEntry.hits() ).isEqualTo( 1L );
		assertThat( cacheEntry.created() ).isNotEqualTo( originalCreated );
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
			throw new BoxRuntimeException( "Failed to deserialize BoxCacheEntry", e );
		}

		assertThat( inflatedEntry.cacheName() ).isEqualTo( Key.of( "default" ) );
		assertThat( inflatedEntry.key() ).isEqualTo( Key.of( "myCacheKey" ) );
		assertThat( inflatedEntry.value().get() ).isEqualTo( "myValue" );
		assertThat( inflatedEntry.metadata() ).isNotNull();
		assertThat( inflatedEntry.timeout() ).isEqualTo( 60 );
		assertThat( inflatedEntry.lastAccessTimeout() ).isEqualTo( 0 );
		assertThat( inflatedEntry.created() ).isNotNull();
		assertThat( inflatedEntry.hits() ).isEqualTo( 0L );

		inflatedEntry.setHits( 1l );
		assertThat( inflatedEntry.hits() ).isEqualTo( 1L );
	}

	@DisplayName( "It can never expire if it is eternal" )
	@Test
	void testIsEternal() {
		// Given
		BoxCacheEntry	eternalEntry	= new BoxCacheEntry(
		    Key.of( "default" ),
		    0, // Eternal
		    0,
		    Key.of( "eternalKey" ),
		    "eternalValue",
		    new Struct()
		);

		// When
		boolean			isEternal		= eternalEntry.isEternal();

		// Then
		assertThat( isEternal ).isTrue();
		assertThat( eternalEntry.isExpired() ).isFalse();
	}

	@DisplayName( "It can expire using a timeout" )
	@Test
	void testIsExpiredWithTimeout() {
		// Given
		BoxCacheEntry	expiredEntry	= new BoxCacheEntry(
		    Key.of( "default" ),
		    1, // 1 second timeout
		    0,
		    Key.of( "expiredKey" ),
		    "expiredValue",
		    new Struct()
		);

		// Spy on it
		Instant			testTime		= Instant.now().minusSeconds( 60 );
		BoxCacheEntry	spyEntry		= Mockito.spy( expiredEntry );
		when( spyEntry.created() ).thenReturn( testTime );

		// Then
		assertThat( spyEntry.created() ).isEqualTo( testTime );
		assertThat( spyEntry.key() ).isEqualTo( Key.of( "expiredKey" ) );
		assertThat( spyEntry.timeout() ).isEqualTo( 1 );
		System.out.println( "Created: " + spyEntry.created() );
		System.out.println( "Current: " + Instant.now() );
		assertThat( spyEntry.isExpired() ).isTrue();
	}

	@DisplayName( "It can expire using a last access timeout" )
	@Test
	void testIsExpiredWithLastAccessTimeout() {
		// Given
		BoxCacheEntry	expiredEntry	= new BoxCacheEntry(
		    Key.of( "default" ),
		    60, // 60 seconds timeout
		    1, // 1 second last access timeout
		    Key.of( "expiredKey" ),
		    "expiredValue",
		    new Struct()
		);

		// Spy on it
		BoxCacheEntry	spyEntry		= Mockito.spy( expiredEntry );
		Mockito.doReturn( Instant.now().minusSeconds( 2 ) ).when( spyEntry ).lastAccessed();

		// Then
		assertThat( spyEntry.key() ).isEqualTo( Key.of( "expiredKey" ) );
		assertThat( spyEntry.lastAccessTimeout() ).isEqualTo( 1 );
		assertThat( spyEntry.isExpired() ).isTrue();
	}
}
