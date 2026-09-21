/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package ortus.boxlang.runtime.bifs.global.async;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.bifs.global.jdbc.BaseJDBCTest;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.Query;

/**
 * Verifies that {@code runAsync()}, {@code asyncAll()} and {@code asyncAllApply()} each execute
 * with their own, isolated JDBC connection/transaction, instead of sharing the transaction that
 * is active on the calling thread. This matches the behavior already provided by the {@code thread}
 * component and other parallel BIFs (e.g. {@code arrayEach( parallel = true )}).
 *
 * @see <a href="https://ortussolutions.atlassian.net/browse/BL-2659">BL-2659</a>
 */
public class AsyncTransactionIsolationTest extends BaseJDBCTest {

	static Key result = new Key( "result" );

	@DisplayName( "runAsync() does not inherit the caller's transaction" )
	@Test
	public void testRunAsyncIsolatesTransaction() {
		getInstance().executeSource(
		    """
		    transaction {
		    	variables.inTransaction = isInTransaction();
		    	variables.result = runAsync( () => isInTransaction() ).get();
		    }
		    """,
		    getContext() );

		assertThat( getVariables().get( Key.of( "inTransaction" ) ) ).isEqualTo( true );
		assertThat( getVariables().get( result ) ).isEqualTo( false );
	}

	@DisplayName( "asyncAll() does not inherit the caller's transaction" )
	@Test
	public void testAsyncAllIsolatesTransaction() {
		getInstance().executeSource(
		    """
		    transaction {
		    	variables.inTransaction = isInTransaction();
		    	variables.result = asyncAll( [ () => isInTransaction() ] ).get();
		    }
		    """,
		    getContext() );

		assertThat( getVariables().get( Key.of( "inTransaction" ) ) ).isEqualTo( true );
		Array asyncResults = ( Array ) getVariables().get( result );
		assertThat( asyncResults.get( 0 ) ).isEqualTo( false );
	}

	@DisplayName( "asyncAllApply() does not inherit the caller's transaction" )
	@Test
	public void testAsyncAllApplyIsolatesTransaction() {
		getInstance().executeSource(
		    """
		    transaction {
		    	variables.inTransaction = isInTransaction();
		    	variables.result = asyncAllApply( [ 1 ], ( i ) => isInTransaction() );
		    }
		    """,
		    getContext() );

		assertThat( getVariables().get( Key.of( "inTransaction" ) ) ).isEqualTo( true );
		Array asyncResults = ( Array ) getVariables().get( result );
		assertThat( asyncResults.get( 0 ) ).isEqualTo( false );
	}

	@DisplayName( "runAsync() can still run its own query on a fresh connection outside of a transaction" )
	@Test
	public void testRunAsyncCanStillQuery() {
		// The isolated context runAsync() executes in doesn't inherit the test datasource that
		// BaseJDBCTest wires up as a per-instance default, so pass it explicitly here. A real
		// application's `defaultDatasource` (Application.bx/boxlang.json) is looked up via the
		// context chain and is unaffected by this isolation.
		getInstance().executeSource(
		    String.format(
		        """
		        variables.result = runAsync( () => queryExecute( "SELECT * FROM developers", {}, { datasource: "%s" } ) ).get();
		        """, getDatasource().getConfiguration().getOriginalName() ),
		    getContext() );

		Query theResult = ( Query ) getVariables().get( result );
		assertThat( theResult.size() ).isEqualTo( 4 );
	}

}
