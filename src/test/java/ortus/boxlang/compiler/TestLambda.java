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

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.ObjectCreationExpr;

import ortus.boxlang.compiler.ast.BoxScript;
import ortus.boxlang.compiler.ast.expression.BoxLambda;
import ortus.boxlang.compiler.ast.expression.BoxStringLiteral;
import ortus.boxlang.compiler.ast.statement.BoxArgumentDeclaration;
import ortus.boxlang.compiler.ast.statement.BoxExpressionStatement;
import ortus.boxlang.compiler.javaboxpiler.JavaTranspiler;
import ortus.boxlang.compiler.parser.BoxParser;
import ortus.boxlang.compiler.parser.Parser;
import ortus.boxlang.compiler.parser.ParsingResult;
import ortus.boxlang.runtime.util.Pair;

public class TestLambda extends TestBase {

	private Pair<MethodDeclaration, Expression> transformLambda( String expression ) throws IOException {
		Parser			parser	= new Parser();
		ParsingResult	result	= parser.parseExpression( expression );
		assertTrue( result.isCorrect() );

		JavaTranspiler transpiler = new JavaTranspiler();
		transpiler.setProperty( "packageName", "ortus.test" );
		transpiler.setProperty( "classname", "MyUDF" );
		transpiler.setProperty( "mappingName", "" );
		transpiler.setProperty( "mappingPath", "" );
		transpiler.setProperty( "relativePath", "" );
		transpiler.transform( result.getRoot() );
		return transpiler.getLambdaInvokers().get( 0 );
	}

	@Test
	public void testLambdaParameters() throws IOException {
		String			code	= """
		                          ( required string param1='default' key="value1",
		                            required string param2='default' key="value2" ) key="Brad" -> {return param1}
		                           """;
		BoxParser		parser	= new BoxParser();
		ParsingResult	result	= parser.parse( code );
		assertTrue( result.isCorrect() );
		BoxScript script = ( BoxScript ) result.getRoot();
		script.getStatements().forEach( stmt -> {
			stmt.getDescendants().forEach( it -> {
				BoxStringLiteral value;
				if ( it instanceof BoxExpressionStatement exp && exp.getExpression() instanceof BoxLambda lambda ) {
					Assertions.assertEquals( 2, lambda.getArgs().size() );
					Assertions.assertEquals( 1, lambda.getAnnotations().size() );

					BoxArgumentDeclaration arg;
					Assertions.assertEquals( 1, lambda.getArgs().get( 0 ).getAnnotations().size() );
					Assertions.assertEquals( 1, lambda.getArgs().get( 1 ).getAnnotations().size() );

				}
			} );
		} );

		// With the new inline lambda architecture, lambdas are stored as Pair<MethodDeclaration, Expression>
		// The Expression is the "new Lambda(...)" instantiation expression
		Pair<MethodDeclaration, Expression>	lambdaPair		= transformLambda( code );
		MethodDeclaration					invokerMethod	= lambdaPair.getFirst();
		Expression							instantiation	= lambdaPair.getSecond();

		// Verify the invoker method exists
		Assertions.assertNotNull( invokerMethod );
		Assertions.assertTrue( invokerMethod.getNameAsString().startsWith( "invokeLambda_" ) );

		// Verify the instantiation expression is a new Lambda(...) call
		Assertions.assertTrue( instantiation instanceof ObjectCreationExpr );
		ObjectCreationExpr newLambda = ( ObjectCreationExpr ) instantiation;
		Assertions.assertEquals( "Lambda", newLambda.getType().getNameAsString() );

		// The Lambda constructor has 12 arguments; argument 2 (index 1) is the Argument[] array
		// new Lambda(name, arguments, returnType, access, annotations, documentation, modifiers, defaultOutput, imports, sourceType, path, invoker)
		Expression argumentsExpr = newLambda.getArguments().get( 1 );
		Assertions.assertTrue( argumentsExpr.isArrayCreationExpr() );
		// The array should have 2 Argument elements
		Assertions.assertEquals( 2, argumentsExpr.asArrayCreationExpr().getInitializer().get().getValues().size() );
	}

}
