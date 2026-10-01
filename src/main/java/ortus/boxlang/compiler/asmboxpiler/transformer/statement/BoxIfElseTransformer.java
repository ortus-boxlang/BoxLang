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
		// An ELSEIF tag (<bx:elseif ...> / <cfelseif ...>) is evaluated when the
		// chain reaches it — mark its tag span at that point (the tag opens at the
		// node start, matching Pass A registration).
		emitElseIfTagMark( nodes, ifElse );
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
	 * Emit a {@code mark(fileId, spanId)} for an ELSEIF tag when its condition is
	 * evaluated. Pass A registered the full {@code <bx:elseif ...>} tag as a span
	 * starting at its {@code <} (the parser fixes tag positions). Only when the
	 * chain reaches this elseif does the tag "run". No-op when profiling is
	 * disabled.
	 *
	 * @param nodes  the instruction list to append to
	 * @param ifElse the if statement (an elseif if its source starts with the
	 *               else-if tag prefix)
	 */
	private void emitElseIfTagMark( List<AbstractInsnNode> nodes, BoxIfElse ifElse ) {
		// No-op when profiling is disabled — never emit mark instructions otherwise.
		if ( !transpiler.hasProfilerId() ) {
			return;
		}
		if ( ifElse.getStart() == null ) {
			return;
		}
		String src = ifElse.getSourceText() == null ? null : ifElse.getSourceText().trim();
		if ( src == null || ! ( src.startsWith( "bx:elseif" ) || src.startsWith( "cfelseif" ) || src.startsWith( "elseif" ) ) ) {
			return;
		}
		// The tag's "<" IS the node start.
		long	packed	= ( ( long ) ifElse.getStart().getLine() << 32 ) | ( ifElse.getStart().getColumn() & 0xFFFFFFFFL );
		int		spanId	= transpiler.getSpanId( packed );
		if ( spanId >= 0 && transpiler.claimSpanMark( spanId ) ) {
			nodes.addAll( transpiler.emitMark( spanId ) );
		}
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
		if ( ifElse.getPosition() == null || ifElse.getPosition().getSource() == null ) {
			return null;
		}
		String	source	= ifElse.getPosition().getSource().getCode();
		int		offset	= offsetOf( source, kw );
		if ( offset <= 0 ) {
			return null;
		}
		// Walk back from the keyword to the tag's "<".
		int i = offset - 1;
		while ( i >= 0 && source.charAt( i ) != '<' ) {
			i--;
		}
		if ( i < 0 ) {
			return null;
		}
		String tag = source.substring( i, Math.min( source.length(), i + 20 ) );
		if ( !tag.matches( "<(?:bx:|cf)?else(?!if)[\\s\\S]*" ) ) {
			return null;
		}
		return pointAt( source, i );
	}

	/**
	 * Locate the {@code else} keyword position by scanning the source back from the
	 * else body's start.
	 *
	 * @param ifElse the if statement
	 *
	 * @return the else keyword point, or null
	 */
	private Point findElseKeyword( BoxIfElse ifElse ) {
		if ( ifElse.getElseBody() == null || ifElse.getElseBody().getStart() == null
		    || ifElse.getPosition() == null || ifElse.getPosition().getSource() == null ) {
			return null;
		}
		String	source	= ifElse.getPosition().getSource().getCode();
		Point	after	= ifElse.getElseBody().getStart();
		int		offset	= offsetOf( source, after );
		if ( offset < 0 ) {
			return null;
		}
		for ( int i = offset - 1; i >= 0; i-- ) {
			if ( Character.isLetterOrDigit( source.charAt( i ) ) ) {
				int	wordEnd		= i + 1;
				int	wordStart	= i;
				while ( wordStart > 0 && Character.isLetterOrDigit( source.charAt( wordStart - 1 ) ) ) {
					wordStart--;
				}
				if ( source.substring( wordStart, wordEnd ).equalsIgnoreCase( "else" ) ) {
					return pointAt( source, wordStart );
				}
				i = wordStart;
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

}
