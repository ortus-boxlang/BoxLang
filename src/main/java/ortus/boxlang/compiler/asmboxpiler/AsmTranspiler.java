package ortus.boxlang.compiler.asmboxpiler;

import org.objectweb.asm.*;
import org.objectweb.asm.tree.AbstractInsnNode;
import ortus.boxlang.compiler.asmboxpiler.transformer.Transformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.expression.BoxAssignmentTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.expression.BoxExpressionStatementTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.expression.BoxIntegerLiteralTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.expression.BoxStringLiteralTransformer;
import ortus.boxlang.compiler.ast.BoxExpression;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.BoxScript;
import ortus.boxlang.compiler.ast.expression.BoxAssignment;
import ortus.boxlang.compiler.ast.expression.BoxIntegerLiteral;
import ortus.boxlang.compiler.ast.expression.BoxStringLiteral;
import ortus.boxlang.compiler.ast.statement.BoxExpressionStatement;
import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.loader.ClassLocator;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;

public class AsmTranspiler extends Transpiler {

	private static HashMap<Class<?>, Transformer>	registry	= new HashMap<>();

	public AsmTranspiler() {
		// TODO: instance write to static field. Seems like an oversight in Java version (retained until clarified).
		registry.put( BoxStringLiteral.class, new BoxStringLiteralTransformer( this ) );
		registry.put( BoxIntegerLiteral.class, new BoxIntegerLiteralTransformer( this ) );
		registry.put( BoxExpressionStatement.class, new BoxExpressionStatementTransformer( this ) );
		registry.put( BoxAssignment.class, new BoxAssignmentTransformer( this ) );
	}

