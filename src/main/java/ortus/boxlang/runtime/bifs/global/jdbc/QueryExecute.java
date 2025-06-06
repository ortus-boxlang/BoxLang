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
package ortus.boxlang.runtime.bifs.global.jdbc;

import java.sql.Connection;
import java.util.Set;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.IJDBCCapableContext;
import ortus.boxlang.runtime.dynamic.ExpressionInterpreter;
import ortus.boxlang.runtime.jdbc.ConnectionManager;
import ortus.boxlang.runtime.jdbc.ExecutedQuery;
import ortus.boxlang.runtime.jdbc.PendingQuery;
import ortus.boxlang.runtime.jdbc.QueryOptions;
import ortus.boxlang.runtime.jdbc.qoq.QoQConnection;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.validation.Validator;

@BoxBIF
public class QueryExecute extends BIF {

	/**
	 * Constructor
	 */
	public QueryExecute() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "String", Key.sql, Set.of( Validator.REQUIRED, Validator.NON_EMPTY ) ),
		    new Argument( false, "any", Key.params, new Array(), Set.of( Validator.typeOneOf( "array", "struct" ) ) ),
		    new Argument( false, "struct", Key.options )
		};
	}

	/**
	 * Execute an SQL query and returns the results.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.sql The SQL to execute
	 *
	 * @argument.params An array of binding parameters or a struct of named binding parameters
	 *
	 * @argument.options A struct of query options
	 *
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		IStruct optionsAsStruct = arguments.getAsStruct( Key.options );
		if ( optionsAsStruct == null ) {
			optionsAsStruct = new Struct();
		}
		String			sql				= arguments.getAsString( Key.sql );
		Object			bindings		= arguments.get( Key.params );

		QueryOptions	options			= new QueryOptions( optionsAsStruct );
		PendingQuery	pendingQuery	= new PendingQuery( context, sql, bindings, options );

		ExecutedQuery	executedQuery;
		// QoQ uses a special QoQ connection
		if ( options.isQoQ() ) {
			Connection connection = new QoQConnection( context );
			executedQuery = pendingQuery.execute( connection, context );
		} else {
			// whereas normal queries use the JDBC connection manager
			IJDBCCapableContext	jdbcContext			= context.getParentOfType( IJDBCCapableContext.class );
			ConnectionManager	connectionManager	= jdbcContext.getConnectionManager();
			executedQuery = pendingQuery.execute( connectionManager, context );
		}

		if ( options.wantsResultStruct() ) {
			assert options.resultVariableName != null;
			ExpressionInterpreter.setVariable( context, options.resultVariableName, executedQuery.getResults().getMetaData() );
		}

		// Encapsulate this into the executed query
		return options.castAsReturnType( executedQuery );
	}

}
