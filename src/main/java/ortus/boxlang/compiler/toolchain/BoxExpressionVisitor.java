package ortus.boxlang.compiler.toolchain;

import ortus.boxlang.compiler.ast.BoxExpression;
import ortus.boxlang.compiler.ast.expression.BoxBooleanLiteral;
import ortus.boxlang.compiler.ast.expression.BoxIdentifier;
import ortus.boxlang.compiler.ast.expression.BoxNull;
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

	@Override
	public BoxExpression visitExprAtoms(BoxScriptGrammar.ExprAtomsContext ctx) {
		var pos = tools.getPosition(ctx.atoms().a);
		var src = tools.getSourceText(ctx.atoms());
		return switch(ctx.atoms().a.getType()) {
			case BoxScriptGrammar.NULL -> new BoxNull(pos, src);
			case BoxScriptGrammar.TRUE -> new BoxBooleanLiteral(true, pos, src);
			case BoxScriptGrammar.FALSE -> new BoxBooleanLiteral(false, pos, src);
			default -> null;  // Cannot happen - satisfy the compiler
		};
	}
}
