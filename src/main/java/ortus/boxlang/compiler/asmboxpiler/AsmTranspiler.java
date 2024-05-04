package ortus.boxlang.compiler.asmboxpiler;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import ortus.boxlang.compiler.asmboxpiler.transformer.Transformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.TransformerContext;
import ortus.boxlang.compiler.asmboxpiler.transformer.expression.*;
import ortus.boxlang.compiler.asmboxpiler.transformer.statement.BoxFunctionDeclarationTransformer;
import ortus.boxlang.compiler.ast.*;
import ortus.boxlang.compiler.ast.expression.*;
import ortus.boxlang.compiler.ast.statement.*;
import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.loader.ImportDefinition;
import ortus.boxlang.runtime.runnables.AbstractBoxClass;
import ortus.boxlang.runtime.runnables.IClassRunnable;
import ortus.boxlang.runtime.scopes.ClassVariablesScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.ThisScope;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Property;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.meta.BoxMeta;
import ortus.boxlang.runtime.types.meta.ClassMeta;
import ortus.boxlang.runtime.types.util.MapHelper;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

public class AsmTranspiler extends Transpiler {

	private static HashMap<Class<?>, Transformer> registry = new HashMap<>();

	public AsmTranspiler() {
		// TODO: instance write to static field. Seems like an oversight in Java version (retained until clarified).
		registry.put( BoxStringLiteral.class, new BoxStringLiteralTransformer( this ) );
		registry.put( BoxIntegerLiteral.class, new BoxIntegerLiteralTransformer( this ) );
		registry.put( BoxExpressionStatement.class, new BoxExpressionStatementTransformer( this ) );
		registry.put( BoxAssignment.class, new BoxAssignmentTransformer( this ) );
		registry.put( BoxArrayLiteral.class, new BoxArrayLiteralTransformer( this ) );
		registry.put( BoxFunctionDeclaration.class, new BoxFunctionDeclarationTransformer( this ) );
		registry.put( BoxFunctionInvocation.class, new BoxFunctionInvocationTransformer( this ) );
		registry.put( BoxArgument.class, new BoxArgumentTransformer( this ) );
		registry.put( BoxStringConcat.class, new BoxStringConcatTransformer( this ) );
		registry.put( BoxStringInterpolation.class, new BoxStringInterpolationTransformer( this ) );
		registry.put( BoxMethodInvocation.class, new BoxMethodInvocationTransformer( this ) );
		registry.put( BoxReturn.class, new BoxReturnTransformer( this ) );
		registry.put( BoxStructLiteral.class, new BoxStructLiteralTransformer( this ) );
		registry.put( BoxIdentifier.class, new BoxIdentifierTransformer( this ) );
		registry.put( BoxBinaryOperation.class, new BoxBinaryOperationTransformer( this ) );
		registry.put( BoxDotAccess.class, new BoxAccessTransformer( this ) );
		registry.put( BoxArrayAccess.class, new BoxAccessTransformer( this ) );
		registry.put( BoxArgumentDeclaration.class, new BoxArgumentDeclarationTransformer( this ) );
		registry.put( BoxNewOperation.class, new BoxNewOperationTransformer( this ) );
		registry.put( BoxFQN.class, new BoxFQNTransformer( this ) );
		registry.put( BoxLambda.class, new BoxLambdaTransformer( this ) );
		registry.put( BoxBooleanLiteral.class, new BoxBooleanLiteralTransformer( this ) );
		registry.put( BoxNull.class, new BoxNullTransformer( this ) );

	}

	@Override
	public ClassNode transpile( BoxScript boxScript ) throws BoxRuntimeException {
		Type		type		= Type.getType( "L" + getProperty( "packageName" ).replace( '.', '/' ) + "/" + getProperty( "classname" ) + ";" );
		ClassNode	classNode	= new ClassNode();

		AsmHelper.init( classNode, true, type, ortus.boxlang.runtime.runnables.BoxScript.class, methodVisitor -> {
		} );
		classNode.visitField( Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL | Opcodes.ACC_STATIC,
		    "keys",
		    Type.getDescriptor( ( Key[].class ) ),
		    null,
		    null ).visitEnd();
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
		    Type.getType( Path.class ),
		    null );
		AsmHelper.addStaticFieldGetter( classNode,
		    type,
		    "sourceType",
		    "getSourceType",
		    Type.getType( BoxSourceType.class ),
		    null );

