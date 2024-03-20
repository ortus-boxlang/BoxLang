package ortus.boxlang.runtime.bifs.global.jdbc;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.invoke.MethodHandles;
import java.sql.SQLException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import tools.JDBCTestUtils;

public class TransactionCommitTest {

	static BoxRuntime			instance;
	IBoxContext					context;
	IScope						variables;
	static Key					result	= new Key( "result" );

	static DataSourceManager	dataSourceManager;
	static DataSource			datasource;
	static Application			testApp;

	@BeforeAll
	public static void setUp() {
		instance			= BoxRuntime.getInstance( true );
		testApp				= new Application( Key.of( MethodHandles.lookup().lookupClass() ) );
		dataSourceManager	= testApp.getDataSourceManager();
		datasource			= new DataSource( Struct.of(
		    "jdbcUrl", "jdbc:derby:memory:" + testApp.getName() + ";create=true"
		) );
		dataSourceManager.setDefaultDataSource( datasource );
		datasource.execute( "CREATE TABLE developers ( id INTEGER, name VARCHAR(155), role VARCHAR(155) )" );
	}

	@AfterAll
	public static void teardown() throws SQLException {
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

	@DisplayName( "It throws if there's no surrounding transaction" )
	@Test
	public void testNoTransactionContext() {
		BoxRuntimeException e = assertThrows( BoxRuntimeException.class, () -> instance.executeStatement( "transactionCommit()" ) );
		assertEquals( "Transaction not started; Please place this method call inside a transaction{} block.", e.getMessage() );
	}

}
