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

public class DecrementTest {

	@DisplayName( "It can Decrement numbers" )
	@Test
	void testItCanAddNumbers() {
		assertThat( Decrement.invoke( 3 ) ).isEqualTo( 2 );
		assertThat( Decrement.invoke( 3.5 ) ).isEqualTo( 2.5 );
	}

	@DisplayName( "It can Decrement strings" )
	@Test
	void testItCanAddStrings() {
		assertThat( Decrement.invoke( "3" ) ).isEqualTo( 2 );
		assertThat( Decrement.invoke( "3.5" ) ).isEqualTo( 2.5 );
	}

	@DisplayName( "It can compound Decrement" )
	@Test
	void testItCanCompountDecrement() {
		IScope scope = new VariablesScope();
		scope.put( Key.of( "i" ), 5 );
		assertThat( Decrement.invokePre( scope, Key.of( "i" ) ) ).isEqualTo( 4 );
		assertThat( scope.get( Key.of( "i" ) ) ).isEqualTo( 4 );

		scope.put( Key.of( "i" ), 5 );
		assertThat( Decrement.invokePost( scope, Key.of( "i" ) ) ).isEqualTo( 5 );
		assertThat( scope.get( Key.of( "i" ) ) ).isEqualTo( 4 );
	}

}
