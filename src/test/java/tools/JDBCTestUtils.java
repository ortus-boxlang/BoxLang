package tools;

import java.sql.SQLException;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.config.segments.DatasourceConfig;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.jdbc.DataSource;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.DatabaseException;

/**
 * A collection of test utilities for assistance with JDBC tests, which are highly environment-specific and depend on certain loaded JDBC drivers.
 */
public class JDBCTestUtils {

	/**
	 * Boolean test for the presence of the BoxLang MariaDB module.
	 * <p>
	 * Useful in `@EnabledIf` annotations for conditional test execution based on the loaded JDBC drivers:
	 * <p>
	 * <code>
	 * &#64;EnabledIf( "tools.JDBCTestUtils#hasMariaDBModule" )
	 * </code>
	 *
	 * @return
	 */
	private static boolean hasModuleByName( Key moduleName ) {
		// System.out.println( String.format( "hasModuleByName(), checking for %s, loaded module names: %s", moduleName,
		// BoxRuntime.getInstance().getModuleService().getModuleNames() ) );
		return BoxRuntime.getInstance().getModuleService().hasModule( moduleName );
	}

	/**
	 * Boolean test for the presence of the BoxLang Postgres module.
	 * <p>
	 * Useful in `@EnabledIf` annotations for conditional test execution based on the loaded JDBC drivers:
	 * <p>
	 * <code>
	 * &#64;EnabledIf( "tools.JDBCTestUtils#hasPostgresModule" )
	 * </code>
	 *
	 * @return
	 */
	public static boolean hasPostgresModule() {
		return hasModuleByName( Key.of( "postgresql" ) );
	}

	/**
	 * Boolean test for the presence of the BoxLang Oracle module.
	 * <p>
	 * Useful in `@EnabledIf` annotations for conditional test execution based on the loaded JDBC drivers:
	 * <p>
	 * <code>
	 * &#64;EnabledIf( "tools.JDBCTestUtils#hasOracleModule" )
	 * </code>
	 *
	 * @return
	 */
	public static boolean hasOracleModule() {
		return hasModuleByName( Key.of( "oracle" ) );
	}

	/**
	 * Boolean test for the presence of the BoxLang SQLite module.
	 * <p>
	 * Useful in `@EnabledIf` annotations for conditional test execution based on the loaded JDBC drivers:
	 * <p>
	 * <code>
	 * &#64;EnabledIf( "tools.JDBCTestUtils#hasSQLiteModule" )
	 * </code>
	 *
	 * @return
	 */
	public static boolean hasSQLiteModule() {
		return hasModuleByName( Key.of( "sqlite" ) );
	}

	/**
	 * Boolean test for the presence of the BoxLang HyperSQL module.
	 * <p>
	 * Useful in `@EnabledIf` annotations for conditional test execution based on the loaded JDBC drivers:
	 * <p>
	 * <code>
	 * &#64;EnabledIf( "tools.JDBCTestUtils#hasHyperSQLModule" )
	 * </code>
	 *
	 * @return
	 */
	public static boolean hasHyperSQLModule() {
		return hasModuleByName( Key.of( "hypersql" ) );
	}

	/**
	 * Boolean test for the presence of the BoxLang MariaDB module.
	 * <p>
	 * Useful in `@EnabledIf` annotations for conditional test execution based on the loaded JDBC drivers:
	 * <p>
	 * <code>
	 * &#64;EnabledIf( "tools.JDBCTestUtils#hasMariaDBModule" )
	 * </code>
	 *
	 * @return
	 */
	public static boolean hasMariaDBModule() {
		return hasModuleByName( Key.of( "mariadb" ) );
	}

	/**
	 * Boolean test for the presence of the BoxLang MySQL module.
	 * <p>
	 * Useful in `@EnabledIf` annotations for conditional test execution based on the loaded JDBC drivers:
	 * <p>
	 * <code>
	 * &#64;EnabledIf( "tools.JDBCTestUtils#hasMySQLModule" )
	 * </code>
	 *
	 * @return
	 */
	public static boolean hasMySQLModule() {
		return hasModuleByName( Key.of( "mysql" ) );
	}

	/**
	 * Boolean test for the presence of the BoxLang MSSQL module
	 * <p>
	 * Useful in `@EnabledIf` annotations for conditional test execution based on the loaded JDBC drivers:
	 * <p>
	 * <code>
	 * &#64;EnabledIf( "tools.JDBCTestUtils#hasMSSQLModule" )
	 * </code>
	 *
	 * @return
	 */
	public static boolean hasMSSQLModule() {
		return hasModuleByName( Key.of( "mssql" ) );
	}

	/**
	 * Build out a structure of datasource configuration for testing. This is to inflate the state of a DatasourceConfig object
	 *
	 * @param databaseName String database name; must be unique for each test. In the future, we can change this to use either reflection or a stack trace
	 * @param properties   The properties to merge in
	 */
	public static IStruct getDatasourceConfig( String databaseName, IStruct properties ) {
		properties.computeIfAbsent( Key.of( "connectionString" ), key -> "jdbc:derby:memory:" + databaseName + ";create=true" );

		return Struct.of(
		    "name", databaseName,
		    "properties", properties
		);
	}

	/**
	 * Build out a structure of datasource configuration for testing. This is to inflate the state of a DatasourceConfig object
	 *
	 * @param databaseName String database name; must be unique for each test. In the future, we can change this to use either reflection or a stack trace
	 */
	public static IStruct getDatasourceConfig( String databaseName ) {
		return getDatasourceConfig( databaseName, new Struct() );
	}

