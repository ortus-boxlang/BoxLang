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

@BoxBIF( description = "Prepend rows from one query to another" )
@BoxMember( type = BoxLangType.QUERY )
public class QueryPrepend extends BIF {

	/**
	 * Constructor
	 */
	public QueryPrepend() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "query", Key.query1 ),
		    new Argument( true, "query", Key.query2 )
		};
	}

	/**
	 * Combines two queries so that the rows of the second query come first. The rows of query1 are added after the existing rows of
	 * query2, and query2 is the query that is modified in place and returned (query1 is not changed). Also available as
	 * <code>query.prepend( query2 )</code>, in which case the member query is query1.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.query1 The query whose rows are placed after the rows of query2. It is not modified.
	 *
	 * @argument.query2 The query that supplies the leading rows. It is modified in place and returned.
	 *
	 * @return The second query, containing its own rows followed by the rows of the first query.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Query	query1	= arguments.getAsQuery( Key.query1 );
		Query	query2	= arguments.getAsQuery( Key.query2 );
		query2.addAll( query1 );
		return query2;
	}
}
