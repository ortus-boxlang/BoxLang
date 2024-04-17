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
package ortus.boxlang.runtime.jdbc.drivers;

import java.util.Arrays;

import ortus.boxlang.runtime.scopes.Key;

/**
 * This driver type enum is used to determine the type of driver to use for a datasource
 *
 * The available driver types are:
 * <ul>
 * <li>MYSQL</li>
 * <li>MARIADB</li>
 * <li>POSTGRES</li>
 * <li>SQLSERVER</li>
 * <li>ORACLE</li>
 * <li>DB2</li>
 * <li>DERBY</li>
 * <li>SQLITE</li>
 * <li>HYPERSONIC</li>
 * <li>GENERIC</li>
 * </ul>
 *
 * Each type is represented by a {@link Key} object
 */
public enum DatabaseDriverType {

	DB2( Key.of( "db2" ) ),
	DERBY( Key.of( "derby" ) ),
	GENERIC( Key.of( "generic" ) ),
	HYPERSONIC( Key.of( "hypersonic" ) ),
	MARIADB( Key.of( "mariadb" ) ),
	MYSQL( Key.of( "mysql" ) ),
	ORACLE( Key.of( "oracle" ) ),
	OTHER( Key.of( "other" ) ),
	POSTGRES( Key.of( "postgres" ) ),
	SQLITE( Key.of( "sqlite" ) ),
	SQLSERVER( Key.of( "sqlserver" ) );

	/**
	 * The key for the driver type
	 */
	private Key key;

	/**
	 * Create a new driver type
	 *
	 * @param key The key for the driver type
	 */
	private DatabaseDriverType( Key key ) {
		this.key = key;
	}

	/**
	 * Get the key for the driver type
	 *
	 * @return The key for the driver type
	 */
	public Key getKey() {
		return key;
	}

	/**
	 * Get the driver type for the given key
	 *
	 * @param key The key to get the driver type for
	 *
	 * @return The driver type for the given key
	 */
	public static DatabaseDriverType fromKey( Key key ) {
		for ( DatabaseDriverType type : values() ) {
			if ( type.key.equals( key ) ) {
				return type;
			}
		}
		return GENERIC;
	}

	/**
	 * Get the driver type for the given key string
	 *
	 * @param key A string representation of the driver type
	 *
	 * @return The driver type for the given key string
	 */
	public static DatabaseDriverType fromKey( String key ) {
		return fromKey( Key.of( key ) );
	}

	/**
	 * Is this a valid driver type
	 *
	 * @param key The key to validate
	 *
	 * @return True if the key is a valid driver type, false otherwise
	 */
	public static boolean isValid( Key key ) {
		for ( DatabaseDriverType type : values() ) {
			if ( type.key.equals( key ) ) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Is this a valid driver type
	 *
	 * @param key The key to validate
	 *
	 * @return True if the key is a valid driver type, false otherwise
	 */
	public static boolean isValid( String key ) {
		return isValid( Key.of( key ) );
	}

	/**
	 * Returns an array of all the driver types
	 */
	public static Key[] toArray() {
		return Arrays.stream( values() )
		    .map( val -> val.key )
		    .sorted()
		    .toArray( Key[]::new );
	}

	/**
	 * Returns the DatabaseDriverType for the given key as a string.
	 */
	@Override
	public String toString() {
		return this.key.getName();
	}

}
