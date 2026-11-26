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
import ortus.boxlang.runtime.jdbc.drivers.GenericJDBCDriver;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.Query;
import ortus.boxlang.runtime.types.QueryColumnType;

@BoxBIF( description = "Set the value of a specific cell in a query" )
@BoxMember( type = BoxLangType.QUERY )
public class QuerySetCell extends BIF {

	/**
	 * Constructor
	 */
	public QuerySetCell() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "query", Key.query ),
		    new Argument( true, "string", Key.column ),
		    new Argument( true, "any", Key.value ),
		    new Argument( false, "integer", Key.row )
		};
	}

	/**
	 * Sets the value of one cell of a query in place. If no row is given, the cell in the last row is set. Also available as
	 * <code>query.setCell( column, value, row )</code>.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.query The query to modify.
	 *
	 * @argument.column The name of the column to set.
	 *
	 * @argument.value The value to store in the cell.
	 *
	 * @argument.row The one-based row number of the cell. Defaults to the last row.
	 *
	 * @return The modified query.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Query	query		= arguments.getAsQuery( Key.query );
		Key		columnName	= Key.of( arguments.getAsString( Key.column ).trim() );
		Integer	rowNumber	= arguments.getAsInteger( Key.row );
		Object	value		= arguments.get( Key.value );

		if ( rowNumber == null ) {
			rowNumber = query.size();
		}

		QueryColumnType columnType = query.getColumn( columnName ).getType();
		if ( Query.queryNullToEmpty && !QueryColumnType.isStringType( columnType ) && value instanceof String castValue
		    && castValue.isEmpty() ) {
			value = null;
		}
		return query.setCell( columnName, rowNumber - 1, value != null ? GenericJDBCDriver.transformValueStatic(
		    columnType.sqlType,
		    QueryColumnType.toSQLType(
		        columnType,
		        value,
		        context,
		        null
		    ),
		    null
		) : null
		);
	}
}
