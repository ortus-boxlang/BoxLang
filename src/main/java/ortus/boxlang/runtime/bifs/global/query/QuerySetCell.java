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
import ortus.boxlang.runtime.dynamic.casters.GenericCaster;

@BoxBIF
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
	 * Sets a cell to a value.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.query The query to set the cell in
	 * 
	 * @argument.column The column name to set the cell in
	 * 
	 * @argument.value The value to set the cell to
	 * 
	 * @argument.row The row number to set the cell in. If no row number is specified, the cell on the last row is set.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Query	query		= arguments.getAsQuery( Key.query );
		Key	columnName		= Key.of( arguments.getAsString( Key.column ) );
		Integer	rowNumber	= arguments.getAsInteger( Key.row );
		Object	value		= arguments.get( Key.value );

		if ( rowNumber == null ) {
			rowNumber = query.size();
		}

		String columnType = query.getColumn( columnName ).getType().toString();
		return query.setCell( columnName, rowNumber - 1, GenericCaster.cast( context, value, columnType ) );
	}
}