		AsmHelper.methodWithContextAndClassLocator( classNode, "_invoke", Type.getType( IBoxContext.class ), Type.getType( Object.class ), true,
		    methodVisitor -> {
			    boxScript.getChildren().forEach( child -> transform( child, TransformerContext.NONE ).forEach( value -> value.accept( methodVisitor ) ) );
		    } );

		AsmHelper.complete( classNode, type, methodVisitor -> {
			methodVisitor.visitLdcInsn( getKeys().size() );
			methodVisitor.visitTypeInsn( Opcodes.ANEWARRAY, Type.getInternalName( Key.class ) );
			int index = 0;
			for ( BoxExpression expression : getKeys().values() ) {
				methodVisitor.visitInsn( Opcodes.DUP );
				methodVisitor.visitLdcInsn( index++ );
				transform( expression, TransformerContext.NONE ).forEach( methodInsnNode -> methodInsnNode.accept( methodVisitor ) );
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

			methodVisitor.visitMethodInsn( Opcodes.INVOKESTATIC,
			    Type.getInternalName( List.class ),
			    "of",
			    Type.getMethodDescriptor( Type.getType( List.class ) ),
			    true );
			methodVisitor.visitFieldInsn( Opcodes.PUTSTATIC,
			    type.getInternalName(),
			    "imports",
			    Type.getDescriptor( List.class ) ); // TODO: imports

			methodVisitor.visitLdcInsn( "unknown" );
			methodVisitor.visitLdcInsn( 0 );
			methodVisitor.visitTypeInsn( Opcodes.ANEWARRAY, Type.getInternalName( String.class ) );
			methodVisitor.visitMethodInsn( Opcodes.INVOKESTATIC,
			    Type.getInternalName( Paths.class ),
			    "get",
			    Type.getMethodDescriptor( Type.getType( Path.class ), Type.getType( String.class ), Type.getType( String[].class ) ),
			    false );
			methodVisitor.visitFieldInsn( Opcodes.PUTSTATIC,
			    type.getInternalName(),
			    "path",
			    Type.getDescriptor( Path.class ) );

			methodVisitor.visitFieldInsn( Opcodes.GETSTATIC,
			    Type.getInternalName( BoxSourceType.class ),
			    "BOXSCRIPT",
			    Type.getDescriptor( BoxSourceType.class ) );
			methodVisitor.visitFieldInsn( Opcodes.PUTSTATIC,
			    type.getInternalName(),
			    "sourceType",
			    Type.getDescriptor( BoxSourceType.class ) );
		} );

		return classNode;
	}

