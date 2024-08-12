
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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.bifs.global.jdbc.BaseJDBCTest;
import ortus.boxlang.runtime.events.BoxEvent;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Query;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.exceptions.DatabaseException;

/**
 * Tests the basics of the transaction component, especially attribute validation.
 * <p>
 * More advanced transactional logic tests should be implemented in
 * <code>ortus.boxlang.runtime.jdbc.TransactionTest</code> class.
 */
public class TransactionTest extends BaseJDBCTest {

	static Key result = new Key( "result" );

	@DisplayName( "Can compile a transaction component" )
	@Test
	public void testBasicTransaction() {
		getInstance().executeSource(
		    """
		    transaction{
		    	variables.result = queryExecute( "SELECT * FROM developers", {} );
		    }
		    """,
		    getContext() );
		Query theResult = ( Query ) getVariables().get( result );
		assertEquals( 3, theResult.size() );
	}

	@DisplayName( "Throws validation error if you try to commit or rollback a non-existing transaction" )
	@Test
	public void testInvalidTransactionUsage() {
		DatabaseException e = assertThrows( DatabaseException.class, () -> getInstance().executeSource(
		    """
		       variables.result = queryExecute( "SELECT * FROM developers", {} );
		       transaction action="commit";
		    """,
		    getContext() )
		);
		assertTrue( e.getMessage().startsWith( "Transaction is not started" ) );
	}

	@DisplayName( "Throws on bad action level" )
	@Test
	public void testActionValidation() {
		assertDoesNotThrow( () -> getInstance().executeSource( "transaction{transaction action='commit';}", getContext() ) );

		BoxRuntimeException e = assertThrows( BoxRuntimeException.class, () -> getInstance().executeSource( "transaction action='foo'{}", getContext() ) );

		assertTrue( e.getMessage().startsWith( "Input [action] for component [Transaction] must be one of the following values:" ) );
	}

	@DisplayName( "Throws on bad isolation level" )
	@Test
	public void testIsolationValidation() {
		assertDoesNotThrow( () -> getInstance().executeSource( "transaction isolation='read_committed'{}", getContext() ) );

		BoxRuntimeException e = assertThrows( BoxRuntimeException.class, () -> getInstance().executeSource( "transaction isolation='foo'{}", getContext() ) );

		assertTrue( e.getMessage().startsWith( "Input [isolation] for component [Transaction] must be one of the following values:" ) );
	}

	@DisplayName( "Re-broadcasts exceptions" )
	@Test
	public void testTransactionException() {
		DatabaseException databaseException = assertThrows( DatabaseException.class,
		    () -> getInstance().executeSource( "transaction { queryExecute( 'SELECxT id FROM developers' ); }", getContext() ) );
		assertTrue( databaseException.getMessage().startsWith( "Syntax error:" ) );

		BoxRuntimeException genericException = assertThrows( BoxRuntimeException.class,
		    () -> getInstance().executeSource( "transaction { queryExecute( 'SELECT id FROM developers' ); throw( message = 'fooey' ); }", getContext() ) );

		assertTrue( genericException.getMessage().startsWith( "fooey" ) );
	}

	@DisplayName( "Properly cleans up transaction context after exceptions" )
	@Test
	public void testTransactionEndsOnException() {
		getInstance().executeSource(
		    """
		    try{
		    	transaction{
		    		queryExecute( 'SELECxT id FROM developers' );
		    	}
		    } catch( any e ){
		    	// continue
		    }
		    transaction{
		    	queryExecute( 'INSERT INTO developers (id) VALUES (111)'  );
		    }
		      """, getContext() );

		Query theResult = ( Query ) getInstance()
		    .executeStatement( "queryExecute( 'SELECT * FROM developers WHERE id IN (111)' );", getContext() );
		assertEquals( 1, theResult.size() );
	}

	@DisplayName( "Emits transactional events" )
	@Test
	public void testTransactionEvents() {
		getInstance().getInterceptorService().register( data -> {
			getVariables().put( "begin", true );
			return false;
		}, BoxEvent.ON_TRANSACTION_BEGIN.key() );
		getInstance().getInterceptorService().register( data -> {
			getVariables().put( "end", true );
			return false;
		}, BoxEvent.ON_TRANSACTION_END.key() );
		getInstance().getInterceptorService().register( data -> {
			getVariables().put( "acquire", true );
			return false;
		}, BoxEvent.ON_TRANSACTION_ACQUIRE.key() );
		getInstance().getInterceptorService().register( data -> {
			getVariables().put( "release", true );
			return false;
		}, BoxEvent.ON_TRANSACTION_RELEASE.key() );
		getInstance().getInterceptorService().register( data -> {
			getVariables().put( "commit", true );
			return false;
		}, BoxEvent.ON_TRANSACTION_COMMIT.key() );
		getInstance().getInterceptorService().register( data -> {
			getVariables().put( "rollback", true );
			return false;
		}, BoxEvent.ON_TRANSACTION_ROLLBACK.key() );
		getInstance().getInterceptorService().register( data -> {
			getVariables().put( "savepoint", true );
			return false;
		}, BoxEvent.ON_TRANSACTION_SET_SAVEPOINT.key() );

		getInstance().executeSource(
		    """
		      transaction{
		      	queryExecute( 'INSERT INTO developers (id) VALUES (111)'  );
		    transactionSetSavepoint( 'insert1' );
		    transactionRollback( 'insert1' );
		      	queryExecute( 'INSERT INTO developers (id) VALUES (222)'  );
		    transactionCommit();
		      }
		      """,
		    getContext() );
		assertTrue( getVariables().containsKey( "begin" ) );
		assertTrue( getVariables().containsKey( "end" ) );
		assertTrue( getVariables().containsKey( "acquire" ) );
		assertTrue( getVariables().containsKey( "release" ) );
		assertTrue( getVariables().containsKey( "commit" ) );
		assertTrue( getVariables().containsKey( "rollback" ) );
		assertTrue( getVariables().containsKey( "savepoint" ) );
	}
}