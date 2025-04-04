
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

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.bifs.global.jdbc.BaseJDBCTest;
import ortus.boxlang.runtime.context.IJDBCCapableContext;
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
		assertThat( theResult.size() ).isEqualTo( 4 );
	}

	@DisplayName( "Can use a case-insensitive isolation level" )
	@Test
	public void testIsolationCaseInsensitive() {
		getInstance().executeSource(
		    """
		    transaction isolation="READ_COMMITTED"{
		    	variables.result = queryExecute( "SELECT * FROM developers", {} );
		    }
		    """,
		    getContext() );
		Query theResult = ( Query ) getVariables().get( result );
		assertThat( theResult.size() ).isEqualTo( 4 );
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
		assertThat( e.getMessage() ).startsWith( "Transaction is not started" );
	}

	@DisplayName( "Throws on bad action level" )
	@Test
	public void testActionValidation() {
		assertDoesNotThrow( () -> getInstance().executeSource( "transaction{transaction action='commit';}", getContext() ) );

		BoxRuntimeException e = assertThrows( BoxRuntimeException.class, () -> getInstance().executeSource( "transaction action='foo'{}", getContext() ) );

		assertThat( e.getMessage() ).startsWith( "Input [action] for component [Transaction] must be one of the following values:" );
	}

	@DisplayName( "Throws on bad isolation level" )
	@Test
	public void testIsolationValidation() {
		assertDoesNotThrow( () -> getInstance().executeSource( "transaction isolation='read_committed'{}", getContext() ) );

		BoxRuntimeException e = assertThrows( BoxRuntimeException.class, () -> getInstance().executeSource( "transaction isolation='foo'{}", getContext() ) );

		assertThat( e.getMessage() ).startsWith( "Input [isolation] for component [Transaction] must be one of the following values:" );
	}

	@DisplayName( "Re-broadcasts exceptions" )
	@Test
	public void testTransactionException() {
		DatabaseException databaseException = assertThrows( DatabaseException.class,
		    () -> getInstance().executeSource( "transaction { queryExecute( 'SELECxT id FROM developers' ); }", getContext() ) );
		assertThat( databaseException.getMessage() ).startsWith( "Syntax error:" );

		BoxRuntimeException genericException = assertThrows( BoxRuntimeException.class,
		    () -> getInstance().executeSource( "transaction { queryExecute( 'SELECT id FROM developers' ); throw( message = 'fooey' ); }", getContext() ) );

		assertThat( genericException.getMessage() ).startsWith( "fooey" );
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
		assertThat( theResult.size() ).isEqualTo( 1 );

		// The connection manager won't close connections if it thinks they are from an active transaction.
		assertThat( ( ( IJDBCCapableContext ) getContext() ).getConnectionManager().isInTransaction() ).isFalse();
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
		assertThat( getVariables() ).containsKey( Key.of( "begin" ) );
		assertThat( getVariables() ).containsKey( Key.of( "end" ) );
		assertThat( getVariables() ).containsKey( Key.of( "acquire" ) );
		assertThat( getVariables() ).containsKey( Key.of( "release" ) );
		assertThat( getVariables() ).containsKey( Key.of( "commit" ) );
		assertThat( getVariables() ).containsKey( Key.of( "rollback" ) );
		assertThat( getVariables() ).containsKey( Key.of( "savepoint" ) );
	}
}