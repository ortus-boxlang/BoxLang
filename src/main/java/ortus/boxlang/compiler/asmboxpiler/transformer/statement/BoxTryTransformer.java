/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package ortus.boxlang.compiler.asmboxpiler.transformer.statement;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import ortus.boxlang.compiler.asmboxpiler.AsmHelper;
import ortus.boxlang.compiler.asmboxpiler.MethodContextTracker;
import ortus.boxlang.compiler.asmboxpiler.Transpiler;
import ortus.boxlang.compiler.asmboxpiler.transformer.AbstractTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.ReturnValueContext;
import ortus.boxlang.compiler.asmboxpiler.transformer.TransformerContext;
import ortus.boxlang.compiler.ast.BoxExpression;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.BoxStatement;
import ortus.boxlang.compiler.ast.Point;
import ortus.boxlang.compiler.ast.statement.BoxTry;
import ortus.boxlang.compiler.ast.statement.BoxTryCatch;
import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.context.CatchBoxContext;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.exceptions.AbortException;
import ortus.boxlang.runtime.types.exceptions.ExceptionUtil;

public class BoxTryTransformer extends AbstractTransformer {

	public BoxTryTransformer( Transpiler transpiler ) {
		super( transpiler );
	}

	@Override
	public List<AbstractInsnNode> transform( BoxNode node, TransformerContext context, ReturnValueContext returnValueContext ) {
		Optional<MethodContextTracker> trackerOption = transpiler.getCurrentMethodContextTracker();
		if ( trackerOption.isEmpty() ) {
			throw new IllegalStateException();
		}
		MethodContextTracker	tracker				= trackerOption.get();
		List<AbstractInsnNode>	nodes				= new ArrayList<>();
		BoxTry					boxTry				= ( BoxTry ) node;

		LabelNode				tryStartLabel		= new LabelNode();
		LabelNode				tryEndLabel			= new LabelNode();
		LabelNode				finallyStartLabel	= new LabelNode();
		LabelNode				finallyEndLabel		= new LabelNode();

		AsmHelper.addDebugLabel( nodes, "BoxTryBlock" );

		nodes.add( tryStartLabel );
		// Ensure the try block always contains at least one bytecode instruction (NOP) so the
		// exception table range [tryStartLabel, tryEndLabel) is never empty. An empty range
		// causes a ClassFormatError ("Illegal exception table range") when the JVM verifies the class.
		nodes.add( new InsnNode( Opcodes.NOP ) );

		nodes.addAll( generateBodyNodesWithInlinedFinally( context, returnValueContext, boxTry.getTryBody(), boxTry.getFinallyBody(), finallyKeyword( boxTry ),
		    () -> tryEndLabel ) );

		// if we hit this instruction we have successfully executed the try body and inlined finally code
		// we can skip to the end of this construct
		AsmHelper.addDebugLabel( nodes, "BoxTryBlock goto finallyEndLabel" );
		nodes.add( new JumpInsnNode( Opcodes.GOTO, finallyEndLabel ) );

		if ( boxTry.getCatches().size() > 0 ) {

			LabelNode javaCatchBodyStart = new LabelNode();
			nodes.add( javaCatchBodyStart );

			var eVar = tracker.storeNewVariable( Opcodes.ASTORE );
			nodes.addAll( eVar.nodes() );

			nodes.add( new VarInsnNode( Opcodes.ALOAD, eVar.index() ) );
			nodes.add( new TypeInsnNode( Opcodes.INSTANCEOF, Type.getInternalName( AbortException.class ) ) );
			LabelNode abortLabel = new LabelNode();
			nodes.add( new JumpInsnNode( Opcodes.IFEQ, abortLabel ) );
			// An abort is NOT catchable by the user's catch clauses, but the finally
			// clause MUST still run (issue #650). Reuse the shared exception-handler
			// copy of the finally by jumping to it with the abort exception on the
			// stack: it runs the finally and then rethrows the abort. Reusing this
			// copy (rather than inlining yet another) keeps profiler marks correct
			// and avoids duplicating the finally body.
			nodes.add( new VarInsnNode( Opcodes.ALOAD, eVar.index() ) );
			nodes.add( new JumpInsnNode( Opcodes.GOTO, finallyStartLabel ) );
			nodes.add( abortLabel );

			for ( BoxTryCatch catchNode : boxTry.getCatches() ) {
				nodes.addAll(
				    generateCatchBodyNodes( context, returnValueContext, tracker, boxTry, catchNode, finallyStartLabel, finallyEndLabel, eVar.index() )
				);
			}

			TryCatchBlockNode catchHandler = new TryCatchBlockNode( tryStartLabel, tryEndLabel, javaCatchBodyStart,
			    null );
			tracker.addTryCatchBlock( catchHandler );

			// if we are here none of our catch handlers matched the error so we inline another finally block
			if ( boxTry.getFinallyBody().size() == 0 ) {
				nodes.add( new InsnNode( Opcodes.ACONST_NULL ) );
				if ( returnValueContext != ReturnValueContext.VALUE_OR_NULL ) {
					nodes.add( new InsnNode( Opcodes.POP ) );
				}
			}
			// Duplicated finally copy (#5, the "no catch matched" path). Snapshot the
			// profiler mark state around it so this copy's claims do NOT starve the
			// exception-handler copy (#6) emitted below — only ONE copy runs at runtime,
			// so BOTH must carry the finally marks (body AND braces).
			var unmatchedCatchSnapshot = transpiler.snapshotEmittedMarks();
			transpiler.emitBraceGroupMark( nodes, finallyKeyword( boxTry ), false );
			nodes.addAll( AsmHelper.transformBodyExpressions( transpiler, boxTry.getFinallyBody(), context, returnValueContext ) );
			transpiler.restoreEmittedMarks( unmatchedCatchSnapshot );
			nodes.add( new VarInsnNode( Opcodes.ALOAD, eVar.index() ) );
			nodes.add( new InsnNode( Opcodes.ATHROW ) );
		}

		TryCatchBlockNode catchHandler = new TryCatchBlockNode( tryStartLabel, tryEndLabel, finallyStartLabel,
		    null );
		tracker.addTryCatchBlock( catchHandler );

		AsmHelper.addDebugLabel( nodes, "BoxTry - finallyStartLabel" );
		nodes.add( finallyStartLabel );

		var errorVarStore = tracker.storeNewVariable( Opcodes.ASTORE );
		nodes.addAll( errorVarStore.nodes() );

		// Duplicated finally copy (#6, the exceptional-handler path). Snapshot the
		// profiler mark state around it so its claims do not leak into any copy
		// transformed later. The finally braces are batch-marked when the finally runs.
		var handlerSnapshot = transpiler.snapshotEmittedMarks();
		transpiler.emitBraceGroupMark( nodes, finallyKeyword( boxTry ), false );

		nodes.addAll( AsmHelper.transformBodyExpressions( transpiler, boxTry.getFinallyBody(), context, returnValueContext ) );

		transpiler.restoreEmittedMarks( handlerSnapshot );

		nodes.add( new VarInsnNode( Opcodes.ALOAD, errorVarStore.index() ) );

		nodes.add( new InsnNode( Opcodes.ATHROW ) );

		AsmHelper.addDebugLabel( nodes, "BoxTry - FinallyEndLabel" );
		nodes.add( finallyEndLabel );

		tracker.addTryCatchBlock( new TryCatchBlockNode( tryStartLabel, tryEndLabel, finallyStartLabel, null ) );

		return AsmHelper.addLineNumberLabels( nodes, node );

	}

