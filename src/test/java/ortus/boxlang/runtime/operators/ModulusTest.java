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
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.exceptions.BoxLangException;

public class ModulusTest {

	private IBoxContext context = new ScriptingRequestBoxContext();

	@DisplayName( "It can Modulus numbers" )
	@Test
	void testItCanModulusNumbers() {
		assertThat( Modulus.invoke( 9, 3 ).doubleValue() ).isEqualTo( 0 );
		assertThat( Modulus.invoke( 3, 1.5 ).doubleValue() ).isEqualTo( 0 );
		assertThat( Modulus.invoke( 3, 2 ).doubleValue() ).isEqualTo( 1 );
		assertThat( Modulus.invoke( 9, 5 ).doubleValue() ).isEqualTo( 4 );
	}

	@DisplayName( "It can Modulus strings" )
	@Test
	void testItCanModulusStrings() {
		assertThat( Modulus.invoke( "9", "3" ).doubleValue() ).isEqualTo( 0 );
		assertThat( Modulus.invoke( "3", "1.5" ).doubleValue() ).isEqualTo( 0 );
	}

	@DisplayName( "It can not Modulus by zero" )
	@Test
	void testItCanNotModulusByZero() {
		assertThrows( BoxLangException.class, () -> Modulus.invoke( 1, 0 ) );
	}

	@DisplayName( "It can compound Modulus" )
	@Test
	void testItCanCompountModulus() {
		IScope scope = new VariablesScope();
		scope.put( Key.of( "i" ), 4 );
		assertThat( Modulus.invoke( context, scope, Key.of( "i" ), 2 ).doubleValue() ).isEqualTo( 0 );
		assertThat( scope.getAsNumber( Key.of( "i" ) ).doubleValue() ).isEqualTo( 0 );
	}

}
