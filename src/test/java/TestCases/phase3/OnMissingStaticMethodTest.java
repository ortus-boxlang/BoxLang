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
package TestCases.phase3;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.runnables.IClassRunnable;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.exceptions.KeyNotFoundException;

/**
 * Tests for the static onMissingStaticMethod() handler (BL-2748)
 */
public class OnMissingStaticMethodTest {

	static BoxRuntime	instance;
	IBoxContext			context;
	IScope				variables;
	static Key			result	= new Key( "result" );

	@BeforeAll
	public static void setUp() {
		instance = BoxRuntime.getInstance( true );
	}

	@BeforeEach
	public void setupEach() {
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
	}

	@DisplayName( "Missing static method with positional arguments reaches the handler" )
	@Test
	public void testPositionalArguments() {
		instance.executeSource(
		    """
		    result = src.test.java.TestCases.phase3.OnMissingStaticMethod::findByEmail( "a@b.com", true );
		    """,
		    context );
		IStruct res = variables.getAsStruct( result );
		assertThat( res.get( "name" ) ).isEqualTo( "findByEmail" );
		IStruct args = res.getAsStruct( Key.of( "args" ) );
		assertThat( args.get( Key.of( "1" ) ) ).isEqualTo( "a@b.com" );
		assertThat( args.get( Key.of( "2" ) ) ).isEqualTo( true );
	}

	@DisplayName( "Missing static method with named arguments reaches the handler" )
	@Test
	public void testNamedArguments() {
		instance.executeSource(
		    """
		    result = src.test.java.TestCases.phase3.OnMissingStaticMethod::countByActive( active = true, max = 10 );
		    """,
		    context );
		IStruct res = variables.getAsStruct( result );
		assertThat( res.get( "name" ) ).isEqualTo( "countByActive" );
		IStruct args = res.getAsStruct( Key.of( "args" ) );
		assertThat( args.get( Key.of( "active" ) ) ).isEqualTo( true );
		assertThat( args.get( Key.of( "max" ) ) ).isEqualTo( 10 );
	}

	@DisplayName( "The handler return value is returned and static. access works inside the handler" )
	@Test
	public void testReturnValueAndStaticAccess() {
		instance.executeSource(
		    """
		    before = src.test.java.TestCases.phase3.OnMissingStaticMethod::handlerCalls;
		    result = src.test.java.TestCases.phase3.OnMissingStaticMethod::anything().prefix;
		    after = src.test.java.TestCases.phase3.OnMissingStaticMethod::handlerCalls;
		    existing = src.test.java.TestCases.phase3.OnMissingStaticMethod::existing();
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "static:" );
		assertThat( variables.getAsNumber( Key.of( "after" ) ).intValue() - variables.getAsNumber( Key.of( "before" ) ).intValue() ).isEqualTo( 1 );
		// Existing static methods do not reach the handler
		assertThat( variables.get( Key.of( "existing" ) ) ).isEqualTo( "existing" );
	}

	@DisplayName( "A handler injected into the static scope at runtime is used" )
	@Test
	public void testInjectedHandler() {
		// Inject the handler into the static scope the way a module would
		instance.executeSource(
		    """
		    obj = new src.test.java.TestCases.phase3.OnMissingStaticMethodInjected();
		    handler = ( missingMethodName, missingMethodArguments ) => {
		    	return "injected:" & missingMethodName & ":" & missingMethodArguments[ 1 ];
		    };
		    """,
		    context );
		IClassRunnable obj = ( IClassRunnable ) variables.get( Key.of( "obj" ) );
		obj.getStaticScope().put( Key.onMissingStaticMethod, variables.get( Key.of( "handler" ) ) );

		instance.executeSource(
		    """
		    import src.test.java.TestCases.phase3.OnMissingStaticMethodInjected;
		    result = OnMissingStaticMethodInjected::findByName( "luis" );
		    hello = OnMissingStaticMethodInjected::hello();
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "injected:findByName:luis" );
		assertThat( variables.get( Key.of( "hello" ) ) ).isEqualTo( "hello" );
	}

