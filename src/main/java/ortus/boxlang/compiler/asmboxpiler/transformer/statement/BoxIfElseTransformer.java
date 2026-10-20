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

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;

import ortus.boxlang.compiler.asmboxpiler.AsmHelper;
import ortus.boxlang.compiler.asmboxpiler.Transpiler;
import ortus.boxlang.compiler.asmboxpiler.transformer.AbstractTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.ReturnValueContext;
import ortus.boxlang.compiler.asmboxpiler.transformer.TransformerContext;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.BoxStatement;
import ortus.boxlang.compiler.ast.Point;
import ortus.boxlang.compiler.ast.statement.BoxIfElse;
import ortus.boxlang.runtime.components.Component;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;

public class BoxIfElseTransformer extends AbstractTransformer {

	private static final int	OUTLINE_SOURCE_THRESHOLD		= 12000;
	private static final int	OUTLINE_DESCENDANT_THRESHOLD	= 250;

	public BoxIfElseTransformer( Transpiler transpiler ) {
		super( transpiler );
	}

	@Override
	public List<AbstractInsnNode> transform( BoxNode node, TransformerContext context, ReturnValueContext returnContext ) {
		BoxIfElse				ifElse	= ( BoxIfElse ) node;

		List<AbstractInsnNode>	nodes	= new ArrayList<>();
		AsmHelper.addDebugLabel( nodes, "BoxIf" );
		nodes.addAll( transpiler.transform( ifElse.getCondition(), TransformerContext.NONE, ReturnValueContext.VALUE ) );
		if ( !ifElse.getCondition().returnsBoolean() ) {
			nodes.add( new MethodInsnNode( Opcodes.INVOKESTATIC,
			    Type.getInternalName( BooleanCaster.class ),
			    "cast",
			    Type.getMethodDescriptor( Type.getType( Boolean.class ), Type.getType( Object.class ) ),
			    false ) );
		}
		nodes.add( new MethodInsnNode( Opcodes.INVOKEVIRTUAL,
		    Type.getInternalName( Boolean.class ),
		    "booleanValue",
		    Type.getMethodDescriptor( Type.BOOLEAN_TYPE ),
		    false ) );
		LabelNode ifLabel = new LabelNode();
		AsmHelper.addDebugLabel( nodes, "BoxIfElse - goto iflabel" );
		nodes.add( new JumpInsnNode( Opcodes.IFEQ, ifLabel ) );
		nodes.addAll( transformBranch( ifElse.getThenBody(), true, returnContext ) );

		LabelNode elseLabel = new LabelNode();
		AsmHelper.addDebugLabel( nodes, "BoxIfElse - goto elselabel" );
		nodes.add( new JumpInsnNode( Opcodes.GOTO, elseLabel ) );

		AsmHelper.addDebugLabel( nodes, "BoxIfElse - ifLabel" );
		nodes.add( ifLabel );

		if ( ifElse.getElseBody() != null ) {
			// The "else" keyword is marked when the else branch runs — emit a manual
			// mark for its span at the else branch entry (only when profiling).
			emitElseKeywordMark( nodes, ifElse );
			nodes.addAll( transformBranch( ifElse.getElseBody(), false, returnContext ) );
		} else if ( returnContext == ReturnValueContext.VALUE_OR_NULL ) {
			nodes.add( new InsnNode( Opcodes.ACONST_NULL ) );
		}

		AsmHelper.addDebugLabel( nodes, "BoxIfElse - elseLabel" );
		nodes.add( elseLabel );

		AsmHelper.addDebugLabel( nodes, "BoxIfElse - end" );

		return AsmHelper.addLineNumberLabels( nodes, node );

	}

	private List<AbstractInsnNode> transformBranch(
	    BoxStatement branchBody,
	    boolean isThenBranch,
	    ReturnValueContext returnContext ) {
		if ( branchBody == null ) {
			return List.of();
		}

		if ( shouldOutlineBranch( branchBody, returnContext ) ) {
			return generateOutlinedComponentBranchCall( branchBody, isThenBranch );
		}

		return transpiler.transform( branchBody, TransformerContext.NONE, returnContext );
	}

	private boolean shouldOutlineBranch( BoxStatement branchBody, ReturnValueContext returnContext ) {
		if ( returnContext != ReturnValueContext.EMPTY || !transpiler.isInsideComponent() ) {
			return false;
		}

		return getNodeSourceLength( branchBody ) >= OUTLINE_SOURCE_THRESHOLD
		    || branchBody.getDescendants().size() >= OUTLINE_DESCENDANT_THRESHOLD;
	}

