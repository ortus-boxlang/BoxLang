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
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;

public class WatcherShutdownAllTest {

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

	@DisplayName( "It stops all watchers and removes them from the registry" )
	@Test
	public void testWatcherShutdownAll() {
		// @formatter:off
		instance.executeSource(
		    """
				one = watcherNew( "watcherOne", [ "./src" ], ( event, watcherContext ) => {} )
				two = watcherNew( "watcherTwo", [ "./src" ], ( event, watcherContext ) => {} )
				watcherShutdownAll( true )
				result = watcherList()
				oneRunning = one.isRunning()
				twoRunning = two.isRunning()
		       """,
		    this.context
		);
		// @formatter:on

		assertThat( this.variables.getAsArray( Key.of( "result" ) ).size() ).isEqualTo( 0 );
		assertThat( this.variables.getAsBoolean( Key.of( "oneRunning" ) ) ).isFalse();
		assertThat( this.variables.getAsBoolean( Key.of( "twoRunning" ) ) ).isFalse();
	}

}
