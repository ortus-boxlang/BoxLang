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
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import javassist.bytecode.Opcode;
import ortus.boxlang.compiler.asmboxpiler.AsmHelper;
import ortus.boxlang.compiler.asmboxpiler.MethodContextTracker;
import ortus.boxlang.compiler.asmboxpiler.Transpiler;
import ortus.boxlang.compiler.asmboxpiler.transformer.AbstractTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.ReturnValueContext;
import ortus.boxlang.compiler.asmboxpiler.transformer.TransformerContext;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.statement.BoxForIndex;
import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;

public class BoxForIndexTransformer extends AbstractTransformer {

	public BoxForIndexTransformer( Transpiler transpiler ) {
		super( transpiler );
	}

	public List<AbstractInsnNode> transform( BoxNode node, TransformerContext context,
	    ReturnValueContext returnValueContext ) {
		BoxForIndex				forIn	= ( BoxForIndex ) node;
		List<AbstractInsnNode>	nodes	= new ArrayList<>();
		AsmHelper.addDebugLabel( nodes, "BoxForIndex" );
		Optional<MethodContextTracker> trackerOption = transpiler.getCurrentMethodContextTracker();

		if ( trackerOption.isEmpty() ) {
			throw new IllegalStateException();
		}

		MethodContextTracker	tracker		= trackerOption.get();

		LabelNode				breakTarget	= new LabelNode();
		LabelNode				firstLoop	= new LabelNode();
		LabelNode				loopStart	= new LabelNode();
		LabelNode				loopEnd		= new LabelNode();

		tracker.setContinue( forIn, loopStart );
		tracker.setBreak( forIn, breakTarget );
		if ( forIn.getLabel() != null ) {
			tracker.setStringLabel( forIn.getLabel(), forIn );
		}

		if ( forIn.getInitializer() != null ) {
			AsmHelper.addDebugLabel( nodes, "BoxForIndex - initializer" );
			nodes.addAll( transpiler.transform( forIn.getInitializer(), context, ReturnValueContext.EMPTY ) );
		}

		var varStore = tracker.storeNewVariable( Opcodes.ASTORE );
		nodes.add( new InsnNode( Opcodes.ACONST_NULL ) );

		AsmHelper.addDebugLabel( nodes, "BoxForIndex - goto firstLoop" );
		nodes.add( new JumpInsnNode( Opcode.GOTO, firstLoop ) );

		AsmHelper.addDebugLabel( nodes, "BoxForIndex - loopStart" );
		nodes.add( loopStart );

		if ( forIn.getStep() != null ) {
			AsmHelper.addDebugLabel( nodes, "BoxForIndex - step" );
			nodes.addAll( transpiler.transform( forIn.getStep(), context, ReturnValueContext.EMPTY ) );
		}

		AsmHelper.addDebugLabel( nodes, "BoxForIndex - firstLoop" );
		nodes.add( firstLoop );

		nodes.addAll( varStore.nodes() );

		AsmHelper.addDebugLabel( nodes, "BoxForIndex - condition" );
		if ( forIn.getCondition() != null ) {
			nodes.addAll( transpiler.transform( forIn.getCondition(), context, ReturnValueContext.VALUE ) );
			nodes.add( new MethodInsnNode( Opcodes.INVOKESTATIC,
			    Type.getInternalName( BooleanCaster.class ),
			    "cast",
			    Type.getMethodDescriptor( Type.getType( Boolean.class ), Type.getType( Object.class ) ),
			    false ) );

			nodes.add( new MethodInsnNode( Opcodes.INVOKEVIRTUAL,
			    Type.getInternalName( Boolean.class ),
			    "booleanValue",
			    Type.getMethodDescriptor( Type.getType( boolean.class ) ),
			    false ) );
		} else {
			nodes.add( new LdcInsnNode( 1 ) );
		}

		AsmHelper.addDebugLabel( nodes, "BoxForIndex - goto loopend" );
		nodes.add( new JumpInsnNode( Opcodes.IFEQ, loopEnd ) );

		AsmHelper.addDebugLabel( nodes, "BoxForIndex - body" );
		nodes.addAll( transpiler.transform( forIn.getBody(), context, ReturnValueContext.VALUE_OR_NULL ) );

		AsmHelper.addDebugLabel( nodes, "BoxForIndex - goto loopStart" );
		nodes.add( new JumpInsnNode( Opcode.GOTO, loopStart ) );

		AsmHelper.addDebugLabel( nodes, "BoxForIndex - breakTarget" );
		nodes.add( breakTarget );

		nodes.addAll( varStore.nodes() );

		AsmHelper.addDebugLabel( nodes, "BoxForIndex - loopEnd" );
		nodes.add( loopEnd );

		nodes.add( new VarInsnNode( Opcodes.ALOAD, varStore.index() ) );

		if ( returnValueContext.empty ) {
			nodes.add( new InsnNode( Opcodes.POP ) );
		}

		tracker.setCurrentBreak( null, null );

		tracker.setCurrentContinue( null, null );

		AsmHelper.addDebugLabel( nodes, "BoxForIndex - done" );

		return nodes;
	}

}