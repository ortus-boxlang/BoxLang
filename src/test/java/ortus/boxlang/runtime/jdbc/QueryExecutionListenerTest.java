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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.services.DatasourceService;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.DatabaseException;
import tools.JDBCTestUtils;

/**
 * Tests for the {@link QueryExecutionListener} interface and the listener management APIs
 * in {@link PendingQuery}.
 */
@DisplayName( "QueryExecutionListener Tests" )
public class QueryExecutionListenerTest {

	// ---------------------------------------------------------------------------
	// Static fixtures
	// ---------------------------------------------------------------------------

	static BoxRuntime			instance;
	static DataSource			datasource;
	static DatasourceService	datasourceService;
	ScriptingRequestBoxContext	context;
	IScope						variables;

	// ---------------------------------------------------------------------------
	// Setup / Teardown
	// ---------------------------------------------------------------------------

	@BeforeAll
	static void setUpAll() {
		instance = BoxRuntime.getInstance( true );
		IBoxContext ctx = new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		datasourceService = instance.getDataSourceService();
		String uniqueName = UUID.randomUUID().toString();
		datasource = JDBCTestUtils.constructTestDataSource( uniqueName, ctx );
		datasourceService.register( Key.of( uniqueName ), datasource );
		instance.getConfiguration().datasources.put( Key.of( uniqueName ), datasource.getConfiguration() );
	}

	@AfterAll
	static void tearDownAll() throws SQLException {
		IBoxContext ctx = new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		JDBCTestUtils.dropTestTable( datasource, ctx, "developers", true );
		datasource.shutdown();
	}

	@BeforeEach
	void setupEach() {
		context = new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		context.getConnectionManager().setDefaultDatasource( datasource );
		variables = context.getScopeNearby( VariablesScope.name );
		assertDoesNotThrow( () -> JDBCTestUtils.resetDevelopersTable( datasource, context ) );
	}

	// ---------------------------------------------------------------------------
	// Helper – creates a simple PendingQuery targeting developers
	// ---------------------------------------------------------------------------

	private PendingQuery buildSelectQuery() {
		return new PendingQuery(
		    context,
		    "SELECT * FROM developers",
		    ( Object ) null,
		    new QueryOptions( new Struct() )
		);
	}

	private PendingQuery buildBadQuery() {
		return new PendingQuery(
		    context,
		    "SELECT * FROM nonexistent_table_xyz",
		    ( Object ) null,
		    new QueryOptions( new Struct() )
		);
	}

	// ---------------------------------------------------------------------------
	// Unit – listener registration API
	// ---------------------------------------------------------------------------

	@Test
	@DisplayName( "addExecutionListener returns the PendingQuery for fluent chaining" )
	void testAddReturnsThis() {
		PendingQuery			pq			= buildSelectQuery();
		QueryExecutionListener	listener	= noopListener();
		PendingQuery			result		= pq.addExecutionListener( listener );

		assertTrue( result == pq, "addExecutionListener should return 'this' for fluent chaining" );
	}

	@Test
	@DisplayName( "removeExecutionListener returns the PendingQuery for fluent chaining" )
	void testRemoveReturnsThis() {
		PendingQuery			pq			= buildSelectQuery();
		QueryExecutionListener	listener	= noopListener();
		pq.addExecutionListener( listener );
		PendingQuery result = pq.removeExecutionListener( listener );

		assertTrue( result == pq, "removeExecutionListener should return 'this' for fluent chaining" );
	}

	@Test
	@DisplayName( "getExecutionListeners returns an unmodifiable snapshot" )
	void testGetExecutionListenersIsUnmodifiable() {
		PendingQuery			pq			= buildSelectQuery();
		QueryExecutionListener	listener	= noopListener();
		pq.addExecutionListener( listener );

		List<QueryExecutionListener> listeners = pq.getExecutionListeners();
		assertEquals( 1, listeners.size() );
		assertThrows( UnsupportedOperationException.class, () -> listeners.add( noopListener() ) );
	}

