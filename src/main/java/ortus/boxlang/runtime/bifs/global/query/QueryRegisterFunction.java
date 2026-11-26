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

import java.util.Set;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.jdbc.qoq.QoQFunctionService;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Function;
import ortus.boxlang.runtime.types.QueryColumnType;
import ortus.boxlang.runtime.validation.Validator;

@BoxBIF( description = "Register a custom function for use in Query of Queries" )
public class QueryRegisterFunction extends BIF {

	/**
	 * Constructor
	 */
	public QueryRegisterFunction() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "string", Key._NAME ),
		    new Argument( true, "function", Key.function ),
		    new Argument( true, "string", Key.returnType, "Object" ),
		    new Argument( true, "string", Key.type, "scalar", Set.of( Validator.valueOneOf( "scalar", "aggregate" ) ) )
		};
	}

	/**
	 * Registers a custom scalar or aggregate function for use in Query of Queries SQL. Functions only need to be registered once per
	 * runtime; they are cached in memory until the runtime restarts.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.name The name of the function to register, without parentheses. Use this name in your SQL.
	 *
	 * @argument.function The function to register: a closure or function reference. A scalar function receives one value per
	 *                    incoming argument and returns a single value. An aggregate function receives an array of the (non-null) values being
	 *                    aggregated and returns a single value.
	 *
	 * @argument.returnType The return type of the function as a query column type. Defaults to "Object". It controls how return
	 *                      values are treated, for example whether customFunc() + customFunc2() adds numbers or concatenates strings.
	 *
	 * @argument.type The type of function to register: "scalar" (the default) or "aggregate".
	 *
	 * @return Nothing (null).
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Key				name		= Key.of( arguments.getAsString( Key._NAME ) );
		Function		function	= arguments.getAsFunction( Key.function );
		Key				type		= Key.of( arguments.getAsString( Key.type ) );
		QueryColumnType	returnType	= QueryColumnType.fromString( arguments.getAsString( Key.returnType ) );

		// Passing 0 for required args for now. It's a runtime check, so the UDF being passed can just declare it's args as required anyway.
		if ( type.equals( Key.scalar ) ) {
			QoQFunctionService.registerCustom( name, function, returnType, 0, context );
		} else {
			QoQFunctionService.registerCustomAggregate( name, function, returnType, 0, context );
		}

		return null;
	}

}