	@Override
	public void transpile( BoxScript script, ClassVisitor classVisitor ) throws BoxRuntimeException {
		Type type = Type.getType("Lboxgenerated/scripts/Statement_7772bb7a11ca73a6556e84aabdb0e2cf;"); // TODO: How to determine name?
		classVisitor.visit(
			Opcodes.V17,
			Opcodes.ACC_PUBLIC,
			type.getInternalName(),
			null,
			Type.getInternalName(BoxScript.class),
			null );

		addGetInstance( classVisitor, type );
		addConstructor( classVisitor );

		addStaticFieldGetter(classVisitor,
			type,
			"compileVersion",
			"getRunnableCompileVersion",
			Type.LONG_TYPE,
			1L);
		addStaticFieldGetter(classVisitor,
			type,
			"compiledOn",
			"getRunnableCompiledOn",
			Type.getType(LocalDateTime.class),
			null);
		addStaticFieldGetter(classVisitor,
			type,
			"ast",
			"getRunnableAST",
			Type.getType(Object.class),
			null);
		addStaticFieldGetter(classVisitor,
			type,
			"path",
			"getRunnablePath",
			Type.getType(Path.class),
			null);
		addStaticFieldGetter(classVisitor,
			type,
			"sourceType",
			"getSourceType",
			Type.getType(BoxSourceType.class),
			null);
		addStaticFieldGetter(classVisitor,
			type,
			"imports",
			"getImports",
			Type.getType(List.class),
			null);

		MethodVisitor methodVisitor = classVisitor.visitMethod(
			Opcodes.ACC_PUBLIC,
			"_invoke",
			Type.getMethodDescriptor(Type.getType(Object.class), Type.getType(IBoxContext.class)),
			null,
			null);
		methodVisitor.visitCode();
		methodVisitor.visitMethodInsn(
			Opcodes.INVOKESTATIC,
			Type.getInternalName(ClassLocator.class),
			"getInstance",
			Type.getMethodDescriptor(Type.getType(ClassLocator.class)),
			false );
		methodVisitor.visitVarInsn( Opcodes.ASTORE, 2 );
		script.getChildren().forEach( child -> transform( child ).forEach(node -> node.accept( methodVisitor ) ) );
		methodVisitor.visitInsn(Opcodes.ARETURN);
		methodVisitor.visitMaxs( 0, 0 );
		methodVisitor.visitEnd();

		FieldVisitor fieldVisitor = classVisitor.visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL | Opcodes.ACC_STATIC,
			"keys",
			Type.getDescriptor((Key[].class)),
			null,
			null);
		fieldVisitor.visitEnd();
		addStaticInitializer(classVisitor, type);
	}

	private static void addConstructor( ClassVisitor classVisitor ) {
		MethodVisitor methodVisitor = classVisitor.visitMethod( Opcodes.ACC_PUBLIC,
			"<init>",
			Type.getMethodDescriptor(Type.VOID_TYPE),
			null,
			null );
		methodVisitor.visitCode();
		methodVisitor.visitVarInsn( Opcodes.ALOAD, 0 );
		methodVisitor.visitMethodInsn( Opcodes.INVOKESPECIAL,
			Type.getInternalName(BoxScript.class),
			"<init>",
			Type.getMethodDescriptor(Type.VOID_TYPE),
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
			null);
		fieldVisitor.visitEnd();
		MethodVisitor methodVisitor = classVisitor.visitMethod(
			Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNCHRONIZED | Opcodes.ACC_STATIC,
			"getInstance",
			Type.getMethodDescriptor(type),
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
		methodVisitor.visitMethodInsn( Opcodes.INVOKESPECIAL,
			type.getInternalName(),
			"<init>",
			Type.getMethodDescriptor(Type.VOID_TYPE),
			false );
		methodVisitor.visitLabel( after );
		methodVisitor.visitFieldInsn( Opcodes.GETSTATIC,
			type.getInternalName(),
			"instance",
			type.getDescriptor() );
		methodVisitor.visitInsn( Opcodes.ARETURN );
		methodVisitor.visitMaxs(0, 0);
		methodVisitor.visitEnd();
	}

	private static void addStaticFieldGetter(ClassVisitor classVisitor, Type type, String field, String method, Type property, Object value ) {
		FieldVisitor fieldVisitor = classVisitor.visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL,
			field,
			property.getDescriptor(),
			null,
			value);
		fieldVisitor.visitEnd();
		MethodVisitor methodVisitor = classVisitor.visitMethod(Opcodes.ACC_PUBLIC,
			method,
			Type.getMethodDescriptor(property),
			null,
			null);
		methodVisitor.visitCode();
		methodVisitor.visitFieldInsn(Opcodes.GETSTATIC,
			type.getInternalName(),
			field,
			property.getDescriptor());
		methodVisitor.visitInsn(property.getOpcode(Opcodes.IRETURN));
		methodVisitor.visitMaxs(0, 0);
		methodVisitor.visitEnd();
	}

	private void addStaticInitializer( ClassVisitor classVisitor, Type type ) {
		MethodVisitor methodVisitor = classVisitor.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
			"<clinit>",
			Type.getMethodDescriptor(Type.VOID_TYPE),
			null,
			null);
		methodVisitor.visitCode();

		methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC,
			Type.getInternalName(List.class),
			"of",
			Type.getMethodDescriptor(Type.getType(List.class)),
			true);
		methodVisitor.visitFieldInsn( Opcodes.PUTSTATIC,
			type.getInternalName(),
			"imports",
			Type.getDescriptor(List.class));

		methodVisitor.visitLdcInsn("unknown");
		methodVisitor.visitLdcInsn(0);
		methodVisitor.visitTypeInsn(Opcodes.ANEWARRAY, Type.getInternalName(String.class));
		methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC,
			Type.getInternalName(Paths.class),
			"get",
			Type.getMethodDescriptor(Type.getType(Path.class), Type.getType(String.class), Type.getType(String[].class)),
			false);
		methodVisitor.visitFieldInsn( Opcodes.PUTSTATIC,
			type.getInternalName(),
			"path",
			Type.getDescriptor(Path.class));

		methodVisitor.visitFieldInsn(Opcodes.GETSTATIC,
			Type.getInternalName(BoxSourceType.class),
			"BOXSCRIPT",
			Type.getDescriptor(BoxSourceType.class));
		methodVisitor.visitFieldInsn( Opcodes.PUTSTATIC,
			type.getInternalName(),
			"sourceType",
			Type.getDescriptor(BoxSourceType.class));

		methodVisitor.visitLdcInsn(1L);
		methodVisitor.visitFieldInsn( Opcodes.PUTSTATIC,
			type.getInternalName(),
			"compileVersion",
			Type.LONG_TYPE.getDescriptor());

		methodVisitor.visitLdcInsn( LocalDateTime.now().toString() );
		methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC,
			Type.getInternalName(LocalDateTime.class),
			"parse",
			Type.getMethodDescriptor(Type.getType(LocalDateTime.class), Type.getType(CharSequence.class)),
			false);
		methodVisitor.visitFieldInsn( Opcodes.PUTSTATIC,
			type.getInternalName(),
			"compiledOn",
			Type.getDescriptor(LocalDateTime.class));

		methodVisitor.visitInsn(Opcodes.ACONST_NULL);
		methodVisitor.visitFieldInsn( Opcodes.PUTSTATIC,
			type.getInternalName(),
			"ast",
			Type.getDescriptor(Object.class));

		methodVisitor.visitLdcInsn(getKeys().size());
		methodVisitor.visitTypeInsn(Opcodes.ANEWARRAY, Type.getInternalName(Key.class));
		int index = 0;
		for (BoxExpression expression : getKeys().values()) {
			methodVisitor.visitInsn(Opcodes.DUP);
			methodVisitor.visitLdcInsn(index);
			transform( expression ).forEach(node -> {
				node.accept(methodVisitor);
				methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC,
					Type.getInternalName(Key.class),
					"of",
					Type.getMethodDescriptor(Type.getType(Key.class), Type.getType(String.class)),
					false);
			});
			methodVisitor.visitInsn(Opcodes.AASTORE);
		}
		methodVisitor.visitFieldInsn( Opcodes.PUTSTATIC,
			type.getInternalName(),
			"keys",
			Type.getDescriptor(Key[].class));

		methodVisitor.visitInsn(Opcodes.RETURN);

		methodVisitor.visitMaxs(0, 0);
		methodVisitor.visitEnd();
	}

	@Override
	public List<AbstractInsnNode> transform(BoxNode node ) {
		Transformer transformer = registry.get( node.getClass() );
		if ( transformer != null ) {
			return transformer.transform( node );
		}
		throw new IllegalStateException( "unsupported: " + node.getClass().getSimpleName() + " : " + node.getSourceText() );
	}
}
