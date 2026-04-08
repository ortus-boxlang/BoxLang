package ortus.boxlang.runtime.jdbc.drivers;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.sql.SQLException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.params.ParameterizedTest;

import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.jdbc.DataSource;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Query;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import tools.JDBCTestUtils;

@EnabledIf( "tools.JDBCTestUtils#hasMSSQLModule" )
public class MSSQLDriverTransactionTest extends AbstractDriverTest {

	public static DataSource	mssqlDatasource;

	protected static Key		datasourceName		= Key.of( "MSSQLdatasource" );

	protected static IStruct	datasourceConfig	= Struct.of(
	    "username", "sa",
	    "password", "123456Password",
	    "host", "localhost",
	    "port", "1433",
	    "driver", "mssql",
	    "database", "master"
	);

	@BeforeAll
	public static void setUp() {
		instance = BoxRuntime.getInstance( true );
		IBoxContext setUpContext = new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		mssqlDatasource = AbstractDriverTest.setupTestDatasource( instance, setUpContext, datasourceName, datasourceConfig );
		MSSQLDriverTest.createGeneratedKeyTable( mssqlDatasource, setUpContext );
	}

	@BeforeEach
	public void setupEach() {
		context = new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		context.getConnectionManager().setDefaultDatasource( mssqlDatasource );
		variables = context.getScopeNearby( VariablesScope.name );
		assertDoesNotThrow( () -> JDBCTestUtils.resetDevelopersTable( mssqlDatasource, context ) );
		// Clear the caches
		instance.getCacheService().getDefaultCache().clearAll();
	}

	@AfterAll
	public static void teardown() throws SQLException {
		IBoxContext tearDownContext = new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		AbstractDriverTest.teardownTestDatasource( tearDownContext, mssqlDatasource );
	}

	/**
	 * Override to provide driver-specific datasource name
	 */
	@Override
	String getDatasourceName() {
		return "MSSQLdatasource";
	}

	@DisplayName( "It sets generatedKey in query meta" )
	@Test
	public void testGeneratedKey() {
	}

	@ParameterizedTest
	@org.junit.jupiter.params.provider.ValueSource( strings = {
	    "read_uncommitted",
	    "read_committed",
	    "repeatable_read",
	    "serializable"
	} )
	@DisplayName( "Can specify isolation levels" )
	public void testSetIsolationLevel( String isolationLevel ) {
		getInstance().executeSource(
		    String.format(
		        """
		        transaction isolation="%s" {
		            queryExecute( "INSERT INTO developers ( id, name, role ) VALUES ( 33, 'Jon Clausen', 'Developer' )", {}, { datasource="MSSQLdatasource" } );
		            transactionCommit();
		            variables.result = queryExecute( "SELECT * FROM developers", {}, { datasource="MSSQLdatasource" } );
		        }
		        """, isolationLevel ),
		    getContext() );
		assertNotNull(
		    getVariables().getAsQuery( result )
		        .stream()
		        .filter( row -> row.getAsString( Key._NAME ).equals( "Jon Clausen" ) )
		        .findFirst()
		        .orElse( null )
		);
	}

	@DisplayName( "Setting an invalid isolation will throw an error" )
	@Test
	public void testInvalidIsolationLevel() {
		assertThrows( BoxRuntimeException.class, () -> {
			getInstance().executeSource(
			    """
			    transaction isolation="foo" {
			    	queryExecute( "INSERT INTO developers ( id, name, role ) VALUES ( 33, 'Jon Clausen', 'Developer' )", {}, { datasource="MSSQLdatasource" } );
			    }
			    """,
			    getContext() );
		} );
	}

