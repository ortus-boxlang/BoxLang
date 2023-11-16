package ortus.boxlang.compiler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;

import org.junit.Ignore;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.stmt.BlockStmt;

import ortus.boxlang.parser.BoxParser;
import ortus.boxlang.parser.ParsingResult;
import ortus.boxlang.transpiler.JavaTranspiler;

@Disabled
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
		Node			javaAST		= new JavaTranspiler().transform( result.getRoot() );

		// myObject must be looked up in the scopes.
		assertEqualsNoWhiteSpaces(
		    """
		       Referencer.getAndInvoke(
		    	context,
		    	context.scopeFindNearby(Key.of("myObject"), null).value(),
		    	Key.of("myMethod"),
		    	newObject[]{
		    		context.scopeFindNearby(Key.of("obj1"), null).value(),
		    		"foo",
		    		42
		    	},
		    	false
		    );
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
		Node			javaAST		= new JavaTranspiler().transform( result.getRoot() );

		// system is explicitly scoped to variables, so we reference that directly.
		assertEqualsNoWhiteSpaces(
		    """
		       Referencer.getAndInvoke(
		    	context,
		    	Referencer.get(
		    		variablesScope.dereference(Key.of("system"),false ),
		    		Key.of("out"),
		    		false
		    	),
		    	Key.of("println"),
		    	newObject[]{"helloworld"},
		    	false
		    );
		                                                       """,
		    javaAST.toString() );
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
		Node			javaAST		= extractFromBlockStmt( new JavaTranspiler().transform( result.getRoot() ) );

		// explicit variables scope access is referenced directly
		assertEquals( "variablesScope.assign(Key.of(\"system\"), \"Hello\");", javaAST.toString() );
	}

	@Test
	public void var() throws IOException {
		String			statement	= """
		                              			var a = b = "value";
		                              """;

		ParsingResult	result		= parseStatement( statement );

		BlockStmt		javaAST		= ( BlockStmt ) new JavaTranspiler().transform( result.getRoot() );

		fail(
		    "I'm not sure what the proper fix is, but this solution is evaluating the right hand side once for each assignment.  It should only be evaluated once.  I assume the second assignment should simply reference a or an intermediate Java variable." );

		assertEqualsNoWhiteSpaces( "context.getScopeNearby(LocalScope.name).assign(Key.of(\"a\"), \"value\");",
		    javaAST.getStatements().get( 0 ).toString() );
		// The var keyword only applies to the a. For both variables to go in the local scope, the code would have needed to have been
		// var a = var b = 1/0;
		assertEqualsNoWhiteSpaces( "context.scopeFindNearby(Key.of(\"foo\"),context.getDefaultAssignmentScope()).scope().assign(Key.of(\"b\"), \"value\");",
		    javaAST.getStatements().get( 1 ).toString() );

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

		Node			javaAST		= new JavaTranspiler().transform( result.getRoot() );
		// Explicit variables scope access is referenced directly
		// Do not pass a default scope when simply retreiving a variable's value
		// Use .value() directly to get the value of a searched-variable
		assertEqualsNoWhiteSpaces(
		    """
		    if(EqualsEquals.invoke(variablesScope.dereference(Key.of("a"),false),"0")){
		    	{
		    		variablesScope.assign(Key.of("a"),Concat.invoke(context.scopeFindNearby(Key.of("a"),null).value(),"1"));
		    	}
		    } else if(Not.invoke(EqualsEquals.invoke(context.scopeFindNearby(Key.of("foo"),null).value(),false))){
		    	{
		    		variablesScope.assign(Key.of("a"),Concat.invoke(context.scopeFindNearby(Key.of("a"),null).value(),"2"));
		    	}
		    }

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

		Node			javaAST		= new JavaTranspiler().transform( result.getRoot() );
		// TODO: There are now {} braces around the Java code for assignments. Is this correct?
		// Explicit variables scope access is referenced directly
		assertEqualsNoWhiteSpaces(
		    """
		    while(EqualsEquals.invoke(variablesScope.dereference(Key.of("a"),false),true)){{variablesScope.assign(Key.of("a"),false);}}
		     """,
		    javaAST.toString() );
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

		Node			javaAST		= new JavaTranspiler().transform( result.getRoot() );
		System.out.println( javaAST );
		// TODO: There are now {} braces around the Java code for assignments. Is this correct?
		// Explicit variables scope access is referenced directly
		assertEqualsNoWhiteSpaces(
		    """
		       do{
		       	if(EqualsEquals.invoke(variablesScope.dereference(Key.of("a"),false),"9")){{
		       		variablesScope.assign(Key.of("a"),"0");
		       		}
		       		break;
		       	}

		       	if(EqualsEquals.invoke(variablesScope.dereference(Key.of("a"),false),"1")){
		       		{
		       			variablesScope.assign(Key.of("a"),"1");
		    		}
		    		break;
		    	}
		    	{
		    		variablesScope.assign(Key.of("a"),"default");
		    	}
		    		break;
		    } while(false);
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

		Node			javaAST		= new JavaTranspiler().transform( result.getRoot() );

		// TODO: There are now {} braces around the Java code for assignments. Is this correct?
		// Explicit variables scope access is referenced directly
		assertEqualsNoWhiteSpaces(
		    """
		      do{
		      	if(GreaterThan.invoke(variablesScope.dereference(Key.of("a"),false),"0")){
		      		{
		      			variablesScope.assign(Key.of("a"),"0");
		    	}
		    	break;
		    }
		    if(LessThan.invoke(variablesScope.dereference(Key.of("a"),false),"1")) {
		    	{
		    		variablesScope.assign(Key.of("a"),"1");
		    	}
		    	break;
		    }
		    {
		    	variablesScope.assign(Key.of("a"),"default");
		    }
		    break;
		      } while(false);
		       		      """,
		    javaAST.toString() );
	}

	@Test
	public void forIn_() throws IOException {
		String			statement	= """
		                              for( keyName in variables ) {
		                              	variables['a'] = variables['a'] + 1;
		                              }
		                              """;

		ParsingResult	result		= parseStatement( statement );

		Node			javaAST		= new JavaTranspiler().transform( result.getRoot() );
		System.out.println( javaAST );
		// TODO: There are now {} braces around the Java code for assignments. Is this correct?
		// Explicit variables scope access is referenced directly
		assertEqualsNoWhiteSpaces(
		    """
		    {
		    	IteratorkeyName=CollectionCaster.cast(variablesScope).iterator();
		    	while(keyName.hasNext()) {
		    		variablesScope.put(Key.of("keyName"),keyName.next());
		    		{
		    			variablesScope.assign(Key.of("a"),Plus.invoke(variablesScope.dereference(Key.of("a"),false),1));
		    		}
		    	}
		    }
		    	""",
		    javaAST.toString() );
	}

	@Test
	public void forIndex_() throws IOException {
		String			statement	= """
		                              for(variables.a = 0; variables.a < 10; variables.a++){
		                              }
		                                                         """;

		ParsingResult	result		= parseStatement( statement );

		Node			javaAST		= new JavaTranspiler().transform( result.getRoot() );
		System.out.println( javaAST );
		// Explicit variables scope access is referenced directly
		assertEqualsNoWhiteSpaces(
		    """
		    {
		    	variablesScope.assign(Key.of("a"),0);
		    	while(LessThan.invoke(variablesScope.dereference(Key.of("a"),false),10)){
		    		Increment.invokePost(variablesScope,Key.of("a"));
		    	}
		    }
		         """, javaAST.toString() );
	}

	@Test
	public void assert_() throws IOException {
		String			statement	= """
		                              		assert variables['a'] == 0;
		                              """;

		ParsingResult	result		= parseStatement( statement );

		Node			javaAST		= new JavaTranspiler().transform( result.getRoot() );
		System.out.println( javaAST );
		// Explicit variables scope access is referenced directly
		assertEqualsNoWhiteSpaces(
		    """
		    Assert.invoke(context,EqualsEquals.invoke(variablesScope.dereference(Key.of("a"),false),0));
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

		Node			javaAST		= new JavaTranspiler().transform( result.getRoot() );
		System.out.println( javaAST );
		// TODO: There are now {} braces around the Java code for assignments. Is this correct?
		assertEqualsNoWhiteSpaces(
		    """
		    try{
		    	{
		    		context.scopeFindNearby(Key.of("a"),context.getDefaultAssignmentScope()).scope().assign(Key.of("a"),Divide.invoke(1,0));
		    	}
		    } catch(Throwable e) {
		    	catchContext= new CatchBoxContext(context,Key.of("e"),e);
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

		Node			javaAST		= extractFromBlockStmt( new JavaTranspiler().transform( result.getRoot() ) );
		System.out.println( javaAST );
		// We are assigning a, but first it must be looked up, so therefore we don't provide a default scope to scopefindnearby. We want an exception thrown
		// if it doesn't exist already
		assertEqualsNoWhiteSpaces(
		    """
		    Plus.invoke(context.scopeFindNearby(Key.of("a"),null).scope(),Key.of("a"),1);
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

		Node			javaAST		= new JavaTranspiler().transform( result.getRoot() );
		System.out.println( javaAST );
		// TODO: There are now {} braces around the Java code for assignments. Is this correct?
		assertEqualsNoWhiteSpaces(
		    """
		    do{
		    	{
		    		context.scopeFindNearby(Key.of("a"),context.getDefaultAssignmentScope()).scope().assign(Key.of("a"),0);
		    	}
		    }while(BooleanCaster.cast(true));

		        """, javaAST.toString() );
	}

	@Test
	public void throw_() throws IOException {
		String			statement	= """
		                              throw new java.lang.RuntimeException("MyMessage");e;
		                              """;

		ParsingResult	result		= parseStatement( statement );

		Node			javaAST		= new JavaTranspiler().transform( result.getRoot() );
		System.out.println( javaAST );

		assertEqualsNoWhiteSpaces(
		    """
		       ExceptionUtil.throwException(
		    	classLocator.load(
		    		context,
		    		StringCaster.cast( "java.lang.RuntimeException" ),
		    		imports
		    	).invokeConstructor(newObject[]{"MyMessage"})
		    );
		         """,
		    javaAST.toString() );
	}

	/**
	 * Not implemented yet
	 */
	public void funcDef() throws IOException {
		String			statement	= """
		                              public function foo(String a = "Hello") {
		                              	variables.a = 0;

		                              }
		                                                    """;

		ParsingResult	result		= parseStatement( statement );

		Node			javaAST		= new JavaTranspiler().transform( result.getRoot() );
		System.out.println( javaAST );
		// TODO: This assert doesn't seem correct
		assertEqualsNoWhiteSpaces(
		    """
		    throw(newjava.lang.RuntimeException("MyMessage"));
		     """, javaAST.toString() );
	}

	@Test
	public void stringEscape1() throws IOException {
		String			statement	= """
		                              test4 = "Brad ""the guy"" Wood"

		                               """;

		ParsingResult	result		= parseStatement( statement );

		Node			javaAST		= extractFromBlockStmt( new JavaTranspiler().transform( result.getRoot() ) );
		System.out.println( javaAST );
		assertEqualsNoWhiteSpaces(
		    """
		    context.scopeFindNearby(Key.of("test4"),context.getDefaultAssignmentScope()).scope().assign(Key.of("test4"),"Brad\\"the guy\\"Wood");
		    """, javaAST.toString() );
	}

	@Test
	public void stringEscape2() throws IOException {
		String			statement	= """
		                              test5 = 'Luis ''the man'' Majano'

		                                              	  """;

		ParsingResult	result		= parseStatement( statement );

		Node			javaAST		= extractFromBlockStmt( new JavaTranspiler().transform( result.getRoot() ) );
		System.out.println( javaAST );
		assertEqualsNoWhiteSpaces(
		    """
		    context.scopeFindNearby(Key.of("test5"),context.getDefaultAssignmentScope()).scope().assign(Key.of("test5"),"Luis 'the man' Majano");
		    """, javaAST.toString() );
	}

	@Test
	public void stringEscape5() throws IOException {
		String			statement	= """
		                              test5 = "I have locker ##20"

		                                              	  """;

		ParsingResult	result		= parseStatement( statement );

		Node			javaAST		= extractFromBlockStmt( new JavaTranspiler().transform( result.getRoot() ) );
		System.out.println( javaAST );
		assertEqualsNoWhiteSpaces(
		    """
		    context.scopeFindNearby(Key.of("test5"),context.getDefaultAssignmentScope()).scope().assign(Key.of("test5"),"I have locker #20");
		    """, javaAST.toString() );
	}

	@Test
	public void stringEscape6() throws IOException {
		String			statement	= """
		                              result = "Box#5+6#Lang"

		                                              	  """;

		ParsingResult	result		= parseStatement( statement );

		Node			javaAST		= extractFromBlockStmt( new JavaTranspiler().transform( result.getRoot() ) );
		System.out.println( javaAST );
		assertEqualsNoWhiteSpaces(
		    """
		    context.scopeFindNearby(Key.of("result"),context.getDefaultAssignmentScope()).scope().assign(Key.of("result"),Concat.invoke("Box",Concat.invoke(Plus.invoke(5,6),"Lang")));
		    """,
		    javaAST.toString() );
	}

}
