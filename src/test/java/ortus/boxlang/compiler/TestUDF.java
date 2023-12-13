package ortus.boxlang.compiler;

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import ortus.boxlang.ast.BoxDocumentation;
import ortus.boxlang.ast.expression.BoxStringLiteral;
import ortus.boxlang.ast.statement.BoxAnnotation;
import ortus.boxlang.parser.BoxCFParser;
import ortus.boxlang.parser.BoxDOCParser;
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
	public void functionDocumentation() throws IOException {

		String			documentation	= """
		                                  /**
		                                  * This function does cool stuff
		                                  *
		                                  * @name Pass the name here that you want
		                                  * @name.isCool yes
		                                  *
		                                  * @author Brad Wood
		                                  * @returns Only the coolest value ever
		                                  */
		                                  		                                  """;
		BoxDOCParser	parser			= new BoxDOCParser();
		ParsingResult	result			= parser.parse( null, documentation );
		assertTrue( result.isCorrect() );
		BoxDocumentation docs = ( BoxDocumentation ) result.getRoot();

		assertTrue( ( ( BoxAnnotation ) docs.getAnnotations().get( 0 ) ).getKey().getValue().equals( "name" ) );
		assertTrue(
		    ( ( BoxStringLiteral ) ( ( BoxAnnotation ) docs.getAnnotations().get( 0 ) ).getValue() ).getValue().equals( "Pass the name here that you want" ) );
	}

	@Test
	public void userDefinedFunctionAnnotations() throws IOException {

		BoxCFParser		parser	= new BoxCFParser();
		ParsingResult	result	= parser.parse(

		    """
		    				/**
		    				* This function does cool stuff
		    				*
		    				* @name Pass the name here that you want
		    				* @name.isCool yes
		    				*
		    				* @author Brad Wood
		    				* @returns Only the coolest value ever
		    				*/
		    				@myAnnotation "value" "another value"
		    				@name.foo "bar"
		    				string function greet( required string name='Brad' inject="myService" ) key="value" keyOnly {
		    				  return "Brad";
		    				}
		    """
		);
		assertTrue( result.isCorrect() );

	}

}
