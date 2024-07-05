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
package ortus.boxlang.runtime.jdbc;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.config.segments.DatasourceConfig;
import ortus.boxlang.runtime.context.ApplicationBoxContext;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.CastAttempt;
import ortus.boxlang.runtime.dynamic.casters.StructCaster;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.services.DatasourceService;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.exceptions.DatabaseException;

/**
 * Manages the active JDBC Connection for the current request/thread/BoxLang context.
 *
 * Primrarily offers transactional context management by tracking whether the current context has an ongoing transaction and returning the appropriate
 * Connection object... However, this class also provides methods for retrieving a JDBC connection matching the datasource Key name or config Struct.
 */
public class ConnectionManager {

	/**
	 * --------------------------------------------------------------------------
	 * Private Properties
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Logger
	 */
	private static final Logger		logger				= LoggerFactory.getLogger( ConnectionManager.class );

	/**
	 * The active transaction (if any) for this request/thread/BoxLang context.
	 *
	 * @TODO: Consider converting this to a HashMap of transactions (using some unique key?) to allow us to track multiple (nested) transactions.
	 */
	private Transaction				transaction;

	/**
	 * The context this ConnectionManager is associated with.
	 */
	private IBoxContext				context;

	/**
	 * A default datasource, that can be set manully mostly for testing purpose mostly
	 */
	private DataSource				defaultDatasource	= null;

	/**
	 * A concurrent map of datasources registered with the manager.
	 */
	private Map<Key, DataSource>	datasources			= new ConcurrentHashMap<>();

	/**
	 * The DatasourceService instance
	 */
	private DatasourceService		datasourceService	= BoxRuntime.getInstance().getDataSourceService();

