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
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.jr.ob.api.ValueWriter;
import com.fasterxml.jackson.jr.ob.impl.JSONWriter;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.runnables.IClassRunnable;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Property;
import ortus.boxlang.runtime.types.Struct;

/**
 * This class provides JSON Serialization of a BoxLang Class
 */
public class BoxClassSerializer implements ValueWriter {

	/**
	 * Logger
	 */
	private static final Logger logger = LoggerFactory.getLogger( BoxClassSerializer.class );

	/**
	 * Custom BoxLang Class Serializer
	 */
	@Override
	public void writeValue( JSONWriter context, JsonGenerator g, Object value ) throws IOException {
		IClassRunnable		bxClass				= ( IClassRunnable ) value;
		Map<Key, Property>	properties			= bxClass.getProperties();
		IStruct				classAnnotations	= bxClass.getAnnotations();
		VariablesScope		variablesScope		= bxClass.getVariablesScope();
		IBoxContext			boxContext			= BoxRuntime.getInstance().getRuntimeContext();

		// Seed the class annotations needed
		Array				classExclude		= new Array(
		    classAnnotations.getOrDefault( Key.jsonExclude, "" ).toString().split( "," )
		);

		// If there is a "toJson" method in the class, then call it
		// The user wants control over the serialization
		if ( variablesScope.containsKey( Key.toJSON ) ) {
			context.writeValue(
			    variablesScope.dereferenceAndInvoke( boxContext, Key.toJSON, new Object[] { context, g, value }, false )
			);
			return;
		}

		// Filter the variables scope with the properties
		IStruct memento = variablesScope.entrySet().stream()
		    // Filter only the properties for the class
		    .filter( entry -> properties.containsKey( entry.getKey() ) )
		    // Filter out any properties that have the jsonExclude annotation
		    .filter( entry -> {
			    Property prop = properties.get( entry.getKey() );
			    // Does the property name exist in the jsonExclude list?
			    return !prop.annotations().containsKey( Key.jsonExclude ) && classExclude.findIndex( prop.name(), false ) == -1;
		    } )
		    // If the property is null, then set it to an empty string
		    .map( entry -> {
			    if ( entry.getValue() == null ) {
				    entry.setValue( "" );
			    }
			    return entry;
		    } )
		    // Collect to a struct object
		    .collect(
		        Collectors.toMap(
		            // key
		            Entry::getKey,
		            // value
		            Entry::getValue,
		            // merge function
		            ( existing, replacement ) -> existing,
		            // map type
		            Struct::new
		        )
		    );

		// logger.debug( "BoxClassSerializer.writeValue: {}", memento.asString() );

		// Iterate and output each name using the entry set
		context.writeValue( memento );
	}

	@Override
	public Class<?> valueType() {
		return IClassRunnable.class;
	}

}
