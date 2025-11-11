package ortus.boxlang.runtime.jdbc.drivers;

import java.sql.SQLException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.condition.EnabledIf;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.jdbc.DataSource;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;

@Disabled( "Lacking generated key table and stored procedure implementations" )
@EnabledIf( "tools.JDBCTestUtils#hasOracleModule" )
public class OracleDriverTest extends AbstractDriverTest {

	public static DataSource	oracleDatasource;

	protected static Key		datasourceName		= Key.of( "OracleDatasource" );

	protected static IStruct	datasourceConfig	= Struct.of(
	    "username", "system",
	    "password", "123456Password",
	    "host", "localhost",
	    "port", "1521",
	    "driver", "oracle",
	    "serviceName", "XEPDB1"
	);

	@BeforeAll
	public static void setUp() {
		instance = BoxRuntime.getInstance( true );
		IBoxContext setUpContext = new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		oracleDatasource = AbstractDriverTest.setupTestDatasource( instance, setUpContext, datasourceName, datasourceConfig );
		OracleDriverTest.createGeneratedKeyTable( oracleDatasource, setUpContext );
		OracleDriverTest.createStoredProcedure( oracleDatasource, setUpContext );
	}

	@AfterAll
	public static void teardown() throws SQLException {
		IBoxContext tearDownContext = new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		AbstractDriverTest.teardownTestDatasource( tearDownContext, oracleDatasource );
	}

	public static void createGeneratedKeyTable( DataSource dataSource, IBoxContext context ) {
		// @TODO: Implement!
	}

	public static void createStoredProcedure( DataSource dataSource, IBoxContext context ) {
		// @TODO: Implement!
	}

	/**
	 * Override to provide driver-specific datasource name
	 */
	@Override
	String getDatasourceName() {
		return "OracleDatasource";
	}
}