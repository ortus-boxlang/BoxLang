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
			nodes.add( new VarInsnNode( Opcodes.ALOAD, eVar.index() ) );
			nodes.add( new InsnNode( Opcodes.ATHROW ) );
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
			nodes.addAll( AsmHelper.transformBodyExpressions( transpiler, boxTry.getFinallyBody(), context, returnValueContext ) );
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

		// The finally braces are batch-marked when the finally runs.
		emitBraceGroupMark( nodes, finallyKeyword( boxTry ), false );

		nodes.addAll( AsmHelper.transformBodyExpressions( transpiler, boxTry.getFinallyBody(), context, returnValueContext ) );

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

		tracker.addFinallyBody( finallyBody );
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
		emitBraceGroupMark( nodes, finallyKeyword, false );

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
	 * Emit a {@code mark(fileId, ...braceGroup)} for a try/catch/finally construct's
	 * braces. Pass A registered the construct's header + closing brace as a span
	 * GROUP keyed by the construct keyword position. The group is consumed for a
	 * catch (each catch handler runs at most once); for a finally it is PEEKED
	 * (non-consuming) because the finally body — and thus its brace mark — is
	 * compiled into BOTH the inline and exception-handler bytecode copies, and only
	 * one runs at runtime.
	 *
	 * @param nodes      the instruction list to append to
	 * @param keywordPos the construct keyword start position (group key)
	 * @param consume    whether to consume the group (catch) or peek (finally)
	 */
	private void emitBraceGroupMark( List<AbstractInsnNode> nodes, Point keywordPos, boolean consume ) {
		// No-op when profiling is disabled — never emit mark instructions otherwise.
		if ( !transpiler.isProfilingEnabled() || transpiler.getFileId() < 0 ) {
			return;
		}
		if ( keywordPos == null ) {
			return;
		}
		long	packed	= ( ( long ) keywordPos.getLine() << 32 ) | ( keywordPos.getColumn() & 0xFFFFFFFFL );
		int		spanId	= transpiler.getSpanId( packed );
		int[]	group	= spanId < 0 ? null : ( consume ? transpiler.takeSpanGroup( spanId ) : transpiler.peekSpanGroup( spanId ) );
		if ( group != null ) {
			// Only emit if no member has been claimed yet (dedup across copies).
			boolean anyClaimed = false;
			for ( int member : group ) {
				if ( transpiler.claimSpanMark( member ) ) {
					anyClaimed = true;
				}
			}
			if ( anyClaimed ) {
				nodes.addAll( AsmHelper.invokeStaticMarkVarargs( transpiler.getFileId(), group ) );
			}
		}
	}

	/**
	 * Locate the {@code finally} keyword position for a try, by scanning the source
	 * back from the first finally body statement past the preceding construct's
	 * closing brace. Mirrors Pass A's {@code findKeywordBraceStart}.
	 *
	 * @param boxTry the try node
	 *
	 * @return the finally keyword point, or null
	 */
	private Point finallyKeyword( BoxTry boxTry ) {
		if ( boxTry.getFinallyBody().isEmpty() || boxTry.getPosition() == null || boxTry.getPosition().getSource() == null ) {
			return null;
		}
		Point	first	= boxTry.getFinallyBody().get( 0 ).getStart();
		String	source	= boxTry.getPosition().getSource().getCode();
		int		offset	= offsetOf( source, first );
		for ( int i = offset - 1; i >= 0; i-- ) {
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
					return pointAt( source, kwStart );
				}
			}
		}
		return null;
	}

	/** Convert a Point to a character offset in the source. */
	private int offsetOf( String source, Point p ) {
		int	line	= 1;
		int	offset	= 0;
		while ( line < p.getLine() && offset < source.length() ) {
			if ( source.charAt( offset ) == '\n' ) {
				line++;
			}
			offset++;
		}
		return offset + p.getColumn();
	}

	/** Convert a character offset back to a Point. */
	private Point pointAt( String source, int offset ) {
		int	line	= 1;
		int	col		= 0;
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
		emitBraceGroupMark( nodes, boxCatch.getPosition().getStart(), true );

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