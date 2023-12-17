package ortus.boxlang.ast.statement;

import ortus.boxlang.ast.BoxExpr;
import ortus.boxlang.ast.BoxNode;
import ortus.boxlang.ast.Position;
import ortus.boxlang.ast.expression.BoxFQN;

import java.util.Map;

/**
 * Represent a javadoc style documentation
 */
public class BoxDocumentationAnnotation extends BoxNode {

	private final BoxFQN	key;
	private final BoxExpr	value;

	/**
	 * Creates a Documentation AST node
	 *
	 * @param key        fqn of the documentation
	 * @param value      expression representing the value
	 * @param position   position of the statement in the source code
	 * @param sourceText source code that originated the Node
	 *
	 */
	public BoxDocumentationAnnotation( BoxFQN key, BoxExpr value, Position position, String sourceText ) {
		super( position, sourceText );
		this.key = key;
		this.key.setParent( this );
		this.value = value;
		this.value.setParent( this );
	}

	public BoxFQN getKey() {
		return key;
	}

	public BoxExpr getValue() {
		return value;
	}

	@Override
	public Map<String, Object> toMap() {
		Map<String, Object> map = super.toMap();

		map.put( "key", key.toMap() );
		map.put( "value", value.toMap() );
		return map;
	}
}
