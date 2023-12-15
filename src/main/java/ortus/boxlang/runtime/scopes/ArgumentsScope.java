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

import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.Struct;

/**
 * Variables scope implementation in BoxLang
 */
public class ArgumentsScope extends BaseScope {

	/**
	 * --------------------------------------------------------------------------
	 * Public Properties
	 * --------------------------------------------------------------------------
	 */
	public static final Key name = Key.of( "arguments" );

	/**
	 * --------------------------------------------------------------------------
	 * Constructors
	 * --------------------------------------------------------------------------
	 */

	public ArgumentsScope() {
		super( ArgumentsScope.name, Struct.Type.LINKED );
	}

	/**
	 * --------------------------------------------------------------------------
	 * Methods
	 * --------------------------------------------------------------------------
	 */

	public Object[] asNativeArray() {
		return values().toArray();
	}

	public Array asArray() {
		return Array.of( asNativeArray() );
	}

	public Struct asStruct() {
		return ( Struct ) this;
	}
	// TODO: Make arguments behave like both a struct and an array

	/**
	 * Dereference this object by a key and return the value, or throw exception
	 *
	 * @param key  The key to dereference
	 * @param safe Whether to throw an exception if the key is not found
	 *
	 * @return The requested object
	 */
	@Override
	public Object dereference( Key key, Boolean safe ) {
		// If we have arguments[3], return the 3rd argument
		// TODO: this won't catch arguments["3"] but I don't want the overhead of casting unless maybe we try the string key first and THEN cast as the
		// fallback
		if ( key instanceof IntKey iKey ) {
			int index = iKey.getIntValue();
			if ( index > 0 && index <= size() ) {
				int i = 1;
				for ( Map.Entry<Key, Object> entry : wrapped.entrySet() ) {
					if ( i++ == index ) {
						return super.dereference( entry.getKey(), safe );
					}
				}
			}
		}
		return super.dereference( key, safe );
	}

	/**
	 * Assign a value to a key
	 *
	 * @param key   The key to assign
	 * @param value The value to assign
	 */
	@Override
	public Object assign( Key key, Object value ) {
		// If we have arguments[3]=foo and there are at least 3 args, then set the 3rd argument
		if ( key instanceof IntKey iKey ) {
			int index = iKey.getIntValue();
			if ( index > 0 && index <= size() ) {
				int i = 1;
				for ( Map.Entry<Key, Object> entry : wrapped.entrySet() ) {
					if ( i++ == index ) {
						return super.assign( entry.getKey(), value );
					}
				}
			}
		}

		return super.assign( key, value );
	}

}
