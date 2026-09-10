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
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;

import ortus.boxlang.compiler.asmboxpiler.AsmTranspiler;
import ortus.boxlang.compiler.asmboxpiler.AsmHelper;
import ortus.boxlang.compiler.asmboxpiler.transformer.AbstractTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.ReturnValueContext;
import ortus.boxlang.compiler.asmboxpiler.transformer.TransformerContext;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.expression.BoxFQN;
import ortus.boxlang.compiler.ast.statement.BoxAnnotation;
import ortus.boxlang.compiler.ast.statement.BoxParam;
import ortus.boxlang.compiler.ast.statement.component.BoxComponent;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.ExpressionInterpreter;

public class BoxParamTransformer extends AbstractTransformer {

	public BoxParamTransformer( AsmTranspiler transpiler ) {
		super( transpiler );
	}

	@Override
	public List<AbstractInsnNode> transform( BoxNode node, TransformerContext context, ReturnValueContext returnContext ) {
		BoxParam			boxParam	= ( BoxParam ) node;
		List<BoxAnnotation>	attrs		= new ArrayList<BoxAnnotation>();
		attrs.add(
		    new BoxAnnotation(
		        new BoxFQN( "name",
		            boxParam.getVariable().getPosition(),
		            boxParam.getVariable().getSourceText() ),
		        boxParam.getVariable(),
		        boxParam.getVariable().getPosition(),
		        boxParam.getVariable().getSourceText()
		    )
		);
		if ( boxParam.getType() != null ) {
			attrs.add(
			    new BoxAnnotation(
			        new BoxFQN( "type",
			            boxParam.getType().getPosition(),
			            boxParam.getType().getSourceText() ),
			        boxParam.getType(),
			        boxParam.getType().getPosition(),
			        boxParam.getType().getSourceText()
			    )
			);
		}
		List<BoxAnnotation> requiredAttrs = new ArrayList<>( attrs );
		if ( boxParam.getDefaultValue() != null ) {
			attrs.add(
			    new BoxAnnotation(
			        new BoxFQN( "default",
			            boxParam.getDefaultValue().getPosition(),
			            boxParam.getDefaultValue().getSourceText() ),
			        boxParam.getDefaultValue(),
			        boxParam.getDefaultValue().getPosition(),
			        boxParam.getDefaultValue().getSourceText()
			    )
			);
		}
		if ( boxParam.getDefaultValue() == null ) {
			return transpiler.transform( new BoxComponent( "param", attrs, node.getPosition(), node.getSourceText() ), context, returnContext );
		}

		// Preserve component processing, but only evaluate the default when the variable is missing.
		List<AbstractInsnNode> nodes = new ArrayList<>();
		nodes.addAll( transpiler.getCurrentMethodContextTracker().get().loadCurrentContext() );
		nodes.addAll( transpiler.transform( boxParam.getVariable(), context, ReturnValueContext.VALUE ) );
		nodes.add( new InsnNode( Opcodes.ICONST_1 ) );
		nodes.add( new MethodInsnNode( Opcodes.INVOKESTATIC,
		    Type.getInternalName( ExpressionInterpreter.class ), "getVariable",
		    Type.getMethodDescriptor( Type.getType( Object.class ), Type.getType( IBoxContext.class ), Type.getType( String.class ), Type.BOOLEAN_TYPE ),
		    false ) );
		LabelNode	existing	= new LabelNode();
		LabelNode	end			= new LabelNode();
		nodes.add( new JumpInsnNode( Opcodes.IFNONNULL, existing ) );
		nodes.addAll( transpiler.transform( new BoxComponent( "param", attrs, node.getPosition(), node.getSourceText() ), context, returnContext ) );
		nodes.add( new JumpInsnNode( Opcodes.GOTO, end ) );
		nodes.add( existing );
		nodes.addAll( transpiler.transform( new BoxComponent( "param", requiredAttrs, node.getPosition(), node.getSourceText() ), context, returnContext ) );
		nodes.add( end );
		return AsmHelper.addLineNumberLabels( nodes, node );
	}
}
