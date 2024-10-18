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
import java.util.Optional;
import java.util.function.Supplier;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import ortus.boxlang.compiler.asmboxpiler.AsmHelper;
import ortus.boxlang.compiler.asmboxpiler.MethodContextTracker;
import ortus.boxlang.compiler.asmboxpiler.Transpiler;
import ortus.boxlang.compiler.asmboxpiler.transformer.AbstractTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.ReturnValueContext;
import ortus.boxlang.compiler.asmboxpiler.transformer.TransformerContext;
import ortus.boxlang.compiler.ast.BoxExpression;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.BoxStatement;
import ortus.boxlang.compiler.ast.statement.BoxTry;
import ortus.boxlang.compiler.ast.statement.BoxTryCatch;
import ortus.boxlang.runtime.context.CatchBoxContext;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.exceptions.ExceptionUtil;

public class BoxTryTransformer extends AbstractTransformer {

	public BoxTryTransformer( Transpiler transpiler ) {
		super( transpiler );
	}

	@Override
	public List<AbstractInsnNode> transform( BoxNode node, TransformerContext context, ReturnValueContext returnValueContext ) {
		Optional<MethodContextTracker> trackerOption = transpiler.getCurrentMethodContextTracker();
		if ( trackerOption.isEmpty() ) {
			throw new IllegalStateException();
		}
		MethodContextTracker	tracker				= trackerOption.get();
		List<AbstractInsnNode>	nodes				= new ArrayList<>();
		BoxTry					boxTry				= ( BoxTry ) node;

		LabelNode				tryStartLabel		= new LabelNode();
		LabelNode				tryEndLabel			= new LabelNode();
		LabelNode				finallyStartLabel	= new LabelNode();
		LabelNode				finallyEndLabel		= new LabelNode();

		nodes.add( tryStartLabel );

		nodes.addAll( generateBodyNodesWithInlinedFinally( context, returnValueContext, boxTry.getTryBody(), boxTry.getFinallyBody(), () -> tryEndLabel ) );

		// if we hit this instruction we have successfully executed the try body and inlined finally code
		// we can skip to the end of this construct
		nodes.add( new JumpInsnNode( Opcodes.GOTO, finallyEndLabel ) );

		if ( boxTry.getCatches().size() > 0 ) {

			LabelNode javaCatchBodyStart = new LabelNode();
			nodes.add( javaCatchBodyStart );

			var eVar = tracker.storeNewVariable( Opcodes.ASTORE );
			nodes.addAll( eVar.nodes() );

			for ( BoxTryCatch catchNode : boxTry.getCatches() ) {
				nodes.addAll(
				    generateCatchBodyNodes( context, returnValueContext, tracker, boxTry, catchNode, finallyStartLabel, finallyEndLabel, eVar.index() )
				);
			}

			TryCatchBlockNode catchHandler = new TryCatchBlockNode( tryStartLabel, tryEndLabel, javaCatchBodyStart,
			    null );
			tracker.addTryCatchBlock( catchHandler );

			// if we are here none of our catch handlers matched the error so we inline another finally block
			if ( boxTry.getFinallyBody().size() == 0 ) {
				nodes.add( new InsnNode( Opcodes.ACONST_NULL ) );
				if ( returnValueContext != ReturnValueContext.VALUE_OR_NULL ) {
					nodes.add( new InsnNode( Opcodes.POP ) );
				}
			}
			nodes.addAll( AsmHelper.transformBodyExpressions( transpiler, boxTry.getFinallyBody(), context, returnValueContext ) );
			nodes.add( new JumpInsnNode( Opcodes.GOTO, finallyEndLabel ) );
		}

		TryCatchBlockNode catchHandler = new TryCatchBlockNode( tryStartLabel, tryEndLabel, finallyStartLabel,
		    null );
		tracker.addTryCatchBlock( catchHandler );

		nodes.add( finallyStartLabel );

		var errorVarStore = tracker.storeNewVariable( Opcodes.ASTORE );
		nodes.addAll( errorVarStore.nodes() );

		nodes.addAll( AsmHelper.transformBodyExpressions( transpiler, boxTry.getFinallyBody(), context, returnValueContext ) );

		nodes.add( new VarInsnNode( Opcodes.ALOAD, errorVarStore.index() ) );

		nodes.add( new InsnNode( Opcodes.ATHROW ) );

		nodes.add( finallyEndLabel );

		tracker.addTryCatchBlock( new TryCatchBlockNode( tryStartLabel, tryEndLabel, finallyStartLabel, null ) );

		return nodes;

	}

	private List<AbstractInsnNode> generateBodyNodesWithInlinedFinally(
	    TransformerContext context,
	    ReturnValueContext returnValueContext,
	    List<BoxStatement> codeBody,
	    List<BoxStatement> finallyBody,
	    Supplier<AbstractInsnNode> inBetween ) {
		List<AbstractInsnNode> nodes = new ArrayList<AbstractInsnNode>();

		if ( codeBody.size() == 0 && finallyBody.size() == 0 ) {
			nodes.add( new InsnNode( Opcodes.ACONST_NULL ) );
			if ( returnValueContext != ReturnValueContext.VALUE_OR_NULL ) {
				nodes.add( new InsnNode( Opcodes.POP ) );
			}

			AbstractInsnNode inBetweenNode = inBetween.get();

			if ( inBetweenNode != null ) {
				nodes.add( inBetweenNode );
			}

			return nodes;
		}

		nodes.addAll( AsmHelper.transformBodyExpressions(
		    transpiler,
		    codeBody,
		    context,
		    finallyBody.size() > 0 ? ReturnValueContext.EMPTY : returnValueContext
		) );

		AbstractInsnNode inBetweenNode = inBetween.get();

		if ( inBetweenNode != null ) {
			nodes.add( inBetweenNode );
		}

		nodes.addAll( AsmHelper.transformBodyExpressions(
		    transpiler,
		    finallyBody,
		    context,
		    returnValueContext
		) );

		return nodes;
	}

