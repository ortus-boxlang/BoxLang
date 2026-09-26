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
package ortus.boxlang.compiler.parser;

import static com.google.common.truth.Truth.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.parser.antlr.GroovyGrammar;
import ortus.boxlang.parser.antlr.GroovyLexer;

/**
 * Phase 1 grammar-level tests for the Groovy parser/transpiler effort.
 * <p>
 * These tests exercise {@code GroovyLexer}/{@code GroovyGrammar} directly (the raw ANTLR
 * grammar, not yet wrapped by a {@code GroovyParser} that builds the shared BoxLang AST -
 * that wrapper is Phase 2 work). The goal here is to verify the grammar itself: that it
 * accepts representative real-world Groovy syntax and rejects genuinely invalid input, and
 * to pin down the currently-out-of-scope constructs as explicit, documented failures rather
 * than silent gaps.
 */
public class GroovyGrammarParsingTest {

	@Test
	@DisplayName( "class with field, method, and GString interpolation" )
	public void testClassWithGString() {
		assertParses( """
		              class Greeter {
		                String prefix = "Hi"
		                def greet(String name) {
		                  def msg = "${prefix}, ${name}!"
		                  return msg
		                }
		              }
		              """ );
	}

	@Test
	@DisplayName( "GString interpolation with a nested closure brace" )
	public void testGStringNestedBraces() {
		// This is the exact case the Phase 0 spike flagged as broken (mode-stack-only brace
		// handling can't tell an interpolation's closing brace from a nested closure's own).
		// The real fix - per-frame brace-depth counting, mirroring CFLexer's
		// expressionCountStack - is implemented in GroovyLexer.g4; this test pins it down.
		assertParses( "def total = \"Names: ${items.collect { it.name }}\"\n" );
	}

	@Test
	@DisplayName( "multi-statement script with no semicolons" )
	public void testNewlineSignificantStatements() {
		assertParses( """
		              def x = 1
		              def y = 2
		              def z = x + y
		              return z
		              """ );
	}

	@Test
	@DisplayName( "semicolons are still accepted alongside newline separation" )
	public void testSemicolonsStillWork() {
		assertParses( "def x = 1; def y = 2; return x + y;\n" );
	}

	@Test
	@DisplayName( "newlines inside parens do not terminate the statement" )
	public void testNewlineSuppressedInsideParens() {
		assertParses( """
		              def result = Math.max(
		                1,
		                2
		              )
		              """ );
	}

	@Test
	@DisplayName( "expression precedence: arithmetic, ternary, elvis, safe-nav, range, instanceof, logical" )
	public void testExpressionPrecedence() {
		assertParses( """
		              def a = 1 + 2 * 3 - 4 / 2
		              def b = a > 1 ? a : 0
		              def c = a?.toString()
		              def d = a ?: 5
		              def e = 1..10
		              def f = (a instanceof Integer) && (b > 0 || c != null)
		              """ );
	}

	@Test
	@DisplayName( "control flow: if/else, while, classic for, for-in, try/catch/finally, throw" )
	public void testControlFlow() {
		assertParses( """
		              def run(items) {
		                if (items == null) {
		                  throw new IllegalArgumentException("null")
		                } else {
		                  def total = 0
		                  for (i = 0; i < items.size(); i++) {
		                    total += items[i]
		                  }
		                  for (item in items) {
		                    total += item
		                  }
		                  while (total > 1000) {
		                    total = total - 1000
		                  }
		                  try {
		                    return total
		                  } catch (Exception e) {
		                    return -1
		                  } finally {
		                    println("done")
		                  }
		                }
		              }
		              """ );
	}

	@Test
	@DisplayName( "list and map literals, including empty forms" )
	public void testListAndMapLiterals() {
		assertParses( """
		              def list = [1, 2, 3]
		              def map = [a: 1, b: 2]
		              def emptyList = []
		              def emptyMap = [:]
		              """ );
	}

	@Test
	@DisplayName( "chained method calls with a trailing closure argument" )
	public void testChainedCallsWithTrailingClosure() {
		assertParses( "def total = items.findAll { it > 0 }.collect { it * 2 }.sum()\n" );
	}

	@Test
	@DisplayName( "package and import statements, including static import" )
	public void testPackageAndImports() {
		assertParses( """
		              package com.example.app
		              import java.util.List
		              import static java.lang.Math.max
		              class Foo {}
		              """ );
	}

	@Test
	@DisplayName( "interface declaration with a bodyless method signature" )
	public void testInterfaceDeclaration() {
		// Interface (and abstract) methods have no body - "block" must be optional on
		// methodDeclaration. A prior version of this grammar required it, which broke this.
		assertParses( """
		              interface Shape {
		                def area()
		              }
		              """ );
	}

