
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

package ortus.boxlang.runtime.jdbc;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.invoke.MethodHandles;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.application.Application;
import ortus.boxlang.runtime.context.ApplicationBoxContext;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Query;
import ortus.boxlang.runtime.types.Struct;
import tools.JDBCTestUtils;

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
		// @TODO: Test vendor-specific datasource settings!
		// , "derby.locks.deadlockTimeout", "3000"
		// , "derby.locks.waitTimeout", "6000"
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

		assertDoesNotThrow( () -> JDBCTestUtils.resetDevelopersTable( datasource ) );
	}

	@DisplayName( "Can commit a transaction" )
	@Test
	public void testCommit() {
		instance.executeSource(
		    """
		    transaction{
		        queryExecute( "INSERT INTO developers ( id, name, role ) VALUES ( 33, 'Jon Clausen', 'Developer' )", {} );
		        transactionCommit();
		        variables.result = queryExecute( "SELECT * FROM developers", {} );
		    }
		    """,
		    context );
		assertNotNull(
		    variables.getAsQuery( result )
		        .stream()
		        .filter( row -> row.getAsString( Key._NAME ).equals( "Jon Clausen" ) )
		        .findFirst()
		        .orElse( null )
		);
	}

	@DisplayName( "automatically commits at the end of a (successful) transaction" )
	@Test
	public void testCommitAtEnd() {
		instance.executeSource(
		    """
		    transaction{
		    	queryExecute( "INSERT INTO developers ( id, name, role ) VALUES ( 33, 'Jon Clausen', 'Developer' )", {} );
		    	try{
		    		throw( "I'm sorry, Dave. I'm afraid I can't do that." );
		    	} catch( any e ){
		    	// do nothing and see if the transaction is still committed.
		    	}
		    }
		    variables.result = queryExecute( "SELECT * FROM developers", {} );
		    """,
		    context );
		assertNotNull(
		    variables.getAsQuery( result )
		        .stream()
		        .filter( row -> row.getAsString( Key._NAME ).equals( "Jon Clausen" ) )
		        .findFirst()
		        .orElse( null )
		);
	}

	@DisplayName( "an exception will roll back the transaction" )
	@Test
	public void testExceptionRollback() {
		instance.executeSource(
		    """
		    	transaction{
		    		queryExecute( "INSERT INTO developers ( id, name, role ) VALUES ( 33, 'Jon Clausen', 'Developer' )", {} );
		    		transactionSetSavepoint( "foo" );
		    		throw( "I'm sorry, Dave. I'm afraid I can't do that." );
		    	}
		    	variables.result = queryExecute( "SELECT * FROM developers", {} );
		    """,
		    context );
		assertNull(
		    variables.getAsQuery( result )
		        .stream()
		        .filter( row -> row.getAsString( Key._NAME ).equals( "Jon Clausen" ) )
		        .findFirst()
		        .orElse( null )
		);
	}

	@DisplayName( "A return in the transaction body should commit and gracefully close the transaction, then release the connection to the connection pool." )
	@Test
	public void testReturnInsideBody() {
		instance.executeSource(
		    """
		    	function doInsert() {
		    		transaction{
		    			queryExecute( "INSERT INTO developers ( id, name, role ) VALUES ( 33, 'Jon Clausen', 'Developer' )", {} );
		    			return;
		    		}
		    	}
		    variables.result = queryExecute( "SELECT * FROM developers", {} );
		       """,
		    context );
		assertNull(
		    variables.getAsQuery( result )
		        .stream()
		        .filter( row -> row.getAsString( Key._NAME ).equals( "Jon Clausen" ) )
		        .findFirst()
		        .orElse( null )
		);
	}

	@DisplayName( "Can handle rollbacks" )
	@Test
	public void testRollback() {
		instance.executeSource(
		    """
		    transaction{
		     queryExecute( "INSERT INTO developers ( id, name, role ) VALUES ( 33, 'Jon Clausen', 'Developer' )", {} );
		     transactionRollback();
		    }
		    variables.result = queryExecute( "SELECT * FROM developers", {} );
		    """,
		    context );
		assertNull(
		    variables.getAsQuery( result )
		        .stream()
		        .filter( row -> row.getAsString( Key._NAME ).equals( "Jon Clausen" ) )
		        .findFirst()
		        .orElse( null )
		);
	}

	@DisplayName( "Commits persist despite rollbacks" )
	@Test
	public void testCommitWithRollback() {
		instance.executeSource(
		    """
		    transaction{
		        queryExecute( "INSERT INTO developers ( id, name, role ) VALUES ( 22, 'Brad Wood', 'Developer' )", {} );
		        transactionCommit();
		        queryExecute( "INSERT INTO developers ( id, name, role ) VALUES ( 33, 'Jon Clausen', 'Developer' )", {} );
		        transactionRollback();
		    }
		    variables.result = queryExecute( "SELECT * FROM developers", {} );
		    """,
		    context );
		Query theResult = variables.getAsQuery( result );

		// This insert should have been committed
		assertNotNull(
		    theResult
		        .stream()
		        .filter( row -> row.getAsString( Key._NAME ).equals( "Brad Wood" ) )
		        .findFirst()
		        .orElse( null )
		);

		// This insert should have been rolled back
		assertNull(
		    theResult
		        .stream()
		        .filter( row -> row.getAsString( Key._NAME ).equals( "Jon Clausen" ) )
		        .findFirst()
		        .orElse( null )
		);
	}

	@DisplayName( "Can roll back to named savepoint" )
	@Test
	public void testSavepoint() {
		instance.executeSource(
		    """
		    transaction{
		     queryExecute( "INSERT INTO developers ( id, name, role ) VALUES ( 33, 'Jon Clausen', 'Developer' )" );
		     transactionSetSavepoint( "savepoint1" );
		     queryExecute( "UPDATE developers SET name='Maxwell Smart' WHERE id=33" );
		     transactionRollback( "savepoint1" );
		    }
		    variables.result = queryExecute( "SELECT * FROM developers", {} );
		    """,
		    context );

		IStruct newRow = variables.getAsQuery( result )
		    .stream()
		    .filter( row -> row.getAsInteger( Key.id ) == 33 )
		    .findFirst()
		    .orElse( null );

		// the insert should not be rolled back
		assertNotNull( newRow );

		// the update should be rolled back
		assertEquals( "Jon Clausen", newRow.getAsString( Key._NAME ) );
	}

	@DisplayName( "Can roll back the entire transaction if no named savepoint is specified" )
	@Test
	public void testRollbackAllSavepoints() {
		instance.executeSource(
		    """
		    transaction{
		     queryExecute( "INSERT INTO developers ( id, name, role ) VALUES ( 33, 'Jon Clausen', 'Developer' )" );
		     transactionSetSavepoint( "savepoint1" );
		     queryExecute( "UPDATE developers SET name='Maxwell Smart' WHERE id=33" );
		     queryExecute( "INSERT INTO developers ( id, name, role ) VALUES ( 44, 'Maxwell Smart', 'Developer' )" );
		     transactionSetSavepoint( "savepoint2" );
		     transactionRollback();
		    }
		    variables.result = queryExecute( "SELECT * FROM developers", {} );
		    """,
		    context );

		// savepoint1 should be rolled back
		assertNull(
		    variables.getAsQuery( result )
		        .stream()
		        .filter( row -> row.getAsInteger( Key.id ) == 33 )
		        .findFirst()
		        .orElse( null )
		);

		// savepoint2 should be rolled back
		assertNull(
		    variables.getAsQuery( result )
		        .stream()
		        .filter( row -> row.getAsInteger( Key.id ) == 44 )
		        .findFirst()
		        .orElse( null )
		);
	}

	@DisplayName( "Can commit a transaction using action=commit" )
	@Test
	public void testActionEqualsCommit() {
		instance.executeSource(
		    """
		    transaction {
		        queryExecute( "INSERT INTO developers ( id, name, role ) VALUES ( 33, 'Jon Clausen', 'Developer' )", {} );
		        transaction action="commit";
		        variables.result = queryExecute( "SELECT * FROM developers", {} );
		    }
		    """,
		    context );
		assertNotNull(
		    variables.getAsQuery( result )
		        .stream()
		        .filter( row -> row.getAsString( Key._NAME ).equals( "Jon Clausen" ) )
		        .findFirst()
		        .orElse( null )
		);
	}

	@DisplayName( "Can set savepoint and rollback via action/savepoint attributes" )
	@Test
	public void testActionEqualsSetSavepoint() {
		instance.executeSource(
		    """
		    transaction{
		     queryExecute( "INSERT INTO developers ( id, name, role ) VALUES ( 33, 'Jon Clausen', 'Developer' )" );
		     transaction action="setsavepoint" savepoint="savepoint1";
		     queryExecute( "UPDATE developers SET name='Maxwell Smart' WHERE id=33" );
		     transaction action="rollback" savepoint="savepoint1";
		    }
		    variables.result = queryExecute( "SELECT * FROM developers", {} );
		    """,
		    context );

		IStruct newRow = variables.getAsQuery( result )
		    .stream()
		    .filter( row -> row.getAsInteger( Key.id ) == 33 )
		    .findFirst()
		    .orElse( null );

		// the insert should not be rolled back
		assertNotNull( newRow );

		// the update should be rolled back
		assertEquals( "Jon Clausen", newRow.getAsString( Key._NAME ) );
	}

	@Disabled( "Not implemented, but very important!" )
	@Test
	public void testCustomQueryDatasource() {
		instance.executeSource(
		    """
		    transaction{
		    	queryExecute( "INSERT INTO developers (id,name) VALUES (444, 'Angela' );", {}, { datasource : "myOtherDatasource" } );
		    	transactionRollback();
		    }
		    variables.result = queryExecute( "SELECT * FROM developers", {}, { datasource : "myOtherDatasource" } );
		    """,
		    context );

		// the insert should not be rolled back, since it's on a separate datasource
		assertNotNull(
		    variables.getAsQuery( result )
		        .stream()
		        .filter( row -> row.getAsInteger( Key.id ) == 444 )
		        .findFirst()
		        .orElse( null )
		);
	}

	@Disabled( "Not implemented" )
	@DisplayName( "Can handle nested transactions" )
	@Test
	public void testNestedTransaction() {
		instance.executeSource(
		    """
		    transaction{
		      queryExecute( "INSERT INTO developers ( id, name, role ) VALUES ( 22, 'Brad Wood', 'Developer' )", {} );
		      transaction{
		    	  queryExecute( "INSERT INTO developers ( id, name, role ) VALUES ( 33, 'Jon Clausen', 'Developer' )", {} );
		    	  // can roll back inner transaction WITHOUT rolling back outer transaction
		    	  transactionRollback();
		      }
		    }
		    variables.result = queryExecute( "SELECT * FROM developers", {} );
		    """,
		    context );
		Query theResult = variables.getAsQuery( result );

		// This insert from the outer transaction should have been committed
		assertNotNull(
		    theResult
		        .stream()
		        .filter( row -> row.getAsString( Key._NAME ).equals( "Brad Wood" ) )
		        .findFirst()
		        .orElse( null )
		);

		// This insert from the inner transaction should have been rolled back
		assertNull(
		    theResult
		        .stream()
		        .filter( row -> row.getAsString( Key._NAME ).equals( "Jon Clausen" ) )
		        .findFirst()
		        .orElse( null )
		);
	}
}
