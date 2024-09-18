package ortus.boxlang.compiler.asmboxpiler.transformer.statement;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;

import ortus.boxlang.compiler.asmboxpiler.AsmHelper;
import ortus.boxlang.compiler.asmboxpiler.MethodContextTracker;
import ortus.boxlang.compiler.asmboxpiler.Transpiler;
import ortus.boxlang.compiler.asmboxpiler.transformer.AbstractTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.ReturnValueContext;
import ortus.boxlang.compiler.asmboxpiler.transformer.TransformerContext;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.BoxStatement;
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

		// create key of component name
		nodes.addAll( transpiler.createKey( boxComponent.getName() ) );

		// convert attributes to struct
		nodes.addAll( transpiler.transformAnnotations( boxComponent.getAttributes() ) );

		// Component.ComponentBody
		nodes.addAll( generateBodyNodes( boxComponent.getBody() ) );

		nodes.add( new MethodInsnNode( Opcodes.INVOKEINTERFACE,
		    Type.getInternalName( IBoxContext.class ),
		    "invokeComponent",
		    Type.getMethodDescriptor( Type.getType( Component.BodyResult.class ), Type.getType( Key.class ), Type.getType( IStruct.class ),
		        Type.getType( Component.ComponentBody.class ) ),
		    true ) );
		nodes.add( new InsnNode( Opcodes.POP ) );

		return nodes;
	}

	private List<AbstractInsnNode> generateBodyNodes( List<BoxStatement> body ) {

		if ( body == null || body.size() == 0 ) {
			return List.of( new InsnNode( Opcodes.ACONST_NULL ) );
		}

		List<AbstractInsnNode>	nodes		= new ArrayList<>();

		// create class
		Type					bodyType	= defineBodyLambdaClass( body );

		// instantiate
		nodes.add( new TypeInsnNode( Opcodes.NEW, bodyType.getInternalName() ) );
		nodes.add( new InsnNode( Opcodes.DUP ) );

		nodes.add( new MethodInsnNode( Opcodes.INVOKESPECIAL,
		    bodyType.getInternalName(),
		    "<init>",
		    Type.getMethodDescriptor( Type.VOID_TYPE ),
		    false )
		);

		return nodes;
	}

	private Type defineBodyLambdaClass( List<BoxStatement> body ) {
		Type		type		= Type.getType( "L" + transpiler.getProperty( "packageName" ).replace( '.', '/' )
		    + "/" + transpiler.getProperty( "classname" )
		    + "$ComponentBodyLambda_" + transpiler.incrementAndGetLambdaCounter() + ";" );

		ClassNode	classNode	= new ClassNode();

		AsmHelper.init( classNode, false, type, Type.getType( Object.class ), methodVisitor -> {
		}, Type.getType( Component.ComponentBody.class ) );

		AsmHelper.methodWithContextAndClassLocator( classNode, "process", Type.getType( IBoxContext.class ), Type.getType( Component.BodyResult.class ), false,
		    transpiler, false,
		    () -> {
			    List<AbstractInsnNode> nodes = new ArrayList<>();

			    nodes.addAll(
			        body.stream()
			            .flatMap( statement -> transpiler.transform( statement, TransformerContext.NONE ).stream() )
			            .toList()
			    );

			    nodes.add(
			        new FieldInsnNode( Opcodes.GETSTATIC,
			            Type.getInternalName( Component.class ),
			            "DEFAULT_RETURN",
			            Type.getDescriptor( Component.BodyResult.class ) )
			    );

			    return nodes;

		    } );

		AsmHelper.complete( classNode, type, methodVisitor -> {

		} );

		transpiler.setAuxiliary( type.getClassName(), classNode );

		return type;
	}
}