	@Test
	@DisplayName( "addExecutionListener throws NullPointerException for null listener" )
	void testAddNullThrows() {
		PendingQuery pq = buildSelectQuery();
		assertThrows( NullPointerException.class, () -> pq.addExecutionListener( null ) );
	}

	@Test
	@DisplayName( "removeExecutionListener on unregistered listener is a no-op" )
	void testRemoveUnregisteredIsNoOp() {
		PendingQuery pq = buildSelectQuery();
		assertDoesNotThrow( () -> pq.removeExecutionListener( noopListener() ) );
		assertEquals( 0, pq.getExecutionListeners().size() );
	}

	@Test
	@DisplayName( "Multiple listeners can be registered independently" )
	void testMultipleListeners() {
		PendingQuery pq = buildSelectQuery();
		pq.addExecutionListener( noopListener() );
		pq.addExecutionListener( noopListener() );
		pq.addExecutionListener( noopListener() );

		assertEquals( 3, pq.getExecutionListeners().size() );
	}

	@Test
	@DisplayName( "removeExecutionListener removes only the first matching instance" )
	void testRemoveRemovesFirstOccurrence() {
		PendingQuery			pq			= buildSelectQuery();
		QueryExecutionListener	listener	= noopListener();
		pq.addExecutionListener( listener );
		pq.addExecutionListener( listener );

		pq.removeExecutionListener( listener );
		assertEquals( 1, pq.getExecutionListeners().size() );
	}

	// ---------------------------------------------------------------------------
	// Integration – lifecycle events during successful execution
	// ---------------------------------------------------------------------------

	@Test
	@DisplayName( "beforeExecution is called once before a successful query" )
	void testBeforeExecutionCalled() {
		AtomicInteger	beforeCount	= new AtomicInteger( 0 );

		PendingQuery	pq			= buildSelectQuery();
		pq.addExecutionListener( new QueryExecutionListener() {

			@Override
			public void beforeExecution( PendingQuery pendingQuery ) {
				beforeCount.incrementAndGet();
				assertNotNull( pendingQuery, "pendingQuery must not be null in beforeExecution" );
			}

			@Override
			public void afterExecution( PendingQuery pendingQuery, ExecutedQuery executedQuery ) {
			}

			@Override
			public void onError( PendingQuery pendingQuery, Exception exception ) {
			}
		} );

		pq.execute( context.getConnectionManager(), context );
		assertEquals( 1, beforeCount.get(), "beforeExecution should be called exactly once" );
	}

	@Test
	@DisplayName( "afterExecution is called once after a successful query with non-null ExecutedQuery" )
	void testAfterExecutionCalled() {
		AtomicInteger		afterCount		= new AtomicInteger( 0 );
		List<ExecutedQuery>	capturedResults	= new ArrayList<>();

		PendingQuery		pq				= buildSelectQuery();
		pq.addExecutionListener( new QueryExecutionListener() {

			@Override
			public void beforeExecution( PendingQuery pendingQuery ) {
			}

			@Override
			public void afterExecution( PendingQuery pendingQuery, ExecutedQuery executedQuery ) {
				afterCount.incrementAndGet();
				assertNotNull( executedQuery );
				capturedResults.add( executedQuery );
			}

			@Override
			public void onError( PendingQuery pendingQuery, Exception exception ) {
			}
		} );

		pq.execute( context.getConnectionManager(), context );

		assertEquals( 1, afterCount.get(), "afterExecution should be called exactly once" );
		assertEquals( 1, capturedResults.size() );
	}

	@Test
	@DisplayName( "onError is NOT called for a successful query" )
	void testOnErrorNotCalledOnSuccess() {
		AtomicInteger	errorCount	= new AtomicInteger( 0 );

		PendingQuery	pq			= buildSelectQuery();
		pq.addExecutionListener( new QueryExecutionListener() {

			@Override
			public void beforeExecution( PendingQuery pendingQuery ) {
			}

			@Override
			public void afterExecution( PendingQuery pendingQuery, ExecutedQuery executedQuery ) {
			}

			@Override
			public void onError( PendingQuery pendingQuery, Exception exception ) {
				errorCount.incrementAndGet();
			}
		} );

		pq.execute( context.getConnectionManager(), context );
		assertEquals( 0, errorCount.get(), "onError should not be called on successful execution" );
	}

