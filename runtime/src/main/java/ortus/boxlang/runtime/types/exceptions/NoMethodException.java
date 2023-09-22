package ortus.boxlang.runtime.types.exceptions;

/**
 * Thrown when a scope is not found
 */
public class NoMethodException extends ExpressionException {

	/**
	 * Constructor
	 *
	 * @param message The message to display
	 */
	public NoMethodException( String message ) {
		super( message );
	}

	/**
	 * Constructor
	 *
	 * @param message The message to display
	 * @param cause   The cause
	 */
	public NoMethodException( String message, Throwable cause ) {
		super( message, cause );
	}

}
