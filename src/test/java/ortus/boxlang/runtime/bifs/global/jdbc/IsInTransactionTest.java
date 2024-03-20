package ortus.boxlang.runtime.bifs.global.jdbc;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import tools.JDBCTestUtils;

public class IsInTransactionTest {

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
		datasource			= JDBCTestUtils.constructTestDataSource( testApp.getName().getName() );
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

	@DisplayName( "It detects a surrounding transaction" )
	@Test
	public void testWithinTransaction() {
		instance.executeSource(
		    """
		    transaction{
		        variables.result = isWithinTransaction();
		    }
		    """,
		    context );
		assertTrue( variables.getAsBoolean( result ) );
	}

	@DisplayName( "It detects no surrounding transaction" )
	@Test
	public void testNotWithinTransaction() {
		instance.executeSource(
		    """
		    transaction{
		    	queryExecute( "SELECT 1" )
		    }
		    variables.result = isWithinTransaction();
		    """,
		    context );
		assertFalse( variables.getAsBoolean( result ) );
	}

	@DisplayName( "It detects a surrounding transaction" )
	@Test
	public void testInTransaction() {
		instance.executeSource(
		    """
		    transaction{
		        variables.result = isInTransaction();
		    }
		    """,
		    context );
		assertTrue( variables.getAsBoolean( result ) );
	}

	@DisplayName( "It detects no surrounding transaction" )
	@Test
	public void testNotInTransaction() {
		instance.executeSource(
		    """
		    transaction{
		    	queryExecute( "SELECT 1" )
		    }
		    variables.result = isInTransaction();
		    """,
		    context );
		assertFalse( variables.getAsBoolean( result ) );
	}

}
