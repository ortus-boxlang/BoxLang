package ortus.boxlang.ast.statement;

import java.util.Map;

import ortus.boxlang.ast.BoxExpr;
import ortus.boxlang.ast.BoxStatement;
import ortus.boxlang.ast.Position;

/**
 * AST Node representing an import statement
 */
public class BoxInclude extends BoxStatement {

	private final BoxExpr file;

	/**
	 * Creates the AST node
	 *
	 * @param position   position of the statement in the source code
	 * @param sourceText source code that originated the Node
	 */
	public BoxInclude( BoxExpr expression, Position position, String sourceText ) {
		super( position, sourceText );
		this.file = expression;
		this.file.setParent( this );
	}

	public BoxExpr getFile() {
		return file;
	}

	@Override
	public Map<String, Object> toMap() {
		Map<String, Object> map = super.toMap();

		map.put( "file", file.toMap() );
		return map;
	}
}
