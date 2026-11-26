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
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.Query;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

@BoxBIF( description = "Get data for a specific row in a query" )
@BoxMember( type = BoxLangType.QUERY, name = "rowData" )
// We are transpiling the CF BIF queryGetRow to this BIF
@BoxMember( type = BoxLangType.QUERY, name = "getRow" )
public class QueryRowData extends BIF {

	/**
	 * Constructor
	 */
	public QueryRowData() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "query", Key.query ),
		    new Argument( true, "integer", Key.rowNumber )
		};
	}

	/**
	 * Returns one row of a query as a struct keyed by column name. An exception is thrown if the row number is outside the query.
	 * Also available as <code>query.rowData( rowNumber )</code> or <code>query.getRow( rowNumber )</code>.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.query The query to read from.
	 *
	 * @argument.rowNumber The one-based number of the row to return.
	 *
	 * @return A struct containing the cells of the row.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Query	query		= arguments.getAsQuery( Key.query );
		int		rowNumber	= arguments.getAsInteger( Key.rowNumber );

		if ( rowNumber < 0 || rowNumber > query.size() ) {
			throw new BoxRuntimeException( "Row number outside of query row range." );
		}

		return query.getRowAsStruct( rowNumber - 1 );
	}
}
