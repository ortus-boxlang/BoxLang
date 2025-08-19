package ortus.boxlang.runtime.jdbc.drivers;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

@EnabledIf( "tools.JDBCTestUtils#hasMSSQLModule" )
public class MSSQLDriverTest extends AbstractDriverTest {

	/**
	 * Override to provide driver-specific datasource name
	 */
	@Override
	String getDatasourceName() {
		return "MSSQLdatasource";
	}

	@DisplayName( "It can return a rowcount in the second SQL statement" )
	@Disabled( "Disabled until BL-1186 is resolved" )
	@Test
	public void testRowCount() {
		// @formatter:off
		instance.executeStatement(
		    String.format( """
				result = queryExecute( "
					update developers set name = 'Michael Borne' where name = 'Michael Born';
					select @@rowcount c;
				", {}, { "datasource" : "%s"} );
			""", getDatasourceName() ),
		    context );
		// @formatter:on
		assertThat( variables.get( result ) ).isEqualTo( 1 );
	}
}
