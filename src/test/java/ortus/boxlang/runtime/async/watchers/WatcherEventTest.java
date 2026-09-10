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
package ortus.boxlang.runtime.async.watchers;

import static com.google.common.truth.Truth.assertThat;

import java.nio.file.Path;
import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.async.watchers.WatcherEvent.Kind;
import ortus.boxlang.runtime.types.IStruct;

class WatcherEventTest {

	private static final Path		watchRoot		= Path.of( "/var/www" );
	private static final Path		absolutePath	= Path.of( "/var/www/app/file.bx" );
	private static final Path		relativePath	= Path.of( "app/file.bx" );
	private static final Instant	timestamp		= Instant.parse( "2026-04-07T12:00:00Z" );

	@Test
	@DisplayName( "It stores all fields correctly for a CREATED event" )
	void testCreatedEvent() {
		WatcherEvent event = new WatcherEvent( Kind.CREATED, absolutePath, relativePath, watchRoot, timestamp );

		assertThat( event.getKind() ).isEqualTo( Kind.CREATED );
		assertThat( event.getPath() ).isEqualTo( absolutePath );
		assertThat( event.getRelativePath() ).isEqualTo( relativePath );
		assertThat( event.getWatchRoot() ).isEqualTo( watchRoot );
		assertThat( event.getTimestamp() ).isEqualTo( timestamp );
	}

	@Test
	@DisplayName( "It stores all fields correctly for a MODIFIED event" )
	void testModifiedEvent() {
		WatcherEvent event = new WatcherEvent( Kind.MODIFIED, absolutePath, relativePath, watchRoot, timestamp );

		assertThat( event.getKind() ).isEqualTo( Kind.MODIFIED );
		assertThat( event.getPath() ).isEqualTo( absolutePath );
	}

	@Test
	@DisplayName( "It stores all fields correctly for a DELETED event" )
	void testDeletedEvent() {
		WatcherEvent event = new WatcherEvent( Kind.DELETED, absolutePath, relativePath, watchRoot, timestamp );

		assertThat( event.getKind() ).isEqualTo( Kind.DELETED );
		assertThat( event.getPath() ).isEqualTo( absolutePath );
	}

	@Test
	@DisplayName( "It sets kind to OVERFLOW and nulls path fields" )
	void testOverflowEvent() {
		WatcherEvent event = new WatcherEvent( timestamp );

		assertThat( event.getKind() ).isEqualTo( Kind.OVERFLOW );
		assertThat( event.getPath() ).isNull();
		assertThat( event.getRelativePath() ).isNull();
		assertThat( event.getWatchRoot() ).isNull();
		assertThat( event.getTimestamp() ).isEqualTo( timestamp );
	}

	@Test
	@DisplayName( "It converts a normal event to a BoxLang struct with correct values" )
	void testNormalEventToStruct() {
		WatcherEvent	event	= new WatcherEvent( Kind.CREATED, absolutePath, relativePath, watchRoot, timestamp );
		IStruct			struct	= event.toStruct();

		assertThat( struct ).isNotNull();
		assertThat( struct.getAsString( ortus.boxlang.runtime.scopes.Key.kind ) ).isEqualTo( "created" );
		assertThat( struct.getAsString( ortus.boxlang.runtime.scopes.Key.path ) ).isEqualTo( absolutePath.toString() );
		assertThat( struct.getAsString( ortus.boxlang.runtime.scopes.Key.relativePath ) ).isEqualTo( relativePath.toString() );
		assertThat( struct.getAsString( ortus.boxlang.runtime.scopes.Key.watchRoot ) ).isEqualTo( watchRoot.toString() );
		assertThat( struct.getAsString( ortus.boxlang.runtime.scopes.Key.timestamp ) ).isEqualTo( timestamp.toString() );
	}

	@Test
	@DisplayName( "It converts a MODIFIED event to a struct with kind 'modified'" )
	void testModifiedEventKindInStruct() {
		WatcherEvent	event	= new WatcherEvent( Kind.MODIFIED, absolutePath, relativePath, watchRoot, timestamp );
		IStruct			struct	= event.toStruct();

		assertThat( struct.getAsString( ortus.boxlang.runtime.scopes.Key.kind ) ).isEqualTo( "modified" );
	}

	@Test
	@DisplayName( "It converts a DELETED event to a struct with kind 'deleted'" )
	void testDeletedEventKindInStruct() {
		WatcherEvent	event	= new WatcherEvent( Kind.DELETED, absolutePath, relativePath, watchRoot, timestamp );
		IStruct			struct	= event.toStruct();

		assertThat( struct.getAsString( ortus.boxlang.runtime.scopes.Key.kind ) ).isEqualTo( "deleted" );
	}

	@Test
	@DisplayName( "It converts an OVERFLOW event to a struct with empty path strings" )
	void testOverflowEventToStruct() {
		WatcherEvent	event	= new WatcherEvent( timestamp );
		IStruct			struct	= event.toStruct();

		assertThat( struct.getAsString( ortus.boxlang.runtime.scopes.Key.kind ) ).isEqualTo( "overflow" );
		assertThat( struct.getAsString( ortus.boxlang.runtime.scopes.Key.path ) ).isEmpty();
		assertThat( struct.getAsString( ortus.boxlang.runtime.scopes.Key.relativePath ) ).isEmpty();
		assertThat( struct.getAsString( ortus.boxlang.runtime.scopes.Key.watchRoot ) ).isEmpty();
	}

	@Test
	@DisplayName( "It includes kind and path in the string representation" )
	void testToString() {
		WatcherEvent	event	= new WatcherEvent( Kind.CREATED, absolutePath, relativePath, watchRoot, timestamp );

		String			result	= event.toString();

		assertThat( result ).contains( "CREATED" );
		assertThat( result ).contains( absolutePath.toString() );
		assertThat( result ).contains( timestamp.toString() );
	}

	@Test
	@DisplayName( "It handles null path in OVERFLOW toString without throwing" )
	void testOverflowToString() {
		WatcherEvent	event	= new WatcherEvent( timestamp );
		String			result	= event.toString();

		assertThat( result ).contains( "OVERFLOW" );
	}
}
