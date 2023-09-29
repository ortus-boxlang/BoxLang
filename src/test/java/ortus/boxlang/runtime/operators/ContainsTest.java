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

public class ContainsTest {

	@DisplayName( "It can find string in string" )
	@Test
	void testItCanFindStringInString() {
		assertThat( Contains.invoke( "BradWood", "Wood" ) ).isTrue();
		assertThat( Contains.invoke( "BradWood", "wOOD" ) ).isTrue();
		assertThat( Contains.invoke( "Luis", "Wood" ) ).isFalse();
	}

	@DisplayName( "It can find number in string" )
	@Test
	void testItCanFindNumberInString() {
		assertThat( Contains.invoke( "Brad is 43 years old.", 43 ) ).isTrue();
		assertThat( Contains.invoke( "Brad is 43 years old.", 82 ) ).isFalse();
	}

	@DisplayName( "It can find char in string" )
	@Test
	void testItCanFindCharInString() {
		assertThat( Contains.invoke( "ABCDEFG", 'F' ) ).isTrue();
		assertThat( Contains.invoke( "ABCDEFG", 'f' ) ).isTrue();
	}

	@DisplayName( "It can find number in number" )
	@Test
	void testItCanFindNumberInNumber() {
		assertThat( Contains.invoke( 12345, 3 ) ).isTrue();
		assertThat( Contains.invoke( 12345, 9 ) ).isFalse();
	}

}
