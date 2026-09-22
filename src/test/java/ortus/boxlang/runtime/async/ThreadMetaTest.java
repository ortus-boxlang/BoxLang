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
package ortus.boxlang.runtime.async;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;

public class ThreadMetaTest {

	static BoxRuntime	instance;
	IBoxContext			context;
	IScope				variables;

	@BeforeAll
	public static void setUp() {
		instance = BoxRuntime.getInstance( true );
	}

	@AfterAll
	public static void teardown() {
	}

	@BeforeEach
	public void setupEach() {
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
	}

	private static Thread sleeper() {
		Thread thread = new Thread( () -> {
			try {
				Thread.sleep( 5000 );
			} catch ( InterruptedException e ) {
				// done
			}
		}, "sleeper" );
		thread.start();
		return thread;
	}

	@DisplayName( "It only captures a stack trace when the stackTrace key is read" )
	@Test
	public void testStackTraceIsComputedOnRead() throws InterruptedException {
		Thread		thread	= sleeper();
		ThreadMeta	meta	= new ThreadMeta( Key.of( "sleeper" ), thread, System.currentTimeMillis() );
		try {
			Thread.sleep( 200 );
			// reading other keys, as every unscoped lookup inside a thread does, leaves the stack alone
			meta.getRaw( Key._NAME );
			meta.get( Key.output );
			meta.getOrDefault( Key.priority, "" );
			assertThat( meta.getWrapped().get( Key.stackTrace ) ).isEqualTo( "" );

			assertThat( meta.getAsString( Key.stackTrace ) ).contains( "Thread.sleep" );
			// sleeping, or still on its way there on a slow box: live either way
			assertThat( meta.get( Key.status ) ).isAnyOf( "WAITING", "RUNNNG" );
			assertThat( meta.getAsLong( Key.elapsedTime ) ).isAtLeast( 200L );
		} finally {
			thread.interrupt();
		}
	}

	@DisplayName( "It freezes the metadata once the thread completes" )
	@Test
	public void testCompleteFreezesTheMetadata() throws InterruptedException {
		Thread		thread	= sleeper();
		ThreadMeta	meta	= new ThreadMeta( Key.of( "sleeper" ), thread, System.currentTimeMillis() );
		try {
			meta.complete( "done", null, false );
			// the thread is still alive, but the stored final values win
			assertThat( meta.get( Key.status ) ).isEqualTo( "COMPLETED" );
			assertThat( meta.getAsString( Key.stackTrace ) ).isEqualTo( "" );
			assertThat( meta.get( Key.output ) ).isEqualTo( "done" );
			long elapsed = meta.getAsLong( Key.elapsedTime );
			Thread.sleep( 50 );
			assertThat( meta.getAsLong( Key.elapsedTime ) ).isEqualTo( elapsed );
		} finally {
			thread.interrupt();
		}
	}

	@DisplayName( "It keeps an INTERRUPTED status set by the manager while the thread is alive" )
	@Test
	public void testInterruptedStatusIsPreserved() {
		Thread		thread	= sleeper();
		ThreadMeta	meta	= new ThreadMeta( Key.of( "sleeper" ), thread, System.currentTimeMillis() );
		try {
			meta.put( Key.status, "INTERRUPTED" );
			assertThat( meta.get( Key.status ) ).isEqualTo( "INTERRUPTED" );
			assertThat( meta.getAsString( Key.stackTrace ) ).contains( "Thread.sleep" );
		} finally {
			thread.interrupt();
		}
	}

	@DisplayName( "It refreshes the live keys when the struct is read as a whole" )
	@Test
	public void testIterationRefreshesLiveKeys() {
		Thread		thread	= sleeper();
		ThreadMeta	meta	= new ThreadMeta( Key.of( "sleeper" ), thread, System.currentTimeMillis() );
		try {
			assertThat( meta.values().stream().anyMatch( v -> v instanceof String s && s.contains( "Thread.sleep" ) ) ).isTrue();
			assertThat( meta.getWrapped().get( Key.status ) ).isAnyOf( "WAITING", "RUNNNG" );
		} finally {
			thread.interrupt();
		}
	}

	@DisplayName( "It reports a terminated status when the thread failed" )
	@Test
	public void testTerminatedStatus() {
		Thread		thread	= new Thread( () -> {
							}, "noop" );
		ThreadMeta	meta	= new ThreadMeta( Key.of( "noop" ), thread, System.currentTimeMillis() );
		meta.complete( "", new RuntimeException( "boom" ), false );
		assertThat( meta.get( Key.status ) ).isEqualTo( "TERMINATED" );
		assertThat( meta.get( Key.error ) ).isInstanceOf( RuntimeException.class );
	}

	@DisplayName( "A thread sees its own live status and stack trace" )
	@Test
	public void testThreadSeesItsOwnMetadata() {
		// @formatter:off
		instance.executeSource(
		    """
				thread name="myThread" {
					variables.innerStatus = thread.status;
					variables.innerTrace  = myThread.stackTrace;
					variables.innerName   = name;
				}
				threadJoin( "myThread" );
				result = myThread;
		    """,
		    context, BoxSourceType.CFSCRIPT );
		// @formatter:on
		assertThat( variables.get( Key.of( "innerStatus" ) ) ).isEqualTo( "RUNNNG" );
		// the frame of the thread body itself is on the stack the thread reads
		assertThat( variables.getAsString( Key.of( "innerTrace" ) ) ).contains( "boxgenerated.scripts" );
		assertThat( variables.get( Key.of( "innerName" ) ).toString() ).isEqualTo( "myThread" );
		assertThat( variables.getAsStruct( Key.of( "result" ) ).get( Key.status ) ).isEqualTo( "COMPLETED" );
	}

	@DisplayName( "The request sees a running thread's live status through bxthread and by name" )
	@Test
	public void testRequestSeesLiveMetadata() {
		// @formatter:off
		instance.executeSource(
		    """
				thread name="myThread" {
					sleep( 1500 );
				}
				sleep( 300 );
				viaScope  = bxthread.myThread.status;
				viaName   = myThread.status;
				trace     = myThread.stackTrace;
				elapsed   = myThread.elapsedTime;
				threadJoin( "myThread" );
				done      = bxthread.myThread.status;
				doneTrace = myThread.stackTrace;
		    """,
		    context, BoxSourceType.CFSCRIPT );
		// @formatter:on
		assertThat( variables.get( Key.of( "viaScope" ) ) ).isAnyOf( "WAITING", "RUNNNG" );
		assertThat( variables.get( Key.of( "viaName" ) ) ).isAnyOf( "WAITING", "RUNNNG" );
		assertThat( variables.getAsString( Key.of( "trace" ) ) ).contains( "sleep" );
		assertThat( variables.getAsLong( Key.of( "elapsed" ) ) ).isAtLeast( 300L );
		assertThat( variables.get( Key.of( "done" ) ) ).isEqualTo( "COMPLETED" );
		assertThat( variables.get( Key.of( "doneTrace" ) ) ).isEqualTo( "" );
	}

}
