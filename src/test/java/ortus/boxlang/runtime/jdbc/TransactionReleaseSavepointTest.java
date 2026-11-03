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
package ortus.boxlang.runtime.jdbc;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import ortus.boxlang.runtime.bifs.global.jdbc.BaseJDBCTest;
import ortus.boxlang.runtime.context.IJDBCCapableContext;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

class TransactionReleaseSavepointTest extends BaseJDBCTest {

	@Test
	void releasesAndReusesBeforeFirstQueryWithDefaultDatasource() {
		getInstance().executeSource( """
		                             transaction {
		                                 transactionSetSavepoint("beforeWork");
		                                 queryExecute("INSERT INTO developers (id,name,role) VALUES (81,'Discard','Developer')");
		                                 transactionRollback("beforeWork");
		                                 transactionReleaseSavepoint("beforeWork");
		                                 transactionSetSavepoint("beforeWork");
		                                 queryExecute("INSERT INTO developers (id,name,role) VALUES (82,'Keep','Developer')");
		                                 transactionReleaseSavepoint("beforeWork");
		                             }
		                             variables.removed = queryExecute("SELECT * FROM developers WHERE id=81").recordCount;
		                             variables.retained = queryExecute("SELECT * FROM developers WHERE id=82").recordCount;
		                             """, getContext() );
		assertEquals( 0, getVariables().getAsInteger( Key.of( "removed" ) ) );
		assertEquals( 1, getVariables().getAsInteger( Key.of( "retained" ) ) );
	}

	@Test
	void componentReleasePreservesChangesAndRollbackTargetRemainsUsable() {
		getInstance().executeSource( """
		                             transaction {
		                                 transaction action="setsavepoint" savepoint="first";
		                                 queryExecute("INSERT INTO developers (id,name,role) VALUES (83,'Keep','Developer')");
		                                 transaction action="setsavepoint" savepoint="second";
		                                 queryExecute("INSERT INTO developers (id,name,role) VALUES (84,'Discard','Developer')");
		                                 transaction action="rollback" savepoint="second";
		                                 transaction action="releasesavepoint" savepoint="second";
		                                 transaction action="releasesavepoint" savepoint="first";
		                             }
		                             variables.result = queryExecute("SELECT * FROM developers WHERE id IN (83,84)");
		                             """, getContext() );
		assertEquals( 1, getVariables().getAsQuery( Key.result ).size() );
	}

	@Test
	void pendingReleaseRequiresNoDatasourceOrConnection() {
		getInstance().executeSource( """
		                             transaction {
		                                 transactionSetSavepoint("pending");
		                                 transactionReleaseSavepoint("pending");
		                             }
		                             """, getContext() );
		assertFalse( ( ( IJDBCCapableContext ) getContext() ).getConnectionManager().isInTransaction() );
	}

	@Test
	void releaseOutsideTransactionFailsAndComponentRetainsNoOpContract() {
		assertThrows( BoxRuntimeException.class, () -> getInstance().executeSource( "transactionReleaseSavepoint('missing');", getContext() ) );
		assertDoesNotThrow( () -> getInstance().executeSource( "transaction action='releasesavepoint' savepoint='missing';", getContext() ) );
	}

	@Test
	void childReleaseCannotReleaseParentSavepoint() {
		try {
			getInstance().getConfiguration().enableNestedTransactions = true;
			( ( IJDBCCapableContext ) getContext() ).getConnectionManager().setEnableNestedTransactions( true );
			getInstance().executeSource( """
			                             transaction {
			                                 transactionSetSavepoint("same");
			                                 transaction {
			                                     transactionSetSavepoint("same");
			                                     queryExecute("INSERT INTO developers (id,name,role) VALUES (85,'Child','Developer')");
			                                     transactionReleaseSavepoint("same");
			                                 }
			                                 transactionRollback("same");
			                                 transactionReleaseSavepoint("same");
			                             }
			                             variables.result = queryExecute("SELECT * FROM developers WHERE id=85");
			                             """, getContext() );
			assertEquals( 0, getVariables().getAsQuery( Key.result ).size() );
		} finally {
			getInstance().getConfiguration().enableNestedTransactions = false;
			( ( IJDBCCapableContext ) getContext() ).getConnectionManager().setEnableNestedTransactions( false );
		}
	}
}
