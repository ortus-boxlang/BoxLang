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

import ortus.boxlang.compiler.asmboxpiler.AsmHelper;
import ortus.boxlang.compiler.asmboxpiler.Transpiler;
import ortus.boxlang.compiler.asmboxpiler.transformer.AbstractTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.ReturnValueContext;
import ortus.boxlang.compiler.asmboxpiler.transformer.TransformerContext;
import ortus.boxlang.compiler.ast.BoxExpression;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.expression.BoxStringConcat;
import ortus.boxlang.compiler.ast.expression.BoxStringLiteral;
import ortus.boxlang.runtime.operators.Concat;

public class BoxStringConcatTransformer extends AbstractTransformer {

	public BoxStringConcatTransformer( Transpiler transpiler ) {
		super( transpiler );
	}

	@Override
	public List<AbstractInsnNode> transform( BoxNode node, TransformerContext context, ReturnValueContext returnContext ) throws IllegalStateException {
		BoxStringConcat		interpolation	= ( BoxStringConcat ) node;
		List<BoxExpression>	optimized		= optimizeStringLiterals( interpolation.getValues() );

		if ( optimized.size() == 1 ) {
			List<AbstractInsnNode> nodes = new ArrayList<>();
			nodes.addAll( transpiler.transform( optimized.get( 0 ), TransformerContext.NONE, ReturnValueContext.VALUE ) );
			return AsmHelper.addLineNumberLabels( nodes, node );
		} else {
			List<AbstractInsnNode> nodes = new ArrayList<>();
			nodes.addAll( AsmHelper.array( Type.getType( Object.class ), optimized,
			    ( value, i ) -> transpiler.transform( value, TransformerContext.NONE, ReturnValueContext.VALUE ) ) );
			nodes.add( new MethodInsnNode( Opcodes.INVOKESTATIC,
			    Type.getInternalName( Concat.class ),
			    "invoke",
			    Type.getMethodDescriptor( Type.getType( String.class ), Type.getType( Object[].class ) ),
			    false ) );
			return AsmHelper.addLineNumberLabels( nodes, node );
		}
	}

	/**
	 * Optimizes a list of expressions by combining contiguous string literals.
	 * For example: ["foo", "bar", var, "baz", "qux"] becomes ["foobar", var, "bazqux"]
	 *
	 * @param values the list of expressions to optimize
	 * 
	 * @return an optimized list with contiguous string literals combined
	 */
	private List<BoxExpression> optimizeStringLiterals( List<BoxExpression> values ) {
		if ( values.isEmpty() ) {
			return values;
		}

		List<BoxExpression>	result				= new ArrayList<>();
		StringBuilder		combinedString		= new StringBuilder();
		BoxExpression		firstLiteralNode	= null;

		for ( BoxExpression value : values ) {
			if ( value instanceof BoxStringLiteral literal ) {
				// Combine all string literals (including empty ones)
				if ( firstLiteralNode == null ) {
					firstLiteralNode = value;
				}
				combinedString.append( literal.getValue() );
			} else {
				// Non-literal expression found, flush any accumulated string (even if empty)
				if ( firstLiteralNode != null ) {
					result.add( new BoxStringLiteral( combinedString.toString(), firstLiteralNode.getPosition(), firstLiteralNode.getSourceText() ) );
					combinedString		= new StringBuilder();
					firstLiteralNode	= null;
				}
				result.add( value );
			}
		}

		// Flush any remaining combined string
		if ( combinedString.length() > 0 ) {
			result.add( new BoxStringLiteral( combinedString.toString(), firstLiteralNode.getPosition(), firstLiteralNode.getSourceText() ) );
		}

		return result;
	}
}
