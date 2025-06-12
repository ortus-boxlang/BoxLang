
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

package ortus.boxlang.runtime.bifs.global.list;

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

public class ListSomeTest {

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
	public void testUseProvidedUDF() {
		instance.executeSource(
		    """
		        indexes = [];
		        nums = "1,2,3,4,5";

		        function eachFn( value, i ){
		            indexes[ i ] = value;
		            return value == 3;
		        };

		        result = ListSome( nums, eachFn );
		    """,
		    context );

		assertThat( variables.get( result ) ).isEqualTo( true );
		Array indexes = ( Array ) variables.get( Key.of( "indexes" ) );
		assertThat( indexes.size() ).isEqualTo( 3 );
		assertThat( indexes.get( 0 ) ).isEqualTo( "1" );
		assertThat( indexes.get( 1 ) ).isEqualTo( "2" );
		assertThat( indexes.get( 2 ) ).isEqualTo( "3" );
	}

	@DisplayName( "It should return false if none of the values match" )
	@Test
	public void testReturnEarlyOnFalse() {
		instance.executeSource(
		    """
		        indexes = [];
		        nums = "1,2,3,4,5";

		        function eachFn( value, i ){
		            indexes[ i ] = value;

		            return false;
		        };

		        result = ListSome( nums, eachFn );
		    """,
		    context );

		assertThat( variables.get( result ) ).isEqualTo( false );
		Array indexes = ( Array ) variables.get( Key.of( "indexes" ) );
		assertThat( indexes.size() ).isEqualTo( 5 );
		assertThat( indexes.get( 0 ) ).isEqualTo( "1" );
		assertThat( indexes.get( 1 ) ).isEqualTo( "2" );
		assertThat( indexes.get( 2 ) ).isEqualTo( "3" );
		assertThat( indexes.get( 3 ) ).isEqualTo( "4" );
		assertThat( indexes.get( 4 ) ).isEqualTo( "5" );
	}

	@DisplayName( "It should allow you to call it as a member function" )
	@Test
	public void testMemberInvocation() {
		instance.executeSource(
		    """
		        indexes = [];
		        nums = "1,2,3,4,5";

		        function eachFn( value, i ){
		            indexes[ i ] = value;

		            return false;
		        };

		        result = nums.listSome( eachFn );
		    """,
		    context );

		assertThat( variables.get( result ) ).isEqualTo( false );
		Array indexes = ( Array ) variables.get( Key.of( "indexes" ) );
		assertThat( indexes.size() ).isEqualTo( 5 );
		assertThat( indexes.get( 0 ) ).isEqualTo( "1" );
		assertThat( indexes.get( 1 ) ).isEqualTo( "2" );
		assertThat( indexes.get( 2 ) ).isEqualTo( "3" );
		assertThat( indexes.get( 3 ) ).isEqualTo( "4" );
		assertThat( indexes.get( 4 ) ).isEqualTo( "5" );
	}

	@DisplayName( "Tests with alternate delimiters and empty includes" )
	@Test
	public void testDelimiters() {
		instance.executeSource(
		    """
		        indexes = [];
		        nums = "1|2|3|4|5";

		        function eachFn( value, i ){
		            indexes[ i ] = value;

		            return false;
		        };

		        result = nums.listSome( eachFn, "|" );
		    """,
		    context );

		assertThat( variables.get( result ) ).isEqualTo( false );
		Array indexes = ( Array ) variables.get( Key.of( "indexes" ) );
		assertThat( indexes.size() ).isEqualTo( 5 );
		assertThat( indexes.get( 0 ) ).isEqualTo( "1" );
		assertThat( indexes.get( 1 ) ).isEqualTo( "2" );
		assertThat( indexes.get( 2 ) ).isEqualTo( "3" );
		assertThat( indexes.get( 3 ) ).isEqualTo( "4" );
		assertThat( indexes.get( 4 ) ).isEqualTo( "5" );

		instance.executeSource(
		    """
		        indexes = [];
		        nums = "1|2||3|4|5";

		        function eachFn( value, i ){
		            indexes[ i ] = value;

		            return false;
		        };

		        result = nums.listSome( eachFn, "|", true );
		    """,
		    context );

		assertThat( variables.get( result ) ).isEqualTo( false );
		indexes = ( Array ) variables.get( Key.of( "indexes" ) );
		assertThat( indexes.size() ).isEqualTo( 6 );
		assertThat( indexes.get( 0 ) ).isEqualTo( "1" );
		assertThat( indexes.get( 1 ) ).isEqualTo( "2" );
		assertThat( indexes.get( 2 ) ).isEqualTo( "" );
		assertThat( indexes.get( 3 ) ).isEqualTo( "3" );
		assertThat( indexes.get( 4 ) ).isEqualTo( "4" );
		assertThat( indexes.get( 5 ) ).isEqualTo( "5" );
	}

	@DisplayName( "Tests with running in parallel" )
	@Test
	public void testParallelExecution() {
		// @formatter:off
		instance.executeSource(
		    """
		        indexes = [];
		        nums = "1,2,3,4,5";

		        function eachFn( value, i ){
		            indexes[ i ] = value;
		            return value == 3;
		        };

		        result = nums.listSome(
					eachFn, // closure
					",",  // delimiter
					false, // include empty fields
					true, // multi-character delimiter
					true // parallel
					);
		    """,
		    context );
		// @formatter:on
		assertThat( variables.get( result ) ).isEqualTo( true );
	}

	@DisplayName( "Tests with running in parallel with max threads" )
	@Test
	public void testParallelExecutionWithMaxThreads() {
		// @formatter:off
		instance.executeSource(
		    """
		        indexes = [];
		        nums = "1,2,3,4,5,6,7,8,9,10";

		        function eachFn( value, i ){
		            indexes[ i ] = value;
		            return value == 3;
		        };

		        result = nums.listSome(
					eachFn, // closure
					",",  // delimiter
					false, // include empty fields
					true, // multi-character delimiter
					true, // parallel
					2 // max threads
					);
		    """,
		    context );
		// @formatter:on
		assertThat( variables.get( result ) ).isEqualTo( true );
	}

}
