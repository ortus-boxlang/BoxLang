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
import ortus.boxlang.runtime.dynamic.casters.IntegerCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.Query;

@BoxBIF
@BoxMember( type = BoxLangType.QUERY )
public class QueryInsertAt extends BIF {

	/**
	 * Constructor
	 */
	public QueryInsertAt() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, Argument.QUERY, Key.query ),
		    new Argument( true, Argument.QUERY, Key.value ),
		    new Argument( true, Argument.NUMERIC, Key.position )
		};
	}

	/**
	 * Inserts a query data into another query at a specific position
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.query The source query to insert to
	 *
	 * @argument.value The query that will be inserted
	 *
	 * @argument.position The position where the query will be inserted
	 *
	 * @return The modified query
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Integer	position	= IntegerCaster.cast( arguments.get( Key.position ) );
		Query	qSource		= arguments.getAsQuery( Key.query );
		Query	qTarget		= arguments.getAsQuery( Key.value );

		return qSource.insertQueryAt( position, qTarget );
	}
}
