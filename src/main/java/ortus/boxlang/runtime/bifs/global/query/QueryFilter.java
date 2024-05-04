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

import java.util.UUID;
import java.util.function.IntPredicate;
import java.util.stream.IntStream;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.services.AsyncService;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.Query;
import ortus.boxlang.runtime.types.util.BLCollector;

@BoxBIF
@BoxMember( type = BoxLangType.QUERY )
public class QueryFilter extends BIF {

	/**
	 * Constructor
	 */
	public QueryFilter() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "query", Key.query ),
		    new Argument( true, "function", Key.callback ),
		    new Argument( false, "boolean", Key.parallel, false ),
		    new Argument( false, "integer", Key.maxThreads ),
		};
	}

	/**
	 * Filters query rows specified in filter criteria
	 *
	 * 
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.query The query to get filtered
	 * 
	 * @argument.callback The function to invoke for each item. The function will be passed 3 arguments: the value, the index, the array.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		var				query		= arguments.getAsQuery( Key.query );
		var				callback	= arguments.getAsFunction( Key.callback );
		var				parallel	= arguments.getAsBoolean( Key.parallel );
		var				maxThreads	= arguments.getAsInteger( Key.maxThreads );

		IntPredicate	test		= idx -> BooleanCaster.cast( context.invokeFunction( callback,
		    new Object[] { query.getRowAsStruct( idx ), idx + 1, query } ) );

		IntStream		intStream	= query.intStream();

		Query			newQuery	= new Query();

		for ( var column : query.getColumns().entrySet() ) {
			newQuery.addColumn( column.getKey(), column.getValue().getType() );
		}

		if ( parallel ) {
			return AsyncService.buildExecutor(
			    "QueryFilter_" + UUID.randomUUID().toString(),
			    AsyncService.ExecutorType.FORK_JOIN,
			    maxThreads
			).submitAndGet( () -> query.intStream().parallel().filter( test ).mapToObj( query::getRowAsStruct ).collect( BLCollector.toQuery( newQuery ) ) );
		} else {
			return intStream
			    .filter( test )
			    .mapToObj( query::getRowAsStruct )
			    .collect( BLCollector.toQuery( newQuery ) );
		}
	}
}
