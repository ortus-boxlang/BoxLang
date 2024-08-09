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

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;

public class MinusTest {

	private IBoxContext context = new ScriptingRequestBoxContext();

	@DisplayName( "It can subtract numbers" )
	@Test
	void testItCanSubtractNumbers() {
		assertThat( Minus.invoke( 3, 2 ).doubleValue() ).isEqualTo( 1 );
		assertThat( Minus.invoke( 3.5, 2.5 ).doubleValue() ).isEqualTo( 1 );
	}

	@DisplayName( "It can subtract strings" )
	@Test
	void testItCanSubtractStrings() {
		assertThat( Minus.invoke( "3", "2" ).doubleValue() ).isEqualTo( 1 );
		assertThat( Minus.invoke( "3.5", "2.5" ).doubleValue() ).isEqualTo( 1 );
	}

	@DisplayName( "It can compound subtract" )
	@Test
	void testItCanCompountSubtract() {
		IScope scope = new VariablesScope();
		scope.put( Key.of( "i" ), 4 );
		assertThat( Minus.invoke( context, scope, Key.of( "i" ), 2 ) ).isEqualTo( 2 );
		assertThat( scope.getAsNumber( Key.of( "i" ) ).doubleValue() ).isEqualTo( 2 );
	}

}