	private int getNodeSourceLength( BoxStatement branchBody ) {
		return branchBody.getSourceText() != null ? branchBody.getSourceText().length() : 0;
	}

	private List<AbstractInsnNode> generateOutlinedComponentBranchCall( BoxStatement branchBody, boolean isThenBranch ) {
		ClassNode	owningClass		= transpiler.getOwningClass();
		String		methodName		= "componentIfBranch_" + ( isThenBranch ? "then" : "else" ) + "_" + transpiler.incrementAndGetLambdaCounter();
		Type		declaringType	= Type.getObjectType( owningClass.name );

		AsmHelper.methodWithContextAndClassLocator(
		    owningClass,
		    methodName,
		    Type.getType( IBoxContext.class ),
		    Type.getType( Component.BodyResult.class ),
		    true,
		    transpiler,
		    false,
		    () -> createOutlinedComponentBranchBody( branchBody )
		);

		List<AbstractInsnNode>	nodes			= new ArrayList<>();
		LabelNode				continueLabel	= new LabelNode();

		nodes.addAll( transpiler.getCurrentMethodContextTracker().orElseThrow().loadCurrentContext() );
		nodes.add( new MethodInsnNode(
		    Opcodes.INVOKESTATIC,
		    declaringType.getInternalName(),
		    methodName,
		    Type.getMethodDescriptor( Type.getType( Component.BodyResult.class ), Type.getType( IBoxContext.class ) ),
		    false
		) );
		nodes.add( new InsnNode( Opcodes.DUP ) );
		nodes.add( new MethodInsnNode(
		    Opcodes.INVOKEVIRTUAL,
		    Type.getInternalName( Component.BodyResult.class ),
		    "isEarlyExit",
		    Type.getMethodDescriptor( Type.BOOLEAN_TYPE ),
		    false
		) );
		nodes.add( new JumpInsnNode( Opcodes.IFEQ, continueLabel ) );
		nodes.add( new InsnNode( Opcodes.ARETURN ) );
		nodes.add( continueLabel );
		nodes.add( new InsnNode( Opcodes.POP ) );

		return nodes;
	}

	private List<AbstractInsnNode> createOutlinedComponentBranchBody( BoxStatement branchBody ) {
		List<AbstractInsnNode> nodes = new ArrayList<>( transpiler.transform( branchBody, TransformerContext.NONE, ReturnValueContext.EMPTY ) );

		nodes.add( new FieldInsnNode(
		    Opcodes.GETSTATIC,
		    Type.getInternalName( Component.class ),
		    "DEFAULT_RETURN",
		    Type.getDescriptor( Component.BodyResult.class )
		) );

		return nodes;
	}

	/**
	 * Emit a {@code mark(id, elseSpanId)} for the {@code else} keyword when the
	 * else branch runs. Pass A registered the keyword as its own span; this fires
	 * only at the else branch entry — never when the if branch is taken. No-op when
	 * profiling is disabled (no mark bytecode may be injected).
	 *
	 * @param nodes  the instruction list to append to
	 * @param ifElse the if statement (for the else keyword position)
	 */
	private void emitElseKeywordMark( List<AbstractInsnNode> nodes, BoxIfElse ifElse ) {
		// No-op when profiling is disabled — never emit mark instructions otherwise.
		if ( !transpiler.hasProfilerId() ) {
			return;
		}
		Point kw = findElseKeyword( ifElse );
		if ( kw == null ) {
			return;
		}
		// In template markup the branch span is the FULL <bx:else> tag, registered
		// at its "<" (before the "else" keyword). Locate the actual "<" so the
		// else-tag span gets marked when the else branch runs; otherwise key the
		// keyword position (script else).
		long	packed;
		Point	tagOpen	= findElseTagOpen( ifElse, kw );
		if ( tagOpen != null ) {
			packed = ( ( long ) tagOpen.getLine() << 32 ) | ( tagOpen.getColumn() & 0xFFFFFFFFL );
		} else {
			packed = ( ( long ) kw.getLine() << 32 ) | ( kw.getColumn() & 0xFFFFFFFFL );
		}
		int spanId = transpiler.getSpanId( packed );
		if ( spanId >= 0 && transpiler.claimSpanMark( spanId ) ) {
			nodes.addAll( transpiler.emitMark( spanId ) );
		}
	}

