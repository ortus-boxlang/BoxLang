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

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ortus.boxlang.runtime.cache.BoxCacheEntry;
import ortus.boxlang.runtime.cache.ICacheEntry;
import ortus.boxlang.runtime.cache.filters.WildcardFilter;
import ortus.boxlang.runtime.cache.providers.ICacheProvider;
import ortus.boxlang.runtime.cache.util.ICacheStats;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;

public abstract class BaseStoreTest {

	/**
	 * The target store to test
	 */
	public IObjectStore		store;

	/**
	 * The target provider to test
	 */
	public ICacheProvider	mockProvider;

	/**
	 * The mock config to use for the provider and store
	 */
	public IStruct			mockConfig		= new Struct();

	/**
	 * Mock Stats
	 */
	public ICacheStats		mockStats		= Mockito.mock( ICacheStats.class );

	/**
	 * Mock Fixtures
	 */
	public Key				testKey			= Key.of( "test" );
	public Key				eternalKey		= Key.of( "eternal" );
	public ICacheEntry		testEntry		= new BoxCacheEntry(
	    Key.of( "test" ),
	    60,
	    10,
	    Key.of( "test" ),
	    Instant.now(),
	    new Struct()
	);
	public ICacheEntry		eternalEntry	= new BoxCacheEntry(
	    Key.of( "test" ),
	    0,
	    0,
	    Key.of( "eternal" ),
	    Instant.now(),
	    new Struct()
	);

	public void populateCacheEntries( IObjectStore store ) {
		store.set( testKey, testEntry );
		store.set( eternalKey, eternalEntry );
	}

	public ICacheProvider getMockProvider( String name ) {
		// Create a mock instance of ICacheProvider
		ICacheProvider mockProvider = Mockito.mock( ICacheProvider.class );

		when( mockProvider.getStats() ).thenReturn( mockStats );
		when( mockProvider.getName() ).thenReturn( name );
		when( mockProvider.getType() ).thenReturn( "boxlang" );
		when( mockProvider.getConfig() ).thenReturn( mockConfig );

		return mockProvider;
	}

	/**
	 * -- Test Methods --
	 * These methods must be run from the parent class
	 */

	public void setup() {
		populateCacheEntries( store );
	}

	@Test
	@DisplayName( "BaseTest: Can get config and provider" )
	public void testGetConfigAndProvider() {
		assertThat( store.getConfig() ).isEqualTo( mockConfig );
		assertThat( store.getProvider() ).isEqualTo( mockProvider );
	}

	@Test
	@DisplayName( "BaseTest: Can shutdown the store" )
	public void testShutdown() {
		store.set( Key.of( "test" ), testEntry );
		store.shutdown();
		assertThat( store.getSize() ).isEqualTo( 0 );
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
		store.set( Key.of( "test" ), testEntry );
		store.clearAll();
		assertThat( store.getSize() ).isEqualTo( 0 );
	}

	@Test
	@DisplayName( "BaseTest: Can clear all using a filter" )
	public void testClearAllWithFilter() {
		store.set( Key.of( "test" ), testEntry );
		store.set( Key.of( "testing" ), testEntry );

		store.clearAll( new WildcardFilter( "test*" ) );
		assertThat( store.lookup( Key.of( "test" ) ) ).isNull();
		assertThat( store.lookup( Key.of( "testing" ) ) ).isNull();
	}

	@Test
	@DisplayName( "BaseTest: Can clear a key" )
	public void testClear() {
		store.set( Key.of( "test" ), testEntry );
		assertThat( store.clear( Key.of( "test" ) ) ).isTrue();
		assertThat( store.lookup( Key.of( "test" ) ) ).isNull();

		assertThat( store.clear( Key.of( "testinnnnnn" ) ) ).isFalse();
	}

	@Test
	@DisplayName( "BaseTest: Can clear multiple keys" )
	public void testClearMultiple() {
		store.set( Key.of( "test" ), testEntry );
		store.set( Key.of( "testing" ), testEntry );

		IStruct results = store.clear(
		    Key.of( "test" ),
		    Key.of( "testing" ),
		    Key.of( "bogus" )
		);
		assertThat( results ).isNotNull();
		assertThat( results.getAsBoolean( Key.of( "test" ) ) ).isTrue();
		assertThat( results.getAsBoolean( Key.of( "testing" ) ) ).isTrue();
		assertThat( results.getAsBoolean( Key.of( "bogus" ) ) ).isFalse();

		assertThat( store.lookup( Key.of( "test" ) ) ).isNull();
		assertThat( store.lookup( Key.of( "testing" ) ) ).isNull();
	}

	@Test
	@DisplayName( "BaseTest: Can get all keys" )
	public void testGetKeys() {
		store.set( Key.of( "test" ), testEntry );
		store.set( Key.of( "testing" ), testEntry );
	}

}
