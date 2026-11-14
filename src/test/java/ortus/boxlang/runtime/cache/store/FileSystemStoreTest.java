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
package ortus.boxlang.runtime.cache.store;

import static com.google.common.truth.Truth.assertThat;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sun.management.UnixOperatingSystemMXBean;

import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.util.FileSystemUtil;

class FileSystemStoreTest extends BaseStoreTest {

	static String tmpDirectory = "src/test/resources/tmp/FileSystemStoreTest";

	@AfterAll
	public static void teardown() {
		if ( FileSystemUtil.exists( tmpDirectory ) ) {
			FileSystemUtil.deleteDirectory( tmpDirectory, true );
		}
	}

	@BeforeAll
	static void setUp() {
		if ( !FileSystemUtil.exists( tmpDirectory ) ) {
			FileSystemUtil.createDirectory( tmpDirectory );
		}
		// Prep the fields to use in the base test
		mockProvider = getMockProvider( "test" );
		mockConfig.properties.put( Key.directory, tmpDirectory );
		store = new FileSystemStore().init( mockProvider, mockConfig.properties );
	}

	@Test
	@DisplayName( "FileSystemStore: rapid write-then-read returns all keys with no empty reads" )
	void testRapidWriteThenReadAllKeys() {
		int count = 200;
		for ( int i = 0; i < count; i++ ) {
			store.set( Key.of( "rapid-" + i ), newTestEntry( "rapid-" + i ) );
		}

		Key[] keys = store.getKeys();
		assertThat( keys ).hasLength( count );

		for ( Key key : keys ) {
			assertThat( store.getQuiet( key ) ).isNotNull();
		}
	}

	@Test
	@DisplayName( "FileSystemStore: getQuiet performs a direct key lookup" )
	void testGetQuietDirectAndMissing() {
		var entry = newTestEntry( "direct" );
		store.set( Key.of( "direct" ), entry );

		assertThat( store.getQuiet( Key.of( "direct" ) ) ).isEqualTo( entry );
		assertThat( store.getQuiet( Key.of( "missing" ) ) ).isNull();
	}

	@Test
	@DisplayName( "FileSystemStore: a key with characters illegal in a file name round-trips" )
	void testKeyWithIllegalFileCharacters() {
		// Compiler-injected call site keys contain ':'. That is legal in a cache key but not in a
		// Windows file name, so the key has to be encoded on the way to disk and decoded on the way back
		Key entryKey = Key.of( "pageid:1f6ebd7eff51118dcbd48817a71800ed_line:1_col:5" );
		store.set( entryKey, newTestEntry( entryKey.getName() ) );

		// The key survives the trip through the file name both directly and when enumerated
		assertThat( store.getQuiet( entryKey ) ).isNotNull();
		assertThat( store.getKeys() ).asList().contains( entryKey );
	}

	@Test
	@DisplayName( "FileSystemStore: two keys differing only in an illegal character do not collide" )
	void testIllegalFileCharactersDoNotCollide() {
		Key	colons	= Key.of( "a:b" );
		Key	percent	= Key.of( "a%3Ab" );
		store.set( colons, newTestEntry( "colons" ) );
		store.set( percent, newTestEntry( "percent" ) );

		// A naive escape would map both of these onto the same file name
		assertThat( store.getQuiet( colons ) ).isNotNull();
		assertThat( store.getQuiet( percent ) ).isNotNull();
		assertThat( store.getKeys() ).asList().containsAtLeast( colons, percent );
	}

	@Test
	@DisplayName( "FileSystemStore: no directory handle leak across repeated operations" )
	void testNoDirectoryHandleLeak() {
		OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
		if ( ! ( osBean instanceof UnixOperatingSystemMXBean unixBean ) ) {
			// Skip on non-Unix platforms where handle accounting is not available
			return;
		}

		store.set( Key.of( "leak" ), newTestEntry( "leak" ) );

		// Warm up so lazy initialization doesn't pollute the measurement
		for ( int i = 0; i < 100; i++ ) {
			store.getQuiet( Key.of( "leak" ) );
		}

		long before = unixBean.getOpenFileDescriptorCount();
		for ( int i = 0; i < 500; i++ ) {
			store.getQuiet( Key.of( "leak" ) );
			store.getKeysStream().count();
			store.getSize();
			store.lookup( Key.of( "leak" ) );
		}
		long after = unixBean.getOpenFileDescriptorCount();

		// Allow a small amount of JVM noise, but reject a linear leak (which would be ~500+)
		assertThat( after - before ).isAtMost( 20 );
	}