	@Test
	@DisplayName( "Lifecycle order: beforeExecution is called before afterExecution" )
	void testLifecycleOrder() {
		List<String>	events	= new ArrayList<>();

		PendingQuery	pq		= buildSelectQuery();
		pq.addExecutionListener( new QueryExecutionListener() {

			@Override
			public void beforeExecution( PendingQuery pendingQuery ) {
				events.add( "before" );
			}

			@Override
			public void afterExecution( PendingQuery pendingQuery, ExecutedQuery executedQuery ) {
				events.add( "after" );
			}

			@Override
			public void onError( PendingQuery pendingQuery, Exception exception ) {
				events.add( "error" );
			}
		} );

		pq.execute( context.getConnectionManager(), context );

		assertEquals( 2, events.size() );
		assertEquals( "before", events.get( 0 ) );
		assertEquals( "after", events.get( 1 ) );
	}

	@Test
	@DisplayName( "All registered listeners receive beforeExecution" )
	void testAllListenersReceiveBeforeExecution() {
		AtomicInteger	count	= new AtomicInteger( 0 );

		PendingQuery	pq		= buildSelectQuery();
		for ( int i = 0; i < 3; i++ ) {
			pq.addExecutionListener( new QueryExecutionListener() {

				@Override
				public void beforeExecution( PendingQuery pendingQuery ) {
					count.incrementAndGet();
				}

				@Override
				public void afterExecution( PendingQuery pendingQuery, ExecutedQuery executedQuery ) {
				}

				@Override
				public void onError( PendingQuery pendingQuery, Exception exception ) {
				}
			} );
		}

		pq.execute( context.getConnectionManager(), context );
		assertEquals( 3, count.get(), "All 3 listeners should receive beforeExecution" );
	}

	// ---------------------------------------------------------------------------
	// Integration – error-handling events
	// ---------------------------------------------------------------------------

	@Test
	@DisplayName( "onError is called when query fails against a bad table" )
	void testOnErrorCalledForBadQuery() {
		AtomicInteger	errorCount		= new AtomicInteger( 0 );
		List<Exception>	capturedErrors	= new ArrayList<>();

		PendingQuery	pq				= buildBadQuery();
		pq.addExecutionListener( new QueryExecutionListener() {

			@Override
			public void beforeExecution( PendingQuery pendingQuery ) {
			}

			@Override
			public void afterExecution( PendingQuery pendingQuery, ExecutedQuery executedQuery ) {
			}

			@Override
			public void onError( PendingQuery pendingQuery, Exception exception ) {
				errorCount.incrementAndGet();
				capturedErrors.add( exception );
			}
		} );

		assertThrows( DatabaseException.class, () -> pq.execute( context.getConnectionManager(), context ) );

		assertTrue( errorCount.get() >= 1, "onError should be called at least once on a failed query" );
		assertFalse( capturedErrors.isEmpty() );
		assertNotNull( capturedErrors.get( 0 ) );
	}

	@Test
	@DisplayName( "afterExecution is NOT called when the query fails" )
	void testAfterExecutionNotCalledOnError() {
		AtomicInteger	afterCount	= new AtomicInteger( 0 );

		PendingQuery	pq			= buildBadQuery();
		pq.addExecutionListener( new QueryExecutionListener() {

			@Override
			public void beforeExecution( PendingQuery pendingQuery ) {
			}

			@Override
			public void afterExecution( PendingQuery pendingQuery, ExecutedQuery executedQuery ) {
				afterCount.incrementAndGet();
			}

			@Override
			public void onError( PendingQuery pendingQuery, Exception exception ) {
			}
		} );

		assertThrows( DatabaseException.class, () -> pq.execute( context.getConnectionManager(), context ) );
		assertEquals( 0, afterCount.get(), "afterExecution must not be called when the query fails" );
	}

	// ---------------------------------------------------------------------------
	// Error isolation – listener exceptions must not interrupt query execution
	// ---------------------------------------------------------------------------

