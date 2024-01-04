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

import java.util.Map;

import ortus.boxlang.runtime.context.IBoxContext;
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
	 * @param context The context we're executing inside of
	 * @param name    The key to dereference
	 * @param safe    Whether to throw an exception if the key is not found
	 *
	 * @return The requested object
	 */
	public Object dereference( IBoxContext context, Key name, Boolean safe );

	/**
	 * Dereference this object by a key and invoke the result as an invokable (UDF, java method) using positional arguments
	 *
	 * @param context             The context we're executing inside of
	 * @param name                The key to dereference
	 * @param positionalArguments The positional arguments to pass to the invokable
	 * @param safe                Whether to throw an exception if the key is not found
	 *
	 * @return The requested object
	 */
	public Object dereferenceAndInvoke( IBoxContext context, Key name, Object[] positionalArguments, Boolean safe );

	/**
	 * Dereference this object by a key and invoke the result as an invokable (UDF, java method) using named arguments
	 *
	 * @param context        The context we're executing inside of
	 * @param name           The key to dereference
	 * @param namedArguments The named arguments to pass to the invokable
	 * @param safe           Whether to throw an exception if the key is not found
	 *
	 * @return The requested object
	 */
	public Object dereferenceAndInvoke( IBoxContext context, Key name, Map<Key, Object> namedArguments, Boolean safe );

	/**
	 * Assign a value to a key in this object
	 *
	 * @param context The context we're executing inside of
	 * @param name    The name of the scope to get
	 * @param value   The value to assign to the scope
	 *
	 * @return The value that was assigned
	 */
	public Object assign( IBoxContext context, Key name, Object value );

}
