/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package ortus.boxlang.compiler.javaboxpiler.transformer.expression;

import java.util.HashMap;
import java.util.Map;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;

import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.expression.BoxArgument;
import ortus.boxlang.compiler.ast.expression.BoxFunctionInvocation;
import ortus.boxlang.compiler.ast.expression.BoxSpreadExpression;
import ortus.boxlang.compiler.javaboxpiler.JavaTranspiler;
import ortus.boxlang.compiler.javaboxpiler.transformer.AbstractTransformer;
import ortus.boxlang.compiler.javaboxpiler.transformer.TransformerContext;

public class BoxFunctionInvocationTransformer extends AbstractTransformer {

	public BoxFunctionInvocationTransformer( JavaTranspiler transpiler ) {
		super( transpiler );
	}

	@Override
	public Node transform( BoxNode node, TransformerContext context ) throws IllegalStateException {

		BoxFunctionInvocation	function			= ( BoxFunctionInvocation ) node;
		String					methodName			= function.getName();
		boolean					isSafeMethodCall	= methodName.equalsIgnoreCase( "isnull" );
		TransformerContext		safe				= isSafeMethodCall ? TransformerContext.SAFE : context;
		boolean					allSpread			= isAllSpread( function.getArguments() );

		// logger.trace( side + node.getSourceText() );

		Map<String, String>		values				= new HashMap<>() {

														{
															put( "functionName", createKey( methodName ).toString() );
															put( "contextName", transpiler.peekContextName() );
														}
													};

		for ( int i = 0; i < function.getArguments().size(); i++ ) {
			BoxArgument arg = function.getArguments().get( i );
			if ( arg.isSpread() ) {
				BoxSpreadExpression	spread		= ( BoxSpreadExpression ) arg.getValue();
				Expression			innerExpr	= ( Expression ) transpiler.transform( spread.getExpression(), safe );
				if ( allSpread ) {
					// For all-spread: pass raw inner values (dispatch handles them)
					values.put( "arg" + i, innerExpr.toString() );
				} else {
					values.put( "arg" + i, "ortus.boxlang.runtime.dynamic.LiteralSpreadUtil.spread(" + innerExpr.toString() + ")" );
				}
			} else {
				Expression expr = ( Expression ) transpiler.transform( arg, safe );
				values.put( "arg" + i, expr.toString() );
			}
		}
		String	template	= getTemplate( function, allSpread );
		Node	javaExpr	= parseExpression( template, values );
		// logger.trace( side + node.getSourceText() + " -> " + javaExpr );
		addIndex( javaExpr, node );
		return javaExpr;
	}

	private String getTemplate( BoxFunctionInvocation function, boolean allSpread ) {
		if ( allSpread ) {
			StringBuilder sb = new StringBuilder(
			    "ortus.boxlang.runtime.dynamic.LiteralSpreadUtil.invokeSpreadOnlyFunction( ${contextName}, ${functionName}" );
			for ( int i = 0; i < function.getArguments().size(); i++ ) {
				sb.append( ", ${arg" ).append( i ).append( "}" );
			}
			sb.append( ")" );
			return sb.toString();
		}
		StringBuilder sb = new StringBuilder( "${contextName}.invokeFunction( ${functionName}, " );
		sb.append( generateArguments( function.getArguments() ) );
		sb.append( ")" );
		return sb.toString();
	}
}
