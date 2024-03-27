
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.parser.BoxSourceType;
import ortus.boxlang.runtime.bifs.global.jdbc.BaseJDBCTest;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Query;

public class TransactionTest extends BaseJDBCTest {

	static Key result = new Key( "result" );

	@DisplayName( "Can commit a transaction" )
	@Test
	public void testCommit() {
		getInstance().executeSource(
		    """
		    transaction{
		        queryExecute( "INSERT INTO developers ( id, name, role ) VALUES ( 33, 'Jon Clausen', 'Developer' )", {} );
		        transactionCommit();
		        variables.result = queryExecute( "SELECT * FROM developers", {} );
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

	@DisplayName( "automatically commits at the end of a (successful) transaction" )
	@Test
	public void testCommitAtEnd() {
		getInstance().executeSource(
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
		    			queryExecute( "INSERT INTO developers ( id, name, role ) VALUES ( 33, 'Jon Clausen', 'Developer' )", {} );
		    			transactionSetSavepoint( "foo" );
		    			throw( "I'm sorry, Dave. I'm afraid I can't do that." );
		    		}
		    	} catch( any e ){}
		    	variables.result = queryExecute( "SELECT * FROM developers", {} );
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
		    			queryExecute( "INSERT INTO developers ( id, name, role ) VALUES ( 33, 'Jon Clausen', 'Developer' )", {} );
		    			return;
		    		}
		    	}
		    variables.result = queryExecute( "SELECT * FROM developers", {} );
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
		     queryExecute( "INSERT INTO developers ( id, name, role ) VALUES ( 33, 'Jon Clausen', 'Developer' )", {} );
		     transactionRollback();
		    }
		    variables.result = queryExecute( "SELECT * FROM developers", {} );
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
		        queryExecute( "INSERT INTO developers ( id, name, role ) VALUES ( 22, 'Brad Wood', 'Developer' )", {} );
		        transactionCommit();
		        queryExecute( "INSERT INTO developers ( id, name, role ) VALUES ( 33, 'Jon Clausen', 'Developer' )", {} );
		        transactionRollback();
		    }
		    variables.result = queryExecute( "SELECT * FROM developers", {} );
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
		     queryExecute( "INSERT INTO developers ( id, name, role ) VALUES ( 33, 'Jon Clausen', 'Developer' )" );
		     transactionSetSavepoint( "savepoint1" );
		     queryExecute( "UPDATE developers SET name='Maxwell Smart' WHERE id=33" );
		     transactionRollback( "savepoint1" );
		    }
		    variables.result = queryExecute( "SELECT * FROM developers", {} );
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
		     queryExecute( "INSERT INTO developers ( id, name, role ) VALUES ( 33, 'Jon Clausen', 'Developer' )" );
		     transactionSetSavepoint( "savepoint1" );
		     queryExecute( "UPDATE developers SET name='Maxwell Smart' WHERE id=33" );
		     queryExecute( "INSERT INTO developers ( id, name, role ) VALUES ( 44, 'Maxwell Smart', 'Developer' )" );
		     transactionSetSavepoint( "savepoint2" );
		     transactionRollback();
		    }
		    variables.result = queryExecute( "SELECT * FROM developers", {} );
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
		        queryExecute( "INSERT INTO developers ( id, name, role ) VALUES ( 33, 'Jon Clausen', 'Developer' )", {} );
		        transaction action="commit";
		        variables.result = queryExecute( "SELECT * FROM developers", {} );
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

	@Disabled( "Need to fix connection management to avoid 'Connection is closed' exceptions on commit and rollback." )
	@DisplayName( "Can commit a transaction in tag syntax" )
	@Test
	public void testTransactionTagSyntax() {
		getInstance().executeSource(
		    """
		    <cftransaction>
		        <cfquery>
		            INSERT INTO developers ( id, name,role )
		            VALUES ( <cfqueryparam value="33">, <cfqueryparam value="Jon Clausen">, <cfqueryparam value="Developer"> )
		        </cfquery>
		        <cftransaction action="commit" />
		        <cfset variables.result = queryExecute( "SELECT * FROM developers", {} ) />
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
		     queryExecute( "INSERT INTO developers ( id, name, role ) VALUES ( 33, 'Jon Clausen', 'Developer' )" );
		     transaction action="setsavepoint" savepoint="savepoint1";
		     queryExecute( "UPDATE developers SET name='Maxwell Smart' WHERE id=33" );
		     transaction action="rollback" savepoint="savepoint1";
		    }
		    variables.result = queryExecute( "SELECT * FROM developers", {} );
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

	@Disabled( "Not implemented, but very important!" )
	@Test
	public void testCustomQueryDatasource() {
		getInstance().executeSource(
		    """
		    transaction{
		    	queryExecute( "INSERT INTO developers (id,name) VALUES (444, 'Angela' );", {}, { datasource : "myOtherDatasource" } );
		    	transactionRollback();
		    }
		    variables.result = queryExecute( "SELECT * FROM developers", {}, { datasource : "myOtherDatasource" } );
		    """,
		    getContext() );

		// the insert should not be rolled back, since it's on a separate datasource
		assertNotNull(
		    getVariables().getAsQuery( result )
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
		getInstance().executeSource(
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
}
