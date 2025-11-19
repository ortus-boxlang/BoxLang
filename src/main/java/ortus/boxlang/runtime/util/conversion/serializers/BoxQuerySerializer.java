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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.jr.ob.api.ValueWriter;
import com.fasterxml.jackson.jr.ob.impl.JSONWriter;

import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Query;
import ortus.boxlang.runtime.types.QueryColumn;
import ortus.boxlang.runtime.types.Struct;
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
			String queryFormat = currentQueryFormat.get();
			if ( queryFormat == null ) {
				queryFormat = "row";
			}

			// "row" is the same as "false". Top level struct with columns (array of strings), data (array of arrays)
			if ( queryFormat.equals( "row" ) || queryFormat.equals( "false" ) ) {
				value = Struct.linkedOf(
				    "columns", bxQuery.getColumns().keySet().stream().map( c -> c.getName() ).toArray( String[]::new ),
				    "data", bxQuery.getData()
				);
				context.writeValue( value );
				// "column" is the same as "true". Top level struct with rowcount, columns (array of strings), data (struct with column name as key and array of
				// values as value)
			} else if ( queryFormat.equals( "column" ) || queryFormat.equals( "true" ) ) {
				var						data	= new Struct( IStruct.TYPES.LINKED );
				Map<Key, QueryColumn>	cols	= bxQuery.getColumns();
				for ( var col : cols.keySet() ) {
					data.put( col, cols.get( col ).getColumnData() );
				}
				value = Struct.linkedOf(
				    "rowCount", bxQuery.size(),
				    "columns", bxQuery.getColumns().keySet().stream().map( c -> c.getName() ).toArray( String[]::new ),
				    "data", data
				);
				context.writeValue( value );
				// "struct" is what we get by default (array of structs)
			} else if ( queryFormat.equals( "struct" ) ) {
				context.writeValue( bxQuery.toArrayOfStructs() );
			} else {
				throw new BoxRuntimeException( "Invalid queryFormat: " + queryFormat );
			}

			// Remove the struct from the set of seen structs
			visited.remove( bxQuery );
		}
	}

	@Override
	public Class<?> valueType() {
		return Query.class;
	}

}
