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

import static org.mockito.Mockito.when;

import java.time.Instant;

import org.mockito.Mockito;

import ortus.boxlang.runtime.cache.BoxCacheEntry;
import ortus.boxlang.runtime.cache.ICacheEntry;
import ortus.boxlang.runtime.cache.providers.ICacheProvider;
import ortus.boxlang.runtime.cache.util.ICacheStats;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;

public class BaseStoreTest {

	public IStruct		mockConfig		= new Struct();
	public ICacheStats	mockStats		= Mockito.mock( ICacheStats.class );
	public Key			testKey			= Key.of( "test" );
	public Key			eternalKey		= Key.of( "eternal" );
	public ICacheEntry	testEntry		= new BoxCacheEntry(
	    Key.of( "test" ),
	    60,
	    10,
	    Key.of( "test" ),
	    Instant.now(),
	    new Struct()
	);
	public ICacheEntry	eternalEntry	= new BoxCacheEntry(
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

}
