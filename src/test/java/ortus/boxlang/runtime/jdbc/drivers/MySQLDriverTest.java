package ortus.boxlang.runtime.jdbc.drivers;

import org.junit.jupiter.api.condition.EnabledIf;

@EnabledIf( "tools.JDBCTestUtils#hasMySQLModule" )
public class MySQLDriverTest extends AbstractDriverTest {

	/**
	 * Override to provide driver-specific datasource name
	 */
	@Override
	String getDatasourceName() {
		return "MySQLdatasource";
	}
}
