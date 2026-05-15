/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package TestCases.phase1;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.FunctionBoxContext;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.runnables.RunnableLoader;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.LocalScope;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.Function.Access;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.SampleUDF;
import ortus.boxlang.runtime.types.exceptions.BoxLangException;

public class ArrayDestructuringAssignmentTest {

	static BoxRuntime	instance;
	IBoxContext			context;
	IScope				variables;

	@BeforeAll
	public static void setUp() {
		instance = BoxRuntime.getInstance( true );
	}

	@BeforeEach
	public void setupEach() {
		RunnableLoader.getInstance().getBoxpiler().clearPagePool();
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
	}

	private void executeSourceFresh( String source, IBoxContext targetContext ) {
		instance.executeSource( source + "\n// nonce: " + System.nanoTime(), targetContext );
	}

	@DisplayName( "basic array destructuring assignment" )
	@Test
	public void testBasicArrayDestructuringAssignment() {
		executeSourceFresh(
		    """
		    data = [ 10, 20 ];
		    [ a, b ] = data;
		    """,
		    context );

		assertThat( variables.get( Key.of( "a" ) ) ).isEqualTo( 10 );
		assertThat( variables.get( Key.of( "b" ) ) ).isEqualTo( 20 );
	}

	@DisplayName( "scoped array destructuring assignment" )
	@Test
	public void testScopedArrayDestructuringAssignment() {
		FunctionBoxContext functionContext = new FunctionBoxContext( context,
		    new SampleUDF( Access.PUBLIC, Key.of( "func" ), "any", new Argument[] {}, "" ) );
		executeSourceFresh(
		    """
		    data = [ 10, 20 ];
		    [ variables.a, arguments.b ] = data;
		    """,
		    functionContext );

		IScope	functionVariables	= functionContext.getScopeNearby( VariablesScope.name );
		IScope	arguments			= functionContext.getScopeNearby( ArgumentsScope.name );
		assertThat( functionVariables.get( Key.of( "a" ) ) ).isEqualTo( 10 );
		assertThat( arguments.get( Key.of( "b" ) ) ).isEqualTo( 20 );
	}

	@DisplayName( "defaults and missing indexes in array destructuring assignment" )
	@Test
	public void testDefaultsAndMissingIndexesArrayDestructuringAssignment() {
		executeSourceFresh(
		    """
		    data = [ 10 ];
		    [ a, b = 20, c ] = data;
		    """,
		    context );

		assertThat( variables.get( Key.of( "a" ) ) ).isEqualTo( 10 );
		assertThat( variables.get( Key.of( "b" ) ) ).isEqualTo( 20 );
		assertThat( variables.get( Key.of( "c" ) ) ).isNull();
	}

	@DisplayName( "nested array destructuring assignment" )
	@Test
	public void testNestedArrayDestructuringAssignment() {
		executeSourceFresh(
		    """
		    data = [ [ 40, 50 ] ];
		    [ [ d, e ] ] = data;
		    """,
		    context );

		assertThat( variables.get( Key.of( "d" ) ) ).isEqualTo( 40 );
		assertThat( variables.get( Key.of( "e" ) ) ).isEqualTo( 50 );
	}

	@DisplayName( "array destructuring rest assignment" )
	@Test
	public void testArrayDestructuringRestAssignment() {
		executeSourceFresh(
		    """
		    data = [ 1, 2, 3 ];
		    [ a, ...rest ] = data;
		    """,
		    context );

		assertThat( variables.get( Key.of( "a" ) ) ).isEqualTo( 1 );
		assertThat( variables.get( Key.of( "rest" ) ) instanceof Array ).isTrue();
		Array rest = ( Array ) variables.get( Key.of( "rest" ) );
		assertThat( rest.size() ).isEqualTo( 2 );
		assertThat( rest.getAt( 1 ) ).isEqualTo( 2 );
		assertThat( rest.getAt( 2 ) ).isEqualTo( 3 );
	}

