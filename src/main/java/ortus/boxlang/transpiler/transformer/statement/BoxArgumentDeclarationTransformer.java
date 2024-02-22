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
package ortus.boxlang.transpiler.transformer.statement;

import java.util.Map;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.type.UnknownType;

import ortus.boxlang.ast.BoxNode;
import ortus.boxlang.ast.statement.BoxArgumentDeclaration;
import ortus.boxlang.transpiler.JavaTranspiler;
import ortus.boxlang.transpiler.transformer.AbstractTransformer;
import ortus.boxlang.transpiler.transformer.TransformerContext;

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
		String					defaultLiteral		= "null";
		String					defaultExpression	= "null";
		if ( boxArgument.getValue() != null ) {
			if ( boxArgument.getValue().isLiteral() ) {
				Node initExpr = transpiler.transform( boxArgument.getValue() );
				defaultLiteral = initExpr.toString();
			} else {
				String lambdaContextName = "lambdaContext" + transpiler.incrementAndGetLambdaContextCounter();
				transpiler.pushContextName( lambdaContextName );
				Node initExpr = transpiler.transform( boxArgument.getValue() );
				transpiler.popContextName();

				LambdaExpr lambda = new LambdaExpr();
				lambda.setParameters( new NodeList<>(
				    new Parameter( new UnknownType(), lambdaContextName ) ) );
				lambda.setBody( new ExpressionStmt( ( Expression ) initExpr ) );
				defaultExpression = lambda.toString();
			}
		}

		/* Process annotations */
		Expression			annotationStruct	= transformAnnotations( boxArgument.getAnnotations() );
		/* Process documentation */
		Expression			documentationStruct	= transformDocumentation( boxArgument.getDocumentation() );

		Map<String, String>	values				= Map.of(
		    "required", String.valueOf( boxArgument.getRequired() ),
		    "type", boxArgument.getType(),
		    "name", createKey( boxArgument.getName() ).toString(),
		    "defaultLiteral", defaultLiteral,
		    "defaultExpression", defaultExpression,
		    "annotations", annotationStruct.toString(),
		    "documentation", documentationStruct.toString()
		);
		String				template			= """
		                                          new Argument( ${required}, "${type}" , ${name}, ${defaultLiteral}, ${defaultExpression}, ${annotations} ,${documentation} )
		                                          """;
		Expression			javaExpr			= ( Expression ) parseExpression( template, values );
		logger.debug( "{} -> {}", node.getSourceText(), javaExpr );
		return javaExpr;
	}

}
