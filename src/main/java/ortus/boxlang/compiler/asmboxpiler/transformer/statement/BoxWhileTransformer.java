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
import org.objectweb.asm.tree.VarInsnNode;

import ortus.boxlang.compiler.asmboxpiler.AsmHelper;
import ortus.boxlang.compiler.asmboxpiler.MethodContextTracker;
import ortus.boxlang.compiler.asmboxpiler.Transpiler;
import ortus.boxlang.compiler.asmboxpiler.transformer.AbstractTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.ReturnValueContext;
import ortus.boxlang.compiler.asmboxpiler.transformer.TransformerContext;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.statement.BoxWhile;
import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;

public class BoxWhileTransformer extends AbstractTransformer {

	public BoxWhileTransformer( Transpiler transpiler ) {
		super( transpiler );
	}

	@Override
	public List<AbstractInsnNode> transform( BoxNode node, TransformerContext context, ReturnValueContext returnContext ) throws IllegalStateException {
		BoxWhile				boxWhile	= ( BoxWhile ) node;

		LabelNode				start		= new LabelNode(),
		    end = new LabelNode(),
		    breakTarget = new LabelNode();
		List<AbstractInsnNode>	nodes		= new ArrayList<>();

		MethodContextTracker	tracker		= transpiler.getCurrentMethodContextTracker().get();
		tracker.setBreak( boxWhile, breakTarget );
		tracker.setContinue( boxWhile, start );

		if ( boxWhile.getLabel() != null ) {
			tracker.setStringLabel( boxWhile.getLabel(), boxWhile );
		}

		// push two nulls onto the stack in order to initialize our strategy for keeping the stack height consistent
		// this is to allow the statement to return an expression in the case of a BoxScript execution
		// if ( returnContext == ReturnValueContext.VALUE_OR_NULL ) {
		nodes.add( new InsnNode( Opcodes.ACONST_NULL ) );
		// nodes.add( new InsnNode( Opcodes.ACONST_NULL ) );
		var varStore = tracker.storeNewVariable( Opcodes.ASTORE );

		// }

		AsmHelper.addDebugLabel( nodes, "BoxWhile - start label" );
		nodes.add( start );
		nodes.addAll( varStore.nodes() );
		// every iteration we will swap the values and pop in order to remove the older value
		// if ( returnContext == ReturnValueContext.VALUE_OR_NULL ) {
		// nodes.add( new InsnNode( Opcodes.SWAP ) );
		// nodes.add( new InsnNode( Opcodes.POP ) );
		// }

		nodes.addAll( transpiler.transform( boxWhile.getCondition(), TransformerContext.RIGHT, ReturnValueContext.VALUE ) );
		nodes.add( new MethodInsnNode( Opcodes.INVOKESTATIC,
		    Type.getInternalName( BooleanCaster.class ),
		    "cast",
		    Type.getMethodDescriptor( Type.getType( Boolean.class ), Type.getType( Object.class ) ),
		    false ) );
		nodes.add( new MethodInsnNode( Opcodes.INVOKEVIRTUAL,
		    Type.getInternalName( Boolean.class ),
		    "booleanValue",
		    Type.getMethodDescriptor( Type.BOOLEAN_TYPE ),
		    false ) );

		AsmHelper.addDebugLabel( nodes, "BoxWhile - jump end" );
		nodes.add( new JumpInsnNode( Opcodes.IFEQ, end ) );

		nodes.addAll( transpiler.transform( boxWhile.getBody(), TransformerContext.NONE, ReturnValueContext.VALUE_OR_NULL ) );

		AsmHelper.addDebugLabel( nodes, "BoxWhile - jump start" );
		nodes.add( new JumpInsnNode( Opcodes.GOTO, start ) );

		AsmHelper.addDebugLabel( nodes, "BoxWhile - breakTarget" );
		nodes.add( breakTarget );

		nodes.addAll( varStore.nodes() );

		AsmHelper.addDebugLabel( nodes, "BoxWhile - end label" );
		nodes.add( end );

		nodes.add( new VarInsnNode( Opcodes.ALOAD, varStore.index() ) );

		// every iteration we will swap the values and pop in order to remove the older value
		if ( returnContext == ReturnValueContext.EMPTY || returnContext == ReturnValueContext.EMPTY_UNLESS_JUMPING ) {
			nodes.add( new InsnNode( Opcodes.POP ) );
		}

		return AsmHelper.addLineNumberLabels( nodes, node );
	}
}
