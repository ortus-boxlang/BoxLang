package ortus.boxlang.runtime.bifs.global.jdbc;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.sql.SQLException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.application.Application;
import ortus.boxlang.runtime.context.ApplicationBoxContext;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.jdbc.DataSource;
import ortus.boxlang.runtime.jdbc.DataSourceManager;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import tools.JDBCTestUtils;

public class BaseJDBCTest {

	static BoxRuntime			instance;
	IBoxContext					context;
	IScope						variables;
	static DataSource			datasource;
	static Application			testApp;
	static DataSourceManager	dataSourceManager;

	@BeforeAll
	public static void setUp() {
		instance			= BoxRuntime.getInstance( true );
		testApp				= new Application( Key.of( java.util.UUID.randomUUID().toString() ) );
		dataSourceManager	= testApp.getDataSourceManager();
		datasource			= JDBCTestUtils.constructTestDataSource( testApp.getName().getName() );
		dataSourceManager.setDefaultDataSource( datasource );
	}

	@AfterAll
	public static void teardown() throws SQLException {
		JDBCTestUtils.dropDevelopersTable( datasource );
		testApp.shutdown();
	}

	@BeforeEach
	public void setupEach() {
		ApplicationBoxContext appContext = new ApplicationBoxContext( testApp );
		appContext.setParent( instance.getRuntimeContext() );
		context		= new ScriptingRequestBoxContext( appContext );
		variables	= context.getScopeNearby( VariablesScope.name );
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