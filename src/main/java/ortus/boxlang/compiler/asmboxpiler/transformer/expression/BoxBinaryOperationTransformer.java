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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;

import ortus.boxlang.compiler.asmboxpiler.AsmHelper;
import ortus.boxlang.compiler.asmboxpiler.MethodContextTracker;
import ortus.boxlang.compiler.asmboxpiler.Transpiler;
import ortus.boxlang.compiler.asmboxpiler.transformer.AbstractTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.ReturnValueContext;
import ortus.boxlang.compiler.asmboxpiler.transformer.TransformerContext;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.expression.BoxBinaryOperation;
import ortus.boxlang.compiler.ast.expression.BoxBinaryOperator;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.operators.BitwiseAnd;
import ortus.boxlang.runtime.operators.BitwiseOr;
import ortus.boxlang.runtime.operators.BitwiseSignedLeftShift;
import ortus.boxlang.runtime.operators.BitwiseSignedRightShift;
import ortus.boxlang.runtime.operators.BitwiseUnsignedRightShift;
import ortus.boxlang.runtime.operators.BitwiseXor;
import ortus.boxlang.runtime.operators.CastAs;
import ortus.boxlang.runtime.operators.Contains;
import ortus.boxlang.runtime.operators.Divide;
import ortus.boxlang.runtime.operators.Elvis;
import ortus.boxlang.runtime.operators.Equivalence;
import ortus.boxlang.runtime.operators.Implies;
import ortus.boxlang.runtime.operators.InstanceOf;
import ortus.boxlang.runtime.operators.IntegerDivide;
import ortus.boxlang.runtime.operators.Minus;
import ortus.boxlang.runtime.operators.Modulus;
import ortus.boxlang.runtime.operators.Multiply;
import ortus.boxlang.runtime.operators.NotContains;
import ortus.boxlang.runtime.operators.Plus;
import ortus.boxlang.runtime.operators.Power;
import ortus.boxlang.runtime.operators.XOR;

public class BoxBinaryOperationTransformer extends AbstractTransformer {

	public BoxBinaryOperationTransformer( Transpiler transpiler ) {
		super( transpiler );
	}

