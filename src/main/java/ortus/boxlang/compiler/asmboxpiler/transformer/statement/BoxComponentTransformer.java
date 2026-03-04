package ortus.boxlang.compiler.asmboxpiler.transformer.statement;

import java.lang.invoke.CallSite;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;

import ortus.boxlang.compiler.asmboxpiler.AsmHelper;
import ortus.boxlang.compiler.asmboxpiler.MethodContextTracker;
import ortus.boxlang.compiler.asmboxpiler.Transpiler;
import ortus.boxlang.compiler.asmboxpiler.transformer.AbstractTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.ReturnValueContext;
import ortus.boxlang.compiler.asmboxpiler.transformer.TransformerContext;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.BoxStatement;
import ortus.boxlang.compiler.ast.expression.BoxFQN;
import ortus.boxlang.compiler.ast.expression.BoxStringLiteral;
import ortus.boxlang.compiler.ast.statement.BoxAnnotation;
import ortus.boxlang.compiler.ast.statement.component.BoxComponent;
import ortus.boxlang.runtime.components.Component;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;

public class BoxComponentTransformer extends AbstractTransformer {

	public BoxComponentTransformer( Transpiler transpiler ) {
		super( transpiler );
	}

	@Override
	public List<AbstractInsnNode> transform( BoxNode node, TransformerContext context, ReturnValueContext returnContext ) throws IllegalStateException {
		BoxComponent					boxComponent	= ( BoxComponent ) node;

		Optional<MethodContextTracker>	trackerOption	= transpiler.getCurrentMethodContextTracker();

		if ( trackerOption.isEmpty() ) {
			throw new IllegalStateException();
		}

		MethodContextTracker	tracker	= trackerOption.get();
		List<AbstractInsnNode>	nodes	= new ArrayList<>();
		nodes.addAll( tracker.loadCurrentContext() );

		String				componentName	= boxComponent.getName();
		List<BoxAnnotation>	attributes		= boxComponent.getAttributes();

		// Check for custom tag shortcut like <cf_brad>
		if ( componentName.startsWith( "_" ) ) {
			attributes.add(
			    new BoxAnnotation(
			        new BoxFQN( "name", null, componentName ),
			        new BoxStringLiteral( componentName.substring( 1 ), null, componentName ),
			        null,
			        null )
			);
			componentName = "component";
		}

		// create key of component name
		nodes.addAll( transpiler.createKey( componentName ) );

		// convert attributes to struct
		nodes.addAll( transpiler.transformAnnotations( attributes, true, false ) );

		// Component.ComponentBody
		transpiler.incrementComponentCounter();
		nodes.addAll( generateBodyNodes( boxComponent.getBody() ) );
		transpiler.decrementComponentCounter();

		nodes.add( new MethodInsnNode( Opcodes.INVOKEINTERFACE,
		    Type.getInternalName( IBoxContext.class ),
		    "invokeComponent",
		    Type.getMethodDescriptor( Type.getType( Component.BodyResult.class ), Type.getType( Key.class ), Type.getType( IStruct.class ),
		        Type.getType( Component.ComponentBody.class ) ),
		    true ) );

		if ( transpiler.isInsideComponent() ) {
			LabelNode ifLabel = new LabelNode();

			nodes.add( new InsnNode( Opcodes.DUP ) );

			nodes.add(
			    new MethodInsnNode(
			        Opcodes.INVOKEVIRTUAL,
			        Type.getInternalName( Component.BodyResult.class ),
			        "isEarlyExit",
			        Type.getMethodDescriptor( Type.BOOLEAN_TYPE ),
			        false
			    )
			);

			nodes.add( new JumpInsnNode( Opcodes.IFEQ, ifLabel ) );

			nodes.add( new InsnNode( Opcodes.ARETURN ) );

			AsmHelper.addDebugLabel( nodes, "BoxComponent - isInsideComponent ifLabel" );
			nodes.add( ifLabel );

			if ( returnContext != ReturnValueContext.VALUE && returnContext != ReturnValueContext.VALUE_OR_NULL ) {
				nodes.add( new InsnNode( Opcodes.POP ) );
			}

			return AsmHelper.addLineNumberLabels( nodes, node );
		} else if ( transpiler.canReturn() ) {
			LabelNode ifLabel = new LabelNode();

			nodes.add( new InsnNode( Opcodes.DUP ) );

			nodes.add(
			    new MethodInsnNode(
			        Opcodes.INVOKEVIRTUAL,
			        Type.getInternalName( Component.BodyResult.class ),
			        "isEarlyExit",
			        Type.getMethodDescriptor( Type.BOOLEAN_TYPE ),
			        false
			    )
			);

			AsmHelper.addDebugLabel( nodes, "BoxComponent - canReturn ifeq ifLabel" );
			nodes.add( new JumpInsnNode( Opcodes.IFEQ, ifLabel ) );

			nodes.add(
			    new MethodInsnNode(
			        Opcodes.INVOKEVIRTUAL,
			        Type.getInternalName( Component.BodyResult.class ),
			        "returnValue",
			        Type.getMethodDescriptor( Type.getType( Object.class ) ),
			        false
			    )
			);

			nodes.add( new InsnNode( Opcodes.ARETURN ) );

			AsmHelper.addDebugLabel( nodes, "BoxComponent - canReturn ifLabel" );
			nodes.add( ifLabel );
			if ( returnContext != ReturnValueContext.VALUE && returnContext != ReturnValueContext.VALUE_OR_NULL ) {
				nodes.add( new InsnNode( Opcodes.POP ) );
			}
		} else {
			// remove the body result because we decided not to use it.
			if ( returnContext != ReturnValueContext.VALUE && returnContext != ReturnValueContext.VALUE_OR_NULL ) {
				nodes.add( new InsnNode( Opcodes.POP ) );
			}
		}

		AsmHelper.addDebugLabel( nodes, "BoxComponent - done" );

		return AsmHelper.addLineNumberLabels( nodes, node );
	}

