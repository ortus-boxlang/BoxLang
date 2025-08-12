package ortus.boxlang.runtime.jdbc.drivers;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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

	@DisplayName( "It sets generatedKey in query meta" )
	@Test
	public void testGeneratedKey() {
		// test that the mysql test is actually running
		assertThat( 1 ).isEqualTo( 0 );
	}
}
