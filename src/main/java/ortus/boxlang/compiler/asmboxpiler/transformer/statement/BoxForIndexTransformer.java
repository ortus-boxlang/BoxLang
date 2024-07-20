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
		LabelNode				loopStart	= new LabelNode();
		LabelNode				loopEnd		= new LabelNode();

		nodes.addAll( transpiler.transform( forIn.getInitializer(), context, returnValueContext ) );

		// push two nulls onto the stack in order to initialize our strategy for keeping
		// the stack height consistent
		// this is to allow the statement to return an expression in the case of a
		// BoxScript execution
		if ( returnValueContext == ReturnValueContext.VALUE_OR_NULL ) {
			nodes.add( new InsnNode( Opcodes.ACONST_NULL ) );
			nodes.add( new InsnNode( Opcodes.ACONST_NULL ) );
		}

		nodes.add( loopStart );

		// every iteration we will swap the values and pop in order to remove the older
		// value
		if ( returnValueContext == ReturnValueContext.VALUE_OR_NULL ) {
			nodes.add( new InsnNode( Opcodes.SWAP ) );
			nodes.add( new InsnNode( Opcodes.POP ) );
		}

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

		nodes.add( new JumpInsnNode( Opcodes.IFEQ, loopEnd ) );

		nodes.addAll( transpiler.transform( forIn.getBody(), context, returnValueContext ) );

		nodes.addAll( transpiler.transform( forIn.getStep(), context ) );

		nodes.add( new JumpInsnNode( Opcode.GOTO, loopStart ) );

		nodes.add( loopEnd );

		return nodes;
	}

}