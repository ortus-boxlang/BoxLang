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
}
