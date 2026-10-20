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
 * distributed under the License is distribu ted on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ortus.boxlang.runtime.jdbc;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.DatabaseException;
import tools.JDBCTestUtils;

public class ConnectionManagerTest {

	static BoxRuntime	instance;
	IBoxContext			context;
	IScope				variables;
	static Key			result	= new Key( "result" );
	ConnectionManager	manager;

	@BeforeAll
	static void setUp() {
		instance = BoxRuntime.getInstance( true );
	}

	@AfterAll
	static void tearDown() {
		instance.getConfiguration().defaultDatasource = "";
	}

	@BeforeEach
	void setupEach() {
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
		manager		= new ConnectionManager( context );
	}

	@DisplayName( "It can create a connection manager" )
	@Test
	public void testCreateConnectionManager() {
		assertThat( manager ).isNotNull();
	}

	@DisplayName( "It can get the default datasource" )
	@Test
	public void testGetDefaultDatasource() {
		DataSource datasource = manager.getDefaultDatasource();
		assertThat( datasource ).isNull();

		// Set the default datasource
		manager.setDefaultDatasource(
		    JDBCTestUtils.buildDatasource( "test" )
		);
		// Now get it
		assertThat( manager.hasDefaultDatasource() ).isTrue();
		assertThat( manager.getDefaultDatasource() ).isNotNull();
		assertThat( manager.getDefaultDatasource().getUniqueName().getName() ).contains( "test" );
	}

	@DisplayName( "It can get the default datasource with a context override" )
	@Test
	public void testGetDefaultDatasourceWithContextOverride() {

		// Mock a context override for default datasource
		instance.getConfiguration().defaultDatasource = "override";
		instance.getConfiguration().datasources.put(
		    Key.of( "override" ),
		    JDBCTestUtils.buildDatasourceConfig( "override" )
		);

		// Get the default datasource
		DataSource datasource = manager.getDefaultDatasource();

		assertThat( datasource ).isNotNull();
		assertThat( datasource.getUniqueName().getName() ).contains( "override" );
	}

	@Disabled( "Weird lifecycle issue where this fails when run with other tests, but passes when run solo." )
	@DisplayName( "It throws if the referenced default datasource is not defined" )
	@Test
	public void testGetDefaultDatasourceReferenceThrow() {

		// Mock a context override for default datasource
		instance.getConfiguration().defaultDatasource = "testing123";
		instance.getConfiguration().datasources.put(
		    Key.of( "foo" ),
		    JDBCTestUtils.buildDatasourceConfig( "foo" )
		);

		DatabaseException e = assertThrows( DatabaseException.class, () -> {
			DataSource datasource = manager.getDefaultDatasource();
		} );
		assertThat( e ).isInstanceOf( DatabaseException.class );
		assertThat( e.getMessage() ).contains( "Default datasource [testing123] not found" );
		assertThat( e.getMessage() ).contains( "Registered datasources are: [foo]" );
	}

	@DisplayName( "It can get a datasource by name" )
	@Test
	public void testGetDatasourceByName() {

		// Set up a datasource
		instance.getConfiguration().datasources.put(
		    Key.of( "bdd" ),
		    JDBCTestUtils.buildDatasourceConfig( "bdd" )
		);

		// Get the datasource
		DataSource datasource = manager.getDatasource( Key.of( "bdd" ) );
		assertThat( datasource ).isNotNull();
		assertThat( datasource.getUniqueName().getName() ).contains( "bdd" );
		assertThat( manager.hasCachedDatasource( Key.of( "bdd" ) ) ).isTrue();
	}

	@DisplayName( "It will return null for a non-existent datasource" )
	@Test
	public void testGetNonExistentDatasource() {

		// Get the datasource
		DataSource datasource = manager.getDatasource( Key.of( "nonexistent" ) );
		assertThat( datasource ).isNull();
	}

	@DisplayName( "It can get a datasource on the fly" )
	@Test
	public void testGetDatasourceOnTheFly() {

		// Get the datasource
		DataSource datasource = manager.getOnTheFlyDataSource( Struct.of(
		    "driver", "derby",
		    "database", "myDB",
		    "connectionString", "jdbc:derby:memory:myDB;create=true"
		) );
		assertThat( datasource ).isNotNull();
		assertThat( datasource.getConfiguration().isOnTheFly() ).isTrue();
	}

