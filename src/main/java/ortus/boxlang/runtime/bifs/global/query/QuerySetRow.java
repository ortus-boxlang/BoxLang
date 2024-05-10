/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package ortus.boxlang.runtime.bifs.global.query;

import java.util.List;
import java.util.stream.Collectors;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.ArrayCaster;
import ortus.boxlang.runtime.dynamic.casters.StructCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Query;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

@BoxBIF
@BoxMember( type = BoxLangType.QUERY )
public class QuerySetRow extends BIF {

	public QuerySetRow() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "query", Key.query ),
		    new Argument( false, "integer", Key.rowNumber, 0 ),  // Optional argument
		    new Argument( true, "any", Key.rowData )
		};
	}

	/**
	 * Adds or updates a row in a query based on the provided row data and position.
	 *
	 * @param context   The execution context.
	 * @param arguments Arguments including query, row number, and row data.
	 * 
	 * @argument.query The query object to which the row should be added or updated.
	 * 
	 * @argument.rowNumber Optional position of the row to update; if omitted or zero, a new row will be added.
	 * 
	 * @argument.rowData A struct or array containing data for the row.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Query	query		= arguments.getAsQuery( Key.query );
		int		rowNumber	= arguments.getAsInteger( Key.rowNumber ) - 1;
		Object	rowData		= arguments.get( Key.rowData );

		if ( rowNumber < 0 ) {
			query.addRows( 1 );
			rowNumber = query.size() - 1;
		}

		List<Key>	columns		= query.getColumns().keySet().stream().collect( Collectors.toList() );
		Object[]	rowValues	= new Object[ columns.size() ];
		fillRowValues( query, rowData, columns, rowValues );

		updateQueryData( query, rowNumber, rowValues );
		return true;
	}

	private void fillRowValues( Query query, Object rowData, List<Key> columns, Object[] rowValues ) {
		if ( rowData instanceof IStruct ) {
			IStruct rowDataStruct = StructCaster.cast( rowData );
			columns.forEach( key -> rowValues[ columns.indexOf( key ) ] = rowDataStruct.getOrDefault( key, query.getCell( key, columns.indexOf( key ) ) ) );
		} else if ( rowData instanceof Array ) {
			Array rowDataArray = ArrayCaster.cast( rowData );
			for ( int i = 0; i < columns.size(); i++ ) {
				rowValues[ i ] = i < rowDataArray.size() ? rowDataArray.get( i ) : null;
			}
		} else {
			throw new BoxRuntimeException( "Invalid row data type: " + rowData.getClass().getSimpleName() );
		}
	}

	private void updateQueryData( Query query, int rowNumber, Object[] rowValues ) {
		if ( query.getRow( rowNumber ) == null ) {
			query.getData().add( rowValues );
		} else {
			query.getData().set( rowNumber, rowValues );
		}
	}
}