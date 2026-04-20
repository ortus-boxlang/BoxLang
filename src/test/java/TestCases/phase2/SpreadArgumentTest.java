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
import ortus.boxlang.runtime.types.IStruct;

/**
 * Tests for the spread operator ({@code ...}) in function call arguments.
 * <p>
 * Spread combinations are tested via {@code BoxFunctionInvocation} (the simplest path).
 * Each additional AST node type ({@code BoxMethodInvocation}, {@code BoxStaticMethodInvocation},
 * {@code BoxNew}, {@code BoxExpressionInvocation}, {@code BoxFunctionalMemberAccess}, and BIFs)
 * gets smoke tests to verify spread reaches each code path.
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

	// =========================================================================
	// BoxFunctionInvocation: spread combinations (all combo tests here)
	// =========================================================================

	@DisplayName( "single spread array positional" )
	@Test
	public void testSingleSpreadArrayPositional() {
		instance.executeSource(
		    """
		    function add( a, b, c ) { return a + b + c; }
		    result = add( ...[ 1, 2, 3 ] );
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 6 );
	}

	@DisplayName( "single spread struct named" )
	@Test
	public void testSingleSpreadStructNamed() {
		instance.executeSource(
		    """
		    function greet( first, last ) { return first & " " & last; }
		    result = greet( ...{ first: "John", last: "Doe" } );
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "John Doe" );
	}

	@DisplayName( "spread empty array passes no arguments" )
	@Test
	public void testSpreadEmptyArray() {
		instance.executeSource(
		    """
		    function noArgs() { return "ok"; }
		    result = noArgs( ...[] );
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "ok" );
	}

	@DisplayName( "spread empty struct passes no arguments" )
	@Test
	public void testSpreadEmptyStruct() {
		instance.executeSource(
		    """
		    function noArgs() { return "ok"; }
		    result = noArgs( ...{} );
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "ok" );
	}

	@DisplayName( "explicit arg before spread positional" )
	@Test
	public void testExplicitBeforeSpreadPositional() {
		instance.executeSource(
		    """
		    function foo() { return arguments.asArray(); }
		    result = foo( "a", ...[ "b", "c" ] );
		    """,
		    context );
		Array arr = ( Array ) variables.get( result );
		assertThat( arr.size() ).isEqualTo( 3 );
		assertThat( arr.get( 0 ) ).isEqualTo( "a" );
		assertThat( arr.get( 1 ) ).isEqualTo( "b" );
		assertThat( arr.get( 2 ) ).isEqualTo( "c" );
	}

	@DisplayName( "explicit arg after spread positional" )
	@Test
	public void testExplicitAfterSpreadPositional() {
		instance.executeSource(
		    """
		    function foo() { return arguments.asArray(); }
		    result = foo( ...[ "a", "b" ], "c" );
		    """,
		    context );
		Array arr = ( Array ) variables.get( result );
		assertThat( arr.size() ).isEqualTo( 3 );
		assertThat( arr.get( 0 ) ).isEqualTo( "a" );
		assertThat( arr.get( 1 ) ).isEqualTo( "b" );
		assertThat( arr.get( 2 ) ).isEqualTo( "c" );
	}

	@DisplayName( "explicit args before and after spread positional" )
	@Test
	public void testExplicitBeforeAndAfterSpreadPositional() {
		instance.executeSource(
		    """
		    function foo() { return arguments.asArray(); }
		    result = foo( "a", ...[ "b", "c" ], "d" );
		    """,
		    context );
		Array arr = ( Array ) variables.get( result );
		assertThat( arr.size() ).isEqualTo( 4 );
		assertThat( arr.get( 0 ) ).isEqualTo( "a" );
		assertThat( arr.get( 1 ) ).isEqualTo( "b" );
		assertThat( arr.get( 2 ) ).isEqualTo( "c" );
		assertThat( arr.get( 3 ) ).isEqualTo( "d" );
	}

	@DisplayName( "explicit named args before and after spread struct" )
	@Test
	public void testExplicitNamedBeforeAndAfterSpreadStruct() {
		instance.executeSource(
		    """
		    function foo() { return arguments; }
		    result = foo( name="brad", ...{ foo: "bar", baz: "bum" }, age=46 );
		    """,
		    context );
		IStruct s = ( IStruct ) variables.get( result );
		assertThat( s.get( "name" ) ).isEqualTo( "brad" );
		assertThat( s.get( "foo" ) ).isEqualTo( "bar" );
		assertThat( s.get( "baz" ) ).isEqualTo( "bum" );
		assertThat( s.get( "age" ) ).isEqualTo( 46 );
	}

	@DisplayName( "multiple spread arrays positional" )
	@Test
	public void testMultipleSpreadArraysPositional() {
		instance.executeSource(
		    """
		    function foo() { return arguments.asArray(); }
		    result = foo( ...[ 1, 2 ], ...[ 3, 4 ] );
		    """,
		    context );
		Array arr = ( Array ) variables.get( result );
		assertThat( arr.size() ).isEqualTo( 4 );
		assertThat( arr.get( 0 ) ).isEqualTo( 1 );
		assertThat( arr.get( 3 ) ).isEqualTo( 4 );
	}

	@DisplayName( "multiple spread structs named" )
	@Test
	public void testMultipleSpreadStructsNamed() {
		instance.executeSource(
		    """
		    function foo() { return arguments; }
		    result = foo( ...{ a: 1 }, ...{ b: 2 }, ...{ c: 3 } );
		    """,
		    context );
		IStruct s = ( IStruct ) variables.get( result );
		assertThat( s.get( "a" ) ).isEqualTo( 1 );
		assertThat( s.get( "b" ) ).isEqualTo( 2 );
		assertThat( s.get( "c" ) ).isEqualTo( 3 );
	}

	@DisplayName( "multiple spreads with explicit args interspersed positional" )
	@Test
	public void testMultipleSpreadsInterspersedPositional() {
		instance.executeSource(
		    """
		    function foo() { return arguments.asArray(); }
		    result = foo( 1, ...[ 2, 3 ], 4, ...[ 5, 6 ], 7 );
		    """,
		    context );
		Array arr = ( Array ) variables.get( result );
		assertThat( arr.size() ).isEqualTo( 7 );
		for ( int i = 0; i < 7; i++ ) {
			assertThat( arr.get( i ) ).isEqualTo( i + 1 );
		}
	}

	@DisplayName( "multiple spreads with explicit args interspersed named" )
	@Test
	public void testMultipleSpreadsInterspersedNamed() {
		instance.executeSource(
		    """
		    function foo() { return arguments; }
		    result = foo( a=1, ...{ b: 2 }, c=3, ...{ d: 4 }, e=5 );
		    """,
		    context );
		IStruct s = ( IStruct ) variables.get( result );
		assertThat( s.get( "a" ) ).isEqualTo( 1 );
		assertThat( s.get( "b" ) ).isEqualTo( 2 );
		assertThat( s.get( "c" ) ).isEqualTo( 3 );
		assertThat( s.get( "d" ) ).isEqualTo( 4 );
		assertThat( s.get( "e" ) ).isEqualTo( 5 );
	}

	@DisplayName( "empty spreads among explicit args" )
	@Test
	public void testEmptySpreadsAmongExplicitArgs() {
		instance.executeSource(
		    """
		    function foo() { return arguments.asArray(); }
		    result = foo( 1, ...[], 2, ...[], 3 );
		    """,
		    context );
		Array arr = ( Array ) variables.get( result );
		assertThat( arr.size() ).isEqualTo( 3 );
	}

	@DisplayName( "later spread overrides earlier named arg" )
	@Test
	public void testLaterSpreadOverridesNamedArg() {
		instance.executeSource(
		    """
		    function foo() { return arguments; }
		    result = foo( name="original", ...{ name: "overridden" } );
		    """,
		    context );
		IStruct s = ( IStruct ) variables.get( result );
		assertThat( s.get( "name" ) ).isEqualTo( "overridden" );
	}

	@DisplayName( "later named arg overrides earlier spread" )
	@Test
	public void testLaterNamedArgOverridesSpread() {
		instance.executeSource(
		    """
		    function foo() { return arguments; }
		    result = foo( ...{ name: "original" }, name="overridden" );
		    """,
		    context );
		IStruct s = ( IStruct ) variables.get( result );
		assertThat( s.get( "name" ) ).isEqualTo( "overridden" );
	}

	@DisplayName( "spread result of function call" )
	@Test
	public void testSpreadResultOfFunctionCall() {
		instance.executeSource(
		    """
		    function getArgs() { return [ 10, 20, 30 ]; }
		    function sum( a, b, c ) { return a + b + c; }
		    result = sum( ...getArgs() );
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 60 );
	}

	@DisplayName( "spread result of method call" )
	@Test
	public void testSpreadResultOfMethodCall() {
		instance.executeSource(
		    """
		    obj = { getArgs: () => { first: "Brad", last: "Wood" } };
		    function greet( first, last ) { return first & " " & last; }
		    result = greet( ...obj.getArgs() );
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "Brad Wood" );
	}

	@DisplayName( "spread variadic args" )
	@Test
	public void testSpreadVariadicArgs() {
		instance.executeSource(
		    """
		    function sum() {
		    	var total = 0;
		    	for ( var arg in arguments ) { total += arg; }
		    	return total;
		    }
		    result = sum( ...[ 1, 2, 3, 4, 5 ] );
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 15 );
	}

	// =========================================================================
	// BoxMethodInvocation: obj.method( ...args )
	// =========================================================================

	@DisplayName( "method: single spread" )
	@Test
	public void testMethodSingleSpread() {
		instance.executeSource(
		    """
		    str = "a-b-c";
		    result = str.listToArray( ...[ "-" ] );
		    """,
		    context );
		Array arr = ( Array ) variables.get( result );
		assertThat( arr.size() ).isEqualTo( 3 );
	}

	@DisplayName( "method: spread with explicit args before and after" )
	@Test
	public void testMethodSpreadWithExplicitArgs() {
		instance.executeSource(
		    """
		    str = "hello world boxlang rocks";
		    result = str.reReplace( regex="world", ...{ substring: "BL" }, count=1 );
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "hello BL boxlang rocks" );
	}

	@DisplayName( "method: multiple spreads" )
	@Test
	public void testMethodMultipleSpreads() {
		instance.executeSource(
		    """
		    function foo() { return arguments.asArray(); }
		    obj = { foo: foo };
		    result = obj.foo( ...[ "a" ], ...[ "b" ], ...[ "c" ] );
		    """,
		    context );
		Array arr = ( Array ) variables.get( result );
		assertThat( arr.size() ).isEqualTo( 3 );
		assertThat( arr.get( 0 ) ).isEqualTo( "a" );
		assertThat( arr.get( 2 ) ).isEqualTo( "c" );
	}

	// =========================================================================
	// BoxStaticMethodInvocation: Class::method( ...args )
	// =========================================================================

	@DisplayName( "static: single spread" )
	@Test
	public void testStaticSingleSpread() {
		instance.executeSource(
		    """
		    import java.lang.Integer;
		    result = Integer::parseInt( ...[ "FF", 16 ] );
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 255 );
	}

	@DisplayName( "static: spread with explicit arg before" )
	@Test
	public void testStaticSpreadWithExplicitArgBefore() {
		instance.executeSource(
		    """
		    import java.lang.Integer;
		    result = Integer::parseInt( "A0", ...[ 16 ] );
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 160 );
	}

	@DisplayName( "static: multiple spreads" )
	@Test
	public void testStaticMultipleSpreads() {
		instance.executeSource(
		    """
		    import java.lang.Integer;
		    result = Integer::parseInt( ...[ "1F" ], ...[ 16 ] );
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 31 );
	}

	// =========================================================================
	// BoxNew: new Foo( ...args )
	// =========================================================================

	@DisplayName( "new: single spread into Java constructor" )
	@Test
	public void testNewSingleSpreadJava() {
		instance.executeSource(
		    """
		    import java.lang.String;
		    result = new String( ...[ "hello spread" ] );
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "hello spread" );
	}

	@DisplayName( "new: spread with explicit arg before into Java constructor" )
	@Test
	public void testNewSpreadWithExplicitArgBeforeJava() {
		instance.executeSource(
		    """
		    import java.lang.String;
		    result = new String( ...[ "spread constructor" ] );
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "spread constructor" );
	}

	@DisplayName( "new: single spread into BoxLang class constructor" )
	@Test
	public void testNewSingleSpreadBoxLang() {
		instance.executeSource(
		    """
		    obj = new src.test.java.TestCases.phase2.SpreadNewTest( ...{ first: "Brad", last: "Wood" } );
		    result = obj.getFirst() & " " & obj.getLast();
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "Brad Wood" );
	}

	@DisplayName( "new: spread with explicit args into BoxLang class constructor" )
	@Test
	public void testNewSpreadWithExplicitArgsBoxLang() {
		instance.executeSource(
		    """
		    obj = new src.test.java.TestCases.phase2.SpreadNewTest( first="Brad", ...{ last: "Wood" }, age=46 );
		    result = obj.getFirst() & " " & obj.getLast() & " " & obj.getAge();
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "Brad Wood 46" );
	}

	@DisplayName( "new: multiple spreads into constructor" )
	@Test
	public void testNewMultipleSpreads() {
		instance.executeSource(
		    """
		    import java.lang.String;
		    result = new String( ...[ "multi" ], ...[] );
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "multi" );
	}

	// =========================================================================
	// BoxExpressionInvocation: getCallback()( ...args )
	// =========================================================================

	@DisplayName( "expression invocation: single spread positional" )
	@Test
	public void testExpressionInvocationSingleSpread() {
		instance.executeSource(
		    """
		    function getAdder() { return ( a, b ) => a + b; }
		    result = getAdder()( ...[ 3, 4 ] );
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 7 );
	}

	@DisplayName( "expression invocation: single spread named" )
	@Test
	public void testExpressionInvocationSingleSpreadNamed() {
		instance.executeSource(
		    """
		    function getGreeter() { return ( first, last ) => first & " " & last; }
		    result = getGreeter()( ...{ first: "Jane", last: "Doe" } );
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "Jane Doe" );
	}

	@DisplayName( "expression invocation: spread with explicit args" )
	@Test
	public void testExpressionInvocationSpreadWithExplicitArgs() {
		instance.executeSource(
		    """
		    function getConcat() { return ( a, b, c ) => a & b & c; }
		    result = getConcat()( "X", ...[ "Y" ], "Z" );
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "XYZ" );
	}

	@DisplayName( "expression invocation: multiple spreads" )
	@Test
	public void testExpressionInvocationMultipleSpreads() {
		instance.executeSource(
		    """
		    function getConcat() { return ( a, b, c, d ) => a & b & c & d; }
		    result = getConcat()( ...[ "A", "B" ], ...[ "C", "D" ] );
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "ABCD" );
	}

	// =========================================================================
	// BoxFunctionalMemberAccess: .method( ...args )
	// =========================================================================

	@DisplayName( "functional member access: single spread" )
	@Test
	public void testFunctionalMemberAccessSingleSpread() {
		instance.executeSource(
		    """
		    result = [ "hello", "world" ].map( .ucase( ...[] ) );
		    """,
		    context );
		Array arr = ( Array ) variables.get( result );
		assertThat( arr.get( 0 ) ).isEqualTo( "HELLO" );
		assertThat( arr.get( 1 ) ).isEqualTo( "WORLD" );
	}

	// =========================================================================
	// BIF: compare( ...args ) — BIFs have their own dispatch path
	// =========================================================================

	@DisplayName( "BIF: single spread named" )
	@Test
	public void testBIFSingleSpreadNamed() {
		instance.executeSource(
		    """
		    result = compare( ...{ string1: "abc", string2: "abc" } );
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 0 );
	}

	@DisplayName( "BIF: single spread positional" )
	@Test
	public void testBIFSingleSpreadPositional() {
		instance.executeSource(
		    """
		    result = compare( ...[ "abc", "abc" ] );
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 0 );
	}

	@DisplayName( "BIF: spread with explicit args before and after" )
	@Test
	public void testBIFSpreadWithExplicitArgs() {
		instance.executeSource(
		    """
		    result = listAppend( list="a,b", ...{ value: "c" }, delimiter="," );
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "a,b,c" );
	}

	@DisplayName( "BIF: multiple spreads" )
	@Test
	public void testBIFMultipleSpreads() {
		instance.executeSource(
		    """
		    result = compare( ...{ string1: "abc" }, ...{ string2: "abc" } );
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 0 );
	}

	@DisplayName( "BIF: spread positional with explicit args" )
	@Test
	public void testBIFSpreadPositionalWithExplicitArgs() {
		instance.executeSource(
		    """
		    result = listAppend( "a", ...[ "b" ], "," );
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "a,b" );
	}
}
