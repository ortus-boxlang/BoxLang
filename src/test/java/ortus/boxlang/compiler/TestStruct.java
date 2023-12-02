package ortus.boxlang.compiler;

import java.io.IOException;

import org.junit.jupiter.api.Test;

public class TestStruct extends TestBase {

	@Test
	public void unorderedStructLiterals() throws IOException {

		assertEqualsNoWhiteSpaces(
		    "new Struct()",
		    transformExpression( "{}" )
		);
		assertEqualsNoWhiteSpaces(
		    """
		    Struct.of("something", Array.of("foo", "bar", Struct.of("luis", true)), "else", 42)
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
		    "new Struct(Struct.Type.LINKED)",
		    transformExpression( "[:]" )
		);
		assertEqualsNoWhiteSpaces(
		    """
		    Struct.linkedOf("brad","wood","luis","majano")
		    """,
		    transformExpression(
		        """
		        			[ "brad" : "wood", "luis" : "majano" ]
		        """ )
		);

		assertEqualsNoWhiteSpaces(
		    """
		    Struct.linkedOf("something", Array.of("foo", "bar", Struct.linkedOf("luis", true)), "else", 42)
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
