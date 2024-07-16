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
public class QueryRowSwap extends BIF {

	/**
	 * Constructor
	 */
	public QueryRowSwap() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, Argument.QUERY, Key.query ),
		    new Argument( true, Argument.NUMERIC, Key.source ),
		    new Argument( true, Argument.NUMERIC, Key.destination )
		};
	}

	/**
	 * In a query object, swap the record in the sourceRow with the record from the destinationRow.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.query The query to swap a row with
	 *
	 * @argument.source The row to swap from
	 *
	 * @argument.destination The row to swap to
	 *
	 * @return The modified query
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Integer	source		= IntegerCaster.cast( arguments.get( Key.source ) );
		Integer	destination	= IntegerCaster.cast( arguments.get( Key.destination ) );
		Query	qSource		= arguments.getAsQuery( Key.query );

		return qSource.swapRow( source--, destination-- );
	}
}
