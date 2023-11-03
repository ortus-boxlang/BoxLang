package ortus.boxlang.ast.statement;

import ortus.boxlang.ast.BoxExpr;
import ortus.boxlang.ast.BoxStatement;
import ortus.boxlang.ast.Position;

/**
 * AST Node representing an import statement
 */
public class BoxImport extends BoxStatement {

	private final BoxExpr	expression;
	private final BoxExpr	alias;

	/**
	 * Creates the AST node
	 *
	 * @param expression argument expression to assert
	 * @param position   position of the statement in the source code
	 * @param sourceText source code that originated the Node
	 */
	public BoxImport( BoxExpr expression, BoxExpr alias, Position position, String sourceText ) {
		super( position, sourceText );
		this.expression = expression;
		this.expression.setParent( this );
		this.alias = alias;
		if ( alias != null ) {
			this.alias.setParent( this );
		}
	}

	public BoxExpr getExpression() {
		return expression;
	}

	public BoxExpr getAlias() {
		return alias;
	}
}
