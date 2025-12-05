/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ortus.boxlang.runtime.components.jdbc;

import java.sql.Connection;
import java.util.Set;

import ortus.boxlang.runtime.components.Attribute;
import ortus.boxlang.runtime.components.BoxComponent;
import ortus.boxlang.runtime.components.Component;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.IJDBCCapableContext;
import ortus.boxlang.runtime.dynamic.ExpressionInterpreter;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.jdbc.BoxConnection;
import ortus.boxlang.runtime.jdbc.ConnectionManager;
import ortus.boxlang.runtime.jdbc.ExecutedQuery;
import ortus.boxlang.runtime.jdbc.PendingQuery;
import ortus.boxlang.runtime.jdbc.QueryOptions;
import ortus.boxlang.runtime.jdbc.qoq.QoQConnection;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.validation.Validator;

@BoxComponent( description = "Execute SQL queries against databases", requiresBody = true, ignoreEnableOutputOnly = true, autoEvaluateBodyExpressions = true )
public class Query extends Component {

	/**
	 * Constructor
	 */
	public Query() {
		super();
		declaredAttributes = new Attribute[] {
		    new Attribute( Key._NAME, "string" ),
		    new Attribute( Key.datasource, "string" ),
		    new Attribute( Key.returnType, "string", "query", Set.of(
		        Validator.valueRequires( "struct", Key.columnKey ),
		        Validator.valueOneOf( "query", "array", "struct" )
		    ) ),
		    new Attribute( Key.columnKey, "string" ),
		    new Attribute( Key.dbtype, "string", Set.of(
		        Validator.NON_EMPTY, Validator.valueOneOf( "query", "hql" )
		    ) ),
		    new Attribute( Key.username, "string" ),
		    new Attribute( Key.password, "string" ),

		    // connection options
		    new Attribute( Key.maxRows, "integer", -1 ),
		    new Attribute( Key.blockfactor, "integer", Set.of( Validator.min( 1 ), Validator.max( 100 ) ), Set.of() ),
		    new Attribute( Key.fetchSize, "integer", Set.of( Validator.min( 1 ), Validator.max( 100 ) ) ),
		    new Attribute( Key.timeout, "integer" ),

		    // cache options
		    new Attribute( Key.cache, "boolean", false ),
		    new Attribute( Key.cacheTimeout, "duration" ),
		    new Attribute( Key.cacheLastAccessTimeout, "duration" ),
		    new Attribute( Key.cacheKey, "string" ),
		    new Attribute( Key.cacheProvider, "string" ),
		    new Attribute( Key.result, "string" ),

		    // Missing
		    new Attribute( Key.clientInfo, "struct", Set.of(
		        Validator.NOT_IMPLEMENTED
		    ) )
		};
	}

	/**
	 * Execute a SQL query to the default or specified datasource.
	 * <p>
	 * <strong>We recommend you ALWAYS use query params on any bind variables</strong>
	 *
	 * @param context        The context in which the Component is being invoked
	 * @param attributes     The attributes to the Component
	 * @param body           The body of the Component
	 * @param executionState The execution state of the Component
	 *
	 * @attribute.name The name of the variable to store the query results in.
	 *
	 * @attribute.datasource The datasource to execute the query against.
	 *
	 * @attribute.returnType The type of the result to return. One of: `query`, `struct`, `array`.
	 *
	 * @attribute.columnKey The key to use for the column names in the result struct.
	 *
	 * @attribute.dbtype The type of query to execute. One of: `query`, `hql`.
	 *
	 * @attribute.maxRows The maximum number of rows to return. -1 for no limit.
	 *
	 * @attribute.blockfactor Maximum rows per block to fetch from the server. Ranges from 1-100.
	 *
	 * @attribute.fetchSize The number of rows to fetch at a time. Ranges from 1-100.
	 *
	 * @attribute.timeout The timeout for the query in seconds.
	 *
	 * @attribute.cache Whether or not to cache the results of the query.
	 *
	 * @attribute.cacheTimeout The timeout for the cached query, using a duration object like `createTimespan( 0, 1, 0, 0 )`.
	 *
	 * @attribute.cacheLastAccessTimeout The timeout for the cached query, using a duration object like `createTimespan( 0, 1, 0, 0 )`.
	 *
	 * @attribute.cacheKey The key to use for the cached query.
	 *
	 * @attribute.cacheProvider String name of the cache provider to use. Defaults to the default cache provider.
	 *
	 * @attribute.result The name of the variable to store the query result in.
	 */
	@Override
	public BodyResult _invoke( IBoxContext context, IStruct attributes, ComponentBody body, IStruct executionState ) {
		// Prepare the attributes
		QueryOptions options = new QueryOptions( attributes );
		executionState.put( Key.queryParams, new Array() );
		StringBuffer	buffer		= new StringBuffer();

		// Process the body of the query
		BodyResult		bodyResult	= processBody( context, body, buffer );

		// If there was a return statement inside our body, we early exit now
		if ( bodyResult.isEarlyExit() ) {
			return bodyResult;
		}

		// If the body did not return anything, we assume it was a query
		String	sql			= buffer.toString();
		Object	bindings	= executionState.getAsArray( Key.queryParams );
		if ( attributes.containsKey( Key.params ) && attributes.get( Key.params ) != null ) {
			if ( ( ( Array ) bindings ).size() > 0 ) {
				throw new IllegalArgumentException( "Cannot specify both query parameters in the body and as an attribute." );
			}
			bindings = attributes.get( Key.params );
		}
		PendingQuery	pendingQuery	= new PendingQuery( context, sql, bindings, options );

		ExecutedQuery	executedQuery;
		// QoQ uses a special QoQ connection
		if ( options.isQoQ() ) {
			Connection connection = new QoQConnection( context );
			executedQuery = pendingQuery.execute( BoxConnection.of( connection, null ), context );
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

		String variableName = StringCaster.cast( attributes.getOrDefault( Key._NAME, "bxquery" ) );
		ExpressionInterpreter.setVariable( context, variableName, options.castAsReturnType( executedQuery ) );

		return DEFAULT_RETURN;
	}
}
