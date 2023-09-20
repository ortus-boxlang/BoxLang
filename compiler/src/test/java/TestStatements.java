import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.stmt.BlockStmt;
import org.junit.Test;
import ortus.boxlang.parser.BoxParser;
import ortus.boxlang.parser.ParsingResult;
import ortus.boxlang.transpiler.BoxLangTranspiler;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestStatements extends TestBase {

	public ParsingResult parseStatement( String statement ) throws IOException {
		BoxParser		parser	= new BoxParser();
		ParsingResult	result	= parser.parseStatement( statement );
		assertTrue( result.isCorrect() );
		return result;
	}

	@Test
	public void invokeMethod() throws IOException {
		String			statement	= """
		                              			myObject.myMethod( obj1, "foo", 42 )
		                              """;

		ParsingResult	result		= parseStatement( statement );
		Node			javaAST		= BoxLangTranspiler.transform( result.getRoot() );

		assertEqualsNoWhiteSpaces(
		    """
		    Referencer.getAndInvoke(myObject,Key.of("myMethod"),newObject[]{context.scopeFindNearby(Key.of("obj1"),variablesScope).scope().get(Key.of("obj1")),"foo",42},false);
		    		                                                  """,
		    javaAST.toString() );
	}

	@Test
	public void invokeMethodWithKnownScope() throws IOException {
		String			statement	= """
		                              			variables.system.out.println(
		                              			 "hello world"
		                              		   )
		                              """;

		ParsingResult	result		= parseStatement( statement );
		Node			javaAST		= BoxLangTranspiler.transform( result.getRoot() );

		assertEqualsNoWhiteSpaces( """
		                           Referencer.getAndInvoke(
		                           	Referencer.get(
		                           		variablesScope.get(Key.of("system")),
		                           		Key.of("out"),
		                           		false),
		                           		Key.of("println"),
		                           		newObject[]{"helloworld"},false);
		                                                 """, javaAST.toString() );
	}

	@Test
	public void assigment() throws IOException {
		// String statement = """
		// variables["system"] = "Hello"
		// """;
		String			statement	= """
		                              			variables["system"] = "Hello"
		                              """;

		ParsingResult	result		= parseStatement( statement );
		Node			javaAST		= BoxLangTranspiler.transform( result.getRoot() );

		assertEquals( "variablesScope.put(Key.of(\"system\"), \"Hello\");", javaAST.toString() );
	}

	@Test
	public void var() throws IOException {
		String			statement	= """
		                              			var a = b = 1/0;
		                              """;

		ParsingResult	result		= parseStatement( statement );

		BlockStmt		javaAST		= ( BlockStmt ) BoxLangTranspiler.transform( result.getRoot() );
		assertEquals( "context.getScopeNearby(Key.of(LocalScope.name)).put(Key.of(\"a\"), Divide.invoke(1, 0));", javaAST.getStatements().get( 0 ).toString() );
		assertEquals( "context.getScopeNearby(Key.of(LocalScope.name)).put(Key.of(\"b\"), Divide.invoke(1, 0));", javaAST.getStatements().get( 1 ).toString() );

	}

	@Test
	public void ifElse() throws IOException {
		String			statement	= """
		                              			if( variables.a == "0" ) {
		                              				variables.a = a & "1";
		                              			} else if( !foo == false ) {
		                              				variables.a = a & "2";
		                                  		}
		                              """;

		ParsingResult	result		= parseStatement( statement );

		Node			javaAST		= BoxLangTranspiler.transform( result.getRoot() );
		assertEqualsNoWhiteSpaces(
		    """
		    if(BooleanCaster.cast(EqualsEquals.invoke(variablesScope.get(Key.of("a")),"0"))){variablesScope.put(Key.of("a"),Concat.invoke(context.scopeFindNearby(Key.of("a"),variablesScope).value(),"1"));}elseif(Not.invoke(EqualsEquals.invoke(context.scopeFindNearby(Key.of("foo"),variablesScope).scope().get(Key.of("foo")),false))){variablesScope.put(Key.of("a"),Concat.invoke(context.scopeFindNearby(Key.of("a"),variablesScope).value(),"2"));}

		         """,
		    javaAST.toString() );
	}

	@Test
	public void while_() throws IOException {
		String			statement	= """
		                              			while( variables.a == true ) {
		                              				variables.a = false;
		                                  		}
		                              """;

		ParsingResult	result		= parseStatement( statement );

		Node			javaAST		= BoxLangTranspiler.transform( result.getRoot() );
		assertEqualsNoWhiteSpaces(
		    """
		    while(BooleanCaster.cast(EqualsEquals.invoke(variablesScope.get(Key.of("a")),true))){
		    	variablesScope.put(Key.of("a"),false);
		    }
		    """, javaAST.toString() );
	}

	@Test
	public void case1_() throws IOException {
		String			statement	= """
		                              switch(variables['a']) {
		                                 case "9": {
		                                  variables['a'] = "0";
		                                 	break;
		                                 }
		                                 case "1": {
		                                  variables['a'] = "1";
		                                 	break;
		                                 }
		                                 default: {
		                                 variables['a'] = "default";
		                                  break;
		                                 }
		                              }
		                              """;

		ParsingResult	result		= parseStatement( statement );

		Node			javaAST		= BoxLangTranspiler.transform( result.getRoot() );
		System.out.println( javaAST );
		assertEqualsNoWhiteSpaces(
		    """
		    do {
		    	if ( BooleanCaster.cast( EqualsEquals.invoke( variablesScope.get( Key.of( "a" ) ), "9" ) ) ) {
		    		variablesScope.put( Key.of( "a" ), "0" );
		    		break;
		    	}
		    	if ( BooleanCaster.cast( EqualsEquals.invoke( variablesScope.get( Key.of( "a" ) ), "1" ) ) ) {
		    		variablesScope.put( Key.of( "a" ), "1" );
		    		break;
		    	}
		    	variablesScope.put( Key.of( "a" ), "default" );
		    	break;
		    } while ( false );
		        		    """,
		    javaAST.toString() );
	}

	@Test
	public void case2_() throws IOException {
		String			statement	= """
		                              switch(0) {
		                                 case variables['a'] > "0": {
		                                  variables['a'] = "0";
		                                 	break;
		                                 }
		                                 case variables['a'] < "1": {
		                                  variables['a'] = "1";
		                                 	break;
		                                 }
		                                 default: {
		                                 variables['a'] = "default";
		                                  break;
		                                 }
		                              }
		                              """;

		ParsingResult	result		= parseStatement( statement );

		Node			javaAST		= BoxLangTranspiler.transform( result.getRoot() );

		assertEqualsNoWhiteSpaces(
		    """
		    do {
		    			if ( BooleanCaster.cast( GreaterThan.invoke( variablesScope.get( Key.of( "a" ) ), "0" ) ) ) {
		    				variablesScope.put( Key.of( "a" ), "0" );
		    				break;
		    			}
		    			if ( BooleanCaster.cast( LessThan.invoke( variablesScope.get( Key.of( "a" ) ), "1" ) ) ) {
		    				variablesScope.put( Key.of( "a" ), "1" );
		    				break;
		    			}
		    			variablesScope.put( Key.of( "a" ), "default" );
		    			break;
		    		} while ( false );
		    		      """, javaAST.toString() );
	}

	@Test
	public void forIn_() throws IOException {
		String			statement	= """
		                              for( keyName in variables ) {
		                              	variables['a'] = variables['a'] + 1;
		                              }
		                              """;

		ParsingResult	result		= parseStatement( statement );

		Node			javaAST		= BoxLangTranspiler.transform( result.getRoot() );
		System.out.println( javaAST );
		assertEqualsNoWhiteSpaces(
		    """
		    {
		    	Iterator keyName = CollectionCaster.cast(variablesScope).iterator();
		    	while (keyName.hasNext()) {
		    		variablesScope.put(Key.of("keyName"), keyName.next());
		    		variablesScope.put(Key.of("a"), Plus.invoke(variablesScope.get(Key.of("a")), 1));
		    	}
		    }
		    """, javaAST.toString() );
	}

	@Test
	public void forIndex_() throws IOException {
		String			statement	= """
		                              for(variables.a = 0; variables.a < 10; variables.a++){
		                              }
		                                                         """;

		ParsingResult	result		= parseStatement( statement );

		Node			javaAST		= BoxLangTranspiler.transform( result.getRoot() );
		System.out.println( javaAST );
		assertEqualsNoWhiteSpaces(
		    """
		    {
		    	variablesScope.put(Key.of("a"),0);
		    	while(BooleanCaster.cast(LessThan.invoke(variablesScope.get(Key.of("a")),10))){
		    		Increment.invokePost(variablesScope,Key.of("a"));
		    	}
		    }
		         """, javaAST.toString() );
	}

	@Test
	public void assert_() throws IOException {
		String			statement	= """
		                              		assert variables['a'] == 0
		                              """;

		ParsingResult	result		= parseStatement( statement );

		Node			javaAST		= BoxLangTranspiler.transform( result.getRoot() );
		System.out.println( javaAST );
		assertEqualsNoWhiteSpaces(
		    """
		    Assert.invoke(EqualsEquals.invoke(variablesScope.get(Key.of("a")),0));
		    """, javaAST.toString() );
	}

	@Test
	public void try_() throws IOException {
		String			statement	= """
		                              	                             try {
		                              	                              	a = 1/0
		                              	                              } catch (any e) {
		                              	                              // Logic to run in catch
		                              	                              } finally {
		                              	                              // Logic to always run
		                              	                              }
		                              """;

		ParsingResult	result		= parseStatement( statement );

		Node			javaAST		= BoxLangTranspiler.transform( result.getRoot() );
		System.out.println( javaAST );
		assertEqualsNoWhiteSpaces(
		    """
		    try{
		    	context.scopeFindNearby(Key.of("a"),variablesScope).scope().put(Key.of("a"),Divide.invoke(1,0));
		    } catch(Throwablee) {
		    	catchContext=newCatchBoxContext(context,Key.of("e"),e);
		    } finally{
		    }

		      """, javaAST.toString() );
	}

	@Test
	public void expression() throws IOException {
		String			statement	= """
		                              a+=1;
		                                                    """;

		ParsingResult	result		= parseStatement( statement );

		Node			javaAST		= BoxLangTranspiler.transform( result.getRoot() );
		System.out.println( javaAST );
		assertEqualsNoWhiteSpaces(
		    """
		    Plus.invoke(context.scopeFindNearby(Key.of("a"),variablesScope).scope(),Key.of("a"),1);
		    """, javaAST.toString() );
	}

	@Test
	public void do_() throws IOException {
		String			statement	= """
		                              do {
		                              		a = 0;
		                                   } while(true)
		                                                                                 """;

		ParsingResult	result		= parseStatement( statement );

		Node			javaAST		= BoxLangTranspiler.transform( result.getRoot() );
		System.out.println( javaAST );
		assertEqualsNoWhiteSpaces(
		    """
		    do{
		    	context.scopeFindNearby(Key.of("a"),variablesScope).scope().put(Key.of("a"),0);
		    } while(BooleanCaster.cast(true));
		      """, javaAST.toString() );
	}

	@Test
	public void throw_() throws IOException {
		String			statement	= """
		                              throw new java.lang.RuntimeException("MyMessage");e;
		                              """;

		ParsingResult	result		= parseStatement( statement );

		Node			javaAST		= BoxLangTranspiler.transform( result.getRoot() );
		System.out.println( javaAST );
		assertEqualsNoWhiteSpaces(
		    """
		    throw(newjava.lang.RuntimeException("MyMessage"));
		     """, javaAST.toString() );
	}

}
