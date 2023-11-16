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
import com.github.javaparser.ast.expr.MethodCallExpr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ortus.boxlang.ast.BoxNode;
import ortus.boxlang.ast.expression.*;
import ortus.boxlang.transpiler.JavaTranspiler;
import ortus.boxlang.transpiler.transformer.AbstractTransformer;
import ortus.boxlang.transpiler.transformer.TransformerContext;

import java.util.HashMap;
import java.util.Map;

/**
 * Transform a BoxUnaryOperatorTransformer Node the equivalent Java Parser AST node
 */
public class BoxUnaryOperationTransformer extends AbstractTransformer {

	Logger logger = LoggerFactory.getLogger( BoxUnaryOperationTransformer.class );

	/**
	 * Transform a unary operator
	 *
	 * @param node    a BoxUnaryOperator instance
	 * @param context transformation context
	 *
	 * @return Generates a Method invocation to the Runtime Increment/Increment
	 *
	 * @throws IllegalStateException
	 */
	@Override
	public Node transform( BoxNode node, TransformerContext context ) throws IllegalStateException {
		BoxUnaryOperation	operation	= ( BoxUnaryOperation ) node;
		Map<String, String>	values		= new HashMap<>();

		Expression			expr		= ( Expression ) resolveScope( JavaTranspiler.transform( operation.getExpr() ), context );
		values.put( "expr", expr.toString() );

		if ( expr instanceof MethodCallExpr methodCall ) {
			values.put( "expr", methodCall.getScope().get().toString() );
			if ( methodCall.getArguments().size() > 0 )
				values.put( "key", methodCall.getArguments().get( 0 ).toString() );
		}

		String template = "";
		if ( operation.getOperator() == BoxUnaryOperator.PrePlusPlus ) {
			if ( values.containsKey( "key" ) ) {
				template = "Increment.invokePre(${expr},${key})";
			} else {
				template = "Increment.invoke(${expr})";
			}
		} else if ( operation.getOperator() == BoxUnaryOperator.PostPlusPlus ) {
			if ( values.containsKey( "key" ) ) {
				template = "Increment.invokePost(${expr},${key})";
				// post increment is ignored if the expression is a literal like 5++
			} else if ( operation.getExpr().isLiteral() ) {
				template = "Increment.invoke(${expr})";
			} else {
				template = "Increment.invoke(${expr})";
			}
		} else if ( operation.getOperator() == BoxUnaryOperator.PreMinusMinus ) {
			if ( values.containsKey( "key" ) ) {
				template = "Decrement.invokePre(${expr},${key})";
			} else {
				template = "Decrement.invoke(${expr})";
			}
		} else if ( operation.getOperator() == BoxUnaryOperator.PostMinusMinus ) {
			if ( values.containsKey( "key" ) ) {
				template = "Decrement.invokePost(${expr},${key})";
				// post increment is ignored if the expression is a literal like 5++
			} else if ( operation.getExpr().isLiteral() ) {
				template = "Decrement.invoke(${expr})";
			} else {
				template = "Decrement.invoke(${expr})";
			}
		} else if ( operation.getOperator() == BoxUnaryOperator.Minus ) {
			values.put( "expr", expr.toString() );
			template = "Negate.invoke(${expr})";
		} else if ( operation.getOperator() == BoxUnaryOperator.Not ) {
			values.put( "expr", expr.toString() );
			template = "Not.invoke(${expr})";
		}

		Node javaExpr = parseExpression( template, values );
		logger.info( "{} -> {}", node.getSourceText(), javaExpr );
		addIndex( javaExpr, node );
		return javaExpr;
	}
}
