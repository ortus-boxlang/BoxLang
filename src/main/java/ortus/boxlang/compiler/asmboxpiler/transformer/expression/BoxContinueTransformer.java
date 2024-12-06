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
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;

import ortus.boxlang.compiler.asmboxpiler.AsmHelper;
import ortus.boxlang.compiler.asmboxpiler.MethodContextTracker;
import ortus.boxlang.compiler.asmboxpiler.Transpiler;
import ortus.boxlang.compiler.asmboxpiler.transformer.AbstractTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.ReturnValueContext;
import ortus.boxlang.compiler.asmboxpiler.transformer.TransformerContext;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.expression.BoxClosure;
import ortus.boxlang.compiler.ast.expression.BoxLambda;
import ortus.boxlang.compiler.ast.statement.BoxContinue;
import ortus.boxlang.compiler.ast.statement.BoxDo;
import ortus.boxlang.compiler.ast.statement.BoxForIn;
import ortus.boxlang.compiler.ast.statement.BoxForIndex;
import ortus.boxlang.compiler.ast.statement.BoxFunctionDeclaration;
import ortus.boxlang.compiler.ast.statement.BoxSwitch;
import ortus.boxlang.compiler.ast.statement.BoxWhile;
import ortus.boxlang.compiler.ast.statement.component.BoxComponent;
import ortus.boxlang.runtime.components.Component;

public class BoxContinueTransformer extends AbstractTransformer {

	public BoxContinueTransformer( Transpiler transpiler ) {
		super( transpiler );
	}

	@Override
	public List<AbstractInsnNode> transform( BoxNode node, TransformerContext context, ReturnValueContext returnContext ) throws IllegalStateException {
		BoxContinue				continueNode	= ( BoxContinue ) node;
		ExitsAllowed			exitsAllowed	= getExitsAllowed( node );

		List<AbstractInsnNode>	nodes			= new ArrayList<AbstractInsnNode>();
		AsmHelper.addDebugLabel( nodes, "BoxContinue" );

		if ( returnContext.nullable || exitsAllowed.equals( ExitsAllowed.FUNCTION ) ) {
			nodes.add( new InsnNode( Opcodes.ACONST_NULL ) );
		}

		// if ( returnContext == ReturnValueContext.VALUE || returnContext == ReturnValueContext.VALUE_OR_NULL || exitsAllowed.equals( ExitsAllowed.FUNCTION ) ) {
		// nodes.add( new InsnNode( Opcodes.ACONST_NULL ) );
		// }

		MethodContextTracker	tracker			= transpiler.getCurrentMethodContextTracker().get();
		BoxNode					labelTarget		= tracker.getStringLabel( continueNode.getLabel() );
		LabelNode				currentBreak	= tracker.getContinue( labelTarget != null ? labelTarget : getTargetAncestor( continueNode ) );

		if ( currentBreak != null ) {
			if ( returnContext.nullable && nodes.size() == 0 ) {
				nodes.add( new InsnNode( Opcodes.ACONST_NULL ) );
			}
			nodes.add( new JumpInsnNode( Opcodes.GOTO, currentBreak ) );
			return nodes;
		}

		if ( exitsAllowed.equals( ExitsAllowed.COMPONENT ) ) {
			nodes.add( new InsnNode( Opcodes.ACONST_NULL ) );
			nodes.add( new MethodInsnNode( Opcodes.INVOKESTATIC,
			    Type.getInternalName( Component.BodyResult.class ),
			    "ofContinue",
			    Type.getMethodDescriptor( Type.getType( Component.BodyResult.class ), Type.getType( String.class ) ),
			    false )
			);
			nodes.add( new InsnNode( Opcodes.ARETURN ) );
			return nodes;
		} else if ( exitsAllowed.equals( ExitsAllowed.LOOP ) ) {
			nodes.add( new JumpInsnNode( Opcodes.GOTO, currentBreak ) );
			return nodes;
			// template = "if(true) break " + breakLabel + ";";
		} else if ( exitsAllowed.equals( ExitsAllowed.FUNCTION ) ) {
			nodes.add( new InsnNode( Opcodes.ARETURN ) );
			return nodes;
		}

		throw new RuntimeException( "Cannot continue from current location - processing: " + transpiler.getProperty( "relativePath" ) );

	}

	@SuppressWarnings( "unchecked" )
	public BoxNode getTargetAncestor( BoxNode node ) {
		return node.getFirstNodeOfTypes( BoxFunctionDeclaration.class, BoxClosure.class, BoxLambda.class, BoxComponent.class, BoxDo.class,
		    BoxForIndex.class, BoxForIn.class,
		    BoxWhile.class, BoxSwitch.class );
	}

	private boolean isLoop( BoxNode node ) {
		return node instanceof BoxForIndex;
	}
}
