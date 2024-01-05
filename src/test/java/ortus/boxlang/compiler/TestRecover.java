package ortus.boxlang.compiler;

import org.junit.jupiter.api.Test;
import ortus.boxlang.parser.BoxCFParser;
import ortus.boxlang.parser.ParsingResult;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestRecover {

	@Test
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
		assertTrue( result.isCorrect() );
	}
}
