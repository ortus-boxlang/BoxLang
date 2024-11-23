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
	public static final Key	name		= Key.arguments;
	private boolean			positional	= false;

	/**
	 * --------------------------------------------------------------------------
	 * Constructors
	 * --------------------------------------------------------------------------
	 */

	public ArgumentsScope() {
		super( ArgumentsScope.name, Struct.TYPES.LINKED );
	}

	/**
	 * Create a new arguments scope with the given attributes
	 *
	 * @param attributes The attributes to add to the scope
	 */
	public ArgumentsScope( IStruct attributes ) {
		super( ArgumentsScope.name, Struct.TYPES.LINKED );
		putAll( attributes );
	}

	/**
	 * --------------------------------------------------------------------------
	 * Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Convert the arguments scope to a native array
	 *
	 * @return The arguments as a native array
	 */
	public Object[] asNativeArray() {
		return values().toArray();
	}

	/**
	 * Convert the arguments scope to a BoxLang array
	 *
	 * @return The arguments as a BoxLang array
	 */
	public Array asArray() {
		return Array.of( asNativeArray() );
	}

	/**
	 * Convert the arguments scope to a BoxLang struct
	 *
	 * @return The arguments as a BoxLang struct
	 */
	public IStruct asStruct() {
		return this;
	}

	@Override
	public boolean containsKey( Key key ) {
		// Leave this unneccessary override as a reminder. All the other get/put methods translate between numeric and named keys, but contains key
		// will ONLY look for real live actual keys. This is largely for CF compat as `arguments[ 1 ]` works but `structKeyExists( arguments, 1 )` returns false.
		// It sort of makes sense if you think about it as the numeric keys only really existing when using the arguments as an array.
		// containsKey() is what powers structKeyExists(), and when using the arguments scope as a struct, it should only return true for actual keys and ignore the "spoofed"
		// positional numeric keys that allow it to behave as an array.
		return super.containsKey( key );
	}

	/**
	 * Helper method to create an arguments scope from a map
	 *
	 * @param key The key to use for the scope
	 *
	 * @return The arguments scope key
	 */
	public Object get( Key key ) {
		key = resolveKey( key );
		return super.get( key );
	}

	@Override
	public Object getOrDefault( Key key, Object defaultValue ) {
		key = resolveKey( key );
		return super.getOrDefault( key, defaultValue );
	}

	@Override
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
	 * arguments[1] is the same as arguments.first
	 * So if we have an int key coming in, change it to the actual key in that position
	 *
	 * @param key The key to resolve
	 *
	 * @return The resolved key
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

	/**
	 * --------------------------------------------------------------------------
	 * Getters and Setters
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Was this arguments scope created with an array of arguments or a struct of named arguments?
	 *
	 * @return True if positional, false if named
	 */
	public boolean isPositional() {
		return positional;
	}

	/**
	 * Set whether this arguments scope was created with an array of arguments or a struct of named arguments
	 *
	 * @param positional True if positional, false if named
	 *
	 * @return This arguments scope
	 */
	public ArgumentsScope setPositional( boolean positional ) {
		this.positional = positional;
		return this;
	}

}
