package ortus.boxlang.runtime.bifs.global.jdbc;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.lang.invoke.MethodHandles;
import java.sql.SQLException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.jdbc.DataSource;
import ortus.boxlang.runtime.jdbc.DataSourceManager;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.VariablesScope;
import tools.JDBCTestUtils;

public class BaseJDBCTest {

	static BoxRuntime			instance;
	ScriptingRequestBoxContext	context;
	IScope						variables;
	static DataSource			datasource;
	static DataSourceManager	dataSourceManager;

	@BeforeAll
	public static void setUp() {
		instance	= BoxRuntime.getInstance( true );
		datasource	= JDBCTestUtils.constructTestDataSource( MethodHandles.lookup().lookupClass().getSimpleName() );
	}

	@AfterAll
	public static void teardown() throws SQLException {
		JDBCTestUtils.dropDevelopersTable( datasource );
		datasource.shutdown();
	}

	@BeforeEach
	public void setupEach() {
		context				= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		variables			= context.getScopeNearby( VariablesScope.name );

		dataSourceManager	= context.getDataSourceManager();
		dataSourceManager.setDefaultDataSource( datasource );

		assertDoesNotThrow( () -> JDBCTestUtils.resetDevelopersTable( datasource ) );
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

	public static DataSourceManager getDataSourceManager() {
		return dataSourceManager;
	}
}