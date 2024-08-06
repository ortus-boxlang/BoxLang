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

import ortus.boxlang.runtime.types.Array;

public class CacheProvidersTest extends BaseCacheTest {

	@Test
	@DisplayName( "It can get the registered custom cache providers" )
	public void canGetCacheProviders() {
		runtime.executeSource(
		    """
		    result = cacheProviders()
		    """,
		    context );

		Array providers = variables.getAsArray( result );
		assertNotNull( providers );
		assertThat( providers.size() ).isAtLeast( 0 );
	}

}