	@Test
	@DisplayName( "top-level script function declaration (not inside a class)" )
	public void testTopLevelMethodDeclaration() {
		// Groovy scripts can define methods directly at script scope, not just inside a
		// class body. A prior version of this grammar only allowed methodDeclaration as a
		// classMember, which broke this extremely common script-level pattern.
		assertParses( """
		              def run(items) {
		                return items.size()
		              }
		              """ );
	}

	@Test
	@DisplayName( "closures with typed params, untyped params, and implicit 'it'" )
	public void testClosureForms() {
		assertParses( """
		              def adder = { int a, int b -> a + b }
		              def doubler = { it * 2 }
		              def noArgs = { 42 }
		              """ );
	}

	@Test
	@DisplayName( "switch statement with fall-through and default" )
	public void testSwitchStatement() {
		assertParses( """
		              switch (x) {
		                case 1:
		                  doThing()
		                  break
		                case 2:
		                case 3:
		                  doOther()
		                  break
		                default:
		                  doDefault()
		              }
		              """ );
	}

	@Test
	@DisplayName( "labeled loop with labeled break/continue" )
	public void testLabeledLoop() {
		assertParses( """
		              outer: for (i in 1..3) {
		                for (j in 1..3) {
		                  if (j == 2) continue outer
		                  if (i == 3) break outer
		                }
		              }
		              """ );
	}

	@Test
	@DisplayName( "assert statement, with and without a message" )
	public void testAssertStatement() {
		assertParses( "assert x > 0\n" );
		assertParses( "assert x > 0 : \"x must be positive\"\n" );
	}

	@Test
	@DisplayName( "spread call argument, alone and mixed with positional arguments" )
	public void testSpreadCallArgument() {
		assertParses( "Math.max(*list)\n" );
		assertParses( "Math.max(1, *list)\n" );
	}

	@Test
	@DisplayName( "annotations parse (and are discarded) on classes, methods, fields, constructors, and parameters" )
	public void testAnnotationsParse() {
		assertParses( """
		              @Deprecated
		              @SuppressWarnings("unchecked")
		              class Foo {
		                @Deprecated
		                def name = "x"

		                @Override
		                String toString() {
		                  return name
		                }

		                @Deprecated
		                Foo() {
		                }

		                def greet(@Deprecated String who) {
		                  return "hi " + who
		                }
		              }
		              """ );
	}

	@Test
	@DisplayName( "tuple declaration, requires at least two names" )
	public void testTupleDeclStatement() {
		assertParses( "def (a, b) = [1, 2]\n" );
		assertParses( "def (a, b, c) = [1, 2, 3]\n" );
		// A single-name "tuple" isn't valid Groovy syntax - falls back to (and fails as) a
		// parenthesized primary expression, not this rule.
		assertFailsToParse( "def (a) = [1]\n" );
	}

	@Test
	@DisplayName( "'in' membership operator" )
	public void testInOperator() {
		assertParses( "def result = 2 in [1, 2, 3]\n" );
	}

	@Test
	@DisplayName( "named/map call arguments" )
	public void testNamedCallArguments() {
		assertParses( "greet(name: \"world\", loud: true)\n" );
	}

	@Test
	@DisplayName( "trailing closure combined with a parenthesized argument list" )
	public void testCallWithTrailingClosure() {
		assertParses( "list.inject(0) { acc, x -> acc + x }\n" );
	}

	@Test
	@DisplayName( "spaceship operator" )
	public void testSpaceshipOperator() {
		assertParses( "def result = 1 <=> 2\n" );
	}

	@Test
	@DisplayName( "top-level enum declaration" )
	public void testEnumDeclaration() {
		assertParses( "enum Color { RED, GREEN, BLUE }\n" );
		assertParses( "enum Color {\n  RED,\n  GREEN,\n  BLUE\n}\n" );
	}

	@Test
	@DisplayName( "triple-quoted multi-line string, with embedded quotes and interpolation" )
	public void testTripleQuotedString() {
		assertParses( "def s = \"\"\"line1\nline2\"\"\"\n" );
		assertParses( "def s = \"\"\"he said \"hi\" there\"\"\"\n" );
		assertParses( "def s = \"\"\"Hello, ${name}!\"\"\"\n" );
	}

	@Test
	@DisplayName( "regex find/match operators with slashy literals" )
	public void testRegexOperators() {
		assertParses( "def result = \"hello\" =~ /l+/\n" );
		assertParses( "def result = \"hello\" ==~ /hello/\n" );
	}

