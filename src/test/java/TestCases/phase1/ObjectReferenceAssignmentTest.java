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

import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.FunctionBoxContext;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.LocalScope;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Function.Access;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.SampleUDF;
import ortus.boxlang.runtime.types.exceptions.BoxLangException;

public class ObjectReferenceAssignmentTest {

	static BoxRuntime	instance;
	static IBoxContext	context;
	static IScope		variables;
	static Key			result	= new Key( "result" );

	@BeforeAll
	public static void setUp() {
		instance	= BoxRuntime.getInstance( true );
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
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
		assertThat( variables.get( result ) ).isEqualTo( "brad" );

		instance.executeSource(
		    """
		    keyName = "result";
		    variables[keyName] = "wood";

		       """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "wood" );

		instance.executeSource(
		    """
		    variables['result'] = "luis";

		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "luis" );

	}

	@DisplayName( "unscoped assignment" )
	@Test
	public void testUnscopedAssignment() {
		instance.executeSource(
		    """
		    result = "brad";
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "brad" );

	}

	@DisplayName( "invaid assignment" )
	@Test
	public void testInvalidAssignment() {

		// These are invalid because they are trying to assign directly to an expression that can't be assigned
		assertThrows( BoxLangException.class, () -> instance.executeSource(
		    """
		    foo() = "brad";
		    """,
		    context ) );

		assertThrows( BoxLangException.class, () -> instance.executeSource(
		    """
		    obj.foo() = "brad";
		    """,
		    context ) );

		// These are all a BoxAccess, but the "var" keyword can only come before an INITIAL identifier or BoxAccess
		assertThrows( BoxLangException.class, () -> instance.executeSource(
		    """
		    var foo().key = "brad";
		    """,
		    context ) );

		assertThrows( BoxLangException.class, () -> instance.executeSource(
		    """
		    var obj.foo().key = "brad";
		    """,
		    context ) );

		assertThrows( BoxLangException.class, () -> instance.executeSource(
		    """
		    var "foo".key = "brad";
		    """,
		    context ) );

	}

	@DisplayName( "dereference scope key" )
	@Test
	public void testDereferenceScopeKey() {
		instance.executeSource(
		    """
		    variables.foo = "luis";
		    result = variables.foo;
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "luis" );

		instance.executeSource(
		    """
		    variables.foo = "gavin";
		    result = variables['foo'];
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "gavin" );

	}

	@DisplayName( "dereference key" )
	@Test
	public void testDereferenceKey() {
		instance.executeSource(
		    """
		    import java:ortus.boxlang.runtime.scopes.Key;
		         str = new java:ortus.boxlang.runtime.types.Struct();
		      str.assign( GetBoxContext(), Key.of("name"), "Brad" );
		         result = str.name;
		         """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "Brad" );

	}

	@DisplayName( "dereference headless" )
	@Test
	public void testDereferenceHeadless() {
		instance.executeSource(
		    """
		    variables.foo = 5;
		    result = foo;
		     """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 5 );

	}

	@DisplayName( "dereference invoke key" )
	@Test
	public void testDereferenceInvokeKey() {
		instance.executeSource(
		    """
		      ctx = new java:ortus.boxlang.runtime.context.ScriptingRequestBoxContext();
		    result = variables.ctx.getDefaultAssignmentScope()
		      """,
		    context );
		assertThat( variables.get( result ) instanceof IScope ).isTrue();

	}

	@DisplayName( "dereference invoke headless" )
	@Test
	public void testDereferenceInvokeHeadless() {
		instance.executeSource(
		    """
		      ctx = new java:ortus.boxlang.runtime.context.ScriptingRequestBoxContext();
		    result = ctx.getDefaultAssignmentScope()
		      """,
		    context );
		assertThat( variables.get( result ) instanceof IScope ).isTrue();
	}

