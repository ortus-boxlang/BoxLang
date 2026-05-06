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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import ortus.boxlang.compiler.ast.expression.BoxMatchExpression;
import ortus.boxlang.compiler.ast.statement.BoxExpressionStatement;
import ortus.boxlang.compiler.parser.Parser;
import ortus.boxlang.compiler.parser.ParsingResult;
import ortus.boxlang.compiler.prettyprint.PrettyPrint;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.types.exceptions.ExpressionException;

public class MatchSyntaxContractTest {

	@BeforeAll
	public static void setupRuntime() {
		BoxRuntime.getInstance( true );
	}

	@Test
	public void testMatchParsesAsExpression() {
		Parser			parser	= new Parser();
		ParsingResult	result	= parser.parseExpression(
		    """
		    match value {
		    	0 -> \"zero\"
		    	_ -> \"many\"
		    }
		    """
		);

		assertTrue( result.isCorrect(), result.getIssues().toString() );
		BoxMatchExpression matchExpression = assertInstanceOf( BoxMatchExpression.class, result.getRoot() );
		assertEquals( 2, matchExpression.getCases().size() );
	}

	@Test
	public void testMatchParsesAsStandaloneStatement() throws Exception {
		Parser			parser	= new Parser();
		ParsingResult	result	= parser.parseStatement(
		    """
		    match value {
		    	0 -> \"zero\"
		    	_ -> \"many\"
		    }
		    """
		);

		assertTrue( result.isCorrect(), result.getIssues().toString() );
		BoxExpressionStatement statement = assertInstanceOf( BoxExpressionStatement.class, result.getRoot() );
		assertInstanceOf( BoxMatchExpression.class, statement.getExpression() );
	}

	@Test
	public void testMatchPrettyPrintsUsingDedicatedAst() {
		Parser			parser	= new Parser();
		ParsingResult	result	= parser.parseExpression(
		    """
		    match value {
		    	0 -> "zero"
		    	_ -> "many"
		    }
		    """
		);

		assertTrue( result.isCorrect(), result.getIssues().toString() );
		String prettyPrinted = PrettyPrint.prettyPrint( result.getRoot() )
		    .replace( "\r\n", "\n" )
		    .replace( "\t", "    " )
		    .stripTrailing();
		assertEquals( "match value {\n    0 -> \"zero\"\n    _ -> \"many\"\n}", prettyPrinted );
	}

	@Test
	public void testMatchAcceptsIfGuardsAndEllipsisRest() {
		Parser			parser	= new Parser();
		ParsingResult	result	= parser.parseExpression(
		    """
		    match value {
		    	[ head, ...tail ] if head > 0 -> head
		    	_ -> null
		    }
		    """
		);

		assertTrue( result.isCorrect(), result.getIssues().toString() );
	}

	@Test
	public void testMatchAcceptsStatementBlockBranchBodiesEndingInExpression() {
		Parser			parser	= new Parser();
		ParsingResult	result	= parser.parseExpression(
		    """
		    match value {
		    	0 -> { foo = \"zero\"; foo }
		    	_ -> \"many\"
		    }
		    """
		);

		assertTrue( result.isCorrect(), result.getIssues().toString() );
	}

	@Test
	public void testMatchRejectsStatementBlockBranchBodiesWithoutFinalExpression() {
		Parser parser = new Parser();

		assertThrows(
		    ExpressionException.class,
		    () -> parser.parseExpression(
		        """
		        match value {
		        	0 -> {
		        		if ( true ) {
		        			foo = \"zero\";
		        		}
		        	}
		        	_ -> \"many\"
		        }
		        """
		    )
		);
	}

	@Test
	public void testMatchRejectsSplatRestSyntax() {
		Parser			parser	= new Parser();
		ParsingResult	result	= parser.parseExpression(
		    """
		    match value {
		    	[ head, *tail ] -> head
		    	_ -> null
		    }
		    """
		);

		assertFalse( result.isCorrect(), result.getIssues().toString() );
	}

	@Test
	public void testMatchRejectsWhenGuards() {
		Parser			parser	= new Parser();
		ParsingResult	result	= parser.parseExpression(
		    """
		    match value {
		    	0 when value > 0 -> \"zero\"
		    	_ -> \"many\"
		    }
		    """
		);

		assertFalse( result.isCorrect(), result.getIssues().toString() );
	}

	@Test
	public void testMatchAcceptsConstructorPatterns() {
		Parser			parser	= new Parser();
		ParsingResult	result	= parser.parseExpression(
		    """
		    match value {
		    	Some( x ) -> x
		    	_ -> null
		    }
		    """
		);

		assertTrue( result.isCorrect(), result.getIssues().toString() );
	}

	@Test
	public void testMatchAcceptsOrPatterns() {
		Parser			parser	= new Parser();
		ParsingResult	result	= parser.parseExpression(
		    """
		    match value {
		    	1 or 2 -> "small"
		    	_ -> "many"
		    }
		    """
		);

		assertTrue( result.isCorrect(), result.getIssues().toString() );
	}

	@Test
	public void testMatchAcceptsAndPatterns() {
		Parser			parser	= new Parser();
		ParsingResult	result	= parser.parseExpression(
		    """
		    match value {
		    	x and _ -> "exact"
		    	_ -> "fallback"
		    }
		    """
		);

		assertTrue( result.isCorrect(), result.getIssues().toString() );
	}

	@Test
	public void testMatchAcceptsNotPatterns() {
		Parser			parser	= new Parser();
		ParsingResult	result	= parser.parseExpression(
		    """
		    match value {
		    	not 1 -> "other"
		    	_ -> "one"
		    }
		    """
		);

		assertTrue( result.isCorrect(), result.getIssues().toString() );
	}

	@Test
	public void testMatchAcceptsPredicatePatterns() {
		Parser			parser	= new Parser();
		ParsingResult	result	= parser.parseExpression(
		    """
		    match value {
		    	?( x -> x > 1 ) -> \"many\"
		    	_ -> \"small\"
		    }
		    """
		);

		assertTrue( result.isCorrect(), result.getIssues().toString() );
	}

	@Test
	public void testMatchAcceptsRangePatterns() {
		Parser			parser	= new Parser();
		ParsingResult	result	= parser.parseExpression(
		    """
		    match value {
		    	1..5 -> "small"
		    	_ -> "many"
		    }
		    """
		);

		assertTrue( result.isCorrect(), result.getIssues().toString() );
	}
}