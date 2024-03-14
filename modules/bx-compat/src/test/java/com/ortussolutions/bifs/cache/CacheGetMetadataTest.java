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
package com.ortussolutions.bifs.cache;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;

public class CacheGetMetadataTest extends BaseCacheTest {

	@Test
	@DisplayName( "can call cacheGetMetadata with a valid key" )
	public void canCallCacheGetMetadata() {
		runtime.executeSource(
		    """
		    result = cacheGetMetadata( "tdd" );
		    """,
		    context );

		IStruct data = variables.getAsStruct( result );
		assertThat( data ).isNotNull();
		assertThat( data ).isInstanceOf( Struct.class );
		assertThat( data.get( "hits" ) ).isEqualTo( 1 );
	}

	@Test
	@DisplayName( "It will get an empty struct with an invalid key" )
	public void canCallCacheGetMetadataWithInvalidKey() {
		runtime.executeSource(
		    """
		    result = cacheGetMetadata( "invalid" );
		    """,
		    context );
		IStruct data = variables.getAsStruct( result );
		assertThat( data.size() ).isEqualTo( 0 );
	}

	@Test
	@DisplayName( "can call cacheGetMetadata with a valid key and cacheName" )
	public void canCallCacheGetMetadataWithCacheName() {
		runtime.executeSource(
		    """
		    result = cacheGetMetadata( "tdd", "default" );
		    """,
		    context );

		IStruct data = variables.getAsStruct( result );
		assertThat( data ).isNotNull();
		assertThat( data ).isInstanceOf( Struct.class );
		assertThat( data.get( "hits" ) ).isEqualTo( 1 );
	}

	@Test
	@DisplayName( "can call cacheGetMetadata with an array of keys" )
	public void canCallCacheGetMetadataWithArrayOfKeys() {
		runtime.executeSource(
		    """
		    result = cacheGetMetadata( [ "tdd", "bdd" ] );
		    """,
		    context );

		IStruct data = variables.getAsStruct( result );
		assertThat( data ).isNotNull();
		assertThat( data ).isInstanceOf( Struct.class );

		var tddKey = Key.of( "tdd" );
		assertThat( data.get( tddKey ) ).isNotNull();
		assertThat( data.getAsStruct( tddKey ) ).isInstanceOf( Struct.class );
		assertThat( data.getAsStruct( tddKey ).get( "hits" ) ).isEqualTo( 1 );

		var bddKey = Key.of( "bdd" );
		assertThat( data.get( bddKey ) ).isNotNull();
		assertThat( data.getAsStruct( bddKey ) ).isInstanceOf( Struct.class );
		assertThat( data.getAsStruct( bddKey ).get( "hits" ) ).isEqualTo( 1 );
	}

}
