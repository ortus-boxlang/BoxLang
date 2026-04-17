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
import ortus.boxlang.compiler.ast.expression.BoxNew;
import ortus.boxlang.compiler.ast.expression.BoxStringInterpolation;
import ortus.boxlang.compiler.ast.expression.BoxStringLiteral;
import ortus.boxlang.compiler.javaboxpiler.JavaTranspiler;
import ortus.boxlang.compiler.javaboxpiler.transformer.AbstractTransformer;
import ortus.boxlang.compiler.javaboxpiler.transformer.TransformerContext;
import ortus.boxlang.runtime.types.exceptions.ExpressionException;

public class BoxNewTransformer extends AbstractTransformer {

	public BoxNewTransformer( JavaTranspiler transpiler ) {
		super( transpiler );
	}

	/**
	 * Transform a new expression
	 *
	 * @param node    a BoxNewOperation instance
	 * @param context transformation context
	 *
	 * @return Generates a throw
	 *
	 */
	@Override
	public Node transform( BoxNode node, TransformerContext context ) {
		BoxNew			boxNew	= ( BoxNew ) node;
		BoxExpression	expr	= boxNew.getExpression();
		String			fqn;
		String			prefix	= boxNew.getPrefix() == null ? "" : boxNew.getPrefix().getName() + ":";
		String			exprSource;
		if ( expr instanceof BoxStringLiteral bsl ) {
			fqn			= bsl.getValue();
			fqn			= prefix + fqn.replace( "java:", "" );
			exprSource	= new StringLiteralExpr( fqn ).toString();
		} else if ( expr instanceof BoxFQN bFqn ) {
			fqn			= bFqn.getValue();
			fqn			= prefix + fqn.replace( "java:", "" );
			exprSource	= new StringLiteralExpr( fqn ).toString();
		} else if ( expr instanceof BoxStringInterpolation bsi ) {
			fqn			= transpiler.transform( ( BoxNode ) bsi, context ).toString();
			exprSource	= "StringCaster.cast(" + fqn + ")";
		} else {
			throw new ExpressionException( "BoxNew expression must be a string literal or FQN, but was a " + expr.getClass().getSimpleName(), expr );
		}
		Map<String, String> values = new HashMap<>() {

			{
				put( "expr", exprSource );
				put( "contextName", transpiler.peekContextName() );

			}
		};
		for ( int i = 0; i < boxNew.getArguments().size(); i++ ) {
			Expression expr2 = ( Expression ) transpiler.transform( ( BoxNode ) boxNew.getArguments().get( i ), context );
			values.put( "arg" + i, expr2.toString() );
		}

		String	template	= "classLocator.load(${contextName}, ${expr}, imports).invokeConstructor( ${contextName}, "
		    + generateArguments( boxNew.getArguments() ) + " ).unWrapBoxLangClass()";
		Node	javaStmt	= parseExpression( template, values );
		// logger.trace( node.getSourceText() + " -> " + javaStmt );
		addIndex( javaStmt, node );
		return javaStmt;

	}
}
