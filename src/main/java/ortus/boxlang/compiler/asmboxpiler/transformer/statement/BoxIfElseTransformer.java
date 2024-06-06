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

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import ortus.boxlang.compiler.asmboxpiler.Transpiler;
import ortus.boxlang.compiler.asmboxpiler.transformer.AbstractTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.TransformerContext;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.statement.BoxIfElse;
import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;

import java.util.ArrayList;
import java.util.List;

public class BoxIfElseTransformer extends AbstractTransformer {

	public BoxIfElseTransformer( Transpiler transpiler ) {
		super( transpiler );
	}

	@Override
	public List<AbstractInsnNode> transform( BoxNode node, TransformerContext context ) {
		BoxIfElse				ifElse	= ( BoxIfElse ) node;

		List<AbstractInsnNode>	nodes	= new ArrayList<>();
		nodes.addAll( transpiler.transform( ifElse.getCondition(), TransformerContext.NONE ) );
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
		LabelNode ifLabel = new LabelNode();
		nodes.add( new JumpInsnNode( Opcodes.IFEQ, ifLabel ) );
		nodes.addAll( transpiler.transform( ifElse.getThenBody(), TransformerContext.NONE ) );

		if ( ifElse.getElseBody() != null ) {
			LabelNode elseLabel = new LabelNode();
			nodes.add( new JumpInsnNode( Opcodes.GOTO, elseLabel ) );
			nodes.add( ifLabel );
			nodes.addAll( transpiler.transform( ifElse.getElseBody(), TransformerContext.NONE ) );
			nodes.add( elseLabel );
		} else {
			nodes.add( ifLabel );
		}

		nodes.add( new InsnNode( Opcodes.ACONST_NULL ) );
		nodes.add( new InsnNode( Opcodes.POP ) );

		return nodes;

	}

}
