package ortus.boxlang.runtime.types.exceptions;

import ortus.boxlang.runtime.types.Struct;

/**
 * Thrown when a key is not found in a struct
 */
public class KeyNotFoundException extends BoxLangException {

	/**
	 * Constructor when we know the actual struct that was being searched
	 *
	 * @param message The message to display
	 * @param target  The struct that was being searched
	 */
	public KeyNotFoundException( String message, Struct target ) {
		super( message );
	}

	/**
	 * Constructor when we don't know the actual struct that was being searched
	 *
	 * @param message The message to display
	 */
	public KeyNotFoundException( String message ) {
		super( message );
	}

}
