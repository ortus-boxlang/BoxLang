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
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.BoxStatement;
import ortus.boxlang.compiler.ast.BoxStaticInitializer;
import ortus.boxlang.compiler.ast.BoxTemplate;
import ortus.boxlang.compiler.ast.Point;
import ortus.boxlang.compiler.ast.expression.BoxAssignment;
import ortus.boxlang.compiler.ast.expression.BoxBinaryOperation;
import ortus.boxlang.compiler.ast.expression.BoxBinaryOperator;
import ortus.boxlang.compiler.ast.expression.BoxClosure;
import ortus.boxlang.compiler.ast.expression.BoxComparisonOperation;
import ortus.boxlang.compiler.ast.expression.BoxLambda;
import ortus.boxlang.compiler.ast.expression.BoxParenthesis;
import ortus.boxlang.compiler.ast.expression.BoxStringConcat;
import ortus.boxlang.compiler.ast.expression.BoxStringInterpolation;
import ortus.boxlang.compiler.ast.expression.BoxStringLiteral;
import ortus.boxlang.compiler.ast.expression.BoxTernaryOperation;
import ortus.boxlang.compiler.ast.statement.BoxAnnotation;
import ortus.boxlang.compiler.ast.statement.BoxArgumentDeclaration;
import ortus.boxlang.compiler.ast.statement.BoxBufferOutput;
import ortus.boxlang.compiler.ast.statement.BoxDo;
import ortus.boxlang.compiler.ast.statement.BoxForIn;
import ortus.boxlang.compiler.ast.statement.BoxForIndex;
import ortus.boxlang.compiler.ast.statement.BoxFunctionDeclaration;
import ortus.boxlang.compiler.ast.statement.BoxIfElse;
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
				closeRunningSpan( stmt.getEnd() );
			} else {
				child.accept( this );
			}
		}
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
	 * A {@code <bx:script>} island inside a template: descend in script mode.
	 *
	 * @param node the script island
	 */
	@Override
	public void visit( BoxScriptIsland node ) {
		this.currentSourceType.push( BoxSourceType.BOXSCRIPT );
		visitChildren( node );
		this.currentSourceType.pop();
	}

	/**
	 * A template island (template content nested inside a script context): descend
	 * in template mode.
	 *
	 * @param node the template island
	 */
	@Override
	public void visit( BoxTemplateIsland node ) {
		this.currentSourceType.push( BoxSourceType.BOXTEMPLATE );
		visitChildren( node );
		this.currentSourceType.pop();
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
						closeRunningSpan( stmt.getEnd() );
					}
					this.runningStart = null;
				} else {
					this.runningStart = node.getStart();
					closeRunningSpan( node.getEnd() );
				}
				return;
			}

			// CONSTRUCT components (<bx:while>, <bx:loop>, <bx:if>, <cfif>, ...):
			// the open tag is the running span, grouped with the close tag.
			if ( node.getBody() != null && !node.getBody().isEmpty() ) {
				int headerId = -1;
				headerId = closeRunningSpan( node.getBody().get( 0 ).getStart() );
				for ( BoxStatement stmt : node.getBody() ) {
					this.runningStart = stmt.getStart();
					stmt.accept( this );
					closeRunningSpan( stmt.getEnd() );
				}
				this.runningStart = null;
				Point afterBody = node.getBody().get( node.getBody().size() - 1 ).getEnd();
				registerTagClose( node, afterBody, name, headerId );
				return;
			}
		}

		// Default: let the generic child walker handle it (each body statement opens
		// its own span; the component tag itself threads the running span).
		visitChildren( node );
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
		boolean broke = false;
		for ( BoxAnnotation attr : node.getAttributes() ) {
			if ( !attr.getKey().getValue().equalsIgnoreCase( "default" ) ) {
				continue;
			}
			BoxExpression value = attr.getValue();
			if ( value == null ) {
				continue;
			}
			// Mirror BoxParamTransformer: a quoted attribute with a single
			// interpolation element unwraps to the raw expression.
			if ( value instanceof BoxStringInterpolation bsi && bsi.getValues().size() == 1 ) {
				value = bsi.getValues().get( 0 );
			}
			if ( couldThrow( value ) ) {
				BoxExpression inner = unwrapParens( value );
				closeRunningSpan( inner.getStart() );
				inner.accept( this );
				closeRunningSpan( inner.getEnd() );
				broke = true;
			}
		}
		// A broken-out default leaves a trailing ";" / ">" that is not executable —
		// reset so the enclosing statement close cannot mint a phantom span over it.
		// A literal default never broke the span: leave runningStart intact so the
		// enclosing statement close still registers the whole statement span.
		if ( broke ) {
			this.runningStart = null;
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
			closeRunningSpan( stmt.getEnd() );
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

		// whenTrue breaks into its own span (may not run).
		BoxExpression whenTrue = unwrapParens( node.getWhenTrue() );
		closeRunningSpan( whenTrue.getStart() );
		whenTrue.accept( this );

		// whenFalse breaks into its own span (may not run).
		BoxExpression whenFalse = unwrapParens( node.getWhenFalse() );
		closeRunningSpan( whenFalse.getStart() );
		whenFalse.accept( this );
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
			BoxExpression inner = unwrapParens( defaultValue );
			closeRunningSpan( inner.getStart() );
			inner.accept( this );
			closeRunningSpan( inner.getEnd() );
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
		// Match "</bx:tagName>" or "</cfTagName>".
		String	close1	= "</bx:" + tagName;
		String	close2	= "</cf" + ( tagName.equalsIgnoreCase( "defaultcase" ) ? "defaultcase" : tagName );
		for ( int i = offset; i < source.length() - close1.length(); i++ ) {
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
		BoxExpression right = unwrapParens( node.getRight() );
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
			BoxExpression part = unwrapParens( parts.get( i ) );
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
		// Left continues the running span.
		node.getLeft().accept( this );

		// Right breaks into its own span when it may not run.
		BoxExpression		right			= unwrapParens( node.getRight() );
		BoxBinaryOperator	op				= node.getOperator();
		// Short-circuit operators (&&, ||, ?:) skip the right operand when the
		// left decides the result — so the right must be its own span, marked only
		// when actually reached.
		boolean				shortCircuit	= op == BoxBinaryOperator.And || op == BoxBinaryOperator.Or || op == BoxBinaryOperator.Elvis;
		if ( shortCircuit || couldThrow( right ) ) {
			closeRunningSpan( leftmostStart( right ) );
		}
		right.accept( this );
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
		for ( BoxArgumentDeclaration arg : node.getArgs() ) {
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
		Point openBrace = node.getBody() != null && !node.getBody().isEmpty()
		    ? findOpenBraceBefore( node, node.getBody().get( 0 ).getStart() )
		    : ( node.getBody() != null ? findOpenBraceBefore( node, node.getEnd() ) : null );
		if ( openBrace != null ) {
			closeRunningSpan( openBrace );
		} else if ( node.getBody() != null && !node.getBody().isEmpty() ) {
			closeRunningSpan( node.getBody().get( 0 ).getStart() );
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
			registerTagClose( node, afterBody, "function", shellHead );
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
			Point annStart = node.getAnnotations().get( 0 ).getPosition() == null
			    ? null
			    : node.getAnnotations().get( 0 ).getPosition().getStart();
			if ( annStart != null ) {
				shellStart = annStart;
			}
		}
		if ( shellStart == null ) {
			shellStart = node.getStart();
		}
		Point	openBrace	= node.getBody() == null || node.getBody().isEmpty()
		    ? findOpenBraceBefore( node, node.getEnd() )
		    : findOpenBraceBefore( node, node.getBody().get( 0 ).getStart() );
		int		shellId		= -1;
		if ( openBrace != null && shellStart != null ) {
			this.runningStart	= shellStart;
			// Close AFTER the "{" so the opening brace is part of the shell span.
			shellId				= closeRunningSpan( new Point( openBrace.getLine(), openBrace.getColumn() + 1 ) );
		}
		Point closeBrace = node.getEnd() == null ? null : new Point( node.getEnd().getLine(), node.getEnd().getColumn() - 1 );
		if ( shellId >= 0 ) {
			registerBraceGroup( shellId, closeBrace );
		}

		for ( BoxStatement stmt : node.getBody() ) {
			if ( stmt instanceof BoxFunctionDeclaration func ) {
				// Member/static function: use the FULL function logic (shell + body
				// braces + invocation-time body) so members behave like UDFs.
				this.runningStart = func.getStart();
				visit( func );
			} else if ( stmt instanceof BoxStaticInitializer init ) {
				// Static initializer: the "static {" header + closing "}" are a
				// group, batch-marked at static load (Pass B emits at static init).
				Point	initOpen	= findOpenBraceBefore( node, init.getBody().isEmpty() ? init.getEnd() : init.getBody().get( 0 ).getStart() );
				Point	initClose	= init.getEnd() == null ? null : new Point( init.getEnd().getLine(), init.getEnd().getColumn() - 1 );
				int		initHeader	= -1;
				this.runningStart = init.getStart();
				if ( initOpen != null ) {
					initHeader = closeRunningSpan( initOpen );
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
		List<Integer> propDefaults = new ArrayList<>();
		shellSpanConsumers.add( ( spanId, start ) -> propDefaults.add( spanId ) );
		for ( BoxProperty prop : node.getProperties() ) {
			visitPropertyDefaults( prop );
		}
		shellSpanConsumers.remove( shellSpanConsumers.size() - 1 );
		if ( !propDefaults.isEmpty() ) {
			transpiler.registerPropertyDefaultSpans( propDefaults.stream().mapToInt( Integer::intValue ).toArray() );
		}

		// Reset so the enclosing statement close doesn't mint a phantom span over
		// the trailing ";" / newline after the class.
		this.runningStart = null;
		this.currentSourceType.pop();
	}

	/**
	 * Visit a property's DEFAULT value expression. The property declaration text
	 * emits no bytecode (it's metadata), but the default expression may execute:
	 * literal defaults inline at class load, non-literal defaults lazily via a
	 * {@code defaultExpr_N} method on first access. Each default opens its own
	 * span at the expression's start.
	 *
	 * @param prop the property
	 */
	private void visitPropertyDefaults( BoxProperty prop ) {
		for ( var annotation : prop.getAllAnnotations() ) {
			if ( annotation.getKey().getValue().equalsIgnoreCase( "default" ) && annotation.getValue() != null ) {
				BoxExpression	value	= annotation.getValue();
				// Non-literal defaults run lazily (may not run) — break into their
				// own span; literal defaults run inline at load (still tracked).
				BoxExpression	inner	= unwrapParens( value );
				this.runningStart = inner.getStart();
				inner.accept( this );
				closeRunningSpan( inner.getEnd() );
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
	 *
	 * @param start the span's start point (may be null)
	 * @param end   the span's end point (may be null)
	 *
	 * @return the registered span id, or -1 if no span was registered
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