	@DisplayName( "Connection Handling: the connection is properly released even if the transaction errors" )
	@Test
	public void testErroredTransactionConnectionClose() {
		Integer activePreTransaction = getDatasource().getPoolStats().getAsInteger( Key.of( "activeConnections" ) );
		assertThrows( BoxRuntimeException.class, () -> {
			getInstance().executeSource(
			    """
			    transaction isolation="foo" {
			    	queryExecute( "INSERT INTO countries ( id, name ) VALUES ( 1, 'This table doesn't exist!' )", {}, { datasource="MSSQLdatasource" } );
			    }
			    """,
			    getContext() );
		} );

		Integer activePostTransaction = getDatasource().getPoolStats().getAsInteger( Key.of( "activeConnections" ) );
		assertThat( activePostTransaction ).isEqualTo( activePreTransaction );
	}

	@DisplayName( "Connection Handling: the connection is properly released even if there's a return statement." )
	@Test
	public void testTransactionConnectionCloseWithReturn() {
		Integer activePreTransaction = getDatasource().getPoolStats().getAsInteger( Key.of( "activeConnections" ) );
		getInstance().executeSource(
		    """
		    transaction {
		    	queryExecute( "INSERT INTO developers ( id, name, role ) VALUES ( 33, 'Jon Clausen', 'Developer' )", {}, { datasource="MSSQLdatasource" } );
		    	return;
		    }
		    """,
		    getContext() );

		Integer activePostTransaction = getDatasource().getPoolStats().getAsInteger( Key.of( "activeConnections" ) );
		assertThat( activePostTransaction ).isEqualTo( activePreTransaction );
	}

	@DisplayName( "Connection Handling: the connection is properly released even if there's a break tag." )
	@Test
	public void testTransactionConnectionCloseWithBreakTag() {
		Integer activePreTransaction = getDatasource().getPoolStats().getAsInteger( Key.of( "activeConnections" ) );
		getInstance().executeSource(
		    """
		    transaction {
		    	do{
		    		queryExecute( "INSERT INTO developers ( id, name, role ) VALUES ( 33, 'Jon Clausen', 'Developer' )", {}, { datasource="MSSQLdatasource" } );
		    		break;
		    	} while( false );
		    }
		    """,
		    getContext() );

		Integer activePostTransaction = getDatasource().getPoolStats().getAsInteger( Key.of( "activeConnections" ) );
		assertThat( activePostTransaction ).isEqualTo( activePreTransaction );
	}

	@DisplayName( "Can commit a transaction" )
	@Test
	public void testCommit() {
		getInstance().executeSource(
		    """
		    transaction{
		        queryExecute( "INSERT INTO developers ( id, name, role ) VALUES ( 33, 'Jon Clausen', 'Developer' )", {}, { datasource="MSSQLdatasource" } );
		        transactionCommit();
		        variables.result = queryExecute( "SELECT * FROM developers", {}, { datasource="MSSQLdatasource" } );
		    }
		    """,
		    getContext() );
		assertNotNull(
		    getVariables().getAsQuery( result )
		        .stream()
		        .filter( row -> row.getAsString( Key._NAME ).equals( "Jon Clausen" ) )
		        .findFirst()
		        .orElse( null )
		);
	}

	@DisplayName( "tests that a thrown exception will not be mutated" )
	@Test
	public void testThrownExceptionNotMutated() {
		getInstance().executeSource(
		    """
		    try{
		    	transaction{
		    		queryExecute( "SELECT * FROM developers", {}, { datasource="MSSQLdatasource" } )
		    		throw( type="FooException", message="foo", extendedInfo="bar" );
		    	}
		    } catch( any e ){
		    	result = e.extendedInfo;
		    }
		       """,
		    getContext() );
		assertThat( getVariables().get( Key.of( "result" ) ) ).isEqualTo( "bar" );
	}

