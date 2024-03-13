package ortus.boxlang.runtime.bifs.global.jdbc;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.sql.SQLException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.jdbc.DataSource;
import ortus.boxlang.runtime.jdbc.DataSourceManager;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

public class TransactionCommitTest {

	static BoxRuntime			instance;
	IBoxContext					context;
	IScope						variables;
	static Key					result	= new Key( "result" );

	static DataSourceManager	datasourceManager;
	static DataSource			datasource;

	@BeforeAll
	public static void setUp() {
		instance			= BoxRuntime.getInstance( true );
		datasourceManager	= DataSourceManager.getInstance();
		datasource			= new DataSource( Struct.of(
		    "jdbcUrl", "jdbc:derby:memory:testQueryExecuteDB;create=true"
		) );
		datasourceManager.setDefaultDataSource( datasource );
		datasource.execute( "CREATE TABLE developers ( id INTEGER, name VARCHAR(155), role VARCHAR(155) )" );
	}

	@AfterAll
	public static void teardown() throws SQLException {
		// datasourceManager.shutdown();
	}

	@BeforeEach
	public void resetTable() {
		assertDoesNotThrow( () -> {
			datasource.execute( "TRUNCATE TABLE developers" );
			datasource.execute( "INSERT INTO developers ( id, name, role ) VALUES ( 77, 'Michael Born', 'Developer' )" );
			datasource.execute( "INSERT INTO developers ( id, name, role ) VALUES ( 1, 'Luis Majano', 'CEO' )" );
			datasource.execute( "INSERT INTO developers ( id, name, role ) VALUES ( 42, 'Eric Peterson', 'Developer' )" );
		} );
	}

	@BeforeEach
	public void setupEach() {
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
	}

	@DisplayName( "It throws if there's no surrounding transaction" )
	@Test
	public void testNoTransactionContext() {
		BoxRuntimeException e = assertThrows( BoxRuntimeException.class, () -> instance.executeStatement( "transactionCommit()" ) );
		assertEquals( "Transaction not started; Please place this method call inside a transaction{} block.", e.getMessage() );
	}

}