	@DisplayName( "It will throw an exception for an on the fly missing a driver" )
	@Test
	public void testGetDatasourceOnTheFlyMissingDriver() {
		// Get the datasource
		try {
			manager.getOnTheFlyDataSource( Struct.of(
			    "host", "127.0.0.1"
			) );
		} catch ( Exception e ) {
			assertThat( e ).isInstanceOf( IllegalArgumentException.class );
			assertThat( e.getMessage() )
			    .contains( "Datasource configuration must contain a 'driver'" );
		}
	}

	@DisplayName( "It will get a datasource on the fly using 'type' instead of driver" )
	@Test
	public void testGetDatasourceOnTheFlyUsingType() {
		// Get the datasource
		DataSource datasource = manager.getOnTheFlyDataSource( Struct.of(
		    "type", "derby",
		    "database", "myDB",
		    "connectionString", "jdbc:derby:memory:myDB;create=true"
		) );
		assertThat( datasource ).isNotNull();
		assertThat( datasource.getConfiguration().isOnTheFly() ).isTrue();
	}

	@DisplayName( "It can register a datasource of key name and properties struct" )
	@Test
	public void testRegisterKeyName() {
		// Get the datasource
		IStruct		props		= Struct.of(
		    "type", "derby",
		    "database", "myDB",
		    "connectionString", "jdbc:derby:memory:myDB;create=true"
		);
		DataSource	datasource	= manager.register( Key.of( "KeyNameTest" ), props );
		assertThat( datasource ).isNotNull();
		String dsName = datasource.getUniqueName().toString();
		assertTrue( dsName.contains( "_KeyNameTest" ) );
		assertFalse( dsName.contains( "unnamed_" ) );
	}

	@DisplayName( "Connection Handling: a connection acquired for a datasource outside the transaction is released back to the pool" )
	@Test
	public void testNonTransactionalConnectionReleasedInsideTransaction() {
		DataSource	dsA	= JDBCTestUtils.buildDatasource( "txnDsA_" + UUID.randomUUID() );
		DataSource	dsB	= JDBCTestUtils.buildDatasource( "txnDsB_" + UUID.randomUUID() );

		try {
			manager.beginTransaction( dsA );

			// A query against a different datasource gets its own pooled connection...
			BoxConnection connection = manager.getBoxConnection( dsB );
			assertThat( dsB.getPoolStats().getAsInteger( Key.activeConnections ) ).isEqualTo( 1 );

			// ... and that connection must go back to the pool even though a transaction is in flight
			assertThat( manager.releaseConnection( connection ) ).isTrue();
			assertThat( dsB.getPoolStats().getAsInteger( Key.activeConnections ) ).isEqualTo( 0 );

			// The transaction's own connection must still be left alone for the transaction to manage
			BoxConnection transactionConnection = manager.getBoxConnection( dsA );
			assertThat( dsA.getPoolStats().getAsInteger( Key.activeConnections ) ).isEqualTo( 1 );
			assertThat( manager.releaseConnection( transactionConnection ) ).isFalse();
			assertThat( dsA.getPoolStats().getAsInteger( Key.activeConnections ) ).isEqualTo( 1 );

			// ... and released once the transaction ends
			manager.endTransaction();
			assertThat( dsA.getPoolStats().getAsInteger( Key.activeConnections ) ).isEqualTo( 0 );
		} finally {
			dsA.shutdown();
			dsB.shutdown();
		}
	}

	@DisplayName( "Connection Handling: a failure ending a child transaction does not abandon the parent transaction" )
	@Test
	public void testShutdownContinuesAfterChildTransactionFailure() throws Exception {
		// A parent transaction whose savepoint creation - and thus the child's end() - always fails
		ITransaction parent = Mockito.mock( ITransaction.class );
		Mockito.when( parent.setSavepoint( Mockito.any() ) ).thenThrow( new DatabaseException( "Simulated child transaction failure" ) );
		ChildTransaction		child				= new ChildTransaction( parent );

		// beginTransaction() cannot produce a failing child, so inject one directly
		java.lang.reflect.Field	transactionField	= ConnectionManager.class.getDeclaredField( "transaction" );
		transactionField.setAccessible( true );
		transactionField.set( manager, child );

		// Shutting down must not throw, and must unwind the child AND the remaining parent transaction
		manager.shutdown();

		assertThat( manager.isInTransaction() ).isFalse();
	}

}
