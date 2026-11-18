package ortus.boxlang.compiler.asmboxpiler.transformer;

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ortus.boxlang.compiler.asmboxpiler.AsmHelper;
import ortus.boxlang.compiler.asmboxpiler.MethodContextTracker;
import ortus.boxlang.compiler.asmboxpiler.Transpiler;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.expression.BoxClosure;
import ortus.boxlang.compiler.ast.expression.BoxLambda;
import ortus.boxlang.compiler.ast.statement.BoxDo;
import ortus.boxlang.compiler.ast.statement.BoxForIn;
import ortus.boxlang.compiler.ast.statement.BoxForIndex;
import ortus.boxlang.compiler.ast.statement.BoxFunctionDeclaration;
import ortus.boxlang.compiler.ast.statement.BoxSwitch;
import ortus.boxlang.compiler.ast.statement.BoxWhile;
import ortus.boxlang.compiler.ast.statement.component.BoxComponent;

public abstract class AbstractTransformer implements Transformer {

	protected Transpiler	transpiler;
	protected Logger		logger;

	public enum ExitsAllowed {
		COMPONENT, LOOP, FUNCTION, DEFAULT
	}

	public AbstractTransformer( Transpiler transpiler ) {
		this.transpiler	= transpiler;
		this.logger		= LoggerFactory.getLogger( this.getClass() );
	}

	@SuppressWarnings( "unchecked" )
	public ExitsAllowed getExitsAllowed( BoxNode node ) {
		BoxNode ancestor = node.getFirstNodeOfTypes( BoxFunctionDeclaration.class, BoxClosure.class, BoxLambda.class, BoxComponent.class, BoxDo.class,
		    BoxForIndex.class, BoxForIn.class,
		    BoxSwitch.class, BoxWhile.class );
		if ( ancestor instanceof BoxFunctionDeclaration || ancestor instanceof BoxClosure || ancestor instanceof BoxLambda ) {
			return ExitsAllowed.FUNCTION;
		} else if ( ancestor instanceof BoxComponent ) {
			return ExitsAllowed.COMPONENT;
		} else if ( ancestor instanceof BoxSwitch sw
		    && sw.hasBreakingCases() ) {
			// Breaking switches are not valid break targets - keep looking
			return getExitsAllowed( ancestor.getParent() );
		} else if ( ancestor != null ) {
			return ExitsAllowed.LOOP;
		} else {
			return ExitsAllowed.DEFAULT;
		}
	}

	/**
	 * Inline the {@code finally} bodies that a {@code break}/{@code continue} must run before
	 * transferring control. The JVM does not execute exception handlers (where the finally
	 * normally lives) for a {@code GOTO}/{@code return}, so — exactly like javac — every
	 * finally between the exit statement and its target is copied onto the exit path.
	 *
	 * @param tracker  the current method's context tracker
	 * @param exitNode the {@code break}/{@code continue} node performing the exit
	 * @param target   the node control transfers to, or null when the exit leaves the current
	 *                 method (then every enclosing finally must run)
	 * @param context  the transformer context of the exit statement
	 *
	 * @return the instructions to emit before the exit jump, empty when no finally is pending
	 */
	protected List<AbstractInsnNode> transformPendingFinallyBodies( MethodContextTracker tracker, BoxNode exitNode, BoxNode target,
	    TransformerContext context ) {
		List<AbstractInsnNode>					nodes	= new ArrayList<>();

		List<MethodContextTracker.FinallyBody>	pending	= tracker.getPendingFinallyBodies( exitNode, target );

		if ( pending.isEmpty() ) {
			return nodes;
		}

		// The finally body is compiled into multiple bytecode copies of which only one runs
		// at runtime, so THIS copy's claims must not starve the copies transformed later.
		var finallyMarksSnapshot = transpiler.snapshotEmittedMarks();

		// Innermost first, matching the order the finally blocks are unwound at runtime.
		for ( MethodContextTracker.FinallyBody finallyBody : pending ) {
			tracker.pushOpenFinallyBody( finallyBody );
			try {
				transpiler.emitBraceGroupMark( nodes, finallyBody.keywordPos(), false );
				nodes.addAll( AsmHelper.transformBodyExpressions(
				    transpiler,
				    finallyBody.body(),
				    context,
				    ReturnValueContext.EMPTY
				) );
			} finally {
				tracker.popOpenFinallyBody();
			}
		}

		transpiler.restoreEmittedMarks( finallyMarksSnapshot );

		return nodes;
	}
}
