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

@BoxBIF( description = "Get the value of a specific cell in a query" )
@BoxMember( type = BoxLangType.QUERY )
public class QueryGetCell extends BIF {

	/**
	 * Constructor
	 */
	public QueryGetCell() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "query", Key.query ),
		    new Argument( true, "string", Key.column_name ),
		    new Argument( false, "integer", Key.row_number )
		};
	}

	/**
	 * Returns the value of a single cell of a query. Also available as <code>query.getCell( column_name, row_number )</code>. Note
	 * that although row_number is declared optional, omitting it causes an error, so always supply it. An out-of-range row throws an
	 * exception.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.query The query to read from.
	 *
	 * @argument.column_name The name of the column to read.
	 *
	 * @argument.row_number The one-based row number of the cell.
	 *
	 * @return The value of the cell.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Query	query		= arguments.getAsQuery( Key.query );
		String	columnName	= arguments.getAsString( Key.column_name ).trim();
		Integer	rowNumber	= arguments.getAsInteger( Key.row_number );
		return query.getCell( Key.of( columnName ), rowNumber - 1 );
	}
}
