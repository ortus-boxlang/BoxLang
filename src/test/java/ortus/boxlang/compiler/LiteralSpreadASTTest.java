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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import ortus.boxlang.compiler.ast.expression.BoxArrayLiteral;
import ortus.boxlang.compiler.ast.expression.BoxSpreadExpression;
import ortus.boxlang.compiler.ast.expression.BoxStructLiteral;
import ortus.boxlang.compiler.ast.expression.BoxStructType;
import ortus.boxlang.compiler.parser.Parser;
import ortus.boxlang.compiler.parser.ParsingResult;
import ortus.boxlang.runtime.BoxRuntime;

public class LiteralSpreadASTTest {

	@BeforeAll
	public static void setupRuntime() {
		BoxRuntime.getInstance( true );
	}

	@Test
	public void testArrayLiteralSpreadAst() {
		Parser			parser	= new Parser();
		ParsingResult	result	= parser.parseExpression( "[ 1, ...values, 4 ]" );
		assertTrue( result.isCorrect(), result.getIssues().toString() );
		assertTrue( result.getRoot() instanceof BoxArrayLiteral );

		BoxArrayLiteral arrayLiteral = ( BoxArrayLiteral ) result.getRoot();
		assertEquals( 3, arrayLiteral.getValues().size() );
		assertTrue( arrayLiteral.getValues().get( 1 ) instanceof BoxSpreadExpression );
	}

	@Test
	public void testStructLiteralSpreadAst() {
		Parser			parser	= new Parser();
		ParsingResult	result	= parser.parseExpression( "{ a: 1, ...values, b: 2 }" );
		assertTrue( result.isCorrect(), result.getIssues().toString() );
		assertTrue( result.getRoot() instanceof BoxStructLiteral );

		BoxStructLiteral structLiteral = ( BoxStructLiteral ) result.getRoot();
		assertEquals( 5, structLiteral.getValues().size() );
		assertTrue( structLiteral.getValues().get( 2 ) instanceof BoxSpreadExpression );
	}

	@Test
	public void testOrderedStructLiteralSpreadAst() {
		Parser			parser	= new Parser();
		ParsingResult	result	= parser.parseExpression( "[ a: 1, ...values, b: 2 ]" );
		assertTrue( result.isCorrect(), result.getIssues().toString() );
		assertTrue( result.getRoot() instanceof BoxStructLiteral );

		BoxStructLiteral structLiteral = ( BoxStructLiteral ) result.getRoot();
		assertEquals( BoxStructType.Ordered, structLiteral.getType() );
		assertEquals( 5, structLiteral.getValues().size() );
		assertTrue( structLiteral.getValues().get( 2 ) instanceof BoxSpreadExpression );
	}

	@Test
	public void testOrderedStructLiteralLeadingSpreadAst() {
		Parser			parser	= new Parser();
		ParsingResult	result	= parser.parseExpression( "[ ...values, a: 1 ]" );
		assertTrue( result.isCorrect(), result.getIssues().toString() );
		assertTrue( result.getRoot() instanceof BoxStructLiteral );

		BoxStructLiteral structLiteral = ( BoxStructLiteral ) result.getRoot();
		assertEquals( BoxStructType.Ordered, structLiteral.getType() );
		assertEquals( 3, structLiteral.getValues().size() );
		assertTrue( structLiteral.getValues().get( 0 ) instanceof BoxSpreadExpression );
	}

	@Test
	public void testBracketSpreadLiteralWithoutKeyIsArrayLiteral() {
		Parser			parser	= new Parser();
		ParsingResult	result	= parser.parseExpression( "[ ...values ]" );
		assertTrue( result.isCorrect(), result.getIssues().toString() );
		assertTrue( result.getRoot() instanceof BoxArrayLiteral );

		BoxArrayLiteral arrayLiteral = ( BoxArrayLiteral ) result.getRoot();
		assertEquals( 1, arrayLiteral.getValues().size() );
		assertTrue( arrayLiteral.getValues().get( 0 ) instanceof BoxSpreadExpression );
	}
}
