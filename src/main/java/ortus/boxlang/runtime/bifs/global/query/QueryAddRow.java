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

@BoxBIF
@BoxMember( type = BoxLangType.QUERY )
public class QueryAddRow extends BIF {

	/**
	 * Constructor
	 */
	public QueryAddRow() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "query", Key.query ),
		    new Argument( true, "any", Key.rowData )
		};
	}

	/**
	 * Return new query
	 * 
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 * 
	 * @argument.query The query to add the row(s) to.
	 * 
	 * @argument.rowData Data to populate the query. Can be a struct (with keys matching column names), an array of structs, or an array of arrays (in
	 *                   same order as columnList)
	 * 
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		Query					query		= arguments.getAsQuery( Key.query );
		Object					rowData		= arguments.get( Key.rowData );
		CastAttempt<Integer>	castAttempt	= IntegerCaster.attempt( rowData );
		if ( castAttempt.wasSuccessful() ) {
			return query.addRows( castAttempt.get() );
		}
		return query.addData( rowData );
	}

}
