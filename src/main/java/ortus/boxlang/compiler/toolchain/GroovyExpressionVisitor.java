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
package ortus.boxlang.compiler.toolchain;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ortus.boxlang.compiler.ast.BoxExpression;
import ortus.boxlang.compiler.ast.BoxStatement;
import ortus.boxlang.compiler.ast.Position;
import ortus.boxlang.compiler.ast.expression.BoxArgument;
import ortus.boxlang.compiler.ast.expression.BoxArrayAccess;
import ortus.boxlang.compiler.ast.expression.BoxArrayLiteral;
import ortus.boxlang.compiler.ast.expression.BoxAssignment;
import ortus.boxlang.compiler.ast.expression.BoxAssignmentOperator;
import ortus.boxlang.compiler.ast.expression.BoxBinaryOperation;
import ortus.boxlang.compiler.ast.expression.BoxBinaryOperator;
import ortus.boxlang.compiler.ast.expression.BoxBooleanLiteral;
import ortus.boxlang.compiler.ast.expression.BoxClosure;
import ortus.boxlang.compiler.ast.expression.BoxComparisonOperation;
import ortus.boxlang.compiler.ast.expression.BoxComparisonOperator;
import ortus.boxlang.compiler.ast.expression.BoxDecimalLiteral;
import ortus.boxlang.compiler.ast.expression.BoxDotAccess;
import ortus.boxlang.compiler.ast.expression.BoxExpressionInvocation;
import ortus.boxlang.compiler.ast.expression.BoxFQN;
import ortus.boxlang.compiler.ast.expression.BoxFunctionInvocation;
import ortus.boxlang.compiler.ast.expression.BoxIdentifier;
import ortus.boxlang.compiler.ast.expression.BoxIntegerLiteral;
import ortus.boxlang.compiler.ast.expression.BoxMethodInvocation;
import ortus.boxlang.compiler.ast.expression.BoxNew;
import ortus.boxlang.compiler.ast.expression.BoxNull;
import ortus.boxlang.compiler.ast.expression.BoxParenthesis;
import ortus.boxlang.compiler.ast.expression.BoxSpreadExpression;
import ortus.boxlang.compiler.ast.expression.BoxStaticAccess;
import ortus.boxlang.compiler.ast.expression.BoxStaticMethodInvocation;
import ortus.boxlang.compiler.ast.expression.BoxStringInterpolation;
import ortus.boxlang.compiler.ast.expression.BoxStringConcat;
import ortus.boxlang.compiler.ast.expression.BoxStringLiteral;
import ortus.boxlang.compiler.ast.expression.BoxStructLiteral;
import ortus.boxlang.compiler.ast.expression.BoxStructType;
import ortus.boxlang.compiler.ast.expression.BoxTernaryOperation;
import ortus.boxlang.compiler.ast.expression.BoxUnaryOperation;
import ortus.boxlang.compiler.ast.expression.BoxUnaryOperator;
import ortus.boxlang.compiler.ast.statement.BoxArgumentDeclaration;
import ortus.boxlang.compiler.ast.statement.BoxIfElse;
import ortus.boxlang.compiler.ast.statement.BoxReturn;
import ortus.boxlang.compiler.ast.statement.BoxStatementBlock;
import ortus.boxlang.compiler.parser.GroovyParser;
import ortus.boxlang.parser.antlr.GroovyGrammar.AdditiveExprContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.ArgumentContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.ArgumentListContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.AsExprContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.AssignExprContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.BinaryLiteralExprContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.BitAndExprContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.BitOrExprContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.BitXorExprContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.CallExprContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.CallWithTrailingClosureExprContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.ClosureContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.ClosureLiteralExprContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.CollectionExprContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.ElvisExprContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.EmptyListLiteralContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.EmptyMapLiteralContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.EqualityExprContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.ExpressionContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.FalseLiteralExprContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.FloatLiteralExprContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.GstringPartContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.HexLiteralExprContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.IdentifierExprContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.InExprContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.IndexExprContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.InstanceofExprContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.IntLiteralExprContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.ListElementContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.ListLiteralContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.LogicalAndExprContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.LogicalOrExprContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.MapEntryContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.MapEntryOrSpreadContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.MapLiteralContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.MemberExprContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.MultiplicativeExprContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.NamedArgumentContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.NewInstanceExprContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.NullLiteralExprContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.OctalLiteralExprContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.ParenExprContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.PlainListElementContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.PlainMapEntryContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.PositionalArgumentContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.PostfixExprContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.PowerExprContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.PrimaryExprContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.RangeExprContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.RegexExprContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.RelationalExprContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.ShiftExprContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.SpreadArgumentContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.SpreadListElementContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.SpreadMapEntryContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.StringExprContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.StringOrGStringContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.SuperExprContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.TernaryExprContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.ThisExprContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.TrailingClosureCallExprContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.TrueLiteralExprContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.TypeNameContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.UnaryExprContext;
import ortus.boxlang.parser.antlr.GroovyGrammarBaseVisitor;
import ortus.boxlang.runtime.types.exceptions.ExpressionException;

/**
 * Walks GroovyGrammar's expression parse tree and builds the shared BoxLang AST
 * ({@code ortus.boxlang.compiler.ast.expression}), following the exact pattern
 * {@code CFExpressionVisitor} uses for CFML. Phase 2 of the Groovy parser/transpiler effort -
 * see GroovyLexer.g4/GroovyGrammar.g4 for the covered syntax subset.
 */
public class GroovyExpressionVisitor extends GroovyGrammarBaseVisitor<BoxExpression> {

	private final GroovyParser	tools;
	private final GroovyVisitor	statementVisitor;
	private Set<String>			knownStaticClassNames	= Set.of();
	private Map<String, String>	staticImportedMembers	= Map.of();

	public GroovyExpressionVisitor( GroovyParser tools, GroovyVisitor statementVisitor ) {
		this.tools				= tools;
		this.statementVisitor	= statementVisitor;
	}

	// Simple names Groovy code can reference as a bare, unqualified class (java.lang is always
	// implicitly imported, plus a handful of extremely common java.util/java.math classes) -
	// used to recognize "Math.max(...)", "Integer.MAX_VALUE", "new BigDecimal(...)" etc. as
	// static class access rather than an ordinary instance dot-access. GroovyParser extends this
	// set with whatever the file explicitly imports by name before parsing the body.
	public static final Set<String> DEFAULT_STATIC_CLASS_NAMES = Set.of(
	    "Math", "System", "Integer", "Long", "Double", "Float", "Boolean", "Character", "Byte", "Short",
	    "String", "Object", "Thread", "StringBuilder", "StringBuffer", "Number", "Class", "Void",
	    "Exception", "RuntimeException", "Error", "Throwable",
	    "Arrays", "Collections", "Optional", "UUID",
	    "BigInteger", "BigDecimal" );

	public void setKnownStaticClassNames( Set<String> knownStaticClassNames ) {
		this.knownStaticClassNames = knownStaticClassNames;
	}

	// "import static java.lang.Math.PI" (or "... as PIE") lets the file reference the member bare
	// ("PI"/"PIE"), unlike a plain class import - so unlike knownStaticClassNames (a set of
	// recognizable CLASS names for the existing "Math.max(...)" dot-access rewrite), this maps the
	// bare MEMBER name itself to its owning class's simple name, populated by GroovyParser from
	// each non-wildcard "import static" statement in the file.
	public void setStaticImportedMembers( Map<String, String> staticImportedMembers ) {
		this.staticImportedMembers = staticImportedMembers;
	}

	/**
	 * If {@code exprCtx} is a bare identifier reference (not the result of another expression)
	 * whose text matches a known-importable class simple name, returns a {@code BoxIdentifier}
	 * for it to use as a static-access/invocation base. Otherwise returns {@code null}, meaning
	 * "treat this as ordinary instance access" - the common case.
	 */
	private BoxIdentifier staticClassBase( ExpressionContext exprCtx ) {
		if ( exprCtx instanceof PrimaryExprContext primaryCtx && primaryCtx.primary() instanceof IdentifierExprContext idCtx ) {
			String name = idCtx.IDENTIFIER().getText();
			if ( knownStaticClassNames.contains( name ) ) {
				var pos = tools.getPosition( idCtx );
				return new BoxIdentifier( name, pos, name );
			}
		}
		return null;
	}

	@Override
	public BoxExpression visitPrimaryExpr( PrimaryExprContext ctx ) {
		return ctx.primary().accept( this );
	}

	@Override
	public BoxExpression visitParenExpr( ParenExprContext ctx ) {
		var	pos	= tools.getPosition( ctx );
		var	src	= tools.getSourceText( ctx );
		return new BoxParenthesis( ctx.expression().accept( this ), pos, src );
	}

	@Override
	public BoxExpression visitIndexExpr( IndexExprContext ctx ) {
		var	pos	= tools.getPosition( ctx );
		var	src	= tools.getSourceText( ctx );
		return new BoxArrayAccess( ctx.expression( 0 ).accept( this ), false, ctx.expression( 1 ).accept( this ), pos, src );
	}

