package tools;

import java.sql.DriverManager;

import ortus.boxlang.runtime.jdbc.DataSource;
import ortus.boxlang.runtime.types.Struct;

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
	public static DataSource constructTestDataSource( String databaseName ) {
		DataSource datasource = new DataSource( Struct.of(
		    "jdbcUrl", "jdbc:derby:memory:" + databaseName + ";create=true"
		) );
		datasource.execute( "CREATE TABLE developers ( id INTEGER, name VARCHAR(155), role VARCHAR(155) )" );
		return datasource;
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
