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

import java.util.Collections;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.Query;
import ortus.boxlang.runtime.util.DuplicationUtil;

@BoxBIF
@BoxMember( type = BoxLangType.QUERY )
public class QueryReverse extends BIF {

	/**
	 * Constructor
	 */
	public QueryReverse() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, Argument.QUERY, Key.query )
		};
	}

	/**
	 * This function reverses the query data
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.query The query to reverse
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Query query = ( Query ) DuplicationUtil.duplicate( arguments.get( Key.query ), false );

		Collections.reverse( query.getData() );

		return query;
	}
}
