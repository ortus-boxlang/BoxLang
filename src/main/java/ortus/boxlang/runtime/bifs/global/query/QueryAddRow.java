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
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.CastAttempt;
import ortus.boxlang.runtime.dynamic.casters.IntegerCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.Query;

@BoxBIF( description = "Add a new row to a query" )
@BoxMember( type = BoxLangType.QUERY )
public class QueryAddRow extends BIF {

	/**
	 * Constructor
	 */
	public QueryAddRow() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "query", Key.query ),
		    new Argument( false, "any", Key.rowData )
		};
	}

	/**
	 * Adds rows to a query, modifying it in place. The rowData argument controls what is added:
	 * <ul>
	 * <li>Omitted: one empty row is added</li>
	 * <li>An integer: that many empty rows are added</li>
	 * <li>A struct: one row is added, matching struct keys to column names</li>
	 * <li>An array of structs or an array of arrays: one row is added per element</li>
	 * <li>An array of simple values: one row is added, with the values in column order</li>
	 * </ul>
	 * Also available as <code>query.addRow()</code>.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.query The query to add the row(s) to.
	 *
	 * @argument.rowData The data for the new row(s): an integer number of empty rows, a struct (with keys matching column names), an
	 *                   array of structs, an array of arrays (in column order), or an array of simple values for a single row.
	 *
	 * @return The row number of the last row added, which is also the new number of rows in the query.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Query					query			= arguments.getAsQuery( Key.query );
		Object					rowData			= arguments.get( Key.rowData );
		CastAttempt<Integer>	IntegerAttempt	= IntegerCaster.attempt( rowData );

		// If rowData is an integer, add that many empty rows
		if ( IntegerAttempt.wasSuccessful() ) {
			return query.addRows( IntegerAttempt.get() );
		}

		// Otherwise, add a single row with the provided data (if any)
		if ( arguments.get( Key.rowData ) == null ) {
			return query.addEmptyRow();
		}

		// If rowData is provided, it must be an array of arrays or structs, or a single struct
		return query.addData( rowData, context );
	}

}
