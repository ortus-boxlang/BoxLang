package ortus.boxlang.runtime.types.exceptions;

/**
 * Thrown when a scope is not found
 */
public class ScopeNotFoundException extends BoxLangException {

	/**
	 * Constructor
	 *
	 * @param message The message to display
	 */
	public ScopeNotFoundException( String message ) {
		super( message );
	}

}
