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
		String statement = """
						if( variables.a == "0" ) {
							variables.a = a & "1";
						} else if( !foo ) {
							variables.a = a & "2";
			    		}
			""";

		BoxLangParser parser = new BoxLangParser();
		ParsingResult result = parser.parseStatement( statement );

		Node javaAST =  BoxLangTranspiler.transform( result.getRoot() );
		assertEqualsNoWhiteSpaces(
			"""
				if (BooleanCaster.cast(EqualsEquals.invoke(variablesScope.get(Key.of("a")), "0"))) {
					variablesScope.put(Key.of("a"), Concat.invoke(context.scopeFindNearby(Key.of("a")).value(), "1"));
				} else if (Negate.invoke(foo)) {
					variablesScope.put(Key.of("a"), Concat.invoke(context.scopeFindNearby(Key.of("a")).value(), "2"));
				}
				"""
			, javaAST.toString());
	}

	@Test
	public void while_() throws IOException {
		String statement = """
						while( variables.a == true ) {
							variables.a = false;
			    		}
			""";

		BoxLangParser parser = new BoxLangParser();
		ParsingResult result = parser.parseStatement( statement );

		Node javaAST =  BoxLangTranspiler.transform( result.getRoot() );
		assertEqualsNoWhiteSpaces(
			"""
				while(BooleanCaster.cast(EqualsEquals.invoke(variablesScope.get(Key.of("a")),true))){
					variablesScope.put(Key.of("a"),false);
				}
				"""
			, javaAST.toString());
	}
	@Test
	public void case_() throws IOException {
		String statement = """
						while( variables.a == true ) {
							variables.a = false;
			    		}
			""";

		BoxLangParser parser = new BoxLangParser();
		ParsingResult result = parser.parseStatement( statement );

		Node javaAST =  BoxLangTranspiler.transform( result.getRoot() );
		assertEqualsNoWhiteSpaces(
			"""
				while(BooleanCaster.cast(EqualsEquals.invoke(variablesScope.get(Key.of("a")),true))){
					variablesScope.put(Key.of("a"),false);
				}
				"""
			, javaAST.toString());
	}
}
