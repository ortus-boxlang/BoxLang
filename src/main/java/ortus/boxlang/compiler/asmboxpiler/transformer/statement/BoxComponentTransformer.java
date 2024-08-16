package ortus.boxlang.compiler.asmboxpiler.transformer.statement;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

import ortus.boxlang.compiler.asmboxpiler.MethodContextTracker;
import ortus.boxlang.compiler.asmboxpiler.Transpiler;
import ortus.boxlang.compiler.asmboxpiler.transformer.AbstractTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.ReturnValueContext;
import ortus.boxlang.compiler.asmboxpiler.transformer.TransformerContext;
import ortus.boxlang.compiler.ast.BoxNode;
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
		nodes.add( new InsnNode( Opcodes.ACONST_NULL ) );

		nodes.add( new MethodInsnNode( Opcodes.INVOKEINTERFACE,
		    Type.getInternalName( IBoxContext.class ),
		    "invokeComponent",
		    Type.getMethodDescriptor( Type.getType( Component.BodyResult.class ), Type.getType( Key.class ), Type.getType( IStruct.class ),
		        Type.getType( Component.ComponentBody.class ) ),
		    true ) );
		nodes.add( new InsnNode( Opcodes.POP ) );

		return nodes;
	}
}
