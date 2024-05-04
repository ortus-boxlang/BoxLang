package tools;

import java.sql.DriverManager;

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
	 * Boolean test for the presence of the MySQL JDBC driver.
	 * <p>
	 * Useful in `@EnabledIf` annotations for conditional test execution based on the loaded JDBC drivers:
	 * <p>
	 * <code>
	 * &#64;EnabledIf( "tools.JDBCTestUtils#hasMySQLDriver" )
	 * </code>
	 *
	 * @return
	 */
	public static boolean hasMySQLDriver() {
		return DriverManager.drivers()
		    .filter( driver -> {
			    String driverName = driver.getClass().getName();
			    return driverName.equals( "com.mysql.jdbc.Driver" ) || driverName.equals( "com.mysql.cj.jdbc.Driver" );
		    } )
		    .findFirst()
		    .map( driver -> true )
		    .orElse( false );
	}

	public static IStruct getDatasourceConfig( String databaseName, String driver, IStruct properties ) {

		properties.computeIfAbsent( Key.of( "connectionString" ), key -> "jdbc:derby:memory:" + databaseName + ";create=true" );

		return Struct.of(
		    "name", databaseName,
		    "driver", driver,
		    "properties", properties
		);
	}

	public static IStruct getDatasourceConfig( String databaseName ) {
		return getDatasourceConfig( databaseName, "derby", new Struct() );
	}

	/**
	 * Build out a DataSource for testing. This doesn't register it, just creates a mock datasource for testing.
	 *
	 * @param databaseName String database name; must be unique for each test. In the future, we can change this to use either reflection or a stack trace
	 *                     to grab the caller class name and thus ensure uniqueness.
	 * @param driver       String driver name or OTHER
	 */
	public static DataSource buildDatasource( String databaseName, String driver, IStruct properties ) {
		return DataSource.fromStruct( getDatasourceConfig( databaseName, driver, properties ) );
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
		return buildDatasource( databaseName, "derby", properties );
	}

	/**
	 * Build out a DataSource for testing. This doesn't register it, just creates a mock datasource for testing.
	 * The driver will be derby
	 *
	 * @param databaseName String database name; must be unique for each test. In the future, we can change this to use either reflection or a stack trace
	 *                     to grab the caller class name and thus ensure uniqueness.
	 */
	public static DataSource buildDatasource( String databaseName ) {
		return buildDatasource( databaseName, "derby", new Struct() );
	}

	/**
	 * Construct a test DataSource for use in testing.
	 * <p>
	 * This method is useful for creating a DataSource for use in testing, and is especially useful for in-memory databases like Apache Derby.
	 *
	 * @param databaseName String database name; must be unique for each test. In the future, we can change this to use either reflection or a stack trace
	 *                     to grab the caller class name and thus ensure uniqueness.
	 *
	 * @param driver       String driver name or OTHER
	 *
	 * @return A DataSource instance with a consistent `DEVELOPERS` table created.
	 */
	public static DataSource constructTestDataSource( String databaseName, String driver ) {
		DataSource datasource = DataSource.fromStruct( Struct.of(
		    "name", databaseName,
		    "driver", driver,
		    "properties", Struct.of( "connectionString", "jdbc:derby:memory:" + databaseName + ";create=true" )
		) );
		try {
			datasource.execute( "CREATE TABLE developers ( id INTEGER, name VARCHAR(155), role VARCHAR(155) )" );
		} catch ( DatabaseException e ) {
			// Ignore the exception if the table already exists
		}
		return datasource;
	}

	public static DataSource constructTestDataSource( String databaseName ) {
		return constructTestDataSource( databaseName, "derby" );
	}

	/**
	 * Remove the developers table from the database.
	 *
	 * @param datasource
	 */
	public static void dropDevelopersTable( DataSource datasource ) {
		datasource.execute( "DROP TABLE developers" );
	}

	/**
	 * Reset the `developers` table to a known, consistent state for testing.
	 *
	 * @param datasource
	 */
	public static void resetDevelopersTable( DataSource datasource ) {
		datasource.execute( "TRUNCATE TABLE developers" );
		datasource.execute( "INSERT INTO developers ( id, name, role ) VALUES ( 77, 'Michael Born', 'Developer' )" );
		datasource.execute( "INSERT INTO developers ( id, name, role ) VALUES ( 1, 'Luis Majano', 'CEO' )" );
		datasource.execute( "INSERT INTO developers ( id, name, role ) VALUES ( 42, 'Eric Peterson', 'Developer' )" );
	}
}
