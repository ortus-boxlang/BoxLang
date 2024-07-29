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

import javassist.bytecode.Opcode;
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
		BoxForIndex						forIn			= ( BoxForIndex ) node;
		List<AbstractInsnNode>			nodes			= new ArrayList<>();
		Optional<MethodContextTracker>	trackerOption	= transpiler.getCurrentMethodContextTracker();

		if ( trackerOption.isEmpty() ) {
			throw new IllegalStateException();
		}

		MethodContextTracker	tracker		= trackerOption.get();
		LabelNode				breakTarget	= new LabelNode();
		LabelNode				firstLoop	= new LabelNode();
		LabelNode				loopStart	= new LabelNode();
		LabelNode				loopEnd		= new LabelNode();

		transpiler.setCurrentBreak( forIn.getLabel(), breakTarget );
		transpiler.setCurrentBreak( null, breakTarget );

		transpiler.setCurrentContinue( null, loopStart );
		transpiler.setCurrentContinue( forIn.getLabel(), loopStart );

		if ( forIn.getInitializer() != null ) {
			nodes.addAll( transpiler.transform( forIn.getInitializer(), context, ReturnValueContext.EMPTY ) );
		}

		// push two nulls onto the stack in order to initialize our strategy for keeping
		// the stack height consistent
		// this is to allow the statement to return an expression in the case of a
		// BoxScript execution
		if ( returnValueContext.nullable ) {
			nodes.add( new InsnNode( Opcodes.ACONST_NULL ) );
			nodes.add( new InsnNode( Opcodes.ACONST_NULL ) );
		}

		nodes.add( new JumpInsnNode( Opcode.GOTO, firstLoop ) );

		nodes.add( loopStart );
		for ( int i = 0; i < 1; i++ ) {
			nodes.add( new InsnNode( Opcode.NOP ) );
		}

		if ( forIn.getStep() != null ) {
			nodes.addAll( transpiler.transform( forIn.getStep(), context, ReturnValueContext.EMPTY ) );
		}

		nodes.add( firstLoop );
		for ( int i = 0; i < 2; i++ ) {
			nodes.add( new InsnNode( Opcode.NOP ) );
		}

		// every iteration we will swap the values and pop in order to remove the older
		// value
		if ( returnValueContext.nullable ) {
			nodes.add( new InsnNode( Opcodes.SWAP ) );
			nodes.add( new InsnNode( Opcodes.POP ) );
		}

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

		nodes.add( new JumpInsnNode( Opcodes.IFEQ, loopEnd ) );

		nodes.addAll( transpiler.transform( forIn.getBody(), context, returnValueContext ) );

		if ( returnValueContext == ReturnValueContext.EMPTY_UNLESS_JUMPING ) {
			nodes.add( new InsnNode( Opcodes.ACONST_NULL ) );
		}

		nodes.add( new JumpInsnNode( Opcode.GOTO, loopStart ) );

		nodes.add( breakTarget );
		for ( int i = 0; i < 3; i++ ) {
			nodes.add( new InsnNode( Opcode.NOP ) );
		}
		// every iteration we will swap the values and pop in order to remove the older
		// value
		if ( returnValueContext.nullable ) {
			nodes.add( new InsnNode( Opcodes.SWAP ) );
			nodes.add( new InsnNode( Opcodes.POP ) );
		}

		if ( !returnValueContext.nullable ) {
			nodes.add( new InsnNode( Opcodes.POP ) );
		}
		nodes.add( loopEnd );
		for ( int i = 0; i < 4; i++ ) {
			nodes.add( new InsnNode( Opcode.NOP ) );
		}

		if ( returnValueContext == ReturnValueContext.EMPTY_UNLESS_JUMPING ) {
			nodes.add( new InsnNode( Opcodes.POP ) );
		}

		return nodes;
	}

}