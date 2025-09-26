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
import ortus.boxlang.runtime.jdbc.drivers.MSSQLDriverTest;
import ortus.boxlang.runtime.jdbc.drivers.MariaDBDriverTest;
import ortus.boxlang.runtime.jdbc.drivers.MySQLDriverTest;
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
	public static DataSource			mssqlDatasource;
	public static DataSource			mysqlDatasource;
	public static DataSource			mariaDBDatasource;
	public static DatasourceService		datasourceService;

	@BeforeAll
	public static void setUp() {
		instance = BoxRuntime.getInstance( true );
		IBoxContext setUpContext = new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		datasourceService = instance.getDataSourceService();
		String uniqueName = UUID.randomUUID().toString();
		datasource = JDBCTestUtils.constructTestDataSource( uniqueName, setUpContext );
		Key datasourceKey = Key.of( uniqueName );
		datasourceService.register( datasourceKey, datasource );
		instance.getConfiguration().datasources.put(
		    datasourceKey,
		    datasource.getConfiguration()
		);
		if ( JDBCTestUtils.hasMSSQLModule() ) {
			mssqlDatasource = MSSQLDriverTest.setupTestDatasource( instance, setUpContext );
		}

		if ( JDBCTestUtils.hasMySQLModule() ) {
			mysqlDatasource = MySQLDriverTest.setupTestDatasource( instance, setUpContext );
		}

		if ( JDBCTestUtils.hasMariaDBModule() ) {
			mariaDBDatasource = MariaDBDriverTest.setupTestDatasource( instance, setUpContext );
		}
	}

	@AfterAll
	public static void teardown() throws SQLException {
		IBoxContext tearDownContext = new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		JDBCTestUtils.dropTestTable( datasource, tearDownContext, "developers", true );
		datasource.shutdown();
		if ( mssqlDatasource != null ) {
			JDBCTestUtils.dropTestTable( mssqlDatasource, tearDownContext, "developers", true );
			mssqlDatasource.shutdown();
		}
		if ( mysqlDatasource != null ) {
			JDBCTestUtils.dropTestTable( mysqlDatasource, tearDownContext, "developers", true );
			mysqlDatasource.shutdown();
		}
		if ( mariaDBDatasource != null ) {
			JDBCTestUtils.dropTestTable( mariaDBDatasource, tearDownContext, "developers", true );
			mariaDBDatasource.shutdown();
		}
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
}