	@Test
	@DisplayName( "FileSystemStore: a corrupt entry file is a miss, not an error, and the next write heals it" )
	void testCorruptEntryIsAMiss() throws Exception {
		Key					key		= Key.of( "corrupt-entry" );
		java.nio.file.Path	path	= ( ( FileSystemStore ) store ).cacheKeyToPath( key );

		store.set( key, newTestEntry( "corrupt-entry" ) );
		assertThat( store.getQuiet( key ) ).isNotNull();

		// Truncate it the way a crash or a non-atomic writer would
		java.nio.file.Files.write( path, new byte[] { 1, 2, 3, 4, 5 } );

		// A reader must get a miss instead of an exception: the cache directory is shared between
		// processes, so a corrupt file is not something the caller did.
		assertThat( store.getQuiet( key ) ).isNull();
		assertThat( store.get( key ) ).isNull();

		// And a write of that key puts it back
		store.set( key, newTestEntry( "corrupt-entry" ) );
		assertThat( store.getQuiet( key ) ).isNotNull();
		assertThat( store.get( key ).value() ).isNotNull();
	}

	@Test
	@DisplayName( "FileSystemStore: an empty entry file is a miss, not an error" )
	void testEmptyEntryFileIsAMiss() throws Exception {
		Key					key		= Key.of( "empty-entry" );
		java.nio.file.Path	path	= ( ( FileSystemStore ) store ).cacheKeyToPath( key );

		store.set( key, newTestEntry( "empty-entry" ) );
		java.nio.file.Files.write( path, new byte[ 0 ] );

		assertThat( store.getQuiet( key ) ).isNull();
	}

	@Test
	@DisplayName( "FileSystemStore: writing an entry never leaves a partially written file behind" )
	void testWritesAreAtomic() throws Exception {
		Key				key		= Key.of( "atomic-write" );
		FileSystemStore	fsStore	= ( FileSystemStore ) store;

		store.set( key, newTestEntry( "atomic-write" ) );

		List<String> names = entryNames( fsStore.getDirectory() );
		assertThat( names ).contains( "atomic-write.cache" );
		// No temp file survives a completed write
		assertThat( names.stream().noneMatch( name -> name.endsWith( ".tmp" ) ) ).isTrue();
	}

	private List<String> entryNames( java.nio.file.Path dir ) throws java.io.IOException {
		try ( var stream = java.nio.file.Files.list( dir ) ) {
			return stream.map( entry -> entry.getFileName().toString() ).toList();
		}
	}

	@Test
	@DisplayName( "FileSystemStore: an orphaned temp file is cleaned up on init, a recent one is left alone" )
	void testOrphanedTempFilesArePurged() throws Exception {
		java.nio.file.Path	dir		= java.nio.file.Files.createTempDirectory( "fsstore-temp-purge" );
		java.nio.file.Path	oldTemp	= java.nio.file.Files.writeString( dir.resolve( "stale.cache123.tmp" ), "junk" );
		java.nio.file.Path	newTemp	= java.nio.file.Files.writeString( dir.resolve( "fresh.cache456.tmp" ), "junk" );

		java.nio.file.Files.setLastModifiedTime( oldTemp, java.nio.file.attribute.FileTime.fromMillis( System.currentTimeMillis() - 3_600_000 ) );

		try {
			FileSystemStore						other	= new FileSystemStore();
			ortus.boxlang.runtime.types.IStruct	config	= ortus.boxlang.runtime.types.Struct.of( Key.directory, dir.toString() );
			other.init( mockProvider, config );

			assertThat( java.nio.file.Files.exists( oldTemp ) ).isFalse();
			// Another process may be writing right now, so a recent temp file is not touched
			assertThat( java.nio.file.Files.exists( newTemp ) ).isTrue();
		} finally {
			FileSystemUtil.deleteDirectory( dir.toString(), true );
		}
	}

}
