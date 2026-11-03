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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.compiler.ast.expression.BoxDotAccess;
import ortus.boxlang.compiler.ast.expression.BoxIdentifier;
import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.compiler.parser.Parser;
import ortus.boxlang.compiler.parser.ParsingResult;
import ortus.boxlang.runtime.BoxRuntime;

/**
 * Regression tests for `obj.#name#` — the `#...#` OUT string syntax used as the
 * RIGHT-hand element of a dot access.
 * <p>
 * {@code visitExprOutString} widens the inner expression's SOURCE TEXT to cover the
 * full {@code #...#} so span/source text is correct. That must be SOURCE METADATA
 * only: it must never become the dot access key. {@code convertDotElement} builds the
 * access {@code BoxIdentifier} for boolean/null/scope keywords, and it used to take the
 * name from {@code expr.getSourceText()} — making {@code s.#true#} look up the key
 * {@code #true#} instead of {@code true}, with profiling DISABLED (i.e. a plain
 * compile bug).
 */
public class BoxedDotAccessASTTest {

	@BeforeAll
	public static void setupRuntime() {
		BoxRuntime.getInstance( true );
	}

	/**
	 * Parses the given code and returns the NAME of the dot access key for the first
	 * {@code BoxDotAccess} found in the tree.
	 */
	private String dotAccessName( String code, BoxSourceType type ) throws IOException {
		ParsingResult result = new Parser().parse( code, type );
		assertTrue( result.isCorrect(), "parse issues: " + result.getIssues() );

		BoxDotAccess access = ( BoxDotAccess ) result.getRoot()
		    .getDescendants()
		    .stream()
		    .filter( BoxDotAccess.class::isInstance )
		    .findFirst()
		    .orElseThrow( () -> new AssertionError( "expected a BoxDotAccess" ) );

		return ( ( BoxIdentifier ) access.getAccess() ).getName();
	}

	@Test
	@DisplayName( "It resolves `s.#true#` / `s.#false#` to the literal keys, not the widened text" )
	public void testBoxedBooleanDotAccessUsesLiteralName() throws IOException {
		assertEquals( "true", dotAccessName( "s = {\"true\": 1}; result = s.#true#;", BoxSourceType.BOXSCRIPT ),
		    "`s.#true#` must access the key `true`" );
		assertEquals( "false", dotAccessName( "s = {\"false\": 1}; result = s.#false#;", BoxSourceType.BOXSCRIPT ),
		    "`s.#false#` must access the key `false`" );
	}

	@Test
	@DisplayName( "It resolves `s.#null#` to the literal key `null`, not the widened text" )
	public void testBoxedNullDotAccessUsesLiteralName() throws IOException {
		assertEquals( "null", dotAccessName( "s = {\"null\": 1}; result = s.#null#;", BoxSourceType.BOXSCRIPT ),
		    "`s.#null#` must access the key `null`" );
	}

	@Test
	@DisplayName( "It resolves a boxed SCOPE name (`s.#variables#`) to the scope keyword" )
	public void testBoxedScopeDotAccessUsesScopeName() throws IOException {
		assertEquals( "variables", dotAccessName( "s = {\"variables\": 1}; result = s.#variables#;", BoxSourceType.BOXSCRIPT ),
		    "`s.#variables#` must access the key `variables`" );
	}

	@Test
	@DisplayName( "It keeps the widened `#...#` text as source metadata on the access node" )
	public void testBoxedDotAccessKeepsWidenedSourceText() throws IOException {
		ParsingResult result = new Parser().parse( "s = {\"true\": 1}; result = s.#true#;", BoxSourceType.BOXSCRIPT );
		assertTrue( result.isCorrect(), "parse issues: " + result.getIssues() );

		BoxDotAccess access = ( BoxDotAccess ) result.getRoot()
		    .getDescendants()
		    .stream()
		    .filter( BoxDotAccess.class::isInstance )
		    .findFirst()
		    .orElseThrow( () -> new AssertionError( "expected a BoxDotAccess" ) );

		assertEquals( "#true#", access.getAccess().getSourceText(), "expected the widened `#true#` text to survive as source metadata" );
	}

	/**
	 * A COMMENT inside a boxed keyword must not become part of the dot-access key. See
	 * {@code visitExprOutString} — the widened source text slices the RAW character stream, so
	 * hidden-channel comments leak in unless the key comes from the keyword's typed value.
	 * Comments may sit BEFORE or AFTER the keyword.
	 */
	@Test
	@DisplayName( "It ignores comments inside a boxed keyword used as a dot-access key" )
	public void testCommentedBoxedKeywordDotAccessUsesTypedValue() throws IOException {
		assertEquals( "true", dotAccessName( "s = {\"true\": 1}; result = s.#/*note*/true#;", BoxSourceType.BOXSCRIPT ),
		    "a leading comment inside `#...#` must not become part of the dot-access key" );
		// Trailing comment — the key must not become `true /* note */`.
		assertEquals( "true", dotAccessName( "s = {\"true\": 1}; result = s.#true /* note */#;", BoxSourceType.BOXSCRIPT ),
		    "a trailing comment inside `#...#` must not become part of the dot-access key" );
	}

	@Test
	@DisplayName( "It resolves boxed boolean/null/scope dot-access names in CF script too" )
	public void testBoxedKeywordDotAccessInCFScript() throws IOException {
		// NOTE: CFML transpilation intentionally uppercases dot-access keys (see
		// CFTranspilerVisitor.upperCaseDotAceessKeys), so the CF names are the uppercase
		// form of the resolved literal/scope name — minus any `#...#` wrapper.
		assertEquals( "TRUE", dotAccessName( "s = {\"true\": 1}; result = s.#true#;", BoxSourceType.CFSCRIPT ) );
		assertEquals( "NULL", dotAccessName( "s = {\"null\": 1}; result = s.#null#;", BoxSourceType.CFSCRIPT ) );
		assertEquals( "VARIABLES", dotAccessName( "s = {\"variables\": 1}; result = s.#variables#;", BoxSourceType.CFSCRIPT ) );
	}
}
