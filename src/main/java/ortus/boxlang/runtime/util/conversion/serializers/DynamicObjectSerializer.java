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
import java.util.Map;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.jr.ob.api.ValueWriter;
import com.fasterxml.jackson.jr.ob.impl.JSONWriter;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.runnables.IClassRunnable;
import ortus.boxlang.runtime.types.util.StructUtil;

/**
 * This class provides JSON Serialization of a BoxLang Dynamic Object or any other object
 */
public class DynamicObjectSerializer implements ValueWriter {

	// ThreadLocal to keep track of seen objects in the current thread
	private static final ThreadLocal<IdentityHashMap<Object, Boolean>> visitedObjects = ThreadLocal.withInitial( IdentityHashMap::new );

	@Override
	public void writeValue( JSONWriter context, JsonGenerator g, Object value ) throws IOException {
		DynamicObject	dynamicObject;
		// Unwrap the DO.
		Object			realValue	= DynamicObject.unWrap( value );
		// If we didn't get a DO, make one.
		if ( value instanceof DynamicObject dob ) {
			dynamicObject = dob;
		} else {
			dynamicObject = DynamicObject.of( value );
		}

		// Get the current thread's set of visited objects
		IdentityHashMap<Object, Boolean> visited = visitedObjects.get();

		if ( visited.containsKey( realValue ) ) {
			g.writeString( "recursive-object-skipping" );
			return;
		}

		// Add the object to the set of visited objects
		visited.put( realValue, Boolean.TRUE );

		try {
			// If the object is a BoxClass, then serialize it as a BoxClass
			if ( realValue instanceof IClassRunnable bxClass ) {
				context.writeValue( bxClass );
				return;
			}

			// If it's a list, then serialize it as a list
			if ( realValue instanceof List<?> castedList ) {
				context.writeValue( castedList );
				return;
			}

			// If it's a map, then serialize it as a map
			if ( realValue instanceof Map<?, ?> castedMap ) {
				context.writeValue( castedMap );
				return;
			}

			// Serialize as generic struct
			context.writeValue( StructUtil.objectToStruct( realValue, BoxRuntime.getInstance().getRuntimeContext(), false ) );
		} finally {
			// Remove the object from the set of visited objects
			visited.remove( realValue );
		}
	}

	@Override
	public Class<?> valueType() {
		return DynamicObject.class;
	}

}
