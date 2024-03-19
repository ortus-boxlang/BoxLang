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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ortus.boxlang.runtime.components.Attribute;
import ortus.boxlang.runtime.components.BoxComponent;
import ortus.boxlang.runtime.components.Component;
import ortus.boxlang.runtime.context.ApplicationBoxContext;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.IJDBCCapableContext;
import ortus.boxlang.runtime.jdbc.ConnectionManager;
import ortus.boxlang.runtime.jdbc.DataSource;
import ortus.boxlang.runtime.jdbc.DataSourceManager;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
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
		    ) )
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
		/**
		 * @TODO: Shove all this boilerplate into a JDBC helper method
		 */
		ConnectionManager	connectionManager	= context.getParentOfType( IJDBCCapableContext.class ).getConnectionManager();

		DataSource			dataSource			= null;
		if ( !connectionManager.isInTransaction() ) {
			// @TODO: Switch to IHasDataSourceManager interface so we can potentially define datasources / datasource manger in more than just the
			// ApplicationBoxContext.
			DataSourceManager dataSourceManager = context.getParentOfType( ApplicationBoxContext.class ).getApplication().getDataSourceManager();
			// @TODO: Support a `datasource` attribute for named datasources.
			dataSource = dataSourceManager.getDefaultDataSource();
			connectionManager.setTransaction( new ortus.boxlang.runtime.jdbc.Transaction( dataSource, dataSource.getConnection() ) );
		}

		if ( body == null ) {
			switch ( attributes.getAsString( Key.action ) ) {
				case "begin" :
					connectionManager.getTransaction().begin();
					break;
				case "end" :
					connectionManager.getTransaction().end();
					break;
				case "commit" :
					connectionManager.getTransaction().commit();
					break;
				case "rollback" :
					connectionManager.getTransaction().rollback( attributes.getAsString( Key.savepoint ) );
					break;
				case "setsavepoint" :
					connectionManager.getTransaction().setSavepoint( attributes.getAsString( Key.savepoint ) );
					break;
				default :
					throw new BoxRuntimeException( "Unknown action: " + attributes.getAsString( Key.action ) );
			}
		} else {
			processBody( context, body );
			connectionManager.getTransaction().end();
			// notify the connection manager that we're no longer in a transaction.
			connectionManager.endTransaction();
		}
		return DEFAULT_RETURN;
	}
}
