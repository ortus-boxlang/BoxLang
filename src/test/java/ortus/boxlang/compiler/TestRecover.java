package ortus.boxlang.compiler;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import ortus.boxlang.parser.BoxCFParser;
import ortus.boxlang.parser.ParsingResult;

public class TestRecover {

	@Test
	@Disabled( "No longer works" )
	public void testRecover() throws IOException {
		String			code	= """
		                          /**
		                          */
		                          function f() {
		                              /**
		                              */
		                          }
		                          """;

		BoxCFParser		parser	= new BoxCFParser();
		ParsingResult	result	= parser.parse( code );
		System.out.println( result.getIssues() );
		assertTrue( result.isCorrect() );
	}
}
