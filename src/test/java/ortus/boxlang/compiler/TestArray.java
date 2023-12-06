package ortus.boxlang.compiler;

import java.io.IOException;

import org.junit.jupiter.api.Test;

public class TestArray extends TestBase {

	@Test
	public void arrayLiterals() throws IOException {

		assertEqualsNoWhiteSpaces(
		    "new Array()",
		    transformExpression( "[]" )
		);
		assertEqualsNoWhiteSpaces(
		    "Array.of(1,2,3)",
		    transformExpression( "[1,2,3]" )
		);
		assertEqualsNoWhiteSpaces(
		    """
		    Array.of("foo","bar")
		    """,
		    transformExpression(
		        """
		        			["foo","bar"]
		        """ )
		);

		assertEqualsNoWhiteSpaces(
		    """
		    Array.of(
		    Array.of(1,2),
		    Array.of(3,4),
		    "brad")
		      """,
		    transformExpression(
		        """
		        [
		          [1,2],
		          [3,4],
		          "brad"
		        ]
		        		        """ )
		);
	}

}
