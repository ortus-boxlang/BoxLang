package ortus.boxlang.compiler.asmboxpiler;

import org.objectweb.asm.*;
import org.objectweb.asm.tree.ClassNode;
import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.loader.ClassLocator;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;

public class AsmHelper {

	public static void init( ClassVisitor classVisitor, Type type, Class<?> superType ) {
		classVisitor.visit(
			Opcodes.V17,
			Opcodes.ACC_PUBLIC,
			type.getInternalName(),
			null,
			Type.getInternalName( superType ),
			null );

		addGetInstance( classVisitor, type );
		addConstructor( classVisitor, superType );

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
		addStaticFieldGetter( classVisitor,
			type,
			"path",
			"getRunnablePath",
			Type.getType( Path.class ),
			null );
		addStaticFieldGetter( classVisitor,
			type,
			"sourceType",
			"getSourceType",
			Type.getType( BoxSourceType.class ),
			null );
		addStaticFieldGetter( classVisitor,
			type,
			"imports",
			"getImports",
			Type.getType( List.class ),
			null );
	}

	private static void addConstructor( ClassVisitor classVisitor, Class<?> superType ) {
		MethodVisitor methodVisitor = classVisitor.visitMethod( Opcodes.ACC_PUBLIC,
			"<init>",
			Type.getMethodDescriptor( Type.VOID_TYPE ),
			null,
			null );
		methodVisitor.visitCode();
		methodVisitor.visitVarInsn( Opcodes.ALOAD, 0 );
		methodVisitor.visitMethodInsn( Opcodes.INVOKESPECIAL,
			Type.getInternalName( superType ),
			"<init>",
			Type.getMethodDescriptor( Type.VOID_TYPE ),
			false );
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

	public static void addStaticFieldGetter( ClassVisitor classVisitor, Type type, String field, String method, Type property, Object value ) {
		FieldVisitor fieldVisitor = classVisitor.visitField( Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL,
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

	public static void complete( ClassVisitor classVisitor, Type type, Consumer<MethodVisitor> onCinit ) {
		MethodVisitor methodVisitor = classVisitor.visitMethod( Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
			"<clinit>",
			Type.getMethodDescriptor( Type.VOID_TYPE ),
			null,
			null );
		methodVisitor.visitCode();

		methodVisitor.visitMethodInsn( Opcodes.INVOKESTATIC,
			Type.getInternalName( List.class ),
			"of",
			Type.getMethodDescriptor( Type.getType( List.class ) ),
			true );
		methodVisitor.visitFieldInsn( Opcodes.PUTSTATIC,
			type.getInternalName(),
			"imports",
			Type.getDescriptor( List.class ) );

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

		onCinit.accept(methodVisitor);

		methodVisitor.visitInsn( Opcodes.RETURN );

		methodVisitor.visitMaxs( 0, 0 );
		methodVisitor.visitEnd();
	}

	public static void invokeWithContextAndClassLocator(ClassNode classNode, Consumer<MethodVisitor> consumer ) {
		MethodVisitor methodVisitor = classNode.visitMethod(
			Opcodes.ACC_PUBLIC,
			"_invoke",
			Type.getMethodDescriptor(Type.getType(Object.class), Type.getType(IBoxContext.class)),
			null,
			null);
		methodVisitor.visitCode();
		methodVisitor.visitMethodInsn(
			Opcodes.INVOKESTATIC,
			Type.getInternalName( ClassLocator.class ),
			"getInstance",
			Type.getMethodDescriptor( Type.getType( ClassLocator.class ) ),
			false );
		methodVisitor.visitVarInsn( Opcodes.ASTORE, 2 );
		consumer.accept(methodVisitor);
		methodVisitor.visitMaxs( 0, 0 );
		methodVisitor.visitEnd();
	}
}
