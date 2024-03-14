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

import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;

public class CacheGetMetadataReportTest extends BaseCacheTest {

	@Test
	@DisplayName( "It can get the metadata report with default values" )
	public void canCallCacheGetMetadata() {
		runtime.executeSource(
		    """
		    result = cacheGetMetadataReport();
		    """,
		    context );

		IStruct data = variables.getAsStruct( result );
		assertThat( data ).isNotNull();
		assertThat( data ).isInstanceOf( Struct.class );
		// Verify tdd and bdd keys are present
		assertThat( data.get( "tdd" ) ).isNotNull();
		assertThat( data.get( "bdd" ) ).isNotNull();
	}

	@Test
	@DisplayName( "It can get the metadata report with a limit" )
	public void canCallCacheGetMetadataWithLimits() {
		runtime.executeSource(
		    """
		    result = cacheGetMetadataReport( 1 );
		    """,
		    context );

		IStruct data = variables.getAsStruct( result );
		assertThat( data ).isNotNull();
		assertThat( data ).isInstanceOf( Struct.class );
		// Verify only one key is present
		assertThat( data.size() ).isEqualTo( 1 );
	}

}
