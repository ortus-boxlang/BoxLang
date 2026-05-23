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
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

import ortus.boxlang.compiler.asmboxpiler.AsmHelper;
import ortus.boxlang.compiler.asmboxpiler.AsmTranspiler;
import ortus.boxlang.compiler.asmboxpiler.transformer.AbstractTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.ReturnValueContext;
import ortus.boxlang.compiler.asmboxpiler.transformer.TransformerContext;
import ortus.boxlang.compiler.ast.BoxExpression;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.expression.BoxSetLiteral;
import ortus.boxlang.compiler.ast.expression.BoxSpreadExpression;
import ortus.boxlang.runtime.dynamic.LiteralSpreadUtil;
import ortus.boxlang.runtime.types.BoxSet;

/**
 * Emits bytecode for {@code set{...}} / {@code set<variant>{...}} literals.
 *
 * <p>
 * The generated sequence pushes (variant-String, Object[] values) and invokes
 * {@code LiteralSpreadUtil.set(String, Object...)} which constructs a
 * {@link BoxSet} of the requested variant.
 */
public class BoxSetLiteralTransformer extends AbstractTransformer {

	public BoxSetLiteralTransformer( AsmTranspiler transpiler ) {
		super( transpiler );
	}

	@Override
	public List<AbstractInsnNode> transform( BoxNode node, TransformerContext context, ReturnValueContext returnContext ) throws IllegalStateException {
		BoxSetLiteral			setLiteral	= ( BoxSetLiteral ) node;
		List<AbstractInsnNode>	nodes		= new ArrayList<>();

		// Push the variant string (or null)
		if ( setLiteral.getVariant() == null ) {
			nodes.add( new InsnNode( Opcodes.ACONST_NULL ) );
		} else {
			nodes.add( new LdcInsnNode( setLiteral.getVariant() ) );
		}

		// Push an Object[] of the values (with spread expansion applied via LiteralSpreadUtil.spread())
		nodes.addAll( AsmHelper.array( Type.getType( Object.class ), setLiteral.getValues(),
		    ( value, i ) -> transformSetMember( value, context ) ) );

		nodes.add( new MethodInsnNode( Opcodes.INVOKESTATIC,
		    Type.getInternalName( LiteralSpreadUtil.class ),
		    "set",
		    Type.getMethodDescriptor( Type.getType( BoxSet.class ), Type.getType( String.class ), Type.getType( Object[].class ) ),
		    false ) );
		return nodes;
	}

	private List<AbstractInsnNode> transformSetMember( BoxExpression value, TransformerContext context ) {
		if ( value instanceof BoxSpreadExpression spread ) {
			List<AbstractInsnNode> spreadNodes = new ArrayList<>( transpiler.transform( spread.getExpression(), context, ReturnValueContext.VALUE_OR_NULL ) );
			spreadNodes.add(
			    new MethodInsnNode( Opcodes.INVOKESTATIC,
			        Type.getInternalName( LiteralSpreadUtil.class ),
			        "spread",
			        Type.getMethodDescriptor( Type.getType( LiteralSpreadUtil.SpreadValue.class ), Type.getType( Object.class ) ),
			        false ) );
			return spreadNodes;
		}
		return transpiler.transform( value, context, ReturnValueContext.VALUE_OR_NULL );
	}
}