	/**
	 * --------------------------------------------------------------------------
	 * Constructor(s)
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Create a new ConnectionManager object.
	 *
	 * @param context The BoxLang context this ConnectionManager is associated with.
	 */
	public ConnectionManager( IBoxContext context ) {
		this.context = context;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Transaction Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Check if we are executing inside a transaction.
	 *
	 * @return true if this ConnectionManager object has a registered transaction, which only exists while a Transaction component is executing.
	 */
	public boolean isInTransaction() {
		return this.transaction != null;
	}

	/**
	 * Get the active transaction (if any) for this request/thread/BoxLang context.
	 *
	 * @throws DatabaseException if no transaction is found for this context.
	 *
	 * @return The BoxLang Transaction object, which manages an underlying JDBC Connection.
	 */
	public Transaction getTransaction() {
		return this.transaction;
	}

	/**
	 * Get the active transaction for this request/thread/BoxLang context, throwing a DatabaseException if no transaction is found.
	 *
	 * @throws DatabaseException if no transaction is found for this context.
	 *
	 * @return The BoxLang Transaction object, which manages an underlying JDBC Connection.
	 */
	public Transaction getTransactionOrThrow() {
		if ( !isInTransaction() ) {
			throw new DatabaseException( "Transaction is not started; Please place this method call inside a transaction{} block" );
		}
		return getTransaction();
	}

	/**
	 * Set the active transaction for this request/thread/BoxLang context.
	 */
	public ConnectionManager setTransaction( Transaction transaction ) {
		this.transaction = transaction;
		return this;
	}

	/**
	 * Set the active transaction for this request/thread/BoxLang context.
	 */
	public ConnectionManager endTransaction() {
		this.transaction = null;
		return this;
	}

	/**
	 * Get the active transaction (if any) for this request/thread/BoxLang context. If none is found, the provided datasource is used to create a new
	 * transaction which is then returned.
	 *
	 * @param datasource DataSource to use if creating a new transaction. Not currently used if a transaction already exists.
	 *
	 * @return The current executing transaction.
	 */
	public Transaction getOrSetTransaction( DataSource datasource ) {
		if ( isInTransaction() ) {
			return getTransaction();
		}
		return this.beginTransaction( datasource );
	}

	/**
	 * Create a new transaction and set it as the active transaction for this request/thread/BoxLang context.
	 *
	 * @throws DatabaseException if a transaction already exists for this context.
	 *
	 * @param datasource DataSource to use if creating a new transaction.
	 *
	 * @return The current executing transaction.
	 */
	public Transaction beginTransaction( DataSource datasource ) {
		if ( isInTransaction() ) {
			throw new DatabaseException(
			    "Transaction already exists for this BoxLang context; Either one has already started or the previous transaction did not shut down correctly." );
		}
		this.transaction = new Transaction( datasource );
		return this.transaction;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Connection Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Get a JDBC Connection to the specified datasource.
	 * <p>
	 * This method uses the following logic to pull the correct connection for the given query/context:
	 * <ol>
	 * <li>check for a transactional context.</li>
	 * <li>If an active transaction is found, this method compares the provided datasource against the transaction's datasource.</li>
	 * <li>If the datasources match, this method then checks the username/password authentication (if not null)</li>
	 * <li>if all those checks succeed, the transactional connection is returned.
	 * <li>if any of those checks fail, a new connection is returned from the provided datasource.</li>
	 * </ol>
	 *
	 * @param datasource The datasource to get a connection for.
	 * @param username   The username to use for authentication - will not check authentication if null.
	 * @param password   The password to use for authentication - will not check authentication if null.
	 *
	 * @return A JDBC Connection object, possibly from a transactional context.
	 */
	public Connection getConnection( DataSource datasource, String username, String password ) {
		if ( isInTransaction() ) {
			logger.atTrace()
			    .log( "Am inside transaction context; will check datasource and authentication to determine if we should return the transactional connection" );

			boolean isSameDatasource = getTransaction().getDataSource().equals( datasource );
			if ( isSameDatasource
			    && ( username == null || getTransaction().getDataSource().isAuthenticationMatch( username, password ) ) ) {
				logger.trace(
				    "Both the query datasource argument and authentication matches; proceeding with established transactional connection" );
				return getTransaction().getConnection();
			} else {
				// A different datasource was specified OR the authentication check failed; thus this is NOT a transactional query and we should use a new
				// connection.
				logger.atTrace()
				    .log( "Datasource OR authentication does not match transaction; Will ignore transaction context and return a new JDBC connection" );
				return datasource.getConnection( username, password );
			}
		}
		logger.trace( "Not within transaction; obtaining new connection from pool" );
		return datasource.getConnection( username, password );
	}

	/**
	 * Release a JDBC Connection back to the pool. Will not release transactional connections.
	 *
	 * @param connection The JDBC connection to release, acquired from ${@link #getConnection(DataSource)}
	 *
	 * @return True if the connection was successfully released, otherwise false.
	 */
	public boolean releaseConnection( Connection connection ) {
		if ( isInTransaction() ) {
			logger.atTrace()
			    .log( "Am inside transaction context; skipping connection release." );
			return false;
		}
		try {
			if ( connection == null || connection.isClosed() ) {
				logger.trace( "Connection is null or already closed; skipping connection release." );
				return false;
			}
			connection.close();
		} catch ( SQLException e ) {
			throw new BoxRuntimeException( "Error releasing connection: " + e.getMessage(), e );
		}
		return true;
	}

	/**
	 * Get a JDBC Connection to a specified datasource.
	 * <p>
	 * This method uses the following logic to pull the correct connection for the given query/context:
	 * <ol>
	 * <li>check for a transactional context.</li>
	 * <li>If an active transaction is found, this method compares the provided datasource against the transaction's datasource.</li>
	 * <li>If the datasources match, the transactional connection is returned.
	 * <li>if not, a new connection is returned from the provided datasource.</li>
	 * </ol>
	 *
	 * @param datasource The datasource to get a connection for.
	 *
	 * @return A JDBC Connection object, possibly from a transactional context.
	 */
	public Connection getConnection( DataSource datasource ) {
		if ( isInTransaction() ) {
			logger.atTrace()
			    .log( "Am inside transaction context; will check datasource to determine if we should return the transactional connection" );

			boolean isSameDatasource = getTransaction().getDataSource().equals( datasource );

			if ( isSameDatasource ) {
				logger.trace(
				    "The query datasource matches the transaction datasource; proceeding with established transactional connection" );
				return getTransaction().getConnection();
			} else {
				// A different datasource was specified OR the authentication check failed; thus this is NOT a transactional query and we should use a new
				// connection.
				logger.atTrace()
				    .log( "Datasource does not match transaction; Will ignore transaction context and return a new JDBC connection" );
				return datasource.getConnection();
			}
		}

		logger.trace( "Not within transaction; obtaining new connection from the datasource object" );
		return datasource.getConnection();
	}

	/**
	 * Get a connection for the provided QueryOptions.
	 *
	 * @return A connection to the configured datasource.
	 */
	public Connection getConnection( QueryOptions options ) {
		if ( options.wantsUsernameAndPassword() ) {
			return getConnection( getDataSource( options ), options.username, options.password );
		} else {
			return getConnection( getDataSource( options ) );
		}
	}

	/**
	 * Determines the datasource to use according to the options and/or BoxLang Defaults
	 */
	public DataSource getDataSource( QueryOptions options ) {
		if ( options.datasource != null ) {
			var						datasourceObject	= options.datasource;
			CastAttempt<IStruct>	datasourceAsStruct	= StructCaster.attempt( datasourceObject );

			// ON THE FLY DATASOURCE
			if ( datasourceAsStruct.wasSuccessful() ) {
				return getOnTheFlyDataSource( datasourceAsStruct.get() );
			}
			// NAMED DATASOURCE
			else if ( datasourceObject instanceof String datasourceName ) {
				return getDatasourceOrThrow( Key.of( datasourceName ) );
			}
			// INVALID DATASOURCE
			throw new BoxRuntimeException( "Invalid datasource type: " + datasourceObject.getClass().getName() );
		}
		return getDefaultDatasourceOrThrow();
	}

	/**
	 * --------------------------------------------------------------------------
	 * Datasource Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Get the default datasource for the application.
	 *
	 * We check the application settings for a default datasource, and if it exists, we return it.
	 * Else, we check the runtime settings for a default datasource, and if it exists, we return it.
	 * Else, we return an empty string
	 *
	 * @return The default datasource object, if found, or null if not found.
	 */
	public DataSource getDefaultDatasource() {

		// short circuit if we have a default datasource
		if ( this.defaultDatasource != null ) {
			return this.defaultDatasource;
		}

		// Discover the datasource name from the settings
		String	defaultDSN			= ( String ) this.context.getConfigItems( Key.runtime, Key.defaultDatasource );
		Key		defaultDSNKey		= Key.of( defaultDSN );
		IStruct	configDatasources	= ( IStruct ) this.context.getConfigItems( Key.runtime, Key.datasources );

		// If the default name is empty or if the name doesn't exist in the datasources map, we return null
		if ( defaultDSN.isEmpty() || !configDatasources.containsKey( defaultDSNKey ) ) {
			return null;
		}

		// Get the datasource config and incorporate the application name
		IStruct				targetConfig	= configDatasources.getAsStruct( defaultDSNKey );

		// Build the DataSourceConfig object from the incoming struct of settings
		DatasourceConfig	dsn				= new DatasourceConfig( defaultDSNKey )
		    .process( targetConfig )
		    .withAppName( getApplicationName() );
		this.defaultDatasource = this.datasourceService.register( dsn );

		return this.defaultDatasource;
	}

	/**
	 * Get the default datasource for the application or throw an exception if not found.
	 *
	 * @return The default datasource object
	 */
	public DataSource getDefaultDatasourceOrThrow() {
		DataSource datasource = getDefaultDatasource();
		if ( datasource == null ) {
			throw new DatabaseException( "No default datasource defined in the application or globally or in the query options" );
		}
		return datasource;
	}

	/**
	 * Get a datasource by name. This method will
	 * first check the application datasources, and if not found, will check the global datasources.
	 *
	 * @param datasourceName The name of the datasource to retrieve
	 *
	 * @return The datasource object, or null if not found.
	 */
	public DataSource getDatasource( Key datasourceName ) {

		// Check in the local cache first
		DataSource target = this.datasources.get( datasourceName );
		if ( target != null ) {
			return target;
		}

		// Try to discover now: These come from the context, so overrides are already applied
		IStruct configDatasources = this.context.getConfig()
		    .getAsStruct( Key.runtime )
		    .getAsStruct( Key.datasources );

		// If the name doesn't exist in the datasources map, we return null
		if ( !configDatasources.containsKey( datasourceName ) ) {
			return null;
		}

		// Build out the config from the struct first.
		DatasourceConfig	dsnConfig	= new DatasourceConfig( datasourceName )
		    .process( configDatasources.getAsStruct( datasourceName ) )
		    .withAppName( getApplicationName() );
		// Register the datasource
		DataSource			dsn			= this.datasourceService.register( dsnConfig );
		// Cache it
		this.datasources.put( datasourceName, dsn );

		return dsn;
	}

	/**
	 * Register a datasource with the connection manager.
	 *
	 * @param target The datasource to register
	 *
	 * @return The datasource object
	 */
	public DataSource register( DataSource target ) {
		this.datasources.put( target.getUniqueName(), target );
		return target;
	}

	/**
	 * Register a datasource with the connection manager.
	 *
	 * @param datasourceName The name of the datasource to register
	 * @param properties     The datasource properties to use
	 *
	 * @return The datasource object
	 */
	public DataSource register( Key datasourceName, IStruct properties ) {
		DataSource target = this.datasourceService.register( new DatasourceConfig( datasourceName, properties ) );
		this.datasources.put( datasourceName, target );
		return target;
	}

	/**
	 * Get a datasource by name or throw an exception if not found.
	 *
	 * @param datasourceName The name of the datasource to retrieve
	 *
	 * @return The datasource object
	 */
	public DataSource getDatasourceOrThrow( Key datasourceName ) {
		DataSource datasource = getDatasource( datasourceName );
		if ( datasource == null ) {
			throw new DatabaseException(
			    "Datasource with name [" + datasourceName.getName() + "] not found in the application or globally"
			);
		}
		return datasource;
	}

	/**
	 * Get an on-the-fly datasource from a struct configuration.
	 *
	 * @param properties The datasource properties to declared for the on the fly datasource
	 *
	 * @return A new or already registered datasource
	 */
	public DataSource getOnTheFlyDataSource( IStruct properties ) {
		Key			datasourceName	= Key.of( "onthefly_" + properties.hashCode() );
		DataSource	target			= this.datasources.get( datasourceName );

		if ( target != null ) {
			return target;
		}

		// Build out the config
		DatasourceConfig config = new DatasourceConfig(
		    Key.of( datasourceName.getName() ),
		    properties
		)
		    .withAppName( getApplicationName() )
		    .setOnTheFly();

		// Register it
		target = this.datasourceService.register( config );
		this.datasources.put( datasourceName, target );

		return target;
	}

	/**
	 * Set the default datasource for this connection manager.
	 * Usually called by tests
	 *
	 * @param datasource The default datasource to set
	 *
	 * @return ConnectionManager
	 */
	public ConnectionManager setDefaultDatasource( DataSource datasource ) {
		this.defaultDatasource = datasource;
		return this;
	}

	/**
	 * Verifies if we have a default datasource or not
	 *
	 * @return true if we have a default datasource, false otherwise
	 */
	public boolean hasDefaultDatasource() {
		return this.defaultDatasource != null;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Local Datasources Cache Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * How many cached datasources do we have?
	 */
	public int getCachedDatasourcesCount() {
		return this.datasources.size();
	}

	/**
	 * Get an array of all cached datasources names
	 */
	public String[] getCachedDatasourcesNames() {
		return this.datasources.keySet()
		    .stream()
		    .map( Key::getName )
		    .sorted()
		    .toArray( String[]::new );
	}

	/**
	 * Do we have a datasource cached with the given name?
	 *
	 * @param datasourceName The name of the datasource to check for
	 *
	 * @return true if the datasource is cached, false otherwise
	 */
	public boolean hasCachedDatasource( Key datasourceName ) {
		return this.datasources.containsKey( datasourceName );
	}

	/**
	 * Get the cached datasources
	 */
	public Map<Key, DataSource> getCachedDatasources() {
		return this.datasources;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Life Cycle Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Shutdown the ConnectionManager and release any resources.
	 */
	public void shutdown() {
		this.datasources.clear();
	}

	/**
	 * Shutdown the ConnectionManager due to an exception
	 */
	public void shutdownExceptionally() {
		this.shutdown();
		// Anything else to do here?
	}

	/**
	 * --------------------------------------------------------------------------
	 * Private Helper Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Get the application name for this connection manager.
	 *
	 * @return The application name, or an empty key if not found.
	 */
	private Key getApplicationName() {
		var appContext = this.context.getParentOfType( ApplicationBoxContext.class );
		if ( appContext != null ) {
			return appContext.getApplication().getName();
		}
		return Key._EMPTY;
	}
}
