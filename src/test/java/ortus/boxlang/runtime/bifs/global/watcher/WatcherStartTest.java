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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.async.watchers.WatcherInstance;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;

public class WatcherStartTest {

	static BoxRuntime	instance;
	IBoxContext			context;
	IScope				variables;

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

	@DisplayName( "It starts a registered watcher and returns the instance" )
	@Test
	public void testWatcherStart() {
		// @formatter:off
		instance.executeSource(
		    """
		       	created = watcherNew( "myWatcher", [ "./src" ], ( event, watcherContext ) => {} )
		       	beforeState = created.getStateAsString()
		       	beforeRunning = created.isRunning()
		       	result  = watcherStart( "myWatcher" )
		    	afterState = result.getStateAsString()
		    	afterRunning = result.isRunning()
		       """,
		    this.context
		);
		// @formatter:on

		WatcherInstance	created			= ( WatcherInstance ) this.variables.get( Key.of( "created" ) );
		WatcherInstance	started			= ( WatcherInstance ) this.variables.get( Key.of( "result" ) );
		WatcherInstance	serviceWatcher	= instance.getWatcherService().getWatcherOrFail( Key.of( "myWatcher" ) );
		assertThat( started ).isEqualTo( created );
		assertThat( serviceWatcher ).isEqualTo( started );
		assertThat( this.variables.getAsString( Key.of( "beforeState" ) ) ).isEqualTo( "CREATED" );
		assertThat( this.variables.getAsBoolean( Key.of( "beforeRunning" ) ) ).isFalse();
		assertThat( this.variables.getAsString( Key.of( "afterState" ) ) ).isEqualTo( "RUNNING" );
		assertThat( this.variables.getAsBoolean( Key.of( "afterRunning" ) ) ).isTrue();
	}

	@DisplayName( "It is idempotent when starting an already running watcher" )
	@Test
	public void testWatcherStartWhenAlreadyRunning() {
		// @formatter:off
		instance.executeSource(
		    """
		       	created = watcherNew( "myWatcher", [ "./src" ], ( event, watcherContext ) => {} )
		       	first = watcherStart( "myWatcher" )
		       	second = watcherStart( "myWatcher" )
		       	firstRunning = first.isRunning()
		       	secondRunning = second.isRunning()
		       """,
		    this.context
		);
		// @formatter:on

		WatcherInstance	created	= ( WatcherInstance ) this.variables.get( Key.of( "created" ) );
		WatcherInstance	first	= ( WatcherInstance ) this.variables.get( Key.of( "first" ) );
		WatcherInstance	second	= ( WatcherInstance ) this.variables.get( Key.of( "second" ) );

		assertThat( first ).isEqualTo( created );
		assertThat( second ).isEqualTo( created );
		assertThat( this.variables.getAsBoolean( Key.of( "firstRunning" ) ) ).isTrue();
		assertThat( this.variables.getAsBoolean( Key.of( "secondRunning" ) ) ).isTrue();
	}

}
