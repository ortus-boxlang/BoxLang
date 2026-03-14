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
import org.junit.jupiter.api.Test;

import ortus.boxlang.compiler.ast.expression.BoxArrayDestructuringBinding;
import ortus.boxlang.compiler.ast.expression.BoxArrayDestructuringPattern;
import ortus.boxlang.compiler.ast.expression.BoxArrayLiteral;
import ortus.boxlang.compiler.ast.expression.BoxAssignment;
import ortus.boxlang.compiler.ast.expression.BoxAssignmentModifier;
import ortus.boxlang.compiler.ast.expression.BoxDotAccess;
import ortus.boxlang.compiler.ast.expression.BoxIdentifier;
import ortus.boxlang.compiler.ast.expression.BoxScope;
import ortus.boxlang.compiler.ast.BoxScript;
import ortus.boxlang.compiler.ast.statement.BoxExpressionStatement;
import ortus.boxlang.compiler.parser.Parser;
import ortus.boxlang.compiler.parser.ParsingResult;
import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.BoxRuntime;

public class ArrayDestructuringASTTest {

	@BeforeAll
	public static void setupRuntime() {
		BoxRuntime.getInstance( true );
	}

	@Test
	public void testArrayDestructuringPatternAst() {
		Parser			parser	= new Parser();
		ParsingResult	result	= parser.parseExpression( "[ a, variables.b, [ d = 40 ], ...rest ] = data" );
		assertTrue( result.isCorrect() );
		assertTrue( result.getRoot() instanceof BoxAssignment );

		BoxAssignment assignment = ( BoxAssignment ) result.getRoot();
		assertTrue( assignment.getLeft() instanceof BoxArrayDestructuringPattern );

		BoxArrayDestructuringPattern pattern = ( BoxArrayDestructuringPattern ) assignment.getLeft();
		assertEquals( 4, pattern.getBindings().size() );

		BoxArrayDestructuringBinding first = pattern.getBindings().get( 0 );
		assertTrue( first.getTarget() instanceof BoxIdentifier );
		assertEquals( "a", ( ( BoxIdentifier ) first.getTarget() ).getName() );

		BoxArrayDestructuringBinding second = pattern.getBindings().get( 1 );
		assertTrue( second.getTarget() instanceof BoxDotAccess );
		BoxDotAccess scopedTarget = ( BoxDotAccess ) second.getTarget();
		assertTrue( scopedTarget.getContext() instanceof BoxScope );
		assertEquals( "variables", ( ( BoxScope ) scopedTarget.getContext() ).getName() );

		BoxArrayDestructuringBinding third = pattern.getBindings().get( 2 );
		assertTrue( third.getPattern() instanceof BoxArrayDestructuringPattern );
		assertEquals( 1, third.getPattern().getBindings().size() );
		assertTrue( third.getPattern().getBindings().get( 0 ).getDefaultValue() != null );

		BoxArrayDestructuringBinding rest = pattern.getBindings().get( 3 );
		assertTrue( rest.isRest() );
		assertTrue( rest.getTarget() instanceof BoxIdentifier );
		assertEquals( "rest", ( ( BoxIdentifier ) rest.getTarget() ).getName() );
	}

	@Test
	public void testArrayDestructuringPatternAstWithMiddleRest() {
		Parser			parser	= new Parser();
		ParsingResult	result	= parser.parseExpression( "[ first, ...middle, last ] = data" );
		assertTrue( result.isCorrect(), result.getIssues().toString() );
		assertTrue( result.getRoot() instanceof BoxAssignment );

		BoxAssignment assignment = ( BoxAssignment ) result.getRoot();
		assertTrue( assignment.getLeft() instanceof BoxArrayDestructuringPattern );

		BoxArrayDestructuringPattern pattern = ( BoxArrayDestructuringPattern ) assignment.getLeft();
		assertEquals( 3, pattern.getBindings().size() );

		BoxArrayDestructuringBinding first = pattern.getBindings().get( 0 );
		assertTrue( first.getTarget() instanceof BoxIdentifier );
		assertEquals( "first", ( ( BoxIdentifier ) first.getTarget() ).getName() );
		assertTrue( !first.isRest() );

		BoxArrayDestructuringBinding middle = pattern.getBindings().get( 1 );
		assertTrue( middle.isRest() );
		assertTrue( middle.getTarget() instanceof BoxIdentifier );
		assertEquals( "middle", ( ( BoxIdentifier ) middle.getTarget() ).getName() );

		BoxArrayDestructuringBinding last = pattern.getBindings().get( 2 );
		assertTrue( last.getTarget() instanceof BoxIdentifier );
		assertEquals( "last", ( ( BoxIdentifier ) last.getTarget() ).getName() );
		assertTrue( !last.isRest() );
	}

