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

package ortus.boxlang.runtime.bifs.global.decision;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

public class IsDefinedTest {

	static BoxRuntime	instance;
	IBoxContext			context;
	IScope				variables;

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

	@DisplayName( "It returns true for existing variables" )
	@Test
	public void testTrueConditions() {
		instance.executeSource(
		    """
		    result        = true;
		    variableName  = "result";
		    variables.foo = "bar";
		    brad          = { age: 42 };

		    stringVarName     = isDefined( variable = "result" );
		    variableReference = isDefined( variableName );
		    // localReference    = isDefined( "local.result" );
		    variableScope     = isDefined( "variables.foo" );
		    structReference   = isDefined( "brad.age" );
		    """,
		    context );
		assertThat( variables.getAsBoolean( Key.of( "stringVarName" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "variableReference" ) ) ).isTrue();
		// @TODO: Discuss var keyword and the `local` scope with brad
		// assertThat( variables.getAsBoolean( Key.of( "localReference" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "variableScope" ) ) ).isTrue();
		assertThat( variables.getAsBoolean( Key.of( "structReference" ) ) ).isTrue();
	}

	@DisplayName( "It returns false for non-existing variables" )
	@Test
	public void testFalseConditions() {
		instance.executeSource(
		    """
		    brad = { age: 42 };

		    stringVarName     = isDefined( variable = "doesntexist" );
		    localReference    = isDefined( "local.result" );
		    variableScope     = isDefined( "variables.bradzooks" );
		    structReference   = isDefined( "brad.oldAge" );
		    """,
		    context );
		assertThat( variables.getAsBoolean( Key.of( "stringVarName" ) ) ).isFalse();
		assertThat( variables.getAsBoolean( Key.of( "localReference" ) ) ).isFalse();
		assertThat( variables.getAsBoolean( Key.of( "variableScope" ) ) ).isFalse();
		assertThat( variables.getAsBoolean( Key.of( "structReference" ) ) ).isFalse();
	}

	@DisplayName( "It returns true for function name" )
	@Test
	public void testFalseFunction() {
		instance.executeSource(
		    """
		    function foo() {}

		    result     = isDefined( "foo" );
		    """,
		    context );
		assertThat( variables.getAsBoolean( Key.of( "result" ) ) ).isTrue();
	}

	@DisplayName( "It errors when calling function" )
	@Test
	public void testErrorCallingFunction() {
		Throwable t = assertThrows( BoxRuntimeException.class, () -> instance.executeSource(
		    """
		    function foo() {}

		    isDefined( "foo()" );
		    """,
		    context ) );
		assertThat( t.getMessage() ).contains( "Function " );

		t = assertThrows( BoxRuntimeException.class, () -> instance.executeSource(
		    """
		    function foo() {}

		    isDefined( "x[ foo() ]" );
		    """,
		    context ) );
		assertThat( t.getMessage() ).contains( "Function " );
	}

	@DisplayName( "It errors on invalid chars" )
	@Test
	public void testErrorInvalidChars() {
		Throwable t = assertThrows( BoxRuntimeException.class, () -> instance.executeSource(
		    """
		    isDefined( "^" );
		    """,
		    context ) );
		assertThat( t.getMessage() ).contains( "Invalid character " );
	}

	@DisplayName( "It works with non-string keys" )
	@Test
	public void testNonStringKeys() {
		instance.executeSource(
		    """
		       function foo() {}
		       str = {};
		       result = isDefined( "str[ foo ]" );

		    str[ foo ] = "bar";
		    result2 = isDefined( "str[ foo ]" );


		             """,
		    context );
		assertThat( variables.getAsBoolean( Key.of( "result" ) ) ).isFalse();
		assertThat( variables.getAsBoolean( Key.of( "result2" ) ) ).isTrue();
	}

	@DisplayName( "It won't run assignment expression" )
	@Test
	public void testNonStringKeysAssignment() {
		Throwable t = assertThrows( BoxRuntimeException.class, () -> instance.executeSource(
		    """
		    isDefined("test[ isAdmin = true ]")
		    """,
		    context ) );
		assertThat( t.getMessage() ).contains( "Invalid character " );

	}

}
