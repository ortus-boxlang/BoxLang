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

import java.util.HashSet;
import java.util.Set;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.types.Function;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.meta.BoxMeta;
import ortus.boxlang.runtime.types.meta.ScopeMeta;

/**
 * Base scope implementation. Extends HashMap for now. May want to switch to composition over inheritance, but this
 * is simpler for now and using the Key class provides our case insensitivity automatically.
 */
public class BaseScope extends Struct implements IScope {

	/**
	 * Each scope can have a human friendly name
	 */
	private final Key		scopeName;

	/**
	 * The unique lock name for this scope instance
	 */
	private final String	lockName;

	/**
	 * Set of final keys which cannot be reassigned
	 */
	private final Set<Key>	finalKeys	= new HashSet<>();

	/**
	 * Metadata object
	 */
	public BoxMeta			$bx;

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
	public BaseScope( Key scopeName ) {
		this( scopeName, Struct.TYPES.DEFAULT );
	}

	/**
	 * Constructor
	 *
	 * @param scopeName The name of the scope
	 * @param type      The Struct type of the scope
	 */
	public BaseScope( Key scopeName, Struct.TYPES type ) {
		// setup props
		super( type );
		this.scopeName	= scopeName;
		this.lockName	= scopeName.getName() + new Object().hashCode();
	}

	/**
	 * --------------------------------------------------------------------------
	 * Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Dereference this object by a key and return the value, or throw exception
	 *
	 * @param key  The key to dereference
	 * @param safe Whether to throw an exception if the key is not found
	 *
	 * @return The requested object
	 */
	@Override
	public Object dereference( IBoxContext context, Key key, Boolean safe ) {
		// Special check for $bx
		if ( key.equals( BoxMeta.key ) ) {
			return getBoxMeta();
		}

		return super.dereference( context, key, safe );
	}

	/**
	 * Get the BoxMetadata object for this struct
	 *
	 * @return The {@Link BoxMeta} object for this struct
	 */
	@Override
	public BoxMeta getBoxMeta() {
		if ( this.$bx == null ) {
			this.$bx = new ScopeMeta( this, this.finalKeys );
		}
		return this.$bx;
	}

	/**
	 * Gets the name of the scope
	 *
	 * @return The name of the scope
	 */
	public Key getName() {
		return scopeName;
	}

	/**
	 * Gets the name of the lock for use in the lock component. Must be unique per scope instance.
	 *
	 * @return The unique lock name for the scope
	 */
	public String getLockName() {
		return lockName;
	}

	/**
	 * Assign a value to a key in this scope, setting it as final
	 *
	 * @param context The context we're executing inside of
	 * @param name    The name of the scope to get
	 * @param value   The value to assign to the scope
	 *
	 * @return The value that was assigned
	 */
	public Object assignFinal( IBoxContext context, Key name, Object value ) {
		Object ret = assign( context, name, value );
		finalKeys.add( name );
		return ret;
	}

	/**
	 * Assign a value to a key in this scope
	 */
	@Override
	public Object put( Key key, Object value ) {
		if ( finalKeys.contains( key ) ) {
			if ( super.get( key ) instanceof Function ) {
				throw new BoxRuntimeException( "Cannot override final function [" + key.getName() + "] in scope [" + scopeName.getName() + "]" );
			}
			throw new BoxRuntimeException( "Cannot reassign final key [" + key.getName() + "] in scope [" + scopeName.getName() + "]" );
		}
		return super.put( key, value );
	}

	/**
	 * Assign a value to a key in this scope if it doesn't exist
	 */
	@Override
	public Object putIfAbsent( Key key, Object value ) {
		if ( finalKeys.contains( key ) ) {
			throw new BoxRuntimeException( "Cannot reassign final key [" + key.getName() + "] in scope [" + scopeName.getName() + "]" );
		}
		return super.putIfAbsent( key, value );
	}

	/**
	 * Remove a value from the struct by a Key object
	 *
	 * @param key The String key to remove
	 */
	@Override
	public Object remove( Key key ) {
		if ( finalKeys.contains( key ) ) {
			throw new BoxRuntimeException( "Cannot delete final key [" + key.getName() + "] in scope [" + scopeName.getName() + "]" );
		}
		return super.remove( key );
	}

}
