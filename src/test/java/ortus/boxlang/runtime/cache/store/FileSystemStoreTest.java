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

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

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

}
