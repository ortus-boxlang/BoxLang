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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.jr.ob.api.ValueWriter;
import com.fasterxml.jackson.jr.ob.impl.JSONWriter;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.runnables.IClassRunnable;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Property;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.util.BLCollector;

/**
 * This class provides JSON Serialization of a BoxLang Class
 */
public class BoxClassSerializer implements ValueWriter {

	// ThreadLocal to keep track of seen structs in the current thread
	private static final ThreadLocal<Set<IClassRunnable>> visitedClasses = ThreadLocal.withInitial( HashSet::new );

	/**
	 * Inflate an annotation value into an Array
	 *
	 * @param value The value to inflate
	 *
	 * @return The inflated array
	 */
	private static Array inflateArray( Object value ) {
		// If the value is already an array, then cast it
		if ( value instanceof Array castedArray ) {
			return castedArray;
		}

		// Split the string by comma and trim the values
		return Arrays.stream( StringCaster.cast( value ).split( "," ) )
		    .map( String::trim )
		    .collect( BLCollector.toArray() );
	}

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

		// Get the current thread's set of visted classes
		Set<IClassRunnable>	visited				= visitedClasses.get();

		if ( visited.contains( bxClass ) ) {
			g.writeString( "recursive-class-skipping" );
			return;
		}

		// Verify if the class is NOT serializable via the "serializable" annotation and it's false, return {}
		if ( BooleanCaster.cast( classAnnotations.getOrDefault( Key.serializable, true ) ) == false ) {
			context.writeValue( new Struct() );
			return;
		}

		// Seed the class annotations needed
		Array classJsonExcludes = inflateArray( classAnnotations.getOrDefault( Key.jsonExclude, "" ) );

		// If there is a "toJson" method in the class, then call it
		// The user wants control over the serialization
		if ( variablesScope.containsKey( Key.toJSON ) ) {
			context.writeValue(
			    variablesScope.dereferenceAndInvoke( boxContext, Key.toJSON, new Object[] { context, g, value }, false )
			);
			return;
		}

		// logger.debug( "BoxClassSerializer.meta: {}", bxClass.getMetaData().toString() );
		// logger.debug( "BoxClassSerializer.variablesScope: {}", variablesScope.toString() );
		// logger.debug( "BoxClassSerializer.properties: {}", properties.toString() );

		// Filter the variables scope with the properties
		IStruct memento = variablesScope.entrySet().stream()
		    // Filter only the properties for the class
		    .filter( entry -> properties.containsKey( entry.getKey() ) )
		    // Filter out any properties that have the jsonExclude annotation
		    .filter( entry -> {
			    Property prop = properties.get( entry.getKey() );

			    // logger.debug( "BoxClassSerializer.writeValue: prop: {}", prop.name() );
			    // logger.debug( "prop has a jsonExclude {}", prop.annotations().containsKey( Key.jsonExclude ) );
			    // logger.debug( "prop is in the classJsonExcludes {}", classJsonExcludes.findIndex( prop.name(), false ) );

			    // Does the property name exist in the jsonExclude list?
			    return !prop.annotations().containsKey( Key.jsonExclude ) && classJsonExcludes.findIndex( prop.name(), false ) == 0;
		    } )
		    // Filter out any properties that have the serialiable = false annotation
		    .filter( entry -> {
			    Property prop = properties.get( entry.getKey() );
			    return BooleanCaster.cast( prop.annotations().getOrDefault( Key.serializable, true ) );
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
		        BLCollector.toStruct()
		    );

		// logger.debug( "BoxClassSerializer.writeValue: {}", memento.asString() );

		// Iterate and output each name using the entry set
		context.writeValue( memento );

		// Cleanup
		visited.remove( bxClass );
	}

	@Override
	public Class<?> valueType() {
		return IClassRunnable.class;
	}

}
