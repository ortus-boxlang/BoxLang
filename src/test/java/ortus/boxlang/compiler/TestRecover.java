package ortus.boxlang.compiler;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import ortus.boxlang.parser.BoxCFParser;
import ortus.boxlang.parser.ParsingResult;

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
		System.out.println( result.getIssues() );
		assertTrue( result.isCorrect() );
	}

	@Test
	public void testRecoverInClass() throws IOException {
		String			code	= """
		                          component /** */ {
		                           /**
		                           */
		                           }
		                                             """;

		BoxCFParser		parser	= new BoxCFParser();
		ParsingResult	result	= parser.parse( code );
		System.out.println( result.getIssues() );
		assertTrue( result.isCorrect() );
	}

	@Test

	public void testRecoverInClassBeforeStatment() throws IOException {
		String			code	= """
		                          component {
		                           /**
		                           */
		                          brad="wood"
		                           }
		                                             """;

		BoxCFParser		parser	= new BoxCFParser();
		ParsingResult	result	= parser.parse( code );
		System.out.println( result.getIssues() );
		assertTrue( result.isCorrect() );
	}

	@Test

	public void testRecoverInFunction() throws IOException {
		String			code	= """
		                                           function f() {
		                                               /**
		                                               */
		                          brad="wood"
		                                           }
		                                           """;

		BoxCFParser		parser	= new BoxCFParser();
		ParsingResult	result	= parser.parse( code );
		System.out.println( result.getIssues() );
		assertTrue( result.isCorrect() );
	}

	@Test

	public void testRecoverInArgs() throws IOException {
		String			code	= """
		                                                          function /** */ f(
		                          /** */
		                          param1,
		                          /** */
		                          required /** */ string /** */ param2="default" /** */
		                            ) {
		                                                          }
		                                                          """;

		BoxCFParser		parser	= new BoxCFParser();
		ParsingResult	result	= parser.parse( code );
		System.out.println( result.getIssues() );
		assertTrue( result.isCorrect() );
	}

	@Test
	public void testRecoverComment() throws IOException {
		String			code	= """
		                              /**
		                              */
		                          """;

		BoxCFParser		parser	= new BoxCFParser();
		ParsingResult	result	= parser.parse( code );
		System.out.println( result.getIssues() );
		assertTrue( result.isCorrect() );
	}

	@Test
	public void testRecoverBeforeStatement() throws IOException {
		String			code	= """
		                                               /**
		                                               */
		                          brad="wood";
		                                           """;

		BoxCFParser		parser	= new BoxCFParser();
		ParsingResult	result	= parser.parse( code );
		System.out.println( result.getIssues() );
		assertTrue( result.isCorrect() );
	}

	@Test
	public void testRecoverInExpression() throws IOException {
		String			code	= """
		                             /** */ foo /** */ = /** */ 1 /** */ + /** */ 2 /** */ ; /** */

		                          """;

		BoxCFParser		parser	= new BoxCFParser();
		ParsingResult	result	= parser.parse( code );
		System.out.println( result.getIssues() );
		assertTrue( result.isCorrect() );
	}

	@Test
	public void testRecoverInStatement() throws IOException {
		String			code	= """
		                          /** */
		                                                if(
		                          	/** */
		                          	true
		                          	/** */
		                            ) /** */ {
		                          	/** */
		                            }

		                                             """;

		BoxCFParser		parser	= new BoxCFParser();
		ParsingResult	result	= parser.parse( code );
		System.out.println( result.getIssues() );
		assertTrue( result.isCorrect() );
	}
}
