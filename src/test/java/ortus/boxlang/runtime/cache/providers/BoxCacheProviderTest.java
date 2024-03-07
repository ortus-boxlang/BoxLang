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
package ortus.boxlang.runtime.cache.providers;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mockito;

import ortus.boxlang.runtime.config.segments.CacheConfig;
import ortus.boxlang.runtime.services.CacheService;

@Disabled
public class BoxCacheProviderTest {

	public static ICacheProvider	provider;
	// Test using defaults
	public static CacheConfig		config			= new CacheConfig();
	static CacheService				cacheService	= Mockito.mock( CacheService.class );

	@BeforeAll
	public static void setUp() {
		provider = new BoxCacheProvider();
		// Startup the cache to test
		provider.configure( cacheService, config );
	}

	@Test
	@DisplayName( "Verify startup procedures" )
	public void testStartup() {
		// Verify that the cache service was started
		assertThat( provider.isEnabled() ).isTrue();
	}

}
