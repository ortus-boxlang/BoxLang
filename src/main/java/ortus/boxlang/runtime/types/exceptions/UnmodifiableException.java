package ortus.boxlang.runtime.types.exceptions;

/**
 * Thrown when a key is not found in a struct
 */
public class UnmodifiableException extends ApplicationException {

	/**
	 * Constructor when we don't know the actual struct that was being searched
	 *
	 * @param message The message to display
	 */
	public UnmodifiableException( String message ) {
		super( message );
	}

	/**
	 * Constructor
	 *
	 * @param message The message to display
	 * @param cause   The cause
	 */
	public UnmodifiableException( String message, Throwable cause ) {
		super( message, cause );
	}

}
