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

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import ortus.boxlang.compiler.asmboxpiler.AsmHelper;
import ortus.boxlang.compiler.asmboxpiler.Transpiler;
import ortus.boxlang.compiler.asmboxpiler.transformer.ReturnValueContext;
import ortus.boxlang.compiler.asmboxpiler.transformer.TransformerContext;
import ortus.boxlang.compiler.ast.BoxClass;
import ortus.boxlang.compiler.ast.BoxExpression;
import ortus.boxlang.compiler.ast.Source;
import ortus.boxlang.compiler.ast.SourceFile;
import ortus.boxlang.compiler.ast.expression.BoxStringLiteral;
import ortus.boxlang.compiler.ast.statement.BoxAnnotation;
import ortus.boxlang.compiler.ast.statement.BoxArgumentDeclaration;
import ortus.boxlang.compiler.ast.statement.BoxFunctionDeclaration;
import ortus.boxlang.compiler.ast.statement.BoxImport;
import ortus.boxlang.compiler.ast.statement.BoxMethodDeclarationModifier;
import ortus.boxlang.compiler.ast.statement.BoxReturnType;
import ortus.boxlang.compiler.ast.statement.BoxType;
import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.dynamic.IReferenceable;
import ortus.boxlang.runtime.dynamic.javaproxy.InterfaceProxyService;
import ortus.boxlang.runtime.loader.ImportDefinition;
import ortus.boxlang.runtime.runnables.BoxClassSupport;
import ortus.boxlang.runtime.runnables.BoxInterface;
import ortus.boxlang.runtime.runnables.IClassRunnable;
import ortus.boxlang.runtime.scopes.ClassVariablesScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.StaticScope;
import ortus.boxlang.runtime.scopes.ThisScope;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.IType;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.meta.BoxMeta;
import ortus.boxlang.runtime.types.util.BLCollector;
import ortus.boxlang.runtime.types.util.ListUtil;
import ortus.boxlang.runtime.util.ResolvedFilePath;
import ortus.boxlang.runtime.util.conversion.ObjectMarshaller;

public class BoxClassTransformer {

	public static final Type	CLASS_TYPE					= Type.getType( Class.class );
	public static final Type	CLASS_ARRAY_TYPE			= Type.getType( Class[].class );

	private static final String	EXTENDS_ANNOTATION_MARKER	= "overrideJava";