	@DisplayName( "automatically commits at the end of a (successful) transaction" )
	@Test
	public void testCommitAtEnd() {
		getInstance().executeSource(
		    """
		    transaction{
		    	queryExecute( "INSERT INTO developers ( id, name, role ) VALUES ( 33, 'Jon Clausen', 'Developer' )", {}, { datasource="MSSQLdatasource" } );
		    	try{
		    		throw( "I'm sorry, Dave. I'm afraid I can't do that." );
		    	} catch( any e ){
		    	// do nothing and see if the transaction is still committed.
		    	}
		    }
		    variables.result = queryExecute( "SELECT * FROM developers", {}, { datasource="MSSQLdatasource" } );
		    """,
		    getContext() );
		assertNotNull(
		    getVariables().getAsQuery( result )
		        .stream()
		        .filter( row -> row.getAsString( Key._NAME ).equals( "Jon Clausen" ) )
		        .findFirst()
		        .orElse( null )
		);
	}

	@DisplayName( "an exception will roll back the transaction" )
	@Test
	public void testExceptionRollback() {
		getInstance().executeSource(
		    """
		    	try{
		    		transaction{
		    			queryExecute( "INSERT INTO developers ( id, name, role ) VALUES ( 33, 'Jon Clausen', 'Developer' )", {}, { datasource="MSSQLdatasource" } );
		    			transactionSetSavepoint( "foo" );
		    			throw( "I'm sorry, Dave. I'm afraid I can't do that." );
		    		}
		    	} catch( any e ){}
		    	variables.result = queryExecute( "SELECT * FROM developers", {}, { datasource="MSSQLdatasource" } );
		    """,
		    getContext() );
		assertNull(
		    getVariables().getAsQuery( result )
		        .stream()
		        .filter( row -> row.getAsString( Key._NAME ).equals( "Jon Clausen" ) )
		        .findFirst()
		        .orElse( null )
		);
	}

	@DisplayName( "A return in the transaction body should commit and gracefully close the transaction, then release the connection to the connection pool." )
	@Test
	public void testReturnInsideBody() {
		getInstance().executeSource(
		    """
		    	function doInsert() {
		    		transaction{
		    			queryExecute( "INSERT INTO developers ( id, name, role ) VALUES ( 33, 'Jon Clausen', 'Developer' )", {}, { datasource="MSSQLdatasource" } );
		    			return;
		    		}
		    	}
		    variables.result = queryExecute( "SELECT * FROM developers", {}, { datasource="MSSQLdatasource" } );
		       """,
		    getContext() );
		assertNull(
		    getVariables().getAsQuery( result )
		        .stream()
		        .filter( row -> row.getAsString( Key._NAME ).equals( "Jon Clausen" ) )
		        .findFirst()
		        .orElse( null )
		);
	}

	@DisplayName( "Can handle rollbacks" )
	@Test
	public void testRollback() {
		getInstance().executeSource(
		    """
		    transaction{
		     queryExecute( "INSERT INTO developers ( id, name, role ) VALUES ( 33, 'Jon Clausen', 'Developer' )", {}, { datasource="MSSQLdatasource" } );
		     transactionRollback();
		    }
		    variables.result = queryExecute( "SELECT * FROM developers", {}, { datasource="MSSQLdatasource" } );
		    """,
		    getContext() );
		assertNull(
		    getVariables().getAsQuery( result )
		        .stream()
		        .filter( row -> row.getAsString( Key._NAME ).equals( "Jon Clausen" ) )
		        .findFirst()
		        .orElse( null )
		);
	}

