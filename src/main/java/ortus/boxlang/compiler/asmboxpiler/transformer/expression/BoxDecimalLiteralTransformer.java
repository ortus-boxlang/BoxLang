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

import java.math.BigDecimal;
import java.util.List;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;

import ortus.boxlang.compiler.asmboxpiler.Transpiler;
import ortus.boxlang.compiler.asmboxpiler.transformer.AbstractTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.ReturnValueContext;
import ortus.boxlang.compiler.asmboxpiler.transformer.TransformerContext;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.expression.BoxDecimalLiteral;
import ortus.boxlang.runtime.types.util.MathUtil;

public class BoxDecimalLiteralTransformer extends AbstractTransformer {

	public BoxDecimalLiteralTransformer( Transpiler transpiler ) {
		super( transpiler );
	}

	@Override
	public List<AbstractInsnNode> transform( BoxNode node, TransformerContext context, ReturnValueContext returnContext ) throws IllegalStateException {
		BoxDecimalLiteral literal = ( BoxDecimalLiteral ) node;

		if ( MathUtil.isHighPrecisionMath() ) {
			return List.of(
			    new TypeInsnNode( Opcodes.NEW, Type.getInternalName( BigDecimal.class ) ),
			    new InsnNode( Opcodes.DUP ),
			    new LdcInsnNode( literal.getValue() ),
			    new MethodInsnNode( Opcodes.INVOKESPECIAL,
			        Type.getInternalName( BigDecimal.class ),
			        "<init>",
			        Type.getMethodDescriptor( Type.VOID_TYPE, Type.getType( String.class ) ),
			        false
			    )
			);
		}

		return List.of( new LdcInsnNode( Double.valueOf( literal.getValue() ) ), new MethodInsnNode( Opcodes.INVOKESTATIC,
		    Type.getInternalName( Double.class ),
		    "valueOf",
		    Type.getMethodDescriptor( Type.getType( Double.class ), Type.DOUBLE_TYPE ),
		    false ) );
	}
}