	/**
	 * If the source immediately before the {@code else} keyword is a template else
	 * tag ({@code <bx:else>} / {@code <cfelse>}), return the tag's {@code <}
	 * position — the key Pass A registered the full tag span at. Otherwise null
	 * (script else-if, keyword-keyed span).
	 *
	 * @param ifElse the if statement (for source access)
	 * @param kw     the else keyword position
	 *
	 * @return the else tag's {@code <} position, or null
	 */
	private Point findElseTagOpen( BoxIfElse ifElse, Point kw ) {
		// Use the node's OWN source text (no whole-file read / disk I/O).
		String source = ifElse.getSourceText();
		if ( source == null ) {
			return null;
		}
		int offset = offsetOf( ifElse, kw );
		if ( offset <= 0 ) {
			return null;
		}
		// Defensive bound: clamp the start so a column-relative offset that lands
		// past the captured text cannot index out of range (nested template elseif
		// whose else branch extends beyond its then body).
		int i = Math.min( offset - 1, source.length() - 1 );
		// Walk back from the keyword to the tag's "<".
		while ( i >= 0 && source.charAt( i ) != '<' ) {
			i--;
		}
		if ( i < 0 ) {
			return null;
		}
		// The text between that "<" and the keyword must be EXACTLY a tag prefix
		// ("<", "<bx:" or "<cf"). A "<" further away — e.g. the one starting a
		// string literal in the PRECEDING branch, as in
		// `if( false ) { x = "<cfelse>"; } else { y = 1; }` — has intervening
		// text, so it is NOT this keyword's tag and must be rejected. Without this
		// the backward scan mistook such a string for the else tag and marked a
		// position where no span was registered, leaving the executed else-keyword
		// span unmarked.
		String prefix = source.substring( i, Math.min( offset, source.length() ) );
		// Tag names are case-insensitive — match the else tag prefix ignoring case.
		if ( !prefix.matches( "(?i)<(?:bx:|cf)?" ) ) {
			return null;
		}
		return pointAt( ifElse, i );
	}

	/**
	 * Locate the {@code else} keyword position by scanning the source back from the
	 * else body's start. In CF tag form the keyword is embedded in the single source
	 * word {@code cfelse}; in that case the position of its {@code else} suffix is
	 * returned so the enclosing {@code <cfelse>} tag can still be resolved.
	 *
	 * @param ifElse the if statement
	 *
	 * @return the else keyword point, or null
	 */
	private Point findElseKeyword( BoxIfElse ifElse ) {
		// Use the node's OWN source text (no whole-file read / disk I/O).
		String source = ifElse.getSourceText();
		if ( source == null || ifElse.getElseBody() == null || ifElse.getElseBody().getStart() == null ) {
			return null;
		}
		Point	after	= ifElse.getElseBody().getStart();
		int		offset	= offsetOf( ifElse, after );
		if ( offset < 0 ) {
			return null;
		}
		// Defensive bound: offsetOf derives a column-relative offset that can land
		// PAST the end of the node's own source text when the else body sits beyond
		// the text the parser captured for this node (a nested template elseif whose
		// else branch extends past its then body). Clamp so the backward scan below
		// can never index out of range.
		int start = Math.min( offset - 1, source.length() - 1 );
		for ( int i = start; i >= 0; i-- ) {
			if ( Character.isLetterOrDigit( source.charAt( i ) ) ) {
				int	wordEnd		= i + 1;
				int	wordStart	= i;
				while ( wordStart > 0 && Character.isLetterOrDigit( source.charAt( wordStart - 1 ) ) ) {
					wordStart--;
				}
				String word = source.substring( wordStart, wordEnd );
				if ( word.equalsIgnoreCase( "else" ) ) {
					return pointAt( ifElse, wordStart );
				}
				// CF tag form: the source word is "cfelse" (a single word — the "cf"
				// prefix is not separated by punctuation). Return the position of its
				// "else" suffix so findElseTagOpen() can walk back to the tag's "<"
				// and mark the FULL registered <cfelse> span.
				if ( word.equalsIgnoreCase( "cfelse" ) ) {
					return pointAt( ifElse, wordStart + 2 );
				}
				i = wordStart;
			}
		}
		return null;
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

}
