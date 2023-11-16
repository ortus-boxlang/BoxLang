package ortus.boxlang.compiler;

import org.junit.jupiter.api.Test;

import java.io.IOException;

public class TestStruct extends TestBase {

	@Test
	public void unorderedStructLiterals() throws IOException {

		assertEqualsNoWhiteSpaces(
		    "new Struct()",
		    transformExpression( "{}" )
		);
		assertEqualsNoWhiteSpaces(
		    """
		    Struct.of(something, Array.fromList(List.of("foo", "bar", Struct.of("luis", true))), "else", 42)
		    """,
		    transformExpression(
		        """
		        {
		          something : [
		        	"foo",
		        	"bar",
		        	{ 'luis': true }
		          ],
		          "else" : 42
		        }
		             """ )
		);
	}

	@Test
	public void orderedStructLiterals() throws IOException {

		assertEqualsNoWhiteSpaces(
		    "new Struct(Struct.LINKED)",
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
