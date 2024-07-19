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
package ortus.boxlang.compiler.javaboxpiler.transformer.expression;

import java.util.HashMap;
import java.util.Map;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.type.UnknownType;

import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.expression.BoxFunctionalMemberAccess;
import ortus.boxlang.compiler.javaboxpiler.JavaTranspiler;
import ortus.boxlang.compiler.javaboxpiler.transformer.AbstractTransformer;
import ortus.boxlang.compiler.javaboxpiler.transformer.TransformerContext;

public class BoxFunctionalMemberAccessTransformer extends AbstractTransformer {

	public BoxFunctionalMemberAccessTransformer( JavaTranspiler transpiler ) {
		super( transpiler );
	}

	/**
	 * Transform a BoxFunctionalMemberAccess expression
	 *
	 * @param node    a BoxFunctionalMemberAccess instance
	 * @param context transformation context
	 *
	 * @return Generates a throw
	 *
	 * @throws IllegalStateException
	 */
	@Override
	public Node transform( BoxNode node, TransformerContext context ) throws IllegalStateException {
		BoxFunctionalMemberAccess	boxFunctionalMemberAccess	= ( BoxFunctionalMemberAccess ) node;
		String						name						= boxFunctionalMemberAccess.getName();

		Map<String, String>			values						= new HashMap<>() {

																	{
																		put( "name", createKey( name ).toString() );
																		put( "contextName", transpiler.peekContextName() );

																	}
																};
		if ( boxFunctionalMemberAccess.getArguments() == null || boxFunctionalMemberAccess.getArguments().isEmpty() ) {
			String	template	= "FunctionalMemberAccess.of( ${name} )";
			Node	javaStmt	= parseExpression( template, values );
			addIndex( javaStmt, node );
			return javaStmt;
		} else {
			boolean			isNamed	= boxFunctionalMemberAccess.getArguments().get( 0 ).getName() != null;
			NullLiteralExpr	_null	= new NullLiteralExpr();

			for ( int i = 0; i < boxFunctionalMemberAccess.getArguments().size(); i++ ) {
				Expression expr = ( Expression ) transpiler.transform( boxFunctionalMemberAccess.getArguments().get( i ) );
				values.put( "arg" + i, expr.toString() );
			}
			String				template			= "new FunctionalMemberAccessArgs( ${name}, ${contextName} )";
			ObjectCreationExpr	javaExpr			= ( ObjectCreationExpr ) parseExpression( template, values );

			String				lambdaContextName	= "lambdaContext" + transpiler.incrementAndGetLambdaContextCounter();
			transpiler.pushContextName( lambdaContextName );
			Expression argExpression = parseExpression( generateArguments( boxFunctionalMemberAccess.getArguments() ), values );
			transpiler.popContextName();

			LambdaExpr lambda = new LambdaExpr();
			lambda.setParameters( new NodeList<>(
			    new Parameter( new UnknownType(), lambdaContextName ) ) );
			BlockStmt body = new BlockStmt();
			body.addStatement( new ReturnStmt( ( Expression ) argExpression ) );
			lambda.setBody( body );

			if ( isNamed ) {
				javaExpr.addArgument( lambda );
				javaExpr.addArgument( _null );
			} else {
				javaExpr.addArgument( _null );
				javaExpr.addArgument( lambda );
			}
			// logger.trace( node.getSourceText() + " -> " + javaExpr );
			addIndex( javaExpr, node );
			return javaExpr;
		}

	}

}
