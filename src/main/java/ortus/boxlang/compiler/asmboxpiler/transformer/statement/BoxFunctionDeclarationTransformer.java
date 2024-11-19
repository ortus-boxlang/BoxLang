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
import java.util.stream.Collectors;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

import ortus.boxlang.compiler.asmboxpiler.AsmHelper;
import ortus.boxlang.compiler.asmboxpiler.AsmTranspiler;
import ortus.boxlang.compiler.asmboxpiler.transformer.AbstractTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.ReturnValueContext;
import ortus.boxlang.compiler.asmboxpiler.transformer.TransformerContext;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.statement.BoxAccessModifier;
import ortus.boxlang.compiler.ast.statement.BoxFunctionDeclaration;
import ortus.boxlang.compiler.ast.statement.BoxMethodDeclarationModifier;
import ortus.boxlang.compiler.ast.statement.BoxReturnType;
import ortus.boxlang.compiler.ast.statement.BoxType;
import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.context.FunctionBoxContext;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Function;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.UDF;
import ortus.boxlang.runtime.util.ResolvedFilePath;

public class BoxFunctionDeclarationTransformer extends AbstractTransformer {

	public BoxFunctionDeclarationTransformer( AsmTranspiler transpiler ) {
		super( transpiler );
	}

	// @formatter:on
	@Override
	public List<AbstractInsnNode> transform( BoxNode node, TransformerContext context, ReturnValueContext returnContext ) throws IllegalStateException {
		BoxFunctionDeclaration	function		= ( BoxFunctionDeclaration ) node;
		TransformerContext		safe			= function.getName().equalsIgnoreCase( "isnull" ) ? TransformerContext.SAFE : context;

		Type					type			= Type.getType( "L" + transpiler.getProperty( "packageName" ).replace( '.', '/' )
		    + "/" + transpiler.getProperty( "classname" )
		    + "$Func_" + function.getName() + ";" );

		BoxReturnType			boxReturnType	= function.getType();
		BoxType					returnType		= BoxType.Any;
		String					fqn				= null;
		if ( boxReturnType != null ) {
			returnType = boxReturnType.getType();
			if ( returnType.equals( BoxType.Fqn ) ) {
				fqn = boxReturnType.getFqn();
			}
		}
		String				returnTypeName	= returnType.equals( BoxType.Fqn ) ? fqn : returnType.name();

		BoxAccessModifier	access			= function.getAccessModifier() == null ? BoxAccessModifier.Public : function.getAccessModifier();

		ClassNode			classNode		= new ClassNode();
		classNode.visitSource( transpiler.getProperty( "filePath" ), null );
		AsmHelper.init( classNode, true, type, Type.getType( UDF.class ), methodVisitor -> {
		} );
		transpiler.setAuxiliary( type.getClassName(), classNode );

		AsmHelper.addStaticFieldGetter( classNode,
		    type,
		    "modifiers",
		    "getModifiers",
		    Type.getType( List.class ),
		    null
		);

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
		    returnTypeName );
		AsmHelper.addStaticFieldGetter( classNode,
		    type,
		    "access",
		    "getAccess",
		    Type.getType( Function.Access.class ),
		    null );
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

		transpiler.incrementfunctionBodyCounter();
		AsmHelper.methodWithContextAndClassLocator( classNode, "_invoke", Type.getType( FunctionBoxContext.class ), Type.getType( Object.class ), false,
		    transpiler, true,
		    () -> {

			    if ( function.getBody() == null ) {
				    return AsmHelper.addLineNumberLabels( new ArrayList<AbstractInsnNode>(), node );
			    }

			    return AsmHelper.addLineNumberLabels( function.getBody()
			        .stream()
			        .flatMap( statement -> transpiler.transform( statement, safe, ReturnValueContext.EMPTY ).stream() )
			        .collect( Collectors.toList() ), node );
		    } );
		transpiler.decrementfunctionBodyCounter();

