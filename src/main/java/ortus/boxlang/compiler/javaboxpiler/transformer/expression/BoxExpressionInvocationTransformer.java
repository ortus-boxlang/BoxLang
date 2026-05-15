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
import com.github.javaparser.ast.expr.Expression;

import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.expression.BoxArgument;
import ortus.boxlang.compiler.ast.expression.BoxExpressionInvocation;
import ortus.boxlang.compiler.ast.expression.BoxSpreadExpression;
import ortus.boxlang.compiler.javaboxpiler.JavaTranspiler;
import ortus.boxlang.compiler.javaboxpiler.transformer.AbstractTransformer;
import ortus.boxlang.compiler.javaboxpiler.transformer.TransformerContext;

public class BoxExpressionInvocationTransformer extends AbstractTransformer {

	public BoxExpressionInvocationTransformer( JavaTranspiler transpiler ) {
		super( transpiler );
	}

	@Override
	public Node transform( BoxNode node, TransformerContext context ) throws IllegalStateException {
		BoxExpressionInvocation	invocation	= ( BoxExpressionInvocation ) node;
		boolean					allSpread	= isAllSpread( invocation.getArguments() );

		Expression				expr		= ( Expression ) transpiler.transform( invocation.getExpr(), context );

		Map<String, String>		values		= new HashMap<>() {

												{
													put( "contextName", transpiler.peekContextName() );
													put( "expr", expr.toString() );
												}
											};

		for ( int i = 0; i < invocation.getArguments().size(); i++ ) {
			BoxArgument arg = invocation.getArguments().get( i );
			if ( arg.isSpread() ) {
				BoxSpreadExpression	spread		= ( BoxSpreadExpression ) arg.getValue();
				Expression			innerExpr	= ( Expression ) transpiler.transform( spread.getExpression(), context );
				if ( allSpread ) {
					values.put( "arg" + i, innerExpr.toString() );
				} else {
					values.put( "arg" + i, "ortus.boxlang.runtime.dynamic.LiteralSpreadUtil.spread(" + innerExpr.toString() + ")" );
				}
			} else {
				Expression argExpr = ( Expression ) transpiler.transform( arg, context );
				values.put( "arg" + i, argExpr.toString() );
			}
		}

		String template;
		if ( allSpread ) {
			StringBuilder sb = new StringBuilder(
			    "ortus.boxlang.runtime.dynamic.LiteralSpreadUtil.invokeSpreadOnlyFunction( ${contextName}, ${expr}" );
			for ( int i = 0; i < invocation.getArguments().size(); i++ ) {
				sb.append( ", ${arg" ).append( i ).append( "}" );
			}
			sb.append( ")" );
			template = sb.toString();
		} else {
			template = "${contextName}.invokeFunction( ${expr}, "
			    + generateArguments( invocation.getArguments() )
			    + " )";
		}

		Node javaExpr = parseExpression( template, values );
		// logger.trace( node.getSourceText() + " -> " + javaExpr );
		addIndex( javaExpr, node );
		return javaExpr;
	}
}
