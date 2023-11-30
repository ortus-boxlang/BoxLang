package ortus.boxlang.compiler;

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import ortus.boxlang.parser.BoxParser;
import ortus.boxlang.parser.ParsingResult;

public class TestUDF extends TestBase {

	public boolean isParsable( String statement ) throws IOException {
		BoxParser		parser	= new BoxParser();
		ParsingResult	result	= parser.parseStatement( statement );
		return result.isCorrect();
	}

	@Test
	public void userDefinedFunction() throws IOException {

		assertTrue( isParsable(
		    """
		    public String function foo(
		    	required string param1 hint="My param",
		    	numeric param2=42 luis="majano"
		    ) hint="my UDF" output=false brad="wood" {
		      return "value";
		    }
		      """
		) );

	}

	@Test
	public void orderedStructLiterals() throws IOException {

		assertEqualsNoWhiteSpaces(
		    "new Struct(Struct.Type.LINKED)",
		    transformExpression( "[:]" )
		);
		assertEqualsNoWhiteSpaces(
		    """
		    Struct.of("brad","wood","luis","majano")
		    """,
		    transformExpression(
		        """
		        			[ "brad" : "wood", "luis" : "majano" ]
		        """ )
		);

		assertEqualsNoWhiteSpaces(
		    """
		    Struct.of(something, Array.fromList(List.of("foo", "bar", Struct.of("luis", true))), "else", 42)
		     """,
		    transformExpression(
		        """
		        [
		        	// This is still an array literal
		          something : [
		        	"foo",
		        	"bar",
		        	// But this is a nested ordered struct literal
		        	[ 'luis': true ]
		          ],
		          "else" : 42
		        ]
		              """ )
		);
	}

}
