/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ortus.boxlang.compiler.asmboxpiler.transformer.expression;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import ortus.boxlang.compiler.asmboxpiler.AsmTranspiler;
import ortus.boxlang.compiler.asmboxpiler.transformer.AbstractTransformer;
import ortus.boxlang.compiler.ast.BoxExpression;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.expression.BoxArrayLiteral;
import ortus.boxlang.runtime.types.Array;

import java.util.ArrayList;
import java.util.List;

public class BoxArrayLiteralTransformer extends AbstractTransformer {

	public BoxArrayLiteralTransformer(AsmTranspiler transpiler ) {
		super( transpiler );
	}

	@Override
	public List<AbstractInsnNode>  transform(BoxNode node ) throws IllegalStateException {
		BoxArrayLiteral		arrayLiteral	= ( BoxArrayLiteral ) node;
		List<AbstractInsnNode> nodes = new ArrayList<>();
		nodes.add(new LdcInsnNode(arrayLiteral.getValues().size()));
		nodes.add(new TypeInsnNode(Opcodes.ANEWARRAY, Type.getInternalName(Object.class)));
		int index = 0;
		for (BoxExpression value : arrayLiteral.getValues()) {
			nodes.add(new InsnNode(Opcodes.DUP));
			nodes.add(new LdcInsnNode(index++));
			nodes.addAll( transpiler.transform( value ) );
			nodes.add(new InsnNode(Opcodes.AASTORE));
		}
		nodes.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
			Type.getInternalName(Array.class),
			"of",
			Type.getMethodDescriptor(Type.getType(Array.class), Type.getType(Object[].class)),
			false));
		return nodes;
	}
}
