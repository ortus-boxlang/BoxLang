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
import ortus.boxlang.runtime.types.exceptions.KeyNotFoundException;

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
	private String scopeName = "none";

	/**
	 * --------------------------------------------------------------------------
	 * Constructors
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Constructor
	 *
	 * @param scopeName The name of the scope
	 */
	public BaseScope( String scopeName ) {
		super();
		this.scopeName = scopeName;
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
		return scopeName;
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
		return this.scopeName == target.getName() == super.equals( obj );
	}

	/**
	 * Hashes the lookupOrder and super class
	 */
	@Override
	public int hashCode() {
		return Objects.hash( this.scopeName, super.hashCode() );
	}

	/**
	 * Derefernce this object by a key and return the value, or throw exception
	 *
	 * @return The requested obect
	 */
	public Object __dereference( Key name ) throws KeyNotFoundException {

		Object result = get( name );
		// Handle full null support
		if ( result != null ) {
			return result;
		}

		// Not found anywhere
		throw new KeyNotFoundException(
		        String.format( "The scope [%s] is not deferencable by key [%s].", this.scopeName, name.getName() )
		);
	}

	/**
	 * Dereference this object by a key and invoke the result as an invokable (UDF, java method)
	 *
	 * @return The requested object
	 */
	public Object __dereferenceAndInvoke( Key name ) throws KeyNotFoundException {
		Object object = __dereference( name );
		// Test if the object is invokable (a UDF or java call site) and invoke it or throw exception if not invokable
		// Ideally, the invoker logic is not here, but in a helper
		throw new RuntimeException( "not implemeneted yet" );
	}

	/**
	 * Safely derefernce this object by a key and return the value, or null if not found
	 *
	 * @return The requested object or null
	 */
	public Object __safeDereference( Key name ) {
		return get( name );
	}

	/**
	 * Get a scope from the context.  If not found, the parent context is asked.
	 * Search all konwn scopes
	 *
	 * @return The requested scope
	 */
	public void __assign( Key name, Object value ) {
		put( name, value);
	}
}
