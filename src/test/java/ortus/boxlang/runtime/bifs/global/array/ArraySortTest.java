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

public class ArraySortTest {

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

	@DisplayName( "It should run the UDF over the array until one returns true" )
	@Test
	public void testSort() {
		instance.executeSource(
		    """
		        result = [ "b", "d", "c", "a" ]

		        ArraySort( result, "text", "asc" );
		    """,
		    context );

		Array res = variables.getAsArray( result );
		assertThat( res.get( 0 ) ).isEqualTo( "a" );
		assertThat( res.get( 1 ) ).isEqualTo( "b" );
		assertThat( res.get( 2 ) ).isEqualTo( "c" );
		assertThat( res.get( 3 ) ).isEqualTo( "d" );
	}

	@DisplayName( "It should sort text ascending" )
	@Test
	public void testTextASC() {
		instance.executeSource(
		    """
		        result = [ "b", "d", "c", "a" ]

		        ArraySort( result, "text", "asc" );
		    """,
		    context );

		Array res = variables.getAsArray( result );
		assertThat( res.get( 0 ) ).isEqualTo( "a" );
		assertThat( res.get( 1 ) ).isEqualTo( "b" );
		assertThat( res.get( 2 ) ).isEqualTo( "c" );
		assertThat( res.get( 3 ) ).isEqualTo( "d" );
	}

	@DisplayName( "It should sort text descending" )
	@Test
	public void testTextDesc() {
		instance.executeSource(
		    """
		        result = [ "b", "d", "c", "a" ]

		        ArraySort( result, "text", "desc" );
		    """,
		    context );

		Array res = variables.getAsArray( result );
		assertThat( res.get( 0 ) ).isEqualTo( "d" );
		assertThat( res.get( 1 ) ).isEqualTo( "c" );
		assertThat( res.get( 2 ) ).isEqualTo( "b" );
		assertThat( res.get( 3 ) ).isEqualTo( "a" );
	}

	@DisplayName( "It should sort text case sensitively ascending" )
	@Test
	public void testTextCaseSensitivelyASC() {
		instance.executeSource(
		    """
		        result = [ "b", "d", "C", "a" ]

		        ArraySort( result, "text", "asc" );
		    """,
		    context );

		Array res = variables.getAsArray( result );
		assertThat( res.get( 0 ) ).isEqualTo( "C" );
		assertThat( res.get( 1 ) ).isEqualTo( "a" );
		assertThat( res.get( 2 ) ).isEqualTo( "b" );
		assertThat( res.get( 3 ) ).isEqualTo( "d" );
	}

	@DisplayName( "It should sort text case sensitively descending" )
	@Test
	public void testTextCaseSensitivelyDesc() {
		instance.executeSource(
		    """
		        result = [ "b", "d", "C", "a" ]

		        ArraySort( result, "text", "desc" );
		    """,
		    context );

		Array res = variables.getAsArray( result );
		assertThat( res.get( 0 ) ).isEqualTo( "d" );
		assertThat( res.get( 1 ) ).isEqualTo( "b" );
		assertThat( res.get( 2 ) ).isEqualTo( "a" );
		assertThat( res.get( 3 ) ).isEqualTo( "C" );
	}

	@DisplayName( "It should sort textnocase ascending" )
	@Test
	public void testTextNoCaseASC() {
		instance.executeSource(
		    """
		        result = [ "b", "d", "C", "a" ]

		        ArraySort( result, "textnocase", "asc" );
		    """,
		    context );

		Array res = variables.getAsArray( result );
		assertThat( res.get( 0 ) ).isEqualTo( "a" );
		assertThat( res.get( 1 ) ).isEqualTo( "b" );
		assertThat( res.get( 2 ) ).isEqualTo( "C" );
		assertThat( res.get( 3 ) ).isEqualTo( "d" );
	}