	private List<AbstractInsnNode> generateCatchBodyNodes(
	    TransformerContext context,
	    ReturnValueContext returnValueContext,
	    MethodContextTracker tracker,
	    BoxTry boxTry,
	    BoxTryCatch boxCatch,
	    LabelNode finallyStartLabel,
	    LabelNode finallyEndLabel,
	    int eVarIndex ) {
		List<AbstractInsnNode>	nodes				= new ArrayList<AbstractInsnNode>();

		LabelNode				startHandlerLabel	= new LabelNode();
		LabelNode				endHandlerLabel		= new LabelNode();

		nodes.addAll( generateCatchIfGuard( context, boxCatch.getCatchTypes(), tracker, startHandlerLabel, endHandlerLabel, eVarIndex ) );

		nodes.add( startHandlerLabel );

		nodes.add( new TypeInsnNode( Opcodes.NEW, Type.getInternalName( CatchBoxContext.class ) ) );

		nodes.add( new InsnNode( Opcodes.DUP ) );

		nodes.addAll( tracker.loadCurrentContext() );

		nodes.addAll( transpiler.createKey( boxCatch.getException().getName() ) );

		nodes.add( new VarInsnNode( Opcodes.ALOAD, eVarIndex ) );

		nodes.add( new MethodInsnNode( Opcodes.INVOKESPECIAL,
		    Type.getInternalName( CatchBoxContext.class ),
		    "<init>",
		    Type.getMethodDescriptor(
		        Type.VOID_TYPE,
		        Type.getType( IBoxContext.class ),
		        Type.getType( Key.class ),
		        Type.getType( Throwable.class ) ),
		    false ) );
		nodes.addAll( tracker.trackNewContext() );
		// end catch context

		nodes.addAll( generateBodyNodesWithInlinedFinally( context, returnValueContext, boxCatch.getCatchBody(), boxTry.getFinallyBody(), () -> {
			tracker.popContext();
			return null;
		} ) );

		nodes.add( new JumpInsnNode( Opcodes.GOTO, finallyEndLabel ) );
		nodes.add( endHandlerLabel );

		tracker.addTryCatchBlock( new TryCatchBlockNode( startHandlerLabel, endHandlerLabel, finallyStartLabel, null ) );

		return nodes;
	}

	private List<AbstractInsnNode> generateCatchIfGuard( TransformerContext context, List<BoxExpression> catchTypes, MethodContextTracker tracker,
	    LabelNode startHandlerLabel, LabelNode endHandlerLabel, int eVarIndex ) {
		List<AbstractInsnNode> nodes = new ArrayList<AbstractInsnNode>();

		if ( catchTypes.size() == 0 ) {
			return new ArrayList<AbstractInsnNode>();
		}

		for ( int i = 0; i < catchTypes.size() - 1; i++ ) {

			nodes.add( new VarInsnNode( Opcodes.ALOAD, eVarIndex ) );

			nodes.addAll( tracker.loadCurrentContext() );

			nodes.add( new InsnNode( Opcodes.SWAP ) );

			nodes.addAll( transpiler.transform( catchTypes.get( i ), context, ReturnValueContext.VALUE ) );

			nodes.add( new MethodInsnNode(
			    Opcodes.INVOKESTATIC,
			    Type.getInternalName( ExceptionUtil.class ),
			    "exceptionIsOfType",
			    Type.getMethodDescriptor( Type.getType( Boolean.class ), Type.getType( IBoxContext.class ), Type.getType( Throwable.class ),
			        Type.getType( String.class ) ),
			    false
			) );

			nodes.add( new MethodInsnNode(
			    Opcodes.INVOKEVIRTUAL,
			    Type.getInternalName( Boolean.class ),
			    "booleanValue",
			    Type.getMethodDescriptor( Type.BOOLEAN_TYPE ),
			    false
			) );

			nodes.add( new JumpInsnNode( Opcodes.IFNE, startHandlerLabel ) );
		}

		nodes.add( new VarInsnNode( Opcodes.ALOAD, eVarIndex ) );

		nodes.addAll( tracker.loadCurrentContext() );

		nodes.add( new InsnNode( Opcodes.SWAP ) );

		nodes.addAll( transpiler.transform( catchTypes.getLast(), context, ReturnValueContext.VALUE ) );

		nodes.add( new MethodInsnNode(
		    Opcodes.INVOKESTATIC,
		    Type.getInternalName( ExceptionUtil.class ),
		    "exceptionIsOfType",
		    Type.getMethodDescriptor( Type.getType( Boolean.class ), Type.getType( IBoxContext.class ), Type.getType( Throwable.class ),
		        Type.getType( String.class ) ),
		    false
		) );

		nodes.add( new MethodInsnNode(
		    Opcodes.INVOKEVIRTUAL,
		    Type.getInternalName( Boolean.class ),
		    "booleanValue",
		    Type.getMethodDescriptor( Type.BOOLEAN_TYPE ),
		    false
		) );

		nodes.add( new JumpInsnNode( Opcodes.IFEQ, endHandlerLabel ) );

		return nodes;
	}

}