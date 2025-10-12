package ortus.boxlang.runtime.bifs.global.jdbc;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.jdbc.DataSource;
import ortus.boxlang.runtime.modules.ModuleRecord;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.services.DatasourceService;
import tools.JDBCTestUtils;

public class BaseJDBCTest {

	public static BoxRuntime			instance;
	public ScriptingRequestBoxContext	context;
	public IScope						variables;
	public static DataSource			datasource;
	public static DatasourceService		datasourceService;
	public static String				moduleDependenciesPath	= Paths.get( "./src/test/resources/modules" ).toAbsolutePath().toString();

	@BeforeAll
	public static void setUp() {
		instance = BoxRuntime.getInstance( true );
		IBoxContext setUpContext = new ScriptingRequestBoxContext( instance.getRuntimeContext() );

		loadModule( Key.of( "derby" ), moduleDependenciesPath + "/bx-derby", setUpContext );

		datasourceService = instance.getDataSourceService();
		String uniqueName = UUID.randomUUID().toString();
		datasource = JDBCTestUtils.constructTestDataSource( uniqueName, setUpContext );
		Key datasourceKey = Key.of( uniqueName );
		datasourceService.register( datasourceKey, datasource );
		instance.getConfiguration().datasources.put(
		    datasourceKey,
		    datasource.getConfiguration()
		);
	}

	@AfterAll
	public static void teardown() throws SQLException {
		IBoxContext tearDownContext = new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		JDBCTestUtils.dropTestTable( datasource, tearDownContext, "developers", true );
		datasource.shutdown();
	}

	@BeforeEach
	public void setupEach() {
		context = new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		context.getConnectionManager().setDefaultDatasource( datasource );
		variables = context.getScopeNearby( VariablesScope.name );
		assertDoesNotThrow( () -> JDBCTestUtils.resetDevelopersTable( datasource, context ) );
		// Clear the caches
		instance.getCacheService().getDefaultCache().clearAll();
	}

	public static BoxRuntime getInstance() {
		return instance;
	}

	public IBoxContext getContext() {
		return context;
	}

	public IScope getVariables() {
		return variables;
	}

	public static DataSource getDatasource() {
		return datasource;
	}

	public static DatasourceService getDatasourceService() {
		return datasourceService;
	}

	protected static void loadModule( Key moduleName, String modulePath, IBoxContext context ) {
		if ( !instance.getModuleService().hasModule( moduleName ) ) {
			System.out.println( String.format( "Loading %s module...", moduleName ) );
			if ( !Files.exists( Paths.get( modulePath ) ) ) {
				System.out.println( String.format( "%s module not found at %s", moduleName, modulePath ) );
				System.out.println( "Please run 'gradle installDerbyModule' to install the required modules." );
				throw new RuntimeException( String.format( "%s module not found at %s", moduleName, modulePath ) );
			}

			ModuleRecord moduleRecord = new ModuleRecord( modulePath );
			instance.getModuleService().getRegistry().put( moduleName, moduleRecord );
			moduleRecord
			    .loadDescriptor( context )
			    .register( context )
			    .activate( context );
		} else {
			System.out.println( String.format( "%s module already loaded, skipping...", moduleName ) );
		}
	}
}
