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

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import ortus.boxlang.runtime.components.Attribute;
import ortus.boxlang.runtime.components.BoxComponent;
import ortus.boxlang.runtime.components.Component;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.IJDBCCapableContext;
import ortus.boxlang.runtime.dynamic.ExpressionInterpreter;
import ortus.boxlang.runtime.dynamic.casters.IntegerCaster;
import ortus.boxlang.runtime.jdbc.ConnectionManager;
import ortus.boxlang.runtime.jdbc.QueryOptions;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.QueryColumnType;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.exceptions.DatabaseException;
import ortus.boxlang.runtime.validation.Validator;

@BoxComponent( requiresBody = true )
public class StoredProc extends Component {

	/**
	 * Constructor
	 */
	public StoredProc() {
		super();
		declaredAttributes = new Attribute[] {
		    new Attribute( Key.procedure, "string", Set.of( Validator.REQUIRED, Validator.NON_EMPTY ) ),
		    new Attribute( Key.datasource, "string" ),
		    new Attribute( Key.username, "string", Set.of( Validator.NOT_IMPLEMENTED ) ),
		    new Attribute( Key.password, "string", Set.of( Validator.NOT_IMPLEMENTED ) ),
		    new Attribute( Key.blockfactor, "numeric", Set.of( Validator.NOT_IMPLEMENTED ) ),
		    new Attribute( Key.debug, "boolean", false, Set.of( Validator.NOT_IMPLEMENTED ) ),
		    new Attribute( Key.returnCode, "boolean", false, Set.of( Validator.NOT_IMPLEMENTED ) ),
		    new Attribute( Key.result, "string", Set.of( Validator.NOT_IMPLEMENTED ) ),
		};

	}

	/**
	 * Execute a stored procedure.
	 *
	 * @param context        The context in which the Component is being invoked
	 * @param attributes     The attributes to the Component
	 * @param body           The body of the Component
	 * @param executionState The execution state of the Component
	 *
	 * @attribute.procedure The name of the procedure to execute.
	 *
	 * @attribute.datasource The name of the datasource where the stored procedure is registered.
	 *
	 */
	public BodyResult _invoke( IBoxContext context, IStruct attributes, ComponentBody body, IStruct executionState ) {
		IJDBCCapableContext	jdbcContext			= context.getParentOfType( IJDBCCapableContext.class );
		ConnectionManager	connectionManager	= jdbcContext.getConnectionManager();
		QueryOptions		options				= new QueryOptions( attributes );

		Array				params				= new Array();
		Array				procResults			= new Array();

		// these are placed in the execution state to allow the ProcParam and ProcResult components to register themselves with this component
		executionState.put( Key.queryParams, params );
		executionState.put( Key.procResult, procResults );

		BodyResult bodyResult = processBody( context, body, null );

		// If there was a return statement inside our body, we early exit now
		if ( bodyResult.isEarlyExit() ) {
			return bodyResult;
		}

		String callString = buildCallString( attributes.getAsString( Key.procedure ), params );
		try (
		    Connection conn = connectionManager.getConnection( options );
		    CallableStatement procedure = conn.prepareCall( callString ); ) {
			registerProcedureParams( procedure, params );

			procedure.execute();

			putOutVariablesInContext( context, procedure, params );
			putResultSetsInContext( context, procedure, procResults );

		} catch ( SQLException e ) {
			throw new DatabaseException( e.getMessage(), e );
		}

		return DEFAULT_RETURN;
	}

	private void registerProcedureParams( CallableStatement procedure, Array params ) throws SQLException {
		for ( int i = 0; i < params.size(); i++ ) {
			IStruct attr = ( IStruct ) params.get( i );
			if ( attr.containsKey( Key.type ) && attr.getAsString( Key.type ).toLowerCase().contains( "in" ) ) {
				procedure.setObject( i + 1, attr.get( Key.value ), QueryColumnType.fromString( attr.getAsString( Key.sqltype ) ).sqlType );
			}

			if ( attr.containsKey( Key.type ) && attr.getAsString( Key.type ).toLowerCase().contains( "out" ) ) {
				procedure.registerOutParameter( i + 1, QueryColumnType.fromString( attr.getAsString( Key.sqltype ) ).sqlType );
			}
		}
	}

	private String buildCallString( String procedureName, Array params ) {
		String paramString = params.stream().map( x -> "?" ).collect( Collectors.joining( ", " ) );
		// @TODO: Support returning a result set via the `{?= call ...}` syntax.
		// See https://docs.oracle.com/javase/8/docs/api/java/sql/CallableStatement.html
		return "{call " + procedureName + "(" + paramString + ")}";
	}

	private void putResultSetsInContext( IBoxContext context, CallableStatement procedure, Array procResults ) throws SQLException {
		if ( procResults.size() == 1 ) {
			IStruct resultSetAttr = ( IStruct ) procResults.get( 0 );

			ExpressionInterpreter.setVariable( context, resultSetAttr.getAsString( Key._name ),
			    ortus.boxlang.runtime.types.Query.fromResultSet( procedure.getResultSet(), IntegerCaster.cast( resultSetAttr.get( Key.maxRows ) ) ) );

			return;
		}

		validateProcResultResultSetAttribute( procResults );

		ResultSet	res		= procedure.getResultSet();
		int			index	= 1;

		while ( res != null ) {

			int					_index			= index;
			Optional<Object>	resultSetAttr	= procResults.stream().filter( ( pr ) -> {
													IStruct attr = ( IStruct ) pr;

													return attr.containsKey( Key.resultSet )
													    && IntegerCaster.cast( attr.get( Key.resultSet ) ) == _index;
												} )
			    .findFirst();

			ResultSet			currentRes		= res;
			resultSetAttr.ifPresent( ( attr ) -> {
				ExpressionInterpreter.setVariable( context, ( ( IStruct ) attr ).getAsString( Key._name ),
				    ortus.boxlang.runtime.types.Query.fromResultSet( currentRes, IntegerCaster.cast( ( ( IStruct ) attr ).get( Key.maxRows ) ) ) );
			} );

			if ( !procedure.getMoreResults() ) {
				break;
			}

			res = procedure.getResultSet();
			index++;
		}
	}

	private void validateProcResultResultSetAttribute( Array procResults ) {
		boolean allHaveIndex = procResults.stream().allMatch( ( pr ) -> {
			IStruct resultSetAttr = ( IStruct ) pr;

			return resultSetAttr.containsKey( Key.resultSet );
		} );

		if ( allHaveIndex ) {
			return;
		}

		throw new BoxRuntimeException( "If there is more than one ProcResult component they must all have a resultSet attribute" );
	}

	private void putOutVariablesInContext( IBoxContext context, CallableStatement procedure, Array params ) throws SQLException {
		for ( int i = 0; i < params.size(); i++ ) {
			IStruct attr = ( IStruct ) params.get( i );
			if ( attr.containsKey( Key.type ) && attr.getAsString( Key.type ).toLowerCase().contains( "out" ) ) {
				ExpressionInterpreter.setVariable( context, attr.getAsString( Key.variable ), procedure.getObject( i + 1 ) );
			}
		}
	}
}
