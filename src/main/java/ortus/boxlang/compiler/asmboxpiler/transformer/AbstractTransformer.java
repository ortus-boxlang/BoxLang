package ortus.boxlang.compiler.asmboxpiler.transformer;

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ortus.boxlang.compiler.asmboxpiler.AsmHelper;
import ortus.boxlang.compiler.asmboxpiler.MethodContextTracker;
import ortus.boxlang.compiler.asmboxpiler.Transpiler;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.BoxStatement;
import ortus.boxlang.compiler.ast.expression.BoxClosure;
import ortus.boxlang.compiler.ast.expression.BoxLambda;
import ortus.boxlang.compiler.ast.statement.BoxBreak;
import ortus.boxlang.compiler.ast.statement.BoxContinue;
import ortus.boxlang.compiler.ast.statement.BoxDo;
import ortus.boxlang.compiler.ast.statement.BoxForIn;
import ortus.boxlang.compiler.ast.statement.BoxForIndex;
import ortus.boxlang.compiler.ast.statement.BoxFunctionDeclaration;
import ortus.boxlang.compiler.ast.statement.BoxReturn;
import ortus.boxlang.compiler.ast.statement.BoxSwitch;
import ortus.boxlang.compiler.ast.statement.BoxWhile;
import ortus.boxlang.compiler.ast.statement.component.BoxComponent;
import ortus.boxlang.runtime.context.IBoxContext;

public abstract class AbstractTransformer implements Transformer {

	/**
	 * A body whose source text is at least this long (or which has at least
	 * {@link #OUTLINE_DESCENDANT_THRESHOLD} descendants) is large enough that compiling it
	 * inline can exceed the JVM's 64KB per-method bytecode limit, so it is hoisted into a
	 * generated method of its own - see {@link #containsEscapingFlowControl}.
	 */
	protected static final int	OUTLINE_SOURCE_THRESHOLD		= 12000;

	/** @see #OUTLINE_SOURCE_THRESHOLD */
	protected static final int	OUTLINE_DESCENDANT_THRESHOLD	= 250;

