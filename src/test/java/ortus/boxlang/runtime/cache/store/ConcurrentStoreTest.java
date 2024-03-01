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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.cache.ICacheEntry;
import ortus.boxlang.runtime.cache.providers.ICacheProvider;
import ortus.boxlang.runtime.config.segments.CacheConfig;
import ortus.boxlang.runtime.scopes.Key;

class ConcurrentStoreTest extends BaseStoreTest {

	private ICacheProvider	mockProvider;
	private IObjectStore	store;

	@BeforeEach
	void setUp() {
		mockConfig		= CacheConfig.DEFAULTS;
		mockProvider	= getMockProvider( "test" );

		store			= new ConcurrentStore().init( mockProvider, mockConfig );
		populateCacheEntries( store );
	}

	@Test
	@DisplayName( "Can get config and provider" )
	void testGetConfigAndProvider() {
		assertThat( store.getConfig() ).isEqualTo( mockConfig );
		assertThat( store.getProvider() ).isEqualTo( mockProvider );
	}

	@Test
	@DisplayName( "set and get an object correctly" )
	void testSetAndGet() {
		ICacheEntry retrievedEntry = store.get( Key.of( "test" ) );
		assertThat( retrievedEntry ).isEqualTo( testEntry );

		retrievedEntry = store.get( Key.of( "eternal" ) );
		assertThat( retrievedEntry ).isEqualTo( eternalEntry );
	}

}
