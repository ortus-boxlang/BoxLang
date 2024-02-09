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

/**
 * Represents a key that represents an integer
 */
public class IntKey extends Key {

	/**
	 * --------------------------------------------------------------------------
	 * Private Properties
	 * --------------------------------------------------------------------------
	 */

	/**
	 * A direct reference to the int value so we don't have to cast.
	 */
	protected int intValue;

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
	public IntKey( int name ) {
		super( String.valueOf( name ), name );
		this.intValue = name;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Get int value
	 */
	public int getIntValue() {
		return this.intValue;
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

		if ( obj instanceof IntKey intKey ) {
			return getIntValue() == intKey.getIntValue();
		} else if ( obj instanceof Key key ) {
			return super.equals( key );
			// Not sure if there's a use csae for this, but it seems useful perhaps
		} else if ( obj instanceof Integer ) {
			return getIntValue() == ( ( Integer ) obj ).intValue();
		}
		return false;
	}

}