		AsmHelper.complete( classNode, type, methodVisitor -> {
			transpiler.createKey( function.getName() ).forEach( methodInsnNode -> methodInsnNode.accept( methodVisitor ) );
			methodVisitor.visitFieldInsn( Opcodes.PUTSTATIC,
			    type.getInternalName(),
			    "name",
			    Type.getDescriptor( Key.class ) );
			methodVisitor.visitLdcInsn( returnTypeName );
			methodVisitor.visitFieldInsn( Opcodes.PUTSTATIC,
			    type.getInternalName(),
			    "returnType",
			    Type.getDescriptor( String.class ) );
			methodVisitor.visitFieldInsn( Opcodes.GETSTATIC,
			    Type.getInternalName( Function.Access.class ),
			    access.name().toUpperCase(),
			    Type.getDescriptor( Function.Access.class ) );
			methodVisitor.visitFieldInsn( Opcodes.PUTSTATIC,
			    type.getInternalName(),
			    "access",
			    Type.getDescriptor( Function.Access.class ) );
			AsmHelper.array( Type.getType( Argument.class ), function.getArgs(), ( arg, i ) -> transpiler.transform( arg, safe ) )
			    .forEach( methodInsnNode -> methodInsnNode.accept( methodVisitor ) );

			methodVisitor.visitFieldInsn( Opcodes.PUTSTATIC,
			    type.getInternalName(),
			    "arguments",
			    Type.getDescriptor( Argument[].class ) );
			transpiler.transformAnnotations( function.getAnnotations() ).forEach( methodInsnNode -> methodInsnNode.accept( methodVisitor ) );
			methodVisitor.visitFieldInsn( Opcodes.PUTSTATIC,
			    type.getInternalName(),
			    "annotations",
			    Type.getDescriptor( IStruct.class ) );
			transpiler.transformDocumentation( function.getDocumentation() ).forEach( methodInsnNode -> methodInsnNode.accept( methodVisitor ) );
			methodVisitor.visitFieldInsn( Opcodes.PUTSTATIC,
			    type.getInternalName(),
			    "documentation",
			    Type.getDescriptor( IStruct.class ) );

			AsmHelper.array(
			    Type.getType( BoxMethodDeclarationModifier.class ),
			    function.getModifiers(),
			    ( bmdm, i ) -> List.of(
			        new FieldInsnNode(
			            Opcodes.GETSTATIC,
			            Type.getInternalName( BoxMethodDeclarationModifier.class ),
			            bmdm.toString().toUpperCase(),
			            Type.getDescriptor( BoxMethodDeclarationModifier.class )
			        )
			    )
			).stream()
			    .forEach( modifierNode -> modifierNode.accept( methodVisitor ) );

			methodVisitor.visitMethodInsn( Opcodes.INVOKESTATIC,
			    Type.getInternalName( List.class ),
			    "of",
			    Type.getMethodDescriptor( Type.getType( List.class ), Type.getType( Object[].class ) ),
			    true );

			methodVisitor.visitFieldInsn( Opcodes.PUTSTATIC,
			    type.getInternalName(),
			    "modifiers",
			    Type.getDescriptor( List.class ) );
		} );

		List<AbstractInsnNode> nodes = new ArrayList<AbstractInsnNode>();

		nodes.addAll( transpiler.getCurrentMethodContextTracker().get().loadCurrentContext() );
		nodes.add(
		    new MethodInsnNode( Opcodes.INVOKESTATIC,
		        type.getInternalName(),
		        "getInstance",
		        Type.getMethodDescriptor( type ),
		        false )
		);
		nodes.add(
		    new MethodInsnNode( Opcodes.INVOKEINTERFACE,
		        Type.getInternalName( IBoxContext.class ),
		        "registerUDF",
		        Type.getMethodDescriptor( Type.VOID_TYPE, Type.getType( UDF.class ) ),
		        true )
		);

		if ( returnContext == ReturnValueContext.VALUE || returnContext == ReturnValueContext.VALUE_OR_NULL ) {
			nodes.add( new InsnNode( Opcodes.ACONST_NULL ) );
		}

		return nodes;
	}
}
