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
package ortus.boxlang.runtime.cache.store;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ortus.boxlang.runtime.cache.BoxCacheEntry;
import ortus.boxlang.runtime.cache.ICacheEntry;
import ortus.boxlang.runtime.cache.filters.WildcardFilter;
import ortus.boxlang.runtime.cache.providers.ICacheProvider;
import ortus.boxlang.runtime.cache.util.BoxCacheStats;
import ortus.boxlang.runtime.cache.util.ICacheStats;
import ortus.boxlang.runtime.config.segments.CacheConfig;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;

public abstract class BaseStoreTest {

	/**
	 * The target store to test: Set by the child class
	 */
	public static IObjectStore		store;

	/**
	 * The target provider to test: Set by the child class
	 */
	public static ICacheProvider	mockProvider;

	/**
	 * The mock config to use for the provider and store: Set by the child class
	 */
	public static CacheConfig		mockConfig	= new CacheConfig();

	/**
	 * Mock Stats
	 */
	public static ICacheStats		mockStats	= new BoxCacheStats();

	public static ICacheEntry newTestEntry( String key ) {
		return new BoxCacheEntry(
		    Key.of( key ),
		    60,
		    10,
		    Key.of( key ),
		    Instant.now(),
		    new Struct()
		);
	}

	public static ICacheEntry newTestEntry(
	    String key,
	    long timeout,
	    long maxIdle,
	    Object value ) {
		return new BoxCacheEntry(
		    Key.of( key ),
		    timeout,
		    maxIdle,
		    Key.of( key ),
		    value,
		    new Struct()
		);
	}

	public static ICacheProvider getMockProvider( String name ) {
		// Create a mock instance of ICacheProvider
		ICacheProvider mockProvider = Mockito.mock( ICacheProvider.class );

		when( mockProvider.getStats() ).thenReturn( mockStats );
		when( mockProvider.getName() ).thenReturn( Key.of( name ) );
		when( mockProvider.getType() ).thenReturn( "boxlang" );
		when( mockProvider.getConfig() ).thenReturn( mockConfig );

		return mockProvider;
	}

	/**
	 * -- Test Methods --
	 */

	@BeforeEach
	public void setup() {
		store.clearAll();
	}

	@Test
	@DisplayName( "BaseTest: Can get config and provider" )
	public void testGetConfigAndProvider() {
		assertThat( store.getConfig() ).isEqualTo( mockConfig.properties );
		assertThat( store.getProvider() ).isEqualTo( mockProvider );
	}

	@Test
	@DisplayName( "BaseTest: Can shutdown the store" )
	public void testShutdown() {
		store.set( Key.of( "test" ), newTestEntry( "test" ) );
		store.shutdown();
		if ( ! ( store instanceof FileSystemStore ) ) {
			assertThat( store.getSize() ).isEqualTo( 0 );
		}
	}

	@Test
	@DisplayName( "BaseTest: Can flush the store" )
	public void testFlush() {
		var results = store.flush();
		assertThat( results ).isNotNull();
	}

	@Test
	@DisplayName( "BaseTest: Can clear all and check size" )
	public void testClearAll() {
		store.set( Key.of( "test" ), newTestEntry( "test" ) );
		store.clearAll();
		assertThat( store.getSize() ).isEqualTo( 0 );
	}

	@Test
	@DisplayName( "BaseTest: Can clear all using a filter" )
	public void testClearAllWithFilter() throws InterruptedException {
		store.set( Key.of( "test" ), newTestEntry( "test" ) );
		store.set( Key.of( "testing" ), newTestEntry( "testing" ) );

		store.clearAll( new WildcardFilter( "test*" ) );
		assertThat( store.lookup( Key.of( "test" ) ) ).isFalse();
		assertThat( store.lookup( Key.of( "testing" ) ) ).isFalse();
	}

	@Test
	@DisplayName( "BaseTest: Can clear a key" )
	public void testClear() throws InterruptedException, IOException {
		store.set( Key.of( "test" ), newTestEntry( "test" ) );
		assertThat( store.clear( Key.of( "test" ) ) ).isTrue();
		assertThat( store.lookup( Key.of( "test" ) ) ).isFalse();
		// Bogus one
		assertThat( store.clear( Key.of( "testinnnnnn" ) ) ).isFalse();
	}