	@DisplayName( "It should sort textnocase descending" )
	@Test
	public void testTextNoCaseDesc() {
		instance.executeSource(
		    """
		        result = [ "b", "d", "C", "a" ]

		        ArraySort( result, "textnocase", "desc" );
		    """,
		    context );

		Array res = variables.getAsArray( result );
		assertThat( res.get( 0 ) ).isEqualTo( "d" );
		assertThat( res.get( 1 ) ).isEqualTo( "C" );
		assertThat( res.get( 2 ) ).isEqualTo( "b" );
		assertThat( res.get( 3 ) ).isEqualTo( "a" );
	}

	@DisplayName( "It should handle international characters in the sort correctly" )
	@Test
	public void testInternationalText() {
		instance.executeSource(
		    """
		    setLocale( "de_DE" );
		            result = ["Apple","Zebra","Ähhhh"];
		      arraySort(
		      	result,
		      	"textnocase",
		      	"asc",
		      	true
		      );
		        """,
		    context );

		Array res = variables.getAsArray( result );
		assertThat( res.get( 0 ) ).isEqualTo( "Ähhhh" );
		assertThat( res.get( 1 ) ).isEqualTo( "Apple" );
		assertThat( res.get( 2 ) ).isEqualTo( "Zebra" );
	}

	@DisplayName( "It should sort numbers ascending" )
	@Test
	public void testNumericASC() {
		instance.executeSource(
		    """
		        result = [ 2, 4.3, 3, -1 ]

		        ArraySort( result, "numeric", "asc" );
		    """,
		    context );

		Array res = variables.getAsArray( result );
		assertThat( res.get( 0 ) ).isEqualTo( -1 );
		assertThat( res.get( 1 ) ).isEqualTo( 2 );
		assertThat( res.get( 2 ) ).isEqualTo( 3 );
		assertThat( res.get( 3 ) ).isEqualTo( 4.3 );
	}

	@DisplayName( "It should sort numbers descending" )
	@Test
	public void testNumericDesc() {
		instance.executeSource(
		    """
		        result = [ 2, 4.3, 3, -1 ]

		        ArraySort( result, "numeric", "desc" );
		    """,
		    context );

		Array res = variables.getAsArray( result );
		assertThat( res.get( 0 ) ).isEqualTo( 4.3 );
		assertThat( res.get( 1 ) ).isEqualTo( 3 );
		assertThat( res.get( 2 ) ).isEqualTo( 2 );
		assertThat( res.get( 3 ) ).isEqualTo( -1 );
	}

	@DisplayName( "It should sort string numbers textually" )
	@Test
	public void testStringNumeric() {
		instance.executeSource(
		    """
		          result = ["1", "50", "100"];
		    arraySort(result, "text", "asc");
		      """,
		    context );

		Array res = variables.getAsArray( result );
		assertThat( res.get( 0 ) ).isEqualTo( "1" );
		assertThat( res.get( 1 ) ).isEqualTo( "100" );
		assertThat( res.get( 2 ) ).isEqualTo( "50" );
	}

	@DisplayName( "It should sort based on a function" )
	@Test
	public void testSortFunction() {
		instance.executeSource(
		    """
		          result = [ 2, 4.3, 3, -1 ]

		    function test( a, b ){
		    	if( a < b ){
		    		return -1;
		    	}

		      		return 1;
		      	}
		          ArraySort( result, test );
		        """,
		    context );

		Array res = variables.getAsArray( result );
		assertThat( res.get( 0 ) ).isEqualTo( -1 );
		assertThat( res.get( 1 ) ).isEqualTo( 2 );
		assertThat( res.get( 2 ) ).isEqualTo( 3 );
		assertThat( res.get( 3 ) ).isEqualTo( 4.3 );
	}

	@DisplayName( "It should allow member invocation" )
	@Test
	public void testMemberInvocation() {
		instance.executeSource(
		    """
		          result = [ 2, 4.3, 3, -1 ]

		    function test( a, b ){
		    	return  (a < b ) ? -1 : 1;
		      	}
		          result.sort( test );
		        """,
		    context );

		Array res = variables.getAsArray( result );
		assertThat( res.get( 0 ) ).isEqualTo( -1 );
		assertThat( res.get( 1 ) ).isEqualTo( 2 );
		assertThat( res.get( 2 ) ).isEqualTo( 3 );
		assertThat( res.get( 3 ) ).isEqualTo( 4.3 );
	}
}
