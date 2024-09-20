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

import ortus.boxlang.compiler.asmboxpiler.Transpiler;
import ortus.boxlang.compiler.asmboxpiler.transformer.AbstractTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.ReturnValueContext;
import ortus.boxlang.compiler.asmboxpiler.transformer.TransformerContext;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.statement.BoxSwitch;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.operators.EqualsEquals;
import ortus.boxlang.runtime.types.exceptions.ExpressionException;
import ortus.boxlang.runtime.types.util.ListUtil;

public class BoxSwitchTransformer extends AbstractTransformer {

	public BoxSwitchTransformer( Transpiler transpiler ) {
		super( transpiler );
	}

	@Override
	public List<AbstractInsnNode> transform( BoxNode node, TransformerContext context, ReturnValueContext returnContext ) throws IllegalStateException {
		BoxSwitch				boxSwitch	= ( BoxSwitch ) node;
		List<AbstractInsnNode>	condition	= transpiler.transform( boxSwitch.getCondition(), TransformerContext.NONE, ReturnValueContext.VALUE );

		List<AbstractInsnNode>	nodes		= new ArrayList<>();
		nodes.addAll( condition );
		nodes.add( new LdcInsnNode( 0 ) );

		LabelNode endLabel = new LabelNode();
		boxSwitch.getCases().forEach( c -> {
			if ( c.getCondition() == null ) {
				return;
			}

			LabelNode startOfCase = new LabelNode(), endOfCase = new LabelNode(), endOfAll = new LabelNode();
			nodes.add( new JumpInsnNode( Opcodes.IFNE, startOfCase ) );
			// this dupes the condition
			nodes.add( new InsnNode( Opcodes.DUP ) );
			if ( c.getDelimiter() == null ) {
				nodes.addAll( transpiler.transform( c.getCondition(), TransformerContext.NONE, ReturnValueContext.VALUE ) );
				nodes.add( new MethodInsnNode( Opcodes.INVOKESTATIC,
				    Type.getInternalName( EqualsEquals.class ),
				    "invoke",
				    Type.getMethodDescriptor( Type.getType( Boolean.class ), Type.getType( Object.class ), Type.getType( Object.class ) ),
				    false ) );
			} else {
				nodes.add( new MethodInsnNode( Opcodes.INVOKESTATIC,
				    Type.getInternalName( StringCaster.class ),
				    "cast",
				    Type.getMethodDescriptor( Type.getType( String.class ), Type.getType( Object.class ) ),
				    false ) );
				nodes.addAll( transpiler.transform( c.getCondition(), TransformerContext.NONE, ReturnValueContext.VALUE ) );
				nodes.add( new MethodInsnNode( Opcodes.INVOKESTATIC,
				    Type.getInternalName( StringCaster.class ),
				    "cast",
				    Type.getMethodDescriptor( Type.getType( String.class ), Type.getType( Object.class ) ),
				    false ) );
				nodes.addAll( transpiler.transform( c.getDelimiter(), TransformerContext.NONE, ReturnValueContext.VALUE ) );
				nodes.add( new MethodInsnNode( Opcodes.INVOKESTATIC,
				    Type.getInternalName( ListUtil.class ),
				    "containsNoCase",
				    Type.getMethodDescriptor( Type.getType( Boolean.class ), Type.getType( String.class ), Type.getType( String.class ),
				        Type.getType( String.class ) ),
				    false ) );
			}
			nodes.add( new MethodInsnNode( Opcodes.INVOKEVIRTUAL,
			    Type.getInternalName( Boolean.class ),
			    "booleanValue",
			    Type.getMethodDescriptor( Type.BOOLEAN_TYPE ),
			    false ) );
			nodes.add( new JumpInsnNode( Opcodes.IFEQ, endOfCase ) );
			nodes.add( startOfCase );
			transpiler.getCurrentMethodContextTracker().get().setCurrentBreak( null, endLabel );
			c.getBody().forEach( stmt -> nodes.addAll( transpiler.transform( stmt, TransformerContext.NONE ) ) );
			transpiler.getCurrentMethodContextTracker().get().removeCurrentBreak( null ); // TODO: label name?
			nodes.add( new LdcInsnNode( 1 ) );
			nodes.add( new JumpInsnNode( Opcodes.GOTO, endOfAll ) );
			nodes.add( endOfCase );
			nodes.add( new LdcInsnNode( 0 ) );
			nodes.add( endOfAll );
		} );

		// TODO: Can there be more than one default case?
		boolean hasDefault = false;
		for ( var c : boxSwitch.getCases() ) {
			if ( c.getCondition() == null ) {
				if ( hasDefault ) {
					throw new ExpressionException( "Multiple default cases not supported", c.getPosition(), c.getSourceText() );
				}
				hasDefault = true;
				nodes.add( new InsnNode( Opcodes.POP ) );
				transpiler.getCurrentMethodContextTracker().get().setCurrentBreak( null, endLabel );
				c.getBody().forEach( stmt -> nodes.addAll( transpiler.transform( stmt, TransformerContext.NONE ) ) );
				transpiler.getCurrentMethodContextTracker().get().removeCurrentBreak( null );
				nodes.add( new JumpInsnNode( Opcodes.GOTO, endLabel ) );
			}
		}

		nodes.add( new InsnNode( Opcodes.POP ) );
		nodes.add( endLabel );
		nodes.add( new InsnNode( Opcodes.POP ) );

		return nodes;
	}
}