	@Override
	public BoxExpression visitCallExpr( CallExprContext ctx ) {
		return buildCallExpression( ctx.expression(), buildArguments( ctx.argumentList() ), tools.getPosition( ctx ), tools.getSourceText( ctx ) );
	}

	// Groovy's "trailing closure after a parenthesized argument list" idiom - e.g.
	// list.inject(0) { acc, x -> acc + x } or (1..10).step(2) { total += it } - is extremely
	// common (unlike the no-parens form visitTrailingClosureCallExpr already handles, this one
	// combines an explicit argument list with a closure). The closure is simply appended as the
	// call's last BoxArgument, exactly like Groovy itself desugars it - except for a couple of
	// specific method names (see isInjectCall/isStepCall) that need a different rewrite entirely.
	@Override
	public BoxExpression visitCallWithTrailingClosureExpr( CallWithTrailingClosureExprContext ctx ) {
		List<BoxArgument>	args		= new ArrayList<>( buildArguments( ctx.argumentList() ) );
		BoxExpression		closureExpr	= visitClosure( ctx.closure() );
		BoxArgument			closureArg	= new BoxArgument( closureExpr, tools.getPosition( ctx.closure() ), tools.getSourceText( ctx.closure() ) );
		if ( isInjectCall( ctx.expression() ) ) {
			// Groovy's list.inject(initial) { acc, x -> ... } aliases to BoxLang's native
			// .reduce(callback, initial) member (see GROOVY_METHOD_ALIASES) - same semantics,
			// but the opposite argument order, so the closure has to go first here, not last.
			args.add( 0, closureArg );
			return buildCallExpression( ctx.expression(), args, tools.getPosition( ctx ), tools.getSourceText( ctx ) );
		}
		if ( isStepCall( ctx.expression() ) ) {
			return buildRangeStepWithClosure( ( MemberExprContext ) ctx.expression(), args, closureArg, tools.getPosition( ctx ),
			    tools.getSourceText( ctx ) );
		}
		args.add( closureArg );
		return buildCallExpression( ctx.expression(), args, tools.getPosition( ctx ), tools.getSourceText( ctx ) );
	}

	private boolean isInjectCall( ExpressionContext calleeCtx ) {
		return calleeCtx instanceof MemberExprContext memberCtx && "inject".equals( memberCtx.IDENTIFIER().getText() );
	}

	private boolean isStepCall( ExpressionContext calleeCtx ) {
		return calleeCtx instanceof MemberExprContext memberCtx && "step".equals( memberCtx.IDENTIFIER().getText() );
	}

	// Groovy's "range.step(amount) { closure }" trailing-closure idiom does NOT mean "call a
	// two/three-argument step() with the closure tacked on" - BoxLang's own native
	// Range.step(Number)/Range.step(Number, String) are plain BUILDER methods (return a new,
	// re-stepped Range; never iterate), and stay that way for every BoxLang dialect. A prior
	// attempt to teach core Range a THIRD "step(amount, closure)" meaning that eagerly iterated
	// and invoked the callback was reverted after review: it collided with the builder meaning
	// under the same overloaded member name and even returned the wrong (unstepped) range.
	// Instead, this rewrites the whole call into three ordinary, already-existing method calls:
	// "range.step(amount)" (the ordinary builder - args here already excludes the closure, since
	// it came from the separate trailing-closure grammar alternative, not argumentList), then
	// ".stream()" (Range's own native Java method, returning a lazy java.util.stream.Stream -
	// never materializes the whole range into a collection), then ".forEach(closure)" (an
	// ordinary Stream method - BoxLang's interop layer casts the closure to Consumer). Like
	// isInjectCall above, this is a syntactic, name-only heuristic with no real receiver-type
	// checking - calling some unrelated user class's own differently-behaved "step" method this
	// way would be misinterpreted the same way "inject" already is, a documented, bounded gap.
	private BoxExpression buildRangeStepWithClosure( MemberExprContext stepCallCtx, List<BoxArgument> stepArgs, BoxArgument closureArg, Position pos,
	    String src ) {
		BoxExpression	stepCall	= buildMethodInvocation( stepCallCtx, stepArgs, pos, src );
		BoxExpression	streamCall	= new BoxMethodInvocation( new BoxIdentifier( "stream", pos, "stream" ), stepCall, List.of(), false, true, pos, src );
		return new BoxMethodInvocation( new BoxIdentifier( "forEach", pos, "forEach" ), streamCall, List.of( closureArg ), false, true, pos, src );
	}

	private BoxExpression buildCallExpression( ExpressionContext callee, List<BoxArgument> args, Position pos, String src ) {
		if ( callee instanceof MemberExprContext memberCtx ) {
			return buildMethodInvocation( memberCtx, args, pos, src );
		}
		if ( callee instanceof PrimaryExprContext primaryCtx && primaryCtx.primary() instanceof IdentifierExprContext idCtx ) {
			return buildBareNameCall( idCtx.IDENTIFIER().getText(), args, pos, src );
		}
		return new BoxExpressionInvocation( callee.accept( this ), args, pos, src );
	}

	// A call whose target is a bare name, not a dotted/computed expression - either an ordinary
	// "expression syntax" call (foo(...)) or Groovy's paren-less command-style call
	// (GroovyVisitor#visitCommandCallStatement). Both need the exact same "is this actually a
	// bare reference to a statically-imported member" rewrite (see setStaticImportedMembers), so
	// it's shared here rather than duplicated.
	BoxExpression buildBareNameCall( String name, List<BoxArgument> args, Position pos, String src ) {
		String ownerClass = staticImportedMembers.get( name );
		if ( ownerClass != null ) {
			BoxIdentifier	nameExpr	= new BoxIdentifier( name, pos, name );
			BoxIdentifier	ownerExpr	= new BoxIdentifier( ownerClass, pos, ownerClass );
			return new BoxStaticMethodInvocation( nameExpr, ownerExpr, args, pos, src );
		}
		return new BoxFunctionInvocation( name, args, pos, src );
	}

	// Groovy collection methods that exist on BoxLang's Array type under a different member
	// name. Only renames apply where the semantics genuinely match - e.g. Groovy's findAll
	// (filter-by-predicate) deliberately maps to BoxLang's "filter", NOT its own native
	// "findAll" member (ArrayFindAll), which instead searches for indices of a given value and
	// would silently do the wrong thing if called with a predicate closure.
	private static final java.util.Map<String, String> GROOVY_METHOD_ALIASES = java.util.Map.of(
	    "collect", "map",
	    "any", "some",
	    "findAll", "filter",
	    "inject", "reduce" );

	@Override
	public BoxExpression visitTrailingClosureCallExpr( TrailingClosureCallExprContext ctx ) {
		var					pos			= tools.getPosition( ctx );
		var					src			= tools.getSourceText( ctx );
		BoxExpression		obj			= ctx.expression().accept( this );
		boolean				safe		= ctx.SAFE_DOT() != null;
		BoxIdentifier		nameExpr	= aliasedIdentifier( ctx.IDENTIFIER() );
		BoxExpression		closureExpr	= visitClosure( ctx.closure() );
		List<BoxArgument>	args		= List.of(
		    new BoxArgument( closureExpr, tools.getPosition( ctx.closure() ), tools.getSourceText( ctx.closure() ) ) );
		if ( ctx.SPREAD_DOT() != null ) {
			BoxExpression perElementCall = new BoxMethodInvocation( nameExpr, itIdentifier( pos ), args, false, true, pos, src );
			return buildSpreadDot( obj, perElementCall, pos, src );
		}
		return new BoxMethodInvocation( nameExpr, obj, args, safe, true, pos, src );
	}

	private BoxExpression buildMethodInvocation( MemberExprContext memberCtx, List<BoxArgument> args, Position pos, String src ) {
		if ( memberCtx.METHOD_POINTER() != null ) {
			// "Type.&method(args)" - immediately calling the method-pointer expression itself,
			// rather than using it as a standalone value (e.g. passed to .collect(...)) - isn't
			// supported. Rejecting explicitly rather than silently treating ".&" as a plain "."
			// here, which would drop the method-pointer semantics entirely without complaint.
			throw new ExpressionException(
			    "Calling a method pointer expression (.&) directly isn't supported - use it as a standalone value instead, e.g. \"list.collect(String.&toUpperCase)\"",
			    pos, src );
		}
		BoxIdentifier nameExpr = aliasedIdentifier( memberCtx.IDENTIFIER() );
		if ( memberCtx.SPREAD_DOT() != null ) {
			BoxExpression	collection		= memberCtx.expression().accept( this );
			BoxExpression	perElementCall	= new BoxMethodInvocation( nameExpr, itIdentifier( pos ), args, false, true, pos, src );
			return buildSpreadDot( collection, perElementCall, pos, src );
		}
		BoxIdentifier staticBase = staticClassBase( memberCtx.expression() );
		if ( staticBase != null ) {
			return new BoxStaticMethodInvocation( nameExpr, staticBase, args, pos, src );
		}
		BoxExpression	obj		= memberCtx.expression().accept( this );
		boolean			safe	= memberCtx.SAFE_DOT() != null;
		return new BoxMethodInvocation( nameExpr, obj, args, safe, true, pos, src );
	}