	@Test
	@DisplayName( "Exception thrown by beforeExecution listener does not interrupt query" )
	void testBeforeExecutionListenerExceptionIsSwallowed() {
		AtomicInteger	afterCount	= new AtomicInteger( 0 );

		PendingQuery	pq			= buildSelectQuery();
		// Throwing listener
		pq.addExecutionListener( new QueryExecutionListener() {

			@Override
			public void beforeExecution( PendingQuery pendingQuery ) {
				throw new RuntimeException( "Simulated listener failure in beforeExecution" );
			}

			@Override
			public void afterExecution( PendingQuery pendingQuery, ExecutedQuery executedQuery ) {
				afterCount.incrementAndGet();
			}

			@Override
			public void onError( PendingQuery pendingQuery, Exception exception ) {
			}
		} );

		// Query should still complete successfully
		ExecutedQuery result = assertDoesNotThrow( () -> pq.execute( context.getConnectionManager(), context ) );
		assertNotNull( result );
		// afterExecution still fires even though beforeExecution listener threw
		assertEquals( 1, afterCount.get() );
	}

	@Test
	@DisplayName( "Exception thrown by afterExecution listener does not propagate to caller" )
	void testAfterExecutionListenerExceptionIsSwallowed() {
		PendingQuery pq = buildSelectQuery();
		pq.addExecutionListener( new QueryExecutionListener() {

			@Override
			public void beforeExecution( PendingQuery pendingQuery ) {
			}

			@Override
			public void afterExecution( PendingQuery pendingQuery, ExecutedQuery executedQuery ) {
				throw new RuntimeException( "Simulated listener failure in afterExecution" );
			}

			@Override
			public void onError( PendingQuery pendingQuery, Exception exception ) {
			}
		} );

		// Must not throw
		assertDoesNotThrow( () -> pq.execute( context.getConnectionManager(), context ) );
	}

	@Test
	@DisplayName( "Exception thrown by onError listener does not suppress original DatabaseException" )
	void testOnErrorListenerExceptionDoesNotSuppressOriginal() {
		PendingQuery pq = buildBadQuery();
		pq.addExecutionListener( new QueryExecutionListener() {

			@Override
			public void beforeExecution( PendingQuery pendingQuery ) {
			}

			@Override
			public void afterExecution( PendingQuery pendingQuery, ExecutedQuery executedQuery ) {
			}

			@Override
			public void onError( PendingQuery pendingQuery, Exception exception ) {
				throw new RuntimeException( "Secondary failure inside onError listener" );
			}
		} );

		// The original DatabaseException must still propagate
		assertThrows( DatabaseException.class, () -> pq.execute( context.getConnectionManager(), context ) );
	}

	@Test
	@DisplayName( "Subsequent listeners still fire even if an earlier listener throws" )
	void testSubsequentListenersFireAfterListenerThrows() {
		AtomicInteger	secondListenerCount	= new AtomicInteger( 0 );

		PendingQuery	pq					= buildSelectQuery();

		// First listener — throws
		pq.addExecutionListener( new QueryExecutionListener() {

			@Override
			public void beforeExecution( PendingQuery pendingQuery ) {
				throw new RuntimeException( "First listener throws" );
			}

			@Override
			public void afterExecution( PendingQuery pendingQuery, ExecutedQuery executedQuery ) {
				throw new RuntimeException( "First listener throws in after" );
			}

			@Override
			public void onError( PendingQuery pendingQuery, Exception exception ) {
			}
		} );

		// Second listener — should still fire
		pq.addExecutionListener( new QueryExecutionListener() {

			@Override
			public void beforeExecution( PendingQuery pendingQuery ) {
				secondListenerCount.incrementAndGet();
			}

			@Override
			public void afterExecution( PendingQuery pendingQuery, ExecutedQuery executedQuery ) {
				secondListenerCount.incrementAndGet();
			}

			@Override
			public void onError( PendingQuery pendingQuery, Exception exception ) {
			}
		} );

		assertDoesNotThrow( () -> pq.execute( context.getConnectionManager(), context ) );
		assertEquals( 2, secondListenerCount.get(), "Second listener should receive both beforeExecution and afterExecution" );
	}