	@DisplayName( "array destructuring rest assignment in the middle" )
	@Test
	public void testArrayDestructuringRestInMiddleAssignment() {
		executeSourceFresh(
		    """
		    numbers = [ 1, 2, 3, 4, 5, 6 ];
		    [ first, ...middle, last ] = numbers;
		    """,
		    context );

		assertThat( variables.get( Key.of( "first" ) ) ).isEqualTo( 1 );
		assertThat( variables.get( Key.of( "last" ) ) ).isEqualTo( 6 );
		assertThat( variables.get( Key.of( "middle" ) ) instanceof Array ).isTrue();
		Array middle = ( Array ) variables.get( Key.of( "middle" ) );
		assertThat( middle.size() ).isEqualTo( 4 );
		assertThat( middle.getAt( 1 ) ).isEqualTo( 2 );
		assertThat( middle.getAt( 2 ) ).isEqualTo( 3 );
		assertThat( middle.getAt( 3 ) ).isEqualTo( 4 );
		assertThat( middle.getAt( 4 ) ).isEqualTo( 5 );
	}

	@DisplayName( "array destructuring middle rest edge cases for short arrays" )
	@Test
	public void testArrayDestructuringMiddleRestShortArrayEdgeCases() {
		executeSourceFresh(
		    """
		    numbers = [ 1, 2 ];
		    [ first2, ...middle2, last2 ] = numbers;

		    numbers = [ 1 ];
		    [ first1, ...middle1, last1 ] = numbers;

		    numbers = [];
		    [ first0, ...middle0, last0 ] = numbers;
		    """,
		    context );

		assertThat( variables.get( Key.of( "first2" ) ) ).isEqualTo( 1 );
		assertThat( variables.get( Key.of( "last2" ) ) ).isEqualTo( 2 );
		assertThat( variables.get( Key.of( "middle2" ) ) instanceof Array ).isTrue();
		assertThat( ( ( Array ) variables.get( Key.of( "middle2" ) ) ).size() ).isEqualTo( 0 );

		assertThat( variables.get( Key.of( "first1" ) ) ).isEqualTo( 1 );
		assertThat( variables.get( Key.of( "last1" ) ) ).isNull();
		assertThat( variables.get( Key.of( "middle1" ) ) instanceof Array ).isTrue();
		assertThat( ( ( Array ) variables.get( Key.of( "middle1" ) ) ).size() ).isEqualTo( 0 );

		assertThat( variables.get( Key.of( "first0" ) ) ).isNull();
		assertThat( variables.get( Key.of( "last0" ) ) ).isNull();
		assertThat( variables.get( Key.of( "middle0" ) ) instanceof Array ).isTrue();
		assertThat( ( ( Array ) variables.get( Key.of( "middle0" ) ) ).size() ).isEqualTo( 0 );
	}

	@DisplayName( "array destructuring middle rest supports bindings on both sides" )
	@Test
	public void testArrayDestructuringMiddleRestSupportsBothSides() {
		executeSourceFresh(
		    """
		    numbers = [ 1, 2, 3, 4, 5, 6, 7, 8, 9 ];
		    [ first, second, ...middle, thirdLast, secondLast, last ] = numbers;
		    """,
		    context );

		assertThat( variables.get( Key.of( "first" ) ) ).isEqualTo( 1 );
		assertThat( variables.get( Key.of( "second" ) ) ).isEqualTo( 2 );
		assertThat( variables.get( Key.of( "thirdLast" ) ) ).isEqualTo( 7 );
		assertThat( variables.get( Key.of( "secondLast" ) ) ).isEqualTo( 8 );
		assertThat( variables.get( Key.of( "last" ) ) ).isEqualTo( 9 );
		assertThat( variables.get( Key.of( "middle" ) ) instanceof Array ).isTrue();
		Array middle = ( Array ) variables.get( Key.of( "middle" ) );
		assertThat( middle.size() ).isEqualTo( 4 );
		assertThat( middle.getAt( 1 ) ).isEqualTo( 3 );
		assertThat( middle.getAt( 2 ) ).isEqualTo( 4 );
		assertThat( middle.getAt( 3 ) ).isEqualTo( 5 );
		assertThat( middle.getAt( 4 ) ).isEqualTo( 6 );
	}

	@DisplayName( "array destructuring middle rest does not overlap left and right bindings" )
	@Test
	public void testArrayDestructuringMiddleRestNoOverlapOnShortInput() {
		executeSourceFresh(
		    """
		    numbers = [ 1, 2, 3 ];
		    [ first, second, ...middle, thirdLast, secondLast, last ] = numbers;
		    """,
		    context );

		assertThat( variables.get( Key.of( "first" ) ) ).isEqualTo( 1 );
		assertThat( variables.get( Key.of( "second" ) ) ).isEqualTo( 2 );
		assertThat( variables.get( Key.of( "thirdLast" ) ) ).isNull();
		assertThat( variables.get( Key.of( "secondLast" ) ) ).isNull();
		assertThat( variables.get( Key.of( "last" ) ) ).isEqualTo( 3 );
		assertThat( variables.get( Key.of( "middle" ) ) instanceof Array ).isTrue();
		assertThat( ( ( Array ) variables.get( Key.of( "middle" ) ) ).size() ).isEqualTo( 0 );
	}

