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

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import ortus.boxlang.compiler.asmboxpiler.AsmTranspiler;
import ortus.boxlang.compiler.asmboxpiler.transformer.AbstractTransformer;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.expression.BoxFunctionInvocation;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.Key;

import java.util.ArrayList;
import java.util.List;

public class BoxFunctionInvocationTransformer extends AbstractTransformer {

	public BoxFunctionInvocationTransformer(AsmTranspiler transpiler ) {
		super( transpiler );
	}

	@Override
	public List<AbstractInsnNode> transform( BoxNode node ) throws IllegalStateException {
		BoxFunctionInvocation	function			= ( BoxFunctionInvocation ) node;

		List<AbstractInsnNode> nodes = new ArrayList<>();
		nodes.add(new VarInsnNode(Opcodes.ALOAD, 1));
		nodes.addAll( createKey( function.getName() ));

		nodes.add(new LdcInsnNode( function.getArguments().size()));
		nodes.add(new TypeInsnNode(Opcodes.ANEWARRAY, Type.getInternalName(Object.class)));
		for ( int i = 0; i < function.getArguments().size(); i++ ) {
			nodes.add(new InsnNode(Opcodes.DUP));
			nodes.add(new LdcInsnNode(i));
			nodes.addAll( transpiler.transform(function.getArguments().get(i)) );
			nodes.add(new InsnNode(Opcodes.AASTORE));
		}

		nodes.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE,
			Type.getInternalName(IBoxContext.class),
			"invokeFunction",
			Type.getMethodDescriptor(Type.getType(Object.class), Type.getType(Key.class), Type.getType(Object[].class)),
			true));

		return nodes;
	}
}
