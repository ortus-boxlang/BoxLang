package ortus.boxlang.compiler.toolchain;

public enum BoxType {

	/**
	 * A type that cannot be determined at compile time
	 */
	UNKNOWN,

	/**
	 * A definite integer
	 */
	INTEGER,

	/**
	 * A definite real number
	 */
	FLOAT,

	/**
	 * A definite string
	 */
	STRING,

	/**
	 * A definite boolean
	 */
	BOOLEAN,

	/**
	 * A definite object, but we don't know of what type
	 */
	OBJECT,

	/**
	 * We know it is some kind of numeric but not if it is INTEGER or REAL etc.
	 */
	NUMERIC,

	/**
	 * An error was discovered in the expression, such as incompatible types
	 */
	ERROR;

	@Override
	public String toString() {
		return switch (this) {
			case INTEGER -> "Integer";
			case FLOAT -> "Float";
			case STRING -> "String";
			case BOOLEAN -> "Boolean";
			case OBJECT -> "Object";
			case NUMERIC -> "Numeric";
			default -> "Unknown";
		};
	}

	/**
	 * Determine if the type is a numeric type o any kind
	 *
	 * @return true if the type is a numeric type
	 */
	public boolean isNumeric() {
		return this == INTEGER || this == FLOAT || this == NUMERIC;
	}
}