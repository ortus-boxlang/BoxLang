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

import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.BoxStatement;
import ortus.boxlang.compiler.ast.Point;

public class MethodContextTracker {

	private int						varCount			= 0;
	private int						classLocatorSlot	= -1;
	private int						unusedStackEntries	= 0;
	private int						returnDepth			= 0;
	private List<Integer>			contextStack		= new ArrayList<Integer>();
	private List<TryCatchBlockNode>	tryCatchBlockNodes	= new ArrayList<TryCatchBlockNode>();
	private List<FinallyBody>		finallyBodies		= new ArrayList<FinallyBody>();
	private Map<String, LabelNode>	breaks				= new LinkedHashMap<>();
	private Map<String, LabelNode>	continues			= new LinkedHashMap<>();
	private final CompilationType	type;
	private Map<BoxNode, LabelNode>	nodeBreaks			= new LinkedHashMap<>();
	private Map<BoxNode, LabelNode>	nodeContinues		= new LinkedHashMap<>();
	private Map<String, BoxNode>	stringLabel			= new LinkedHashMap<>();

	public enum CompilationType {
		BoxClass,
		Component,
		Function
	}

	public record VarStore( int index, List<AbstractInsnNode> nodes ) {

	}

	/**
	 * A {@code finally} body queued for inlining by an enclosing {@code return},
	 * paired with the {@code finally} keyword position so the return path can emit
	 * the finally brace-group mark (the header + closing brace) — the SAME group the
	 * fall-through, no-catch-matched and exception-handler copies emit.
	 *
	 * @param body       the finally body statements
	 * @param keywordPos the {@code finally} keyword start position (brace-group key)
	 */
	public record FinallyBody( List<BoxStatement> body, Point keywordPos ) {

	}

	public MethodContextTracker( boolean isStatic ) {
		this( CompilationType.BoxClass, isStatic );
	}

	public MethodContextTracker( CompilationType type, boolean isStatic ) {
		this.type	= type;
		varCount	= isStatic ? -1 : 0;
	}

	public int getReturnDepth() {
		return this.returnDepth;
	}

	public void incrementReturnDepth() {
		this.returnDepth += 1;
	}

	public void decrementReturnDepth() {
		this.returnDepth -= 1;
	}

	public List<FinallyBody> getFinallyBodies() {
		return this.finallyBodies;
	}

	/**
	 * Queue a {@code finally} body for inlining by any enclosing {@code return}.
	 * The {@code finally} keyword position is retained so the return path can also
	 * emit the finally brace-group mark (its body is compiled into several bytecode
	 * copies of which only one runs).
	 *
	 * @param finallyBody the finally body statements
	 * @param keywordPos  the {@code finally} keyword start position
	 */
	public void addFinallyBody( List<BoxStatement> finallyBody, Point keywordPos ) {
		this.finallyBodies.add( new FinallyBody( finallyBody, keywordPos ) );
	}

	public void popFinallyBody() {
		this.finallyBodies.removeLast();
	}

	public boolean canReturn() {
		return this.type == CompilationType.Function;
	}

	public void setStringLabel( String label, BoxNode target ) {
		stringLabel.put( label, target );
	}

	public void inheritLoopControlState( MethodContextTracker parentTracker ) {
		if ( parentTracker == null ) {
			return;
		}

		this.nodeBreaks.putAll( parentTracker.nodeBreaks );
		this.nodeContinues.putAll( parentTracker.nodeContinues );
		this.stringLabel.putAll( parentTracker.stringLabel );
	}

	public BoxNode getStringLabel( String label ) {
		return stringLabel.get( label );
	}

	public void setBreak( BoxNode node, LabelNode label ) {
		nodeBreaks.put( node, label );
	}

	public LabelNode getBreak( BoxNode node ) {
		return nodeBreaks.get( node );
	}

	public void setContinue( BoxNode node, LabelNode label ) {
		nodeContinues.put( node, label );
	}

	public LabelNode getContinue( BoxNode node ) {
		return nodeContinues.get( node );
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

	/**
	 * The highest JVM local variable slot this tracker has allocated so far. Callers
	 * that clone a body into a NEW method (split sub-methods) use this as a floor so
	 * the new method's own locals never collide with the cloned body's slots.
	 *
	 * @return the highest allocated slot (-1 for a static method with none yet)
	 */
	public int getVarCount() {
		return this.varCount;
	}

	/**
	 * Prevent {@link #storeNewVariable(int)} from allocating any slot at or below
	 * {@code highestUsedSlot}.
	 * <p>
	 * A split sub-method receives the parent method's instructions verbatim, so those
	 * instructions keep reading and writing the local slots the PARENT tracker handed
	 * out (context, class locator and every body temporary). A sub-method gets a
	 * FRESH tracker that would otherwise restart numbering from scratch, so a newly
	 * allocated local could land on one of those inherited slots — clobbering it and
	 * making the verifier reject the handler that reads it
	 * ({@code VerifyError: Bad local variable type}). Reserving past the inherited
	 * slots keeps new allocations out of the cloned body's way.
	 *
	 * @param highestUsedSlot the highest slot used by the inherited body
	 */
	public void reserveLocalSlotsUpTo( int highestUsedSlot ) {
		if ( highestUsedSlot > this.varCount ) {
			this.varCount = highestUsedSlot;
		}
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

	public void setClassLocatorSlot( int slot ) {
		this.classLocatorSlot = slot;
	}

	public int getClassLocatorSlot() {
		return this.classLocatorSlot;
	}

	public List<AbstractInsnNode> loadClassLocator() {
		if ( this.classLocatorSlot < 0 ) {
			throw new IllegalStateException( "ClassLocator slot has not been set" );
		}
		return List.of(
		    new VarInsnNode( Opcodes.ALOAD, this.classLocatorSlot )
		);
	}
}