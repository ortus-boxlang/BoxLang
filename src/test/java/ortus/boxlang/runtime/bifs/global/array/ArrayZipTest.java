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

import org.junit.jupiter.api.*;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.Array;

import static com.google.common.truth.Truth.assertThat;

public class ArrayZipTest {

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

	@DisplayName( "It can zip two arrays together" )
	@Test
	public void testCanZipTwoArrays() {
		instance.executeSource(
		    """
		    	data = [ 1, 2, 3 ];
		    	zipWith = [ 4, 5, 6 ];

		    	result = arrayZip( data, zipWith );
		    """,
		    context );
		assertThat( ( Array ) variables.get( result ) ).isEqualTo(
		    Array.of(
		        Array.of( 1, 4 ),
		        Array.of( 2, 5 ),
		        Array.of( 3, 6 )
		    )
		);
	}

	@DisplayName( "It can zip two arrays together with a projection function" )
	@Test
	public void testCanZipWithProjectionFunction() {
		instance.executeSource(
		    """
		    	data = [ 1, 2, 3 ];
		    	zipWith = [ 4, 5, 6 ];

		    	result = arrayZip( data, zipWith, ( a, b ) => a + b );
		    """,
		    context );
		assertThat( ( Array ) variables.get( result ) ).isEqualTo( Array.of( 5, 7, 9 ) );
	}

	@DisplayName( "throws an exception if the two arrays are different lengths" )
	@Test
	public void testThrowsIfArraysAreDifferentLengths() {
		instance.executeSource(
		    """
		    	data = [ 1, 2, 3 ];
		    	zipWith = [ 4, 5 ];

		    	try {
		    		result = data.zip( zipWith );
		    	} catch ( any e ) {
		    	    result = e.message;
		    	}
		    """,
		    context );
		assertThat( ( String ) variables.get( result ) ).isEqualTo( "The two arrays do not have the same length.  array1 length: [3]. array2 length: [2]" );
	}
}
