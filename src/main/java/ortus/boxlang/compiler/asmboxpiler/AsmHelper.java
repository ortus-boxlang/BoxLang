package ortus.boxlang.compiler.asmboxpiler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
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

import ortus.boxlang.compiler.BoxByteCodeVersion;
import ortus.boxlang.compiler.IBoxpiler;
import ortus.boxlang.compiler.asmboxpiler.transformer.ReturnValueContext;
import ortus.boxlang.compiler.asmboxpiler.transformer.TransformerContext;
import ortus.boxlang.compiler.ast.BoxClass;
import ortus.boxlang.compiler.ast.BoxExpression;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.BoxStatement;
import ortus.boxlang.compiler.ast.expression.BoxArgument;
import ortus.boxlang.compiler.ast.expression.BoxIdentifier;
import ortus.boxlang.compiler.ast.statement.BoxExpressionStatement;
import ortus.boxlang.compiler.ast.statement.BoxFunctionDeclaration;
import ortus.boxlang.compiler.ast.statement.BoxReturnType;
import ortus.boxlang.compiler.ast.statement.BoxType;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.dynamic.IReferenceable;
import ortus.boxlang.runtime.dynamic.Referencer;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.loader.ClassLocator;
import ortus.boxlang.runtime.runnables.BoxClassSupport;
import ortus.boxlang.runtime.runnables.IClassRunnable;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.AbstractFunction;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.DefaultExpression;
import ortus.boxlang.runtime.types.FlowControlResult;
import ortus.boxlang.runtime.types.Function;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.util.MapHelper;
import ortus.boxlang.runtime.util.ResolvedFilePath;

public class AsmHelper {

	/**
	 * Legacy instruction count limit (kept for backwards compatibility).
	 * Prefer using MethodSplitter.BYTECODE_SIZE_LIMIT for byte-accurate estimation.
	 */
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

	/**
	 * Debugging helper for calling System.out.println within the ASM code.
	 * 
	 * @param message
	 * 
	 * @return
	 */
	public static List<AbstractInsnNode> println( String message ) {
		List<AbstractInsnNode> nodes = new ArrayList<>();

		nodes.add( new FieldInsnNode(
		    Opcodes.GETSTATIC,
		    "java/lang/System",
		    "out",
		    "Ljava/io/PrintStream;"
		) );
		nodes.add( new LdcInsnNode( message ) );
		nodes.add( new MethodInsnNode( Opcodes.INVOKEVIRTUAL,
		    Type.getInternalName( java.io.PrintStream.class ),
		    "println",
		    Type.getMethodDescriptor( Type.VOID_TYPE, Type.getType( String.class ) ),
		    false
		) );

		return nodes;
	}

	/**
	 * Generates ASM instructions to box a primitive value to its wrapper type.
	 * If the type is already an object type, no boxing instructions are generated.
	 * 
	 * @param visitor       The method visitor or node to add instructions to
	 * @param primitiveType The type to potentially box
	 */
	public static void boxPrimitive( MethodVisitor visitor, Type primitiveType ) {

		if ( primitiveType.getSort() == Type.OBJECT
		    || primitiveType.getSort() == Type.ARRAY ) {
			// Already an object, no boxing needed
			return;
		}

		String	boxedName	= null;
		Type	boxedType	= null;

		switch ( primitiveType.getSort() ) {
			case Type.BOOLEAN :
				boxedName = "java/lang/Boolean";
				boxedType = Type.getType( Boolean.class );
				break;
			case Type.BYTE :
				boxedName = "java/lang/Byte";
				boxedType = Type.getType( Byte.class );
				break;
			case Type.CHAR :
				boxedName = "java/lang/Character";
				boxedType = Type.getType( Character.class );
				break;
			case Type.SHORT :
				boxedName = "java/lang/Short";
				boxedType = Type.getType( Short.class );
				break;
			case Type.INT :
				boxedName = "java/lang/Integer";
				boxedType = Type.getType( Integer.class );
				break;
			case Type.LONG :
				boxedName = "java/lang/Long";
				boxedType = Type.getType( Long.class );
				break;
			case Type.FLOAT :
				boxedName = "java/lang/Float";
				boxedType = Type.getType( Float.class );
				break;
			case Type.DOUBLE :
				boxedName = "java/lang/Double";
				boxedType = Type.getType( Double.class );
				break;
		}

		visitor.visitMethodInsn( Opcodes.INVOKESTATIC,
		    boxedName,
		    "valueOf",
		    Type.getMethodDescriptor( boxedType, primitiveType ),
		    false );
	}

