package ortus.boxlang.ast.statement;

import ortus.boxlang.ast.BoxStatement;
import ortus.boxlang.ast.Position;
import ortus.boxlang.ast.expression.BoxArgument;
import ortus.boxlang.ast.expression.BoxFQN;

import java.util.Collections;
import java.util.List;

/**
 * AST Node representing a new statement
 */
public class BoxNew extends BoxStatement {

	private final BoxFQN			fqn;
	private final List<BoxArgument>	arguments;

	/**
	 * Creates the AST node
	 *
	 * @param arguments  list of arguments
	 * @param position   position of the statement in the source code
	 * @param sourceText source code that originated the Node
	 */
	public BoxNew( BoxFQN fqn, List<BoxArgument> arguments, Position position, String sourceText ) {
		super( position, sourceText );
		this.fqn = fqn;
		this.fqn.setParent( this );
		this.arguments = Collections.unmodifiableList( arguments );
		this.arguments.forEach( arg -> arg.setParent( this ) );
	}

	public BoxFQN getFqn() {
		return fqn;
	}

	public List<BoxArgument> getArguments() {
		return arguments;
	}
}
