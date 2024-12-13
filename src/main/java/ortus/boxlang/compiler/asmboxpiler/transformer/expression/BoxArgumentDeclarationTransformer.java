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
import java.util.Optional;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;

import ortus.boxlang.compiler.asmboxpiler.MethodContextTracker;
import ortus.boxlang.compiler.asmboxpiler.Transpiler;
import ortus.boxlang.compiler.asmboxpiler.transformer.AbstractTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.ReturnValueContext;
import ortus.boxlang.compiler.asmboxpiler.transformer.TransformerContext;
import ortus.boxlang.compiler.ast.BoxExpression;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.statement.BoxArgumentDeclaration;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.loader.ClassLocator;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.DefaultExpression;
import ortus.boxlang.runtime.types.IStruct;

public class BoxArgumentDeclarationTransformer extends AbstractTransformer {

	public BoxArgumentDeclarationTransformer( Transpiler transpiler ) {
		super( transpiler );
	}

	@Override
	public List<AbstractInsnNode> transform( BoxNode node, TransformerContext context, ReturnValueContext returnContext ) throws IllegalStateException {
		BoxArgumentDeclaration	boxArgument			= ( BoxArgumentDeclaration ) node;

		/* Process default value */
		List<AbstractInsnNode>	defaultLiteral		= List.of( new InsnNode( Opcodes.ACONST_NULL ) );
		List<AbstractInsnNode>	defaultExpression	= List.of( new InsnNode( Opcodes.ACONST_NULL ) );
		if ( boxArgument.getValue() != null ) {
			if ( boxArgument.getValue().isLiteral() ) {
				defaultLiteral = transpiler.transform( boxArgument.getValue(), TransformerContext.NONE, ReturnValueContext.VALUE_OR_NULL );
			} else {
				defaultExpression = getDefaultExpression( boxArgument.getValue() );
				// defaultExpression = transpiler.transform( boxArgument.getValue(), TransformerContext.NONE, ReturnValueContext.VALUE );
			}
		}

		List<AbstractInsnNode> nodes = new ArrayList<>();
		nodes.add( new TypeInsnNode( Opcodes.NEW, Type.getInternalName( Argument.class ) ) );
		nodes.add( new InsnNode( Opcodes.DUP ) );
		nodes.add( new LdcInsnNode( boxArgument.getRequired() ? 1 : 0 ) );
		nodes.add( new LdcInsnNode( Optional.ofNullable( boxArgument.getType() ).orElse( "any" ) ) );
		nodes.addAll( transpiler.createKey( boxArgument.getName() ) );
		nodes.addAll( defaultLiteral );
		nodes.add( new LdcInsnNode( "DEBUG -args" ) );
		nodes.add( new InsnNode( Opcodes.POP ) );
		nodes.addAll( defaultExpression );
		nodes.addAll( transpiler.transformAnnotations( boxArgument.getAnnotations() ) );
		nodes.addAll( transpiler.transformDocumentation( boxArgument.getDocumentation() ) );
		nodes.add( new MethodInsnNode( Opcodes.INVOKESPECIAL,
		    Type.getInternalName( Argument.class ),
		    "<init>",
		    Type.getMethodDescriptor( Type.VOID_TYPE,
		        Type.BOOLEAN_TYPE,
		        Type.getType( String.class ),
		        Type.getType( Key.class ),
		        Type.getType( Object.class ),
		        Type.getType( DefaultExpression.class ),
		        Type.getType( IStruct.class ),
		        Type.getType( IStruct.class ) ),
		    false ) );

		return nodes;
		// return AsmHelper.addLineNumberLabels( nodes, node );
	}

	private List<AbstractInsnNode> getDefaultExpression( BoxExpression body ) {
		Type		type		= Type.getType( "L" + transpiler.getProperty( "packageName" ).replace( '.', '/' )
		    + "/" + transpiler.getProperty( "classname" )
		    + "$Lambda_" + transpiler.incrementAndGetLambdaCounter() + ";" );

		ClassNode	classNode	= new ClassNode();

		classNode.visit(
		    Opcodes.V17,
		    Opcodes.ACC_PUBLIC,
		    type.getInternalName(),
		    null,
		    Type.getInternalName( Object.class ),
		    new String[] { Type.getInternalName( DefaultExpression.class ) } );

		MethodVisitor initVisitor = classNode.visitMethod( Opcodes.ACC_PUBLIC,
		    "<init>",
		    Type.getMethodDescriptor( Type.VOID_TYPE ),
		    null,
		    null );
		initVisitor.visitCode();
		initVisitor.visitVarInsn( Opcodes.ALOAD, 0 );
		initVisitor.visitMethodInsn( Opcodes.INVOKESPECIAL,
		    Type.getInternalName( Object.class ),
		    "<init>",
		    Type.getMethodDescriptor( Type.VOID_TYPE ),
		    false );
		initVisitor.visitInsn( Opcodes.RETURN );
		initVisitor.visitEnd();

		MethodContextTracker t = new MethodContextTracker( false );
		transpiler.addMethodContextTracker( t );
		// Object evaluate( IBoxContext context );
		MethodVisitor methodVisitor = classNode.visitMethod(
		    Opcodes.ACC_PUBLIC,
		    "evaluate",
		    Type.getMethodDescriptor( Type.getType( Object.class ), Type.getType( IBoxContext.class ) ),
		    null,
		    null );
		methodVisitor.visitCode();

		t.trackNewContext();

		methodVisitor.visitMethodInsn(
		    Opcodes.INVOKESTATIC,
		    Type.getInternalName( ClassLocator.class ),
		    "getInstance",
		    Type.getMethodDescriptor( Type.getType( ClassLocator.class ) ),
		    false );
		t.storeNewVariable( Opcodes.ASTORE ).nodes().forEach( ( node ) -> node.accept( methodVisitor ) );

		transpiler.transform( body, TransformerContext.NONE, ReturnValueContext.VALUE_OR_NULL )
		    .forEach( ( ins ) -> ins.accept( methodVisitor ) );

		methodVisitor.visitInsn( Opcodes.ARETURN );
		methodVisitor.visitMaxs( 0, 0 );
		methodVisitor.visitEnd();

		transpiler.popMethodContextTracker();

		transpiler.setAuxiliary( type.getClassName(), classNode );

		List<AbstractInsnNode> nodes = new ArrayList<AbstractInsnNode>();

		nodes.add( new TypeInsnNode( Opcodes.NEW, type.getInternalName() ) );
		nodes.add( new InsnNode( Opcodes.DUP ) );
		nodes.add( new MethodInsnNode( Opcodes.INVOKESPECIAL,
		    type.getInternalName(),
		    "<init>",
		    Type.getMethodDescriptor( Type.VOID_TYPE ),
		    false ) );
		return nodes;
	}
}
