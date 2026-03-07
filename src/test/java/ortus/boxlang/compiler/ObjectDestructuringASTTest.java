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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import ortus.boxlang.compiler.ast.expression.BoxAssignment;
import ortus.boxlang.compiler.ast.expression.BoxDotAccess;
import ortus.boxlang.compiler.ast.expression.BoxIdentifier;
import ortus.boxlang.compiler.ast.expression.BoxObjectDestructuringBinding;
import ortus.boxlang.compiler.ast.expression.BoxObjectDestructuringPattern;
import ortus.boxlang.compiler.ast.expression.BoxParenthesis;
import ortus.boxlang.compiler.ast.expression.BoxScope;
import ortus.boxlang.compiler.ast.expression.BoxStringLiteral;
import ortus.boxlang.compiler.parser.Parser;
import ortus.boxlang.compiler.parser.ParsingResult;
import ortus.boxlang.runtime.BoxRuntime;

public class ObjectDestructuringASTTest {

	@BeforeAll
	public static void setupRuntime() {
		BoxRuntime.getInstance( true );
	}

	@Test
	public void testObjectDestructuringPatternAst() {
		Parser			parser	= new Parser();
		ParsingResult	result	= parser.parseExpression( "{ a, b: variables.b, c: { d = 40 }, ...rest } = data" );
		assertTrue( result.isCorrect() );
		assertTrue( result.getRoot() instanceof BoxAssignment );

		BoxAssignment assignment = ( BoxAssignment ) result.getRoot();
		assertTrue( assignment.getLeft() instanceof BoxObjectDestructuringPattern );

		BoxObjectDestructuringPattern pattern = ( BoxObjectDestructuringPattern ) assignment.getLeft();
		assertEquals( 4, pattern.getBindings().size() );

		BoxObjectDestructuringBinding first = pattern.getBindings().get( 0 );
		assertTrue( first.getKey() instanceof BoxIdentifier || first.getKey() instanceof BoxStringLiteral );
		String firstKey = first.getKey() instanceof BoxIdentifier
		    ? ( ( BoxIdentifier ) first.getKey() ).getName()
		    : ( ( BoxStringLiteral ) first.getKey() ).getValue();
		assertEquals( "a", firstKey );
		assertTrue( first.getTarget() instanceof BoxIdentifier );

		BoxObjectDestructuringBinding second = pattern.getBindings().get( 1 );
		assertTrue( second.getTarget() instanceof BoxDotAccess );
		BoxDotAccess scopedTarget = ( BoxDotAccess ) second.getTarget();
		assertTrue( scopedTarget.getContext() instanceof BoxScope );
		assertEquals( "variables", ( ( BoxScope ) scopedTarget.getContext() ).getName() );

		BoxObjectDestructuringBinding third = pattern.getBindings().get( 2 );
		assertTrue( third.getPattern() instanceof BoxObjectDestructuringPattern );
		assertEquals( 1, third.getPattern().getBindings().size() );
		assertTrue( third.getPattern().getBindings().get( 0 ).getDefaultValue() != null );

		BoxObjectDestructuringBinding rest = pattern.getBindings().get( 3 );
		assertTrue( rest.isRest() );
		assertTrue( rest.getTarget() instanceof BoxIdentifier );
		assertEquals( "rest", ( ( BoxIdentifier ) rest.getTarget() ).getName() );
	}

	@Test
	public void testParenthesizedScopedRenameDestructuringAst() {
		Parser			parser	= new Parser();
		ParsingResult	result	= parser.parseExpression( "({ a: variables.a, b: arguments.b } = data)" );
		assertTrue( result.isCorrect() );
		assertTrue( result.getRoot() instanceof BoxParenthesis );
		BoxParenthesis parenthesized = ( BoxParenthesis ) result.getRoot();
		assertTrue( parenthesized.getExpression() instanceof BoxAssignment );

		BoxAssignment assignment = ( BoxAssignment ) parenthesized.getExpression();
		assertTrue( assignment.getLeft() instanceof BoxObjectDestructuringPattern );
		BoxObjectDestructuringPattern pattern = ( BoxObjectDestructuringPattern ) assignment.getLeft();
		assertEquals( 2, pattern.getBindings().size() );

		BoxObjectDestructuringBinding first = pattern.getBindings().get( 0 );
		assertTrue( first.getTarget() instanceof BoxDotAccess );
		BoxDotAccess firstTarget = ( BoxDotAccess ) first.getTarget();
		assertTrue( firstTarget.getContext() instanceof BoxScope );
		assertEquals( "variables", ( ( BoxScope ) firstTarget.getContext() ).getName() );

		BoxObjectDestructuringBinding second = pattern.getBindings().get( 1 );
		assertTrue( second.getTarget() instanceof BoxDotAccess );
		BoxDotAccess secondTarget = ( BoxDotAccess ) second.getTarget();
		assertTrue( secondTarget.getContext() instanceof BoxScope || secondTarget.getContext() instanceof BoxIdentifier );
		String secondScopeName = secondTarget.getContext() instanceof BoxScope scope
		    ? scope.getName()
		    : ( ( BoxIdentifier ) secondTarget.getContext() ).getName();
		assertEquals( "arguments", secondScopeName );
	}

	@Test
	public void testSimpleShorthandDestructuringAst() {
		Parser			parser	= new Parser();
		ParsingResult	result	= parser.parseExpression( "{ a } = data" );
		assertTrue( result.isCorrect() );
		assertTrue( result.getRoot() instanceof BoxAssignment );
		BoxAssignment assignment = ( BoxAssignment ) result.getRoot();
		assertTrue( assignment.getLeft() instanceof BoxObjectDestructuringPattern );
		BoxObjectDestructuringPattern pattern = ( BoxObjectDestructuringPattern ) assignment.getLeft();
		assertEquals( 1, pattern.getBindings().size() );
		assertTrue( pattern.getBindings().get( 0 ).getKey() instanceof BoxStringLiteral );
		assertEquals( "a", ( ( BoxStringLiteral ) pattern.getBindings().get( 0 ).getKey() ).getValue() );
		assertTrue( pattern.getBindings().get( 0 ).getTarget() instanceof BoxIdentifier );
		assertEquals( "a", ( ( BoxIdentifier ) pattern.getBindings().get( 0 ).getTarget() ).getName() );
	}

	@Test
	public void testInvalidShorthandKeyRequiresRename() {
		Parser			parser	= new Parser();
		ParsingResult	result	= parser.parseExpression( "{ \"key with spaces\" } = data" );
		assertFalse( result.isCorrect() );
		assertTrue( result.getIssues().stream().anyMatch( issue -> issue.getMessage().contains( "cannot use shorthand" ) ) );
		assertTrue( result.getIssues().stream().anyMatch( issue -> issue.getMessage().contains( "Use an explicit binding" ) ) );
	}

	@Test
	public void testInvalidNumericShorthandKeyRequiresRename() {
		Parser			parser	= new Parser();
		ParsingResult	result	= parser.parseExpression( "{ 123 } = data" );
		assertFalse( result.isCorrect() );
		assertTrue( result.getIssues().stream().anyMatch( issue -> issue.getMessage().contains( "cannot use shorthand" ) ) );
		assertTrue( result.getIssues().stream().anyMatch( issue -> issue.getMessage().contains( "Use an explicit binding" ) ) );
	}
}
