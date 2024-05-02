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

import com.github.javaparser.ast.expr.Expression;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import ortus.boxlang.compiler.asmboxpiler.AsmTranspiler;
import ortus.boxlang.compiler.asmboxpiler.AsmHelper;
import ortus.boxlang.compiler.asmboxpiler.transformer.AbstractTransformer;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.BoxStatement;
import ortus.boxlang.compiler.ast.statement.BoxAccessModifier;
import ortus.boxlang.compiler.ast.statement.BoxFunctionDeclaration;
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

import java.nio.file.Path;
import java.util.List;

public class BoxFunctionDeclarationTransformer extends AbstractTransformer {

	public BoxFunctionDeclarationTransformer( AsmTranspiler transpiler ) {
		super( transpiler );
	}

	// @formatter:on
	@Override
	public List<AbstractInsnNode> transform( BoxNode node ) throws IllegalStateException {
		BoxFunctionDeclaration	function		= ( BoxFunctionDeclaration ) node;

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
		AsmHelper.init( classNode, type, UDF.class );
		transpiler.setAuxiliary( type.getClassName(), classNode );

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
		AsmHelper.addParentFieldGetter( classNode,
		    declaringType,
		    "imports",
		    "getImports",
		    Type.getType( List.class ) );
		AsmHelper.addParentFieldGetter( classNode,
		    declaringType,
		    "path",
		    "getRunnablePath",
		    Type.getType( Path.class ) );
		AsmHelper.addParentFieldGetter( classNode,
		    declaringType,
		    "sourceType",
		    "getSourceType",
		    Type.getType( BoxSourceType.class ) );

		AsmHelper.invokeWithContextAndClassLocator( classNode, Type.getType( FunctionBoxContext.class ), methodVisitor -> {
			for ( BoxStatement statement : function.getBody() ) {
				transpiler.transform( statement ).forEach( methodInsNode -> methodInsNode.accept( methodVisitor ) );
			}
		} );

		AsmHelper.complete( classNode, type, methodVisitor -> {
			createKey( function.getName() ).forEach( methodInsnNode -> methodInsnNode.accept( methodVisitor ) );
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
			AsmHelper.array( Type.getType( Argument.class ), function.getArgs(), ( arg, i ) -> transpiler.transform( arg ) )
			    .forEach( methodInsnNode -> methodInsnNode.accept( methodVisitor ) );
			methodVisitor.visitFieldInsn( Opcodes.PUTSTATIC,
			    type.getInternalName(),
			    "arguments",
			    Type.getDescriptor( Argument[].class ) );
			transformAnnotations( function.getAnnotations() ).forEach( methodInsnNode -> methodInsnNode.accept( methodVisitor ) );
			methodVisitor.visitFieldInsn( Opcodes.PUTSTATIC,
			    type.getInternalName(),
			    "annotations",
			    Type.getDescriptor( IStruct.class ) );
			transformDocumentation( function.getDocumentation() ).forEach( methodInsnNode -> methodInsnNode.accept( methodVisitor ) );
			methodVisitor.visitFieldInsn( Opcodes.PUTSTATIC,
			    type.getInternalName(),
			    "documentation",
			    Type.getDescriptor( IStruct.class ) );
		} );

		return List.of(
		    new VarInsnNode( Opcodes.ALOAD, 1 ),
		    new MethodInsnNode( Opcodes.INVOKESTATIC,
		        type.getInternalName(),
		        "getInstance",
		        Type.getMethodDescriptor( type ),
		        false ),
		    new MethodInsnNode( Opcodes.INVOKEINTERFACE,
		        Type.getInternalName( IBoxContext.class ),
		        "registerUDF",
		        Type.getMethodDescriptor( Type.VOID_TYPE, Type.getType( UDF.class ) ),
		        true )
		);
	}
}
