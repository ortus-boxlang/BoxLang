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
import org.objectweb.asm.tree.TypeInsnNode;

import ortus.boxlang.compiler.asmboxpiler.Transpiler;
import ortus.boxlang.compiler.asmboxpiler.transformer.AbstractTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.ReturnValueContext;
import ortus.boxlang.compiler.asmboxpiler.transformer.TransformerContext;
import ortus.boxlang.compiler.ast.BoxExpression;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.statement.BoxThrow;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.types.exceptions.CustomException;
import ortus.boxlang.runtime.types.exceptions.ExceptionUtil;

public class BoxThrowTransformer extends AbstractTransformer {

	public BoxThrowTransformer( Transpiler transpiler ) {
		super( transpiler );
	}

	@Override
	public List<AbstractInsnNode> transform( BoxNode node, TransformerContext context, ReturnValueContext returnContext ) {
		List<AbstractInsnNode>	nodes		= new ArrayList<>();
		BoxThrow				boxThrow	= ( BoxThrow ) node;

		nodes.addAll( getNewCustomExceptionInstructions( boxThrow ) );

		nodes.add( new MethodInsnNode(
		    Opcodes.INVOKESTATIC,
		    Type.getInternalName( ExceptionUtil.class ),
		    "throwException",
		    Type.getMethodDescriptor( Type.VOID_TYPE, Type.getType( Object.class ) ),
		    false
		) );

		// this is a noop but needs to be present for validation purposes
		nodes.add( new InsnNode( Opcodes.ACONST_NULL ) );

		return nodes;

	}

	private List<AbstractInsnNode> getNewCustomExceptionInstructions( BoxThrow boxThrow ) {
		BoxExpression	object			= boxThrow.getExpression();
		BoxExpression	type			= boxThrow.getType();
		BoxExpression	message			= boxThrow.getMessage();
		BoxExpression	detail			= boxThrow.getDetail();
		BoxExpression	errorcode		= boxThrow.getErrorCode();
		BoxExpression	extendedinfo	= boxThrow.getExtendedInfo();

		if ( message == null && object == null ) {
			return List.of(
			    new TypeInsnNode( Opcodes.NEW, Type.getInternalName( CustomException.class ) ),
			    new InsnNode( Opcodes.DUP ),
			    new InsnNode( Opcodes.ACONST_NULL ),
			    new MethodInsnNode( Opcodes.INVOKESPECIAL,
			        Type.getInternalName( CustomException.class ),
			        "<init>",
			        Type.getMethodDescriptor( Type.VOID_TYPE, Type.getType( String.class ) ),
			        false )
			);
		} else if ( message == null ) {
			return transpiler.transform( boxThrow.getExpression(), TransformerContext.RIGHT, ReturnValueContext.VALUE_OR_NULL );
		}

		List<AbstractInsnNode> nodes = new ArrayList<>();

		nodes.add( new TypeInsnNode( Opcodes.NEW, Type.getInternalName( CustomException.class ) ) );
		nodes.add( new InsnNode( Opcodes.DUP ) );

		nodes.addAll( transpiler.transform( message, null ) );

		nodes.addAll( nullGuard( detail ) );
		nodes.addAll( nullGuard( errorcode ) );
		nodes.addAll( nullGuard( type ) );

		if ( extendedinfo == null ) {
			nodes.add( new InsnNode( Opcodes.ACONST_NULL ) );
		} else {
			nodes.addAll( transpiler.transform( extendedinfo, null ) );
		}

		if ( object == null ) {
			nodes.add( new InsnNode( Opcodes.ACONST_NULL ) );
		} else {
			nodes.addAll( transpiler.transform( boxThrow.getExpression(), TransformerContext.RIGHT ) );
			nodes.add( new MethodInsnNode( Opcodes.INVOKEVIRTUAL,
			    Type.getInternalName( DynamicObject.class ),
			    "unWrap",
			    Type.getMethodDescriptor(
			        Type.getType( Object.class ),
			        Type.getType( Object.class )
			    ),
			    false
			) );
		}

		nodes.add( new MethodInsnNode( Opcodes.INVOKESPECIAL,
		    Type.getInternalName( CustomException.class ),
		    "<init>",
		    Type.getMethodDescriptor(
		        Type.VOID_TYPE,
		        Type.getType( String.class ),
		        Type.getType( String.class ),
		        Type.getType( String.class ),
		        Type.getType( String.class ),
		        Type.getType( Object.class ),
		        Type.getType( Throwable.class )
		    ),
		    false
		) );

		return nodes;
	}

	private List<AbstractInsnNode> nullGuard( BoxExpression expr ) {
		if ( expr == null ) {
			return List.of( new InsnNode( Opcodes.ACONST_NULL ) );
		}

		List<AbstractInsnNode> nodes = new ArrayList<>();

		nodes.addAll( transpiler.transform( expr, null ) );
		LabelNode ifNullLabel = new LabelNode();
		nodes.add( new JumpInsnNode( Opcodes.IFNULL, ifNullLabel ) );
		nodes.add( new InsnNode( Opcodes.ACONST_NULL ) );

		LabelNode elseLabel = new LabelNode();
		nodes.add( new JumpInsnNode( Opcodes.GOTO, elseLabel ) );
		nodes.add( ifNullLabel );

		nodes.addAll( transpiler.transform( expr, null ) );
		nodes.add( new MethodInsnNode( Opcodes.INVOKESTATIC,
		    Type.getInternalName( StringCaster.class ),
		    "cast",
		    Type.getMethodDescriptor(
		        Type.getType( String.class ),
		        Type.getType( Object.class )
		    ),
		    false
		) );
		nodes.add( elseLabel );

		return nodes;
	}

}
