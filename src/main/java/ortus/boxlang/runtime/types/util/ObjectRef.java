package ortus.boxlang.runtime.types.util;

/**
 * I am a simple wrapper for a value which allows it to be passed by reference into a method, possibly modified in that method, and then the
 * modified value accessed without needing to return it from the method.
 * 
 */
public class ObjectRef<T> {

	/**
	 * The value being wrapped
	 */
	private T value;

	/**
	 * Constructor
	 * 
	 * @param value The value to wrap
	 */
	public ObjectRef( T value ) {
		this.value = value;
	}

	/**
	 * Factory method to create a new ObjectRef
	 * 
	 * @param value The value to wrap
	 * 
	 * @return A new ObjectRef wrapping the value
	 */
	public static <T> ObjectRef<T> of( T value ) {
		return new ObjectRef<>( value );
	}

	/**
	 * Get the value
	 */
	public T get() {
		return value;
	}

	/**
	 * Set the value
	 * 
	 * @param value The new value
	 */
	public void set( T value ) {
		this.value = value;
	}

	/**
	 * Helper method for bytecode that needs to represent a literal value as an expression
	 * which Java will accept as a statement.
	 * 
	 * @param o The value to return
	 * 
	 * @return The value
	 */
	public static Object echoValue( Object o ) {
		return o;
	}

}
