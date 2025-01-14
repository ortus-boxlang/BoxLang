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
import org.objectweb.asm.tree.LdcInsnNode;
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
import ortus.boxlang.compiler.ast.statement.BoxBreak;
import ortus.boxlang.compiler.ast.statement.BoxDo;
import ortus.boxlang.compiler.ast.statement.BoxForIn;
import ortus.boxlang.compiler.ast.statement.BoxForIndex;
import ortus.boxlang.compiler.ast.statement.BoxFunctionDeclaration;
import ortus.boxlang.compiler.ast.statement.BoxSwitch;
import ortus.boxlang.compiler.ast.statement.BoxWhile;
import ortus.boxlang.compiler.ast.statement.component.BoxComponent;
import ortus.boxlang.runtime.components.Component;

public class BoxBreakTransformer extends AbstractTransformer {

	public BoxBreakTransformer( Transpiler transpiler ) {
		super( transpiler );
	}

	@Override
	public List<AbstractInsnNode> transform( BoxNode node, TransformerContext context, ReturnValueContext returnContext ) throws IllegalStateException {
		BoxBreak				breakNode		= ( BoxBreak ) node;
		ExitsAllowed			exitsAllowed	= getExitsAllowed( node );

		MethodContextTracker	tracker			= transpiler.getCurrentMethodContextTracker().get();
		List<AbstractInsnNode>	nodes			= new ArrayList<AbstractInsnNode>();

		BoxNode					labelTarget		= tracker.getStringLabel( breakNode.getLabel() );
		if ( labelTarget == null ) {
			labelTarget = getTargetAncestor( breakNode );
		}

		if ( returnContext.nullable
		    || exitsAllowed.equals( ExitsAllowed.FUNCTION ) ) {
			nodes.add( new InsnNode( Opcodes.ACONST_NULL ) );
		}

		LabelNode currentBreak = tracker.getBreak( labelTarget != null ? labelTarget : getTargetAncestor( breakNode ) );

		if ( currentBreak != null ) {
			if ( returnContext.nullable && nodes.size() == 0 ) {
				nodes.add( new InsnNode( Opcodes.ACONST_NULL ) );
			}
			nodes.add( new JumpInsnNode( Opcodes.GOTO, currentBreak ) );
			return AsmHelper.addLineNumberLabels( nodes, node );
		}

		if ( exitsAllowed.equals( ExitsAllowed.COMPONENT ) ) {
			nodes.add( new LdcInsnNode( "" ) );
			nodes.add( new MethodInsnNode( Opcodes.INVOKESTATIC,
			    Type.getInternalName( Component.BodyResult.class ),
			    "ofBreak",
			    Type.getMethodDescriptor( Type.getType( Component.BodyResult.class ), Type.getType( String.class ) ),
			    false )
			);
			nodes.add( new InsnNode( Opcodes.ARETURN ) );
			return AsmHelper.addLineNumberLabels( nodes, node );
		} else if ( exitsAllowed.equals( ExitsAllowed.LOOP ) ) {
			nodes.add( new InsnNode( transpiler.canReturn() ? Opcodes.ARETURN : Opcodes.RETURN ) );
			return AsmHelper.addLineNumberLabels( nodes, node );
		} else if ( exitsAllowed.equals( ExitsAllowed.FUNCTION ) ) {
			nodes.add( new InsnNode( Opcodes.ARETURN ) );
			return AsmHelper.addLineNumberLabels( nodes, node );
		}

		throw new RuntimeException( "Cannot break from current location" );

	}

	public BoxNode getTargetAncestor( BoxNode node ) {
		return node.getFirstNodeOfTypes( BoxSwitch.class, BoxFunctionDeclaration.class, BoxClosure.class, BoxLambda.class, BoxComponent.class, BoxDo.class,
		    BoxForIndex.class, BoxForIn.class,
		    BoxWhile.class );
	}

	public int countIntermediateLoops( BoxNode target, BoxBreak breakNode ) {
		int		count	= 0;

		BoxNode	parent	= breakNode.getParent();

		while ( parent != target ) {
			if ( parent instanceof BoxWhile || parent instanceof BoxDo || parent instanceof BoxForIn || parent instanceof BoxForIndex ) {
				count++;
			}

			parent = parent.getParent();
		}

		return count;
	}
}
