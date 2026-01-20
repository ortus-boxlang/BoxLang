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

import java.util.List;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

import ortus.boxlang.compiler.asmboxpiler.AsmHelper;
import ortus.boxlang.compiler.asmboxpiler.Transpiler;
import ortus.boxlang.compiler.asmboxpiler.transformer.AbstractTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.ReturnValueContext;
import ortus.boxlang.compiler.asmboxpiler.transformer.TransformerContext;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.expression.BoxLambda;
import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.context.FunctionBoxContext;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Function;
import ortus.boxlang.runtime.types.Function.Access;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Lambda;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.meta.FunctionMeta;
import ortus.boxlang.runtime.util.ResolvedFilePath;

/**
 * Transform a Lambda in the equivalent Java Class
 */
public class BoxLambdaTransformer extends AbstractTransformer {

	public BoxLambdaTransformer( Transpiler transpiler ) {
		super( transpiler );
	}

	@Override
	public List<AbstractInsnNode> transform( BoxNode node, TransformerContext context, ReturnValueContext returnContext ) throws IllegalStateException {
		BoxLambda	boxLambda	= ( BoxLambda ) node;

		Type		type		= Type.getType( "L" + transpiler.getProperty( "packageName" ).replace( '.', '/' )
		    + "/" + transpiler.getProperty( "classname" )
		    + "$Lambda_" + transpiler.incrementAndGetLambdaCounter() + ";" );

		ClassNode	classNode	= new ClassNode();
		classNode.visitSource( transpiler.getProperty( "filePath" ), null );

		AsmHelper.init( classNode, true, type, Type.getType( Lambda.class ), methodVisitor -> {
		} );

		// Add the @BoxByteCodeVersion annotation to the generated class
		AsmHelper.addBoxByteCodeVersionAnnotation( classNode );

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

		// Add metadata cache fields (private static volatile)
		AsmHelper.addNullStaticField( classNode, "metadata", Type.getType( IStruct.class ), true );
		AsmHelper.addNullStaticField( classNode, "legacyMetadata", Type.getType( IStruct.class ), true );

		// Add static boolean fields for isClosure and isLambda
		AsmHelper.addStaticFieldWithInitialValue( classNode, "isClosure", Type.BOOLEAN_TYPE, false );
		AsmHelper.addStaticFieldWithInitialValue( classNode, "isLambda", Type.BOOLEAN_TYPE, true );

		// Add defaultOutput as public static field (lambdas can't output by default)
		AsmHelper.addStaticFieldWithInitialValue( classNode, "defaultOutput", Type.BOOLEAN_TYPE, false );

		// Add getMetaDataStatic() method with double-check locking
		AsmHelper.addDoubleCheckLockedStaticMethod(
		    classNode,
		    type,
		    "getMetaDataStatic",
		    "metadata",
		    Type.getType( IStruct.class ),
		    methodVisitor -> {
			    // Call FunctionMeta.generateMeta(name, arguments, returnType, access, documentation, isClosure, isLambda, superClass, interfaces, canOutput,
			    // declaringClass, annotations)

			    // name (Key)
			    methodVisitor.visitFieldInsn( Opcodes.GETSTATIC, type.getInternalName(), "name", Type.getDescriptor( Key.class ) );

			    // arguments (Argument[])
			    methodVisitor.visitFieldInsn( Opcodes.GETSTATIC, type.getInternalName(), "arguments", Type.getDescriptor( Argument[].class ) );

			    // returnType (String)
			    methodVisitor.visitFieldInsn( Opcodes.GETSTATIC, type.getInternalName(), "returnType", Type.getDescriptor( String.class ) );

			    // access (Access)
			    methodVisitor.visitFieldInsn( Opcodes.GETSTATIC, type.getInternalName(), "access", Type.getDescriptor( Access.class ) );

			    // documentation (IStruct)
			    methodVisitor.visitFieldInsn( Opcodes.GETSTATIC, type.getInternalName(), "documentation", Type.getDescriptor( IStruct.class ) );

			    // isClosure (boolean) - false for lambda
			    methodVisitor.visitInsn( Opcodes.ICONST_0 );

			    // isLambda (boolean) - true for lambda
			    methodVisitor.visitInsn( Opcodes.ICONST_1 );

			    // superClass (String) - null for lambda
			    methodVisitor.visitInsn( Opcodes.ACONST_NULL );

			    // interfaces (String[]) - empty for lambda
			    methodVisitor.visitInsn( Opcodes.ICONST_0 );
			    methodVisitor.visitTypeInsn( Opcodes.ANEWARRAY, "java/lang/String" );

			    // canOutput (boolean) - false for lambda
			    methodVisitor.visitInsn( Opcodes.ICONST_0 );

			    // declaringClass (Class<?>) - this lambda's class
			    methodVisitor.visitLdcInsn( type );

			    // annotations (IStruct)
			    methodVisitor.visitFieldInsn( Opcodes.GETSTATIC, type.getInternalName(), "annotations", Type.getDescriptor( IStruct.class ) );

			    // Call FunctionMeta.generateMeta(...)
			    methodVisitor.visitMethodInsn( Opcodes.INVOKESTATIC,
			        Type.getInternalName( FunctionMeta.class ),
			        "generateMeta",
			        Type.getMethodDescriptor(
			            Type.getType( IStruct.class ),
			            Type.getType( Key.class ),
			            Type.getType( Argument[].class ),
			            Type.getType( String.class ),
			            Type.getType( Access.class ),
			            Type.getType( IStruct.class ),
			            Type.BOOLEAN_TYPE,
			            Type.BOOLEAN_TYPE,
			            Type.getType( String.class ),
			            Type.getType( String[].class ),
			            Type.BOOLEAN_TYPE,
			            Type.getType( Class.class ),
			            Type.getType( IStruct.class ) ),
			        false );
		    } );

		// Add getMetaStatic() method with double-check locking (returns same as getMetaDataStatic for lambdas)
		AsmHelper.addDoubleCheckLockedStaticMethod(
		    classNode,
		    type,
		    "getMetaStatic",
		    "legacyMetadata",
		    Type.getType( IStruct.class ),
		    methodVisitor -> {
			    // For lambdas, legacy metadata is same as regular metadata
			    // Call FunctionMeta.generateMeta(name, arguments, returnType, access, documentation, isClosure, isLambda, superClass, interfaces, canOutput,
			    // declaringClass, annotations)

			    // name (Key)
			    methodVisitor.visitFieldInsn( Opcodes.GETSTATIC, type.getInternalName(), "name", Type.getDescriptor( Key.class ) );

			    // arguments (Argument[])
			    methodVisitor.visitFieldInsn( Opcodes.GETSTATIC, type.getInternalName(), "arguments", Type.getDescriptor( Argument[].class ) );

			    // returnType (String)
			    methodVisitor.visitFieldInsn( Opcodes.GETSTATIC, type.getInternalName(), "returnType", Type.getDescriptor( String.class ) );

			    // access (Access)
			    methodVisitor.visitFieldInsn( Opcodes.GETSTATIC, type.getInternalName(), "access", Type.getDescriptor( Access.class ) );

			    // documentation (IStruct)
			    methodVisitor.visitFieldInsn( Opcodes.GETSTATIC, type.getInternalName(), "documentation", Type.getDescriptor( IStruct.class ) );

			    // isClosure (boolean) - false for lambda
			    methodVisitor.visitInsn( Opcodes.ICONST_0 );

			    // isLambda (boolean) - true for lambda
			    methodVisitor.visitInsn( Opcodes.ICONST_1 );

			    // superClass (String) - null for lambda
			    methodVisitor.visitInsn( Opcodes.ACONST_NULL );

			    // interfaces (String[]) - empty for lambda
			    methodVisitor.visitInsn( Opcodes.ICONST_0 );
			    methodVisitor.visitTypeInsn( Opcodes.ANEWARRAY, "java/lang/String" );

			    // canOutput (boolean) - false for lambda
			    methodVisitor.visitInsn( Opcodes.ICONST_0 );

			    // declaringClass (Class<?>) - this lambda's class
			    methodVisitor.visitLdcInsn( type );

			    // annotations (IStruct)
			    methodVisitor.visitFieldInsn( Opcodes.GETSTATIC, type.getInternalName(), "annotations", Type.getDescriptor( IStruct.class ) );

			    // Call FunctionMeta.generateMeta(...)
			    methodVisitor.visitMethodInsn( Opcodes.INVOKESTATIC,
			        Type.getInternalName( FunctionMeta.class ),
			        "generateMeta",
			        Type.getMethodDescriptor(
			            Type.getType( IStruct.class ),
			            Type.getType( Key.class ),
			            Type.getType( Argument[].class ),
			            Type.getType( String.class ),
			            Type.getType( Access.class ),
			            Type.getType( IStruct.class ),
			            Type.BOOLEAN_TYPE,
			            Type.BOOLEAN_TYPE,
			            Type.getType( String.class ),
			            Type.getType( String[].class ),
			            Type.BOOLEAN_TYPE,
			            Type.getType( Class.class ),
			            Type.getType( IStruct.class ) ),
			        false );
		    } );

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

		int componentCounter = transpiler.getComponentCounter();
		transpiler.setComponentCounter( 0 );
		transpiler.incrementfunctionBodyCounter();
		AsmHelper.methodWithContextAndClassLocator( classNode, "_invoke", Type.getType( FunctionBoxContext.class ), Type.getType( Object.class ), false,
		    transpiler, false,
		    () -> {
			    if ( boxLambda.getBody().getChildren().size() == 0 ) {
				    return List.of( new InsnNode( Opcodes.ACONST_NULL ) );
			    }
			    return boxLambda.getBody().getChildren().stream()
			        .flatMap( statement -> transpiler.transform( statement, TransformerContext.NONE, ReturnValueContext.VALUE_OR_NULL ).stream() )
			        .toList();
		    } );
		transpiler.decrementfunctionBodyCounter();
		transpiler.setComponentCounter( componentCounter );

		AsmHelper.complete( classNode, type, methodVisitor -> {
			methodVisitor.visitFieldInsn( Opcodes.GETSTATIC,
			    Type.getInternalName( Lambda.class ),
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
			    boxLambda.getArgs().stream().map( decl -> transpiler.transform( decl, TransformerContext.NONE ) ).toList() )
			    .forEach( abstractInsnNode -> abstractInsnNode.accept( methodVisitor ) );
			methodVisitor.visitFieldInsn( Opcodes.PUTSTATIC,
			    type.getInternalName(),
			    "arguments",
			    Type.getDescriptor( Argument[].class ) );

			transpiler.transformAnnotations( boxLambda.getAnnotations() ).forEach( abstractInsnNode -> abstractInsnNode.accept( methodVisitor ) );
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

		return List.of( new MethodInsnNode( Opcodes.INVOKESTATIC,
		    type.getInternalName(),
		    "getInstance",
		    Type.getMethodDescriptor( type ),
		    false ) );
	}
}
