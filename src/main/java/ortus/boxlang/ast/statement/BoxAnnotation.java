package ortus.boxlang.ast.statement;

import java.util.Map;

import ortus.boxlang.ast.BoxExpr;
import ortus.boxlang.ast.BoxNode;
import ortus.boxlang.ast.Position;
import ortus.boxlang.ast.expression.BoxFQN;

/**
 * There are two methods of adding annotations to BoxLang methods and arguments.
 * The first is an inline key/value pairs on the method or argument
 * declaration where the value is an empty string if left out, or can be any object literal.
 */
public class BoxAnnotation extends BoxNode {

	private final BoxFQN	key;
	private final BoxExpr	value;

	/**
	 * Creates an Annotation AST node
	 *
	 * @param key        fqn of the annotation
	 * @param value      expression representing the value
	 * @param position   position of the statement in the source code
	 * @param sourceText source code that originated the Node
	 */
	public BoxAnnotation( BoxFQN key, BoxExpr value, Position position, String sourceText ) {
		super( position, sourceText );
		this.key	= key;
		this.value	= value;
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
