/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ortus.boxlang.runtime.util;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Represents a simple name-value pair commonly used for HTTP headers, query parameters,
 * form fields, and other key-value data structures.
 * <p>
 * This immutable class provides a type-safe way to represent pairs where:
 * <ul>
 * <li>The name is always required (non-null)</li>
 * <li>The value is optional (nullable)</li>
 * </ul>
 * <p>
 * Common use cases include:
 * <ul>
 * <li>HTTP request/response headers</li>
 * <li>URL query parameters</li>
 * <li>HTML form fields</li>
 * <li>Cookie attributes</li>
 * <li>Configuration key-value pairs</li>
 * </ul>
 * <p>
 * Example usage:
 *
 * <pre>
 * // Create a header with a value
 * NameValuePair header = new NameValuePair( "Content-Type", "application/json" );
 *
 * // Create a flag-style parameter (no value)
 * NameValuePair flag = new NameValuePair( "verbose", null );
 *
 * // Create from array
 * NameValuePair param = NameValuePair.fromNativeArray( new String[] { "page", "1" } );
 *
 * // String representation
 * System.out.println( header.toString() ); // "Content-Type=application/json"
 * System.out.println( flag.toString() );   // "verbose"
 * </pre>
 * <p>
 * This class is immutable and thread-safe.
 *
 * @see java.util.Map.Entry
 */
public class NameValuePair {

	/**
	 * The name component of the pair. Never null.
	 */
	private final @NonNull String	name;

	/**
	 * The value component of the pair. May be null to represent a name-only entry
	 * (e.g., a flag or boolean parameter).
	 */
	private final @Nullable String	value;

	/**
	 * Pre-computed hash code for performance optimization.
	 * Computed once in constructor since this class is immutable.
	 */
	private final int				hashCode;

	/**
	 * ------------------------------------------------------------------------
	 * Constructor(s)
	 * ------------------------------------------------------------------------
	 */

	/**
	 * Constructs a new name-value pair.
	 *
	 * @param name  the name component (required, must not be null)
	 * @param value the value component (optional, may be null)
	 *
	 * @throws NullPointerException if name is null (enforced by @NonNull annotation)
	 */
	public NameValuePair( @NonNull String name, @Nullable String value ) {
		this.name		= name;
		this.value		= value;
		this.hashCode	= computeHashCode();
	}

	/**
	 * Computes the hash code for this pair based on name and value.
	 *
	 * @return the computed hash code
	 */
	private int computeHashCode() {
		int result = this.name.hashCode();
		result = 31 * result + ( this.value != null ? this.value.hashCode() : 0 );
		return result;
	}

	/**
	 * Creates a NameValuePair from a native String array.
	 * <p>
	 * This factory method supports two array formats:
	 * <ul>
	 * <li>Two-element array: [name, value] - creates a pair with both name and value</li>
	 * <li>Single-element array: [name] - creates a pair with name only (null value)</li>
	 * </ul>
	 * <p>
	 * This is particularly useful when parsing delimited strings or working with
	 * data structures that represent pairs as arrays.
	 * <p>
	 * Example:
	 *
	 * <pre>
	 *
	 * String input = "Content-Type=application/json";
	 * String[] parts = input.split( "=", 2 );
	 * NameValuePair pair = NameValuePair.fromNativeArray( parts );
	 * // pair.getName() returns "Content-Type"
	 * // pair.getValue() returns "application/json"
	 *
	 * String flag = "verbose";
	 * String[] flagParts = new String[] { flag };
	 * NameValuePair flagPair = NameValuePair.fromNativeArray( flagParts );
	 * // flagPair.getName() returns "verbose"
	 * // flagPair.getValue() returns null
	 * </pre>
	 *
	 * @param nameAndValue array containing name and optionally value (length must be 1 or 2)
	 *
	 * @return a new NameValuePair instance
	 *
	 * @throws ArrayIndexOutOfBoundsException if array is empty
	 * @throws NullPointerException           if array or first element is null
	 */
	public static NameValuePair fromNativeArray( String[] nameAndValue ) {
		if ( nameAndValue.length > 1 ) {
			return new NameValuePair( nameAndValue[ 0 ], nameAndValue[ 1 ] );
		}
		return new NameValuePair( nameAndValue[ 0 ], null );
	}

	/**
	 * ------------------------------------------------------------------------
	 * Getters
	 * ------------------------------------------------------------------------
	 */

	/**
	 * Returns the name component of this pair.
	 *
	 * @return the name (never null)
	 */
	@NonNull
	public String getName() {
		return name;
	}

	/**
	 * Returns the value component of this pair.
	 *
	 * @return the value, or null if this pair represents a name-only entry
	 */
	@Nullable
	public String getValue() {
		return value;
	}

	/**
	 * ------------------------------------------------------------------------
	 * Utility Methods
	 * ------------------------------------------------------------------------
	 */

	/**
	 * Returns a string representation of this name-value pair.
	 * <p>
	 * The format is:
	 * <ul>
	 * <li>If value is null: returns just the name (e.g., "verbose")</li>
	 * <li>If value is present: returns "name=value" (e.g., "Content-Type=application/json")</li>
	 * </ul>
	 * <p>
	 * This format is commonly used in HTTP headers, query strings, and form data.
	 *
	 * @return string representation of the pair
	 */
	public String toString() {
		if ( this.value == null ) {
			return this.name;
		}
		return this.name + "=" + this.value;
	}

	/**
	 * Compares this NameValuePair with another object for equality.
	 * <p>
	 * Two NameValuePair objects are considered equal if:
	 * <ul>
	 * <li>Both have the same name (case-sensitive comparison)</li>
	 * <li>Both have the same value (case-sensitive comparison, null equals null)</li>
	 * </ul>
	 * <p>
	 * This method is essential for using NameValuePair in collections like HashSet
	 * or as keys in HashMap.
	 *
	 * @param obj the object to compare with
	 *
	 * @return true if the objects are equal, false otherwise
	 */
	@Override
	public boolean equals( Object obj ) {
		if ( this == obj ) {
			return true;
		}
		if ( obj == null || getClass() != obj.getClass() ) {
			return false;
		}
		NameValuePair other = ( NameValuePair ) obj;
		if ( !this.name.equals( other.name ) ) {
			return false;
		}
		if ( this.value == null ) {
			return other.value == null;
		}
		return this.value.equals( other.value );
	}

	/**
	 * Returns the pre-computed hash code value for this NameValuePair.
	 * <p>
	 * The hash code is computed once in the constructor based on both the name
	 * and value fields, ensuring that equal objects have equal hash codes (as
	 * required by the general contract of hashCode).
	 * <p>
	 * This method enables proper use of NameValuePair in hash-based collections
	 * like HashSet, HashMap, and HashTable.
	 * <p>
	 * Performance note: Since this class is immutable, the hash code is pre-computed
	 * for optimal performance in hash-based collections.
	 *
	 * @return the pre-computed hash code value for this object
	 */
	@Override
	public int hashCode() {
		return this.hashCode;
	}

}
