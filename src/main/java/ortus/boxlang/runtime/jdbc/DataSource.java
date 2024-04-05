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
import java.util.Objects;

import com.zaxxer.hikari.HikariDataSource;

import ortus.boxlang.runtime.config.segments.DatasourceConfig;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.exceptions.DatabaseException;

/**
 * Encapsulates a datasource configuration and connection pool, providing methods for executing queries (transactionally or single) on the datasource.
 */
public class DataSource {

	/**
	 * Underlying HikariDataSource object, used in connection pooling.
	 */
	private final HikariDataSource	hikariDataSource;

	private final DatasourceConfig	configuration;

	/**
	 * Configure and initialize a new DataSourceRecord object from a struct of properties.
	 *
	 * @param config A struct of properties to configure the datasource. Hikari itself will require either `dataSourceClassName` or `jdbcUrl` to be
	 *               defined, and potentially `username` and `password` as well.
	 */
	public DataSource( DatasourceConfig config ) {
		this.configuration		= config;
		this.hikariDataSource	= new HikariDataSource( this.configuration.toHikariConfig() );
	}

	public DatasourceConfig getConfiguration() {
		return configuration;
	}

	/**
	 * Create a new DataSource object from a struct of properties, performing the necessary conversion from CFML-style property names to Hikari-style
	 * config names.
	 *
	 * @param config A struct of properties to configure the datasource. Will likely be defined via <code>Application.cfc</code> or a web admin.
	 *
	 * @return a DataSource object configured from the provided struct.
	 */
	public static DataSource fromDataSourceStruct( IStruct config ) {
		return new DataSource( new DatasourceConfig( null, null, config ) );
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
			return hikariDataSource.getConnection();
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
			return hikariDataSource.getConnection( username, password );
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
		hikariDataSource.close();
		return this;
	}

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
		return executePendingQuery( pendingQuery, conn );
	}

	/**
	 * Execute a query with a List of parameters on a given connection.
	 */
	public ExecutedQuery execute( String query, List<QueryParameter> parameters, Connection conn ) {
		PendingQuery pendingQuery = new PendingQuery( query, parameters );
		return executePendingQuery( pendingQuery, conn );
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
		PendingQuery pendingQuery = new PendingQuery( query, parameters );
		return executePendingQuery( pendingQuery, conn );
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
		PendingQuery pendingQuery = PendingQuery.fromStructParameters( query, parameters );
		return executePendingQuery( pendingQuery, conn );
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

	public ExecutedQuery executePendingQuery( PendingQuery pendingQuery, Connection conn ) {
		return pendingQuery.execute( conn );
	}

	/**
	 * Begin a transaction on the connection. (i.e. acquire a transaction object for further operations)
	 */
	public void executeTransactionally( String[] query ) {
		try ( Connection conn = getConnection() ) {
			executeTransactionally( query, conn );
		} catch ( SQLException e ) {
			throw new BoxRuntimeException( "Unable to close connection:", e );
		}
	}

	/**
	 * Execute a series of statements in a transaction.
	 * <p>
	 * Note the connection passed in is NOT closed automatically. It is up to the caller to close the connection when they are done with it. If you want
	 * an automanaged, i.e. autoclosed transaction, use the <code>executeTransactionally(String[])</code> method.
	 *
	 * @param query An array of SQL statements to execute in the transaction.
	 * @param conn  The connection to execute the transaction on. A connection is required - use <code>executeTransactionally(String[])</code> if you
	 *              don't wish to provide one.
	 */
	public void executeTransactionally( String[] query, Connection conn ) {
		try {
			conn.setAutoCommit( false );
			for ( String sql : query ) {
				try ( var stmt = conn.createStatement() ) {
					// @TODO: Flip between this for vanilla SQL and PreparedStatement for parameterized queries.
					stmt.execute( sql );

					// @TODO: Process the ResultSet, i.e. stmt.getResultSet()
					// ResultSet rs = stmt.getResultSet();
				}
			}
			conn.commit();
		} catch ( SQLException e ) {
			BoxRuntimeException bre = new BoxRuntimeException( "Error in transaction", e );
			// @TODO: Rolling back the transaction is a good idea... right?
			try {
				conn.rollback();
			} catch ( SQLException e2 ) {
				// keep our original exception as the "cause" so we're not obscuring upstream exceptions.
				bre = new BoxRuntimeException( "Error rolling back transaction", bre );
			}
			throw bre;
		} finally {
			try {
				conn.setAutoCommit( true );
			} catch ( SQLException e ) {
				throw new BoxRuntimeException( "Unable to re-enable autoCommit:", e );
			}
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
		return hikariDataSource.getUsername().equals( username ) && hikariDataSource.getPassword().equals( password );
	}

	/**
	 * Return whether the given configuration matches this datasource.
	 * <p>
	 * Useful for checking if a query is using the same datasource as a transaction, or if two datasources are the same prior to starting up a new
	 * datasource connection pool.
	 * <p>
	 * Compares ALL configuration properties. Any differences in configuration will return false.
	 */
	public Boolean isConfigurationMatch( DatasourceConfig datasourceConfig ) {
		return Objects.equals( getConfiguration().getProperties(), datasourceConfig.getProperties() );
	}
}
