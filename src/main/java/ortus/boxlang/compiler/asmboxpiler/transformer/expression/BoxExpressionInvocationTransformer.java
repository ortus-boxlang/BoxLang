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
import org.objectweb.asm.tree.VarInsnNode;

import ortus.boxlang.compiler.asmboxpiler.AsmHelper;
import ortus.boxlang.compiler.asmboxpiler.AsmTranspiler;
import ortus.boxlang.compiler.asmboxpiler.transformer.AbstractTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.ReturnValueContext;
import ortus.boxlang.compiler.asmboxpiler.transformer.TransformerContext;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.expression.BoxExpressionInvocation;
import ortus.boxlang.compiler.ast.expression.BoxLambda;
import ortus.boxlang.runtime.scopes.Key;

public class BoxExpressionInvocationTransformer extends AbstractTransformer {

	public BoxExpressionInvocationTransformer( AsmTranspiler transpiler ) {
		super( transpiler );
	}

	@Override
	public List<AbstractInsnNode> transform( BoxNode node, TransformerContext context, ReturnValueContext returnContext ) throws IllegalStateException {
		BoxExpressionInvocation	invocation	= ( BoxExpressionInvocation ) node;

		List<AbstractInsnNode>	nameNodes	= transpiler.transform( invocation.getExpr(), context, ReturnValueContext.VALUE );

		List<AbstractInsnNode>	nodes		= new ArrayList<>();
		nodes.add( new VarInsnNode( Opcodes.ALOAD, 1 ) );

		Type invokeType = invocation.getExpr() instanceof BoxLambda || invocation.getExpr().getDescendantsOfType( BoxLambda.class ).size() > 0
		    ? Type.getType( Object.class )
		    : Type.getType( Key.class );

		nodes.addAll( AsmHelper.callinvokeFunction( transpiler, invokeType, invocation.getArguments(), nameNodes, context, false ) );

		if ( returnContext.empty ) {
			nodes.add( new InsnNode( Opcodes.POP ) );
		}

		return nodes;
	}
}