	// ---------------------------------------------------------------------------
	// Thread safety
	// ---------------------------------------------------------------------------

	@Test
	@DisplayName( "Concurrent addExecutionListener calls are thread-safe" )
	void testConcurrentAddIsThreadSafe() throws InterruptedException {
		int				threads		= 20;
		PendingQuery	pq			= buildSelectQuery();
		CountDownLatch	start		= new CountDownLatch( 1 );
		CountDownLatch	done		= new CountDownLatch( threads );
		ExecutorService	executor	= Executors.newFixedThreadPool( threads );

		for ( int i = 0; i < threads; i++ ) {
			executor.submit( () -> {
				try {
					start.await();
					pq.addExecutionListener( noopListener() );
				} catch ( InterruptedException e ) {
					Thread.currentThread().interrupt();
				} finally {
					done.countDown();
				}
			} );
		}

		start.countDown();
		assertTrue( done.await( 10, TimeUnit.SECONDS ), "All threads must complete within 10 seconds" );
		executor.shutdown();

		assertEquals( threads, pq.getExecutionListeners().size(), "All " + threads + " listeners must be registered" );
	}

	@Test
	@DisplayName( "Concurrent execution with multiple listeners is thread-safe" )
	void testConcurrentExecutionWithListeners() throws InterruptedException {
		int						threads		= 10;
		AtomicInteger			beforeTotal	= new AtomicInteger( 0 );
		AtomicInteger			afterTotal	= new AtomicInteger( 0 );
		CountDownLatch			done		= new CountDownLatch( threads );
		ExecutorService			executor	= Executors.newFixedThreadPool( threads );

		QueryExecutionListener	listener	= new QueryExecutionListener() {

												@Override
												public void beforeExecution( PendingQuery pendingQuery ) {
													beforeTotal.incrementAndGet();
												}

												@Override
												public void afterExecution( PendingQuery pendingQuery, ExecutedQuery executedQuery ) {
													afterTotal.incrementAndGet();
												}

												@Override
												public void onError( PendingQuery pendingQuery, Exception exception ) {
												}
											};

		for ( int i = 0; i < threads; i++ ) {
			executor.submit( () -> {
				try {
					// Each thread creates its own PendingQuery to avoid sharing state
					PendingQuery pq = buildSelectQuery();
					pq.addExecutionListener( listener );
					pq.execute( context.getConnectionManager(), context );
				} finally {
					done.countDown();
				}
			} );
		}

		assertTrue( done.await( 30, TimeUnit.SECONDS ), "All threads must complete within 30 seconds" );
		executor.shutdown();

		assertEquals( threads, beforeTotal.get(), "beforeExecution should be called once per thread" );
		assertEquals( threads, afterTotal.get(), "afterExecution should be called once per thread" );
	}

	@Test
	@DisplayName( "Listener collection snapshot (getExecutionListeners) is independent of subsequent mutations" )
	void testSnapshotIndependentOfMutations() {
		PendingQuery			pq		= buildSelectQuery();
		QueryExecutionListener	first	= noopListener();
		pq.addExecutionListener( first );

		List<QueryExecutionListener> snapshot = pq.getExecutionListeners();
		assertEquals( 1, snapshot.size() );

		// Mutate the live list after snapshot
		pq.addExecutionListener( noopListener() );
		pq.removeExecutionListener( first );

		// Snapshot must be unchanged
		assertEquals( 1, snapshot.size(), "Snapshot must not reflect mutations that happened after it was taken" );
	}

	// ---------------------------------------------------------------------------
	// Helper factory
	// ---------------------------------------------------------------------------

	private static QueryExecutionListener noopListener() {
		return new QueryExecutionListener() {

			@Override
			public void beforeExecution( PendingQuery pendingQuery ) {
			}

			@Override
			public void afterExecution( PendingQuery pendingQuery, ExecutedQuery executedQuery ) {
			}

			@Override
			public void onError( PendingQuery pendingQuery, Exception exception ) {
			}
		};
	}
}