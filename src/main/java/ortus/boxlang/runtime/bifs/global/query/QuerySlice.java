/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package ortus.boxlang.runtime.bifs.global.query;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.Query;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

@BoxBIF
@BoxMember( type = BoxLangType.QUERY )
public class QuerySlice extends BIF {

	/**
	 * Constructor
	 */
	public QuerySlice() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "query", Key.query ),
		    new Argument( true, "integer", Key.offset ),
		    new Argument( false, "integer", Key.length, 0 )
		};
	}

	/**
	 * Returns a subset of rows from an existing query
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.query The query object to which the rows should be returned.
	 * 
	 * @argument.offset The first row to include in the new query.
	 * 
	 * @argument.length The number of rows to include, defaults to all remaining rows.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Query	query	= arguments.getAsQuery( Key.query );
		int		offset	= arguments.getAsInteger( Key.offset ) - 1; // Adjust to zero-based index
		int		length	= arguments.getAsInteger( Key.length );

		// Validate negative offset and adjust it
		if ( offset < 0 ) {
			offset += query.size(); // Adjust to zero-based index
			if ( offset < 0 ) {
				throw new BoxRuntimeException( "Offset is outside the query row range." );
			}
		}

		// Ensure the offset is within the valid range
		if ( offset >= query.size() - 1 ) {
			throw new BoxRuntimeException( "Offset is outside the query row range." );
		}

		// Check if the offset + length exeeds the size of the query
		if ( offset + length - 1 >= query.size() ) {
			throw new BoxRuntimeException( "Length is outside the query row range." );
		}

		// Check if length is zero, in which case we return all remaining rows
		if ( length == 0 ) {
			length = query.size() - offset;
		}

		// Create a new query to hold the sliced data
		Query slicedQuery = new Query();
		query.getColumns().entrySet().stream().forEach( e -> slicedQuery.addColumn( e.getKey(), e.getValue().getType() ) );

		// Extract the subset of rows from the query
		for ( int i = offset; i < query.size() && ( length == 0 || i < offset + length ); i++ ) {
			slicedQuery.addRow( query.getRow( i ) );
		}

		return slicedQuery;
	}
}
