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
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import ortus.boxlang.compiler.asmboxpiler.Transpiler;
import ortus.boxlang.compiler.asmboxpiler.transformer.AbstractTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.TransformerContext;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.statement.BoxAssert;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.operators.Assert;

import java.util.ArrayList;
import java.util.List;

public class BoxAssertTransformer extends AbstractTransformer {

	public BoxAssertTransformer(Transpiler transpiler ) {
		super( transpiler );
	}

	@Override
	public List<AbstractInsnNode> transform(BoxNode node, TransformerContext context ) throws IllegalStateException {
		BoxAssert			boxAssert	= ( BoxAssert ) node;
		List<AbstractInsnNode> nodes = new ArrayList<>();
		nodes.add(new VarInsnNode(Opcodes.ALOAD, 1));
		nodes.addAll(transpiler.transform( boxAssert.getExpression(), TransformerContext.RIGHT ));
		nodes.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
			Type.getInternalName(Assert.class),
			"invoke",
			Type.getMethodDescriptor(Type.getType(Boolean.class), Type.getType(IBoxContext.class), Type.getType(Object.class)),
			false));
		return nodes;
	}
}
