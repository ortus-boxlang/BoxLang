package ortus.boxlang.runtime.types.exceptions;

import ortus.boxlang.runtime.types.Struct;

/**
 * Thrown when a scope is not found
 */
public class ScopeNotFoundException extends RuntimeException {

	/**
	 * Constructor when we know the actual struct that was being searched
	 *
	 * @param message The message to display
	 * @param target  The struct that was being searched
	 */
	public ScopeNotFoundException( String message, Struct target ) {
		super( message );
	}

	/**
	 * Constructor when we don't know the actual struct that was being searched
	 *
	 * @param message The message to display
	 */
	public ScopeNotFoundException( String message ) {
		super( message );
	}

}
