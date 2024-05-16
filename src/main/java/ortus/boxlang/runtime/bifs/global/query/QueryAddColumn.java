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
		    new Argument( true, Argument.QUERY, Key.query ),
		    new Argument( true, Argument.STRING, Key.columnName ),
		    new Argument( false, Argument.ANY, Key.datatype, "Varchar" ),
		    new Argument( false, Argument.ARRAY, Key.array, new Array() )
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
	 * @argument.datatype The column data type of the new column or the array to populate the column with as a shortcut for "varchar".
	 *
	 * @argument.arrayName The one-dimensional array used to populate the column.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Query	query		= arguments.getAsQuery( Key.query );
		String	columnName	= arguments.getAsString( Key.columnName );
		String	columnType	= "";
		Array	array		= arguments.getAsArray( Key.array );

		// Check if the column already exists
		if ( query.getColumns().containsKey( Key.of( columnName ) ) ) {
			throw new BoxRuntimeException( "Column '" + columnName + "' already exists in the query." );
		}

		// Do we have an array for a column type as a shortcut for "varchar", or normal type?
		if ( arguments.get( Key.datatype ) instanceof Array castedArray ) {
			array		= castedArray;
			columnType	= "Varchar";
		} else {
			columnType = arguments.getAsString( Key.datatype );
		}

		// Get the column type native value
		QueryColumnType queryColumnType = QueryColumnType.fromString( columnType );

		// If the array is empty, then populate it with empty strings the size of the query
		if ( array.isEmpty() ) {
			for ( int i = 0; i < query.size(); i++ ) {
				array.add( "" );
			}
		}

		// Add the new column to the query
		query.addColumn( Key.of( columnName ), queryColumnType, array.toArray() );

		// Return the index of the added column
		return query.getColumns().size();
	}
}