	private BoxIdentifier aliasedIdentifier( org.antlr.v4.runtime.tree.TerminalNode node ) {
		String name = GROOVY_METHOD_ALIASES.getOrDefault( node.getText(), node.getText() );
		return new BoxIdentifier( name, tools.getPosition( node.getSymbol() ), node.getText() );
	}

	@Override
	public BoxExpression visitMemberExpr( MemberExprContext ctx ) {
		var	pos	= tools.getPosition( ctx );
		var	src	= tools.getSourceText( ctx );
		if ( ctx.METHOD_POINTER() != null ) {
			return buildMethodPointer( ctx.expression(), identifier( ctx.IDENTIFIER() ), pos, src );
		}
		if ( ctx.SPREAD_DOT() != null ) {
			BoxExpression	collection		= ctx.expression().accept( this );
			BoxExpression	perElementDot	= new BoxDotAccess( itIdentifier( pos ), false, identifier( ctx.IDENTIFIER() ), pos, src );
			return buildSpreadDot( collection, perElementDot, pos, src );
		}
		BoxIdentifier staticBase = staticClassBase( ctx.expression() );
		if ( staticBase != null ) {
			return new BoxStaticAccess( staticBase, ctx.SAFE_DOT() != null, identifier( ctx.IDENTIFIER() ), pos, src );
		}
		boolean safe = ctx.SAFE_DOT() != null;
		return new BoxDotAccess( ctx.expression().accept( this ), safe, identifier( ctx.IDENTIFIER() ), pos, src );
	}

	// Groovy's spread-dot operator (people*.name) maps a property/method access over every
	// element of a collection - equivalent to people.collect { it.name }. Desugaring it this way
	// (rather than adding new runtime machinery) reuses BoxLang's existing "map" member function
	// exactly the same way GROOVY_METHOD_ALIASES already maps a user-written ".collect(...)" call
	// to it.
	private BoxExpression buildSpreadDot( BoxExpression collection, BoxExpression perElementExpr, Position pos, String src ) {
		BoxArgumentDeclaration	itParam		= new BoxArgumentDeclaration( false, "Any", "it", null, List.of(), List.of(), pos, src );
		BoxStatement			returnStmt	= new BoxReturn( perElementExpr, pos, src );
		BoxExpression			closure		= new BoxClosure( new ArrayList<>( List.of( itParam ) ),
		    List.of(), new BoxStatementBlock( List.of( returnStmt ), pos, src ), pos, src );
		BoxArgument				closureArg	= new BoxArgument( closure, pos, src );
		return new BoxMethodInvocation( new BoxIdentifier( "map", pos, "map" ), collection, List.of( closureArg ), false, true, pos, src );
	}

	private BoxIdentifier itIdentifier( Position pos ) {
		return new BoxIdentifier( "it", pos, "it" );
	}

	// The forwarded-argument count a method pointer closure will fully forward, via an
	// "arguments.len() == N" dispatch (see buildMethodPointerClosure). Deliberately stops at 2,
	// NOT raised to match BoxLang's own 3-arg (ArrayEach/ArrayMap/ArrayFilter/ArrayReduce/
	// StructEach/QueryEach/etc.) or 4-arg (StructReduce/QueryReduce) non-strict callback
	// conventions - confirmed the hard way, as a real regression this exact PR caught in its own
	// test suite: raising this to 3 broke both "list.each(other.&add)" (ArrayEach forwarding
	// element/index/array, so "add" - a 1-arg method - was suddenly called with 3 args) and
	// "list.collect(String.&toUpperCase)" (an unbound pointer forwarding index/array as bogus
	// extra arguments to the receiver's own 0-arg "toUpperCase"). 3 and 4 are exactly the arg
	// counts those real, common conventions use, so there is no count above 2 this parser can
	// safely assume is a "genuine direct call" rather than "one of those conventions" - 2 remains
	// the highest count reachable ONLY by directly invoking the pointer value itself with that
	// many real arguments (e.g. "def f = acc.&plus; f(3, 4)"), never by any non-strict BIF
	// convention in this codebase.
	private static final int METHOD_POINTER_MAX_FORWARDED_ARGS = 2;

	// Groovy's method pointer operator (Type.&methodName / instance.&methodName) produces a
	// standalone invokable value.
	// - Unbound (base is a known static class name, e.g. String.&toUpperCase): see
	// buildUnboundMethodPointer.
	// - Bound (any other base expression, e.g. myList.&add): see buildBoundMethodPointer.
	private BoxExpression buildMethodPointer( ExpressionContext baseCtx, BoxIdentifier methodName, Position pos, String src ) {
		BoxIdentifier staticBase = staticClassBase( baseCtx );
		if ( staticBase != null ) {
			return buildUnboundMethodPointer( methodName, pos, src );
		}
		return buildBoundMethodPointer( baseCtx, methodName, pos, src );
	}

	// A bound method pointer (myList.&add) is built as a closure forwarding however many
	// arguments it's actually called with (up to METHOD_POINTER_MAX_FORWARDED_ARGS) as the
	// target method's own arguments, on a fixed, already-known receiver (baseCtx).
	private BoxExpression buildBoundMethodPointer( ExpressionContext baseCtx, BoxIdentifier methodName, Position pos, String src ) {
		return buildMethodPointerClosure( pos, src, ( argumentsId, forwardedCount ) -> new BoxMethodInvocation(
		    methodName, baseCtx.accept( this ), argSlotArguments( argumentsId, 1, forwardedCount, pos, src ), false, true, pos, src ) );
	}

	// An unbound method pointer (String.&toUpperCase) is built the same way as the bound form,
	// except the receiver ISN'T already known - real Groovy semantics for this shape are that
	// the first forwarded argument itself becomes the receiver, and any remaining arguments are
	// the target method's own arguments (e.g. "String.&startsWith" called with ("hello", "he")
	// means "hello".startsWith("he")). Previously this returned BoxLang's own native
	// BoxFunctionalMemberAccess AST node directly (the exact ".methodName" functional-member-
	// access value BoxLang's own grammar already supports) - correct in spirit, but that shared
	// runtime node's own documented contract ("no args will be passed to the member method")
	// hard-caps it at a single (receiver-only) argument; widening THAT node is out of scope here
	// since it also backs native BoxLang's own ".methodName" syntax, not just this Groovy
	// feature. Building an explicit closure here instead (mirroring the bound form) reaches the
	// same forwarding depth without touching shared runtime code.
	private BoxExpression buildUnboundMethodPointer( BoxIdentifier methodName, Position pos, String src ) {
		return buildMethodPointerClosure( pos, src, ( argumentsId, forwardedCount ) -> {
			BoxExpression receiver = argSlot( argumentsId, 1, pos, src );
			return new BoxMethodInvocation( methodName, receiver, argSlotArguments( argumentsId, 2, forwardedCount, pos, src ), false, true, pos, src );
		} );
	}

	// Shared builder for both method-pointer forms: a closure whose body dispatches on
	// "arguments.len()" (exactly like GroovyVisitor#withVarargsPreamble already does for a
	// genuinely variadic parameter - no reflection or speculative retry needed, so a
	// side-effecting target method is never invoked more than once) and forwards every argument
	// it was actually called with, from METHOD_POINTER_MAX_FORWARDED_ARGS down to 2 - NOT a
	// blind "forward everything" (which was tried and found to break the common
	// "myList.each(other.&add)" idiom before this dispatch existed at all: BoxLang's own
	// ArrayEach invokes a non-strict callback with 3 arguments unconditionally - element, index,
	// array - so forwarding all of them to a 2-arg method like "add" would throw). The fallback
	// (arguments.len() outside [2, MAX], including exactly 1) forwards only the first argument -
	// this is also what arguments.len() itself reports when the closure is invoked with truly
	// ZERO arguments, since its sole declared parameter is optional and ArgumentsScope pads a
	// missing-but-declared argument rather than reporting the true (lower) call-time count.
	//
	// @param callForArgCount given the arguments identifier and a forwarded-argument count N (2..MAX,
	// then 1 for the fallback), builds the call expression that forwards exactly N arguments.
	private BoxExpression buildMethodPointerClosure( Position pos, String src,
	    java.util.function.BiFunction<BoxIdentifier, Integer, BoxExpression> callForArgCount ) {
		BoxArgumentDeclaration	itParam			= new BoxArgumentDeclaration( false, "Any", "it", null, List.of(), List.of(), pos, src );
		BoxIdentifier			argumentsId		= new BoxIdentifier( "arguments", pos, "arguments" );
		BoxExpression			argumentsLen	= new BoxMethodInvocation( new BoxIdentifier( "len", pos, "len" ), argumentsId, List.of(), false, true, pos,
		    src );

		BoxStatement			body			= new BoxStatementBlock(
		    List.of( new BoxReturn( callForArgCount.apply( argumentsId, 1 ), pos, src ) ), pos, src );
		for ( int forwardedCount = METHOD_POINTER_MAX_FORWARDED_ARGS; forwardedCount >= 2; forwardedCount-- ) {
			BoxExpression	isThisManyArgs	= new BoxComparisonOperation( argumentsLen, BoxComparisonOperator.Equal,
			    new BoxIntegerLiteral( String.valueOf( forwardedCount ), pos, src ), pos, src );
			BoxStatement	thenBranch		= new BoxStatementBlock(
			    List.of( new BoxReturn( callForArgCount.apply( argumentsId, forwardedCount ), pos, src ) ), pos, src );
			body = new BoxStatementBlock( List.of( new BoxIfElse( isThisManyArgs, thenBranch, body, pos, src ) ), pos, src );
		}

		return new BoxClosure( new ArrayList<>( List.of( itParam ) ), List.of(), body, pos, src );
	}

