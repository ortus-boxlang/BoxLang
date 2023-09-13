package ourtus.boxlang.ast.statement;

import ourtus.boxlang.ast.BoxExpr;
import ourtus.boxlang.ast.BoxStatement;
import ourtus.boxlang.ast.Position;

/**
 * AST Node representing an assigment statement
 */
public class BoxAssert extends BoxStatement {

	private final BoxExpr expression;

	/**
	 * Creates the AST node
	 * 
	 * @param expression argument expression to assert
	 * @param position   position of the statement in the source code
	 * @param sourceText source code that originated the Node
	 */
	public BoxAssert( BoxExpr expression, Position position, String sourceText ) {
		super( position, sourceText );
		this.expression = expression;
		this.expression.setParent( this );
	}

	public BoxExpr getExpression() {
		return expression;
	}
}
