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

import ortus.boxlang.runtime.context.BaseBoxContext;
import ortus.boxlang.runtime.context.IBoxContext;

public class TernaryTest {

	@DisplayName( "It can work correctly" )
	@Test
	void testItWork() {
		IBoxContext context = new BaseBoxContext();
		assertThat( Ternary.invoke( context, true, ( c ) -> "was true", ( c ) -> "was false" ) ).isEqualTo( "was true" );
		assertThat( Ternary.invoke( context, false, ( c ) -> "was true", ( c ) -> "was false" ) ).isEqualTo( "was false" );

		assertThat( Ternary.invoke( context, 1, ( c ) -> "was true", ( c ) -> "was false" ) ).isEqualTo( "was true" );
		assertThat( Ternary.invoke( context, 0, ( c ) -> "was true", ( c ) -> "was false" ) ).isEqualTo( "was false" );

		assertThat( Ternary.invoke( context, "yes", ( c ) -> "was true", ( c ) -> "was false" ) ).isEqualTo( "was true" );
		assertThat( Ternary.invoke( context, "no", ( c ) -> "was true", ( c ) -> "was false" ) ).isEqualTo( "was false" );
	}

}
