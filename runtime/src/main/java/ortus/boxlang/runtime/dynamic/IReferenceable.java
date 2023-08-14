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
package ortus.boxlang.runtime.dynamic;

import ortus.boxlang.runtime.types.exceptions.KeyNotFoundException;

import ortus.boxlang.runtime.scopes.Key;

/**
 * This represents the most basic box context.
 */
public interface IReferenceable {

	/**
	 * --------------------------------------------------------------------------
	 * Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Dereference this object by a key and return the value, or throw exception
	 *
	 * @return The requested object
	 */
	public Object __dereference( Key name ) throws KeyNotFoundException;

	/**
	 * Dereference this object by a key and invoke the result as an invokable (UDF, java method)
	 *
	 * @return The requested object
	 */
	public Object __dereferenceAndInvoke( Key name ) throws KeyNotFoundException;

	/**
	 * Safely derefernce this object by a key and return the value, or null if not found
	 *
	 * @return The requested object or null
	 */
	public Object __safeDereference( Key name );

	/**
	 * Get a scope from the context. If not found, the parent context is asked.
	 * Search all konwn scopes
	 *
	 */
	public void __assign( Key name, Object value );

}
