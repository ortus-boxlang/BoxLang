package ortus.boxlang.compiler.asmboxpiler;

import org.objectweb.asm.*;
import org.objectweb.asm.tree.AbstractInsnNode;
import ortus.boxlang.compiler.asmboxpiler.transformer.Transformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.expression.BoxAssignmentTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.expression.BoxExpressionStatementTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.expression.BoxIntegerLiteralTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.expression.BoxStringLiteralTransformer;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.BoxScript;
import ortus.boxlang.compiler.ast.expression.BoxAssignment;
import ortus.boxlang.compiler.ast.expression.BoxIntegerLiteral;
import ortus.boxlang.compiler.ast.expression.BoxStringLiteral;
import ortus.boxlang.compiler.ast.statement.BoxExpressionStatement;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.loader.ClassLocator;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

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


		// TODO: add fields and methods
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

	@Override
	public List<AbstractInsnNode> transform(BoxNode node ) {
		Transformer transformer = registry.get( node.getClass() );
		if ( transformer != null ) {
			return transformer.transform( node );
		}
		throw new IllegalStateException( "unsupported: " + node.getClass().getSimpleName() + " : " + node.getSourceText() );
	}
}
