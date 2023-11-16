package ortus.boxlang.ast.expression;

import ortus.boxlang.ast.BoxExpr;
import ortus.boxlang.ast.Position;

import java.util.Collections;
import java.util.List;

/**
 * AST Node representing an array literal.
 * An array literal is surrounded by square braces []
 * and contains zero or more comma-delimited expressions.
 * Example
 * {@snippet :
 * []
 * [1,2,3]
 * ["foo","bar"]
 * [
 *   [1,2],
 *   [3,4],
 *   "brad"
 * ]
 * }
 * </code>
 */
public class BoxArrayLiteral extends BoxExpr {

	private final List<BoxExpr> values;

	/**
	 * Creates the AST node for an anonymous argument
	 * 
	 * @param values     initialization values
	 * @param position   position of the statement in the source code
	 * @param sourceText source code that originated the Node
	 */
	public BoxArrayLiteral( List<BoxExpr> values, Position position, String sourceText ) {
		super( position, sourceText );
		this.values = Collections.unmodifiableList( values );
		this.values.forEach( arg -> arg.setParent( this ) );
	}

	public List<BoxExpr> getValues() {
		return values;
	}
}
