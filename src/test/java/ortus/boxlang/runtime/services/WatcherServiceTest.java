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
package ortus.boxlang.runtime.services;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.watchers.WatcherContext;
import ortus.boxlang.runtime.watchers.WatcherEvent;
import ortus.boxlang.runtime.watchers.WatcherInstance;
import ortus.boxlang.runtime.watchers.listeners.IWatcherListener;

class WatcherServiceTest {

	private WatcherService	service;
	private BoxRuntime		runtime;

	@BeforeEach
	void setupBeforeEach() {
		this.runtime	= BoxRuntime.getInstance();
		this.service	= this.runtime.getWatcherService();
		this.service.shutdownAll( false );
	}

	@DisplayName( "It can create the watcher service" )
	@Test
	void testItCanCreateIt() {
		assertThat( this.service ).isNotNull();
	}

	@DisplayName( "It can register and lookup a watcher" )
	@Test
	void testItCanRegisterAndLookupAWatcher() {
		WatcherInstance watcher = buildWatcher( "watcherOne" );

		this.service.register( watcher, false );

		assertThat( this.service.size() ).isEqualTo( 1 );
		assertThat( this.service.hasWatcher( Key.of( "watcherOne" ) ) ).isTrue();
		assertThat( this.service.getWatcher( Key.of( "watcherOne" ) ) ).isEqualTo( watcher );
		assertThat( this.service.getWatcherNames().toList() ).contains( "watcherOne" );
	}

	@DisplayName( "It throws when registering a duplicate watcher without force" )
	@Test
	void testItThrowsOnDuplicateRegistrationWithoutForce() {
		this.service.register( buildWatcher( "duplicateWatcher" ), false );

		assertThrows( BoxRuntimeException.class, () -> this.service.register( buildWatcher( "duplicateWatcher" ), false ) );
	}

	@DisplayName( "It can replace a watcher when force is true" )
	@Test
	void testItCanForceReplaceAWatcher() {
		WatcherInstance	original	= buildWatcher( "replaceableWatcher" );
		WatcherInstance	replacement	= buildWatcher( "replaceableWatcher" );

		this.service.register( original, false );
		this.service.register( replacement, true );

		assertThat( this.service.size() ).isEqualTo( 1 );
		assertThat( this.service.getWatcher( Key.of( "replaceableWatcher" ) ) ).isEqualTo( replacement );
	}

	@DisplayName( "It can remove an existing watcher" )
	@Test
	void testItCanRemoveAWatcher() {
		this.service.register( buildWatcher( "toRemove" ), false );

		boolean removed = this.service.removeWatcher( Key.of( "toRemove" ) );

		assertThat( removed ).isTrue();
		assertThat( this.service.hasWatcher( Key.of( "toRemove" ) ) ).isFalse();
		assertThat( this.service.size() ).isEqualTo( 0 );
	}

	@DisplayName( "It returns false when removing a missing watcher" )
	@Test
	void testItReturnsFalseWhenRemovingMissingWatcher() {
		boolean removed = this.service.removeWatcher( Key.of( "missingWatcher" ) );

		assertThat( removed ).isFalse();
	}

	@DisplayName( "It throws when getting an unknown watcher with getWatcherOrFail" )
	@Test
	void testItThrowsOnGetWatcherOrFailForUnknownWatcher() {
		assertThrows( BoxRuntimeException.class, () -> this.service.getWatcherOrFail( Key.of( "doesNotExist" ) ) );
	}

	@DisplayName( "It can shutdown all watchers and clear the registry" )
	@Test
	void testItCanShutdownAllWatchers() {
		this.service.register( buildWatcher( "shutdownOne" ), false );
		this.service.register( buildWatcher( "shutdownTwo" ), false );

		this.service.shutdownAll( false );

		assertThat( this.service.size() ).isEqualTo( 0 );
		assertThat( this.service.getWatchers() ).isEmpty();
	}

	private WatcherInstance buildWatcher( String name ) {
		return WatcherInstance.builder( Key.of( name ) )
		    .addPath( "./src" )
		    .parentContext( this.runtime.getRuntimeContext() )
		    .listener( new NoopWatcherListener() )
		    .build();
	}

	private static class NoopWatcherListener implements IWatcherListener {

		@Override
		public void onEvent( WatcherEvent event, WatcherContext context ) {
			// no-op
		}
	}

}