	private List<AbstractInsnNode> generateBodyNodesWithInlinedFinally(
	    TransformerContext context,
	    ReturnValueContext returnValueContext,
	    List<BoxStatement> codeBody,
	    List<BoxStatement> finallyBody,
	    Point finallyKeyword,
	    Supplier<AbstractInsnNode> inBetween ) {
		MethodContextTracker	tracker	= transpiler.getCurrentMethodContextTracker().get();
		List<AbstractInsnNode>	nodes	= new ArrayList<AbstractInsnNode>();

		if ( codeBody.size() == 0 && finallyBody.size() == 0 ) {
			nodes.add( new InsnNode( Opcodes.ACONST_NULL ) );
			if ( returnValueContext != ReturnValueContext.VALUE_OR_NULL ) {
				nodes.add( new InsnNode( Opcodes.POP ) );
			}

			AbstractInsnNode inBetweenNode = inBetween.get();

			if ( inBetweenNode != null ) {
				nodes.add( inBetweenNode );
			}

			return nodes;
		}

		tracker.addFinallyBody( finallyBody, finallyKeyword );
		nodes.addAll( AsmHelper.transformBodyExpressions(
		    transpiler,
		    codeBody,
		    context,
		    finallyBody.size() > 0 ? ReturnValueContext.EMPTY : returnValueContext
		) );
		tracker.popFinallyBody();

		AbstractInsnNode inBetweenNode = inBetween.get();

		if ( inBetweenNode != null ) {
			nodes.add( inBetweenNode );
		}

		// The finally body is compiled twice: here (inline, after normal try-body
		// completion) and again in the exception handler (see transform() above).
		// Only one path executes at runtime, so the profiler mark for each finally
		// statement must exist in BOTH copies. Snapshot the claimed marks before
		// transforming the inline copy, then restore after, so the exception-handler
		// copy can reclaim (and thus re-emit) the same marks.
		var finallyMarksSnapshot = transpiler.snapshotEmittedMarks();

		// The finally braces are batch-marked when the finally runs (inline path).
		transpiler.emitBraceGroupMark( nodes, finallyKeyword, false );

		nodes.addAll( AsmHelper.transformBodyExpressions(
		    transpiler,
		    finallyBody,
		    context,
		    returnValueContext
		) );

		transpiler.restoreEmittedMarks( finallyMarksSnapshot );

		AsmHelper.addDebugLabel( nodes, "BoxTryBlock - END" );

		return nodes;
	}

