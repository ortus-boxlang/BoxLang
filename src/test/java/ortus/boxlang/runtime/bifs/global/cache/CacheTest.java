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
package ortus.boxlang.runtime.bifs.global.cache;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.cache.providers.ICacheProvider;

public class CacheTest extends BaseCacheTest {

	@Test
	@DisplayName( "It can get the default cache" )
	public void canGetDefaultCache() {
		runtime.executeSource(
		    """
		    result = cache()
		    """,
		    context );

		ICacheProvider cache = ( ICacheProvider ) variables.get( result );
		assertNotNull( cache );
		assertThat( cache.getName().getName() ).isEqualTo( "default" );
	}

	@Test
	@DisplayName( "It can get a named cache" )
	public void canGetNamedCache() {
		runtime.executeSource(
		    """
		    result = cache( "imports" )
		    """,
		    context );

		ICacheProvider cache = ( ICacheProvider ) variables.get( result );
		assertNotNull( cache );
		assertThat( cache.getName().getName() ).isEqualTo( "imports" );
	}

}
