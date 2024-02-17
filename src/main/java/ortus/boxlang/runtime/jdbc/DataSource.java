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

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * Encapsulates a datasource configuration.
 */
public class DataSource {

	/**
	 * Driver type, used in constructing connection urls, as well as choosing which connection pool to utilize.
	 *
	 * <ul>
	 * <li><code>postgresql</code></li>
	 * <li><code>mysql</code></li>
	 * <li><code>mariadb</code></li>
	 * <li><code>mssql</code></li>
	 * <li>etc, etc.</li>
	 * </ul>
	 */
	private String				driver;

	/**
	 * Username for the datasource. Generally required on all datasource connections with the exception of in-memory databases.
	 */
	private String				username;

	/**
	 * Password for the datasource. Generally required on all datasource connections with the exception of in-memory databases.
	 */
	private String				password;

	/**
	 * JDBC URL for the datasource.
	 * <p>
	 * Specifies the driver type, host, port, and database name. The format of this URL is specific to the database being used - for example, a PostgreSQL
	 * connection url looks like <code>jdbc:postgresql://host:port/database</code>.
	 * <p>
	 * The URL is optional, but mutually exclusive with `driver`, `host`, `port`, and `databaseName`. If the URL is provided, `driver`, `host`, `port`,
	 * and `databaseName` are
	 * ignored.
	 */
	private String				url;
	private String				databaseName;
	private String				host;
	private Integer				port;

	/**
	 * Underlying HikariDataSource object, used in connection pooling.
	 */
	private HikariDataSource	hikariDataSource;

	/**
	 * Private constructor. Use fromStruct() instead.
	 */
	private DataSource( String driver, String username, String password, String url, String host, Integer port, String databaseName ) {
		this.driver			= driver;
		this.username		= username;
		this.password		= password;
		this.url			= url;
		this.host			= host;
		this.port			= port;
		this.databaseName	= databaseName;

		HikariConfig config = new HikariConfig();
		config.setJdbcUrl( url );
		config.setUsername( username );
		config.setPassword( password );
		// @TODO: Support ad-hoc properties:
		// config.addDataSourceProperty( "cachePrepStmts", "true" );
		// config.addDataSourceProperty( "prepStmtCacheSize", "250" );
		// config.addDataSourceProperty( "prepStmtCacheSqlLimit", "2048" );

		this.hikariDataSource = new HikariDataSource( config );
	}

	/**
	 * Configure and initialize a new DataSourceRecord object from a struct of properties.
	 *
	 * @param properties Struct of properties for configuring the datasource.
	 *                   The following properties are required:
	 *                   <li><code>username</code></li>
	 *                   <li><code>password</code></li>
	 *                   <li><code>url</code></li>
	 *                   <li><code>databaseName</code></li>
	 *                   <li><code>port</code></li>
	 *
	 * @return
	 */
	public static DataSource fromStruct( IStruct properties ) {
		// @TODO: Do we need to wrap exceptions in a BoxRuntimeException? Hikari already throws a RuntimeException if the connection pool can't be
		// established, so wrapping as a BoxRuntimeException (or a BoxSQLException?) would be redundant, not to mention the additional overhead.
		return new DataSource(
		    properties.getAsString( Key.driver ),
		    properties.getAsString( Key.username ),
		    properties.getAsString( Key.password ),
		    properties.getAsString( Key.URL ),
		    properties.getAsString( Key.host ),
		    properties.getAsInteger( Key.port ),
		    properties.getAsString( Key.databaseName )
		);
	}

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
	 * Execute a query on the connection.
	 */

	/**
	 * Begin a transaction on the connection. (i.e. acquire a transaction object for further operations)
	 */
	public void executeTransactionally( String[] query ) {
		executeTransactionally( query, getConnection() );
	}

	/**
	 * Execute a series of statements in a transaction.
	 *
	 * @param conn The connection to execute the transaction on. If not provided, a connection will be acquired from the datasource connection pool.
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
			conn.setAutoCommit( true );
		} catch ( SQLException e ) {
			// @TODO: Rollback the transaction?
			// conn.rollback();
			throw new RuntimeException( e );
		}
	}

}
