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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ortus.boxlang.runtime.components.Attribute;
import ortus.boxlang.runtime.components.BoxComponent;
import ortus.boxlang.runtime.components.Component;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.IJDBCCapableContext;
import ortus.boxlang.runtime.jdbc.ConnectionManager;
import ortus.boxlang.runtime.jdbc.DataSource;
import ortus.boxlang.runtime.jdbc.DataSourceManager;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.exceptions.DatabaseException;
import ortus.boxlang.runtime.validation.Validator;

@BoxComponent( allowsBody = true )
public class Transaction extends Component {

	Logger log = LoggerFactory.getLogger( Transaction.class );

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
	 *
	 *
	 * @param context        The context in which the Component is being invoked
	 * @param attributes     The attributes to the Component
	 * @param body           The body of the Component
	 * @param executionState The execution state of the Component
	 *
	 */
	public BodyResult _invoke( IBoxContext context, IStruct attributes, ComponentBody body, IStruct executionState ) {
		IJDBCCapableContext						jdbcContext			= context.getParentOfType( IJDBCCapableContext.class );
		ConnectionManager						connectionManager	= jdbcContext.getConnectionManager();
		DataSourceManager						dataSourceManager	= context.getDataSourceManager();
		// DataSource dataSource = dataSourceManager.getDefaultDataSource();
		// @TODO: Add tests for the datasource attribute.
		DataSource								dataSource			= attributes.containsKey( Key.datasource )
		    ? dataSourceManager.getDataSource( Key.of( attributes.getAsString( Key.datasource ) ) )
		    : dataSourceManager.getDefaultDataSource();

		// @TODO: Add validation that the transaction has started before allowing any other actions.
		// i.e. if the connection manger has no transaction context, we can't commit, rollback, etc, and should throw an exception. There's no point acquiring
		// a connection or commiting a transaction if you haven't started one yet.
		ortus.boxlang.runtime.jdbc.Transaction	transaction			= connectionManager.getOrSetTransaction( dataSource );

		if ( attributes.containsKey( Key.isolation ) ) {
			transaction.setIsolationLevel( getIsolationLevel( attributes.getAsString( Key.isolation ) ) );
		}

		if ( body == null ) {
			switch ( attributes.getAsString( Key.action ) ) {
				case "begin" :
					transaction.begin();
					break;
				case "end" :
					transaction.end();
					break;
				case "commit" :
					transaction.commit();
					break;
				case "rollback" :
					transaction.rollback( attributes.getAsString( Key.savepoint ) );
					break;
				case "setsavepoint" :
					transaction.setSavepoint( attributes.getAsString( Key.savepoint ) );
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
			} catch ( DatabaseException e ) {
				log.error( "Encountered generic exception while processing transaction; rolling back", e );
				transaction.rollback();
				throw new DatabaseException( e.getMessage(), e );
			} catch ( Throwable e ) {
				log.error( "Encountered database exception while processing transaction; rolling back", e );
				transaction.rollback();
				throw new BoxRuntimeException( e.getMessage(), e );
			}
			transaction.end();
			// notify the connection manager that we're no longer in a transaction.
			// @TODO: Move this to the Transaction itself??? Or vice/versa, move the transaction.begin() and transaction.end() to the connection manager?
			connectionManager.endTransaction();
			// Don't return until AFTER cleaning up the transaction. This resolves an issue in some CF engines where
			// the transaction is not properly closed if a return statement is encountered.
			return bodyResult == null ? DEFAULT_RETURN : bodyResult;
		}
		return DEFAULT_RETURN;
	}

	private int getIsolationLevel( String isolationLevel ) {
		switch ( isolationLevel ) {
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
