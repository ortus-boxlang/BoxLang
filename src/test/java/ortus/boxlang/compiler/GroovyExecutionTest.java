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
	@DisplayName( "'+=' concatenates when the right side is syntactically a string literal/GString" )
	public void testPlusEqualsConcatenatesWhenRightSideIsAStringLiteral() {
		// Same gap/fix as testPlusConcatenatesWhenOneSideIsAStringLiteral, but for the compound
		// "+=" assignment operator - BoxLang's PlusEqual is strictly numeric too.
		IBoxContext	context	= newContext();
		Object		result	= run( "def s = \"a\"\ns += \"b\"\nreturn s\n", context );
		assertThat( result ).isEqualTo( "ab" );
	}

	@Test
	@DisplayName( "'+=' is still numeric when both sides are numbers" )
	public void testPlusEqualsStaysNumericForNumbers() {
		IBoxContext	context	= newContext();
		Object		result	= run( "def n = 1\nn += 2\nreturn n\n", context );
		assertThat( result.toString() ).isEqualTo( "3" );
	}

	@Test
	@DisplayName( "spread call argument expands a list's elements as individual positional arguments" )
	public void testSpreadCallArgument() {
		IBoxContext	context	= newContext();
		Object		result	= run( "def args = [1, 2]\nreturn Math.max(*args)\n", context );
		assertThat( result.toString() ).isEqualTo( "2" );
	}

	@Test
	@DisplayName( "spread call argument can be mixed with a leading positional argument" )
	public void testSpreadCallArgumentMixedWithPositional() {
		IBoxContext	context	= newContext();
		Object		result	= run( "def rest = [7]\nreturn Math.max(3, *rest)\n", context );
		assertThat( result.toString() ).isEqualTo( "7" );
	}

	@Test
	@DisplayName( "tuple declaration destructures a list into separate variables" )
	public void testTupleDeclaration() {
		// "def (a, b) = [1, 2]" reuses BoxLang's own native array-destructuring assignment AST
		// (BoxArrayDestructuringPattern) - the same node ArrayDestructurer already compiles for
		// BoxParser's own "[a, b] = expr" syntax - so no new runtime support was needed.
		IBoxContext	context	= newContext();
		Object		result	= run( "def (a, b) = [1, 2]\nreturn a + b\n", context );
		assertThat( result.toString() ).isEqualTo( "3" );
	}

	@Test
	@DisplayName( "tuple declaration works with more than two names and a function-returned list" )
	public void testTupleDeclarationThreeNamesFromFunctionResult() {
		IBoxContext	context	= newContext();
		Object		result	= run(
		    "def pair() {\n"
		        + "  return [1, 2, 3]\n"
		        + "}\n"
		        + "def (x, y, z) = pair()\n"
		        + "return \"${x},${y},${z}\"\n",
		    context );
		assertThat( result ).isEqualTo( "1,2,3" );
	}

	@Test
	@DisplayName( "'*' repeats a string when the left side is syntactically a string literal" )
	public void testStarRepeatsStringLiteral() {
		// Groovy overloads "*" for String.multiply(Number). Same bounded, syntactic-detection
		// approach as the "+" concat fix - desugars to BoxLang's own RepeatString BIF.
		IBoxContext	context	= newContext();
		Object		result	= run( "return \"ab\" * 3\n", context );
		assertThat( result ).isEqualTo( "ababab" );
	}

	@Test
	@DisplayName( "'*' is still numeric multiplication for numbers" )
	public void testStarStaysNumericForNumbers() {
		IBoxContext	context	= newContext();
		Object		result	= run( "return 3 * 4\n", context );
		assertThat( result.toString() ).isEqualTo( "12" );
	}

	@Test
	@DisplayName( "'in' operator tests list membership" )
	public void testInOperatorMembership() {
		IBoxContext	context	= newContext();
		Object		result	= run( "def list = [1, 2, 3]\nreturn 2 in list\n", context );
		assertThat( result ).isEqualTo( true );
	}

	@Test
	@DisplayName( "'in' operator is false for a non-member, and negatable with '!( ... )'" )
	public void testInOperatorNegated() {
		IBoxContext	context	= newContext();
		Object		result	= run( "def list = [1, 2, 3]\nreturn !(9 in list)\n", context );
		assertThat( result ).isEqualTo( true );
	}

	@Test
	@DisplayName( "named/map call arguments are collected into a single trailing struct argument" )
	public void testNamedCallArguments() {
		IBoxContext	context	= newContext();
		Object		result	= run(
		    "def greet(Map args) {\n"
		        + "  return \"hi \" + args.name\n"
		        + "}\n"
		        + "return greet(name: \"world\")\n",
		    context );
		assertThat( result ).isEqualTo( "hi world" );
	}

	@Test
	@DisplayName( "trailing closure combined with a parenthesized argument list, on a user-defined method" )
	public void testCallWithTrailingClosureOnUserMethod() {
		IBoxContext	context	= newContext();
		Object		result	= run(
		    "def apply(n, fn) {\n"
		        + "  return fn(n)\n"
		        + "}\n"
		        + "return apply(21) { it * 2 }\n",
		    context );
		assertThat( result.toString() ).isEqualTo( "42" );
	}

	@Test
	@DisplayName( "'inject' aliases to BoxLang's reduce(), with the closure moved to the front to match its argument order" )
	public void testInjectAliasesToReduceWithSwappedArgumentOrder() {
		IBoxContext	context	= newContext();
		Object		result	= run( "def list = [1, 2, 3]\nreturn list.inject(0) { acc, x -> acc + x }\n", context );
		assertThat( result.toString() ).isEqualTo( "6" );
	}

	@Test
	@DisplayName( "spaceship operator returns -1/0/1 via BoxLang's own general-purpose compare" )
	public void testSpaceshipOperator() {
		IBoxContext context = newContext();
		assertThat( run( "return 1 <=> 2\n", context ).toString() ).isEqualTo( "-1" );
		assertThat( run( "return 5 <=> 5\n", context ).toString() ).isEqualTo( "0" );
		assertThat( run( "return 2 <=> 1\n", context ).toString() ).isEqualTo( "1" );
	}

	@Test
	@DisplayName( "spaceship operator works as a sort comparator closure" )
	public void testSpaceshipOperatorInSortClosure() {
		IBoxContext	context	= newContext();
		Object		result	= run( "def list = [3, 1, 2]\nreturn list.sort { a, b -> a <=> b }.toList(\",\")\n", context );
		assertThat( result ).isEqualTo( "1,2,3" );
	}

	@Test
	@DisplayName( "smart-switch: a Class case value matches by instanceof, not equality" )
	public void testSmartSwitchClassCase() {
		// BoxSwitch (shared with CFVisitor/BoxVisitor) only ever compares by equality, so a
		// switch with at least one Class/Range/List case value is rewritten as an if/else-if
		// chain instead - see GroovyVisitor#visitSwitchStatement.
		IBoxContext context = newContext();
		assertThat( run( "def x = \"hi\"\nswitch (x) {\n case String:\n  return \"string\"\n default:\n  return \"other\"\n}\n", context ) )
		    .isEqualTo( "string" );
		assertThat( run( "def x = 5\nswitch (x) {\n case String:\n  return \"string\"\n default:\n  return \"other\"\n}\n", context ) )
		    .isEqualTo( "other" );
	}

	@Test
	@DisplayName( "smart-switch: a Range case value matches by containment" )
	public void testSmartSwitchRangeCase() {
		IBoxContext context = newContext();
		assertThat( run( "def x = 5\nswitch (x) {\n case 1..10:\n  return \"in range\"\n default:\n  return \"other\"\n}\n", context ) )
		    .isEqualTo( "in range" );
		assertThat( run( "def x = 50\nswitch (x) {\n case 1..10:\n  return \"in range\"\n default:\n  return \"other\"\n}\n", context ) )
		    .isEqualTo( "other" );
	}

	@Test
	@DisplayName( "smart-switch: a List case value matches by containment" )
	public void testSmartSwitchListCase() {
		IBoxContext context = newContext();
		assertThat( run( "def x = 2\nswitch (x) {\n case [1, 2, 3]:\n  return \"in list\"\n default:\n  return \"other\"\n}\n", context ) )
		    .isEqualTo( "in list" );
		assertThat( run( "def x = 9\nswitch (x) {\n case [1, 2, 3]:\n  return \"in list\"\n default:\n  return \"other\"\n}\n", context ) )
		    .isEqualTo( "other" );
	}

	@Test
	@DisplayName( "smart-switch: mixing a plain equality case with a smart case in the same switch" )
	public void testSmartSwitchMixedWithPlainEqualityCase() {
		IBoxContext	context	= newContext();
		String		source	= "def classify(x) {\n"
		    + "  switch (x) {\n"
		    + "    case \"hi\":\n"
		    + "      return \"greeting\"\n"
		    + "    case 1..10:\n"
		    + "      return \"small number\"\n"
		    + "    default:\n"
		    + "      return \"other\"\n"
		    + "  }\n"
		    + "}\n"
		    + "return \"${classify('hi')},${classify(5)},${classify(99)}\"\n";
		assertThat( run( source, context ) ).isEqualTo( "greeting,small number,other" );
	}

	@Test
	@DisplayName( "smart-switch: an explicit break exits after the matched case, no fallthrough" )
	public void testSmartSwitchBreakStopsAtMatchedCase() {
		IBoxContext	context	= newContext();
		String		source	= "def x = 5\n"
		    + "def hit = []\n"
		    + "switch (x) {\n"
		    + "  case 1..10:\n"
		    + "    hit.add(\"first\")\n"
		    + "    break\n"
		    + "  case Integer:\n"
		    + "    hit.add(\"second\")\n"
		    + "    break\n"
		    + "}\n"
		    + "return hit.toList(\",\")\n";
		assertThat( run( source, context ) ).isEqualTo( "first" );
	}

	@Test
	@DisplayName( "Range.step(n) { } iterates the range advancing by n, invoking the closure" )
	public void testRangeStepWithClosure() {
		// Desugars entirely at parse time into "(1..10).step(2).stream().forEach { ... }" - see
		// GroovyExpressionVisitor#buildRangeStepWithClosure. No core Range/BIF changes: an
		// earlier attempt to teach core Range.step() a third "eagerly iterate with a callback"
		// meaning (alongside its two existing builder-only overloads) was reverted after review -
		// it collided with the builder meaning under the same overloaded member name, and the
		// same result is achievable with Range's own existing, unmodified builder/stream methods.
		IBoxContext	context	= newContext();
		Object		result	= run( "total = 0\n(1..10).step(2) { total = total + it }\nreturn total\n", context );
		assertThat( result.toString() ).isEqualTo( "25" );
	}

	@Test
	@DisplayName( "enum declaration: constant access, equality, values(), interpolation" )
	public void testEnumDeclaration() {
		// See GroovyVisitor#visitEnumDeclaration for exactly what this desugars to - a struct
		// whose values are real GroovyEnumValue instances (own name/ordinal), not plain strings,
		// but still comparable/interchangeable with a plain string for these idioms.
		IBoxContext	context	= newContext();
		Object		result	= run(
		    "enum Color { RED, GREEN, BLUE }\n"
		        + "def x = Color.GREEN\n"
		        + "def matches = (x == Color.GREEN)\n"
		        + "def all = Color.values.toList(\",\")\n"
		        + "return \"${Color.RED},${matches},${all}\"\n",
		    context );
		assertThat( result ).isEqualTo( "RED,true,RED,GREEN,BLUE" );
	}

	@Test
	@DisplayName( "enum constant supports real ordinal() and name() member calls" )
	public void testEnumOrdinalAndName() {
		IBoxContext	context	= newContext();
		Object		result	= run(
		    "enum Color { RED, GREEN, BLUE }\n"
		        + "def g = Color.GREEN\n"
		        + "return \"${g.name()}:${g.ordinal()}:${Color.RED.ordinal()}:${Color.BLUE.ordinal()}\"\n",
		    context );
		assertThat( result ).isEqualTo( "GREEN:1:0:2" );
	}

	@Test
	@DisplayName( "enum constants compare by ordinal via the spaceship operator" )
	public void testEnumSpaceshipComparesByOrdinal() {
		IBoxContext	context	= newContext();
		Object		result	= run(
		    "enum Color { RED, GREEN, BLUE }\n"
		        + "return Color.RED <=> Color.BLUE\n",
		    context );
		assertThat( result.toString() ).isEqualTo( "-1" );
	}

	@Test
	@DisplayName( "an enum constant equals a plain string in either comparison order" )
	public void testEnumEqualsStringBothDirections() {
		IBoxContext	context	= newContext();
		Object		result	= run(
		    "enum Color { RED, GREEN, BLUE }\n"
		        + "def a = (Color.RED == \"RED\")\n"
		        + "def b = (\"RED\" == Color.RED)\n"
		        + "return \"${a}:${b}\"\n",
		    context );
		assertThat( result ).isEqualTo( "true:true" );
	}

	@Test
	@DisplayName( "enum constant used as a switch case (plain equality, since it's just a string)" )
	public void testEnumInSwitchCase() {
		IBoxContext	context	= newContext();
		Object		result	= run(
		    "enum Color { RED, GREEN, BLUE }\n"
		        + "def x = Color.RED\n"
		        + "def result = \"none\"\n"
		        + "switch (x) {\n"
		        + "  case Color.RED:\n"
		        + "    result = \"is red\"\n"
		        + "    break\n"
		        + "  default:\n"
		        + "    result = \"other\"\n"
		        + "}\n"
		        + "return result\n",
		    context );
		assertThat( result ).isEqualTo( "is red" );
	}

	@Test
	@DisplayName( "an enum declared inside a class body is usable from that class's own methods" )
	public void testEnumNestedInClassBody() {
		// enumDeclaration is now a valid classMember (see GroovyGrammar.g4/GroovyVisitor#
		// visitEnumDeclaration's own header for the nested-form scoping trade-off: this runs once
		// per constructed instance, into that instance's own "variables" scope, exactly like an
		// ordinary field would - so it's reachable bare from the class's own methods, but NOT as
		// "TrafficLight.Color.RED" without an instance.
		IBoxContext	context	= newContext();
		Object		result	= run(
		    "class TrafficLight {\n"
		        + "  enum Color { RED, GREEN, BLUE }\n"
		        + "  def current() { return Color.GREEN }\n"
		        + "  def compare(other) { return current() <=> other }\n"
		        + "}\n"
		        + "def t = new TrafficLight()\n"
		        + "def c = t.current()\n"
		        + "return \"${c}:${c.ordinal()}:${t.compare(c)}\"\n",
		    context );
		assertThat( result ).isEqualTo( "GREEN:1:0" );
	}

	@Test
	@DisplayName( "triple-quoted string spans multiple lines" )
	public void testTripleQuotedStringSpansLines() {
		IBoxContext	context	= newContext();
		Object		result	= run( "def s = \"\"\"line1\nline2\"\"\"\nreturn s.contains(\"line2\")\n", context );
		assertThat( result ).isEqualTo( true );
	}

	@Test
	@DisplayName( "triple-quoted string tolerates embedded single/double quotes that aren't the closing delimiter" )
	public void testTripleQuotedStringEmbeddedQuotes() {
		IBoxContext	context	= newContext();
		Object		result	= run( "return \"\"\"he said \"hi\" there\"\"\"\n", context );
		assertThat( result ).isEqualTo( "he said \"hi\" there" );
	}

	@Test
	@DisplayName( "triple-quoted string supports the same interpolation forms as a regular GString" )
	public void testTripleQuotedStringInterpolation() {
		IBoxContext	context	= newContext();
		Object		result	= run( "def name = \"World\"\nreturn \"\"\"Hello, ${name}!\"\"\"\n", context );
		assertThat( result ).isEqualTo( "Hello, World!" );
	}

	@Test
	@DisplayName( "'=~' returns a boolean, not a live Matcher (a documented simplification)" )
	public void testRegexFindReturnsBoolean() {
		// Real Groovy's "=~" returns a java.util.regex.Matcher (truthy only via Groovy's own
		// Matcher.asBoolean() override that BoxLang has no equivalent for). Rather than let
		// "if (str =~ pattern)" always be true regardless of match, this eagerly evaluates
		// find() and returns a plain boolean - correct for the common find/no-find idiom, but
		// deliberately not the same as real Groovy's richer Matcher-returning semantics.
		IBoxContext context = newContext();
		assertThat( run( "return \"hello\" =~ /l+/\n", context ) ).isEqualTo( true );
		assertThat( run( "return \"hello\" =~ /xyz/\n", context ) ).isEqualTo( false );
	}

	@Test
	@DisplayName( "'==~' tests a full-string regex match" )
	public void testRegexFullMatch() {
		IBoxContext context = newContext();
		assertThat( run( "return \"hello\" ==~ /hello/\n", context ) ).isEqualTo( true );
		assertThat( run( "return \"hello world\" ==~ /hello/\n", context ) ).isEqualTo( false );
	}

	@Test
	@DisplayName( "slashy string literal: regex metacharacters pass through untouched" )
	public void testSlashyStringLiteral() {
		IBoxContext	context	= newContext();
		Object		result	= run( "def pattern = /\\d+/\nreturn \"abc123\" =~ pattern\n", context );
		assertThat( result ).isEqualTo( true );
	}

	@Test
	@DisplayName( "slashy string literal: '\\/' is the escape for a literal '/'" )
	public void testSlashyStringEscapedSlash() {
		IBoxContext	context	= newContext();
		Object		result	= run( "return /a\\/b/\n", context );
		assertThat( result ).isEqualTo( "a/b" );
	}

	@Test
	@DisplayName( "division still works correctly - not confused with the new slashy-string/regex syntax" )
	public void testDivisionStillWorksAfterRegexSupport() {
		IBoxContext context = newContext();
		assertThat( run( "return 10 / 2\n", context ).toString() ).isEqualTo( "5" );
		assertThat( run( "def x = 10\nreturn x / 2\n", context ).toString() ).isEqualTo( "5" );
		assertThat( run( "return (5 + 5) / 2\n", context ).toString() ).isEqualTo( "5" );
		assertThat( run( "def list = [10, 20]\nreturn list[1] / 2\n", context ).toString() ).isEqualTo( "5" );
		assertThat( run( "def a = 100\ndef b = 5\ndef c = 2\nreturn a / b / c\n", context ).toString() ).isEqualTo( "10" );
		assertThat( run( "def x = 10\nx++\nreturn x / 2\n", context ).toString() ).isEqualTo( "5.5" );
	}

	@Test
	@DisplayName( "varargs: extra positional arguments collect into an array" )
	public void testVarargsCollectsExtraArguments() {
		IBoxContext	context	= newContext();
		Object		result	= run( "def sum(int... nums) { return nums.toList(\",\") }\nreturn sum(1,2,3,4)\n", context );
		assertThat( result ).isEqualTo( "1,2,3,4" );
	}

	@Test
	@DisplayName( "varargs: works with a leading required parameter before the variadic one" )
	public void testVarargsWithLeadingParameter() {
		IBoxContext	context	= newContext();
		Object		result	= run(
		    "def concat(String sep, String... parts) {\n"
		        + "  return parts.toList(sep)\n"
		        + "}\n"
		        + "return concat(\",\", \"a\", \"b\", \"c\")\n",
		    context );
		assertThat( result ).isEqualTo( "a,b,c" );
	}

	@Test
	@DisplayName( "varargs: callable with zero extra arguments" )
	public void testVarargsWithZeroExtraArguments() {
		IBoxContext	context	= newContext();
		Object		result	= run( "def sum(int... nums) { return nums.size() }\nreturn sum()\n", context );
		assertThat( result.toString() ).isEqualTo( "0" );
	}

	@Test
	@DisplayName( "varargs: collected values are usable, not a self-referential array from the collection preamble" )
	public void testVarargsValuesAreUsable() {
		// Regression test for a real bug found while building this: the parameter and its
		// "arguments[N]" slot are the same underlying storage, so resetting the parameter to []
		// before finishing reading "arguments" made arguments[N] alias the array being built,
		// corrupting it into containing itself (surfaced as a StackOverflowError out of
		// Array.toString()). Fixed by collecting into a separate temp var first.
		IBoxContext	context	= newContext();
		Object		result	= run(
		    "def sum(int... nums) {\n"
		        + "  total = 0\n"
		        + "  for (n in nums) {\n"
		        + "    total = total + n\n"
		        + "  }\n"
		        + "  return total\n"
		        + "}\n"
		        + "return sum(1, 2, 3, 4)\n",
		    context );
		assertThat( result.toString() ).isEqualTo( "10" );
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

	// -----------------------------------------------------------------------------------------
	// Round 2 idiom-audit gaps

	@Test
	@DisplayName( "hex and binary integer literals, with underscore separators" )
	public void testHexAndBinaryLiterals() {
		IBoxContext context = newContext();
		assertThat( run( "return 0xFF", context ).toString() ).isEqualTo( "255" );
		assertThat( run( "return 0b1010", context ).toString() ).isEqualTo( "10" );
		assertThat( run( "return 0xFF_00", context ).toString() ).isEqualTo( "65280" );
	}

	@Test
	@DisplayName( "octal integer literals, with underscore separators and a type suffix" )
	public void testOctalLiterals() {
		IBoxContext context = newContext();
		assertThat( run( "return 010", context ).toString() ).isEqualTo( "8" );
		assertThat( run( "return 0777", context ).toString() ).isEqualTo( "511" );
		assertThat( run( "return 01_000", context ).toString() ).isEqualTo( "512" );
		assertThat( run( "return 010L", context ).toString() ).isEqualTo( "8" );
		// A lone "0" and a leading-zero literal with a non-octal digit stay plain decimal.
		assertThat( run( "return 0", context ).toString() ).isEqualTo( "0" );
		assertThat( run( "return 09", context ).toString() ).isEqualTo( "9" );
	}

	@Test
	@DisplayName( "underscore digit separators and L/G/F/D type suffixes on ordinary literals" )
	public void testUnderscoreSeparatorsAndTypeSuffixes() {
		IBoxContext context = newContext();
		assertThat( run( "return 1_000_000", context ).toString() ).isEqualTo( "1000000" );
		assertThat( run( "return 10L", context ).toString() ).isEqualTo( "10" );
		assertThat( run( "return 5G", context ).toString() ).isEqualTo( "5" );
		assertThat( run( "return 10.5F", context ).toString() ).isEqualTo( "10.5" );
	}

	@Test
	@DisplayName( "'final' on a local variable declaration is accepted as a no-op" )
	public void testFinalLocalVariable() {
		IBoxContext	context	= newContext();
		Object		result	= run( "final int x = 5\nreturn x + 1\n", context );
		assertThat( result.toString() ).isEqualTo( "6" );
	}

	@Test
	@DisplayName( "tuple destructuring reassignment without 'def' swaps existing variables" )
	public void testDestructuringReassignmentWithoutDef() {
		IBoxContext	context	= newContext();
		Object		result	= run( "def a = 1\ndef b = 2\n(a, b) = [b, a]\nreturn \"${a},${b}\"\n", context );
		assertThat( result ).isEqualTo( "2,1" );
	}

	@Test
	@DisplayName( "spread operator inside list and map literals" )
	public void testSpreadInListAndMapLiterals() {
		IBoxContext	context	= newContext();
		Object		list	= run( "def a = [1, 2]\ndef b = [3, 4]\nreturn [*a, *b, 5]\n", context );
		assertThat( list.toString() ).isEqualTo( "[1, 2, 3, 4, 5]" );

		Object map = run( "def m1 = [a: 1]\ndef m2 = [b: 2]\ndef merged = [*: m1, *: m2, c: 3]\nreturn merged.c\n", context );
		assertThat( map.toString() ).isEqualTo( "3" );
	}

	@Test
	@DisplayName( "spread-dot operator maps a property access over every element" )
	public void testSpreadDotPropertyAccess() {
		// Confirms the correctness fix: before it, "people*.name" silently compiled as a plain
		// dot-access on the collection itself instead of mapping over its elements.
		IBoxContext	context	= newContext();
		Object		result	= run( "def people = [[name: \"Alice\"], [name: \"Bob\"]]\nreturn people*.name\n", context );
		assertThat( result.toString() ).isEqualTo( "[Alice, Bob]" );
	}

	@Test
	@DisplayName( "spread-dot operator maps a method call over every element" )
	public void testSpreadDotMethodCall() {
		IBoxContext	context	= newContext();
		Object		result	= run( "def words = [\"ab\", \"cde\"]\nreturn words*.length()\n", context );
		assertThat( result.toString() ).isEqualTo( "[2, 3]" );
	}

	@Test
	@DisplayName( "import static allows referencing a class's static member bare" )
	public void testImportStaticMember() {
		IBoxContext	context	= newContext();
		Object		result	= run( "import static java.lang.Math.max\nreturn max(3, 7)\n", context );
		assertThat( result.toString() ).isEqualTo( "7" );
	}

	@Test
	@DisplayName( "multi-line fluent method chaining with a leading-dot continuation" )
	public void testMultiLineFluentChaining() {
		IBoxContext	context	= newContext();
		Object		result	= run(
		    "def list = [1, 2, 3, 4]\n"
		        + "return list.findAll { it > 1 }\n"
		        + "    .collect { it * 2 }\n",
		    context );
		assertThat( result.toString() ).isEqualTo( "[4, 6, 8]" );
	}

	// -----------------------------------------------------------------------------------------
	// Round 3: annotations, "<<" append, method pointers, curry()

	@Test
	@DisplayName( "'<<' appends to a collection and returns it for chaining" )
	public void testLeftShiftAppendsToCollection() {
		IBoxContext	context	= newContext();
		Object		result	= run( "def list = [1, 2]\nlist << 3 << 4\nreturn list\n", context );
		assertThat( result.toString() ).isEqualTo( "[1, 2, 3, 4]" );
	}

	@Test
	@DisplayName( "'<<' is still numeric left-shift for numbers" )
	public void testLeftShiftStillNumericForNumbers() {
		IBoxContext	context	= newContext();
		Object		result	= run( "return 1 << 3", context );
		assertThat( result.toString() ).isEqualTo( "8" );
	}

	@Test
	@DisplayName( "unbound method pointer (Type.&method) used as a collection-mapping callback" )
	public void testUnboundMethodPointer() {
		IBoxContext	context	= newContext();
		Object		result	= run( "def words = [\"ab\", \"cd\"]\nreturn words.collect(String.&toUpperCase)\n", context );
		assertThat( result.toString() ).isEqualTo( "[AB, CD]" );
	}

	@Test
	@DisplayName( "bound method pointer (instance.&method) used as a single-argument callback" )
	public void testBoundMethodPointer() {
		IBoxContext	context	= newContext();
		Object		result	= run( "def sums = []\n[1, 2, 3].each(sums.&add)\nreturn sums\n", context );
		assertThat( result.toString() ).isEqualTo( "[1, 2, 3]" );
	}

	@Test
	@DisplayName( "unbound method pointer called directly with two arguments treats the first as the receiver and forwards the second" )
	public void testUnboundMethodPointerForwardsTwoArguments() {
		// Generalization beyond the receiver-only (0 forwarded args) callback case: when the
		// pointer VALUE itself is invoked directly with exactly two arguments (not via a BIF's own
		// non-strict callback convention - see buildUnboundMethodPointer's header comment), the
		// first becomes the receiver and the second is forwarded as the target method's own
		// argument - matching real Groovy's own unbound-method-reference semantics.
		IBoxContext	context	= newContext();
		Object		result	= run(
		    "def f = String.&startsWith\n"
		        + "return f(\"hello\", \"he\")\n",
		    context );
		assertThat( result ).isEqualTo( true );
	}

	@Test
	@DisplayName( "bound method pointer called directly with two arguments forwards both" )
	public void testBoundMethodPointerForwardsTwoArguments() {
		// Generalization beyond the single-argument callback case: when the method pointer VALUE
		// itself is invoked directly with exactly two arguments (not via a BIF's own non-strict
		// callback convention - see buildBoundMethodPointer's header comment), both are forwarded
		// to the target method.
		IBoxContext	context	= newContext();
		Object		result	= run(
		    "class Adder {\n"
		        + "  def add(a, b) { return a + b }\n"
		        + "}\n"
		        + "def adder = new Adder()\n"
		        + "def f = adder.&add\n"
		        + "return f(3, 4)\n",
		    context );
		assertThat( result.toString() ).isEqualTo( "7" );
	}

	@Test
	@DisplayName( "closure.curry() binds leading arguments, callable with the rest later" )
	public void testClosureCurry() {
		IBoxContext	context	= newContext();
		Object		result	= run(
		    "def add = { a, b -> a + b }\n"
		        + "def add5 = add.curry(5)\n"
		        + "return add5(3)\n",
		    context );
		assertThat( result.toString() ).isEqualTo( "8" );
	}

	@Test
	@DisplayName( "command-style call (no parens) invokes a user-defined function" )
	public void testCommandStyleCallExecutesFunctionCall() {
		IBoxContext	context	= newContext();
		Object		result	= run(
		    "def captured = null\n"
		        + "def capture(msg) { captured = msg }\n"
		        + "capture \"hello\"\n"
		        + "return captured\n",
		    context );
		assertThat( result ).isEqualTo( "hello" );
	}

	@Test
	@DisplayName( "command-style call with a bare-identifier argument (println x) executes correctly" )
	public void testCommandStyleCallWithBareIdentifierArgument() {
		// Recognized via GroovyParserControl#isLikelyMethodName's naming-convention heuristic
		// (lowercase-leading call name => not a type name) - not general symbol resolution. Proves
		// both that "println x" actually reaches println (not silently misparsed as a redundant
		// var-decl) and that a same-shaped, uppercase-typed declaration ("int x" uses the primitive
		// keyword token, a separate grammar path entirely) still works unaffected.
		IBoxContext	context	= newContext();
		Object		result	= run(
		    "def message = \"hi from a bare identifier\"\n"
		        + "println message\n"
		        + "int x\n"
		        + "x = 5\n"
		        + "return x\n",
		    context );
		assertThat( result.toString() ).isEqualTo( "5" );
	}

	@Test
	@DisplayName( "command-style call with a bare-identifier argument works for any lowercase-named user function, not just println/print/printf" )
	public void testCommandStyleCallWithBareIdentifierArgumentGeneralizesToUserFunctions() {
		// GroovyParserControl#isLikelyMethodName is a general naming-convention heuristic (any
		// lowercase-leading call name), not a curated list of built-in names - this proves a
		// user-defined function ("capture", never special-cased anywhere) works the same way, while
		// an uppercase-typed declaration ("String other") in the same script still parses as a
		// declaration, unaffected.
		IBoxContext	context	= newContext();
		Object		result	= run(
		    "def captured = null\n"
		        + "def capture(msg) { captured = msg }\n"
		        + "def message = \"hi from a user function\"\n"
		        + "capture message\n"
		        + "String other\n"
		        + "other = captured\n"
		        + "return other\n",
		    context );
		assertThat( result ).isEqualTo( "hi from a user function" );
	}

	@Test
	@DisplayName( "annotations are accepted (and discarded) at runtime, not just at parse time" )
	public void testAnnotationsAreNoOpAtRuntime() {
		IBoxContext	context	= newContext();
		Object		result	= run(
		    "@Deprecated\n"
		        + "def greet(@Deprecated String name) {\n"
		        + "  return \"hi \" + name\n"
		        + "}\n"
		        + "return greet(\"world\")\n",
		    context );
		assertThat( result ).isEqualTo( "hi world" );
	}

	@Test
	@DisplayName( "anonymous inner class at script top level" )
	public void testAnonymousClassAtTopLevel() {
		IBoxContext	context	= newContext();
		Object		result	= run(
		    "def r = new MyTask() {\n"
		        + "  void run() {\n"
		        + "    return \"ran\"\n"
		        + "  }\n"
		        + "}\n"
		        + "return r.run()\n",
		    context );
		assertThat( result ).isEqualTo( "ran" );
	}

	@Test
	@DisplayName( "anonymous inner class used inside a top-level function body" )
	public void testAnonymousClassInsideFunctionBody() {
		// The interesting part: BoxLocalClass has a hard compile-time rule against being nested
		// inside a function/closure/lambda body (see BoxLocalClassTransformer), so the synthesized
		// class is hoisted out to the script's own top level - this proves that hoisting actually
		// works, not just the "already at top level" case above.
		IBoxContext	context	= newContext();
		Object		result	= run(
		    "def makeMyTask() {\n"
		        + "  def r = new MyTask() {\n"
		        + "    void run() {\n"
		        + "      return \"hi\"\n"
		        + "    }\n"
		        + "  }\n"
		        + "  return r.run()\n"
		        + "}\n"
		        + "return makeMyTask()\n",
		    context );
		assertThat( result ).isEqualTo( "hi" );
	}

	@Test
	@DisplayName( "two sibling anonymous classes in the same scope both resolve correctly" )
	public void testTwoAnonymousClassesInSameScope() {
		// Previously a hard error: the hoisting mechanism drained "whatever is currently pending"
		// to build EACH anonymous class's own body, which - since a class is only added to the
		// pending list AFTER its own body finishes building - misattributed an earlier sibling
		// (still pending) as having been discovered INSIDE the later one's body, nesting it there
		// and hiding it from the real enclosing scope entirely. Fixed by giving every class-shaped
		// body its own isolated hoist-scope stack frame (see GroovyExpressionVisitor#
		// pushHoistScope) so siblings can never be confused with descendants.
		IBoxContext	context	= newContext();
		Object		result	= run(
		    "def a = new MyTask() { void run() { return \"a\" } }\n"
		        + "def b = new MyTask() { void run() { return \"b\" } }\n"
		        + "return \"${a.run()}${b.run()}\"\n",
		    context );
		assertThat( result ).isEqualTo( "ab" );
	}

	@Test
	@DisplayName( "three sibling anonymous classes in the same scope all resolve correctly" )
	public void testThreeAnonymousClassesInSameScope() {
		IBoxContext	context	= newContext();
		Object		result	= run(
		    "def a = new MyTask() { void run() { return \"a\" } }\n"
		        + "def b = new MyTask() { void run() { return \"b\" } }\n"
		        + "def c = new MyTask() { void run() { return \"c\" } }\n"
		        + "return \"${a.run()}${b.run()}${c.run()}\"\n",
		    context );
		assertThat( result ).isEqualTo( "abc" );
	}

	@Test
	@DisplayName( "a class declaration mixed with other top-level script statements is supported" )
	public void testClassDeclarationMixedWithTopLevelStatements() {
		// Previously a hard, documented error: a file was only ever "a class" (exactly one
		// top-level class declaration, nothing else) or "a script" (zero top-level classes). Real
		// Groovy allows both together in the same file - the class declaration becomes a
		// BoxLocalClass peer statement in the script (the exact mechanism a named nested class or
		// a hoisted anonymous class already use), reusing AsmTranspiler's existing scan of a
		// BoxScript's statements for BoxLocalClass entries.
		IBoxContext	context	= newContext();
		Object		result	= run(
		    "class Point {\n"
		        + "  def x\n"
		        + "  def y\n"
		        + "  Point(px, py) { x = px; y = py }\n"
		        + "  def sum() { return x + y }\n"
		        + "}\n"
		        + "def p = new Point(3, 4)\n"
		        + "return p.sum()\n",
		    context );
		assertThat( result.toString() ).isEqualTo( "7" );
	}

	@Test
	@DisplayName( "two classes mixed with top-level statements both resolve correctly" )
	public void testTwoClassDeclarationsMixedWithTopLevelStatements() {
		IBoxContext	context	= newContext();
		Object		result	= run(
		    "class Greeter {\n"
		        + "  def greet(name) { return \"hi \" + name }\n"
		        + "}\n"
		        + "class Farewell {\n"
		        + "  def bye(name) { return \"bye \" + name }\n"
		        + "}\n"
		        + "def g = new Greeter()\n"
		        + "def f = new Farewell()\n"
		        + "return \"${g.greet('a')} ${f.bye('b')}\"\n",
		    context );
		assertThat( result ).isEqualTo( "hi a bye b" );
	}

	// -----------------------------------------------------------------------------------------
	// Real Java interop for anonymous classes implementing a known Java interface

	@Test
	@DisplayName( "an anonymous Runnable is a genuine java.lang.Runnable, not just duck-typed" )
	public void testAnonymousRunnableIsRealJavaInterop() {
		// GroovyExpressionVisitor#resolveJavaInterfaceFqn recognizes "Runnable" as a known Java
		// interface, so the constructed instance is wrapped in a real JDK dynamic proxy via
		// createDynamicProxy() - genuinely implementing java.lang.Runnable at the JVM level (an
		// "instanceof" check against it succeeds), unlike the plain BoxLocalClass instance this
		// replaced, which was never a REAL Runnable, merely an object with a same-named method.
		// Proven twice: the "instanceof" check below, and handing it to a real java.lang.Thread's
		// constructor (which only accepts a true Runnable) and running it synchronously (run(),
		// not start(), so this stays deterministic with no threading involved).
		IBoxContext	context	= newContext();
		Object		result	= run(
		    "def r = new Runnable() {\n"
		        + "  void run() {}\n"
		        + "}\n"
		        + "def isRealRunnable = (r instanceof Runnable)\n"
		        + "def thread = createObject(\"java\", \"java.lang.Thread\").init(r)\n"
		        + "thread.run()\n"
		        + "return isRealRunnable\n",
		    context );
		assertThat( result ).isEqualTo( true );
	}

	@Test
	@DisplayName( "a Runnable's own run() returns void when called directly, per real Java semantics" )
	public void testAnonymousRunnableDirectCallReturnsVoid() {
		// Calling .run() directly on the proxy dispatches through the REAL java.lang.Runnable
		// interface method, which real Java declares "void" - so the return value is always
		// null/void here, exactly like real Groovy/Java, even though the underlying method body
		// still executes (the anonymous class's own return statement is simply discarded, which
		// is real Java's own behavior for a void-declared interface method, not a bug).
		IBoxContext	context	= newContext();
		Object		result	= run(
		    "def r = new Runnable() {\n"
		        + "  void run() {\n"
		        + "    return \"ignored\"\n"
		        + "  }\n"
		        + "}\n"
		        + "return r.run()\n",
		    context );
		assertThat( result ).isNull();
	}

	@Test
	@DisplayName( "an anonymous Comparator's compare() returns a real int through the proxy" )
	public void testAnonymousComparatorReturnValuePreserved() {
		// Unlike Runnable.run(), Comparator.compare() is declared to return an int - proving that
		// a non-void interface method's return value DOES survive the proxy round-trip correctly.
		IBoxContext	context	= newContext();
		Object		result	= run(
		    "def c = new Comparator() {\n"
		        + "  int compare(a, b) {\n"
		        + "    return a <=> b\n"
		        + "  }\n"
		        + "}\n"
		        + "return c.compare(3, 5)\n",
		    context );
		assertThat( result.toString() ).isEqualTo( "-1" );
	}

	@Test
	@DisplayName( "an anonymous class for an explicitly imported (non-curated) Java interface is also proxied" )
	public void testAnonymousClassForExplicitlyImportedInterfaceIsRealJavaInterop() {
		// GroovyExpressionVisitor#resolveJavaInterfaceFqn isn't limited to the small curated
		// KNOWN_JAVA_INTERFACES set - "Supplier" isn't in it, but this file's own explicit
		// "import java.util.function.Supplier" is itself a strong signal the name is real, so it
		// still gets wrapped in a genuine JDK dynamic proxy purely because of that import. The
		// instanceof check uses the FULLY qualified name deliberately - resolving a bare name on
		// instanceof's own right-hand side is a separate, pre-existing limitation unrelated to
		// this proxying feature (see toTypeExpression), not something this test means to exercise.
		IBoxContext	context	= newContext();
		Object		result	= run(
		    "import java.util.function.Supplier\n"
		        + "def s = new Supplier() {\n"
		        + "  def get() { return \"hi\" }\n"
		        + "}\n"
		        + "return (s instanceof java.util.function.Supplier)\n",
		    context );
		assertThat( result ).isEqualTo( true );
	}

	@Test
	@DisplayName( "an anonymous class for an unrecognized type name stays a plain, un-proxied instance" )
	public void testAnonymousClassForUnknownTypeStaysPlain() {
		// GroovyExpressionVisitor#resolveJavaInterfaceFqn is deliberately conservative - a bare
		// name that ISN'T a well-known Java interface (a custom/BoxLang-native type, here one
		// that doesn't even exist as a real class at all) falls back to the plain, un-proxied
		// instance exactly as before this feature, rather than failing trying to load a
		// non-existent Java class.
		IBoxContext	context	= newContext();
		Object		result	= run(
		    "def w = new SomeCustomWidget() {\n"
		        + "  def describe() { return \"a widget\" }\n"
		        + "}\n"
		        + "return w.describe()\n",
		    context );
		assertThat( result ).isEqualTo( "a widget" );
	}

}