	@Test
	@DisplayName( "BaseTest: Can clear multiple keys" )
	public void testClearMultiple() {
		store.set( Key.of( "test" ), newTestEntry( "test" ) );
		store.set( Key.of( "testing" ), newTestEntry( "testing" ) );

		IStruct results = store.clear(
		    Key.of( "test" ),
		    Key.of( "testing" ),
		    Key.of( "bogus" )
		);
		assertThat( results ).isNotNull();
		assertThat( results.getAsBoolean( Key.of( "test" ) ) ).isTrue();
		assertThat( results.getAsBoolean( Key.of( "testing" ) ) ).isTrue();
		assertThat( results.getAsBoolean( Key.of( "bogus" ) ) ).isFalse();

		assertThat( store.lookup( Key.of( "test" ) ) ).isFalse();
		assertThat( store.lookup( Key.of( "testing" ) ) ).isFalse();
	}

	@Test
	@DisplayName( "BaseTest: Can get all keys" )
	public void testGetKeys() {
		store.set( Key.of( "test" ), newTestEntry( "test" ) );
		store.set( Key.of( "testing" ), newTestEntry( "testing" ) );

		Key[] keys = store.getKeys();
		assertThat( keys ).isNotNull();
		assertThat( List.of( keys ) ).containsExactly(
		    Key.of( "test" ),
		    Key.of( "testing" )
		);
	}

	@Test
	@DisplayName( "BaseTest: Can get all keys with a filter" )
	public void testGetKeysWithFilter() {
		store.set( Key.of( "test" ), newTestEntry( "test" ) );
		store.set( Key.of( "testing" ), newTestEntry( "testing" ) );

		Key[] keys = store.getKeys( new WildcardFilter( "test*" ) );
		assertThat( keys ).isNotNull();
		assertThat( List.of( keys ) ).containsExactly(
		    Key.of( "test" ),
		    Key.of( "testing" )
		);
	}

	@Test
	@DisplayName( "BaseTest: Can get all keys as a stream" )
	public void testGetKeysStream() {
		store.set( Key.of( "test" ), newTestEntry( "test" ) );
		store.set( Key.of( "testing" ), newTestEntry( "testing" ) );

		var keys = store.getKeysStream();
		assertThat( keys ).isNotNull();
		assertThat( keys.count() ).isEqualTo( 2 );
	}

	@Test
	@DisplayName( "BaseTest: Can get all keys as a stream with a filter" )
	public void testGetKeysStreamWithFilter() {
		store.set( Key.of( "test" ), newTestEntry( "test" ) );
		store.set( Key.of( "testing" ), newTestEntry( "testing" ) );

		var keys = store.getKeysStream( new WildcardFilter( "test*" ) );
		assertThat( keys ).isNotNull();
		assertThat( keys.count() ).isEqualTo( 2 );
	}

	@Test
	@DisplayName( "BaseTest: Can check if a key exists" )
	public void testLookup() {
		store.set( Key.of( "test" ), newTestEntry( "test" ) );
		assertThat( store.lookup( Key.of( "test" ) ) ).isTrue();
		assertThat( store.lookup( Key.of( "bogus" ) ) ).isFalse();
	}

	@Test
	@DisplayName( "BaseTest: Can check if multiple keys exist" )
	public void testLookupMultiple() {
		store.set( Key.of( "test" ), newTestEntry( "test" ) );
		store.set( Key.of( "testing" ), newTestEntry( "testing" ) );

		IStruct results = store.lookup(
		    Key.of( "test" ),
		    Key.of( "testing" ),
		    Key.of( "bogus" )
		);
		assertThat( results ).isNotNull();
		assertThat( results.getAsBoolean( Key.of( "test" ) ) ).isTrue();
		assertThat( results.getAsBoolean( Key.of( "testing" ) ) ).isTrue();
		assertThat( results.getAsBoolean( Key.of( "bogus" ) ) ).isFalse();
	}

