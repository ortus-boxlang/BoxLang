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
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.LocalScope;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Function.Access;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.SampleUDF;
import ortus.boxlang.runtime.types.exceptions.BoxLangException;

public class ObjectDestructuringAssignmentTest {

	static BoxRuntime	instance;
	IBoxContext			context;
	IScope				variables;

	@BeforeAll
	public static void setUp() {
		instance = BoxRuntime.getInstance( true );
	}

	@BeforeEach
	public void setupEach() {
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
	}

	@DisplayName( "basic object destructuring assignment" )
	@Test
	public void testBasicObjectDestructuringAssignment() {
		instance.executeSource(
		    """
		    data = { a: 10, b: 20 };
		    ({ a, b } = data);
		    """,
		    context );

		assertThat( variables.get( Key.of( "a" ) ) ).isEqualTo( 10 );
		assertThat( variables.get( Key.of( "b" ) ) ).isEqualTo( 20 );
	}

	@DisplayName( "scoped object destructuring assignment" )
	@Test
	public void testScopedObjectDestructuringAssignment() {
		FunctionBoxContext functionContext = new FunctionBoxContext( context,
		    new SampleUDF( Access.PUBLIC, Key.of( "func" ), "any", new Argument[] {}, "" ) );
		instance.executeSource(
		    """
		    data = { a: 10, b: 20 };
		    ({ a: variables.a, b: arguments.b } = data);
		    """,
		    functionContext );

		IScope	functionVariables	= functionContext.getScopeNearby( VariablesScope.name );
		IScope	arguments			= functionContext.getScopeNearby( ArgumentsScope.name );
		assertThat( functionVariables.get( Key.of( "a" ) ) ).isEqualTo( 10 );
		assertThat( arguments.get( Key.of( "b" ) ) ).isEqualTo( 20 );
	}

	@DisplayName( "defaults and missing keys in object destructuring assignment" )
	@Test
	public void testDefaultsAndMissingKeysObjectDestructuringAssignment() {
		instance.executeSource(
		    """
		    data = { a: 10 };
		    ({ a, b = 20, c } = data);
		    """,
		    context );

		assertThat( variables.get( Key.of( "a" ) ) ).isEqualTo( 10 );
		assertThat( variables.get( Key.of( "b" ) ) ).isEqualTo( 20 );
		assertThat( variables.get( Key.of( "c" ) ) ).isNull();
	}

	@DisplayName( "nested object destructuring assignment" )
	@Test
	public void testNestedObjectDestructuringAssignment() {
		instance.executeSource(
		    """
		    data = { c: { d: 40, e: 50 } };
		    ({ c: { d, e } } = data);
		    """,
		    context );

		assertThat( variables.get( Key.of( "d" ) ) ).isEqualTo( 40 );
		assertThat( variables.get( Key.of( "e" ) ) ).isEqualTo( 50 );
	}

	@DisplayName( "object destructuring rest assignment" )
	@Test
	public void testObjectDestructuringRestAssignment() {
		instance.executeSource(
		    """
		    data = { a: 1, b: 2, c: 3 };
		    ({ a, ...rest } = data);
		    """,
		    context );

		assertThat( variables.get( Key.of( "a" ) ) ).isEqualTo( 1 );
		assertThat( variables.get( Key.of( "rest" ) ) instanceof IStruct ).isTrue();
		IStruct rest = ( IStruct ) variables.get( Key.of( "rest" ) );
		assertThat( rest.get( Key.of( "b" ) ) ).isEqualTo( 2 );
		assertThat( rest.get( Key.of( "c" ) ) ).isEqualTo( 3 );
	}

	@DisplayName( "var declaration object destructuring targets local scope" )
	@Test
	public void testVarDeclarationObjectDestructuringTargetsLocalScope() {
		FunctionBoxContext functionContext = new FunctionBoxContext( context,
		    new SampleUDF( Access.PUBLIC, Key.of( "func" ), "any", new Argument[] {}, "" ) );
		instance.executeSource(
		    """
		    data = { a: 1, b: 2 };
		    var { a, b } = data;
		    """,
		    functionContext );

		IScope local = functionContext.getScopeNearby( LocalScope.name );
		assertThat( local.get( Key.of( "a" ) ) ).isEqualTo( 1 );
		assertThat( local.get( Key.of( "b" ) ) ).isEqualTo( 2 );
	}

