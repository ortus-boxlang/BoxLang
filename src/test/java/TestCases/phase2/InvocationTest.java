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

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;

@Disabled
public class InvocationTest {

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

	@DisplayName( "Invoke single identifier" )
	@Test
	public void testSingleIdentifierInocation() {
		instance.executeSource(
		    """
		    foo = () => "test";
		    foo();
		    """,
		    context );
	}

	@DisplayName( "Invoke after dot" )
	@Test
	public void testAfterDotInvocation() {
		instance.executeSource(
		    """
		    foo.bar = () => "test";
		    foo.bar();
		    """,
		    context );
	}

	@DisplayName( "Invoke after multiple dots" )
	@Test
	public void testAfterMultipleDotInvocation() {
		instance.executeSource(
		    """
		    foo.bar = () => {
		        return {
		            baz: () => "test"
		        };
		    };
		    foo.bar().baz();
		    """,
		    context );
	}

	@DisplayName( "Invoke within parenthesis" )
	@Test
	public void testInsideParenthesisInvocation() {
		instance.executeSource(
		    """
		    foo.bar = () => "test";
		    (foo.bar());
		    """,
		    context );
	}

	@DisplayName( "Invoke result of parenthesis" )
	@Test
	public void testResultOfParenthesisInvocation() {
		instance.executeSource(
		    """
		    foo.bar = () => {
		        return () => "test";
		    };
		    (foo.bar())();
		    """,
		    context );
	}

	@DisplayName( "Invoke result of invocation" )
	@Test
	public void testResultOfInvocationInvocation() {
		instance.executeSource(
		    """
		    foo.bar = () => {
		        return () => "test";
		    };
		    foo.bar()();
		    """,
		    context );
	}

	@DisplayName( "Invoke result of result of invocation" )
	@Test
	public void testResultOfResultOfInvocationInvocation() {
		instance.executeSource(
		    """
		    foo.bar = () => {
		        return () => {
		            return () => "test";
		        };
		    };
		    foo.bar()()();
		    """,
		    context );
	}

	@DisplayName( "Invoke result of array access of invocation" )
	@Test
	public void testResultOfArrayAccessInvocation() {
		instance.executeSource(
		    """
		    foo.bar = () => "test";
		    foo["bar"]();
		    """,
		    context );
	}

	@DisplayName( "Invoke result of result of array access of invocation" )
	@Test
	public void testResultOfResultOfArrayAccessInvocation() {
		instance.executeSource(
		    """
		    foo.bar = () => {
		        return () => "test";
		    };
		    foo["bar"]()();
		    """,
		    context );
	}

	@DisplayName( "String literal method dot invocation" )
	@Test
	public void testStringLiteralMethodDotInvocation() {
		instance.executeSource(
		    """
		    "test".len();
		    """,
		    context );
	}

	@DisplayName( "String literal method bracket invocation" )
	@Test
	public void testStringLiteralMethodBracketInvocation() {
		instance.executeSource(
		    """
		    "test"[ "len" ]();
		    """,
		    context );
	}

	@DisplayName( "Boolean literal method dot invocation" )
	@Test
	public void testBooleanLiteralMethodDotInvocation() {
		instance.executeSource(
		    """
		    true.yesNoFormat();
		    """,
		    context );
	}

	@DisplayName( "Boolean literal method bracket invocation" )
	@Test
	public void testBooleanLiteralMethodBracketInvocation() {
		instance.executeSource(
		    """
		    true[ "yesNoFormat" ]();
		    """,
		    context );
	}

	@DisplayName( "Numeric literal method dot invocation" )
	@Test
	public void testNumericLiteralMethodDotInvocation() {
		instance.executeSource(
		    """
		    5.floor();
		    """,
		    context );
	}

	@DisplayName( "Numeric literal method bracket invocation" )
	@Test
	public void testNumericLiteralMethodBracketInvocation() {
		instance.executeSource(
		    """
		    5[ "floor" ]();
		    """,
		    context );
	}

	@DisplayName( "Array literal method dot invocation" )
	@Test
	public void testArrayLiteralMethodDotInvocation() {
		instance.executeSource(
		    """
		    [].len();
		    """,
		    context );
	}

	@DisplayName( "Array literal method bracket invocation" )
	@Test
	public void testArrayLiteralMethodBracketInvocation() {
		instance.executeSource(
		    """
		    [][ "len" ]();
		    """,
		    context );
	}

	@DisplayName( "Struct literal method dot invocation" )
	@Test
	public void testStructLiteralMethodDotInvocation() {
		instance.executeSource(
		    """
		    {}.len();
		    """,
		    context );
	}

	@DisplayName( "Struct literal method bracket invocation" )
	@Test
	public void testStructLiteralMethodBracketInvocation() {
		instance.executeSource(
		    """
		    {}[ "len" ]();
		    """,
		    context );
	}

}
