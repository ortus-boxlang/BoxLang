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
	 * Each scope can have a human friendly name
	 */
	private String name = "none";

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
	public BaseScope( int lookupOrder, String name ) {
		super();
		this.lookupOrder	= lookupOrder;
		this.name			= name;
	}

	/**
	 * Constructor with default lookup order
	 *
	 * @param name The name of the scope
	 */
	public BaseScope( String name ) {
		super();
		this.lookupOrder	= 0;
		this.name			= name;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Gets the name of the scope
	 *
	 * @return The name of the scope
	 */
	public String getName() {
		return name;
	}

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
		BaseScope target = ( BaseScope ) obj;
		return this.lookupOrder == target.getLookupOrder() && this.name == target.getName() == super.equals( obj );
	}

	/**
	 * Hashes the lookupOrder and super class
	 */
	@Override
	public int hashCode() {
		return Objects.hash( this.name, this.lookupOrder, super.hashCode() );
	}
}
