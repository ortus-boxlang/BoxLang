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
import ortus.boxlang.runtime.types.DateTime;

public class MultiplyTest {

	private IBoxContext context = new ScriptingRequestBoxContext();

	@DisplayName( "It can Multiply numbers" )
	@Test
	void testItCanMultiplyNumbers() {
		assertThat( Multiply.invoke( 3, 2 ).doubleValue() ).isEqualTo( 6 );
		assertThat( Multiply.invoke( 3.5, 2.5 ).doubleValue() ).isEqualTo( 8.75 );
	}

	@DisplayName( "It can Multiply strings" )
	@Test
	void testItCanMultiplyStrings() {
		assertThat( Multiply.invoke( "3", "2" ).doubleValue() ).isEqualTo( 6 );
		assertThat( Multiply.invoke( "3.5", "2.5" ).doubleValue() ).isEqualTo( 8.75 );
	}

	@DisplayName( "It can compound Multiply" )
	@Test
	void testItCanCompountMultiply() {
		IScope scope = new VariablesScope();
		scope.put( Key.of( "i" ), 4 );
		assertThat( Multiply.invoke( context, scope, Key.of( "i" ), 4 ).doubleValue() ).isEqualTo( 16 );
		assertThat( scope.getAsNumber( Key.of( "i" ) ).doubleValue() ).isEqualTo( 16 );
	}

	@DisplayName( "It can multiply a number by a date" )
	@Test
	void testItCanMultiplyNumberToDate() {
		DateTime refDate = new DateTime( "2025-01-01T00:00:00Z" );
		assertThat( Multiply.invoke( refDate, 2 ) ).isNotNull();
	}

}
