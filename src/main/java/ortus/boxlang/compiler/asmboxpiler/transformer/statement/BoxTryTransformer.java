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

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.VarInsnNode;

import ortus.boxlang.compiler.asmboxpiler.Transpiler;
import ortus.boxlang.compiler.asmboxpiler.transformer.AbstractTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.TransformerContext;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.statement.BoxTry;

public class BoxTryTransformer extends AbstractTransformer {

	public BoxTryTransformer( Transpiler transpiler ) {
		super( transpiler );
	}

	@Override
	public List<AbstractInsnNode> transform( BoxNode node, TransformerContext context ) {
		List<AbstractInsnNode>	nodes				= new ArrayList<>();
		BoxTry					boxTry				= ( BoxTry ) node;

		LabelNode				tryStartLabel		= new LabelNode();
		LabelNode				tryEndLabel			= new LabelNode();
		LabelNode				finallyStartLabel	= new LabelNode();
		LabelNode				finallyEndLabel		= new LabelNode();

		nodes.add( tryStartLabel );

		nodes.addAll(
		    boxTry.getTryBody()
		        .stream()
		        .map( ( boxNode ) -> transpiler.transform( boxNode, context )
		        ).flatMap( ( nodeList ) -> nodeList.stream() )
		        .toList()
		);

		nodes.add( tryEndLabel );

		// inline the finally after the try body
		nodes.addAll(
		    boxTry.getFinallyBody()
		        .stream()
		        .map( ( boxNode ) -> transpiler.transform( boxNode, context ) )
		        .flatMap( ( nodeList ) -> nodeList.stream() )
		        .toList()
		);

		// since we inlined our finally when the code doesnt error we can skip the final finally block
		nodes.add( new JumpInsnNode( Opcodes.GOTO, finallyEndLabel ) );

		nodes.add( finallyStartLabel );

		// TODO how do I know the right index to use?
		nodes.add( new VarInsnNode( Opcodes.ASTORE, 2 ) );

		// should these be reused from above or created a second time?
		nodes.addAll(
		    boxTry.getFinallyBody()
		        .stream()
		        .map( ( boxNode ) -> transpiler.transform( boxNode, context )
		        ).flatMap( ( nodeList ) -> nodeList.stream() )
		        .toList()
		);

		nodes.add( new VarInsnNode( Opcodes.ALOAD, 2 ) );

		nodes.add( new InsnNode( Opcodes.ATHROW ) );

		nodes.add( finallyEndLabel );

		transpiler.addTryCatchBlock( new TryCatchBlockNode( tryStartLabel, tryEndLabel, finallyStartLabel, null ) );

		return nodes;

	}

}
