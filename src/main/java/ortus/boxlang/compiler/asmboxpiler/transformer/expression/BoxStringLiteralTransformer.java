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

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

import ortus.boxlang.compiler.asmboxpiler.AsmHelper;
import ortus.boxlang.compiler.asmboxpiler.Transpiler;
import ortus.boxlang.compiler.asmboxpiler.transformer.AbstractTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.ReturnValueContext;
import ortus.boxlang.compiler.asmboxpiler.transformer.TransformerContext;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.expression.BoxStringLiteral;

public class BoxStringLiteralTransformer extends AbstractTransformer {

	private static final int MAX_LITERAL_LENGTH = 30000; // 64KB limit

	public BoxStringLiteralTransformer( Transpiler transpiler ) {
		super( transpiler );
	}

	@Override
	public List<AbstractInsnNode> transform( BoxNode node, TransformerContext context, ReturnValueContext returnContext ) throws IllegalStateException {
		BoxStringLiteral		literal	= ( BoxStringLiteral ) node;

		String					value	= literal.getValue();
		List<AbstractInsnNode>	nodes	= new ArrayList<AbstractInsnNode>();

		if ( value.length() < MAX_LITERAL_LENGTH ) {
			nodes.add( new LdcInsnNode( literal.getValue() ) );

			if ( returnContext != ReturnValueContext.VALUE && returnContext != ReturnValueContext.VALUE_OR_NULL ) {
				nodes.add( new InsnNode( Opcodes.POP ) );
			}

			return nodes;
			// return AsmHelper.addLineNumberLabels( nodes, node );

		}
		List<String> parts = splitStringIntoParts( value );

		nodes.add( new LdcInsnNode( "" ) );
		nodes.addAll(
		    AsmHelper.array( Type.getType( String.class ), parts.stream().map( s -> {
			    List<AbstractInsnNode> x = List.of( new LdcInsnNode( s ) );

			    return x;
		    }
		    ).toList() ) );

		nodes.add( new MethodInsnNode(
		    Opcodes.INVOKESTATIC,
		    Type.getInternalName( String.class ),
		    "join",
		    Type.getMethodDescriptor( Type.getType( String.class ),
		        Type.getType( CharSequence.class ),
		        Type.getType( CharSequence[].class )
		    ),
		    false )
		);

		if ( returnContext != ReturnValueContext.VALUE && returnContext != ReturnValueContext.VALUE_OR_NULL ) {
			nodes.add( new InsnNode( Opcodes.POP ) );
		}

		return nodes;
		// return AsmHelper.addLineNumberLabels( nodes, node );
	}

	/**
	 * Split a large string into parts
	 *
	 * @param str The input string.
	 * 
	 * @return A list of StringLiteralExpr parts.
	 **/
	private List<String> splitStringIntoParts( String str ) {
		List<String>	parts	= new ArrayList<>();
		int				length	= str.length();
		for ( int i = 0; i < length; i += MAX_LITERAL_LENGTH ) {
			int		end		= Math.min( length, i + MAX_LITERAL_LENGTH );
			String	part	= str.substring( i, end );
			parts.add( part );
		}
		return parts;
	}
}
