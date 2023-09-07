import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.BlockStmt;
import org.junit.Test;
import ourtus.boxlang.parser.BoxFileType;
import ourtus.boxlang.parser.BoxLangParser;
import ourtus.boxlang.parser.ParsingResult;
import ourtus.boxlang.transpiler.BoxLangTranspiler;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class TestStatements extends TestBase {
	@Test
	public void invokeMethod() throws IOException {
		String expression = """
						myObject.myMethod( obj1, "foo", 42 )
			""";

		BoxLangParser parser = new BoxLangParser();
		ParsingResult result = parser.parseStatement( expression );
		Node javaAST = BoxLangTranspiler.transform( result.getRoot() );

		assertEqualsNoWhiteSpaces( """
			Referencer.getAndInvoke(
			  context,
			  myObject,
			  Key.of( "myMethod" ),
			  new Object[] { obj1, "foo", 42 },
			  false
			);
			  """, javaAST.toString() );
	}	@Test
	public void invokeMethodWithKnownScope() throws IOException {
		String expression = """
						variables.system.out.println(
						 "hello world"
					   )
			""";

		BoxLangParser parser = new BoxLangParser();
		ParsingResult result = parser.parseStatement( expression );
		Node javaAST = BoxLangTranspiler.transform( result.getRoot() );

		assertEqualsNoWhiteSpaces("""
				Referencer.getAndInvoke(
					context,
					Referencer.get(
									variablesScope.get(Key.of("system")).get(Key.of("out")),false),
									Key.of("println"),
									newObject[]{"helloworld"},
									false);
				""", javaAST.toString() );
	}


	@Test
	public void assigment() throws IOException {
		String expression = """
						variables["system"] = "Hello"
			""";

		BoxLangParser parser = new BoxLangParser();
		ParsingResult result = parser.parseStatement( expression );

		Node javaAST = BoxLangTranspiler.transform( result.getRoot() );

		assertEquals( "variablesScope.put(Key.of(\"system\"), \"Hello\");", javaAST.toString() );

	}

	@Test
	public void var() throws IOException {
		String expression = """
						var a = b = 1/0;
			""";

		BoxLangParser parser = new BoxLangParser();
		ParsingResult result = parser.parseStatement( expression );

		BlockStmt javaAST = (BlockStmt)BoxLangTranspiler.transform( result.getRoot() );
		assertEquals( "context.getScopeNearby(Key.of(LocalScope.name)).put(Key.of(\"a\"), Divide.invoke(1, 0));", javaAST.getStatements().get(0).toString() );
		assertEquals( "context.getScopeNearby(Key.of(LocalScope.name)).put(Key.of(\"b\"), Divide.invoke(1, 0));", javaAST.getStatements().get(1).toString() );


	}

	@Test
	public void ifElse() throws IOException {
		String expression = """
      					var a = 1 + 2 * 3;
      					var b = "0";
						if( variables.a == "0" &&  variables.b == "0" ) {
							variables.a = a & "1";
						} else if( !foo ) {
							variables.a = a & "2";
			    		}
			""";

		BoxLangParser parser = new BoxLangParser();
		ParsingResult result = parser.parse( expression , BoxFileType.CF);

		Node javaAST = new BoxLangTranspiler().transpile( result.getRoot() );

		MethodDeclaration invokeMethod = javaAST.findCompilationUnit().orElseThrow()
			.getClassByName("TestClass").orElseThrow()
			.getMethodsByName("invoke").get(0);

		System.out.println(invokeMethod.toString());
		assertEquals("/**\n * Each template must implement the invoke() method which executes the template\n *\n * @param context The execution context requesting the execution\n */\npublic void invoke(IBoxContext context) throws Throwable {\n    // Reference to the variables scope\n    IScope variablesScope = context.getScopeNearby(Key.of(\"variables\"));\n    ClassLocator JavaLoader = ClassLocator.getInstance();\n    context.getScopeNearby(Key.of(LocalScope.name)).put(Key.of(\"a\"), 0);\n    if (BooleanCaster.cast(EqualsEquals.invoke(variablesScope.get(Key.of(\"a\")), 0))) {\n        variablesScope.put(Key.of(\"a\"), Plus.invoke(context.scopeFindNearby(Key.of(\"a\")), 1));\n    } else {\n        if (BooleanCaster.cast(Negate.invoke(foo))) {\n        }\n    }\n}", invokeMethod.toString() );


	}
}