	public static ClassNode transpile( Transpiler transpiler, BoxClass boxClass ) throws BoxRuntimeException {
		Source	source		= boxClass.getPosition().getSource();
		String	sourceType	= transpiler.getProperty( "sourceType" );

		String	filePath	= source instanceof SourceFile file && file.getFile() != null ? file.getFile().getAbsolutePath()
		    : "unknown";
		transpiler.setProperty( "filePath", filePath );
		String boxClassName = transpiler.getProperty( "boxFQN" );
		transpiler.setProperty( "boxClassName", boxClassName );
		String	mappingName		= transpiler.getProperty( "mappingName" );
		String	mappingPath		= transpiler.getProperty( "mappingPath" );
		String	relativePath	= transpiler.getProperty( "relativePath" );

		Type	type			= Type.getType( "L" + transpiler.getProperty( "packageName" ).replace( '.', '/' )
		    + "/" + transpiler.getProperty( "classname" ) + ";" );
		transpiler.setProperty( "classType", type.getDescriptor() );
		transpiler.setProperty( "classTypeInternal", type.getInternalName() );

		List<Type> interfaces = new ArrayList<>();
		interfaces.add( Type.getType( IClassRunnable.class ) );
		interfaces.add( Type.getType( IReferenceable.class ) );
		interfaces.add( Type.getType( IType.class ) );
		interfaces.add( Type.getType( Serializable.class ) );
		List<MethodNode>	interfaceMethods	= List.of();
		BoxExpression		implementsValue		= boxClass.getAnnotations().stream()
		    .filter( it -> it.getKey().getValue().equalsIgnoreCase( "implements" ) )
		    .findFirst()
		    .map( BoxAnnotation::getValue )
		    .orElse( null );
		if ( implementsValue instanceof BoxStringLiteral str ) {
			String	implementsStringList		= str.getValue();
			// Collect and trim all strings starting with "java:"
			Array	implementsArray				= ListUtil.asList( implementsStringList, "," ).stream()
			    .map( String::valueOf )
			    .map( String::trim )
			    .filter( it -> it.toLowerCase().startsWith( "java:" ) )
			    .map( it -> it.substring( 5 ) )
			    .collect( BLCollector.toArray() );
			var		interfaceProxyDefinition	= InterfaceProxyService.generateDefinition( new ScriptingRequestBoxContext(), implementsArray );
			// TODO: Remove methods that already have a @overrideJava UDF definition to avoid duplicates
			interfaces.addAll( interfaceProxyDefinition.interfaces().stream().map( iface -> Type.getType( "L" + iface.replace( '.', '/' ) + ";" ) ).toList() );
			interfaceMethods = interfaceProxyDefinition.methods().stream()
			    .map( method -> AsmHelper.dereferenceAndInvoke( method.getName(), Type.getType( method ), type ) )
			    .toList();
		}

		Type				superclass		= Type.getType( Object.class );
		boolean				isJavaExtends;
		List<MethodNode>	extendsMethods	= List.of();
		BoxExpression		extendsValue	= boxClass.getAnnotations().stream()
		    .filter( it -> it.getKey().getValue().equalsIgnoreCase( "extends" ) )
		    .findFirst()
		    .map( BoxAnnotation::getValue )
		    .orElse( null );
		if ( extendsValue instanceof BoxStringLiteral str ) {
			String extendsStringValue = str.getValue().trim();
			if ( extendsStringValue.toLowerCase().startsWith( "java:" ) ) {
				superclass		= Type.getType( "L" + extendsStringValue.substring( 5 ).replace( '.', '/' ) + ";" );
				isJavaExtends	= true;
				// search for UDFs that need a proxy created
				extendsMethods	= boxClass.getDescendantsOfType( BoxFunctionDeclaration.class )
				    .stream()
				    .filter( it -> it.getAnnotations().stream().anyMatch( anno -> anno.getKey().getValue().equalsIgnoreCase( EXTENDS_ANNOTATION_MARKER ) ) )
				    .map( func -> {
									    BoxReturnType boxReturnType	= func.getType();
									    BoxType		boxType			= BoxType.Any;
									    String		fqn				= null;
									    if ( boxReturnType != null ) {
										    boxType = boxReturnType.getType();
										    if ( boxType.equals( BoxType.Fqn ) ) {
											    fqn = boxReturnType.getFqn();
										    }
									    }
									    String returnTypeString = ( boxType.equals( BoxType.Fqn ) ? fqn : boxType.getSymbol() );
									    if ( returnTypeString.equalsIgnoreCase( "Object" ) ) {
										    returnTypeString = "java.lang.Object";
									    }
									    // Type returnType = Type
									    // .getType( "L" + ( boxType.equals( BoxType.Fqn ) ? fqn : boxType.getSymbol() ).replace( '.', '/' ) + ";" );
									    // TODO this needs to be improved substantially
									    Type						returnType		= switch ( returnTypeString ) {
																														    case "void" -> Type.VOID_TYPE;
																														    case "long" -> Type
																														        .getType( Long.class );
																														    default -> Type.getType( "L"
																														        + returnTypeString.replace( '.',
																														            '/' )
																														        + ";" );
																													    };
									    List<BoxArgumentDeclaration> parameters		= func.getArgs();
									    Type[]						parameterTypes	= new Type[ parameters.size() ];
									    for ( int i = 0; i < parameters.size(); i++ ) {
										    BoxArgumentDeclaration parameter = parameters.get( i );
										    parameterTypes[ i ] = Type.getType( "L" + parameter.getType().replace( '.', '/' ) + ";" );

									    }
									    return AsmHelper.dereferenceAndInvoke( func.getName(), Type.getMethodType( returnType, parameterTypes ), type );
								    } )
				    .toList();
			} else {
				isJavaExtends = false;
			}
		} else {
			isJavaExtends = false;
		}

		ClassNode classNode = new ClassNode();
		transpiler.setOwningClass( classNode );
		transpiler.setProperty( "enclosingClassInternalName", type.getInternalName() );

		AsmHelper.init( classNode, false, type, superclass, cv -> {
			classNode.visitSource( filePath, null );
		}, methodVisitor -> {
			methodVisitor.visitVarInsn( Opcodes.ALOAD, 0 );
			methodVisitor.visitTypeInsn( Opcodes.NEW, Type.getInternalName( ClassVariablesScope.class ) );
			methodVisitor.visitInsn( Opcodes.DUP );
			methodVisitor.visitVarInsn( Opcodes.ALOAD, 0 );
			methodVisitor.visitMethodInsn( Opcodes.INVOKESPECIAL,
			    Type.getInternalName( ClassVariablesScope.class ),
			    "<init>",
			    Type.getMethodDescriptor( Type.VOID_TYPE, Type.getType( IClassRunnable.class ) ),
			    false );
			methodVisitor.visitFieldInsn( Opcodes.PUTFIELD, type.getInternalName(), "variablesScope", Type.getDescriptor( VariablesScope.class ) );

			methodVisitor.visitVarInsn( Opcodes.ALOAD, 0 );
			methodVisitor.visitTypeInsn( Opcodes.NEW, Type.getInternalName( ThisScope.class ) );
			methodVisitor.visitInsn( Opcodes.DUP );
			methodVisitor.visitMethodInsn( Opcodes.INVOKESPECIAL,
			    Type.getInternalName( ThisScope.class ),
			    "<init>",
			    Type.getMethodDescriptor( Type.VOID_TYPE ),
			    false );
			methodVisitor.visitFieldInsn( Opcodes.PUTFIELD, type.getInternalName(), "thisScope", Type.getDescriptor( ThisScope.class ) );

			methodVisitor.visitVarInsn( Opcodes.ALOAD, 0 );
			transpiler.createKey( boxClassName ).forEach( abstractInsnNode -> abstractInsnNode.accept( methodVisitor ) );
			methodVisitor.visitFieldInsn( Opcodes.PUTFIELD, type.getInternalName(), "name", Type.getDescriptor( Key.class ) );

			methodVisitor.visitVarInsn( Opcodes.ALOAD, 0 );
			methodVisitor.visitTypeInsn( Opcodes.NEW, Type.getInternalName( ArrayList.class ) );
			methodVisitor.visitInsn( Opcodes.DUP );
			methodVisitor.visitMethodInsn( Opcodes.INVOKESPECIAL, Type.getInternalName( ArrayList.class ), "<init>", Type.getMethodDescriptor( Type.VOID_TYPE ),
			    false );
			methodVisitor.visitFieldInsn( Opcodes.PUTFIELD, type.getInternalName(), "interfaces", Type.getDescriptor( List.class ) );

		}, interfaces.toArray( Type[]::new ) );

		interfaceMethods.forEach( methodNode -> methodNode.accept( classNode ) );
		extendsMethods.forEach( methodNode -> methodNode.accept( classNode ) );

		classNode.visitField( Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL | Opcodes.ACC_STATIC, "serialVersionUID", Type.getDescriptor( long.class ), null, 1L )
		    .visitEnd();

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
		AsmHelper.addStaticFieldGetterWithStaticGetter( classNode,
		    type,
		    "annotations",
		    "getAnnotations",
		    "getAnnotationsStatic",
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
		    "properties",
		    "getProperties",
		    Type.getType( Map.class ),
		    null );
		AsmHelper.addStaticFieldGetter( classNode,
		    type,
		    "getterLookup",
		    "getGetterLookup",
		    Type.getType( Map.class ),
		    null );
		AsmHelper.addStaticFieldGetter( classNode,
		    type,
		    "setterLookup",
		    "getSetterLookup",
		    Type.getType( Map.class ),
		    null );
		AsmHelper.addStaticFieldGetter( classNode,
		    type,
		    "isJavaExtends",
		    "isJavaExtends",
		    Type.BOOLEAN_TYPE,
		    isJavaExtends ? 1 : 0 );
		AsmHelper.addStaticFieldGetterWithStaticGetter( classNode,
		    type,
		    "staticScope",
		    "getStaticScope",
		    "getStaticScopeStatic",
		    Type.getType( StaticScope.class ),
		    null );

		classNode.visitField( Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL,
		    "keys",
		    Type.getDescriptor( Key[].class ),
		    null,
		    null ).visitEnd();
		classNode.visitField( Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
		    "staticInitialized",
		    Type.getDescriptor( boolean.class ),
		    null,
		    0 ).visitEnd();

		// classNode.visitField( Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC,
		// "compileTimeMethodNames",
		// Type.getDescriptor( Set.class ),
		// null,
		// null ).visitEnd();
		AsmHelper.addPrviateStaticFieldGetter( classNode,
		    type,
		    "compileTimeMethodNames",
		    "getCompileTimeMethodNames",
		    Type.getType( Set.class ),
		    null );

		AsmHelper.addPrviateStaticFieldGetter( classNode,
		    type,
		    "abstractMethods",
		    "getAbstractMethods",
		    Type.getType( Map.class ),
		    null );
		// TODO this is on the right track but needs need to match the body of the java version
		MethodVisitor getAllAbstractMethodsMethodVisitor = classNode.visitMethod( Opcodes.ACC_PUBLIC,
		    "getAllAbstractMethods",
		    Type.getMethodDescriptor( Type.getType( Map.class ) ),
		    null,
		    null );
		getAllAbstractMethodsMethodVisitor.visitCode();
		getAllAbstractMethodsMethodVisitor.visitVarInsn( Opcodes.ALOAD, 0 );
		getAllAbstractMethodsMethodVisitor.visitFieldInsn( Opcodes.GETSTATIC,
		    type.getInternalName(),
		    "abstractMethods",
		    Type.getType( Map.class ).getDescriptor() );
		getAllAbstractMethodsMethodVisitor.visitInsn( Type.getType( Map.class ).getOpcode( Opcodes.IRETURN ) );
		getAllAbstractMethodsMethodVisitor.visitMaxs( 0, 0 );
		getAllAbstractMethodsMethodVisitor.visitEnd();

		MethodVisitor writeReplaceMethodVisitor = classNode.visitMethod( Opcodes.ACC_PRIVATE,
		    "writeReplace",
		    Type.getMethodDescriptor( Type.getType( Object.class ) ),
		    null,
		    new String[] { Type.getInternalName( ObjectStreamException.class ) } );
		writeReplaceMethodVisitor.visitCode();
		// return ObjectMarshaller.serializeClass( this );
		writeReplaceMethodVisitor.visitVarInsn( Opcodes.ALOAD, 0 );
		writeReplaceMethodVisitor.visitMethodInsn(
		    Opcodes.INVOKESTATIC,
		    Type.getInternalName( ObjectMarshaller.class ),
		    "serializeClass",
		    Type.getMethodDescriptor( Type.getType( Object.class ), Type.getType( IClassRunnable.class ) ),
		    false
		);
		writeReplaceMethodVisitor.visitInsn( Type.getType( Map.class ).getOpcode( Opcodes.IRETURN ) );
		writeReplaceMethodVisitor.visitMaxs( 0, 0 );
		writeReplaceMethodVisitor.visitEnd();

		defineLookupPrivateMethod( transpiler, classNode, type );
		defineLookupPrivateField( transpiler, classNode, type );

		AsmHelper.addPrivateFieldGetter( classNode,
		    type,
		    "variablesScope",
		    "getVariablesScope",
		    Type.getType( VariablesScope.class ),
		    null );
		AsmHelper.addPrivateFieldGetter( classNode,
		    type,
		    "thisScope",
		    "getThisScope",
		    Type.getType( ThisScope.class ),
		    null );
		AsmHelper.addPrivateFieldGetter( classNode,
		    type,
		    "name",
		    "bxGetName",
		    Type.getType( Key.class ),
		    null );
		AsmHelper.addPrivateFieldGetter( classNode,
		    type,
		    "interfaces",
		    "getInterfaces",
		    Type.getType( List.class ),
		    null );
		AsmHelper.addPrivateFieldGetterAndSetter( classNode,
		    type,
		    "_super",
		    "getSuper",
		    "_setSuper",
		    Type.getType( IClassRunnable.class ),
		    null );
		AsmHelper.addPrivateFieldGetterAndSetter( classNode,
		    type,
		    "child",
		    "getChild",
		    "setChild",
		    Type.getType( IClassRunnable.class ),
		    null );
		AsmHelper.addPrivateFieldGetterAndSetter( classNode,
		    type,
		    "canOutput",
		    "getCanOutput",
		    "setCanOutput",
		    Type.getType( Boolean.class ),
		    null );
		AsmHelper.addPrivateFieldGetterAndSetter( classNode,
		    type,
		    "$bx",
		    "_getbx",
		    "_setbx",
		    Type.getType( BoxMeta.class ),
		    null );
		AsmHelper.addPrivateFieldGetterAndSetter( classNode,
		    type,
		    "canInvokeImplicitAccessor",
		    "getCanInvokeImplicitAccessor",
		    "setCanInvokeImplicitAccessor",
		    Type.getType( Boolean.class ),
		    null );

		AsmHelper.boxClassSupport( classNode, "pseudoConstructor", Type.VOID_TYPE, Type.getType( IBoxContext.class ) );
		AsmHelper.boxClassSupport( classNode, "canOutput", Type.getType( Boolean.class ) );
		AsmHelper.boxClassSupport( classNode, "getBoxMeta", Type.getType( BoxMeta.class ) );
		AsmHelper.boxClassSupport( classNode, "getMetaData", Type.getType( IStruct.class ) );
		AsmHelper.boxClassSupport( classNode, "asString", Type.getType( String.class ) );
		AsmHelper.boxClassSupport( classNode, "canInvokeImplicitAccessor", Type.getType( Boolean.class ), Type.getType( IBoxContext.class ) );
		AsmHelper.boxClassSupport( classNode, "setSuper", Type.VOID_TYPE, Type.getType( IClassRunnable.class ) );
		AsmHelper.boxClassSupport( classNode, "getBottomClass", Type.getType( IClassRunnable.class ) );
		AsmHelper.boxClassSupport( classNode, "assign", Type.getType( Object.class ), Type.getType( IBoxContext.class ), Type.getType( Key.class ),
		    Type.getType( Object.class ) );
		AsmHelper.boxClassSupport( classNode, "dereference", Type.getType( Object.class ), Type.getType( IBoxContext.class ), Type.getType( Key.class ),
		    Type.getType( Boolean.class ) );
		AsmHelper.boxClassSupport( classNode, "dereferenceAndInvoke", Type.getType( Object.class ), Type.getType( IBoxContext.class ),
		    Type.getType( Key.class ), Type.getType( Object[].class ), Type.getType( Boolean.class ) );
		AsmHelper.boxClassSupport( classNode, "dereferenceAndInvoke", Type.getType( Object.class ), Type.getType( IBoxContext.class ),
		    Type.getType( Key.class ), Type.getType( Map.class ), Type.getType( Boolean.class ) );
		AsmHelper.boxClassSupport( classNode, "registerInterface", Type.VOID_TYPE, Type.getType( BoxInterface.class ) );

		// these imports need to happen before any methods are processed - the actual nodes will be used later on in the static init section
		List<List<AbstractInsnNode>> imports = new ArrayList<>();
		for ( BoxImport statement : boxClass.getImports() ) {
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
			    List<AbstractInsnNode> psuedoBody = new ArrayList<>();
			    List<AbstractInsnNode> body		= boxClass.getBody()
			        .stream()
			        .flatMap( statement -> transpiler.transform( statement, TransformerContext.NONE, ReturnValueContext.EMPTY ).stream() )
			        .collect( Collectors.toList() );
			    psuedoBody.addAll( transpiler.getUDFRegistrations() );
			    psuedoBody.addAll( body );

			    psuedoBody.add( new VarInsnNode( Opcodes.ALOAD, 0 ) );
			    psuedoBody.add( new VarInsnNode( Opcodes.ALOAD, 1 ) );

			    psuedoBody.add(
			        new MethodInsnNode( Opcodes.INVOKESTATIC,
			            Type.getInternalName( BoxClassSupport.class ),
			            "defaultProperties",
			            Type.getMethodDescriptor( Type.VOID_TYPE, Type.getType( IClassRunnable.class ), Type.getType( IBoxContext.class ) ),
			            false
			        )
			    );

			    return psuedoBody;
		    }
		);

