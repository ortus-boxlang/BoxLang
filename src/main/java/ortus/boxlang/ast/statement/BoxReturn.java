package ortus.boxlang.ast.statement;

import ortus.boxlang.ast.BoxExpr;
import ortus.boxlang.ast.BoxStatement;
import ortus.boxlang.ast.Position;

/**
 * AST Node representing a return statement
 */
public class BoxReturn extends BoxStatement {

	private final BoxExpr expression;

	/**
	 * Creates the AST node
	 *
	 * @param expression argument expression to return
	 * @param position   position of the statement in the source code
	 * @param sourceText source code that originated the Node
	 */
	public BoxReturn( BoxExpr expression, Position position, String sourceText ) {
		super( position, sourceText );
		this.expression = expression;
		this.expression.setParent( this );
	}

	public BoxExpr getExpression() {
		return expression;
	}
}
