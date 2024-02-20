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

package ortus.boxlang.runtime.bifs.global.array;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.Array;

public class ArrayEachTest {

	static BoxRuntime	instance;
	IBoxContext			context;
	IScope				variables;
	static Key			result	= new Key( "result" );

	@BeforeAll
	public static void setUp() {
		instance = BoxRuntime.getInstance( true );
	}

	@AfterAll
	public static void teardown() {

	}

	@BeforeEach
	public void setupEach() {
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
	}

	@DisplayName( "It should use the provided udf over the array" )
	@Test
	public void testUseProvidedUDF() {
		instance.executeSource(
		    """
		        indexes = [];
		        nums = [ 1, 2, 3, 4, 5 ];

		        function eachFn( value, i ){
		            indexes[ i ] = value;
		        };

		        ArrayEach( nums, eachFn );
		    """,
		    context );
		Array indexes = ( Array ) variables.get( Key.of( "indexes" ) );
		assertThat( indexes.size() ).isEqualTo( 5 );
		assertThat( indexes.get( 0 ) ).isEqualTo( 1 );
		assertThat( indexes.get( 1 ) ).isEqualTo( 2 );
		assertThat( indexes.get( 2 ) ).isEqualTo( 3 );
		assertThat( indexes.get( 3 ) ).isEqualTo( 4 );
		assertThat( indexes.get( 4 ) ).isEqualTo( 5 );
	}

	@DisplayName( "It should allow you to call it as a member function" )
	@Test
	public void testUseProvidedUDFCallMember() {
		instance.executeSource(
		    """
		        indexes = [];
		        nums = [ 1, 2, 3, 4, 5 ];

		        function eachFn( value, i ){
		            indexes[ i ] = value;
		        };

		        nums.each( eachFn );
		    """,
		    context );
		Array indexes = ( Array ) variables.get( Key.of( "indexes" ) );
		assertThat( indexes.size() ).isEqualTo( 5 );
		assertThat( indexes.get( 0 ) ).isEqualTo( 1 );
		assertThat( indexes.get( 1 ) ).isEqualTo( 2 );
		assertThat( indexes.get( 2 ) ).isEqualTo( 3 );
		assertThat( indexes.get( 3 ) ).isEqualTo( 4 );
		assertThat( indexes.get( 4 ) ).isEqualTo( 5 );
	}
}
