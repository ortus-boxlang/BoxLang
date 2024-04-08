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

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import ortus.boxlang.compiler.asmboxpiler.Transpiler;
import ortus.boxlang.compiler.asmboxpiler.transformer.AbstractTransformer;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.statement.BoxWhile;

public class BoxWhileTransformer extends AbstractTransformer {

	public BoxWhileTransformer(Transpiler processor ) {
		super( processor );
	}

	@Override
	public void transform(BoxNode node, MethodVisitor visitor) throws IllegalStateException {
		BoxWhile	boxWhile	= ( BoxWhile ) node;
		Label start = new Label(), end = new Label();
		visitor.visitLabel(start);
		transpiler.transpile( boxWhile.getCondition(), visitor );
		visitor.visitJumpInsn(Opcodes.IFEQ, end);
		for ( BoxNode statement : boxWhile.getBody() ) {
			transpiler.transform( statement, visitor );
		}
		visitor.visitJumpInsn(Opcodes.GOTO, start);
		visitor.visitLabel(end);
	}
}
