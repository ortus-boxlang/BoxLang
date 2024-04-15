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

import org.objectweb.asm.MethodVisitor;
import ortus.boxlang.compiler.asmboxpiler.AsmTranspiler;
import ortus.boxlang.compiler.asmboxpiler.transformer.AbstractTransformer;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.expression.BoxAssignment;
import ortus.boxlang.compiler.ast.statement.BoxAssignmentOperator;

public class BoxAssignmentTransformer extends AbstractTransformer {

	public BoxAssignmentTransformer(AsmTranspiler transpiler ) {
		super( transpiler );
	}

	@Override
	public void transform(BoxNode node, MethodVisitor visitor) throws IllegalStateException {
		BoxAssignment assigment = ( BoxAssignment ) node;
		if ( assigment.getOp() == BoxAssignmentOperator.Equal ) {
			transpiler.transform( assigment.getRight(), visitor );
			/*transformEquals( assigment.getLeft(), assigment.getOp(), assigment.getModifiers(), assigment.getSourceText(),
			    visitor );*/
		} else {
			throw new RuntimeException(); // TODO: what to do?
		}

	}
}
