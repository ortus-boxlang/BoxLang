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
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnNode;
import ortus.boxlang.compiler.asmboxpiler.Transpiler;
import ortus.boxlang.compiler.asmboxpiler.transformer.AbstractTransformer;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.statement.BoxReturn;

import java.util.ArrayList;
import java.util.List;

public class BoxReturnTransformer extends AbstractTransformer {

	public BoxReturnTransformer(Transpiler transpiler ) {
		super( transpiler );
	}

	@Override
	public List<AbstractInsnNode> transform(BoxNode node ) throws IllegalStateException {
		BoxReturn			boxReturn	= ( BoxReturn ) node;

		List<AbstractInsnNode> nodes = new ArrayList<>();
		if ( boxReturn.getExpression() == null ) {
			nodes.add(new InsnNode(Opcodes.ACONST_NULL));
		} else {
			nodes.addAll(transpiler.transform( boxReturn.getExpression() ));
		}
		nodes.add(new InsnNode(Opcodes.ARETURN));
		return nodes;
	}

}
