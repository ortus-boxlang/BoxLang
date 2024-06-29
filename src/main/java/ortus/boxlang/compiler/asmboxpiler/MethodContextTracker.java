package ortus.boxlang.compiler.asmboxpiler;

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

public class MethodContextTracker {

	private int				varCount		= 0;
	private List<Integer>	contextStack	= new ArrayList<Integer>();

	public record VarStore( int index, List<AbstractInsnNode> nodes ) {

	}

	public MethodContextTracker( boolean isStatic ) {
		varCount = isStatic ? -1 : 0;
	}

	public VarStore storeNewVariable( int opcode ) {
		varCount += 1;
		return new VarStore( varCount, List.of( new VarInsnNode( opcode, varCount ) ) );
	}

	public List<AbstractInsnNode> trackNewContext() {
		VarStore res = storeNewVariable( Opcodes.ASTORE );

		contextStack.add( res.index );

		return res.nodes;
	}

	public void popContext() {
		contextStack.removeLast();
	}

	public List<AbstractInsnNode> loadCurrentContext() {
		return List.of(
		    new VarInsnNode( Opcodes.ALOAD, contextStack.getLast() )
		);
	}
}
