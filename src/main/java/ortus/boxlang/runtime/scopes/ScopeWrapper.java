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

import java.util.List;
import java.util.Map;

import ortus.boxlang.runtime.types.Struct;

/**
 * I am decorator/wrapper for a scope that allows you to spoof variables in the wrapped scope.
 */
public class ScopeWrapper extends BaseScope {

	/**
	 * --------------------------------------------------------------------------
	 * Private Properties
	 * --------------------------------------------------------------------------
	 */
	IScope wrappedScope;

	/**
	 * --------------------------------------------------------------------------
	 * Constructors
	 * --------------------------------------------------------------------------
	 */

	public ScopeWrapper( IScope wrappedScope ) {
		this( wrappedScope, null );
	}

	public ScopeWrapper( IScope wrappedScope, Map<Key, Object> override ) {
		super( Key.of( "wrapper" ) );
		this.wrappedScope = wrappedScope;
		if ( override != null ) {
			super.putAll( override );
		}
	}

	/**
	 * --------------------------------------------------------------------------
	 * Methods
	 * --------------------------------------------------------------------------
	 */
	public IScope getWrapped() {
		return wrappedScope;
	}

	public void setWrapped( IScope wrappedScope ) {
		this.wrappedScope = wrappedScope;
	}

	/**
	 * Gets the name of the scope
	 *
	 * @return The name of the scope
	 */
	public Key getName() {
		return wrappedScope.getName();
	}

	@Override
	public boolean containsKey( Object name ) throws NullPointerException {
		if ( super.containsKey( name ) ) {
			return true;
		}
		return wrappedScope.containsKey( name );
	}

	public boolean containsKey( Key name ) throws NullPointerException {
		if ( super.containsKey( name ) ) {
			return true;
		}
		return wrappedScope.containsKey( name );
	}

	@Override
	public Object get( Object name ) {
		Object result = super.getRaw( ( Key ) name );
		if ( result != null ) {
			return Struct.unWrapNull( result );
		}
		return wrappedScope.get( name );
	}

	@Override
	public Object put( Key name, Object value ) {
		if ( super.containsKey( name ) ) {
			return super.put( name, value );
		}
		return wrappedScope.put( name, value );
	}

	@Override
	public List<String> getKeysAsStrings() {
		List<String> result = keySet().stream().map( Key::getName ).collect( java.util.stream.Collectors.toList() );
		result.addAll( wrappedScope.keySet().stream().map( Key::getName ).collect( java.util.stream.Collectors.toList() ) );
		return result;
	}

	@Override
	public List<Key> getKeys() {
		List<Key> result = keySet().stream().collect( java.util.stream.Collectors.toList() );
		result.addAll( wrappedScope.keySet().stream().collect( java.util.stream.Collectors.toList() ) );
		return result;
	}

}
