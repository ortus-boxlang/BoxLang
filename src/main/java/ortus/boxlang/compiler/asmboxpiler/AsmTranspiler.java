package ortus.boxlang.compiler.asmboxpiler;

import org.objectweb.asm.*;
import org.objectweb.asm.tree.AbstractInsnNode;
import ortus.boxlang.compiler.asmboxpiler.transformer.Transformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.expression.*;
import ortus.boxlang.compiler.asmboxpiler.transformer.statement.BoxFunctionDeclarationTransformer;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.BoxScript;
import ortus.boxlang.compiler.ast.expression.*;
import ortus.boxlang.compiler.ast.statement.BoxExpressionStatement;
import ortus.boxlang.compiler.ast.statement.BoxFunctionDeclaration;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.loader.ClassLocator;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

import java.util.HashMap;
import java.util.List;

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

	}

	@Override
	public void transpile( BoxScript script, ClassVisitor classVisitor ) throws BoxRuntimeException {
		Type type = Type.getType( "L" + getProperty( "packageName" ).replace('.', '/') + "/" + getProperty( "classname" ) + ";" );
		ClassCreator.init( classVisitor, type );

		FieldVisitor fieldVisitor = classVisitor.visitField( Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL | Opcodes.ACC_STATIC,
			"keys",
			Type.getDescriptor( ( Key[].class ) ),
			null,
			null );
		fieldVisitor.visitEnd();

		MethodVisitor methodVisitor = classVisitor.visitMethod(
		    Opcodes.ACC_PUBLIC,
		    "_invoke",
		    Type.getMethodDescriptor( Type.getType( Object.class ), Type.getType( IBoxContext.class ) ),
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
		script.getChildren().forEach( child -> transform( child ).forEach( node -> node.accept( methodVisitor ) ) );
		methodVisitor.visitInsn( Opcodes.ARETURN );
		methodVisitor.visitMaxs( 0, 0 );
		methodVisitor.visitEnd();

		ClassCreator.addStaticInitializer( this, getKeys(), classVisitor, type );
	}

	@Override
	public List<AbstractInsnNode> transform( BoxNode node ) {
		Transformer transformer = registry.get( node.getClass() );
		if ( transformer != null ) {
			return transformer.transform( node );
		}
		throw new IllegalStateException( "unsupported: " + node.getClass().getSimpleName() + " : " + node.getSourceText() );
	}
}
