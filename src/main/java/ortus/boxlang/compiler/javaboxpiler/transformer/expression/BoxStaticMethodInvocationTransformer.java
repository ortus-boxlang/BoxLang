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
import com.github.javaparser.ast.expr.StringLiteralExpr;

import ortus.boxlang.compiler.ast.BoxExpression;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.expression.BoxFQN;
import ortus.boxlang.compiler.ast.expression.BoxIdentifier;
import ortus.boxlang.compiler.ast.expression.BoxStaticMethodInvocation;
import ortus.boxlang.compiler.javaboxpiler.JavaTranspiler;
import ortus.boxlang.compiler.javaboxpiler.transformer.AbstractTransformer;
import ortus.boxlang.compiler.javaboxpiler.transformer.TransformerContext;
import ortus.boxlang.runtime.types.exceptions.ExpressionException;

public class BoxStaticMethodInvocationTransformer extends AbstractTransformer {

	public BoxStaticMethodInvocationTransformer( JavaTranspiler transpiler ) {
		super( transpiler );
	}

	@Override
	public Node transform( BoxNode node, TransformerContext context ) throws IllegalStateException {
		BoxStaticMethodInvocation	invocation	= ( BoxStaticMethodInvocation ) node;
		String						side		= context == TransformerContext.NONE ? "" : "(" + context.toString() + ") ";
		BoxExpression				baseObject	= invocation.getObj();
		Expression					expr;

		if ( baseObject instanceof BoxFQN fqn ) {
			expr = new StringLiteralExpr( fqn.getValue() );
		} else if ( baseObject instanceof BoxIdentifier id ) {
			expr = ( Expression ) transpiler.transform( id, context );
		} else {
			throw new ExpressionException( "Unexpected base token in static method access.", baseObject );
		}
		Map<String, String> values = new HashMap<>() {

			{
				put( "contextName", transpiler.peekContextName() );
				put( "methodKey", createKey( ( invocation.getName() ).getName() ).toString() );
			}
		};

		for ( int i = 0; i < invocation.getArguments().size(); i++ ) {
			Expression expr2 = ( Expression ) transpiler.transform( ( BoxNode ) invocation.getArguments().get( i ), context );
			values.put( "arg" + i, expr2.toString() );
		}

		values.put( "expr", expr.toString() );

		Node javaExpr = parseExpression( getTemplate( invocation ), values );
		// logger.trace( side + node.getSourceText() + " -> " + javaExpr );
		addIndex( javaExpr, node );
		return javaExpr;
	}

	private String getTemplate( BoxStaticMethodInvocation function ) {
		StringBuilder sb = new StringBuilder(
		    "Referencer.getAndInvoke(${contextName},BoxClassSupport.ensureClass(${contextName},${expr},imports),${methodKey}," );

		sb.append( generateArguments( function.getArguments() ) );
		sb.append( ", false)" );
		return sb.toString();
	}
}
