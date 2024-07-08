package ortus.boxlang.runtime.bifs.global.jdbc;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.sql.SQLException;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.jdbc.DataSource;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.services.DatasourceService;
import tools.JDBCTestUtils;

import ortus.boxlang.runtime.config.segments.DatasourceConfig;
import ortus.boxlang.runtime.types.Struct;

public class BaseJDBCTest {

	static BoxRuntime			instance;
	ScriptingRequestBoxContext	context;
	IScope						variables;
	static DataSource			datasource;
	static DataSource			mssqlDatasource;
	static DatasourceService	datasourceService;

	@BeforeAll
	public static void setUp() {
		instance			= BoxRuntime.getInstance( true );
		datasourceService	= instance.getDataSourceService();
		String uniqueName = UUID.randomUUID().toString();
		datasource = JDBCTestUtils.constructTestDataSource( uniqueName );
		datasourceService.register( Key.of( uniqueName ), datasource );

		if ( JDBCTestUtils.hasMSSQLModule() ) {
			// Register a MSSQL datasource for later use
			mssqlDatasource = DataSource.fromStruct( Key.of( "MSSQLdatasource" ), Struct.of(
			    "username", "sa",
			    "password", "123456Password",
			    "host", "localhost",
			    "port", "1433",
			    "driver", "mssql",
			    "database", "master"
			) );
			instance.getConfiguration().runtime.datasources.put(
			    Key.of( "MSSQLdatasource" ),
			    mssqlDatasource.getConfiguration()
			);
			mssqlDatasource.execute( "CREATE TABLE developers ( id INTEGER, name VARCHAR(155), role VARCHAR(155) )" );
		}
	}

	@AfterAll
	public static void teardown() throws SQLException {
		JDBCTestUtils.dropDevelopersTable( datasource );
		datasource.shutdown();
		if ( mssqlDatasource != null ) {
			JDBCTestUtils.dropDevelopersTable( mssqlDatasource );
			mssqlDatasource.shutdown();
		}
	}

	@BeforeEach
	public void setupEach() {
		context = new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		context.getConnectionManager().setDefaultDatasource( datasource );
		variables = context.getScopeNearby( VariablesScope.name );
		assertDoesNotThrow( () -> JDBCTestUtils.resetDevelopersTable( datasource ) );
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
}
