package ortus.boxlang.compiler.asmboxpiler;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.VarInsnNode;

public class MethodContextTracker {

	private int						varCount			= 0;
	private int						unusedStackEntries	= 0;
	private List<Integer>			contextStack		= new ArrayList<Integer>();
	private List<TryCatchBlockNode>	tryCatchBlockNodes	= new ArrayList<TryCatchBlockNode>();
	private Map<String, LabelNode>	breaks				= new LinkedHashMap<>();
	private Map<String, LabelNode>	continues			= new LinkedHashMap<>();

	public record VarStore( int index, List<AbstractInsnNode> nodes ) {

	}

	public LabelNode getCurrentBreak( String label ) {
		return breaks.get( label == null ? "" : label );
	}

	public void setCurrentBreak( String label, LabelNode labelNode ) {
		this.breaks.put( label == null ? "" : label, labelNode );
	}

	public void removeCurrentBreak( String label ) {
		this.breaks.remove( label == null ? "" : label );
	}

	public LabelNode getCurrentContinue( String label ) {
		return continues.get( label == null ? "" : label );
	}

	public void setCurrentContinue( String label, LabelNode labelNode ) {
		this.continues.put( label == null ? "" : label, labelNode );
	}

	public void removeCurrentContinue( String label ) {
		this.continues.remove( label == null ? "" : label );
	}

	public List<TryCatchBlockNode> getTryCatchStack() {
		return tryCatchBlockNodes;
	}

	public void addTryCatchBlock( TryCatchBlockNode tryCatchBlockNode ) {
		tryCatchBlockNodes.add( tryCatchBlockNode );
	}

	public void clearTryCatchStack() {
		tryCatchBlockNodes = new ArrayList<TryCatchBlockNode>();
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