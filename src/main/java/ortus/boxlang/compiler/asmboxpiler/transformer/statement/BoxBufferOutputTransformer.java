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
package ortus.boxlang.compiler.asmboxpiler.transformer.statement;

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

import ortus.boxlang.compiler.asmboxpiler.AsmHelper;
import ortus.boxlang.compiler.asmboxpiler.Transpiler;
import ortus.boxlang.compiler.asmboxpiler.transformer.AbstractTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.ReturnValueContext;
import ortus.boxlang.compiler.asmboxpiler.transformer.TransformerContext;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.statement.BoxBufferOutput;
import ortus.boxlang.runtime.context.IBoxContext;

public class BoxBufferOutputTransformer extends AbstractTransformer {

	public BoxBufferOutputTransformer( Transpiler transpiler ) {
		super( transpiler );
	}

	@Override
	public List<AbstractInsnNode> transform( BoxNode node, TransformerContext context, ReturnValueContext returnContext ) throws IllegalStateException {
		BoxBufferOutput			bufferOuput	= ( BoxBufferOutput ) node;

		List<AbstractInsnNode>	nodes		= new ArrayList<>();
		nodes.addAll( transpiler.getCurrentMethodContextTracker().get().loadCurrentContext() );
		nodes
		    .addAll( transpiler.transform( bufferOuput.getExpression(), TransformerContext.NONE, ReturnValueContext.VALUE_OR_NULL ) );
		nodes.add( new MethodInsnNode( Opcodes.INVOKEINTERFACE,
		    Type.getInternalName( IBoxContext.class ),
		    "writeToBuffer",
		    Type.getMethodDescriptor( Type.getType( IBoxContext.class ), Type.getType( Object.class ) ),
		    true ) );

		if ( returnContext != ReturnValueContext.VALUE && returnContext != ReturnValueContext.VALUE_OR_NULL ) {
			nodes.add( new InsnNode( Opcodes.POP ) );
		}

		AsmHelper.addDebugLabel( nodes, "BoxBufferOutput - end" );

		return AsmHelper.addLineNumberLabels( nodes, node );
	}
}
