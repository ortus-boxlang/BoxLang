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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import ortus.boxlang.runtime.components.Attribute;
import ortus.boxlang.runtime.components.BoxComponent;
import ortus.boxlang.runtime.components.Component;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.IJDBCCapableContext;
import ortus.boxlang.runtime.dynamic.ExpressionInterpreter;
import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.dynamic.casters.IntegerCaster;
import ortus.boxlang.runtime.jdbc.BoxCallableStatement;
import ortus.boxlang.runtime.jdbc.BoxConnection;
import ortus.boxlang.runtime.jdbc.ConnectionManager;
import ortus.boxlang.runtime.jdbc.QueryOptions;
import ortus.boxlang.runtime.jdbc.drivers.IJDBCDriver;
import ortus.boxlang.runtime.jdbc.drivers.JDBCDriverFeature;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Query;
import ortus.boxlang.runtime.types.QueryColumnType;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.exceptions.DatabaseException;
import ortus.boxlang.runtime.validation.Validator;

@BoxComponent( description = "Execute stored procedures with parameters and result capture", requiresBody = true )
public class StoredProc extends Component {

	/**
	 * Constructor
	 */
	public StoredProc() {
		super();
		declaredAttributes = new Attribute[] {
		    new Attribute( Key.procedure, "string", Set.of( Validator.REQUIRED, Validator.NON_EMPTY ) ),
		    new Attribute( Key.datasource, "string" ),
		    new Attribute( Key.blockfactor, "integer", Set.of( Validator.NOT_IMPLEMENTED ) ),
		    new Attribute( Key.debug, "boolean", false ),
		    new Attribute( Key.returnCode, "boolean", false ),
		    new Attribute( Key.result, "string", "bxstoredproc" ),
		    new Attribute( Key.username, "string" ),
		    new Attribute( Key.password, "string" ),
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
	 * @attribute.blockfactor The fetch size to use for batching rows and reducing network round trips when reading results.
	 *
	 * @attribute.debug If enabled, list debugging info on each statement.
	 *
	 * @attribute.returnCode True/false whether to capture the return code of the stored procedure in the result variable.
	 *
	 * @attribute.result The name of the variable to store the result set in.
	 *
	 */
	public BodyResult _invoke( IBoxContext context, IStruct attributes, ComponentBody body, IStruct executionState ) {
		IJDBCCapableContext	jdbcContext			= context.getParentOfType( IJDBCCapableContext.class );
		ConnectionManager	connectionManager	= jdbcContext.getConnectionManager();
		QueryOptions		options				= new QueryOptions( attributes );

		Array				params				= new Array();
		Array				procResults			= new Array();
		int					returnCode			= 0;
		int					executionTimeMS		= 0;
		String				resultVarName		= attributes.getAsString( Key.result );
		boolean				debug				= attributes.getAsBoolean( Key.debug );
		String				procedureName		= attributes.getAsString( Key.procedure );

		// these are placed in the execution state to allow the ProcParam and ProcResult components to register themselves with this component
		executionState.put( Key.queryParams, params );
		executionState.put( Key.procResult, procResults );

		BodyResult bodyResult = processBody( context, body, null );

		// If there was a return statement inside our body, we early exit now
		if ( bodyResult.isEarlyExit() ) {
			return bodyResult;
		}

		BoxConnection	conn	= connectionManager.getBoxConnection( options );
		IJDBCDriver		driver;
		boolean			hasReturnCode;
		String			callString;
		try {
			driver			= conn.getDataSource().getConfiguration().getDriver();
			// TODO: If the stored proc call requests a return code but the driver doesn't support it,
			// do we error or just ignore the request? Right now we are ignoring.
			hasReturnCode	= attributes.getAsBoolean( Key.returnCode )
			    && driver.hasFeature( JDBCDriverFeature.SUPPORTS_STORED_PROC_RETURN_CODE );

			// Give the driver a chance to pre-process the call (e.g., Oracle needs to swap proc results with proc params)
			driver.preProcessProcCall( conn, procedureName, params, procResults, context, debug );

			callString = buildCallString( procedureName, params, hasReturnCode, debug, driver );
		} catch ( SQLException e ) {
			try {
				conn.close();
			} catch ( SQLException e2 ) {
				throw new DatabaseException( e2 );
			}
			throw new DatabaseException( e );
		}
		try (

		    BoxCallableStatement procedure = conn.prepareCall( callString ); ) {

			// Register return code parameter if needed
			int paramOffset = 0;
			if ( hasReturnCode ) {
				procedure.registerOutParameter( 1, java.sql.Types.INTEGER );
				paramOffset = 1;
			}

			registerProcedureParams( procedure, params, paramOffset, debug, procedureName, context );

			if ( options.maxRows > 0 ) {
				procedure.setLargeMaxRows( options.maxRows );
			}

			int		startMS		= ( int ) System.currentTimeMillis();
			boolean	hasResult	= procedure.execute();
			executionTimeMS = ( int ) System.currentTimeMillis() - startMS;

			// MUST get result sets before getting return code or out parameters
			putResultSetsInContext( context, procedure, procResults, hasResult );

			// Capture return code if requested. Return code is just the first out parameter
			if ( hasReturnCode ) {
				returnCode = procedure.getInt( 1 );

			}

			// capture out and inout parameters
			putOutVariablesInContext( context, procedure, params, paramOffset, driver );

			// Create result struct. Is there anything else to put in here? Update count? Generated key?
			ExpressionInterpreter.setVariable(
			    context,
			    resultVarName,
			    Struct.of(
			        // TODO: remove returnCode in 2.x
			        Key.returnCode, returnCode,
			        Key.statusCode, returnCode,
			        Key.executionTime, executionTimeMS
			    )
			);
			procedure.close();

		} catch ( SQLException e ) {
			throw new DatabaseException( e );
		} finally {
			try {
				conn.close();
			} catch ( SQLException e ) {
				throw new DatabaseException( e );
			}
		}
		return DEFAULT_RETURN;
	}

	/**
	 * Build a call string for the stored procedure, including input parameters and optional return code.
	 *
	 * @param procedureName The name of the stored procedure.
	 * @param params        The parameters to the stored procedure.
	 * @param hasReturnCode Whether to include return code parameter
	 * @param debug         Whether to output debug info
	 */
	private String buildCallString( String procedureName, Array params, boolean hasReturnCode, boolean debug, IJDBCDriver driver ) {
		boolean			hasPositional	= false;
		boolean			hasNamed		= false;
		boolean			first			= true;
		StringBuilder	paramString		= new StringBuilder();
		for ( int i = 0; i < params.size(); i++ ) {
			if ( !first ) {
				paramString.append( ", " );
			} else {
				first = false;
			}
			IStruct	attr	= ( IStruct ) params.get( i );
			String	varName	= null;
			if ( attr.containsKey( Key.DBVarName ) && attr.getAsString( Key.DBVarName ) != null && !attr.getAsString( Key.DBVarName ).isEmpty() ) {
				varName = attr.getAsString( Key.DBVarName );
				if ( hasPositional ) {
					throw new BoxRuntimeException(
					    "Cannot mix positional and named parameters in a stored procedure call. Named parameter: '" + varName + "' at index " + ( i + 1 ) );
				}
				hasNamed = true;
				// Different drivers handle named parameters differently, if at all
				driver.emitStoredProcNamedParam( paramString, varName );
			} else {
				if ( hasNamed ) {
					throw new BoxRuntimeException(
					    "Cannot mix positional and named parameters in a stored procedure call. Positional parameter at index " + ( i + 1 )
					);
				}
				hasPositional = true;
				paramString.append( "?" );
			}
		}

		if ( hasReturnCode ) {
			if ( debug ) {
				System.out.println( "{? = call " + procedureName + "(" + paramString.toString() + ")}" );
			}
			// Use {? = call ...} syntax for procedures that return a value
			return "{? = call " + procedureName + "(" + paramString.toString() + ")}";
		} else {
			// Use standard {call ...} syntax
			if ( debug ) {
				System.out.println( "{call " + procedureName + "(" + paramString.toString() + ")}" );
			}
			return "{call " + procedureName + "(" + paramString.toString() + ")}";
		}
	}

	/**
	 * Attach the IN and OUT parameters to the callable statement.
	 *
	 * @param procedure     The callable statement.
	 * @param params        The parameters to the stored procedure.
	 * @param paramOffset   Offset for parameter positions (1 if return code is present, 0 otherwise)
	 * @param debug         Whether to output debug info
	 * @param procedureName The name of the stored procedure, for debug output
	 */
	private void registerProcedureParams( BoxCallableStatement procedure, Array params, int paramOffset, boolean debug, String procedureName,
	    IBoxContext context )
	    throws SQLException {
		for ( int i = 0; i < params.size(); i++ ) {
			IStruct	attr	= ( IStruct ) params.get( i );
			String	varType	= "";
			if ( attr.containsKey( Key.type ) ) {
				varType = attr.getAsString( Key.type ).toLowerCase();
			}
			QueryColumnType	queryType	= QueryColumnType.fromString( attr.getAsString( Key.sqltype ) );
			int				sqlType		= queryType.sqlType;
			Object			value		= attr.get( Key.value );
			if ( varType.contains( "in" ) ) {
				if ( debug ) {
					String paramName = attr.getAsString( Key.DBVarName );
					if ( paramName != null ) {
						paramName = "[" + paramName + "] ";
					} else {
						paramName = "";
					}
					QueryColumnType typeInfo = QueryColumnType.fromSQLType( sqlType );
					System.out.println(
					    "Procedure [" + procedureName + "] Setting IN param " + paramName +
					        "in position " + ( i + 1 + paramOffset ) +
					        " (type: " + typeInfo + ")"
					);
				}
				if ( BooleanCaster.cast( attr.getOrDefault( Key.nulls, false ) ) ) {
					value = null;
				} else {
					value = QueryColumnType.toSQLType( queryType, value, context, procedure.getConnection() );
				}
				procedure.setObject( i + 1 + paramOffset, value, sqlType );
			}
			if ( varType.contains( "out" ) ) {
				if ( debug ) {
					String paramName = attr.getAsString( Key.DBVarName );
					if ( paramName != null ) {
						paramName = "[" + paramName + "] ";
					} else {
						paramName = "";
					}
					QueryColumnType typeInfo = QueryColumnType.fromSQLType( sqlType );
					System.out.println( "Procedure [" + procedureName + "] Setting OUT param " + paramName + "in position "
					    + ( i + 1 + paramOffset ) + " (type: " + typeInfo + ")" );
				}
				procedure.registerOutParameter( i + 1 + paramOffset, sqlType );
			}
		}
	}

	/**
	 * Read the stored procedure ResultSet and copy them into the Boxlang script context variable scope.
	 *
	 * @param context     The Boxlang script context.
	 * @param procedure   The callable statement.
	 * @param procResults The stored procedure results - It's an array because there can be multiple ResultSets.
	 */
	private void putResultSetsInContext( IBoxContext context, BoxCallableStatement procedure, Array procResults, boolean hasResult ) throws SQLException {
		var	indexedProcResults	= processProcResults( procResults );

		int	resultSetIndex		= 1;
		int	updateCount			= 0;

		while ( true ) {
			if ( hasResult ) {
				IStruct matchingProcResult = indexedProcResults.get( resultSetIndex );
				// Is there a procResult definition for this result set?
				// If not, we just skip it.
				if ( matchingProcResult != null ) {
					// Name is required in the procResult component, so safe to assume it's there
					String		resultSetAttr	= matchingProcResult.getAsString( Key._NAME );

					ResultSet	res				= procedure.getResultSet();
					ExpressionInterpreter.setVariable( context, resultSetAttr,
					    Query.fromResultSet( procedure, res, matchingProcResult.getAsInteger( Key.maxRows ) ) );
					res.close();
				}
				resultSetIndex++;
			} else {
				updateCount = procedure.getUpdateCount();
				if ( updateCount > -1 ) {
					// Not doing anything with this yet
				}
			}

			if ( !hasResult && updateCount == -1 ) {
				break;
			}

			hasResult = procedure.getMoreResults();
		}

		// Should we throw an error if there were more procResult components than actual result sets returned?
		// CF appears to ignore unused proc results, which is probably the more useful behavior since a given proc can have dynamic logic inside
	}

	/**
	 * Validate that all ProcResult components either have a resultSet attribute, or don't. Positional vs indexed.
	 * Throw an exception if there is a mix of both.
	 *
	 * Return a map of resultSet index to ProcResult attribute struct. If they were positional, then assign them indexes starting at 1.
	 * If they were indexed, then use the provided index. There may be gaps in the indexes. If more than one procresult used the same index, the last one wins (overwrite)
	 *
	 * @param procResults The array of proc result definitions
	 *
	 * @return Map of resultSet index to ProcResult attribute struct.
	 */
	private Map<Integer, IStruct> processProcResults( Array procResults ) {
		boolean					hasPositional	= false;
		boolean					hasIndexed		= false;
		Map<Integer, IStruct>	resultMap		= new HashMap<>();
		for ( int i = 0; i < procResults.size(); i++ ) {
			IStruct	attr		= ( IStruct ) procResults.get( i );
			boolean	thisIndexed	= attr.containsKey( Key.resultSet );
			if ( thisIndexed ) {
				hasIndexed = true;
				resultMap.put( IntegerCaster.cast( attr.get( Key.resultSet ) ), attr );
			} else {
				hasPositional = true;
				resultMap.put( i + 1, attr );
			}
			if ( hasPositional && hasIndexed ) {
				// World's best error message. So descriptive!
				throw new BoxRuntimeException( "Cannot mix positional and indexed ProcResult components in a StoredProc. "
				    + " ProcParm [" + attr.getAsString( Key._name ) + "] is " + ( thisIndexed ? "indexed" : "positional" ) + ","
				    + " but the " + ( resultMap.size() - 1 ) + " ProcResult component(s) above it are " + ( thisIndexed ? "positional" : "indexed" ) + "." );
			}
		}
		return resultMap;
	}

	/**
	 * Read the stored procedure OUT parameters and copy them into the BoxLang script context variable scope.
	 *
	 * @param context     The BoxLang script context.
	 * @param procedure   The executed procedure CallableStatement.
	 * @param params      The stored procedure parameters.
	 * @param paramOffset Offset for parameter positions (1 if return code is present, 0 otherwise)
	 */
	private void putOutVariablesInContext( IBoxContext context, BoxCallableStatement procedure, Array params, int paramOffset, IJDBCDriver driver )
	    throws SQLException {
		for ( int i = 0; i < params.size(); i++ ) {
			IStruct attr = ( IStruct ) params.get( i );
			if ( attr.containsKey( Key.type )
			    && attr.getAsString( Key.type ).toLowerCase().contains( "out" )
			    && attr.containsKey( Key.variable )
			    && attr.getAsString( Key.variable ) != null && !attr.getAsString( Key.variable ).isEmpty() ) {

				// Get the out sql type, default to OBJECT if not specified
				QueryColumnType BLType = QueryColumnType.fromString( ( String ) attr.getOrDefault( Key.sqltype, "OBJECT" ) );

				// Get the value from the procedure, transform it if needed
				Object value = driver.transformValue(
				    BLType.sqlType,
				    procedure.getObject( i + 1 + paramOffset ),
				    procedure
				);

				// Set the variable in the context
				ExpressionInterpreter.setVariable( context, attr.getAsString( Key.variable ), value );
			}
		}
	}
}
