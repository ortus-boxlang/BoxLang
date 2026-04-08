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
package ortus.boxlang.runtime.async.watchers.listeners;

import static com.google.common.truth.Truth.assertThat;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.async.watchers.WatcherContext;
import ortus.boxlang.runtime.async.watchers.WatcherEvent;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.runnables.IClassRunnable;
import ortus.boxlang.runtime.scopes.Key;

class ClassListenerTest {

	private static BoxRuntime	instance;

	private IBoxContext			context;

	@BeforeAll
	static void beforeAll() {
		instance = BoxRuntime.getInstance( true );
		String mappingPath = Paths.get( "src/test/resources" ).toAbsolutePath().toString();
		instance.getConfiguration().registerMapping( "/watchertests", mappingPath );
	}

	@BeforeEach
	void beforeEach() {
		this.context = new ScriptingRequestBoxContext( instance.getRuntimeContext() );
	}

	@Test
	@DisplayName( "It can instantiate class listener from BoxLang class and dispatch lifecycle methods" )
	void testClassListenerDispatch() throws Exception {
		ClassListener	listener		= new ClassListener( "watchertests.components.Listener", this.context );
		WatcherContext	watcherContext	= new WatcherContext( this.context, null );

		WatcherEvent	createdEvent	= new WatcherEvent(
		    WatcherEvent.Kind.CREATED,
		    Path.of( "/tmp/watched/file.txt" ),
		    Path.of( "file.txt" ),
		    Path.of( "/tmp/watched" ),
		    Instant.parse( "2026-04-07T12:00:00Z" )
		);

		listener.onEvent( createdEvent, watcherContext );

		IClassRunnable runnable = getRunnableInstance( listener );
		assertThat( runnable.getAsInteger( Key.of( "onCreateCount" ) ) ).isEqualTo( 1 );
		assertThat( runnable.getAsInteger( Key.of( "onEventCount" ) ) ).isEqualTo( 1 );
		assertThat( runnable.getThisScope().getAsString( Key.of( "lastKind" ) ) ).isEqualTo( "created" );

		WatcherEvent overflowEvent = new WatcherEvent( Instant.parse( "2026-04-07T12:01:00Z" ) );
		listener.onEvent( overflowEvent, watcherContext );

		assertThat( runnable.getAsInteger( Key.of( "onCreateCount" ) ) ).isEqualTo( 1 );
		assertThat( runnable.getAsInteger( Key.of( "onEventCount" ) ) ).isEqualTo( 2 );
		assertThat( runnable.getThisScope().getAsString( Key.of( "lastKind" ) ) ).isEqualTo( "overflow" );

		listener.onError( new RuntimeException( "boom" ), watcherContext );
		assertThat( runnable.getAsInteger( Key.of( "onErrorCount" ) ) ).isEqualTo( 1 );
	}

	private IClassRunnable getRunnableInstance( ClassListener listener ) throws Exception {
		Field instanceField = ClassListener.class.getDeclaredField( "instance" );
		instanceField.setAccessible( true );
		return ( IClassRunnable ) instanceField.get( listener );
	}
}
