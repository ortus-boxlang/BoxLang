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

@BoxBIF( description = "Append rows from one query to another" )
@BoxMember( type = BoxLangType.QUERY )
public class QueryAppend extends BIF {

	/**
	 * Constructor
	 */
	public QueryAppend() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "query", Key.query1 ),
		    new Argument( true, "query", Key.query2 )
		};
	}

	/**
	 * Appends all rows of the second query to the end of the first query. The first query is modified in place and returned. Values
	 * are matched by column name; columns of query2 that do not exist in query1 are ignored and missing values become null. Also
	 * available as <code>query.append( query2 )</code>.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.query1 The query to append to. It is modified and returned.
	 *
	 * @argument.query2 The query whose rows are appended. It is not modified.
	 *
	 * @return The first query, containing its original rows followed by the rows of the second query.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Query query1 = arguments.getAsQuery( Key.query1 );
		query1.addAll( arguments.getAsQuery( Key.query2 ) );
		return query1;
	}
}