	// Builds "arguments[from]", "arguments[from+1]", ..., "arguments[to]" as a list of positional
	// BoxArguments (to < from yields an empty list). argumentsId (a simple leaf identifier with no
	// side effects) is deliberately reused across every slot and branch here - unlike a full
	// expression subtree, sharing a single leaf node instance across sibling positions is already
	// established practice in this class.
	private List<BoxArgument> argSlotArguments( BoxIdentifier argumentsId, int from, int to, Position pos, String src ) {
		List<BoxArgument> args = new ArrayList<>();
		for ( int i = from; i <= to; i++ ) {
			args.add( new BoxArgument( argSlot( argumentsId, i, pos, src ), pos, src ) );
		}
		return args;
	}

	private BoxExpression argSlot( BoxIdentifier argumentsId, int index, Position pos, String src ) {
		return new BoxArrayAccess( argumentsId, false, new BoxIntegerLiteral( String.valueOf( index ), pos, src ), pos, src );
	}

	@Override
	public BoxExpression visitPostfixExpr( PostfixExprContext ctx ) {
		var				pos		= tools.getPosition( ctx );
		var				src		= tools.getSourceText( ctx );
		BoxExpression	expr	= ctx.expression().accept( this );
		var				op		= ctx.INC() != null ? BoxUnaryOperator.PostPlusPlus : BoxUnaryOperator.PostMinusMinus;
		return new BoxUnaryOperation( expr, op, pos, src );
	}

	@Override
	public BoxExpression visitUnaryExpr( UnaryExprContext ctx ) {
		var					pos		= tools.getPosition( ctx );
		var					src		= tools.getSourceText( ctx );
		BoxExpression		expr	= ctx.expression().accept( this );
		BoxUnaryOperator	op;
		if ( ctx.INC() != null ) {
			op = BoxUnaryOperator.PrePlusPlus;
		} else if ( ctx.DEC() != null ) {
			op = BoxUnaryOperator.PreMinusMinus;
		} else if ( ctx.PLUS() != null ) {
			op = BoxUnaryOperator.Plus;
		} else if ( ctx.MINUS() != null ) {
			op = BoxUnaryOperator.Minus;
		} else if ( ctx.BANG() != null ) {
			op = BoxUnaryOperator.Not;
		} else {
			op = BoxUnaryOperator.BitwiseComplement;
		}
		return new BoxUnaryOperation( expr, op, pos, src );
	}

	@Override
	public BoxExpression visitPowerExpr( PowerExprContext ctx ) {
		return binary( ctx.expression( 0 ), BoxBinaryOperator.Power, ctx.expression( 1 ), ctx );
	}

	@Override
	public BoxExpression visitMultiplicativeExpr( MultiplicativeExprContext ctx ) {
		if ( ctx.STAR() != null && isStringLiteralExpr( ctx.expression( 0 ) ) ) {
			// Groovy overloads "*" for string repetition (String.multiply(Number)) - only the
			// left side can be a string (Groovy never defines Number.multiply(String)), so
			// unlike the "+" fix this only needs to check one side. Same bounded, syntactic-
			// literal-only approach: desugar to a call to BoxLang's own RepeatString BIF
			// (CF's repeatString()) rather than attempting runtime type dispatch on "*" itself.
			var					pos		= tools.getPosition( ctx );
			var					src		= tools.getSourceText( ctx );
			List<BoxArgument>	args	= List.of(
			    new BoxArgument( ctx.expression( 0 ).accept( this ), tools.getPosition( ctx.expression( 0 ) ), tools.getSourceText( ctx.expression( 0 ) ) ),
			    new BoxArgument( ctx.expression( 1 ).accept( this ), tools.getPosition( ctx.expression( 1 ) ), tools.getSourceText( ctx.expression( 1 ) ) ) );
			return new BoxFunctionInvocation( "RepeatString", args, pos, src );
		}
		var op = ctx.STAR() != null ? BoxBinaryOperator.Star : ctx.SLASH() != null ? BoxBinaryOperator.Slash : BoxBinaryOperator.Mod;
		return binary( ctx.expression( 0 ), op, ctx.expression( 1 ), ctx );
	}

	@Override
	public BoxExpression visitAdditiveExpr( AdditiveExprContext ctx ) {
		if ( ctx.PLUS() != null && ( isStringLiteralExpr( ctx.expression( 0 ) ) || isStringLiteralExpr( ctx.expression( 1 ) ) ) ) {
			// Groovy overloads "+" for string concatenation, but BoxLang's own "+" is strictly
			// numeric (see GroovyExecutionTest.testPlusIsNumericOnlyNotStringConcat for the
			// full explanation). When one side is SYNTACTICALLY a string literal/GString -
			// "prefix" + var or var + "suffix", by far the most common real-world case - we
			// can tell at parse time that concatenation, not addition, is meant, and build a
			// BoxStringConcat instead of a numeric BoxBinaryOperation. The fully general case
			// (both sides are variables of unknown type, only known at runtime) still isn't
			// handled - that needs actual runtime type dispatch, deliberately not built here.
			var	pos	= tools.getPosition( ctx );
			var	src	= tools.getSourceText( ctx );
			return new BoxStringConcat( List.of( ctx.expression( 0 ).accept( this ), ctx.expression( 1 ).accept( this ) ), pos, src );
		}
		var op = ctx.PLUS() != null ? BoxBinaryOperator.Plus : BoxBinaryOperator.Minus;
		return binary( ctx.expression( 0 ), op, ctx.expression( 1 ), ctx );
	}

	/**
	 * True when the given expression is syntactically a string literal or GString (not merely
	 * a value that happens to be a string at runtime - that general case needs runtime type
	 * dispatch this method deliberately doesn't attempt).
	 */
	private boolean isStringLiteralExpr( ortus.boxlang.parser.antlr.GroovyGrammar.ExpressionContext ctx ) {
		return ctx instanceof PrimaryExprContext primaryCtx && primaryCtx.primary() instanceof StringExprContext;
	}

	@Override
	public BoxExpression visitShiftExpr( ShiftExprContext ctx ) {
		if ( ctx.LSHIFT() != null ) {
			// Groovy overloads "<<" for collection append (list << item), in addition to Java's
			// numeric left-shift - runtime type dispatch (the collection is virtually always a
			// plain variable, not a literal, so there's no syntactic shortcut like "+"'s string-
			// literal check above) via a dedicated Groovy-only operator class, rather than
			// changing BitwiseSignedLeftShift itself, which every BoxLang dialect's own "<<"
			// shares - see GroovyLeftShiftOrAppend's own header for the full reasoning.
			var					pos			= tools.getPosition( ctx );
			var					src			= tools.getSourceText( ctx );
			BoxExpression		left		= ctx.expression( 0 ).accept( this );
			BoxExpression		right		= ctx.expression( 1 ).accept( this );
			BoxExpression		classRef	= new BoxFQN( "ortus.boxlang.runtime.operators.GroovyLeftShiftOrAppend", pos,
			    "ortus.boxlang.runtime.operators.GroovyLeftShiftOrAppend" );
			List<BoxArgument>	args		= List.of(
			    new BoxArgument( left, left.getPosition(), left.getSourceText() ),
			    new BoxArgument( right, right.getPosition(), right.getSourceText() ) );
			return new BoxStaticMethodInvocation( new BoxIdentifier( "invoke", pos, "invoke" ), classRef, args, pos, src );
		}
		return binary( ctx.expression( 0 ), BoxBinaryOperator.BitwiseSignedRightShift, ctx.expression( 1 ), ctx );
	}

	@Override
	public BoxExpression visitRangeExpr( RangeExprContext ctx ) {
		var op = ctx.RANGE_EXCL() != null ? BoxBinaryOperator.RangeRightExclusive : BoxBinaryOperator.Range;
		return binary( ctx.expression( 0 ), op, ctx.expression( 1 ), ctx );
	}

	@Override
	public BoxExpression visitInstanceofExpr( InstanceofExprContext ctx ) {
		var				pos		= tools.getPosition( ctx );
		var				src		= tools.getSourceText( ctx );
		BoxExpression	left	= ctx.expression().accept( this );
		BoxExpression	right	= toTypeExpression( ctx.typeName() );
		return new BoxBinaryOperation( left, BoxBinaryOperator.InstanceOf, right, pos, src );
	}

