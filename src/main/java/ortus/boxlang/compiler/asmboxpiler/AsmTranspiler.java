package ortus.boxlang.compiler.asmboxpiler;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import ortus.boxlang.compiler.asmboxpiler.transformer.Transformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.expression.*;
import ortus.boxlang.compiler.asmboxpiler.transformer.statement.BoxFunctionDeclarationTransformer;
import ortus.boxlang.compiler.ast.BoxExpression;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.BoxScript;
import ortus.boxlang.compiler.ast.expression.*;
import ortus.boxlang.compiler.ast.statement.BoxArgumentDeclaration;
import ortus.boxlang.compiler.ast.statement.BoxExpressionStatement;
import ortus.boxlang.compiler.ast.statement.BoxFunctionDeclaration;
import ortus.boxlang.compiler.ast.statement.BoxReturn;
import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

import java.nio.file.Path;
import java.nio.file.Paths;
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
		registry.put( BoxReturn.class, new BoxReturnTransformer( this ) );
		registry.put( BoxStructLiteral.class, new BoxStructLiteralTransformer( this ) );
		registry.put( BoxIdentifier.class, new BoxIdentifierTransformer( this ) );
		registry.put( BoxBinaryOperation.class, new BoxBinaryOperationTransformer( this ) );
		registry.put( BoxDotAccess.class, new BoxAccessTransformer( this ) );
		registry.put( BoxArrayAccess.class, new BoxAccessTransformer( this ) );
		registry.put( BoxArgumentDeclaration.class, new BoxArgumentDeclarationTransformer( this ) );

	}

	@Override
	public ClassNode transpile(BoxScript script) throws BoxRuntimeException {
		Type type = Type.getType( "L" + getProperty( "packageName" ).replace('.', '/') + "/" + getProperty( "classname" ) + ";" );
		ClassNode classNode = new ClassNode();

		AsmHelper.init( classNode, type, ortus.boxlang.runtime.runnables.BoxScript.class );
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

		AsmHelper.invokeWithContextAndClassLocator(classNode, Type.getType(IBoxContext.class), methodVisitor -> {
			script.getChildren().forEach(child -> transform( child ).forEach(value -> value.accept( methodVisitor ) ) );
			methodVisitor.visitInsn( Opcodes.ARETURN );
		});

		AsmHelper.complete( classNode, type, methodVisitor -> {
			methodVisitor.visitLdcInsn( getKeys().size() );
			methodVisitor.visitTypeInsn( Opcodes.ANEWARRAY, Type.getInternalName(Key.class) );
			int index = 0;
			for ( BoxExpression expression : getKeys().values() ) {
				methodVisitor.visitInsn( Opcodes.DUP );
				methodVisitor.visitLdcInsn( index++ );
				transform( expression ).forEach( methodInsnNode -> {
					methodInsnNode.accept( methodVisitor );
					methodVisitor.visitMethodInsn( Opcodes.INVOKESTATIC,
						Type.getInternalName( Key.class ),
						"of",
						Type.getMethodDescriptor( Type.getType( Key.class ), Type.getType( String.class ) ),
						false );
				} );
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
		} );

		return classNode;
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