	@Test
	@DisplayName( "BaseTest: Can get lookups with a cache filter" )
	public void testLookupWithFilter() {
		store.set( Key.of( "test" ), newTestEntry( "test" ) );
		store.set( Key.of( "testing" ), newTestEntry( "testing" ) );

		IStruct results = store.lookup( new WildcardFilter( "test*" ) );
		assertThat( results ).isNotNull();
		assertThat( results.getAsBoolean( Key.of( "test" ) ) ).isTrue();
		assertThat( results.getAsBoolean( Key.of( "testing" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "BaseTest: Can getQuiet entries" )
	public void testGetQuiet() {
		var testEntry = newTestEntry( "test" );
		store.set( Key.of( "test" ), testEntry );
		assertThat( store.getQuiet( Key.of( "test" ) ) ).isEqualTo( testEntry );
		assertThat( store.getQuiet( Key.of( "bogus" ) ) ).isNull();
	}

	@Test
	@DisplayName( "BaseTest: Can getQuiet multiple entries" )
	public void testGetQuietMultiple() {
		var	testEntry		= newTestEntry( "test" );
		var	testingEntry	= newTestEntry( "testing" );

		store.set( Key.of( "test" ), testEntry );
		store.set( Key.of( "testing" ), testingEntry );

		IStruct results = store.getQuiet(
		    Key.of( "test" ),
		    Key.of( "testing" ),
		    Key.of( "bogus" )
		);
		assertThat( results ).isNotNull();
		assertThat( results.get( Key.of( "test" ) ) ).isEqualTo( testEntry );
		assertThat( results.get( Key.of( "testing" ) ) ).isEqualTo( testingEntry );
		assertThat( results.get( Key.of( "bogus" ) ) ).isNull();
	}

	@Test
	@DisplayName( "BaseTest: Can getQuiet entries with a filter" )
	public void testGetQuietWithFilter() {
		var	testEntry		= newTestEntry( "test" );
		var	testingEntry	= newTestEntry( "testing" );
		store.set( Key.of( "test" ), testEntry );
		store.set( Key.of( "testing" ), testingEntry );

		IStruct results = store.getQuiet( new WildcardFilter( "test*" ) );
		assertThat( results ).isNotNull();
		assertThat( results.get( Key.of( "test" ) ) ).isEqualTo( testEntry );
		assertThat( results.get( Key.of( "testing" ) ) ).isEqualTo( testingEntry );
	}

	@Test
	@DisplayName( "BaseTest: Can get entries" )
	public void testGet() {
		var testEntry = newTestEntry( "test" );
		store.set( Key.of( "test" ), testEntry );

		var results = store.get( Key.of( "test" ) );
		assertThat( results ).isEqualTo( testEntry );
		assertThat( results.hits() ).isEqualTo( 1 );
		assertThat( results.lastAccessed() ).isNotNull();
		assertThat( store.get( Key.of( "bogus" ) ) ).isNull();
	}

	@Test
	@DisplayName( "BaseTest: Can get multiple entries" )
	public void testGetMultiple() {
		var	testEntry		= newTestEntry( "test" );
		var	testingEntry	= newTestEntry( "testing" );

		store.set( Key.of( "test" ), testEntry );
		store.set( Key.of( "testing" ), testingEntry );

		IStruct results = store.get(
		    Key.of( "test" ),
		    Key.of( "testing" ),
		    Key.of( "bogus" )
		);
		assertThat( results ).isNotNull();
		assertThat( results.get( Key.of( "test" ) ) ).isEqualTo( testEntry );
		assertThat( results.get( Key.of( "testing" ) ) ).isEqualTo( testingEntry );
		assertThat( results.get( Key.of( "bogus" ) ) ).isNull();
	}

	@Test
	@DisplayName( "BaseTest: Can get entries with a filter" )
	public void testGetWithFilter() {
		var	testEntry		= newTestEntry( "test" );
		var	testingEntry	= newTestEntry( "testing" );

		store.set( Key.of( "test" ), testEntry );
		store.set( Key.of( "testing" ), testingEntry );

		IStruct results = store.get( new WildcardFilter( "test*" ) );
		assertThat( results ).isNotNull();
		assertThat( results.get( Key.of( "test" ) ) ).isEqualTo( testEntry );
		assertThat( results.get( Key.of( "testing" ) ) ).isEqualTo( testingEntry );
	}

	@Test
	@DisplayName( "BaseTest: Can set an entry" )
	public void testSet() {
		var testEntry = newTestEntry( "test" );
		store.set( Key.of( "test" ), testEntry );
		assertThat( store.lookup( Key.of( "test" ) ) ).isTrue();
		assertThat( store.get( Key.of( "test" ) ) ).isEqualTo( testEntry );
	}

	@Test
	@DisplayName( "BaseTest: Can set multiple entries" )
	public void testSetMultiple() {
		var	testEntry		= newTestEntry( "test" );
		var	testingEntry	= newTestEntry( "testing" );

		store.set( Key.of( "test" ), testEntry );
		store.set( Key.of( "testing" ), testingEntry );

		assertThat( store.lookup( Key.of( "test" ) ) ).isTrue();
		assertThat( store.lookup( Key.of( "testing" ) ) ).isTrue();
		assertThat( store.get( Key.of( "test" ) ) ).isEqualTo( testEntry );
		assertThat( store.get( Key.of( "testing" ) ) ).isEqualTo( testingEntry );
	}

	@Test
	@DisplayName( "BaseTest: Can evict entries using the default LRU eviction policy" )
	public void testEvict() throws InterruptedException {
		var	testEntry		= newTestEntry( "test" );
		var	testingEntry	= newTestEntry( "testing" );

		store.set( Key.of( "test" ), testEntry );
		store.set( Key.of( "testing" ), testingEntry );

		// Run the eviction process
		store.evict();

		assertThat( store.getSize() ).isEqualTo( 1 );
	}

}
