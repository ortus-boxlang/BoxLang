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

import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.FunctionBoxContext;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.LocalScope;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.Function.Access;
import ortus.boxlang.runtime.types.Function.Argument;
import ortus.boxlang.runtime.types.SampleUDF;

public class ObjectReferenceAssignmentTest {

	static BoxRuntime	instance;
	static IBoxContext	context;
	static IScope		variables;
	static Key			result	= new Key( "result" );

	@BeforeAll
	public static void setUp() {
		instance	= BoxRuntime.getInstance( true );
		context		= new ScriptingBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
	}

	@AfterAll
	public static void teardown() {
		instance.shutdown();
	}

	@BeforeEach
	public void setupEach() {
		variables.clear();
	}

	@DisplayName( "scope assignment" )
	@Test
	public void testScopeAssignment() {
		instance.executeSource(
		    """
		    variables.result = "brad";
		    """,
		    context );
		assertThat( variables.dereference( result, false ) ).isEqualTo( "brad" );

		instance.executeSource(
		    """
		    variables['result'] = "wood";
		    """,
		    context );
		assertThat( variables.dereference( result, false ) ).isEqualTo( "wood" );

	}

	@DisplayName( "unscoped assignment" )
	@Test
	public void tesUnscopedAssignment() {
		instance.executeSource(
		    """
		    result = "brad";
		    """,
		    context );
		assertThat( variables.dereference( result, false ) ).isEqualTo( "brad" );

	}

	@DisplayName( "dereference scope key" )
	@Test
	public void tesDereferenceScopeKey() {
		instance.executeSource(
		    """
		    variables.foo = "luis";
		    result = variables.foo;
		    """,
		    context );
		assertThat( variables.dereference( result, false ) ).isEqualTo( "luis" );

		instance.executeSource(
		    """
		    variables.foo = "gavin";
		    result = variables['foo'];
		    """,
		    context );
		assertThat( variables.dereference( result, false ) ).isEqualTo( "gavin" );

	}

	@DisplayName( "dereference key" )
	@Test
	public void tesDereferenceKey() {
		// MT TODO: The new keyword doesn't work, so this test is failing
		instance.executeSource(
		    """
		       str = new java:ortus.boxlang.runtime.types.Struct();
		    str.name = "Brad"
		       result = str.name;
		       """,
		    context );
		assertThat( variables.dereference( result, false ) ).isEqualTo( "Brad" );

	}

	@DisplayName( "dereference headless" )
	@Test
	public void tesDereferenceHeadless() {
		// MT TODO: unscoped foo needs searched for in nearby scopes
		instance.executeSource(
		    """
		    variables.foo = 5;
		    result = foo;
		    """,
		    context );
		assertThat( variables.dereference( result, false ) ).isEqualTo( 5 );

	}

	@DisplayName( "safe navigation" )
	@Test
	public void testSafeNavigation() {
		instance.executeSource(
		    """
		    result = variables?.foo?.bar?.baz;
		    """,
		    context );
		assertThat( variables.dereference( result, false ) ).isEqualTo( null );

		Object theResult = instance.executeStatement( "variables?.foo?.bar?.baz", context );
		assertThat( theResult ).isEqualTo( null );

	}

	@DisplayName( "var keyword for local" )
	@Test
	public void testVarKeywordForLocal() {
		FunctionBoxContext functionBoxContext = new FunctionBoxContext( context,
		    new SampleUDF( Access.PUBLIC, Key.of( "func" ), "any", new Argument[] {}, "", false, "" ) );
		// MT TODO: Java source is trying to access localScope symbol, but it's never defined. Need to track what scopes are used
		instance.executeSource(
		    """
		    // Needs extranous `Key.of( LocalScope.name )` turned into just `LocalScope.name`
		    var foo = 5;
		    // Don't know why this fails
		    local.bar = 6;
		    """,
		    functionBoxContext );
		IScope localScope = functionBoxContext.getScopeNearby( LocalScope.name );
		assertThat( localScope.dereference( Key.of( "foo" ), false ) ).isEqualTo( 5 );
		assertThat( localScope.dereference( Key.of( "bar" ), false ) ).isEqualTo( 5 );

	}

	@DisplayName( "deep assignment" )
	@Test
	public void testDeepAssignment() {
		instance.executeSource(
		    """
		    variables.foo.bar.baz="brad"
		    """,
		    context );
		assertThat( variables.containsKey( Key.of( "foo" ) ) ).isEqualTo( true );
		Object foo = variables.get( Key.of( "foo" ) );
		assertThat( foo instanceof Map ).isEqualTo( true );
		assertThat( ( ( Map ) foo ).containsKey( Key.of( "bar" ) ) ).isEqualTo( true );
		Object bar = ( ( Map ) foo ).get( Key.of( "bar" ) );
		assertThat( bar instanceof Map ).isEqualTo( true );
		Object baz = ( ( Map ) bar ).get( Key.of( "baz" ) );
		assertThat( baz instanceof String ).isEqualTo( true );
	}

	@DisplayName( "assignment returns value" )
	@Test
	public void testAssignmentReturnsValue() {
		instance.executeSource(
		    """
		    foo = bar = brad = "wood"
		       """,
		    context );

		assertThat( variables.dereference( Key.of( "foo" ), false ) ).isEqualTo( "wood" );
		assertThat( variables.dereference( Key.of( "bar" ), false ) ).isEqualTo( "wood" );
		assertThat( variables.dereference( Key.of( "brad" ), false ) ).isEqualTo( "wood" );
	}

	@DisplayName( "scoped assignment returns value" )
	@Test
	public void testScopedAssignmentReturnsValue() {
		instance.executeSource(
		    """
		    variables.foo2 = variables.bar2 = variables.brad2 = "wood2"
		       """,
		    context );

		assertThat( variables.dereference( Key.of( "foo2" ), false ) ).isEqualTo( "wood2" );
		assertThat( variables.dereference( Key.of( "bar2" ), false ) ).isEqualTo( "wood2" );
		assertThat( variables.dereference( Key.of( "brad2" ), false ) ).isEqualTo( "wood2" );
	}

	@DisplayName( "call method on object" )
	@Test
	public void testCallMethodOnObject() {
		instance.executeSource(
		    """
		    system = createObject('java', 'java.lang.System');
		    system.out.println("Hello World");
		    """,
		    context );

	}

	@DisplayName( "null keyword" )
	@Test
	public void testNullKeyword() {
		instance.executeSource(
		    """
		    nothing = null;
		    result = nothing == null;
		    """,
		    context );
		assertThat( variables.dereference( result, false ) ).isEqualTo( true );

	}

}
