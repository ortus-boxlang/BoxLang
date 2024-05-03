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

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import ortus.boxlang.compiler.asmboxpiler.AsmHelper;
import ortus.boxlang.compiler.asmboxpiler.Transpiler;
import ortus.boxlang.compiler.asmboxpiler.transformer.AbstractTransformer;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.expression.BoxIdentifier;
import ortus.boxlang.compiler.ast.expression.BoxMethodInvocation;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.Referencer;
import ortus.boxlang.runtime.scopes.Key;

import java.util.ArrayList;
import java.util.List;

public class BoxMethodInvocationTransformer extends AbstractTransformer {

	public BoxMethodInvocationTransformer( Transpiler transpiler ) {
		super( transpiler );
	}

	@Override
	public List<AbstractInsnNode> transform( BoxNode node ) throws IllegalStateException {
		BoxMethodInvocation		invocation	= ( BoxMethodInvocation ) node;

		List<AbstractInsnNode>	nodes		= new ArrayList<>();

		nodes.add( new VarInsnNode( Opcodes.ALOAD, 1 ) );
		nodes.addAll( transpiler.transform( invocation.getObj() ) );

		if ( invocation.getUsedDotAccess() ) {
			nodes.addAll( transpiler.createKey( ( ( BoxIdentifier ) invocation.getName() ).getName() ) );
		} else {
			nodes.addAll( transpiler.createKey( invocation.getName() ) );
		}

		nodes.addAll( AsmHelper.array( Type.getType( Object.class ), invocation.getArguments(), ( argument, i ) -> transpiler.transform( argument ) ) );

		nodes.add( new FieldInsnNode( Opcodes.GETSTATIC,
		    Type.getInternalName( Boolean.class ),
		    "FALSE",
		    Type.getDescriptor( Boolean.class ) ) ); // TODO: "safe"

		nodes.add( new MethodInsnNode( Opcodes.INVOKESTATIC,
		    Type.getInternalName( Referencer.class ),
		    "getAndInvoke",
		    Type.getMethodDescriptor( Type.getType( Object.class ),
		        Type.getType( IBoxContext.class ),
		        Type.getType( Object.class ),
		        Type.getType( Key.class ),
		        Type.getType( Object[].class ),
		        Type.getType( Boolean.class ) ),
		    false ) );

		return nodes;
	}
}
