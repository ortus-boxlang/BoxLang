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

public class ArrayToListTest {

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

	@DisplayName( "It should concat the values as a string" )
	@Test
	public void testUseProvidedUDF() {
		instance.executeSource(
		    """
		        nums = [ 1, 2, 3, 4, 5 ];

		        result = ArrayToList( nums );
		    """,
		    context );
		String joined = ( String ) variables.get( result );
		assertThat( joined ).isEqualTo( "1,2,3,4,5" );
	}

	@DisplayName( "It should let you set the delimiter" )
	@Test
	public void testCustomDelimiter() {
		instance.executeSource(
		    """
		        nums = [ 1, 2, 3, 4, 5 ];

		        result = ArrayToList( nums, "|" );
		    """,
		    context );
		String joined = ( String ) variables.get( result );
		assertThat( joined ).isEqualTo( "1|2|3|4|5" );
	}

	@DisplayName( "It should allow you to call it as a member function" )
	@Test
	public void testMemberFunction() {
		instance.executeSource(
		    """
		        nums = [ 1, 2, 3, 4, 5 ];

		        result = nums.toList( "|" );
		    """,
		    context );
		String joined = ( String ) variables.get( result );
		assertThat( joined ).isEqualTo( "1|2|3|4|5" );
	}

	@DisplayName( "It should allow you to call it as a member function using the name join" )
	@Test
	public void testJoinMemberFunction() {
		instance.executeSource(
		    """
		        nums = [ 1, 2, 3, 4, 5 ];

		        result = nums.join( "|" );
		    """,
		    context );
		String joined = ( String ) variables.get( result );
		assertThat( joined ).isEqualTo( "1|2|3|4|5" );
	}

	@DisplayName( "It should cast nulls to empty strings" )
	@Test
	public void testCastNullToString() {
		instance.executeSource(
		    """
		    arr = []
		    arr[4] = 3
		    result = arr.toList();
		      """,
		    context );
		String joined = ( String ) variables.get( result );
		assertThat( joined ).isEqualTo( ",,,3" );
	}

	@DisplayName( "It should preseve empty delimiters" )
	@Test
	public void testPreserveEmpty() {
		instance.executeSource(
		    """
		    arr = []
		    arr[5] = true
		    result = arrayToList( arr );
		        """,
		    context );
		String joined = ( String ) variables.get( result );
		assertThat( joined ).isEqualTo( ",,,,true" );
	}

}
