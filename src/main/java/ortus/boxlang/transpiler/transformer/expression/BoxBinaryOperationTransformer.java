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

import java.util.HashMap;
import java.util.Map;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;

import ortus.boxlang.ast.BoxNode;
import ortus.boxlang.ast.expression.BoxBinaryOperation;
import ortus.boxlang.ast.expression.BoxBinaryOperator;
import ortus.boxlang.transpiler.JavaTranspiler;
import ortus.boxlang.transpiler.transformer.AbstractTransformer;
import ortus.boxlang.transpiler.transformer.TransformerContext;

/**
 * Transform a BoxBinaryOperation Node the equivalent Java Parser AST nodes
 */
public class BoxBinaryOperationTransformer extends AbstractTransformer {

	public BoxBinaryOperationTransformer( JavaTranspiler transpiler ) {
		super( transpiler );
	}

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
		TransformerContext	safe		= operation.getOperator() == BoxBinaryOperator.Elvis ? TransformerContext.SAFE : context;
		Expression			left		= ( Expression ) transpiler.transform( operation.getLeft(), safe );
		Expression			right		= ( Expression ) transpiler.transform( operation.getRight(), context );

		Map<String, String>	values		= new HashMap<>() {

											{
												put( "left", left.toString() );
												put( "right", right.toString() );
												put( "contextName", transpiler.peekContextName() );

											}
										};

		String				template	= "";
		if ( operation.getOperator() == BoxBinaryOperator.Plus ) {
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
			template = "XOR.invoke(${left},${right})";
		} else if ( operation.getOperator() == BoxBinaryOperator.Mod ) {
			template = "Modulus.invoke(${left},${right})";
		} else if ( operation.getOperator() == BoxBinaryOperator.And ) {
			template = "And.invoke(${left},${right})";
		} else if ( operation.getOperator() == BoxBinaryOperator.Or ) {
			template = "Or.invoke(${left},${right})";
		} else if ( operation.getOperator() == BoxBinaryOperator.Equivalence ) {
			template = "Equivalence.invoke(${left},${right})";
		} else if ( operation.getOperator() == BoxBinaryOperator.Implies ) {
			template = "Implies.invoke(${left},${right})";
		} else if ( operation.getOperator() == BoxBinaryOperator.Elvis ) {
			template = "Elvis.invoke(${left},${right})";
		} else if ( operation.getOperator() == BoxBinaryOperator.InstanceOf ) {
			template = "InstanceOf.invoke(${contextName},${left},${right})";
		} else if ( operation.getOperator() == BoxBinaryOperator.Contains ) {
			template = "Contains.invoke(${left},${right})";
		} else if ( operation.getOperator() == BoxBinaryOperator.NotContains ) {
			template = "!Contains.invoke(${left},${right})";
		} else if ( operation.getOperator() == BoxBinaryOperator.CastAs ) {
			template = "CastAs.invoke(${left},${right})";
		} else {
			throw new IllegalStateException( "not implemented" );
		}
		Node javaExpr = parseExpression( template, values );
		logger.debug( node.getSourceText() + " (" + context.name() + ") -> " + javaExpr );
		// addIndex( javaExpr, node );
		return javaExpr;
	}

}
