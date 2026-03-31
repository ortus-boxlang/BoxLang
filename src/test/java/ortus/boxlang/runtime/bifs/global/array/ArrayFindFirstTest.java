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

import static com.google.common.truth.Truth.assertThat;

public class ArrayFindFirstTest {

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

	@DisplayName( "It returns the first element of an array that matches the predicate function" )
	@Test
	public void testFindFirst() {
		instance.executeSource(
		    """
		    	data = [ 1, 2, 3, 4 ];

		    	result = arrayFindFirst( data, ( n ) => n > 2 );
		    """,
		    context );
		assertThat( ( Integer ) variables.get( result ) ).isEqualTo( 3 );
	}

	@DisplayName( "It returns the default value if no value matches" )
	@Test
	public void testFindFirstDefaultValue() {
		instance.executeSource(
		    """
		    	data = [ 1, 2, 3, 4 ];

		    	result = arrayFindFirst( data, ( n ) => n > 4, 5 );
		    """,
		    context );
		assertThat( ( Integer ) variables.get( result ) ).isEqualTo( 5 );
	}

	@DisplayName( "It returns the default value if the array is empty" )
	@Test
	public void testFindFirstDefaultValueEmptyArray() {
		instance.executeSource(
		    """
		    	data = [];

		    	result = arrayFindFirst( data, ( n ) => n > 4, 6 );
		    """,
		    context );
		assertThat( ( Integer ) variables.get( result ) ).isEqualTo( 6 );
	}

	@DisplayName( "It throws an exception if no item matches the predicate function" )
	@Test
	public void testFindFirstException() {
		instance.executeSource(
		    """
		    	data = [ 1, 2, 3, 4 ];

		    	try {
		    		result = arrayFindFirst( data, ( n ) => n > 5 );
		    	} catch ( any e ) {
		    	    result = e.message;
		    	}
		    """,
		    context );
		assertThat( ( String ) variables.get( result ) ).isEqualTo( "Could not find any results that matched the predicate function." );
	}

	@DisplayName( "It throws an exception if the array is empty" )
	@Test
	public void testFindFirstEmptyException() {
		instance.executeSource(
		    """
		    	data = [];

		    	try {
		    		result = arrayFindFirst( data, ( n ) => n > 1 );
		    	} catch ( any e ) {
		    	    result = e.message;
		    	}
		    """,
		    context );
		assertThat( ( String ) variables.get( result ) ).isEqualTo( "Cannot retrieve the first record of an empty array." );
	}
}
