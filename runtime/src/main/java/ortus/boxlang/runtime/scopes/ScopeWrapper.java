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

import java.util.Map;

import ortus.boxlang.runtime.types.exceptions.KeyNotFoundException;

/**
 * I am decorator/wrapper for a scope that allows you to spoof variables in the wrapped scope.
 */
public class ScopeWrapper extends BaseScope {

	/**
	 * --------------------------------------------------------------------------
	 * Private Properties
	 * --------------------------------------------------------------------------
	 */
	IScope	wrapped;
	IScope	override;

	/**
	 * --------------------------------------------------------------------------
	 * Constructors
	 * --------------------------------------------------------------------------
	 */

	public ScopeWrapper( IScope wrapped, IScope override ) {
		super( Key.of( "wrapper" ) );
		this.wrapped	= wrapped;
		this.override	= override;
	}

	public ScopeWrapper( IScope wrapped ) {
		this( wrapped, new BaseScope( Key.of( "override" ) ) );
	}

	public ScopeWrapper( IScope wrapped, Map<Key, Object> override ) {
		this( wrapped );
		this.override.putAll( override );
	}

	/**
	 * --------------------------------------------------------------------------
	 * Methods
	 * --------------------------------------------------------------------------
	 */
	public IScope getWrapped() {
		return wrapped;
	}

	public void setWrapped( IScope wrapped ) {
		this.wrapped = wrapped;
	}

	public IScope getOverride() {
		return override;
	}

	public void setOverride( IScope override ) {
		this.override = override;
	}

	/**
	 * Gets the name of the scope
	 *
	 * @return The name of the scope
	 */
	public Key getName() {
		return wrapped.getName();
	}

	/**
	 * Dereference this object by a key and return the value, or throw exception
	 *
	 * @param name The key to look for
	 * @param safe Whether to throw an exception if the key is not found
	 *
	 * @return The requested obect
	 */
	public Object dereference( Key name, Boolean safe ) throws KeyNotFoundException {
		if ( override.containsKey( name ) ) {
			return override.dereference( name, safe );
		}
		return wrapped.dereference( name, safe );
	}

	/**
	 * Dereference this object by a key and invoke the result as an invokable (UDF, java method)
	 *
	 * @param name      The key to look for
	 * @param arguments The arguments to pass to the invokable
	 *
	 * @return The requested object
	 */
	@Override
	public Object dereferenceAndInvoke( Key name, Object[] arguments, Boolean safe ) throws KeyNotFoundException {
		if ( override.containsKey( name ) ) {
			return override.dereferenceAndInvoke( name, arguments, safe );
		}
		return wrapped.dereferenceAndInvoke( name, arguments, safe );
	}

	/**
	 * Derefence by assignment (x = y)
	 *
	 * @param name  The key to assign to
	 * @param value The value to assign
	 */
	@Override
	public void assign( Key name, Object value ) {
		if ( override.containsKey( name ) ) {
			override.assign( name, value );
		}
		wrapped.assign( name, value );
	}

}