	@DisplayName( "Commits persist despite rollbacks" )
	@Test
	public void testCommitWithRollback() {
		getInstance().executeSource(
		    """
		    transaction{
		        queryExecute( "INSERT INTO developers ( id, name, role ) VALUES ( 22, 'Brad Wood', 'Developer' )", {}, 	{ datasource="MSSQLdatasource" } );
		        transactionCommit();
		        queryExecute( "INSERT INTO developers ( id, name, role ) VALUES ( 33, 'Jon Clausen', 'Developer' )", {}, { datasource="MSSQLdatasource" } );
		        transactionRollback();
		    }
		    variables.result = queryExecute( "SELECT * FROM developers", {}, { datasource="MSSQLdatasource" } );
		    """,
		    getContext() );
		Query theResult = getVariables().getAsQuery( result );

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
		getInstance().executeSource(
		    """
		    transaction{
		     queryExecute( "INSERT INTO developers ( id, name, role ) VALUES ( 33, 'Jon Clausen', 'Developer' )", {}, { datasource="MSSQLdatasource" } );
		     transactionSetSavepoint( "savepoint1" );
		     queryExecute( "UPDATE developers SET name='Maxwell Smart' WHERE id=33", {}, { datasource="MSSQLdatasource" } );
		     transactionRollback( "savepoint1" );
		    }
		    variables.result = queryExecute( "SELECT * FROM developers", {}, { datasource="MSSQLdatasource" } );
		    """,
		    getContext() );

		IStruct newRow = getVariables().getAsQuery( result )
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
		getInstance().executeSource(
		    """
		    transaction{
		     queryExecute( "INSERT INTO developers ( id, name, role ) VALUES ( 33, 'Jon Clausen', 'Developer' )", {}, { datasource="MSSQLdatasource" } );
		     transactionSetSavepoint( "savepoint1" );
		     queryExecute( "UPDATE developers SET name='Maxwell Smart' WHERE id=33", {}, { datasource="MSSQLdatasource" } );
		     queryExecute( "INSERT INTO developers ( id, name, role ) VALUES ( 44, 'Maxwell Smart', 'Developer' )", {}, { datasource="MSSQLdatasource" } );
		     transactionSetSavepoint( "savepoint2" );
		     transactionRollback();
		    }
		    variables.result = queryExecute( "SELECT * FROM developers", {}, { datasource="MSSQLdatasource" } );
		    """,
		    getContext() );

		// savepoint1 should be rolled back
		assertNull(
		    getVariables().getAsQuery( result )
		        .stream()
		        .filter( row -> row.getAsInteger( Key.id ) == 33 )
		        .findFirst()
		        .orElse( null )
		);

		// savepoint2 should be rolled back
		assertNull(
		    getVariables().getAsQuery( result )
		        .stream()
		        .filter( row -> row.getAsInteger( Key.id ) == 44 )
		        .findFirst()
		        .orElse( null )
		);
	}

	@DisplayName( "Can commit a transaction using action=commit" )
	@Test
	public void testActionEqualsCommit() {
		getInstance().executeSource(
		    """
		    transaction {
		        queryExecute( "INSERT INTO developers ( id, name, role ) VALUES ( 33, 'Jon Clausen', 'Developer' )", {}, { datasource="MSSQLdatasource" } );
		        transaction action="commit";
		        variables.result = queryExecute( "SELECT * FROM developers", {}, { datasource="MSSQLdatasource" } );
		    }
		    """,
		    getContext() );
		assertNotNull(
		    getVariables().getAsQuery( result )
		        .stream()
		        .filter( row -> row.getAsString( Key._NAME ).equals( "Jon Clausen" ) )
		        .findFirst()
		        .orElse( null )
		);
	}

	@DisplayName( "Can commit a transaction in tag syntax" )
	@Test
	public void testTransactionTagSyntax() {
		getInstance().executeSource(
		    """
		    <cftransaction>
		        <cfquery datasource="MSSQLdatasource">
		            INSERT INTO developers ( id, name,role )
		            VALUES ( <cfqueryparam value="33">, <cfqueryparam value="Jon Clausen">, <cfqueryparam value="Developer"> )
		        </cfquery>
		        <cftransaction action="commit" />
		        <cfset variables.result = queryExecute( "SELECT * FROM developers", {}, { datasource="MSSQLdatasource" } ) />
		    </cftransaction>
		    """,
		    getContext(), BoxSourceType.CFTEMPLATE );
		assertNotNull(
		    getVariables().getAsQuery( result )
		        .stream()
		        .filter( row -> row.getAsString( Key._NAME ).equals( "Jon Clausen" ) )
		        .findFirst()
		        .orElse( null )
		);
	}

