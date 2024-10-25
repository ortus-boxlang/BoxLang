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
import java.util.Map;
import java.util.Map.Entry;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.jr.ob.api.ValueWriter;
import com.fasterxml.jackson.jr.ob.impl.JSONWriter;

/**
 * This class provides JSON Serialization of a BoxLang Struct
 */
public class BoxStructSerializer implements ValueWriter {

	// ThreadLocal to keep track of seen structs in the current thread
	private static final ThreadLocal<IdentityHashMap<Map<?, ?>, Boolean>> visitedStructs = ThreadLocal.withInitial( IdentityHashMap::new );

	/**
	 * Custom BoxLang Struct Serializer
	 */
	@Override
	public void writeValue( JSONWriter context, JsonGenerator g, Object value ) throws IOException {
		Map<?, ?>							bxStruct	= ( Map<?, ?> ) value;

		// Get the current thread's set of seen structs
		IdentityHashMap<Map<?, ?>, Boolean>	visited		= visitedStructs.get();

		if ( visited.containsKey( bxStruct ) ) {
			// If the struct has already been seen, write "nested struct" instead
			g.writeString( "recursive-struct-skipping" );
		} else {
			// Add the struct to the set of seen structs
			visited.put( bxStruct, Boolean.TRUE );

			// Write the struct's properties
			g.writeStartObject();

			// iterate over the entry set
			for ( Entry<?, ?> entry : bxStruct.entrySet() ) {
				// Write the property name
				g.writeFieldName( entry.getKey().toString() );
				// Write the property value
				context.writeValue( entry.getValue() );
			}

			g.writeEndObject();

			// Remove the struct from the set of seen structs
			visited.remove( bxStruct );
		}
	}

	@Override
	public Class<?> valueType() {
		return Map.class;
	}

}