	@Override
	public BoxExpression visitAsExpr( AsExprContext ctx ) {
		var				pos		= tools.getPosition( ctx );
		var				src		= tools.getSourceText( ctx );
		BoxExpression	left	= ctx.expression().accept( this );
		BoxExpression	right	= toTypeExpression( ctx.typeName() );
		return new BoxBinaryOperation( left, BoxBinaryOperator.CastAs, right, pos, src );
	}

	@Override
	public BoxExpression visitRelationalExpr( RelationalExprContext ctx ) {
		var						pos		= tools.getPosition( ctx );
		var						src		= tools.getSourceText( ctx );
		BoxExpression			left	= ctx.expression( 0 ).accept( this );
		BoxExpression			right	= ctx.expression( 1 ).accept( this );
		BoxComparisonOperator	op;
		if ( ctx.LT() != null ) {
			op = BoxComparisonOperator.LessThan;
		} else if ( ctx.GT() != null ) {
			op = BoxComparisonOperator.GreaterThan;
		} else if ( ctx.LE() != null ) {
			op = BoxComparisonOperator.LessThanEquals;
		} else {
			op = BoxComparisonOperator.GreaterThanEquals;
		}
		return new BoxComparisonOperation( left, op, right, pos, src );
	}

	// Groovy's "in" membership test (e.g. "2 in list") is sugar for "list.isCase(2)", which for
	// the common container types (Array, Set, String) collapses to a plain containment check -
	// desugar to a ".contains(...)" method call on the right-hand side, reusing BoxLang's own
	// native member function rather than adding new runtime machinery.
	@Override
	public BoxExpression visitInExpr( InExprContext ctx ) {
		var					pos			= tools.getPosition( ctx );
		var					src			= tools.getSourceText( ctx );
		BoxExpression		left		= ctx.expression( 0 ).accept( this );
		BoxExpression		right		= ctx.expression( 1 ).accept( this );
		List<BoxArgument>	args		= List.of( new BoxArgument( left, left.getPosition(), left.getSourceText() ) );
		BoxIdentifier		nameExpr	= new BoxIdentifier( "contains", pos, "contains" );
		return new BoxMethodInvocation( nameExpr, right, args, false, true, pos, src );
	}

	private static final String PATTERN_FQN = "java.util.regex.Pattern";

	// Groovy's "=~" (find) and "==~" (full match) regex operators. Real Groovy's "=~" returns a
	// live java.util.regex.Matcher (truthy only via Groovy's own Matcher.asBoolean() override,
	// which BoxLang has no equivalent for - an arbitrary non-null Java object is otherwise just
	// truthy regardless of whether it matched anything). Rather than return a Matcher that would
	// silently misbehave in the overwhelmingly common "if (str =~ pattern)" idiom, "=~" eagerly
	// evaluates find() here and returns a plain boolean - correct for that idiom and for simple
	// yes/no checks, but it means the richer Matcher API (group extraction, iterating multiple
	// matches) isn't available from a "=~" result the way it is in real Groovy. "==~" needed no
	// such compromise - it already returns a plain boolean in real Groovy too.
	@Override
	public BoxExpression visitRegexExpr( RegexExprContext ctx ) {
		var				pos			= tools.getPosition( ctx );
		var				src			= tools.getSourceText( ctx );
		BoxExpression	left		= ctx.expression( 0 ).accept( this );
		BoxExpression	right		= ctx.expression( 1 ).accept( this );
		BoxExpression	patternRef	= new BoxFQN( PATTERN_FQN, pos, PATTERN_FQN );

		if ( ctx.REGEX_MATCH() != null ) {
			List<BoxArgument> args = List.of(
			    new BoxArgument( right, right.getPosition(), right.getSourceText() ),
			    new BoxArgument( left, left.getPosition(), left.getSourceText() ) );
			return new BoxStaticMethodInvocation( new BoxIdentifier( "matches", pos, "matches" ), patternRef, args, pos, src );
		}

		BoxExpression	compiled	= new BoxStaticMethodInvocation( new BoxIdentifier( "compile", pos, "compile" ), patternRef,
		    List.of( new BoxArgument( right, right.getPosition(), right.getSourceText() ) ), pos, src );
		BoxExpression	matcher		= new BoxMethodInvocation( new BoxIdentifier( "matcher", pos, "matcher" ), compiled,
		    List.of( new BoxArgument( left, left.getPosition(), left.getSourceText() ) ), false, true, pos, src );
		return new BoxMethodInvocation( new BoxIdentifier( "find", pos, "find" ), matcher, List.of(), false, true, pos, src );
	}

	@Override
	public BoxExpression visitEqualityExpr( EqualityExprContext ctx ) {
		var	pos	= tools.getPosition( ctx );
		var	src	= tools.getSourceText( ctx );
		if ( ctx.SPACESHIP() != null ) {
			// Groovy overloads "<=>" for Comparable.compareTo(...). BoxLang's own runtime
			// already has a general-purpose three-way compare used internally for <, >, etc
			// (numbers compared numerically, strings case-insensitively, dates chronologically) -
			// reuse it directly via a static call rather than adding new AST/runtime machinery.
			List<BoxArgument>	args		= List.of(
			    new BoxArgument( ctx.expression( 0 ).accept( this ), tools.getPosition( ctx.expression( 0 ) ), tools.getSourceText( ctx.expression( 0 ) ) ),
			    new BoxArgument( ctx.expression( 1 ).accept( this ), tools.getPosition( ctx.expression( 1 ) ), tools.getSourceText( ctx.expression( 1 ) ) ) );
			BoxIdentifier		nameExpr	= new BoxIdentifier( "invoke", pos, "invoke" );
			BoxExpression		classRef	= new BoxFQN( "ortus.boxlang.runtime.operators.Compare", pos, "ortus.boxlang.runtime.operators.Compare" );
			return new BoxStaticMethodInvocation( nameExpr, classRef, args, pos, src );
		}
		BoxExpression			left	= ctx.expression( 0 ).accept( this );
		BoxExpression			right	= ctx.expression( 1 ).accept( this );
		BoxComparisonOperator	op;
		if ( ctx.EQUAL() != null ) {
			op = BoxComparisonOperator.Equal;
		} else if ( ctx.NOTEQUAL() != null ) {
			op = BoxComparisonOperator.NotEqual;
		} else if ( ctx.IDENTICAL() != null ) {
			op = BoxComparisonOperator.TEqual;
		} else {
			op = BoxComparisonOperator.TNotEqual;
		}
		return new BoxComparisonOperation( left, op, right, pos, src );
	}

	@Override
	public BoxExpression visitBitAndExpr( BitAndExprContext ctx ) {
		return binary( ctx.expression( 0 ), BoxBinaryOperator.BitwiseAnd, ctx.expression( 1 ), ctx );
	}

	@Override
	public BoxExpression visitBitXorExpr( BitXorExprContext ctx ) {
		return binary( ctx.expression( 0 ), BoxBinaryOperator.BitwiseXor, ctx.expression( 1 ), ctx );
	}

	@Override
	public BoxExpression visitBitOrExpr( BitOrExprContext ctx ) {
		return binary( ctx.expression( 0 ), BoxBinaryOperator.BitwiseOr, ctx.expression( 1 ), ctx );
	}

	@Override
	public BoxExpression visitLogicalAndExpr( LogicalAndExprContext ctx ) {
		return binary( ctx.expression( 0 ), BoxBinaryOperator.And, ctx.expression( 1 ), ctx );
	}

	@Override
	public BoxExpression visitLogicalOrExpr( LogicalOrExprContext ctx ) {
		return binary( ctx.expression( 0 ), BoxBinaryOperator.Or, ctx.expression( 1 ), ctx );
	}

	@Override
	public BoxExpression visitElvisExpr( ElvisExprContext ctx ) {
		return binary( ctx.expression( 0 ), BoxBinaryOperator.Elvis, ctx.expression( 1 ), ctx );
	}

	@Override
	public BoxExpression visitTernaryExpr( TernaryExprContext ctx ) {
		var	pos	= tools.getPosition( ctx );
		var	src	= tools.getSourceText( ctx );
		return new BoxTernaryOperation( ctx.expression( 0 ).accept( this ), ctx.expression( 1 ).accept( this ), ctx.expression( 2 ).accept( this ), pos,
		    src );
	}

