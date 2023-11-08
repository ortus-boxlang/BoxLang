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
package ortus.boxlang.transpiler.transformer.expression;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ortus.boxlang.ast.BoxNode;
import ortus.boxlang.ast.expression.BoxFunctionInvocation;
import ortus.boxlang.transpiler.JavaTranspiler;
import ortus.boxlang.transpiler.transformer.AbstractTransformer;
import ortus.boxlang.transpiler.transformer.TransformerContext;

import java.util.HashMap;
import java.util.Map;

public class BoxFunctionInvocationTransformer extends AbstractTransformer {

	Logger logger = LoggerFactory.getLogger( BoxFunctionInvocationTransformer.class );

	public BoxFunctionInvocationTransformer() {

	}

	@Override
	public Node transform( BoxNode node, TransformerContext context ) throws IllegalStateException {

		BoxFunctionInvocation	function	= ( BoxFunctionInvocation ) node;
		String					side		= context == TransformerContext.NONE ? "" : "(" + context.toString() + ") ";
		logger.info( side + node.getSourceText() );

		Map<String, String> values = new HashMap<>() {

			{
				put( "name", function.getName().getName() );
			}
		};
		for ( int i = 0; i < function.getArguments().size(); i++ ) {
			Expression expr = ( Expression ) JavaTranspiler.transform( function.getArguments().get( i ) );
			values.put( "arg" + i, expr.toString() );
		}
		String	template	= getTemplate( function );
		Node	javaExpr	= parseExpression( template, values );
		logger.info( side + node.getSourceText() + " -> " + javaExpr );
		addIndex( javaExpr, node );
		return javaExpr;
	}

	private String getTemplate( BoxFunctionInvocation function ) {

		String target = BoxBuiltinRegistry.getInstance().getRegistry().get( function.getName().getName().toLowerCase() );
		;
		if ( target != null )
			return target;
		StringBuilder sb = new StringBuilder( "context.invokeFunction( Key.of( \"${name}\" )" );

		sb.append( ", new Object[] { " );
		for ( int i = 0; i < function.getArguments().size(); i++ ) {
			sb.append( "${" ).append( "arg" ).append( i ).append( "}" );
			if ( i < function.getArguments().size() - 1 ) {
				sb.append( "," );
			}
		}
		sb.append( "}" );
		sb.append( ")" );
		return sb.toString();
	}
}
