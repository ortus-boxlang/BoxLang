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

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.ArrayCaster;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.dynamic.casters.StructCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.Query;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.util.BLCollector;
import ortus.boxlang.runtime.types.util.ListUtil;

@BoxBIF( description = "Create a new query" )
public class QueryNew extends BIF {

	/**
	 * Constructor
	 */
	public QueryNew() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "any", Key.columnList ),
		    new Argument( false, "string", Key.columnTypeList, "" ),
		    new Argument( false, "any", Key.rowData )
		};
	}

	/**
	 * Creates a new query from a column list, optional column types and optional row data.
	 * <p>
	 * Available column types are:
	 * <ul>
	 * <li>bigint</li>
	 * <li>binary</li>
	 * <li>bit</li>
	 * <li>date</li>
	 * <li>decimal</li>
	 * <li>double</li>
	 * <li>integer</li>
	 * <li>null</li>
	 * <li>object</li>
	 * <li>other</li>
	 * <li>time</li>
	 * <li>timestamp</li>
	 * <li>varchar</li>
	 * </ul>
	 * If <code>columnTypeList</code> is empty, all columns will be of type "object". If provided, it must have the same number of
	 * entries as the column list, otherwise an exception is thrown.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.columnList The column names as a comma-delimited string (for example "name, age, dob"), or an array of column names.
	 *                      If an array of structs is passed with no other arguments, it is used as the row data and the column names are taken from
	 *                      the keys of the first struct.
	 *
	 * @argument.columnTypeList A comma-delimited list of column data types, one per column (for example "varchar, integer, date").
	 *                          If empty, all columns are of type "object".
	 *
	 * @argument.rowData Data to populate the query. Can be a struct (with keys matching column names, producing one row), an array
	 *                   of structs, or an array of arrays (in the same order as the column list).
	 *
	 * @return The new query.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Object	rowData			= arguments.get( Key.rowData );
		Object	columnList		= arguments.get( Key.columnList );
		String	columnTypeList	= arguments.getAsString( Key.columnTypeList );
		Array	columnNames;

		// Build out Column Names
		if ( columnList instanceof String cl ) {
			columnNames = ArrayCaster.cast(
			    ListUtil.asList( cl, "," )
			        .stream()
			        .map( String::valueOf )
			        .map( String::trim )
			        .collect( BLCollector.toArray() )
			);
		}
		// If it's an array, then it's data
		else if ( columnList instanceof Array castedRowData && columnTypeList.isEmpty() && rowData == null ) {
			rowData		= castedRowData;
			columnNames	= new Array();
			if ( !castedRowData.isEmpty() ) {
				if ( StructCaster.attempt( castedRowData.get( 0 ) ).wasSuccessful() ) {
					// If the first element is a struct, then use the keys as column names
					columnNames = Array.fromList( StructCaster.cast( castedRowData.get( 0 ) ).getKeysAsStrings() );
				} else {
					castedRowData.stream().map( StringCaster::cast ).forEach( columnNames::add );
				}
			}
		} else if ( columnList instanceof Array castedRowData ) {
			columnNames = castedRowData;
		} else {
			throw new BoxRuntimeException( "The [columnList] must be a string, or an array of data, or" );
		}

		// Verify Column Types
		Array columnTypes = ListUtil
		    .asList( columnTypeList, "," )
		    .stream()
		    .map( String::valueOf )
		    .map( String::trim )
		    .collect( BLCollector.toArray() );
		if ( columnTypes.isEmpty() ) {
			// add "object" as default type
			for ( int i = 0; i < columnNames.size(); i++ ) {
				columnTypes.add( "object" );
			}
		} else if ( columnNames.size() != columnTypes.size() ) {
			throw new BoxRuntimeException( "columnList and columnTypeList must have the same number of elements" );
		}

		return Query.fromArray( columnNames, columnTypes, rowData, context );
	}

}
