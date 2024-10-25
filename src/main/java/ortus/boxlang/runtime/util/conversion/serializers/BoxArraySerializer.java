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
package ortus.boxlang.runtime.util.conversion.serializers;

import java.io.IOException;
import java.util.IdentityHashMap;
import java.util.List;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.jr.ob.api.ValueWriter;
import com.fasterxml.jackson.jr.ob.impl.JSONWriter;

/**
 * This class provides JSON Serialization of a BoxLang Arrays
 */
public class BoxArraySerializer implements ValueWriter {

	// ThreadLocal to keep track of seen structs in the current thread
	private static final ThreadLocal<IdentityHashMap<List<?>, Boolean>> visitedArrays = ThreadLocal.withInitial( IdentityHashMap::new );

	/**
	 * Custom BoxLang Array Serializer
	 */
	@Override
	public void writeValue( JSONWriter context, JsonGenerator g, Object value ) throws IOException {
		// Get the list
		List<?>								bxArray	= ( List<?> ) value;

		// Get the current thread's set of seen structs
		IdentityHashMap<List<?>, Boolean>	visited	= visitedArrays.get();

		if ( visited.containsKey( bxArray ) ) {
			g.writeString( "recursive-array-skipping" );
		} else {
			visited.put( bxArray, Boolean.TRUE );

			// Write each array element
			g.writeStartArray();

			// iterate over the array elements
			for ( Object element : bxArray ) {
				context.writeValue( element );
			}

			g.writeEndArray();

			// Remove the struct from the set of seen structs
			visited.remove( bxArray );
		}
	}

	@Override
	public Class<?> valueType() {
		return List.class;
	}

}
