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
package ortus.boxlang.runtime.util;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.cache.providers.ICacheProvider;
import ortus.boxlang.runtime.scopes.Key;

public class RegexBuilderTest {

	static BoxRuntime instance;

	@BeforeAll
	public static void setUp() {
		instance = BoxRuntime.getInstance( true );
	}

	@DisplayName( "Build a new regex matcher of a string only" )
	@Test
	public void testOf() {
		var matcher = RegexBuilder.of( "test" );
		assertThat( matcher ).isNotNull();
	}

	@DisplayName( "Build a new regex matcher of a string and a pattern" )
	@Test
	public void testOfWithPattern() {
		var matcher = RegexBuilder.of( "test", RegexBuilder.BACKSLASH );
		assertThat( matcher ).isNotNull();
	}

	@DisplayName( "Build a new regex matcher of a string and a string pattern" )
	@Test
	public void testOfWithStringPattern() {
		ICacheProvider regexCache = instance.getCacheService().getCache( Key.bxRegex );
		regexCache.clearAll();

		var matcher = RegexBuilder.of( "test", "\\\\" );
		assertThat( matcher ).isNotNull();
		assertThat( regexCache.getSize() ).isEqualTo( 1 );
	}
}
