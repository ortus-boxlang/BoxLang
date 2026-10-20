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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.runnables.RunnableLoader;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * Empirical sweep: every `#expr#` (the "OUT string" wrapper) must evaluate to EXACTLY
 * the same result as the bare `expr`.
 * <p>
 * {@code visitExprOutString} widens the inner node's SOURCE TEXT to cover the whole
 * {@code #...#} wrapper. That is source METADATA only, but several code paths derive
 * SEMANTIC values (lookup names, keys, masks) from {@code getSourceText()}. Those
 * paths silently break when the node is wrapped in pounds. This test pins the
 * behavior for every expression shape that flows through one of those paths.
 */
public class BoxedExpressionExecutionTest {

	@BeforeAll
	public static void setupRuntime() {
		BoxRuntime.getInstance( true );
		// Compiled classes are keyed by MD5 of the source, and ad-hoc source has no file
		// timestamp to invalidate them — so drop any classes left on disk by an earlier
		// (pre-fix) run before this class executes.
		RunnableLoader.getInstance().getBoxpiler().clearClassFiles();
	}

	/**
	 * Drop the in-JVM compiled-class pool so every ad-hoc source is recompiled from
	 * the CURRENT main classes rather than reused from the pool.
	 */
	@BeforeEach
	void resetClassPool() {
		RunnableLoader.getInstance().getBoxpiler().clearPagePool();
	}

	/**
	 * Executes {@code body} and returns the value it leaves in the {@code result} variable.
	 */
	private Object run( String body, BoxSourceType type ) throws IOException {
		BoxRuntime	runtime	= BoxRuntime.getInstance();
		IBoxContext	context	= new ScriptingRequestBoxContext( runtime.getRuntimeContext() );
		runtime.executeSource( body, context, type );
		return context.getScopeNearby( VariablesScope.name ).get( Key.of( "result" ) );
	}

	/**
	 * Asserts that {@code boxed} produces the same {@code result} as {@code bare}.
	 */
	private void assertSame( String label, String bare, String boxed, BoxSourceType type ) throws IOException {
		Object	expected	= run( bare, type );
		Object	actual		= run( boxed, type );
		assertEquals( expected, actual, label + " — boxed form must match the bare form (" + type + ")" );
	}

	private void assertSameBoth( String label, String bare, String boxed ) throws IOException {
		assertSame( label, bare, boxed, BoxSourceType.BOXSCRIPT );
		assertSame( label, bare, boxed, BoxSourceType.CFSCRIPT );
	}

	@Test
	@DisplayName( "A boxed identifier evaluates like the bare identifier" )
	public void testBoxedIdentifierValue() throws IOException {
		assertSameBoth( "identifier value", "x = 42; result = x;", "x = 42; result = #x#;" );
	}

	@Test
	@DisplayName( "A boxed literal evaluates like the bare literal" )
	public void testBoxedLiterals() throws IOException {
		assertSameBoth( "boolean literal", "result = true;", "result = #true#;" );
		assertSameBoth( "null literal", "result = null;", "result = #null#;" );
		assertSameBoth( "integer literal", "result = 5;", "result = #5#;" );
		assertSameBoth( "string literal", "result = 'x';", "result = #'x'#;" );
	}

	@Test
	@DisplayName( "A boxed identifier used as a function NAME looks up the bare name" )
	public void testBoxedInvocationName() throws IOException {
		assertSameBoth( "invocation name", "f = () => 1; result = f();", "f = () => 1; result = #f#();" );
		assertSameBoth( "invocation name + args", "f = (a,b) => a+b; result = f(1,2);", "f = (a,b) => a+b; result = #f#(1,2);" );
	}

	@Test
	@DisplayName( "A boxed keyword used as a DOT-ACCESS key looks up the bare key" )
	public void testBoxedDotAccessKey() throws IOException {
		assertSameBoth( "boolean key", "s = {'true': 1}; result = s.true;", "s = {'true': 1}; result = s.#true#;" );
		assertSameBoth( "null key", "s = {'null': 1}; result = s.null;", "s = {'null': 1}; result = s.#null#;" );
		assertSameBoth( "scope key", "s = {'variables': 1}; result = s.variables;", "s = {'variables': 1}; result = s.#variables#;" );
	}

	@Test
	@DisplayName( "A dot-access key keeps its ORIGINAL spelling (case-sensitive struct)" )
	public void testKeywordDotAccessPreservesCase() throws IOException {
		// A dot dereference is a STATIC name lookup, so `s.TRUE` must request the key spelled
		// `TRUE` — never the boolean value spelled `true`.
		assertEquals( 1, run( "s = structNew( 'casesensitive' ); s['TRUE'] = 1; result = s.TRUE;", BoxSourceType.BOXSCRIPT ) );
		assertEquals( 2, run( "s = structNew( 'casesensitive' ); s['TrUe'] = 2; result = s.TrUe;", BoxSourceType.BOXSCRIPT ) );
		// ...and a different spelling must NOT resolve, proving the case is significant.
		assertThrows( BoxRuntimeException.class,
		    () -> run( "s = structNew( 'casesensitive' ); s['TRUE'] = 1; result = s.true;", BoxSourceType.BOXSCRIPT ) );
	}

	@Test
	@DisplayName( "A dot-access method name keeps its ORIGINAL spelling (case-sensitive struct)" )
	public void testKeywordMethodNamePreservesCase() throws IOException {
		assertEquals( 7, run( "s = structNew( 'casesensitive' ); s['TRUE'] = () => 7; result = s.TRUE();", BoxSourceType.BOXSCRIPT ) );
		assertEquals( 8, run( "s = structNew( 'casesensitive' ); s['TrUe'] = () => 8; result = s.TrUe();", BoxSourceType.BOXSCRIPT ) );
	}

	@Test
	@DisplayName( "A boxed identifier used as a METHOD name is invoked on the bare name" )
	public void testBoxedMethodInvocation() throws IOException {
		assertSameBoth( "method name", "s = {'foo': () => 7}; result = s.foo();", "s = {'foo': () => 7}; result = s.#foo#();" );
	}

	@Test
	@DisplayName( "Boxed operands of a dot-access chain resolve each key" )
	public void testBoxedDotAccessChain() throws IOException {
		assertSameBoth( "boxed chain", "o = {'inner': {'k': 9}}; result = o.inner.k;", "o = {'inner': {'k': 9}}; result = #o#.#inner#.#k#;" );
	}

	@Test
	@DisplayName( "A boxed array-access index evaluates like the bare index" )
	public void testBoxedArrayAccess() throws IOException {
		assertSameBoth( "array index", "a = [10,20,30]; i = 2; result = a[i];", "a = [10,20,30]; i = 2; result = a[#i#];" );
	}

	@Test
	@DisplayName( "A boxed argument evaluates like the bare argument" )
	public void testBoxedArgument() throws IOException {
		assertSameBoth( "argument", "f = (x) => x + 1; result = f(1);", "f = (x) => x + 1; result = f(#1#);" );
	}

	@Test
	@DisplayName( "A boxed operand of a binary operation evaluates like the bare operand" )
	public void testBoxedBinaryOperand() throws IOException {
		assertSameBoth( "binary operand", "result = 1 + 2;", "result = #1# + 2;" );
		assertSameBoth( "both operands", "result = 1 + 2;", "result = #1# + #2#;" );
	}

	@Test
	@DisplayName( "A boxed branch of a ternary evaluates like the bare branch" )
	public void testBoxedTernaryBranch() throws IOException {
		assertSameBoth( "then branch", "result = true ? 1 : 2;", "result = true ? #1# : 2;" );
		assertSameBoth( "else branch", "result = false ? 1 : 2;", "result = false ? 1 : #2#;" );
	}

	@Test
	@DisplayName( "A boxed assignment LHS assigns to the bare identifier" )
	public void testBoxedAssignmentLhs() throws IOException {
		assertSameBoth( "assignment lhs", "x = 0; x = 5; result = x;", "x = 0; #x# = 5; result = x;" );
	}

	@Test
	@DisplayName( "A boxed expression in a return evaluates like the bare expression" )
	public void testBoxedReturn() throws IOException {
		assertSameBoth( "return value",
		    "f = () => { return 3; }; result = f();",
		    "f = () => { return #3#; }; result = f();" );
	}

	@Test
	@DisplayName( "Boxed expressions nested inside a struct literal keep their values" )
	public void testBoxedInStructLiteral() throws IOException {
		assertSameBoth( "struct member value", "s = {'a': 1}; result = s.a;", "s = {'a': #1#}; result = s.a;" );
	}

	@Test
	@DisplayName( "A boxed expression used as a match/case value evaluates like the bare value" )
	public void testBoxedSwitchCase() throws IOException {
		String	bare	= """
		                  x = 2;
		                  result = 0;
		                  switch( x ) {
		                      case 1: result = 10; break;
		                      case 2: result = 20; break;
		                  }
		                  """;
		String	boxed	= """
		                  x = 2;
		                  result = 0;
		                  switch( x ) {
		                      case #2#: result = 20; break;
		                  }
		                  """;
		assertSameBoth( "switch case", bare, boxed );
	}

	@Test
	@DisplayName( "A boxed for-in list expression iterates the same values" )
	public void testBoxedForIn() throws IOException {
		String	bare	= """
		                  total = 0;
		                  for( i in [1,2,3] ) { total = total + i; }
		                  result = total;
		                  """;
		String	boxed	= """
		                  total = 0;
		                  for( i in #[1,2,3]# ) { total = total + i; }
		                  result = total;
		                  """;
		assertSameBoth( "for-in list", bare, boxed );
	}

	@Test
	@DisplayName( "The numberFormat mask preserves the literal text even when boxed" )
	public void testBoxedNumberFormatMask() throws IOException {
		// The CF transpiler uses the numeric literal's SOURCE TEXT as the mask string
		// (to preserve things like leading zeros). Boxing must not leak the pounds.
		assertEquals( "3.14", String.valueOf( run( "result = numberFormat( 3.14159, 0.00 );", BoxSourceType.CFSCRIPT ) ) );
		assertEquals( "3.14", String.valueOf( run( "result = numberFormat( 3.14159, #0.00# );", BoxSourceType.CFSCRIPT ) ),
		    "a boxed numberFormat mask must not include the `#...#` wrapper" );
		// Leading zeros in the mask must also survive boxing.
		assertEquals( "005.500", String.valueOf( run( "result = numberFormat( 5.5, 000.000 );", BoxSourceType.CFSCRIPT ) ) );
		assertEquals( "005.500", String.valueOf( run( "result = numberFormat( 5.5, #000.000# );", BoxSourceType.CFSCRIPT ) ),
		    "a boxed numberFormat mask must preserve leading zeros" );
		// Whitespace PADDING inside the pounds is boxed-expression syntax, not part of the mask.
		assertEquals( "005.500", String.valueOf( run( "result = numberFormat( 5.5, # 000.000 # );", BoxSourceType.CFSCRIPT ) ),
		    "a boxed numberFormat mask must not include the surrounding whitespace" );
		// A COMMENT inside the pounds is hidden-channel trivia, never part of the mask.
		assertEquals( "005.500", String.valueOf( run( "result = numberFormat( 5.5, #/*note*/000.000# );", BoxSourceType.CFSCRIPT ) ),
		    "a boxed numberFormat mask must not include an embedded comment" );
		// CFML template comments (`<!--- --->`) are also hidden-channel trivia, and they NEST.
		assertEquals( "005.500", String.valueOf( run( "result = numberFormat( 5.5, #<!---mask--->000.000# );", BoxSourceType.CFSCRIPT ) ),
		    "a boxed numberFormat mask must not include a leading CFML comment" );
		assertEquals( "005.500", String.valueOf( run( "result = numberFormat( 5.5, #000.000<!---mask---># );", BoxSourceType.CFSCRIPT ) ),
		    "a boxed numberFormat mask must not include a trailing CFML comment" );
		assertEquals( "005.500", String.valueOf( run( "result = numberFormat( 5.5, #<!---a<!---b--->c--->000.000# );", BoxSourceType.CFSCRIPT ) ),
		    "a boxed numberFormat mask must not include a NESTED CFML comment" );
		// Same in a CF tag expression.
		assertEquals( "005.500", String.valueOf( run( "<cfset result = numberFormat( 5.5, #<!---mask--->000.000# )>", BoxSourceType.CFTEMPLATE ) ),
		    "a CFML comment inside a tag-expression mask must not leak into the mask" );
	}

	@Test
	@DisplayName( "A boxed array/struct literal evaluates like the bare literal" )
	public void testBoxedLiteralsComposite() throws IOException {
		assertSameBoth( "array literal", "result = [1,2,3].len();", "result = #[1,2,3]#.len();" );
		assertSameBoth( "struct literal", "result = {'a':1}.a;", "result = #{'a':1}#.a;" );
	}

	@Test
	@DisplayName( "A boxed expression inside a string interpolation still interpolates" )
	public void testBoxedInsideInterpolation() throws IOException {
		assertSameBoth( "interpolation",
		    "x = 7; result = 'v=#x#';",
		    "x = 7; result = 'v=#x#';" );
	}

	@Test
	@DisplayName( "Boxed prefix/postfix increment targets the bare identifier" )
	public void testBoxedIncrement() throws IOException {
		assertSameBoth( "postfix",
		    "x = 1; x++; result = x;",
		    "x = 1; #x#++; result = x;" );
		assertSameBoth( "prefix",
		    "x = 1; ++x; result = x;",
		    "x = 1; ++#x#; result = x;" );
	}

	@Test
	@DisplayName( "Boxed operands of concat and elvis behave like the bare forms" )
	public void testBoxedConcatAndElvis() throws IOException {
		assertSameBoth( "concat", "result = 'a' & 'b';", "result = #'a'# & #'b'#;" );
		assertSameBoth( "elvis", "x = ''; result = x ?: 'z';", "x = ''; result = #x# ?: 'z';" );
	}

	@Test
	@DisplayName( "A boxed expression survives pretty-printing round trip" )
	public void testBoxedPrettyPrintRoundTrip() throws IOException {
		// Sanity: the widened source text must not corrupt pretty printing of a boxed value.
		runtimeParse( "x = 1; result = #x#;" );
	}

	private void runtimeParse( String code ) throws IOException {
		var result = new ortus.boxlang.compiler.parser.Parser().parse( code, BoxSourceType.BOXSCRIPT );
		assertTrue( result.isCorrect(), "parse issues: " + result.getIssues() );
		List<String> pretty = List.of( result.getRoot().toString().split( "\n" ) );
		assertTrue( !pretty.isEmpty(), "pretty print produced no output" );
	}
}
