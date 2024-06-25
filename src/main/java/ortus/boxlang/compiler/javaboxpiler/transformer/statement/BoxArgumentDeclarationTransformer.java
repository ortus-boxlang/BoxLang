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
package ortus.boxlang.compiler.javaboxpiler.transformer.statement;

import java.util.Map;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.UnknownType;

import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.statement.BoxArgumentDeclaration;
import ortus.boxlang.compiler.javaboxpiler.JavaTranspiler;
import ortus.boxlang.compiler.javaboxpiler.transformer.AbstractTransformer;
import ortus.boxlang.compiler.javaboxpiler.transformer.TransformerContext;

/**
 * Transform a BoxArgumentDeclarationTransformer Node the equivalent Java Parser AST nodes
 */
public class BoxArgumentDeclarationTransformer extends AbstractTransformer {

	public BoxArgumentDeclarationTransformer( JavaTranspiler transpiler ) {
		super( transpiler );
	}

	@Override
	public Node transform( BoxNode node, TransformerContext context ) throws IllegalStateException {
		BoxArgumentDeclaration	boxArgument			= ( BoxArgumentDeclaration ) node;

		/* Process default value */
		Expression				defaultLiteral		= new NullLiteralExpr();
		Expression				defaultExpression	= new NullLiteralExpr();
		if ( boxArgument.getValue() != null ) {
			if ( boxArgument.getValue().isLiteral() ) {
				Node initExpr = transpiler.transform( boxArgument.getValue() );
				defaultLiteral = ( Expression ) initExpr;
			} else {
				String lambdaContextName = "lambdaContext" + transpiler.incrementAndGetLambdaContextCounter();
				transpiler.pushContextName( lambdaContextName );
				Node initExpr = transpiler.transform( boxArgument.getValue() );
				transpiler.popContextName();

				LambdaExpr lambda = new LambdaExpr();
				lambda.setParameters( new NodeList<>(
				    new Parameter( new UnknownType(), lambdaContextName ) ) );
				BlockStmt body = new BlockStmt();
				body.addStatement( parseStatement( "ClassLocator classLocator = ClassLocator.getInstance();", Map.of() ) );
				body.addStatement( new ReturnStmt( ( Expression ) initExpr ) );
				lambda.setBody( body );
				defaultExpression = lambda;
			}
		}

		/* Process annotations */
		Expression				annotationStruct	= transformAnnotations( boxArgument.getAnnotations() );
		/* Process documentation */
		Expression				documentationStruct	= transformDocumentation( boxArgument.getDocumentation() );

		// Create the argument list
		NodeList<Expression>	arguments			= new NodeList<Expression>(
		    new BooleanLiteralExpr( boxArgument.getRequired() ),
		    new StringLiteralExpr( boxArgument.getType() ),
		    createKey( boxArgument.getName() ),
		    defaultLiteral,
		    defaultExpression,
		    annotationStruct,
		    documentationStruct
		);

		// Create the object creation expression
		Expression				javaExpr			= new ObjectCreationExpr(
		    null,
		    new ClassOrInterfaceType( null, "Argument" ),
		    arguments
		);

		// logger.trace( "{} -> {}", node.getSourceText(), javaExpr );
		return javaExpr;
	}

}
