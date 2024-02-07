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
package ortus.boxlang.runtime.scopes;

import java.util.Arrays;

/**
 * Represents a case-insenstive key, while retaining the original case too.
 * Implements the Serializable interface in case duplication is requested within a native HashMap or ArrayList
 */
public class KeyCased extends Key {

	/**
	 * Constructor for a key that is not a string
	 *
	 * @param name The target key to use, which is the original case.
	 */
	public KeyCased( String name, Object originalValue ) {
		super( name, originalValue );
		this.hashCode = this.name.hashCode();
	}

	/**
	 * Constructor
	 *
	 * @param name The target key to use, which is the original case.
	 */
	public KeyCased( String name ) {
		this( name, name );
	}

	/**
	 * --------------------------------------------------------------------------
	 * Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * @return The hash code of the key name in upper case
	 */
	@Override
	public int hashCode() {
		return this.hashCode;
	}

	/**
	 * Static builder of a case-insensitive key using the incoming key name
	 *
	 * @param obj Object value to use as the key
	 *
	 * @return A case-insensitive key class
	 */
	public static Key of( Object obj ) {
		if ( obj instanceof Double d ) {
			return Key.of( d );
		}
		return new KeyCased( obj.toString(), obj );
	}

	/**
	 * Static builder of case-insensitive key trackers using an incoming array of key names
	 *
	 * @param names The key names to use. This can be on or more.
	 *
	 * @return An array of case-insensitive key classes
	 */
	public static Key[] of( String... names ) {
		return Arrays.stream( names ).map( KeyCased::of ).toArray( KeyCased[]::new );
	}

	/**
	 * The string representation of the key which includes
	 * the original case and the upper case version.
	 *
	 * @return The string representation of the key
	 */
	@Override
	public String toString() {
		return String.format( "Key [name=%s]", name );
	}

	/**
	 * Compare keys in a case-insensitive manner.
	 *
	 * @param otherKey The key to compare to.
	 *
	 * @return A negative integer, zero, or a positive integer if this key is less than, equal to, or greater than the specified key.
	 */
	@Override
	public int compareTo( Key otherKey ) {
		return this.name.compareTo( otherKey.name );
	}

	/**
	 * Compare keys in a case-sensitive manner.
	 *
	 * @param otherKey The key to compare to.
	 *
	 * @return A negative integer, zero, or a positive integer if this key is less than, equal to, or greater than the specified key.
	 */
	public int compareToWithCase( Key otherKey ) {
		return this.compareTo( otherKey );
	}

}
