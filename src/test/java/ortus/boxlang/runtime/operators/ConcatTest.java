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

import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;

public class ConcatTest {

	@DisplayName( "It can concatenate strings" )
	@Test
	void testItCanConcatStrings() {
		assertThat( Concat.invoke( "Brad", "Wood" ) ).isEqualTo( "BradWood" );
	}

	@DisplayName( "It can concatenate numbers" )
	@Test
	void testItCanConcatNumbers() {
		assertThat( Concat.invoke( 4, 2 ) ).isEqualTo( "42" );
		assertThat( Concat.invoke( Double.valueOf( "4" ), Integer.valueOf( "2" ) ) ).isEqualTo( "42" );
	}

	@DisplayName( "It can concatenate number and string mixure" )
	@Test
	void testItCanConcatNumberAndString() {
		assertThat( Concat.invoke( "4", Integer.valueOf( "2" ) ) ).isEqualTo( "42" );
	}

	@DisplayName( "It can concatenate chars" )
	@Test
	void testItCanConcatNumberAndChars() {
		assertThat( Concat.invoke( 'B', 'W' ) ).isEqualTo( "BW" );
		assertThat( Concat.invoke( "B", 'W' ) ).isEqualTo( "BW" );
		assertThat( Concat.invoke( 'B', "W" ) ).isEqualTo( "BW" );
	}

	@DisplayName( "It can concatenate byte array" )
	@Test
	void testItCanConcatByteArray() {
		assertThat( Concat.invoke( "Brad".getBytes(), "Wood".getBytes() ) ).isEqualTo( "BradWood" );
	}

	@DisplayName( "It can compound concatenate" )
	@Test
	void testItCanCompountConcatenate() {
		IScope scope = new VariablesScope();
		scope.put( Key.of( "i" ), "brad" );
		assertThat( Concat.invoke( scope, Key.of( "i" ), "wood" ) ).isEqualTo( "bradwood" );
		assertThat( scope.get( Key.of( "i" ) ) ).isEqualTo( "bradwood" );
	}

}
