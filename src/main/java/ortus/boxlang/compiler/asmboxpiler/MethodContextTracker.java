package ortus.boxlang.compiler.asmboxpiler;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.VarInsnNode;

public class MethodContextTracker {

	private int				varCount			= 0;
	private int				unusedStackEntries	= 0;
	private List<Integer>	contextStack		= new ArrayList<Integer>();

	public record VarStore( int index, List<AbstractInsnNode> nodes ) {

	}

	public int getUnusedStackCount() {
		return unusedStackEntries;
	}

	public MethodContextTracker( boolean isStatic ) {
		varCount = isStatic ? -1 : 0;
	}

	public void trackUnusedStackEntry() {
		unusedStackEntries += 1;
	}

	public void clearStackCounter() {
		this.unusedStackEntries = 0;
	}

	public int getCurrentStackHeight() {
		return this.unusedStackEntries;
	}

	public void decrementStackCounter( int amount ) {
		this.unusedStackEntries -= amount;
	}

	public List<AbstractInsnNode> popAllStackEntries() {
		return popStackEntries( unusedStackEntries );
	}

	public List<AbstractInsnNode> popStackEntries( int numberToPop ) {

		if ( this.unusedStackEntries == 0 ) {
			return new ArrayList<AbstractInsnNode>();
		}

		this.unusedStackEntries -= numberToPop;

		return IntStream.range( 0, numberToPop )
		    .mapToObj( ( i ) -> ( AbstractInsnNode ) new InsnNode( Opcodes.POP ) )
		    .toList();
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