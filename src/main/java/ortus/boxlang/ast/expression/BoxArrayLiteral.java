package ortus.boxlang.ast.expression;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ortus.boxlang.ast.BoxExpr;
import ortus.boxlang.ast.Position;

/**
 * AST Node representing an array literal.
 * An array literal is surrounded by square braces []
 * and contains zero or more comma-delimited expressions.
 * Example
 * 
 * <pre>
 * []
 * [1,2,3]
 * ["foo","bar"]
 * [
 * [1,2],
 * [3,4],
 * "brad"
 * ]
 * </pre>
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

	@Override
	public Map<String, Object> toMap() {
		Map<String, Object> map = super.toMap();

		map.put( "values", values.stream().map( BoxExpr::toMap ).collect( Collectors.toList() ) );
		return map;
	}
}
