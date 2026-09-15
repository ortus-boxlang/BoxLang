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
package ortus.boxlang.compiler;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;

/**
 * Phase 2 end-to-end execution tests for the Groovy parser/transpiler effort.
 * <p>
 * Unlike {@code GroovyGrammarParsingTest} (which only checks that the ANTLR grammar accepts/
 * rejects syntax), these tests drive real Groovy source all the way through
 * {@code GroovyParser} -> the shared BoxLang AST -> the existing Boxpiler/bytecode pipeline ->
 * actual execution on {@link BoxRuntime}, exactly like {@code TestExecution} does for native
 * BoxLang source. A passing test here proves the Groovy AST this parser builds is not just
 * well-formed, but semantically correct enough to run.
 * <p>
 * Scope note: these tests exercise "script" mode only (top-level statements/methods, no class
 * declaration) since {@code executeSource} explicitly rejects class-shaped compiled output
 * ("Cannot define class in an ad-hoc script"). Class-file parsing is covered separately (AST
 * shape only, not full runtime instantiation) in {@link GroovyClassParsingTest}.
 */
public class GroovyExecutionTest {

	private IBoxContext newContext() {
		BoxRuntime instance = BoxRuntime.getInstance( true );
		return new ScriptingRequestBoxContext( instance.getRuntimeContext() );
	}

	private Object run( String source, IBoxContext context ) {
		return BoxRuntime.getInstance().executeSource( source, context, BoxSourceType.GROOVYSCRIPT );
	}

	@Test
	@DisplayName( "arithmetic expression with a top-level return" )
	public void testArithmeticReturn() {
		IBoxContext	context	= newContext();
		Object		result	= run( "def x = 1 + 2 * 3\nreturn x\n", context );
		assertThat( result.toString() ).isEqualTo( "7" );
	}

	@Test
	@DisplayName( "GString interpolation produces the expected string" )
	public void testGStringInterpolation() {
		IBoxContext	context	= newContext();
		Object		result	= run( "def name = \"World\"\nreturn \"Hello, ${name}!\"\n", context );
		assertThat( result ).isEqualTo( "Hello, World!" );
	}

	@Test
	@DisplayName( "GString with a nested closure brace interpolates correctly" )
	public void testGStringNestedBraceInterpolation() {
		IBoxContext	context	= newContext();
		// A closure literal (its own { }) invoked inline inside a GString interpolation -
		// exercises the nested-brace lexer path (the Phase 0 spike's flagged gap) without
		// depending on a Groovy-stdlib collection method BoxLang's Array type doesn't have.
		Object		result	= run( "return \"Answer: ${ { 6 * 7 }() }\"\n", context );
		assertThat( result.toString() ).isEqualTo( "Answer: 42" );
	}

	@Test
	@DisplayName( "if/else control flow" )
	public void testIfElse() {
		IBoxContext	context	= newContext();
		Object		result	= run(
		    "def x = 5\n"
		        + "if (x > 3) {\n"
		        + "  return \"big\"\n"
		        + "} else {\n"
		        + "  return \"small\"\n"
		        + "}\n",
		    context );
		assertThat( result ).isEqualTo( "big" );
	}

	@Test
	@DisplayName( "while loop accumulates into the variables scope" )
	public void testWhileLoop() {
		IBoxContext	context		= newContext();
		IScope		variables	= context.getScopeNearby( VariablesScope.name );
		// Bare (unscoped) assignment, same convention TestExecution.java uses - it resolves
		// into the variables scope via normal scope-chain fallback. Indexing a bare
		// "variables" identifier isn't valid in an ad-hoc scripting context.
		run( "total = 0\ndef i = 0\nwhile (i < 5) {\n  total = total + i\n  i = i + 1\n}\n", context );
		assertThat( variables.get( Key.of( "total" ) ).toString() ).isEqualTo( "10" );
	}

	@Test
	@DisplayName( "classic for loop accumulates into the variables scope" )
	public void testClassicForLoop() {
		IBoxContext	context		= newContext();
		IScope		variables	= context.getScopeNearby( VariablesScope.name );
		run( "total = 0\nfor (i = 0; i < 5; i = i + 1) {\n  total = total + i\n}\n", context );
		assertThat( variables.get( Key.of( "total" ) ).toString() ).isEqualTo( "10" );
	}

	@Test
	@DisplayName( "for-in loop over a list" )
	public void testForInLoop() {
		IBoxContext	context		= newContext();
		IScope		variables	= context.getScopeNearby( VariablesScope.name );
		run( "total = 0\nfor (item in [1, 2, 3, 4]) {\n  total = total + item\n}\n", context );
		assertThat( variables.get( Key.of( "total" ) ).toString() ).isEqualTo( "10" );
	}

	@Test
	@DisplayName( "top-level method declaration and call" )
	public void testTopLevelMethodCall() {
		IBoxContext	context	= newContext();
		Object		result	= run(
		    "def square(x) {\n"
		        + "  return x * x\n"
		        + "}\n"
		        + "return square(6)\n",
		    context );
		assertThat( result.toString() ).isEqualTo( "36" );
	}

	@Test
	@DisplayName( "closure assigned to a variable and invoked" )
	public void testClosureInvocation() {
		IBoxContext	context	= newContext();
		Object		result	= run( "def doubler = { it * 2 }\nreturn doubler(21)\n", context );
		assertThat( result.toString() ).isEqualTo( "42" );
	}

