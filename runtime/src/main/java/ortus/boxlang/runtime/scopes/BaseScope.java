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

import java.util.Objects;

import ortus.boxlang.runtime.types.Struct;

/**
 * Base scope implementation. Extends HashMap for now. May want to switch to composition over inheritance, but this
 * is simpler for now and using the Key class provides our case insensitivity automatically.
 */
public class BaseScope extends Struct implements IScope {

	/**
	 * --------------------------------------------------------------------------
	 * Public Properties
	 * --------------------------------------------------------------------------
	 */

	/**
	 * --------------------------------------------------------------------------
	 * Private Properties
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Used for scope hunting. Lower numbers are searched first.
	 */
	private int lookupOrder;

	/**
	 * --------------------------------------------------------------------------
	 * Constructors
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Constructor
	 *
	 * @param lookupOrder Used for scope hunting. Lower numbers are searched first.
	 */
	public BaseScope( int lookupOrder ) {
		super();
		this.lookupOrder = lookupOrder;
	}

	/**
	 * Constructor with default lookup order
	 */
	public BaseScope() {
		super();
		this.lookupOrder = 0;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Returns the lookup order
	 */
	public int getLookupOrder() {
		return this.lookupOrder;
	}

	/**
	 * Verifies equality with the following rules:
	 * - Same object
	 * - Same state + super class
	 */
	@Override
	public boolean equals( Object obj ) {
		// Same object
		if ( this == obj ) {
			return true;
		}
		// Null and class checks
		if ( obj == null || getClass() != obj.getClass() ) {
			return false;
		}
		// State + Super
		return lookupOrder == ( ( BaseScope ) obj ).getLookupOrder() && super.equals( obj );
	}

	/**
	 * Hashes the lookupOrder and super class
	 */
	@Override
	public int hashCode() {
		return Objects.hash( this.lookupOrder, super.hashCode() );
	}
}
