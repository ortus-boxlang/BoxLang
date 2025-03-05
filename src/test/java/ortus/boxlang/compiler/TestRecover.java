package ortus.boxlang.compiler;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import ortus.boxlang.compiler.parser.CFParser;
import ortus.boxlang.compiler.parser.ParsingResult;

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

		CFParser		parser	= new CFParser();
		ParsingResult	result	= parser.parse( code );
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

		CFParser		parser	= new CFParser();
		ParsingResult	result	= parser.parse( code, true, true );
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

		CFParser		parser	= new CFParser();
		ParsingResult	result	= parser.parse( code, true, true );
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

		CFParser		parser	= new CFParser();
		ParsingResult	result	= parser.parse( code );
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

		CFParser		parser	= new CFParser();
		ParsingResult	result	= parser.parse( code );
		assertTrue( result.isCorrect() );
	}

	@Test

	public void testRecoverInArgs2() throws IOException {
		String			code	= """
		                                                                                       function /** */ // test
		                                     						   f(
		                          // test
		                                     						   /** */ // test
		                                                       param1,
		                          // test
		                                                       /** */ // test
		                                                       required
		                                    /** */  // test
		                                      string /** */  // test
		                                       param2="default"
		                          // test
		                           /** */  // test
		                                                         ) {
		                                                                                       }
		                                                                                       """;

		CFParser		parser	= new CFParser();
		ParsingResult	result	= parser.parse( code );
		System.out.println( result.getIssues() );
		assertTrue( result.isCorrect() );
	}

	@Test

	public void testRecoverInArgs3() throws IOException {
		String			code	= """
		                                                                            function // test
		                          						   f(
		                          						   // test
		                                            param1,
		                                            // test
		                                            required
		                          // test
		                           string // test
		                            param2="default" // test
		                                              ) {
		                                                                            }
		                                                                            """;

		CFParser		parser	= new CFParser();
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

		CFParser		parser	= new CFParser();
		ParsingResult	result	= parser.parse( code );
		assertTrue( result.isCorrect() );
	}

	@Test
	public void testRecoverBeforeStatement() throws IOException {
		String			code	= """
		                                               /**
		                                               */
		                          brad="wood";
		                                           """;

		CFParser		parser	= new CFParser();
		ParsingResult	result	= parser.parse( code );
		assertTrue( result.isCorrect() );
	}

	@Test
	public void testRecoverInExpression() throws IOException {
		String			code	= """
		                             /** */ foo /** */ = /** */ 1 /** */ + /** */ 2 /** */ ; /** */

		                          """;

		CFParser		parser	= new CFParser();
		ParsingResult	result	= parser.parse( code );
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

		CFParser		parser	= new CFParser();
		ParsingResult	result	= parser.parse( code );
		assertTrue( result.isCorrect() );
	}
}
