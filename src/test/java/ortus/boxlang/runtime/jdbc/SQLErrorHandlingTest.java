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

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.events.BoxEvent;
import ortus.boxlang.runtime.events.IInterceptorLambda;
import ortus.boxlang.runtime.types.IStruct;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicInteger;
import ortus.boxlang.runtime.types.exceptions.DatabaseException;
import tools.JDBCTestUtils;

/**
 * Test that SQL errors (like creating duplicate tables) are properly reported
 * instead of being masked by NullPointerException.
 */
public class SQLErrorHandlingTest {

	static BoxRuntime	instance;
	static DataSource	datasource;
	static IBoxContext	context;

	@BeforeAll
	public static void setUp() {
		instance	= BoxRuntime.getInstance( true );
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		datasource	= JDBCTestUtils.buildDatasource( "sqlErrorTest" );

		// Register the datasource
		Key datasourceKey = Key.of( "sqlErrorTest" );
		instance.getDataSourceService().register( datasourceKey, datasource );
		instance.getConfiguration().datasources.put( datasourceKey, datasource.getConfiguration() );
	}

	@AfterAll
	public static void tearDown() {
		if ( datasource != null ) {
			// Clean up
			try {
				datasource.execute( "DROP TABLE testusers", context );
			} catch ( Exception e ) {
				// Ignore if table doesn't exist
			}
			datasource.shutdown();
		}
	}

	@DisplayName( "It should report the actual SQL error, not a NullPointerException" )
	@Test
	public void testDuplicateTableCreationError() {
		String createTableSQL = "CREATE TABLE testusers ( id INTEGER PRIMARY KEY, name VARCHAR(155) )";

		// First creation should succeed
		datasource.execute( createTableSQL, context );

		// Second creation should fail with proper SQL error message
		DatabaseException	exception	= assertThrows( DatabaseException.class, () -> {
											datasource.execute( createTableSQL, context );
										} );

		// Verify the exception message contains the actual SQL error, not NullPointerException
		String				message		= exception.getMessage();
		assertThat( message ).isNotNull();
		assertThat( message.toLowerCase() ).doesNotContain( "nullpointerexception" );
		assertThat( message.toLowerCase() ).doesNotContain( "this.pointer" );
		assertThat( message.toLowerCase() ).doesNotContain( "isclosed" );

		// Verify it contains information about the actual error (table exists)
		// Derby error message says "already exists"
		assertThat( message.toLowerCase() ).containsMatch( "(already exists|duplicate)" );
	}

	@DisplayName( "It should properly handle SQL syntax errors" )
	@Test
	public void testSQLSyntaxError() {
		String				invalidSQL	= "CREATE INVALID SYNTAX HERE";

		// Should fail with proper SQL error message
		DatabaseException	exception	= assertThrows( DatabaseException.class, () -> {
											datasource.execute( invalidSQL, context );
										} );

		// Verify the exception message contains the actual SQL error, not NullPointerException
		String				message		= exception.getMessage();
		assertThat( message ).isNotNull();
		assertThat( message.toLowerCase() ).doesNotContain( "nullpointerexception" );
		assertThat( message.toLowerCase() ).doesNotContain( "this.pointer" );

		// Verify it contains information about syntax error
		assertThat( message.toLowerCase() ).containsMatch( "(syntax|invalid)" );
	}