	protected Transpiler		transpiler;
	protected Logger			logger;

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
	 * True when the body contains a {@code return}/{@code break}/{@code continue} that must
	 * transfer control to a point OUTSIDE the body - the enclosing method, or a loop/switch
	 * that encloses the body.
	 * <p>
	 * Such a body cannot be moved into its own method: the exit compiles to a jump to a label
	 * which only exists in the enclosing method. A body without escaping flow control is safe
	 * to hoist into a generated helper (see {@code BoxIfElseTransformer} and the loop
	 * transformers).
	 *
	 * @param body the statement body to inspect
	 *
	 * @return true when the body contains an exit that leaves it
	 */
	protected boolean containsEscapingFlowControl( BoxStatement body ) {
		for ( BoxNode node : body.getDescendants() ) {
			if ( node instanceof BoxReturn ) {
				// Only an enclosing function/closure/lambda contains a return; anything else
				// (including a component body) lets it escape to the enclosing method.
				if ( firstEnclosingBoundary( node, body, BoxFunctionDeclaration.class, BoxClosure.class, BoxLambda.class ) == null ) {
					return true;
				}
			} else if ( node instanceof BoxBreak || node instanceof BoxContinue ) {
				BoxNode boundary = firstEnclosingBoundary( node, body, BoxFunctionDeclaration.class, BoxClosure.class, BoxLambda.class,
				    BoxComponent.class, BoxSwitch.class, BoxDo.class, BoxForIndex.class, BoxForIn.class, BoxWhile.class );
				if ( boundary == null ) {
					return true;
				}
				// A function/closure/lambda/component/switch boundary contains the exit; a loop
				// boundary only contains it when that loop is part of the body being hoisted.
				boolean isLoop = boundary instanceof BoxDo || boundary instanceof BoxForIndex || boundary instanceof BoxForIn
				    || boundary instanceof BoxWhile;
				if ( isLoop && !isAncestor( body, boundary ) ) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Walk the ancestors of {@code node}, stopping at (and excluding) {@code body}, and return
	 * the first one matching any of the supplied types.
	 *
	 * @param node  the node to start walking from (inclusive)
	 * @param body  the body the walk must not leave
	 * @param types the ancestor types to look for
	 *
	 * @return the first matching ancestor inside the body, or null
	 */
	@SafeVarargs
	private final BoxNode firstEnclosingBoundary( BoxNode node, BoxNode body, Class<? extends BoxNode>... types ) {
		for ( BoxNode current = node; current != null && current != body; current = current.getParent() ) {
			for ( Class<? extends BoxNode> type : types ) {
				if ( type.isInstance( current ) ) {
					return current;
				}
			}
		}
		return null;
	}

	/** True when {@code ancestor} is {@code node} itself or one of its ancestors. */
	private boolean isAncestor( BoxNode ancestor, BoxNode node ) {
		for ( BoxNode current = node; current != null; current = current.getParent() ) {
			if ( current == ancestor ) {
				return true;
			}
		}
		return false;
	}

	/**
	 * True when a body is big enough that compiling it inline risks exceeding the JVM's 64KB
	 * per-method bytecode limit.
	 *
	 * @param body the body to measure
	 *
	 * @return true when the body should be hoisted into its own method
	 */
	protected boolean isLargeBody( BoxNode body ) {
		return ( body.getSourceText() != null && body.getSourceText().length() >= OUTLINE_SOURCE_THRESHOLD )
		    || body.getDescendants().size() >= OUTLINE_DESCENDANT_THRESHOLD;
	}

	/**
	 * True when a statement body may be moved into its own method without changing behaviour.
	 * <p>
	 * A component invocation inside the body can report an early exit through
	 * {@link ortus.boxlang.runtime.components.Component.BodyResult}, and
	 * {@code BoxComponentTransformer} turns that into an {@code ARETURN} of the <b>enclosing</b>
	 * method when the code is inside a component body ({@code isInsideComponent()}) or in a
	 * method whose result matters ({@code canReturn()} - a function, closure, lambda, or a
	 * script whose {@code _invoke} returns a value). That {@code ARETURN} only unwinds the
	 * correct method while the call site still lives in it, so a body may only be hoisted where
	 * the component result is simply discarded: a template's {@code void _invoke}, where neither
	 * condition holds and the component call site compiles to a plain {@code POP}.
	 * <p>
	 * Hoisting a body that can early-exit out of a method that is expected to return the exit
	 * would silently swallow the exit (or emit an {@code ARETURN} into a void method).
	 *
	 * @return true when bodies may be hoisted into generated methods
	 */
	protected boolean canHoistStatementBodies() {
		return !transpiler.isInsideComponent() && !transpiler.canReturn();
	}

	/**
	 * Transform a loop body, hoisting it into its own method when it is large enough to
	 * threaten the JVM's per-method bytecode limit and nothing inside it escapes into the
	 * enclosing method.
	 * <p>
	 * The generated method returns {@code Object} and leaves the body's value on the stack,
	 * exactly like the inline body does, so the loop keeps its value semantics.
	 *
	 * @param body       the loop body
	 * @param context    the transformer context the body is normally compiled with
	 * @param namePrefix the prefix for the generated method name
	 *
	 * @return the instructions for the body
	 */
	protected List<AbstractInsnNode> transformLoopBody( BoxStatement body, TransformerContext context, String namePrefix ) {
		if ( body != null && canHoistStatementBodies() && isLargeBody( body ) && !containsEscapingFlowControl( body ) ) {
			return generateOutlinedLoopBodyCall( body, context, namePrefix );
		}

		return transpiler.transform( body, context, ReturnValueContext.VALUE_OR_NULL );
	}

	private List<AbstractInsnNode> generateOutlinedLoopBodyCall( BoxStatement body, TransformerContext context, String namePrefix ) {
		ClassNode	owningClass		= transpiler.getOwningClass();
		String		methodName		= namePrefix + "_" + transpiler.incrementAndGetLambdaCounter();
		Type		declaringType	= Type.getObjectType( owningClass.name );

		AsmHelper.methodWithContextAndClassLocator(
		    owningClass,
		    methodName,
		    Type.getType( IBoxContext.class ),
		    Type.getType( Object.class ),
		    true,
		    transpiler,
		    false,
		    () -> new ArrayList<>( transpiler.transform( body, context, ReturnValueContext.VALUE_OR_NULL ) )
		);

		List<AbstractInsnNode> nodes = new ArrayList<>();

		nodes.addAll( transpiler.getCurrentMethodContextTracker().orElseThrow().loadCurrentContext() );
		nodes.add( new MethodInsnNode(
		    Opcodes.INVOKESTATIC,
		    declaringType.getInternalName(),
		    methodName,
		    Type.getMethodDescriptor( Type.getType( Object.class ), Type.getType( IBoxContext.class ) ),
		    false
		) );

		return nodes;
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
