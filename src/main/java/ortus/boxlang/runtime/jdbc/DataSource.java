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
import java.util.ArrayList;
import java.util.List;

import com.zaxxer.hikari.HikariDataSource;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.config.segments.DatasourceConfig;
import ortus.boxlang.runtime.events.BoxEvent;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.exceptions.DatabaseException;

/**
 * Encapsulates a datasource configuration and connection pool, providing methods for executing queries (transactionally or single) on the datasource.
 */
public class DataSource implements Comparable<DataSource> {

	/**
	 * --------------------------------------------------------------------------
	 * Private Properties
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Underlying HikariDataSource object, used in connection pooling.
	 */
	private final HikariDataSource	hikariDataSource;

	/**
	 * The configuration object for this datasource.
	 */
	private final DatasourceConfig	configuration;

	/**
	 * --------------------------------------------------------------------------
	 * Constructor(s)
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Configure and initialize a new DataSourceRecord object from a struct of properties.
	 *
	 * @param config A struct of properties to configure the datasource. Hikari itself will require either `dataSourceClassName` or `jdbcUrl` to be
	 *               defined, and potentially `username` and `password` as well.
	 */
	public DataSource( DatasourceConfig config ) {
		IStruct eventParams = Struct.of(
		    "name", config.getUniqueName(),
		    "properties", config.properties,
		    "config", config
		);
		BoxRuntime.getInstance().getInterceptorService().announce(
		    BoxEvent.ON_DATASOURCE_STARTUP,
		    eventParams
		);
		// Retrieve and store the potentially modified configuration from the event.
		this.configuration = eventParams.getAs( DatasourceConfig.class, Key.of( "config" ) );
		try {
			this.hikariDataSource = new HikariDataSource( this.configuration.toHikariConfig() );
		} catch ( RuntimeException e ) {
			throw new BoxRuntimeException( "Unable to create datasource connection: " + e.getMessage(), e );
		}
	}

	/**
	 * Helper builder to build out a new DataSource object from a struct of properties and a name.
	 *
	 * @param name       The string name of the datasource.
	 * @param properties A struct of properties to configure the datasource. Will likely be defined via <code>Application.bx</code> or a web admin.
	 *
	 * @return a DataSource object configured from the provided struct.
	 */
	public static DataSource fromStruct( String name, IStruct properties ) {
		return fromStruct( Key.of( name ), properties );
	}

	/**
	 * Helper builder to build out a new DataSource object from a struct of properties and a name.
	 *
	 * @param name       The name of the datasource.
	 * @param properties A struct of properties to configure the datasource. Will likely be defined via <code>Application.bx</code> or a web admin.
	 *
	 * @return a DataSource object configured from the provided struct.
	 */
	public static DataSource fromStruct( Key name, IStruct properties ) {
		return new DataSource( new DatasourceConfig( name, properties ) );
	}

	/**
	 * --------------------------------------------------------------------------
	 * Utility Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Get the original name of the datasource - this is NOT unique and should not be used for identification.
	 *
	 * @return The original name of the datasource.
	 */
	public String getOriginalName() {
		return this.configuration.getOriginalName();
	}

	/**
	 * Get a unique datasource name which includes a hash of the properties
	 * Following the pattern: <code>bx_{name}_{properties_hash}</code>
	 */
	public Key getUniqueName() {
		return this.configuration.getUniqueName();
	}

	/**
	 * Are we an on the fly datasource?
	 */
	public Boolean isOnTheFly() {
		return this.configuration.isOnTheFly();
	}

	/**
	 * Get's the hashcode according to the datasource's unique name
	 *
	 * @return the hashcode
	 */
	@Override
	public int hashCode() {
		return getUniqueName().hashCode();
	}

	/**
	 * Verifies equality between two Datasource objects
	 *
	 * @param obj The other object to compare
	 */
	@Override
	public boolean equals( Object obj ) {
		if ( obj == this ) {
			return true;
		}
		if ( obj == null || obj.getClass() != this.getClass() ) {
			return false;
		}

		DataSource other = ( DataSource ) obj;
		return this.getUniqueName().equals( other.getUniqueName() );
	}

	/**
	 * Compares two DataSource objects
	 *
	 * @param otherConfig The other DataSource object to compare
	 *
	 * @return A negative integer, zero, or a positive integer as this object is less than, equal to, or greater than the specified object.
	 */
	@Override
	public int compareTo( DataSource otherConfig ) {
		return this.getUniqueName().compareTo( otherConfig.getUniqueName() );
	}

	/**
	 * Get the configuration object for this datasource.
	 *
	 * @return The configuration object for this datasource.
	 */
	public DatasourceConfig getConfiguration() {
		return this.configuration;
	}

	/**
	 * Get a connection to the configured datasource.
	 *
	 * @return A JDBC connection to the configured datasource.
	 *
	 * @throws BoxRuntimeException if connection could not be established.
	 */
	public Connection getConnection() {
		try {
			return this.hikariDataSource.getConnection();
		} catch ( SQLException e ) {
			// @TODO: Recast as BoxSQLException?
			throw new BoxRuntimeException( "Unable to open connection:", e );
		}
	}