	@Override
	public BoxExpression visitAssignExpr( AssignExprContext ctx ) {
		var		pos				= tools.getPosition( ctx );
		var		src				= tools.getSourceText( ctx );

		String	assignOpText	= ctx.assignOp().getText();
		return switch ( assignOpText ) {
			case "=" -> new BoxAssignment( ctx.expression( 0 ).accept( this ), BoxAssignmentOperator.Equal, ctx.expression( 1 ).accept( this ), List.of(),
			    pos, src );
			// Same reasoning as visitAdditiveExpr's "+" handling: BoxLang's PlusEqual is strictly
			// numeric, so when the right side is SYNTACTICALLY a string literal/GString, desugar
			// into `left = left & right` (string concat) instead of the numeric PlusEqual.
			case "+=" -> isStringLiteralExpr( ctx.expression( 1 ) )
			    ? desugarCompoundConcatAssign( ctx, pos, src )
			    : new BoxAssignment( ctx.expression( 0 ).accept( this ), BoxAssignmentOperator.PlusEqual, ctx.expression( 1 ).accept( this ),
			        List.of(), pos, src );
			case "-=" -> new BoxAssignment( ctx.expression( 0 ).accept( this ), BoxAssignmentOperator.MinusEqual, ctx.expression( 1 ).accept( this ),
			    List.of(), pos, src );
			case "*=" -> new BoxAssignment( ctx.expression( 0 ).accept( this ), BoxAssignmentOperator.StarEqual, ctx.expression( 1 ).accept( this ),
			    List.of(), pos, src );
			case "/=" -> new BoxAssignment( ctx.expression( 0 ).accept( this ), BoxAssignmentOperator.SlashEqual, ctx.expression( 1 ).accept( this ),
			    List.of(), pos, src );
			case "%=" -> new BoxAssignment( ctx.expression( 0 ).accept( this ), BoxAssignmentOperator.ModEqual, ctx.expression( 1 ).accept( this ),
			    List.of(), pos, src );
			// No direct BoxAssignmentOperator equivalent for these (BoxLang's ConcatEqual is "&=" string
			// concat, NOT bitwise-and-assign, so it must not be reused for Groovy's "&="). Desugar into
			// `left = left OP right` instead, re-visiting the left-hand side twice so each occurrence in
			// the rebuilt tree is an independent node instance.
			case "**=" -> desugarCompoundAssign( ctx, BoxBinaryOperator.Power, pos, src );
			case "&=" -> desugarCompoundAssign( ctx, BoxBinaryOperator.BitwiseAnd, pos, src );
			case "|=" -> desugarCompoundAssign( ctx, BoxBinaryOperator.BitwiseOr, pos, src );
			case "^=" -> desugarCompoundAssign( ctx, BoxBinaryOperator.BitwiseXor, pos, src );
			case "<<=" -> desugarCompoundAssign( ctx, BoxBinaryOperator.BitwiseSignedLeftShift, pos, src );
			case ">>=" -> desugarCompoundAssign( ctx, BoxBinaryOperator.BitwiseSignedRightShift, pos, src );
			default -> throw new ExpressionException( "Unknown assignment operator: " + assignOpText, pos, src );
		};
	}

	private BoxAssignment desugarCompoundAssign( AssignExprContext ctx, BoxBinaryOperator op, Position pos, String src ) {
		BoxExpression	leftForAssignTarget	= ctx.expression( 0 ).accept( this );
		BoxExpression	leftForBinaryOp		= ctx.expression( 0 ).accept( this );
		BoxExpression	right				= ctx.expression( 1 ).accept( this );
		BoxExpression	combined			= new BoxBinaryOperation( leftForBinaryOp, op, right, pos, src );
		return new BoxAssignment( leftForAssignTarget, BoxAssignmentOperator.Equal, combined, List.of(), pos, src );
	}

	private BoxAssignment desugarCompoundConcatAssign( AssignExprContext ctx, Position pos, String src ) {
		BoxExpression	leftForAssignTarget	= ctx.expression( 0 ).accept( this );
		BoxExpression	leftForConcat		= ctx.expression( 0 ).accept( this );
		BoxExpression	right				= ctx.expression( 1 ).accept( this );
		BoxExpression	combined			= new BoxStringConcat( List.of( leftForConcat, right ), pos, src );
		return new BoxAssignment( leftForAssignTarget, BoxAssignmentOperator.Equal, combined, List.of(), pos, src );
	}

	// -----------------------------------------------------------------------------------------
	// Primary / literals

	@Override
	public BoxExpression visitIdentifierExpr( IdentifierExprContext ctx ) {
		var		pos			= tools.getPosition( ctx );
		var		src			= tools.getSourceText( ctx );
		String	name		= ctx.getText();
		String	ownerClass	= staticImportedMembers.get( name );
		if ( ownerClass != null ) {
			return new BoxStaticAccess( new BoxIdentifier( ownerClass, pos, ownerClass ), false, new BoxIdentifier( name, pos, name ), pos, src );
		}
		return new BoxIdentifier( name, pos, src );
	}

	@Override
	public BoxExpression visitIntLiteralExpr( IntLiteralExprContext ctx ) {
		return new BoxIntegerLiteral( stripNumericSuffix( ctx.getText() ), tools.getPosition( ctx ), tools.getSourceText( ctx ) );
	}

	@Override
	public BoxExpression visitFloatLiteralExpr( FloatLiteralExprContext ctx ) {
		return new BoxDecimalLiteral( stripNumericSuffix( ctx.getText() ), tools.getPosition( ctx ), tools.getSourceText( ctx ) );
	}

	@Override
	public BoxExpression visitHexLiteralExpr( HexLiteralExprContext ctx ) {
		return buildRadixIntegerLiteral( ctx.getText(), 2, 16, tools.getPosition( ctx ), tools.getSourceText( ctx ) );
	}

	@Override
	public BoxExpression visitBinaryLiteralExpr( BinaryLiteralExprContext ctx ) {
		return buildRadixIntegerLiteral( ctx.getText(), 2, 2, tools.getPosition( ctx ), tools.getSourceText( ctx ) );
	}

	@Override
	public BoxExpression visitOctalLiteralExpr( OctalLiteralExprContext ctx ) {
		// Unlike HEX_LITERAL/BINARY_LITERAL, an octal literal has no separate prefix character to
		// skip past - just the leading "0" itself - so prefixLength is 1, not 2.
		return buildRadixIntegerLiteral( ctx.getText(), 1, 8, tools.getPosition( ctx ), tools.getSourceText( ctx ) );
	}

	// A trailing L/G/F/D/I type suffix (Groovy's 100000000000L, 10.5G, 5F, etc.) is recognized and
	// stripped here so the literal text is plain digits BoxIntegerLiteral/BoxDecimalLiteral can
	// parse - those AST nodes have no concept of a Groovy type suffix. Underscore digit separators
	// don't need handling here: both AST nodes already strip "_" themselves (see
	// IBoxSimpleLiteral#removeUnderscores), the same generic support BoxLang's own literal syntax
	// uses. Honestly documented, not silently overstated: stripping the suffix makes the literal
	// PARSE, but doesn't force a distinct runtime type beyond what BoxLang's own length-based int/
	// long/BigDecimal selection (see BoxIntegerLiteralTransformer) already produces for that many
	// digits.
	private static final String NUMERIC_SUFFIX_CHARS = "lLgGfFdDiI";

	private String stripNumericSuffix( String text ) {
		char last = text.charAt( text.length() - 1 );
		return NUMERIC_SUFFIX_CHARS.indexOf( last ) >= 0 ? text.substring( 0, text.length() - 1 ) : text;
	}

	// HEX_LITERAL/BINARY_LITERAL's own trailing suffix set (see GroovyLexer.g4) - deliberately NOT
	// the shared NUMERIC_SUFFIX_CHARS used by plain decimal literals below: "F"/"D" are valid HEX
	// digits, so stripping them unconditionally would corrupt a literal like "0xFF" into "0xF".
	private static final String RADIX_SUFFIX_CHARS = "lLgGiI";

	// Converts a "0x1F_00L"/"0b1010_1010"-shaped literal into the plain decimal digit string
	// BoxIntegerLiteral expects - BoxIntegerLiteralTransformer dispatches purely on that string's
	// length, with no concept of a radix prefix, so hex/binary literals must be pre-converted to
	// decimal text here rather than passed through as-is.
	private BoxExpression buildRadixIntegerLiteral( String text, int prefixLength, int radix, Position pos, String src ) {
		char	last	= text.charAt( text.length() - 1 );
		String	trimmed	= RADIX_SUFFIX_CHARS.indexOf( last ) >= 0 ? text.substring( 0, text.length() - 1 ) : text;
		String	body	= trimmed.substring( prefixLength ).replace( "_", "" );
		var		value	= new java.math.BigInteger( body, radix );
		return new BoxIntegerLiteral( value.toString(), pos, src );
	}

	@Override
	public BoxExpression visitTrueLiteralExpr( TrueLiteralExprContext ctx ) {
		return new BoxBooleanLiteral( Boolean.TRUE, tools.getPosition( ctx ), tools.getSourceText( ctx ) );
	}

	@Override
	public BoxExpression visitFalseLiteralExpr( FalseLiteralExprContext ctx ) {
		return new BoxBooleanLiteral( Boolean.FALSE, tools.getPosition( ctx ), tools.getSourceText( ctx ) );
	}

	@Override
	public BoxExpression visitNullLiteralExpr( NullLiteralExprContext ctx ) {
		return new BoxNull( tools.getPosition( ctx ), tools.getSourceText( ctx ) );
	}

	@Override
	public BoxExpression visitThisExpr( ThisExprContext ctx ) {
		return new BoxIdentifier( "this", tools.getPosition( ctx ), tools.getSourceText( ctx ) );
	}

	@Override
	public BoxExpression visitSuperExpr( SuperExprContext ctx ) {
		return new BoxIdentifier( "super", tools.getPosition( ctx ), tools.getSourceText( ctx ) );
	}