	@Test
	public void testQueryErrorEventPreservesFailureAndCorrelation() {
		ListAppender<ILoggingEvent> logAppender = new ListAppender<>();
		logAppender.setContext( instance.getLoggingService().getLoggerContext() );
		logAppender.start();
		instance.getLoggingService().DATASOURCE_LOGGER.addAppender( logAppender );
		AtomicReference<IStruct>	observed	= new AtomicReference<>();
		AtomicInteger				count		= new AtomicInteger();
		IInterceptorLambda			listener	= data -> {
													observed.set( data );
													count.incrementAndGet();
													throw new IllegalStateException( "observer failed" );
												};
		instance.getInterceptorService().register( listener, BoxEvent.ON_QUERY_EXECUTE_ERROR.key() );
		try {
			DatabaseException failure = assertThrows( DatabaseException.class,
			    () -> datasource.execute( "SELECT missing_column FROM missing_table", context ) );
			assertThat( count.get() ).isEqualTo( 1 );
			assertThat( observed.get().get( Key.exception ) ).isSameInstanceAs( failure );
			assertThat( observed.get().get( Key.context ) ).isSameInstanceAs( context );
			assertThat( observed.get().get( Key.pendingQuery ) ).isInstanceOf( PendingQuery.class );
			assertThat( ( ( Number ) observed.get().get( Key.executionTime ) ).doubleValue() ).isAtLeast( 0.0 );
			ILoggingEvent diagnostic = logAppender.list.stream()
			    .filter( event -> event.getFormattedMessage().equals( "Failed to announce onQueryExecuteError" ) )
			    .findFirst().orElseThrow();
			assertThat( diagnostic.getLevel() ).isEqualTo( Level.ERROR );
			assertThat( diagnostic.getThrowableProxy().getClassName() ).isEqualTo( IllegalStateException.class.getName() );
			assertThat( diagnostic.getThrowableProxy().getMessage() ).isEqualTo( "observer failed" );
		} finally {
			instance.getInterceptorService().unregister( listener );
			instance.getLoggingService().DATASOURCE_LOGGER.detachAppender( logAppender );
			logAppender.stop();
		}
	}

	@Test
	public void testCachedQueryAnnouncementsCorrelateWithoutADatabaseRoundTrip() {
		java.util.List<IStruct>	before	= new java.util.ArrayList<>();
		java.util.List<IStruct>	after	= new java.util.ArrayList<>();
		IInterceptorLambda		pre		= data -> {
											if ( data.getAsString( Key.sql ).contains( "AS sentry_cache_probe" ) ) {
												before.add( data );
											}
											return false;
										};
		IInterceptorLambda		post	= data -> {
											if ( data.getAsString( Key.sql ).contains( "AS sentry_cache_probe" ) ) {
												after.add( data );
											}
											return false;
										};
		instance.getInterceptorService().register( pre, BoxEvent.PRE_QUERY_EXECUTE.key() );
		instance.getInterceptorService().register( post, BoxEvent.POST_QUERY_EXECUTE.key() );
		try {
			instance.executeSource(
			    """
			    queryExecute( "SELECT 1 AS sentry_cache_probe FROM SYSIBM.SYSDUMMY1", [], { datasource: "sqlErrorTest", cache: true, cacheKey: "sentry-probe-" & createUUID(), result: "probeFirst" } );
			    probeKey = "sentry-probe-" & createUUID();
			    queryExecute( "SELECT 1 AS sentry_cache_probe FROM SYSIBM.SYSDUMMY1", [], { datasource: "sqlErrorTest", cache: true, cacheKey: probeKey } );
			    queryExecute( "SELECT 1 AS sentry_cache_probe FROM SYSIBM.SYSDUMMY1", [], { datasource: "sqlErrorTest", cache: true, cacheKey: probeKey, result: "probeCached" } );
			    assert probeCached.cached;
			    """,
			    context );
			assertThat( before ).hasSize( 3 );
			assertThat( after ).hasSize( 3 );
			assertThat( before.get( 2 ).getAsBoolean( Key.cached ) ).isTrue();
			assertThat( before.get( 2 ).get( Key.pendingQuery ) ).isSameInstanceAs( after.get( 2 ).get( Key.pendingQuery ) );
			assertThat( ( ( Number ) after.get( 2 ).get( Key.executionTime ) ).longValue() ).isEqualTo( 0 );
		} finally {
			instance.getInterceptorService().unregister( pre );
			instance.getInterceptorService().unregister( post );
		}
	}

}
