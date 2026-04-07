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

import java.nio.file.Paths;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.async.watchers.WatcherInstance;
import ortus.boxlang.runtime.async.watchers.listeners.ClassListener;
import ortus.boxlang.runtime.async.watchers.listeners.ClosureListener;
import ortus.boxlang.runtime.async.watchers.listeners.StructListener;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

public class WatcherNewTest {

	static BoxRuntime	instance;
	IBoxContext			context;
	IScope				variables;

	@BeforeAll
	public static void setUp() {
		instance = BoxRuntime.getInstance( true );
		String mappingPath = Paths.get( "src/test/resources" ).toAbsolutePath().toString();
		instance.getConfiguration().registerMapping( "/watchertests", mappingPath );
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

	@DisplayName( "It creates and registers a new watcher instance" )
	@Test
	public void testWatcherNew() {
		// @formatter:off
		instance.executeSource(
		    """
		       	result = watcherNew( "myWatcher", [ "./src" ], ( event, watcherContext ) => {} )
		    	running = result.isRunning()
		       """,
		    this.context
		);
		// @formatter:on

		WatcherInstance watcher = ( WatcherInstance ) this.variables.get( Key.of( "result" ) );
		assertThat( watcher ).isNotNull();
		assertThat( instance.getWatcherService().hasWatcher( Key.of( "myWatcher" ) ) ).isTrue();
		assertThat( this.variables.getAsBoolean( Key.of( "running" ) ) ).isFalse();
	}

	@DisplayName( "It accepts a single path string and wraps a closure listener" )
	@Test
	public void testWatcherNewWithSinglePathStringAndClosureListener() {
		// @formatter:off
		instance.executeSource(
		    """
		      result = watcherNew( "singlePathWatcher", "./src", ( event ) => {} )
		    """,
		    this.context
		);
		// @formatter:on

		WatcherInstance watcher = ( WatcherInstance ) this.variables.get( Key.of( "result" ) );
		assertThat( watcher.getWatchPaths() ).hasSize( 1 );
		assertThat( watcher.getListener() ).isInstanceOf( ClosureListener.class );
	}

	@DisplayName( "It can resolve a struct listener map into a StructListener" )
	@Test
	public void testWatcherNewWithStructListener() {
		// @formatter:off
		instance.executeSource(
		    """
		      result = watcherNew(
		        "structListenerWatcher",
		        [ "./src" ],
		        {
		          onEvent: ( event ) => {}
		        }
		      )
		    """,
		    this.context
		);
		// @formatter:on

		WatcherInstance watcher = ( WatcherInstance ) this.variables.get( Key.of( "result" ) );
		assertThat( watcher.getListener() ).isInstanceOf( StructListener.class );
	}

	@DisplayName( "It can resolve a class name listener into a ClassListener" )
	@Test
	public void testWatcherNewWithClassListener() {
		// @formatter:off
		instance.executeSource(
		    """
		      result = watcherNew( "classListenerWatcher", [ "./src" ], "watchertests.components.Listener" )
		    """,
		    this.context
		);
		// @formatter:on

		WatcherInstance watcher = ( WatcherInstance ) this.variables.get( Key.of( "result" ) );
		assertThat( watcher.getListener() ).isInstanceOf( ClassListener.class );
	}

	@DisplayName( "It auto-generates a watcher name when none is provided" )
	@Test
	public void testWatcherNewAutoGeneratesName() {
		// @formatter:off
		instance.executeSource(
		    """
		      result = watcherNew( "", [ "./src" ], ( event ) => {} )
		      generatedName = result.getName().getName()
		    """,
		    this.context
		);
		// @formatter:on

		String generatedName = this.variables.getAsString( Key.of( "generatedName" ) );
		assertThat( generatedName ).startsWith( "watcher-" );
		assertThat( instance.getWatcherService().hasWatcher( Key.of( generatedName ) ) ).isTrue();
	}

	@DisplayName( "It throws when paths is not a string or array" )
	@Test
	public void testWatcherNewInvalidPathsType() {
		assertThrows(
		    BoxRuntimeException.class,
		    () -> instance.executeSource(
		        "result = watcherNew( \"badPaths\", 123, ( event ) => {} )",
		        this.context
		    )
		);
	}

	@DisplayName( "It throws when listener type is unsupported" )
	@Test
	public void testWatcherNewInvalidListenerType() {
		assertThrows(
		    BoxRuntimeException.class,
		    () -> instance.executeSource(
		        "result = watcherNew( \"badListener\", [ \"./src\" ], 42 )",
		        this.context
		    )
		);
	}

	@DisplayName( "It throws on duplicate watcher name unless force is true" )
	@Test
	public void testWatcherNewDuplicateAndForce() {
		// @formatter:off
		instance.executeSource(
		    """
		      watcherNew( "dupWatcher", [ "./src" ], ( event ) => {} )
		    """,
		    this.context
		);
		// @formatter:on

		assertThrows(
		    BoxRuntimeException.class,
		    () -> instance.executeSource(
		        "watcherNew( \"dupWatcher\", [ \"./src\" ], ( event ) => {} )",
		        this.context
		    )
		);

		// @formatter:off
		instance.executeSource(
		    """
		      replaced = watcherNew( "dupWatcher", [ "./src" ], ( event ) => {}, true, 0, 0, true, 0, 10, true )
		      replacedName = replaced.getName().getName()
		    """,
		    this.context
		);
		// @formatter:on

		assertThat( this.variables.getAsString( Key.of( "replacedName" ) ) ).isEqualTo( "dupWatcher" );
		assertThat( instance.getWatcherService().hasWatcher( Key.of( "dupWatcher" ) ) ).isTrue();
	}

}
