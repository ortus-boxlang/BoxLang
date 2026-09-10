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

import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Query;
import ortus.boxlang.runtime.types.QueryColumn;
import ortus.boxlang.runtime.types.QueryColumnType;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * This class provides JSON Serialization of a BoxLang Querys
 */
public class BoxQuerySerializer implements ValueWriter {

	// ThreadLocal to keep track of seen structs in the current thread
	private static final ThreadLocal<IdentityHashMap<Query, Boolean>>	visitedQuerys		= ThreadLocal.withInitial( IdentityHashMap::new );

	// ThreadLocal for query format
	public static final ThreadLocal<String>								currentQueryFormat	= new ThreadLocal<>();

	/**
	 * Custom BoxLang Query Serializer
	 */
	@Override
	public void writeValue( JSONWriter context, JsonGenerator g, Object value ) throws IOException {
		// Get the query
		Query							bxQuery	= ( Query ) value;

		// Get the current thread's set of seen structs
		IdentityHashMap<Query, Boolean>	visited	= visitedQuerys.get();

		if ( visited.containsKey( bxQuery ) ) {
			g.writeString( "recursive-Query-skipping" );
		} else {
			visited.put( bxQuery, Boolean.TRUE );

			try {
				String queryFormat = currentQueryFormat.get();
				if ( queryFormat == null ) {
					queryFormat = "row";
				}
				Map<Key, QueryColumn>	cols	= bxQuery.getColumns();
				// Read raw values to preserve nulls regardless of queryNullToEmpty.
				List<Object[]>			rows	= bxQuery.getData();

				// "row" is the same as "false". Top level struct with columns (array of strings), data (array of arrays)
				if ( queryFormat.equals( "row" ) || queryFormat.equals( "false" ) ) {
					g.writeStartObject();
					g.writeFieldName( "columns" );
					writeColumns( g, cols );
					g.writeFieldName( "data" );
					g.writeStartArray();
					for ( Object[] row : rows ) {
						g.writeStartArray();
						for ( QueryColumn column : cols.values() ) {
							writeValue( context, g, column, row[ column.getIndex() ] );
						}
						g.writeEndArray();
					}
					g.writeEndArray();
					g.writeEndObject();
					// "column" is the same as "true". Top level struct with rowcount, columns (array of strings), data (struct with column name as key and array of
					// values as value)
				} else if ( queryFormat.equals( "column" ) || queryFormat.equals( "true" ) ) {
					g.writeStartObject();
					g.writeNumberField( "rowCount", bxQuery.size() );
					g.writeFieldName( "columns" );
					writeColumns( g, cols );
					g.writeObjectFieldStart( "data" );
					for ( var col : cols.keySet() ) {
						QueryColumn column = cols.get( col );
						g.writeFieldName( col.toString() );
						g.writeStartArray();
						for ( Object[] row : rows ) {
							writeValue( context, g, column, row[ column.getIndex() ] );
						}
						g.writeEndArray();
					}
					g.writeEndObject();
					g.writeEndObject();
					// "struct" is what we get by default (array of structs)
				} else if ( queryFormat.equals( "struct" ) ) {
					g.writeStartArray();
					for ( Object[] row : rows ) {
						g.writeStartObject();
						for ( var entry : cols.entrySet() ) {
							g.writeFieldName( entry.getKey().toString() );
							QueryColumn column = entry.getValue();
							writeValue( context, g, column, row[ column.getIndex() ] );
						}
						g.writeEndObject();
					}
					g.writeEndArray();
				} else {
					throw new BoxRuntimeException( "Invalid queryFormat: " + queryFormat );
				}
			} finally {
				// Remove the query from the set of seen queries
				visited.remove( bxQuery );
			}
		}
	}

	private static void writeColumns( JsonGenerator g, Map<Key, QueryColumn> columns ) throws IOException {
		g.writeStartArray();
		for ( QueryColumn column : columns.values() ) {
			g.writeString( column.getName().toString() );
		}
		g.writeEndArray();
	}

	private static void writeValue( JSONWriter context, JsonGenerator g, QueryColumn column, Object value ) throws IOException {
		context.writeValue( column.getType() == QueryColumnType.BIT && value != null ? BooleanCaster.cast( value ) : value );
	}

	@Override
	public Class<?> valueType() {
		return Query.class;
	}

}
