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

import ortus.boxlang.compiler.asmboxpiler.AsmHelper;
import ortus.boxlang.compiler.asmboxpiler.AsmTranspiler;
import ortus.boxlang.compiler.asmboxpiler.transformer.AbstractTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.ReturnValueContext;
import ortus.boxlang.compiler.asmboxpiler.transformer.TransformerContext;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.expression.BoxFunctionInvocation;
import ortus.boxlang.runtime.scopes.Key;

public class BoxFunctionInvocationTransformer extends AbstractTransformer {

	public BoxFunctionInvocationTransformer( AsmTranspiler transpiler ) {
		super( transpiler );
	}

	@Override
	public List<AbstractInsnNode> transform( BoxNode node, TransformerContext context, ReturnValueContext returnContext ) throws IllegalStateException {
		BoxFunctionInvocation	function	= ( BoxFunctionInvocation ) node;
		boolean					safe		= function.getName().equalsIgnoreCase( "isnull" ) ? true : false;

		List<AbstractInsnNode>	nodes		= new ArrayList<>();
		nodes.addAll( transpiler.getCurrentMethodContextTracker().get().loadCurrentContext() );

		// INCLUSIVE timing: bracket the invocation so the call-site span records how
		// long the call was on the stack (argument evaluation happens inside the call
		// helper below, so this measures args + the call — per the agreed design).
		// Additive only: it does NOT disturb the flat self-time probe chain. The call's
		// atomic SHELL members (head `callee(` + tail `)`) are charged the same time so
		// the `)` reports the SAME on-stack time as the head.
		int		callSpan	= transpiler.callSpanIdOf( function );
		int[]	shell		= callSpan >= 0 ? transpiler.peekCallShell( callSpan ) : null;
		nodes.addAll( transpiler.emitMarkEnter( callSpan, shell ) );

		TransformerContext argContext = safe ? TransformerContext.SAFE : context;
		nodes.addAll( AsmHelper.callinvokeFunction( transpiler, Type.getType( Key.class ), function.getArguments(), transpiler.createKey( function.getName() ),
		    argContext, safe, function.isNamedArgs(), function.hasSpread() ) );

		if ( callSpan >= 0 ) {
			nodes.addAll( transpiler.emitMarkExit() );
		}

		if ( returnContext.empty ) {
			nodes.add( new InsnNode( Opcodes.POP ) );
		}

		return AsmHelper.addLineNumberLabels( nodes, node );
	}
}