	/**
	 * Locate the {@code finally} keyword position for a try, by scanning the source
	 * back from the first finally body statement past the preceding construct's
	 * closing brace. Mirrors Pass A's {@code findKeywordBraceStart}.
	 * <p>
	 * The result is ONLY used to emit the profiler brace-group mark, so this scan is
	 * skipped entirely when profiling is disabled — otherwise every ordinary
	 * compilation performs repeated full-source reads ({@code SourceFile.getCode()}
	 * re-reads the file) solely to locate an unused mark.
	 *
	 * @param boxTry the try node
	 *
	 * @return the finally keyword point, or null
	 */
	private Point finallyKeyword( BoxTry boxTry ) {
		// No profiler mark will be emitted — skip the source scan entirely.
		if ( !transpiler.hasProfilerId() ) {
			return null;
		}
		// Use the node's OWN source text (no whole-file read / disk I/O).
		String source = boxTry.getSourceText();
		if ( source == null ) {
			return null;
		}
		// For a NON-empty finally, scan back from the first body statement — the try
		// OWNS that body, so the keyword found is unambiguously this try's. For an
		// EMPTY finally there is no body statement to anchor on, and the AST cannot
		// distinguish an EMPTY finally (`finally {}`) from an ABSENT one (no finally
		// clause at all): both expose an empty getFinallyBody(). Scanning back from the
		// construct's end therefore risks walking into a NESTED try's finally (which
		// lives inside this try's body or a catch body) and marking a finally that
		// never executed. Bound the scan to the text AFTER this try's own body and
		// catch clauses so only this try's own finally can match.
		boolean	emptyFinally	= boxTry.getFinallyBody().isEmpty();
		Point	scanFrom		= emptyFinally ? boxTry.getEnd() : boxTry.getFinallyBody().get( 0 ).getStart();
		int		offset			= scanFrom == null ? -1 : offsetOf( boxTry, scanFrom );
		int		lowerBound		= emptyFinally ? ownClauseLowerBound( boxTry ) : 0;
		// Tag-based finally (<bx:finally> / <cffinally>): the open tag is a "<",
		// and Pass A keyed its span group at that "<". Return the "<" position.
		// ONLY template source can carry a tag form. Script source has no tags, so an
		// unrelated "<" in script (e.g. inside a string literal like "<foo>") must NOT
		// abort this scan before the script keyword lookup below.
		String	sourceType		= transpiler.getProperty( "sourceType" );
		boolean	isTemplate		= BoxSourceType.BOXTEMPLATE.name().equalsIgnoreCase( sourceType )
		    || BoxSourceType.CFTEMPLATE.name().equalsIgnoreCase( sourceType );
		if ( isTemplate ) {
			for ( int i = offset - 1; i >= lowerBound; i-- ) {
				if ( source.charAt( i ) == '<' ) {
					String rest = source.substring( i, Math.min( source.length(), i + 30 ) );
					// Tag names are case-insensitive — match the finally tag ignoring case.
					if ( rest.matches( "(?i)<(?:bx:|cf)?finally[\\s\\S]*" ) ) {
						return pointAt( boxTry, i );
					}
					// A different tag — the finally tag must be after it. A "<" that is
					// NOT followed by a tag-name letter (e.g. the less-than operator in
					// `i < 3`) is not a tag and must not stop the scan.
					if ( rest.length() > 1 && Character.isLetter( rest.charAt( 1 ) ) ) {
						return null;
					}
				}
			}
		}
		// Script finally: scan back past the preceding "}" for the keyword.
		for ( int i = offset - 1; i >= lowerBound; i-- ) {
			if ( source.charAt( i ) == '{' ) {
				int kwEnd = i;
				while ( kwEnd > 0 && source.charAt( kwEnd - 1 ) == ' ' ) {
					kwEnd--;
				}
				int kwStart = kwEnd;
				while ( kwStart > 0 && Character.isLetterOrDigit( source.charAt( kwStart - 1 ) ) ) {
					kwStart--;
				}
				if ( source.substring( kwStart, kwEnd ).equalsIgnoreCase( "finally" ) ) {
					return pointAt( boxTry, kwStart );
				}
			}
		}
		return null;
	}

