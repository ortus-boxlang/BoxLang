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
package ortus.boxlang.runtime.types;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.util.FileSystemUtil;

public class FileTest {

	private static String	tmpDirectory	= "src/test/resources/tmp";
	private static String	testFile		= "src/test/resources/tmp/file-test.txt";
	private static String	emptyFile		= "src/test/resources/tmp/file-write-test.txt";
	private static File		readFile		= null;
	private static File		writeFile		= null;

	@AfterAll
	public static void teardown() throws IOException {
		if ( readFile != null ) {
			readFile.close();
		}
		if ( writeFile != null ) {
			writeFile.close();
		}
		if ( FileSystemUtil.exists( tmpDirectory ) ) {
			FileSystemUtil.deleteDirectory( tmpDirectory, true );
		}
	}

	@BeforeEach
	public void setupEach() throws IOException {
		if ( !FileSystemUtil.exists( testFile ) ) {
			FileSystemUtil.write( testFile, "open file test!".getBytes( "UTF-8" ), true );
		}
		if ( FileSystemUtil.exists( emptyFile ) ) {
			FileSystemUtil.deleteFile( emptyFile );
		}
	}

	@DisplayName( "Test Constructors" )
	@Test
	void testConstructors() {
		// tests the default constructor
		readFile = new File( testFile );
		assertThat( readFile.filename ).isEqualTo( "file-test.txt" );
		assertFalse( readFile.isEOF() );
		readFile.close();

		// tests the constructor with the read mode
		readFile = new File( testFile, "read" );
		assertThat( readFile.filename ).isEqualTo( "file-test.txt" );
		assertFalse( readFile.isEOF() );
		assertThat( readFile.readLine() ).isEqualTo( "open file test!" );
		readFile.close();

		// tests the constructor with the read mode
		writeFile = new File( testFile, "write" );
		assertThat( writeFile.filename ).isEqualTo( "file-test.txt" );
		assertThat( writeFile.mode ).isEqualTo( "write" );

		// tests the constructor with the append mode
		writeFile = new File( testFile, "append" );
		assertThat( writeFile.filename ).isEqualTo( "file-test.txt" );
		assertThat( writeFile.mode ).isEqualTo( "append" );
	}

}
