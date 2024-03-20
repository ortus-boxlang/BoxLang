
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

	@DisplayName( "Throws on bad action level" )
	@Test
	public void testActionValidation() {
		assertDoesNotThrow( () -> getInstance().executeSource( "transaction action='commit';", getContext() ) );

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
		DatabaseException	databaseException	= assertThrows( DatabaseException.class,
		    () -> getInstance().executeSource( "transaction { queryExecute( 'SELECxT id FROM developers' ); }", getContext() ) );

		String				message				= databaseException.getMessage();
		assertTrue( databaseException.getMessage().startsWith( "Syntax error:" ) );
		BoxRuntimeException genericException = assertThrows( BoxRuntimeException.class,
		    () -> getInstance().executeSource( "transaction { queryExecute( 'SELECT id FROM developers' ); throw( message = 'fooey' ); }", getContext() ) );

		assertTrue( genericException.getMessage().startsWith( "fooey" ) );
	}
}