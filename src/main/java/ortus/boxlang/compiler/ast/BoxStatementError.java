package ortus.boxlang.compiler.ast;

import ortus.boxlang.compiler.ast.visitor.ReplacingBoxVisitor;
import ortus.boxlang.compiler.ast.visitor.VoidBoxVisitor;

public class BoxStatementError extends BoxStatement {

	/**
	 * Constructor
	 *
	 * @param position   position of the statement in the source code
	 * @param sourceText source code of the statement
	 */
	public BoxStatementError( Position position, String sourceText ) {
		super( position, sourceText );
	}

	/**
	 * Do not call as this node means the AST is in error
	 * 
	 * @param v the visitor implementation
	 */
	@Override
	public void accept( VoidBoxVisitor v ) {

	}

	/**
	 * Do not call as this indicates there was a parser error and you should not try to generate
	 * any code when the AST is in error.
	 *
	 * @param v the visitor implementation
	 * 
	 * @return null
	 */
	@Override
	public BoxNode accept( ReplacingBoxVisitor v ) {
		return null;
	}
}
