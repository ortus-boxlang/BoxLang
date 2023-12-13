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
 */
public class Key {

	/**
	 * --------------------------------------------------------------------------
	 * Private Properties
	 * --------------------------------------------------------------------------
	 */

	/**
	 * The original key name
	 */
	protected String	name;

	/**
	 * The key name in upper case
	 */
	protected String	nameNoCase;

	/**
	 * The original value of the key, which could be a complex object
	 * if this key was being used to derefernce a native Map.
	 */
	protected Object	originalValue;

	/**
	 * Keys are immutable, so we can cache the hash code
	 */
	protected int		hashCode;

	/**
	 * --------------------------------------------------------------------------
	 * Constructors
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Constructor
	 *
	 * @param name The target key to use, which is the original case.
	 */
	public Key( String name ) {
		this.name			= name;
		this.originalValue	= name;
		this.nameNoCase		= name.toUpperCase();
		this.hashCode		= this.nameNoCase.hashCode();
	}

	/**
	 * Constructor for a key that is not a string
	 *
	 * @param name The target key to use, which is the original case.
	 */
	public Key( String name, Object originalValue ) {
		this.name			= name;
		this.originalValue	= originalValue;
		this.nameNoCase		= name.toUpperCase();
		this.hashCode		= this.nameNoCase.hashCode();
	}

	/**
	 * --------------------------------------------------------------------------
	 * Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * @return The key name in upper case.
	 */
	public String getNameNoCase() {
		return this.nameNoCase;
	}

	/**
	 * @return The original key case.
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * @return The original value of the key, which could be a complex object
	 */
	public Object getOriginalValue() {
		return this.originalValue;
	}

	/**
	 * Verifies equality with the following rules:
	 * - Same object
	 * - Same key name (case-insensitive)
	 *
	 * @param obj The object to compare against.
	 */
	@Override
	public boolean equals( Object obj ) {
		// Same object
		if ( this == obj ) {
			return true;
		}

		if ( obj != null && obj instanceof Key key ) {
			// Same key name
			return hashCode() == key.hashCode();
		}

		return false;
	}

	/**
	 * Verifies equality with the following rules:
	 * - Same object
	 * - Same key name (case-sensitive)
	 *
	 * @param obj The object to compare against.
	 *
	 * @return True if the objects are equal.
	 */
	public boolean equalsWithCase( Object obj ) {
		// Same object
		if ( this == obj )
			return true;
		// Null and class checks
		if ( obj == null || ! ( obj instanceof Key ) ) {
			return false;
		}
		// Same key name
		return getName().equals( ( ( Key ) obj ).getName() );
	}

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
	 * @param name The key name to use.
	 *
	 * @return A case-insensitive key class
	 */
	public static Key of( String name ) {
		return new Key( name );
	}

	/**
	 * Static builder of a case-insensitive key using the incoming key name
	 *
	 * @param name The key name to use.
	 *
	 * @return A case-insensitive key class
	 */
	public static Key of( Object obj ) {
		return new Key( obj.toString(), obj );
	}

	/**
	 * Static builder of an Integer key
	 *
	 * @param name The key name to use.
	 *
	 * @return A case-insensitive key class
	 */
	public static IntKey of( Integer obj ) {
		return new IntKey( obj );
	}

	/**
	 * Static builder of an int key
	 *
	 * @param name The key name to use.
	 *
	 * @return A case-insensitive key class
	 */
	public static IntKey of( int obj ) {
		return new IntKey( obj );
	}

	/**
	 * Static builder of a Double key
	 *
	 * @param name The key name to use.
	 *
	 * @return An IntKey instance if the Double was an integer, otherwise a Key instance.
	 */
	public static Key of( Double obj ) {
		return Key.of( obj.doubleValue() );
	}

	/**
	 * Static builder of an int key
	 *
	 * @param name The key name to use.
	 *
	 * @return An IntKey instance if the Double was an integer, otherwise a Key instance.
	 */
	public static Key of( double obj ) {
		if ( obj == ( int ) obj ) {
			return new IntKey( ( int ) obj );
		} else {
			return new Key( String.valueOf( obj ), obj );
		}
	}

	/**
	 * Static builder of case-insensitive key trackers using an incoming array of key names
	 *
	 * @param names The key names to use. This can be on or more.
	 *
	 * @return An array of case-insensitive key classes
	 */
	public static Key[] of( String... names ) {
		return Arrays.stream( names ).map( Key::of ).toArray( Key[]::new );
	}

	/**
	 * The string representation of the key which includes
	 * the original case and the upper case version.
	 *
	 * @return The string representation of the key
	 */
	@Override
	public String toString() {
		return String.format( "Key [name=%s, nameNoCase=%s]", name, nameNoCase );
	}

}
