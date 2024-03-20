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

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.events.BoxEvent;
import ortus.boxlang.runtime.types.Struct;

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

		// announce the scope creation
		BoxRuntime.getInstance().announce(
		    BoxEvent.ON_SCOPE_CREATION,
		    Struct.of(
		        "scope", this,
		        "name", scopeName
		    )
		);
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

}
