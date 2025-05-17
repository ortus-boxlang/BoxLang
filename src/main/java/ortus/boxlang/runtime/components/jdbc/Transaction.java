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

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.components.Attribute;
import ortus.boxlang.runtime.components.BoxComponent;
import ortus.boxlang.runtime.components.Component;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.IJDBCCapableContext;
import ortus.boxlang.runtime.jdbc.ConnectionManager;
import ortus.boxlang.runtime.jdbc.DataSource;
import ortus.boxlang.runtime.jdbc.ITransaction;
import ortus.boxlang.runtime.logging.BoxLangLogger;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.exceptions.DatabaseException;
import ortus.boxlang.runtime.validation.Validator;

@BoxComponent( allowsBody = true )
public class Transaction extends Component {

	/**
	 * Logger
	 */
	private static final BoxLangLogger logger = BoxRuntime.getInstance().getLoggingService().DATASOURCE_LOGGER;

	/**
	 * Constructor
	 */
	public Transaction() {
		super();
		declaredAttributes = new Attribute[] {
		    new Attribute( Key.action, "string", "begin", Set.of(
		        Validator.valueOneOf(
		            "begin",
		            "commit",
		            "rollback",
		            "setsavepoint"
		        )
		    ) ),
		    new Attribute( Key.isolation, "string", Set.of(
		        Validator.valueOneOf(
		            "read_uncommitted",
		            "read_committed",
		            "repeatable_read",
		            "serializable"
		        )
		    ) ),
		    new Attribute( Key.savepoint, "string" ),
		    new Attribute( Key.nested, "boolean", false, Set.of(
		        Validator.TYPE
		    ) ),
		    new Attribute( Key.datasource, "string" )
		};
	}

	/**
	 * Demarcate or manage a JDBC transaction.
	 *
	 * @param context        The context in which the Component is being invoked
	 * @param attributes     The attributes to the Component
	 * @param body           The body of the Component
	 * @param executionState The execution state of the Component
	 *
	 * @attribute.action When used inside a transaction block, perform some action upon an existing transaction. One of: `begin`, `commit`, `rollback`, or `setsavepoint`.
	 *
	 * @attribute.isolation The isolation level to use for the transaction. Can only be set upon transaction begin. One of: `read_uncommitted`, `read_committed`, `repeatable_read`, or `serializable`.
	 *
	 * @attribute.savepoint The name of the savepoint to set or rollback to. Used with `savepoint` or `rollback` actions.
	 *
	 * @attribute.nested Whether or not this transaction is nested within another transaction. Default is `false`.
	 *
	 * @attribute.datasource The name of the datasource to use for the transaction. If not provided, the first query execution inside the transaction will set the datasource.
	 */
	public BodyResult _invoke( IBoxContext context, IStruct attributes, ComponentBody body, IStruct executionState ) {
		boolean				isTransactionBeginning	= attributes.getAsString( Key.action ).equals( "begin" ) || body != null;
		IJDBCCapableContext	jdbcContext				= context.getParentOfType( IJDBCCapableContext.class );
		ConnectionManager	connectionManager		= jdbcContext.getConnectionManager();
		ITransaction		transaction;

		if ( isTransactionBeginning ) {
			DataSource dataSource = attributes.containsKey( Key.datasource )
			    ? connectionManager.getDatasource( Key.of( attributes.getAsString( Key.datasource ) ) )
			    : connectionManager.getDefaultDatasource();

			if ( dataSource != null ) {
				// Just a note that we'll have to set the datasource on the transaction later.
			}

			transaction = connectionManager.beginTransaction( dataSource );
			if ( attributes.containsKey( Key.isolation ) ) {
				// isolation level is only set on the initial transaction start.
				transaction.setIsolationLevel( getIsolationLevel( attributes.getAsString( Key.isolation ) ) );
			}
		} else {
			transaction = connectionManager.getTransactionOrThrow();
		}

		if ( body == null ) {
			switch ( attributes.getAsString( Key.action ) ) {
				case "begin" :
					transaction.begin();
					break;
				case "end" :
					// notify the connection manager that we're no longer in a transaction. This calls transaction.end() internally.
					connectionManager.endTransaction();
					break;
				case "commit" :
					transaction.commit();
					break;
				case "rollback" :
					String savepoint = attributes.getAsString( Key.savepoint );
					if ( savepoint == null ) {
						transaction.rollback();
					} else {
						transaction.rollback( Key.of( savepoint ) );
					}
					break;
				case "setsavepoint" :
					transaction.setSavepoint( Key.of( attributes.getAsString( Key.savepoint ) ) );
					break;
				default :
					throw new BoxRuntimeException( "Unknown action: " + attributes.getAsString( Key.action ) );
			}
		} else {
			transaction.begin();
			BodyResult bodyResult = null;
			try {
				bodyResult = processBody( context, body );
				transaction.commit();
			} catch ( BoxRuntimeException e ) {
				logger.error( "Encountered runtime exception while processing transaction; rolling back", e );
				transaction.rollback();
				// if it is already a runtime exception throw it as-is
				throw e;
			} catch ( DatabaseException e ) {
				logger.error( "Encountered generic exception while processing transaction; rolling back", e );
				transaction.rollback();
				throw new DatabaseException( e.getMessage(), e );
			} catch ( Throwable e ) {
				logger.error( "Encountered database exception while processing transaction; rolling back", e );
				transaction.rollback();
				throw new BoxRuntimeException( e.getMessage(), e );
			} finally {
				// notify the connection manager that we're no longer in a transaction.
				connectionManager.endTransaction();
			}
			// Don't return until AFTER cleaning up the transaction. This resolves an issue in some CF engines where
			// the transaction is not properly closed if a return statement is encountered.
			return bodyResult == null ? DEFAULT_RETURN : bodyResult;
		}
		return DEFAULT_RETURN;
	}

	private int getIsolationLevel( String isolationLevel ) {
		switch ( isolationLevel.toLowerCase() ) {
			case "read_uncommitted" :
				return Connection.TRANSACTION_READ_UNCOMMITTED;
			case "read_committed" :
				return Connection.TRANSACTION_READ_COMMITTED;
			case "repeatable_read" :
				return Connection.TRANSACTION_REPEATABLE_READ;
			case "serializable" :
				return Connection.TRANSACTION_SERIALIZABLE;
			default :
				throw new BoxRuntimeException( "Unsupported isolation level: " + isolationLevel );
		}
	}
}