	@DisplayName( "var declaration object destructuring supports shorthand defaults in function scope" )
	@Test
	public void testVarDeclarationObjectDestructuringSupportsShorthandDefaultsInFunctionScope() {
		instance.executeSource(
		    """
		    function drawChart( struct options = {} ) {
		    	var { size = "big", coords = { x: 0, y: 0 }, radius = 25 } = options;
		    	return { size: size, x: coords.x, y: coords.y, radius: radius };
		    }

		    result1 = drawChart( { coords: { x: 18, y: 30 }, radius: 30 } );
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

	@DisplayName( "scoped targets are disallowed for declaration destructuring" )
	@Test
	public void testScopedTargetsDisallowedForDeclarationDestructuring() {
		assertThrows( BoxLangException.class, () -> instance.executeSource(
		    """
		    data = { a: 1 };
		    var { a: variables.a } = data;
		    """,
		    context ) );
	}

	@DisplayName( "scoped targets are disallowed for final declaration destructuring" )
	@Test
	public void testScopedTargetsDisallowedForFinalDeclarationDestructuring() {
		assertThrows( BoxLangException.class, () -> instance.executeSource(
		    """
		    data = { a: 1 };
		    final { a: variables.a } = data;
		    """,
		    context ) );
	}

	@DisplayName( "shorthand scoped target syntax is invalid in object destructuring" )
	@Test
	public void testShorthandScopedTargetSyntaxIsInvalidInObjectDestructuring() {
		assertThrows( BoxLangException.class, () -> instance.executeSource(
		    """
		    data = { a: 1 };
		    ({ variables.a } = data);
		    """,
		    context ) );
	}

	@DisplayName( "string keys with spaces must be renamed in object destructuring shorthand" )
	@Test
	public void testStringKeysWithSpacesMustBeRenamedInObjectDestructuringShorthand() {
		Throwable t = assertThrows( BoxLangException.class, () -> instance.executeSource(
		    """
		    test = { "key with spaces": 1 };
		    var { "key with spaces" } = test;
		    """,
		    context ) );

		assertThat( t.getMessage() ).contains( "cannot use shorthand" );
		assertThat( t.getMessage() ).contains( "Use an explicit binding" );
	}

	@DisplayName( "numeric keys must be renamed in object destructuring shorthand" )
	@Test
	public void testNumericKeysMustBeRenamedInObjectDestructuringShorthand() {
		Throwable t = assertThrows( BoxLangException.class, () -> instance.executeSource(
		    """
		    test = { 123: 1 };
		    ({ 123 } = test);
		    """,
		    context ) );

		assertThat( t.getMessage() ).contains( "cannot use shorthand" );
		assertThat( t.getMessage() ).contains( "Use an explicit binding" );
	}

	@DisplayName( "non declaration object destructuring must be parenthesized" )
	@Test
	public void testNonDeclarationObjectDestructuringMustBeParenthesized() {
		Throwable t = assertThrows( BoxLangException.class, () -> instance.executeSource(
		    """
		    { "key with spaces" } = test;
		    """,
		    context ) );

		assertThat( t.getMessage() ).contains( "must be wrapped in parentheses" );
		assertThat( t.getMessage() ).contains( "({ a } = source)" );
	}

	@DisplayName( "non struct rhs throws in object destructuring assignment" )
	@Test
	public void testNonStructRhsThrowsInObjectDestructuringAssignment() {
		assertThrows( BoxLangException.class, () -> instance.executeSource(
		    """
		    ({ a } = 42);
		    """,
		    context ) );
	}

	@DisplayName( "nested non struct value throws in object destructuring assignment" )
	@Test
	public void testNestedNonStructValueThrowsInObjectDestructuringAssignment() {
		assertThrows( BoxLangException.class, () -> instance.executeSource(
		    """
		    data = { c: 1 };
		    ({ c: { d } } = data);
		    """,
		    context ) );
	}
}
