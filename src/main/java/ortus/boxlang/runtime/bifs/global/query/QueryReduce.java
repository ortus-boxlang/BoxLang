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

@BoxBIF( description = "Reduce query data to a single value using a callback function" )
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
	 * Reduces a query to a single value by invoking the callback for each row with the running accumulator.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.query The query to reduce.
	 *
	 * @argument.callback The reducer function. It receives the accumulator, the row as a struct, the one-based row number, and the
	 *                    array of row structs. You can alternatively pass a Java BiFunction, which receives only the accumulator and the row.
	 *
	 * @argument.initialValue The initial value of the accumulator.
	 *
	 * @return The final accumulated value.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		return ListUtil.reduce(
		    arguments.getAsQuery( Key.query ).toArrayOfStructs(),
		    arguments.getAsFunction( Key.callback ),
		    context,
		    arguments.get( Key.initialValue )
		);
	}
}
