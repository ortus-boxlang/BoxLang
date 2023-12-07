package ortus.boxlang.ast.expression;

import java.util.List;
import java.util.Map;

import ortus.boxlang.ast.BoxExpr;
import ortus.boxlang.ast.Position;
import ortus.boxlang.ast.statement.BoxAssignmentOperator;

/**
 * Assigment as expression
 */
public class BoxAssignment extends BoxExpr {

	private final BoxExpr						left;
	private final BoxExpr						right;
	private final BoxAssignmentOperator			op;
	private final List<BoxAssignmentModifier>	modifiers;

	/**
	 * Constructor
	 *
	 * @param left       left side of the assigment
	 * @param right      right side of the assigment
	 * @param position   position of the expression in the source code
	 * @param sourceText source code of the expression
	 */
	public BoxAssignment( BoxExpr left, BoxAssignmentOperator op, BoxExpr right, List<BoxAssignmentModifier> modifiers, Position position, String sourceText ) {
		super( position, sourceText );
		this.left = left;
		this.left.setParent( this );
		this.op		= op;
		this.right	= right;
		this.right.setParent( this );
		this.modifiers = modifiers;
	}

	public BoxExpr getLeft() {
		return left;
	}

	public BoxExpr getRight() {
		return right;
	}

	public BoxAssignmentOperator getOp() {
		return op;
	}

	public List<BoxAssignmentModifier> getModifiers() {
		return modifiers;
	}

	@Override
	public Map<String, Object> toMap() {
		Map<String, Object> map = super.toMap();

		map.put( "modifiers", modifiers.stream().map( op -> enumToMap( op ) ).toList() );
		map.put( "left", left.toMap() );
		map.put( "op", enumToMap( op ) );
		map.put( "right", right.toMap() );
		return map;
	}
}
