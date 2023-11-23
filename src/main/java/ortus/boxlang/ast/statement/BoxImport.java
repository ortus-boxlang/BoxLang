package ortus.boxlang.ast.statement;

import java.util.Map;

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

	@Override
	public Map<String, Object> toMap() {
		Map<String, Object> map = super.toMap();

		map.put( "expression", expression.toMap() );
		if ( alias != null ) {
			map.put( "alias", alias.toMap() );
		} else {
			map.put( "alias", null );
		}
		return map;
	}
}
