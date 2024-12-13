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

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnNode;

import javassist.bytecode.Opcode;
import ortus.boxlang.compiler.asmboxpiler.AsmHelper;
import ortus.boxlang.compiler.asmboxpiler.Transpiler;
import ortus.boxlang.compiler.asmboxpiler.transformer.AbstractTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.ReturnValueContext;
import ortus.boxlang.compiler.asmboxpiler.transformer.TransformerContext;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.statement.BoxStatementBlock;

public class BoxStatementBlockTransformer extends AbstractTransformer {

	public BoxStatementBlockTransformer( Transpiler transpiler ) {
		super( transpiler );
	}

	@Override
	public List<AbstractInsnNode> transform( BoxNode node, TransformerContext context, ReturnValueContext returnContext ) throws IllegalStateException {
		BoxStatementBlock		boxStatementBlock	= ( BoxStatementBlock ) node;
		List<AbstractInsnNode>	nodes				= new ArrayList<AbstractInsnNode>();

		AsmHelper.addDebugLabel( nodes, "BoxStatementBlock" );

		if ( boxStatementBlock.getBody().size() == 0 && ReturnValueContext.VALUE_OR_NULL == returnContext ) {
			nodes.add( new InsnNode( Opcode.ACONST_NULL ) );
			return AsmHelper.addLineNumberLabels( nodes, node );
		}

		nodes.addAll( AsmHelper.transformBodyExpressions( transpiler, boxStatementBlock.getBody(), context, returnContext ) );

		return AsmHelper.addLineNumberLabels( nodes, node );
	}
}
