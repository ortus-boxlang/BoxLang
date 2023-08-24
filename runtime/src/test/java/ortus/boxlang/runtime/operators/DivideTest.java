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

import org.junit.Ignore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DivideTest {

	@DisplayName( "It can Divide numbers" )
	@Test
	void testItCanDivideNumbers() {
		assertThat( Divide.invoke( 9, 3 ) ).isEqualTo( 3 );
		assertThat( Divide.invoke( 3, 1.5 ) ).isEqualTo( 2 );
		assertThat( Divide.invoke( 3, 2 ) ).isEqualTo( 1.5 );
	}

	@DisplayName( "It can Divide strings" )
	@Test
	void testItCanDivideStrings() {
		assertThat( Divide.invoke( "9", "3" ) ).isEqualTo( 3 );
		assertThat( Divide.invoke( "3", "1.5" ) ).isEqualTo( 2 );
	}

	@DisplayName( "It can not Divide by zero" )
	@Test
	void testItCanNotDivideByZero() {
		assertThrows( RuntimeException.class, () -> Divide.invoke( 1, 0 ) );
	}

	@DisplayName( "It can compound Divide" )
	@Test
	void testItCanCompountDivide() {
		IScope scope = new VariablesScope();
		scope.put( Key.of( "i" ), 4 );
		assertThat( Divide.invoke( scope, Key.of( "i" ), 2 ) ).isEqualTo( 2 );
		assertThat( scope.get( Key.of( "i" ) ) ).isEqualTo( 2 );
	}

}