	@DisplayName( "Can set savepoint and rollback via action/savepoint attributes" )
	@Test
	public void testActionEqualsSetSavepoint() {
		getInstance().executeSource(
		    """
		    transaction{
		     queryExecute( "INSERT INTO developers ( id, name, role ) VALUES ( 33, 'Jon Clausen', 'Developer' )", {}, { datasource="MSSQLdatasource" } );
		     transaction action="setsavepoint" savepoint="savepoint1";
		     queryExecute( "UPDATE developers SET name='Maxwell Smart' WHERE id=33", {}, { datasource="MSSQLdatasource" } );
		     transaction action="rollback" savepoint="savepoint1";
		    }
		    variables.result = queryExecute( "SELECT * FROM developers", {}, { datasource="MSSQLdatasource" } );
		    """,
		    getContext() );

		IStruct newRow = getVariables().getAsQuery( result )
		    .stream()
		    .filter( row -> row.getAsInteger( Key.id ) == 33 )
		    .findFirst()
		    .orElse( null );

		// the insert should not be rolled back
		assertNotNull( newRow );

		// the update should be rolled back
		assertEquals( "Jon Clausen", newRow.getAsString( Key._NAME ) );
	}

	@DisplayName( "Nested transactions: the connection is properly released on parent transaction end" )
	@Test
	public void testNestedTransactionConnectionClose() {
		Integer activePreTransaction = getDatasource().getPoolStats().getAsInteger( Key.of( "activeConnections" ) );
		getInstance().executeSource(
		    """
		    transaction{
		      queryExecute( "INSERT INTO developers ( id, name, role ) VALUES ( 22, 'Brad Wood', 'Developer' )", {}, { datasource="MSSQLdatasource" } );
		      transaction{
		        queryExecute( "INSERT INTO developers ( id, name, role ) VALUES ( 33, 'Jon Clausen', 'Developer' )", {}, { datasource="MSSQLdatasource" } );
		      }
		    }
		    """,
		    getContext() );

		Integer activePostTransaction = getDatasource().getPoolStats().getAsInteger( Key.of( "activeConnections" ) );
		assertThat( activePostTransaction ).isEqualTo( activePreTransaction );
	}

