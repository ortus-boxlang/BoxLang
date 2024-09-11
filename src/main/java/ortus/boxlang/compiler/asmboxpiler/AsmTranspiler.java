package ortus.boxlang.compiler.asmboxpiler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;

import ortus.boxlang.compiler.asmboxpiler.transformer.ReturnValueContext;
import ortus.boxlang.compiler.asmboxpiler.transformer.Transformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.TransformerContext;
import ortus.boxlang.compiler.asmboxpiler.transformer.expression.BoxAccessTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.expression.BoxArgumentDeclarationTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.expression.BoxArgumentTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.expression.BoxArrayLiteralTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.expression.BoxAssignmentTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.expression.BoxBinaryOperationTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.expression.BoxBooleanLiteralTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.expression.BoxBreakTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.expression.BoxClosureTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.expression.BoxComparisonOperationTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.expression.BoxContinueTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.expression.BoxDecimalLiteralTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.expression.BoxExpressionStatementTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.expression.BoxFQNTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.expression.BoxFunctionInvocationTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.expression.BoxIdentifierTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.expression.BoxImportTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.expression.BoxIntegerLiteralTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.expression.BoxLambdaTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.expression.BoxMethodInvocationTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.expression.BoxNewTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.expression.BoxNullTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.expression.BoxParenthesisTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.expression.BoxReturnTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.expression.BoxScopeTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.expression.BoxStatementBlockTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.expression.BoxStaticAccessTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.expression.BoxStaticMethodInvocationTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.expression.BoxStringConcatTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.expression.BoxStringInterpolationTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.expression.BoxStringLiteralTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.expression.BoxStructLiteralTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.expression.BoxSwitchTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.expression.BoxTernaryOperationTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.expression.BoxUnaryOperationTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.statement.BoxAssertTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.statement.BoxBufferOutputTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.statement.BoxClassTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.statement.BoxComponentTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.statement.BoxDoTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.statement.BoxForInTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.statement.BoxForIndexTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.statement.BoxFunctionDeclarationTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.statement.BoxIfElseTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.statement.BoxRethrowTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.statement.BoxStaticInitializerTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.statement.BoxThrowTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.statement.BoxTryTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.statement.BoxWhileTransformer;
import ortus.boxlang.compiler.ast.BoxClass;
import ortus.boxlang.compiler.ast.BoxExpression;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.BoxScript;
import ortus.boxlang.compiler.ast.BoxStaticInitializer;
import ortus.boxlang.compiler.ast.Source;
import ortus.boxlang.compiler.ast.SourceFile;
import ortus.boxlang.compiler.ast.expression.BoxArgument;
import ortus.boxlang.compiler.ast.expression.BoxArrayAccess;
import ortus.boxlang.compiler.ast.expression.BoxArrayLiteral;
import ortus.boxlang.compiler.ast.expression.BoxAssignment;
import ortus.boxlang.compiler.ast.expression.BoxBinaryOperation;
import ortus.boxlang.compiler.ast.expression.BoxBooleanLiteral;
import ortus.boxlang.compiler.ast.expression.BoxClosure;
import ortus.boxlang.compiler.ast.expression.BoxComparisonOperation;
import ortus.boxlang.compiler.ast.expression.BoxDecimalLiteral;
import ortus.boxlang.compiler.ast.expression.BoxDotAccess;
import ortus.boxlang.compiler.ast.expression.BoxFQN;
import ortus.boxlang.compiler.ast.expression.BoxFunctionInvocation;
import ortus.boxlang.compiler.ast.expression.BoxIdentifier;
import ortus.boxlang.compiler.ast.expression.BoxIntegerLiteral;
import ortus.boxlang.compiler.ast.expression.BoxLambda;
import ortus.boxlang.compiler.ast.expression.BoxMethodInvocation;
import ortus.boxlang.compiler.ast.expression.BoxNew;
import ortus.boxlang.compiler.ast.expression.BoxNull;
import ortus.boxlang.compiler.ast.expression.BoxParenthesis;
import ortus.boxlang.compiler.ast.expression.BoxScope;
import ortus.boxlang.compiler.ast.expression.BoxStaticAccess;
import ortus.boxlang.compiler.ast.expression.BoxStaticMethodInvocation;
import ortus.boxlang.compiler.ast.expression.BoxStringConcat;
import ortus.boxlang.compiler.ast.expression.BoxStringInterpolation;
import ortus.boxlang.compiler.ast.expression.BoxStringLiteral;
import ortus.boxlang.compiler.ast.expression.BoxStructLiteral;
import ortus.boxlang.compiler.ast.expression.BoxTernaryOperation;
import ortus.boxlang.compiler.ast.expression.BoxUnaryOperation;
import ortus.boxlang.compiler.ast.statement.BoxAnnotation;
import ortus.boxlang.compiler.ast.statement.BoxArgumentDeclaration;
import ortus.boxlang.compiler.ast.statement.BoxAssert;
import ortus.boxlang.compiler.ast.statement.BoxBreak;
import ortus.boxlang.compiler.ast.statement.BoxBufferOutput;
import ortus.boxlang.compiler.ast.statement.BoxContinue;
import ortus.boxlang.compiler.ast.statement.BoxDo;
import ortus.boxlang.compiler.ast.statement.BoxExpressionStatement;
import ortus.boxlang.compiler.ast.statement.BoxForIn;
import ortus.boxlang.compiler.ast.statement.BoxForIndex;
import ortus.boxlang.compiler.ast.statement.BoxFunctionDeclaration;
import ortus.boxlang.compiler.ast.statement.BoxIfElse;
import ortus.boxlang.compiler.ast.statement.BoxImport;
import ortus.boxlang.compiler.ast.statement.BoxProperty;
import ortus.boxlang.compiler.ast.statement.BoxRethrow;
import ortus.boxlang.compiler.ast.statement.BoxReturn;
import ortus.boxlang.compiler.ast.statement.BoxStatementBlock;
import ortus.boxlang.compiler.ast.statement.BoxSwitch;
import ortus.boxlang.compiler.ast.statement.BoxThrow;
import ortus.boxlang.compiler.ast.statement.BoxTry;
import ortus.boxlang.compiler.ast.statement.BoxWhile;
import ortus.boxlang.compiler.ast.statement.component.BoxComponent;
import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.loader.ImportDefinition;
import ortus.boxlang.runtime.runnables.IClassRunnable;
import ortus.boxlang.runtime.scopes.ClassVariablesScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.AbstractFunction;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Function;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Property;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.exceptions.ExpressionException;
import ortus.boxlang.runtime.types.util.MapHelper;
import ortus.boxlang.runtime.util.ResolvedFilePath;

