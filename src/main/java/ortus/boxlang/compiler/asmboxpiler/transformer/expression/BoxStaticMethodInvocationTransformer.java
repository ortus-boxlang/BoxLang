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
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

import ortus.boxlang.compiler.asmboxpiler.AsmHelper;
import ortus.boxlang.compiler.asmboxpiler.Transpiler;
import ortus.boxlang.compiler.asmboxpiler.transformer.AbstractTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.ReturnValueContext;
import ortus.boxlang.compiler.asmboxpiler.transformer.TransformerContext;
import ortus.boxlang.compiler.ast.BoxExpression;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.expression.BoxFQN;
import ortus.boxlang.compiler.ast.expression.BoxIdentifier;
import ortus.boxlang.compiler.ast.expression.BoxStaticMethodInvocation;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.runnables.BoxClassSupport;

public class BoxStaticMethodInvocationTransformer extends AbstractTransformer {

	public BoxStaticMethodInvocationTransformer( Transpiler transpiler ) {
		super( transpiler );
	}

	@Override
	public List<AbstractInsnNode> transform( BoxNode node, TransformerContext context, ReturnValueContext returnValueContext ) throws IllegalStateException {
		BoxStaticMethodInvocation	invocation	= ( BoxStaticMethodInvocation ) node;
		List<AbstractInsnNode>		nodes		= new ArrayList<>();
		BoxExpression				baseObject	= invocation.getObj();
		// Expression expr;

		transpiler.getCurrentMethodContextTracker().ifPresent( t -> nodes.addAll( t.loadCurrentContext() ) );

		nodes.add( new InsnNode( Opcodes.DUP ) );

		if ( baseObject instanceof BoxFQN fqn ) {
			nodes.add( new LdcInsnNode( fqn.getValue() ) );
		} else if ( baseObject instanceof BoxIdentifier id ) {
			nodes.addAll( transpiler.transform( id, context, ReturnValueContext.VALUE ) );
		} else {
			nodes.addAll( transpiler.transform( baseObject, context, ReturnValueContext.VALUE ) );
			// throw new ExpressionException( "Unexpected base token in static method access.", baseObject );
		}

		nodes.add( new FieldInsnNode( Opcodes.GETSTATIC, transpiler.getProperty( "classTypeInternal" ), "imports",
		    Type.getDescriptor( List.class ) ) );

		// invoke BoxClassSupport.ensureClass(${contextName},${scopeReference},imports),
		nodes.add( new MethodInsnNode(
		    Opcodes.INVOKESTATIC,
		    Type.getInternalName( BoxClassSupport.class ),
		    "ensureClass",
		    Type.getMethodDescriptor( Type.getType( DynamicObject.class ),
		        Type.getType( IBoxContext.class ),
		        Type.getType( Object.class ),
		        Type.getType( List.class )
		    ),
		    false )
		);

		nodes.addAll( AsmHelper.callReferencerGetAndInvoke( transpiler, invocation.getArguments(), invocation.getName().toString(), context, false ) );

		return AsmHelper.addLineNumberLabels( nodes, node );
	}
}