	@Override
	public BoxExpression visitStringExpr( StringExprContext ctx ) {
		return buildStringOrGString( ctx.stringOrGString(), tools.getPosition( ctx ), tools.getSourceText( ctx ) );
	}

	private BoxExpression buildStringOrGString( StringOrGStringContext ctx, Position pos, String src ) {
		if ( ctx.SQUOTE_STRING() != null ) {
			String text = ctx.SQUOTE_STRING().getText();
			return new BoxStringLiteral( unescapeSingleQuoted( stripQuotes( text, 1 ) ), pos, src );
		}
		if ( ctx.SLASHY_STRING() != null ) {
			// Non-interpolated, single-line - see GroovyLexer's SLASHY_STRING rule header comment
			// for exactly what's simplified. "\/" is the only recognized escape; everything else
			// (crucially, other backslash sequences like "\d") passes through untouched so regex
			// metacharacters reach Pattern.compile() as-is.
			String text = stripQuotes( ctx.SLASHY_STRING().getText(), 1 );
			return new BoxStringLiteral( text.replace( "\\/", "/" ), pos, src );
		}
		if ( ctx.tripleGstring() != null ) {
			return buildGString( ctx.tripleGstring().gstringPart(), tools.getPosition( ctx.tripleGstring() ), tools.getSourceText( ctx.tripleGstring() ) );
		}
		return buildGString( ctx.gstring().gstringPart(), tools.getPosition( ctx.gstring() ), tools.getSourceText( ctx.gstring() ) );
	}

	private BoxExpression buildGString( List<GstringPartContext> gstringParts, Position pos, String src ) {
		List<BoxExpression> parts = new ArrayList<>();
		for ( GstringPartContext partCtx : gstringParts ) {
			if ( partCtx.GSTRING_TEXT() != null ) {
				parts.add( new BoxStringLiteral( unescapeGStringText( partCtx.GSTRING_TEXT().getText() ), tools.getPosition( partCtx ),
				    tools.getSourceText( partCtx ) ) );
			} else if ( partCtx.GSTRING_DOLLAR_IDENTIFIER() != null ) {
				// "$name" or "$a.b.c" shorthand - strip the leading '$' and split into a dotted access chain
				String			text		= partCtx.GSTRING_DOLLAR_IDENTIFIER().getText().substring( 1 );
				String[]		segments	= text.split( "\\." );
				BoxExpression	current		= new BoxIdentifier( segments[ 0 ], tools.getPosition( partCtx ), segments[ 0 ] );
				for ( int i = 1; i < segments.length; i++ ) {
					current = new BoxDotAccess( current, false, new BoxIdentifier( segments[ i ], tools.getPosition( partCtx ), segments[ i ] ),
					    tools.getPosition( partCtx ), text );
				}
				parts.add( current );
			} else {
				parts.add( partCtx.expression().accept( this ) );
			}
		}
		// A GString with no interpolated parts at all behaves exactly like a plain string - build it
		// as one so it doesn't pay runtime interpolation overhead for nothing.
		boolean hasInterpolation = gstringParts.stream().anyMatch( p -> p.GSTRING_DOLLAR_LBRACE() != null || p.GSTRING_DOLLAR_IDENTIFIER() != null );
		if ( !hasInterpolation ) {
			String text = parts.stream().map( p -> ( ( BoxStringLiteral ) p ).getValue() ).reduce( "", String::concat );
			return new BoxStringLiteral( text, pos, src );
		}
		return new BoxStringInterpolation( parts, pos, src );
	}

	@Override
	public BoxExpression visitCollectionExpr( CollectionExprContext ctx ) {
		return ctx.listOrMapLiteral().accept( this );
	}

	@Override
	public BoxExpression visitEmptyListLiteral( EmptyListLiteralContext ctx ) {
		return new BoxArrayLiteral( List.of(), tools.getPosition( ctx ), tools.getSourceText( ctx ) );
	}

	@Override
	public BoxExpression visitListLiteral( ListLiteralContext ctx ) {
		List<BoxExpression> values = ctx.listElement().stream().map( this::buildListElement ).toList();
		return new BoxArrayLiteral( values, tools.getPosition( ctx ), tools.getSourceText( ctx ) );
	}

	// STAR-prefixed spread list element (e.g. [*listA, *listB]) expands a collection's elements in
	// place at literal-construction time - the exact same BoxSpreadExpression AST shape a spread
	// call argument produces (see buildArgument above), so BoxArrayLiteralTransformer already knows
	// how to expand it with no Groovy-specific handling needed downstream of this visitor.
	private BoxExpression buildListElement( ListElementContext ctx ) {
		if ( ctx instanceof SpreadListElementContext spreadCtx ) {
			return new BoxSpreadExpression( spreadCtx.expression().accept( this ), tools.getPosition( spreadCtx ), tools.getSourceText( spreadCtx ) );
		}
		return ( ( PlainListElementContext ) ctx ).expression().accept( this );
	}

	@Override
	public BoxExpression visitEmptyMapLiteral( EmptyMapLiteralContext ctx ) {
		return new BoxStructLiteral( BoxStructType.Ordered, List.of(), tools.getPosition( ctx ), tools.getSourceText( ctx ) );
	}

	@Override
	public BoxExpression visitMapLiteral( MapLiteralContext ctx ) {
		// BoxStructLiteral takes a flat alternating key/value list, except for a BoxSpreadExpression
		// entry, which occupies a SINGLE slot (see BoxStructLiteral#isLiteral, which advances past a
		// spread entry by 1, not 2) - the same shape a spread struct entry already produces
		// elsewhere in the compiler, so no Groovy-specific handling is needed downstream of this
		// visitor to expand it.
		List<BoxExpression> values = new ArrayList<>();
		for ( MapEntryOrSpreadContext entryCtx : ctx.mapEntryOrSpread() ) {
			if ( entryCtx instanceof SpreadMapEntryContext spreadCtx ) {
				values.add( new BoxSpreadExpression( spreadCtx.expression().accept( this ), tools.getPosition( spreadCtx ),
				    tools.getSourceText( spreadCtx ) ) );
				continue;
			}
			MapEntryContext	entry	= ( ( PlainMapEntryContext ) entryCtx ).mapEntry();
			BoxExpression	key;
			if ( entry.mapKey().IDENTIFIER() != null ) {
				key = new BoxStringLiteral( entry.mapKey().IDENTIFIER().getText(), tools.getPosition( entry.mapKey() ),
				    tools.getSourceText( entry.mapKey() ) );
			} else if ( entry.mapKey().stringOrGString() != null ) {
				key = buildStringOrGString( entry.mapKey().stringOrGString(), tools.getPosition( entry.mapKey() ), tools.getSourceText( entry.mapKey() ) );
			} else {
				key = entry.mapKey().expression().accept( this );
			}
			values.add( key );
			values.add( entry.expression().accept( this ) );
		}
		return new BoxStructLiteral( BoxStructType.Ordered, values, tools.getPosition( ctx ), tools.getSourceText( ctx ) );
	}

	@Override
	public BoxExpression visitClosureLiteralExpr( ClosureLiteralExprContext ctx ) {
		return visitClosure( ctx.closure() );
	}

	public BoxExpression visitClosure( ClosureContext ctx ) {
		var	pos		= tools.getPosition( ctx );
		var	src		= tools.getSourceText( ctx );
		var	args	= ctx.closureParams() == null
		    // No explicit parameter list ("{ it * 2 }") - Groovy implicitly binds the single
		    // argument to "it", so declare it as such rather than leaving the closure param-less.
		    ? new ArrayList<>( List.of(
		        new ortus.boxlang.compiler.ast.statement.BoxArgumentDeclaration( false, "Any", "it", null, List.of(), List.of(), pos, src ) ) )
		    : ctx.closureParams().parameter().stream().map( statementVisitor::buildParameterDeclaration ).collect( java.util.stream.Collectors.toList() );
		var	body	= statementVisitor.buildStatementBlockFromBlockStatements( ctx.blockStatements(), pos, src );
		return new BoxClosure( args, List.of(), body, pos, src );
	}

