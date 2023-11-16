package ortus.boxlang.compiler;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.Assert.assertTrue;

public class TestArray extends TestBase {

	@Test
	public void arrayLiterals() throws IOException {

		assertEqualsNoWhiteSpaces(
		    "new Array()",
		    transformExpression( "[]" )
		);
		assertEqualsNoWhiteSpaces(
		    "Array.fromList(List.of(1,2,3))",
		    transformExpression( "[1,2,3]" )
		);
		assertEqualsNoWhiteSpaces(
		    """
		    Array.fromList(List.of("foo","bar"))
		    """,
		    transformExpression(
		        """
		        			["foo","bar"]
		        """ )
		);

		assertEqualsNoWhiteSpaces(
		    """
		    Array.fromList(List.of(
		    Array.fromList(List.of(1,2)),
		    Array.fromList(List.of(3,4)),
		    "brad"))
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