	/**
	 * The offset within a try's own source text at which THIS try's own clause region
	 * begins — just after the last of its try-body / catch-body characters — used to
	 * bound the empty-finally keyword scan so it cannot reach into a nested
	 * construct that lives inside the try body or a catch body.
	 * <p>
	 * The AST does not record the presence of a finally clause, so for an empty
	 * {@code getFinallyBody()} the scan must be restricted to the tail of the node.
	 * The bound is the maximum offset of the try body's and every catch body's last
	 * statement END. A nested finally's keyword sits BEFORE its body's closing brace,
	 * which is still inside this body text, so it can never be at or past this bound.
	 *
	 * @param boxTry the try node whose own text defines the coordinate space
	 *
	 * @return the offset at which this try's own clause region begins, or 0 when no
	 *         bound can be computed (falls back to the unbounded scan)
	 */
	private int ownClauseLowerBound( BoxTry boxTry ) {
		int bound = 0;
		for ( BoxStatement stmt : boxTry.getTryBody() ) {
			bound = Math.max( bound, offsetOf( boxTry, stmt.getEnd() ) );
		}
		for ( BoxTryCatch catchNode : boxTry.getCatches() ) {
			for ( BoxStatement stmt : catchNode.getCatchBody() ) {
				bound = Math.max( bound, offsetOf( boxTry, stmt.getEnd() ) );
			}
			// A CF catch with no positioned body statements has no statement end to
			// bound on; fall back to the catch node's own end.
			if ( catchNode.getCatchBody().isEmpty() ) {
				bound = Math.max( bound, offsetOf( boxTry, catchNode.getEnd() ) );
			}
		}
		return Math.max( 0, bound );
	}

	/**
	 * Convert a Point to a character offset within the given node's OWN source text
	 * ({@link BoxNode#getSourceText()}) — no whole-file read. Returns -1 when the
	 * node has no source text/position or the point precedes the node start.
	 *
	 * @param node the node whose own source text defines the coordinate space
	 * @param p    the point to convert
	 *
	 * @return the offset within the node's text, or -1
	 */
	private int offsetOf( BoxNode node, Point p ) {
		String	source	= node.getSourceText();
		Point	origin	= node.getStart();
		if ( p == null || source == null || origin == null ) {
			return -1;
		}
		int	line	= origin.getLine();
		int	offset	= 0;
		while ( line < p.getLine() && offset < source.length() ) {
			if ( source.charAt( offset ) == '\n' ) {
				line++;
			}
			offset++;
		}
		if ( line != p.getLine() ) {
			return -1;
		}
		// On the node's own start line the column is relative to the node's start
		// column; on any later line the text's line starts at column 0.
		return line == origin.getLine() ? offset + ( p.getColumn() - origin.getColumn() ) : offset + p.getColumn();
	}

	/**
	 * Convert a character offset within the given node's OWN source text
	 * ({@link BoxNode#getSourceText()}) back to an absolute Point.
	 *
	 * @param node   the node whose own source text defines the coordinate space
	 * @param offset the offset within the node's text
	 *
	 * @return the absolute point
	 */
	private Point pointAt( BoxNode node, int offset ) {
		Point	origin	= node.getStart();
		String	source	= node.getSourceText();
		if ( origin == null || source == null ) {
			return null;
		}
		int	line	= origin.getLine();
		int	col		= origin.getColumn();
		for ( int i = 0; i < offset && i < source.length(); i++ ) {
			if ( source.charAt( i ) == '\n' ) {
				line++;
				col = 0;
			} else {
				col++;
			}
		}
		return new Point( line, col );
	}

