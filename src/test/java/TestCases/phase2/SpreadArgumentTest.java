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
package TestCases.phase2;

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

/**
 * Tests for the spread operator ({@code ...}) in function call arguments.
 * <p>
 * The spread operator allows arrays and structs to be expanded into function
 * arguments. Under the hood it desugars to the {@code argumentCollection}
 * named-argument convention already supported by the BoxLang runtime.
 *
 * <pre>{@code
 * // Spread an array as positional arguments
 * args = [ 1, 2, 3 ];
 * result = myFunc( ...args );
 *
 * // Spread a struct as named arguments
 * opts = { name: "BoxLang", version: 1 };
 * result = myFunc( ...opts );
 * }</pre>
 */
public class SpreadArgumentTest {

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

	@DisplayName( "spread array into positional arguments" )
	@Test
	public void testSpreadArrayPositional() {
		instance.executeSource(
		    """
		    function add( a, b, c ) {
		    	return a + b + c;
		    }
		    args = [ 1, 2, 3 ];
		    result = add( ...args );
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 6 );
	}

	@DisplayName( "spread struct into named arguments" )
	@Test
	public void testSpreadStructNamed() {
		instance.executeSource(
		    """
		    function greet( first, last ) {
		    	return "Hello, " & first & " " & last;
		    }
		    person = { first: "John", last: "Doe" };
		    result = greet( ...person );
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "Hello, John Doe" );
	}

	@DisplayName( "spread array into BIF" )
	@Test
	public void testSpreadArrayIntoBIF() {
		instance.executeSource(
		    """
		    args = { string1: "abc", string2: "abc" };
		    result = compare( ...args );
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 0 );
	}

	@DisplayName( "spread struct into BIF" )
	@Test
	public void testSpreadStructIntoBIF() {
		instance.executeSource(
		    """
		    args = { string1: "hello", string2: "hello" };
		    result = compare( ...args );
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 0 );
	}

	@DisplayName( "spread inline array literal" )
	@Test
	public void testSpreadInlineArrayLiteral() {
		instance.executeSource(
		    """
		    function add( a, b, c ) {
		    	return a + b + c;
		    }
		    result = add( ...[ 10, 20, 30 ] );
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 60 );
	}

	@DisplayName( "spread inline struct literal" )
	@Test
	public void testSpreadInlineStructLiteral() {
		instance.executeSource(
		    """
		    function greet( first, last ) {
		    	return first & " " & last;
		    }
		    result = greet( ...{ first: "Jane", last: "Smith" } );
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "Jane Smith" );
	}

	@DisplayName( "spread into method call" )
	@Test
	public void testSpreadIntoMethodCall() {
		instance.executeSource(
		    """
		    str = "Hello World";
		    args = { delimiter: " " };
		    result = str.listToArray( ...args );
		    """,
		    context );
		Object res = variables.get( result );
		assertThat( res ).isInstanceOf( Array.class );
		Array arr = ( Array ) res;
		assertThat( arr.size() ).isEqualTo( 2 );
		assertThat( arr.get( 0 ) ).isEqualTo( "Hello" );
	}

	@DisplayName( "spread array with more args than params uses extra as positional" )
	@Test
	public void testSpreadArrayExtraArgs() {
		instance.executeSource(
		    """
		    function sum() {
		    	var total = 0;
		    	for( var arg in arguments ) {
		    		total += arg;
		    	}
		    	return total;
		    }
		    args = [ 1, 2, 3, 4, 5 ];
		    result = sum( ...args );
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 15 );
	}

	@DisplayName( "spread empty array passes no arguments" )
	@Test
	public void testSpreadEmptyArray() {
		instance.executeSource(
		    """
		    function noArgs() {
		    	return "ok";
		    }
		    args = [];
		    result = noArgs( ...args );
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "ok" );
	}

	@DisplayName( "spread empty struct passes no arguments" )
	@Test
	public void testSpreadEmptyStruct() {
		instance.executeSource(
		    """
		    function noArgs() {
		    	return "ok";
		    }
		    args = {};
		    result = noArgs( ...args );
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "ok" );
	}

	@DisplayName( "spread with closure" )
	@Test
	public void testSpreadWithClosure() {
		instance.executeSource(
		    """
		    myClosure = ( a, b ) => a & b;
		    args = [ "hello", "world" ];
		    result = myClosure( ...args );
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "helloworld" );
	}

	@DisplayName( "spread with lambda" )
	@Test
	public void testSpreadWithLambda() {
		instance.executeSource(
		    """
		    myLambda = function( a, b ) { return a - b; };
		    args = [ 100, 58 ];
		    result = myLambda( ...args );
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 42 );
	}

	@DisplayName( "spread struct into constructor (new)" )
	@Test
	public void testSpreadIntoConstructor() {
		instance.executeSource(
		    """
		    args = { string1: "hello", string2: "hello" };
		    result = compare( ...args );
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 0 );
	}

	@DisplayName( "spread with function expression result" )
	@Test
	public void testSpreadWithExpressionInvocation() {
		instance.executeSource(
		    """
		    function getAdder() {
		    	return ( a, b ) => a + b;
		    }
		    args = [ 3, 4 ];
		    result = getAdder()( ...args );
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 7 );
	}

	@DisplayName( "spread struct into expression invocation with named arguments" )
	@Test
	public void testSpreadStructIntoExpressionInvocation() {
		instance.executeSource(
		    """
		    function getGreeter() {
		    	return ( first, last ) => "Hi, " & first & " " & last;
		    }
		    args = { first: "Jane", last: "Doe" };
		    result = getGreeter()( ...args );
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "Hi, Jane Doe" );
	}
}
