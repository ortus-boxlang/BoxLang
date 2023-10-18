package ortus.boxlang.ast.statement;

import ortus.boxlang.ast.BoxExpr;
import ortus.boxlang.ast.BoxStatement;
import ortus.boxlang.ast.Position;

/**
 * AST Node representing a rethrow statement
 */
public class BoxRethrow extends BoxStatement {

	/**
	 * Creates the AST node
	 *
	 * @param position   position of the statement in the source code
	 * @param sourceText source code that originated the Node
	 */
	public BoxRethrow( Position position, String sourceText ) {
		super( position, sourceText );
	}

}
