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

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;

import ortus.boxlang.compiler.asmboxpiler.AsmHelper;
import ortus.boxlang.compiler.asmboxpiler.Transpiler;
import ortus.boxlang.compiler.asmboxpiler.transformer.AbstractTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.ReturnValueContext;
import ortus.boxlang.compiler.asmboxpiler.transformer.TransformerContext;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.expression.BoxClosure;
import ortus.boxlang.compiler.ast.statement.BoxExpressionStatement;
import ortus.boxlang.compiler.ast.statement.BoxStatementBlock;
import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.context.FunctionBoxContext;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Closure;
import ortus.boxlang.runtime.types.Function;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.util.ResolvedFilePath;

/**
 * Transform a Lambda in the equivalent Java Class
 */
public class BoxClosureTransformer extends AbstractTransformer {

	public BoxClosureTransformer( Transpiler transpiler ) {
		super( transpiler );
	}

	@Override
	public List<AbstractInsnNode> transform( BoxNode node, TransformerContext context, ReturnValueContext returnContext ) throws IllegalStateException {
		BoxClosure	boxClosure	= ( BoxClosure ) node;

		Type		type		= Type.getType( "L" + transpiler.getProperty( "packageName" ).replace( '.', '/' )
		    + "/" + transpiler.getProperty( "classname" )
		    + "$Closure_" + transpiler.incrementAndGetLambdaCounter() + ";" );

		ClassNode	classNode	= new ClassNode();
		classNode.visitSource( transpiler.getProperty( "filePath" ), null );

		AsmHelper.init( classNode, true, type, Type.getType( Closure.class ), methodVisitor -> {
		} );

		// create our constructor
		MethodVisitor contextConstructorVistior = classNode.visitMethod( Opcodes.ACC_PUBLIC,
		    "<init>",
		    Type.getMethodDescriptor( Type.VOID_TYPE, Type.getType( IBoxContext.class ) ),
		    null,
		    null );
		contextConstructorVistior.visitCode();
		contextConstructorVistior.visitVarInsn( Opcodes.ALOAD, 0 );
		contextConstructorVistior.visitVarInsn( Opcodes.ALOAD, 1 );
		contextConstructorVistior.visitMethodInsn( Opcodes.INVOKESPECIAL,
		    Type.getType( Closure.class ).getInternalName(),
		    "<init>",
		    Type.getMethodDescriptor( Type.VOID_TYPE, Type.getType( IBoxContext.class ) ),
		    false );
		contextConstructorVistior.visitInsn( Opcodes.RETURN );
		contextConstructorVistior.visitEnd();

		AsmHelper.addStaticFieldGetter( classNode,
		    type,
		    "name",
		    "getName",
		    Type.getType( Key.class ),
		    null );
		AsmHelper.addStaticFieldGetter( classNode,
		    type,
		    "arguments",
		    "getArguments",
		    Type.getType( Argument[].class ),
		    null );
		AsmHelper.addStaticFieldGetter( classNode,
		    type,
		    "returnType",
		    "getReturnType",
		    Type.getType( String.class ),
		    "any" );
		AsmHelper.addStaticFieldGetter( classNode,
		    type,
		    "annotations",
		    "getAnnotations",
		    Type.getType( IStruct.class ),
		    null );
		AsmHelper.addStaticFieldGetter( classNode,
		    type,
		    "documentation",
		    "getDocumentation",
		    Type.getType( IStruct.class ),
		    null );
		AsmHelper.addStaticFieldGetter( classNode,
		    type,
		    "access",
		    "getAccess",
		    Type.getType( Function.Access.class ),
		    null );

		Type declaringType = Type.getType( "L" + transpiler.getProperty( "packageName" ).replace( '.', '/' )
		    + "/" + transpiler.getProperty( "classname" )
		    + ";" );
		AsmHelper.addParentGetter( classNode,
		    declaringType,
		    "imports",
		    "getImports",
		    Type.getType( List.class ) );
		AsmHelper.addParentGetter( classNode,
		    declaringType,
		    "path",
		    "getRunnablePath",
		    Type.getType( ResolvedFilePath.class ) );
		AsmHelper.addParentGetter( classNode,
		    declaringType,
		    "sourceType",
		    "getSourceType",
		    Type.getType( BoxSourceType.class ) );

		boolean	isBlock				= boxClosure.getBody() instanceof BoxStatementBlock;

		int		componentCounter	= transpiler.getComponentCounter();
		transpiler.setComponentCounter( 0 );
		transpiler.incrementfunctionBodyCounter();

		ReturnValueContext closureReturnContext = isBlock ? ReturnValueContext.EMPTY : ReturnValueContext.VALUE_OR_NULL;
		AsmHelper.methodWithContextAndClassLocator( classNode, "_invoke", Type.getType( FunctionBoxContext.class ), Type.getType( Object.class ), false,
		    transpiler, isBlock,
		    () -> {
			    List<AbstractInsnNode> bodyNodes = new ArrayList();

			    BoxNode				body		= boxClosure.getBody();

			    if ( body instanceof BoxExpressionStatement boxExpr ) {
				    bodyNodes.addAll( transpiler.transform( boxExpr.getExpression(), TransformerContext.NONE, closureReturnContext ) );
			    } else {
				    bodyNodes.addAll( transpiler.transform( body, TransformerContext.NONE, closureReturnContext ) );
			    }

			    if ( isBlock ) {
				    bodyNodes.add( new InsnNode( Opcodes.ACONST_NULL ) );
			    }

			    return bodyNodes;
		    } );
		transpiler.decrementfunctionBodyCounter();
		transpiler.setComponentCounter( componentCounter );

		AsmHelper.complete( classNode, type, methodVisitor -> {
			methodVisitor.visitFieldInsn( Opcodes.GETSTATIC,
			    Type.getInternalName( Closure.class ),
			    "defaultName",
			    Type.getDescriptor( Key.class ) );
			methodVisitor.visitFieldInsn( Opcodes.PUTSTATIC,
			    type.getInternalName(),
			    "name",
			    Type.getDescriptor( Key.class ) );

			methodVisitor.visitLdcInsn( "any" );
			methodVisitor.visitFieldInsn( Opcodes.PUTSTATIC,
			    type.getInternalName(),
			    "returnType",
			    Type.getDescriptor( String.class ) );
			AsmHelper.array(
			    Type.getType( Argument.class ),
			    boxClosure.getArgs().stream().map( decl -> transpiler.transform( decl, TransformerContext.NONE, ReturnValueContext.VALUE ) ).toList() )
			    .forEach( abstractInsnNode -> abstractInsnNode.accept( methodVisitor ) );
			methodVisitor.visitFieldInsn( Opcodes.PUTSTATIC,
			    type.getInternalName(),
			    "arguments",
			    Type.getDescriptor( Argument[].class ) );

			transpiler.transformAnnotations( boxClosure.getAnnotations() ).forEach( abstractInsnNode -> abstractInsnNode.accept( methodVisitor ) );
			methodVisitor.visitFieldInsn( Opcodes.PUTSTATIC,
			    type.getInternalName(),
			    "annotations",
			    Type.getDescriptor( IStruct.class ) );

			methodVisitor.visitFieldInsn( Opcodes.GETSTATIC,
			    Type.getInternalName( Struct.class ),
			    "EMPTY",
			    Type.getDescriptor( IStruct.class ) );
			methodVisitor.visitFieldInsn( Opcodes.PUTSTATIC,
			    type.getInternalName(),
			    "documentation",
			    Type.getDescriptor( IStruct.class ) );

			methodVisitor.visitFieldInsn( Opcodes.GETSTATIC,
			    Type.getInternalName( Function.Access.class ),
			    "PUBLIC",
			    Type.getDescriptor( Function.Access.class ) );
			methodVisitor.visitFieldInsn( Opcodes.PUTSTATIC,
			    type.getInternalName(),
			    "access",
			    Type.getDescriptor( Function.Access.class ) );
		} );

		transpiler.setAuxiliary( type.getClassName(), classNode );

		List<AbstractInsnNode> nodes = new ArrayList<AbstractInsnNode>();

		nodes.add( new TypeInsnNode( Opcodes.NEW, type.getInternalName() ) );
		nodes.add( new InsnNode( Opcodes.DUP ) );

		nodes.addAll( transpiler.getCurrentMethodContextTracker().get().loadCurrentContext() );

		nodes.add( new MethodInsnNode( Opcodes.INVOKESPECIAL,
		    type.getInternalName(),
		    "<init>",
		    Type.getMethodDescriptor( Type.VOID_TYPE, Type.getType( IBoxContext.class ) ),
		    false ) );

		return AsmHelper.addLineNumberLabels( nodes, node );
	}
}