	@Test
	@DisplayName( "elvis and safe-navigation operators" )
	public void testElvisAndSafeNav() {
		IBoxContext	context	= newContext();
		Object		result	= run( "def x = null\ndef y = x ?: 99\nreturn y\n", context );
		assertThat( result.toString() ).isEqualTo( "99" );
	}

	@Test
	@DisplayName( "ternary operator" )
	public void testTernary() {
		IBoxContext	context	= newContext();
		Object		result	= run( "def x = 10\nreturn x > 5 ? \"yes\" : \"no\"\n", context );
		assertThat( result ).isEqualTo( "yes" );
	}

	@Test
	@DisplayName( "try/catch/finally with throw" )
	public void testTryCatchFinally() {
		IBoxContext	context		= newContext();
		IScope		variables	= context.getScopeNearby( VariablesScope.name );
		run(
		    "finallyRan = false\n"
		        + "try {\n"
		        + "  throw new RuntimeException(\"boom\")\n"
		        + "} catch (RuntimeException e) {\n"
		        + "  caught = true\n"
		        + "} finally {\n"
		        + "  finallyRan = true\n"
		        + "}\n",
		    context );
		assertThat( variables.get( Key.of( "caught" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "finallyRan" ) ) ).isEqualTo( true );
	}

	@Test
	@DisplayName( "list and map literals" )
	public void testListAndMapLiterals() {
		IBoxContext	context	= newContext();
		Object		result	= run( "def list = [1, 2, 3]\nreturn list.size()\n", context );
		assertThat( result.toString() ).isEqualTo( "3" );
	}

	@Test
	@DisplayName( "map literal values are accessible by key" )
	public void testMapLiteralAccess() {
		IBoxContext	context	= newContext();
		Object		result	= run( "def m = [a: 1, b: 2]\nreturn m['a'] + m['b']\n", context );
		assertThat( result.toString() ).isEqualTo( "3" );
	}

	@Test
	@DisplayName( "multi-statement script with no semicolons executes" )
	public void testNewlineSeparatedStatements() {
		IBoxContext	context	= newContext();
		Object		result	= run( "def a = 1\ndef b = 2\ndef c = a + b\nreturn c\n", context );
		assertThat( result.toString() ).isEqualTo( "3" );
	}

	@Test
	@DisplayName( "switch statement: matching case with break" )
	public void testSwitchMatchingCaseBreaks() {
		IBoxContext	context	= newContext();
		Object		result	= run(
		    "def x = 2\n"
		        + "def result = \"none\"\n"
		        + "switch (x) {\n"
		        + "  case 1:\n"
		        + "    result = \"one\"\n"
		        + "    break\n"
		        + "  case 2:\n"
		        + "    result = \"two\"\n"
		        + "    break\n"
		        + "  default:\n"
		        + "    result = \"other\"\n"
		        + "}\n"
		        + "return result\n",
		    context );
		assertThat( result ).isEqualTo( "two" );
	}

	@Test
	@DisplayName( "switch statement: grouped cases fall through to a shared body" )
	public void testSwitchFallThroughGroupedCases() {
		IBoxContext	context	= newContext();
		Object		result	= run(
		    "def x = 3\n"
		        + "def result = \"none\"\n"
		        + "switch (x) {\n"
		        + "  case 2:\n"
		        + "  case 3:\n"
		        + "    result = \"two-or-three\"\n"
		        + "    break\n"
		        + "  default:\n"
		        + "    result = \"other\"\n"
		        + "}\n"
		        + "return result\n",
		    context );
		assertThat( result ).isEqualTo( "two-or-three" );
	}

	@Test
	@DisplayName( "switch statement: no matching case falls to default" )
	public void testSwitchDefaultCase() {
		IBoxContext	context	= newContext();
		Object		result	= run(
		    "def x = 99\n"
		        + "def result = \"none\"\n"
		        + "switch (x) {\n"
		        + "  case 1:\n"
		        + "    result = \"one\"\n"
		        + "    break\n"
		        + "  default:\n"
		        + "    result = \"other\"\n"
		        + "}\n"
		        + "return result\n",
		    context );
		assertThat( result ).isEqualTo( "other" );
	}

	@Test
	@DisplayName( "for-in over a range literal" )
	public void testForInRange() {
		IBoxContext	context	= newContext();
		Object		result	= run( "total = 0\nfor (i in 1..5) {\n  total = total + i\n}\nreturn total\n", context );
		assertThat( result.toString() ).isEqualTo( "15" );
	}

	@Test
	@DisplayName( "collect{} aliases to BoxLang's Array.map()" )
	public void testCollectAliasesToMap() {
		IBoxContext	context	= newContext();
		Object		result	= run( "def doubled = [1, 2, 3].collect { it * 2 }\nreturn doubled.toList(\",\")\n", context );
		assertThat( result.toString() ).isEqualTo( "2,4,6" );
	}

	@Test
	@DisplayName( "findAll{} aliases to BoxLang's Array.filter(), not its own native findAll" )
	public void testFindAllAliasesToFilter() {
		IBoxContext	context	= newContext();
		Object		result	= run( "def positives = [-2, -1, 0, 1, 2].findAll { it > 0 }\nreturn positives.toList(\",\")\n", context );
		assertThat( result.toString() ).isEqualTo( "1,2" );
	}

	@Test
	@DisplayName( "any{} aliases to BoxLang's Array.some()" )
	public void testAnyAliasesToSome() {
		IBoxContext	context	= newContext();
		Object		result	= run( "return [1, 2, 3].any { it > 2 }\n", context );
		assertThat( result ).isEqualTo( true );
	}

}
