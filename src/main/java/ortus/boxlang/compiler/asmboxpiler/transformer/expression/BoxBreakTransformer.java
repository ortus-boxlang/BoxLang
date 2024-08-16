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
package ortus.boxlang.compiler.asmboxpiler.transformer.expression;

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

import ortus.boxlang.compiler.asmboxpiler.Transpiler;
import ortus.boxlang.compiler.asmboxpiler.transformer.AbstractTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.ReturnValueContext;
import ortus.boxlang.compiler.asmboxpiler.transformer.TransformerContext;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.statement.BoxBreak;
import ortus.boxlang.runtime.components.Component;

public class BoxBreakTransformer extends AbstractTransformer {

	public BoxBreakTransformer( Transpiler transpiler ) {
		super( transpiler );
	}

	@Override
	public List<AbstractInsnNode> transform( BoxNode node, TransformerContext context, ReturnValueContext returnContext ) throws IllegalStateException {
		BoxBreak				breakNode		= ( BoxBreak ) node;
		ExitsAllowed			exitsAllowed	= getExitsAllowed( node );

		LabelNode				currentBreak	= transpiler.getCurrentBreak( breakNode.getLabel() );
		List<AbstractInsnNode>	nodes			= new ArrayList<AbstractInsnNode>();

		if ( returnContext.nullable || exitsAllowed.equals( ExitsAllowed.FUNCTION ) ) {
			nodes.add( new InsnNode( Opcodes.ACONST_NULL ) );
		}

		if ( currentBreak != null ) {
			nodes.add( new JumpInsnNode( Opcodes.GOTO, currentBreak ) );
			return nodes;
		}

		if ( exitsAllowed.equals( ExitsAllowed.COMPONENT ) ) {
			nodes.add( new LdcInsnNode( "" ) );
			nodes.add( new MethodInsnNode( Opcodes.INVOKESTATIC,
			    Type.getInternalName( Component.BodyResult.class ),
			    "ofBreak",
			    Type.getMethodDescriptor( Type.getType( Component.BodyResult.class ), Type.getType( String.class ) ),
			    false )
			);
			nodes.add( new InsnNode( Opcodes.ARETURN ) );
			return nodes;
		} else if ( exitsAllowed.equals( ExitsAllowed.LOOP ) ) {
			// template = "if(true) break " + breakLabel + ";";
			nodes.add( new InsnNode( Opcodes.ARETURN ) );
			return nodes;
		} else if ( exitsAllowed.equals( ExitsAllowed.FUNCTION ) ) {
			nodes.add( new InsnNode( Opcodes.ARETURN ) );
			return nodes;
		} else {
			// template = "if(true) return;";
		}
		// if ( currentBreak == null ) {
		// throw new RuntimeException( "Cannot break from current location" );
		// }

		throw new RuntimeException( "Cannot break from current location" );

	}
}