	@DisplayName( "var declaration array destructuring targets local scope" )
	@Test
	public void testVarDeclarationArrayDestructuringTargetsLocalScope() {
		FunctionBoxContext functionContext = new FunctionBoxContext( context,
		    new SampleUDF( Access.PUBLIC, Key.of( "func" ), "any", new Argument[] {}, "" ) );
		executeSourceFresh(
		    """
		    data = [ 1, 2 ];
		    var [ a, b ] = data;
		    """,
		    functionContext );

		IScope local = functionContext.getScopeNearby( LocalScope.name );
		assertThat( local.get( Key.of( "a" ) ) ).isEqualTo( 1 );
		assertThat( local.get( Key.of( "b" ) ) ).isEqualTo( 2 );
	}

	@DisplayName( "var declaration array destructuring supports shorthand defaults in function scope" )
	@Test
	public void testVarDeclarationArrayDestructuringSupportsShorthandDefaultsInFunctionScope() {
		executeSourceFresh(
		    """
		    function drawChart( array options = [] ) {
		    	var [ size = "big", coords = [ 0, 0 ], radius = 25 ] = options;
		    	return { size: size, x: coords[ 1 ], y: coords[ 2 ], radius: radius };
		    }

		    result1 = drawChart( [ null, [ 18, 30 ], 30 ] );
		    result2 = drawChart();
		    """,
		    context );

		IStruct	result1	= ( IStruct ) variables.get( Key.of( "result1" ) );
		IStruct	result2	= ( IStruct ) variables.get( Key.of( "result2" ) );

		assertThat( result1.get( Key.of( "size" ) ) ).isEqualTo( "big" );
		assertThat( result1.get( Key.of( "x" ) ) ).isEqualTo( 18 );
		assertThat( result1.get( Key.of( "y" ) ) ).isEqualTo( 30 );
		assertThat( result1.get( Key.of( "radius" ) ) ).isEqualTo( 30 );

		assertThat( result2.get( Key.of( "size" ) ) ).isEqualTo( "big" );
		assertThat( result2.get( Key.of( "x" ) ) ).isEqualTo( 0 );
		assertThat( result2.get( Key.of( "y" ) ) ).isEqualTo( 0 );
		assertThat( result2.get( Key.of( "radius" ) ) ).isEqualTo( 25 );
	}

	@DisplayName( "scoped targets are disallowed for declaration array destructuring" )
	@Test
	public void testScopedTargetsDisallowedForDeclarationArrayDestructuring() {
		assertThrows( BoxLangException.class, () -> executeSourceFresh(
		    """
		    data = [ 1 ];
		    var [ variables.a ] = data;
		    """,
		    context ) );
	}

	@DisplayName( "scoped targets are disallowed for final declaration array destructuring" )
	@Test
	public void testScopedTargetsDisallowedForFinalDeclarationArrayDestructuring() {
		assertThrows( BoxLangException.class, () -> executeSourceFresh(
		    """
		    data = [ 1 ];
		    final [ variables.a ] = data;
		    """,
		    context ) );
	}

	@DisplayName( "non array rhs throws in array destructuring assignment" )
	@Test
	public void testNonArrayRhsThrowsInArrayDestructuringAssignment() {
		assertThrows( BoxLangException.class, () -> executeSourceFresh(
		    """
		    [ a ] = 42;
		    """,
		    context ) );
	}

	@DisplayName( "nested non array value throws in array destructuring assignment" )
	@Test
	public void testNestedNonArrayValueThrowsInArrayDestructuringAssignment() {
		assertThrows( BoxLangException.class, () -> executeSourceFresh(
		    """
		    data = [ 1 ];
		    [ [ d ] ] = data;
		    """,
		    context ) );
	}

	@DisplayName( "multiple array rest bindings are rejected" )
	@Test
	public void testMultipleArrayRestBindingsAreRejected() {
		assertThrows( BoxLangException.class, () -> executeSourceFresh(
		    """
		    data = [ 1, 2, 3 ];
		    [ a, ...rest1, ...rest2 ] = data;
		    """,
		    context ) );
	}
}
