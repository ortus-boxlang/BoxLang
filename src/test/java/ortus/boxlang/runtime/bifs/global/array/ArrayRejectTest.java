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
import ortus.boxlang.runtime.types.unmodifiable.UnmodifiableArray;

public class ArrayRejectTest {

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

		    	function rejectFn( value, i ){
		    		indexes[ i ] = value;
		    		return true;
		    	};

		    	result = ArrayReject( nums, rejectFn );
		    """,
		    context );
		Array resultArray = ( Array ) variables.get( result );
		assertThat( resultArray.size() ).isEqualTo( 0 );
		Array indexes = ( Array ) variables.get( Key.of( "indexes" ) );
		assertThat( indexes.size() ).isEqualTo( 5 );
		assertThat( indexes.get( 0 ) ).isEqualTo( 1 );
		assertThat( indexes.get( 1 ) ).isEqualTo( 2 );
		assertThat( indexes.get( 2 ) ).isEqualTo( 3 );
		assertThat( indexes.get( 3 ) ).isEqualTo( 4 );
		assertThat( indexes.get( 4 ) ).isEqualTo( 5 );
	}

	@DisplayName( "It is fault tolerant of a mutation to the original array" )
	@Test
	public void testMutation() {
		instance.executeSource(
		    """
		     nums = [ 1, 2, 3, 4, 5 ];

		     function rejectFn( value, i, arr ){
		         arr.clear();
		    	 return true;
		     };

		     result = ArrayReject( nums, rejectFn );
		    """,
		    context );
		assertThat( variables.getAsArray( Key.of( "nums" ) ).size() ).isEqualTo( 0 );
	}

	@DisplayName( "It should remove values that the UDF returns true for" )
	@Test
	public void testRemovesFalseValues() {
		instance.executeSource(
		    """
		    	indexes = [];
		    	nums = [ 1, 2, 3, 4, 5 ];

		    	function rejectFn( value, i ){
		    		indexes[ i ] = value;
		    		return i != 3 && i != 5;
		    	};

		    	result = ArrayReject( nums, rejectFn );
		    """,
		    context );
		Array resultArray = ( Array ) variables.get( result );
		assertThat( resultArray.size() ).isEqualTo( 2 );
		assertThat( resultArray.get( 0 ) ).isEqualTo( 3 );
		assertThat( resultArray.get( 1 ) ).isEqualTo( 5 );
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
	public void testMemberFunction() {
		instance.executeSource(
		    """
		    	indexes = [];
		    	nums = [ 1, 2, 3, 4, 5 ];

		    	function rejectFn( value, i ){
		    		indexes[ i ] = value;
		    		return i != 3 && i != 5;
		    	};

		    	result = nums.reject( rejectFn );
		    """,
		    context );
		Array resultArray = ( Array ) variables.get( result );
		assertThat( resultArray.size() ).isEqualTo( 2 );
		assertThat( resultArray.get( 0 ) ).isEqualTo( 3 );
		assertThat( resultArray.get( 1 ) ).isEqualTo( 5 );
		Array indexes = ( Array ) variables.get( Key.of( "indexes" ) );
		assertThat( indexes.size() ).isEqualTo( 5 );
		assertThat( indexes.get( 0 ) ).isEqualTo( 1 );
		assertThat( indexes.get( 1 ) ).isEqualTo( 2 );
		assertThat( indexes.get( 2 ) ).isEqualTo( 3 );
		assertThat( indexes.get( 3 ) ).isEqualTo( 4 );
		assertThat( indexes.get( 4 ) ).isEqualTo( 5 );
	}

	@DisplayName( "It should execute the reject in parallel - with the default max threads" )
	@Test
	public void testParallelMemberFunction() {
		instance.executeSource(
		    """
		    	indexes = [];
		    	nums = [ 1, 2, 3, 4, 5 ];

		    	function rejectFn( value, i ){
		    		indexes[ i ] = value;
		    		return i != 3 && i != 5;
		    	};

		    	result = nums.reject( rejectFn, true );
		    """,
		    context );
		Array resultArray = ( Array ) variables.get( result );
		assertThat( resultArray.size() ).isEqualTo( 2 );
		assertThat( resultArray.get( 0 ) ).isEqualTo( 3 );
		assertThat( resultArray.get( 1 ) ).isEqualTo( 5 );
		Array indexes = ( Array ) variables.get( Key.of( "indexes" ) );
		assertThat( indexes.size() ).isEqualTo( 5 );
		assertThat( indexes.get( 0 ) ).isEqualTo( 1 );
		assertThat( indexes.get( 1 ) ).isEqualTo( 2 );
		assertThat( indexes.get( 2 ) ).isEqualTo( 3 );
		assertThat( indexes.get( 3 ) ).isEqualTo( 4 );
		assertThat( indexes.get( 4 ) ).isEqualTo( 5 );
	}

	@DisplayName( "It should execute the reject in parallel - a specified max threads" )
	@Test
	public void testParallelMaxThreadMemberFunction() {
		instance.executeSource(
		    """
		    	indexes = [];
		    	nums = [ 1, 2, 3, 4, 5 ];

