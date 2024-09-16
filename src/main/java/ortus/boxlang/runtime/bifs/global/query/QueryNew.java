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
import ortus.boxlang.runtime.dynamic.casters.StructCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.Query;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.util.ListUtil;

@BoxBIF
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
	 * Return new query based on the provided column list, column types, and/or row data.
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
	 * <p>
	 * If <code>columnTypeList</code> is empty, all columns will be of type "object".
	 *
	 * @See {@link QueryColumnType}
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.columnList The column list to be used in the query, Ex: "name, age, dob". It can also be an array of structs that will be used as the row data.
	 *
	 * @argument.columnTypeList Comma-delimited list specifying column data types. If empty, all columns will be of type "object". Ex: "varchar, integer, date"
	 *
	 * @argument.rowData Data to populate the query. Can be a struct (with keys matching column names), an array of structs, or an array of arrays (in
	 *                   same order as columnList). Ex: [{name: "John", age: 30}, {name: "Jane", age: 25}]
	 *
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Object	rowData		= arguments.get( Key.rowData );
		Object	columnList	= arguments.get( Key.columnList );
		Array	columnNames;

		// Build out Column Names
		if ( columnList instanceof String cl ) {
			columnNames = ArrayCaster.cast(
			    ListUtil.asList( cl, "," )
			);
		}
		// If it's an array, then it's data
		else if ( columnList instanceof Array castedRowData ) {
			rowData		= castedRowData;
			columnNames	= new Array();
			if ( !castedRowData.isEmpty() ) {
				columnNames = Array.fromList( StructCaster.cast( castedRowData.get( 0 ) ).getKeysAsStrings() );
			}
		} else {
			throw new BoxRuntimeException( "The [columnList] must be a string, or an array of data, or" );
		}

		// Verify Column Types
		Array columnTypes = ListUtil.asList( arguments.getAsString( Key.columnTypeList ), "," );
		if ( columnTypes.isEmpty() ) {
			// add "object" as default type
			for ( int i = 0; i < columnNames.size(); i++ ) {
				columnTypes.add( "object" );
			}
		} else if ( columnNames.size() != columnTypes.size() ) {
			throw new BoxRuntimeException( "columnList and columnTypeList must have the same number of elements" );
		}

		return Query.fromArray( columnNames, columnTypes, rowData );
	}

}
