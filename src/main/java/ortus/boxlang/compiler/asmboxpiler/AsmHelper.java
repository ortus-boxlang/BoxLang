package ortus.boxlang.compiler.asmboxpiler;

import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.loader.ClassLocator;
import ortus.boxlang.runtime.runnables.BoxClassSupport;
import ortus.boxlang.runtime.runnables.IClassRunnable;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.util.ResolvedFilePath;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class AsmHelper {

	public static void init( ClassVisitor classVisitor, boolean singleton, Type type, Type superClass, Consumer<MethodVisitor> onConstruction,
	    Type... interfaces ) {
		classVisitor.visit(
		    Opcodes.V17,
		    Opcodes.ACC_PUBLIC,
		    type.getInternalName(),
		    null,
		    superClass.getInternalName(),
		    interfaces.length == 0 ? null : Arrays.stream( interfaces ).map( Type::getInternalName ).toArray( String[]::new ) );

		if ( singleton ) {
			addGetInstance( classVisitor, type );
		}
		addConstructor( classVisitor, !singleton, superClass, onConstruction );

		addStaticFieldGetter( classVisitor,
		    type,
		    "compileVersion",
		    "getRunnableCompileVersion",
		    Type.LONG_TYPE,
		    1L );
		addStaticFieldGetter( classVisitor,
		    type,
		    "compiledOn",
		    "getRunnableCompiledOn",
		    Type.getType( LocalDateTime.class ),
		    null );
		addStaticFieldGetter( classVisitor,
		    type,
		    "ast",
		    "getRunnableAST",
		    Type.getType( Object.class ),
		    null );
	}

	private static void addConstructor( ClassVisitor classVisitor, boolean isPublic, Type superClass, Consumer<MethodVisitor> onConstruction ) {
		MethodVisitor methodVisitor = classVisitor.visitMethod( isPublic ? Opcodes.ACC_PUBLIC : Opcodes.ACC_PRIVATE,
		    "<init>",
		    Type.getMethodDescriptor( Type.VOID_TYPE ),
		    null,
		    null );
		methodVisitor.visitCode();
		methodVisitor.visitVarInsn( Opcodes.ALOAD, 0 );
		methodVisitor.visitMethodInsn( Opcodes.INVOKESPECIAL,
		    superClass.getInternalName(),
		    "<init>",
		    Type.getMethodDescriptor( Type.VOID_TYPE ),
		    false );
		onConstruction.accept( methodVisitor );
		methodVisitor.visitInsn( Opcodes.RETURN );
		methodVisitor.visitEnd();
	}

	private static void addGetInstance( ClassVisitor classVisitor, Type type ) {
		FieldVisitor fieldVisitor = classVisitor.visitField(
		    Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC,
		    "instance",
		    type.getDescriptor(),
		    null,
		    null );
		fieldVisitor.visitEnd();
		MethodVisitor methodVisitor = classVisitor.visitMethod(
		    Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNCHRONIZED | Opcodes.ACC_STATIC,
		    "getInstance",
		    Type.getMethodDescriptor( type ),
		    null,
		    null );
		methodVisitor.visitCode();
		Label after = new Label();
		methodVisitor.visitFieldInsn( Opcodes.GETSTATIC,
		    type.getInternalName(),
		    "instance",
		    type.getDescriptor() );
		methodVisitor.visitJumpInsn( Opcodes.IFNONNULL, after );
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
		methodVisitor.visitLabel( after );
		methodVisitor.visitFieldInsn( Opcodes.GETSTATIC,
		    type.getInternalName(),
		    "instance",
		    type.getDescriptor() );
		methodVisitor.visitInsn( Opcodes.ARETURN );
		methodVisitor.visitMaxs( 0, 0 );
		methodVisitor.visitEnd();
	}

	public static void addStaticFieldGetterWithStaticGetter( ClassVisitor classVisitor, Type type, String field, String method, String staticMethod,
	    Type property, Object value ) {
		addStaticFieldGetter( classVisitor, type, field, method, property, value );
		MethodVisitor methodVisitor = classVisitor.visitMethod( Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
		    staticMethod,
		    Type.getMethodDescriptor( property ),
		    null,
		    null );
		methodVisitor.visitCode();
		methodVisitor.visitFieldInsn( Opcodes.GETSTATIC,
		    type.getInternalName(),
		    field,
		    property.getDescriptor() );
		methodVisitor.visitInsn( property.getOpcode( Opcodes.IRETURN ) );
		methodVisitor.visitMaxs( 0, 0 );
		methodVisitor.visitEnd();
	}

	public static void addStaticFieldGetter( ClassVisitor classVisitor, Type type, String field, String method, Type property, Object value ) {
		FieldVisitor fieldVisitor = classVisitor.visitField( Opcodes.ACC_STATIC | Opcodes.ACC_FINAL | Opcodes.ACC_PUBLIC,
		    field,
		    property.getDescriptor(),
		    null,
		    value );
		fieldVisitor.visitEnd();
		MethodVisitor methodVisitor = classVisitor.visitMethod( Opcodes.ACC_PUBLIC,
		    method,
		    Type.getMethodDescriptor( property ),
		    null,
		    null );
		methodVisitor.visitCode();
		methodVisitor.visitFieldInsn( Opcodes.GETSTATIC,
		    type.getInternalName(),
		    field,
		    property.getDescriptor() );
		methodVisitor.visitInsn( property.getOpcode( Opcodes.IRETURN ) );
		methodVisitor.visitMaxs( 0, 0 );
		methodVisitor.visitEnd();
	}

	public static void addFieldGetter( ClassVisitor classVisitor, Type type, String field, String method, Type property, Object value ) {
		FieldVisitor fieldVisitor = classVisitor.visitField( Opcodes.ACC_PRIVATE,
		    field,
		    property.getDescriptor(),
		    null,
		    value );
		fieldVisitor.visitEnd();
		MethodVisitor methodVisitor = classVisitor.visitMethod( Opcodes.ACC_PUBLIC,
		    method,
		    Type.getMethodDescriptor( property ),
		    null,
		    null );
		methodVisitor.visitCode();
		methodVisitor.visitVarInsn( Opcodes.ALOAD, 0 );
		methodVisitor.visitFieldInsn( Opcodes.GETFIELD,
		    type.getInternalName(),
		    field,
		    property.getDescriptor() );
		methodVisitor.visitInsn( property.getOpcode( Opcodes.IRETURN ) );
		methodVisitor.visitMaxs( 0, 0 );
		methodVisitor.visitEnd();
	}

	public static void addFieldGetterAndSetter( ClassVisitor classVisitor, Type type, String field, String getter, String setter, Type property, Object value,
	    Consumer<MethodVisitor> onAfterSet ) {
		addFieldGetter( classVisitor, type, field, getter, property, value );
		MethodVisitor methodVisitor = classVisitor.visitMethod( Opcodes.ACC_PUBLIC,
		    setter,
		    Type.getMethodDescriptor( Type.VOID_TYPE, property ),
		    null,
		    null );
		methodVisitor.visitCode();
		methodVisitor.visitVarInsn( Opcodes.ALOAD, 0 );
		methodVisitor.visitVarInsn( Opcodes.ALOAD, 1 );
		methodVisitor.visitFieldInsn( Opcodes.PUTFIELD,
		    type.getInternalName(),
		    field,
		    property.getDescriptor() );
		onAfterSet.accept( methodVisitor );
		methodVisitor.visitInsn( Opcodes.RETURN );
		methodVisitor.visitMaxs( 0, 0 );
		methodVisitor.visitEnd();
	}

	public static void complete( ClassVisitor classVisitor, Type type, Consumer<MethodVisitor> onCinit ) {
		MethodVisitor methodVisitor = classVisitor.visitMethod( Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
		    "<clinit>",
		    Type.getMethodDescriptor( Type.VOID_TYPE ),
		    null,
		    null );
		methodVisitor.visitCode();

		methodVisitor.visitLdcInsn( 1L );
		methodVisitor.visitFieldInsn( Opcodes.PUTSTATIC,
		    type.getInternalName(),
		    "compileVersion",
		    Type.LONG_TYPE.getDescriptor() );

		methodVisitor.visitLdcInsn( LocalDateTime.now().toString() );
		methodVisitor.visitMethodInsn( Opcodes.INVOKESTATIC,
		    Type.getInternalName( LocalDateTime.class ),
		    "parse",
		    Type.getMethodDescriptor( Type.getType( LocalDateTime.class ), Type.getType( CharSequence.class ) ),
		    false );
		methodVisitor.visitFieldInsn( Opcodes.PUTSTATIC,
		    type.getInternalName(),
		    "compiledOn",
		    Type.getDescriptor( LocalDateTime.class ) );

		methodVisitor.visitInsn( Opcodes.ACONST_NULL );
		methodVisitor.visitFieldInsn( Opcodes.PUTSTATIC,
		    type.getInternalName(),
		    "ast",
		    Type.getDescriptor( Object.class ) );

		onCinit.accept( methodVisitor );

		methodVisitor.visitInsn( Opcodes.RETURN );

		methodVisitor.visitMaxs( 0, 0 );
		methodVisitor.visitEnd();
	}

	public static void methodWithContextAndClassLocator( ClassNode classNode,
	    String name,
	    Type parameterType,
	    Type returnType,
	    Supplier<List<AbstractInsnNode>> supplier ) {
		MethodVisitor methodVisitor = classNode.visitMethod(
		    Opcodes.ACC_PUBLIC,
		    name,
		    Type.getMethodDescriptor( returnType, parameterType ),
		    null,
		    null );
		methodVisitor.visitCode();
		methodVisitor.visitMethodInsn(
		    Opcodes.INVOKESTATIC,
		    Type.getInternalName( ClassLocator.class ),
		    "getInstance",
		    Type.getMethodDescriptor( Type.getType( ClassLocator.class ) ),
		    false );
		methodVisitor.visitVarInsn( Opcodes.ASTORE, 2 );
		List<AbstractInsnNode> nodes = supplier.get();
		if ( !nodes.isEmpty() && ( nodes.get( nodes.size() - 1 ).getOpcode() == Opcodes.POP || nodes.get( nodes.size() - 1 ).getOpcode() == Opcodes.POP2 ) ) {
			nodes.subList( 0, nodes.size() - 1 ).forEach( node -> node.accept( methodVisitor ) );
		} else {
			nodes.forEach( node -> node.accept( methodVisitor ) );
		}
		methodVisitor.visitInsn( returnType.getOpcode( Opcodes.IRETURN ) );
		methodVisitor.visitMaxs( 0, 0 );
		methodVisitor.visitEnd();
	}

	public static List<AbstractInsnNode> array( Type type, List<List<AbstractInsnNode>> values ) {
		return array( type, values, ( abstractInsnNodes, i ) -> abstractInsnNodes );
	}

	public static <T> List<AbstractInsnNode> array( Type type, List<T> values, BiFunction<T, Integer, List<AbstractInsnNode>> transformer ) {
		List<AbstractInsnNode> nodes = new ArrayList<>();
		nodes.add( new LdcInsnNode( values.size() ) );
		nodes.add( new TypeInsnNode( Opcodes.ANEWARRAY, type.getInternalName() ) );
		for ( int i = 0; i < values.size(); i++ ) {
			nodes.add( new InsnNode( Opcodes.DUP ) );
			nodes.add( new LdcInsnNode( i ) );
			nodes.addAll( transformer.apply( values.get( i ), i ) );
			nodes.add( new InsnNode( Opcodes.AASTORE ) );
		}
		return nodes;
	}

	public static void addParentGetter( ClassNode classNode, Type declaringType, String name, String method, Type property ) {
		MethodVisitor methodVisitor = classNode.visitMethod( Opcodes.ACC_PUBLIC,
		    method,
		    Type.getMethodDescriptor( property ),
		    null,
		    null );
		methodVisitor.visitCode();
		methodVisitor.visitFieldInsn( Opcodes.GETSTATIC,
		    declaringType.getInternalName(),
		    name,
		    property.getDescriptor() );
		methodVisitor.visitInsn( Opcodes.ARETURN );
		methodVisitor.visitEnd();
	}

	public static void resolvedFilePath( MethodVisitor methodVisitor, String mappingName, String mappingPath, String relativePath, String filePath ) {
		methodVisitor.visitLdcInsn( mappingName == null ? "" : mappingName );
		methodVisitor.visitLdcInsn( mappingPath == null ? "" : mappingPath );
		methodVisitor.visitLdcInsn( relativePath == null ? "" : relativePath );
		methodVisitor.visitLdcInsn( filePath );
		methodVisitor.visitMethodInsn( Opcodes.INVOKESTATIC,
		    Type.getInternalName( ResolvedFilePath.class ),
		    "of",
		    Type.getMethodDescriptor( Type.getType( ResolvedFilePath.class ), Type.getType( String.class ), Type.getType( String.class ),
		        Type.getType( String.class ), Type.getType( String.class ) ),
		    false );
	}

	public static void boxClassSupport( ClassVisitor classVisitor, String method, Type type, Type... parameters ) {
		MethodVisitor methodVisitor = classVisitor.visitMethod( Opcodes.ACC_PUBLIC, method, Type.getMethodDescriptor( type, parameters ), null,
		    null );
		methodVisitor.visitCode();
		methodVisitor.visitVarInsn( Opcodes.ALOAD, 0 );
		for ( int index = 0; index < parameters.length; index++ ) {
			methodVisitor.visitVarInsn( Opcodes.ALOAD, index + 1 );
		}
		Type[] parametersAndThis = new Type[ parameters.length + 1 ];
		parametersAndThis[ 0 ] = Type.getType( IClassRunnable.class );
		System.arraycopy( parameters, 0, parametersAndThis, 1, parameters.length );
		methodVisitor.visitMethodInsn( Opcodes.INVOKESTATIC,
		    Type.getInternalName( BoxClassSupport.class ),
		    method,
		    Type.getMethodDescriptor( type, parametersAndThis ),
		    false );
		methodVisitor.visitInsn( type.getOpcode( Opcodes.IRETURN ) );
		methodVisitor.visitMaxs( 0, 0 );
		methodVisitor.visitEnd();
	}

	public static MethodNode dereferenceAndInvoke( String name, Type descriptor, Type type ) {
		MethodNode node = new MethodNode(
		    Opcodes.ACC_PUBLIC,
		    name,
		    descriptor.getDescriptor(),
		    null,
		    null );

		node.visitCode();

		node.visitVarInsn( Opcodes.ALOAD, 0 );

		node.visitTypeInsn( Opcodes.NEW, Type.getInternalName( ScriptingRequestBoxContext.class ) );
		node.visitInsn( Opcodes.DUP );
		node.visitMethodInsn( Opcodes.INVOKESTATIC,
		    Type.getInternalName( BoxRuntime.class ),
		    "getInstance",
		    Type.getMethodDescriptor( Type.getType( BoxRuntime.class ) ),
		    false );
		node.visitMethodInsn( Opcodes.INVOKESTATIC,
		    Type.getInternalName( BoxRuntime.class ),
		    "getRuntimeContext",
		    Type.getMethodDescriptor( Type.getType( IBoxContext.class ) ),
		    false );
		node.visitMethodInsn( Opcodes.INVOKESPECIAL,
		    Type.getInternalName( ScriptingRequestBoxContext.class ),
		    "<init>>",
		    Type.getMethodDescriptor( Type.VOID_TYPE, Type.getType( IBoxContext.class ) ),
		    false );

		node.visitLdcInsn( name );
		node.visitMethodInsn( Opcodes.INVOKESTATIC,
		    Type.getInternalName( Key.class ),
		    "of",
		    Type.getMethodDescriptor( Type.getType( Key.class ), Type.getType( String.class ) ),
		    false );

		node.visitLdcInsn( descriptor.getArgumentCount() );
		node.visitTypeInsn( Opcodes.ANEWARRAY, Type.getInternalName( Object.class ) );

		for ( int index = 0, offset = 1; index < descriptor.getArgumentCount(); index++ ) {
			node.visitInsn( Opcodes.DUP );
			node.visitLdcInsn( index );
			node.visitVarInsn( descriptor.getArgumentTypes()[ index ].getOpcode( Opcodes.ILOAD ), offset );
			// TODO: boxing of primitives
			node.visitInsn( Opcodes.AASTORE );
			offset += descriptor.getArgumentTypes()[ index ].getSize();
		}

		node.visitFieldInsn( Opcodes.GETSTATIC, Type.getInternalName( Boolean.class ), "FALSE", Type.getDescriptor( Boolean.class ) );

		node.visitMethodInsn( Opcodes.INVOKEVIRTUAL, type.getInternalName(), "dereferenceAndInvoke",
		    Type.getMethodDescriptor( Type.getType( Object.class ), Type.getType( IBoxContext.class ), Type.getType( Key.class ), Type.getType( Object.class ),
		        Type.getType( Boolean.class ) ),
		    false );

		if ( descriptor.getReturnType().getSort() == Type.VOID ) {
			node.visitInsn( Opcodes.POP );
		} else {
			// TODO: unboxing of primitives
			node.visitTypeInsn( Opcodes.CHECKCAST, descriptor.getReturnType().getInternalName() );
		}
		node.visitInsn( descriptor.getReturnType().getOpcode( Opcodes.IRETURN ) );

		node.visitMaxs( 0, 0 );
		node.visitEnd();

		return node;
	}
}
