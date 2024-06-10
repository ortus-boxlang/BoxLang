package ortus.boxlang.compiler.toolchain;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import ortus.boxlang.compiler.ast.BoxExpression;
import ortus.boxlang.compiler.ast.expression.*;
import ortus.boxlang.parser.antlr.BoxScriptGrammar;
import ortus.boxlang.parser.antlr.BoxScriptGrammarBaseVisitor;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class BoxExpressionVisitor extends BoxScriptGrammarBaseVisitor<BoxExpression> {
	private final Tools tools = new Tools();

	@Override
	public BoxExpression visitExprDotAccess(BoxScriptGrammar.ExprDotAccessContext ctx) {
		var pos = tools.getPosition(ctx);
		var src = tools.getSourceText(ctx);
		var left = ctx.expression(0).accept(this);
		var right = ctx.expression(1).accept(this);

		if (right instanceof BoxMethodInvocation invocation) {

			// The method invocation needs to know what it is being invoked upon
			invocation.setObj(left);
			invocation.setSafe(ctx.QM() != null);
			return invocation;
		} else {
			return new BoxDotAccess(left, ctx.QM() != null, right, pos, src);
		}
	}

	@Override
	public BoxMethodInvocation visitExprFunctionCall(BoxScriptGrammar.ExprFunctionCallContext ctx) {
		var pos = tools.getPosition(ctx);
		var src = tools.getSourceText(ctx);
		var name = ctx.expression().accept(this);
		var args = Optional.ofNullable(ctx.argumentList())
						   .map(argumentList -> argumentList.argument()
															.stream()
															.map(arg -> (BoxArgument) arg.accept(this))
															.toList())
						   .orElse(Collections.emptyList());

		// We do not know what we invoked it on yet, so we will let the Dot operator handle it
		return new BoxMethodInvocation((BoxIdentifier) name, args, pos, src);
	}

	@Override
	public BoxExpression visitExprStaticAccess(BoxScriptGrammar.ExprStaticAccessContext ctx) {
		var pos = tools.getPosition(ctx);
		var src = tools.getSourceText(ctx);
		var left = ctx.expression(0)
					   .accept(this);
		var right = ctx.expression(1)
					  .accept(this);
		return new BoxStaticAccess(left,false, right, pos, src);
	}

	@Override
	public BoxExpression visitExprNew(BoxScriptGrammar.ExprNewContext ctx) {
		return ctx.new_()
				  .accept(this);
	}

	@Override
	public BoxExpression visitNew(BoxScriptGrammar.NewContext ctx) {
		var           pos    = tools.getPosition(ctx);
		var           src    = tools.getSourceText(ctx);
		BoxIdentifier prefix;
		BoxExpression expr;

		List<BoxArgument> args = Optional.ofNullable(ctx.argumentList())
										 .map(argumentList -> argumentList.argument()
																		  .stream()
																		  .map(arg -> (BoxArgument) arg.accept(this))
																		  .toList())
										 .orElse(Collections.emptyList());

		expr = ctx.expression()
				  .accept(this);

		prefix = Optional.ofNullable(ctx.PREFIX())
						 .map(token -> new BoxIdentifier(token.getText(), tools.getPosition(token), token.getText()))
						 .orElse(null);

		return new BoxNew(prefix, expr, args, pos, src);
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
	 * @return The BoxScope AST
	 */
	private BoxExpression buildScope(Token prefix) {
		var scope = prefix.getText()
						  .replaceAll("[:]+$", "")
						  .toUpperCase();
		return new BoxScope(scope, tools.getPosition(prefix), prefix.getText());
	}

	@Override
	public BoxExpression visitExprLiterals(BoxScriptGrammar.ExprLiteralsContext ctx) {
		return ctx.literals()
				  .accept(this);
	}

	/**
	 * Visit the identifier context to generate the AST node for the identifier.
	 * <p>
	 *     TODO: Note that the original code check to build a scope but then other AST constructors
	 *           just deconstruct it. There may not need to be a separate scope object.
	 * </p>
	 * @param ctx the parse tree
	 * @return Either a BoxIdentifier or BoxScope AST node
	 */
	@Override
	public BoxExpression visitExprIdentifier(BoxScriptGrammar.ExprIdentifierContext ctx) {
		if (tools.isScope(ctx.getText())) {
			return new BoxScope(ctx.getText(), tools.getPosition(ctx), ctx.getText());
		}
		return new BoxIdentifier(ctx.getText(), tools.getPosition(ctx), ctx.getText());
	}


	@Override
	public BoxExpression visitLiterals(BoxScriptGrammar.LiteralsContext ctx) {
		return ctx.accept(this);
	}

	@Override
	public BoxExpression visitStringLiteral(BoxScriptGrammar.StringLiteralContext ctx) {
		var pos = tools.getPosition(ctx);
		var src = tools.getSourceText(ctx);
		var quoteChar = ctx.getText()
						   .substring(0, 1);
		var text = ctx.getText()
					  .substring(1, ctx.getText()
									   .length() - 1);

		if (ctx.expression()
			   .isEmpty()) {
			return new BoxStringLiteral(tools.escapeStringLiteral(quoteChar, text), pos, src);
		}

		var parts = ctx.children.stream()
								.filter(it -> it instanceof BoxScriptGrammar.StringLiteralPartContext || it instanceof BoxScriptGrammar.ExpressionContext)
								.map(it -> it instanceof BoxScriptGrammar.StringLiteralPartContext ?
                                        new BoxStringLiteral(tools.escapeStringLiteral(quoteChar,
                                                                                       tools.getSourceText((ParserRuleContext) it)), tools.getPosition((ParserRuleContext) it), tools.getSourceText((ParserRuleContext) it)) : it.accept(this))
								.toList();

		return new BoxStringInterpolation(parts, pos, src);
	}

	@Override
	public BoxExpression visitStructExpression(BoxScriptGrammar.StructExpressionContext ctx) {
		var pos           = tools.getPosition(ctx);
		var src           = tools.getSourceText(ctx);
		var type          = ctx.RBRACKET() != null ? BoxStructType.Ordered : BoxStructType.Unordered;
		var structMembers = ctx.structMembers();
		List<BoxExpression> values = structMembers != null ? structMembers.structMember()
																		  .stream()
																		  .flatMap(it -> it.expression()
																						   .stream())
																		  .map(expr -> expr.accept(this))
																		  .toList() : Collections.emptyList();
		return new BoxStructLiteral(type, values, pos, src);
	}

	@Override
	public BoxExpression visitExprAtoms(BoxScriptGrammar.ExprAtomsContext ctx) {
		var pos = tools.getPosition(ctx.atoms().a);
		var src = tools.getSourceText(ctx.atoms());
		return switch (ctx.atoms().a.getType()) {
			case BoxScriptGrammar.NULL -> new BoxNull(pos, src);
			case BoxScriptGrammar.TRUE -> new BoxBooleanLiteral(true, pos, src);
			case BoxScriptGrammar.FALSE -> new BoxBooleanLiteral(false, pos, src);
			case BoxScriptGrammar.INTEGER_LITERAL -> new BoxIntegerLiteral(src, pos, src);
			case BoxScriptGrammar.FLOAT_LITERAL -> new BoxDecimalLiteral(src, pos, src);
			default -> null;  // Cannot happen - satisfy the compiler
		};
	}


}
