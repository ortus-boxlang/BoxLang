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

public class ArrayAppendTest {

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

	@DisplayName( "It should increase the size of the array" )
	@Test
	public void testInreasesSIze() {
		instance.executeSource(
		    """
		    arr = [ 1, 2, 3 ];
		    arrayAppend( arr, 4 );
		    result = arrayLen( arr );
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 4 );
	}

	@DisplayName( "It should place the item at the end of the array" )
	@Test
	public void testAtEndOfArray() {
		instance.executeSource(
		    """
		    arr = [ 1, 2, 3 ];
		    arrayAppend( arr, "test" );
		    result = arrayLen( arr );
		    """,
		    context );
		assertThat( ( ( Array ) variables.get( Key.of( "arr" ) ) ).get( 3 ) ).isEqualTo( "test" );
	}

	@DisplayName( "It can append another array" )
	@Test
	public void testAppendArrays() {
		instance.executeSource(
		    """
		    arr = [ 1, 2, 3 ];
		    arrayAppend( arr, [ 4, 5, 6 ], true );
		    result = arrayLen( arr );
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 6 );
		assertThat( ( ( Array ) variables.get( Key.of( "arr" ) ) ).get( 3 ) ).isEqualTo( 4 );
	}

	@DisplayName( "It can append a single value even when the merge flag is true" )
	@Test
	public void testAppendSingleValueInMergeMode() {
		instance.executeSource(
		    """
		    arr = [ 1, 2, 3 ];
		    arrayAppend( arr, 4, true );
		    result = arrayLen( arr );
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 4 );
		assertThat( ( ( Array ) variables.get( Key.of( "arr" ) ) ).get( 3 ) ).isEqualTo( 4 );
	}

}
