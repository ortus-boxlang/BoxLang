package ortus.boxlang.compiler;

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.VariableDeclarator;

import ortus.boxlang.ast.BoxScript;
import ortus.boxlang.ast.expression.BoxLambda;
import ortus.boxlang.ast.expression.BoxStringLiteral;
import ortus.boxlang.ast.statement.BoxArgumentDeclaration;
import ortus.boxlang.ast.statement.BoxExpression;
import ortus.boxlang.parser.BoxCFParser;
import ortus.boxlang.parser.BoxParser;
import ortus.boxlang.parser.ParsingResult;
import ortus.boxlang.transpiler.JavaTranspiler;

public class TestLambda extends TestBase {

	private Node transformLambda( String expression ) throws IOException {
		BoxParser		parser	= new BoxParser();
		ParsingResult	result	= parser.parseExpression( expression );
		assertTrue( result.isCorrect() );

		JavaTranspiler transpiler = new JavaTranspiler();
		transpiler.setProperty( "packageName", "ortus.test" );
		transpiler.setProperty( "classname", "MyUDF" );
		return transpiler.transform( result.getRoot() );
	}

	@Test
	public void testLambdaParameters() throws IOException {
		String			code	= """
		                          ( required string param1='default' key="value1",
		                            required string param2='default' key="value2" ) key="Brad" -> {return param1}
		                           """;
		BoxCFParser		parser	= new BoxCFParser();
		ParsingResult	result	= parser.parse( code );
		assertTrue( result.isCorrect() );
		BoxScript script = ( BoxScript ) result.getRoot();
		script.getStatements().forEach( stmt -> {
			stmt.walk().forEach( it -> {
				BoxStringLiteral value;
				if ( it instanceof BoxExpression exp && exp.getExpression() instanceof BoxLambda lambda ) {
					Assertions.assertEquals( 2, lambda.getArgs().size() );
					Assertions.assertEquals( 1, lambda.getAnnotations().size() );

					BoxArgumentDeclaration arg;
					Assertions.assertEquals( 1, lambda.getArgs().get( 0 ).getAnnotations().size() );
					Assertions.assertEquals( 1, lambda.getArgs().get( 1 ).getAnnotations().size() );

				}
			} );
		} );

		CompilationUnit javaAST = ( CompilationUnit ) transformLambda( code );
		System.out.println( javaAST.toString() );
		VariableDeclarator arguments = javaAST.getType( 0 ).getFieldByName( "arguments" ).get().getVariable( 0 );
		Assertions.assertEquals( 2, arguments.getInitializer().get().asArrayInitializerExpr().getValues().size() );
		VariableDeclarator annotations = javaAST.getType( 0 ).getFieldByName( "annotations" ).get().getVariable( 0 );
		assertEqualsNoWhiteSpaces( """
		                           Struct.of(Key.of("key"),"Brad")
		                           """, annotations.getInitializer().get().toString() );

	}

}
