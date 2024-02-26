package tools;

import java.sql.DriverManager;

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
}
