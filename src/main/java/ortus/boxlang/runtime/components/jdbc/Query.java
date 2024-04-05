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

import java.util.Set;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ortus.boxlang.runtime.components.Attribute;
import ortus.boxlang.runtime.components.BoxComponent;
import ortus.boxlang.runtime.components.Component;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.IJDBCCapableContext;
import ortus.boxlang.runtime.dynamic.ExpressionInterpreter;
import ortus.boxlang.runtime.dynamic.casters.ArrayCaster;
import ortus.boxlang.runtime.dynamic.casters.CastAttempt;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.dynamic.casters.StructCaster;
import ortus.boxlang.runtime.jdbc.ConnectionManager;
import ortus.boxlang.runtime.jdbc.DataSourceManager;
import ortus.boxlang.runtime.jdbc.ExecutedQuery;
import ortus.boxlang.runtime.jdbc.PendingQuery;
import ortus.boxlang.runtime.jdbc.QueryOptions;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.validation.Validator;

@BoxComponent( requiresBody = true )
public class Query extends Component {

	Logger log = LoggerFactory.getLogger( Query.class );

	/**
	 * Constructor
	 */
	public Query() {
		super();
		declaredAttributes = new Attribute[] {
		    new Attribute( Key._NAME, "string" ),
		    new Attribute( Key.datasource, "string" ),
		    // new Attribute( Key.timezone, "string" ),
		    // new Attribute( Key.dbtype, "string" ),
		    // new Attribute( Key.username, "string" ),
		    // new Attribute( Key.password, "string" ),
		    new Attribute( Key.maxRows, "numeric", -1 ),
		    // new Attribute( Key.blockfactor, "numeric", Set.of( Validator.min(1), Validator.max(100) ) ),
		    new Attribute( Key.timeout, "numeric" ),
		    // new Attribute( Key.cachedAfter, "date" ),
		    // new Attribute( Key.cachedWithin, "numeric" ),
		    // new Attribute( Key.debug, "boolean", false ),
		    new Attribute( Key.result, "string" ),
		    // new Attribute( Key.ormoptions, "struct" ),
		    // new Attribute( Key.cacheID, "string" ),
		    // new Attribute( Key.cacheRegion, "string" ),
		    // new Attribute( Key.clientInfo, "struct" ),
		    // new Attribute( Key.fetchClientInfo, "boolean", false ),
		    // new Attribute( Key.lazy, "boolean", false ),
		    // new Attribute( Key.psq, "boolean", false ),
		    new Attribute( Key.returnType, "string", "query", Set.of(
		        Validator.valueRequires( "struct", Key.columnKey )
		    ) ),
		    new Attribute( Key.columnKey, "string" )
		};

	}

	public BodyResult _invoke( IBoxContext context, IStruct attributes, ComponentBody body, IStruct executionState ) {
		IJDBCCapableContext	jdbcContext			= context.getParentOfType( IJDBCCapableContext.class );
		DataSourceManager	dataSourceManager	= context.getDataSourceManager();
		ConnectionManager	connectionManager	= jdbcContext.getConnectionManager();
		QueryOptions		options				= new QueryOptions( dataSourceManager, connectionManager, attributes );

		executionState.put( Key.queryParams, new Array() );
		StringBuffer	buffer		= new StringBuffer();
		BodyResult		bodyResult	= processBody( context, body, buffer );
		// IF there was a return statement inside our body, we early exit now
		if ( bodyResult.isEarlyExit() ) {
			return bodyResult;
		}
		String			sql				= buffer.toString();

		Array			bindings		= executionState.getAsArray( Key.queryParams );
		PendingQuery	pendingQuery	= createPendingQueryWithBindings( sql, bindings, options );

		pendingQuery.setQueryTimeout( options.getQueryTimeout() );
		pendingQuery.setMaxRows( options.getMaxRows() );

		ExecutedQuery executedQuery = pendingQuery.execute( options.getConnnection() );

		if ( options.wantsResultStruct() ) {
			assert options.getResultVariableName() != null;
			ExpressionInterpreter.setVariable( context, options.getResultVariableName(), executedQuery.getResultStruct() );
		}

		String variableName = StringCaster.cast( attributes.getOrDefault( Key._NAME, "cfquery" ) );
		ExpressionInterpreter.setVariable( context, variableName, options.castAsReturnType( executedQuery ) );

		return DEFAULT_RETURN;
	}

	private PendingQuery createPendingQueryWithBindings( @Nonnull String sql, Object bindings, QueryOptions options ) {
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

		// We always have bindings, since we exit early if there are none
		String className = bindings.getClass().getName();
		throw new BoxRuntimeException( "Invalid type for params. Expected array or struct. Received: " + className );
	}
}
