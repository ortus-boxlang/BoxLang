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
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.ExpressionInterpreter;
import ortus.boxlang.runtime.dynamic.casters.*;
import ortus.boxlang.runtime.jdbc.*;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.*;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

import javax.annotation.Nonnull;
import java.sql.Connection;
import java.util.Optional;

@BoxBIF
public class QueryExecute extends BIF {

	/**
	 * Constructor
	 */
	public QueryExecute() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "String", Key.sql ),
		    new Argument( false, "any", Key.params, new Array() ),
		    new Argument( false, "struct", Key.options )
		};
	}

	/**
	 * Executes a query and returns the results.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.sql The SQL to execute
	 *
	 * @argument.params An array of binding parameters or a struct of named binding parameters
	 *
	 * @argument.options A struct of queryExecute options
	 *
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		CastAttempt<IStruct>	optionsAsStruct	= StructCaster.attempt( arguments.get( Key.options ) );
		QueryOptions			options			= new QueryOptions( optionsAsStruct.getOrDefault( new Struct() ) );

		String					sql				= arguments.getAsString( Key.sql );
		Object					bindings		= arguments.get( Key.params );
		PendingQuery			pendingQuery	= createPendingQueryWithBindings( sql, bindings, options );

		pendingQuery.setQueryTimeout( options.getQueryTimeout() );
		pendingQuery.setMaxRows( options.getMaxRows() );

		ExecutedQuery executedQuery = pendingQuery.execute( options.getConnnection() );

		if ( options.wantsResultStruct() ) {
			assert options.getResultVariableName() != null;
			ExpressionInterpreter.setVariable( context, options.getResultVariableName(), executedQuery.getResultStruct() );
		}

		return options.castAsReturnType( executedQuery );
	}

	private PendingQuery createPendingQueryWithBindings( @Nonnull String sql, Object bindings, QueryOptions options ) {
		PendingQuery query = null;

		if ( bindings == null ) {
			return new PendingQuery( sql );
		}

		CastAttempt<Array> castAsArray = ArrayCaster.attempt( bindings );
		if ( castAsArray.wasSuccessful() ) {
			return new PendingQuery( sql, castAsArray.getOrFail() );
		}

		CastAttempt<IStruct> castAsStruct = StructCaster.attempt( bindings );
		if ( castAsStruct.wasSuccessful() ) {
			return PendingQuery.fromStructParameters( sql, castAsStruct.getOrFail() );
		}

		String className = "null";
		if ( bindings != null ) {
			className = bindings.getClass().getName();
		}
		throw new BoxRuntimeException( "Invalid type for params. Expected array or struct. Received: " + className );
	}

}
