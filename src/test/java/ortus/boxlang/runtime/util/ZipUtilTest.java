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
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.context.RequestBoxContext;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

public class ZipUtilTest {

	String	sourceFolder	= "src/test/resources/";
	String	destination;

	@BeforeEach
	public void setUp() {
		// Uniqueness just in case
		destination = "build/test" + java.util.UUID.randomUUID().toString() + ".zip";
		// Clean up any previous test files
		File file = new File( destination );
		if ( file.exists() ) {
			file.delete();
		}
	}

	@DisplayName( "Compress without base folder" )
	@Test
	public void testCompressUsingDefaults() throws IOException {
		destination = "build/test" + java.util.UUID.randomUUID().toString() + ".zip";
		ZipUtil.compress(
		    ZipUtil.COMPRESSION_FORMAT.ZIP,
		    sourceFolder,
		    destination,
		    false,
		    true,
		    null,
		    null,
		    false,
		    ZipUtil.DEFAULT_COMPRESSION_LEVEL,
		    null
		);
		Array list = ZipUtil.listEntriesFlat( destination, "", false, null );
		// System.out.println( list );
		assertThat( list.toList() ).doesNotContain( "resources" );
		assertThat( list.size() ).isAtLeast( 3 );

		String extractedPath = "src/test/resources/tmp/ZipExtractTest";
		// Now extract with a filter
		ZipUtil.extractZip(
		    destination,
		    extractedPath,
		    true,
		    true,
		    "*.jpg",
		    null,
		    RequestBoxContext.getCurrent()
		);

		assertThat(
		    Files.walk( Path.of( extractedPath ).toAbsolutePath() )
		        .filter( path -> FileSystemUtil.matchesType( path, "file" ) )
		        .allMatch( path -> path.getFileName().toString().contains( "jpg" ) )
		).isTrue();
	}

	@DisplayName( "Compress with base folder" )
	@Test
	public void testCompressWithBaseFolder() {
		ZipUtil.compress(
		    ZipUtil.COMPRESSION_FORMAT.ZIP,
		    sourceFolder,
		    destination,
		    true,
		    true,
		    null,
		    null,
		    false,
		    ZipUtil.DEFAULT_COMPRESSION_LEVEL,
		    null
		);
		Array list = ZipUtil.listEntriesFlat( destination, "", false, null );
		System.out.println( list );
		assertThat( list.toList() ).contains( "resources/" );
		assertThat( list.size() ).isAtMost( 1 );
	}

	@DisplayName( "Can delete entries" )
	@Test
	public void testDeleteEntries() {
		ZipUtil.compress(
		    ZipUtil.COMPRESSION_FORMAT.ZIP,
		    sourceFolder,
		    destination,
		    false,
		    true,
		    null,
		    null,
		    false,
		    ZipUtil.DEFAULT_COMPRESSION_LEVEL,
		    null
		);
		ZipUtil.deleteEntries( destination, "libs/*.*", new Array(), null );
		Array list = ZipUtil.listEntriesFlat( destination, "", true, null );
		// System.out.println( list );
		assertThat( list.toList() ).doesNotContain( "libs/" );
	}

	@DisplayName( "Compress using gzip and max compression level" )
	@Test
	public void testCompressUsingGzipWithMaxCompression() {
		String gzipDestination = "build/test" + java.util.UUID.randomUUID().toString() + ".gz";
		ZipUtil.compress(
		    ZipUtil.COMPRESSION_FORMAT.GZIP,
		    sourceFolder,
		    gzipDestination,
		    false,
		    true,
		    null,
		    null,
		    false,
		    9, // Maximum compression level
		    null
		);
		// Assert that the gzip file was created
		File gzipFile = new File( gzipDestination );
		assertThat( gzipFile.exists() ).isTrue();
		assertThat( gzipFile.length() ).isGreaterThan( 0 );
	}

	@DisplayName( "Compress with maximum compression level" )
	@Test
	public void testCompressWithMaxCompression() {
		ZipUtil.compress(
		    ZipUtil.COMPRESSION_FORMAT.ZIP,
		    sourceFolder,
		    destination,
		    false,
		    true,
		    null,
		    null,
		    false,
		    9, // Maximum compression level
		    null
		);
		Array list = ZipUtil.listEntriesFlat( destination, "", false, null );
		assertThat( list.size() ).isAtLeast( 3 );
		assertThat( list.toList() ).doesNotContain( "resources" );
	}

	@DisplayName( "Compress with min compression level" )
	@Test
	public void testCompressWithMinCompression() {
		ZipUtil.compress(
		    ZipUtil.COMPRESSION_FORMAT.ZIP,
		    sourceFolder,
		    destination,
		    false,
		    true,
		    null,
		    null,
		    false,
		    0, // Minimum compression level
		    null
		);
		Array list = ZipUtil.listEntriesFlat( destination, "", false, null );
		assertThat( list.size() ).isAtLeast( 3 );
		assertThat( list.toList() ).doesNotContain( "resources" );
	}

	@DisplayName( "Throw an exception if an invalid compression level is used" )
	@Test
	public void testInvalidCompressionLevel() {
		assertThrows( BoxRuntimeException.class, () -> {
			ZipUtil.compress(
			    ZipUtil.COMPRESSION_FORMAT.ZIP,
			    sourceFolder,
			    destination,
			    false,
			    true,
			    null,
			    null,
			    false,
			    -1, // Invalid compression level
			    null
			);
		} );
	}

}
