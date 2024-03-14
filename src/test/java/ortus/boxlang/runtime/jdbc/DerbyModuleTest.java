package ortus.boxlang.runtime.jdbc;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Paths;
import java.sql.SQLException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import ortus.boxlang.parser.BoxScriptType;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.services.ModuleService;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Query;
import ortus.boxlang.runtime.types.Struct;

public class DerbyModuleTest {

	static DataSourceManager	dataSourceManager;
	static BoxRuntime			instance;
	IBoxContext					context;
	IScope						variables;
	static Key					result	= new Key( "result" );

	@BeforeAll
	public static void setUp() {
		instance			= BoxRuntime.getInstance( true );
		dataSourceManager	= DataSourceManager.getInstance();
	}

	@AfterAll
	public static void teardown() throws SQLException {
		// dataSourceManager.shutdown();
	}

	@BeforeEach
	public void setupEach() {
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
	}

	@Test
	@Disabled( "Not sure why, check this please Michel Born" )
	void testDerbyConnection() throws SQLException {
		BoxRuntime		instance		= BoxRuntime.getInstance( true );
		ModuleService	moduleService	= instance.getModuleService();
		String			modulesPath		= Paths.get( "./modules" ).toAbsolutePath().toString();
		instance.getConfiguration().runtime.modulesDirectory.add( modulesPath );
		moduleService.onStartup();

		DataSource datasource = new DataSource( Struct.of(
		    "jdbcUrl", "jdbc:derby:memory:DerbyModuleTest;create=true"
		) );
		datasource.execute( "CREATE TABLE developers ( id INTEGER, name VARCHAR(155), role VARCHAR(155) )" );
		dataSourceManager.setDefaultDataSource( datasource );

		instance.executeSource( """
		                        <cfquery>
		                        	INSERT INTO developers ( id, name, role ) VALUES (
		                        		<cfqueryparam value="77" sqltype="INTEGER" />,
		                        		<cfqueryparam value="Michael Born" sqltype="VARCHAR" />,
		                        		<cfqueryparam value="Developer" />
		                        	)
		                        </cfquery>
		                        """,
		    context, BoxScriptType.CFMARKUP );

		instance.executeSource(
		    """
		    result = queryExecute( "SELECT * FROM developers ORDER BY id" );
		    """,
		    context );

		assertThat( variables.get( result ) ).isInstanceOf( Query.class );
		Query query = variables.getAsQuery( result );
		assertEquals( 1, query.size() );

		IStruct michael = query.getRowAsStruct( 0 );
		assertEquals( 77, michael.get( "id" ) );
		assertEquals( "Michael Born", michael.get( "name" ) );
		assertEquals( "Developer", michael.get( "role" ) );
	}
}
