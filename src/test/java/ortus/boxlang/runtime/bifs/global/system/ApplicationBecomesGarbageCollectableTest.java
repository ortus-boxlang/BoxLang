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
package ortus.boxlang.runtime.bifs.global.system;

import static com.google.common.truth.Truth.assertThat;

import java.lang.management.ManagementFactory;
import java.lang.ref.WeakReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import com.sun.management.HotSpotDiagnosticMXBean;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.application.Application;
import ortus.boxlang.runtime.context.ApplicationBoxContext;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.services.ApplicationService;

public class ApplicationBecomesGarbageCollectableTest {

	static BoxRuntime			runtime;
	static ApplicationService	applicationService;

	@BeforeAll
	public static void setUp() {
		runtime				= BoxRuntime.getInstance( true );
		applicationService	= runtime.getApplicationService();
	}

	@DisplayName( "It can stop an application and the Application object becomes unreachable" )
	@Test
	@Timeout( 60 )
	void testItCanStopAnApplication() {
		String						appName	= "unit-test-app-collect-" + UUID.randomUUID();
		Key							appKey	= Key.of( appName );
		WeakReference<Application>	appRef	= startAndStopApplication( appName, appKey );

		assertThatWeakReferentIsNoLongerStronglyReachable( appRef );
	}

	/**
	 * Run start/stop in a callee so the request context is not a Java-frame local
	 * on the thread that GCs and dumps the heap.
	 */
	private static WeakReference<Application> startAndStopApplication( String appName, Key appKey ) {
		IBoxContext context = new ScriptingRequestBoxContext( runtime.getRuntimeContext() );
		try {
			runtime.executeSource(
			    """
			    bx:application name="%s" sessionmanagement="true";
			         """.formatted( appName ),
			    context );

			Application					targetApp	= context.getParentOfType( ApplicationBoxContext.class ).getApplication();
			WeakReference<Application>	appRef		= new WeakReference<>( targetApp );

			assertThat( targetApp.hasStarted() ).isTrue();
			assertThat( targetApp.getSessionCount() ).isEqualTo( 1 );
			assertThat( targetApp.getStartTime() ).isNotNull();
			assertThat( applicationService.hasApplication( appKey ) ).isTrue();

			runtime.executeSource(
			    """
			    applicationStop();
			    """,
			    context );

			assertThat( targetApp.hasStarted() ).isFalse();
			assertThat( targetApp.getSessionCount() ).isEqualTo( 0 );
			assertThat( targetApp.getStartTime() ).isNull();
			assertThat( applicationService.hasApplication( appKey ) ).isFalse();

			return appRef;
		} finally {
			context.shutdown();
		}
	}

	public static volatile byte[] __garbage = null;

	// Try (best effort) to force a GC run, with the caller expecting that the supplied WeakRef refers
	// to something no longer strongly reachable, meaning that the referent will be GC'd, and before this
	// function returns we assert that calling `get` returns null because the object was absolutely collected.
	private static void assertThatWeakReferentIsNoLongerStronglyReachable( WeakReference<?> ref ) {
		for ( int i = 0; i < 50; i++ ) {
			System.gc();
			if ( ref.get() == null ) {
				return;
			}
			__garbage		= new byte[ 1024 * 1024 ];
			__garbage[ 0 ]	= 1;
			try {
				Thread.sleep( 20 );
			} catch ( InterruptedException e ) {
				Thread.currentThread().interrupt();
				break;
			}
		}
		try {
			assertThat( ref.get() ).isNull();
		} catch ( Throwable e ) {
			if ( System.getenv( "HEAPDUMPS" ) instanceof String s && s.equals( "1" ) ) {
				Path dump = dumpHeap( "application-still-reachable" );
				System.out.println( "Heap dump written to " + dump.toAbsolutePath() );
			}

			throw e;
		}
	}

	private static Path dumpHeap( String label ) {
		try {
			Path dir = Path.of( "build", "heapdumps" );
			Files.createDirectories( dir );
			String					stamp	= LocalDateTime.now().format( DateTimeFormatter.ofPattern( "yyyyMMddHHmmss" ) );
			Path					dump	= dir.resolve( label + "-" + stamp + ".hprof" );
			HotSpotDiagnosticMXBean	mxBean	= ManagementFactory.getPlatformMXBean( HotSpotDiagnosticMXBean.class );
			mxBean.dumpHeap( dump.toAbsolutePath().toString(), true );
			return dump;
		} catch ( Exception e ) {
			throw new RuntimeException( "Failed to write heap dump", e );
		}
	}

}
