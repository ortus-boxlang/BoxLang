package ortus.boxlang.runtime.jdbc.drivers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class DerbyDriverTest extends AbstractDriverTest {

	/**
	 * Override to provide driver-specific datasource name
	 */
	@Override
	String getDatasourceName() {
		return datasource.getOriginalName();
	}

	@Override
	@DisplayName( "It sets generatedKey in query meta" )
	@Test
	public void testGeneratedKey() {
		// broken in Apache Derby due to `getUpdateCount()` returning -1.
		// https://issues.apache.org/jira/browse/DERBY-211
	}
}
