package ortus.boxlang.compiler.toolchain;

import ortus.boxlang.compiler.ast.BoxExpression;
import ortus.boxlang.compiler.ast.expression.BoxIdentifier;
import ortus.boxlang.parser.antlr.BoxScriptGrammar;
import ortus.boxlang.parser.antlr.BoxScriptGrammarBaseVisitor;

public class BoxExpressionVisitor extends BoxScriptGrammarBaseVisitor<BoxExpression> {
	private final Tools tools = new Tools();

	@Override
	public BoxExpression visitExprLiterals(BoxScriptGrammar.ExprLiteralsContext ctx) {
		return ctx.literals().accept(this);
	}

	@Override
	public BoxExpression visitExprIdentifier(BoxScriptGrammar.ExprIdentifierContext ctx) {
		return new BoxIdentifier(ctx.getText(), tools.getPosition(ctx), ctx.getText());
	}

}
