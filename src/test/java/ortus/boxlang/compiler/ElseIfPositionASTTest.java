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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.BoxTemplate;
import ortus.boxlang.compiler.ast.statement.BoxIfElse;
import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.compiler.parser.Parser;
import ortus.boxlang.compiler.parser.ParsingResult;
import ortus.boxlang.runtime.BoxRuntime;

/**
 * Regression tests for the template {@code <bx:elseif>} / {@code <cfelseif>} AST
 * position and sourceText. The elseif node must start at the tag's open {@code <}
 * (column 0), not one column past it. A previous implementation hardcoded a -3
 * offset from the {@code elseif} token which was correct for {@code <cfelseif>}
 * but off-by-one for {@code <bx:elseif>} (whose prefix is {@code bx:}).
 */
public class ElseIfPositionASTTest {

	@BeforeAll
	public static void setupRuntime() {
		BoxRuntime.getInstance( true );
	}

	/**
	 * Find the (first) else-if node reachable from an if/else-if/else template.
	 * The elseBody of the top-level if is a BoxStatementBlock wrapping the
	 * nested BoxIfElse that represents the elseif.
	 */
	private BoxIfElse findElseIf( BoxNode node ) {
		if ( node instanceof BoxIfElse iff ) {
			if ( iff.getElseBody() instanceof ortus.boxlang.compiler.ast.statement.BoxStatementBlock block
			    && block.getBody().get( 0 ) instanceof BoxIfElse nested ) {
				return nested;
			}
			return null;
		}
		return null;
	}

	private void assertElseIfPosition( String source, BoxSourceType type, String tag ) throws Exception {
		Parser			parser	= new Parser();
		ParsingResult	result	= parser.parse( source, type );
		assertTrue( result.isCorrect(), "parse should succeed" );
		BoxTemplate	template	= ( BoxTemplate ) result.getRoot();
		BoxIfElse	topIf		= ( BoxIfElse ) template.getStatements().get( 0 );
		BoxIfElse	elseIf		= findElseIf( topIf );
		assertNotNull( elseIf, "elseif node should exist" );

		// The elseif AST node must start at the "<" of the tag.
		assertEquals( 3, elseIf.getPosition().getStart().getLine() );
		assertEquals( 0, elseIf.getPosition().getStart().getColumn(), tag + " elseif must start at column 0 (the '<')" );

		// And its sourceText must include the leading "<".
		String srcText = elseIf.getSourceText();
		assertTrue( srcText.startsWith( tag ), tag + " elseif sourceText must include the '<' but was: " + srcText );
	}

	@Test
	public void testBoxElseIfPosition() throws Exception {
		String source = """
		                <bx:if true>
		                    <bx:set a = 1>
		                <bx:elseif false>
		                    <bx:set a = 2>
		                <bx:else>
		                    <bx:set a = 3>
		                </bx:if>
		                """;
		assertElseIfPosition( source, BoxSourceType.BOXTEMPLATE, "<bx:elseif" );
	}

	@Test
	public void testCFElseIfPosition() throws Exception {
		String source = """
		                <cfif true>
		                    <cfset a = 1>
		                <cfelseif false>
		                    <cfset a = 2>
		                <cfelse>
		                    <cfset a = 3>
		                </cfif>
		                """;
		assertElseIfPosition( source, BoxSourceType.CFTEMPLATE, "<cfelseif" );
	}

	@Test
	public void testBoxMultipleElseIfPositions() throws Exception {
		String			source	= """
		                          <bx:if true>
		                              <bx:set a = 1>
		                          <bx:elseif false>
		                              <bx:set a = 2>
		                          <bx:elseif false>
		                              <bx:set a = 3>
		                          <bx:else>
		                              <bx:set a = 4>
		                          </bx:if>
		                          """;
		Parser			parser	= new Parser();
		ParsingResult	result	= parser.parse( source, BoxSourceType.BOXTEMPLATE );
		assertTrue( result.isCorrect() );
		BoxTemplate	template	= ( BoxTemplate ) result.getRoot();
		BoxIfElse	topIf		= ( BoxIfElse ) template.getStatements().get( 0 );

		// First elseif: <bx:elseif false> on line 3.
		BoxIfElse	elseIf1		= findElseIf( topIf );
		assertNotNull( elseIf1 );
		assertEquals( 3, elseIf1.getPosition().getStart().getLine() );
		assertEquals( 0, elseIf1.getPosition().getStart().getColumn() );
		assertTrue( elseIf1.getSourceText().startsWith( "<bx:elseif" ) );

		// Second elseif: nested inside the first's elseBody — <bx:elseif false> on line 5.
		BoxIfElse elseIf2 = findElseIf( elseIf1 );
		assertNotNull( elseIf2 );
		assertEquals( 5, elseIf2.getPosition().getStart().getLine() );
		assertEquals( 0, elseIf2.getPosition().getStart().getColumn() );
		assertTrue( elseIf2.getSourceText().startsWith( "<bx:elseif" ) );
	}

	@Test
	public void testCFMultipleElseIfPositions() throws Exception {
		String			source	= """
		                          <cfif true>
		                              <cfset a = 1>
		                          <cfelseif false>
		                              <cfset a = 2>
		                          <cfelseif false>
		                              <cfset a = 3>
		                          <cfelse>
		                              <cfset a = 4>
		                          </cfif>
		                          """;
		Parser			parser	= new Parser();
		ParsingResult	result	= parser.parse( source, BoxSourceType.CFTEMPLATE );
		assertTrue( result.isCorrect() );
		BoxTemplate	template	= ( BoxTemplate ) result.getRoot();
		BoxIfElse	topIf		= ( BoxIfElse ) template.getStatements().get( 0 );

		BoxIfElse	elseIf1		= findElseIf( topIf );
		assertNotNull( elseIf1 );
		assertEquals( 3, elseIf1.getPosition().getStart().getLine() );
		assertEquals( 0, elseIf1.getPosition().getStart().getColumn() );
		assertTrue( elseIf1.getSourceText().startsWith( "<cfelseif" ) );

		BoxIfElse elseIf2 = findElseIf( elseIf1 );
		assertNotNull( elseIf2 );
		assertEquals( 5, elseIf2.getPosition().getStart().getLine() );
		assertEquals( 0, elseIf2.getPosition().getStart().getColumn() );
		assertTrue( elseIf2.getSourceText().startsWith( "<cfelseif" ) );
	}
}
