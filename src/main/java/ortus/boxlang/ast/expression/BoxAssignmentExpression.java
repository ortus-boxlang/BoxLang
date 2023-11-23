package ortus.boxlang.ast.expression;

import java.util.Map;

import ortus.boxlang.ast.BoxExpr;
import ortus.boxlang.ast.Position;

/**
 * Assigment as expression
 */
public class BoxAssignmentExpression extends BoxExpr {

	private final BoxExpr	left;
	private final BoxExpr	right;

	/**
	 * Constructor
	 *
	 * @param left       left side of the assigment
	 * @param right      right side of the assigment
	 * @param position   position of the expression in the source code
	 * @param sourceText source code of the expression
	 */
	public BoxAssignmentExpression( BoxExpr left, BoxExpr right, Position position, String sourceText ) {
		super( position, sourceText );
		this.left	= left;
		this.right	= right;
	}

	public BoxExpr getLeft() {
		return left;
	}

	public BoxExpr getRight() {
		return right;
	}

	@Override
	public Map<String, Object> toMap() {
		Map<String, Object> map = super.toMap();

		map.put( "left", left.toMap() );
		map.put( "right", right.toMap() );
		return map;
	}
}
