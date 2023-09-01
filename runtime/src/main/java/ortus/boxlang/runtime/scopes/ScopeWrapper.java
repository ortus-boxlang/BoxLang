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

import ortus.boxlang.runtime.types.Struct;
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
	IScope wrapped;

	/**
	 * --------------------------------------------------------------------------
	 * Constructors
	 * --------------------------------------------------------------------------
	 */

	public ScopeWrapper( IScope wrapped ) {
		this( wrapped, null );
	}

	public ScopeWrapper( IScope wrapped, Map<Key, Object> override ) {
		super( Key.of( "wrapper" ) );
		this.wrapped = wrapped;
		if ( override != null ) {
			this.putAll( override );
		}
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

	/**
	 * Gets the name of the scope
	 *
	 * @return The name of the scope
	 */
	public Key getName() {
		return wrapped.getName();
	}

	@Override
	public boolean containsKey( Object name ) throws ClassCastException, NullPointerException {
		if ( super.containsKey( name ) ) {
			return true;
		}
		return wrapped.containsKey( name );
	}

	public boolean containsKey( Key name ) throws ClassCastException, NullPointerException {
		if ( super.containsKey( name ) ) {
			return true;
		}
		return wrapped.containsKey( name );
	}

	@Override
	public Object get( Object name ) {
		Object result = super.getRaw( ( Key ) name );
		if ( result != null ) {
			return Struct.unWrapNull( result );
		}
		return wrapped.get( name );
	}

	@Override
	public Object put( Key name, Object value ) {
		if ( super.containsKey( name ) ) {
			return super.put( name, value );
		}
		return wrapped.put( name, value );
	}

}
