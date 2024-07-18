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
import ortus.boxlang.runtime.types.util.ListUtil;

@BoxBIF
@BoxMember( type = BoxLangType.QUERY )
public class QueryReduce extends BIF {

	/**
	 * Constructor
	 */
	public QueryReduce() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "query", Key.query ),
		    new Argument( true, "function:BiFunction", Key.callback ),
		    new Argument( true, "any", Key.initialValue )
		};
	}

	/**
	 * This function reduces the query to a single value.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.query The query to iterate over
	 *
	 * @argument.callback The function to invoke for each item. The function will be passed 4 arguments: the accumulator, the current item, the current index, and the query. You can alternatively pass a Java Predicate which will only receive the first 2
	 *                    args.
	 *
	 * @argument.initialValue The initial value to use for the reduction
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		return ListUtil.reduce(
		    arguments.getAsQuery( Key.query ).toStructArray(),
		    arguments.getAsFunction( Key.callback ),
		    context,
		    arguments.get( Key.initialValue )
		);
	}
}
