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
import static org.junit.Assert.assertTrue;

public class TestStatements extends TestBase {

	public ParsingResult parseStatement(String statement) throws IOException {
		BoxLangParser parser = new BoxLangParser();
		ParsingResult result = parser.parseStatement( statement );
		assertTrue(result.isCorrect());
		return result;
	}

	@Test
	public void invokeMethod() throws IOException {
		String statement = """
						myObject.myMethod( obj1, "foo", 42 )
			""";

		ParsingResult result = parseStatement(statement);
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
		String statement = """
						variables.system.out.println(
						 "hello world"
					   )
			""";

		ParsingResult result = parseStatement(statement);
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
		String statement = """
						variables["system"] = "Hello"
			""";

		ParsingResult result = parseStatement(statement);
		Node javaAST = BoxLangTranspiler.transform( result.getRoot() );

		assertEquals( "variablesScope.put(Key.of(\"system\"), \"Hello\");", javaAST.toString() );
	}

	@Test
	public void var() throws IOException {
		String statement = """
						var a = b = 1/0;
			""";

		ParsingResult result = parseStatement(statement);

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

		ParsingResult result = parseStatement(statement);

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

		ParsingResult result = parseStatement(statement);

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
	public void case1_() throws IOException {
		String statement = """
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

		ParsingResult result = parseStatement(statement);

		Node javaAST =  BoxLangTranspiler.transform( result.getRoot() );
		System.out.println(javaAST);
		assertEqualsNoWhiteSpaces(
			"""
				do {
					if (BooleanCaster.cast(EqualsEquals.invoke((variablesScope.get(Key.of("a"))), "9"))) {
						variablesScope.put(Key.of("a"), "0");
						break;
					}
					if (BooleanCaster.cast(EqualsEquals.invoke((variablesScope.get(Key.of("a"))), "1"))) {
						variablesScope.put(Key.of("a"), "1");
						break;
					}
					variablesScope.put(Key.of("a"), "default");
					break;
				} while (false);
				"""
			, javaAST.toString());
	}
	@Test
	public void case2_() throws IOException {
		String statement = """
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

		ParsingResult result = parseStatement(statement);

		Node javaAST =  BoxLangTranspiler.transform( result.getRoot() );
		System.out.println(javaAST);
		assertEqualsNoWhiteSpaces(
			"""
				do {
					if (BooleanCaster.cast(GreaterThan.invoke(variablesScope.get(Key.of("a")), "0"))) {
						variablesScope.put(Key.of("a"), "0");
						break;
					}
					if (BooleanCaster.cast(LessThan.invoke(variablesScope.get(Key.of("a")), "1"))) {
						variablesScope.put(Key.of("a"), "1");
						break;
					}
					variablesScope.put(Key.of("a"), "default");
					break;
				} while (false);
				"""
			, javaAST.toString());
	}

	@Test
	public void for_() throws IOException {
		String statement = """
			for( keyName in variables ) {
				variables['a'] = variables['a'] + 1;
			}
			""";

		ParsingResult result = parseStatement(statement);

		Node javaAST =  BoxLangTranspiler.transform( result.getRoot() );
		System.out.println(javaAST);
		assertEqualsNoWhiteSpaces(
			"""
				{
					Iterator keyName = CollectionCaster.cast(variablesScope).iterator();
					while (keyName.hasNext()) {
						variablesScope.put(Key.of("keyName"), keyName.next());
						variablesScope.put(Key.of("a"), Plus.invoke(variablesScope.get(Key.of("a")), 1));
					}
				}
				"""
			, javaAST.toString());
	}
}
