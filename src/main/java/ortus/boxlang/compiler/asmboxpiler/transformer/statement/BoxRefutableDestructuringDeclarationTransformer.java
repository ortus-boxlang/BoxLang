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
package ortus.boxlang.compiler.asmboxpiler.transformer.statement;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
import ortus.boxlang.compiler.asmboxpiler.transformer.ReturnValueContext;
import ortus.boxlang.compiler.asmboxpiler.transformer.TransformerContext;
import ortus.boxlang.compiler.asmboxpiler.transformer.expression.BoxMatchExpressionTransformer;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.statement.BoxRefutableDestructuringDeclaration;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.MatchExpression;
import ortus.boxlang.runtime.dynamic.MatchExpression.Pattern;

public class BoxRefutableDestructuringDeclarationTransformer extends BoxMatchExpressionTransformer {

	public BoxRefutableDestructuringDeclarationTransformer( Transpiler transpiler ) {
		super( transpiler );
	}

	@Override
	public List<AbstractInsnNode> transform( BoxNode node, TransformerContext context, ReturnValueContext returnContext ) throws IllegalStateException {
		BoxRefutableDestructuringDeclaration	declaration	= ( BoxRefutableDestructuringDeclaration ) node;
		Optional<MethodContextTracker>			tracker		= transpiler.getCurrentMethodContextTracker();
		List<AbstractInsnNode>					nodes		= new ArrayList<>();

		declaration.validatePatternHasBindingTarget();

		tracker.ifPresent( t -> nodes.addAll( t.loadCurrentContext() ) );
		nodes.addAll( transpiler.transform( declaration.getSubject(), TransformerContext.NONE, ReturnValueContext.VALUE ) );
		nodes.addAll( buildPatternNodes( declaration.getPattern(), true ) );
		nodes.add( new LdcInsnNode( declaration.isLocalDeclaration() ? 1 : 0 ) );
		nodes.add( new LdcInsnNode( declaration.isFinalDeclaration() ? 1 : 0 ) );
		nodes.add( new MethodInsnNode(
		    Opcodes.INVOKESTATIC,
		    Type.getInternalName( MatchExpression.class ),
		    "declare",
		    Type.getMethodDescriptor( Type.BOOLEAN_TYPE, Type.getType( IBoxContext.class ), Type.getType( Object.class ), Type.getType( Pattern.class ),
		        Type.BOOLEAN_TYPE, Type.BOOLEAN_TYPE ),
		    false ) );

		LabelNode	successLabel	= new LabelNode();
		LabelNode	endLabel		= new LabelNode();
		nodes.add( new JumpInsnNode( Opcodes.IFNE, successLabel ) );
		nodes.addAll( transpiler.transform( declaration.getElseBody(), TransformerContext.NONE, returnContext ) );
		nodes.add( new JumpInsnNode( Opcodes.GOTO, endLabel ) );
		nodes.add( successLabel );
		if ( returnContext == ReturnValueContext.VALUE || returnContext == ReturnValueContext.VALUE_OR_NULL ) {
			nodes.add( new InsnNode( Opcodes.ACONST_NULL ) );
		}
		nodes.add( endLabel );

		return AsmHelper.addLineNumberLabels( nodes, node );
	}
}