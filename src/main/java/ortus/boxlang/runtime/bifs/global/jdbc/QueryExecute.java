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
		    new Argument( true, Argument.STRING, Key.sql, Set.of( Validator.REQUIRED, Validator.NON_EMPTY ) ),
		    new Argument( false, Argument.ANY, Key.params, new Array(), Set.of( Validator.typeOneOf( "array", "struct" ) ) ),
		    new Argument( false, Argument.STRUCT, Key.options, new Struct() )
		};
	}

	/**
	 * Execute an SQL query and returns the results.
	 * <h2>Parameters</h2>
	 * The <code>parameters</code> argument can be used to bind parameters to the SQL query.
	 * You can use either an array of binding parameters or a struct of named binding parameters.
	 * The SQL must have the parameters bound using the syntax <code>?</code> for positional parameters or <code>:name</code> for named parameters.
	 * <p>
	 * Example:
	 *
	 * <pre>
	 * queryExecute( sql: "SELECT * FROM users WHERE id = ?", params: [ 1 ] );
	 * queryExecute( sql: "SELECT * FROM users WHERE id = :id", params: { id: 1 } );
	 * </pre>
	 * <p>
	 * You can also treat each named parameter to not only be a key-value pair, but also a struct with additional options:
	 * <ul>
	 * <li><strong>value:any</strong> - The value to bind to the parameter</li>
	 * <li><strong>type:string</strong> - The type of the value, defaults to "varchar"</li>
	 * <li><strong>maxLength:numeric</strong> - The maximum length of the value, only applicable for string types</li>
	 * <li><strong>scale:numeric</strong> - The scale of the value, only applicable for decimal types, defaults to 0</li>
	 * <li><strong>null:boolean</strong> - Whether the value is null, defaults to false</li>
	 * <li><strong>list:boolean</strong> - Whether the value is a list, defaults to false</li>
	 * <li><strong>separator:string</strong> - The separator to use for list values, defaults to ","</li>
	 * </ul>
	 * <p>
	 * Example:
	 *
	 * <pre>
	 * queryExecute( sql: "SELECT * FROM users WHERE id = :id", params: { id: { value: 1, type: "integer" } } );
	 * queryExecute( sql: "SELECT * FROM users WHERE id IN (:ids)", params: { ids: { value: [ 1, 2, 3 ], type: "integer", list: true, separator: "," } } );
	 * </pre>
	 *
	 * <h2>Options</h2>
	 * The available options for this BIF are:
	 * <ul>
	 * <><strong>cache:boolean</strong> - Whether to cache the query results, defaults to false</li>
	 * <li><strong>cacheKey:string</strong> - Your own cache key, if not specified, the SQL will be used as the cache key</li>
	 * <li><strong>cacheTimeout:timespan|seconds</strong> - The timeout for the cache, defaults to 0 (no timeout)</li>
	 * <li><strong>cacheLastAccessTimeout:timespan|seconds</strong> - The timeout for the last access to the cache, defaults to 0 (no timeout)</li>
	 * <li><strong>cacheProvider:string</strong> - The cache provider to use, defaults to the default cache provider</li>
	 * <li><strong>columnKey:string</strong> - The column to use as the key when returntype is "struct"</li>
	 * <li><strong>datasource:string</strong> - The datasource name to use for the query, if not specified, the default datasource will be used</li>
	 * <li><strong>dbtype:string</strong> - The database type to use for the query, this is either for query of queries or HQL. Mutually exclusive with <code>datasource</code></li>
	 * <li><strong>fetchsize:numeric</strong> - Number of rows to fetch from database at once, defaults to all rows (0)</li>
	 * <li><strong>maxrows:numeric</strong> - Maximum number of rows to return</li>
	 * <li><strong>result</strong> - The name of the variable to store the results of the query</li>
	 * <li><strong>returntype</strong> - The return type: "query", "array", "struct"</li>
	 * <li><strong>timeout</strong> - Query timeout in seconds</li>
	 * </ul>
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.sql The SQL to execute
	 *
	 * @argument.params An array of binding parameters or a struct of named binding parameters
	 *
	 * @argument.options A struct of query options
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		String			sql				= arguments.getAsString( Key.sql );
		Object			bindings		= arguments.get( Key.params );
		QueryOptions	options			= new QueryOptions( arguments.getAsStruct( Key.options ) );
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