	@DisplayName( "Instance calls fall through to onMissingStaticMethod when nothing else resolves" )
	@Test
	public void testInstanceFallThrough() {
		instance.executeSource(
		    """
		    obj = new src.test.java.TestCases.phase3.OnMissingStaticMethod();
		    result = obj.findByEmail( "a@b.com" );
		    named = obj.findByEmail( email = "x@y.com" );
		    """,
		    context );
		IStruct res = variables.getAsStruct( result );
		assertThat( res.get( "name" ) ).isEqualTo( "findByEmail" );
		assertThat( res.getAsStruct( Key.of( "args" ) ).get( Key.of( "1" ) ) ).isEqualTo( "a@b.com" );
		IStruct named = variables.getAsStruct( Key.of( "named" ) );
		assertThat( named.getAsStruct( Key.of( "args" ) ).get( Key.of( "email" ) ) ).isEqualTo( "x@y.com" );
	}

	@DisplayName( "Java superclass methods run before the missing static method handler" )
	@Test
	public void testJavaExtendsInstanceFallThrough() {
		instance.executeSource(
		    """
		    obj = new src.test.java.TestCases.phase3.OnMissingStaticMethodJavaExtends();
		    inherited = obj.getCount();
		    missing = obj.findByEmail( "a@b.com" );
		    """,
		    context );
		assertThat( variables.getAsNumber( Key.of( "inherited" ) ).intValue() ).isEqualTo( 42 );
		IStruct missing = variables.getAsStruct( Key.of( "missing" ) );
		assertThat( missing.get( "name" ) ).isEqualTo( "findByEmail" );
		assertThat( missing.getAsStruct( Key.of( "args" ) ).get( Key.of( "1" ) ) ).isEqualTo( "a@b.com" );
	}

	@DisplayName( "Instance onMissingMethod wins over onMissingStaticMethod for instance calls" )
	@Test
	public void testInstanceOnMissingMethodWins() {
		instance.executeSource(
		    """
		    import src.test.java.TestCases.phase3.OnMissingStaticMethodBoth;
		    obj = new OnMissingStaticMethodBoth();
		    result = obj.foo();
		    named = obj.foo( a = 1 );
		    staticResult = OnMissingStaticMethodBoth::foo();
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "instance:foo" );
		assertThat( variables.get( Key.of( "named" ) ) ).isEqualTo( "instance:foo" );
		assertThat( variables.get( Key.of( "staticResult" ) ) ).isEqualTo( "static:foo" );
	}

	@DisplayName( "Without a handler the errors are unchanged" )
	@Test
	public void testNoHandler() {
		KeyNotFoundException e = assertThrows( KeyNotFoundException.class, () -> instance.executeSource(
		    """
		    result = src.test.java.TestCases.phase3.OnMissingStaticMethodNone::nope( 1 );
		    """,
		    context ) );
		assertThat( e.getMessage() ).contains( "The key [nope] was not found in the struct" );

		e = assertThrows( KeyNotFoundException.class, () -> instance.executeSource(
		    """
		    result = src.test.java.TestCases.phase3.OnMissingStaticMethodNone::nope( a = 1 );
		    """,
		    context ) );
		assertThat( e.getMessage() ).contains( "The key [nope] was not found in the struct" );

		BoxRuntimeException e2 = assertThrows( BoxRuntimeException.class, () -> instance.executeSource(
		    """
		    result = new src.test.java.TestCases.phase3.OnMissingStaticMethodNone().nope();
		    """,
		    context ) );
		assertThat( e2.getMessage() ).isEqualTo( "Method 'nope' not found" );
	}

	@DisplayName( "A handler can cache a method so the second call does not reach the handler" )
	@Test
	public void testHandlerCaching() {
		instance.executeSource(
		    """
		    import src.test.java.TestCases.phase3.OnMissingStaticMethodCache;
		    first = OnMissingStaticMethodCache::findByName( "luis" );
		    second = OnMissingStaticMethodCache::findByName( "brad" );
		    calls = OnMissingStaticMethodCache::handlerCalls;
		    """,
		    context );
		assertThat( variables.get( Key.of( "first" ) ) ).isEqualTo( "handler:luis" );
		assertThat( variables.get( Key.of( "second" ) ) ).isEqualTo( "cached:brad" );
		assertThat( variables.getAsNumber( Key.of( "calls" ) ).intValue() ).isEqualTo( 1 );
	}

}
