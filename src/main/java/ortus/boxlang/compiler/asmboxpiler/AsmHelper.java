package ortus.boxlang.compiler.asmboxpiler;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import ortus.boxlang.compiler.asmboxpiler.transformer.ReturnValueContext;
import ortus.boxlang.compiler.asmboxpiler.transformer.TransformerContext;
import ortus.boxlang.compiler.ast.BoxClass;
import ortus.boxlang.compiler.ast.BoxExpression;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.BoxStatement;
import ortus.boxlang.compiler.ast.expression.BoxArgument;
import ortus.boxlang.compiler.ast.expression.BoxIdentifier;
import ortus.boxlang.compiler.ast.statement.BoxFunctionDeclaration;
import ortus.boxlang.compiler.ast.statement.BoxReturnType;
import ortus.boxlang.compiler.ast.statement.BoxType;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.dynamic.Referencer;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.loader.ClassLocator;
import ortus.boxlang.runtime.runnables.BoxClassSupport;
import ortus.boxlang.runtime.runnables.IClassRunnable;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.AbstractFunction;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.DefaultExpression;
import ortus.boxlang.runtime.types.Function;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.util.MapHelper;
import ortus.boxlang.runtime.util.ResolvedFilePath;

public class AsmHelper {

	private static final int METHOD_SIZE_LIMIT = 25000;

	public record LineNumberIns( List<AbstractInsnNode> start, List<AbstractInsnNode> end ) {

	}

	public static LineNumberIns translatePosition( BoxNode node ) {
		LabelNode	start	= new LabelNode();
		LabelNode	end		= new LabelNode();

		return new LineNumberIns(
		    List.of( start, new LineNumberNode( node.getPosition().getStart().getLine(), start ) ),
		    List.of( end, new LineNumberNode( node.getPosition().getEnd().getLine(), end ) )
		);
	}

	public static List<AbstractInsnNode> addLineNumberLabels( List<AbstractInsnNode> nodes, BoxNode node ) {
		LabelNode	start	= new LabelNode();
		LabelNode	end		= new LabelNode();

		if ( node.getPosition() == null ) {
			return nodes;
		}

		nodes.add( 0, start );
		nodes.add( 1, new LineNumberNode( node.getPosition().getStart().getLine(), start ) );

		// nodes.add( end );
		// nodes.add( new LineNumberNode( node.getPosition().getEnd().getLine(), end ) );

		return nodes;
	}

