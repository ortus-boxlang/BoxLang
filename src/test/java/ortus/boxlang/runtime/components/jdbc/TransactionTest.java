
/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ortus.boxlang.runtime.components.jdbc;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.invoke.MethodHandles;

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
import ortus.boxlang.runtime.types.Query;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * Tests the basics of the transaction component, especially attribute validation.
 * <p>
 * More advanced transactional logic tests should be implemented in
 * <code>ortus.boxlang.runtime.jdbc.TransactionTest</code> class.
 */
public class TransactionTest {

	static DataSourceManager	dataSourceManager;
	static DataSource			datasource;
	static BoxRuntime			instance;
	IBoxContext					context;
	IScope						variables;
	static Key					result	= new Key( "result" );
	static Application			testApp;

	@BeforeAll
	public static void setUp() {
		instance			= BoxRuntime.getInstance( true );
		testApp				= new Application( Key.of( MethodHandles.lookup().lookupClass() ) );
		dataSourceManager	= testApp.getDataSourceManager();
		datasource			= new DataSource( Struct.of(
		    "jdbcUrl", "jdbc:derby:memory:" + testApp.getName() + ";create=true"
		) );

		// Transactions generally assume a default datasource set at the application level.
		dataSourceManager.setDefaultDataSource( datasource );
		datasource.execute( "CREATE TABLE developers ( id INTEGER, name VARCHAR(155), role VARCHAR(155) )" );
	}

	@AfterAll
	public static void teardown() {
		testApp.shutdown();
	}

	@BeforeEach
	public void setupEach() {
		ApplicationBoxContext appContext = new ApplicationBoxContext( testApp );
		appContext.setParent( instance.getRuntimeContext() );
		context		= new ScriptingRequestBoxContext( appContext );
		variables	= context.getScopeNearby( VariablesScope.name );

		assertDoesNotThrow( () -> {
			datasource.execute( "TRUNCATE TABLE developers" );
			datasource.execute( "INSERT INTO developers ( id, name, role ) VALUES ( 77, 'Michael Born', 'Developer' )" );
			datasource.execute( "INSERT INTO developers ( id, name, role ) VALUES ( 1, 'Luis Majano', 'CEO' )" );
			datasource.execute( "INSERT INTO developers ( id, name, role ) VALUES ( 42, 'Eric Peterson', 'Developer' )" );
		} );
	}

	@DisplayName( "Can compile a transaction component" )
	@Test
	public void testBasicTransaction() {
		instance.executeSource(
		    """
		    transaction{
		    	variables.result = queryExecute( "SELECT * FROM developers", {} );
		    }
		    """,
		    context );
		Query theResult = ( Query ) variables.get( result );
		assertEquals( 3, theResult.size() );
	}

	@DisplayName( "Throws on bad action level" )
	@Test
	public void testActionValidation() {
		assertDoesNotThrow( () -> instance.executeSource( "transaction action='commit';", context ) );

		BoxRuntimeException e = assertThrows( BoxRuntimeException.class, () -> instance.executeSource( "transaction action='foo'{}", context ) );

		assertTrue( e.getMessage().startsWith( "Record [action] for component [Transaction] must be one of the following values:" ) );
	}

	@DisplayName( "Throws on bad isolation level" )
	@Test
	public void testIsolationValidation() {
		assertDoesNotThrow( () -> instance.executeSource( "transaction isolation='read_committed'{}", context ) );

		BoxRuntimeException e = assertThrows( BoxRuntimeException.class, () -> instance.executeSource( "transaction isolation='foo'{}", context ) );

		assertTrue( e.getMessage().startsWith( "Record [isolation] for component [Transaction] must be one of the following values:" ) );
	}
}
