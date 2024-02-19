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
import java.sql.Statement;
import java.util.Properties;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * Encapsulates a datasource configuration.
 *
 * @TODO: This class needs to move to a JDBC module to allow a leaner, lighter-weight BoxLang Core.
 */
public class DataSource {

	// /**
	// * Driver type. For future use.
	// *
	// * <ul>
	// * <li><code>postgresql</code></li>
	// * <li><code>mysql</code></li>
	// * <li><code>mariadb</code></li>
	// * <li><code>mssql</code></li>
	// * <li>etc, etc.</li>
	// * </ul>
	// */
	// private String driver;

	// /**
	// * Username for the datasource. Generally required on all datasource connections with the exception of in-memory databases.
	// */
	// private String username;

	// /**
	// * Password for the datasource. Generally required on all datasource connections with the exception of in-memory databases.
	// */
	// private String password;

	// /**
	// * JDBC URL for the datasource.
	// * <p>
	// * Specifies the driver type, host, port, and database name. The format of this URL is specific to the database being used - for example, a
	// PostgreSQL
	// * connection url looks like <code>jdbc:postgresql://host:port/database</code>.
	// * <p>
	// * The URL is optional, but mutually exclusive with `driver`, `host`, `port`, and `databaseName`. If the URL is provided, `driver`, `host`, `port`,
	// * and `databaseName` are
	// * ignored.
	// */
	// private String url;

	// /**
	// * Database name to connect to. Most, but not all, database vendors require this.
	// */
	// private String databaseName;

	// /**
	// * Hostname of the database server. Most, but not all, database vendors require this.
	// */
	// private String host;

	// /**
	// * Port number of the database server. Most, but not all, database vendors require this.
	// */
	// private Integer port;

	/**
	 * Underlying HikariDataSource object, used in connection pooling.
	 */
	private HikariDataSource hikariDataSource;

	/**
	 * Configure and initialize a new DataSourceRecord object from a struct of properties.
	 *
	 * @param properties Struct of properties for configuring the datasource. Be aware that the struct keys are case-sensitive and must match the Hikari
	 *                   configuration property names. (We'll be adding support for case-insensitive keys in the near future.)
	 *
	 * @return
	 */
	public DataSource( IStruct config ) {
		Properties properties = new Properties();
		config.forEach( ( key, value ) -> {
			properties.setProperty( key.getName(), ( String ) value );
		} );

		HikariConfig hikariConfig = new HikariConfig( properties );
		this.hikariDataSource = new HikariDataSource( hikariConfig );

	}

	// public static DataSource fromStruct( IStruct properties ) {
	// // @TODO: Do we need to wrap exceptions in a BoxRuntimeException? Hikari already throws a RuntimeException if the connection pool can't be
	// // established, so wrapping as a BoxRuntimeException (or a BoxSQLException?) would be redundant, not to mention the additional overhead.
	// return new DataSource(
	// properties.getAsString( Key.driver ),
	// properties.getAsString( Key.username ),
	// properties.getAsString( Key.password ),
	// properties.getAsString( Key.URL ),
	// properties.getAsString( Key.host ),
	// properties.getAsInteger( Key.port ),
	// properties.getAsString( Key.databaseName )
	// );
	// }

	/**
	 * Get a connection to the configured datasource.
	 *
	 * @return A JDBC connection to the configured datasource.
	 *
	 * @throws SQLException if connection could not be established.
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
	 * Shut down the datasource, including the connection pool and all connections.
	 *
	 * @return This DataSource object, which is now shut down and useless for any further operations.
	 */
	public DataSource shutdown() {
		hikariDataSource.close();
		return this;
	}

	/**
	 * Execute a query on the connection, using a connection from the connection pool.
	 */
	public void execute( String query ) {
		try ( Connection conn = getConnection(); ) {
			execute( query, conn );
		} catch ( SQLException e ) {
			throw new BoxRuntimeException( "Unable to close connection:", e );
		}
	}

	/**
	 * Execute a query on the connection, using the provided connection.
	 * <p>
	 * Note the connection passed in is NOT closed automatically. It is up to the caller to close the connection when they are done with it. If you want
	 * an automanaged, i.e. autoclosed connection, use the <code>execute(String)</code> method.
	 *
	 * @param query The SQL query to execute.
	 */
	public void execute( String query, Connection conn ) {
		try ( Statement stmt = conn.createStatement() ) {
			// @TODO: Implement parameterized queries with PreparedStatement.
			stmt.execute( query );
			// @TODO: Implement ResultSet processing and return an array or query object.
		} catch ( SQLException e ) {
			throw new BoxRuntimeException( "Unable to execute query:", e );
		}
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

}