	/**
	 * Get a connection to the configured datasource with the provided username and password.
	 *
	 * @return A JDBC connection to the configured datasource.
	 *
	 * @throws BoxRuntimeException if connection could not be established.
	 */
	public Connection getConnection( String username, String password ) {
		try {
			return this.hikariDataSource.getConnection( username, password );
		} catch ( SQLException e ) {
			// @TODO: Recast as BoxSQLException?
			throw new BoxRuntimeException( "Unable to open connection:", e );
		}
	}

	/**
	 * Shut down the datasource, including the connection pool and all connections.
	 *
	 * @return This DataSource object, which is now shut down and useless for any further operations.
	 */
	public DataSource shutdown() {
		this.hikariDataSource.close();
		return this;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Execute Query Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Execute a query on the default connection.
	 *
	 * @param query The SQL query to execute.
	 *
	 * @return An array of Structs, each representing a row of the result set (if any). If there are no results (say, for an UPDATE statement), an empty
	 *         array is returned.
	 */
	public ExecutedQuery execute( String query ) {
		try ( Connection conn = getConnection() ) {
			return execute( query, conn );
		} catch ( SQLException e ) {
			throw new DatabaseException( e.getMessage(), e );
		}
	}

	/**
	 * Execute a query on the connection, using the provided connection.
	 * <p>
	 * Note the connection passed in is NOT closed automatically. It is up to the caller to close the connection when they are done with it. If you want
	 * an automanaged, i.e. autoclosed connection, use the <code>execute(String)</code> method.
	 *
	 * @param query The SQL query to execute.
	 * @param conn  The connection to execute the query on. A connection is required - use <code>execute(String)</code> if you don't wish to
	 *              provide one.
	 *
	 * @return An array of Structs, each representing a row of the result set (if any). If there are no results (say, for an UPDATE statement), an empty
	 *         array is returned.
	 */
	public ExecutedQuery execute( String query, Connection conn ) {
		PendingQuery pendingQuery = new PendingQuery( query, new ArrayList<>() );
		return pendingQuery.execute( conn );
	}

	/**
	 * Execute a query with a List of parameters on a given connection.
	 */
	public ExecutedQuery execute( String query, List<QueryParameter> parameters, Connection conn ) {
		PendingQuery pendingQuery = new PendingQuery( query, parameters );
		return pendingQuery.execute( conn );
	}

	/**
	 * Execute a query with a List of parameters on the default connection.
	 */
	public ExecutedQuery execute( String query, List<QueryParameter> parameters ) {
		try ( Connection conn = getConnection() ) {
			return execute( query, parameters, conn );
		} catch ( SQLException e ) {
			throw new DatabaseException( e.getMessage(), e );
		}
	}

	/**
	 * Execute a query with an array of parameters on a given connection.
	 */
	public ExecutedQuery execute( String query, Array parameters, Connection conn ) {
		PendingQuery pendingQuery = new PendingQuery( query, parameters, new QueryOptions( new Struct() ) );
		return pendingQuery.execute( conn );
	}

	/**
	 * Execute a query with an array of parameters on the default connection.
	 */
	public ExecutedQuery execute( String query, Array parameters ) {
		try ( Connection conn = getConnection() ) {
			return execute( query, parameters, conn );
		} catch ( SQLException e ) {
			throw new DatabaseException( e.getMessage(), e );
		}
	}

	/**
	 * Execute a query with a struct of parameters on a given connection.
	 */
	public ExecutedQuery execute( String query, IStruct parameters, Connection conn ) {
		PendingQuery pendingQuery = new PendingQuery( query, parameters, new QueryOptions( new Struct() ) );
		return pendingQuery.execute( conn );
	}

	/**
	 * Execute a query with a struct of parameters on the default connection.
	 */
	public ExecutedQuery execute( String query, IStruct parameters ) {
		try ( Connection conn = getConnection() ) {
			return execute( query, parameters, conn );
		} catch ( SQLException e ) {
			throw new DatabaseException( e.getMessage(), e );
		}
	}

	/**
	 *
	 * Check the provided username and password against the current datasource credentials.
	 * <p>
	 * For obvious reasons, the string comparison is case-sensitive.
	 *
	 * @param username Username to check against the established datasource
	 * @param password Password to check against the established datasource
	 *
	 * @return True if the username and password match.
	 */
	public Boolean isAuthenticationMatch( String username, String password ) {
		return this.hikariDataSource.getUsername().equals( username ) && hikariDataSource.getPassword().equals( password );
	}

	/**
	 * Allow access to the underlying HikariDataSource object.
	 *
	 * @return The HikariDataSource object.
	 */
	public HikariDataSource getHikariDataSource() {
		return this.hikariDataSource;
	}

	/**
	 * Get the current pool statistics for the datasource.
	 *
	 * @return A struct containing the current pool statistics, including active connections, idle connections, and total connections.
	 */
	public IStruct getPoolStats() {
		var pool = this.hikariDataSource.getHikariPoolMXBean();
		return Struct.of(
		    "pendingThreads", pool.getThreadsAwaitingConnection(),
		    "idleConnections", pool.getIdleConnections(),
		    "totalConnections", pool.getTotalConnections(),
		    "activeConnections", pool.getActiveConnections(),
		    "maxConnections", hikariDataSource.getMaximumPoolSize(),
		    "minConnections", hikariDataSource.getMinimumIdle()
		);
	}
}
