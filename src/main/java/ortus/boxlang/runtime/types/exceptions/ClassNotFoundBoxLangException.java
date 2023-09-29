package ortus.boxlang.runtime.types.exceptions;

/**
 * Thrown when a key is not found in a struct
 */
public class ClassNotFoundBoxLangException extends ApplicationException {

	/**
	 * Constructor when we don't know the actual struct that was being searched
	 *
	 * @param message The message to display
	 */
	public ClassNotFoundBoxLangException( String message ) {
		super( message );
	}

	/**
	 * Constructor
	 *
	 * @param message The message to display
	 * @param cause   The cause
	 */
	public ClassNotFoundBoxLangException( String message, Throwable cause ) {
		super( message, cause );
	}

}
