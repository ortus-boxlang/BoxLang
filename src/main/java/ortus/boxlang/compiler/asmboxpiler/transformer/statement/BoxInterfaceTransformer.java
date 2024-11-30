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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

import ortus.boxlang.compiler.asmboxpiler.AsmHelper;
import ortus.boxlang.compiler.asmboxpiler.Transpiler;
import ortus.boxlang.compiler.asmboxpiler.transformer.ReturnValueContext;
import ortus.boxlang.compiler.asmboxpiler.transformer.TransformerContext;
import ortus.boxlang.compiler.ast.BoxExpression;
import ortus.boxlang.compiler.ast.BoxInterface;
import ortus.boxlang.compiler.ast.BoxStaticInitializer;
import ortus.boxlang.compiler.ast.Source;
import ortus.boxlang.compiler.ast.SourceFile;
import ortus.boxlang.compiler.ast.statement.BoxFunctionDeclaration;
import ortus.boxlang.compiler.ast.statement.BoxImport;
import ortus.boxlang.compiler.ast.statement.BoxMethodDeclarationModifier;
import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.InterfaceBoxContext;
import ortus.boxlang.runtime.loader.ImportDefinition;
import ortus.boxlang.runtime.runnables.IBoxRunnable;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.StaticScope;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.exceptions.ExpressionException;
import ortus.boxlang.runtime.util.ResolvedFilePath;

public class BoxInterfaceTransformer {