	public static List<AbstractInsnNode> generateMapOfAbstractMethodNames( Transpiler transpiler, BoxNode classOrInterface ) {
		List<List<AbstractInsnNode>>	methodKeyLists	= classOrInterface.getDescendantsOfType( BoxFunctionDeclaration.class )
		    .stream()
		    .filter( func -> func.getBody() == null )
		    .map( func -> {
															    List<List<AbstractInsnNode>> absFunc = List.of(
															        transpiler.createKey( func.getName() ),
															        createAbstractFunction( transpiler, func )
															    );

															    return absFunc;
														    } )
		    .flatMap( x -> x.stream() )
		    .collect( java.util.stream.Collectors.toList() );

		List<AbstractInsnNode>			nodes			= new ArrayList<AbstractInsnNode>();

		nodes.addAll( AsmHelper.array( Type.getType( Object.class ), methodKeyLists ) );

		nodes.add( new MethodInsnNode( Opcodes.INVOKESTATIC,
		    Type.getInternalName( MapHelper.class ),
		    "LinkedHashMapOfAny",
		    Type.getMethodDescriptor( Type.getType( Map.class ), Type.getType( Object[].class ) ),
		    false ) );

		return nodes;
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

	private static String getFunctionReturnType( BoxReturnType returnType ) {
		if ( returnType == null || returnType.getType() == null ) {
			return "any";
		}

		if ( returnType.getType().equals( BoxType.Fqn ) ) {
			return returnType.getFqn();
		}

		return returnType.getType().name();
	}

	public static List<AbstractInsnNode> createAbstractFunction( Transpiler transpiler, BoxFunctionDeclaration func ) {
		List<AbstractInsnNode> nodes = new ArrayList<AbstractInsnNode>();

		nodes.add( new TypeInsnNode( Opcodes.NEW, Type.getInternalName( AbstractFunction.class ) ) );
		nodes.add( new InsnNode( Opcodes.DUP ) );

		// args
		// Key name
		nodes.addAll( transpiler.createKey( func.getName() ) );
		// Argument[] arguments
		List<List<AbstractInsnNode>> argList = func.getArgs()
		    .stream()
		    .map( arg -> transpiler.transform( arg, TransformerContext.NONE ) )
		    .toList();
		nodes.addAll( AsmHelper.array( Type.getType( Argument.class ), argList ) );

		nodes.add( new LdcInsnNode( getFunctionReturnType( func.getType() ) ) );

		String accessModifier = "PUBLIC";

		if ( func.getAccessModifier() != null ) {
			accessModifier = func.getAccessModifier().name().toUpperCase();
		}
		// Access access
		nodes.add(
		    new FieldInsnNode(
		        Opcodes.GETSTATIC,
		        Type.getInternalName( Function.Access.class ),
		        accessModifier,
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
		nodes.add( new LdcInsnNode( transpiler.getProperty( "boxClassName" ) ) );
		// String sourceObjectType
		nodes.add( new LdcInsnNode( "class" ) );

		nodes.add(
		    new MethodInsnNode(
		        Opcodes.INVOKESPECIAL,
		        Type.getInternalName( AbstractFunction.class ),
		        "<init>",
		        Type.getMethodDescriptor(
		            Type.VOID_TYPE,
		            Type.getType( Key.class ),
		            Type.getType( Argument[].class ),
		            Type.getType( String.class ),
		            Type.getType( Function.Access.class ),
		            Type.getType( IStruct.class ),
		            Type.getType( IStruct.class ),
		            Type.getType( String.class ),
		            Type.getType( String.class )
		        ),
		        false
		    )
		);

		return nodes;
	}

	public static void addDebugLabel( List<AbstractInsnNode> nodes, String label ) {
		if ( !ASMBoxpiler.DEBUG ) {
			return;
		}

		nodes.add( new LdcInsnNode( label ) );
		nodes.add( new InsnNode( Opcodes.POP ) );
	}

	public static List<AbstractInsnNode> getDefaultExpression( AsmTranspiler transpiler, BoxExpression body ) {
		Type		type		= Type.getType( "L" + transpiler.getProperty( "packageName" ).replace( '.', '/' )
		    + "/" + transpiler.getProperty( "classname" )
		    + "$Lambda_" + transpiler.incrementAndGetLambdaCounter() + ";" );

		ClassNode	classNode	= new ClassNode();
		classNode.visitSource( transpiler.getProperty( "filePath" ), null );

		classNode.visit(
		    Opcodes.V17,
		    Opcodes.ACC_PUBLIC,
		    type.getInternalName(),
		    null,
		    Type.getInternalName( Object.class ),
		    new String[] { Type.getInternalName( DefaultExpression.class ) } );

		MethodVisitor initVisitor = classNode.visitMethod( Opcodes.ACC_PUBLIC,
		    "<init>",
		    Type.getMethodDescriptor( Type.VOID_TYPE ),
		    null,
		    null );
		initVisitor.visitCode();
		initVisitor.visitVarInsn( Opcodes.ALOAD, 0 );
		initVisitor.visitMethodInsn( Opcodes.INVOKESPECIAL,
		    Type.getInternalName( Object.class ),
		    "<init>",
		    Type.getMethodDescriptor( Type.VOID_TYPE ),
		    false );
		initVisitor.visitInsn( Opcodes.RETURN );
		initVisitor.visitEnd();

		MethodContextTracker t = new MethodContextTracker( false );
		transpiler.addMethodContextTracker( t );
		// Object evaluate( IBoxContext context );
		MethodVisitor methodVisitor = classNode.visitMethod(
		    Opcodes.ACC_PUBLIC,
		    "evaluate",
		    Type.getMethodDescriptor( Type.getType( Object.class ), Type.getType( IBoxContext.class ) ),
		    null,
		    null );
		methodVisitor.visitCode();

		t.trackNewContext();

		transpiler.transform( body, TransformerContext.NONE, ReturnValueContext.VALUE_OR_NULL )
		    .forEach( ( ins ) -> ins.accept( methodVisitor ) );

		methodVisitor.visitInsn( Opcodes.ARETURN );
		methodVisitor.visitMaxs( 0, 0 );
		methodVisitor.visitEnd();

		transpiler.popMethodContextTracker();

		transpiler.setAuxiliary( type.getClassName(), classNode );

		List<AbstractInsnNode> nodes = new ArrayList<AbstractInsnNode>();

		nodes.add( new TypeInsnNode( Opcodes.NEW, type.getInternalName() ) );
		nodes.add( new InsnNode( Opcodes.DUP ) );
		nodes.add( new MethodInsnNode( Opcodes.INVOKESPECIAL,
		    type.getInternalName(),
		    "<init>",
		    Type.getMethodDescriptor( Type.VOID_TYPE ),
		    false ) );
		return nodes;
	}

	public static List<AbstractInsnNode> callinvokeFunction(
	    Transpiler transpiler,
	    Type invokeType,
	    List<BoxArgument> args,
	    List<AbstractInsnNode> name,
	    TransformerContext context,
	    boolean safe ) {
		List<AbstractInsnNode> nodes = new ArrayList<AbstractInsnNode>();

		nodes.addAll( name );

		// handle positional args
		if ( args.size() == 0 || args.get( 0 ).getName() == null ) {
			nodes.addAll(
			    AsmHelper.array( Type.getType( Object.class ), args,
			        ( argument, i ) -> transpiler.transform( args.get( i ), context, ReturnValueContext.VALUE ) )
			);

			nodes.add( new MethodInsnNode( Opcodes.INVOKEINTERFACE,
			    Type.getInternalName( IBoxContext.class ),
			    "invokeFunction",
			    Type.getMethodDescriptor( Type.getType( Object.class ), invokeType, Type.getType( Object[].class ) ),
			    true ) );

			return nodes;
		}

		List<List<AbstractInsnNode>> keyValues = args.stream()
		    .map( arg -> {
			    List<List<AbstractInsnNode>> kv = List.of(
			        transpiler.createKey( arg.getName() ),
			        transpiler.transform( arg, context, ReturnValueContext.VALUE )
			    );

			    return kv;
		    } )
		    .flatMap( x -> x.stream() )
		    .collect( Collectors.toList() );

		nodes.addAll( AsmHelper.array( Type.getType( Object.class ), keyValues ) );

		nodes.add(
		    new MethodInsnNode( Opcodes.INVOKESTATIC,
		        Type.getInternalName( Struct.class ),
		        "linkedOf",
		        Type.getMethodDescriptor( Type.getType( IStruct.class ), Type.getType( Object[].class ) ),
		        false
		    )
		);

		nodes.add( new MethodInsnNode( Opcodes.INVOKEINTERFACE,
		    Type.getInternalName( IBoxContext.class ),
		    "invokeFunction",
		    Type.getMethodDescriptor( Type.getType( Object.class ), invokeType, Type.getType( Map.class ) ),
		    true ) );

		return nodes;

	}

	public static List<AbstractInsnNode> callReferencerGetAndInvoke(
	    Transpiler transpiler,
	    List<BoxArgument> args,
	    String name,
	    TransformerContext context,
	    boolean safe ) {
		return callReferencerGetAndInvoke( transpiler, args, transpiler.createKey( name ), context, safe );
	}

	public static List<AbstractInsnNode> callReferencerGetAndInvoke(
	    Transpiler transpiler,
	    List<BoxArgument> args,
	    List<AbstractInsnNode> name,
	    TransformerContext context,
	    boolean safe ) {
		List<AbstractInsnNode> nodes = new ArrayList<AbstractInsnNode>();

		nodes.addAll( name );

		// handle positional args
		if ( args.size() == 0 || args.get( 0 ).getName() == null ) {
			nodes.addAll(
			    AsmHelper.array( Type.getType( Object.class ), args,
			        ( argument, i ) -> transpiler.transform( args.get( i ), context, ReturnValueContext.VALUE ) )
			);

			nodes.add( new FieldInsnNode( Opcodes.GETSTATIC, Type.getInternalName( Boolean.class ), safe ? "TRUE" : "FALSE",
			    Type.getDescriptor( Boolean.class ) ) );

			nodes.add( new MethodInsnNode(
			    Opcodes.INVOKESTATIC,
			    Type.getInternalName( Referencer.class ),
			    "getAndInvoke",
			    Type.getMethodDescriptor( Type.getType( Object.class ),
			        Type.getType( IBoxContext.class ),
			        Type.getType( Object.class ),
			        Type.getType( Key.class ),
			        Type.getType( Object[].class ),
			        Type.getType( Boolean.class )
			    ),
			    false )
			);

			return nodes;
		}

		List<List<AbstractInsnNode>> keyValues = args.stream()
		    .map( arg -> {
			    List<List<AbstractInsnNode>> kv = List.of(
			        transpiler.createKey( arg.getName() ),
			        transpiler.transform( arg, context, ReturnValueContext.VALUE )
			    );

			    return kv;
		    } )
		    .flatMap( x -> x.stream() )
		    .collect( Collectors.toList() );

		nodes.addAll( AsmHelper.array( Type.getType( Object.class ), keyValues ) );

		nodes.add(
		    new MethodInsnNode( Opcodes.INVOKESTATIC,
		        Type.getInternalName( Struct.class ),
		        "linkedOf",
		        Type.getMethodDescriptor( Type.getType( IStruct.class ), Type.getType( Object[].class ) ),
		        false
		    )
		);

		nodes.add( new FieldInsnNode( Opcodes.GETSTATIC, Type.getInternalName( Boolean.class ), safe ? "TRUE" : "FALSE",
		    Type.getDescriptor( Boolean.class ) ) );

		nodes.add( new MethodInsnNode(
		    Opcodes.INVOKESTATIC,
		    Type.getInternalName( Referencer.class ),
		    "getAndInvoke",
		    Type.getMethodDescriptor( Type.getType( Object.class ),
		        Type.getType( IBoxContext.class ),
		        Type.getType( Object.class ),
		        Type.getType( Key.class ),
		        Type.getType( Map.class ),
		        Type.getType( Boolean.class )
		    ),
		    false )
		);

		return nodes;

	}

	public static List<AbstractInsnNode> callDynamicObjectInvokeConstructor( Transpiler transpiler, List<BoxArgument> args, TransformerContext context ) {
		List<AbstractInsnNode> nodes = new ArrayList<AbstractInsnNode>();

		// handle positional args
		if ( args.size() == 0 || args.get( 0 ).getName() == null ) {
			nodes.addAll(
			    AsmHelper.array( Type.getType( Object.class ), args,
			        ( argument, i ) -> transpiler.transform( args.get( i ), context, ReturnValueContext.VALUE ) )
			);

			nodes.add( new MethodInsnNode( Opcodes.INVOKEVIRTUAL,
			    Type.getInternalName( DynamicObject.class ),
			    "invokeConstructor",
			    Type.getMethodDescriptor( Type.getType( DynamicObject.class ),
			        Type.getType( IBoxContext.class ),
			        Type.getType( Object[].class ) ),
			    false ) );

			return nodes;
		}

		List<List<AbstractInsnNode>> keyValues = args.stream()
		    .map( arg -> {
			    List<List<AbstractInsnNode>> kv = List.of(
			        transpiler.createKey( arg.getName() ),
			        transpiler.transform( arg, context, ReturnValueContext.VALUE )
			    );

			    return kv;
		    } )
		    .flatMap( x -> x.stream() )
		    .collect( Collectors.toList() );

		nodes.addAll( AsmHelper.array( Type.getType( Object.class ), keyValues ) );

		nodes.add(
		    new MethodInsnNode( Opcodes.INVOKESTATIC,
		        Type.getInternalName( Struct.class ),
		        "linkedOf",
		        Type.getMethodDescriptor( Type.getType( IStruct.class ), Type.getType( Object[].class ) ),
		        false
		    )
		);

		nodes.add( new MethodInsnNode( Opcodes.INVOKEVIRTUAL,
		    Type.getInternalName( DynamicObject.class ),
		    "invokeConstructor",
		    Type.getMethodDescriptor( Type.getType( DynamicObject.class ),
		        Type.getType( IBoxContext.class ),
		        Type.getType( Map.class ) ),
		    false ) );

		return nodes;

	}

	public static void init( ClassVisitor classVisitor, boolean singleton, Type type, Type superClass, Consumer<MethodVisitor> onConstruction,
	    Type... interfaces ) {
		init( classVisitor, singleton, type, superClass, null, onConstruction, interfaces );
	}

	public static void init( ClassVisitor classVisitor, boolean singleton, Type type, Type superClass, Consumer<ClassVisitor> postVisit,
	    Consumer<MethodVisitor> onConstruction,
	    Type... interfaces ) {
		classVisitor.visit(
		    Opcodes.V17,
		    Opcodes.ACC_PUBLIC,
		    type.getInternalName(),
		    null,
		    superClass.getInternalName(),
		    interfaces.length == 0 ? null : Arrays.stream( interfaces ).map( Type::getInternalName ).toArray( String[]::new ) );

		if ( postVisit != null ) {
			postVisit.accept( classVisitor );
		}

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
		addStaticFieldGetterWithStaticGetter( classVisitor, type, field, method, staticMethod, property, value, true );
	}

	public static void addStaticFieldGetterWithStaticGetter( ClassVisitor classVisitor, Type type, String field, String method, String staticMethod,
	    Type property, Object value, boolean isFinal ) {
		addStaticFieldGetter( classVisitor, type, field, method, property, value, isFinal );
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
		addStaticFieldGetter( classVisitor, type, field, method, property, value, true );
	}

	public static void addStaticFieldGetter( ClassVisitor classVisitor, Type type, String field, String method, Type property, Object value, boolean isFinal ) {
		FieldVisitor fieldVisitor = classVisitor.visitField( Opcodes.ACC_STATIC | ( isFinal ? Opcodes.ACC_FINAL : 0 ) | Opcodes.ACC_PUBLIC,
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

	public static void addPublicStaticFieldAndPublicStaticGetter(
	    ClassVisitor classVisitor,
	    Type owningType,
	    String field,
	    String method,
	    Type propertyType,
	    Object defaultValue ) {
		FieldVisitor fieldVisitor = classVisitor.visitField( Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
		    field,
		    propertyType.getDescriptor(),
		    null,
		    defaultValue );
		fieldVisitor.visitEnd();
		MethodVisitor methodVisitor = classVisitor.visitMethod( Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
		    method,
		    Type.getMethodDescriptor( propertyType ),
		    null,
		    null );
		methodVisitor.visitCode();
		methodVisitor.visitFieldInsn( Opcodes.GETSTATIC,
		    owningType.getInternalName(),
		    field,
		    propertyType.getDescriptor() );
		methodVisitor.visitInsn( propertyType.getOpcode( Opcodes.IRETURN ) );
		methodVisitor.visitMaxs( 0, 0 );
		methodVisitor.visitEnd();
	}

	public static void addPrivateFieldGetter( ClassVisitor classVisitor, Type type, String field, String method, Type property, Object value ) {
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

	public static void addPrivateFieldGetterAndSetter( ClassVisitor classVisitor, Type type, String field, String getter, String setter, Type property,
	    Object value ) {
		addPrivateFieldGetter( classVisitor, type, field, getter, property, value );
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
		methodVisitor.visitInsn( Opcodes.RETURN );
		methodVisitor.visitMaxs( 0, 0 );
		methodVisitor.visitEnd();
	}

	public static void addFieldGetter( ClassVisitor classVisitor, Type type, String field, String method, Type property, Object value ) {
		FieldVisitor fieldVisitor = classVisitor.visitField( Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC,
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
		methodVisitor.visitFieldInsn( Opcodes.GETSTATIC,
		    type.getInternalName(),
		    field,
		    property.getDescriptor() );
		methodVisitor.visitInsn( property.getOpcode( Opcodes.IRETURN ) );
		methodVisitor.visitMaxs( 0, 0 );
		methodVisitor.visitEnd();
	}

	public static void addPrviateStaticFieldGetter( ClassVisitor classVisitor, Type type, String field, String method, Type property, Object value ) {
		FieldVisitor fieldVisitor = classVisitor.visitField( Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC,
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
		methodVisitor.visitFieldInsn( Opcodes.GETSTATIC,
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

	public static List<AbstractInsnNode> transformBodyExpressions( Transpiler transpiler, List<BoxStatement> statements, TransformerContext context,
	    ReturnValueContext finalReturnValueContext ) {

		if ( statements.isEmpty() ) {
			return new ArrayList<>();
		}

		ReturnValueContext		bodyContext		= finalReturnValueContext == ReturnValueContext.EMPTY ? ReturnValueContext.EMPTY
		    : ReturnValueContext.EMPTY_UNLESS_JUMPING;

		List<AbstractInsnNode>	nodes			= statements.stream().limit( statements.size() - 1 )
		    .flatMap( child -> transpiler.transform( child, context, bodyContext ).stream() )
		    .collect( Collectors.toList() );

		BoxStatement			lastStatement	= statements.getLast();

		nodes.addAll( transpiler.transform( lastStatement, context, finalReturnValueContext ) );

		return nodes;
	}

	public static void methodWithContextAndClassLocator( ClassNode classNode,
	    String name,
	    Type parameterType,
	    Type returnType,
	    boolean isStatic,
	    Transpiler transpiler,
	    boolean implicityReturnNull,
	    Supplier<List<AbstractInsnNode>> supplier ) {
		MethodContextTracker tracker = new MethodContextTracker( isStatic );
		transpiler.addMethodContextTracker( tracker );
		MethodVisitor methodVisitor = classNode.visitMethod(
		    Opcodes.ACC_PUBLIC | ( isStatic ? Opcodes.ACC_STATIC : 0 ),
		    name,
		    Type.getMethodDescriptor( returnType, parameterType ),
		    null,
		    null );
		methodVisitor.visitCode();
		Label	startContextLabel	= new Label();
		Label	endContextLabel		= new Label();
		methodVisitor.visitLabel( startContextLabel );
		methodVisitor.visitLocalVariable( "context", Type.getDescriptor( IBoxContext.class ), null, startContextLabel, endContextLabel, isStatic ? 0 : 1 );

		if ( !isStatic ) {
			methodVisitor.visitLocalVariable( "this", Type.getObjectType( classNode.name ).getDescriptor(), null, startContextLabel, endContextLabel, 0 );
		}

		// start tracking the context
		methodVisitor.visitVarInsn( Opcodes.ALOAD, isStatic ? 0 : 1 );
		tracker.trackNewContext().forEach( ( node ) -> node.accept( methodVisitor ) );
		methodVisitor.visitMethodInsn(
		    Opcodes.INVOKESTATIC,
		    Type.getInternalName( ClassLocator.class ),
		    "getInstance",
		    Type.getMethodDescriptor( Type.getType( ClassLocator.class ) ),
		    false );
		tracker.storeNewVariable( Opcodes.ASTORE ).nodes().forEach( ( node ) -> node.accept( methodVisitor ) );

		supplier.get().forEach( node -> node.accept( methodVisitor ) );

		if ( implicityReturnNull && !returnType.equals( Type.VOID_TYPE ) ) {
			// push a null onto the stack so that we can return it if there isn't an explicity return
			methodVisitor.visitInsn( Opcodes.ACONST_NULL );
		}

		methodVisitor.visitInsn( returnType.getOpcode( Opcodes.IRETURN ) );
		methodVisitor.visitMaxs( 0, 0 );

		// TODO needs to only use try catches that match labels in the above node list
		// TODO should only clear the used nodes
		tracker.getTryCatchStack().stream().forEach( ( tryNode ) -> tryNode.accept( methodVisitor ) );
		tracker.clearTryCatchStack();
		methodVisitor.visitLabel( endContextLabel );
		methodVisitor.visitEnd();
		transpiler.popMethodContextTracker();
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

			List<AbstractInsnNode> toAdd = transformer.apply( values.get( i ), i );
			if ( toAdd.size() == 0 ) {
				nodes.add( new InsnNode( Opcodes.ACONST_NULL ) );
			}

			nodes.addAll( toAdd );
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
		node.visitMethodInsn( Opcodes.INVOKEVIRTUAL,
		    Type.getInternalName( BoxRuntime.class ),
		    "getRuntimeContext",
		    Type.getMethodDescriptor( Type.getType( IBoxContext.class ) ),
		    false );
		node.visitMethodInsn( Opcodes.INVOKESPECIAL,
		    Type.getInternalName( ScriptingRequestBoxContext.class ),
		    "<init>",
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

	public static void addLazySingleton( ClassVisitor classVisitor, Type type, Consumer<MethodVisitor> instantiation, Type... arguments ) {
		classVisitor.visitField( Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC,
		    "instance",
		    type.getDescriptor(),
		    null,
		    null ).visitEnd();
		MethodVisitor methodVisitor = classVisitor.visitMethod( Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
		    "getInstance",
		    Type.getMethodDescriptor( type, arguments ),
		    null,
		    null );

		methodVisitor.visitCode();
		Label endOfMethod = new Label();
		methodVisitor.visitFieldInsn( Opcodes.GETSTATIC,
		    type.getInternalName(),
		    "instance",
		    type.getDescriptor() );
		methodVisitor.visitJumpInsn( Opcodes.IFNONNULL, endOfMethod );
		methodVisitor.visitLdcInsn( type );
		methodVisitor.visitInsn( Opcodes.MONITORENTER );
		methodVisitor.visitFieldInsn( Opcodes.GETSTATIC,
		    type.getInternalName(),
		    "instance",
		    type.getDescriptor() );
		Label start = new Label(), end = new Label(), handler = new Label();
		methodVisitor.visitTryCatchBlock( start, end, handler, null );
		methodVisitor.visitLabel( start );
		methodVisitor.visitJumpInsn( Opcodes.IFNONNULL, end );
		instantiation.accept( methodVisitor );
		methodVisitor.visitLabel( end );
		methodVisitor.visitLdcInsn( type );
		methodVisitor.visitInsn( Opcodes.MONITOREXIT );
		methodVisitor.visitLabel( endOfMethod );
		methodVisitor.visitFieldInsn( Opcodes.GETSTATIC,
		    type.getInternalName(),
		    "instance",
		    type.getDescriptor() );
		methodVisitor.visitInsn( Opcodes.ARETURN );
		methodVisitor.visitLabel( handler );
		methodVisitor.visitLdcInsn( type );
		methodVisitor.visitInsn( Opcodes.MONITOREXIT );
		methodVisitor.visitInsn( Opcodes.ATHROW );
		methodVisitor.visitMaxs( 0, 0 );
		methodVisitor.visitEnd();
	}

	/**
	 * Create the nodes for loading a class from the class locator
	 * 
	 * classLocator.load( context, "NameOfClass", imports )
	 * 
	 * @param transpiler The transpiler
	 * @param identifier The identifier of the class to load
	 * 
	 * @return The nodes
	 */
	public static List<AbstractInsnNode> loadClass( Transpiler transpiler, BoxIdentifier identifier ) {
		List<AbstractInsnNode> nodes = new ArrayList<>();
		// the variable at slot 2 needs to be an instance of ClassLocator
		// todo convert this to use some specific method like tracker.loadClassLocator()
		nodes.add( new VarInsnNode( Opcodes.ALOAD, 2 ) );
		transpiler.getCurrentMethodContextTracker().ifPresent( ( t ) -> nodes.addAll( t.loadCurrentContext() ) );
		nodes.add( new LdcInsnNode( identifier.getName() ) );
		nodes.add( new FieldInsnNode( Opcodes.GETSTATIC,
		    transpiler.getProperty( "packageName" ).replace( '.', '/' )
		        + "/"
		        + transpiler.getProperty( "classname" ),
		    "imports",
		    Type.getDescriptor( List.class ) ) );
		nodes.add( new MethodInsnNode( Opcodes.INVOKEVIRTUAL,
		    Type.getInternalName( ClassLocator.class ),
		    "load",
		    Type.getMethodDescriptor( Type.getType( DynamicObject.class ), Type.getType( IBoxContext.class ), Type.getType( String.class ),
		        Type.getType( List.class ) ),
		    false ) );
		return nodes;
	}

	public static List<AbstractInsnNode> methodLengthGuard(
	    Type mainType,
	    List<AbstractInsnNode> nodes,
	    ClassNode classNode,
	    String name,
	    Type parameterType,
	    Type returnType,
	    Transpiler transpiler ) {

		if ( nodes.size() < METHOD_SIZE_LIMIT ) {
			return nodes;
		}

		List<List<AbstractInsnNode>>	subNodes	= splitifyInstructions( nodes );

		List<AbstractInsnNode>			toReturn	= subNodes.stream().map( nodeList -> {
														String subName = "_sub_" + name + nodeList.hashCode();
														methodWithContextAndClassLocator(
														    classNode,
														    subName,
														    parameterType,
														    returnType,
														    false,
														    transpiler,
														    true,
														    () -> nodeList
														);

														List<AbstractInsnNode> subMethodCallNodes = new ArrayList<AbstractInsnNode>();

														subMethodCallNodes.add(
														    new VarInsnNode( Opcodes.ALOAD, 0 )
														);

														subMethodCallNodes.add(
														    new VarInsnNode( Opcodes.ALOAD, 1 )
														);

														subMethodCallNodes.add(
														    new MethodInsnNode(
														        Opcodes.INVOKEVIRTUAL,
														        mainType.getInternalName(),
														        subName,
														        Type.getMethodDescriptor( returnType, parameterType ),
														        false
														    )
														);

														subMethodCallNodes.add( new InsnNode( Opcodes.POP ) );

														return subMethodCallNodes;
													} )
		    .flatMap( s -> s.stream() )
		    .collect( Collectors.toList() );

		toReturn.removeLast();

		return toReturn;
	}

	private static List<List<AbstractInsnNode>> splitifyInstructions( List<AbstractInsnNode> nodes ) {
		List<List<AbstractInsnNode>>	subNodes	= new ArrayList<>();
		int								min			= 0;

		while ( min < nodes.size() ) {

			for ( var i = min + Math.min( METHOD_SIZE_LIMIT, nodes.size() - min ); i >= min; i-- ) {
				if ( ! ( nodes.get( i ) instanceof DividerNode ) && i != min ) {
					continue;
				}

				if ( min == i ) {
					i = nodes.size();
					for ( var j = min + Math.min( METHOD_SIZE_LIMIT, nodes.size() - min ); j < nodes.size(); j++ ) {
						if ( ! ( nodes.get( j ) instanceof DividerNode ) ) {
							continue;
						}

						i = j;
						break;
					}
				}

				subNodes.add( nodes.subList( min, i ) );
				min = i;
				break;
			}

			if ( nodes.size() - min <= METHOD_SIZE_LIMIT ) {
				subNodes.add( nodes.subList( min, nodes.size() ) );
				break;
			}
		}

		return subNodes;
	}
}
