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

import java.util.List;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnNode;

import ortus.boxlang.compiler.asmboxpiler.Transpiler;
import ortus.boxlang.compiler.asmboxpiler.transformer.AbstractTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.ReturnValueContext;
import ortus.boxlang.compiler.asmboxpiler.transformer.TransformerContext;
import ortus.boxlang.compiler.ast.BoxNode;

/**
 * No-op transformer for {@code BoxLocalClass} statement nodes.
 *
 * Named local classes in scripts and templates (e.g. {@code class Foo {}}) are pre-compiled
 * into auxiliary JVM classes by {@code AsmTranspiler.transpile(BoxScript)} <em>before</em>
 * the {@code _invoke} method body is generated. By the time the body is compiled this
 * statement has already been handled, so the transformer simply emits nothing.
 */
public class BoxLocalClassTransformer extends AbstractTransformer {

	public BoxLocalClassTransformer( Transpiler transpiler ) {
		super( transpiler );
	}

	@Override
	public List<AbstractInsnNode> transform( BoxNode node, TransformerContext context, ReturnValueContext returnContext ) throws IllegalStateException {
		// Local class was already pre-compiled. Emit no instructions for the statement itself.
		// However, callers with a non-EMPTY return context expect something on the stack (the
		// contract of VALUE_OR_NULL). Push null so that callers like transformBodyExpressionsFromScript
		// can safely pop it and replace it with their own null return value.
		if ( returnContext != ReturnValueContext.EMPTY && returnContext != ReturnValueContext.EMPTY_UNLESS_JUMPING ) {
			return List.of( new InsnNode( Opcodes.ACONST_NULL ) );
		}
		return List.of();
	}
}
