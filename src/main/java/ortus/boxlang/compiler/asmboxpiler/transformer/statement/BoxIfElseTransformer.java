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
package ortus.boxlang.compiler.asmboxpiler.transformer.statement;

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;

import ortus.boxlang.compiler.asmboxpiler.AsmHelper;
import ortus.boxlang.compiler.asmboxpiler.Transpiler;
import ortus.boxlang.compiler.asmboxpiler.transformer.AbstractTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.ReturnValueContext;
import ortus.boxlang.compiler.asmboxpiler.transformer.TransformerContext;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.BoxStatement;
import ortus.boxlang.compiler.ast.statement.BoxIfElse;
import ortus.boxlang.runtime.components.Component;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;

public class BoxIfElseTransformer extends AbstractTransformer {

	private static final int	OUTLINE_SOURCE_THRESHOLD		= 12000;
	private static final int	OUTLINE_DESCENDANT_THRESHOLD	= 250;

	public BoxIfElseTransformer( Transpiler transpiler ) {
		super( transpiler );
	}

	@Override
	public List<AbstractInsnNode> transform( BoxNode node, TransformerContext context, ReturnValueContext returnContext ) {
		BoxIfElse				ifElse	= ( BoxIfElse ) node;

		List<AbstractInsnNode>	nodes	= new ArrayList<>();
		AsmHelper.addDebugLabel( nodes, "BoxIf" );
		nodes.addAll( transpiler.transform( ifElse.getCondition(), TransformerContext.NONE, ReturnValueContext.VALUE ) );
		if ( !ifElse.getCondition().returnsBoolean() ) {
			nodes.add( new MethodInsnNode( Opcodes.INVOKESTATIC,
			    Type.getInternalName( BooleanCaster.class ),
			    "cast",
			    Type.getMethodDescriptor( Type.getType( Boolean.class ), Type.getType( Object.class ) ),
			    false ) );
		}
		nodes.add( new MethodInsnNode( Opcodes.INVOKEVIRTUAL,
		    Type.getInternalName( Boolean.class ),
		    "booleanValue",
		    Type.getMethodDescriptor( Type.BOOLEAN_TYPE ),
		    false ) );
		LabelNode ifLabel = new LabelNode();
		AsmHelper.addDebugLabel( nodes, "BoxIfElse - goto iflabel" );
		nodes.add( new JumpInsnNode( Opcodes.IFEQ, ifLabel ) );
		nodes.addAll( transformBranch( ifElse.getThenBody(), true, returnContext ) );

		LabelNode elseLabel = new LabelNode();
		AsmHelper.addDebugLabel( nodes, "BoxIfElse - goto elselabel" );
		nodes.add( new JumpInsnNode( Opcodes.GOTO, elseLabel ) );

		AsmHelper.addDebugLabel( nodes, "BoxIfElse - ifLabel" );
		nodes.add( ifLabel );

		if ( ifElse.getElseBody() != null ) {
			nodes.addAll( transformBranch( ifElse.getElseBody(), false, returnContext ) );
		} else if ( returnContext == ReturnValueContext.VALUE_OR_NULL ) {
			nodes.add( new InsnNode( Opcodes.ACONST_NULL ) );
		}

		AsmHelper.addDebugLabel( nodes, "BoxIfElse - elseLabel" );
		nodes.add( elseLabel );

		AsmHelper.addDebugLabel( nodes, "BoxIfElse - end" );

		return AsmHelper.addLineNumberLabels( nodes, node );

	}

	private List<AbstractInsnNode> transformBranch(
	    BoxStatement branchBody,
	    boolean isThenBranch,
	    ReturnValueContext returnContext ) {
		if ( branchBody == null ) {
			return List.of();
		}

		if ( shouldOutlineBranch( branchBody, returnContext ) ) {
			return generateOutlinedComponentBranchCall( branchBody, isThenBranch );
		}

		return transpiler.transform( branchBody, TransformerContext.NONE, returnContext );
	}

	private boolean shouldOutlineBranch( BoxStatement branchBody, ReturnValueContext returnContext ) {
		if ( returnContext != ReturnValueContext.EMPTY || !transpiler.isInsideComponent() ) {
			return false;
		}

		return getNodeSourceLength( branchBody ) >= OUTLINE_SOURCE_THRESHOLD
		    || branchBody.getDescendants().size() >= OUTLINE_DESCENDANT_THRESHOLD;
	}

	private int getNodeSourceLength( BoxStatement branchBody ) {
		return branchBody.getSourceText() != null ? branchBody.getSourceText().length() : 0;
	}

	private List<AbstractInsnNode> generateOutlinedComponentBranchCall( BoxStatement branchBody, boolean isThenBranch ) {
		ClassNode	owningClass		= transpiler.getOwningClass();
		String		methodName		= "componentIfBranch_" + ( isThenBranch ? "then" : "else" ) + "_" + transpiler.incrementAndGetLambdaCounter();
		Type		declaringType	= Type.getObjectType( owningClass.name );

		AsmHelper.methodWithContextAndClassLocator(
		    owningClass,
		    methodName,
		    Type.getType( IBoxContext.class ),
		    Type.getType( Component.BodyResult.class ),
		    true,
		    transpiler,
		    false,
		    () -> createOutlinedComponentBranchBody( branchBody )
		);

		List<AbstractInsnNode>	nodes			= new ArrayList<>();
		LabelNode				continueLabel	= new LabelNode();

		nodes.addAll( transpiler.getCurrentMethodContextTracker().orElseThrow().loadCurrentContext() );
		nodes.add( new MethodInsnNode(
		    Opcodes.INVOKESTATIC,
		    declaringType.getInternalName(),
		    methodName,
		    Type.getMethodDescriptor( Type.getType( Component.BodyResult.class ), Type.getType( IBoxContext.class ) ),
		    false
		) );
		nodes.add( new InsnNode( Opcodes.DUP ) );
		nodes.add( new MethodInsnNode(
		    Opcodes.INVOKEVIRTUAL,
		    Type.getInternalName( Component.BodyResult.class ),
		    "isEarlyExit",
		    Type.getMethodDescriptor( Type.BOOLEAN_TYPE ),
		    false
		) );
		nodes.add( new JumpInsnNode( Opcodes.IFEQ, continueLabel ) );
		nodes.add( new InsnNode( Opcodes.ARETURN ) );
		nodes.add( continueLabel );
		nodes.add( new InsnNode( Opcodes.POP ) );

		return nodes;
	}

	private List<AbstractInsnNode> createOutlinedComponentBranchBody( BoxStatement branchBody ) {
		List<AbstractInsnNode> nodes = new ArrayList<>( transpiler.transform( branchBody, TransformerContext.NONE, ReturnValueContext.EMPTY ) );

		nodes.add( new FieldInsnNode(
		    Opcodes.GETSTATIC,
		    Type.getInternalName( Component.class ),
		    "DEFAULT_RETURN",
		    Type.getDescriptor( Component.BodyResult.class )
		) );

		return nodes;
	}

}
