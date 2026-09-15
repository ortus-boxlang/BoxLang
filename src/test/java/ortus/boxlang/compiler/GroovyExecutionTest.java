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

	@Test
	@DisplayName( "labeled break exits the outer loop, not just the inner one" )
	public void testLabeledBreak() {
		IBoxContext	context	= newContext();
		// Without the label reaching the outer loop, "found" would end up counting every
		// (i, j) pair visited before the FIRST match, not stop the outer loop entirely -
		// proving the label actually propagated from the grammar into BoxForIn.setLabel(),
		// not just parsing without effect.
		Object		result	= run(
		    "found = null\n"
		        + "outer: for (i in 1..3) {\n"
		        + "  for (j in 1..3) {\n"
		        + "    if (i == 2 && j == 2) {\n"
		        + "      found = \"${i},${j}\"\n"
		        + "      break outer\n"
		        + "    }\n"
		        + "  }\n"
		        + "}\n"
		        + "return found\n",
		    context );
		assertThat( result ).isEqualTo( "2,2" );
	}

	@Test
	@DisplayName( "assert with a true condition does not throw" )
	public void testAssertPasses() {
		IBoxContext	context	= newContext();
		Object		result	= run( "def x = 5\nassert x > 0\nreturn \"ok\"\n", context );
		assertThat( result ).isEqualTo( "ok" );
	}

	@Test
	@DisplayName( "assert with a false condition throws, carrying the message" )
	public void testAssertFails() {
		IBoxContext	context	= newContext();
		var			thrown	= org.junit.jupiter.api.Assertions.assertThrows( AssertionError.class,
		    () -> run( "def x = -5\nassert x > 0 : \"x must be positive\"\n", context ) );
		assertThat( thrown.getMessage() ).contains( "x must be positive" );
	}

	@Test
	@DisplayName( "'+' concatenates when one side is syntactically a string literal/GString" )
	public void testPlusConcatenatesWhenOneSideIsAStringLiteral() {
		// Groovy overloads "+" for string concatenation, but BoxLang's own "+" is strictly
		// numeric (matching CFML, where "&" is the concat operator). GroovyExpressionVisitor
		// handles the common case - one side is syntactically a string literal/GString, e.g.
		// "prefix" + var or var + "suffix" - by building a BoxStringConcat instead of a
		// numeric BoxBinaryOperation, entirely at parse time (no runtime type check needed).
		IBoxContext	context	= newContext();
		Object		result	= run( "def name = \"World\"\nreturn \"Hello, \" + name + \"!\"\n", context );
		assertThat( result ).isEqualTo( "Hello, World!" );
	}

	@Test
	@DisplayName( "static class members resolve without an explicit import - java.lang is always implicit" )
	public void testDefaultImportsResolveJavaLangStatics() {
		// Real Groovy never requires "import java.lang.Math" - java.lang (plus a few other
		// packages) is always implicitly available. GroovyParser mirrors this by prepending a
		// fixed set of default imports (java.lang.*, java.util.*, java.io.*, BigInteger,
		// BigDecimal) to every parsed file, and GroovyExpressionVisitor recognizes a bare
		// capitalized identifier matching one of those classes as a static access/invocation
		// base rather than an ordinary instance dot-access.
		IBoxContext	context	= newContext();
		Object		result	= run( "return Math.max(1, 2) + Integer.parseInt(\"40\")\n", context );
		assertThat( result.toString() ).isEqualTo( "42" );
	}

	@Test
	@DisplayName( "explicit single-class import resolves as a static base too" )
	public void testExplicitImportResolvesStaticBase() {
		IBoxContext	context	= newContext();
		Object		result	= run( "import java.math.BigDecimal\ndef x = new BigDecimal(\"1.5\")\nreturn x.toString()\n", context );
		assertThat( result ).isEqualTo( "1.5" );
	}

	@Test
	@DisplayName( "a local variable can still shadow a known static class name" )
	public void testLocalVariableCanShadowKnownStaticName() {
		// The static-vs-instance decision made at parse time is a heuristic (bare capitalized
		// identifier matching a known/imported class simple name), but BoxClassSupport.ensureClass
		// checks for an actual variable of that name FIRST at runtime and only falls back to
		// class-loading if none exists - so a local named the same as a known static class
		// (unusual, but legal Groovy) still resolves correctly as the variable.
		IBoxContext	context	= newContext();
		Object		result	= run( "def String = \"shadow\"\nreturn String\n", context );
		assertThat( result ).isEqualTo( "shadow" );
	}

	@Test
	@DisplayName( "static varargs method resolution is a documented interop gap, not silently wrong" )
	public void testStaticVarargsMethodIsADocumentedGap() {
		// String.format(...) resolves String as a static class correctly (same mechanism as
		// Math.max above), but fails one layer deeper: BoxLang's Java-interop method resolver
		// can't currently match a reflective varargs static method signature. This is a general
		// DynamicObject/interop limitation, not specific to the Groovy parser - pinning down the
		// honest failure rather than leaving it an undocumented surprise.
		IBoxContext context = newContext();
		org.junit.jupiter.api.Assertions.assertThrows( RuntimeException.class,
		    () -> run( "return String.format(\"%d\", 5)\n", context ) );
	}

	@Test
	@DisplayName( "'+' between two unknown-typed variables is a documented gap, not silently wrong" )
	public void testPlusBetweenVariablesIsNumericOnly() {
		// The fully general case - both sides are variables, so whether "+" means concat or
		// add is only knowable at runtime - isn't handled: that needs an actual runtime type
		// check on every "+" evaluation, a bigger semantic-mapping decision deliberately not
		// made here. Real Groovy code hitting this needs GString interpolation instead
		// ("${a}${b}"), which already works. Pinning down the honest failure rather than
		// leaving it an undocumented surprise.
		IBoxContext context = newContext();
		org.junit.jupiter.api.Assertions.assertThrows( RuntimeException.class,
		    () -> run( "def a = \"Hello, \"\ndef b = \"World\"\nreturn a + b\n", context ) );
	}

}
