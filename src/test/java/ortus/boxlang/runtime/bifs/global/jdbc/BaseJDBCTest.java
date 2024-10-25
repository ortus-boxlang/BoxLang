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
import ortus.boxlang.runtime.types.Struct;
import tools.JDBCTestUtils;

public class BaseJDBCTest {

	static BoxRuntime			instance;
	ScriptingRequestBoxContext	context;
	IScope						variables;
	static DataSource			datasource;
	static DataSource			mssqlDatasource;
	static DataSource			mysqlDatasource;
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
			Key mssqlName = Key.of( "MSSQLdatasource" );
			mssqlDatasource = DataSource.fromStruct( mssqlName, Struct.of(
			    "username", "sa",
			    "password", "123456Password",
			    "host", "localhost",
			    "port", "1433",
			    "driver", "mssql",
			    "database", "master"
			) );
			instance.getConfiguration().datasources.put(
			    mssqlName,
			    mssqlDatasource.getConfiguration()
			);
			datasourceService.register( mssqlName, mssqlDatasource );
			JDBCTestUtils.ensureTestTableExists( mssqlDatasource );
		}

		if ( JDBCTestUtils.hasMySQLModule() ) {
			/**
			 * docker run -d \
			 * --name MYSQL_boxlang \
			 * -p 3306:3306 \
			 * -e MYSQL_DATABASE=mysqlDB \
			 * -e MYSQL_ROOT_PASSWORD=123456Password \
			 * mysql:8
			 */
			// Register a mysql datasource for later use
			Key mysqlName = Key.of( "MySQLdatasource" );
			mysqlDatasource = DataSource.fromStruct( mysqlName, Struct.of(
			    "username", "root",
			    "password", "123456Password",
			    "host", "localhost",
			    "port", "3306",
			    "driver", "mysql",
			    "database", "mysqlDB"
			) );
			instance.getConfiguration().datasources.put(
			    mysqlName,
			    mysqlDatasource.getConfiguration()
			);
			datasourceService.register( mysqlName, mysqlDatasource );
			JDBCTestUtils.ensureTestTableExists( mysqlDatasource );
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
		if ( mysqlDatasource != null ) {
			JDBCTestUtils.dropDevelopersTable( mysqlDatasource );
			mysqlDatasource.shutdown();
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