	/**
	 * Generates ASM instructions to unbox a wrapper type to its primitive value.
	 * 
	 * @param visitor             The method visitor to add instructions to
	 * @param wrapperType         The wrapper type to unbox
	 * @param targetPrimitiveType The target primitive type
	 */
	public static void unboxPrimitive( MethodVisitor visitor, Type wrapperType, Type targetPrimitiveType ) {
		if ( targetPrimitiveType.getSort() == Type.OBJECT || targetPrimitiveType.getSort() == Type.ARRAY ) {
			// Target is already an object, just cast if needed
			if ( !wrapperType.equals( targetPrimitiveType ) ) {
				visitor.visitTypeInsn( Opcodes.CHECKCAST, targetPrimitiveType.getInternalName() );
			}
			return;
		}

		switch ( targetPrimitiveType.getSort() ) {
			case Type.BOOLEAN :
				visitor.visitTypeInsn( Opcodes.CHECKCAST, Type.getInternalName( Boolean.class ) );
				visitor.visitMethodInsn( Opcodes.INVOKEVIRTUAL,
				    Type.getInternalName( Boolean.class ),
				    "booleanValue",
				    Type.getMethodDescriptor( Type.BOOLEAN_TYPE ),
				    false );
				break;
			case Type.BYTE :
				visitor.visitTypeInsn( Opcodes.CHECKCAST, Type.getInternalName( Number.class ) );
				visitor.visitMethodInsn( Opcodes.INVOKEVIRTUAL,
				    Type.getInternalName( Number.class ),
				    "byteValue",
				    Type.getMethodDescriptor( Type.BYTE_TYPE ),
				    false );
				break;
			case Type.CHAR :
				visitor.visitTypeInsn( Opcodes.CHECKCAST, Type.getInternalName( Character.class ) );
				visitor.visitMethodInsn( Opcodes.INVOKEVIRTUAL,
				    Type.getInternalName( Character.class ),
				    "charValue",
				    Type.getMethodDescriptor( Type.CHAR_TYPE ),
				    false );
				break;
			case Type.SHORT :
				visitor.visitTypeInsn( Opcodes.CHECKCAST, Type.getInternalName( Number.class ) );
				visitor.visitMethodInsn( Opcodes.INVOKEVIRTUAL,
				    Type.getInternalName( Number.class ),
				    "shortValue",
				    Type.getMethodDescriptor( Type.SHORT_TYPE ),
				    false );
				break;
			case Type.INT :
				visitor.visitTypeInsn( Opcodes.CHECKCAST, Type.getInternalName( Number.class ) );
				visitor.visitMethodInsn( Opcodes.INVOKEVIRTUAL,
				    Type.getInternalName( Number.class ),
				    "intValue",
				    Type.getMethodDescriptor( Type.INT_TYPE ),
				    false );
				break;
			case Type.LONG :
				visitor.visitTypeInsn( Opcodes.CHECKCAST, Type.getInternalName( Number.class ) );
				visitor.visitMethodInsn( Opcodes.INVOKEVIRTUAL,
				    Type.getInternalName( Number.class ),
				    "longValue",
				    Type.getMethodDescriptor( Type.LONG_TYPE ),
				    false );
				break;
			case Type.FLOAT :
				visitor.visitTypeInsn( Opcodes.CHECKCAST, Type.getInternalName( Number.class ) );
				visitor.visitMethodInsn( Opcodes.INVOKEVIRTUAL,
				    Type.getInternalName( Number.class ),
				    "floatValue",
				    Type.getMethodDescriptor( Type.FLOAT_TYPE ),
				    false );
				break;
			case Type.DOUBLE :
				visitor.visitTypeInsn( Opcodes.CHECKCAST, Type.getInternalName( Number.class ) );
				visitor.visitMethodInsn( Opcodes.INVOKEVIRTUAL,
				    Type.getInternalName( Number.class ),
				    "doubleValue",
				    Type.getMethodDescriptor( Type.DOUBLE_TYPE ),
				    false );
				break;
		}
	}