		AsmHelper.methodWithContextAndClassLocator( classNode, "staticInitializer", Type.getType( IBoxContext.class ), Type.VOID_TYPE, true, transpiler, false,
		    () -> {
			    List<AbstractInsnNode> staticNodes = ( List<AbstractInsnNode> ) transpiler.getBoxStaticInitializers()
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
			        .collect( Collectors.toList() );

			    boxClass.getDescendantsOfType( BoxFunctionDeclaration.class, ( expr ) -> {
				    BoxFunctionDeclaration func = ( BoxFunctionDeclaration ) expr;

				    return func.getModifiers().contains( BoxMethodDeclarationModifier.STATIC );
			    } ).forEach( func -> {
				    staticNodes.addAll( transpiler.transform( func, TransformerContext.NONE ) );
			    } );

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

			List<AbstractInsnNode>			annotations		= transpiler.transformAnnotations( boxClass.getAnnotations() );
			List<AbstractInsnNode>			documenation	= transpiler.transformDocumentation( boxClass.getDocumentation() );
			List<List<AbstractInsnNode>>	properties		= transpiler.transformProperties( type, boxClass.getProperties(), sourceType );

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

			methodVisitor.visitLdcInsn( 0 );
			methodVisitor.visitFieldInsn( Opcodes.PUTSTATIC,
			    type.getInternalName(),
			    "staticInitialized",
			    Type.getDescriptor( boolean.class ) );

			methodVisitor.visitTypeInsn( Opcodes.NEW, Type.getInternalName( StaticScope.class ) );
			methodVisitor.visitInsn( Opcodes.DUP );
			methodVisitor.visitMethodInsn( Opcodes.INVOKESPECIAL,
			    Type.getInternalName( StaticScope.class ),
			    "<init>",
			    Type.getMethodDescriptor( Type.VOID_TYPE ),
			    false );
			methodVisitor.visitFieldInsn( Opcodes.PUTSTATIC,
			    type.getInternalName(),
			    "staticScope",
			    Type.getDescriptor( StaticScope.class ) );

			methodVisitor.visitLdcInsn( isJavaExtends ? 1 : 0 );
			methodVisitor.visitFieldInsn( Opcodes.PUTSTATIC, type.getInternalName(), "isJavaExtends", Type.getDescriptor( boolean.class ) );

			methodVisitor.visitLdcInsn( 1L );
			methodVisitor.visitFieldInsn( Opcodes.PUTSTATIC, type.getInternalName(), "serialVersionUID", Type.getDescriptor( long.class ) );

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

			properties.get( 0 ).forEach( abstractInsnNode -> abstractInsnNode.accept( methodVisitor ) );
			methodVisitor.visitFieldInsn( Opcodes.PUTSTATIC,
			    type.getInternalName(),
			    "properties",
			    Type.getDescriptor( Map.class ) );

			properties.get( 1 ).forEach( abstractInsnNode -> abstractInsnNode.accept( methodVisitor ) );
			methodVisitor.visitFieldInsn( Opcodes.PUTSTATIC,
			    type.getInternalName(),
			    "getterLookup",
			    Type.getDescriptor( Map.class ) );

			properties.get( 2 ).forEach( abstractInsnNode -> abstractInsnNode.accept( methodVisitor ) );
			methodVisitor.visitFieldInsn( Opcodes.PUTSTATIC,
			    type.getInternalName(),
			    "setterLookup",
			    Type.getDescriptor( Map.class ) );

			generateSetOfCompileTimeMethodNames( transpiler, boxClass ).forEach( node -> node.accept( methodVisitor ) );
			methodVisitor.visitFieldInsn( Opcodes.PUTSTATIC, type.getInternalName(), "compileTimeMethodNames", Type.getDescriptor( Set.class ) );

			AsmHelper.generateMapOfAbstractMethodNames( transpiler, boxClass ).forEach( node -> node.accept( methodVisitor ) );
			methodVisitor.visitFieldInsn( Opcodes.PUTSTATIC, type.getInternalName(), "abstractMethods", Type.getDescriptor( Map.class ) );
		} );

