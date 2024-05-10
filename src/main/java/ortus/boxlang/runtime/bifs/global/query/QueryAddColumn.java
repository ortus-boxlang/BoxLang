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
 * Unless required by applicable law or agreed to in writing, software distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package ortus.boxlang.runtime.bifs.global.query;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.Query;
import ortus.boxlang.runtime.types.QueryColumnType;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

@BoxBIF
@BoxMember( type = BoxLangType.QUERY )
public class QueryAddColumn extends BIF {

	public QueryAddColumn() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "query", Key.query ),
		    new Argument( true, "string", Key.columnName ),
		    new Argument( false, "string", Key.columnType, "Varchar" ), // Default to Varchar
		    new Argument( true, "array", Key.array )
		};
	}

	/**
	 * Adds a column to a query and populates its rows with the contents of a one-dimensional array.
	 *
	 * @param context   The execution context.
	 * @param arguments Arguments including query, column name, data type, and the array to populate the column with.
	 *
	 * @argument.query The query object to which the column should be added.
	 * 
	 * @argument.columnName The name of the column to add.
	 * 
	 * @argument.columnType The column type of the new column.
	 * 
	 * @argument.arrayName The one-dimensional array used to populate the column.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Query	query		= arguments.getAsQuery( Key.query );
		String	columnName	= arguments.getAsString( Key.columnName );
		String	columnType	= arguments.getAsString( Key.columnType );
		Array	array		= arguments.getAsArray( Key.array );

		// Check if the column already exists
		if ( query.getColumns().containsKey( Key.of( columnName ) ) ) {
			throw new BoxRuntimeException( "Column '" + columnName + "' already exists in the query." );
		}

		QueryColumnType queryColumnType = QueryColumnType.fromString( columnType );

		// Add the new column to the query
		query.addColumn( Key.of( columnName ), queryColumnType, array.toArray() );

		// Return the index of the added column
		return query.getColumns().size();
	}
}
