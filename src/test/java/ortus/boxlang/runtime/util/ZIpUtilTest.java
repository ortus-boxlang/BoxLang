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
package ortus.boxlang.runtime.util;

import static com.google.common.truth.Truth.assertThat;

import java.io.File;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.types.Array;

public class ZIpUtilTest {

	String	sourceFolder	= "src/test/resources/";
	String	destination		= "build/test.zip";

	@BeforeEach
	public void setUp() {
		// Clean up any previous test files
		File file = new File( destination );
		if ( file.exists() ) {
			file.delete();
		}
	}

	@DisplayName( "Compress without base folder" )
	@Test
	public void testCompressUsingDefaults() {
		ZipUtil.compress( ZipUtil.COMPRESSION_FORMAT.ZIP, sourceFolder, destination, false, true );
		Array list = ZipUtil.listEntriesFlat( destination, "", false, null );
		// System.out.println( list );
		assertThat( list.toList() ).doesNotContain( "resources" );
		assertThat( list.size() ).isAtLeast( 3 );
	}

	@DisplayName( "Compress with base folder" )
	@Test
	public void testCompressWithBaseFolder() {
		ZipUtil.compress( ZipUtil.COMPRESSION_FORMAT.ZIP, sourceFolder, destination, true, true );
		Array list = ZipUtil.listEntriesFlat( destination, "", false, null );
		System.out.println( list );
		assertThat( list.toList() ).contains( "resources/" );
		assertThat( list.size() ).isAtMost( 1 );
	}

	@DisplayName( "Can delete entries" )
	@Test
	public void testDeleteEntries() {
		ZipUtil.compress( ZipUtil.COMPRESSION_FORMAT.ZIP, sourceFolder, destination, false, true );
		ZipUtil.deleteEntries( destination, "libs/*.*", null );
		Array list = ZipUtil.listEntriesFlat( destination, "", true, null );
		// System.out.println( list );
		assertThat( list.toList() ).doesNotContain( "libs/" );
	}

}
