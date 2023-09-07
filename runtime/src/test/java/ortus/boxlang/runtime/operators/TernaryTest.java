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

public class TernaryTest {

	@DisplayName( "It can work correctly" )
	@Test
	void testItWork() {
		assertThat( Ternary.invoke( true, "was true", "was false" ) ).isEqualTo( "was true" );
		assertThat( Ternary.invoke( false, "was true", "was false" ) ).isEqualTo( "was false" );

		assertThat( Ternary.invoke( 1, "was true", "was false" ) ).isEqualTo( "was true" );
		assertThat( Ternary.invoke( 0, "was true", "was false" ) ).isEqualTo( "was false" );

		assertThat( Ternary.invoke( "yes", "was true", "was false" ) ).isEqualTo( "was true" );
		assertThat( Ternary.invoke( "no", "was true", "was false" ) ).isEqualTo( "was false" );
	}

}