	@Override
	public List<AbstractInsnNode> transform( BoxNode node, TransformerContext context, ReturnValueContext returnContext ) throws IllegalStateException {
		BoxBinaryOperation		operation	= ( BoxBinaryOperation ) node;
		TransformerContext		safe		= operation.getOperator() == BoxBinaryOperator.Elvis ? TransformerContext.SAFE : context;
		List<AbstractInsnNode>	left		= transpiler.transform( operation.getLeft(), safe, ReturnValueContext.VALUE );
		List<AbstractInsnNode>	right		= transpiler.transform( operation.getRight(), context, ReturnValueContext.VALUE );
		MethodContextTracker	tracker		= transpiler.getCurrentMethodContextTracker().get();

		List<AbstractInsnNode>	nodes		= switch ( operation.getOperator() ) {
												case Plus -> // "Plus.invoke(${left},${right})";
												    generateNumericBinaryMethodCallNodes( Plus.class, Number.class, operation, left, right );

												case Minus -> // "Minus.invoke(${left},${right})";
												    generateNumericBinaryMethodCallNodes( Minus.class, Number.class, operation, left, right );

												case Star -> // "Multiply.invoke(${left},${right})";
												    generateNumericBinaryMethodCallNodes( Multiply.class, Number.class, operation, left, right );

												case Slash -> // "Divide.invoke(${left},${right})";
												    generateNumericBinaryMethodCallNodes( Divide.class, Number.class, operation, left, right );

												case Backslash -> // "IntegerDivide.invoke(${left},${right})";
												    generateNumericBinaryMethodCallNodes( IntegerDivide.class, Number.class, operation, left, right );

												case Power -> // "Power.invoke(${left},${right})";
												    generateNumericBinaryMethodCallNodes( Power.class, Number.class, operation, left, right );

												case Xor -> // "XOR.invoke(${left},${right})";
												    generateBinaryMethodCallNodes( XOR.class, Boolean.class, left, right );

												case Mod -> // "Modulus.invoke(${left},${right})";
												    generateNumericBinaryMethodCallNodes( Modulus.class, Number.class, operation, left, right );

												case And -> {
													LabelNode				ifFalse		= new LabelNode(), end = new LabelNode();
													List<AbstractInsnNode>	expression	= new ArrayList<>();
													expression.addAll( left );
													if ( !operation.getLeft().returnsBoolean() ) {
														expression.add( new MethodInsnNode( Opcodes.INVOKESTATIC,
														    Type.getInternalName( BooleanCaster.class ),
														    "cast",
														    Type.getMethodDescriptor( Type.getType( Boolean.class ), Type.getType( Object.class ) ),
														    false ) );
													}
													expression.add( new MethodInsnNode( Opcodes.INVOKEVIRTUAL,
													    Type.getInternalName( Boolean.class ),
													    "booleanValue",
													    Type.getMethodDescriptor( Type.getType( boolean.class ) ),
													    false ) );
													expression.add( new JumpInsnNode( Opcodes.IFEQ, ifFalse ) );
													expression.addAll( right );
													if ( !operation.getRight().returnsBoolean() ) {
														expression.add( new MethodInsnNode( Opcodes.INVOKESTATIC,
														    Type.getInternalName( BooleanCaster.class ),
														    "cast",
														    Type.getMethodDescriptor( Type.getType( Boolean.class ), Type.getType( Object.class ) ),
														    false ) );
													}
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
													if ( !operation.getLeft().returnsBoolean() ) {
														expression.add( new MethodInsnNode( Opcodes.INVOKESTATIC,
														    Type.getInternalName( BooleanCaster.class ),
														    "cast",
														    Type.getMethodDescriptor( Type.getType( Boolean.class ), Type.getType( Object.class ) ),
														    false ) );
													}
													expression.add( new MethodInsnNode( Opcodes.INVOKEVIRTUAL,
													    Type.getInternalName( Boolean.class ),
													    "booleanValue",
													    Type.getMethodDescriptor( Type.getType( boolean.class ) ),
													    false ) );
													expression.add( new JumpInsnNode( Opcodes.IFNE, ifTrue ) );
													expression.addAll( right );
													if ( !operation.getRight().returnsBoolean() ) {
														expression.add( new MethodInsnNode( Opcodes.INVOKESTATIC,
														    Type.getInternalName( BooleanCaster.class ),
														    "cast",
														    Type.getMethodDescriptor( Type.getType( Boolean.class ), Type.getType( Object.class ) ),
														    false ) );
													}
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

												case Elvis -> {// "Elvis.invoke(${left},${right})";
													List<AbstractInsnNode> elvisNodes = new ArrayList<>();
													elvisNodes.addAll( tracker.loadCurrentContext() );
													elvisNodes.addAll( left );
													elvisNodes.addAll( AsmHelper.generateArgumentProducerLambda( transpiler,
													    () -> transpiler.transform( operation.getRight(), context, ReturnValueContext.VALUE ) ) );

													elvisNodes.add( new MethodInsnNode( Opcodes.INVOKESTATIC,
													    Type.getInternalName( Elvis.class ),
													    "invoke",
													    Type.getMethodDescriptor(
													        Type.getType( Object.class ),
													        Type.getType( IBoxContext.class ),
													        Type.getType( Object.class ),
													        Type.getType( Function.class )
													    ),
													    false ) );

													yield elvisNodes;
												}

												case InstanceOf -> // "InstanceOf.invoke(${contextName},${left},${right})";
												    generateBinaryMethodCallNodesWithContext( transpiler, InstanceOf.class, Boolean.class, left, right );

												case Contains -> // "Contains.invoke(${left},${right})";
												    generateBinaryMethodCallNodes( Contains.class, Boolean.class, left, right );

												case NotContains -> // "NotContains.invoke(${left},${right})";
												    generateBinaryMethodCallNodes( NotContains.class, Boolean.class, left, right );

												case CastAs -> // "CastAs.invoke(${contextName},${left},${right})";
												    generateBinaryMethodCallNodesWithContext( transpiler, CastAs.class, Object.class, left, right );

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

		if ( returnContext.empty ) {
			nodes.add( new InsnNode( Opcodes.POP ) );
		}

		return AsmHelper.addLineNumberLabels( nodes, node );
	}

	@NonNull
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

	/**
	 * Generate binary method call nodes for numeric operators, using the invoke(Number, Number) overload
	 * when both operands are known to return Number values to avoid unnecessary NumberCaster calls.
	 *
	 * @param dispatcher the operator class
	 * @param returned   the return type class
	 * @param operation  the BoxBinaryOperation AST node
	 * @param left       the left operand instructions
	 * @param right      the right operand instructions
	 *
	 * @return the instruction list
	 */
	@NonNull
	private static List<AbstractInsnNode> generateNumericBinaryMethodCallNodes( Class<?> dispatcher, Class<?> returned,
	    BoxBinaryOperation operation, List<AbstractInsnNode> left, List<AbstractInsnNode> right ) {
		if ( operation.getLeft().returnsNumber() && operation.getRight().returnsNumber() ) {
			List<AbstractInsnNode> nodes = new ArrayList<>();
			nodes.addAll( left );
			nodes.add( new TypeInsnNode( Opcodes.CHECKCAST, Type.getInternalName( Number.class ) ) );
			nodes.addAll( right );
			nodes.add( new TypeInsnNode( Opcodes.CHECKCAST, Type.getInternalName( Number.class ) ) );
			nodes.add( new MethodInsnNode( Opcodes.INVOKESTATIC,
			    Type.getInternalName( dispatcher ),
			    "invoke",
			    Type.getMethodDescriptor( Type.getType( returned ), Type.getType( Number.class ), Type.getType( Number.class ) ),
			    false ) );
			return nodes;
		}
		return generateBinaryMethodCallNodes( dispatcher, returned, left, right );
	}

	@NonNull
	private static List<AbstractInsnNode> generateBinaryMethodCallNodesWithContext( Transpiler transpiler, Class<?> dispatcher, Class<?> returned,
	    List<AbstractInsnNode> left,
	    List<AbstractInsnNode> right ) {
		List<AbstractInsnNode> nodes = new ArrayList<>();
		nodes.addAll( transpiler.getCurrentMethodContextTracker().get().loadCurrentContext() );
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
