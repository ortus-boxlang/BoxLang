/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http: //www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ortus.boxlang.compiler.asmboxpiler;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import ortus.boxlang.compiler.ast.BoxClass;
import ortus.boxlang.compiler.ast.BoxExpression;
import ortus.boxlang.compiler.ast.BoxInterface;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.BoxStatement;
import ortus.boxlang.compiler.ast.BoxStaticInitializer;
import ortus.boxlang.compiler.ast.BoxTemplate;
import ortus.boxlang.compiler.ast.Point;
import ortus.boxlang.compiler.ast.expression.BoxArgument;
import ortus.boxlang.compiler.ast.expression.BoxArrayLiteral;
import ortus.boxlang.compiler.ast.expression.BoxAssignment;
import ortus.boxlang.compiler.ast.expression.BoxBinaryOperation;
import ortus.boxlang.compiler.ast.expression.BoxBinaryOperator;
import ortus.boxlang.compiler.ast.expression.BoxClosure;
import ortus.boxlang.compiler.ast.expression.BoxComparisonOperation;
import ortus.boxlang.compiler.ast.expression.BoxExpressionInvocation;
import ortus.boxlang.compiler.ast.expression.BoxFunctionInvocation;
import ortus.boxlang.compiler.ast.expression.BoxLambda;
import ortus.boxlang.compiler.ast.expression.BoxMethodInvocation;
import ortus.boxlang.compiler.ast.expression.BoxParenthesis;
import ortus.boxlang.compiler.ast.expression.BoxStaticMethodInvocation;
import ortus.boxlang.compiler.ast.expression.BoxStringConcat;
import ortus.boxlang.compiler.ast.expression.BoxStringLiteral;
import ortus.boxlang.compiler.ast.expression.BoxStructLiteral;
import ortus.boxlang.compiler.ast.expression.BoxTernaryOperation;
import ortus.boxlang.compiler.ast.statement.BoxAnnotation;
import ortus.boxlang.compiler.ast.statement.BoxArgumentDeclaration;
import ortus.boxlang.compiler.ast.statement.BoxBufferOutput;
import ortus.boxlang.compiler.ast.statement.BoxDo;
import ortus.boxlang.compiler.ast.statement.BoxForIn;
import ortus.boxlang.compiler.ast.statement.BoxForIndex;
import ortus.boxlang.compiler.ast.statement.BoxFunctionDeclaration;
import ortus.boxlang.compiler.ast.statement.BoxIfElse;
import ortus.boxlang.compiler.ast.statement.BoxImport;
import ortus.boxlang.compiler.ast.statement.BoxLocalClass;
import ortus.boxlang.compiler.ast.statement.BoxParam;
import ortus.boxlang.compiler.ast.statement.BoxProperty;
import ortus.boxlang.compiler.ast.statement.BoxScriptIsland;
import ortus.boxlang.compiler.ast.statement.BoxStatementBlock;
import ortus.boxlang.compiler.ast.statement.BoxSwitch;
import ortus.boxlang.compiler.ast.statement.BoxSwitchCase;
import ortus.boxlang.compiler.ast.statement.BoxTry;
import ortus.boxlang.compiler.ast.statement.BoxTryCatch;
import ortus.boxlang.compiler.ast.statement.BoxWhile;
import ortus.boxlang.compiler.ast.statement.component.BoxComponent;
import ortus.boxlang.compiler.ast.statement.component.BoxTemplateIsland;
import ortus.boxlang.compiler.ast.visitor.VoidBoxVisitor;
import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.services.Blueprint;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * FIRST PASS (Pass A) span-discovery visitor.
 * <p>
 * Walks the AST once and assigns a sequential id to every executable span. The
 * rule is deliberately simple:
 * <ul>
 * <li>A statement opens a running span at its start position.</li>
 * <li>Wrapping nodes (assignment, parens, ...) thread the running span through
 * their children — punctuation like {@code =} or {@code (} is simply included
 * inside the running span.</li>
 * <li>{@link BoxTernaryOperation} always evaluates its condition, then picks
 * exactly one branch. So the condition continues the running span, and each
 * branch (whenTrue / whenFalse) BREAKS out into its own span, because it may
 * not run.</li>
 * </ul>
 * That single rule composes recursively — nested ternaries, branching conditions,
 * etc. — with no descendant scanning and no per-construct predicates. The generic
 * {@code visitChildren} hook gives every statement its own span boundary; only
 * branching expressions need custom logic.
 */
public class SpanCollectorVisitor extends VoidBoxVisitor {

	private final Transpiler				transpiler;
	private final List<Blueprint.SpanDef>	spanDefs				= new ArrayList<>();

	/**
	 * Start positions of close tags already registered via {@link #registerTagClose}
	 * — each {@code </bx:name>} / {@code </cf...>} may only be registered ONCE (a
	 * nested construct inside another can otherwise claim the same close tag, e.g.
	 * multiple nested if-components all seeing the same {@code </cfif>}).
	 */
	private final java.util.Set<Long>		registeredTagCloses		= new java.util.HashSet<>();
	private final java.util.Set<Long>		registeredSpanStarts	= new java.util.HashSet<>();

	/**
	 * The start of the currently-open running span, or {@code null} if none.
	 */
	private Point							runningStart;

	/**
	 * Source-type stack, mirroring {@code PrettyPrintBoxVisitor}: push a template
	 * marker while descending into tag markup ({@link BoxTemplate},
	 * {@link BoxTemplateIsland}) and a script marker inside {@link BoxScriptIsland}
	 * / {@link BoxClass}. Determines whether a construct is tag-based
	 * ({@code <bx:while>} vs script {@code while}) without inspecting source text.
	 */
	private final Stack<BoxSourceType>		currentSourceType		= new Stack<>();

	/**
	 * Construct the collector.
	 *
	 * @param transpiler the active transpiler (span registry lives here)
	 */
	public SpanCollectorVisitor( Transpiler transpiler ) {
		this.transpiler = transpiler;
		this.currentSourceType.push( BoxSourceType.BOXSCRIPT );
	}

	/**
	 * Whether we are currently inside template tag markup (as opposed to script).
	 *
	 * @return true when the top of the source-type stack is a template type
	 */
	private boolean isTemplate() {
		return this.currentSourceType.peek() == BoxSourceType.BOXTEMPLATE
		    || this.currentSourceType.peek() == BoxSourceType.CFTEMPLATE;
	}

	/**
	 * The collected executable spans, in source order. Call after accepting the root.
	 *
	 * @return the accumulated SpanDefs
	 */
	public List<Blueprint.SpanDef> spanDefs() {
		return spanDefs;
	}

	/**
	 * The generic statement hook: whenever a container visits its children, every
	 * statement child opens a span at its start and closes it at its end. This
	 * needs no per-container or per-statement-type override — the base visitor
	 * already threads all children; we only add the open/close boundary around
	 * each statement.
	 *
	 * @param node the node whose children are visited
	 */
	@Override
	protected void visitChildren( BoxNode node ) {
		for ( BoxNode child : node.getChildren() ) {
			// A blank/whitespace-only static template-text buffer output (e.g. the
			// raw newlines between <bx:> tags) is pure source-formatting noise, not
			// real execution, and cannot throw. Skip it so it never becomes a span.
			// Non-empty literal output text AND interpolated buffers are tracked
			// normally — they are real (possibly repeated) rendered output, and
			// interpolated buffers run real expressions.
			if ( child instanceof BoxBufferOutput bufOut
			    && bufOut.getExpression() instanceof BoxStringLiteral lit
			    && lit.getValue().isBlank() ) {
				continue;
			}
			if ( child instanceof BoxStatement stmt ) {
				this.runningStart = stmt.getStart();
				child.accept( this );
				// The parser reports a multi-line buffer output's END on the same
				// line as its START (it does not account for embedded newlines in a
				// text token). That collapses the span so only the first `` `\r` `` is
				// covered, leaving the rendered text ("foo"/"bar") untracked. Recompute
				// the true end from the source when it genuinely spans newlines.
				closeRunningSpan( bufferOutputEnd( stmt ) );
			} else {
				child.accept( this );
			}
		}
	}

	/**
	 * The true end point of a statement for span registration. Ordinary statements
	 * use their computed end; a {@link BoxBufferOutput} whose source text spans
	 * multiple lines gets a corrected end so its rendered text is fully covered.
	 *
	 * @param stmt the statement
	 *
	 * @return the end point to close the span at
	 */
	private Point bufferOutputEnd( BoxStatement stmt ) {
		if ( stmt instanceof BoxBufferOutput buf && buf.getExpression() instanceof BoxStringLiteral lit ) {
			String	text	= lit.getValue();
			Point	start	= stmt.getStart();
			// Only expand NON-BLANK rendered text. Whitespace-only buffers (the raw
			// newlines between tags) are pure formatting — leave them collapsed (and
			// they are skipped entirely by visitChildren's blank check).
			if ( start != null && text != null && !text.isBlank() && text.contains( "\n" ) ) {
				String[]	lines	= text.split( "\n", -1 );
				int			endLine	= start.getLine() + ( lines.length - 1 );
				String		last	= lines[ lines.length - 1 ];
				if ( last.endsWith( "\r" ) ) {
					last = last.substring( 0, last.length() - 1 );
				}
				int		endCol	= text.endsWith( "\n" ) ? 0 : last.length();
				Point	p		= new Point( endLine, endCol );
				if ( p.getLine() != start.getLine() || p.getColumn() != start.getColumn() ) {
					return p;
				}
			}
		}
		return stmt.getEnd();
	}

	/**
	 * Template markup: push the template marker while descending (so tag
	 * constructs register tag spans), then pop. The root node type determines the
	 * marker — {@link BoxTemplate} for Box/CF tags.
	 *
	 * @param node the template
	 */
	@Override
	public void visit( BoxTemplate node ) {
		this.currentSourceType.push( node.getBoxSourceType() );
		visitChildren( node );
		this.currentSourceType.pop();
	}

	/**
	 * A {@code <bx:script>} island inside a template: descend in script mode. The
	 * {@code <bx:script>} OPEN tag is its own span, and the {@code </bx:script>}
	 * CLOSE tag is grouped with it so both are marked executed when the island
	 * runs (they are atomic with the statements). The trailing semicolon after the
	 * last statement is NOT executable — runningStart is reset so the enclosing
	 * statement cannot mint a phantom span over it and the close tag.
	 *
	 * @param node the script island
	 */
	@Override
	public void visit( BoxScriptIsland node ) {
		this.currentSourceType.push( BoxSourceType.BOXSCRIPT );
		int		openId	= -1;
		Point	openEnd	= node.getStart() == null ? null : findTagCloseAfter( node, node.getStart() );
		if ( openEnd != null ) {
			openId = addSpan( node.getStart(), new Point( openEnd.getLine(), openEnd.getColumn() + 1 ) );
		}
		for ( BoxStatement stmt : node.getStatements() ) {
			if ( stmt instanceof BoxStaticInitializer init ) {
				// A static initializer inside a <cfscript> island: use the SAME
				// header + brace-group handling as the class body branch so the
				// "static {" header (with its opening brace) and the closing "}"
				// are batch-marked — the generic statement wrap would otherwise
				// mint a phantom span over the trailing ";" + "}".
				Point	initOpen	= findOpenBraceBefore( node, init.getBody().isEmpty() ? init.getEnd() : init.getBody().get( 0 ).getStart() );
				Point	initClose	= init.getEnd() == null ? null : new Point( init.getEnd().getLine(), init.getEnd().getColumn() - 1 );
				int		initHeader	= -1;
				this.runningStart = init.getStart();
				if ( initOpen != null ) {
					initHeader = closeRunningSpan( new Point( initOpen.getLine(), initOpen.getColumn() + 1 ) );
				}
				for ( BoxStatement s : init.getBody() ) {
					this.runningStart = s.getStart();
					s.accept( this );
					closeRunningSpan( bufferOutputEnd( s ) );
				}
				if ( initHeader >= 0 && initClose != null ) {
					registerBraceGroup( initHeader, initClose );
				}
				this.runningStart = null;
				continue;
			}
			this.runningStart = stmt.getStart();
			stmt.accept( this );
			closeRunningSpan( bufferOutputEnd( stmt ) );
		}
		// The close tag </bx:script> / </cfscript> is grouped with the open tag so
		// both are marked when the island's entry fires.
		if ( openId >= 0 && !node.getStatements().isEmpty() ) {
			Point	afterLast	= node.getStatements().get( node.getStatements().size() - 1 ).getEnd();
			Point	closeOpen	= findTagCloseOpen( node, afterLast, "script" );
			if ( closeOpen != null ) {
				Point closeEnd = findTagCloseAfter( node, closeOpen );
				if ( closeEnd != null ) {
					int closeId = addSpan( closeOpen, new Point( closeEnd.getLine(), closeEnd.getColumn() + 1 ) );
					if ( closeId >= 0 ) {
						transpiler.registerSpanGroup( new int[] { openId, closeId } );
					}
				}
			}
		}
		this.runningStart = null;
		this.currentSourceType.pop();
	}

	/**
	 * A template island (nested {@code ```...```} template content inside a script
	 * context): descend in template mode. The OPENING {@code ```} and CLOSING
	 * {@code ```} delimiters are part of the island markup and are registered as a
	 * span GROUP so both are marked executed when the island runs. The parser's
	 * island node spans only the template content BETWEEN the delimiters, so the
	 * backticks themselves are located in source and added here.
	 *
	 * @param node the template island
	 */
	@Override
	public void visit( BoxTemplateIsland node ) {
		this.currentSourceType.push( BoxSourceType.BOXTEMPLATE );
		// Opening delimiter ``` (immediately preceding the island's content start).
		int		openId		= -1;
		Point	delimOpen	= findDelimiterBefore( node, node.getStart() );
		if ( delimOpen != null ) {
			openId = addSpan( delimOpen, new Point( delimOpen.getLine(), delimOpen.getColumn() + 3 ) );
		}
		// Capture the FIRST content span created while descending, so the delimiters
		// can be grouped with it (its mark fires when the island runs).
		final int[] firstContentId = { -1 };
		shellSpanConsumers.add( ( spanId, start ) -> {
			if ( firstContentId[ 0 ] < 0 ) {
				firstContentId[ 0 ] = spanId;
			}
		} );
		visitChildren( node );
		shellSpanConsumers.remove( shellSpanConsumers.size() - 1 );
		// Closing delimiter ``` (after the island's content end). Group BOTH
		// delimiters with the first content span so they are marked when the island
		// actually executes (count 1), rather than sitting RED.
		int		closeId		= -1;
		Point	delimClose	= findDelimiterAfter( node, node.getEnd() );
		if ( delimClose != null ) {
			closeId = addSpan( delimClose, new Point( delimClose.getLine(), delimClose.getColumn() + 3 ) );
		}
		if ( openId >= 0 || closeId >= 0 ) {
			List<Integer> ids = new ArrayList<>();
			if ( firstContentId[ 0 ] >= 0 ) {
				ids.add( firstContentId[ 0 ] );
			}
			if ( openId >= 0 && !ids.contains( openId ) ) {
				ids.add( openId );
			}
			if ( closeId >= 0 && !ids.contains( closeId ) ) {
				ids.add( closeId );
			}
			if ( ids.size() > 1 ) {
				transpiler.registerSpanGroup( ids.stream().mapToInt( Integer::intValue ).toArray() );
			}
		}
		this.currentSourceType.pop();
	}

	/**
	 * Find the position of an opening {@code ```} delimiter immediately BEFORE the
	 * given point (the start of a template island's content). Scans backward for a
	 * run of three backticks.
	 *
	 * @param node   the containing node (for source access)
	 * @param before the island content start
	 *
	 * @return the position of the first backtick, or null
	 */
	private Point findDelimiterBefore( BoxNode node, Point before ) {
		if ( before == null || node.getPosition() == null || node.getPosition().getSource() == null ) {
			return null;
		}
		String	source	= node.getPosition().getSource().getCode();
		int		offset	= offsetOf( source, before );
		if ( offset < 0 ) {
			return null;
		}
		// Scan back to the start of the line containing (or before) the point.
		for ( int i = offset - 1; i >= 0; i-- ) {
			if ( source.charAt( i ) == '`' ) {
				// Walk to the start of the backtick run.
				int start = i;
				while ( start > 0 && source.charAt( start - 1 ) == '`' ) {
					start--;
				}
				return pointAt( source, start );
			}
			if ( source.charAt( i ) == '\n' ) {
				return null;
			}
		}
		return null;
	}

	/**
	 * Find the position of the closing {@code ```} delimiter immediately AFTER the
	 * given point (the end of a template island's content). Scans forward for a run
	 * of three backticks.
	 *
	 * @param node  the containing node (for source access)
	 * @param after the island content end
	 *
	 * @return the position of the first backtick, or null
	 */
	private Point findDelimiterAfter( BoxNode node, Point after ) {
		if ( after == null || node.getPosition() == null || node.getPosition().getSource() == null ) {
			return null;
		}
		String	source	= node.getPosition().getSource().getCode();
		int		offset	= offsetOf( source, after );
		if ( offset < 0 ) {
			return null;
		}
		for ( int i = offset; i < source.length(); i++ ) {
			if ( source.charAt( i ) == '`' ) {
				return pointAt( source, i );
			}
			if ( source.charAt( i ) == '\n' && i > offset ) {
				return null;
			}
		}
		return null;
	}

	/**
	 * A generic OPEN/CLOSE tag component ({@code <bx:loop>...</bx:loop>},
	 * {@code <bx:output>...</bx:output>}, etc.) executes its body between the two
	 * tags. The OPEN tag is its own span (opened by visitChildren at the statement
	 * start); the CLOSE tag {@code </bx:name>} is grouped with it so both are
	 * marked when the component's body runs. Self-closing / single-tag components
	 * ({@code <bx:set>}, {@code <bx:throw>}) have no close tag — the existing
	 * statement span covers the whole tag.
	 */
	@Override
	public void visit( BoxComponent node ) {
		String name = node.getName() == null ? "" : node.getName().toLowerCase();
		// PARAM components (<bx:param>/<cfparam>/script `param name=... default=...`)
		// defer their DEFAULT VALUE: the runtime only evaluates it when the variable
		// does NOT already exist. Break the default out into its own span (when it
		// is a non-literal expression that may not run) so the exists-case shows it
		// RED — mirroring function argument lazy defaults.
		if ( name.equals( "param" ) ) {
			visitParamComponent( node );
			return;
		}
		if ( isTagContext( node ) ) {
			// BRANCH components (<bx:else>, <bx:elseif>, <cfelse>, <cfelseif>) are
			// self-closing — no close tag to register. Their open tag is registered
			// as the running span so the whole tag is tracked; the body (if any)
			// follows. The Pass B transformer marks these when the branch runs.
			if ( name.equals( "else" ) || name.equals( "elseif" ) ) {
				if ( node.getBody() != null && !node.getBody().isEmpty() ) {
					this.runningStart = node.getStart();
					closeRunningSpan( node.getBody().get( 0 ).getStart() );
					for ( BoxStatement stmt : node.getBody() ) {
						this.runningStart = stmt.getStart();
						stmt.accept( this );
						closeRunningSpan( bufferOutputEnd( stmt ) );
					}
					this.runningStart = null;
				} else {
					this.runningStart = node.getStart();
					closeRunningSpan( node.getEnd() );
				}
				return;
			}

			// CONDITIONAL LOOP components (<bx:loop condition="i < 3"> /
			// <cfloop condition="i < 3">): the condition is re-evaluated EVERY
			// iteration (n true + 1 false exit), so — exactly like a script while
			// loop — it must break out of the always-run header span into its OWN
			// span covering the WHOLE quoted attribute value ("i < 3" INCLUDING the
			// quotes — they are part of the attribute's source). Pass B marks it
			// inside the loop's closure invoker (which runs per-iteration).
			if ( name.equals( "loop" ) ) {
				Point[] condSpan = findLoopConditionSpan( node );
				if ( condSpan != null ) {
					// Header runs once at loop entry: "<bx:loop condition=".
					int headerId = closeRunningSpan( condSpan[ 0 ] );
					// The condition span: the full quoted value ("i < 3"), closed at
					// its end (after the closing quote).
					this.runningStart = condSpan[ 0 ];
					closeRunningSpan( condSpan[ 1 ] );
					// Body statements each open/close their own span; BLANK buffer
					// outputs (the raw newlines between tags) are formatting noise
					// and are skipped, exactly like visitChildren does.
					if ( node.getBody() != null ) {
						for ( BoxStatement stmt : node.getBody() ) {
							if ( stmt instanceof BoxBufferOutput bufOut
							    && bufOut.getExpression() instanceof BoxStringLiteral lit
							    && lit.getValue().isBlank() ) {
								continue;
							}
							this.runningStart = stmt.getStart();
							stmt.accept( this );
							closeRunningSpan( bufferOutputEnd( stmt ) );
						}
					}
					this.runningStart = null;
					// The tag's closing ">" and the </bx:loop> / </cfloop> close both
					// run when the loop is entered — group BOTH with the header so
					// they are GREEN (and merging prevents the second registration
					// from overwriting the first, keyed by the same headerId).
					List<Integer> group = new ArrayList<>();
					group.add( headerId );
					Point tagClose = findTagCloseAfter( node, condSpan[ 1 ] );
					if ( tagClose != null ) {
						int closeId = addSpan( condSpan[ 1 ], new Point( tagClose.getLine(), tagClose.getColumn() + 1 ) );
						if ( closeId >= 0 && !group.contains( closeId ) ) {
							group.add( closeId );
						}
					}
					Point	afterBody	= node.getBody() == null || node.getBody().isEmpty()
					    ? node.getEnd()
					    : node.getBody().get( node.getBody().size() - 1 ).getEnd();
					Point	closeOpen	= findTagCloseOpen( node, afterBody, name );
					if ( closeOpen != null ) {
						Point closeEnd = findTagCloseAfter( node, closeOpen );
						if ( closeEnd != null ) {
							int closeId = addSpan( closeOpen, new Point( closeEnd.getLine(), closeEnd.getColumn() + 1 ) );
							if ( closeId >= 0 && !group.contains( closeId ) ) {
								group.add( closeId );
							}
						}
					}
					if ( group.size() > 1 ) {
						transpiler.registerSpanGroup( group.stream().mapToInt( Integer::intValue ).toArray() );
					}
					return;
				}
			}

			// CONSTRUCT components (<bx:while>, <bx:loop>, <bx:if>, <cfif>, ...):
			// the open tag is the running span, grouped with the close tag.
			if ( node.getBody() != null && !node.getBody().isEmpty() ) {
				int headerId = -1;
				headerId = closeRunningSpan( node.getBody().get( 0 ).getStart() );
				for ( BoxStatement stmt : node.getBody() ) {
					this.runningStart = stmt.getStart();
					stmt.accept( this );
					closeRunningSpan( bufferOutputEnd( stmt ) );
				}
				this.runningStart = null;
				Point afterBody = node.getBody().get( node.getBody().size() - 1 ).getEnd();
				registerTagClose( node, afterBody, name, headerId );
				return;
			}
		}

		// Default: a SCRIPT component body ({@code lock name="..." { ... }},
		// {@code savecontent { ... }}, {@code transaction { ... }}, etc.) is a plain
		// list of body statements that each open their own span. The header (the
		// {@code lock name="..." {} line) is the running span opened by the enclosing
		// statement walk; we close it at the first body statement and group it with
		// the closing {@code }} so both braces stay GREEN together (exactly like a
		// {@link BoxStatementBlock}). Resetting {@code runningStart} prevents the
		// enclosing statement's {@code closeRunningSpan} from minting a phantom
		// RED span over the {@code }\n} gap after the last body statement.
		if ( isTagContext( node ) ) {
			// TAG-based component with a body that reached here — thread the running
			// span generically (each body statement opens its own span).
			visitChildren( node );
			return;
		}
		if ( node.getBody() != null && !node.getBody().isEmpty() ) {
			int headerId = closeRunningSpan( node.getBody().get( 0 ).getStart() );
			for ( BoxStatement stmt : node.getBody() ) {
				if ( stmt instanceof BoxBufferOutput bufOut
				    && bufOut.getExpression() instanceof BoxStringLiteral lit
				    && lit.getValue().isBlank() ) {
					continue;
				}
				this.runningStart = stmt.getStart();
				stmt.accept( this );
				closeRunningSpan( bufferOutputEnd( stmt ) );
			}
			Point closeBrace = node.getEnd() == null ? null : new Point( node.getEnd().getLine(), node.getEnd().getColumn() - 1 );
			registerBraceGroup( headerId, closeBrace );
			this.runningStart = null;
			return;
		}

		// No body (or non-statement children): let the generic child walker handle
		// it — each child statement opens its own span threaded off the running one.
		visitChildren( node );
	}

	/**
	 * Extract the CONDITION expression from a conditional loop component
	 * ({@code <bx:loop condition=...>} / {@code <cfloop condition=...>}). The
	 * parser wraps it as {@code BoxClosure( BoxReturn( expr ) )}; unwrap to the
	 * real expression (which carries the condition's source position).
	 *
	 * @param node the loop component
	 *
	 * @return the condition expression, or null
	 */
	/**
	 * Find the [start, end) span of a conditional loop's WHOLE quoted attribute
	 * value (e.g. {@code "i < 3"} INCLUDING both quotes) in the source. The
	 * condition runs per-iteration, so its span must cover the full attribute
	 * source — the quotes are part of it — not just the inner expression text
	 * (which would truncate at {@code i < 3} and leave the closing quote + ">"
	 * untracked).
	 *
	 * @param node the loop component
	 *
	 * @return a two-point array {valueStart, valueEnd}, or null if no condition
	 */
	private Point[] findLoopConditionSpan( BoxComponent node ) {
		if ( node.getPosition() == null || node.getPosition().getSource() == null ) {
			return null;
		}
		String	source	= node.getPosition().getSource().getCode();
		Point	start	= node.getStart();
		if ( start == null ) {
			return null;
		}
		int offset = offsetOf( source, start );
		if ( offset < 0 ) {
			return null;
		}
		// Scan forward from the tag start for "condition=", then the opening quote.
		int		i		= offset;
		boolean	found	= false;
		for ( ; i < source.length() - "condition=".length(); i++ ) {
			if ( source.regionMatches( true, i, "condition=", 0, "condition=".length() ) ) {
				found = true;
				break;
			}
		}
		if ( !found ) {
			return null;
		}
		i += "condition=".length();
		// Skip whitespace, then expect the opening quote.
		while ( i < source.length() && Character.isWhitespace( source.charAt( i ) ) ) {
			i++;
		}
		if ( i >= source.length() || ( source.charAt( i ) != '"' && source.charAt( i ) != '\'' ) ) {
			return null;
		}
		int		openQuote	= i;
		char	quote		= source.charAt( i );
		// Find the closing quote.
		int		j			= i + 1;
		while ( j < source.length() && source.charAt( j ) != quote ) {
			j++;
		}
		if ( j >= source.length() ) {
			return null;
		}
		return new Point[] { pointAt( source, openQuote ), pointAt( source, j + 1 ) };
	}

	/**
	 * A {@code param} component's DEFAULT attribute is deferred — the runtime only
	 * evaluates it when the variable does NOT already exist (see
	 * {@code Param._invoke}). A non-literal default may therefore never run, so it
	 * breaks into its OWN span (marked only when the deferred closure is invoked);
	 * the rest of the tag (name/type attributes) always runs and stays in the
	 * statement span. Literal defaults cannot throw and are fused inline (they
	 * always evaluate as part of the statement).
	 *
	 * @param node the param component
	 */
	private void visitParamComponent( BoxComponent node ) {
		// The statement span was opened by the enclosing statement at the tag start.
		// Find the "default" attribute; if its value is a non-literal expression,
		// break the running span right before it and visit it as its own span.
		boolean	broke	= false;
		int		headId	= -1;
		Point	lastEnd	= null;
		for ( BoxAnnotation attr : node.getAttributes() ) {
			if ( !attr.getKey().getValue().equalsIgnoreCase( "default" ) ) {
				continue;
			}
			BoxExpression value = attr.getValue();
			if ( value == null ) {
				continue;
			}
			if ( couldThrow( value ) ) {
				// The DEFAULT VALUE'S span includes its own outermost parentheses
				// (e.g. `default=( now() )`) and its interpolation pounds (e.g.
				// `default=#now()#`) — both are part of the deferred expression, not
				// the always-running statement head. Do NOT unwrap them, so the
				// SKIPPED case shows the WHOLE `( now() )` / `#now()#` RED, and the
				// used case shows it all GREEN.
				headId = closeRunningSpan( value.getStart() );
				value.accept( this );
				closeRunningSpan( value.getEnd() );
				lastEnd	= value.getEnd();
				broke	= true;
			}
		}
		// A broken-out default leaves a trailing ";" / ">" that is not executable —
		// reset so the enclosing statement close cannot mint a phantom span over it.
		// A literal default never broke the span: leave runningStart intact so the
		// enclosing statement close still registers the whole statement span.
		if ( broke ) {
			this.runningStart = null;
			// TAG form (<bx:param ...> / <cfparam ...>): the tag's closing ">" is
			// not executable markup of its own, but it must be COVERED — group it
			// with the always-running head span so it is GREEN whenever the tag runs
			// (not left untracked, and not glued to the deferred default).
			if ( isTagContext( node ) && headId >= 0 && lastEnd != null ) {
				Point tagClose = findTagCloseAfter( node, lastEnd );
				if ( tagClose != null ) {
					int closeId = addSpan( lastEnd, new Point( tagClose.getLine(), tagClose.getColumn() + 1 ) );
					if ( closeId >= 0 ) {
						transpiler.registerSpanGroup( new int[] { headId, closeId } );
					}
				}
			}
		}
	}

	/**
	 * A statement block (curly-brace {@code { ... }} body) executes as a unit: its
	 * body statements each open/close their own span (mirroring
	 * {@link #visitChildren}), and the BLOCK ITSELF — both the opening
	 * {@code {} and closing {@code }} braces — is executable. Entering the block
	 * runs the opening brace; reaching the end runs the closing brace; a
	 * {@code break}/{@code return}/{@code throw} mid-body still counts the block
	 * as "entered". Because the block executes atomically at the brace level, both
	 * braces are registered as ONE span GROUP so Pass B emits a single varargs
	 * {@code mark(fileId, {open, close})} at block entry.
	 * <p>
	 * The opening brace region ({@code {} plus trailing whitespace to the first
	 * body statement) is dropped by the first body statement's plain assignment of
	 * {@code runningStart} — only the {@code {} character itself is its own span.
	 * Likewise the semicolon(s) after the last body statement are NOT executable,
	 * so we reset {@code runningStart} to {@code null} after registering — the
	 * enclosing statement's {@code closeRunningSpan} then cannot mint a phantom
	 * {@code ;\n} span.
	 * 
	<p>
	 * Tag-based bodies are ALSO {@link BoxStatementBlock}s but have no curlies —
	 * their source text is the template body, not {@code { } }. Detect a real
	 * curly block by the source starting with {@code {} and ending with {@code }}.
	 * A null position / source (manual or unsourced node) is left alone.
	 */
	@Override
	public void visit( BoxStatementBlock node ) {
		// Each body statement opens/closes its own span (same as visitChildren).
		for ( BoxStatement stmt : node.getBody() ) {
			this.runningStart = stmt.getStart();
			stmt.accept( this );
			closeRunningSpan( bufferOutputEnd( stmt ) );
		}

		// A curly-brace block { ... }: batch-mark both braces as one atomic group.
		Point	start		= node.getStart();
		Point	end			= node.getEnd();
		String	sourceText	= node.getSourceText();
		if ( start != null && end != null && sourceText != null
		    && sourceText.startsWith( "{" ) && sourceText.endsWith( "}" ) ) {
			Point	closeStart	= new Point( end.getLine(), end.getColumn() - 1 );
			int		openId		= addSpan( start, new Point( start.getLine(), start.getColumn() + 1 ) );
			int		closeId		= addSpan( closeStart, end );
			if ( openId >= 0 && closeId >= 0 ) {
				transpiler.registerSpanGroup( new int[] { openId, closeId } );
			}
		}

		// Reset the running span so the enclosing statement close doesn't create a
		// phantom span over the trailing semicolon / newline gap.
		this.runningStart = null;
	}

	/**
	 * A ternary always evaluates its condition, then picks one branch. The condition
	 * continues the running span; each branch breaks into its own span because it
	 * may not run.
	 *
	 * @param node the ternary operation
	 */
	@Override
	public void visit( BoxTernaryOperation node ) {
		// Condition continues the running span.
		node.getCondition().accept( this );

		// whenTrue breaks into its own span (may not run). Do NOT unwrap parens:
		// a `( expr )` branch's span starts at the `(` so the whole parenthesized
		// expression is GREEN/RED together.
		BoxExpression	whenTrue	= node.getWhenTrue();
		int				headId		= closeRunningSpan( leftmostStart( whenTrue ) );
		whenTrue.accept( this );

		// whenFalse breaks into its own span (may not run).
		BoxExpression whenFalse = node.getWhenFalse();
		closeRunningSpan( leftmostStart( whenFalse ) );
		whenFalse.accept( this );

		// TAG context: a self-closing tag like `<cfset x = bar ? baz : bum>` must
		// not glue its closing ">" onto the UNTAKEN false branch (that would show
		// the ">" RED even though the tag itself ran). Close the false branch at its
		// own end, then register the trailing ">" as its own span GROUPED with the
		// ternary's always-run head so it is GREEN.
		if ( isTagContext( node ) ) {
			Point whenFalseEnd = leftmostEnd( whenFalse );
			closeRunningSpan( whenFalseEnd );
			Point tagClose = findTagCloseAfter( node, whenFalseEnd );
			if ( tagClose != null && headId >= 0 ) {
				int closeId = addSpan( whenFalseEnd, new Point( tagClose.getLine(), tagClose.getColumn() + 1 ) );
				if ( closeId >= 0 ) {
					transpiler.registerSpanGroup( new int[] { headId, closeId } );
				}
			}
			this.runningStart = null;
		} else {
			// SCRIPT mode: when a MULTI-LINE parenthesized ternary ends with its
			// closing `)` on a LATER line than the false branch (e.g. `a = (\n
			// ... ? "x" : "y"\n );`), the false-branch span would extend through that
			// trailing `\n)` and inherit its RED (untaken) status — marking the closing
			// `)` line uncovered even though the paren always executes when the ternary
			// completes. The closing `)` must be split out of the false branch and
			// grouped with the ALWAYS-RUN condition/head so it stays GREEN regardless
			// of which branch ran.
			//
			// Only a `)` on a DISTINCT LATER line qualifies. A single-line ternary
			// (`x = a ? b : c;`) has no trailing `)` beyond its false branch, and a
			// nested ternary (`( a ? b : c ) ? 1 : 2`) has its `)` followed by an
			// operator on the SAME line — neither must split, so their threading is
			// untouched (preserving the existing span model).
			Point	falseEnd	= leftmostEnd( whenFalse );
			Point	parenClose	= findParenAfter( node, node.getEnd() );
			if ( parenClose != null && parenClose.getLine() > falseEnd.getLine() ) {
				// Close the false branch at its OWN end so the tail span is purely
				// `[falseEnd, parenEnd]` and does NOT swallow the false branch (which
				// would wrongly count the untaken branch when the tail's mark fires).
				closeRunningSpan( falseEnd );
				Point tailEnd = new Point( parenClose.getLine(), parenClose.getColumn() + 1 );
				// Register the pure closing-paren tail grouped with the always-run head
				// so it is GREEN when the ternary completes, but NEVER counts the branch.
				this.runningStart = falseEnd;
				int tailId = closeRunningSpan( tailEnd );
				if ( tailId >= 0 && headId >= 0 && tailId != headId ) {
					transpiler.registerSpanGroup( new int[] { headId, tailId } );
				}
			}
		}
	}

	/**
	 * A struct literal's values are interleaved [key, value, key, value, ...].
	 * Pass B emits a mark at each VALUE's start, so each value must START its own
	 * span or it can never be counted. The `, key: ` SEPARATOR between the
	 * previous value's end and this value's start runs unconditionally when the
	 * struct is built, but it is pure text — no node starts there, so nothing
	 * would mark it. To keep it GREEN, we group each separator span with the
	 * VALUE that follows it: the value's mark increments both.
	 *
	 * @param node the struct literal
	 */
	@Override
	public void visit( BoxStructLiteral node ) {
		// A struct literal's keys and values are ordinary EXPRESSIONS. Each key and
		// each value must be its OWN span covering EXACTLY that expression — nothing
		// wider. The `:` between key and value, the `,` between entries, and any
		// comment/blank lines before entries are NOT expressions and must NOT be part
		// of any span. So we register each key span and each value span directly
		// (keyStart->keyEnd, valueStart->valueEnd) and do NOT thread a running span
		// across entries (which would create wide gap/separator spans that swallow
		// comments). Pass B marks each key/value when its node is transformed.
		//
		// Close the outer statement's head (`x = {`) right AFTER the opening brace so
		// it stays its own span (`x = {`) and is never extended across a following
		// comment into the first key. The head still exists, so lineAt(statement
		// start) is covered.
		if ( node.getStart() != null && this.runningStart != null ) {
			Point open = node.getStart();
			closeRunningSpan( new Point( open.getLine(), open.getColumn() + 1 ) );
		}
		// After the head close, clear the running span so the enclosing statement's
		// own close cannot mint a phantom span across the whole struct.
		this.runningStart = null;
		List<BoxExpression>	values		= node.getValues();
		int					lastValueId	= -1;
		for ( int i = 0; i < values.size(); i += 2 ) {
			BoxExpression	key		= values.get( i );
			// Register the key's tight span, then visit it.
			int				keyId	= addSpan( leftmostStart( key ), leftmostEnd( key ) );
			key.accept( this );
			if ( i + 1 >= values.size() ) {
				break;
			}
			BoxExpression	value	= values.get( i + 1 );
			// Register the value's tight span, then visit it (Pass B marks it).
			int				valueId	= addSpan( leftmostStart( value ), leftmostEnd( value ) );
			value.accept( this );
			// Group the KEY's span with the VALUE's span: an identifier/scope key is
			// compiled to a bare LdcInsnNode in BoxStructLiteralTransformer.transformKey,
			// which BYPASSES the Pass B mark hook — so its span would never be marked
			// (RED). The VALUE always goes through transform() and IS marked; grouping
			// the key with the value makes the value's mark cover the key too. String
			// keys go through transform() themselves, so they need no group.
			if ( keyId >= 0 && valueId >= 0 && keyId != valueId ) {
				transpiler.registerSpanGroup( new int[] { valueId, keyId } );
			}
			// Remember the value span (for the closing-delimiter tail grouping).
			Point	valueStart	= leftmostStart( value );
			long	packed		= ( ( long ) valueStart.getLine() << 32 ) | ( valueStart.getColumn() & 0xFFFFFFFFL );
			lastValueId = transpiler.getSpanId( packed );
		}
		// The struct's CLOSING `}` (and any `)` of an enclosing call that trails
		// after the last value) runs whenever the struct runs, but no node starts
		// there — so it would be an unmarked span and stay RED. Group it with the
		// LAST VALUE's span so the last value's mark covers it.
		if ( lastValueId >= 0 && node.getEnd() != null ) {
			Point	tailEnd		= node.getEnd();
			Point	callClose	= findParenAfter( node, tailEnd );
			if ( callClose != null ) {
				tailEnd = new Point( callClose.getLine(), callClose.getColumn() + 1 );
			}
			// Only close the tail FROM the last value's end (never across a preceding
			// comment), so the closing delimiters are covered without dragging comments
			// into a span.
			int tailId = addSpan( leftmostEnd( values.get( values.size() - 1 ) ), tailEnd );
			if ( tailId >= 0 && tailId != lastValueId ) {
				// MERGE with any EXISTING group keyed by the last value (e.g. the
				// last value's key group) instead of overwriting — a plain
				// registerSpanGroup would drop the last key from the group, leaving
				// it RED.
				List<Integer>	group		= new ArrayList<>();
				int[]			existing	= transpiler.peekSpanGroup( lastValueId );
				if ( existing != null ) {
					for ( int e : existing ) {
						if ( !group.contains( e ) ) {
							group.add( e );
						}
					}
				} else {
					group.add( lastValueId );
				}
				if ( !group.contains( tailId ) ) {
					group.add( tailId );
				}
				transpiler.registerSpanGroup( group.stream().mapToInt( Integer::intValue ).toArray() );
			}
		}
		this.runningStart = null;
	}

	/**
	 * An array literal's elements are each worth an executable span (Pass B emits
	 * a mark at each element's start). The elements themselves may split further
	 * (ternary/elvis inside an element), exactly like struct values. After the
	 * last element, the CLOSING {@code ]} (and any {@code )} of an enclosing call
	 * that trails after it) runs whenever the array runs, but no node starts there
	 * — so, as with a struct, we close the running span at the {@code ]} and group
	 * that tail with the LAST element's span so the last element's mark covers it.
	 * <p>
	 * This mirrors {@link #visit(BoxStructLiteral)} exactly: visit each element,
	 * thread the running span, remember the last element's span id, then group the
	 * closing tail with it. Arrays are otherwise NOT handled anywhere in this
	 * visitor — they previously fell through to {@link #visitChildren}, which left
	 * the closing {@code ]} as an ungrouped (RED) tail.
	 *
	 * @param node the array literal
	 */
	@Override
	public void visit( BoxArrayLiteral node ) {
		List<BoxExpression>	values		= node.getValues();
		int					lastValueId	= -1;
		for ( int i = 0; i < values.size(); i++ ) {
			BoxExpression	value		= values.get( i );
			// Break the element out of the running head at its start — UNCONDITIONALLY,
			// mirroring visit(BoxStructLiteral) which breaks at every value whether or
			// not it can throw. This guarantees each element has a registered span start
			// so the closing-delimiter tail can be grouped with the LAST element's span
			// (see below). Guard against the first element sharing the statement head
			// (sepId resolves to the head id) so we don't double-count.
			Point			elemStart	= leftmostStart( value );
			int				sepId		= closeRunningSpan( elemStart );
			// Visit the element — its internal ternary/throw logic further subdivides
			// it, and Pass B marks the span starting at its start.
			value.accept( this );
			// Close the element's span at its rightmost end so the next separator
			// starts fresh.
			closeRunningSpan( leftmostEnd( value ) );
			// Remember the element span that Pass B resolves at the element's start
			// (for the LAST element this is what the closing-delimiter tail gets
			// grouped with).
			Point valueStart = leftmostStart( value );
			if ( valueStart != null ) {
				long packed = ( ( long ) valueStart.getLine() << 32 ) | ( valueStart.getColumn() & 0xFFFFFFFFL );
				lastValueId = transpiler.getSpanId( packed );
			}
			// Group the separator (`,` between elements) with the element that follows
			// it, exactly as structs group `, key: ` separators with the value — this
			// keeps the separators GREEN when the element runs. Skip i==0 where the
			// separator IS the statement head (already marked by the assignment).
			if ( i > 0 && sepId >= 0 && lastValueId >= 0 && lastValueId != sepId ) {
				transpiler.registerSpanGroup( new int[] { lastValueId, sepId } );
			}
		}
		// Group the CLOSING `]` (and any trailing `)` of an enclosing call) with the
		// last element's span, so the last element's mark covers the tail — the same
		// mechanism `visit(BoxStructLiteral)` uses for its closing `}` / `)`.
		if ( lastValueId >= 0 && node.getEnd() != null ) {
			Point	tailEnd		= node.getEnd();
			Point	callClose	= findParenAfter( node, tailEnd );
			if ( callClose != null ) {
				tailEnd = new Point( callClose.getLine(), callClose.getColumn() + 1 );
			}
			int tailId = closeRunningSpan( tailEnd );
			if ( tailId >= 0 && tailId != lastValueId ) {
				List<Integer>	group		= new ArrayList<>();
				int[]			existing	= transpiler.peekSpanGroup( lastValueId );
				if ( existing != null ) {
					for ( int e : existing ) {
						if ( !group.contains( e ) ) {
							group.add( e );
						}
					}
				} else {
					group.add( lastValueId );
				}
				if ( !group.contains( tailId ) ) {
					group.add( tailId );
				}
				transpiler.registerSpanGroup( group.stream().mapToInt( Integer::intValue ).toArray() );
			}
		}
	}

	/**
	 * A function invocation's callee and ALL arguments execute whenever the call
	 * runs (there is no short-circuit across args). Each argument value is threaded
	 * through its own visit (so a struct/array/ternary argument still subdivides
	 * into its own spans), and — critically — the trailing region after the LAST
	 * argument up to the closing {@code )} (e.g. {@code spec = {...},\nsuite =
	 * ...\ndata = ...\n);} on their own lines) runs unconditionally with the call.
	 * That trailing tail is pure text with no node starting there, so, as with a
	 * struct's closing {@code }}/)`, it must be grouped with the invocation's head
	 * span (the callee start — always marked) so it stays GREEN.
	 *
	 * @param node the function invocation
	 */
	@Override
	public void visit( BoxExpressionInvocation node ) {
		visitInvocation( node, node.getExpr(), node.getArguments() );
	}

	@Override
	public void visit( BoxFunctionInvocation node ) {
		visitInvocation( node, null, node.getArguments() );
	}

	@Override
	public void visit( BoxMethodInvocation node ) {
		// The object+name form: thread the running span through the object and the
		// name (the "callee" is the whole dotted path), then the arguments.
		visitInvocation( node, null, node.getArguments() );
		if ( node.getObj() != null ) {
			node.getObj().accept( this );
		}
		if ( node.getName() != null ) {
			node.getName().accept( this );
		}
	}

	@Override
	public void visit( BoxStaticMethodInvocation node ) {
		visitInvocation( node, node.getObj(), node.getArguments() );
		if ( node.getName() != null ) {
			node.getName().accept( this );
		}
	}

	/**
	 * A function invocation's callee and ALL arguments execute whenever the call
	 * runs (there is no short-circuit across args). Each argument value is threaded
	 * through its own visit (so a struct/array/ternary argument still subdivides
	 * into its own spans), and — critically — a MULTI-LINE invocation whose CLOSING
	 * {@code )} sits on a LATER line than the last argument (e.g. {@code fn(
	 * spec = {...},\nsuite = ...,\ndata = ...\n);} on their own lines) leaves that
	 * trailing {@code \n);} region as pure text with no node starting there — it
	 * runs unconditionally with the call, so it must be grouped with the
	 * invocation's head span (the callee start — always marked) so it stays GREEN.
	 * Single-line invocations need no tail (the {@code )} is covered by the
	 * enclosing statement's span), preserving existing span models.
	 *
	 * @param node      the invocation (for its source bounds)
	 * @param callee    the expression to thread first (may be null for a bare-name
	 *                  function call whose name is not an expression)
	 * @param arguments the invocation's arguments
	 */
	private void visitInvocation( BoxExpression node, BoxExpression callee, List<BoxArgument> arguments ) {
		// The callee always runs; thread the running span through it.
		if ( callee != null ) {
			callee.accept( this );
		}
		// MULTI-LINE invocations (args on later lines, closing `)` on its own line)
		// need per-argument spans AND a grouped closing tail so every line is
		// covered — e.g. `fn( spec = {...},\nsuite = ...,\ndata = ...\n);`. For a
		// SINGLE-LINE call the enclosing statement's span already covers everything
		// and adding per-arg spans would change the existing span model — so the
		// single-line path just threads the args through untouched.
		boolean multiLine = node.getStart() != null && node.getEnd() != null
		    && node.getEnd().getLine() > node.getStart().getLine();
		if ( !multiLine ) {
			for ( BoxArgument arg : arguments ) {
				if ( arg.getName() != null ) {
					arg.getName().accept( this );
				}
				if ( arg.getValue() != null ) {
					arg.getValue().accept( this );
				}
			}
			return;
		}

		// Multi-line: break each argument's VALUE out of the running head so it (and
		// the `,\n` separator before it) gets a REAL span — without this, a named
		// arg like `suite = thread.suite` on its own line threads the running span
		// and gets swallowed into the trailing tail with no span of its own (RED).
		Point	lastArgEnd	= null;
		int[]	lastValueId	= { -1 };
		for ( int i = 0; i < arguments.size(); i++ ) {
			BoxArgument		arg		= arguments.get( i );
			BoxExpression	value	= arg.getValue();
			if ( arg.getName() != null ) {
				arg.getName().accept( this );
			}
			if ( value != null ) {
				Point	valueStart	= leftmostStart( value );
				int		sepId		= closeRunningSpan( valueStart );
				value.accept( this );
				Point valueEnd = leftmostEnd( value );
				closeRunningSpan( valueEnd );
				if ( valueStart != null ) {
					long packed = ( ( long ) valueStart.getLine() << 32 ) | ( valueStart.getColumn() & 0xFFFFFFFFL );
					lastValueId[ 0 ] = transpiler.getSpanId( packed );
				}
				// Group the separator (`,` before this arg) with this arg's value so
				// it stays GREEN when the value runs — mirroring struct/array
				// separators. Skip i==0 where the separator IS the call head.
				if ( i > 0 && sepId >= 0 && lastValueId[ 0 ] >= 0 && lastValueId[ 0 ] != sepId ) {
					transpiler.registerSpanGroup( new int[] { lastValueId[ 0 ], sepId } );
				}
				if ( valueEnd != null && ( lastArgEnd == null
				    || valueEnd.getLine() > lastArgEnd.getLine()
				    || ( valueEnd.getLine() == lastArgEnd.getLine() && valueEnd.getColumn() > lastArgEnd.getColumn() ) ) ) {
					lastArgEnd = valueEnd;
				}
			}
		}
		// The CLOSING `)` on a LATER line than the last argument is pure text with
		// no node starting there — it runs whenever the LAST ARGUMENT runs (the `)`
		// always follows the final arg), so it must be grouped with the LAST
		// ARGUMENT's span — exactly how a struct's closing `}` is grouped with its
		// last value — to stay GREEN.
		if ( lastArgEnd != null && lastValueId[ 0 ] >= 0 && node.getEnd() != null
		    && node.getEnd().getLine() > lastArgEnd.getLine() ) {
			Point	tailEnd	= leftmostEnd( node );
			int		tailId	= closeRunningSpan( tailEnd );
			if ( tailId >= 0 && tailId != lastValueId[ 0 ] ) {
				List<Integer>	group		= new ArrayList<>();
				int[]			existing	= transpiler.peekSpanGroup( lastValueId[ 0 ] );
				if ( existing != null ) {
					for ( int e : existing ) {
						if ( !group.contains( e ) ) {
							group.add( e );
						}
					}
				} else {
					group.add( lastValueId[ 0 ] );
				}
				if ( !group.contains( tailId ) ) {
					group.add( tailId );
				}
				transpiler.registerSpanGroup( group.stream().mapToInt( Integer::intValue ).toArray() );
			}
		}
	}

	/**
	 * An assignment's target (left) continues the running span; the RHS (right) is
	 * dispatched through its own visitor logic so a binary-op chain or ternary on
	 * the right still splits into per-operand spans.
	 *
	 * @param node the assignment
	 */
	@Override
	public void visit( BoxAssignment node ) {
		node.getLeft().accept( this );
		if ( node.getRight() != null ) {
			node.getRight().accept( this );
		}
	}

	/**
	 * A {@code param} statement's DEFAULT VALUE is evaluated lazily — the runtime
	 * only invokes the deferred default expression when the variable does NOT
	 * already exist (see {@code Param._invoke}). A literal default is compiled
	 * inline (it always runs when the param statement runs, so it stays in the
	 * statement span), but a non-literal default breaks into its OWN span: it may
	 * not run at all when the variable already exists — exactly like a function
	 * argument's lazy default. This makes the exists-case visibly RED in the
	 * coverage HTML.
	 * <p>
	 * The tag form ({@code <bx:param name="x" default="now()">} /
	 * {@code <cfparam ...>}) is a {@link BoxComponent} named "param" whose
	 * attributes hold the default — handled in {@link #visit(BoxComponent)}.
	 *
	 * @param node the param statement
	 */
	@Override
	public void visit( BoxParam node ) {
		BoxExpression	defaultValue	= node.getDefaultValue();
		boolean			broke			= false;
		if ( defaultValue != null && couldThrow( defaultValue ) ) {
			// The default's span includes its own outermost parentheses (e.g.
			// `default=( now() )`) — those parens belong to the deferred expression,
			// not the always-running statement head. Do NOT unwrap them, so the
			// skipped case shows the WHOLE `( now() )` RED and the used case all
			// GREEN.
			closeRunningSpan( defaultValue.getStart() );
			defaultValue.accept( this );
			closeRunningSpan( defaultValue.getEnd() );
			broke = true;
		}
		// The variable name/type and literal defaults continue the running span.
		if ( node.getVariable() != null ) {
			node.getVariable().accept( this );
		}
		if ( node.getType() != null ) {
			node.getType().accept( this );
		}
		// A broken-out default leaves a trailing ";" that is not executable — reset
		// so the enclosing statement close cannot mint a phantom span over it. A
		// literal default never broke the span: leave runningStart intact so the
		// enclosing statement close still registers the whole statement span.
		if ( broke ) {
			this.runningStart = null;
		}
	}

	/**
	 * An if's condition always evaluates (continues the running span), then exactly
	 * one branch body runs. The then-body and else-body each break into their own
	 * span because only one of them may run. The {@code else} KEYWORD is itself
	 * executable — it runs exactly when the else branch runs — so it gets its own
	 * span, marked at the else branch entry (Pass B emits a manual mark for it).
	 *
	 * @param node the if statement
	 */
	@Override
	public void visit( BoxIfElse node ) {
		// An ELSEIF is a BoxIfElse nested in a parent if's else branch. In TAG
		// form its source is the "<bx:elseif ...>" / "<cfelseif ...>" tag, whose
		// AST start points at the "<" (the parser fixes tag positions) — so the
		// whole tag runs from start through the tag's ">". Script "else if" is a
		// plain keyword chain handled by the normal span logic below.
		boolean isElseIf = isTagContext( node ) && isElseIfNode( node );
		if ( isElseIf && node.getStart() != null ) {
			Point tagEnd = findTagCloseAfter( node, node.getStart() );
			if ( tagEnd != null ) {
				// Close whatever the enclosing block opened at the elseif "<".
				this.runningStart = node.getStart();
				closeRunningSpan( new Point( tagEnd.getLine(), tagEnd.getColumn() + 1 ) );
				// Condition continues a FRESH span at the condition start.
				this.runningStart = null;
			}
		}

		// Condition continues the running span.
		node.getCondition().accept( this );

		// Then-body breaks into its own span (may not run). Always close the
		// condition span at the body start, then visit the body. A block body
		// (BoxStatementBlock) opens/closes its statements and braces itself; ANY
		// other single statement body is opened/closed here as its own executable
		// span (e.g. `if( x ) foo();` gives `foo()` its own span). This handles
		// any statement type — block, expression, assignment, etc.
		int headerId = -1;
		if ( node.getThenBody() != null ) {
			if ( node.getThenBody() instanceof BoxStatementBlock ) {
				headerId = closeRunningSpan( node.getThenBody().getStart() );
				node.getThenBody().accept( this );
			} else {
				headerId			= closeRunningSpan( node.getThenBody().getStart() );
				this.runningStart	= node.getThenBody().getStart();
				node.getThenBody().accept( this );
				closeRunningSpan( node.getThenBody().getEnd() );
			}
		}

		// Else-body breaks into its own span (may not run). The "else" keyword
		// before it is a span of its own, marked when the else branch runs. The
		// else body starts FRESH (plain assignment) so the whitespace between the
		// "else" keyword and the body (e.g. the space in "else if") never becomes a
		// phantom span. Same block-vs-single-statement handling as the then body.
		if ( node.getElseBody() != null ) {
			// In template markup, the branch keyword is the FULL tag (<bx:else>,
			// <bx:elseif ...>, <cfelse>, <cfelseif ...>). Find the whole tag so the
			// "<bx:" prefix is part of the span.
			Point elseKw = findKeywordBefore( node, node.getElseBody().getStart(), "else" );
			if ( isTagContext( node ) ) {
				Point fullTag = findBranchTagBefore( node, node.getElseBody().getStart() );
				if ( fullTag != null ) {
					Point tagEnd = findTagCloseAfter( node, fullTag );
					if ( tagEnd != null ) {
						this.runningStart = fullTag;
						closeRunningSpan( new Point( tagEnd.getLine(), tagEnd.getColumn() + 1 ) );
						elseKw = null; // full tag handled
					}
				}
			}
			if ( elseKw != null ) {
				this.runningStart = elseKw;
				closeRunningSpan( new Point( elseKw.getLine(), elseKw.getColumn() + 4 ) );
			}
			if ( node.getElseBody() instanceof BoxStatementBlock ) {
				this.runningStart = node.getElseBody().getStart();
				node.getElseBody().accept( this );
			} else {
				this.runningStart = node.getElseBody().getStart();
				node.getElseBody().accept( this );
				closeRunningSpan( node.getElseBody().getEnd() );
			}
		}

		// Reset so the enclosing statement close doesn't mint a phantom span over
		// the trailing ";" / newline after the last branch body.
		this.runningStart = null;

		// Tag-based if: the closing </bx:if> / </cfif> is marked together with the
		// opening <bx:if> tag when the if is entered. (Elseif nodes are nested
		// BoxIfElses — their sourceText starts with the elseif prefix; only the
		// TOP-LEVEL if's own close tag is registered here, keyed by its header.)
		// Scan for the close tag after the LAST body (else body if present, else
		// then body).
		if ( isTagContext( node ) && !isElseIfNode( node ) ) {
			Point afterBody = node.getEnd();
			if ( node.getElseBody() != null && node.getElseBody().getEnd() != null ) {
				afterBody = node.getElseBody().getEnd();
			} else if ( node.getThenBody() != null && node.getThenBody().getEnd() != null ) {
				afterBody = node.getThenBody().getEnd();
			}
			registerTagClose( node, afterBody, "if", headerId );
		}
	}

	/**
	 * Whether the current visit context is template tag markup. The source-type
	 * stack is pushed/popped while descending ({@link #visit(BoxTemplate)},
	 * {@link #visit(BoxScriptIsland)}, {@link #visit(BoxTemplateIsland)},
	 * {@link #visit(BoxClass)}), so no source-text inspection is needed.
	 *
	 * @return true when inside template markup
	 */
	private boolean isTagContext( BoxNode node ) {
		return isTemplate();
	}

	/**
	 * Whether this node is an ELSEIF — a {@link BoxIfElse} that is (or whose
	 * enclosing statement block is) the {@code elseBody} of another
	 * {@link BoxIfElse}. This is purely structural: the parser models both the
	 * tag form ({@code <bx:elseif>} / {@code <cfelseif>}) and the script form
	 * ({@code else if}) identically — an if nested in a parent if's else branch.
	 *
	 * @param node the node
	 *
	 * @return true if the node is an elseif
	 */
	private boolean isElseIfNode( BoxNode node ) {
		if ( ! ( node instanceof BoxIfElse ) ) {
			return false;
		}
		// A script "else if" is DIRECTLY the parent if's elseBody; a tag
		// <bx:elseif> is wrapped in a BoxStatementBlock that is the elseBody.
		BoxNode	parent	= node.getParent();
		boolean	wrapped	= false;
		if ( parent instanceof BoxStatementBlock block ) {
			wrapped	= true;
			parent	= block.getParent();
		}
		if ( ! ( parent instanceof BoxIfElse parentIf ) || parentIf.getElseBody() == null ) {
			return false;
		}
		// The parent's elseBody must BE this node (script) or the block wrapping it
		// (tag). Also confirm the node is actually INSIDE that elseBody.
		return wrapped
		    ? parentIf.getElseBody() == node.getParent()
		    : parentIf.getElseBody() == node;
	}

	/**
	 * A while loop evaluates its condition (continues the running span), then the
	 * body may run zero or more times. The body breaks into its own span because
	 * it may never run (empty loop).
	 *
	 * @param node the while statement
	 */
	@Override
	public void visit( BoxWhile node ) {
		// Tag-based while (<bx:while ...>...</bx:while>): split the open-tag header
		// span from the CONDITION so the condition can be marked per-iteration. The
		// header (the "<bx:while condition=") runs once at loop entry, but the
		// condition expression ("i < 3") is re-evaluated every iteration — giving it
		// its own span keyed at its own start lets Pass B mark it inside the loop.
		int headerId = -1;
		if ( isTagContext( node ) && node.getCondition() != null && node.getCondition().getPosition() != null ) {
			// The header continues the running span up to the condition's start.
			headerId = closeRunningSpan( node.getCondition().getPosition().getStart() );
			node.getCondition().accept( this );
		} else if ( node.getCondition() != null && node.getCondition().getPosition() != null ) {
			// SCRIPT while: the header ("while( ") runs once at loop entry, but the
			// CONDITION expression is re-evaluated EVERY iteration (n+1 times — n
			// true + one final false to exit). Fusing it into the header span would
			// hide those per-iteration counts (Pass B's mark hook fires per
			// transformed node, but only the FIRST claim of a span wins). So split
			// the condition into its OWN span keyed at its own start, letting Pass
			// B emit its mark inside the loop body where it runs each iteration.
			headerId = closeRunningSpan( node.getCondition().getPosition().getStart() );
			node.getCondition().accept( this );
		} else {
			// Script while with no positioned condition: it continues the running span.
			node.getCondition().accept( this );
		}

		// The (single) body statement breaks into its own span (may not run).
		if ( node.getBody() != null ) {
			closeRunningSpan( node.getBody().getStart() );
			node.getBody().accept( this );
		}

		// Tag-based while: the CLOSE tag is marked together with the OPEN tag when
		// the loop is entered.
		if ( isTagContext( node ) ) {
			registerTagClose( node, node.getBody() == null ? node.getEnd() : node.getBody().getEnd(), "while", headerId );
		}
	}

	/**
	 * A do-while loop always evaluates its body at least once, then re-tests the
	 * condition. Both the {@code do} keyword and the {@code while(} header are
	 * GUARANTEED to run (the body always runs, and the condition is always tested
	 * at least once), so they are registered as ONE span GROUP and batch-marked
	 * when the do-while executes. The condition expression itself may not re-run
	 * (the loop may not iterate again) — it breaks into its own span.
	 *
	 * @param node the do statement
	 */
	@Override
	public void visit( BoxDo node ) {
		// "do " header: the running span (do keyword, set by visitChildren) closes
		// at the body start.
		int doId = -1;
		if ( node.getBody() != null ) {
			doId = closeRunningSpan( node.getBody().getStart() );
		} else {
			doId = closeRunningSpan( node.getCondition().getStart() );
		}

		// Body is guaranteed to run at least once.
		if ( node.getBody() != null ) {
			node.getBody().accept( this );
		}

		// "while( " header: find the while keyword and register a span from it to
		// the condition start (covers "while(" — always evaluated at least once).
		int		whileId	= -1;
		Point	whileKw	= findKeywordBefore( node, node.getCondition().getStart(), "while" );
		if ( whileKw != null ) {
			this.runningStart	= whileKw;
			whileId				= closeRunningSpan( node.getCondition().getStart() );
		}

		// The condition may not re-run (loop may not iterate again) — its own span.
		this.runningStart = node.getCondition().getStart();
		node.getCondition().accept( this );
		closeRunningSpan( node.getCondition().getEnd() );

		// The closing ")" after the condition is punctuation that always runs (the
		// condition is always tested at least once) — its own span, batched with
		// "do" + "while(". The span covers from the condition's end THROUGH any
		// whitespace to the ")" so no gap is left untracked.
		int		closeParenId	= -1;
		Point	condEnd			= node.getCondition().getEnd();
		Point	closeParen		= findParenAfter( node, condEnd );
		if ( closeParen != null && condEnd != null ) {
			closeParenId = addSpan( condEnd, new Point( closeParen.getLine(), closeParen.getColumn() + 1 ) );
		}

		// Batch-mark "do" + "while(" + ")" together when the do-while executes.
		if ( doId >= 0 ) {
			List<Integer> group = new ArrayList<>();
			group.add( doId );
			if ( whileId >= 0 && !group.contains( whileId ) ) {
				group.add( whileId );
			}
			if ( closeParenId >= 0 && !group.contains( closeParenId ) ) {
				group.add( closeParenId );
			}
			if ( group.size() > 1 ) {
				transpiler.registerSpanGroup( group.stream().mapToInt( Integer::intValue ).toArray() );
			}
		}

		// Reset so the enclosing statement close doesn't mint a phantom over the
		// trailing ";" / newline.
		this.runningStart = null;
	}

	/**
	 * A C-style for loop evaluates its header (initializer, condition, step) then
	 * the body may run zero or more times. The header continues the running span;
	 * the body breaks into its own span because it may never run.
	 *
	 * @param node the for-index statement
	 */
	@Override
	public void visit( BoxForIndex node ) {
		// TAG-based loop (<bx:loop from=... to=...>...</bx:loop>): the header tag
		// is the running span (opened at the statement start), and the </bx:loop>
		// close tag is grouped with it, marked once at loop entry.
		if ( isTagContext( node ) ) {
			int headerId = -1;
			if ( node.getBody() != null ) {
				headerId = closeRunningSpan( node.getBody().getStart() );
			} else {
				headerId = closeRunningSpan( node.getEnd() );
			}
			if ( node.getBody() != null ) {
				node.getBody().accept( this );
			}
			Point afterBody = node.getBody() == null ? node.getEnd() : node.getBody().getEnd();
			registerTagClose( node, afterBody, "loop", headerId );
			this.runningStart = null;
			return;
		}

		// The header has THREE independent run-counts: the INITIALIZER runs once,
		// the CONDITION is re-evaluated every iteration (n+1 times: n true + one
		// final false to exit), and the STEP runs once per iteration (n times).
		// Fusing them into one span would hide those counts — Pass B's mark hook
		// fires per transformed node, but only the FIRST claim of a span wins.
		// So each header part gets its OWN span, keyed at its own start.
		if ( node.getInitializer() != null ) {
			node.getInitializer().accept( this );
		}
		if ( node.getCondition() != null && node.getCondition().getPosition() != null ) {
			closeRunningSpan( node.getCondition().getPosition().getStart() );
			node.getCondition().accept( this );
		}
		if ( node.getStep() != null && node.getStep().getPosition() != null ) {
			closeRunningSpan( node.getStep().getPosition().getStart() );
			node.getStep().accept( this );
		}

		// The (single) body statement breaks into its own span (may not run).
		if ( node.getBody() != null ) {
			closeRunningSpan( node.getBody().getStart() );
			node.getBody().accept( this );
		}
	}

	/**
	 * A for-in loop evaluates its collection, then the body may run zero or more
	 * times. The collection (and any loop variables) continues the running span;
	 * the body breaks into its own span because an empty collection never runs it.
	 *
	 * @param node the for-in statement
	 */
	@Override
	public void visit( BoxForIn node ) {
		// TAG-based loop (<bx:loop array=...>...</bx:loop>): the header tag is the
		// running span (opened at the statement start), and the </bx:loop> close
		// tag is grouped with it, marked once at loop entry.
		if ( isTagContext( node ) ) {
			int headerId = -1;
			if ( node.getBody() != null ) {
				headerId = closeRunningSpan( node.getBody().getStart() );
			} else {
				headerId = closeRunningSpan( node.getEnd() );
			}
			if ( node.getBody() != null ) {
				node.getBody().accept( this );
			}
			Point afterBody = node.getBody() == null ? node.getEnd() : node.getBody().getEnd();
			registerTagClose( node, afterBody, "loop", headerId );
			this.runningStart = null;
			return;
		}

		// The collection and loop variables continue the running span.
		if ( node.getVariable() != null ) {
			node.getVariable().accept( this );
		}
		if ( node.getSecondVariable() != null ) {
			node.getSecondVariable().accept( this );
		}
		if ( node.getExpression() != null ) {
			node.getExpression().accept( this );
		}

		// The (single) body statement breaks into its own span (may not run).
		if ( node.getBody() != null ) {
			closeRunningSpan( node.getBody().getStart() );
			node.getBody().accept( this );
		}
	}

	/**
	 * A switch evaluates its condition once, then each case label is tested in
	 * order until one matches. The condition continues the running span (the
	 * switch header); the closing brace is batched with the header so both are
	 * marked when the switch is entered (like a {@link BoxStatementBlock}'s
	 * braces). Each case label breaks into its own span — marked when that case
	 * is evaluated — and each case body statement breaks into its own span because
	 * it may not run (only the matching case's body runs).
	 * <p>
	 * Tag-based switches are ALSO {@link BoxSwitch} but have no curlies (their
	 * source is the {@code <bx:switch>...} markup). Only register the closing
	 * brace when the source genuinely ends in {@code }}.
	 *
	 * @param node the switch statement
	 */
	@Override
	public void visit( BoxSwitch node ) {
		// Condition continues the running span (switch header).
		node.getCondition().accept( this );

		// The header closes at the first case's label start (the running span
		// covers "switch( x ) {" — condition evaluation + entry). Capture its id
		// to batch with the closing brace.
		int headerId = -1;
		if ( !node.getCases().isEmpty() ) {
			BoxSwitchCase	first			= node.getCases().get( 0 );
			Point			firstLabelStart	= first.getCondition() != null ? first.getCondition().getStart() : first.getStart();
			headerId = closeRunningSpan( firstLabelStart );
		} else {
			headerId = closeRunningSpan( node.getEnd() );
		}

		// Each case breaks into its own span (at most one may run).
		for ( BoxSwitchCase switchCase : node.getCases() ) {
			switchCase.accept( this );
		}

		// The closing "}" — batched with the header so both mark at switch entry.
		Point	end			= node.getEnd();
		String	sourceText	= node.getSourceText();
		if ( end != null && sourceText != null && sourceText.endsWith( "}" ) ) {
			Point	braceStart	= new Point( end.getLine(), end.getColumn() - 1 );
			int		closeId		= addSpan( braceStart, end );
			if ( headerId >= 0 && closeId >= 0 ) {
				transpiler.registerSpanGroup( new int[] { headerId, closeId } );
			}
		}

		// Tag-based switch: the closing </bx:switch> / </cfswitch> is marked with
		// the opening tag when the switch is entered.
		if ( isTagContext( node ) && end != null && ( sourceText == null || !sourceText.endsWith( "}" ) ) ) {
			Point afterBody = node.getCases().isEmpty() ? end : node.getCases().get( node.getCases().size() - 1 ).getEnd();
			registerTagClose( node, afterBody, "switch", headerId );
		}

		// Reset so the enclosing statement close doesn't mint a phantom span over
		// the trailing semicolon / newline gap.
		this.runningStart = null;
	}

	/**
	 * A switch case evaluates its (optional) condition label, then its body runs
	 * only if this case matches. The case label — the {@code case}/{@code default}
	 * KEYWORD through the value expression and colon — breaks into its OWN span so
	 * the keyword itself is executable when the case is evaluated. For a value
	 * case, the switch transformer emits a manual mark for this label span right
	 * before evaluating the condition (the value node's own mark would only cover
	 * the value, not the keyword). A {@code default:} label has no condition, so
	 * it is batched with its first body statement and marked when the default
	 * actually runs. Each body statement breaks into its own span because the body
	 * may not run.
	 * <p>
	 * The running span is reset after the body so the next case's label (or the
	 * switch's closing brace) starts clean — no phantom {@code ;\ncase} span.
	 *
	 * @param node the switch case
	 */
	@Override
	public void visit( BoxSwitchCase node ) {
		// The case label (keyword + value + colon) is its own span, starting at
		// the case/default keyword.
		this.runningStart = node.getStart();
		if ( node.getCondition() != null ) {
			node.getCondition().accept( this );
		}
		Point	labelEnd	= node.getBody().isEmpty() ? node.getEnd() : node.getBody().get( 0 ).getStart();
		int		labelId		= closeRunningSpan( labelEnd );

		// Each body statement opens/closes its own span (body may not run).
		int		firstBodyId	= -1;
		for ( BoxStatement stmt : node.getBody() ) {
			this.runningStart = stmt.getStart();
			stmt.accept( this );
			int id = closeRunningSpan( stmt.getEnd() );
			if ( firstBodyId < 0 ) {
				firstBodyId = id;
			}
		}

		// A default label has no condition node to mark it — batch it with the
		// first body statement so both are marked when the default runs.
		if ( node.getCondition() == null && labelId >= 0 && firstBodyId >= 0 && !node.getBody().isEmpty() ) {
			transpiler.registerSpanGroup( new int[] { firstBodyId, labelId } );
		}

		// Tag-based case/defaultcase: the closing </bx:case> / </cfcase> (or
		// </bx:defaultcase> / </cfdefaultcase>) is marked together with the case
		// label when the case is evaluated. A default case has no condition — its
		// label is batched with the first body statement (keyed by firstBodyId), so
		// group the close tag with THAT so it fires when the default runs.
		if ( isTagContext( node ) ) {
			Point	afterBody	= node.getBody().isEmpty() ? node.getEnd() : node.getBody().get( node.getBody().size() - 1 ).getEnd();
			boolean	isDefault	= node.getCondition() == null;
			registerTagClose( node, afterBody, isDefault ? "defaultcase" : "case", isDefault ? firstBodyId : labelId );
		}

		// Reset so the next case / closing brace starts clean.
		this.runningStart = null;
	}

	/**
	 * A try/catch/finally executes its try body (with its braces batch-marked at
	 * entry), its catch bodies (braces marked only when a catch actually runs),
	 * and its finally body (braces batch-marked when the finally runs). Each
	 * construct's braces — the header span covering {@code try {}/{@code catch(
	 * ... ) {}/{@code finally {} and the closing {@code }} — are registered as ONE
	 * span GROUP and batch-marked at the construct's entry point.
	 * 
	<p>
	 * Body statements each open/close their own span, and the running span is
	 * reset after each construct so no phantom {@code ;\n} span is minted over the
	 * trailing semicolons (which are NOT executable).
	 * 
	<p>
	 * Tag-based tries are also {@link BoxTry} but have no curlies (their source is
	 * the {@code <bx:try>...} markup). Only register braces when the source
	 * genuinely contains them.
	 *
	 * @param node the try statement
	 */
	@Override
	public void visit( BoxTry node ) {
		// TAG-based try (<bx:try>...</bx:try>): the open/close tags of try, each
		// catch, and finally are registered as tag groups (marked with their
		// construct's entry). Script tries use braces instead.
		if ( isTagContext( node ) ) {
			visitTagTry( node );
			return;
		}

		// TRY: the header (try {) is the running span; close it at the first body
		// statement, and group it with the closing "}".
		int		headerId	= -1;
		Point	tryClose	= findBraceBefore( node, firstConstructStart( node ) );
		Point	tryFirst	= firstPositionedStart( node.getTryBody() );
		if ( tryFirst != null ) {
			headerId = closeRunningSpan( tryFirst );
		} else if ( tryClose != null ) {
			headerId = closeRunningSpan( tryClose );
		}
		for ( BoxStatement stmt : node.getTryBody() ) {
			if ( stmt.getStart() == null || stmt.getEnd() == null ) {
				continue; // phantom statement (no source position) — nothing to track
			}
			this.runningStart = stmt.getStart();
			stmt.accept( this );
			closeRunningSpan( stmt.getEnd() );
		}
		registerBraceGroup( headerId, tryClose );
		this.runningStart = null;

		// CATCHES: each catch's braces are a group, marked only when that catch runs.
		for ( BoxTryCatch catchNode : node.getCatches() ) {
			Point	catchClose	= catchNode.getEnd() == null ? null : new Point( catchNode.getEnd().getLine(), catchNode.getEnd().getColumn() - 1 );
			int		catchHeader	= -1;
			this.runningStart = catchNode.getStart();
			Point catchFirst = firstPositionedStart( catchNode.getCatchBody() );
			if ( catchFirst != null ) {
				catchHeader = closeRunningSpan( catchFirst );
			} else if ( catchClose != null ) {
				catchHeader = closeRunningSpan( catchClose );
			}
			for ( BoxStatement stmt : catchNode.getCatchBody() ) {
				if ( stmt.getStart() == null || stmt.getEnd() == null ) {
					continue; // phantom statement (no source position) — nothing to track
				}
				this.runningStart = stmt.getStart();
				stmt.accept( this );
				closeRunningSpan( stmt.getEnd() );
			}
			registerBraceGroup( catchHeader, catchClose );
			this.runningStart = null;
		}

		// FINALLY: its braces are a group, marked when the finally runs.
		if ( !node.getFinallyBody().isEmpty() ) {
			Point	finallyClose	= node.getEnd() == null ? null : new Point( node.getEnd().getLine(), node.getEnd().getColumn() - 1 );
			// The finally header starts at the "finally" keyword (found by scanning
			// back from the first finally body statement past the preceding "}").
			Point	firstFinally	= firstPositionedStart( node.getFinallyBody() );
			Point	finallyKw		= firstFinally == null ? null : findKeywordBraceStart( node, firstFinally, "finally" );
			this.runningStart = finallyKw != null ? finallyKw : ( firstFinally != null ? firstFinally : node.getStart() );
			int finallyHeader = closeRunningSpan( firstFinally != null ? firstFinally : finallyClose );
			for ( BoxStatement stmt : node.getFinallyBody() ) {
				if ( stmt.getStart() == null || stmt.getEnd() == null ) {
					continue; // phantom statement (no source position) — nothing to track
				}
				this.runningStart = stmt.getStart();
				stmt.accept( this );
				closeRunningSpan( stmt.getEnd() );
			}
			registerBraceGroup( finallyHeader, finallyClose );
			this.runningStart = null;
		}
	}

	/**
	 * The start point of the construct that follows the try body — the first
	 * catch's keyword, the finally block, or the try's own end. Used to locate the
	 * try's closing brace (just before it).
	 *
	 * @param node the try
	 *
	 * @return the point just after the try body
	 */
	private Point firstConstructStart( BoxTry node ) {
		if ( !node.getCatches().isEmpty() ) {
			return node.getCatches().get( 0 ).getStart();
		}
		if ( !node.getFinallyBody().isEmpty() ) {
			return node.getFinallyBody().get( 0 ).getStart();
		}
		return node.getEnd();
	}

	/**
	 * The first body statement with a real source position, or null if none. Some
	 * AST builders emit a phantom statement (null position/source) at the front of
	 * a construct body; the header must close at the FIRST POSITIONED statement or
	 * the header span silently disappears.
	 *
	 * @param body the construct's body statements
	 *
	 * @return the first positioned statement's start point, or null
	 */
	private Point firstPositionedStart( List<BoxStatement> body ) {
		if ( body != null ) {
			for ( BoxStatement stmt : body ) {
				if ( stmt.getStart() != null ) {
					return stmt.getStart();
				}
			}
		}
		return null;
	}

	/**
	 * Register a brace group (header/entry span + closing brace) as an atomic
	 * unit. The header span already includes the opening brace (it runs from the
	 * construct keyword through the {@code {}); the closing {@code }} is added as
	 * its own span and batched with the header. If either is absent (tag code, or
	 * an empty construct), only what exists is registered.
	 *
	 * @param headerId the id of the header/entry span
	 * @param close    the closing brace span start point (or null)
	 */
	private void registerBraceGroup( int headerId, Point close ) {
		List<Integer> ids = new ArrayList<>();
		if ( headerId >= 0 ) {
			ids.add( headerId );
		}
		if ( close != null ) {
			int closeId = addSpan( close, new Point( close.getLine(), close.getColumn() + 1 ) );
			if ( closeId >= 0 && !ids.contains( closeId ) ) {
				ids.add( closeId );
			}
		}
		if ( ids.size() > 1 ) {
			transpiler.registerSpanGroup( ids.stream().mapToInt( Integer::intValue ).toArray() );
		}
	}

	/**
	 * Visit a TAG-based try ({@code <bx:try>...</bx:try>}): register the try open
	 * tag (grouped with its {@code </bx:try>} close), each catch open tag (grouped
	 * with its {@code </bx:catch>} close), and the finally open + close tags
	 * (grouped together), plus all body statements. Body statements open/close
	 * their own spans; the running span is reset after each construct.
	 *
	 * @param node the try statement
	 */
	private void visitTagTry( BoxTry node ) {
		// TRY open tag is the running span (opened by visitChildren at the "<bx:try"
		// statement start); close it at the first body statement and group with the
		// closing </bx:try>.
		int headerId = -1;
		if ( !node.getTryBody().isEmpty() ) {
			headerId = closeRunningSpan( node.getTryBody().get( 0 ).getStart() );
		} else {
			headerId = closeRunningSpan( node.getEnd() );
		}
		for ( BoxStatement stmt : node.getTryBody() ) {
			this.runningStart = stmt.getStart();
			stmt.accept( this );
			closeRunningSpan( stmt.getEnd() );
		}
		this.runningStart = null;
		// </bx:try> close grouped with the try open tag — scan BACKWARD from the
		// node end (the close tag sits after the catch/finally bodies).
		if ( isTagContext( node ) ) {
			registerTagCloseBackward( node, node.getEnd(), "try", headerId );
		}

		// CATCHES: each catch open tag grouped with its </bx:catch> close. The open
		// tag (<bx:catch> / <cfcatch>) may not be the AST node's start (CF nodes can
		// point at the body) — locate the real tag by scanning source.
		for ( BoxTryCatch catchNode : node.getCatches() ) {
			int		catchHeader	= -1;
			Point	catchOpen	= findCatchOpenTag( node, catchNode );
			this.runningStart = catchOpen != null ? catchOpen : catchNode.getStart();
			Point firstBodyStart = catchNode.getCatchBody().isEmpty() || catchNode.getCatchBody().get( 0 ).getStart() == null
			    ? null
			    : catchNode.getCatchBody().get( 0 ).getStart();
			if ( firstBodyStart != null ) {
				catchHeader = closeRunningSpan( firstBodyStart );
			} else if ( catchOpen != null ) {
				// No positioned body statement (CF): the open tag runs through its ">".
				Point tagEnd = findTagCloseAfter( node, catchOpen );
				catchHeader = closeRunningSpan( tagEnd != null ? new Point( tagEnd.getLine(), tagEnd.getColumn() + 1 ) : catchNode.getEnd() );
			} else {
				catchHeader = closeRunningSpan( catchNode.getEnd() );
			}
			for ( BoxStatement stmt : catchNode.getCatchBody() ) {
				this.runningStart = stmt.getStart();
				stmt.accept( this );
				closeRunningSpan( stmt.getEnd() );
			}
			this.runningStart = null;
			Point catchEnd = catchNode.getCatchBody().isEmpty()
			    ? catchNode.getEnd()
			    : catchNode.getCatchBody().get( catchNode.getCatchBody().size() - 1 ).getEnd();
			// </bx:catch> is right after the catch body — forward scan.
			registerTagClose( node, catchEnd, "catch", catchHeader );
		}

		// FINALLY: BOTH the <bx:finally> open and </bx:finally> close tags are
		// registered and grouped — marked when the finally runs.
		if ( !node.getFinallyBody().isEmpty() ) {
			int finallyHeader = -1;
			this.runningStart = findFinallyOpenTag( node );
			if ( !node.getFinallyBody().isEmpty() ) {
				finallyHeader = closeRunningSpan( node.getFinallyBody().get( 0 ).getStart() );
			}
			for ( BoxStatement stmt : node.getFinallyBody() ) {
				this.runningStart = stmt.getStart();
				stmt.accept( this );
				closeRunningSpan( stmt.getEnd() );
			}
			this.runningStart = null;
			Point finallyEnd = node.getFinallyBody().get( node.getFinallyBody().size() - 1 ).getEnd();
			registerTagClose( node, finallyEnd, "finally", finallyHeader );
		}
	}

	/**
	 * Find the position of the {@code <bx:finally>} / {@code <cffinally>} open tag
	 * before the finally body (scanning back from the first finally body
	 * statement).
	 *
	 * @param node the try statement
	 *
	 * @return the finally open tag's {@code <} position, or null
	 */
	private Point findFinallyOpenTag( BoxTry node ) {
		if ( node.getFinallyBody().isEmpty() || node.getPosition() == null || node.getPosition().getSource() == null ) {
			return null;
		}
		String	source	= node.getPosition().getSource().getCode();
		Point	first	= node.getFinallyBody().get( 0 ).getStart();
		int		offset	= offsetOf( source, first );
		if ( offset < 0 ) {
			return null;
		}
		for ( int i = offset - 1; i >= 0; i-- ) {
			if ( source.charAt( i ) == '<' ) {
				String rest = source.substring( i, Math.min( source.length(), i + 30 ) );
				if ( rest.matches( "<(?:bx:|cf)?finally[\\s\\S]*" ) ) {
					return pointAt( source, i );
				}
				// Previous tag — the finally tag must be after it.
				return null;
			}
		}
		return null;
	}

	/**
	 * Find the position of the {@code <bx:catch>} / {@code <cfcatch>} open tag
	 * before the catch body (scanning back from the first catch body statement).
	 * The AST catch node's start may point at the body (CF nodes), so the tag is
	 * located in source — mirroring {@link #findFinallyOpenTag}.
	 *
	 * @param node      the try statement (for source access)
	 * @param catchNode the catch clause
	 *
	 * @return the catch open tag's {@code <} position, or null
	 */
	private Point findCatchOpenTag( BoxTry node, BoxTryCatch catchNode ) {
		if ( node.getPosition() == null || node.getPosition().getSource() == null ) {
			return null;
		}
		// An empty catch body (CF) leaves no body statement to scan back from — use
		// the catch node's own start (which CF positions at the tag's "<").
		if ( catchNode.getCatchBody().isEmpty() ) {
			Point s = catchNode.getStart();
			if ( s == null ) {
				return null;
			}
			String	source	= node.getPosition().getSource().getCode();
			int		off		= offsetOf( source, s );
			if ( off < 0 ) {
				return null;
			}
			String rest = source.substring( off, Math.min( source.length(), off + 30 ) );
			return rest.matches( "<(?:bx:|cf)?catch[\\s\\S]*" ) ? s : null;
		}
		String	source	= node.getPosition().getSource().getCode();
		Point	first	= catchNode.getCatchBody().isEmpty() || catchNode.getCatchBody().get( 0 ).getStart() == null
		    ? catchNode.getEnd()
		    : catchNode.getCatchBody().get( 0 ).getStart();
		if ( first == null ) {
			return null;
		}
		int offset = offsetOf( source, first );
		if ( offset < 0 ) {
			return null;
		}
		for ( int i = offset - 1; i >= 0; i-- ) {
			if ( source.charAt( i ) == '<' ) {
				String rest = source.substring( i, Math.min( source.length(), i + 30 ) );
				if ( rest.matches( "<(?:bx:|cf)?catch[\\s\\S]*" ) ) {
					return pointAt( source, i );
				}
				// A closing tag (</...>) between the end and the open tag is skipped
				// (the catch's own </cfcatch>); any OTHER tag before it means the
				// catch open tag is after it, so stop.
				if ( !rest.startsWith( "</" ) ) {
					return null;
				}
			}
		}
		return null;
	}

	/**
	 * Find the {@code <} of the closing tag {@code </bx:name>} / {@code </cf...>}
	 * for the given tag name, scanning BACKWARD from the given point (the node's
	 * end) for the LAST occurrence. Used when the close tag is BEFORE the point
	 * (e.g. the {@code </bx:try>} which sits after the finally/catch bodies but
	 * before the node end).
	 *
	 * @param node    the construct node (for source access)
	 * @param from    the point to scan back from
	 * @param tagName the tag name to match
	 *
	 * @return the closing tag's {@code <} position, or null
	 */
	private Point findTagCloseOpenBackward( BoxNode node, Point from, String tagName ) {
		if ( from == null || node.getPosition() == null || node.getPosition().getSource() == null ) {
			return null;
		}
		String	source	= node.getPosition().getSource().getCode();
		int		offset	= offsetOf( source, from );
		if ( offset < 0 ) {
			return null;
		}
		String	close1	= "</bx:" + tagName;
		String	close2	= "</cf" + tagName;
		for ( int i = offset - 1; i >= 0; i-- ) {
			if ( source.startsWith( close1, i ) || source.startsWith( close2, i ) ) {
				return pointAt( source, i );
			}
		}
		return null;
	}

	/**
	 * For a TAG-based construct ({@code <bx:while>...</bx:while>}, etc.), find the
	 * closing {@code </bx:name>} / {@code </cf...>} tag that follows the given
	 * point and register it as a span GROUPED with the construct's header span, so
	 * Pass B marks BOTH the open and close tag when the construct's entry fires.
	 * If the header span is absent (no header id) or no close tag is found, no
	 * group is registered (the close tag, if found, is still registered so it has
	 * a span).
	 *
	 * @param node      the construct node (for source access)
	 * @param afterBody the point just after the construct's body (scan forward)
	 * @param tagName   the tag name to match (e.g. "while", "if", "switch")
	 * @param headerId  the construct's header/entry span id (or -1)
	 */
	private void registerTagClose( BoxNode node, Point afterBody, String tagName, int headerId ) {
		Point closeOpen = findTagCloseOpen( node, afterBody, tagName );
		if ( closeOpen == null ) {
			return;
		}
		// Dedup: a given close tag is registered only once (nested constructs may
		// all see the same enclosing close tag).
		long packed = ( ( long ) closeOpen.getLine() << 32 ) | ( closeOpen.getColumn() & 0xFFFFFFFFL );
		if ( !registeredTagCloses.add( packed ) ) {
			return;
		}
		// The close-tag span runs from "<" through ">" (the whole </bx:name>).
		Point closeEnd = findTagCloseAfter( node, closeOpen );
		if ( closeEnd == null ) {
			return;
		}
		int closeId = addSpan( closeOpen, new Point( closeEnd.getLine(), closeEnd.getColumn() + 1 ) );
		if ( closeId < 0 ) {
			return;
		}
		List<Integer> ids = new ArrayList<>();
		if ( headerId >= 0 ) {
			ids.add( headerId );
		}
		if ( !ids.contains( closeId ) ) {
			ids.add( closeId );
		}
		if ( ids.size() > 1 ) {
			transpiler.registerSpanGroup( ids.stream().mapToInt( Integer::intValue ).toArray() );
		}
	}

	/**
	 * Same as {@link #registerTagClose} but finds the closing tag by scanning
	 * BACKWARD from the given point (used when the close tag precedes the point,
	 * e.g. {@code </bx:try>} after the finally body).
	 */
	private void registerTagCloseBackward( BoxNode node, Point from, String tagName, int headerId ) {
		Point closeOpen = findTagCloseOpenBackward( node, from, tagName );
		if ( closeOpen == null ) {
			return;
		}
		Point closeEnd = findTagCloseAfter( node, closeOpen );
		if ( closeEnd == null ) {
			return;
		}
		int closeId = addSpan( closeOpen, new Point( closeEnd.getLine(), closeEnd.getColumn() + 1 ) );
		if ( closeId < 0 ) {
			return;
		}
		List<Integer> ids = new ArrayList<>();
		if ( headerId >= 0 ) {
			ids.add( headerId );
		}
		if ( !ids.contains( closeId ) ) {
			ids.add( closeId );
		}
		if ( ids.size() > 1 ) {
			transpiler.registerSpanGroup( ids.stream().mapToInt( Integer::intValue ).toArray() );
		}
	}

	/**
	 * Find the {@code <} of the closing tag {@code </bx:name>} / {@code </cf...>}
	 * for the given tag name, scanning forward from the given point (the end of
	 * the construct's body).
	 *
	 * @param node    the construct node (for source access)
	 * @param from    the point to scan forward from
	 * @param tagName the tag name to match
	 *
	 * @return the closing tag's {@code <} position, or null
	 */
	private Point findTagCloseOpen( BoxNode node, Point from, String tagName ) {
		if ( from == null || node.getPosition() == null || node.getPosition().getSource() == null ) {
			return null;
		}
		String	source	= node.getPosition().getSource().getCode();
		int		offset	= offsetOf( source, from );
		if ( offset < 0 ) {
			return null;
		}
		// Match "</bx:tagName>" or "</cfTagName>". The CF form is SHORTER (</cf vs
		// </bx:), so the scan bound must use the shorter pattern — otherwise a
		// `</cfloop>` sitting at the very END of the file (no trailing newline)
		// falls past `source.length() - "</bx:loop".length()` and is never found.
		String	close1	= "</bx:" + tagName;
		String	close2	= "</cf" + ( tagName.equalsIgnoreCase( "defaultcase" ) ? "defaultcase" : tagName );
		int		maxLen	= Math.min( close1.length(), close2.length() );
		for ( int i = offset; i <= source.length() - maxLen; i++ ) {
			if ( source.startsWith( close1, i ) || source.startsWith( close2, i ) ) {
				return pointAt( source, i );
			}
		}
		return null;
	}

	/**
	 * Register an OPENING + CLOSING brace pair as ONE atomic span group, keyed by
	 * the opening brace — so both are batch-marked when the body they enclose
	 * executes. Used for function body braces (and anything else whose braces are
	 * not a {@link BoxStatementBlock}).
	 *
	 * @param open the opening {@code {} position (group key)
	 *             @param close the closing {@code }} position (or null)
	 */
	private void registerOpenCloseBraceGroup( Point open, Point close ) {
		int openId = addSpan( open, new Point( open.getLine(), open.getColumn() + 1 ) );
		if ( openId < 0 ) {
			return;
		}
		List<Integer> ids = new ArrayList<>();
		ids.add( openId );
		if ( close != null ) {
			int closeId = addSpan( close, new Point( close.getLine(), close.getColumn() + 1 ) );
			if ( closeId >= 0 && !ids.contains( closeId ) ) {
				ids.add( closeId );
			}
		}
		if ( ids.size() > 1 ) {
			transpiler.registerSpanGroup( ids.stream().mapToInt( Integer::intValue ).toArray() );
		}
	}

	/**
	 * Find the {@code }} character that closes a construct, scanning backward from
	 * the given point to the first non-whitespace character.
	 *
	 * @param node  the containing node (for source access)
	 * @param point the point to scan back from (exclusive)
	 *
	 * @return the closing brace point, or null
	 */
	private Point findBraceBefore( BoxNode node, Point point ) {
		if ( point == null || node.getPosition() == null || node.getPosition().getSource() == null ) {
			return null;
		}
		String	source	= node.getPosition().getSource().getCode();
		int		offset	= offsetOf( source, point );
		if ( offset < 0 ) {
			return null;
		}
		for ( int i = offset - 1; i >= 0; i-- ) {
			char c = source.charAt( i );
			if ( c == '}' ) {
				return pointAt( source, i );
			}
			if ( !Character.isWhitespace( c ) ) {
				return null;
			}
		}
		return null;
	}

	/**
	 * Find the construct keyword (e.g. {@code finally}) that precedes a point, by
	 * scanning back from the given position past the preceding construct's closing
	 * brace to the first {@code } keyword {} pattern.
	 *
	 * @param node    the containing node (for source access)
	 * @param after   the point just after the construct (its first body statement)
	 * @param keyword the construct keyword to match
	 *
	 * @return the keyword position, or null
	 */
	private Point findKeywordBraceStart( BoxNode node, Point after, String keyword ) {
		if ( after == null || node.getPosition() == null || node.getPosition().getSource() == null ) {
			return null;
		}
		String	source	= node.getPosition().getSource().getCode();
		int		offset	= offsetOf( source, after );
		if ( offset < 0 ) {
			return null;
		}
		for ( int i = offset - 1; i >= 0; i-- ) {
			if ( source.charAt( i ) == '{' ) {
				// Walk back from the brace over spaces to the keyword token.
				int kwEnd = i;
				while ( kwEnd > 0 && source.charAt( kwEnd - 1 ) == ' ' ) {
					kwEnd--;
				}
				int kwStart = kwEnd;
				while ( kwStart > 0 && Character.isLetterOrDigit( source.charAt( kwStart - 1 ) ) ) {
					kwStart--;
				}
				if ( source.substring( kwStart, kwEnd ).equalsIgnoreCase( keyword ) ) {
					return pointAt( source, kwStart );
				}
			}
		}
		return null;
	}

	/**
	 * Find a standalone keyword word (e.g. {@code while}, {@code finally}) that
	 * occurs immediately BEFORE the given point in the source — scanning back from
	 * the point for the first occurrence of the word delimited by
	 * non-letter-or-digit characters.
	 *
	 * @param node    the containing node (for source access)
	 * @param before  the point to scan back from (the keyword is right before it)
	 * @param keyword the keyword to match
	 *
	 * @return the keyword position, or null
	 */
	private Point findKeywordBefore( BoxNode node, Point before, String keyword ) {
		if ( before == null || node.getPosition() == null || node.getPosition().getSource() == null ) {
			return null;
		}
		String	source	= node.getPosition().getSource().getCode();
		int		offset	= offsetOf( source, before );
		if ( offset < 0 ) {
			return null;
		}
		for ( int i = offset - 1; i >= 0; i-- ) {
			char c = source.charAt( i );
			// In template markup, a "<" begins a tag and a ">" ends one — do not
			// scan past them for a keyword (prevents grabbing "else" out of a
			// comment or a different tag above).
			if ( c == '<' || c == '>' ) {
				return null;
			}
			if ( Character.isLetterOrDigit( c ) ) {
				// Walk back to the start of this word.
				int	wordEnd		= i + 1;
				int	wordStart	= i;
				while ( wordStart > 0 && Character.isLetterOrDigit( source.charAt( wordStart - 1 ) ) ) {
					wordStart--;
				}
				if ( source.substring( wordStart, wordEnd ).equalsIgnoreCase( keyword ) ) {
					return pointAt( source, wordStart );
				}
				i = wordStart; // continue scanning before this word
			}
		}
		return null;
	}

	/**
	 * Find the opening {@code <} of a template tag that precedes (or contains) the
	 * given point — scanning back from the point for the nearest {@code <}. Used
	 * to capture the FULL tag markup (e.g. {@code <bx:else>}, {@code <cfelse>})
	 * rather than just the keyword word inside it. Returns null if no {@code <} is
	 * found before the point.
	 *
	 * @param node   the containing node (for source access)
	 * @param before the point to scan back from
	 *
	 * @return the {@code <} position, or null
	 */
	private Point findTagOpenBefore( BoxNode node, Point before ) {
		if ( before == null || node.getPosition() == null || node.getPosition().getSource() == null ) {
			return null;
		}
		String	source	= node.getPosition().getSource().getCode();
		int		offset	= offsetOf( source, before );
		if ( offset < 0 ) {
			return null;
		}
		for ( int i = offset - 1; i >= 0; i-- ) {
			char c = source.charAt( i );
			if ( c == '<' ) {
				return pointAt( source, i );
			}
			// A statement boundary before the tag means the tag starts elsewhere.
			if ( c == '\n' || c == '>' ) {
				return null;
			}
		}
		return null;
	}

	/**
	 * Find the closing {@code >} of a template tag that begins at (or before) the
	 * given point — scanning forward for the nearest {@code >}. Used to end a full
	 * tag span (e.g. {@code <bx:elseif x EQ 2>}) right after the {@code >}.
	 *
	 * @param node the containing node (for source access)
	 * @param from the point to scan forward from
	 *
	 * @return the {@code >} position (the tag's closing angle), or null
	 */
	private Point findTagCloseAfter( BoxNode node, Point from ) {
		if ( from == null || node.getPosition() == null || node.getPosition().getSource() == null ) {
			return null;
		}
		String	source	= node.getPosition().getSource().getCode();
		int		offset	= offsetOf( source, from );
		if ( offset < 0 ) {
			return null;
		}
		for ( int i = offset; i < source.length(); i++ ) {
			char c = source.charAt( i );
			if ( c == '>' ) {
				return pointAt( source, i );
			}
			// A newline before the > means this is not a single-line tag.
			if ( c == '\n' ) {
				return null;
			}
		}
		return null;
	}

	/**
	 * Find the closing {@code >} of a self-closing tag when ONLY WHITESPACE sits
	 * between the given point and the {@code >} (the point is the end of the tag's
	 * last operand). Used to decide whether a right operand is the LAST expression
	 * in the tag — if more expression text follows, it is not.
	 *
	 * @param node the containing node (for source access)
	 * @param from the operand's end (scan forward from here)
	 *
	 * @return the {@code >} position, or null if none / non-whitespace intervenes
	 */
	private Point tagCloseAfterWhitespaceOnly( BoxNode node, Point from ) {
		if ( from == null || node.getPosition() == null || node.getPosition().getSource() == null ) {
			return null;
		}
		String	source	= node.getPosition().getSource().getCode();
		int		offset	= offsetOf( source, from );
		if ( offset < 0 ) {
			return null;
		}
		for ( int i = offset; i < source.length(); i++ ) {
			char c = source.charAt( i );
			if ( c == '>' ) {
				return pointAt( source, i );
			}
			if ( !Character.isWhitespace( c ) ) {
				return null;
			}
		}
		return null;
	}

	/**
	 * Find the opening {@code <} of the branch tag ({@code <bx:else>},
	 * {@code <bx:elseif ...>}, {@code <cfelse>}, {@code <cfelseif ...>}) that
	 * immediately precedes the given point, scanning back past the else-body
	 * content and stopping at the previous tag's {@code >}. Returns the {@code <}
	 * position of the branch tag, or null.
	 *
	 * @param node   the containing node (for source access)
	 * @param before the else-body start (scan back from here)
	 *
	 * @return the branch tag {@code <} position, or null
	 */
	private Point findBranchTagBefore( BoxNode node, Point before ) {
		if ( before == null || node.getPosition() == null || node.getPosition().getSource() == null ) {
			return null;
		}
		String	source	= node.getPosition().getSource().getCode();
		int		offset	= offsetOf( source, before );
		if ( offset < 0 ) {
			return null;
		}
		// Scan back for the LAST else/elseif tag whose ">" is closest to "before":
		// find each "<", resolve its tag, and keep the nearest one that precedes
		// the else-body start.
		Point	best	= null;
		int		bestEnd	= -1;
		for ( int i = offset - 1; i >= 0; i-- ) {
			if ( source.charAt( i ) == '<' ) {
				String rest = source.substring( i, Math.min( source.length(), i + 50 ) );
				// Match a plain else tag (<bx:else>/<cfelse>) OR an elseif tag
				// (<bx:elseif>/<cfelseif>). The elseif tag is also captured here
				// (via the previous branch's else path); addSpan dedups by start.
				if ( rest.matches( "<(?:bx:|cf)?else[a-zA-Z]*[\\s\\S]*" ) ) {
					// Resolve this tag's close.
					int j = i + 1;
					while ( j < source.length() && source.charAt( j ) != '>' ) {
						j++;
					}
					if ( j < source.length() && j > bestEnd ) {
						bestEnd	= j;
						best	= pointAt( source, i );
					}
				}
				// The immediate previous tag boundary — keep scanning past it for
				// older tags is pointless (best is already the nearest), so if this
				// was a non-else tag we can stop: the branch tag must be AFTER it.
				if ( !rest.matches( "<(?:bx:|cf)?else[a-zA-Z]*[\\s\\S]*" ) && best == null ) {
					return null;
				}
			}
		}
		return best;
	}

	/**
	 * Find the closing {@code )} after a point, skipping any whitespace between.
	 * Used to capture the {@code )} that closes a {@code while(...)} header — it is
	 * punctuation that always runs.
	 *
	 * @param node  the containing node (for source access)
	 * @param after the point just before the {@code )} (may be followed by spaces)
	 *
	 * @return the {@code )} position, or null
	 */
	private Point findParenAfter( BoxNode node, Point after ) {
		if ( after == null || node.getPosition() == null || node.getPosition().getSource() == null ) {
			return null;
		}
		String	source	= node.getPosition().getSource().getCode();
		int		offset	= offsetOf( source, after );
		if ( offset < 0 ) {
			return null;
		}
		for ( int i = offset; i < source.length(); i++ ) {
			char c = source.charAt( i );
			if ( c == ')' ) {
				return pointAt( source, i );
			}
			if ( !Character.isWhitespace( c ) ) {
				return null;
			}
		}
		return null;
	}

	/**
	 * Find the opening {@code {} that occurs BEFORE the given point in the source —
	 * scanning backward for the first {@code {} character. Used to locate a body's
	 * opening brace (e.g. the {@code {} of a function body, which is between the
	 * declaration shell and the first body statement).
	 *
	 * @param node the containing node (for source access)
	 * 
	 * @param before the point to scan back from
	 *
	 * @return the opening brace point, or null
	 */
	private Point findOpenBraceBefore( BoxNode node, Point before ) {
		if ( before == null || node.getPosition() == null || node.getPosition().getSource() == null ) {
			return null;
		}
		String	source	= node.getPosition().getSource().getCode();
		int		offset	= offsetOf( source, before );
		if ( offset < 0 ) {
			return null;
		}
		for ( int i = offset - 1; i >= 0; i-- ) {
			if ( source.charAt( i ) == '{' ) {
				return pointAt( source, i );
			}
			if ( source.charAt( i ) == ';' || source.charAt( i ) == '}' ) {
				// Passed a statement boundary / closing brace — not a block open.
				return null;
			}
		}
		return null;
	}

	/**
	 * Find the opening {@code {} of an EMPTY-BODY construct ({@code function
	 * foo(){} }) whose node end points at/past the CLOSING {@code }}. Scanning
	 * backward with {@link #findOpenBraceBefore} fails there because it aborts at
	 * the first {@code }} — there is no body statement to anchor from. Instead,
	 * scan backward past the single closing {@code }} to the matching {@code {.
	 * 
	<p>
	 * Handles the canonical empty body {@code {} }; multi-line empty bodies with
	 * interior whitespace are matched too. If the source doesn't show a closing
	 * brace immediately (tag context / abstract), returns null.
	 *
	 * @param node the empty-body function
	 *
	 * @return the opening brace point, or null
	 */
	private Point findEmptyBodyOpenBrace( BoxNode node ) {
		Point before = node.getEnd();
		if ( before == null || node.getPosition() == null || node.getPosition().getSource() == null ) {
			return null;
		}
		String	source	= node.getPosition().getSource().getCode();
		int		offset	= offsetOf( source, before );
		if ( offset < 0 ) {
			return null;
		}
		// Skip the closing "}" (and any trailing whitespace) to reach the "{", but
		// only if the node's tail really looks like an empty body: scan back over
		// the closing "}" then immediately to the opening "{".
		boolean seenClose = false;
		for ( int i = offset - 1; i >= 0; i-- ) {
			char c = source.charAt( i );
			if ( c == '}' ) {
				seenClose = true;
				continue;
			}
			if ( seenClose ) {
				if ( c == '{' ) {
					return pointAt( source, i );
				}
				if ( c == ';' ) {
					// Passed a statement boundary without finding "{ }" — not an
					// empty body (e.g. an abstract `function foo();`).
					return null;
				}
				// Between "}" and "{": only whitespace/annotations allowed in an
				// empty body; anything else means it wasn't an empty body.
				if ( !Character.isWhitespace( c ) ) {
					return null;
				}
			} else if ( c == '{' || c == ';' ) {
				// No closing "}" seen before a "{" or ";" — not an empty `{}` body.
				return null;
			}
		}
		return null;
	}

	/**
	 * Find the opening {@code {} that occurs AFTER the given point in the source —
	 * scanning forward for the first {@code {} character. Used to locate a body's
	 * opening brace (e.g. a class's {@code {} after its header keyword). Unlike
	 * {@link #findOpenBraceBefore}, this cannot be derailed by a property
	 * declaration's {@code ;} that sits between the class header and its first
	 * body statement.
	 *
	 * @param node the containing node (for source access)
	 * 
	 * @param from the point to scan forward from (the class keyword)
	 *
	 * @return the opening brace point, or null
	 */
	private Point findOpenBraceAfter( BoxNode node, Point from ) {
		if ( from == null || node.getPosition() == null || node.getPosition().getSource() == null ) {
			return null;
		}
		String	source	= node.getPosition().getSource().getCode();
		int		offset	= offsetOf( source, from );
		if ( offset < 0 ) {
			return null;
		}
		for ( int i = offset; i < source.length(); i++ ) {
			if ( source.charAt( i ) == '{' ) {
				return pointAt( source, i );
			}
		}
		return null;
	}

	/**
	 * Convert a Point to a character offset in the source.
	 */
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

	/**
	 * Convert a character offset back to a Point.
	 */
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

	/**
	 * A comparison operation is its own node class (distinct from
	 * {@link BoxBinaryOperation}) but has the same partial-execution semantics:
	 * the left operand continues the running span, and the RIGHT operand breaks
	 * into its own span because it runs arbitrary logic and may throw (leaving the
	 * rest of the statement unexecuted).
	 *
	 * @param node the comparison operation
	 */
	@Override
	public void visit( BoxComparisonOperation node ) {
		// Left continues the running span.
		node.getLeft().accept( this );

		// Right breaks into its own span unless it's a literal that cannot throw.
		// Use the LEFTMOIST start of the right operand — an invocation reports its
		// start at the trailing call parens, AFTER its own callee; closing at that
		// point would produce an inverted span when the callee's body opens earlier.
		// Do NOT unwrap parens: a `( expr )` right operand's span starts at the `(`
		// so the whole parenthesized expression is GREEN/RED together.
		BoxExpression right = node.getRight();
		if ( couldThrow( right ) ) {
			closeRunningSpan( leftmostStart( right ) );
		}
		right.accept( this );
	}

	/**
	 * A string concatenation ({@code a & b & c}) is a list of parts; each part runs
	 * arbitrary logic and may throw. The first part continues the running span;
	 * every later part breaks into its own span.
	 *
	 * @param node the string concat
	 */
	@Override
	public void visit( BoxStringConcat node ) {
		List<BoxExpression> parts = node.getValues();
		if ( parts.isEmpty() ) {
			return;
		}
		parts.get( 0 ).accept( this );
		for ( int i = 1; i < parts.size(); i++ ) {
			// Do NOT unwrap parens: the parens are part of the operand's span. A
			// `( expr )` operand's span must start at the `(`, so the WHOLE parenthesized
			// expression is GREEN/RED together — not with the `(` left in the running span.
			BoxExpression part = parts.get( i );
			if ( couldThrow( part ) ) {
				closeRunningSpan( leftmostStart( part ) );
			}
			part.accept( this );
		}
	}

	/**
	 * A binary operation's operands each run arbitrary logic and may throw — if
	 * one throws, everything after it in the statement never runs. So the left
	 * operand continues the running span, and the RIGHT operand breaks into its
	 * own span whenever it MAY NOT RUN.
	 * <p>
	 * The right may not run for two reasons:
	 * <ul>
	 * <li><b>Short-circuit</b> ({@code &&} / {@code ||} / {@code ?:}): the right
	 * is skipped entirely when the left decides the result, so it must be its own
	 * span — marked only when the left falls through to it.</li>
	 * <li><b>Throw</b>: a right operand that can throw (an identifier, call,
	 * property access, ...) aborts the rest of the statement, so anything after it
	 * never runs.</li>
	 * </ul>
	 * A literal right operand of a non-short-circuiting operator cannot throw and
	 * always runs, so it stays fused into the running span.
	 *
	 * @param node the binary operation
	 */
	@Override
	public void visit( BoxBinaryOperation node ) {
		// An OPEN-ENDED range (`..10` or `1..`) models the missing bound as a null
		// operand. That absent bound has no source span and can't emit bytecode, so
		// we visit whatever operand IS present and skip the null side.
		BoxExpression	left	= node.getLeft();
		BoxExpression	right	= node.getRight();
		if ( left == null && right == null ) {
			return;
		}

		// Left continues the running span.
		if ( left != null ) {
			left.accept( this );
		}

		// If there's no right operand, there is nothing more to split out.
		if ( right == null ) {
			return;
		}

		// Right breaks into its own span when it may not run. Do NOT unwrap parens:
		// a `( expr )` right operand's span starts at the `(` so the WHOLE
		// parenthesized expression is GREEN/RED together (the parens are part of the
		// expression, not the always-run head).
		BoxBinaryOperator	op				= node.getOperator();
		// Short-circuit operators (&&, ||, ?:) skip the right operand when the
		// left decides the result — so the right must be its own span, marked only
		// when actually reached.
		boolean				shortCircuit	= op == BoxBinaryOperator.And || op == BoxBinaryOperator.Or || op == BoxBinaryOperator.Elvis;
		int					headId			= -1;
		if ( shortCircuit || couldThrow( right ) ) {
			headId = closeRunningSpan( leftmostStart( right ) );
		}
		right.accept( this );
		// TAG context: a self-closing tag like `<bx:set x = a && b>` must not glue
		// its closing ">" onto the right operand (which may be short-circuited and
		// show RED). If the right operand is the LAST expression before the tag's
		// ">", register the ">" as its own span grouped with the always-run head so
		// it is GREEN. Only the OUTERMOST operand ends immediately (whitespace-only)
		// before the ">" — nested operands have more expression text after them.
		if ( isTagContext( node ) && headId >= 0 ) {
			Point	rightEnd	= leftmostEnd( right );
			Point	tagClose	= tagCloseAfterWhitespaceOnly( node, rightEnd );
			if ( tagClose != null ) {
				closeRunningSpan( rightEnd );
				int closeId = addSpan( rightEnd, new Point( tagClose.getLine(), tagClose.getColumn() + 1 ) );
				if ( closeId >= 0 ) {
					transpiler.registerSpanGroup( new int[] { headId, closeId } );
				}
				this.runningStart = null;
			}
		}
	}

	/**
	 * A parenthesis is transparent: it wraps an inner expression that always runs
	 * (when the parent node runs). Thread the running span through it so the paren
	 * itself never becomes a span boundary — the inner expression's own start
	 * becomes the boundary, which is what Pass B's transform hook can claim.
	 *
	 * @param node the parenthesis
	 */
	@Override
	public void visit( BoxParenthesis node ) {
		node.getExpression().accept( this );
	}

	/**
	 * A function declaration always runs (UDFs are hoisted): the shell text
	 * (modifiers, name, parens, commas, arg names/types) executes at declaration
	 * time. Arg DECLARATIONS are NOT statements — the generic statement hook must
	 * not wrap them in their own span boundaries. Each argument's DEFAULT VALUE is
	 * the only part that may not run at declaration: literal defaults evaluate
	 * inline (they run at declaration, so they stay in the shell span), but
	 * non-literal defaults are compiled to a lazy {@code defaultExpr_N} method
	 * evaluated only when the argument is omitted at call time — so each non-literal
	 * default breaks into its own span (its mark fires at invocation).
	 * <p>
	 * The function BODY statements run at invocation, not declaration; each is a
	 * statement and gets its own span via the generic statement hook, with marks
	 * emitted inside the UDF body method.
	 *
	 * @param node the function declaration
	 */
	@Override
	public void visit( BoxFunctionDeclaration node ) {
		// The declaration shell runs at declaration time: thread the running span
		// through each argument declaration. Each non-literal default breaks into
		// its OWN isolated span (compiled to a lazy defaultExpr_N method marked
		// only when the arg is omitted at call time): break the shell right before
		// it, thread it, then close right after it so the following shell text (and
		// the default itself) are not fused into one span.
		//
		// The HEAD + INTERSTITIAL + TAIL shell fragments (literal args, commas,
		// parens around/after the isolated defaults) run atomically at declaration
		// but have no AST node to carry a mark. They are grouped and registered so
		// the transformer can emit ONE varargs mark(fileId, ...shell) for the shell.
		preCollectShell( node );
		if ( isTagContext( node ) ) {
			// TAG functions (<cffunction>/<bx:function>): the shell is just the
			// OPEN TAG, ending at its ">". Close it FIRST (before any arguments),
			// so the whitespace between the open tag and the first <cfargument>
			// is never covered. Each <cfargument> is then its OWN span below.
			Point openTagEnd = findTagCloseAfter( node, node.getStart() );
			if ( openTagEnd != null ) {
				closeRunningSpan( new Point( openTagEnd.getLine(), openTagEnd.getColumn() + 1 ) );
				this.runningStart = null;
			}
		}
		for ( BoxArgumentDeclaration arg : node.getArgs() ) {
			// TAG functions (<cffunction>/<bx:function>): each <cfargument> tag is
			// its OWN executable span (open at its tag start, close at its end),
			// so the whitespace around it is not covered.
			if ( isTagContext( node ) ) {
				if ( arg.getStart() != null ) {
					this.runningStart = arg.getStart();
					closeRunningSpan( arg.getEnd() );
					this.runningStart = null;
				}
				continue;
			}
			BoxExpression defaultValue = arg.getValue();
			if ( defaultValue != null && couldThrow( defaultValue ) ) {
				BoxExpression inner = unwrapParens( defaultValue );
				closeRunningSpan( inner.getStart() );
				inner.accept( this );
				closeRunningSpan( inner.getEnd() );
			}
			// Literal defaults and args with no default continue the shell span.
		}

		// The body statements run at invocation: each breaks into its own span.
		// The FIRST break closes the shell TAIL span — but NOT including the body's
		// opening "{" (that belongs to the body, batch-marked at invocation with the
		// closing "}"). Still part of the atomic shell, so capture it before
		// registering the group. Then the consumer is removed so the body spans are
		// NOT swept into the shell group.
		//
		// TAG functions (<cffunction>/<bx:function>) have no braces: the shell is
		// the open tag + any <cfargument> tags, and it must close right after the
		// LAST ARGUMENT — NOT at the first body statement. Closing at the body
		// start would swallow the whitespace between the arguments and the body
		// into the shell span, falsely claiming the body's line as run.
		// A function's body braces are a GROUP, batch-marked when the body
		// EXECUTES (invocation). For a NON-EMPTY body the opening "{" is found by
		// scanning back from the FIRST BODY STATEMENT (which lives after the "{").
		// For an EMPTY body (`function foo() {}`) there is no body statement to
		// anchor from, so node.getEnd() points at/past the closing "}" — scanning
		// back from it fails on the "}". Find the "{...}" pair directly instead.
		Point openBrace = node.getBody() != null && !node.getBody().isEmpty()
		    ? findOpenBraceBefore( node, node.getBody().get( 0 ).getStart() )
		    : ( node.getBody() != null ? findEmptyBodyOpenBrace( node ) : null );
		if ( openBrace != null ) {
			closeRunningSpan( openBrace );
		} else if ( isTagContext( node ) ) {
			// TAG function (<cffunction>/<bx:function>): no braces. The shell is
			// just the OPEN TAG, ending at its ">" — each <cfargument> is its own
			// span (registered above), so the whitespace between the open tag and
			// the first argument must NOT be covered.
			Point openTagEnd = findTagCloseAfter( node, node.getStart() );
			if ( openTagEnd != null ) {
				closeRunningSpan( new Point( openTagEnd.getLine(), openTagEnd.getColumn() + 1 ) );
			}
		} else if ( node.getBody() != null && !node.getBody().isEmpty() ) {
			closeRunningSpan( node.getBody().get( 0 ).getStart() );
		} else if ( node.getBody() == null && !isTagContext( node ) ) {
			// ABSTRACT method (no body, SCRIPT syntax — e.g. `function name();` in
			// an interface, with NO braces): the DECLARATION TEXT is static
			// metadata — it runs at class/interface load (the runtime registers the
			// method signature). Register the whole declaration as ONE span so it
			// can be batch-marked GREEN when the class/interface loads (grouped
			// with the shell by the caller). A tag abstract (<cffunction />) is
			// handled by its open/close tags; an empty-body concrete function
			// (`function foo() {}`) has braces and takes the openBrace path above.
			if ( node.getEnd() != null && node.getStart() != null ) {
				closeRunningSpan( new Point( node.getEnd().getLine(), node.getEnd().getColumn() ) );
			}
		}
		postCollectAndRegisterShell();
		// Capture the shell HEAD id NOW (before body statements may collect nested
		// shells) so the tag close can group with the declaration shell.
		int shellHead = shellHeadId();

		if ( node.getBody() != null ) {
			for ( BoxStatement stmt : node.getBody() ) {
				this.runningStart = stmt.getStart();
				stmt.accept( this );
				closeRunningSpan( stmt.getEnd() );
			}
		}

		// The body braces are batch-marked when the body EXECUTES (invocation), not
		// at declaration. Register "{" + "}" as a group keyed by the "{".
		if ( openBrace != null ) {
			Point closeBrace = node.getEnd() == null ? null : new Point( node.getEnd().getLine(), node.getEnd().getColumn() - 1 );
			registerOpenCloseBraceGroup( openBrace, closeBrace );
		}

		// Tag-based function (<bx:function ...>...</bx:function>): no braces — the
		// shell's last span and the closing </bx:function> tag are marked together
		// at declaration. The shell tail span (from the shell group) is the entry.
		if ( isTagContext( node ) && openBrace == null ) {
			Point afterBody = node.getBody() != null && !node.getBody().isEmpty() && node.getBody().get( node.getBody().size() - 1 ).getEnd() != null
			    ? node.getBody().get( node.getBody().size() - 1 ).getEnd()
			    : node.getEnd();
			if ( node.getBody() == null || node.getBody().isEmpty() ) {
				// Abstract/empty TAG function (<cffunction ...></cffunction>): no
				// body, so node.getEnd() is AT (or past) the close tag — scanning
				// FORWARD from it can't find the close. Scan BACKWARD from the end
				// to register the close tag and group it with the declaration shell.
				registerTagCloseBackward( node, node.getEnd(), "function", shellHead );
			} else {
				registerTagClose( node, afterBody, "function", shellHead );
			}
		}

		// Reset so the enclosing statement close doesn't mint a phantom span over
		// the trailing ";" / newline before the closing brace.
		this.runningStart = null;
	}

	/**
	 * The registered span id of the function's shell HEAD (the first span of the
	 * declaration shell group), used to group the tag close with the declaration.
	 *
	 * @return the shell head span id, or -1 if none was captured
	 */
	private int shellHeadId() {
		if ( !shellAccumulator.isEmpty() ) {
			return shellAccumulator.get( 0 );
		}
		return -1;
	}

	/**
	 * A closure declaration is an expression that produces a {@code Closure} at
	 * declaration time: the shell text (parens, arg names/types, literal defaults)
	 * runs when the closure is CREATED (like a function declaration shell). Each
	 * non-literal default breaks into its own span (lazy {@code defaultExpr_N},
	 * marked only when the arg is omitted at call time). The body is a single
	 * statement that runs at INVOCATION — it breaks into its own span with its
	 * mark inside the closure invoker method.
	 *
	 * @param node the closure
	 */
	@Override
	public void visit( BoxClosure node ) {
		visitClosureLike( node.getArgs(), node.getBody() );
	}

	/**
	 * A lambda declaration is identical to a closure in span structure (shell +
	 * non-literal defaults + invocation-only body) — see
	 * {@link #visit(BoxClosure)}.
	 *
	 * @param node the lambda
	 */
	@Override
	public void visit( BoxLambda node ) {
		visitClosureLike( node.getArgs(), node.getBody() );
	}

	/**
	 * Shared span walk for closure/lambda argument lists and bodies.
	 *
	 * @param args the argument declarations (shell threading)
	 * @param body the single body statement (invocation-only span)
	 */
	private void visitClosureLike( List<BoxArgumentDeclaration> args, BoxStatement body ) {
		// The declaration shell runs when the closure/lambda is created: thread the
		// running span through each argument declaration. Each non-literal default
		// breaks into its OWN isolated span (lazy defaultExpr_N, marked only when
		// the arg is omitted at call time): break before it, thread it, close after
		// it so it isn't fused with the surrounding shell text. The HEAD +
		// INTERSTITIAL + TAIL shell fragments are batched into one varargs mark.
		preCollectShell();
		for ( BoxArgumentDeclaration arg : args ) {
			BoxExpression defaultValue = arg.getValue();
			if ( defaultValue != null && couldThrow( defaultValue ) ) {
				BoxExpression inner = unwrapParens( defaultValue );
				closeRunningSpan( inner.getStart() );
				inner.accept( this );
				closeRunningSpan( inner.getEnd() );
			}
			// Literal defaults and args with no default continue the shell span.
		}

		// The body runs at invocation: breaks into its own span. The FIRST break
		// closes the shell TAIL span — still part of the atomic shell, so capture
		// it before registering the group. Then the consumer is removed so the body
		// is not swept into the shell group.
		if ( body != null ) {
			closeRunningSpan( body.getStart() );
		}
		postCollectAndRegisterShell();

		if ( body != null ) {
			body.accept( this );
		}
	}

	/**
	 * A class declaration's own text emits no bytecode (local classes are
	 * pre-compiled as auxiliary JVM classes and loaded lazily), so there is no
	 * declaration shell span. The class body contains three kinds of executable
	 * code with three different run-times:
	 * <ul>
	 * <li>static {@link BoxFunctionDeclaration}s and {@link BoxStaticInitializer}
	 * bodies — run at CLASS LOAD (staticInitializer method)</li>
	 * <li>non-static body statements — run at INSTANTIATION (_pseudoConstructor)</li>
	 * <li>member {@link BoxFunctionDeclaration}s — run at INVOCATION</li>
	 * <li>property DEFAULT values — literal defaults run at CLASS LOAD (inline in
	 * the properties map), non-literal defaults run lazily on first access</li>
	 * </ul>
	 * For functions, only their BODY statements are executable (the function
	 * shell/declaration emits no bytecode inside a class — it's metadata). Each
	 * body statement opens its own span; marks land in the corresponding
	 * auxiliary-class methods.
	 *
	 * @param node the class
	 */
	@Override
	public void visit( BoxClass node ) {
		// A class body's constructs are written in the class's OWN source type:
		// a box script class uses script syntax, a CF script component uses
		// CFSCRIPT, and a CF TAG component (<cfcomponent>) uses CFTEMPLATE — so
		// its <cfset>/<cffunction>/<cfproperty> tags must register TAG spans, not
		// script spans. Push the class's real source type (falls back to the
		// current context if the parser didn't set one).
		BoxSourceType classType = node.getBoxSourceType();
		this.currentSourceType.push( classType != null ? classType : this.currentSourceType.peek() );
		// The CLASS SHELL (pre-annotations + "class" keyword + post-annotations +
		// opening "{") runs ONCE at class load (clinit), as does the closing "}".
		// Register them as ONE group so they batch-mark when the class is loaded.
		// The shell starts at the FIRST pre-annotation (or the "class" keyword if
		// there are none).
		Point shellStart = null;
		if ( node.getAnnotations() != null && !node.getAnnotations().isEmpty() ) {
			// A CF component's annotations (component attributes) may have a null
			// position — guard and fall back to the class start.
			Point	annStart	= node.getAnnotations().get( 0 ).getPosition() == null
			    ? null
			    : node.getAnnotations().get( 0 ).getPosition().getStart();
			// The shell must start at the EARLIEST source point: the class keyword
			// (`node.getStart()`) OR the first pre-annotation. A post-annotation
			// like `extends=ProfilerSuper` starts AFTER the `class` keyword, so it
			// must not shrink the shell away from the keyword.
			Point	classStart	= node.getStart();
			if ( annStart != null && classStart != null ) {
				shellStart = ( annStart.getLine() < classStart.getLine()
				    || ( annStart.getLine() == classStart.getLine() && annStart.getColumn() < classStart.getColumn() ) )
				        ? annStart
				        : classStart;
			} else if ( annStart != null ) {
				shellStart = annStart;
			}
		}
		if ( shellStart == null ) {
			shellStart = node.getStart();
		}
		// The CLASS SHELL runs at class load. For a SCRIPT class the shell is the
		// header text through the opening "{"; for a TAG class (<cfcomponent> /
		// <bx:component>) it is the OPEN tag. The opening brace is found by a
		// FORWARD scan from the class keyword — a backward scan aborts at the
		// first property declaration's ";" (properties sit between the "{" and the
		// first body statement), losing the outer class shell entirely.
		boolean	isTagClass	= isTemplate();
		int		shellId		= -1;
		if ( isTagClass ) {
			// Tag class: the open tag <cfcomponent ...> is the shell header.
			Point tagEnd = findTagCloseAfter( node, node.getStart() );
			if ( tagEnd != null && shellStart != null ) {
				this.runningStart	= shellStart;
				shellId				= closeRunningSpan( new Point( tagEnd.getLine(), tagEnd.getColumn() + 1 ) );
			}
			// The </cfcomponent> close tag is grouped with the open tag.
			if ( shellId >= 0 ) {
				Point closeOpen = findTagCloseOpen( node, node.getBody() == null || node.getBody().isEmpty()
				    ? node.getEnd()
				    : node.getBody().get( node.getBody().size() - 1 ).getEnd(), "component" );
				if ( closeOpen != null ) {
					Point closeEnd = findTagCloseAfter( node, closeOpen );
					if ( closeEnd != null ) {
						int closeId = addSpan( closeOpen, new Point( closeEnd.getLine(), closeEnd.getColumn() + 1 ) );
						if ( closeId >= 0 ) {
							transpiler.registerSpanGroup( new int[] { shellId, closeId } );
						}
					}
				}
			}
		} else {
			// Script class: header text through the opening "{", grouped with "}".
			Point openBrace = node.getBody() == null || node.getBody().isEmpty()
			    ? findOpenBraceAfter( node, shellStart )
			    : findOpenBraceAfter( node, shellStart );
			if ( openBrace != null && shellStart != null ) {
				this.runningStart	= shellStart;
				// Close AFTER the "{" so the opening brace is part of the shell span.
				shellId				= closeRunningSpan( new Point( openBrace.getLine(), openBrace.getColumn() + 1 ) );
			}
			Point closeBrace = node.getEnd() == null ? null : new Point( node.getEnd().getLine(), node.getEnd().getColumn() - 1 );
			if ( shellId >= 0 ) {
				registerBraceGroup( shellId, closeBrace );
			}
		}

		// Abstract method shells become static metadata of the class, like property
		// declarations — collect them to merge with the class shell (marked GREEN at
		// load). Also captures the tag </cffunction> close for abstract tag methods.
		List<Integer> abstractShells = new ArrayList<>();
		for ( BoxStatement stmt : node.getBody() ) {
			if ( stmt instanceof BoxFunctionDeclaration func ) {
				// Member/static function: use the FULL function logic (shell + body
				// braces + invocation-time body) so members behave like UDFs.
				boolean isAbstract = func.getBody() == null;
				this.runningStart = func.getStart();
				visit( func );
				if ( isAbstract ) {
					// Abstract fn: no body, so it emits no invoker mark. Consume its
					// shell group (incl. tag close) and merge it with the shell group.
					int head = shellHeadId();
					if ( head >= 0 ) {
						int[] group = transpiler.takeSpanGroup( head );
						if ( group != null ) {
							for ( int g : group ) {
								if ( !abstractShells.contains( g ) ) {
									abstractShells.add( g );
								}
							}
						} else if ( !abstractShells.contains( head ) ) {
							abstractShells.add( head );
						}
					}
				}
			} else if ( stmt instanceof BoxStaticInitializer init ) {
				// Static initializer: the "static {" header + closing "}" are a
				// group, batch-marked at static load (Pass B emits at static init).
				// The header span includes the OPENING "{" (close AFTER it), so the
				// "static {" text is one span — the "{" is executable markup too.
				Point	initOpen	= findOpenBraceBefore( node, init.getBody().isEmpty() ? init.getEnd() : init.getBody().get( 0 ).getStart() );
				Point	initClose	= init.getEnd() == null ? null : new Point( init.getEnd().getLine(), init.getEnd().getColumn() - 1 );
				int		initHeader	= -1;
				this.runningStart = init.getStart();
				if ( initOpen != null ) {
					initHeader = closeRunningSpan( new Point( initOpen.getLine(), initOpen.getColumn() + 1 ) );
				}
				for ( BoxStatement s : init.getBody() ) {
					this.runningStart = s.getStart();
					s.accept( this );
					closeRunningSpan( s.getEnd() );
				}
				if ( initHeader >= 0 && initClose != null ) {
					registerBraceGroup( initHeader, initClose );
				}
				this.runningStart = null;
			} else {
				// A body statement runs at instantiation: open its own span.
				this.runningStart = stmt.getStart();
				stmt.accept( this );
				closeRunningSpan( stmt.getEnd() );
			}
		}
		// Property default values are expressions; each may run (literal at class
		// load, complex lazily on first access). They are HOISTED — the whole set
		// is applied by defaultProperties() in the pseudo-constructor — so collect
		// them as ONE atomic span group emitted as a single varargs mark there.
		// The property DECLARATION text (everything before the default value, e.g.
		// `property name="threshold" default=`) is metadata applied at class load —
		// its span is grouped with the CLASS SHELL so it is always GREEN.
		List<Integer>	propDefaults	= new ArrayList<>();
		List<Integer>	propTexts		= new ArrayList<>();
		List<Integer>	propCloses		= new ArrayList<>();
		shellSpanConsumers.add( ( spanId, start ) -> propDefaults.add( spanId ) );
		for ( BoxProperty prop : node.getProperties() ) {
			visitPropertyDefaults( prop, propTexts, propCloses );
		}
		shellSpanConsumers.remove( shellSpanConsumers.size() - 1 );
		if ( !propDefaults.isEmpty() ) {
			transpiler.registerPropertyDefaultSpans( propDefaults.stream().mapToInt( Integer::intValue ).toArray() );
		}
		// Group the property-declaration text spans (and their closing ">")
		// with the class shell header so they are marked (GREEN) when the class
		// loads. Merge with any EXISTING shell group (e.g. the tag class's
		// </cfcomponent> close) so it is not lost.
		if ( shellId >= 0 && ( !propTexts.isEmpty() || !propCloses.isEmpty() ) ) {
			List<Integer>	group		= new ArrayList<>();
			int[]			existing	= transpiler.peekSpanGroup( shellId );
			if ( existing != null ) {
				for ( int e : existing ) {
					group.add( e );
				}
			} else {
				group.add( shellId );
			}
			for ( int pid : propTexts ) {
				if ( !group.contains( pid ) ) {
					group.add( pid );
				}
			}
			for ( int pid : propCloses ) {
				if ( !group.contains( pid ) ) {
					group.add( pid );
				}
			}
			transpiler.registerSpanGroup( group.stream().mapToInt( Integer::intValue ).toArray() );
		}
		// Merge abstract-method shells into the class SHELL group so their
		// declarations (static metadata) are marked GREEN when the class loads.
		if ( shellId >= 0 && !abstractShells.isEmpty() ) {
			List<Integer>	group		= new ArrayList<>();
			int[]			existing	= transpiler.peekSpanGroup( shellId );
			if ( existing != null ) {
				for ( int e : existing ) {
					if ( !group.contains( e ) ) {
						group.add( e );
					}
				}
			} else {
				group.add( shellId );
			}
			for ( int s : abstractShells ) {
				if ( !group.contains( s ) ) {
					group.add( s );
				}
			}
			transpiler.registerSpanGroup( group.stream().mapToInt( Integer::intValue ).toArray() );
		}

		// Reset so the enclosing statement close doesn't mint a phantom span over
		// the trailing ";" / newline after the class.
		this.runningStart = null;
		this.currentSourceType.pop();
	}

	/**
	 * Visit an interface declaration. Interfaces hold executable spans in their
	 * SHELL (the {@code interface ... { } header, marked once at interface load),
	 * their STATIC INITIALIZER (header + closing brace, batch-marked at static
	 * load), and their STATIC functions and DEFAULT methods (shell + body, marked
	 * at declaration and again when invoked). Abstract members have no body and
	 * hence no spans. Mirrors {@link #visit(BoxClass)} but without properties
	 * (interfaces declare none).
	 *
	 * @param node the interface node
	 */
	public void visit( BoxInterface node ) {
		// Push the interface's real source type (script vs CFSCRIPT vs CFTEMPLATE)
		// so its body constructs register the right span flavor.
		BoxSourceType interfaceType = node.getBoxSourceType();
		this.currentSourceType.push( interfaceType != null ? interfaceType : this.currentSourceType.peek() );

		// The INTERFACE SHELL (pre-annotations + "interface" keyword +
		// post-annotations + opening "{") runs ONCE at interface load (clinit), as
		// does the closing "}". Register them as ONE group so they batch-mark when
		// the interface is loaded. The shell starts at the first pre-annotation
		// (or the "interface" keyword if there are none).
		Point shellStart = node.getStart();
		if ( node.getAnnotations() != null && !node.getAnnotations().isEmpty() ) {
			Point	annStart		= node.getAnnotations().get( 0 ).getPosition() == null
			    ? null
			    : node.getAnnotations().get( 0 ).getPosition().getStart();
			Point	interfaceStart	= node.getStart();
			if ( annStart != null && interfaceStart != null ) {
				shellStart = ( annStart.getLine() < interfaceStart.getLine()
				    || ( annStart.getLine() == interfaceStart.getLine()
				        && annStart.getColumn() < interfaceStart.getColumn() ) )
				            ? annStart
				            : interfaceStart;
			} else if ( annStart != null ) {
				shellStart = annStart;
			}
		}

		// Tag-based interfaces (<cfinterface>) use an open tag as their shell;
		// script interfaces use the "@interface ... {" header.
		int shellId = -1;
		if ( isTemplate() ) {
			Point tagEnd = findTagCloseAfter( node, node.getStart() );
			if ( tagEnd != null && shellStart != null ) {
				this.runningStart	= shellStart;
				shellId				= closeRunningSpan( new Point( tagEnd.getLine(), tagEnd.getColumn() + 1 ) );
			}
			if ( shellId >= 0 ) {
				Point closeOpen = findTagCloseOpen( node,
				    node.getBody() == null || node.getBody().isEmpty() ? node.getEnd() : node.getBody().get( node.getBody().size() - 1 ).getEnd(),
				    "interface" );
				if ( closeOpen != null ) {
					Point closeEnd = findTagCloseAfter( node, closeOpen );
					if ( closeEnd != null ) {
						int closeId = addSpan( closeOpen, new Point( closeEnd.getLine(), closeEnd.getColumn() + 1 ) );
						if ( closeId >= 0 ) {
							transpiler.registerSpanGroup( new int[] { shellId, closeId } );
						}
					}
				}
			}
		} else {
			// Script interface: header text through the opening "{", grouped with "}".
			Point openBrace = findOpenBraceAfter( node, shellStart );
			if ( openBrace != null && shellStart != null ) {
				this.runningStart	= shellStart;
				shellId				= closeRunningSpan( new Point( openBrace.getLine(), openBrace.getColumn() + 1 ) );
			}
			Point closeBrace = node.getEnd() == null ? null : new Point( node.getEnd().getLine(), node.getEnd().getColumn() - 1 );
			if ( shellId >= 0 ) {
				registerBraceGroup( shellId, closeBrace );
			}
		}

		// Body: static functions, default methods, and static initializers carry
		// runnable spans. Abstract member declarations have no body — their SHELLS
		// (and tag close, if any) become static metadata of the interface, so they
		// are grouped with the interface shell and marked GREEN at load, exactly
		// like a property declaration.
		List<Integer> abstractShells = new ArrayList<>();
		for ( BoxStatement stmt : node.getBody() ) {
			if ( stmt instanceof BoxFunctionDeclaration func ) {
				boolean isAbstract = func.getBody() == null;
				// Static function or DEFAULT method: use the full function logic
				// (shell + body braces + invocation-time body).
				this.runningStart = func.getStart();
				visit( func );
				if ( isAbstract ) {
					// Abstract fn: no body, so it emits no invoker mark. Consume its
					// shell group and merge it with the interface shell group so
					// the declaration is marked GREEN when the interface loads.
					int head = shellHeadId();
					if ( head >= 0 ) {
						int[] group = transpiler.takeSpanGroup( head );
						if ( group != null ) {
							for ( int g : group ) {
								if ( !abstractShells.contains( g ) ) {
									abstractShells.add( g );
								}
							}
						} else if ( !abstractShells.contains( head ) ) {
							abstractShells.add( head );
						}
					}
				}
			} else if ( stmt instanceof BoxStaticInitializer init ) {
				// Static initializer: "static {" header + closing "}" group,
				// batch-marked at static load. Header span includes the "{".
				Point	initOpen	= findOpenBraceBefore( node, init.getBody().isEmpty() ? init.getEnd() : init.getBody().get( 0 ).getStart() );
				Point	initClose	= init.getEnd() == null ? null : new Point( init.getEnd().getLine(), init.getEnd().getColumn() - 1 );
				int		initHeader	= -1;
				this.runningStart = init.getStart();
				if ( initOpen != null ) {
					initHeader = closeRunningSpan( new Point( initOpen.getLine(), initOpen.getColumn() + 1 ) );
				}
				for ( BoxStatement s : init.getBody() ) {
					this.runningStart = s.getStart();
					s.accept( this );
					closeRunningSpan( s.getEnd() );
				}
				if ( initHeader >= 0 && initClose != null ) {
					registerBraceGroup( initHeader, initClose );
				}
				this.runningStart = null;
			} else if ( stmt instanceof BoxImport ) {
				// imports carry no executable span — skip.
			} else {
				throw new BoxRuntimeException(
				    "Unsupported interface body statement for span collection: " + stmt.getClass().getSimpleName() );
			}
		}

		// Merge any abstract-method shells into the interface SHELL group so they
		// are batch-marked GREEN when the interface loads (their declarations are
		// static metadata, like property declarations).
		if ( shellId >= 0 && !abstractShells.isEmpty() ) {
			List<Integer>	group		= new ArrayList<>();
			int[]			existing	= transpiler.peekSpanGroup( shellId );
			if ( existing != null ) {
				for ( int e : existing ) {
					if ( !group.contains( e ) ) {
						group.add( e );
					}
				}
			} else {
				group.add( shellId );
			}
			for ( int s : abstractShells ) {
				if ( !group.contains( s ) ) {
					group.add( s );
				}
			}
			transpiler.registerSpanGroup( group.stream().mapToInt( Integer::intValue ).toArray() );
		}

		// Reset so the enclosing statement close doesn't mint a phantom span over
		// the trailing ";" / newline after the interface.
		this.runningStart = null;
		this.currentSourceType.pop();
	}

	/**
	 * Visit a property declaration. The declaration TEXT (the {@code property}
	 * keyword, its name/type/annotations) emits no bytecode — it's metadata applied
	 * at class load — so the WHOLE declaration text is registered as ONE span and
	 * grouped with the class shell so it is always GREEN. The only attribute with
	 * special treatment is {@code default}: its VALUE is an expression that
	 * executes (literal inline at class load, non-literal lazily on first access),
	 * so it opens its OWN span. Every other attribute stays in the declaration-text
	 * span.
	 *
	 * @param prop      the property
	 * @param propTexts collector for the property-declaration-text span ids
	 */
	private void visitPropertyDefaults( BoxProperty prop, List<Integer> propTexts, List<Integer> propCloses ) {
		Point	propStart	= prop.getStart();
		Point	propEnd		= prop.getEnd();
		if ( propStart == null ) {
			return;
		}

		// Find the default value expression, if any.
		BoxExpression defaultValue = null;
		for ( var annotation : prop.getAllAnnotations() ) {
			if ( annotation.getKey().getValue().equalsIgnoreCase( "default" ) && annotation.getValue() != null ) {
				defaultValue = annotation.getValue();
				break;
			}
		}

		// The declaration-text span runs from the property start to the start of the
		// default value, or to the end of the declaration if there's no default.
		Point textEnd = propEnd;
		if ( defaultValue != null ) {
			Point valueStart = unwrapParens( defaultValue ).getStart();
			if ( valueStart != null ) {
				textEnd = valueStart;
			}
		}
		int textId = addSpan( propStart, textEnd );
		if ( textId >= 0 ) {
			propTexts.add( textId );
		}

		// If there IS a default value, it opens its own span (it may or may not run).
		if ( defaultValue != null ) {
			BoxExpression inner = unwrapParens( defaultValue );
			this.runningStart = inner.getStart();
			inner.accept( this );
			closeRunningSpan( inner.getEnd() );
			// The tag's closing ">" (e.g. `...#( 40 + 2 )#>`) is metadata markup
			// applied at class load — register it so it is GREEN (not left
			// untracked). Its id is collected into propCloses and grouped with
			// the class shell (NOT a group keyed at the property text — that
			// group never fires because the shell mark takes the shellId group).
			Point tagClose = findTagCloseAfter( prop, inner.getEnd() );
			if ( tagClose != null ) {
				int closeId = addSpan( inner.getEnd(), new Point( tagClose.getLine(), tagClose.getColumn() + 1 ) );
				if ( closeId >= 0 ) {
					propCloses.add( closeId );
				}
			}
		}
	}

	/**
	 * Visit a class function's BODY statements only (no shell — a class member
	 * declaration emits no bytecode). Each body statement opens its own span.
	 *
	 * @param func the function declaration
	 */
	private void visitFunctionBody( BoxFunctionDeclaration func ) {
		if ( func.getBody() != null ) {
			for ( BoxStatement stmt : func.getBody() ) {
				this.runningStart = stmt.getStart();
				stmt.accept( this );
				closeRunningSpan( stmt.getEnd() );
			}
		}
	}

	/**
	 * A local class (inner class in a script/template) has the same span structure
	 * as a top-level class — see {@link #visit(BoxClass)}.
	 *
	 * @param node the local class
	 */
	@Override
	public void visit( BoxLocalClass node ) {
		visit( ( BoxClass ) node );
	}

	/**
	 * A static initializer block runs when the class is loaded. Each body statement
	 * breaks into its own span (marks emitted in the staticInitializer method).
	 *
	 * @param node the static initializer
	 */
	@Override
	public void visit( BoxStaticInitializer node ) {
		this.runningStart = null;
		for ( BoxStatement stmt : node.getBody() ) {
			this.runningStart = stmt.getStart();
			stmt.accept( this );
			closeRunningSpan( stmt.getEnd() );
		}
	}

	/**
	 * Whether evaluating this operand could throw an exception, aborting the rest
	 * of the statement. A literal (scalar or a compound whose every sub-expression
	 * is a literal) cannot throw and therefore does not break the current span;
	 * anything else — an identifier, property access, cast, or call — can.
	 *
	 * @param expr the operand
	 *
	 * @return true if the operand could throw when evaluated
	 */
	private boolean couldThrow( BoxExpression expr ) {
		return !expr.isLiteral();
	}

	/**
	 * Unwrap any outer parentheses from an expression, mirroring how the ASM
	 * transformers treat them (transparent). Used so a span boundary lands on the
	 * innermost expression's start — the position Pass B's transform hook claims.
	 *
	 * @param expr the expression
	 *
	 * @return the innermost non-parenthesis expression
	 */
	private BoxExpression unwrapParens( BoxExpression expr ) {
		while ( expr instanceof BoxParenthesis paren ) {
			expr = paren.getExpression();
		}
		return expr;
	}

	/**
	 * The leftmost source start of an expression — the first byte of its text as
	 * written. For most nodes this equals {@code getStart()}, but some nodes (e.g.
	 * {@code BoxExpressionInvocation}) report a start at the trailing call parens,
	 * AFTER their own leading content (the invoked callee). When a concat/binary
	 * operand wraps such a node, splitting at {@code getStart()} would place the
	 * boundary past the operand's own callee, so the recursive visit of that
	 * earlier callee would emit an inverted span. Resolving the true leftmost
	 * start (scanning descendants) keeps spans in source order.
	 *
	 * @param expr the expression
	 *
	 * @return the leftmost (minimum) start point across the expression subtree, or
	 *         the expression's own start if it has no children
	 */
	private Point leftmostStart( BoxExpression expr ) {
		Point best = expr.getStart();
		if ( best == null ) {
			return null;
		}
		for ( BoxNode child : expr.getChildren() ) {
			if ( child instanceof BoxExpression childExpr ) {
				Point candidate = leftmostStart( childExpr );
				if ( candidate != null && ( candidate.getLine() < best.getLine()
				    || ( candidate.getLine() == best.getLine() && candidate.getColumn() < best.getColumn() ) ) ) {
					best = candidate;
				}
			}
		}
		return best;
	}

	/**
	 * The rightmost (maximum) END point across an expression's subtree. Used where
	 * a span must extend through the LAST source character of the whole operand
	 * (e.g. the closing paren of a parenthesized ternary branch).
	 *
	 * @param expr the expression
	 *
	 * @return the rightmost end point, or the expression's own end if none
	 */
	private Point leftmostEnd( BoxExpression expr ) {
		Point best = expr.getEnd();
		if ( best == null ) {
			return null;
		}
		for ( BoxNode child : expr.getChildren() ) {
			if ( child instanceof BoxExpression childExpr ) {
				Point candidate = leftmostEnd( childExpr );
				if ( candidate != null && ( candidate.getLine() > best.getLine()
				    || ( candidate.getLine() == best.getLine() && candidate.getColumn() > best.getColumn() ) ) ) {
					best = candidate;
				}
			}
		}
		return best;
	}

	/**
	 * Close the currently-open running span, ending it just before the given point.
	 * Any region between the running start and the break point becomes one span.
	 * If no running span is open, the break point starts a fresh one.
	 *
	 * @param breakPoint the point where a new span begins
	 *
	 * @return the registered span id of the span just closed, or -1 if none was
	 *         closed (e.g. a null break point or no running span)
	 */
	private int closeRunningSpan( Point breakPoint ) {
		int spanId = -1;
		if ( this.runningStart != null && breakPoint != null ) {
			spanId = addSpan( this.runningStart, breakPoint );
		}
		this.runningStart = breakPoint;
		return spanId;
	}

	/**
	 * Register an executable span (keyed by its start position) and add its
	 * {@link Blueprint.SpanDef}. If either point is null (manual, unsourced node),
	 * no span is emitted.
	 * <p>
	 * A span may NEVER cross a comment-only or blank line — comments are not
	 * executable and must never be part of a span. If {@code [start, end]} crosses
	 * such a line, the span is SPLIT into comment-free pieces, each registered as
	 * its own span and batched into ONE group keyed by the first piece, so the
	 * single mark Pass B resolves at {@code start} covers every piece.
	 *
	 * @param start the span's start point (may be null)
	 * @param end   the span's end point (may be null)
	 *
	 * @return the registered span id (the id of the FIRST piece, i.e. the group
	 *         head that Pass B resolves), or -1 if no span was registered
	 */
	/**
	 * Register an executable span (keyed by its start position) and add its
	 * {@link Blueprint.SpanDef}. If either point is null (manual, unsourced node),
	 * no span is emitted.
	 *
	 * @param start the span's start point (may be null)
	 * @param end   the span's end point (may be null)
	 *
	 * @return the registered span id, or -1 no span was registered
	 */
	private int addSpan( Point start, Point end ) {
		if ( start == null || end == null ) {
			return -1;
		}
		// Skip zero-width spans (same start/end point) — they cover no source and
		// cannot be marked meaningfully.
		if ( start.getLine() == end.getLine() && start.getColumn() == end.getColumn() ) {
			return -1;
		}
		long packed = ( ( long ) start.getLine() << 32 ) | ( start.getColumn() & 0xFFFFFFFFL );
		// Dedup: a span already registered at this start key is returned as-is (no
		// duplicate SpanDef). Post-registration back-references that hit the same
		// start (e.g. a tag close seen from multiple nested constructs) reuse it.
		if ( registeredSpanStarts.contains( packed ) ) {
			return transpiler.getSpanId( packed );
		}
		registeredSpanStarts.add( packed );
		int spanId = transpiler.registerSpan( packed );
		spanDefs.add( new Blueprint.SpanDef( start.getLine(), start.getColumn(), end.getLine(), end.getColumn(), true ) );
		for ( SpanConsumer consumer : shellSpanConsumers ) {
			consumer.accept( spanId, start );
		}
		return spanId;
	}

	/** Consumer invoked whenever a span is added, used to capture shell span ids. */
	@FunctionalInterface
	private interface SpanConsumer {

		void accept( int spanId, Point start );
	}

	/** Currently-active consumers for capturing shell-group span ids (LIFO). */
	private final List<SpanConsumer>	shellSpanConsumers		= new ArrayList<>();
	private final List<Integer>			shellAccumulator		= new ArrayList<>();
	private final java.util.Set<Long>	shellLazyDefaultStarts	= new java.util.HashSet<>();

	/**
	 * Begin collecting shell fragments for a declaration, so they can be batched
	 * into one varargs mark at the declaration site. Shell fragments (head,
	 * interstitial, tail around literal args) have no AST node of their own and run
	 * atomically at declaration — unlike the lazy-default spans, which run at call
	 * time and must stay separate.
	 */
	private void preCollectShell() {
		shellAccumulator.clear();
		shellLazyDefaultStarts.clear();
		shellSpanConsumers.add( ( spanId, start ) -> {
			if ( start != null ) {
				long packed = ( ( long ) start.getLine() << 32 ) | ( start.getColumn() & 0xFFFFFFFFL );
				if ( !shellLazyDefaultStarts.contains( packed ) ) {
					shellAccumulator.add( spanId );
				}
			}
		} );
	}

	/**
	 * Begin collecting shell fragments for a function declaration, recording the
	 * lazy-default start positions so those spans are excluded from the shell
	 * group (they run later at call time).
	 */
	private void preCollectShell( BoxFunctionDeclaration node ) {
		// Fill the exclusion set FIRST, then arm the consumer (which clears the set
		// must not run before the defaults are recorded).
		for ( BoxArgumentDeclaration arg : node.getArgs() ) {
			BoxExpression defaultValue = arg.getValue();
			if ( defaultValue != null && couldThrow( defaultValue ) ) {
				Point p = unwrapParens( defaultValue ).getStart();
				if ( p != null ) {
					shellLazyDefaultStarts.add( ( ( long ) p.getLine() << 32 ) | ( p.getColumn() & 0xFFFFFFFFL ) );
				}
			}
		}
		// Now arm the consumer WITHOUT clearing the exclusion set we just built.
		shellAccumulator.clear();
		shellSpanConsumers.add( ( spanId, start ) -> {
			if ( start != null ) {
				long packed = ( ( long ) start.getLine() << 32 ) | ( start.getColumn() & 0xFFFFFFFFL );
				if ( !shellLazyDefaultStarts.contains( packed ) ) {
					shellAccumulator.add( spanId );
				}
			}
		} );
	}

	/**
	 * Finish shell collection and register the group on the transpiler so the
	 * transformer can emit a single varargs {@code mark(fileId, ...shell)}.
	 */
	private void postCollectAndRegisterShell() {
		shellSpanConsumers.remove( shellSpanConsumers.size() - 1 );
		if ( !shellAccumulator.isEmpty() ) {
			transpiler.registerSpanGroup( shellAccumulator.stream().mapToInt( Integer::intValue ).toArray() );
		}
	}

}