	@DisplayName( "Nested transactions: A rollback on the child will not roll back the parent" )
	@Test
	public void testChildRollback() {
		getInstance().executeSource(
		    """
		    transaction{
		      queryExecute( "INSERT INTO developers ( id, name, role ) VALUES ( 22, 'Brad Wood', 'Developer' )", {}, { datasource="MSSQLdatasource" } );
		      transaction{
		        queryExecute( "INSERT INTO developers ( id, name, role ) VALUES ( 33, 'Jon Clausen', 'Developer' )", {}, { datasource="MSSQLdatasource" } );
		        transactionRollback();
		      }
		    }
		    variables.result = queryExecute( "SELECT * FROM developers", {}, { datasource="MSSQLdatasource" } );
		    """,
		    getContext() );
		Query theResult = getVariables().getAsQuery( result );

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

	@DisplayName( "Nested transactions: A rollback on the parent will roll back the child" )
	@Test
	public void testParentRollback() {
		getInstance().executeSource(
		    """
		      transaction{
		    queryExecute( "INSERT INTO developers ( id, name, role ) VALUES ( 22, 'Brad Wood', 'Developer' )", {}, { datasource="MSSQLdatasource" } );
		    transaction{
		    	queryExecute( "INSERT INTO developers ( id, name, role ) VALUES ( 33, 'Jon Clausen', 'Developer' )", {}, { datasource="MSSQLdatasource" } );
		    }
		    // will roll back both the parent and child transactions
		    transactionRollback();
		      }
		      variables.result = queryExecute( "SELECT * FROM developers", {}, { datasource="MSSQLdatasource" } );
		      """,
		    getContext() );
		Query theResult = getVariables().getAsQuery( result );

		// This insert from the outer transaction should be rolled back
		assertNull(
		    theResult
		        .stream()
		        .filter( row -> row.getAsString( Key._NAME ).equals( "Brad Wood" ) )
		        .findFirst()
		        .orElse( null )
		);

		// This insert from the inner transaction should also be rolled back
		assertNull(
		    theResult
		        .stream()
		        .filter( row -> row.getAsString( Key._NAME ).equals( "Jon Clausen" ) )
		        .findFirst()
		        .orElse( null )
		);
	}

	@DisplayName( "Nested transactions: Savepoints do not collide between the parent and child" )
	@Test
	public void testNestedSavepointCollisions() {
		getInstance().executeSource(
		    """
		        transaction{
		            queryExecute( "INSERT INTO developers ( id, name, role ) VALUES ( 22, 'Brad Wood', 'Developer' )", {}, { datasource="MSSQLdatasource" } );
		            transactionSetSavepoint( 'foo' );
		            transaction{
		            	transactionSetSavepoint( 'foo' );
		            	queryExecute( "INSERT INTO developers ( id, name, role ) VALUES ( 33, 'Jon Clausen', 'Developer' )", {}, { datasource="MSSQLdatasource" } );
		            	transactionRollback( 'foo' );
		            }
		        }
		        variables.result = queryExecute( "SELECT * FROM developers", {}, { datasource="MSSQLdatasource" } );
		    """,
		    getContext() );
		Query theResult = getVariables().getAsQuery( result );

		// This insert from the outer transaction should NOT be rolled back
		assertNotNull(
		    theResult
		        .stream()
		        .filter( row -> row.getAsString( Key._NAME ).equals( "Brad Wood" ) )
		        .findFirst()
		        .orElse( null )
		);
		// This insert from the inner transaction should be rolled back
		assertNull(
		    theResult
		        .stream()
		        .filter( row -> row.getAsString( Key._NAME ).equals( "Jon Clausen" ) )
		        .findFirst()
		        .orElse( null )
		);
	}

	// @Disabled( "Fails due to savepoint not existing. More testing to do here." )
	@DisplayName( "Nested transactions: Won't throw 'savepoint name too long' on 4+ level transaction savepoints" )
	@Test
	public void testHighlyNestedSavepoints() {
		getInstance().executeSource(
		    """
		    transaction{
		    	transaction{
		    		transaction{
		    			transaction{
		    				queryExecute( "INSERT INTO developers ( id, name, role ) VALUES ( 22, 'Brad Wood', 'Developer' )", {}, { datasource="MSSQLdatasource" } );
		    				transactionCommit();
		    			}
		    		}
		    	}
		    }
		    variables.result = queryExecute( "SELECT * FROM developers", {}, { datasource="MSSQLdatasource" } );
		    """,
		    getContext() );
		Query theResult = getVariables().getAsQuery( result );

		// This row from the inner transaction should exist
		assertNotNull(
		    theResult
		        .stream()
		        .filter( row -> row.getAsString( Key._NAME ).equals( "Brad Wood" ) )
		        .findFirst()
		        .orElse( null )
		);
	}

	@DisplayName( "Nested transactions: Can set transaction datasource / execute query from child transaction" )
	@Test
	public void testNestedTransactionDatasource() {
		getInstance().executeSource(
		    """
		        transaction{
		            transaction{
		                queryExecute( "INSERT INTO developers ( id, name, role ) VALUES ( 22, 'Brad Wood', 'Developer' )", {}, { datasource="MSSQLdatasource" } );
		            }
		        }
		        variables.result = queryExecute( "SELECT * FROM developers", {}, { datasource="MSSQLdatasource" } );
		    """,
		    getContext() );
		Query theResult = getVariables().getAsQuery( result );

		// This row from the inner transaction should exist
		assertNotNull(
		    theResult
		        .stream()
		        .filter( row -> row.getAsString( Key._NAME ).equals( "Brad Wood" ) )
		        .findFirst()
		        .orElse( null )
		);
	}

}
