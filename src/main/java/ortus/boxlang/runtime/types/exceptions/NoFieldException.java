package ortus.boxlang.runtime.types.exceptions;

/**
 * Thrown when a scope is not found
 */
public class NoFieldException extends ExpressionException {

	/**
	 * Constructor
	 *
	 * @param message The message to display
	 */
	public NoFieldException( String message ) {
		super( message );
	}

	/**
	 * Constructor
	 *
	 * @param message The message to display
	 * @param cause   The cause
	 */
	public NoFieldException( String message, Throwable cause ) {
		super( message, cause );
	}

}
