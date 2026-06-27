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

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

import ortus.boxlang.compiler.asmboxpiler.AsmTranspiler;
import ortus.boxlang.compiler.asmboxpiler.transformer.AbstractTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.ReturnValueContext;
import ortus.boxlang.compiler.asmboxpiler.transformer.TransformerContext;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.expression.BoxStringBuilderLiteral;
import ortus.boxlang.runtime.types.BoxStringBuilder;

/**
 * Emits bytecode for {@code sb"..."} literals.
 *
 * <p>
 * The generated sequence pushes the inner string expression onto the stack and
 * then invokes {@code BoxStringBuilder.of(Object)} to create the wrapped instance.
 */
public class BoxStringBuilderLiteralTransformer extends AbstractTransformer {

	public BoxStringBuilderLiteralTransformer( AsmTranspiler transpiler ) {
		super( transpiler );
	}

	@Override
	public List<AbstractInsnNode> transform( BoxNode node, TransformerContext context, ReturnValueContext returnContext ) throws IllegalStateException {
		BoxStringBuilderLiteral	literal	= ( BoxStringBuilderLiteral ) node;
		List<AbstractInsnNode>	nodes	= new ArrayList<>();

		// Push the initial string value expression onto the stack
		nodes.addAll( transpiler.transform( literal.getInitialValue(), context, ReturnValueContext.VALUE_OR_NULL ) );

		// Call BoxStringBuilder.of(Object) to wrap the string value
		nodes.add( new MethodInsnNode(
		    Opcodes.INVOKESTATIC,
		    Type.getInternalName( BoxStringBuilder.class ),
		    "of",
		    Type.getMethodDescriptor( Type.getType( BoxStringBuilder.class ), Type.getType( Object.class ) ),
		    false ) );

		return nodes;
	}

}