	private List<AbstractInsnNode> generateBodyNodes( List<BoxStatement> body ) {

		if ( body == null ) {
			return List.of( new InsnNode( Opcodes.ACONST_NULL ) );
		}

		List<AbstractInsnNode>				nodes			= new ArrayList<>();

		String								methodName		= "componentBody_" + transpiler.incrementAndGetLambdaCounter();

		Type								declaringType	= Type.getType( "L" + transpiler.getProperty( "packageName" ).replace( '.', '/' )
		    + "/" + transpiler.getProperty( "classname" )
		    + ";" );

		org.objectweb.asm.tree.ClassNode	owningClass		= transpiler.getOwningClass();

		// Generate the static method: static Component.BodyResult componentBody_N(IBoxContext context) { ... }
		AsmHelper.methodWithContextAndClassLocator( owningClass, methodName, Type.getType( IBoxContext.class ),
		    Type.getType( Component.BodyResult.class ), true,
		    transpiler, false,
		    () -> {
			    List<AbstractInsnNode> methodNodes = new ArrayList<>();

			    methodNodes.addAll( body.stream()
			        .flatMap( statement -> transpiler.transform( statement, TransformerContext.NONE ).stream() )
			        .toList() );

			    methodNodes.add(
			        new FieldInsnNode( Opcodes.GETSTATIC,
			            Type.getInternalName( Component.class ),
			            "DEFAULT_RETURN",
			            Type.getDescriptor( Component.BodyResult.class ) )
			    );

			    return methodNodes;
		    } );

		// Use INVOKEDYNAMIC to create ComponentBody from static method reference
		nodes.add( new InvokeDynamicInsnNode(
		    "process",
		    "()" + Type.getDescriptor( Component.ComponentBody.class ),
		    new Handle(
		        Opcodes.H_INVOKESTATIC,
		        "java/lang/invoke/LambdaMetafactory",
		        "metafactory",
		        Type.getMethodDescriptor(
		            Type.getType( CallSite.class ),
		            Type.getType( MethodHandles.Lookup.class ),
		            Type.getType( String.class ),
		            Type.getType( MethodType.class ),
		            Type.getType( MethodType.class ),
		            Type.getType( MethodHandle.class ),
		            Type.getType( MethodType.class )
		        ),
		        false
		    ),
		    Type.getMethodType( "(" + Type.getDescriptor( IBoxContext.class ) + ")" + Type.getDescriptor( Component.BodyResult.class ) ),
		    new Handle(
		        Opcodes.H_INVOKESTATIC,
		        declaringType.getInternalName(),
		        methodName,
		        "(" + Type.getDescriptor( IBoxContext.class ) + ")" + Type.getDescriptor( Component.BodyResult.class ),
		        false
		    ),
		    Type.getMethodType( "(" + Type.getDescriptor( IBoxContext.class ) + ")" + Type.getDescriptor( Component.BodyResult.class ) )
		) );

		return nodes;
	}
}
