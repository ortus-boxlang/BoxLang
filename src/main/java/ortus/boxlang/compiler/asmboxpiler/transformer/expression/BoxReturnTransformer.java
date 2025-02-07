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
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import ortus.boxlang.compiler.asmboxpiler.AsmHelper;
import ortus.boxlang.compiler.asmboxpiler.MethodContextTracker;
import ortus.boxlang.compiler.asmboxpiler.Transpiler;
import ortus.boxlang.compiler.asmboxpiler.transformer.AbstractTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.ReturnValueContext;
import ortus.boxlang.compiler.asmboxpiler.transformer.TransformerContext;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.statement.BoxFunctionDeclaration;
import ortus.boxlang.compiler.ast.statement.BoxReturn;
import ortus.boxlang.compiler.ast.statement.component.BoxComponent;
import ortus.boxlang.runtime.components.Component;

public class BoxReturnTransformer extends AbstractTransformer {

	public BoxReturnTransformer( Transpiler transpiler ) {
		super( transpiler );
	}

	@Override
	public List<AbstractInsnNode> transform( BoxNode node, TransformerContext context, ReturnValueContext returnContext ) throws IllegalStateException {
		BoxReturn				boxReturn	= ( BoxReturn ) node;
		MethodContextTracker	tracker		= transpiler.getCurrentMethodContextTracker().get();

		List<AbstractInsnNode>	nodes		= new ArrayList<>();

		if ( !transpiler.canReturn() ) {
			nodes.add( new InsnNode( Opcodes.RETURN ) );
			if ( returnContext.nullable ) {
				nodes.add( new InsnNode( Opcodes.ARETURN ) );
			}
			return AsmHelper.addLineNumberLabels( nodes, node );
		}

		BoxNode	firstFound		= node.getFirstNodeOfTypes( BoxFunctionDeclaration.class, BoxComponent.class );
		boolean	preferFunction	= firstFound instanceof BoxFunctionDeclaration;

		if ( boxReturn.getExpression() == null ) {
			nodes.add( new InsnNode( Opcodes.ACONST_NULL ) );
		} else {
			nodes.addAll( transpiler.transform( boxReturn.getExpression(), TransformerContext.NONE, ReturnValueContext.VALUE_OR_NULL ) );
		}

		if ( transpiler.isInsideComponent() && !preferFunction ) {
			nodes.add(
			    new MethodInsnNode(
			        Opcodes.INVOKESTATIC,
			        Type.getInternalName( Component.BodyResult.class ),
			        "ofReturn",
			        Type.getMethodDescriptor( Type.getType( Component.BodyResult.class ), Type.getType( Object.class ) ),
			        false
			    )
			);
		}

		// store our return value (null or an expression) before we evaluate our finally blocks
		var varStore = tracker.storeNewVariable( Opcodes.ASTORE );
		nodes.addAll( varStore.nodes() );

		// start iterating through our return blocks
		var reversed = tracker.getFinallyBodies().stream().collect( Collectors.toList() );

		Collections.reverse( reversed );

		var returnDepth = tracker.getReturnDepth();
		tracker.incrementReturnDepth();

		reversed.stream().skip( returnDepth ).forEach( finallyBody -> {
			nodes.addAll( AsmHelper.transformBodyExpressions(
			    transpiler,
			    finallyBody,
			    context,
			    returnContext
			) );
		} );

		tracker.decrementReturnDepth();
		// finish iterating through return blocks

		// load our original value to return
		nodes.add( new VarInsnNode( Opcodes.ALOAD, varStore.index() ) );
		nodes.add( new InsnNode( Opcodes.ARETURN ) );

		return AsmHelper.addLineNumberLabels( nodes, node );
	}

}
