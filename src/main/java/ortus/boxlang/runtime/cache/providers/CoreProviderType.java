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
package ortus.boxlang.runtime.cache.providers;

import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

public enum CoreProviderType {

	BOXCACHE( Key.boxCacheProvider );

	/**
	 * This is used to store the key
	 */
	private final Key key;

	/**
	 * Constructor
	 *
	 * @param name The key name of the core provider
	 */
	CoreProviderType( Key name ) {
		this.key = name;
	}

	/**
	 * Returns the key of the enum
	 */
	public Key getKey() {
		return this.key;
	}

	/**
	 * Get the value of the enum by the key
	 *
	 * @param key The key of the enum
	 *
	 * @return The value of the enum
	 */
	public static CoreProviderType getValueByKey( Key key ) {
		for ( CoreProviderType value : CoreProviderType.values() ) {
			if ( value.getKey().equals( key ) ) {
				return value;
			}
		}
		throw new BoxRuntimeException( "No Core Provider " + CoreProviderType.class.getCanonicalName() + " with key " + key );
	}

	/**
	 * Build out the provider according to the Enum type
	 */
	public ICacheProvider buildProvider() {
		switch ( this ) {
			case BOXCACHE :
				return new BoxCacheProvider();
			default :
				throw new BoxRuntimeException( "No Core Provider " + CoreProviderType.class.getCanonicalName() );
		}
	}

	/**
	 * Method to validate if an incoming value is a Core Provider
	 */
	public static boolean isCore( Key key ) {
		for ( CoreProviderType value : CoreProviderType.values() ) {
			if ( value.getKey().equals( key ) ) {
				return true;
			}
		}
		return false;
	}

}