	public static List<AbstractInsnNode> addLineNumberLabels( List<AbstractInsnNode> nodes, BoxNode node ) {
		LabelNode start = new LabelNode();

		if ( node.getPosition() == null ) {
			return nodes;
		}

		int startLine = node.getPosition().getStart().getLine();

		nodes.add( 0, start );
		nodes.add( 1, new LineNumberNode( startLine, start ) );

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

	/***
	 * Only used for manually debugging
	 */
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

	public static ClassNode initializeClassDefinition( Type type, Type superType, Type[] interfaces ) {
		ClassNode classNode = new ClassNode();

		classNode.visit(
		    Opcodes.V21,
		    Opcodes.ACC_PUBLIC,
		    type.getInternalName(),
		    null,
		    superType.getInternalName(),
		    interfaces == null || interfaces.length == 0 ? null : Arrays.stream( interfaces ).map( Type::getInternalName ).toArray( String[]::new ) );

		return classNode;
	}

	public static void init( ClassVisitor classVisitor, boolean singleton, Type type, Type superClass, Consumer<ClassVisitor> postVisit,
	    Consumer<MethodVisitor> onConstruction,
	    Type... interfaces ) {
		classVisitor.visit(
		    Opcodes.V21,
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

	}

	public static void addConstructor(
	    ClassVisitor classVisitor,
	    boolean isPublic,
	    Type superClass,
	    Consumer<MethodVisitor> onConstruction ) {
		addConstructor(
		    classVisitor,
		    isPublic,
		    superClass,
		    new Type[] {},
		    mv -> {
		    },
		    onConstruction
		);
	}

	public static void addConstructor(
	    ClassVisitor classVisitor,
	    boolean isPublic,
	    Type superClass,
	    Type[] superArgumentTypes,
	    Consumer<MethodVisitor> preSuper,
	    Consumer<MethodVisitor> onConstruction ) {
		MethodVisitor methodVisitor = classVisitor.visitMethod( isPublic ? Opcodes.ACC_PUBLIC : Opcodes.ACC_PRIVATE,
		    "<init>",
		    Type.getMethodDescriptor( Type.VOID_TYPE ),
		    null,
		    null );
		methodVisitor.visitCode();
		methodVisitor.visitVarInsn( Opcodes.ALOAD, 0 );
		preSuper.accept( methodVisitor );
		methodVisitor.visitMethodInsn( Opcodes.INVOKESPECIAL,
		    superClass.getInternalName(),
		    "<init>",
		    Type.getMethodDescriptor( Type.VOID_TYPE, superArgumentTypes ),
		    false );
		onConstruction.accept( methodVisitor );
		methodVisitor.visitInsn( Opcodes.RETURN );
		methodVisitor.visitEnd();
	}

	public static void addGetInstance( ClassVisitor classVisitor, Type type ) {
		FieldVisitor fieldVisitor = classVisitor.visitField(
		    Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC,
		    "instance",
		    type.getDescriptor(),
		    null,
		    null );
		fieldVisitor.visitEnd();
		MethodVisitor methodVisitor = classVisitor.visitMethod(
		    Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
		    "getInstance",
		    Type.getMethodDescriptor( type ),
		    null,
		    null );
		methodVisitor.visitCode();

		// First null check (outside synchronized block)
		Label endOfMethod = new Label();
		methodVisitor.visitFieldInsn( Opcodes.GETSTATIC,
		    type.getInternalName(),
		    "instance",
		    type.getDescriptor() );
		methodVisitor.visitJumpInsn( Opcodes.IFNONNULL, endOfMethod );

		// Synchronized block on class
		methodVisitor.visitLdcInsn( type );
		methodVisitor.visitInsn( Opcodes.MONITORENTER );

		// Second null check (inside synchronized block)
		methodVisitor.visitFieldInsn( Opcodes.GETSTATIC,
		    type.getInternalName(),
		    "instance",
		    type.getDescriptor() );
		Label start = new Label(), end = new Label(), handler = new Label();
		methodVisitor.visitTryCatchBlock( start, end, handler, null );
		methodVisitor.visitLabel( start );
		methodVisitor.visitJumpInsn( Opcodes.IFNONNULL, end );

		// Create new instance
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

		// Exit synchronized block normally
		methodVisitor.visitLabel( end );
		methodVisitor.visitLdcInsn( type );
		methodVisitor.visitInsn( Opcodes.MONITOREXIT );

		// Return instance
		methodVisitor.visitLabel( endOfMethod );
		methodVisitor.visitFieldInsn( Opcodes.GETSTATIC,
		    type.getInternalName(),
		    "instance",
		    type.getDescriptor() );
		methodVisitor.visitInsn( Opcodes.ARETURN );

		// Exception handler for synchronized block
		methodVisitor.visitLabel( handler );
		methodVisitor.visitLdcInsn( type );
		methodVisitor.visitInsn( Opcodes.MONITOREXIT );
		methodVisitor.visitInsn( Opcodes.ATHROW );

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

		onCinit.accept( methodVisitor );

		methodVisitor.visitInsn( Opcodes.RETURN );

		methodVisitor.visitMaxs( 0, 0 );
		methodVisitor.visitEnd();
	}

	/**
	 * Complete a class with a static initializer that may be split into sub-methods if too large.
	 * This is the preferred method for complex classes with many properties or initializers.
	 *
	 * @param classNode The class node to add the static initializer to
	 * @param type      The class type
	 * @param supplier  Supplier that produces the static initializer instructions
	 */
	public static void completeWithSplitting( ClassNode classNode, Type type, Supplier<List<AbstractInsnNode>> supplier ) {
		List<AbstractInsnNode>	nodes			= supplier.get();
		int						estimatedSize	= MethodSplitter.estimateBytecodeSize( nodes );

		// If under the limit, use the simple approach
		if ( estimatedSize < MethodSplitter.BYTECODE_SIZE_LIMIT ) {
			MethodVisitor methodVisitor = classNode.visitMethod( Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
			    "<clinit>",
			    Type.getMethodDescriptor( Type.VOID_TYPE ),
			    null,
			    null );
			methodVisitor.visitCode();

			nodes.forEach( node -> node.accept( methodVisitor ) );

			methodVisitor.visitInsn( Opcodes.RETURN );
			methodVisitor.visitMaxs( 0, 0 );
			methodVisitor.visitEnd();
			return;
		}

		// Need to split the static initializer into sub-methods
		List<List<AbstractInsnNode>>	segments	= splitClinitIntoSegments( nodes );
		int								subCounter	= 0;

		// Create sub-methods for all but the last segment
		for ( int i = 0; i < segments.size() - 1; i++ ) {
			String			subName		= "_clinit_part_" + subCounter++;
			var				segment		= segments.get( i );

			// Create a static void method for this segment
			MethodVisitor	subMethod	= classNode.visitMethod(
			    Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC,
			    subName,
			    Type.getMethodDescriptor( Type.VOID_TYPE ),
			    null,
			    null );
			subMethod.visitCode();

			segment.forEach( node -> node.accept( subMethod ) );

			subMethod.visitInsn( Opcodes.RETURN );
			subMethod.visitMaxs( 0, 0 );
			subMethod.visitEnd();
		}

		// Create the main <clinit> that calls all sub-methods
		MethodVisitor methodVisitor = classNode.visitMethod( Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
		    "<clinit>",
		    Type.getMethodDescriptor( Type.VOID_TYPE ),
		    null,
		    null );
		methodVisitor.visitCode();

		// Call each sub-method
		for ( int i = 0; i < segments.size() - 1; i++ ) {
			String subName = "_clinit_part_" + i;
			methodVisitor.visitMethodInsn(
			    Opcodes.INVOKESTATIC,
			    type.getInternalName(),
			    subName,
			    Type.getMethodDescriptor( Type.VOID_TYPE ),
			    false );
		}

		// Inline the last segment directly in <clinit>
		segments.get( segments.size() - 1 ).forEach( node -> node.accept( methodVisitor ) );

		methodVisitor.visitInsn( Opcodes.RETURN );
		methodVisitor.visitMaxs( 0, 0 );
		methodVisitor.visitEnd();
	}

	/**
	 * Split a list of instructions into segments suitable for separate static methods.
	 * Ensures that segments don't exceed the bytecode size limit and that related
	 * operations (like array element stores) aren't split mid-operation.
	 *
	 * @param nodes The instruction list to split
	 *
	 * @return List of instruction segments
	 */
	private static List<List<AbstractInsnNode>> splitClinitIntoSegments( List<AbstractInsnNode> nodes ) {
		List<List<AbstractInsnNode>>	segments		= new ArrayList<>();
		List<AbstractInsnNode>			currentSegment	= new ArrayList<>();
		int								currentSize		= 0;
		int								targetSize		= MethodSplitter.BYTECODE_SIZE_LIMIT / 2; // Split at ~27KB to be safe

		// Track stack depth to find safe split points (when stack is empty)
		int								stackDepth		= 0;
		int								lastSafePoint	= 0;
		List<AbstractInsnNode>			pendingNodes	= new ArrayList<>();

		for ( int i = 0; i < nodes.size(); i++ ) {
			AbstractInsnNode	node		= nodes.get( i );
			int					nodeSize	= MethodSplitter.estimateInstructionSize( node );

			// Track stack depth changes
			stackDepth += getStackDelta( node );

			currentSegment.add( node );
			currentSize += nodeSize;

			// When stack is empty, this is a safe place to potentially split
			if ( stackDepth == 0 ) {
				lastSafePoint = currentSegment.size();
			}

			// Check if we should split
			if ( currentSize >= targetSize && stackDepth == 0 ) {
				// Split here - stack is empty so it's safe
				segments.add( new ArrayList<>( currentSegment ) );
				currentSegment	= new ArrayList<>();
				currentSize		= 0;
				lastSafePoint	= 0;
			} else if ( currentSize >= targetSize * 1.5 && lastSafePoint > 0 ) {
				// We've exceeded the target by a lot - split at the last safe point
				List<AbstractInsnNode>	toSplit		= new ArrayList<>( currentSegment.subList( 0, lastSafePoint ) );
				List<AbstractInsnNode>	remaining	= new ArrayList<>( currentSegment.subList( lastSafePoint, currentSegment.size() ) );

				segments.add( toSplit );
				currentSegment	= remaining;
				currentSize		= MethodSplitter.estimateBytecodeSize( currentSegment );
				stackDepth		= calculateStackDepth( currentSegment );
				lastSafePoint	= 0;
			}
		}

		// Add any remaining instructions
		if ( !currentSegment.isEmpty() ) {
			segments.add( currentSegment );
		}

		return segments;
	}

	/**
	 * Calculate the stack delta for an instruction.
	 * Positive = pushes values, Negative = pops values.
	 *
	 * @param node The instruction node
	 *
	 * @return Stack depth change
	 */
	private static int getStackDelta( AbstractInsnNode node ) {
		int opcode = node.getOpcode();
		if ( opcode == -1 ) {
			return 0; // Label, LineNumber, Frame - no stack effect
		}

		return switch ( opcode ) {
			// Push single value onto stack
			case Opcodes.ACONST_NULL, Opcodes.ICONST_M1, Opcodes.ICONST_0, Opcodes.ICONST_1, Opcodes.ICONST_2, Opcodes.ICONST_3, Opcodes.ICONST_4,
			    Opcodes.ICONST_5, Opcodes.LCONST_0, Opcodes.LCONST_1, Opcodes.FCONST_0, Opcodes.FCONST_1, Opcodes.FCONST_2, Opcodes.DCONST_0, Opcodes.DCONST_1,
			    Opcodes.BIPUSH, Opcodes.SIPUSH, Opcodes.LDC, Opcodes.ILOAD, Opcodes.LLOAD, Opcodes.FLOAD, Opcodes.DLOAD, Opcodes.ALOAD, Opcodes.GETSTATIC,
			    Opcodes.NEW -> 1;

			// Pop single value from stack
			case Opcodes.ISTORE, Opcodes.LSTORE, Opcodes.FSTORE, Opcodes.DSTORE, Opcodes.ASTORE, Opcodes.POP, Opcodes.IRETURN, Opcodes.LRETURN, Opcodes.FRETURN,
			    Opcodes.DRETURN, Opcodes.ARETURN, Opcodes.ATHROW, Opcodes.MONITORENTER, Opcodes.MONITOREXIT, Opcodes.IFNULL, Opcodes.IFNONNULL, Opcodes.IFEQ,
			    Opcodes.IFNE, Opcodes.IFLT, Opcodes.IFGE, Opcodes.IFGT, Opcodes.IFLE, Opcodes.TABLESWITCH, Opcodes.LOOKUPSWITCH -> -1;

			// Pop 2 values from stack
			case Opcodes.POP2, Opcodes.IF_ICMPEQ, Opcodes.IF_ICMPNE, Opcodes.IF_ICMPLT, Opcodes.IF_ICMPGE, Opcodes.IF_ICMPGT, Opcodes.IF_ICMPLE,
			    Opcodes.IF_ACMPEQ, Opcodes.IF_ACMPNE, Opcodes.IADD, Opcodes.LADD, Opcodes.FADD, Opcodes.DADD, Opcodes.ISUB, Opcodes.LSUB, Opcodes.FSUB,
			    Opcodes.DSUB, Opcodes.IMUL, Opcodes.LMUL, Opcodes.FMUL, Opcodes.DMUL, Opcodes.IDIV, Opcodes.LDIV, Opcodes.FDIV, Opcodes.DDIV, Opcodes.IREM,
			    Opcodes.LREM, Opcodes.FREM, Opcodes.DREM, Opcodes.ISHL, Opcodes.LSHL, Opcodes.ISHR, Opcodes.LSHR, Opcodes.IUSHR, Opcodes.LUSHR, Opcodes.IAND,
			    Opcodes.LAND, Opcodes.IOR, Opcodes.LOR, Opcodes.IXOR, Opcodes.LXOR, Opcodes.LCMP, Opcodes.FCMPL, Opcodes.FCMPG, Opcodes.DCMPL, Opcodes.DCMPG,
			    Opcodes.PUTFIELD -> -2;

			// Pop 3 values from stack
			case Opcodes.IASTORE, Opcodes.LASTORE, Opcodes.FASTORE, Opcodes.DASTORE, Opcodes.AASTORE, Opcodes.BASTORE, Opcodes.CASTORE, Opcodes.SASTORE -> -3;

			// DUP operations
			case Opcodes.DUP -> 1;
			case Opcodes.DUP_X1 -> 1;
			case Opcodes.DUP_X2 -> 1;
			case Opcodes.DUP2 -> 2;
			case Opcodes.DUP2_X1 -> 2;
			case Opcodes.DUP2_X2 -> 2;
			case Opcodes.SWAP -> 0;

			// Array load operations (pop 2, push 1)
			case Opcodes.IALOAD, Opcodes.LALOAD, Opcodes.FALOAD, Opcodes.DALOAD, Opcodes.AALOAD, Opcodes.BALOAD, Opcodes.CALOAD, Opcodes.SALOAD -> -1;

			// Get field (pop 1, push 1)
			case Opcodes.GETFIELD -> 0;

			// Put static (pop 1)
			case Opcodes.PUTSTATIC -> -1;

			// Array length (pop 1, push 1)
			case Opcodes.ARRAYLENGTH -> 0;

			// No stack change
			case Opcodes.NOP, Opcodes.RETURN, Opcodes.GOTO, Opcodes.IINC -> 0;

			// Checkcast, instanceof (pop 1, push 1)
			case Opcodes.CHECKCAST, Opcodes.INSTANCEOF -> 0;

			// Invoke instructions - complex, estimate conservatively
			default -> {
				if ( node instanceof MethodInsnNode methodNode ) {
					Type	methodType	= Type.getMethodType( methodNode.desc );
					int		delta		= -methodType.getArgumentTypes().length;
					if ( methodNode.getOpcode() != Opcodes.INVOKESTATIC ) {
						delta--; // Pop receiver
					}
					if ( methodType.getReturnType() != Type.VOID_TYPE ) {
						delta++; // Push return value
					}
					yield delta;
				}
				// For other instructions, assume neutral
				yield 0;
			}
		};
	}

	/**
	 * Calculate the total stack depth from a list of instructions.
	 *
	 * @param nodes The instruction list
	 *
	 * @return Current stack depth
	 */
	private static int calculateStackDepth( List<AbstractInsnNode> nodes ) {
		int depth = 0;
		for ( AbstractInsnNode node : nodes ) {
			depth += getStackDelta( node );
		}
		return depth;
	}

	public static List<AbstractInsnNode> transformBodyExpressionsFromScript( Transpiler transpiler, List<BoxStatement> statements, TransformerContext context,
	    ReturnValueContext finalReturnValueContext ) {

		if ( statements.isEmpty() ) {
			return new ArrayList<>();
		}

		ReturnValueContext	bodyContext					= ReturnValueContext.EMPTY_UNLESS_JUMPING;

		BoxStatement		lastStatement				= statements.getLast();

		boolean				lastStatementIsReturnable	= lastStatement instanceof BoxExpressionStatement;

		if ( !lastStatementIsReturnable ) {
			bodyContext = ReturnValueContext.EMPTY;
		}

		ReturnValueContext		finalBody	= bodyContext;

		List<AbstractInsnNode>	nodes		= statements.stream().limit( statements.size() - 1 )
		    .flatMap( child -> transpiler.transform( child, context, finalBody ).stream() )
		    .collect( Collectors.toList() );

		nodes.addAll( transpiler.transform( lastStatement, context, ReturnValueContext.VALUE_OR_NULL ) );

		if ( !lastStatementIsReturnable ) {
			nodes.add( new InsnNode( Opcodes.POP ) );
			nodes.add( new InsnNode( Opcodes.ACONST_NULL ) );
		}

		return nodes;
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

	/**
	 * Create a method with context and ClassLocator setup, applying method splitting if needed.
	 * This overload automatically handles large methods by splitting them into sub-methods.
	 *
	 * @param classNode           The class node to add the method to
	 * @param name                The method name
	 * @param parameterType       The parameter type (typically IBoxContext)
	 * @param returnType          The return type
	 * @param isStatic            Whether the method is static
	 * @param transpiler          The transpiler instance
	 * @param implicityReturnNull Whether to implicitly return null if no explicit return
	 * @param supplier            Supplier for the method body instructions
	 */
	public static void methodWithContextAndClassLocator( ClassNode classNode,
	    String name,
	    Type parameterType,
	    Type returnType,
	    boolean isStatic,
	    Transpiler transpiler,
	    boolean implicityReturnNull,
	    Supplier<List<AbstractInsnNode>> supplier ) {
		// Call the overload with the main type derived from classNode
		methodWithContextAndClassLocator(
		    classNode,
		    name,
		    parameterType,
		    returnType,
		    isStatic,
		    transpiler,
		    implicityReturnNull,
		    Type.getObjectType( classNode.name ),
		    supplier
		);
	}

	/**
	 * Create a method with context and ClassLocator setup, applying method splitting if needed.
	 * This overload allows specifying the main type for method invocations in split methods.
	 *
	 * @param classNode           The class node to add the method to
	 * @param name                The method name
	 * @param parameterType       The parameter type (typically IBoxContext)
	 * @param returnType          The return type
	 * @param isStatic            Whether the method is static
	 * @param transpiler          The transpiler instance
	 * @param implicityReturnNull Whether to implicitly return null if no explicit return
	 * @param mainType            The main type for method invocations in split methods
	 * @param supplier            Supplier for the method body instructions
	 */
	public static void methodWithContextAndClassLocator( ClassNode classNode,
	    String name,
	    Type parameterType,
	    Type returnType,
	    boolean isStatic,
	    Transpiler transpiler,
	    boolean implicityReturnNull,
	    Type mainType,
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

		var				nodes		= supplier.get();

		// Collect all labels that are in the original node list
		Set<LabelNode>	allLabels	= new HashSet<>();
		for ( AbstractInsnNode node : nodes ) {
			if ( node instanceof LabelNode labelNode ) {
				allLabels.add( labelNode );
			}
		}

		// Apply method length guard to split large methods if needed
		// Pass the tracker so we can check for try-catch blocks (which can't be split across methods)
		var				processedNodes	= methodLengthGuard( mainType, nodes, classNode, name, parameterType, returnType, transpiler, tracker );

		// Collect labels that remain in the processed nodes (after potential splitting)
		Set<LabelNode>	remainingLabels	= new HashSet<>();
		for ( AbstractInsnNode node : processedNodes ) {
			if ( node instanceof LabelNode labelNode ) {
				remainingLabels.add( labelNode );
			}
		}

		processedNodes.forEach( node -> node.accept( methodVisitor ) );

		if ( ( implicityReturnNull && !returnType.equals( Type.VOID_TYPE ) )
		    || ( processedNodes.size() == 0 && !returnType.equals( Type.VOID_TYPE ) ) ) {
			// push a null onto the stack so that we can return it if there isn't an explicity return
			methodVisitor.visitInsn( Opcodes.ACONST_NULL );
		}

		methodVisitor.visitInsn( returnType.getOpcode( Opcodes.IRETURN ) );
		methodVisitor.visitMaxs( 0, 0 );

		// Only write try-catch blocks whose labels are ALL present in the processed nodes
		// This filters out try-catch blocks that were moved to sub-methods during splitting
		tracker.getTryCatchStack().stream()
		    .filter( tryNode -> remainingLabels.contains( tryNode.start )
		        && remainingLabels.contains( tryNode.end )
		        && remainingLabels.contains( tryNode.handler ) )
		    .forEach( tryNode -> tryNode.accept( methodVisitor ) );
		tracker.clearTryCatchStack();
		methodVisitor.visitLabel( endContextLabel );
		methodVisitor.visitEnd();
		transpiler.popMethodContextTracker();
	}

	public static List<AbstractInsnNode> generateArgumentProducerLambda( Transpiler transpiler, Supplier<List<AbstractInsnNode>> nodeSupplier ) {
		Type		type		= Type.getType( "L" + transpiler.getProperty( "packageName" ).replace( '.', '/' )
		    + "/" + transpiler.getProperty( "classname" )
		    + "$Lambda_" + transpiler.incrementAndGetLambdaCounter() + ";" );

		ClassNode	classNode	= new ClassNode();

		classNode.visit(
		    Opcodes.V21,
		    Opcodes.ACC_PUBLIC,
		    type.getInternalName(),
		    null,
		    Type.getInternalName( Object.class ),
		    new String[] { Type.getInternalName( java.util.function.Function.class ) } );

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
		    "apply",
		    Type.getMethodDescriptor( Type.getType( Object.class ), Type.getType( Object.class ) ),
		    null,
		    null );
		methodVisitor.visitCode();

		t.trackNewContext();

		nodeSupplier.get().forEach( n -> n.accept( methodVisitor ) );

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

	/**
	 * Create nodes for building a ResolvedFilePath.
	 * This is the node-based version of resolvedFilePath() for use with splitting.
	 *
	 * @param mappingName  The mapping name
	 * @param mappingPath  The mapping path
	 * @param relativePath The relative path
	 * @param filePath     The file path
	 *
	 * @return List of instruction nodes
	 */
	public static List<AbstractInsnNode> resolvedFilePathNodes( String mappingName, String mappingPath, String relativePath, String filePath ) {
		List<AbstractInsnNode> nodes = new ArrayList<>();
		nodes.add( new LdcInsnNode( mappingName == null ? "" : mappingName ) );
		nodes.add( new LdcInsnNode( mappingPath == null ? "" : mappingPath ) );
		nodes.add( new LdcInsnNode( relativePath == null ? "" : relativePath ) );
		nodes.add( new LdcInsnNode( filePath ) );
		nodes.add( new MethodInsnNode(
		    Opcodes.INVOKESTATIC,
		    Type.getInternalName( ResolvedFilePath.class ),
		    "of",
		    Type.getMethodDescriptor( Type.getType( ResolvedFilePath.class ), Type.getType( String.class ), Type.getType( String.class ),
		        Type.getType( String.class ), Type.getType( String.class ) ),
		    false ) );
		return nodes;
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
			Type argumentType = descriptor.getArgumentTypes()[ index ];
			node.visitVarInsn( argumentType.getOpcode( Opcodes.ILOAD ), offset );
			// Box primitives to their wrapper types
			boxPrimitive( node, argumentType );
			node.visitInsn( Opcodes.AASTORE );
			offset += argumentType.getSize();
		}

		node.visitFieldInsn( Opcodes.GETSTATIC, Type.getInternalName( Boolean.class ), "FALSE", Type.getDescriptor( Boolean.class ) );

		node.visitMethodInsn( Opcodes.INVOKEVIRTUAL, type.getInternalName(), "dereferenceAndInvoke",
		    Type.getMethodDescriptor( Type.getType( Object.class ), Type.getType( IBoxContext.class ), Type.getType( Key.class ),
		        Type.getType( Object[].class ),
		        Type.getType( Boolean.class ) ),
		    false );

		if ( descriptor.getReturnType().getSort() == Type.VOID ) {
			node.visitInsn( Opcodes.POP );
		} else {
			// Unbox primitives from their wrapper types
			unboxPrimitive( node, Type.getType( Object.class ), descriptor.getReturnType() );
		}
		node.visitInsn( descriptor.getReturnType().getOpcode( Opcodes.IRETURN ) );

		node.visitMaxs( 0, 0 );
		node.visitEnd();

		return node;
	}

	public static MethodNode generateJavaMethodStub( String name, Type descriptor, Type type ) {
		MethodNode node = new MethodNode(
		    Opcodes.ACC_PUBLIC,
		    name,
		    descriptor.getDescriptor(),
		    null,
		    null );

		node.visitCode();

		// push this onto the stack
		node.visitVarInsn( Opcodes.ALOAD, 0 );

		// create key and leave it on the stack
		node.visitLdcInsn( name );
		node.visitMethodInsn( Opcodes.INVOKESTATIC,
		    Type.getInternalName( Key.class ),
		    "of",
		    Type.getMethodDescriptor( Type.getType( Key.class ), Type.getType( String.class ) ),
		    false );

		// make an array of the arguments
		node.visitLdcInsn( descriptor.getArgumentCount() );
		node.visitTypeInsn( Opcodes.ANEWARRAY, Type.getInternalName( Object.class ) );

		for ( int index = 0, offset = 1; index < descriptor.getArgumentCount(); index++ ) {
			node.visitInsn( Opcodes.DUP );
			node.visitLdcInsn( index );
			Type argumentType = descriptor.getArgumentTypes()[ index ];
			node.visitVarInsn( argumentType.getOpcode( Opcodes.ILOAD ), offset );
			// Box primitives to their wrapper types
			boxPrimitive( node, argumentType );
			node.visitInsn( Opcodes.AASTORE );
			offset += argumentType.getSize();
		}

		// BoxClassSupport.javaMethodStub( this, functionNameKey, args )
		node.visitMethodInsn( Opcodes.INVOKESTATIC,
		    Type.getInternalName( BoxClassSupport.class ),
		    "javaMethodStub",
		    Type.getMethodDescriptor(
		        Type.getType( Object.class ),
		        Type.getType( IReferenceable.class ),
		        Type.getType( Key.class ),
		        Type.getType( Object[].class )
		    ),
		    false );

		if ( descriptor.getReturnType().getSort() == Type.VOID ) {
			node.visitInsn( Opcodes.POP );
		} else {
			// Unbox primitives from their wrapper types
			unboxPrimitive( node, Type.getType( Object.class ), descriptor.getReturnType() );
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

	/**
	 * Guard against methods exceeding JVM's 64KB bytecode limit.
	 * Overload without tracker parameter - uses null (allows splitting).
	 *
	 * @see #methodLengthGuard(Type, List, ClassNode, String, Type, Type, Transpiler, MethodContextTracker)
	 */
	public static List<AbstractInsnNode> methodLengthGuard(
	    Type mainType,
	    List<AbstractInsnNode> nodes,
	    ClassNode classNode,
	    String name,
	    Type parameterType,
	    Type returnType,
	    Transpiler transpiler ) {
		return methodLengthGuard( mainType, nodes, classNode, name, parameterType, returnType, transpiler, null );
	}

	/**
	 * Guard against methods exceeding JVM's 64KB bytecode limit.
	 * Uses bytecode size estimation and splits methods at DividerNode boundaries.
	 * Sub-methods return FlowControlResult to propagate return/break/continue.
	 *
	 * @param mainType      The main class type for method invocation
	 * @param nodes         The instruction list to potentially split
	 * @param classNode     The class node to add sub-methods to
	 * @param name          The base method name
	 * @param parameterType The parameter type (typically IBoxContext)
	 * @param returnType    The expected return type
	 * @param transpiler    The transpiler instance
	 * @param tracker       The method context tracker (may contain try-catch blocks)
	 *
	 * @return Processed instruction list (may include sub-method calls)
	 */
	public static List<AbstractInsnNode> methodLengthGuard(
	    Type mainType,
	    List<AbstractInsnNode> nodes,
	    ClassNode classNode,
	    String name,
	    Type parameterType,
	    Type returnType,
	    Transpiler transpiler,
	    MethodContextTracker tracker ) {

		// First check using bytecode size estimation (more accurate)
		int estimatedSize = MethodSplitter.estimateBytecodeSize( nodes );
		if ( estimatedSize < MethodSplitter.BYTECODE_SIZE_LIMIT ) {
			return nodes;
		}

		// Note: Methods with try-catch blocks CAN be split now, because we filter
		// TryCatchBlockNodes when writing - only those with all labels in the current
		// method's instruction list will be written. Try-catch blocks that span
		// split boundaries will be written to the sub-methods that contain them.

		// Use the new MethodSplitter for splitting
		MethodSplitter			splitter	= new MethodSplitter( transpiler, classNode, mainType );
		Type					resultType	= Type.getType( FlowControlResult.class );

		// Split the method - sub-methods return FlowControlResult
		List<AbstractInsnNode>	splitNodes	= splitter.processMethod( nodes, name, parameterType, resultType );

		// If the return type is Object (typical for BoxLang methods), we need to
		// unwrap the final FlowControlResult to get the actual value
		if ( returnType.equals( Type.getType( Object.class ) ) && !splitNodes.isEmpty() ) {
			// The last instruction sequence should have a FlowControlResult on the stack
			// We need to call getValue() to unwrap it
			List<AbstractInsnNode> unwrapNodes = new ArrayList<>( splitNodes );

			// Add getValue() call to unwrap the result
			unwrapNodes.add( new MethodInsnNode(
			    Opcodes.INVOKEVIRTUAL,
			    Type.getInternalName( FlowControlResult.class ),
			    "getValue",
			    Type.getMethodDescriptor( Type.getType( Object.class ) ),
			    false
			) );

			return unwrapNodes;
		}

		return splitNodes;
	}

	/**
	 * Legacy method for splitting instructions by instruction count.
	 * Prefer using MethodSplitter for byte-accurate splitting.
	 *
	 * @deprecated Use MethodSplitter instead
	 */
	@Deprecated
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

	/**
	 * Adds the @BoxByteCodeVersion annotation to a generated class.
	 * This annotation contains the BoxLang version and bytecode version for compatibility validation.
	 *
	 * @param classNode The ClassNode to add the annotation to
	 */
	public static void addBoxByteCodeVersionAnnotation( ClassNode classNode ) {
		String	boxlangVersion		= BoxRuntime.getInstance().getVersionInfo().getAsString( Key.of( "version" ) );
		int		bytecodeVersion		= IBoxpiler.BYTECODE_VERSION;
		var		annotationVisitor	= classNode.visitAnnotation( Type.getDescriptor( BoxByteCodeVersion.class ), false );
		annotationVisitor.visit( "boxlangVersion", boxlangVersion );
		annotationVisitor.visit( "bytecodeVersion", bytecodeVersion );
		annotationVisitor.visitEnd();
	}

	/**
	 * Adds a static method with double-check locking pattern for lazy initialization.
	 * This is used for metadata methods like getMetaDataStatic() and getMetaStatic().
	 *
	 * @param classVisitor   The class visitor to add the method to
	 * @param type           The type of the class being modified
	 * @param methodName     The name of the static method to create
	 * @param cacheFieldName The name of the static field used to cache the result
	 * @param generateBody   Consumer that generates the method body to compute the value
	 */
	public static void addDoubleCheckLockedStaticMethod(
	    ClassVisitor classVisitor,
	    Type type,
	    String methodName,
	    String cacheFieldName,
	    Type returnType,
	    Consumer<MethodVisitor> generateBody ) {

		MethodVisitor mv = classVisitor.visitMethod(
		    Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
		    methodName,
		    Type.getMethodDescriptor( returnType ),
		    null,
		    null );
		mv.visitCode();

		// First null check (outside synchronized block)
		Label endOfMethod = new Label();
		mv.visitFieldInsn( Opcodes.GETSTATIC,
		    type.getInternalName(),
		    cacheFieldName,
		    returnType.getDescriptor() );
		mv.visitJumpInsn( Opcodes.IFNONNULL, endOfMethod );

		// Synchronized block on class
		mv.visitLdcInsn( type );
		mv.visitInsn( Opcodes.MONITORENTER );

		// Second null check (inside synchronized block)
		mv.visitFieldInsn( Opcodes.GETSTATIC,
		    type.getInternalName(),
		    cacheFieldName,
		    returnType.getDescriptor() );
		Label start = new Label(), end = new Label(), handler = new Label();
		mv.visitTryCatchBlock( start, end, handler, null );
		mv.visitLabel( start );
		mv.visitJumpInsn( Opcodes.IFNONNULL, end );

		// Generate the body that computes and stores the value
		generateBody.accept( mv );
		mv.visitFieldInsn( Opcodes.PUTSTATIC,
		    type.getInternalName(),
		    cacheFieldName,
		    returnType.getDescriptor() );

		// Exit synchronized block normally
		mv.visitLabel( end );
		mv.visitLdcInsn( type );
		mv.visitInsn( Opcodes.MONITOREXIT );

		// Return cached value
		mv.visitLabel( endOfMethod );
		mv.visitFieldInsn( Opcodes.GETSTATIC,
		    type.getInternalName(),
		    cacheFieldName,
		    returnType.getDescriptor() );
		mv.visitInsn( Opcodes.ARETURN );

		// Exception handler for synchronized block
		mv.visitLabel( handler );
		mv.visitLdcInsn( type );
		mv.visitInsn( Opcodes.MONITOREXIT );
		mv.visitInsn( Opcodes.ATHROW );

		mv.visitMaxs( 0, 0 );
		mv.visitEnd();
	}

	/**
	 * Add a static field initialized to null.
	 *
	 * @param classVisitor The class visitor
	 * @param fieldName    The name of the field
	 * @param fieldType    The type of the field
	 * @param isVolatile   Whether the field should be volatile
	 */
	public static void addNullStaticField( ClassVisitor classVisitor, String fieldName, Type fieldType, boolean isVolatile ) {
		int access = Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC;
		if ( isVolatile ) {
			access |= Opcodes.ACC_VOLATILE;
		}
		FieldVisitor fieldVisitor = classVisitor.visitField( access,
		    fieldName,
		    fieldType.getDescriptor(),
		    null,
		    null );
		fieldVisitor.visitEnd();
	}

	/**
	 * Add a public static field with an initial boolean value.
	 *
	 * @param classVisitor The class visitor
	 * @param fieldName    The name of the field
	 * @param fieldType    The type of the field (should be Type.BOOLEAN_TYPE)
	 * @param initialValue The initial boolean value
	 */
	public static void addStaticFieldWithInitialValue( ClassVisitor classVisitor, String fieldName, Type fieldType, boolean initialValue ) {
		FieldVisitor fieldVisitor = classVisitor.visitField( Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL,
		    fieldName,
		    fieldType.getDescriptor(),
		    null,
		    initialValue ? 1 : 0 );
		fieldVisitor.visitEnd();
	}

	/**
	 * Add a static getter method for an existing field (does NOT create the field).
	 * Use this when the field is already defined elsewhere.
	 *
	 * @param classVisitor The class visitor
	 * @param type         The type of the class
	 * @param fieldName    The name of the existing field
	 * @param methodName   The name of the getter method
	 * @param returnType   The return type of the getter
	 */
	public static void addStaticGetterMethodOnly( ClassVisitor classVisitor, Type type, String fieldName, String methodName, Type returnType ) {
		MethodVisitor methodVisitor = classVisitor.visitMethod( Opcodes.ACC_PUBLIC,
		    methodName,
		    Type.getMethodDescriptor( returnType ),
		    null,
		    null );
		methodVisitor.visitCode();
		methodVisitor.visitFieldInsn( Opcodes.GETSTATIC,
		    type.getInternalName(),
		    fieldName,
		    returnType.getDescriptor() );
		methodVisitor.visitInsn( returnType.getOpcode( Opcodes.IRETURN ) );
		methodVisitor.visitMaxs( 0, 0 );
		methodVisitor.visitEnd();
	}
}
