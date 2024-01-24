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

import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
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
		super( ArgumentsScope.name, Struct.TYPES.LINKED );
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

	public IStruct asStruct() {
		return ( IStruct ) this;
	}

	@Override
	public boolean containsKey( Key key ) {
		key = resolveKey( key );
		return super.containsKey( key );
	}

	public Object get( Key key ) {
		key = resolveKey( key );
		return super.get( key );
	}

	public Object getOrDefault( Key key, Object defaultValue ) {
		key = resolveKey( key );
		return super.getOrDefault( key, defaultValue );
	}

	public Object getRaw( Key key ) {
		key = resolveKey( key );
		return super.getRaw( key );
	}

	@Override
	public Object put( Key key, Object value ) {
		key = resolveKey( key );
		return super.put( key, value );
	}

	@Override
	public Object putIfAbsent( Key key, Object value ) {
		key = resolveKey( key );
		return super.putIfAbsent( key, value );
	}

	@Override
	public Object remove( Key key ) {
		key = resolveKey( key );
		return super.remove( key );
	}

	/**
	 * Resolve a key to the actual key in the scope
	 * Arguments allows existing items to be referenced by name OR position.
	 * argumetns[1] is the same as arguments.first
	 * So if we have an int key coming in, change it to the actual key in that position
	 *
	 * @param key
	 *
	 * @return
	 */
	private Key resolveKey( Key key ) {
		if ( key instanceof IntKey iKey ) {
			int index = iKey.getIntValue();
			if ( index > 0 && index <= size() ) {
				int i = 1;
				for ( Key k : wrapped.keySet() ) {
					if ( i++ == index ) {
						return k;
					}
				}
			}
		}
		return key;
	}

}
