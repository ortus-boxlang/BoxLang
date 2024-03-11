
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

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
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
import ortus.boxlang.runtime.types.Query;
import ortus.boxlang.runtime.types.Struct;

public class TransactionTest {

	static DataSourceManager	datasourceManager;
	static DataSource			datasource;
	static BoxRuntime			instance;
	IBoxContext					context;
	IScope						variables;
	static Key					result	= new Key( "result" );
	static DataSource			MySQLDataSource;

	@BeforeAll
	public static void setUp() {
		instance			= BoxRuntime.getInstance( true );
		datasourceManager	= DataSourceManager.getInstance();
		datasource			= new DataSource( Struct.of(
		    "jdbcUrl", "jdbc:derby:memory:testDatasourceDB;create=true"
		) );

		// Transactions generally assume a default datasource set at the application level.
		datasourceManager.setDefaultDataSource( datasource );
		datasource.execute( "CREATE TABLE developers ( id INTEGER, name VARCHAR(155), role VARCHAR(155) )" );
	}

	@AfterAll
	public static void teardown() {
		datasourceManager.shutdown();
	}

	@BeforeEach
	public void setupEach() {
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
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

	@Disabled( "Not implemented" )
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

	@Disabled( "Not implemented" )
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
