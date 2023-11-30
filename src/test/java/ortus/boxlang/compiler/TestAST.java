package ortus.boxlang.compiler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import ortus.boxlang.ast.expression.BoxBinaryOperation;
import ortus.boxlang.parser.BoxParser;
import ortus.boxlang.parser.ParsingResult;

public class TestAST extends TestBase {

	protected BoxParser parser = new BoxParser();

	@Test
	public void testBinaryOperation() throws IOException {
		String[] epressions = new String[] {
		    "1 + 2",
		    "1 + 2.0",
		    "1 + \"a\"",
		    "1 + (1 + a)",
		    "1 + (-1)",
		    "1 - 2",
		    "1 * 2",
		    "1 / 2",
		    "1 + variables['system']",
		    "1 + create('a')",
		    "1 + a.create('a')",
		    "1 + a.create(p1='a')",
		    "1 + a.b",
		    "true && false",
		    "true || false"
			// "1 % 2",

		};

		for ( int i = 0; i < epressions.length; i++ ) {
			System.out.println( epressions[ i ] );
			ParsingResult result = parser.parseExpression( epressions[ i ] );
			assertTrue( result.isCorrect() );
			assertTrue( result.getRoot() instanceof BoxBinaryOperation );

			BoxBinaryOperation operation = ( BoxBinaryOperation ) result.getRoot();
			assertEquals( 2, operation.getChildren().size() );
			operation.getChildren().forEach( it -> {
				assertEquals( it.getParent(), operation );
			} );

		}
	}

	@Test
	public void testParser() throws IOException {
		List<Path> files = scanForFiles( "../boxlang/examples/cf_to_java/HelloWorld", Set.of( "cfc", "cfm", "cfml" ) );
		System.out.printf( "Testing parser against %s file(s)%n", files.size() );
		for ( Path file : files ) {
			System.out.println( "Testing " + file );
			ParsingResult result = parser.parse( file.toFile() );
			if ( !result.isCorrect() ) {
				result.getIssues().forEach( System.out::println );
			}
		}
	}

}
