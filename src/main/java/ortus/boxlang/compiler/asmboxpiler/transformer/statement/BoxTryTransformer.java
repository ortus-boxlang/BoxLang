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
import ortus.boxlang.compiler.asmboxpiler.transformer.TransformerContext;
import ortus.boxlang.compiler.ast.BoxExpression;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.statement.BoxTry;
import ortus.boxlang.runtime.context.CatchBoxContext;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.exceptions.ExceptionUtil;

public class BoxTryTransformer extends AbstractTransformer {

	public BoxTryTransformer( Transpiler transpiler ) {
		super( transpiler );
	}

	@Override
	public List<AbstractInsnNode> transform( BoxNode node, TransformerContext context ) {
		MethodContextTracker	tracker				= transpiler.getCurrentMethodContextTracker();
		List<AbstractInsnNode>	nodes				= new ArrayList<>();
		BoxTry					boxTry				= ( BoxTry ) node;

		LabelNode				tryStartLabel		= new LabelNode();
		LabelNode				tryEndLabel			= new LabelNode();
		LabelNode				finallyStartLabel	= new LabelNode();
		LabelNode				finallyEndLabel		= new LabelNode();

		nodes.add( tryStartLabel );

		nodes.addAll(
		    boxTry.getTryBody()
		        .stream()
		        .map( ( boxNode ) -> transpiler.transform( boxNode, context )
		        ).flatMap( ( nodeList ) -> nodeList.stream() )
		        .toList()
		);

		nodes.add( tryEndLabel );

		// inline the finally after the try body
		nodes.addAll(
		    boxTry.getFinallyBody()
		        .stream()
		        .map( ( boxNode ) -> transpiler.transform( boxNode, context ) )
		        .flatMap( ( nodeList ) -> nodeList.stream() )
		        .toList()
		);

		// since we inlined our finally when the code doesnt error we can skip the final finally block
		nodes.add( new JumpInsnNode( Opcodes.GOTO, finallyEndLabel ) );

		// our catch code

		if ( boxTry.getCatches().size() > 0 ) {
			// build up abort check
			// LabelNode abortCatchStartLabel = new LabelNode();
			// LabelNode abortCatchEndLabel = new LabelNode();

			// nodes.add( abortCatchStartLabel );
			// nodes.add( new InsnNode( Opcodes.ATHROW ) );
			// nodes.add( abortCatchEndLabel );

			// TryCatchBlockNode abortHandler = new TryCatchBlockNode( tryStartLabel, tryEndLabel, abortCatchStartLabel,
			// Type.getInternalName( AbortException.class ) );
			// transpiler.addTryCatchBlock( abortHandler );
			// TryCatchBlockNode abortFinallyHandler = new TryCatchBlockNode( abortCatchStartLabel, abortCatchEndLabel, finallyStartLabel,
			// Type.getInternalName( AbortException.class ) );
			// transpiler.addTryCatchBlock( abortFinallyHandler );

			// store the exception

			// build up try catches
			for ( var boxCatch : boxTry.getCatches() ) {
				LabelNode			catchBodyStart	= new LabelNode();
				List<BoxExpression>	catchTypes		= boxCatch.getCatchTypes();
				// create an endHandlerLabel
				LabelNode			endHandlerLabel	= new LabelNode();
				// create an if condition that verifies the exception matches the passed in jumps to endHandlerLabel if doesnt
				for ( int i = 0; i < catchTypes.size() - 1; i++ ) {
					// build them and use ors
					// e -> e, e
					nodes.add( new InsnNode( Opcodes.DUP ) );
					// e, e -> e, e, context
					nodes.addAll( tracker.loadCurrentContext() );
					// e, e, context -> e, context, e
					nodes.add( new InsnNode( Opcodes.SWAP ) );

					// e, context, e -> e, context, e, type
					nodes.addAll( transpiler.transform( catchTypes.get( i ), context ) );

					// e, context, e, type -> e, bool
					nodes.add( new MethodInsnNode(
					    Opcodes.INVOKESTATIC,
					    Type.getInternalName( ExceptionUtil.class ),
					    "exceptionIsOfType",
					    Type.getMethodDescriptor( Type.getType( Boolean.class ), Type.getType( IBoxContext.class ), Type.getType( String.class ),
					        Type.getType( Boolean.class ) ),
					    false
					) );

					// if true move to the catch body
					// e, bool -> e
					nodes.add( new JumpInsnNode( Opcodes.IFNE, catchBodyStart ) );
				}

				// e -> e, e
				nodes.add( new InsnNode( Opcodes.DUP ) );
				// e, e -> e, e, context
				nodes.add( new VarInsnNode( Opcodes.ALOAD, 1 ) );
				// e, e, context -> e, context, e
				nodes.add( new InsnNode( Opcodes.SWAP ) );

				// e, context, e -> e, context, e, type
				nodes.addAll( transpiler.transform( catchTypes.getLast(), context ) );

				// e, context, e, type -> e, bool
				nodes.add( new MethodInsnNode(
				    Opcodes.INVOKESTATIC,
				    Type.getInternalName( ExceptionUtil.class ),
				    "exceptionIsOfType",
				    Type.getMethodDescriptor( Type.getType( Boolean.class ), Type.getType( IBoxContext.class ), Type.getType( String.class ),
				        Type.getType( Boolean.class ) ),
				    false
				) );

				nodes.add( new JumpInsnNode( Opcodes.IFEQ, endHandlerLabel ) );

				// start catchBody
				nodes.add( catchBodyStart );
				// create an exception context
				// "CatchBoxContext ${catchContextName} = new CatchBoxContext( ${contextName}, ${catchNameKey}, ${throwableName} );"
				// copy the exception
				// e -> e,e
				nodes.add( new InsnNode( Opcodes.DUP ) );
				// e,e -> e, e, context
				nodes.addAll( tracker.loadCurrentContext() );
				// e, e, context -> e, context, e
				nodes.add( new InsnNode( Opcodes.SWAP ) );

				// e, context, e -> e, context, e, key
				// create key
				nodes.addAll( transpiler.createKey( boxCatch.getException().getName() ) );

				// e, context, e, key -> e, context, key, e
				nodes.add( new InsnNode( Opcodes.SWAP ) );

				// e, context, key, e -> e, catchBoxContext
				nodes.add( new TypeInsnNode( Opcodes.NEW, Type.getInternalName( CatchBoxContext.class ) ) );
				nodes.add( new InsnNode( Opcodes.DUP ) );
				nodes.add( new InsnNode( Opcodes.ACONST_NULL ) );
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

				nodes.addAll(
				    boxCatch.getCatchBody()
				        .stream()
				        .map( ( boxNode ) -> transpiler.transform( boxNode, context )
				        ).flatMap( ( nodeList ) -> nodeList.stream() )
				        .toList()
				);
				tracker.popContext();
				// inline finally
				nodes.addAll(
				    boxTry.getFinallyBody()
				        .stream()
				        .map( ( boxNode ) -> transpiler.transform( boxNode, context )
				        ).flatMap( ( nodeList ) -> nodeList.stream() )
				        .toList()
				);

				nodes.add( new JumpInsnNode( Opcodes.GOTO, finallyEndLabel ) );

				LabelNode catchBodyEnd = new LabelNode();
				nodes.add( catchBodyEnd );

				TryCatchBlockNode catchHandler = new TryCatchBlockNode( tryStartLabel, tryEndLabel, catchBodyStart,
				    null );
				transpiler.addTryCatchBlock( catchHandler );
				TryCatchBlockNode catchFinallyHandler = new TryCatchBlockNode( catchBodyStart, catchBodyEnd, finallyStartLabel,
				    null );
				transpiler.addTryCatchBlock( catchFinallyHandler );

				nodes.add( endHandlerLabel );
			}
		}

		// end catch code
		LabelNode catchBodyStart = new LabelNode();
		nodes.add( catchBodyStart );
		nodes.add( new JumpInsnNode( Opcodes.GOTO, finallyEndLabel ) );
		TryCatchBlockNode catchHandler = new TryCatchBlockNode( tryStartLabel, tryEndLabel, catchBodyStart,
		    null );
		transpiler.addTryCatchBlock( catchHandler );

		nodes.add( finallyStartLabel );

		var errorVarStore = tracker.storeNewVariable( Opcodes.ASTORE );
		nodes.addAll( errorVarStore.nodes() );
		// should these be reused from above or created a second time?
		nodes.addAll(
		    boxTry.getFinallyBody()
		        .stream()
		        .map( ( boxNode ) -> transpiler.transform( boxNode, context )
		        ).flatMap( ( nodeList ) -> nodeList.stream() )
		        .toList()
		);

		nodes.add( new VarInsnNode( Opcodes.ALOAD, errorVarStore.index() ) );

		nodes.add( new InsnNode( Opcodes.ATHROW ) );

		nodes.add( finallyEndLabel );

		transpiler.addTryCatchBlock( new TryCatchBlockNode( tryStartLabel, tryEndLabel, finallyStartLabel, null ) );

		return nodes;

	}

}