public class AsmTranspiler extends Transpiler {

	private static HashMap<Class<?>, Transformer>	registry					= new HashMap<>();
	private static final String						EXTENDS_ANNOTATION_MARKER	= "overrideJava";

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
		registry.put( BoxFQN.class, new BoxFQNTransformer( this ) );
		registry.put( BoxLambda.class, new BoxLambdaTransformer( this ) );
		registry.put( BoxBooleanLiteral.class, new BoxBooleanLiteralTransformer( this ) );
		registry.put( BoxNull.class, new BoxNullTransformer( this ) );
		registry.put( BoxNew.class, new BoxNewTransformer( this ) );
		registry.put( BoxUnaryOperation.class, new BoxUnaryOperationTransformer( this ) );
		registry.put( BoxDecimalLiteral.class, new BoxDecimalLiteralTransformer( this ) );
		registry.put( BoxStatementBlock.class, new BoxStatementBlockTransformer( this ) );
		registry.put( BoxIfElse.class, new BoxIfElseTransformer( this ) );
		registry.put( BoxComparisonOperation.class, new BoxComparisonOperationTransformer( this ) );
		registry.put( BoxTernaryOperation.class, new BoxTernaryOperationTransformer( this ) );
		registry.put( BoxSwitch.class, new BoxSwitchTransformer( this ) );
		registry.put( BoxScope.class, new BoxScopeTransformer( this ) );
		registry.put( BoxBreak.class, new BoxBreakTransformer( this ) );
		registry.put( BoxContinue.class, new BoxContinueTransformer( this ) );
		registry.put( BoxThrow.class, new BoxThrowTransformer( this ) );
		registry.put( BoxTry.class, new BoxTryTransformer( this ) );
		registry.put( BoxRethrow.class, new BoxRethrowTransformer( this ) );
		registry.put( BoxAssert.class, new BoxAssertTransformer( this ) );
		registry.put( BoxParenthesis.class, new BoxParenthesisTransformer( this ) );
		registry.put( BoxImport.class, new BoxImportTransformer( this ) );
		registry.put( BoxBufferOutput.class, new BoxBufferOutputTransformer( this ) );
		registry.put( BoxWhile.class, new BoxWhileTransformer( this ) );
		registry.put( BoxDo.class, new BoxDoTransformer( this ) );
		registry.put( BoxForIn.class, new BoxForInTransformer( this ) );
		registry.put( BoxForIndex.class, new BoxForIndexTransformer( this ) );
		registry.put( BoxClosure.class, new BoxClosureTransformer( this ) );
		registry.put( BoxComponent.class, new BoxComponentTransformer( this ) );
		registry.put( BoxStaticInitializer.class, new BoxStaticInitializerTransformer( this ) );
		registry.put( BoxStaticAccess.class, new BoxStaticAccessTransformer( this ) );
		registry.put( BoxStaticMethodInvocation.class, new BoxStaticMethodInvocationTransformer( this ) );
	}

	@Override
	public ClassNode transpile( BoxScript boxScript ) throws BoxRuntimeException {
		Type type = Type.getType( "L" + getProperty( "packageName" ).replace( '.', '/' ) + "/" + getProperty( "classname" ) + ";" );
		setProperty( "classType", type.getDescriptor() );
		setProperty( "classTypeInternal", type.getInternalName() );
		ClassNode	classNode		= new ClassNode();
		String		mappingName		= getProperty( "mappingName" );
		String		mappingPath		= getProperty( "mappingPath" );
		String		relativePath	= getProperty( "relativePath" );
		Source		source			= boxScript.getPosition().getSource();
		String		filePath		= source instanceof SourceFile file && file.getFile() != null ? file.getFile().getAbsolutePath() : "unknown";

		String		baseClassName	= getProperty( "baseclass" ) != null ? getProperty( "baseclass" ) : "BoxScript";

		Class<?>	baseClass		= switch ( baseClassName.toUpperCase() ) {
										case "BOXTEMPLATE" -> ortus.boxlang.runtime.runnables.BoxTemplate.class;
										default -> ortus.boxlang.runtime.runnables.BoxScript.class;
									};

		String		returnTypeName	= baseClass.equals( "BoxScript" ) ? "Object" : "void";
		returnTypeName = getProperty( "returnType" ) != null ? getProperty( "returnType" ) : returnTypeName;

		Type returnType = switch ( returnTypeName.toUpperCase() ) {
			case "OBJECT" -> Type.getType( Object.class );
			default -> Type.VOID_TYPE;
		};

		AsmHelper.init( classNode, true, type, Type.getType( baseClass ), methodVisitor -> {
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
		    Type.getType( ResolvedFilePath.class ),
		    null );
		AsmHelper.addStaticFieldGetter( classNode,
		    type,
		    "sourceType",
		    "getSourceType",
		    Type.getType( BoxSourceType.class ),
		    null );

		AsmHelper.methodWithContextAndClassLocator(
		    classNode,
		    "_invoke",
		    Type.getType( IBoxContext.class ),
		    returnType,
		    false,
		    this,
		    false,
		    () -> AsmHelper.transformBodyExpressions( this, boxScript.getStatements(), TransformerContext.NONE,
		        returnType == Type.VOID_TYPE ? ReturnValueContext.EMPTY : ReturnValueContext.VALUE_OR_NULL )
		);

		AsmHelper.complete( classNode, type, methodVisitor -> {
			AsmHelper.array( Type.getType( ImportDefinition.class ), getImports(), ( raw, index ) -> {
				List<AbstractInsnNode> nodes = new ArrayList<>();
				nodes.addAll( raw );
				nodes.add( new MethodInsnNode( Opcodes.INVOKESTATIC,
				    Type.getInternalName( ImportDefinition.class ),
				    "parse",
				    Type.getMethodDescriptor( Type.getType( ImportDefinition.class ), Type.getType( String.class ) ),
				    false ) );
				return nodes;
			} ).forEach( node -> node.accept( methodVisitor ) );
			methodVisitor.visitMethodInsn( Opcodes.INVOKESTATIC,
			    Type.getInternalName( List.class ),
			    "of",
			    Type.getMethodDescriptor( Type.getType( List.class ), Type.getType( Object[].class ) ),
			    true );
			methodVisitor.visitFieldInsn( Opcodes.PUTSTATIC,
			    type.getInternalName(),
			    "imports",
			    Type.getDescriptor( List.class ) );

			AsmHelper.resolvedFilePath( methodVisitor, mappingName, mappingPath, relativePath, filePath );
			methodVisitor.visitFieldInsn( Opcodes.PUTSTATIC,
			    type.getInternalName(),
			    "path",
			    Type.getDescriptor( ResolvedFilePath.class ) );

			methodVisitor.visitFieldInsn( Opcodes.GETSTATIC,
			    Type.getInternalName( BoxSourceType.class ),
			    "BOXSCRIPT",
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
				transform( expression, TransformerContext.NONE, ReturnValueContext.EMPTY ).forEach( methodInsnNode -> methodInsnNode.accept( methodVisitor ) );
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
		} );

		return classNode;
	}

	@Override
	public ClassNode transpile( BoxClass boxClass ) throws BoxRuntimeException {
		return BoxClassTransformer.transpile( this, boxClass );
	}

	@Override
	public List<AbstractInsnNode> transform( BoxNode node, TransformerContext context, ReturnValueContext returnValueContext ) {
		Transformer transformer = registry.get( node.getClass() );
		if ( transformer != null ) {
			return transformer.transform( node, context, returnValueContext );
		}
		throw new IllegalStateException( "unsupported: " + node.getClass().getSimpleName() + " : " + node.getSourceText() );
	}

	@Override
	public List<List<AbstractInsnNode>> transformProperties( Type declaringType, List<BoxProperty> properties, String sourceType ) {
		List<List<AbstractInsnNode>>	members			= new ArrayList<>();
		List<List<AbstractInsnNode>>	getterLookup	= new ArrayList<>();
		List<List<AbstractInsnNode>>	setterLookup	= new ArrayList<>();
		properties.forEach( prop -> {
			List<AbstractInsnNode>	documentationStruct	= transformDocumentation( prop.getDocumentation() );
			/*
			 * normalize annotations to allow for
			 * property String userName;
			 */
			List<BoxAnnotation>		finalAnnotations	= normlizePropertyAnnotations( prop );
			// Start wiith all inline annotatinos

			BoxAnnotation			nameAnnotation		= finalAnnotations.stream().filter( it -> it.getKey().getValue().equalsIgnoreCase( "name" ) )
			    .findFirst()
			    .orElseThrow( () -> new ExpressionException( "Property [" + prop.getSourceText() + "] missing name annotation", prop ) );
			BoxAnnotation			typeAnnotation		= finalAnnotations.stream().filter( it -> it.getKey().getValue().equalsIgnoreCase( "type" ) )
			    .findFirst()
			    .orElseThrow( () -> new ExpressionException( "Property [" + prop.getSourceText() + "] missing type annotation", prop ) );
			BoxAnnotation			defaultAnnotation	= finalAnnotations.stream().filter( it -> it.getKey().getValue().equalsIgnoreCase( "default" ) )
			    .findFirst()
			    .orElse( null );

			// Process the default value
			List<AbstractInsnNode>	init				= List.of( new InsnNode( Opcodes.ACONST_NULL ) );
			if ( defaultAnnotation != null && defaultAnnotation.getValue() != null ) {
				init = transform( ( BoxNode ) defaultAnnotation.getValue(), TransformerContext.NONE, ReturnValueContext.VALUE );
			}

			// name and type must be simple values
			String	name;
			String	type;
			if ( nameAnnotation != null && nameAnnotation.getValue() instanceof BoxStringLiteral namelit ) {
				name = namelit.getValue().trim();
				if ( name.isEmpty() )
					throw new ExpressionException( "Property [" + prop.getSourceText() + "] name cannot be empty", nameAnnotation );
			} else {
				throw new ExpressionException( "Property [" + prop.getSourceText() + "] name must be a simple value", nameAnnotation );
			}
			if ( typeAnnotation != null && typeAnnotation.getValue() instanceof BoxStringLiteral typelit ) {
				type = typelit.getValue().trim();
				if ( type.isEmpty() )
					throw new ExpressionException( "Property [" + prop.getSourceText() + "] type cannot be empty", typeAnnotation );
			} else {
				throw new ExpressionException( "Property [" + prop.getSourceText() + "] type must be a simple value", typeAnnotation );
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
			javaExpr.addAll( transformAnnotations( finalAnnotations ) );
			javaExpr.addAll( documentationStruct );

			javaExpr.add( new FieldInsnNode( Opcodes.GETSTATIC,
			    Type.getInternalName( BoxSourceType.class ),
			    sourceType.toUpperCase(),
			    Type.getDescriptor( BoxSourceType.class ) ) );

			javaExpr.add( new MethodInsnNode( Opcodes.INVOKESPECIAL,
			    Type.getInternalName( Property.class ),
			    "<init>",
			    Type.getMethodDescriptor( Type.VOID_TYPE, Type.getType( Key.class ), Type.getType( String.class ), Type.getType( Object.class ),
			        Type.getType( IStruct.class ), Type.getType( IStruct.class ), Type.getType( BoxSourceType.class ) ),
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

	public static List<BoxAnnotation> normlizePropertyAnnotations( BoxProperty prop ) {

		/**
		 * normalize annotations to allow for
		 * property String userName;
		 * This means all inline and pre annotations are treated as post annotations
		 */
		List<BoxAnnotation>	finalAnnotations	= new ArrayList<>();
		// Start wiith all inline annotatinos
		List<BoxAnnotation>	annotations			= prop.getPostAnnotations();
		// Add in any pre annotations that have a value, which allows type, name, or default to be set before
		annotations.addAll( prop.getAnnotations().stream().filter( it -> it.getValue() != null ).toList() );

		// Find the position of the name, type, and default annotations
		int					namePosition			= annotations.stream()
		    .filter( it -> it.getKey().getValue().equalsIgnoreCase( "name" ) && it.getValue() != null )
		    .findFirst()
		    .map( annotations::indexOf ).orElse( -1 );
		int					typePosition			= annotations.stream()
		    .filter( it -> it.getKey().getValue().equalsIgnoreCase( "type" ) && it.getValue() != null )
		    .findFirst()
		    .map( annotations::indexOf ).orElse( -1 );
		int					defaultPosition			= annotations.stream()
		    .filter( it -> it.getKey().getValue().equalsIgnoreCase( "default" ) && it.getValue() != null )
		    .findFirst()
		    .map( annotations::indexOf ).orElse( -1 );

		// Count the number of non-valued keys to determine how to handle them by position later
		int					numberOfNonValuedKeys	= ( int ) annotations.stream()
		    .map( BoxAnnotation::getValue )
		    .filter( Objects::isNull )
		    .count();
		List<BoxAnnotation>	nonValuedKeys			= annotations.stream()
		    .filter( it -> it.getValue() == null )
		    .collect( java.util.stream.Collectors.toList() );

		// Find the name, type, and default annotations
		BoxAnnotation		nameAnnotation			= null;
		BoxAnnotation		typeAnnotation			= null;
		BoxAnnotation		defaultAnnotation		= null;
		if ( namePosition > -1 )
			nameAnnotation = annotations.get( namePosition );
		if ( typePosition > -1 )
			typeAnnotation = annotations.get( typePosition );
		if ( defaultPosition > -1 )
			defaultAnnotation = annotations.get( defaultPosition );

		/**
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
				throw new ExpressionException( "Property [" + prop.getSourceText() + "] has no name", prop );
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
		// Now that name, type, and default are finalized, add in any remaining non-valued keys
		finalAnnotations.addAll( prop.getAnnotations().stream().filter( it -> it.getValue() == null ).toList() );

		return finalAnnotations;
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

	private List<AbstractInsnNode> createAbstractFunction( BoxFunctionDeclaration func ) {
		List<AbstractInsnNode> nodes = new ArrayList<AbstractInsnNode>();

		// public AbstractFunction( Key name, Argument[] arguments, String returnType, Access access, IStruct annotations, IStruct documentation,
		// String sourceObjectName, String sourceObjectType ) {
		// this.name = name;
		// this.arguments = arguments;
		// this.returnType = returnType;
		// this.access = access;
		// this.annotations = annotations;
		// this.documentation = documentation;
		// this.sourceObjectName = sourceObjectName;
		// this.sourceObjectType = sourceObjectType;
		// }

		nodes.add( new TypeInsnNode( Opcodes.NEW, Type.getInternalName( AbstractFunction.class ) ) );
		nodes.add( new InsnNode( Opcodes.DUP ) );

		// args
		// Key name
		nodes.addAll( createKey( func.getName() ) );
		// Argument[] arguments
		List<List<AbstractInsnNode>> argList = func.getArgs()
		    .stream()
		    .map( arg -> transform( arg, TransformerContext.NONE ) )
		    .toList();
		nodes.addAll( AsmHelper.array( Type.getType( Argument.class ), argList ) );
		// String returnType
		nodes.addAll( transform( func.getType(), TransformerContext.NONE ) );
		// Access access
		nodes.add(
		    new FieldInsnNode(
		        Opcodes.GETSTATIC,
		        Type.getDescriptor( Function.Access.class ),
		        func.getAccessModifier().name().toUpperCase(),
		        Type.getDescriptor( Function.Access.class )
		    )
		);
		// IStruct annotations
		// TODO
		nodes.add( new FieldInsnNode( Opcodes.GETSTATIC,
		    Type.getInternalName( Struct.class ),
		    "EMPTY",
		    Type.getDescriptor( IStruct.class ) ) );
		// IStruct documentation
		// TODO
		nodes.add( new FieldInsnNode( Opcodes.GETSTATIC,
		    Type.getInternalName( Struct.class ),
		    "EMPTY",
		    Type.getDescriptor( IStruct.class ) ) );
		// String sourceObjectName
		nodes.add( new LdcInsnNode( getProperty( "boxClassName" ) ) );
		// String sourceObjectType
		nodes.add( new LdcInsnNode( "class" ) );

		nodes.add(
		    new MethodInsnNode(
		        Opcodes.INVOKESPECIAL,
		        Type.getInternalName( ClassVariablesScope.class ),
		        "<init>",
		        Type.getMethodDescriptor( Type.VOID_TYPE, Type.getType( IClassRunnable.class ) ),
		        false
		    )
		);

		return nodes;
	}

	private List<AbstractInsnNode> generateMapOfAbstractMethodNames( BoxClass boxClass ) {
		List<List<AbstractInsnNode>>	methodKeyLists	= boxClass.getDescendantsOfType( BoxFunctionDeclaration.class )
		    .stream()
		    .filter( func -> func.getBody() == null )
		    .map( func -> {
															    List<List<AbstractInsnNode>> absFunc = List.of(
															        createKey( func.getName() ),
															        createAbstractFunction( func )
															    );

															    return absFunc;
														    } )
		    .flatMap( x -> x.stream() )
		    .collect( java.util.stream.Collectors.toList() );

		List<AbstractInsnNode>			nodes			= new ArrayList<AbstractInsnNode>();

		nodes.addAll( AsmHelper.array( Type.getType( Key.class ), methodKeyLists ) );

		nodes.add( new MethodInsnNode( Opcodes.INVOKESTATIC,
		    Type.getInternalName( MapHelper.class ),
		    "LinkedHashMapOfProperties",
		    Type.getMethodDescriptor( Type.getType( Map.class ), Type.getType( Object[].class ) ),
		    false ) );

		return nodes;
	}

	private List<AbstractInsnNode> generateSetOfCompileTimeMethodNames( BoxClass boxClass ) {
		List<List<AbstractInsnNode>>	methodKeyLists	= boxClass.getDescendantsOfType( BoxFunctionDeclaration.class )
		    .stream()
		    .map( BoxFunctionDeclaration::getName )
		    .map( this::createKey )
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
}
