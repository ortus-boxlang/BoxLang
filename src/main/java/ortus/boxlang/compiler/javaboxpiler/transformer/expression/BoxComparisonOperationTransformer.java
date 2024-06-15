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
import ortus.boxlang.compiler.ast.expression.BoxComparisonOperation;
import ortus.boxlang.compiler.ast.expression.BoxComparisonOperator;
import ortus.boxlang.compiler.javaboxpiler.JavaTranspiler;
import ortus.boxlang.compiler.javaboxpiler.transformer.AbstractTransformer;
import ortus.boxlang.compiler.javaboxpiler.transformer.TransformerContext;
import ortus.boxlang.runtime.types.exceptions.ExpressionException;

/**
 * Transform a BoxComparisonOperation Node the equivalent Java Parser AST nodes
 */
public class BoxComparisonOperationTransformer extends AbstractTransformer {

	public BoxComparisonOperationTransformer( JavaTranspiler transpiler ) {
		super( transpiler );
	}

	/**
	 * Transform BoxComparisonOperation operator
	 *
	 * @param node    a BoxComparisonOperation instance
	 * @param context transformation context
	 *
	 * @return generates a Java Parser Method invocation to the corresponding runtime implementation
	 *
	 * @throws IllegalStateException
	 *
	 * @see BoxComparisonOperation
	 * @see BoxComparisonOperator foe the supported comparision operators
	 */
	@Override
	public Node transform( BoxNode node, TransformerContext context ) throws IllegalStateException {
		BoxComparisonOperation	operation	= ( BoxComparisonOperation ) node;
		Expression				left		= ( Expression ) transpiler.transform( operation.getLeft() );
		Expression				right		= ( Expression ) transpiler.transform( operation.getRight() );

		Map<String, String>		values		= new HashMap<>() {

												{
													put( "left", left.toString() );
													put( "right", right.toString() );
													put( "contextName", transpiler.peekContextName() );

												}
											};
		String					template	= "";

		if ( operation.getOperator() == BoxComparisonOperator.Equal ) {
			template = "EqualsEquals.invoke(${left},${right})";
		} else if ( operation.getOperator() == BoxComparisonOperator.NotEqual ) {
			template = "!EqualsEquals.invoke(${left},${right})";
		} else if ( operation.getOperator() == BoxComparisonOperator.TEqual ) {
			template = "EqualsEqualsEquals.invoke(${left},${right})";
		} else if ( operation.getOperator() == BoxComparisonOperator.GreaterThan ) {
			template = "GreaterThan.invoke(${left},${right})";
		} else if ( operation.getOperator() == BoxComparisonOperator.GreaterThanEquals ) {
			template = "GreaterThanEqual.invoke(${left},${right})";
		} else if ( operation.getOperator() == BoxComparisonOperator.LessThan ) {
			template = "LessThan.invoke(${left},${right})";
		} else if ( operation.getOperator() == BoxComparisonOperator.LessThanEquals) {
			template = "LessThanEqual.invoke(${left},${right})";
		} else {
			throw new ExpressionException( "not implemented", operation );
		}
		Node javaExpr = parseExpression( template, values );
		// logger.trace( node.getSourceText() + " (" + context.name() + ") -> " + javaExpr );
		addIndex( javaExpr, node );
		return javaExpr;
	}

}
