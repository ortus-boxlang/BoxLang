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
package ortus.boxlang.runtime.cache.store;

import ortus.boxlang.runtime.cache.providers.CoreProviderType;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * The enum of available object storages
 * Each with a name and corresponding Key
 */
public enum ObjectStoreType {

	BLACKHOLE( Key.of( "BlackHoleStore" ) ),
	CONCURRENT( Key.of( "ConcurrentStore" ) ),
	CONCURRENT_SOFT_REFERENCE( Key.of( "ConcurrentSoftReferenceStore" ) ),
	DISK( Key.of( "DiskStore" ) ),
	JDBC( Key.of( "JDBCStore" ) );

	/**
	 * This class is used to store the key of the enum.
	 */
	private final Key key;

	/**
	 * Constructor
	 *
	 * @param name The key name of the object store
	 */
	ObjectStoreType( Key name ) {
		this.key = name;
	}

	/**
	 * Returns the key of the enum.
	 *
	 * @return The key of the enum
	 */
	public Key getKey() {
		return this.key;
	}

	/**
	 * Get the value of the enum by the key
	 *
	 * @param key The key to get the value of
	 *
	 * @return The value of the enum
	 */
	public static ObjectStoreType getValueByKey( Key key ) {
		for ( ObjectStoreType store : ObjectStoreType.values() ) {
			if ( store.getKey().equals( key ) ) {
				return store;
			}
		}
		throw new BoxRuntimeException( "No Core Object Store" + ObjectStoreType.class.getCanonicalName() + " with key " + key );
	}

	/**
	 * Build the store according to the Enum type
	 *
	 * @return The built store
	 */
	public IObjectStore buildStore() {
		// Switch on the enum value
		switch ( this ) {
			case BLACKHOLE :
				return new BlackHoleStore();
			case CONCURRENT :
				return new ConcurrentStore();
			case CONCURRENT_SOFT_REFERENCE :
				return new ConcurrentSoftReferenceStore();
			default :
				throw new BoxRuntimeException( "No Object Store " + CoreProviderType.class.getCanonicalName() );
		}
	}

	/**
	 * Method to validate if an incoming value is a core ObjectStore
	 *
	 * @param value The key to validate
	 *
	 * @return True if the key is a core ObjectStore
	 */
	public static boolean isCore( Key value ) {
		for ( ObjectStoreType store : ObjectStoreType.values() ) {
			if ( store.getKey().equals( value ) ) {
				return true;
			}
		}
		return false;
	}
}
