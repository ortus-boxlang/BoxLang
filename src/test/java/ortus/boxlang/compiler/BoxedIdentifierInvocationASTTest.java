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
package ortus.boxlang.compiler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.compiler.ast.BoxScript;
import ortus.boxlang.compiler.ast.expression.BoxClosure;
import ortus.boxlang.compiler.ast.expression.BoxFunctionInvocation;
import ortus.boxlang.compiler.ast.expression.BoxIdentifier;
import ortus.boxlang.compiler.ast.statement.BoxExpressionStatement;
import ortus.boxlang.compiler.ast.statement.BoxFunctionDeclaration;
import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.compiler.parser.Parser;
import ortus.boxlang.compiler.parser.ParsingResult;
import ortus.boxlang.runtime.BoxRuntime;

/**
 * Regression tests for `#name#()` — the `#...#` OUT string syntax applied to a
 * function call.
 * <p>
 * {@code visitExprOutString} widens the inner expression's SOURCE TEXT to cover the
 * full {@code #...#} so span/source text is correct. That must be SOURCE METADATA
 * only: it must never become the invocation's lookup NAME. {@code visitExprFunctionCall}
 * builds {@code BoxFunctionInvocation} from {@code name.getSourceText()}, so widening
 * an identifier's source text used to leak into the name — making {@code #f#()} look up
 * {@code #f#} instead of {@code f}, with profiling DISABLED (i.e. a plain compile bug).
 */
public class BoxedIdentifierInvocationASTTest {

	@BeforeAll
	public static void setupRuntime() {
		BoxRuntime.getInstance( true );
	}

	private ParsingResult parse( String code ) throws IOException {
		return new Parser().parse( code, BoxSourceType.BOXSCRIPT );
	}

	@Test
	@DisplayName( "It keeps `#name#()` looking up the identifier, not the widened `#name#` text" )
	public void testBoxedIdentifierInvocationUsesLogicalName() throws IOException {
		ParsingResult result = parse( "f = () => 1; result = #f#();" );
		assertTrue( result.isCorrect() );

		BoxFunctionInvocation invocation = ( BoxFunctionInvocation ) result.getRoot()
		    .getDescendants()
		    .stream()
		    .filter( BoxFunctionInvocation.class::isInstance )
		    .findFirst()
		    .orElseThrow( () -> new AssertionError( "expected a BoxFunctionInvocation for #f#()" ) );

		// The LOOKUP NAME must be the identifier's logical name...
		assertEquals( "f", invocation.getName(), "`#f#()` must invoke `f`" );
		// ...while the SOURCE TEXT may still cover the full `#f#` wrapper as metadata.
		assertTrue( invocation.getSourceText().startsWith( "#f#" ),
		    "expected source text to keep the `#f#` wrapper, got: " + invocation.getSourceText() );
	}

	@Test
	@DisplayName( "It keeps `#fn()#` (boxed declaration call) looking up the declaration" )
	public void testBoxedFunctionDeclarationInvocationUsesLogicalName() throws IOException {
		ParsingResult result = parse( "function lookme() { return 1; } result = #lookme#();" );
		assertTrue( result.isCorrect() );

		assertTrue( result.getRoot().getDescendants().stream().anyMatch( BoxFunctionDeclaration.class::isInstance ),
		    "expected the function declaration to survive" );

		BoxFunctionInvocation invocation = ( BoxFunctionInvocation ) result.getRoot()
		    .getDescendants()
		    .stream()
		    .filter( BoxFunctionInvocation.class::isInstance )
		    .findFirst()
		    .orElseThrow( () -> new AssertionError( "expected a BoxFunctionInvocation for #lookme#()" ) );

		assertEquals( "lookme", invocation.getName(), "`#lookme#()` must invoke `lookme`" );
	}

	@Test
	@DisplayName( "It still produces a BoxIdentifier for a boxed identifier (assignment LHS preserved)" )
	public void testBoxedIdentifierStaysIdentifier() throws IOException {
		ParsingResult result = parse( "f = () => 1; g = #f#();" );
		assertTrue( result.isCorrect() );

		// The `#f#` inside the call must remain a BoxIdentifier node (the visitor's
		// whole point is to preserve the inner node, unlike string interpolation).
		boolean hasIdentifier = result.getRoot()
		    .getDescendants()
		    .stream()
		    .anyMatch( node -> node instanceof BoxIdentifier id && "f".equals( id.getName() ) );
		assertTrue( hasIdentifier, "expected `#f#` to stay a BoxIdentifier" );

		// Sanity: the lambda assignment itself is intact.
		BoxScript	script		= ( BoxScript ) result.getRoot();
		boolean		hasLambda	= script.getStatements()
		    .stream()
		    .filter( BoxExpressionStatement.class::isInstance )
		    .map( BoxExpressionStatement.class::cast )
		    .anyMatch( stmt -> stmt.getExpression() instanceof ortus.boxlang.compiler.ast.expression.BoxAssignment );
		assertTrue( hasLambda, "expected the `f = () => 1` assignment statement" );
		assertTrue( result.getRoot().getDescendants().stream().anyMatch( BoxClosure.class::isInstance ), "expected the closure to survive" );
	}

