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

}
