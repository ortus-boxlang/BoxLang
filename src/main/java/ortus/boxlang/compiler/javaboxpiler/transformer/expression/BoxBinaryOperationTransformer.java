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

import javax.annotation.Nonnull;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.UnaryExpr;

import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.expression.BoxBinaryOperation;
import ortus.boxlang.compiler.ast.expression.BoxBinaryOperator;
import ortus.boxlang.compiler.javaboxpiler.JavaTranspiler;
import ortus.boxlang.compiler.javaboxpiler.transformer.AbstractTransformer;
import ortus.boxlang.compiler.javaboxpiler.transformer.TransformerContext;
import ortus.boxlang.runtime.types.exceptions.ExpressionException;

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

		Node				javaExpr	= switch ( operation.getOperator() ) {
											case Plus -> // "Plus.invoke(${left},${right})";
											    generateBinaryMethodCallExpr( "Plus", left, right );

											case Minus -> // "Minus.invoke(${left},${right})";
											    generateBinaryMethodCallExpr( "Minus", left, right );

											case Star -> // "Multiply.invoke(${left},${right})";
											    generateBinaryMethodCallExpr( "Multiply", left, right );

											case Slash -> // "Divide.invoke(${left},${right})";
											    generateBinaryMethodCallExpr( "Divide", left, right );

											case Backslash -> // "IntegerDivide.invoke(${left},${right})";
											    generateBinaryMethodCallExpr( "IntegerDivide", left, right );

											case Power -> // "Power.invoke(${left},${right})";
											    generateBinaryMethodCallExpr( "Power", left, right );

											case Xor -> // "XOR.invoke(${left},${right})";
											    generateBinaryMethodCallExpr( "XOR", left, right );

											case Mod -> // "Modulus.invoke(${left},${right})";
											    generateBinaryMethodCallExpr( "Modulus", left, right );

											case And -> {
												// "BooleanCaster.cast( ${left} ) && BooleanCaster.cast( ${right} )";
												BinaryExpr		binaryExpr		= new BinaryExpr();
												NameExpr		booleanNameExpr	= new NameExpr( "BooleanCaster" );

												MethodCallExpr	leftExpr		= new MethodCallExpr( booleanNameExpr, "cast" );
												leftExpr.addArgument( left );
												binaryExpr.setLeft( leftExpr );

												binaryExpr.setOperator( BinaryExpr.Operator.AND );

												MethodCallExpr rightExpr = new MethodCallExpr( booleanNameExpr, "cast" );
												rightExpr.addArgument( right );
												binaryExpr.setRight( rightExpr );

												yield binaryExpr;
											}
											case Or -> {
												// "BooleanCaster.cast( ${left} ) || BooleanCaster.cast( ${right} )";
												BinaryExpr		binaryExpr		= new BinaryExpr();
												NameExpr		booleanNameExpr	= new NameExpr( "BooleanCaster" );

												MethodCallExpr	leftExpr		= new MethodCallExpr( booleanNameExpr, "cast" );
												leftExpr.addArgument( left );
												binaryExpr.setLeft( leftExpr );

												binaryExpr.setOperator( BinaryExpr.Operator.OR );

												MethodCallExpr rightExpr = new MethodCallExpr( booleanNameExpr, "cast" );
												rightExpr.addArgument( right );
												binaryExpr.setRight( rightExpr );

												yield binaryExpr;
											}
											case Equivalence -> // "Equivalence.invoke(${left},${right})";
											    generateBinaryMethodCallExpr( "Equivalence", left, right );

											case Implies -> // "Implies.invoke(${left},${right})";
											    generateBinaryMethodCallExpr( "Implies", left, right );

											case Elvis -> // "Elvis.invoke(${left},${right})";
											    generateBinaryMethodCallExpr( "Elvis", left, right );

											case InstanceOf -> // "InstanceOf.invoke(${contextName},${left},${right})";
											    generateBinaryMethodCallExpr( "InstanceOf", transpiler.peekContextName(), left, right );

											case Contains -> // "Contains.invoke(${left},${right})";
											    generateBinaryMethodCallExpr( "Contains", left, right );

											case NotContains -> // "!Contains.invoke(${left},${right})";
											    new UnaryExpr( generateBinaryMethodCallExpr( "Contains", left, right ), UnaryExpr.Operator.LOGICAL_COMPLEMENT );

											case CastAs -> // "CastAs.invoke(${contextName},${left},${right})";
											    generateBinaryMethodCallExpr( "CastAs", transpiler.peekContextName(), left, right );

											case BitwiseAnd -> // "BitwiseAnd.invoke(${left},${right})";
											    generateBinaryMethodCallExpr( "BitwiseAnd", left, right );

											case BitwiseOr -> // "BitwiseOr.invoke(${left},${right})";
											    generateBinaryMethodCallExpr( "BitwiseOr", left, right );

											case BitwiseXor -> // "BitwiseXor.invoke(${left},${right})";
											    generateBinaryMethodCallExpr( "BitwiseXor", left, right );

											case BitwiseSignedLeftShift -> // "BitwiseSignedLeftShift.invoke(${left},${right})";
											    generateBinaryMethodCallExpr( "BitwiseSignedLeftShift", left, right );

											case BitwiseSignedRightShift -> // "BitwiseSignedRightShift.invoke(${left},${right})";
											    generateBinaryMethodCallExpr( "BitwiseSignedRightShift", left, right );

											case BitwiseUnsignedRightShift -> // "BitwiseUnsignedRightShift.invoke(${left},${right})";
											    generateBinaryMethodCallExpr( "BitwiseUnsignedRightShift", left, right );

											default -> throw new ExpressionException( "not implemented", operation );
										};
		// logger.trace( node.getSourceText() + " (" + context.name() + ") -> " + javaExpr );
		// addIndex( javaExpr, node );
		return javaExpr;
	}

	@Nonnull
	private static MethodCallExpr generateBinaryMethodCallExpr( String methodName, Object... args ) {
		NameExpr		nameExpr		= new NameExpr( methodName );
		MethodCallExpr	methodCallExpr	= new MethodCallExpr( nameExpr, "invoke" );
		for ( Object o : args ) {
			if ( o instanceof Expression expr ) {
				methodCallExpr.addArgument( expr );
			} else if ( o instanceof String s ) {
				methodCallExpr.addArgument( s );
			} else {
				String type = "null";
				if ( o != null ) {
					type = o.getClass().getName();
				}
				throw new IllegalStateException( "Invalid argument type: " + type );
			}
		}
		return methodCallExpr;
	}

}
