package ortus.boxlang.runtime.jdbc;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Paths;
import java.sql.SQLException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.services.DatasourceService;
import ortus.boxlang.runtime.services.ModuleService;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Query;
import tools.JDBCTestUtils;

public class DerbyModuleTest {

	static DatasourceService	datasourceService;
	static BoxRuntime			runtime;
	ScriptingRequestBoxContext	context;
	IScope						variables;
	static Key					result	= new Key( "result" );

	@BeforeAll
	public static void setUp() {
		runtime = BoxRuntime.getInstance( true );
	}

	@AfterAll
	public static void teardown() throws SQLException {
		if ( datasourceService != null ) {
			datasourceService.onShutdown( true );
		}
	}

	@BeforeEach
	public void setupEach() {
		context		= new ScriptingRequestBoxContext( runtime.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
	}

	@Test
	void testDerbyConnection() throws SQLException {
		ModuleService	moduleService	= runtime.getModuleService();
		String			modulesPath		= Paths.get( "./modules" ).toAbsolutePath().toString();
		runtime.getConfiguration().runtime.modulesDirectory.add( modulesPath );
		moduleService.onStartup();

		DataSource datasource = JDBCTestUtils.constructTestDataSource(
		    "DerbyModuleTest"
		);

		context.getConnectionManager().setDefaultDatasource( datasource );

		// @formatter:off
		runtime.executeSource( """
			<cfquery>
				INSERT INTO developers ( id, name, role ) VALUES (
				<cfqueryparam value="77" sqltype="INTEGER" />,
				<cfqueryparam value="Michael Born" sqltype="VARCHAR" />,
				<cfqueryparam value="Developer" />
			)
			</cfquery>
			""",
			context,
			BoxSourceType.CFTEMPLATE
		);

		runtime.executeSource(
		    """
		    result = queryExecute( "SELECT * FROM developers ORDER BY id" );
		    """,
		 context );
		// @formatter:on

		assertThat( variables.get( result ) ).isInstanceOf( Query.class );
		Query query = variables.getAsQuery( result );
		assertEquals( 1, query.size() );

		IStruct michael = query.getRowAsStruct( 0 );
		assertEquals( 77, michael.get( "id" ) );
		assertEquals( "Michael Born", michael.get( "name" ) );
		assertEquals( "Developer", michael.get( "role" ) );
	}
}
