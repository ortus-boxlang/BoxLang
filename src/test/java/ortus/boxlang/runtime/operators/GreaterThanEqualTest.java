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
package ortus.boxlang.runtime.operators;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class GreaterThanEqualTest {

	@DisplayName( "It can compare strings" )
	@Test
	void testItCanCompareStrings() {
		assertThat( GreaterThanEqual.invoke( "Brad", "Brad" ) ).isTrue();
		assertThat( GreaterThanEqual.invoke( "Brad", "BRAD" ) ).isTrue();
		assertThat( GreaterThanEqual.invoke( "B", "A" ) ).isTrue();
	}

	@DisplayName( "It can compare numbers" )
	@Test
	void testItCanCompareNumbers() {
		assertThat( GreaterThanEqual.invoke( 1, 1 ) ).isTrue();
		assertThat( GreaterThanEqual.invoke( 2, 1 ) ).isTrue();

		assertThat( GreaterThanEqual.invoke( 1.5, 1.5 ) ).isTrue();
		assertThat( GreaterThanEqual.invoke( 2.8, 0.6 ) ).isTrue();
	}

	@DisplayName( "It can compare strings as numbers" )
	@Test
	void testItCanCompareStringsAsNumbers() {
		assertThat( GreaterThanEqual.invoke( "1", "1" ) ).isTrue();
		assertThat( GreaterThanEqual.invoke( "2", "1" ) ).isTrue();

		assertThat( GreaterThanEqual.invoke( "1.5", "1.5" ) ).isTrue();
		assertThat( GreaterThanEqual.invoke( "2.8", "0.6" ) ).isTrue();

		assertThat( GreaterThanEqual.invoke( "1.5", "1.500" ) ).isTrue();
		assertThat( GreaterThanEqual.invoke( "2.8", "00000.600000" ) ).isTrue();
	}

}
