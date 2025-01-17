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

@BoxBIF
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
	 * Register a new scalar or aggregate function for use with Query of Query. Functions only need to be registered once per runtime and they will be cached in memory until
	 * the runtime restarts.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.name The name of the function to register without parenthesis. You will reference it in your SQL with this name.
	 * 
	 * @argument.function The function to register. This can be a closure or a function reference. For scalar functions, the function will receive a value for each incoming argument and must return a single value.
	 *                    For aggregate functions, the function will receive a value for each incoming argument in the form of an array of values representing each row being aggregated and must return a single value.
	 *                    Null values are not passed into aggregates. Aggregate functions should return a scalar value
	 *                    so the size of the array won't necessarily match the number of rows in the query, and may be different across columns.
	 * 
	 * @argument.type The type of function to register. This can be "scalar" or "aggregate". Default is "scalar".
	 * 
	 * @argument.returnType The return type of the function as a query column type. Default is "Object". This can be any valid query column type. This can be used to enforce
	 *                      how the return values of this function are handled. For example, customFunc() + customFunc2() will behave differently based on whether the custom functions return strings or numbers.
	 * 
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
