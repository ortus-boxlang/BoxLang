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
import static org.mockito.Mockito.*;

import java.sql.Savepoint;
import java.sql.SQLException;
import java.util.List;
import java.util.ArrayList;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.exceptions.DatabaseException;

class TransactionSavepointTest {

	private Transaction			transaction;
	private BoxConnection		connection;
	private DataSource			datasource;
	private static BoxRuntime	runtime;
	private static final Key	FIRST	= Key.of( "first" );
	private static final Key	SECOND	= Key.of( "second" );

	@BeforeAll
	static void initialize() {
		runtime = BoxRuntime.getInstance( true );
	}

	@BeforeEach
	void setup() throws SQLException {
		this.datasource	= mock( DataSource.class );
		this.connection	= mock( BoxConnection.class );
		when( this.datasource.getBoxConnection() ).thenReturn( this.connection );
		when( this.connection.setSavepoint( anyString() ) ).thenAnswer( invocation -> mock( Savepoint.class ) );
		this.transaction = new Transaction( new ScriptingRequestBoxContext( runtime.getRuntimeContext() ), this.datasource );
	}

	@Test
	void createsPendingSavepointsBeforeFirstQuery() throws SQLException {
		assertNull( this.transaction.setSavepoint( FIRST ) );
		assertNull( this.transaction.setSavepoint( SECOND ) );
		verifyNoInteractions( this.datasource, this.connection );
		this.transaction.getBoxConnection();
		InOrder order = inOrder( this.connection );
		order.verify( this.connection ).setAutoCommit( false );
		order.verify( this.connection ).setSavepoint( "first" );
		order.verify( this.connection ).setSavepoint( "second" );
		assertNotNull( this.transaction.getSavepoints().get( FIRST ) );
	}

	@Test
	void pendingReleaseAndRollbackKeepRegistryConsistent() throws SQLException {
		this.transaction.setSavepoint( FIRST );
		this.transaction.setSavepoint( SECOND );
		this.transaction.rollback( FIRST );
		assertEquals( List.of( FIRST ), new ArrayList<>( this.transaction.getSavepoints().keySet() ) );
		this.transaction.setSavepoint( SECOND );
		this.transaction.releaseSavepoint( FIRST );
		assertTrue( this.transaction.getSavepoints().isEmpty() );
		this.transaction.setSavepoint( FIRST );
		this.transaction.getBoxConnection();
		verify( this.connection, never() ).setSavepoint( "second" );
	}

	@Test
	void releaseDoesNotCommitOrRollbackAndAllowsCaseInsensitiveReuse() throws SQLException {
		this.transaction.getBoxConnection();
		Savepoint point = this.transaction.setSavepoint( FIRST );
		this.transaction.setSavepoint( SECOND );
		this.transaction.releaseSavepoint( Key.of( "FiRsT" ) );
		verify( this.connection ).releaseSavepoint( point );
		verify( this.connection, never() ).commit();
		verify( this.connection, never() ).rollback();
		assertTrue( this.transaction.getSavepoints().isEmpty() );
		assertThrows( DatabaseException.class, () -> this.transaction.rollback( SECOND ) );
		this.transaction.setSavepoint( FIRST );
	}

	@Test
	void failedReleaseLeavesHandlesAvailableForRecovery() throws SQLException {
		this.transaction.getBoxConnection();
		Savepoint point = this.transaction.setSavepoint( FIRST );
		this.transaction.setSavepoint( SECOND );
		doThrow( new SQLException( "unsupported" ) ).when( this.connection ).releaseSavepoint( point );
		assertThrows( DatabaseException.class, () -> this.transaction.releaseSavepoint( FIRST ) );
		assertEquals( 2, this.transaction.getSavepoints().size() );
		this.transaction.rollback( FIRST );
		assertEquals( List.of( FIRST ), new ArrayList<>( this.transaction.getSavepoints().keySet() ) );
	}

	@Test
	void commitAndFullRollbackClearAllHandles() {
		this.transaction.setSavepoint( FIRST );
		this.transaction.commit();
		this.transaction.setSavepoint( FIRST );
		this.transaction.rollback();
		assertTrue( this.transaction.getSavepoints().isEmpty() );
		this.transaction.getBoxConnection();
		this.transaction.setSavepoint( FIRST );
		this.transaction.commit();
		assertTrue( this.transaction.getSavepoints().isEmpty() );
		this.transaction.setSavepoint( FIRST );
		this.transaction.rollback();
		assertTrue( this.transaction.getSavepoints().isEmpty() );
	}

	@Test
	void invalidAndMissingNamesFailBeforeJdbc() {
		assertThrows( DatabaseException.class, () -> this.transaction.releaseSavepoint( null ) );
		assertThrows( DatabaseException.class, () -> this.transaction.releaseSavepoint( Key.of( " " ) ) );
		assertThrows( DatabaseException.class, () -> this.transaction.releaseSavepoint( FIRST ) );
		assertThrows( DatabaseException.class, () -> this.transaction.setSavepoint( Key.of( "" ) ) );
		this.transaction.setSavepoint( FIRST );
		assertThrows( DatabaseException.class, () -> this.transaction.setSavepoint( FIRST ) );
		verifyNoInteractions( this.connection );
	}

	@Test
	void nestedReleaseKeepsParentAndSiblingOwnership() {
		this.transaction.setSavepoint( FIRST );
		ChildTransaction child = new ChildTransaction( this.transaction );
		child.begin();
		child.setSavepoint( FIRST );
		child.setSavepoint( SECOND );
		child.releaseSavepoint( FIRST );
		assertTrue( this.transaction.getSavepoints().containsKey( FIRST ) );
		assertEquals( 1, child.getSavepoints().size() ); // internal BEGIN remains
		child.setSavepoint( FIRST );
		ChildTransaction grandchild = new ChildTransaction( child );
		grandchild.begin();
		grandchild.setSavepoint( FIRST );
		grandchild.releaseSavepoint( FIRST );
		assertEquals( 2, child.getSavepoints().size() - grandchild.getSavepoints().size() );
		this.transaction.rollback( FIRST );
		assertTrue( child.getSavepoints().isEmpty() );
		assertTrue( grandchild.getSavepoints().isEmpty() );
		assertThrows( DatabaseException.class, () -> child.releaseSavepoint( FIRST ) );
	}

	@Test
	void acquisitionFailureDoesNotRetainReleasedJdbcHandles() throws SQLException {
		this.transaction.setSavepoint( FIRST );
		this.transaction.setSavepoint( SECOND );
		when( this.connection.setSavepoint( "second" ) ).thenThrow( new SQLException( "failed" ) );
		assertThrows( DatabaseException.class, () -> this.transaction.getBoxConnection() );
		assertNull( this.transaction.getBoxConnectionIfPresent() );
		assertTrue( this.transaction.getSavepoints().values().stream().allMatch( value -> value == null ) );
		verify( this.connection ).close();
	}
}
