package ortus.boxlang.compiler.toolchain;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import ortus.boxlang.compiler.ast.BoxExpression;
import ortus.boxlang.compiler.ast.BoxStatement;
import ortus.boxlang.compiler.ast.expression.*;
import ortus.boxlang.compiler.ast.statement.BoxAnnotation;
import ortus.boxlang.compiler.ast.statement.BoxArgumentDeclaration;
import ortus.boxlang.parser.antlr.BoxScriptGrammar;
import ortus.boxlang.parser.antlr.BoxScriptGrammarBaseVisitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BoxExpressionVisitor extends BoxScriptGrammarBaseVisitor<BoxExpression> {

	private final Tools			tools				= new Tools();
	private final BoxVisitor	statementVisitor	= new BoxVisitor();

	/**
	 * Manufactures an AST node that indicates that the wrapped expression is in parentheses.
	 * <p>
	 *
	 * Generally, one does not explicitly put this in an AST. If we need to regenerate the source we can see that
	 * an expression was parenthesised because the operator precedence will be different. However,
	 * in some cases it is useful to have this information in the AST, for instance if we wish to preserve
	 * redundant parentheses.
	 * </p>
	 *
	 * @param ctx the parse tree
	 * 
	 * @return The AST for the parenthesised expression
	 */
	@Override
	public BoxExpression visitExprPrecedence( BoxScriptGrammar.ExprPrecedenceContext ctx ) {
		var	pos	= tools.getPosition( ctx );
		var	src	= tools.getSourceText( ctx );
		return new BoxParenthesis( ctx.expression().accept( this ), pos, src );
	}

	@Override
	public BoxExpression visitExprUnary( BoxScriptGrammar.ExprUnaryContext ctx ) {
		var	pos		= tools.getPosition( ctx );
		var	src		= tools.getSourceText( ctx );
		var	right	= ctx.expression().accept( this );
		var	op		= switch ( ctx.op.getType() ) {
						case BoxScriptGrammar.PLUS -> BoxUnaryOperator.Plus;
						case BoxScriptGrammar.MINUS -> BoxUnaryOperator.Minus;
						case BoxScriptGrammar.NOT -> BoxUnaryOperator.Not;
						default -> null;  // Cannot happen - satisfy the compiler
					};
		return new BoxUnaryOperation( right, op, pos, src );
	}

	@Override
	public BoxExpression visitExprPostfix( BoxScriptGrammar.ExprPostfixContext ctx ) {
		var	pos		= tools.getPosition( ctx );
		var	src		= tools.getSourceText( ctx );
		var	left	= ctx.expression().accept( this );
		var	op		= switch ( ctx.op.getType() ) {
						case BoxScriptGrammar.PLUSPLUS -> BoxUnaryOperator.PostPlusPlus;
						case BoxScriptGrammar.MINUSMINUS -> BoxUnaryOperator.PostMinusMinus;
						default -> null;  // Cannot happen - satisfy the compiler
					};
		return new BoxUnaryOperation( left, op, pos, src );
	}

	@Override
	public BoxExpression visitExprPrefix( BoxScriptGrammar.ExprPrefixContext ctx ) {
		var	pos		= tools.getPosition( ctx );
		var	src		= tools.getSourceText( ctx );
		var	right	= ctx.expression().accept( this );
		var	op		= switch ( ctx.op.getType() ) {
						case BoxScriptGrammar.PLUSPLUS -> BoxUnaryOperator.PrePlusPlus;
						case BoxScriptGrammar.MINUSMINUS -> BoxUnaryOperator.PreMinusMinus;
						case BoxScriptGrammar.BITWISE_COMPLEMENT -> BoxUnaryOperator.BitwiseComplement;
						default -> null;  // Cannot happen - satisfy the compiler
					};
		return new BoxUnaryOperation( right, op, pos, src );
	}

	/**
	 * visits the Dot accessor operation and generates the relevant AST.
	 * <p>
	 * With dot accessors, there some special cases where the left and right are folded
	 * into one node rather than encapsulated into a DotAccess. For example method
	 * invocations will be seen on the right of x.y() and the invocation AST for y()
	 * will have the x reference folded into it.
	 * </p>
	 *
	 * @param ctx the parse tree
	 *
	 * @return the AST for a particular accessor operation
	 */
	@Override
	public BoxExpression visitExprDotAccess( BoxScriptGrammar.ExprDotAccessContext ctx ) {
		var	pos		= tools.getPosition( ctx );
		var	src		= tools.getSourceText( ctx );
		var	left	= ctx.expression( 0 ).accept( this );
		var	right	= ctx.expression( 1 ).accept( this );

		if ( right instanceof BoxMethodInvocation invocation ) {

			// The method invocation needs to know what it is being invoked upon
			invocation.setObj( left );
			invocation.setSafe( ctx.QM() != null );
			return invocation;
		} else if ( right instanceof BoxArrayAccess arrayAccess ) {
			return new BoxArrayAccess( left, ctx.QM() != null, arrayAccess.getAccess(), pos, src );
		} else {
			return new BoxDotAccess( left, ctx.QM() != null, right, pos, src );
		}
	}

	@Override
	public BoxExpression visitExprPower( BoxScriptGrammar.ExprPowerContext ctx ) {
		var	pos		= tools.getPosition( ctx );
		var	src		= tools.getSourceText( ctx );
		var	left	= ctx.expression( 0 ).accept( this );
		var	right	= ctx.expression( 1 ).accept( this );
		return new BoxBinaryOperation( left, BoxBinaryOperator.Power, right, pos, src );
	}

	@Override
	public BoxExpression visitExprMult( BoxScriptGrammar.ExprMultContext ctx ) {
		var	pos		= tools.getPosition( ctx );
		var	src		= tools.getSourceText( ctx );
		var	left	= ctx.expression( 0 ).accept( this );
		var	right	= ctx.expression( 1 ).accept( this );
		var	op		= switch ( ctx.op.getType() ) {
						case BoxScriptGrammar.STAR -> BoxBinaryOperator.Star;
						case BoxScriptGrammar.SLASH -> BoxBinaryOperator.Slash;
						case BoxScriptGrammar.MOD -> BoxBinaryOperator.Mod;
						case BoxScriptGrammar.BACKSLASH -> BoxBinaryOperator.Backslash;
						default -> null;  // Cannot happen - satisfy the compiler
					};
		return new BoxBinaryOperation( left, op, right, pos, src );
	}

	@Override
	public BoxExpression visitExprAdd( BoxScriptGrammar.ExprAddContext ctx ) {
		var	pos		= tools.getPosition( ctx );
		var	src		= tools.getSourceText( ctx );
		var	left	= ctx.expression( 0 ).accept( this );
		var	right	= ctx.expression( 1 ).accept( this );
		var	op		= switch ( ctx.op.getType() ) {
						case BoxScriptGrammar.PLUS -> BoxBinaryOperator.Plus;
						case BoxScriptGrammar.MINUS -> BoxBinaryOperator.Minus;
						default -> null; // Cannot happen - satisfy the compiler
					};
		return new BoxBinaryOperation( left, BoxBinaryOperator.Plus, right, pos, src );
	}

	@Override
	public BoxExpression visitExprBitShift( BoxScriptGrammar.ExprBitShiftContext ctx ) {
		var	pos		= tools.getPosition( ctx );
		var	src		= tools.getSourceText( ctx );
		var	left	= ctx.expression( 0 ).accept( this );
		var	right	= ctx.expression( 1 ).accept( this );
		var	op		= switch ( ctx.op.getType() ) {
						case BoxScriptGrammar.BITWISE_SIGNED_LEFT_SHIFT -> BoxBinaryOperator.BitwiseSignedLeftShift;
						case BoxScriptGrammar.BITWISE_SIGNED_RIGHT_SHIFT -> BoxBinaryOperator.BitwiseSignedRightShift;
						case BoxScriptGrammar.BITWISE_UNSIGNED_RIGHT_SHIFT -> BoxBinaryOperator.BitwiseUnsignedRightShift;
						default -> null;  // Cannot happen - satisfy the compiler
					};
		return new BoxBinaryOperation( left, op, right, pos, src );
	}

	@Override
	public BoxExpression visitExprBinary( BoxScriptGrammar.ExprBinaryContext ctx ) {
		var	pos		= tools.getPosition( ctx );
		var	src		= tools.getSourceText( ctx );
		var	left	= ctx.expression( 0 ).accept( this );
		var	right	= ctx.expression( 1 ).accept( this );
		var	op		= buildBinOp( ctx.binOps() );
		return new BoxBinaryOperation( left, op, right, pos, src );
	}

	@Override
	public BoxExpression visitExprBAnd( BoxScriptGrammar.ExprBAndContext ctx ) {
		var	pos		= tools.getPosition( ctx );
		var	src		= tools.getSourceText( ctx );
		var	left	= ctx.expression( 0 ).accept( this );
		var	right	= ctx.expression( 1 ).accept( this );
		return new BoxBinaryOperation( left, BoxBinaryOperator.BitwiseAnd, right, pos, src );
	}

	@Override
	public BoxExpression visitExprBXor( BoxScriptGrammar.ExprBXorContext ctx ) {
		var	pos		= tools.getPosition( ctx );
		var	src		= tools.getSourceText( ctx );
		var	left	= ctx.expression( 0 ).accept( this );
		var	right	= ctx.expression( 1 ).accept( this );
		return new BoxBinaryOperation( left, BoxBinaryOperator.BitwiseXor, right, pos, src );
	}

	@Override
	public BoxExpression visitExprBor( BoxScriptGrammar.ExprBorContext ctx ) {
		var	pos		= tools.getPosition( ctx );
		var	src		= tools.getSourceText( ctx );
		var	left	= ctx.expression( 0 ).accept( this );
		var	right	= ctx.expression( 1 ).accept( this );
		return new BoxBinaryOperation( left, BoxBinaryOperator.BitwiseOr, right, pos, src );
	}

	@Override
	public BoxExpression visitExprRelational( BoxScriptGrammar.ExprRelationalContext ctx ) {
		var	pos		= tools.getPosition( ctx );
		var	src		= tools.getSourceText( ctx );
		var	left	= ctx.expression( 0 ).accept( this );
		var	right	= ctx.expression( 1 ).accept( this );
		var	op		= buildRelOp( ctx.relOps() );
		return new BoxComparisonOperation( left, op, right, pos, src );
	}

	@Override
	public BoxExpression visitExprEqual( BoxScriptGrammar.ExprEqualContext ctx ) {
		var	pos		= tools.getPosition( ctx );
		var	src		= tools.getSourceText( ctx );
		var	left	= ctx.expression( 0 ).accept( this );
		var	right	= ctx.expression( 1 ).accept( this );
		return new BoxComparisonOperation( left, BoxComparisonOperator.Equal, right, pos, src );
	}

	@Override
	public BoxExpression visitExprXor( BoxScriptGrammar.ExprXorContext ctx ) {
		var	pos		= tools.getPosition( ctx );
		var	src		= tools.getSourceText( ctx );
		var	left	= ctx.expression( 0 ).accept( this );
		var	right	= ctx.expression( 1 ).accept( this );
		return new BoxBinaryOperation( left, BoxBinaryOperator.Xor, right, pos, src );
	}

	@Override
	public BoxExpression visitExprCat( BoxScriptGrammar.ExprCatContext ctx ) {
		var					pos		= tools.getPosition( ctx );
		var					src		= tools.getSourceText( ctx );
		List<BoxExpression>	parts;

		var					left	= ctx.expression( 0 ).accept( this );
		var					right	= ctx.expression( 1 ).accept( this );

		// If the left is a concat, we can just add the right to it to chain the concatenation. The
		// code generator should check the parts and if both left and right are literal strings, then
		// it should concatenate them into a single string before code generation.
		if ( left instanceof BoxStringConcat concat ) {
			concat.getValues().add( right );
			concat.setValues( concat.getValues() );  // Cause parents to be reset
			return concat;
		}

		// If the left is not a concat, we need to create a new one
		parts = new ArrayList<>();
		parts.add( left );
		parts.add( right );

		return new BoxStringConcat( parts, pos, src );
	}

	@Override
	public BoxExpression visitExprNotContains( BoxScriptGrammar.ExprNotContainsContext ctx ) {
		var	pos		= tools.getPosition( ctx );
		var	src		= tools.getSourceText( ctx );
		var	left	= ctx.expression( 0 ).accept( this );
		var	right	= ctx.expression( 1 ).accept( this );
		return new BoxBinaryOperation( left, BoxBinaryOperator.NotContains, right, pos, src );
	}

	@Override
	public BoxExpression visitExprAnd( BoxScriptGrammar.ExprAndContext ctx ) {
		var	pos		= tools.getPosition( ctx );
		var	src		= tools.getSourceText( ctx );
		var	left	= ctx.expression( 0 ).accept( this );
		var	right	= ctx.expression( 1 ).accept( this );
		return new BoxBinaryOperation( left, BoxBinaryOperator.And, right, pos, src );
	}

	@Override
	public BoxExpression visitExprOr( BoxScriptGrammar.ExprOrContext ctx ) {
		var	pos		= tools.getPosition( ctx );
		var	src		= tools.getSourceText( ctx );
		var	left	= ctx.expression( 0 ).accept( this );
		var	right	= ctx.expression( 1 ).accept( this );
		return new BoxBinaryOperation( left, BoxBinaryOperator.Or, right, pos, src );
	}

	/**
	 * Generate the ELVIS AST node.
	 *
	 * @apiNote Elvis needs boats
	 *
	 * @param bermudaTriangle the parse tree
	 *
	 * @return The binary operation representing Elvis
	 */
	@Override
	public BoxExpression visitExprElvis( BoxScriptGrammar.ExprElvisContext bermudaTriangle ) {
		var	pos			= tools.getPosition( bermudaTriangle );
		var	src			= tools.getSourceText( bermudaTriangle );
		var	elvisDock	= bermudaTriangle.expression( 0 ).accept( this );
		var	boat		= bermudaTriangle.expression( 1 ).accept( this );
		return new BoxBinaryOperation( elvisDock, BoxBinaryOperator.Elvis, boat, pos, src );
	}

	@Override
	public BoxExpression visitExprInstanceOf( BoxScriptGrammar.ExprInstanceOfContext ctx ) {
		var	pos		= tools.getPosition( ctx );
		var	src		= tools.getSourceText( ctx );
		var	left	= ctx.expression( 0 ).accept( this );
		var	right	= ctx.expression( 1 ).accept( this );
		return new BoxBinaryOperation( left, BoxBinaryOperator.InstanceOf, right, pos, src );
	}

	@Override
	public BoxExpression visitExprCastAs( BoxScriptGrammar.ExprCastAsContext ctx ) {
		var	pos		= tools.getPosition( ctx );
		var	src		= tools.getSourceText( ctx );
		var	left	= ctx.expression( 0 ).accept( this );
		var	right	= ctx.expression( 1 ).accept( this );
		return new BoxBinaryOperation( left, BoxBinaryOperator.CastAs, right, pos, src );
	}

	@Override
	public BoxExpression visitExprTernary( BoxScriptGrammar.ExprTernaryContext ctx ) {
		var	pos			= tools.getPosition( ctx );
		var	src			= tools.getSourceText( ctx );
		var	condition	= ctx.expression( 0 ).accept( this );
		var	trueExpr	= ctx.expression( 1 ).accept( this );
		var	falseExpr	= ctx.expression( 2 ).accept( this );
		return new BoxTernaryOperation( condition, trueExpr, falseExpr, pos, src );
	}

	@Override
	public BoxExpression visitExprAssign( BoxScriptGrammar.ExprAssignContext ctx ) {
		var	pos		= tools.getPosition( ctx );
		var	src		= tools.getSourceText( ctx );
		var	left	= ctx.expression( 0 ).accept( this );
		var	right	= ctx.expression( 1 ).accept( this );
		var	op		= buildAssignOp( ctx.op );
		return new BoxAssignment( left, op, right, null, pos, src );
	}

	@Override
	public BoxExpression visitExprOutString( BoxScriptGrammar.ExprOutStringContext ctx ) {
		return ctx.expression().accept( this );
	}

	@Override
	public BoxExpression visitExprArrayAccess( BoxScriptGrammar.ExprArrayAccessContext ctx ) {
		var	pos		= tools.getPosition( ctx );
		var	src		= tools.getSourceText( ctx );
		var	object	= ctx.expression( 0 ).accept( this );
		var	access	= ctx.expression( 1 ).accept( this );
		return new BoxArrayAccess( object, false, access, pos, src );
	}

	@Override
	public BoxExpression visitExprArrayLiteral( BoxScriptGrammar.ExprArrayLiteralContext ctx ) {
		var	pos		= tools.getPosition( ctx );
		var	src		= tools.getSourceText( ctx );
		var	values	= Optional.ofNullable( ctx.expressionList() )
		    .map( expressionList -> expressionList.expression().stream()
		        .map( expr -> expr.accept( this ) )
		        .collect( Collectors.toList() ) )
		    .orElse( Collections.emptyList() );
		return new BoxArrayLiteral( values, pos, src );
	}

	@Override
	public BoxExpression visitClosureFunc( BoxScriptGrammar.ClosureFuncContext ctx ) {
		var								pos				= tools.getPosition( ctx );
		var								src				= tools.getSourceText( ctx );
		List<BoxArgumentDeclaration>	params			= Optional.ofNullable( ctx.functionParamList() )
		    .map( paramList -> paramList.functionParam().stream()
		        .map( param -> ( BoxArgumentDeclaration ) param.accept( statementVisitor ) )
		        .collect( Collectors.toList() ) )
		    .orElse( Collections.emptyList() );

		var								body			= ctx.statementBlock().accept( statementVisitor );

		List<BoxAnnotation>				postAnnotations	= Optional.ofNullable( ctx.postAnnotation() )
		    .map( postAnnotationList -> postAnnotationList.stream()
		        .map( postAnnotation -> ( BoxAnnotation ) postAnnotation.accept( statementVisitor ) )
		        .collect( Collectors.toList() ) )
		    .orElse( Collections.emptyList() );

		return new BoxClosure( params, postAnnotations, ( BoxStatement ) body, pos, src );
	}

	@Override
	public BoxExpression visitLambdaFunc( BoxScriptGrammar.LambdaFuncContext ctx ) {
		var								pos				= tools.getPosition( ctx );
		var								src				= tools.getSourceText( ctx );

		// The parameters are either a single identifier or a list of parameters, but will never be both.
		// So rather than have lots of if statements, we can just concatenate the two streams, the identifier
		// stream only ever returning one element.
		List<BoxArgumentDeclaration>	params			= Stream.concat(
		    Optional.ofNullable( ctx.identifier() )
		        .map( identifier -> new BoxArgumentDeclaration( false, "Any", identifier.getText(), null, new ArrayList<>(),
		            new ArrayList<>(), tools.getPosition( identifier ), tools.getSourceText( identifier ) ) )
		        .stream(),
		    Optional.ofNullable( ctx.functionParamList() )
		        .map( paramList -> paramList.functionParam().stream()
		            .map( param -> ( BoxArgumentDeclaration ) param.accept( statementVisitor ) ) )
		        .orElseGet( Stream::empty ) )
		    .collect( Collectors.toList() );

		var								body			= ctx.statement().accept( statementVisitor );

		List<BoxAnnotation>				postAnnotations	= Optional.ofNullable( ctx.postAnnotation() )
		    .map( postAnnotationList -> postAnnotationList.stream()
		        .map( postAnnotation -> ( BoxAnnotation ) postAnnotation.accept( statementVisitor ) )
		        .collect( Collectors.toList() ) )
		    .orElse( Collections.emptyList() );

		return new BoxLambda( params, postAnnotations, ( BoxStatement ) body, pos, src );
	}

	@Override
	public BoxMethodInvocation visitExprFunctionCall( BoxScriptGrammar.ExprFunctionCallContext ctx ) {
		var	pos		= tools.getPosition( ctx );
		var	src		= tools.getSourceText( ctx );
		var	name	= ctx.expression().accept( this );
		var	args	= Optional.ofNullable( ctx.argumentList() )
		    .map( argumentList -> argumentList.argument().stream().map( arg -> ( BoxArgument ) arg.accept( this ) ).toList() )
		    .orElse( Collections.emptyList() );

		// We do not know what we invoked it on yet, so we will let the Dot operator handle it
		// TODO: Probably better to return this as a function call, then translate to MethodInvocation in DOT
		return new BoxMethodInvocation( ( BoxIdentifier ) name, args, pos, src );
	}

	@Override
	public BoxExpression visitExprStaticAccess( BoxScriptGrammar.ExprStaticAccessContext ctx ) {
		var	pos		= tools.getPosition( ctx );
		var	src		= tools.getSourceText( ctx );
		var	left	= ctx.expression( 0 ).accept( this );
		var	right	= ctx.expression( 1 ).accept( this );
		return new BoxStaticAccess( left, false, right, pos, src );
	}

	@Override
	public BoxExpression visitExprNew( BoxScriptGrammar.ExprNewContext ctx ) {
		return ctx.new_().accept( this );
	}

	@Override
	public BoxExpression visitNew( BoxScriptGrammar.NewContext ctx ) {
		var					pos		= tools.getPosition( ctx );
		var					src		= tools.getSourceText( ctx );
		BoxIdentifier		prefix;
		BoxExpression		expr;

		List<BoxArgument>	args	= Optional.ofNullable( ctx.argumentList() )
		    .map( argumentList -> argumentList.argument().stream().map( arg -> ( BoxArgument ) arg.accept( this ) ).toList() )
		    .orElse( Collections.emptyList() );

		expr	= ctx.expression().accept( this );

		prefix	= Optional.ofNullable( ctx.PREFIX() ).map( token -> new BoxIdentifier( token.getText(), tools.getPosition( token ), token.getText() ) )
		    .orElse( null );

		return new BoxNew( prefix, expr, args, pos, src );
	}

	@Override
	public BoxExpression visitExprLiterals( BoxScriptGrammar.ExprLiteralsContext ctx ) {
		return ctx.literals().accept( this );
	}

	/**
	 * Visit the identifier context to generate the AST node for the identifier.
	 * <p>
	 * TODO: Note that the original code check to build a scope but then other AST constructors
	 * just deconstruct it. There may not need to be a separate scope object.
	 * </p>
	 *
	 * @param ctx the parse tree
	 *
	 * @return Either a BoxIdentifier or BoxScope AST node
	 */
	@Override
	public BoxExpression visitExprIdentifier( BoxScriptGrammar.ExprIdentifierContext ctx ) {
		if ( tools.isScope( ctx.getText() ) ) {
			return new BoxScope( ctx.getText(), tools.getPosition( ctx ), ctx.getText() );
		}
		return new BoxIdentifier( ctx.getText(), tools.getPosition( ctx ), ctx.getText() );
	}

	@Override
	public BoxExpression visitLiterals( BoxScriptGrammar.LiteralsContext ctx ) {
		return ctx.accept( this );
	}

	@Override
	public BoxExpression visitStringLiteral( BoxScriptGrammar.StringLiteralContext ctx ) {
		var	pos			= tools.getPosition( ctx );
		var	src			= tools.getSourceText( ctx );
		var	quoteChar	= ctx.getText().substring( 0, 1 );
		var	text		= ctx.getText().substring( 1, ctx.getText().length() - 1 );

		if ( ctx.expression().isEmpty() ) {
			return new BoxStringLiteral( tools.escapeStringLiteral( quoteChar, text ), pos, src );
		}

		var parts = ctx.children.stream()
		    .filter( it -> it instanceof BoxScriptGrammar.StringLiteralPartContext || it instanceof BoxScriptGrammar.ExpressionContext )
		    .map( it -> it instanceof BoxScriptGrammar.StringLiteralPartContext
		        ? new BoxStringLiteral( tools.escapeStringLiteral( quoteChar, tools.getSourceText( ( ParserRuleContext ) it ) ),
		            tools.getPosition( ( ParserRuleContext ) it ), tools.getSourceText( ( ParserRuleContext ) it ) )
		        : it.accept( this ) )
		    .toList();

		return new BoxStringInterpolation( parts, pos, src );
	}

	@Override
	public BoxExpression visitStructExpression( BoxScriptGrammar.StructExpressionContext ctx ) {
		var					pos				= tools.getPosition( ctx );
		var					src				= tools.getSourceText( ctx );
		var					type			= ctx.RBRACKET() != null ? BoxStructType.Ordered : BoxStructType.Unordered;
		var					structMembers	= ctx.structMembers();
		List<BoxExpression>	values			= structMembers != null
		    ? structMembers.structMember().stream().flatMap( it -> it.expression().stream() ).map( expr -> expr.accept( this ) ).toList()
		    : Collections.emptyList();
		return new BoxStructLiteral( type, values, pos, src );
	}

	@Override
	public BoxExpression visitExprAtoms( BoxScriptGrammar.ExprAtomsContext ctx ) {
		var	pos	= tools.getPosition( ctx.atoms().a );
		var	src	= tools.getSourceText( ctx.atoms() );
		return switch ( ctx.atoms().a.getType() ) {
			case BoxScriptGrammar.NULL -> new BoxNull( pos, src );
			case BoxScriptGrammar.TRUE -> new BoxBooleanLiteral( true, pos, src );
			case BoxScriptGrammar.FALSE -> new BoxBooleanLiteral( false, pos, src );
			case BoxScriptGrammar.INTEGER_LITERAL -> new BoxIntegerLiteral( src, pos, src );
			case BoxScriptGrammar.FLOAT_LITERAL -> new BoxDecimalLiteral( src, pos, src );
			default -> null;  // Cannot happen - satisfy the compiler
		};
	}

	@Override
	public BoxExpression visitFqn( BoxScriptGrammar.FqnContext ctx ) {
		var	pos	= tools.getPosition( ctx );
		var	src	= tools.getSourceText( ctx );
		return new BoxFQN( ctx.getText(), pos, src );
	}

	// ==============================================================================================
	// Builder methods
	//
	// Builders perform specialized task for the visitor functions where the task
	// is too complex to be done inline or otherwise obfuscates what the visitor is doing

	/**
	 * Visit the relational operator context to generate the AST node for the operator.
	 * <p>
	 * As the operations seem to have grown in the telling so to speak, there are
	 * some wierd and wonderful combinations that are utterly superfluous even as
	 * syntactic sugar. However, as they are in the language, we must deal with them.
	 * </p>
	 *
	 * @param ctx the parse tree
	 *
	 * @return the operation AST node
	 */
	public BoxComparisonOperator buildRelOp( BoxScriptGrammar.RelOpsContext ctx ) {

		// Convert the context to a string without whitespace. Then we can just have a string
		// switch
		var op = ctx.getText().replaceAll( "\\s+", "" ).toUpperCase();

		return switch ( op ) {
			case "GT", ">", "GREATERTHAN" -> BoxComparisonOperator.GreaterThan;
			case "GTE", ">=", "GE", "GREATERTHANOREQTO", "GREATERTHANOREQUALTO" ->
			    BoxComparisonOperator.GreaterThanEquals;
			case "===" ->
			    BoxComparisonOperator.TEqual;
			case "LE", "<=", "LTE", "LESSTHANOREQTO", "LESSTHANOREQUALTO" ->
			    BoxComparisonOperator.LessThanEquals;
			case "LT", "<", "LESSTHAN" ->
			    BoxComparisonOperator.LessThan;
			case "NE", "!=", "NOTEQUAL", "ISNOT", "<>" ->
			    BoxComparisonOperator.NotEqual;
			default ->
			    null; // Cannot happen - satisfy the compiler
		};
	}

	/**
	 * Visit the relational operator context to generate the AST node for the operator.
	 * <p>
	 * As the operations seem to have grown in the telling so to speak, there are
	 * some wierd and wonderful combinations that are utterly superfluous even as
	 * syntactic sugar. However, as they are in the language, we must deal with them.
	 * </p>
	 *
	 * @param ctx the parse tree
	 *
	 * @return the operation AST node
	 */
	public BoxBinaryOperator buildBinOp( BoxScriptGrammar.BinOpsContext ctx ) {

		// Convert the context to a string without whitespace. Then we can just have a string
		// switch
		var op = ctx.getText().replaceAll( "\\s+", "" ).toUpperCase();

		return switch ( op ) {
			case "EQV" ->
			    BoxBinaryOperator.Equivalence;
			case "IMP" ->
			    BoxBinaryOperator.Implies;
			case "CONTAINS" ->
			    BoxBinaryOperator.Contains;
			case "NOTCONTAINS" ->
			    BoxBinaryOperator.NotContains;
			default -> // Cannot happen - satisfy the compiler
			    null;
		};
	}

	/**
	 * Build the assignment operator from the token
	 *
	 * @param token The token to build the operator from
	 *
	 * @return The BoxAssignmentOperator AST
	 */
	private BoxAssignmentOperator buildAssignOp( Token token ) {
		return switch ( token.getType() ) {
			case BoxScriptGrammar.EQUALSIGN -> BoxAssignmentOperator.Equal;
			case BoxScriptGrammar.PLUSEQUAL -> BoxAssignmentOperator.PlusEqual;
			case BoxScriptGrammar.MINUSEQUAL -> BoxAssignmentOperator.MinusEqual;
			case BoxScriptGrammar.STAREQUAL -> BoxAssignmentOperator.StarEqual;
			case BoxScriptGrammar.SLASHEQUAL -> BoxAssignmentOperator.SlashEqual;
			case BoxScriptGrammar.MODEQUAL -> BoxAssignmentOperator.ModEqual;
			case BoxScriptGrammar.CONCATEQUAL -> BoxAssignmentOperator.ConcatEqual;
			default -> null;  // Cannot happen without grammar change - satisfy the compiler
		};
	}

	/**
	 * Build the scope for a new expression, given that we know the prefix should
	 * be a scope. Also used by the Identifier builder to build the scope if it detects
	 * that the identifier is one of the predefined scopes.
	 * <p>
	 * Note that this function does not check that the scope is valid and that should
	 * be done in the verification pass.
	 * </p>
	 *
	 * @param prefix The possibly COLON-suffixed string for scope generation.
	 *
	 * @return The BoxScope AST
	 */
	private BoxExpression buildScope( Token prefix ) {
		var scope = prefix.getText().replaceAll( "[:]+$", "" ).toUpperCase();
		return new BoxScope( scope, tools.getPosition( prefix ), prefix.getText() );
	}

}