	@Override
	public ClassNode transpile( BoxClass boxClass ) throws BoxRuntimeException {
		Source	source			= boxClass.getPosition().getSource();
		String	sourceType		= getProperty( "sourceType" );
		String	filePath		= source instanceof SourceFile file && file.getFile() != null ? file.getFile().getAbsolutePath()
		    : "unknown";
		String	fileName		= source instanceof SourceFile file && file.getFile() != null ? file.getFile().getName() : "unknown";
		String	boxPackageName	= getProperty( "boxPackageName" );
		String	rawBoxClassName	= boxPackageName + "." + fileName.replace( ".bx", "" ).replace( ".cfc", "" ), boxClassName;
		// trim leading . if exists
		if ( rawBoxClassName.startsWith( "." ) ) {
			boxClassName = rawBoxClassName.substring( 1 );
		} else {
			boxClassName = rawBoxClassName;
		}

		Type		type		= Type.getType( "L" + getProperty( "packageName" ).replace( '.', '/' )
		    + "/" + getProperty( "classname" ) + ";" );

		ClassNode	classNode	= new ClassNode();

		AsmHelper.init( classNode, false, type, AbstractBoxClass.class, methodVisitor -> {
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
			createKey( boxClassName ).forEach( abstractInsnNode -> abstractInsnNode.accept( methodVisitor ) );
			methodVisitor.visitFieldInsn( Opcodes.PUTFIELD, type.getInternalName(), "name", Type.getDescriptor( Key.class ) );
		} );
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
		    Type.getType( Path.class ),
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

		classNode.visitField( Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL,
		    "keys",
		    Type.getDescriptor( Key[].class ),
		    null,
		    null ).visitEnd();
		classNode.visitField( Opcodes.ACC_PUBLIC,
		    "$bx",
		    Type.getDescriptor( BoxMeta.class ),
		    null,
		    null ).visitEnd();
		classNode.visitField( Opcodes.ACC_PRIVATE,
		    "canOutput",
		    Type.getDescriptor( Boolean.class ),
		    null,
		    null ).visitEnd();

		AsmHelper.addFieldGetter( classNode,
		    type,
		    "variablesScope",
		    "getVariablesScope",
		    Type.getType( VariablesScope.class ),
		    null );
		AsmHelper.addFieldGetter( classNode,
		    type,
		    "thisScope",
		    "getThisScope",
		    Type.getType( ThisScope.class ),
		    null );
		AsmHelper.addFieldGetter( classNode,
		    type,
		    "name",
		    "getName",
		    Type.getType( Key.class ),
		    null );
		AsmHelper.addFieldGetterAndSetter( classNode,
		    type,
		    "_super",
		    "getSuper",
		    "setSuper",
		    Type.getType( IClassRunnable.class ),
		    null,
		    methodVisitor -> {
			    methodVisitor.visitVarInsn( Opcodes.ALOAD, 0 );
			    methodVisitor.visitVarInsn( Opcodes.ALOAD, 1 );
			    methodVisitor.visitMethodInsn( Opcodes.INVOKEVIRTUAL,
			        Type.getInternalName( AbstractBoxClass.class ),
			        "doSetSuper",
			        Type.getMethodDescriptor( Type.VOID_TYPE, Type.getType( IClassRunnable.class ) ),
			        false );
		    } );
		AsmHelper.addFieldGetterAndSetter( classNode,
		    type,
		    "child",
		    "getChild",
		    "setChild",
		    Type.getType( IClassRunnable.class ),
		    null,
		    methodVisitor -> {
		    } );

		MethodVisitor canOutput = classNode.visitMethod( Opcodes.ACC_PUBLIC, "canOutput", Type.getMethodDescriptor( Type.BOOLEAN_TYPE ), null, null );
		canOutput.visitCode();
		Label canOutputInitialized = new Label();
		canOutput.visitVarInsn( Opcodes.ALOAD, 0 );
		canOutput.visitFieldInsn( Opcodes.GETFIELD, type.getInternalName(), "canOutput", Type.getDescriptor( Boolean.class ) );
		canOutput.visitJumpInsn( Opcodes.IFNONNULL, canOutputInitialized );
		canOutput.visitVarInsn( Opcodes.ALOAD, 0 );
		canOutput.visitVarInsn( Opcodes.ALOAD, 0 );
		canOutput.visitMethodInsn( Opcodes.INVOKEVIRTUAL, Type.getInternalName( AbstractBoxClass.class ), "doCanOutput",
		    Type.getMethodDescriptor( Type.getType( Boolean.class ) ), false );
		canOutput.visitFieldInsn( Opcodes.PUTFIELD, type.getInternalName(), "canOutput", Type.getDescriptor( Boolean.class ) );
		canOutput.visitLabel( canOutputInitialized );
		canOutput.visitVarInsn( Opcodes.ALOAD, 0 );
		canOutput.visitFieldInsn( Opcodes.GETFIELD, type.getInternalName(), "canOutput", Type.getDescriptor( Boolean.class ) );
		canOutput.visitMethodInsn( Opcodes.INVOKEVIRTUAL, Type.getInternalName( Boolean.class ), "booleanValue", Type.getMethodDescriptor( Type.BOOLEAN_TYPE ),
		    false );
		canOutput.visitInsn( Opcodes.IRETURN );
		canOutput.visitMaxs( 0, 0 );
		canOutput.visitEnd();

		MethodVisitor getBoxMeta = classNode.visitMethod( Opcodes.ACC_PUBLIC, "getBoxMeta", Type.getMethodDescriptor( Type.getType( BoxMeta.class ) ), null,
		    null );
		getBoxMeta.visitCode();
		Label boxMetaInitialized = new Label();
		getBoxMeta.visitVarInsn( Opcodes.ALOAD, 0 );
		getBoxMeta.visitFieldInsn( Opcodes.GETFIELD, type.getInternalName(), "$bx", Type.getDescriptor( BoxMeta.class ) );
		getBoxMeta.visitJumpInsn( Opcodes.IFNONNULL, boxMetaInitialized );
		getBoxMeta.visitVarInsn( Opcodes.ALOAD, 0 );
		getBoxMeta.visitTypeInsn( Opcodes.NEW, Type.getInternalName( ClassMeta.class ) );
		getBoxMeta.visitInsn( Opcodes.DUP );
		getBoxMeta.visitVarInsn( Opcodes.ALOAD, 0 );
		getBoxMeta.visitMethodInsn( Opcodes.INVOKESPECIAL, Type.getInternalName( ClassMeta.class ), "<init>",
		    Type.getMethodDescriptor( Type.VOID_TYPE, Type.getType( IClassRunnable.class ) ), false );
		getBoxMeta.visitFieldInsn( Opcodes.PUTFIELD, type.getInternalName(), "$bx", Type.getDescriptor( BoxMeta.class ) );
		getBoxMeta.visitLabel( boxMetaInitialized );
		getBoxMeta.visitVarInsn( Opcodes.ALOAD, 0 );
		getBoxMeta.visitFieldInsn( Opcodes.GETFIELD, type.getInternalName(), "$bx", Type.getDescriptor( BoxMeta.class ) );
		getBoxMeta.visitInsn( Opcodes.ARETURN );
		getBoxMeta.visitMaxs( 0, 0 );
		getBoxMeta.visitEnd();

		AsmHelper.methodWithContextAndClassLocator( classNode, "_pseudoConstructor", Type.getType( IBoxContext.class ), Type.VOID_TYPE, false,
		    methodVisitor -> {
			    for ( BoxStatement statement : boxClass.getBody() ) {
				    transform( statement, TransformerContext.NONE ).forEach( abstractInsnNode -> abstractInsnNode.accept( methodVisitor ) );
			    }
		    } );

		AsmHelper.complete( classNode, type, methodVisitor -> {
			methodVisitor.visitMethodInsn( Opcodes.INVOKESTATIC,
			    Type.getInternalName( List.class ),
			    "of",
			    Type.getMethodDescriptor( Type.getType( List.class ) ),
			    true );
			methodVisitor.visitFieldInsn( Opcodes.PUTSTATIC,
			    type.getInternalName(),
			    "imports",
			    Type.getDescriptor( List.class ) );

			methodVisitor.visitLdcInsn( filePath );
			methodVisitor.visitLdcInsn( 0 );
			methodVisitor.visitTypeInsn( Opcodes.ANEWARRAY, Type.getInternalName( String.class ) );
			methodVisitor.visitMethodInsn( Opcodes.INVOKESTATIC,
			    Type.getInternalName( Paths.class ),
			    "get",
			    Type.getMethodDescriptor( Type.getType( Path.class ), Type.getType( String.class ), Type.getType( String[].class ) ),
			    false );
			methodVisitor.visitFieldInsn( Opcodes.PUTSTATIC,
			    type.getInternalName(),
			    "path",
			    Type.getDescriptor( Path.class ) );

			methodVisitor.visitFieldInsn( Opcodes.GETSTATIC,
			    Type.getInternalName( BoxSourceType.class ),
			    sourceType,
			    Type.getDescriptor( BoxSourceType.class ) );
			methodVisitor.visitFieldInsn( Opcodes.PUTSTATIC,
			    type.getInternalName(),
			    "sourceType",
			    Type.getDescriptor( BoxSourceType.class ) );

			methodVisitor.visitLdcInsn( getKeys().size() );
			methodVisitor.visitTypeInsn( Opcodes.ANEWARRAY, Type.getInternalName( Key.class ) );
			int index = 0;
			for ( BoxExpression expression : getKeys().values() ) {
				methodVisitor.visitInsn( Opcodes.DUP );
				methodVisitor.visitLdcInsn( index++ );
				transform( expression, TransformerContext.NONE ).forEach( methodInsnNode -> methodInsnNode.accept( methodVisitor ) );
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

			List<List<AbstractInsnNode>> imports = new ArrayList<>();
			for ( BoxImport statement : boxClass.getImports() ) {
				imports.add( transform( statement, TransformerContext.NONE ) );
			}
			AsmHelper.array( Type.getType( ImportDefinition.class ), imports ).forEach( node -> node.accept( methodVisitor ) );
			methodVisitor.visitMethodInsn( Opcodes.INVOKESTATIC,
			    Type.getInternalName( List.class ),
			    "of",
			    Type.getMethodDescriptor( Type.getType( List.class ), Type.getType( Object[].class ) ),
			    true );
			methodVisitor.visitFieldInsn( Opcodes.PUTSTATIC,
			    type.getInternalName(),
			    "imports",
			    Type.getDescriptor( List.class ) );

			transformAnnotations( boxClass.getAnnotations() ).forEach( abstractInsnNode -> abstractInsnNode.accept( methodVisitor ) );
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

			List<List<AbstractInsnNode>> properties = transformProperties( type, boxClass.getProperties() );

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
		} );

		return classNode;
	}

	@Override
	public List<AbstractInsnNode> transform( BoxNode node, TransformerContext context ) {
		Transformer transformer = registry.get( node.getClass() );
		if ( transformer != null ) {
			return transformer.transform( node, context );
		}
		throw new IllegalStateException( "unsupported: " + node.getClass().getSimpleName() + " : " + node.getSourceText() );
	}

	private List<List<AbstractInsnNode>> transformProperties( Type declaringType, List<BoxProperty> properties ) {
		List<List<AbstractInsnNode>>	members			= new ArrayList<>();
		List<List<AbstractInsnNode>>	getterLookup	= new ArrayList<>();
		List<List<AbstractInsnNode>>	setterLookup	= new ArrayList<>();
		properties.forEach( prop -> {
			List<AbstractInsnNode>	documentationStruct		= transformDocumentation( prop.getDocumentation() );
			/*
			 * normalize annotations to allow for
			 * property String userName;
			 */
			List<BoxAnnotation>		finalAnnotations		= new ArrayList<BoxAnnotation>();
			var						annotations				= prop.getAnnotations();
			int						namePosition			= annotations.stream().map( BoxAnnotation::getKey ).map( BoxFQN::getValue )
			    .map( String::toLowerCase )
			    .collect( java.util.stream.Collectors.toList() ).indexOf( "name" );
			int						typePosition			= annotations.stream().map( BoxAnnotation::getKey ).map( BoxFQN::getValue )
			    .map( String::toLowerCase )
			    .collect( java.util.stream.Collectors.toList() ).indexOf( "type" );
			int						defaultPosition			= annotations.stream().map( BoxAnnotation::getKey ).map( BoxFQN::getValue )
			    .map( String::toLowerCase )
			    .collect( java.util.stream.Collectors.toList() ).indexOf( "default" );
			int						numberOfNonValuedKeys	= ( int ) annotations.stream().map( BoxAnnotation::getValue ).filter( it -> it == null ).count();
			List<BoxAnnotation>		nonValuedKeys			= annotations.stream().filter( it -> it.getValue() == null )
			    .collect( java.util.stream.Collectors.toList() );
			BoxAnnotation			nameAnnotation			= null;
			BoxAnnotation			typeAnnotation			= null;
			BoxAnnotation			defaultAnnotation		= null;

			if ( namePosition > -1 )
				nameAnnotation = annotations.get( namePosition );
			if ( typePosition > -1 )
				typeAnnotation = annotations.get( typePosition );
			if ( defaultPosition > -1 )
				defaultAnnotation = annotations.get( defaultPosition );
			/*
			 * If there is no name, if there is more than one nonvalued keys and no type, use the first nonvalued key
			 * as the type and second nonvalued key as the name. Otherwise, if there are more than one non-valued key, use the first as the name.
			 */
			if ( namePosition == -1 ) {
				if ( numberOfNonValuedKeys > 1 && typePosition == -1 ) {
					typeAnnotation	= new BoxAnnotation( new BoxFQN( "type", null, null ),
					    new BoxStringLiteral( nonValuedKeys.get( 0 ).getKey().getValue(), null, null ), null,
					    null );
					nameAnnotation	= new BoxAnnotation( new BoxFQN( "name", null, null ),
					    new BoxStringLiteral( nonValuedKeys.get( 1 ).getKey().getValue(), null, null ), null,
					    null );
					finalAnnotations.add( nameAnnotation );
					finalAnnotations.add( typeAnnotation );
					annotations.remove( nonValuedKeys.get( 0 ) );
					annotations.remove( nonValuedKeys.get( 1 ) );
				} else if ( numberOfNonValuedKeys > 0 ) {
					nameAnnotation = new BoxAnnotation( new BoxFQN( "name", null, null ),
					    new BoxStringLiteral( nonValuedKeys.get( 0 ).getKey().getValue(), null, null ), null,
					    null );
					finalAnnotations.add( nameAnnotation );
					annotations.remove( nonValuedKeys.get( 0 ) );
				} else {
					throw new BoxRuntimeException( "Property [" + prop.getSourceText() + "] has no name" );
				}
			}
			// add type with value of any if not present
			if ( typeAnnotation == null ) {
				typeAnnotation = new BoxAnnotation( new BoxFQN( "type", null, null ), new BoxStringLiteral( "any", null, null ), null,
				    null );
				finalAnnotations.add( typeAnnotation );
			}
			// add default with value of null if not present
			if ( defaultPosition == -1 ) {
				defaultAnnotation = new BoxAnnotation( new BoxFQN( "default", null, null ), new BoxNull( null, null ), null,
				    null );
				finalAnnotations.add( defaultAnnotation );
			}
			// add remaining annotations
			finalAnnotations.addAll( annotations );

			List<AbstractInsnNode>	annotationStruct	= transformAnnotations( finalAnnotations );
			/* Process default value */
			List<AbstractInsnNode>	init;
			if ( defaultAnnotation.getValue() != null ) {
				init = transform( defaultAnnotation.getValue(), TransformerContext.NONE );
			} else {
				init = List.of( new InsnNode( Opcodes.ACONST_NULL ) );
			}
			// name and type must be simple values
			String	name;
			String	type;
			if ( nameAnnotation.getValue() instanceof BoxStringLiteral namelit ) {
				name = namelit.getValue().trim();
				if ( name.isEmpty() )
					throw new BoxRuntimeException( "Property [" + prop.getSourceText() + "] name cannot be empty" );
			} else {
				throw new BoxRuntimeException( "Property [" + prop.getSourceText() + "] name must be a simple value" );
			}
			if ( typeAnnotation.getValue() instanceof BoxStringLiteral typelit ) {
				type = typelit.getValue().trim();
				if ( type.isEmpty() )
					throw new BoxRuntimeException( "Property [" + prop.getSourceText() + "] type cannot be empty" );
			} else {
				throw new BoxRuntimeException( "Property [" + prop.getSourceText() + "] type must be a simple value" );
			}
			List<AbstractInsnNode>	jNameKey	= createKey( name );
			List<AbstractInsnNode>	jGetNameKey	= createKey( "get" + name );
			List<AbstractInsnNode>	jSetNameKey	= createKey( "set" + name );

			List<AbstractInsnNode>	javaExpr	= new ArrayList<>();
			javaExpr.add( new TypeInsnNode( Opcodes.NEW, Type.getInternalName( Property.class ) ) );
			javaExpr.add( new InsnNode( Opcodes.DUP ) );
			javaExpr.addAll( jNameKey );
			javaExpr.add( new LdcInsnNode( type ) );
			javaExpr.addAll( init );
			javaExpr.addAll( annotationStruct );
			javaExpr.addAll( documentationStruct );
			javaExpr.add( new MethodInsnNode( Opcodes.INVOKESPECIAL,
			    Type.getInternalName( Property.class ),
			    "<init>",
			    Type.getMethodDescriptor( Type.VOID_TYPE, Type.getType( Key.class ), Type.getType( String.class ), Type.getType( Object.class ),
			        Type.getType( IStruct.class ), Type.getType( IStruct.class ) ),
			    false ) );

			members.add( jNameKey );
			members.add( javaExpr );

			// Check if getter key annotation is defined in finalAnnotations and false. I don't love this as annotations can technically be any literal
			boolean getter = !finalAnnotations.stream()
			    .anyMatch( it -> it.getKey().getValue().equalsIgnoreCase( "getter" ) && !BooleanCaster.cast( getBoxExprAsString( it.getValue() ) ) );
			if ( getter ) {
				getterLookup.add( jGetNameKey );
				List<AbstractInsnNode> get = new ArrayList<>();
				get.add( new FieldInsnNode( Opcodes.GETSTATIC, declaringType.getInternalName(), "properties", Type.getDescriptor( Map.class ) ) );
				get.addAll( jNameKey );
				get.add( new MethodInsnNode( Opcodes.INVOKEINTERFACE, Type.getInternalName( Map.class ), "get",
				    Type.getMethodDescriptor( Type.getType( Object.class ), Type.getType( Object.class ) ), true ) );
				getterLookup.add( get );
			}
			// Check if setter key annotation is defined in finalAnnotations and false. I don't love this as annotations can technically be any literal
			boolean setter = !finalAnnotations.stream()
			    .anyMatch( it -> it.getKey().getValue().equalsIgnoreCase( "setter" ) && !BooleanCaster.cast( getBoxExprAsString( it.getValue() ) ) );
			if ( setter ) {
				setterLookup.add( jSetNameKey );
				List<AbstractInsnNode> set = new ArrayList<>();
				set.add( new FieldInsnNode( Opcodes.GETSTATIC, declaringType.getInternalName(), "properties", Type.getDescriptor( Map.class ) ) );
				set.addAll( jNameKey );
				set.add( new MethodInsnNode( Opcodes.INVOKEINTERFACE, Type.getInternalName( Map.class ), "get",
				    Type.getMethodDescriptor( Type.getType( Object.class ), Type.getType( Object.class ) ), true ) );
				setterLookup.add( set );
			}
		} );
		if ( members.isEmpty() ) {
			List<AbstractInsnNode>	linked	= List.of(
			    new LdcInsnNode( 0 ),
			    new TypeInsnNode( Opcodes.ANEWARRAY, Type.getInternalName( Object.class ) ),
			    new MethodInsnNode( Opcodes.INVOKESTATIC,
			        Type.getInternalName( MapHelper.class ),
			        "LinkedHashMapOfProperties",
			        Type.getMethodDescriptor( Type.getType( Map.class ), Type.getType( Object[].class ) ),
			        false ) );
			List<AbstractInsnNode>	hashed	= List.of(
			    new LdcInsnNode( 0 ),
			    new TypeInsnNode( Opcodes.ANEWARRAY, Type.getInternalName( Object.class ) ),
			    new MethodInsnNode( Opcodes.INVOKESTATIC,
			        Type.getInternalName( MapHelper.class ),
			        "HashMapOfProperties",
			        Type.getMethodDescriptor( Type.getType( Map.class ), Type.getType( Object[].class ) ),
			        false ) );
			return List.of( linked, hashed, hashed );
		} else {
			List<AbstractInsnNode> propertiesStruct = new ArrayList<>();
			propertiesStruct.addAll( AsmHelper.array( Type.getType( Object.class ), members ) );
			propertiesStruct.add( new MethodInsnNode( Opcodes.INVOKESTATIC,
			    Type.getInternalName( MapHelper.class ),
			    "LinkedHashMapOfProperties",
			    Type.getMethodDescriptor( Type.getType( Map.class ), Type.getType( Object[].class ) ),
			    false ) );
			List<AbstractInsnNode> getterStruct = new ArrayList<>();
			getterStruct.addAll( AsmHelper.array( Type.getType( Object.class ), getterLookup ) );
			getterStruct.add( new MethodInsnNode( Opcodes.INVOKESTATIC,
			    Type.getInternalName( MapHelper.class ),
			    "HashMapOfProperties",
			    Type.getMethodDescriptor( Type.getType( Map.class ), Type.getType( Object[].class ) ),
			    false ) );
			List<AbstractInsnNode> setterStruct = new ArrayList<>();
			setterStruct.addAll( AsmHelper.array( Type.getType( Object.class ), setterLookup ) );
			setterStruct.add( new MethodInsnNode( Opcodes.INVOKESTATIC,
			    Type.getInternalName( MapHelper.class ),
			    "HashMapOfProperties",
			    Type.getMethodDescriptor( Type.getType( Map.class ), Type.getType( Object[].class ) ),
			    false ) );
			return List.of( propertiesStruct, getterStruct, setterStruct );
		}
	}

	private static String getBoxExprAsString( BoxExpression expr ) {
		if ( expr == null ) {
			return "";
		}
		if ( expr instanceof BoxStringLiteral str ) {
			return str.getValue();
		}
		if ( expr instanceof BoxBooleanLiteral bool ) {
			return bool.getValue() ? "true" : "false";
		} else {
			throw new BoxRuntimeException( "Unsupported BoxExpr type: " + expr.getClass().getSimpleName() );
		}
	}

}
