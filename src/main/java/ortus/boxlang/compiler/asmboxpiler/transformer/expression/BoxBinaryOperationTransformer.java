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
import org.objectweb.asm.tree.*;
import ortus.boxlang.compiler.asmboxpiler.Transpiler;
import ortus.boxlang.compiler.asmboxpiler.transformer.AbstractTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.TransformerContext;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.expression.BoxBinaryOperation;
import ortus.boxlang.compiler.ast.expression.BoxBinaryOperator;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.operators.*;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class BoxBinaryOperationTransformer extends AbstractTransformer {

	public BoxBinaryOperationTransformer( Transpiler transpiler ) {
		super( transpiler );
	}

	@Override
	public List<AbstractInsnNode> transform( BoxNode node, TransformerContext context ) throws IllegalStateException {
		BoxBinaryOperation		operation	= ( BoxBinaryOperation ) node;
		TransformerContext		safe		= operation.getOperator() == BoxBinaryOperator.Elvis ? TransformerContext.SAFE : context;
		List<AbstractInsnNode>	left		= transpiler.transform( operation.getLeft(), safe );
		List<AbstractInsnNode>	right		= transpiler.transform( operation.getRight(), context );

		return switch ( operation.getOperator() ) {
			case Plus -> // "Plus.invoke(${left},${right})";
			    generateBinaryMethodCallNodes( Plus.class, Double.class, left, right );

			case Minus -> // "Minus.invoke(${left},${right})";
			    generateBinaryMethodCallNodes( Minus.class, Double.class, left, right );

			case Star -> // "Multiply.invoke(${left},${right})";
			    generateBinaryMethodCallNodes( Multiply.class, Double.class, left, right );

			case Slash -> // "Divide.invoke(${left},${right})";
			    generateBinaryMethodCallNodes( Divide.class, Double.class, left, right );

			case Backslash -> // "IntegerDivide.invoke(${left},${right})";
			    generateBinaryMethodCallNodes( IntegerDivide.class, Double.class, left, right );

			case Power -> // "Power.invoke(${left},${right})";
			    generateBinaryMethodCallNodes( Power.class, Double.class, left, right );

			case Xor -> // "XOR.invoke(${left},${right})";
			    generateBinaryMethodCallNodes( XOR.class, Boolean.class, left, right );

			case Mod -> // "Modulus.invoke(${left},${right})";
			    generateBinaryMethodCallNodes( Modulus.class, Double.class, left, right );

			case And -> {
				LabelNode				ifFalse		= new LabelNode(), end = new LabelNode();
				List<AbstractInsnNode>	expression	= new ArrayList<>();
				expression.addAll( left );
				expression.add( new MethodInsnNode( Opcodes.INVOKESTATIC,
				    Type.getInternalName( BooleanCaster.class ),
				    "cast",
				    Type.getMethodDescriptor( Type.getType( Boolean.class ), Type.getType( Object.class ) ),
				    false ) );
				expression.add( new MethodInsnNode( Opcodes.INVOKEVIRTUAL,
				    Type.getInternalName( Boolean.class ),
				    "booleanValue",
				    Type.getMethodDescriptor( Type.getType( boolean.class ) ),
				    false ) );
				expression.add( new JumpInsnNode( Opcodes.IFEQ, ifFalse ) );
				expression.addAll( right );
				expression.add( new MethodInsnNode( Opcodes.INVOKESTATIC,
				    Type.getInternalName( BooleanCaster.class ),
				    "cast",
				    Type.getMethodDescriptor( Type.getType( Boolean.class ), Type.getType( Object.class ) ),
				    false ) );
				expression.add( new MethodInsnNode( Opcodes.INVOKEVIRTUAL,
				    Type.getInternalName( Boolean.class ),
				    "booleanValue",
				    Type.getMethodDescriptor( Type.getType( boolean.class ) ),
				    false ) );
				expression.add( new JumpInsnNode( Opcodes.IFEQ, ifFalse ) );
				expression.add( new FieldInsnNode( Opcodes.GETSTATIC,
				    Type.getInternalName( Boolean.class ),
				    "TRUE",
				    Type.getDescriptor( Boolean.class ) ) );
				expression.add( new JumpInsnNode( Opcodes.GOTO, end ) );
				expression.add( ifFalse );
				expression.add( new FieldInsnNode( Opcodes.GETSTATIC,
				    Type.getInternalName( Boolean.class ),
				    "FALSE",
				    Type.getDescriptor( Boolean.class ) ) );
				expression.add( end );
				yield expression;
			}
			case Or -> {
				LabelNode				ifTrue		= new LabelNode(), end = new LabelNode();
				List<AbstractInsnNode>	expression	= new ArrayList<>();
				expression.addAll( left );
				expression.add( new MethodInsnNode( Opcodes.INVOKESTATIC,
				    Type.getInternalName( BooleanCaster.class ),
				    "cast",
				    Type.getMethodDescriptor( Type.getType( Boolean.class ), Type.getType( Object.class ) ),
				    false ) );
				expression.add( new MethodInsnNode( Opcodes.INVOKEVIRTUAL,
				    Type.getInternalName( Boolean.class ),
				    "booleanValue",
				    Type.getMethodDescriptor( Type.getType( boolean.class ) ),
				    false ) );
				expression.add( new JumpInsnNode( Opcodes.IFNE, ifTrue ) );
				expression.addAll( right );
				expression.add( new MethodInsnNode( Opcodes.INVOKESTATIC,
				    Type.getInternalName( BooleanCaster.class ),
				    "cast",
				    Type.getMethodDescriptor( Type.getType( Boolean.class ), Type.getType( Object.class ) ),
				    false ) );
				expression.add( new MethodInsnNode( Opcodes.INVOKEVIRTUAL,
				    Type.getInternalName( Boolean.class ),
				    "booleanValue",
				    Type.getMethodDescriptor( Type.getType( boolean.class ) ),
				    false ) );
				expression.add( new JumpInsnNode( Opcodes.IFNE, ifTrue ) );
				expression.add( new FieldInsnNode( Opcodes.GETSTATIC,
				    Type.getInternalName( Boolean.class ),
				    "FALSE",
				    Type.getDescriptor( Boolean.class ) ) );
				expression.add( new JumpInsnNode( Opcodes.GOTO, end ) );
				expression.add( ifTrue );
				expression.add( new FieldInsnNode( Opcodes.GETSTATIC,
				    Type.getInternalName( Boolean.class ),
				    "TRUE",
				    Type.getDescriptor( Boolean.class ) ) );
				expression.add( end );
				yield expression;
			}
			case Equivalence -> // "Equivalence.invoke(${left},${right})";
			    generateBinaryMethodCallNodes( Equivalence.class, Object.class, left, right );

			case Implies -> // "Implies.invoke(${left},${right})";
			    generateBinaryMethodCallNodes( Implies.class, Object.class, left, right );

			case Elvis -> // "Elvis.invoke(${left},${right})";
			    generateBinaryMethodCallNodes( Elvis.class, Object.class, left, right );

			case InstanceOf -> // "InstanceOf.invoke(${contextName},${left},${right})";
			    generateBinaryMethodCallNodesWithContext( InstanceOf.class, Boolean.class, left, right );

			case Contains -> // "Contains.invoke(${left},${right})";
			    generateBinaryMethodCallNodes( Contains.class, Boolean.class, left, right );

			case NotContains -> // "NotContains.invoke(${left},${right})";
			    generateBinaryMethodCallNodes( NotContains.class, Boolean.class, left, right );

			case CastAs -> // "CastAs.invoke(${contextName},${left},${right})";
			    generateBinaryMethodCallNodesWithContext( CastAs.class, Object.class, left, right );

			case BitwiseAnd -> // "BitwiseAnd.invoke(${left},${right})";
			    generateBinaryMethodCallNodes( BitwiseAnd.class, Number.class, left, right );

			case BitwiseOr -> // "BitwiseOr.invoke(${left},${right})";
			    generateBinaryMethodCallNodes( BitwiseOr.class, Number.class, left, right );

			case BitwiseXor -> // "BitwiseXor.invoke(${left},${right})";
			    generateBinaryMethodCallNodes( BitwiseXor.class, Number.class, left, right );

			case BitwiseSignedLeftShift -> // "BitwiseSignedLeftShift.invoke(${left},${right})";
			    generateBinaryMethodCallNodes( BitwiseSignedLeftShift.class, Number.class, left, right );

			case BitwiseSignedRightShift -> // "BitwiseSignedRightShift.invoke(${left},${right})";
			    generateBinaryMethodCallNodes( BitwiseSignedRightShift.class, Number.class, left, right );

			case BitwiseUnsignedRightShift -> // "BitwiseUnsignedRightShift.invoke(${left},${right})";
			    generateBinaryMethodCallNodes( BitwiseUnsignedRightShift.class, Number.class, left, right );

			default -> throw new IllegalStateException( "not implemented" );
		};
	}

	@Nonnull
	private static List<AbstractInsnNode> generateBinaryMethodCallNodes( Class<?> dispatcher, Class<?> returned, List<AbstractInsnNode> left,
	    List<AbstractInsnNode> right ) {
		List<AbstractInsnNode> nodes = new ArrayList<>();
		nodes.addAll( left );
		nodes.addAll( right );
		nodes.add( new MethodInsnNode( Opcodes.INVOKESTATIC,
		    Type.getInternalName( dispatcher ),
		    "invoke",
		    Type.getMethodDescriptor( Type.getType( returned ), Type.getType( Object.class ), Type.getType( Object.class ) ),
		    false ) );
		return nodes;
	}

	@Nonnull
	private static List<AbstractInsnNode> generateBinaryMethodCallNodesWithContext( Class<?> dispatcher, Class<?> returned, List<AbstractInsnNode> left,
	    List<AbstractInsnNode> right ) {
		List<AbstractInsnNode> nodes = new ArrayList<>();
		nodes.add( new VarInsnNode( Opcodes.ALOAD, 1 ) );
		nodes.addAll( left );
		nodes.addAll( right );
		nodes.add( new MethodInsnNode( Opcodes.INVOKESTATIC,
		    Type.getInternalName( dispatcher ),
		    "invoke",
		    Type.getMethodDescriptor( Type.getType( returned ), Type.getType( IBoxContext.class ), Type.getType( Object.class ), Type.getType( Object.class ) ),
		    false ) );
		return nodes;
	}

}