	private List<AbstractInsnNode> generateCatchBodyNodes(
	    TransformerContext context,
	    ReturnValueContext returnValueContext,
	    MethodContextTracker tracker,
	    BoxTry boxTry,
	    BoxTryCatch boxCatch,
	    LabelNode finallyStartLabel,
	    LabelNode finallyEndLabel,
	    int eVarIndex ) {
		List<AbstractInsnNode>	nodes				= new ArrayList<AbstractInsnNode>();

		LabelNode				startHandlerLabel	= new LabelNode();
		LabelNode				endHandlerLabel		= new LabelNode();

		nodes.addAll( generateCatchIfGuard( context, boxCatch.getCatchTypes(), tracker, startHandlerLabel, endHandlerLabel, eVarIndex ) );

		nodes.add( startHandlerLabel );

		// The catch braces are batch-marked only when this catch actually runs.
		transpiler.emitBraceGroupMark( nodes, boxCatch.getPosition().getStart(), true );

		nodes.add( new TypeInsnNode( Opcodes.NEW, Type.getInternalName( CatchBoxContext.class ) ) );

		nodes.add( new InsnNode( Opcodes.DUP ) );

		nodes.addAll( tracker.loadCurrentContext() );

		nodes.addAll( transpiler.createKey( boxCatch.getException().getName() ) );

		nodes.add( new VarInsnNode( Opcodes.ALOAD, eVarIndex ) );

		nodes.add( new MethodInsnNode( Opcodes.INVOKESPECIAL,
		    Type.getInternalName( CatchBoxContext.class ),
		    "<init>",
		    Type.getMethodDescriptor(
		        Type.VOID_TYPE,
		        Type.getType( IBoxContext.class ),
		        Type.getType( Key.class ),
		        Type.getType( Throwable.class ) ),
		    false ) );
		nodes.addAll( tracker.trackNewContext() );
		// end catch context

		nodes.addAll( generateBodyNodesWithInlinedFinally( context, returnValueContext, boxCatch.getCatchBody(), boxTry.getFinallyBody(),
		    finallyKeyword( boxTry ), () -> {
			    tracker.popContext();
			    return null;
		    } ) );

		nodes.add( new JumpInsnNode( Opcodes.GOTO, finallyEndLabel ) );
		nodes.add( endHandlerLabel );

		tracker.addTryCatchBlock( new TryCatchBlockNode( startHandlerLabel, endHandlerLabel, finallyStartLabel, null ) );

		return nodes;
	}

	private List<AbstractInsnNode> generateCatchIfGuard( TransformerContext context, List<BoxExpression> catchTypes, MethodContextTracker tracker,
	    LabelNode startHandlerLabel, LabelNode endHandlerLabel, int eVarIndex ) {
		List<AbstractInsnNode> nodes = new ArrayList<AbstractInsnNode>();

		if ( catchTypes.size() == 0 ) {
			return new ArrayList<AbstractInsnNode>();
		}

		for ( int i = 0; i < catchTypes.size() - 1; i++ ) {

			nodes.add( new VarInsnNode( Opcodes.ALOAD, eVarIndex ) );

			nodes.addAll( tracker.loadCurrentContext() );

			nodes.add( new InsnNode( Opcodes.SWAP ) );

			nodes.addAll( transpiler.transform( catchTypes.get( i ), context, ReturnValueContext.VALUE ) );

			nodes.add( new MethodInsnNode(
			    Opcodes.INVOKESTATIC,
			    Type.getInternalName( ExceptionUtil.class ),
			    "exceptionIsOfType",
			    Type.getMethodDescriptor( Type.getType( Boolean.class ), Type.getType( IBoxContext.class ), Type.getType( Throwable.class ),
			        Type.getType( String.class ) ),
			    false
			) );

			nodes.add( new MethodInsnNode(
			    Opcodes.INVOKEVIRTUAL,
			    Type.getInternalName( Boolean.class ),
			    "booleanValue",
			    Type.getMethodDescriptor( Type.BOOLEAN_TYPE ),
			    false
			) );

			nodes.add( new JumpInsnNode( Opcodes.IFNE, startHandlerLabel ) );
		}

		nodes.add( new VarInsnNode( Opcodes.ALOAD, eVarIndex ) );

		nodes.addAll( tracker.loadCurrentContext() );

		nodes.add( new InsnNode( Opcodes.SWAP ) );

		nodes.addAll( transpiler.transform( catchTypes.getLast(), context, ReturnValueContext.VALUE ) );

		nodes.add( new MethodInsnNode(
		    Opcodes.INVOKESTATIC,
		    Type.getInternalName( ExceptionUtil.class ),
		    "exceptionIsOfType",
		    Type.getMethodDescriptor( Type.getType( Boolean.class ), Type.getType( IBoxContext.class ), Type.getType( Throwable.class ),
		        Type.getType( String.class ) ),
		    false
		) );

		nodes.add( new MethodInsnNode(
		    Opcodes.INVOKEVIRTUAL,
		    Type.getInternalName( Boolean.class ),
		    "booleanValue",
		    Type.getMethodDescriptor( Type.BOOLEAN_TYPE ),
		    false
		) );

		nodes.add( new JumpInsnNode( Opcodes.IFEQ, endHandlerLabel ) );

		return nodes;
	}

}