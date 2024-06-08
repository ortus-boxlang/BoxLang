package ortus.boxlang.compiler.toolchain;

import ortus.boxlang.parser.antlr.BoxScriptGrammar;
import ortus.boxlang.parser.antlr.BoxScriptGrammarBaseVisitor;

/**
 * Resolves the type of  BoxScript expressions by visiting the expression's parse tree
 * and returning the type of the expression as evinced by primitives and operators, etc.
 * <p>
 * In a truly typeless language, it can be impossible to resolve the type of some expressions
 * at compile time. but as this visitor is used within the semantic analysis phase, we can
 * spot things that are resolvable, and leave the rest to the runtime.
 */
public class TypeResolver extends BoxScriptGrammarBaseVisitor<BoxType> {


	/**
	 * Unary expressions must be a numeric type as they will become so after evaluation.
	 * <p>
	 * If we can determine the type of the expression at compile time, we can narrow down the
	 * numeric type, otherwise we can only say that the expression is numeric.
	 *
	 * @param ctx the parse tree
	 * @return The type of the expression after unary operations are applied
	 */
	@Override
	public BoxType visitExprUnary(BoxScriptGrammar.ExprUnaryContext ctx) {
		// See if the expression yields a specific type and use it if it is any kind of numeric
		// otherwise, we have to assume that the runtime will coerce the value to a numeric type of
		// some kind.
		BoxType exprType = ctx.expression()
							  .accept(this);
		return exprType.isNumeric() ? exprType : BoxType.NUMERIC;
	}

	/**
	 * Check that the two operands for additive operation are numeric
	 */
	@Override
	public BoxType visitExprAdd(BoxScriptGrammar.ExprAddContext ctx) {
		BoxType left = ctx.expression(0)
						  .accept(this);
		BoxType right = ctx.expression(1)
						   .accept(this);
		if (!left.isNumeric() || !right.isNumeric()) {
			return BoxType.ERROR;
		}

		// Both operands are numeric, but can we determine the type more exactly?
		// For instance FLOAT + INTEGER yields FLOAT, and INTEGER + INTEGER yields INTEGER
		if (left == BoxType.FLOAT || right == BoxType.FLOAT) {
			return BoxType.FLOAT;
		}

		// As neither operand is FLOAT, we can safely assume that the result will be INTEGER
		return BoxType.INTEGER;
	}

	// And so on for all the other expression operations..

}