	public static ClassNode transpile( Transpiler transpiler, BoxInterface boxInterface ) throws BoxRuntimeException {

		Source	source			= boxInterface.getPosition().getSource();
		String	packageName		= transpiler.getProperty( "packageName" );
		String	boxPackageName	= transpiler.getProperty( "boxFQN" );
		transpiler.setProperty( "boxClassName", boxPackageName );
		String	classname			= transpiler.getProperty( "classname" );
		String	mappingName			= transpiler.getProperty( "mappingName" );
		String	mappingPath			= transpiler.getProperty( "mappingPath" );
		String	relativePath		= transpiler.getProperty( "relativePath" );
		String	fileName			= source instanceof SourceFile file && file.getFile() != null ? file.getFile().getName() : "unknown";
		String	filePath			= source instanceof SourceFile file && file.getFile() != null ? file.getFile().getAbsolutePath() : "unknown";
		// trim leading . if exists
		String	boxInterfacename	= transpiler.getProperty( "boxFQN" );
		String	sourceType			= transpiler.getProperty( "sourceType" );

		Type	type				= Type.getType( "L" + packageName.replace( '.', '/' )
		    + "/" + classname + ";" );
		transpiler.setProperty( "classType", type.getDescriptor() );
		transpiler.setProperty( "classTypeInternal", type.getInternalName() );

		ClassNode classNode = new ClassNode();

		AsmHelper.init( classNode, false, type, Type.getType( ortus.boxlang.runtime.runnables.BoxInterface.class ), methodVisitor -> {
		} );

		AsmHelper.addLazySingleton( classNode, type, methodVisitor -> {
			methodVisitor.visitTypeInsn( Opcodes.NEW, type.getInternalName() );
			methodVisitor.visitInsn( Opcodes.DUP );
			methodVisitor.visitMethodInsn( Opcodes.INVOKESPECIAL,
			    type.getInternalName(),
			    "<init>",
			    Type.getMethodDescriptor( Type.VOID_TYPE ),
			    false );
			methodVisitor.visitFieldInsn( Opcodes.PUTSTATIC,
			    type.getInternalName(),
			    "instance",
			    type.getDescriptor() );

			methodVisitor.visitTypeInsn( Opcodes.NEW, Type.getInternalName( StaticScope.class ) );
			methodVisitor.visitInsn( Opcodes.DUP );
			methodVisitor.visitFieldInsn( Opcodes.GETSTATIC,
			    type.getInternalName(),
			    "instance",
			    type.getDescriptor() );
			methodVisitor.visitMethodInsn( Opcodes.INVOKESPECIAL,
			    Type.getInternalName( StaticScope.class ),
			    "<init>",
			    Type.getMethodDescriptor( Type.VOID_TYPE, Type.getType( ortus.boxlang.runtime.runnables.BoxInterface.class ) ),
			    false );
			methodVisitor.visitFieldInsn( Opcodes.PUTSTATIC,
			    type.getInternalName(),
			    "staticScope",
			    Type.getDescriptor( StaticScope.class ) );

			methodVisitor.visitTypeInsn( Opcodes.NEW, Type.getInternalName( InterfaceBoxContext.class ) );
			methodVisitor.visitInsn( Opcodes.DUP );
			methodVisitor.visitVarInsn( Opcodes.ALOAD, 0 );
			methodVisitor.visitFieldInsn( Opcodes.GETSTATIC,
			    type.getInternalName(),
			    "instance",
			    type.getDescriptor() );
			methodVisitor.visitMethodInsn( Opcodes.INVOKESPECIAL,
			    Type.getInternalName( InterfaceBoxContext.class ),
			    "<init>",
			    Type.getMethodDescriptor( Type.VOID_TYPE,
			        Type.getType( IBoxContext.class ),
			        Type.getType( ortus.boxlang.runtime.runnables.BoxInterface.class ) ),
			    false );
			methodVisitor.visitVarInsn( Opcodes.ASTORE, 1 );

			methodVisitor.visitVarInsn( Opcodes.ALOAD, 1 );
			methodVisitor.visitFieldInsn( Opcodes.GETSTATIC,
			    type.getInternalName(),
			    "instance",
			    type.getDescriptor() );
			methodVisitor.visitMethodInsn( Opcodes.INVOKEVIRTUAL,
			    Type.getInternalName( InterfaceBoxContext.class ),
			    "pushTemplate",
			    Type.getMethodDescriptor( Type.getType( IBoxContext.class ), Type.getType( IBoxRunnable.class ) ),
			    false );
			methodVisitor.visitInsn( Opcodes.POP );

			methodVisitor.visitFieldInsn( Opcodes.GETSTATIC,
			    type.getInternalName(),
			    "instance",
			    type.getDescriptor() );
			methodVisitor.visitVarInsn( Opcodes.ALOAD, 1 );
			methodVisitor.visitMethodInsn( Opcodes.INVOKEVIRTUAL,
			    Type.getInternalName( ortus.boxlang.runtime.runnables.BoxInterface.class ),
			    "resolveSupers",
			    Type.getMethodDescriptor( Type.VOID_TYPE, Type.getType( IBoxContext.class ) ),
			    false );

			methodVisitor.visitVarInsn( Opcodes.ALOAD, 1 );
			methodVisitor.visitMethodInsn( Opcodes.INVOKESTATIC,
			    type.getInternalName(),
			    "staticInitializer",
			    Type.getMethodDescriptor( Type.VOID_TYPE, Type.getType( IBoxContext.class ) ),
			    false );

			methodVisitor.visitFieldInsn( Opcodes.GETSTATIC,
			    type.getInternalName(),
			    "instance",
			    type.getDescriptor() );
			methodVisitor.visitVarInsn( Opcodes.ALOAD, 1 );
			methodVisitor.visitMethodInsn( Opcodes.INVOKEVIRTUAL,
			    type.getInternalName(),
			    "pseudoConstructor",
			    Type.getMethodDescriptor( Type.VOID_TYPE, Type.getType( IBoxContext.class ) ),
			    false );

			methodVisitor.visitVarInsn( Opcodes.ALOAD, 1 );
			methodVisitor.visitMethodInsn( Opcodes.INVOKEVIRTUAL,
			    Type.getInternalName( InterfaceBoxContext.class ),
			    "popTemplate",
			    Type.getMethodDescriptor( Type.getType( ResolvedFilePath.class ) ),
			    false );
			methodVisitor.visitInsn( Opcodes.POP );
		}, Type.getType( IBoxContext.class ) );

		AsmHelper.addStaticFieldGetter( classNode,
		    type,
		    "imports",
		    "getImports",
		    Type.getType( List.class ),
		    null );
		AsmHelper.addStaticFieldGetter( classNode,
		    type,
		    "path",
		    "getRunnablePath",
		    Type.getType( ResolvedFilePath.class ),
		    null );
		AsmHelper.addStaticFieldGetter( classNode,
		    type,
		    "sourceType",
		    "getSourceType",
		    Type.getType( BoxSourceType.class ),
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
		AsmHelper.addStaticFieldGetterWithStaticGetter( classNode,
		    type,
		    "staticScope",
		    "getStaticScope",
		    "getStaticScopeStatic",
		    Type.getType( StaticScope.class ),
		    null,
		    false );

		AsmHelper.addStaticFieldGetter( classNode,
		    type,
		    "name",
		    "getName",
		    Type.getType( Key.class ),
		    null );

		AsmHelper.addStaticFieldGetter( classNode,
		    type,
		    "_supers",
		    "getSupers",
		    Type.getType( List.class ),
		    null );
		MethodVisitor addSuper = classNode.visitMethod( Opcodes.ACC_PUBLIC,
		    "_addSuper",
		    Type.getMethodDescriptor( Type.VOID_TYPE, Type.getType( ortus.boxlang.runtime.runnables.BoxInterface.class ) ),
		    null,
		    null );
		addSuper.visitCode();
		addSuper.visitFieldInsn( Opcodes.GETSTATIC,
		    type.getInternalName(),
		    "_supers",
		    Type.getDescriptor( List.class ) );
		addSuper.visitVarInsn( Opcodes.ALOAD, 1 );
		addSuper.visitMethodInsn( Opcodes.INVOKEINTERFACE,
		    Type.getInternalName( List.class ),
		    "add",
		    Type.getMethodDescriptor( Type.BOOLEAN_TYPE, Type.getType( Object.class ) ),
		    true );
		addSuper.visitInsn( Opcodes.POP );
		addSuper.visitInsn( Opcodes.RETURN );
		addSuper.visitMaxs( 0, 0 );
		addSuper.visitEnd();

		classNode.visitField( Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL,
		    "keys",
		    Type.getDescriptor( Key[].class ),
		    null,
		    null ).visitEnd();

		AsmHelper.addPrviateStaticFieldGetter( classNode,
		    type,
		    "abstractMethods",
		    "getAbstractMethods",
		    Type.getType( Map.class ),
		    null );
		AsmHelper.addPrviateStaticFieldGetter( classNode,
		    type,
		    "defaultMethods",
		    "getDefaultMethods",
		    Type.getType( Map.class ),
		    null );

		Label			start				= new Label(), end = new Label(), handler = new Label();
		MethodVisitor	pseudoConstructor	= classNode.visitMethod( Opcodes.ACC_PUBLIC,
		    "pseudoConstructor",
		    Type.getMethodDescriptor( Type.VOID_TYPE, Type.getType( IBoxContext.class ) ),
		    null,
		    null );
		pseudoConstructor.visitTryCatchBlock( start, end, handler, null );
		pseudoConstructor.visitCode();
		pseudoConstructor.visitVarInsn( Opcodes.ALOAD, 1 );
		pseudoConstructor.visitVarInsn( Opcodes.ALOAD, 0 );
		pseudoConstructor.visitMethodInsn( Opcodes.INVOKEINTERFACE,
		    Type.getInternalName( IBoxContext.class ),
		    "pushTemplate",
		    Type.getMethodDescriptor( Type.getType( IBoxContext.class ), Type.getType( IBoxRunnable.class ) ),
		    true );
		pseudoConstructor.visitInsn( Opcodes.POP );
		pseudoConstructor.visitLabel( start );
		pseudoConstructor.visitVarInsn( Opcodes.ALOAD, 0 );
		pseudoConstructor.visitVarInsn( Opcodes.ALOAD, 1 );
		pseudoConstructor.visitMethodInsn( Opcodes.INVOKEVIRTUAL,
		    type.getInternalName(),
		    "_pseudoConstructor",
		    Type.getMethodDescriptor( Type.VOID_TYPE, Type.getType( IBoxContext.class ) ),
		    false );
		pseudoConstructor.visitLabel( end );
		pseudoConstructor.visitVarInsn( Opcodes.ALOAD, 1 );
		pseudoConstructor.visitMethodInsn( Opcodes.INVOKEINTERFACE,
		    Type.getInternalName( IBoxContext.class ),
		    "popTemplate",
		    Type.getMethodDescriptor( Type.getType( ResolvedFilePath.class ) ),
		    true );
		pseudoConstructor.visitInsn( Opcodes.POP );
		pseudoConstructor.visitInsn( Opcodes.RETURN );
		pseudoConstructor.visitLabel( handler );
		pseudoConstructor.visitVarInsn( Opcodes.ALOAD, 1 );
		pseudoConstructor.visitMethodInsn( Opcodes.INVOKEINTERFACE,
		    Type.getInternalName( IBoxContext.class ),
		    "popTemplate",
		    Type.getMethodDescriptor( Type.getType( ResolvedFilePath.class ) ),
		    true );
		pseudoConstructor.visitInsn( Opcodes.POP );
		pseudoConstructor.visitInsn( Opcodes.ATHROW );
		pseudoConstructor.visitMaxs( 0, 0 );
		pseudoConstructor.visitEnd();

		// these imports need to happen before any methods are processed - the actual nodes will be used later on in the static init section
		List<List<AbstractInsnNode>> imports = new ArrayList<>();
		for ( BoxImport statement : boxInterface.getImports() ) {
			imports.add( transpiler.transform( statement, TransformerContext.NONE, ReturnValueContext.EMPTY ) );
		}
		List<AbstractInsnNode> importNodes = AsmHelper.array( Type.getType( ImportDefinition.class ), Stream.concat(
		    imports.stream(),
		    transpiler.getImports().stream().map( raw -> {
			    List<AbstractInsnNode> nodes = new ArrayList<>();
			    nodes.addAll( raw );
			    nodes.add( new MethodInsnNode( Opcodes.INVOKESTATIC,
			        Type.getInternalName( ImportDefinition.class ),
			        "parse",
			        Type.getMethodDescriptor( Type.getType( ImportDefinition.class ), Type.getType( String.class ) ),
			        false ) );
			    return nodes;
		    } )
		).filter( l -> l.size() > 0 ).toList() );
		// end import node setup

		AsmHelper.methodWithContextAndClassLocator( classNode, "_pseudoConstructor", Type.getType( IBoxContext.class ), Type.VOID_TYPE, false, transpiler,
		    false,
		    () -> {
			    return boxInterface.getBody()
			        .stream()
			        .sorted( ( a, b ) -> {
				        if ( a instanceof BoxFunctionDeclaration && ! ( b instanceof BoxFunctionDeclaration ) ) {
					        return -1;
				        } else if ( b instanceof BoxFunctionDeclaration && ! ( a instanceof BoxFunctionDeclaration ) ) {
					        return 1;
				        }

				        return 0;

			        } )
			        .flatMap( statement -> {

				        if ( ! ( statement instanceof BoxFunctionDeclaration )
				            && ! ( statement instanceof BoxImport )
				            && ! ( statement instanceof BoxStaticInitializer ) ) {
					        throw new ExpressionException(
					            "Statement type not supported in an interface: " + statement.getClass().getSimpleName(),
					            statement
					        );
				        }

				        if ( statement instanceof BoxFunctionDeclaration bfd && bfd.getBody() == null ) {
					        return new ArrayList<InsnNode>().stream();
				        }

				        return transpiler.transform( statement, TransformerContext.NONE, ReturnValueContext.EMPTY ).stream();
			        } )
			        .toList();
		    }
		);

		AsmHelper.methodWithContextAndClassLocator( classNode, "staticInitializer", Type.getType( IBoxContext.class ), Type.VOID_TYPE, true, transpiler, false,
		    () -> {
			    List<AbstractInsnNode> staticNodes = new ArrayList<AbstractInsnNode>();

			    boxInterface.getDescendantsOfType( BoxFunctionDeclaration.class, ( expr ) -> {
				    BoxFunctionDeclaration func = ( BoxFunctionDeclaration ) expr;

				    return func.getModifiers().contains( BoxMethodDeclarationModifier.STATIC );
			    } ).forEach( func -> {
				    staticNodes.addAll( transpiler.transform( func, TransformerContext.NONE ) );
			    } );

			    staticNodes.addAll( transpiler.getBoxStaticInitializers()
			        .stream()
			        .map( ( staticInitializer ) -> {
				        if ( staticInitializer == null || staticInitializer.getBody().size() == 0 ) {
					        return new ArrayList<AbstractInsnNode>();
				        }

				        return staticInitializer.getBody()
				            .stream()
				            .map( statement -> transpiler.transform( statement, TransformerContext.NONE ) )
				            .flatMap( nodes -> nodes.stream() )
				            .collect( Collectors.toList() );
			        } )
			        .flatMap( s -> s.stream() )
			        .collect( Collectors.toList() )
			    );

			    return staticNodes;
		    }
		);

		AsmHelper.complete( classNode, type, methodVisitor -> {
			AsmHelper.resolvedFilePath( methodVisitor, mappingName, mappingPath, relativePath, filePath );
			methodVisitor.visitFieldInsn( Opcodes.PUTSTATIC,
			    type.getInternalName(),
			    "path",
			    Type.getDescriptor( ResolvedFilePath.class ) );

			methodVisitor.visitFieldInsn( Opcodes.GETSTATIC,
			    Type.getInternalName( BoxSourceType.class ),
			    sourceType,
			    Type.getDescriptor( BoxSourceType.class ) );
			methodVisitor.visitFieldInsn( Opcodes.PUTSTATIC,
			    type.getInternalName(),
			    "sourceType",
			    Type.getDescriptor( BoxSourceType.class ) );

			List<AbstractInsnNode>	annotations		= transpiler.transformAnnotations( boxInterface.getAllAnnotations() );
			List<AbstractInsnNode>	documenation	= transpiler.transformDocumentation( boxInterface.getDocumentation() );
			List<AbstractInsnNode>	name			= transpiler.createKey( boxInterfacename );

			List<AbstractInsnNode>	abstractMethods	= AsmHelper.generateMapOfAbstractMethodNames( transpiler, boxInterface );

			methodVisitor.visitLdcInsn( transpiler.getKeys().size() );
			methodVisitor.visitTypeInsn( Opcodes.ANEWARRAY, Type.getInternalName( Key.class ) );
			int index = 0;
			for ( BoxExpression expression : transpiler.getKeys().values() ) {
				methodVisitor.visitInsn( Opcodes.DUP );
				methodVisitor.visitLdcInsn( index++ );
				transpiler.transform( expression, TransformerContext.NONE, ReturnValueContext.VALUE )
				    .forEach( methodInsnNode -> methodInsnNode.accept( methodVisitor ) );
				methodVisitor.visitMethodInsn( Opcodes.INVOKESTATIC,
				    Type.getInternalName( Key.class ),
				    "of",
				    Type.getMethodDescriptor( Type.getType( Key.class ), Type.getType( Object.class ) ),
				    false );
				methodVisitor.visitInsn( Opcodes.AASTORE );
			}
			methodVisitor.visitFieldInsn( Opcodes.PUTSTATIC,
			    type.getInternalName(),
			    "keys",
			    Type.getDescriptor( Key[].class ) );

			name.forEach( abstractInsnNode -> abstractInsnNode.accept( methodVisitor ) );
			methodVisitor.visitFieldInsn( Opcodes.PUTSTATIC,
			    type.getInternalName(),
			    "name",
			    Type.getDescriptor( Key.class ) );

			importNodes.forEach( node -> node.accept( methodVisitor ) );
			methodVisitor.visitMethodInsn( Opcodes.INVOKESTATIC,
			    Type.getInternalName( List.class ),
			    "of",
			    Type.getMethodDescriptor( Type.getType( List.class ), Type.getType( Object[].class ) ),
			    true );
			methodVisitor.visitFieldInsn( Opcodes.PUTSTATIC,
			    type.getInternalName(),
			    "imports",
			    Type.getDescriptor( List.class ) );

			annotations.forEach( abstractInsnNode -> abstractInsnNode.accept( methodVisitor ) );
			methodVisitor.visitFieldInsn( Opcodes.PUTSTATIC,
			    type.getInternalName(),
			    "annotations",
			    Type.getDescriptor( IStruct.class ) );

			documenation.forEach( abstractInsnNode -> abstractInsnNode.accept( methodVisitor ) );
			methodVisitor.visitFieldInsn( Opcodes.PUTSTATIC,
			    type.getInternalName(),
			    "documentation",
			    Type.getDescriptor( IStruct.class ) );

			methodVisitor.visitTypeInsn( Opcodes.NEW, Type.getInternalName( LinkedHashMap.class ) );
			methodVisitor.visitInsn( Opcodes.DUP );
			methodVisitor.visitMethodInsn( Opcodes.INVOKESPECIAL,
			    Type.getInternalName( LinkedHashMap.class ),
			    "<init>",
			    Type.getMethodDescriptor( Type.VOID_TYPE ),
			    false );
			methodVisitor.visitFieldInsn( Opcodes.PUTSTATIC, type.getInternalName(), "abstractMethods", Type.getDescriptor( Map.class ) );

			methodVisitor.visitTypeInsn( Opcodes.NEW, Type.getInternalName( LinkedHashMap.class ) );
			methodVisitor.visitInsn( Opcodes.DUP );
			methodVisitor.visitMethodInsn( Opcodes.INVOKESPECIAL,
			    Type.getInternalName( LinkedHashMap.class ),
			    "<init>",
			    Type.getMethodDescriptor( Type.VOID_TYPE ),
			    false );
			methodVisitor.visitFieldInsn( Opcodes.PUTSTATIC, type.getInternalName(), "defaultMethods", Type.getDescriptor( Map.class ) );

			methodVisitor.visitTypeInsn( Opcodes.NEW, Type.getInternalName( ArrayList.class ) );
			methodVisitor.visitInsn( Opcodes.DUP );
			methodVisitor.visitMethodInsn( Opcodes.INVOKESPECIAL,
			    Type.getInternalName( ArrayList.class ),
			    "<init>",
			    Type.getMethodDescriptor( Type.VOID_TYPE ),
			    false );
			methodVisitor.visitFieldInsn( Opcodes.PUTSTATIC, type.getInternalName(), "_supers", Type.getDescriptor( List.class ) );

			abstractMethods.forEach( node -> node.accept( methodVisitor ) );
			methodVisitor.visitFieldInsn( Opcodes.PUTSTATIC, type.getInternalName(), "abstractMethods", Type.getDescriptor( Map.class ) );
		} );

		return classNode;
	}
}