		    	function rejectFn( value, i ){
		    		indexes[ i ] = value;
		    		return i != 3 && i != 5;
		    	};

		    	result = nums.reject( rejectFn, true, 5 );
		    """,
		    context );
		Array resultArray = ( Array ) variables.get( result );
		assertThat( resultArray.size() ).isEqualTo( 2 );
		assertThat( resultArray.get( 0 ) ).isEqualTo( 3 );
		assertThat( resultArray.get( 1 ) ).isEqualTo( 5 );
		Array indexes = ( Array ) variables.get( Key.of( "indexes" ) );
		assertThat( indexes.size() ).isEqualTo( 5 );
		assertThat( indexes.get( 0 ) ).isEqualTo( 1 );
		assertThat( indexes.get( 1 ) ).isEqualTo( 2 );
		assertThat( indexes.get( 2 ) ).isEqualTo( 3 );
		assertThat( indexes.get( 3 ) ).isEqualTo( 4 );
		assertThat( indexes.get( 4 ) ).isEqualTo( 5 );
	}

	@DisplayName( "It should execute the reject in parallel using virtual threads" )
	@Test
	public void testParallelVirtualMemberFunction() {
		instance.executeSource(
		    """
		    	indexes = [];
		    	nums = [ 1, 2, 3, 4, 5 ];

		    	function rejectFn( value, i ){
		    		indexes[ i ] = value;
		    		return i != 3 && i != 5;
		    	};

		    	result = nums.reject( callback=rejectFn, parallel=true, virtual=true );
		    """,
		    context );
		Array resultArray = ( Array ) variables.get( result );
		assertThat( resultArray.size() ).isEqualTo( 2 );
		assertThat( resultArray.get( 0 ) ).isEqualTo( 3 );
		assertThat( resultArray.get( 1 ) ).isEqualTo( 5 );
		Array indexes = ( Array ) variables.get( Key.of( "indexes" ) );
		assertThat( indexes.size() ).isEqualTo( 5 );
		assertThat( indexes.get( 0 ) ).isEqualTo( 1 );
		assertThat( indexes.get( 1 ) ).isEqualTo( 2 );
		assertThat( indexes.get( 2 ) ).isEqualTo( 3 );
		assertThat( indexes.get( 3 ) ).isEqualTo( 4 );
		assertThat( indexes.get( 4 ) ).isEqualTo( 5 );
	}

	@DisplayName( "It should execute the reject in parallel using virtual threads and the optional positional virtual arg" )
	@Test
	public void testParallelAltVirtualMemberFunction() {
		instance.executeSource(
		    """
		    	indexes = [];
		    	nums = [ 1, 2, 3, 4, 5 ];

		    	function rejectFn( value, i ){
		    		indexes[ i ] = value;
		    		return i != 3 && i != 5;
		    	};

		    	result = nums.reject( rejectFn, true, true );
		    """,
		    context );
		Array resultArray = ( Array ) variables.get( result );
		assertThat( resultArray.size() ).isEqualTo( 2 );
		assertThat( resultArray.get( 0 ) ).isEqualTo( 3 );
		assertThat( resultArray.get( 1 ) ).isEqualTo( 5 );
		Array indexes = ( Array ) variables.get( Key.of( "indexes" ) );
		assertThat( indexes.size() ).isEqualTo( 5 );
		assertThat( indexes.get( 0 ) ).isEqualTo( 1 );
		assertThat( indexes.get( 1 ) ).isEqualTo( 2 );
		assertThat( indexes.get( 2 ) ).isEqualTo( 3 );
		assertThat( indexes.get( 3 ) ).isEqualTo( 4 );
		assertThat( indexes.get( 4 ) ).isEqualTo( 5 );
	}

	@DisplayName( "It should filter an unmodifiable array to a modifiable array" )
	@Test
	public void testUnmodifiableArrayFiltering() {
		instance.executeSource(
		    """
		    		  nums = [ "red", "blue", "green" ].toUnmodifiable();

		    		  result = nums.reject( (i)->i != 'red' );
		    """,
		    context );
		assertThat( variables.get( result ) ).isInstanceOf( Array.class );
		assertThat( variables.get( result ) ).isNotInstanceOf( UnmodifiableArray.class );
		Array res = ( Array ) variables.get( result );
		assertThat( res.size() ).isEqualTo( 1 );
		assertThat( res.get( 0 ) ).isEqualTo( "red" );
	}
}
