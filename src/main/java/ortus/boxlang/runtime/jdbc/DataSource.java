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
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.config.segments.DatasourceConfig;
import ortus.boxlang.runtime.context.IBoxContext;
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
	private HikariDataSource		hikariDataSource;

	/**
	 * The configuration object for this datasource.
	 */
	private final DatasourceConfig	configuration;

	/**
	 * The Hikari configuration object for this datasource.
	 */
	private final HikariConfig		hikariConfig;

	/**
	 * --------------------------------------------------------------------------
	 * Constructor(s)
	 * --------------------------------------------------------------------------
	 */
	/**
	 * Configure and initialize a new DataSource given a configuration struct.
	 * 
	 * Immediately begins connection pooling.
	 *
	 * @param config A struct of properties to configure the datasource. Hikari itself will require either `dataSourceClassName` or `jdbcUrl` to be
	 *               defined, and potentially `username` and `password` as well.
	 */
	public DataSource( DatasourceConfig config ) {
		this( config, true );
	}

	/**
	 * Configure and initialize a new DataSource given a configuration struct.
	 * 
	 * Optionally specify beginPooling=false to delay connection pooling until calling {@link #beginPooling()} manually when desired.
	 *
	 * @param config       A struct of properties to configure the datasource. Hikari itself will require either `dataSourceClassName` or `jdbcUrl` to be
	 *                     defined, and potentially `username` and `password` as well.
	 * 
	 * @param beginPooling Whether to begin connection pooling immediately.
	 */
	public DataSource( DatasourceConfig config, boolean beginPooling ) {
		IStruct eventParams = Struct.ofNonConcurrent(
		    Key._NAME, config.getUniqueName(),
		    Key.properties, config.properties,
		    Key.config, config
		);
		BoxRuntime.getInstance().getInterceptorService().announce(
		    BoxEvent.ON_DATASOURCE_STARTUP,
		    eventParams
		);
		// Retrieve and store the potentially modified configuration from the event.
		this.configuration = eventParams.getAs( DatasourceConfig.class, Key.of( "config" ) );

		// Warn if driver is not found in the datasource service
		this.configuration.validateDriver();

		this.hikariConfig = this.configuration.toHikariConfig();
		if ( beginPooling ) {
			beginPooling();
		}
	}

	/**
	 * Begin connection pooling for this datasource.
	 * 
	 * No-op if pooling is already started.
	 *
	 * @return This DataSource object, now with an active connection pool.
	 *
	 * @throws BoxRuntimeException if the connection pool could not be established.
	 */
	public DataSource beginPooling() {
		if ( isPoolingStarted() ) {
			return this;
		}
		try {
			this.hikariDataSource = new HikariDataSource( hikariConfig );
		} catch ( RuntimeException e ) {
			String message = String.format( "Unable to create datasource connection to URL [%s] : ", hikariConfig.getJdbcUrl() );
			throw new BoxRuntimeException( message + e.getMessage(), e );
		}
		return this;
	}

	/**
	 * Is connection pooling started for this datasource?
	 */
	public boolean isPoolingStarted() {
		return this.hikariDataSource != null && !this.hikariDataSource.isClosed();
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
	 * Will begin connection pooling automatically.
	 *
	 * @param name       The name of the datasource.
	 * @param properties A struct of properties to configure the datasource. Will likely be defined via <code>Application.bx</code> or a web admin.
	 *
	 * @return a DataSource object configured from the provided struct.
	 */
	public static DataSource fromStruct( Key name, IStruct properties ) {
		return new DataSource( new DatasourceConfig( name, properties ), true );
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
	public BoxConnection getBoxConnection() {
		try {
			return BoxConnection.of( this.hikariDataSource.getConnection(), this );
		} catch ( SQLException e ) {
			throw new DatabaseException( "Unable to open connection:", e );
		}
	}

	/**
	 * Get a connection to the configured datasource.
	 * 
	 * This method is deprecated. Use {@link #getBoxConnection()} instead.
	 *
	 * @return A JDBC connection to the configured datasource.
	 *
	 * @throws BoxRuntimeException if connection could not be established.
	 */
	@Deprecated
	public Connection getConnection() {
		return getBoxConnection();
	}

	/**
	 * Get an unpooled BoxConnection to the configured datasource.
	 *
	 * @return A JDBC connection to the configured datasource.
	 */
	protected BoxConnection getUnpooledBoxConnection() {
		try {
			return BoxConnection.of(
			    DriverManager.getConnection( this.hikariDataSource.getJdbcUrl(), this.hikariDataSource.getUsername(), this.hikariDataSource.getPassword() ),
			    this );
		} catch ( SQLException e ) {
			throw new DatabaseException( "Unable to open connection:", e );
		}
	}

	/**
	 * Get an unpooled connection to the configured datasource.
	 * 
	 * This method is deprecated. Use {@link #getUnpooledBoxConnection()} instead.
	 * 
	 * @return A JDBC connection to the configured datasource.
	 */
	@Deprecated
	protected Connection getUnpooledConnection() {
		return getUnpooledBoxConnection();
	}

	/**
	 * Get a connection to the configured datasource with the provided username and password.
	 * <p>
	 * <strong>Important:</strong> Some JDBC drivers do not support the pooled
	 * {@code getConnection(username, password)} method and will throw a
	 * {@link java.sql.SQLFeatureNotSupportedException}. In such cases, this method will
	 * automatically fall back to using {@link DriverManager} to create a direct connection with the
	 * provided credentials. This fallback connection will NOT be pooled and may impact performance
	 * if used frequently.
	 * <p>
	 * <strong>Known drivers that do NOT support this feature:</strong>
	 * <ul>
	 * <li>Oracle (ojdbc)</li>
	 * <li>Microsoft SQL Server (mssql-jdbc)</li>
	 * <li>IBM DB2</li>
	 * </ul>
	 * <p>
	 * <strong>Drivers that support this feature (but bypass pooling):</strong>
	 * <ul>
	 * <li>PostgreSQL (pgjdbc)</li>
	 * <li>MySQL (mysql-connector-j)</li>
	 * <li>MariaDB (mariadb-java-client)</li>
	 * <li>H2</li>
	 * </ul>
	 * <p>
	 * <strong>Best Practice:</strong> Define credentials in the datasource configuration rather than
	 * overriding them per-query to ensure connection pooling is used.
	 *
	 * @param username The username to use for authentication
	 * @param password The password to use for authentication
	 *
	 * @return A JDBC connection to the configured datasource.
	 *
	 * @throws DatabaseException if connection could not be established.
	 */
	public BoxConnection getBoxConnection( String username, String password ) {
		try {
			return BoxConnection.of( this.hikariDataSource.getConnection( username, password ), this );
		} catch ( java.sql.SQLFeatureNotSupportedException e ) {
			// Some JDBC drivers (Oracle, MS SQL Server, IBM DB2) don't support getConnection(user, pass) on pooled connections
			// Fall back to DriverManager for a direct, unpooled connection
			try {
				return BoxConnection.of( DriverManager.getConnection( this.hikariDataSource.getJdbcUrl(), username, password ), this );
			} catch ( SQLException fallbackException ) {
				throw new DatabaseException(
				    "Unable to open connection with provided credentials. Driver does not support pooled credential override and direct connection failed:",
				    fallbackException
				);
			}
		} catch ( SQLException e ) {
			throw new DatabaseException( "Unable to open connection:", e );
		}
	}

	/**
	 * Get a connection to the configured datasource with the provided username and password.
	 * 
	 * This method is deprecated. Use {@link #getBoxConnection(String, String)} instead.
	 * 
	 * <p>
	 * <strong>Important:</strong> Some JDBC drivers do not support the pooled
	 * {@code getConnection(username, password)} method and will throw a
	 * {@link java.sql.SQLFeatureNotSupportedException}. In such cases, this method will
	 * automatically fall back to using {@link DriverManager} to create a direct connection with the
	 * provided credentials. This fallback connection will NOT be pooled and may impact performance
	 * if used frequently.
	 * <p>
	 * <strong>Known drivers that do NOT support this feature:</strong>
	 * <ul>
	 * <li>Oracle (ojdbc)</li>
	 * <li>Microsoft SQL Server (mssql-jdbc)</li>
	 * <li>IBM DB2</li>
	 * </ul>
	 * <p>
	 * <strong>Drivers that support this feature (but bypass pooling):</strong>
	 * <ul>
	 * <li>PostgreSQL (pgjdbc)</li>
	 * <li>MySQL (mysql-connector-j)</li>
	 * <li>MariaDB (mariadb-java-client)</li>
	 * <li>H2</li>
	 * </ul>
	 * <p>
	 * <strong>Best Practice:</strong> Define credentials in the datasource configuration rather than
	 * overriding them per-query to ensure connection pooling is used.
	 *
	 * @param username The username to use for authentication
	 * @param password The password to use for authentication
	 *
	 * @return A JDBC connection to the configured datasource.
	 *
	 * @throws DatabaseException if connection could not be established.
	 */
	@Deprecated
	public Connection getConnection( String username, String password ) {
		return getBoxConnection( username, password );
	}

	/**
	 * Shut down the datasource, including the connection pool and all connections.
	 *
	 * @return This DataSource object, which is now shut down and useless for any further operations.
	 */
	public DataSource shutdown() {
		if ( isPoolingStarted() ) {
			this.hikariDataSource.close();
		}
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
		try ( BoxConnection conn = getBoxConnection() ) {
			return execute( query, conn, null );
		} catch ( SQLException e ) {
			throw new DatabaseException( e );
		}
	}

	/**
	 * Execute a query on the default connection, within the specific context.
	 *
	 * @param query   The SQL query to execute.
	 * @param context The boxlang context for localization. Useful for localization; i.e., queries with date or time values.
	 */
	public ExecutedQuery execute( String query, IBoxContext context ) {
		try ( BoxConnection conn = getBoxConnection() ) {
			return execute( query, conn, context );
		} catch ( SQLException e ) {
			throw new DatabaseException( e );
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
	public ExecutedQuery execute( String query, BoxConnection conn, IBoxContext context ) {
		PendingQuery pendingQuery = new PendingQuery( context, query, new ArrayList<>() );
		return pendingQuery.execute( conn, context );
	}

	/**
	 * Execute a query on the connection, using the provided connection.
	 * 
	 * @deprecated Use {@link #execute(String, BoxConnection, IBoxContext)} instead.
	 *             <p>
	 *             Note the connection passed in is NOT closed automatically. It is up to the caller to close the connection when they are done with it. If you want
	 *             an automanaged, i.e. autoclosed connection, use the <code>execute(String)</code> method.
	 *
	 * @param query The SQL query to execute.
	 * @param conn  The connection to execute the query on. A connection is required - use <code>execute(String)</code> if you don't wish to
	 *              provide one.
	 *
	 * @return An array of Structs, each representing a row of the result set (if any). If there are no results (say, for an UPDATE statement), an empty
	 *         array is returned.
	 */
	@Deprecated
	public ExecutedQuery execute( String query, Connection conn, IBoxContext context ) {
		return execute( query, BoxConnection.of( conn, this ), context );
	}

	/**
	 * Execute a query with a List of parameters on a given connection.
	 */
	public ExecutedQuery execute( String query, List<QueryParameter> parameters, BoxConnection conn, IBoxContext context ) {
		PendingQuery pendingQuery = new PendingQuery( context, query, parameters );
		return pendingQuery.execute( conn, context );
	}

	/**
	 * Execute a query with a List of parameters on a given connection.
	 * 
	 * @deprecated Use {@link #execute(String, List, BoxConnection, IBoxContext)} instead.
	 */
	@Deprecated
	public ExecutedQuery execute( String query, List<QueryParameter> parameters, Connection conn, IBoxContext context ) {
		return execute( query, parameters, BoxConnection.of( conn, this ), context );
	}

	/**
	 * Execute a query with a List of parameters on the default connection.
	 */
	public ExecutedQuery execute( String query, List<QueryParameter> parameters, IBoxContext context ) {
		try ( BoxConnection conn = getBoxConnection() ) {
			return execute( query, parameters, conn, context );
		} catch ( SQLException e ) {
			throw new DatabaseException( e );
		}
	}

	/**
	 * Execute a query with an array of parameters on a given connection.
	 */
	public ExecutedQuery execute( String query, Array parameters, BoxConnection conn, IBoxContext context ) {
		PendingQuery pendingQuery = new PendingQuery( context, query, parameters, new QueryOptions( new Struct() ) );
		return pendingQuery.execute( conn, context );
	}

	/**
	 * Execute a query with an array of parameters on a given connection.
	 * 
	 * @deprecated Use {@link #execute(String, Array, BoxConnection, IBoxContext)} instead.
	 */
	@Deprecated
	public ExecutedQuery execute( String query, Array parameters, Connection conn, IBoxContext context ) {
		return execute( query, parameters, BoxConnection.of( conn, this ), context );
	}

	/**
	 * Execute a query with an array of parameters on the default connection.
	 */
	public ExecutedQuery execute( String query, Array parameters, IBoxContext context ) {
		try ( BoxConnection conn = getBoxConnection() ) {
			return execute( query, parameters, conn, context );
		} catch ( SQLException e ) {
			throw new DatabaseException( e );
		}
	}

	/**
	 * Execute a query with a struct of parameters on a given connection.
	 */
	public ExecutedQuery execute( String query, IStruct parameters, BoxConnection conn, IBoxContext context ) {
		PendingQuery pendingQuery = new PendingQuery( context, query, parameters, new QueryOptions( new Struct() ) );
		return pendingQuery.execute( conn, context );
	}

	/**
	 * Execute a query with a struct of parameters on a given connection.
	 * 
	 * @deprecated Use {@link #execute(String, IStruct, BoxConnection, IBoxContext)} instead.
	 */
	@Deprecated
	public ExecutedQuery execute( String query, IStruct parameters, Connection conn, IBoxContext context ) {
		return execute( query, parameters, BoxConnection.of( conn, this ), context );
	}

	/**
	 * Execute a query with a struct of parameters on the default connection.
	 */
	public ExecutedQuery execute( String query, IStruct parameters, IBoxContext context ) {
		try ( BoxConnection conn = getBoxConnection() ) {
			return execute( query, parameters, conn, context );
		} catch ( SQLException e ) {
			throw new DatabaseException( e );
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
