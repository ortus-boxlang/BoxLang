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
package ortus.boxlang.runtime.bifs.global.watcher;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.AfterEach;
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
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.watchers.WatcherInstance;

public class WatchersTest {

	static BoxRuntime	instance;
	IBoxContext			context;
	IScope				variables;
	static Key			result	= new Key( "result" );

	@BeforeAll
	public static void setUp() {
		instance = BoxRuntime.getInstance( true );
	}

	@BeforeEach
	public void setupEach() {
		this.context	= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		this.variables	= this.context.getScopeNearby( VariablesScope.name );
		instance.getWatcherService().shutdownAll( false );
	}

	@AfterEach
	public void tearDownEach() {
		instance.getWatcherService().shutdownAll( false );
	}

	@DisplayName( "It can manage watchers via the watcher BIF lifecycle" )
	@Test
	public void testWatcherBIFLifecycle() {
		instance.executeSource(
		    """
		    watcherOne = watcherNew( "watcherOne", [ "./src" ], ( event, watcherContext ) => {} )
		    watcherTwo = watcherNew( "watcherTwo", [ "./src" ], ( event, watcherContext ) => {} )

		    existsOne = watcherExists( "watcherOne" )
		    fetchedOne = watcherGet( "watcherOne" )
		    allWatchers = watcherGetAll()
		    watcherNames = watcherList()

		    started = watcherStart( "watcherOne" )
		    restarted = watcherRestart( "watcherOne" )
		    watcherStop( "watcherOne" )
		    watcherStopAll()
		    watcherShutdownAll( true )

		    namesAfterShutdown = watcherList()
		    allAfterShutdown = watcherGetAll()
		    existsAfterShutdown = watcherExists( "watcherOne" )
		    """,
		    this.context
		);

		WatcherInstance	watcherOne			= ( WatcherInstance ) this.variables.get( Key.of( "watcherOne" ) );
		WatcherInstance	fetchedOne			= ( WatcherInstance ) this.variables.get( Key.of( "fetchedOne" ) );
		WatcherInstance	started				= ( WatcherInstance ) this.variables.get( Key.of( "started" ) );
		WatcherInstance	restarted			= ( WatcherInstance ) this.variables.get( Key.of( "restarted" ) );
		IStruct			allWatchers			= this.variables.getAsStruct( Key.of( "allWatchers" ) );
		Array			watcherNames		= this.variables.getAsArray( Key.of( "watcherNames" ) );
		Array			namesAfterShutdown	= this.variables.getAsArray( Key.of( "namesAfterShutdown" ) );
		IStruct			allAfterShutdown	= this.variables.getAsStruct( Key.of( "allAfterShutdown" ) );

		assertThat( this.variables.getAsBoolean( Key.of( "existsOne" ) ) ).isTrue();
		assertThat( this.variables.getAsBoolean( Key.of( "existsAfterShutdown" ) ) ).isFalse();

		assertThat( fetchedOne ).isEqualTo( watcherOne );
		assertThat( started ).isEqualTo( watcherOne );
		assertThat( restarted ).isEqualTo( watcherOne );

		assertThat( allWatchers.containsKey( Key.of( "watcherOne" ) ) ).isTrue();
		assertThat( allWatchers.containsKey( Key.of( "watcherTwo" ) ) ).isTrue();
		assertThat( watcherNames ).contains( "watcherOne" );
		assertThat( watcherNames ).contains( "watcherTwo" );

		assertThat( namesAfterShutdown.size() ).isEqualTo( 0 );
		assertThat( allAfterShutdown.size() ).isEqualTo( 0 );
	}

	@DisplayName( "It throws when watcherGet is called for a missing watcher" )
	@Test
	public void testWatcherGetThrowsForMissingWatcher() {
		assertThrows( BoxRuntimeException.class, () -> instance.executeSource( "result = watcherGet( \"does-not-exist\" )", this.context ) );
	}

}