	@Test
	@DisplayName( "It parses the same `#name#()` form in CF script source" )
	public void testBoxedIdentifierInvocationInCFScript() throws IOException {
		ParsingResult result = new Parser().parse( "lookme = function() { return 1; }\nresult = #lookme#();", BoxSourceType.CFSCRIPT );
		assertTrue( result.isCorrect(), "parse issues: " + result.getIssues() );

		BoxFunctionInvocation invocation = ( BoxFunctionInvocation ) result.getRoot()
		    .getDescendants()
		    .stream()
		    .filter( BoxFunctionInvocation.class::isInstance )
		    .findFirst()
		    .orElseThrow( () -> new AssertionError( "expected a BoxFunctionInvocation for #lookme#()" ) );

		assertEquals( "lookme", invocation.getName(), "`#lookme#()` must invoke `lookme` in CF script too" );
	}

	/**
	 * The invocation lookup name for a boxed KEYWORD must be resolved from the node's
	 * typed accessor (boolean value / scope name / literal null), NOT the widened
	 * source text — otherwise `#true#()` would look up `#true#` instead of `true`.
	 * A boolean literal, a scope keyword and `null` all widen their source text to the
	 * full `#...#` wrapper exactly like an identifier does.
	 */
	private String boxedInvocationName( String code, BoxSourceType type ) throws IOException {
		ParsingResult result = new Parser().parse( code, type );
		assertTrue( result.isCorrect(), "parse issues: " + result.getIssues() );
		return ( ( BoxFunctionInvocation ) result.getRoot()
		    .getDescendants()
		    .stream()
		    .filter( BoxFunctionInvocation.class::isInstance )
		    .findFirst()
		    .orElseThrow( () -> new AssertionError( "expected a BoxFunctionInvocation" ) ) ).getName();
	}

	@Test
	@DisplayName( "It resolves `#true#()` / `#false#()` to the literal names, not the widened text" )
	public void testBoxedBooleanLiteralInvocationUsesLiteralName() throws IOException {
		assertEquals( "true", boxedInvocationName( "variables[\"true\"] = () => 1; result = #true#();", BoxSourceType.BOXSCRIPT ),
		    "`#true#()` must invoke `true`" );
		assertEquals( "false", boxedInvocationName( "variables[\"false\"] = () => 1; result = #false#();", BoxSourceType.BOXSCRIPT ),
		    "`#false#()` must invoke `false`" );
	}

	@Test
	@DisplayName( "It resolves `#null#()` to the literal name `null`, not the widened text" )
	public void testBoxedNullInvocationUsesLiteralName() throws IOException {
		assertEquals( "null", boxedInvocationName( "variables[\"null\"] = () => 1; result = #null#();", BoxSourceType.BOXSCRIPT ),
		    "`#null#()` must invoke `null`" );
	}

	@Test
	@DisplayName( "It resolves a boxed SCOPE name (`#variables#()`) to the scope name" )
	public void testBoxedScopeInvocationUsesScopeName() throws IOException {
		assertEquals( "variables", boxedInvocationName( "result = #variables#();", BoxSourceType.BOXSCRIPT ),
		    "`#variables#()` must invoke `variables`" );
	}

	@Test
	@DisplayName( "It resolves boxed boolean/null/scope names in CF script too" )
	public void testBoxedKeywordInvocationInCFScript() throws IOException {
		assertEquals( "true", boxedInvocationName( "variables[\"true\"] = function() { return 1; }\nresult = #true#();", BoxSourceType.CFSCRIPT ) );
		assertEquals( "null", boxedInvocationName( "variables[\"null\"] = function() { return 1; }\nresult = #null#();", BoxSourceType.CFSCRIPT ) );
		assertEquals( "variables", boxedInvocationName( "result = #variables#();", BoxSourceType.CFSCRIPT ) );
	}

	/**
	 * A COMMENT inside a boxed keyword must not become part of the lookup name. The lexer sends
	 * comments to the hidden channel, so they never appear in the token stream the parser sees —
	 * but {@code visitExprOutString} widens the source text by slicing the RAW character stream,
	 * which DOES include the comment. The lookup name must come from the keyword's typed value,
	 * never from that widened text. Comments may sit BEFORE or AFTER the keyword.
	 */
	@Test
	@DisplayName( "It ignores comments inside a boxed keyword when resolving the invocation name" )
	public void testCommentedBoxedKeywordInvocationUsesTypedValue() throws IOException {
		// Leading comment.
		assertEquals( "true", boxedInvocationName( "variables[\"true\"] = () => 1; result = #/*note*/true#();", BoxSourceType.BOXSCRIPT ),
		    "a leading comment inside `#...#` must not become part of the invocation name" );
		assertEquals( "false", boxedInvocationName( "variables[\"false\"] = () => 1; result = #/*note*/false#();", BoxSourceType.BOXSCRIPT ),
		    "a leading comment inside `#...#` must not become part of the invocation name" );
		assertEquals( "null", boxedInvocationName( "variables[\"null\"] = () => 1; result = #/*note*/null#();", BoxSourceType.BOXSCRIPT ),
		    "a leading comment inside `#...#` must not become part of the invocation name" );
		// Trailing comment — the keyword must not be resolved as `true /* note */`.
		assertEquals( "true", boxedInvocationName( "variables[\"true\"] = () => 1; result = #true /* note */#();", BoxSourceType.BOXSCRIPT ),
		    "a trailing comment inside `#...#` must not become part of the invocation name" );
		assertEquals( "null", boxedInvocationName( "variables[\"null\"] = () => 1; result = #null /* note */#();", BoxSourceType.BOXSCRIPT ),
		    "a trailing comment inside `#...#` must not become part of the invocation name" );
		// Both syntaxes.
		assertEquals( "true", boxedInvocationName( "variables[\"true\"] = function() { return 1; }\nresult = #true /* note */#();", BoxSourceType.CFSCRIPT ),
		    "a trailing comment inside `#...#` must not become part of the invocation name in CF script either" );
	}
}
