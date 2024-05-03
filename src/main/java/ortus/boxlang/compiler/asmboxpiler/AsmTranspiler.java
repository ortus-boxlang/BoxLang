package ortus.boxlang.compiler.asmboxpiler;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import ortus.boxlang.compiler.asmboxpiler.transformer.Transformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.expression.*;
import ortus.boxlang.compiler.asmboxpiler.transformer.statement.BoxFunctionDeclarationTransformer;
import ortus.boxlang.compiler.ast.*;
import ortus.boxlang.compiler.ast.expression.*;
import ortus.boxlang.compiler.ast.statement.BoxArgumentDeclaration;
import ortus.boxlang.compiler.ast.statement.BoxExpressionStatement;
import ortus.boxlang.compiler.ast.statement.BoxFunctionDeclaration;
import ortus.boxlang.compiler.ast.statement.BoxReturn;
import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.runnables.AbstractBoxClass;
import ortus.boxlang.runtime.runnables.IClassRunnable;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.ThisScope;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.meta.BoxMeta;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

	}

	@Override
	public ClassNode transpile( BoxScript boxScript) throws BoxRuntimeException {
		Type		type		= Type.getType( "L" + getProperty( "packageName" ).replace( '.', '/' ) + "/" + getProperty( "classname" ) + ";" );
		ClassNode	classNode	= new ClassNode();

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

		AsmHelper.methodWithContextAndClassLocator( classNode, "_invoke", Type.getType( IBoxContext.class ), Type.getType(Object.class), true, methodVisitor -> {
			boxScript.getChildren().forEach(child -> transform( child ).forEach(value -> value.accept( methodVisitor ) ) );
		} );

		AsmHelper.complete( classNode, type, methodVisitor -> {
			methodVisitor.visitLdcInsn( getKeys().size() );
			methodVisitor.visitTypeInsn( Opcodes.ANEWARRAY, Type.getInternalName( Key.class ) );
			int index = 0;
			for ( BoxExpression expression : getKeys().values() ) {
				methodVisitor.visitInsn( Opcodes.DUP );
				methodVisitor.visitLdcInsn( index++ );
				transform( expression ).forEach( methodInsnNode -> methodInsnNode.accept( methodVisitor ) );
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
	public ClassNode transpile(BoxClass boxClass) throws BoxRuntimeException {
		Source source			= boxClass.getPosition().getSource();
		String		sourceType		= getProperty( "sourceType" );
		String		filePath		= source instanceof SourceFile file && file.getFile() != null ? file.getFile().getAbsolutePath()
			: "unknown";

		Type type			= Type.getType( "L" + getProperty( "packageName" ).replace( '.', '/' )
			+ "/" + getProperty( "classname" ) + ";" );

		ClassNode classNode	= new ClassNode();

		AsmHelper.init( classNode, type, AbstractBoxClass.class );
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

		classNode.visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL,
			"keys",
			Type.getDescriptor(Key[].class),
			null,
			null).visitEnd();
		classNode.visitField(Opcodes.ACC_PUBLIC,
			"$bx",
			Type.getDescriptor(BoxMeta.class),
			null,
			null).visitEnd();
		classNode.visitField(Opcodes.ACC_PRIVATE,
			"canOutput",
			Type.getDescriptor(Boolean.class),
			null,
			null).visitEnd();

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
				methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
				methodVisitor.visitVarInsn(Opcodes.ALOAD, 1);
				methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
					Type.getInternalName(AbstractBoxClass.class),
					"doSetSuper",
					Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(IClassRunnable.class)),
					false );
			} );
		AsmHelper.addFieldGetterAndSetter( classNode,
			type,
			"child",
			"getChild",
			"setChild",
			Type.getType( IClassRunnable.class ),
			null,
			methodVisitor -> { });

		MethodVisitor canOutput = classNode.visitMethod(Opcodes.ACC_PUBLIC, "canOutput", Type.getMethodDescriptor(Type.BOOLEAN_TYPE), null, null);
		canOutput.visitCode();
		Label initialized = new Label();
		canOutput.visitVarInsn(Opcodes.ALOAD, 0);
		canOutput.visitFieldInsn(Opcodes.GETFIELD, type.getInternalName(), "canOutput", Type.getDescriptor(Boolean.class));
		canOutput.visitJumpInsn(Opcodes.IFNONNULL, initialized);
		canOutput.visitVarInsn(Opcodes.ALOAD, 0);
		canOutput.visitVarInsn(Opcodes.ALOAD, 0);
		canOutput.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(AbstractBoxClass.class), "doCanOutput", Type.getMethodDescriptor(Type.getType(Boolean.class)), false);
		canOutput.visitFieldInsn(Opcodes.PUTFIELD, type.getInternalName(), "canOutput", Type.getDescriptor(Boolean.class));
		canOutput.visitLabel(initialized);
		canOutput.visitVarInsn(Opcodes.ALOAD, 0);
		canOutput.visitFieldInsn(Opcodes.GETFIELD, type.getInternalName(), "canOutput", Type.getDescriptor(Boolean.class));
		canOutput.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(Boolean.class), "booleanValue", Type.getMethodDescriptor(Type.BOOLEAN_TYPE), false);
		canOutput.visitInsn(Opcodes.IRETURN);
		canOutput.visitMaxs(0, 0);
		canOutput.visitEnd();

		AsmHelper.methodWithContextAndClassLocator( classNode, "_pseudoConstructor", Type.getType( IBoxContext.class ), Type.VOID_TYPE, false, methodVisitor -> {
			for ( BoxStatement statement : boxClass.getBody() ) {
				transform( statement ).forEach(abstractInsnNode -> abstractInsnNode.accept(methodVisitor));
			}
		} );

		AsmHelper.complete( classNode, type, methodVisitor -> {
			methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC,
				Type.getInternalName(List.class),
				"of",
				Type.getMethodDescriptor(Type.getType(List.class)),
				true);
			methodVisitor.visitFieldInsn(Opcodes.PUTSTATIC,
				type.getInternalName(),
				"imports",
				Type.getDescriptor(List.class));
			methodVisitor.visitLdcInsn(filePath);
			methodVisitor.visitLdcInsn(0);
			methodVisitor.visitTypeInsn(Opcodes.ANEWARRAY, Type.getInternalName(String.class));
			methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC,
				Type.getInternalName(Paths.class),
				"get",
				Type.getMethodDescriptor(Type.getType(Path.class), Type.getType(String.class), Type.getType(String[].class)),
				false);
			methodVisitor.visitFieldInsn(Opcodes.PUTSTATIC,
				type.getInternalName(),
				"path",
				Type.getDescriptor(Path.class));
			methodVisitor.visitFieldInsn(Opcodes.GETSTATIC,
				Type.getInternalName(BoxSourceType.class),
				sourceType,
				Type.getDescriptor(BoxSourceType.class));
			methodVisitor.visitFieldInsn(Opcodes.PUTSTATIC,
				type.getInternalName(),
				"sourceType",
				Type.getDescriptor(BoxSourceType.class));
			methodVisitor.visitLdcInsn(0);
			methodVisitor.visitTypeInsn(Opcodes.ANEWARRAY, Type.getInternalName(Key.class));
			methodVisitor.visitFieldInsn(Opcodes.PUTSTATIC,
				type.getInternalName(),
				"keys",
				Type.getDescriptor(Key[].class));
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
