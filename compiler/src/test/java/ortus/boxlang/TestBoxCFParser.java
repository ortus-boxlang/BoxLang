package ortus.boxlang;

import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import ourtus.boxlang.parser.BoxCFParser;
import ourtus.boxlang.parser.ParsingResult;

import java.io.IOException;

import static org.junit.Assert.assertTrue;

public class TestBoxCFParser {

	@Test
	@DisplayName( "Can parse an empty component" )
	public void testEmptyComponent() throws IOException {
		var code = """
		  component {}
		  """;
		ParsingResult result = new BoxCFParser().parse( code );
		assertTrue( result.isCorrect() );
	}

	@Test
	@DisplayName( "Can parse a component with comments and this.variable declaration" )
	public void testCommentedComponent() throws IOException {
		var code = """
		   /**
			* This is a comment.
			* @author Ortus Solutions
			* @component {}
			*/
			component {
				this.name = "Funky";
			}
		""";
		ParsingResult result = new BoxCFParser().parse( code );
		assertTrue( result.isCorrect() );
	}
}
