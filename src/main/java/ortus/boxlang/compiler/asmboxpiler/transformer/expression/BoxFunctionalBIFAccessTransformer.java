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
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.expression.BoxFunctionalBIFAccess;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.FunctionalBIFAccess;

public class BoxFunctionalBIFAccessTransformer extends AbstractTransformer {

	public BoxFunctionalBIFAccessTransformer( Transpiler transpiler ) {
		super( transpiler );
	}

	@Override
	public List<AbstractInsnNode> transform( BoxNode node, TransformerContext context, ReturnValueContext returnContext ) throws IllegalStateException {
		BoxFunctionalBIFAccess	BIFAccess	= ( BoxFunctionalBIFAccess ) node;

		List<AbstractInsnNode>	nodes		= new ArrayList<>();

		// nodes.addAll( transpiler.getCurrentMethodContextTracker().get().loadCurrentContext() );

		nodes.addAll( transpiler.createKey( BIFAccess.getName() ) );

		nodes.add( new MethodInsnNode(
		    Opcodes.INVOKESTATIC,
		    Type.getInternalName( FunctionalBIFAccess.class ),
		    "of",
		    Type.getMethodDescriptor( Type.getType( FunctionalBIFAccess.class ),
		        Type.getType( Key.class ) ),
		    false ) );

		return AsmHelper.addLineNumberLabels( nodes, node );
	}
}