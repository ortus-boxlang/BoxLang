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
import java.lang.reflect.Array;
import java.util.IdentityHashMap;
import java.util.List;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.jr.ob.api.ValueWriter;
import com.fasterxml.jackson.jr.ob.impl.JSONWriter;

/**
 * This class provides JSON Serialization of a Java Arrays
 */
public class JavaArraySerializer implements ValueWriter {

	// ThreadLocal to keep track of seen structs in the current thread
	private static final ThreadLocal<IdentityHashMap<Object, Boolean>> visitedArrays = ThreadLocal.withInitial( IdentityHashMap::new );

	@Override
	public void writeValue( JSONWriter context, JsonGenerator g, Object value ) throws IOException {
		// Get the current thread's set of seen arrays
		IdentityHashMap<Object, Boolean> visited = visitedArrays.get();

		if ( visited.containsKey( value ) ) {
			g.writeString( "recursive-array-skipping" );
		} else {
			visited.put( value, Boolean.TRUE );

			g.writeStartArray();
			int len = Array.getLength( value );
			for ( int i = 0; i < len; i++ ) {
				Object element = Array.get( value, i );
				context.writeValue( element );
			}
			g.writeEndArray();
			// Remove the array from the set of seen arrays
			visited.remove( value );
		}
	}

	@Override
	public Class<?> valueType() {
		// Return the superclass of Object[].class, which is Object.class, to match all array types
		return Object[].class.getSuperclass();
	}

}
