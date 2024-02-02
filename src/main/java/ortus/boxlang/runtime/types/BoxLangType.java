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
package ortus.boxlang.runtime.types;

import ortus.boxlang.runtime.scopes.Key;

/**
 * This enum represents the types that are supported by BoxLang.
 */
public enum BoxLangType {

	ANY( Key._ANY ),
	ARRAY( Key._ARRAY ),
	BOOLEAN( Key._BOOLEAN ),
	DATE( Key._DATE ),
	DATETIME( Key._DATETIME ),
	FILE( Key._FILE ),
	LIST( Key._LIST ),
	NUMERIC( Key._NUMERIC ),
	QUERY( Key._QUERY ),
	STRING( Key._STRING ),
	STRUCT( Key._STRUCT );

	/**
	 * This class is used to store the key of the enum.
	 */
	private final Key key;

	/**
	 * Constructor
	 *
	 * @param value The Key value of the enum
	 */
	BoxLangType( Key value ) {
		this.key = value;
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
	 * Validate if the incoming value is a valid BoxLangType.
	 *
	 * @param value The key value to validate
	 *
	 * @return True if the value is a valid BoxLangType, false otherwise
	 */
	public static boolean isValid( Key value ) {
		for ( BoxLangType type : BoxLangType.values() ) {
			if ( type.getKey().equals( value ) ) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Validate if the incoming value is a valid BoxLangType.
	 *
	 * @param value The key value to validate
	 *
	 * @return True if the value is a valid BoxLangType, false otherwise
	 */
	public static boolean isValid( String value ) {
		return isValid( Key.of( value ) );
	}

}