		return classNode;
	}

	private static List<AbstractInsnNode> generateSetOfCompileTimeMethodNames( Transpiler transpiler, BoxClass boxClass ) {
		List<List<AbstractInsnNode>>	methodKeyLists	= boxClass.getDescendantsOfType( BoxFunctionDeclaration.class )
		    .stream()
		    .map( BoxFunctionDeclaration::getName )
		    .map( transpiler::createKey )
		    .collect( java.util.stream.Collectors.toList() );

		List<AbstractInsnNode>			nodes			= new ArrayList<AbstractInsnNode>();

		nodes.addAll( AsmHelper.array( Type.getType( Key.class ), methodKeyLists ) );
		nodes.add(
		    new MethodInsnNode(
		        Opcodes.INVOKESTATIC,
		        Type.getInternalName( Set.class ),
		        "of",
		        Type.getMethodDescriptor( Type.getType( Set.class ), Type.getType( Object[].class ) ),
		        true
		    )
		);

		return nodes;

	}

	private static void defineLookupPrivateMethod( Transpiler transpiler, ClassNode classNode, Type thisType ) {
		/**
		 * This code MUST be inside the class to allow for the lookupPrivate method to work
		 * This proxy is called from the dynamic interop service when calling a super method
		 * while using java extends, and it will return the method handle for the corresponding
		 * method in the super class.
		 */
		// public MethodHandle lookupPrivateMethod( Method method ) {
		// try {
		// return MethodHandles.lookup().findSpecial(
		// method.getDeclaringClass(),
		// method.getName(),
		// MethodType.methodType(method.getReturnType(), method.getParameterTypes()),
		// this.getClass()
		// );
		// } catch (NoSuchMethodException | IllegalAccessException e) {
		// throw new BoxRuntimeException( "Error getting Java super class method " + method.getName(), e );
		// }
		// }
		Type			returnType		= Type.getType( MethodHandle.class );
		MethodVisitor	methodVisitor	= classNode.visitMethod(
		    Opcodes.ACC_PUBLIC,
		    "lookupPrivateMethod",
		    Type.getMethodDescriptor( returnType, Type.getType( Method.class ) ),
		    null,
		    null );
		methodVisitor.visitCode();

		Label tryStart = new Label();

		methodVisitor.visitLabel( tryStart );

		methodVisitor.visitMethodInsn(
		    Opcodes.INVOKESTATIC,
		    Type.getType( MethodHandles.class ).getInternalName(),
		    "lookup",
		    Type.getMethodDescriptor( Type.getType( Lookup.class ) ),
		    false
		);

		methodVisitor.visitVarInsn( Opcodes.ALOAD, 1 );
		methodVisitor.visitMethodInsn(
		    Opcodes.INVOKEINTERFACE,
		    Type.getType( Member.class ).getInternalName(),
		    "getDeclaringClass",
		    Type.getMethodDescriptor( CLASS_TYPE ),
		    true
		);

		methodVisitor.visitVarInsn( Opcodes.ALOAD, 1 );
		methodVisitor.visitMethodInsn(
		    Opcodes.INVOKEINTERFACE,
		    Type.getType( Member.class ).getInternalName(),
		    "getName",
		    Type.getMethodDescriptor( Type.getType( String.class ) ),
		    true
		);

		// start MethodType.methodType(method.getReturnType(), method.getParameterTypes()),
		methodVisitor.visitVarInsn( Opcodes.ALOAD, 1 );
		methodVisitor.visitMethodInsn(
		    Opcodes.INVOKEVIRTUAL,
		    Type.getType( Method.class ).getInternalName(),
		    "getReturnType",
		    Type.getMethodDescriptor( CLASS_TYPE ),
		    false
		);

		methodVisitor.visitVarInsn( Opcodes.ALOAD, 1 );
		methodVisitor.visitMethodInsn(
		    Opcodes.INVOKEVIRTUAL,
		    Type.getType( Method.class ).getInternalName(),
		    "getParameterTypes",
		    Type.getMethodDescriptor( CLASS_ARRAY_TYPE ),
		    false
		);

		methodVisitor.visitMethodInsn(
		    Opcodes.INVOKESTATIC,
		    Type.getType( MethodType.class ).getInternalName(),
		    "methodType",
		    Type.getMethodDescriptor( Type.getType( MethodType.class ), CLASS_TYPE, CLASS_ARRAY_TYPE ),
		    false
		);
		// end MethodType.methodType(method.getReturnType(), method.getParameterTypes()),

		methodVisitor.visitLdcInsn( thisType );
		// methodVisitor.visitFieldInsn( Opcodes.GETSTATIC, thisType.getInternalName(), "class", CLASS_TYPE.getDescriptor() );

		methodVisitor.visitMethodInsn(
		    Opcodes.INVOKEVIRTUAL,
		    Type.getType( Lookup.class ).getInternalName(),
		    "findSpecial",
		    Type.getMethodDescriptor( Type.getType( MethodHandle.class ), CLASS_TYPE, Type.getType( String.class ), Type.getType( MethodType.class ),
		        CLASS_TYPE ),
		    false
		);

		methodVisitor.visitInsn( Opcodes.ARETURN );

		Label	tryEnd	= new Label();
		Label	handler	= new Label();

		methodVisitor.visitLabel( tryEnd );
		methodVisitor.visitLabel( handler );

		methodVisitor.visitVarInsn( Opcodes.ASTORE, 2 );

		methodVisitor.visitTryCatchBlock( tryStart, tryEnd, handler, Type.getInternalName( NoSuchMethodException.class ) );
		methodVisitor.visitTryCatchBlock( tryStart, tryEnd, handler, Type.getInternalName( IllegalAccessException.class ) );

		methodVisitor.visitTypeInsn( Opcodes.NEW, Type.getInternalName( BoxRuntimeException.class ) );
		methodVisitor.visitInsn( Opcodes.DUP );

		methodVisitor.visitLdcInsn( "Error getting Java super class method " );
		methodVisitor.visitVarInsn( Opcodes.ALOAD, 1 );
		methodVisitor.visitMethodInsn(
		    Opcodes.INVOKEINTERFACE,
		    Type.getType( Member.class ).getInternalName(),
		    "getName",
		    Type.getMethodDescriptor( Type.getType( String.class ) ),
		    true
		);

		methodVisitor.visitMethodInsn(
		    Opcodes.INVOKEVIRTUAL,
		    Type.getInternalName( String.class ),
		    "concat",
		    Type.getMethodDescriptor( Type.getType( String.class ), Type.getType( String.class ) ),
		    false
		);

		methodVisitor.visitVarInsn( Opcodes.ALOAD, 2 );

		methodVisitor.visitMethodInsn( Opcodes.INVOKESPECIAL,
		    Type.getInternalName( BoxRuntimeException.class ),
		    "<init>",
		    Type.getMethodDescriptor( Type.VOID_TYPE, Type.getType( String.class ), Type.getType( Throwable.class ) ),
		    false );

		methodVisitor.visitInsn( Opcodes.ATHROW );

		methodVisitor.visitMaxs( 0, 0 );

		methodVisitor.visitEnd();
	}

	private static void defineLookupPrivateField( Transpiler transpiler, ClassNode classNode, Type thisType ) {
		/**
		 * Same as above
		 */
		// public MethodHandle lookupPrivateField( Field field ) {
		// try {
		// return MethodHandles.lookup().unreflectGetter( field );
		// } catch ( IllegalAccessException e) {
		// throw new BoxRuntimeException( "Error getting Java super class field " + field.getName(), e );
		// }
		// }
		Type			returnType		= Type.getType( MethodHandle.class );
		MethodVisitor	methodVisitor	= classNode.visitMethod(
		    Opcodes.ACC_PUBLIC,
		    "lookupPrivateField",
		    Type.getMethodDescriptor( returnType, Type.getType( Field.class ) ),
		    null,
		    null );
		methodVisitor.visitCode();

		Label tryStart = new Label();

		methodVisitor.visitLabel( tryStart );

		methodVisitor.visitMethodInsn(
		    Opcodes.INVOKESTATIC,
		    Type.getType( MethodHandles.class ).getInternalName(),
		    "lookup",
		    Type.getMethodDescriptor( Type.getType( Lookup.class ) ),
		    false
		);

		methodVisitor.visitVarInsn( Opcodes.ALOAD, 1 );

		methodVisitor.visitMethodInsn(
		    Opcodes.INVOKEVIRTUAL,
		    Type.getType( Lookup.class ).getInternalName(),
		    "unreflectGetter",
		    Type.getMethodDescriptor( Type.getType( MethodHandle.class ), Type.getType( Field.class ) ),
		    false
		);

		methodVisitor.visitInsn( Opcodes.ARETURN );

		Label	tryEnd	= new Label();
		Label	handler	= new Label();

		methodVisitor.visitLabel( tryEnd );
		methodVisitor.visitLabel( handler );

		methodVisitor.visitVarInsn( Opcodes.ASTORE, 2 );

		methodVisitor.visitTryCatchBlock( tryStart, tryEnd, handler, Type.getInternalName( NoSuchMethodException.class ) );
		methodVisitor.visitTryCatchBlock( tryStart, tryEnd, handler, Type.getInternalName( IllegalAccessException.class ) );

		methodVisitor.visitTypeInsn( Opcodes.NEW, Type.getInternalName( BoxRuntimeException.class ) );
		methodVisitor.visitInsn( Opcodes.DUP );

		methodVisitor.visitLdcInsn( "Error getting Java super class field " );
		methodVisitor.visitVarInsn( Opcodes.ALOAD, 1 );
		methodVisitor.visitMethodInsn(
		    Opcodes.INVOKEINTERFACE,
		    Type.getType( Field.class ).getInternalName(),
		    "getName",
		    Type.getMethodDescriptor( Type.getType( String.class ) ),
		    true
		);

		methodVisitor.visitMethodInsn(
		    Opcodes.INVOKEVIRTUAL,
		    Type.getInternalName( String.class ),
		    "concat",
		    Type.getMethodDescriptor( Type.getType( String.class ), Type.getType( String.class ) ),
		    false
		);

		methodVisitor.visitVarInsn( Opcodes.ALOAD, 2 );

		methodVisitor.visitMethodInsn( Opcodes.INVOKESPECIAL,
		    Type.getInternalName( BoxRuntimeException.class ),
		    "<init>",
		    Type.getMethodDescriptor( Type.VOID_TYPE, Type.getType( String.class ), Type.getType( Throwable.class ) ),
		    false );

		methodVisitor.visitInsn( Opcodes.ATHROW );

		methodVisitor.visitMaxs( 0, 0 );

		methodVisitor.visitEnd();
	}

}
