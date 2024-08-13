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

@BoxBIF
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
	 * Returns the cells of a query row as a structure
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.query The query object to which the row should be returned.
	 * 
	 * @argument.rowNumber Position of the query row to return.
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