	@Test
	public void testArrayDestructuringPatternRejectsMultipleRestBindings() {
		Parser			parser	= new Parser();
		ParsingResult	result	= parser.parseExpression( "[ a, ...rest1, ...rest2 ] = data" );
		assertTrue( !result.isCorrect() );
	}

	@Test
	public void testSimpleShorthandArrayDestructuringAst() {
		Parser			parser	= new Parser();
		ParsingResult	result	= parser.parseExpression( "[ a, b ] = data" );
		assertTrue( result.isCorrect() );
		assertTrue( result.getRoot() instanceof BoxAssignment );
		BoxAssignment assignment = ( BoxAssignment ) result.getRoot();
		assertTrue( assignment.getLeft() instanceof BoxArrayDestructuringPattern );
		BoxArrayDestructuringPattern pattern = ( BoxArrayDestructuringPattern ) assignment.getLeft();
		assertEquals( 2, pattern.getBindings().size() );
		assertTrue( pattern.getBindings().get( 0 ).getTarget() instanceof BoxIdentifier );
		assertEquals( "a", ( ( BoxIdentifier ) pattern.getBindings().get( 0 ).getTarget() ).getName() );
		assertTrue( pattern.getBindings().get( 1 ).getTarget() instanceof BoxIdentifier );
		assertEquals( "b", ( ( BoxIdentifier ) pattern.getBindings().get( 1 ).getTarget() ).getName() );
	}

	@Test
	public void testArrayLiteralExpressionIsUnchanged() {
		Parser			parser	= new Parser();
		ParsingResult	result	= parser.parseExpression( "[ 1, 2, 3 ]" );
		assertTrue( result.isCorrect() );
		assertTrue( result.getRoot() instanceof BoxArrayLiteral );
	}

	@Test
	public void testVarDeclarationArrayDestructuringAst() throws IOException {
		Parser			parser	= new Parser();
		ParsingResult	result	= parser.parseStatement( "var [ a, b ] = data" );
		assertTrue( result.isCorrect(), result.getIssues().toString() );
		assertTrue( result.getRoot() instanceof BoxExpressionStatement );

		BoxExpressionStatement statement = ( BoxExpressionStatement ) result.getRoot();
		assertTrue( statement.getExpression() instanceof BoxAssignment );
		BoxAssignment assignment = ( BoxAssignment ) statement.getExpression();
		assertTrue( assignment.getModifiers().contains( BoxAssignmentModifier.VAR ) );
		assertTrue( assignment.getLeft() instanceof BoxArrayDestructuringPattern );
	}

	@Test
	public void testScriptVarDeclarationArrayDestructuringAst() throws IOException {
		Parser			parser	= new Parser();
		ParsingResult	result	= parser.parse(
		    """
		    data = [ 1, 2 ];
		    var [ a, b ] = data;
		    """,
		    BoxSourceType.BOXSCRIPT
		);
		assertTrue( result.isCorrect(), result.getIssues().toString() );
		assertTrue( result.getRoot() instanceof BoxScript );

		BoxScript script = ( BoxScript ) result.getRoot();
		assertEquals( 2, script.getStatements().size() );
		assertTrue( script.getStatements().get( 1 ) instanceof BoxExpressionStatement );

		BoxExpressionStatement statement = ( BoxExpressionStatement ) script.getStatements().get( 1 );
		assertTrue( statement.getExpression() instanceof BoxAssignment );

		BoxAssignment assignment = ( BoxAssignment ) statement.getExpression();
		assertTrue( assignment.getModifiers().contains( BoxAssignmentModifier.VAR ) );
		assertTrue( assignment.getLeft() instanceof BoxArrayDestructuringPattern );
	}
}