	@DisplayName( "safe navigation" )
	@Test
	public void testSafeNavigation() {
		instance.executeSource(
		    """
		    result = variables?.foo?.bar?.baz;
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( null );

		Object theResult = instance.executeStatement( "variables?.foo?.bar?.baz", context );
		assertThat( theResult ).isEqualTo( null );

	}

	@DisplayName( "var keyword for local" )
	@Test
	public void testVarKeywordForLocal() {
		FunctionBoxContext functionBoxContext = new FunctionBoxContext( context,
		    new SampleUDF( Access.PUBLIC, Key.of( "func" ), "any", new Argument[] {}, "" ) );
		instance.executeSource(
		    """
		    var foo = 5;
		    local.bar = 6;
		    """,
		    functionBoxContext );

		IScope localScope = functionBoxContext.getScopeNearby( LocalScope.name );
		assertThat( localScope.get( Key.of( "foo" ) ) ).isEqualTo( 5 );
		assertThat( localScope.get( Key.of( "bar" ) ) ).isEqualTo( 6 );

	}

	@DisplayName( "var keyword for local Deep" )
	@Test
	public void testVarKeywordForLocalDeep() {
		FunctionBoxContext	functionBoxContext	= new FunctionBoxContext( context,
		    new SampleUDF( Access.PUBLIC, Key.of( "func" ), "any", new Argument[] {}, "" ) );
		IScope				localScope			= functionBoxContext.getScopeNearby( LocalScope.name );
		instance.executeSource(
		    """
		    var foo.bar = 5;
		    """,
		    functionBoxContext );
		assertThat( localScope.get( Key.of( "foo" ) ) instanceof IStruct ).isTrue();
		IStruct foo = ( IStruct ) localScope.get( Key.of( "foo" ) );
		assertThat( foo.get( Key.of( "bar" ) ) ).isEqualTo( 5 );

	}

	@DisplayName( "var keyword for scope" )
	@Test
	public void testVarKeywordForLocalForScope() {
		FunctionBoxContext	functionBoxContext	= new FunctionBoxContext( context,
		    new SampleUDF( Access.PUBLIC, Key.of( "func" ), "any", new Argument[] {}, "" ) );
		IScope				localScope			= functionBoxContext.getScopeNearby( LocalScope.name );
		instance.executeSource(
		    """
		    var variables = 5;
		    """,
		    functionBoxContext );
		assertThat( localScope.get( Key.of( "variables" ) ) ).isEqualTo( 5 );

	}

	@DisplayName( "var keyword for scope deep" )
	@Test
	public void testVarKeywordForLocalForScopeDeep() {
		FunctionBoxContext	functionBoxContext	= new FunctionBoxContext( context,
		    new SampleUDF( Access.PUBLIC, Key.of( "func" ), "any", new Argument[] {}, "" ) );
		IScope				localScope			= functionBoxContext.getScopeNearby( LocalScope.name );
		instance.executeSource(
		    """
		    var variables.bar = 5;
		    """,
		    functionBoxContext );
		assertThat( localScope.get( Key.of( "variables" ) ) instanceof IStruct ).isTrue();
		IStruct variables = ( IStruct ) localScope.get( Key.of( "variables" ) );
		assertThat( variables.get( Key.of( "bar" ) ) ).isEqualTo( 5 );

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

		assertThat( variables.get( Key.of( "foo" ) ) ).isEqualTo( "wood" );
		assertThat( variables.get( Key.of( "bar" ) ) ).isEqualTo( "wood" );
		assertThat( variables.get( Key.of( "brad" ) ) ).isEqualTo( "wood" );
	}

	@DisplayName( "scoped assignment returns value" )
	@Test
	public void testScopedAssignmentReturnsValue() {
		instance.executeSource(
		    """
		    variables.foo2 = variables.bar2 = variables.brad2 = "wood2"
		       """,
		    context );

		assertThat( variables.get( Key.of( "foo2" ) ) ).isEqualTo( "wood2" );
		assertThat( variables.get( Key.of( "bar2" ) ) ).isEqualTo( "wood2" );
		assertThat( variables.get( Key.of( "brad2" ) ) ).isEqualTo( "wood2" );
	}

	@DisplayName( "call method on object" )
	@Test
	public void testCallMethodOnObject() {
		instance.executeSource(
		    """
		    system = createObject('java', 'java.lang.System');
		    system.out.println("Hello World");
		    system["out"]["println"]("Hello World");
		    """,
		    context );

	}

	@DisplayName( "static method call on imported class" )
	@Test
	public void testStaticMethodCallOnImportedClass() {
		instance.executeSource(
		    """
		    import java:java.lang.System;

		    system.out.println( 2+3 )
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
		assertThat( variables.get( result ) ).isEqualTo( true );

	}

}
