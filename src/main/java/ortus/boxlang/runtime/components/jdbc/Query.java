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
import ortus.boxlang.runtime.jdbc.ConnectionManager;
import ortus.boxlang.runtime.jdbc.ExecutedQuery;
import ortus.boxlang.runtime.jdbc.PendingQuery;
import ortus.boxlang.runtime.jdbc.QueryOptions;
import ortus.boxlang.runtime.jdbc.qoq.QoQConnection;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.validation.Validator;

@BoxComponent( requiresBody = true )
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
		        Validator.valueRequires( "struct", Key.columnKey )
		    ) ),
		    new Attribute( Key.columnKey, "string" ),

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

		    // UNIMPLEMENTED query options:
		    new Attribute( Key.timezone, "string", Set.of(
		        Validator.NOT_IMPLEMENTED
		    ) ),
		    new Attribute( Key.dbtype, "string", Set.of(
		        Validator.NON_EMPTY, Validator.valueOneOf( "query", "hql" )
		    ) ),
		    new Attribute( Key.username, "string", Set.of(
		        Validator.NOT_IMPLEMENTED
		    ) ),
		    new Attribute( Key.password, "string", Set.of(
		        Validator.NOT_IMPLEMENTED
		    ) ),
		    new Attribute( Key.debug, "boolean", false, Set.of(
		        Validator.NOT_IMPLEMENTED
		    ) ),
		    new Attribute( Key.result, "string" ),
		    new Attribute( Key.ormoptions, "struct", Set.of(
		        Validator.NOT_IMPLEMENTED
		    ) ),
		    new Attribute( Key.clientInfo, "struct", Set.of(
		        Validator.NOT_IMPLEMENTED
		    ) ),
		    new Attribute( Key.fetchClientInfo, "boolean", false, Set.of(
		        Validator.NOT_IMPLEMENTED
		    ) ),
		    new Attribute( Key.lazy, "boolean", false, Set.of(
		        Validator.NOT_IMPLEMENTED
		    ) ),
		    new Attribute( Key.psq, "boolean", false, Set.of(
		        Validator.NOT_IMPLEMENTED
		    ) )
		};

	}

	public BodyResult _invoke( IBoxContext context, IStruct attributes, ComponentBody body, IStruct executionState ) {
		QueryOptions options = new QueryOptions( attributes );

		executionState.put( Key.queryParams, new Array() );

		StringBuffer buffer = new StringBuffer();

		// Spoof being in the output component in case the app has enableoutputonly=true
		context.pushComponent(
		    Struct.of(
		        Key._NAME, Key.output,
		        Key._CLASS, null,
		        Key.attributes, Struct.EMPTY
		    )
		);
		BodyResult bodyResult = processBody( context, body, buffer );
		context.popComponent();

		// IF there was a return statement inside our body, we early exit now
		if ( bodyResult.isEarlyExit() ) {
			return bodyResult;
		}

		String	sql			= buffer.toString();
		Object	bindings	= executionState.getAsArray( Key.queryParams );
		if ( attributes.containsKey( Key.params ) && attributes.get( Key.params ) != null ) {
			if ( ( ( Array ) bindings ).size() > 0 ) {
				throw new IllegalArgumentException( "Cannot specify both query parameters in the body and as an attribute." );
			}
			bindings = attributes.get( Key.params );
		}
		PendingQuery	pendingQuery	= new PendingQuery( sql, bindings, options );

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

		String variableName = StringCaster.cast( attributes.getOrDefault( Key._NAME, "bxquery" ) );
		ExpressionInterpreter.setVariable( context, variableName, options.castAsReturnType( executedQuery ) );

		return DEFAULT_RETURN;
	}
}