	@Override
	public BoxExpression visitNewInstanceExpr( NewInstanceExprContext ctx ) {
		var					pos		= tools.getPosition( ctx );
		var					src		= tools.getSourceText( ctx );
		List<BoxArgument>	args	= buildArguments( ctx.argumentList() );
		if ( ctx.LBRACE() == null ) {
			return new BoxNew( null, toTypeExpression( ctx.typeName() ), args, pos, src );
		}
		// Anonymous inner class (new Runnable() { void run() {...} }). BoxLocalClass has a hard
		// compiler-level rule (BoxLocalClassTransformer, both backends): nesting one inside a
		// function/closure/lambda body is a compile-time error, everywhere, not just a Groovy
		// parser choice - and an anonymous class is almost always written INSIDE a method body.
		// So the synthesized class can never stay where it's written: it's hoisted out to the
		// nearest enclosing class body (or the script's own top level, for a top-level function)
		// via GroovyVisitor#buildClassMemberBody's draining, and only a "new <synthetic-name>(...)"
		// is left behind at this expression's own position.
		// Multiple anonymous classes ARE supported, including siblings reachable from different
		// statements in the same enclosing scope - see pushHoistScope/popHoistScope for how each
		// class-shaped body (script top level, a real class, or another anonymous class) gets its
		// own isolated nesting level, so a class hoisted from an EARLIER sibling expression is never
		// misattributed as having been discovered INSIDE this one's own body.
		String			anonymousName	= "__GroovyAnon" + ( ++anonymousClassCounter );
		BoxIdentifier	nameId			= new BoxIdentifier( anonymousName, pos, anonymousName );
		BoxStatement	localClass		= statementVisitor.buildAnonymousLocalClass( nameId, ctx.classBody(), pos, src );
		currentHoistScope().add( localClass );
		BoxExpression	newExpr			= new BoxNew( null, new BoxFQN( anonymousName, pos, anonymousName ), args, pos, src );

		// Real Java interop: when the anonymous class's own declared type is CONFIDENTLY a real
		// Java interface (see resolveJavaInterfaceFqn), wrap the constructed instance in a real
		// JDK dynamic proxy implementing it, via BoxLang's own createDynamicProxy() BIF - so the
		// result is genuinely passable to Java code expecting that exact interface type (e.g.
		// "new Thread(new Runnable() {...}).start()"), not just duck-typed from BoxLang's own
		// side. The anonymous BoxLocalClass itself still declares no "implements" annotation
		// (see buildAnonymousLocalClass's own header) - only the returned VALUE is wrapped.
		// Deliberately conservative: a bare, unqualified type name is only treated as a Java
		// interface when it's in the small KNOWN_JAVA_INTERFACES set below, or already fully
		// qualified under "java."/"javax." - anything else (a custom/BoxLang-native type, or an
		// unrecognized bare Java name) falls back to the plain, un-proxied instance exactly as
		// before, since createDynamicProxy() would otherwise throw trying to Class.forName() a
		// name that was never a real, loadable Java interface to begin with.
		String			interfaceFqn	= resolveJavaInterfaceFqn( ctx.typeName().getText() );
		if ( interfaceFqn == null ) {
			return newExpr;
		}
		List<BoxArgument> proxyArgs = List.of(
		    new BoxArgument( newExpr, pos, src ),
		    new BoxArgument( new BoxStringLiteral( interfaceFqn, pos, interfaceFqn ), pos, src ) );
		return new BoxFunctionInvocation( "createDynamicProxy", proxyArgs, pos, src );
	}

	// Common java.lang/java.util functional/marker interfaces real-world anonymous-class Java
	// interop overwhelmingly targets - deliberately small and curated rather than a general
	// bare-name-to-FQN resolver (which would need real import/classpath awareness this
	// single-pass parser doesn't have). A fully-qualified name (containing a dot) under
	// "java."/"javax." is trusted directly without needing to be in this map.
	private static final java.util.Map<String, String> KNOWN_JAVA_INTERFACES = java.util.Map.of(
	    "Runnable", "java.lang.Runnable",
	    "Callable", "java.util.concurrent.Callable",
	    "Comparator", "java.util.Comparator",
	    "Comparable", "java.lang.Comparable",
	    "Iterable", "java.lang.Iterable",
	    "Iterator", "java.util.Iterator" );

	private String resolveJavaInterfaceFqn( String rawTypeName ) {
		if ( rawTypeName.startsWith( "java." ) || rawTypeName.startsWith( "javax." ) ) {
			return rawTypeName;
		}
		return KNOWN_JAVA_INTERFACES.get( rawTypeName );
	}

	private int											anonymousClassCounter	= 0;

	// A STACK of pending-hoisted-class lists, one frame per class-shaped body currently being
	// built (script top level, a real class/interface, or a synthesized anonymous class) - see
	// pushHoistScope/popHoistScope. A flat, single list (this class's original design) is wrong
	// for 2+ sibling anonymous classes: buildAnonymousLocalClass builds class B's own body (which
	// drains "whatever is currently pending" to attribute it to B) BEFORE the expression visiting
	// B itself gets a chance to add B to the list - so if sibling A was hoisted first (still
	// pending, not yet added, since visitNewInstanceExpr only adds its result once
	// buildAnonymousLocalClass returns), building B's body would drain and nest A INSIDE B,
	// hiding A from the real enclosing scope entirely (confirmed empirically: this was the exact
	// cause of a ClassNotFoundBoxLangException on the FIRST class once a second one was added, and
	// what forced the original one-per-scope guard). A stack fixes this: each body gets its own
	// isolated frame, so only classes genuinely discovered WITHIN that body's own construction are
	// ever attributed to it.
	private final java.util.Deque<List<BoxStatement>>	hoistScopes				= new java.util.ArrayDeque<>();

	private List<BoxStatement> currentHoistScope() {
		return hoistScopes.peek();
	}

	/**
	 * Starts a new hoisting nesting level - call before building any class-shaped body (script top
	 * level via GroovyParser#toAst, or a real/anonymous class body via GroovyVisitor#
	 * buildClassMemberBody) so anonymous classes discovered while building it are attributed to
	 * THIS body, not to whatever enclosing/sibling scope happened to still have classes pending.
	 */
	public void pushHoistScope() {
		hoistScopes.push( new ArrayList<>() );
	}

	/**
	 * Ends the current hoisting nesting level, returning exactly the anonymous classes discovered
	 * directly within it (not any belonging to an enclosing or sibling scope).
	 */
	public List<BoxStatement> popHoistScope() {
		return hoistScopes.pop();
	}

	// -----------------------------------------------------------------------------------------
	// Helpers

	private BoxExpression binary( ortus.boxlang.parser.antlr.GroovyGrammar.ExpressionContext left, BoxBinaryOperator op,
	    ortus.boxlang.parser.antlr.GroovyGrammar.ExpressionContext right, org.antlr.v4.runtime.ParserRuleContext ctx ) {
		return new BoxBinaryOperation( left.accept( this ), op, right.accept( this ), tools.getPosition( ctx ), tools.getSourceText( ctx ) );
	}

	private BoxIdentifier identifier( org.antlr.v4.runtime.tree.TerminalNode node ) {
		return new BoxIdentifier( node.getText(), tools.getPosition( node.getSymbol() ), node.getText() );
	}

	List<BoxArgument> buildArguments( ArgumentListContext ctx ) {
		if ( ctx == null ) {
			return new ArrayList<>();
		}
		List<BoxArgument>	args			= new ArrayList<>();
		List<BoxExpression>	namedEntries	= new ArrayList<>();
		for ( ArgumentContext argCtx : ctx.argument() ) {
			if ( argCtx instanceof NamedArgumentContext namedCtx ) {
				// Groovy's "name: value" call arguments (e.g. foo(name: "x")) are collected into
				// a single trailing Map/struct argument - the same flat alternating key/value
				// shape visitMapLiteral already builds for "[a: 1]" map literals.
				namedEntries.add( new BoxStringLiteral( namedCtx.IDENTIFIER().getText(), tools.getPosition( namedCtx.IDENTIFIER().getSymbol() ),
				    namedCtx.IDENTIFIER().getText() ) );
				namedEntries.add( namedCtx.expression().accept( this ) );
			} else {
				args.add( buildArgument( argCtx ) );
			}
		}
		if ( !namedEntries.isEmpty() ) {
			var	pos	= tools.getPosition( ctx );
			var	src	= tools.getSourceText( ctx );
			args.add( new BoxArgument( new BoxStructLiteral( BoxStructType.Ordered, namedEntries, pos, src ), pos, src ) );
		}
		return args;
	}

	// Groovy's STAR-prefixed spread argument (func(*list)) expands a list's elements as
	// individual positional arguments at that call site - wrapping the expression in a
	// BoxSpreadExpression is the exact same AST shape CFGrammar's own "...expr" spread argument
	// produces, so the shared compiler pipeline expands it identically at compile time with no
	// Groovy-specific handling needed downstream of this visitor.
	private BoxArgument buildArgument( ArgumentContext argCtx ) {
		var	pos	= tools.getPosition( argCtx );
		var	src	= tools.getSourceText( argCtx );
		if ( argCtx instanceof SpreadArgumentContext spreadCtx ) {
			BoxExpression spreadExpr = new BoxSpreadExpression( spreadCtx.expression().accept( this ), pos, src );
			return new BoxArgument( spreadExpr, pos, src );
		}
		PositionalArgumentContext positionalCtx = ( PositionalArgumentContext ) argCtx;
		return new BoxArgument( positionalCtx.expression().accept( this ), pos, src );
	}

	BoxExpression toTypeExpression( TypeNameContext ctx ) {
		return new BoxFQN( ctx.qualifiedName().getText(), tools.getPosition( ctx ), tools.getSourceText( ctx ) );
	}

	private String stripQuotes( String text, int quoteLength ) {
		return text.substring( quoteLength, text.length() - quoteLength );
	}

	private String unescapeSingleQuoted( String text ) {
		return text.replace( "\\'", "'" ).replace( "\\\\", "\\" );
	}

	private String unescapeGStringText( String text ) {
		return text.replace( "\\\"", "\"" ).replace( "\\\\", "\\" ).replace( "\\$", "$" );
	}

}
