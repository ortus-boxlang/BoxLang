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
package ortus.boxlang.runtime.types.util;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Property;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

public class MapHelper {

	/**
	 * Create a LinkedHashMap from a list of values. The values must be in pairs, key, value, key, value, etc.
	 *
	 * @param values The values to create the struct from
	 *
	 * @return The struct
	 */
	public static Map<Key, Object> LinkedHashMapOfAny( Object... values ) {
		if ( values.length % 2 != 0 ) {
			throw new BoxRuntimeException( "Invalid number of arguments.  Must be an even number." );
		}
		var map = new LinkedHashMap<Key, Object>();
		for ( int i = 0; i < values.length; i += 2 ) {
			map.put( ( Key ) values[ i ], ( Object ) values[ i + 1 ] );
		}
		return map;
	}

	/**
	 * Create a LinkedHashMap from a list of values. The values must be in pairs, key, value, key, value, etc.
	 *
	 * @param values The values to create the struct from
	 *
	 * @return The struct
	 */
	public static Map<Key, Property> LinkedHashMapOfProperties( Object... values ) {
		if ( values.length % 2 != 0 ) {
			throw new BoxRuntimeException( "Invalid number of arguments.  Must be an even number." );
		}
		var map = new LinkedHashMap<Key, Property>();
		for ( int i = 0; i < values.length; i += 2 ) {
			map.put( ( Key ) values[ i ], ( Property ) values[ i + 1 ] );
		}
		return map;
	}

	/**
	 * Create a HashMap from a list of values. The values must be in pairs, key, value, key, value, etc.
	 *
	 * @param values The values to create the struct from
	 *
	 * @return The struct
	 */
	public static Map<Key, Property> HashMapOfProperties( Object... values ) {
		if ( values.length % 2 != 0 ) {
			throw new BoxRuntimeException( "Invalid number of arguments.  Must be an even number." );
		}
		var map = new HashMap<Key, Property>();
		for ( int i = 0; i < values.length; i += 2 ) {
			map.put( ( Key ) values[ i ], ( Property ) values[ i + 1 ] );
		}
		return map;
	}

}
