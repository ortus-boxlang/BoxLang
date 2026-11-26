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
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.Query;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

@BoxBIF( description = "Delete a column from a query" )
@BoxMember( type = BoxLangType.QUERY )
public class QueryDeleteColumn extends BIF {

	public QueryDeleteColumn() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "query", Key.query ),
		    new Argument( true, "string", Key.column )
		};
	}

	/**
	 * Deletes a column from a query. The query is modified in place. An exception is thrown if the column does not exist. Also
	 * available as <code>query.deleteColumn( column )</code>.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.query The query to delete the column from.
	 *
	 * @argument.column The name of the column to delete.
	 *
	 * @return The query without the deleted column.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Query	query		= arguments.getAsQuery( Key.query );
		String	column		= arguments.getAsString( Key.column ).trim();
		Key		columnKey	= Key.of( column );

		// Verify that the column exists, throw error if it does not
		if ( !query.hasColumn( Key.of( columnKey ) ) ) {
			throw new BoxRuntimeException( "Column '" + column + "' does not exist in the query." );
		}

		// Delete our column
		query.deleteColumn( columnKey );

		return query;
	}
}