	/**
	 * Build out a DatasourceConfig object for testing.
	 *
	 * @param databaseName String database name; must be unique for each test. In the future, we can change this to use either reflection or a stack trace
	 */
	public static DatasourceConfig buildDatasourceConfig( String databaseName ) {
		return new DatasourceConfig(
		    Key.of( databaseName ),
		    Struct.of(
		        "database", databaseName,
		        "driver", "derby",
		        "connectionString", "jdbc:derby:memory:" + databaseName + ";create=true"
		    )
		);
	}

	/**
	 * Build out a DataSource for testing. This doesn't register it, just creates a mock datasource for testing.
	 * The driver will be derby
	 *
	 * @param databaseName String database name; must be unique for each test. In the future, we can change this to use either reflection or a stack trace
	 *                     to grab the caller class name and thus ensure uniqueness.
	 * @param properties   The properties to merge in
	 */
	public static DataSource buildDatasource( String databaseName, IStruct properties ) {
		return DataSource.fromStruct(
		    databaseName,
		    Struct.of(
		        "database", databaseName,
		        "driver", "derby",
		        "connectionString", "jdbc:derby:memory:" + databaseName + ";create=true"
		    ) );
	}

	/**
	 * Build out a DataSource for testing. This doesn't register it, just creates a mock datasource for testing.
	 * The driver will be derby
	 *
	 * @param databaseName String database name; must be unique for each test. In the future, we can change this to use either reflection or a stack trace
	 *                     to grab the caller class name and thus ensure uniqueness.
	 */
	public static DataSource buildDatasource( String databaseName ) {
		return buildDatasource( databaseName, new Struct() );
	}

	/**
	 * Construct a test DataSource for use in testing.
	 * <p>
	 * This method is useful for creating a DataSource for use in testing, and is especially useful for in-memory databases like Apache Derby.
	 *
	 * @param databaseName String database name; must be unique for each test. In the future, we can change this to use either reflection or a stack trace
	 *                     to grab the caller class name and thus ensure uniqueness.
	 *
	 * @return A DataSource instance with a consistent `DEVELOPERS` table created.
	 */
	public static DataSource constructTestDataSource( String databaseName, IBoxContext context ) {
		DataSource datasource = DataSource.fromStruct(
		    databaseName,
		    Struct.of(
		        "database", databaseName,
		        "driver", "derby",
		        "connectionString", "jdbc:derby:memory:" + databaseName + ";create=true"
		    ) );
		ensureTestTableExists( datasource, context );
		return datasource;
	}

	/**
	 * Drop a test table from the database, optionally ignoring exceptions if it doesn't exist.
	 *
	 * @param datasource   The datasource on which this table exists
	 * @param context      The context to use for executing the drop
	 * @param tableName    The name of the table to drop
	 * @param ignoreErrors if true, will ignore exceptions if the table doesn't exist
	 */
	public static void dropTestTable( DataSource datasource, IBoxContext context, String tableName, boolean ignoreErrors ) {
		try {
			datasource.execute( "DROP TABLE " + tableName, context );
		} catch ( DatabaseException ignored ) {
			if ( !ignoreErrors ) {
				throw ignored;
			}
		}
	}

	/**
	 * Ensure various tables exist in the database for testing purposes.
	 */
	public static void ensureTestTableExists( DataSource datasource, IBoxContext context ) {
		try {
			if ( datasource.getBoxConnection().getMetaData().getDriverName().toLowerCase().contains( "microsoft" ) ) {
				datasource.execute( "CREATE TABLE developers ( id INTEGER, name VARCHAR(155), role VARCHAR(155), createdAt DATETIME )", context );
			} else {
				datasource.execute( "CREATE TABLE developers ( id INTEGER, name VARCHAR(155), role VARCHAR(155), createdAt TIMESTAMP )", context );
			}
		} catch ( DatabaseException | SQLException e ) {
			e.printStackTrace();
			// Ignore the exception if the table already exists
		}
	}

	/**
	 * Reset the `developers` table to a known, consistent state for testing.
	 *
	 * @param datasource
	 */
	public static void resetDevelopersTable( DataSource datasource, IBoxContext context ) {
		String currentDate = getCurrentDate( datasource );
		datasource.execute( "TRUNCATE TABLE developers", context );
		datasource.execute( "INSERT INTO developers ( id, name, role, createdAt ) VALUES ( 77, 'Michael Born', 'Developer', " + currentDate + " )", context );
		datasource.execute( "INSERT INTO developers ( id, name, role, createdAt ) VALUES ( 1, 'Luis Majano', 'CEO', " + currentDate + " )", context );
		datasource.execute( "INSERT INTO developers ( id, name, role, createdAt ) VALUES ( 42, 'Eric Peterson', 'Developer', " + currentDate + " )", context );
		datasource.execute( "INSERT INTO developers ( id, name, role, createdAt ) VALUES ( 9001, 'Bob O''Reily', 'QA', " + currentDate + " )", context );
	}

	private static String getCurrentDate( DataSource datasource ) {
		String driverName;
		try {
			driverName = datasource.getBoxConnection().getMetaData().getDriverName().toLowerCase();
		} catch ( SQLException e ) {
			throw new DatabaseException( "Failed to get current date", e );
		}
		if ( driverName.contains( "microsoft" ) ) {
			return "GETDATE()";
		} else if ( driverName.contains( "oracle" ) ) {
			return "SYSDATE";
		} else if ( driverName.contains( "mysql" ) ) {
			return "NOW()";
		} else {
			// Derby, PostgreSQL, most others
			return "CURRENT_TIMESTAMP";
		}
	}

}
