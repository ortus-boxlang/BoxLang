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
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.jr.ob.api.ValueWriter;
import com.fasterxml.jackson.jr.ob.impl.JSONWriter;

import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.runnables.IClassRunnable;

/**
 * This class provides JSON Serialization of a BoxLang Dynamic Object
 */
public class DynamicObjectSerializer implements ValueWriter {

	@Override
	public void writeValue( JSONWriter context, JsonGenerator g, Object value ) throws IOException {
		DynamicObject dynamicObject = ( DynamicObject ) value;

		// If the object is a BoxClass, then serialize it as a BoxClass
		if ( dynamicObject.unWrap() instanceof IClassRunnable bxClass ) {
			context.writeValue( bxClass );
			return;
		}

		// If it's a list, then serialize it as a list
		if ( dynamicObject.unWrap() instanceof List<?> castedList ) {
			context.writeValue( castedList );
			return;
		}

		// If it's a map, then serialize it as a map
		if ( dynamicObject.unWrap() instanceof Map<?, ?> castedMap ) {
			context.writeValue( castedMap );
			return;
		}

		// Get all the public fields for this object
		g.writeStartObject();
		dynamicObject.getFieldsAsStream()
		    // Fiter ONLY public fields
		    .filter( field -> Modifier.isPublic( field.getModifiers() ) )
		    // Write it to the JSON
		    .forEach( field -> {
			    try {
				    g.writeObjectField( field.getName(), dynamicObject.getField( field.getName() ).orElse( "" ).toString() );
			    } catch ( IOException e ) {
				    e.printStackTrace();
			    }
		    } );
		g.writeEndObject();
	}

	@Override
	public Class<?> valueType() {
		return DynamicObject.class;
	}

}
