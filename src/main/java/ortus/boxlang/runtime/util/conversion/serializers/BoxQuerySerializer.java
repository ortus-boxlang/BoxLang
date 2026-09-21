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
import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.events.BoxEvent;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Query;
import ortus.boxlang.runtime.types.QueryColumn;
import ortus.boxlang.runtime.types.QueryColumnType;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * This class provides JSON Serialization of a BoxLang Querys
 */
public class BoxQuerySerializer implements ValueWriter {

	// ThreadLocal to keep track of seen structs in the current thread
	private static final ThreadLocal<IdentityHashMap<Query, Boolean>>	visitedQuerys		= ThreadLocal.withInitial( IdentityHashMap::new );
	private static final BoxRuntime										runtime				= BoxRuntime.getInstance();

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
				Map<Key, QueryColumn>	cols			= bxQuery.getColumns();
				// Read raw values to preserve nulls regardless of queryNullToEmpty.
				List<Object[]>			rows			= bxQuery.getData();
				Object					serializedQuery	= createInterceptedValue( queryFormat, cols, rows, bxQuery );
				if ( runtime.getInterceptorService().hasState( BoxEvent.ON_JSON_QUERY_SERIALIZE ) ) {
					runtime.announce( BoxEvent.ON_JSON_QUERY_SERIALIZE, () -> Struct.ofNonConcurrent( Key.data, serializedQuery ) );
				}
				context.writeValue( serializedQuery );
			} finally {
				// Remove the query from the set of seen queries
				visited.remove( bxQuery );
			}
		}
	}

	private static Object createInterceptedValue( String queryFormat, Map<Key, QueryColumn> columns, List<Object[]> rows, Query query ) {
		if ( queryFormat.equals( "row" ) || queryFormat.equals( "false" ) ) {
			List<Object[]> serializedRows = rows.stream()
			    .map( row -> serializeRow( columns, row ) )
			    .toList();
			return Struct.linkedOf(
			    "columns", columns.keySet().stream().map( Key::getName ).toArray( String[]::new ),
			    "data", serializedRows
			);
		}
		if ( queryFormat.equals( "column" ) || queryFormat.equals( "true" ) ) {
			var data = new Struct( IStruct.TYPES.LINKED );
			for ( var entry : columns.entrySet() ) {
				QueryColumn column = entry.getValue();
				data.put( entry.getKey(), rows.stream().map( row -> serializedValue( column, row[ column.getIndex() ] ) ).toArray() );
			}
			return Struct.linkedOf(
			    "rowCount", query.size(),
			    "columns", columns.keySet().stream().map( Key::getName ).toArray( String[]::new ),
			    "data", data
			);
		}
		if ( queryFormat.equals( "struct" ) ) {
			Array serializedData = new Array();
			for ( Object[] row : rows ) {
				IStruct serializedRow = new Struct( IStruct.TYPES.LINKED );
				columns.forEach( ( name, column ) -> serializedRow.put( name, serializedValue( column, row[ column.getIndex() ] ) ) );
				serializedData.add( serializedRow );
			}
			return serializedData;
		}
		throw new BoxRuntimeException( "Invalid queryFormat: " + queryFormat );
	}

	private static Object[] serializeRow( Map<Key, QueryColumn> columns, Object[] row ) {
		Object[] serializedRow = new Object[ row.length ];
		for ( QueryColumn column : columns.values() ) {
			serializedRow[ column.getIndex() ] = serializedValue( column, row[ column.getIndex() ] );
		}
		return serializedRow;
	}

	private static Object serializedValue( QueryColumn column, Object value ) {
		return column.getType() == QueryColumnType.BIT && value != null ? BooleanCaster.cast( value ) : value;
	}

	@Override
	public Class<?> valueType() {
		return Query.class;
	}

}
