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
import ortus.boxlang.ast.expression.BoxBinaryOperation;
import ortus.boxlang.ast.expression.BoxBinaryOperator;
import ortus.boxlang.transpiler.BoxLangTranspiler;
import ortus.boxlang.transpiler.transformer.AbstractTransformer;
import ortus.boxlang.transpiler.transformer.TransformerContext;

import java.util.HashMap;
import java.util.Map;

/**
 * Transform a BoxBinaryOperation Node the equivalent Java Parser AST nodes
 */
public class BoxBinaryOperationTransformer extends AbstractTransformer {

	Logger logger = LoggerFactory.getLogger( BoxBinaryOperationTransformer.class );

	/**
	 * Transform BoxBinaryOperation argument
	 *
	 * @param node    a BoxBinaryOperation instance
	 * @param context transformation context
	 *
	 * @return generates a Java Parser Method invocation to the corresponding runtime implementation
	 *
	 * @throws IllegalStateException
	 *
	 * @see BoxBinaryOperation
	 * @see BoxBinaryOperator foe the supported operators
	 */
	@Override
	public Node transform( BoxNode node, TransformerContext context ) throws IllegalStateException {
		BoxBinaryOperation	operation	= ( BoxBinaryOperation ) node;
		Expression			left		= ( Expression ) resolveScope( BoxLangTranspiler.transform( operation.getLeft(), context ), context );
		Expression			right		= ( Expression ) resolveScope( BoxLangTranspiler.transform( operation.getRight(), context ), context );

		Map<String, String>	values		= new HashMap<>() {

											{
												put( "left", left.toString() );
												put( "right", right.toString() );

											}
										};

		String				template	= "";
		if ( operation.getOperator() == BoxBinaryOperator.Concat ) {
			template = "Concat.invoke(${left},${right})";
		} else if ( operation.getOperator() == BoxBinaryOperator.Plus ) {
			template = "Plus.invoke(${left},${right})";
		} else if ( operation.getOperator() == BoxBinaryOperator.Minus ) {
			template = "Minus.invoke(${left},${right})";
		} else if ( operation.getOperator() == BoxBinaryOperator.Star ) {
			template = "Multiply.invoke(${left},${right})";
		} else if ( operation.getOperator() == BoxBinaryOperator.Slash ) {
			template = "Divide.invoke(${left},${right})";
		} else if ( operation.getOperator() == BoxBinaryOperator.Backslash ) {
			template = "IntegerDivide.invoke(${left},${right})";
		} else if ( operation.getOperator() == BoxBinaryOperator.Power ) {
			template = "Power.invoke(${left},${right})";
		} else if ( operation.getOperator() == BoxBinaryOperator.Xor ) {
			template = "Xor.invoke(${left},${right})";
		} else if ( operation.getOperator() == BoxBinaryOperator.Mod ) {
			template = "Mod.invoke(${left},${right})";
		} else if ( operation.getOperator() == BoxBinaryOperator.And ) {
			template = "And.invoke(${left},${right})";
		} else if ( operation.getOperator() == BoxBinaryOperator.Or ) {
			template = "Or.invoke(${left},${right})";
		} else if ( operation.getOperator() == BoxBinaryOperator.Elvis ) {
			template = "Elvis.invoke(${left},${right})";
		} else if ( operation.getOperator() == BoxBinaryOperator.InstanceOf ) {
			template = "InstanceOf.invoke(${left},${right})";
		} else if ( operation.getOperator() == BoxBinaryOperator.Contains ) {
			template = "Contains.contains(${left},${right})";
		} else if ( operation.getOperator() == BoxBinaryOperator.NotContains ) {
			template = "!Contains.contains(${left},${right})";
		} else {
			throw new IllegalStateException( "not implemented" );
		}
		Node javaExpr = parseExpression( template, values );
		logger.info( node.getSourceText() + " -> " + javaExpr );
		// addIndex( javaExpr, node );
		return javaExpr;
	}

}
