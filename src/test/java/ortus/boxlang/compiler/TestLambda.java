/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
import ortus.boxlang.parser.BoxScriptParser;
import ortus.boxlang.parser.Parser;
import ortus.boxlang.parser.ParsingResult;
import ortus.boxlang.transpiler.JavaTranspiler;

public class TestLambda extends TestBase {

	private Node transformLambda( String expression ) throws IOException {
		Parser			parser	= new Parser();
		ParsingResult	result	= parser.parseExpression( expression );
		assertTrue( result.isCorrect() );

		JavaTranspiler transpiler = new JavaTranspiler();
		transpiler.setProperty( "packageName", "ortus.test" );
		transpiler.setProperty( "classname", "MyUDF" );
		transpiler.transform( result.getRoot() );
		return transpiler.getCallables().get( 0 );
	}

	@Test
	public void testLambdaParameters() throws IOException {
		String			code	= """
		                          ( required string param1='default' key="value1",
		                            required string param2='default' key="value2" ) key="Brad" -> {return param1}
		                           """;
		BoxScriptParser	parser	= new BoxScriptParser();
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

		CompilationUnit		javaAST		= ( CompilationUnit ) transformLambda( code );
		VariableDeclarator	arguments	= javaAST.getType( 0 ).getFieldByName( "arguments" ).get().getVariable( 0 );
		Assertions.assertEquals( 2, arguments.getInitializer().get().asArrayInitializerExpr().getValues().size() );
		VariableDeclarator annotations = javaAST.getType( 0 ).getFieldByName( "annotations" ).get().getVariable( 0 );

	}

}
