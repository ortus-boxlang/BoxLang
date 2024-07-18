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

import ortus.boxlang.compiler.asmboxpiler.MethodContextTracker;
import ortus.boxlang.compiler.asmboxpiler.Transpiler;
import ortus.boxlang.compiler.asmboxpiler.transformer.AbstractTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.ReturnValueContext;
import ortus.boxlang.compiler.asmboxpiler.transformer.TransformerContext;
import ortus.boxlang.compiler.ast.BoxExpression;
import ortus.boxlang.compiler.ast.BoxNode;
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

		// nodes.addAll( tracker.popAllStackEntries() );

		nodes.add( tryStartLabel );

		nodes.add( new InsnNode( Opcodes.NOP ) );

		nodes.addAll(
		    boxTry.getTryBody()
		        .stream()
		        .map( ( boxNode ) -> transpiler.transform( boxNode, context, ReturnValueContext.EMPTY )
		        ).flatMap( ( nodeList ) -> nodeList.stream() )
		        .toList()
		);

		// nodes.addAll( tracker.popAllStackEntries() );

		nodes.add( tryEndLabel );

		// inline the finally after the try body
		nodes.addAll(
		    boxTry.getFinallyBody()
		        .stream()
		        .map( ( boxNode ) -> transpiler.transform( boxNode, context, ReturnValueContext.EMPTY ) )
		        .flatMap( ( nodeList ) -> nodeList.stream() )
		        .toList()
		);

		// nodes.addAll( tracker.popAllStackEntries() );

		// if we hit this instruction we have successfully executed the try body and inlined finally code
		// we can skip to the end of this construct
		nodes.add( new JumpInsnNode( Opcodes.GOTO, finallyEndLabel ) );

		if ( boxTry.getCatches().size() > 0 ) {

			LabelNode javaCatchBodyStart = new LabelNode();
			nodes.add( javaCatchBodyStart );

			// when entering an exception handler the stack is cleared and the exception is pushed onto the stack
			// tracker.clearStackCounter();

			var eVar = tracker.storeNewVariable( Opcodes.ASTORE );
			nodes.addAll( eVar.nodes() );

			for ( BoxTryCatch catchNode : boxTry.getCatches() ) {
				nodes.addAll(
				    generateCatchBodyNodes( context, tracker, boxTry, catchNode, finallyStartLabel, finallyEndLabel, eVar.index() )
				);
			}

			TryCatchBlockNode catchHandler = new TryCatchBlockNode( tryStartLabel, tryEndLabel, javaCatchBodyStart,
			    null );
			transpiler.addTryCatchBlock( catchHandler );

			// if we hit this node we have successfully handled the error and we need to jump over the final finally block
			nodes.add( new JumpInsnNode( Opcodes.GOTO, finallyEndLabel ) );
		}

		TryCatchBlockNode catchHandler = new TryCatchBlockNode( tryStartLabel, tryEndLabel, finallyStartLabel,
		    null );
		transpiler.addTryCatchBlock( catchHandler );

		nodes.add( finallyStartLabel );
		// tracker.popAllStackEntries();

		var errorVarStore = tracker.storeNewVariable( Opcodes.ASTORE );
		nodes.addAll( errorVarStore.nodes() );

		nodes.addAll(
		    boxTry.getFinallyBody()
		        .stream()
		        .map( ( boxNode ) -> transpiler.transform( boxNode, context, ReturnValueContext.EMPTY )
		        ).flatMap( ( nodeList ) -> nodeList.stream() )
		        .toList()
		);

		// nodes.addAll( tracker.popAllStackEntries() );

		nodes.add( new VarInsnNode( Opcodes.ALOAD, errorVarStore.index() ) );

		nodes.add( new InsnNode( Opcodes.ATHROW ) );

		finallyEndLabel.getLabel().info = "this is a test";
		nodes.add( finallyEndLabel );

		transpiler.addTryCatchBlock( new TryCatchBlockNode( tryStartLabel, tryEndLabel, finallyStartLabel, null ) );

		// tracker.trackUnusedStackEntry();

		nodes.add( new InsnNode( Opcodes.ACONST_NULL ) );

		return nodes;

	}

	private List<AbstractInsnNode> generateCatchBodyNodes(
	    TransformerContext context,
	    MethodContextTracker tracker,
	    BoxTry boxTry,
	    BoxTryCatch boxCatch,
	    LabelNode finallyStartLabel,
	    LabelNode finallyEndLabel,
	    int eVarIndex ) {
		List<AbstractInsnNode>	nodes				= new ArrayList<AbstractInsnNode>();

		LabelNode				startHandlerLabel	= new LabelNode();
		LabelNode				endHandlerLabel		= new LabelNode();
		// need to build up our if logic
		nodes.addAll( generateCatchIfGuard( context, boxCatch.getCatchTypes(), tracker, startHandlerLabel, endHandlerLabel, eVarIndex ) );
		// end if logic

		nodes.add( startHandlerLabel );
		// setup our catch context
		nodes.add( new TypeInsnNode( Opcodes.NEW, Type.getInternalName( CatchBoxContext.class ) ) );

		// catchBoxContext -> catchBoxContext, catchBoxContext
		nodes.add( new InsnNode( Opcodes.DUP ) );
		// catchBoxContext, catchBoxContext -> catchBoxContext, catchBoxContext, context
		nodes.addAll( tracker.loadCurrentContext() );

		// e, context, e -> e, context, e, key
		// create key
		nodes.addAll( transpiler.createKey( boxCatch.getException().getName() ) );

		// e, context, e, key -> e, context, key, e
		nodes.add( new VarInsnNode( Opcodes.ALOAD, eVarIndex ) );
		// nodes.add( new InsnNode( Opcodes.SWAP ) );

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

		// first catch body
		nodes.addAll(
		    boxCatch.getCatchBody()
		        .stream()
		        .map( ( boxNode ) -> transpiler.transform( boxNode, context, ReturnValueContext.EMPTY )
		        ).flatMap( ( nodeList ) -> nodeList.stream() )
		        .toList()
		);

		tracker.popContext();

		// inline finally
		nodes.addAll(
		    boxTry.getFinallyBody()
		        .stream()
		        .map( ( boxNode ) -> transpiler.transform( boxNode, context, ReturnValueContext.EMPTY )
		        ).flatMap( ( nodeList ) -> nodeList.stream() )
		        .toList()
		);

		// nodes.addAll( tracker.popAllStackEntries() );

		nodes.add( new JumpInsnNode( Opcodes.GOTO, finallyEndLabel ) );
		nodes.add( endHandlerLabel );

		// I think we need a TryCatchBlockNode that will send the catch code to the finally handler if it errors
		transpiler.addTryCatchBlock( new TryCatchBlockNode( startHandlerLabel, endHandlerLabel, finallyStartLabel, null ) );

		return nodes;
	}

	private List<AbstractInsnNode> generateCatchIfGuard( TransformerContext context, List<BoxExpression> catchTypes, MethodContextTracker tracker,
	    LabelNode startHandlerLabel, LabelNode endHandlerLabel, int eVarIndex ) {
		List<AbstractInsnNode> nodes = new ArrayList<AbstractInsnNode>();

		if ( catchTypes.size() == 0 ) {
			return new ArrayList<AbstractInsnNode>();
		}

		for ( int i = 0; i < catchTypes.size() - 1; i++ ) {
			// build them and use ors
			nodes.add( new VarInsnNode( Opcodes.ALOAD, eVarIndex ) );
			// e, e -> e, e, context
			nodes.addAll( tracker.loadCurrentContext() );
			// e, e, context -> e, context, e
			nodes.add( new InsnNode( Opcodes.SWAP ) );

			nodes.addAll( transpiler.transform( catchTypes.get( i ), context, ReturnValueContext.VALUE ) );
			// decrement by 1 because we consume the catchType
			// tracker.decrementStackCounter( 1 );

			// e, context, e, type -> e, bool
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

			// if true move to the catch body
			// e, bool -> e
			nodes.add( new JumpInsnNode( Opcodes.IFNE, startHandlerLabel ) );
		}

		nodes.add( new VarInsnNode( Opcodes.ALOAD, eVarIndex ) );
		// e, e -> e, e, context
		nodes.addAll( tracker.loadCurrentContext() );
		// e, e, context -> e, context, e
		nodes.add( new InsnNode( Opcodes.SWAP ) );

		// e, context, e -> e, context, e, type
		nodes.addAll( transpiler.transform( catchTypes.getLast(), context, ReturnValueContext.VALUE ) );
		// decrement by 1 because we consume the catchType
		// tracker.decrementStackCounter( 1 );

		// e, context, e, type -> e, bool
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