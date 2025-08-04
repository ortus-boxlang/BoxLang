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
import ortus.boxlang.runtime.types.exceptions.DatabaseException;
import tools.JDBCTestUtils;

public class BaseJDBCTest {

	public static BoxRuntime			instance;
	public ScriptingRequestBoxContext	context;
	public IScope						variables;
	public static DataSource			datasource;
	public static DataSource			mssqlDatasource;
	public static DataSource			mysqlDatasource;
	public static DatasourceService		datasourceService;

	@BeforeAll
	public static void setUp() {
		instance = BoxRuntime.getInstance( true );
		IBoxContext setUpContext = new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		datasourceService = instance.getDataSourceService();
		String uniqueName = UUID.randomUUID().toString();
		datasource = JDBCTestUtils.constructTestDataSource( uniqueName, setUpContext );
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
			JDBCTestUtils.ensureTestTableExists( mssqlDatasource, setUpContext );

			try {
				mssqlDatasource.execute( "DROP TABLE generatedKeyTest", setUpContext );
			} catch ( DatabaseException ignored ) {
			}
			try {
				mssqlDatasource.execute( "CREATE TABLE generatedKeyTest( id INT IDENTITY(1,1) PRIMARY KEY, name VARCHAR(155))", setUpContext );
			} catch ( DatabaseException ignored ) {
			}
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
			    "port", "3309",
			    "driver", "mysql",
			    "database", "myDB",
			    "custom", "allowMultiQueries=true"
			) );
			instance.getConfiguration().datasources.put(
			    mysqlName,
			    mysqlDatasource.getConfiguration()
			);
			datasourceService.register( mysqlName, mysqlDatasource );
			JDBCTestUtils.ensureTestTableExists( mysqlDatasource, setUpContext );
			try {
				mysqlDatasource.execute( "DROP TABLE generatedKeyTest", setUpContext );
			} catch ( DatabaseException ignored ) {
			}
			try {
				mysqlDatasource.execute( "CREATE TABLE generatedKeyTest( id INT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(155))", setUpContext );
			} catch ( DatabaseException ignored ) {
			}
		}
	}

	@AfterAll
	public static void teardown() throws SQLException {
		IBoxContext tearDownContext = new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		JDBCTestUtils.dropDevelopersTable( datasource, tearDownContext );
		datasource.shutdown();
		if ( mssqlDatasource != null ) {
			JDBCTestUtils.dropDevelopersTable( mssqlDatasource, tearDownContext );
			mssqlDatasource.shutdown();
		}
		if ( mysqlDatasource != null ) {
			JDBCTestUtils.dropDevelopersTable( mysqlDatasource, tearDownContext );
			mysqlDatasource.shutdown();
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

	public static DataSource getMysqlDatasource() {
		return mysqlDatasource;
	}

	public static DataSource getMssqlDatasource() {
		return mssqlDatasource;
	}

	public static DatasourceService getDatasourceService() {
		return datasourceService;
	}
}
