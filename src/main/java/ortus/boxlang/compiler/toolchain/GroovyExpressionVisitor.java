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
import java.util.Set;

import ortus.boxlang.compiler.ast.BoxExpression;
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
import ortus.boxlang.compiler.parser.GroovyParser;
import ortus.boxlang.parser.antlr.GroovyGrammar.AdditiveExprContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.ArgumentContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.ArgumentListContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.AsExprContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.AssignExprContext;
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
import ortus.boxlang.parser.antlr.GroovyGrammar.IdentifierExprContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.InExprContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.IndexExprContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.InstanceofExprContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.IntLiteralExprContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.ListLiteralContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.LogicalAndExprContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.LogicalOrExprContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.MapEntryContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.MapLiteralContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.MemberExprContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.MultiplicativeExprContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.NamedArgumentContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.NewInstanceExprContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.NullLiteralExprContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.ParenExprContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.PositionalArgumentContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.PostfixExprContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.PowerExprContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.PrimaryExprContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.RangeExprContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.RegexExprContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.RelationalExprContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.ShiftExprContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.SpreadArgumentContext;
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
	// call's last BoxArgument, exactly like Groovy itself desugars it.
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
		} else {
			args.add( closureArg );
		}
		return buildCallExpression( ctx.expression(), args, tools.getPosition( ctx ), tools.getSourceText( ctx ) );
	}

	private boolean isInjectCall( ExpressionContext calleeCtx ) {
		return calleeCtx instanceof MemberExprContext memberCtx && "inject".equals( memberCtx.IDENTIFIER().getText() );
	}

	private BoxExpression buildCallExpression( ExpressionContext callee, List<BoxArgument> args, Position pos, String src ) {
		if ( callee instanceof MemberExprContext memberCtx ) {
			return buildMethodInvocation( memberCtx, args, pos, src );
		}
		if ( callee instanceof PrimaryExprContext primaryCtx && primaryCtx.primary() instanceof IdentifierExprContext idCtx ) {
			return new BoxFunctionInvocation( idCtx.IDENTIFIER().getText(), args, pos, src );
		}
		return new BoxExpressionInvocation( callee.accept( this ), args, pos, src );
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
		BoxExpression		nameExpr	= aliasedIdentifier( ctx.IDENTIFIER() );
		BoxExpression		closureExpr	= visitClosure( ctx.closure() );
		List<BoxArgument>	args		= List.of(
		    new BoxArgument( closureExpr, tools.getPosition( ctx.closure() ), tools.getSourceText( ctx.closure() ) ) );
		return new BoxMethodInvocation( nameExpr, obj, args, safe, true, pos, src );
	}

	private BoxExpression buildMethodInvocation( MemberExprContext memberCtx, List<BoxArgument> args, Position pos, String src ) {
		if ( memberCtx.METHOD_POINTER() != null ) {
			throw new ExpressionException( "Method pointer expressions (.&) are not yet supported by the Groovy parser", pos, src );
		}
		BoxIdentifier	nameExpr	= aliasedIdentifier( memberCtx.IDENTIFIER() );
		BoxIdentifier	staticBase	= staticClassBase( memberCtx.expression() );
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
			throw new ExpressionException( "Method pointer expressions (.&) are not yet supported by the Groovy parser", pos, src );
		}
		BoxIdentifier staticBase = staticClassBase( ctx.expression() );
		if ( staticBase != null ) {
			return new BoxStaticAccess( staticBase, ctx.SAFE_DOT() != null, identifier( ctx.IDENTIFIER() ), pos, src );
		}
		boolean safe = ctx.SAFE_DOT() != null;
		return new BoxDotAccess( ctx.expression().accept( this ), safe, identifier( ctx.IDENTIFIER() ), pos, src );
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
		var op = ctx.LSHIFT() != null ? BoxBinaryOperator.BitwiseSignedLeftShift : BoxBinaryOperator.BitwiseSignedRightShift;
		return binary( ctx.expression( 0 ), op, ctx.expression( 1 ), ctx );
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
		return new BoxIdentifier( ctx.getText(), tools.getPosition( ctx ), tools.getSourceText( ctx ) );
	}

	@Override
	public BoxExpression visitIntLiteralExpr( IntLiteralExprContext ctx ) {
		return new BoxIntegerLiteral( ctx.getText(), tools.getPosition( ctx ), tools.getSourceText( ctx ) );
	}

	@Override
	public BoxExpression visitFloatLiteralExpr( FloatLiteralExprContext ctx ) {
		return new BoxDecimalLiteral( ctx.getText(), tools.getPosition( ctx ), tools.getSourceText( ctx ) );
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
		List<BoxExpression> values = ctx.expression().stream().map( e -> e.accept( this ) ).toList();
		return new BoxArrayLiteral( values, tools.getPosition( ctx ), tools.getSourceText( ctx ) );
	}

	@Override
	public BoxExpression visitEmptyMapLiteral( EmptyMapLiteralContext ctx ) {
		return new BoxStructLiteral( BoxStructType.Ordered, List.of(), tools.getPosition( ctx ), tools.getSourceText( ctx ) );
	}

	@Override
	public BoxExpression visitMapLiteral( MapLiteralContext ctx ) {
		// BoxStructLiteral takes a flat alternating key/value list.
		List<BoxExpression> values = new ArrayList<>();
		for ( MapEntryContext entry : ctx.mapEntry() ) {
			BoxExpression key;
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
		var					pos			= tools.getPosition( ctx );
		var					src			= tools.getSourceText( ctx );
		BoxExpression		typeExpr	= toTypeExpression( ctx.typeName() );
		List<BoxArgument>	args		= buildArguments( ctx.argumentList() );
		return new BoxNew( null, typeExpr, args, pos, src );
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
