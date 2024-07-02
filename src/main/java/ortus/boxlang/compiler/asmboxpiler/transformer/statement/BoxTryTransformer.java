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
import ortus.boxlang.compiler.asmboxpiler.transformer.TransformerContext;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.statement.BoxTry;
import ortus.boxlang.runtime.context.CatchBoxContext;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.Key;

public class BoxTryTransformer extends AbstractTransformer {

	public BoxTryTransformer( Transpiler transpiler ) {
		super( transpiler );
	}

	@Override
	public List<AbstractInsnNode> transform( BoxNode node, TransformerContext context ) {
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

		// this is being used to help avoid an "incompatible stack exception"
		// nodes.add( new InsnNode( Opcodes.POP ) );
		// nodes.add( new InsnNode( Opcodes.POP ) );

		nodes.addAll( tracker.popAllStackEntries() );
		// since we inlined our finally when the code doesnt error we can skip the final finally block
		nodes.add( new JumpInsnNode( Opcodes.GOTO, finallyEndLabel ) );

		// our catch code

		if ( boxTry.getCatches().size() > 0 ) {
			LabelNode javaCatchBodyStart = new LabelNode();
			nodes.add( javaCatchBodyStart );
			tracker.clearStackCounter();
			var eVar = tracker.storeNewVariable( Opcodes.ASTORE );
			nodes.addAll( eVar.nodes() );

			// setup our catch context
			nodes.add( new TypeInsnNode( Opcodes.NEW, Type.getInternalName( CatchBoxContext.class ) ) );

			// catchBoxContext -> catchBoxContext, catchBoxContext
			nodes.add( new InsnNode( Opcodes.DUP ) );
			// catchBoxContext, catchBoxContext -> catchBoxContext, catchBoxContext, context
			nodes.addAll( tracker.loadCurrentContext() );

			// e, context, e -> e, context, e, key
			// create key
			nodes.addAll( transpiler.createKey( boxTry.getCatches().getFirst().getException().getName() ) );

			// e, context, e, key -> e, context, key, e
			nodes.add( new VarInsnNode( Opcodes.ALOAD, eVar.index() ) );
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
			    boxTry.getCatches().getFirst().getCatchBody()
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

			nodes.addAll( tracker.popAllStackEntries() );
			nodes.add( new JumpInsnNode( Opcodes.GOTO, finallyEndLabel ) );

			TryCatchBlockNode catchHandler = new TryCatchBlockNode( tryStartLabel, tryEndLabel, javaCatchBodyStart,
			    null );
			transpiler.addTryCatchBlock( catchHandler );

		}

		// end catch code
		// LabelNode catchBodyStart = new LabelNode();
		// nodes.add( catchBodyStart );
		// nodes.add( new JumpInsnNode( Opcodes.GOTO, finallyEndLabel ) );
		TryCatchBlockNode catchHandler = new TryCatchBlockNode( tryStartLabel, tryEndLabel, finallyStartLabel,
		    null );
		transpiler.addTryCatchBlock( catchHandler );

		nodes.add( finallyStartLabel );
		tracker.popAllStackEntries();

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

		nodes.add( new InsnNode( Opcodes.ACONST_NULL ) );

		return nodes;

	}

}
