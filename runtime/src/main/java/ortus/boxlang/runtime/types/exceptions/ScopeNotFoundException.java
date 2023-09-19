package ortus.boxlang.runtime.types.exceptions;

/**
 * Thrown when a scope is not found
 */
public class ScopeNotFoundException extends ApplicationException {

	/**
	 * Constructor
	 *
	 * @param message The message to display
	 */
	public ScopeNotFoundException( String message ) {
		super( message );
	}

}