	@Test
	@DisplayName( "division still parses correctly in every position a slashy-string could be confused for" )
	public void testDivisionNotConfusedWithSlashyString() {
		assertParses( "def x = 10 / 2\n" );
		assertParses( "def x = 10\ndef y = x / 2\n" );
		assertParses( "def x = (5 + 5) / 2\n" );
		assertParses( "def a = 100\ndef b = 5\ndef c = 2\ndef result = a / b / c\n" );
		assertParses( "def x = 10\nx++\ndef y = x / 2\n" );
	}

	@Test
	@DisplayName( "varargs (variadic) trailing parameter" )
	public void testVarargsParameter() {
		assertParses( "def sum(int... nums) { return nums.size() }\n" );
		assertParses( "def concat(String sep, String... parts) { return parts.toList(sep) }\n" );
	}

	@Test
	@DisplayName( "unterminated string literal is rejected" )
	public void testUnterminatedStringFails() {
		assertFailsToParse( "def x = \"unterminated\n" );
	}

	@Test
	@DisplayName( "mismatched braces are rejected" )
	public void testMismatchedBracesFails() {
		assertFailsToParse( """
		                    class Foo {
		                      def bar() {
		                    }
		                    """ );
	}

	@Test
	@DisplayName( "two statements on one line with no separator are rejected" )
	public void testMissingSeparatorFails() {
		assertFailsToParse( "def x = 1 def y = 2\n" );
	}

	@Test
	@DisplayName( "command-style call (no parens) with a literal, list, closure, or named argument" )
	public void testCommandStyleCall() {
		assertParses( "println \"done\"\n" );
		assertParses( "println 42\n" );
		assertParses( "println true\n" );
		assertParses( "println [1, 2, 3]\n" );
		assertParses( "apply plugin: \"groovy\"\n" );
		assertParses( "println \"a\", \"b\"\n" );
	}

	@Test
	@DisplayName( "command-style call ambiguities resolve in favor of the pre-existing grammar" )
	public void testCommandStyleCallDoesNotShadowExistingAmbiguities() {
		// A bare "IDENTIFIER IDENTIFIER" is genuinely ambiguous with a typed local declaration
		// ("Type varName") - GroovyParserControl#isCommandStyleCallStart deliberately never
		// recognizes a second bare identifier as a command-call argument, so this still parses as
		// (and only as) a typed declaration, exactly as before this feature existed.
		assertParses( "String bar\n" );
		assertParses( "int x\n" );
		assertParses( "long total\n" );
		// A leading unary +/- must still read as a single arithmetic expression statement, not a
		// command-style call with a unary-prefixed argument.
		assertParses( "x + 1\n" );
		assertParses( "x - 1\n" );
		// A bare identifier argument is recognized as a command call only for a curated,
		// unambiguous call-name set (println/print/printf - see GroovyParserControl#
		// bareIdentifierCommandNames), since those names can never legitimately be a type name in
		// a "Type varName" declaration. Any other bare-identifier-argument call (a user-defined
		// function name) remains a documented, narrower gap, and still parses the pre-existing
		// way: as a (valid, if unusual) varDeclStatement.
		assertParses( "println x\n" );
		assertParses( "shout x\n" );
	}

	/**
	 * Parses the given source with {@code GroovyGrammar.compilationUnit()} and asserts it
	 * parses with zero syntax errors.
	 */
	private void assertParses( String source ) {
		assertThat( parse( source ) ).isEmpty();
	}

	/**
	 * Parses the given source and asserts it produces at least one syntax error.
	 */
	private void assertFailsToParse( String source ) {
		assertThat( parse( source ) ).isNotEmpty();
	}

	private List<String> parse( String source ) {
		GroovyLexer			lexer		= new GroovyLexer( CharStreams.fromString( source ) );
		CommonTokenStream	tokens		= new CommonTokenStream( lexer );
		GroovyGrammar		parser		= new GroovyGrammar( tokens );

		List<String>		errors		= new ArrayList<>();
		BaseErrorListener	listener	= new BaseErrorListener() {

											@Override
											public void syntaxError( Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine,
											    String msg,
											    RecognitionException e ) {
												errors.add( "line " + line + ":" + charPositionInLine + " " + msg );
											}
										};
		lexer.removeErrorListeners();
		lexer.addErrorListener( listener );
		parser.removeErrorListeners();
		parser.addErrorListener( listener );

		try {
			parser.compilationUnit();
		} catch ( RuntimeException e ) {
			errors.add( e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage() );
		}

		return errors;
	}

}
