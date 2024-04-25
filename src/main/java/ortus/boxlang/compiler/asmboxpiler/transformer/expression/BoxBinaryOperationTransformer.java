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
package ortus.boxlang.compiler.asmboxpiler.transformer.expression;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import ortus.boxlang.compiler.asmboxpiler.Transpiler;
import ortus.boxlang.compiler.asmboxpiler.transformer.AbstractTransformer;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.expression.BoxBinaryOperation;
import ortus.boxlang.runtime.operators.*;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class BoxBinaryOperationTransformer extends AbstractTransformer {

	public BoxBinaryOperationTransformer(Transpiler transpiler ) {
		super( transpiler );
	}

	@Override
	public List<AbstractInsnNode> transform(BoxNode node ) throws IllegalStateException {
		BoxBinaryOperation	operation	= ( BoxBinaryOperation ) node;
		List<AbstractInsnNode>			left		= transpiler.transform( operation.getLeft() );
		List<AbstractInsnNode>			right		= transpiler.transform( operation.getRight() );

		return switch ( operation.getOperator() ) {
			case Plus -> // "Plus.invoke(${left},${right})";
				generateBinaryMethodCallNodes( Plus.class, left, right );

			case Minus -> // "Minus.invoke(${left},${right})";
				generateBinaryMethodCallNodes( Minus.class, left, right );

			case Star -> // "Multiply.invoke(${left},${right})";
				generateBinaryMethodCallNodes( Multiply.class, left, right );

			case Slash -> // "Divide.invoke(${left},${right})";
				generateBinaryMethodCallNodes( Divide.class, left, right );

			case Backslash -> // "IntegerDivide.invoke(${left},${right})";
				generateBinaryMethodCallNodes( IntegerDivide.class, left, right );

			case Power -> // "Power.invoke(${left},${right})";
				generateBinaryMethodCallNodes( Power.class, left, right );

			case Xor -> // "XOR.invoke(${left},${right})";
				generateBinaryMethodCallNodes( XOR.class, left, right );

			case Mod -> // "Modulus.invoke(${left},${right})";
				generateBinaryMethodCallNodes( Modulus.class, left, right );

			case And -> {
				// "BooleanCaster.cast( ${left} ) && BooleanCaster.cast( ${right} )";
//				BinaryExpr		binaryExpr		= new BinaryExpr();
//				NameExpr		booleanNameExpr	= new NameExpr( BooleanCaster.class );
//
//				MethodCallExpr	leftExpr		= new MethodCallExpr( booleanNameExpr, "cast" );
//				leftExpr.addArgument( left );
//				binaryExpr.setLeft( leftExpr );
//
//				binaryExpr.setOperator( BinaryExpr.Operator.AND );
//
//				MethodCallExpr rightExpr = new MethodCallExpr( booleanNameExpr, "cast" );
//				rightExpr.addArgument( right );
//				binaryExpr.setRight( rightExpr );
//
//				yield binaryExpr;
				throw new UnsupportedOperationException();
			}
			case Or -> {
				// "BooleanCaster.cast( ${left} ) || BooleanCaster.cast( ${right} )";
//				BinaryExpr		binaryExpr		= new BinaryExpr();
//				NameExpr		booleanNameExpr	= new NameExpr( BooleanCaster.class );
//
//				MethodCallExpr	leftExpr		= new MethodCallExpr( booleanNameExpr, "cast" );
//				leftExpr.addArgument( left );
//				binaryExpr.setLeft( leftExpr );
//
//				binaryExpr.setOperator( BinaryExpr.Operator.OR );
//
//				MethodCallExpr rightExpr = new MethodCallExpr( booleanNameExpr, "cast" );
//				rightExpr.addArgument( right );
//				binaryExpr.setRight( rightExpr );
//
//				yield binaryExpr;
				throw new UnsupportedOperationException();
			}
			case Equivalence -> // "Equivalence.invoke(${left},${right})";
				generateBinaryMethodCallNodes( Equivalence.class, left, right );

			case Implies -> // "Implies.invoke(${left},${right})";
				generateBinaryMethodCallNodes( Implies.class, left, right );

			case Elvis -> // "Elvis.invoke(${left},${right})";
				generateBinaryMethodCallNodes( Elvis.class, left, right );

			case InstanceOf -> // "InstanceOf.invoke(${contextName},${left},${right})";
//				generateBinaryMethodCallNodes( InstanceOf.class, transpiler.peekContextName(), left, right );
				throw new UnsupportedOperationException();

			case Contains -> // "Contains.invoke(${left},${right})";
				generateBinaryMethodCallNodes( Contains.class, left, right );

			case NotContains -> // "!Contains.invoke(${left},${right})";
//				new UnaryExpr( generateBinaryMethodCallNodes( Contains.class, left, right ), UnaryExpr.Operator.LOGICAL_COMPLEMENT );
				throw new UnsupportedOperationException();
			case CastAs -> // "CastAs.invoke(${contextName},${left},${right})";
//				generateBinaryMethodCallNodes( CastAs.class, transpiler.peekContextName(), left, right );
				throw new UnsupportedOperationException();

			case BitwiseAnd -> // "BitwiseAnd.invoke(${left},${right})";
				generateBinaryMethodCallNodes( BitwiseAnd.class, left, right );

			case BitwiseOr -> // "BitwiseOr.invoke(${left},${right})";
				generateBinaryMethodCallNodes( BitwiseOr.class, left, right );

			case BitwiseXor -> // "BitwiseXor.invoke(${left},${right})";
				generateBinaryMethodCallNodes( BitwiseXor.class, left, right );

			case BitwiseSignedLeftShift -> // "BitwiseSignedLeftShift.invoke(${left},${right})";
				generateBinaryMethodCallNodes( BitwiseSignedLeftShift.class, left, right );

			case BitwiseSignedRightShift -> // "BitwiseSignedRightShift.invoke(${left},${right})";
				generateBinaryMethodCallNodes( BitwiseSignedRightShift.class, left, right );

			case BitwiseUnsignedRightShift -> // "BitwiseUnsignedRightShift.invoke(${left},${right})";
				generateBinaryMethodCallNodes( BitwiseUnsignedRightShift.class, left, right );

			default -> throw new IllegalStateException( "not implemented" );
		};
	}

	@Nonnull
	private static List<AbstractInsnNode> generateBinaryMethodCallNodes(Class<?> dispatcher, List<AbstractInsnNode> left, List<AbstractInsnNode> right ) {
		List<AbstractInsnNode> nodes = new ArrayList<>();
		nodes.addAll(left);
		nodes.addAll(right);
		nodes.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
			Type.getInternalName(dispatcher),
			"invoke",
			Type.getMethodDescriptor(Type.getType(Double.class), Type.getType(Object.class), Type.getType(Object.class)),
			false));
		return nodes;
	}

}
