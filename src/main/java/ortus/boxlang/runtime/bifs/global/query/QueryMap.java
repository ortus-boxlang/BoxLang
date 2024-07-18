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
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.Query;
import ortus.boxlang.runtime.types.util.ListUtil;

@BoxBIF
@BoxMember( type = BoxLangType.QUERY )
public class QueryMap extends BIF {

	/**
	 * Constructor
	 */
	public QueryMap() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "query", Key.query ),
		    new Argument( true, "function:Function", Key.callback ),
		    new Argument( false, "boolean", Key.parallel, false ),
		    new Argument( false, "integer", Key.maxThreads )
		};
	}

	/**
	 * This function maps the query to a new query.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.query The query to iterate over
	 *
	 * @argument.callback The function to invoke for each item. The function will be passed 3 arguments: the row, the currentRow, the query. You can alternatively pass a Java Function which will only receive the 1st arg.
	 *
	 * @argument.parallel Specifies whether the items can be executed in parallel
	 * 
	 * @argument.maxThreads The maximum number of threads to use when parallel = true
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Query	query			= arguments.getAsQuery( Key.query );

		Array	mappedResult	= ListUtil.map(
		    query.toStructArray(),
		    arguments.getAsFunction( Key.callback ),
		    context,
		    arguments.getAsBoolean( Key.parallel ),
		    arguments.getAsInteger( Key.maxThreads )
		);

		query.clear();
		query.addData( mappedResult );
		return query;
	}
}
