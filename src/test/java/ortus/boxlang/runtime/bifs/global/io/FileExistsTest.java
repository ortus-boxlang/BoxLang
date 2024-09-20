
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

package ortus.boxlang.runtime.bifs.global.io;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterAll;
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
import ortus.boxlang.runtime.util.FileSystemUtil;

public class FileExistsTest {

	static BoxRuntime	instance;
	IBoxContext			context;
	IScope				variables;
	static Key			result			= new Key( "result" );
	static String		tmpDirectory	= "src/test/resources/tmp/FileExistsTest";

	@BeforeAll
	public static void setUp() {
		instance = BoxRuntime.getInstance( true );
	}

	@AfterAll
	public static void teardown() throws IOException {
		if ( FileSystemUtil.exists( tmpDirectory ) ) {
			FileSystemUtil.deleteDirectory( tmpDirectory, true );
		}
	}

	@BeforeEach
	public void setupEach() {
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
	}

	@DisplayName( "It tests the BIF FileExists on an existing file" )
	@Test
	public void testFileExists() throws IOException {
		String testFile = tmpDirectory + "exists.txt";
		variables.put( Key.of( "testFile" ), Path.of( testFile ).toAbsolutePath().toString() );
		FileSystemUtil.write( testFile, "file exists test!".getBytes( "UTF-8" ), true );
		assertTrue( FileSystemUtil.exists( testFile ) );
		instance.executeSource(
		    """
		    result = fileExists( variables.testFile );
		    """,
		    context );
		Boolean result = ( Boolean ) variables.get( Key.of( "result" ) );
		assertTrue( result );
	}

	@DisplayName( "It tests the BIF FileExists on a non-existent file" )
	@Test
	public void testFileNotExists() throws IOException {
		String testFile = tmpDirectory + "not-exists.txt";
		variables.put( Key.of( "testFile" ), Path.of( testFile ).toAbsolutePath().toString() );
		instance.executeSource(
		    """
		    result = fileExists( variables.testFile );
		    """,
		    context );
		Boolean result = ( Boolean ) variables.get( Key.of( "result" ) );
		assertFalse( result );
	}

	@DisplayName( "It tests the BIF FileExists on a directory" )
	@Test
	public void testDirectoryNotExists() throws IOException {
		String testFile = tmpDirectory;
		variables.put( Key.of( "testFile" ), Path.of( testFile ).toAbsolutePath().toString() );
		instance.executeSource(
		    """
		    result = fileExists( variables.testFile );
		    """,
		    context );
		Boolean result = ( Boolean ) variables.get( Key.of( "result" ) );
		assertFalse( result );
	}

	@DisplayName( "It tests the BIF FileExists on an invalid directory" )
	@Test
	public void testDirectoryInvalidPath() throws IOException {
		instance.executeSource(
		    """
		    result = fileExists( "C://invalid/path/file.txt" );
		    """,
		    context );
		Boolean result = ( Boolean ) variables.get( Key.of( "result" ) );
		assertFalse( result );
	}

	@DisplayName( "It ignores invalid paths" )
	@Test
	public void testIgnoresInvalidPaths() {
		instance.executeSource(
		    """
		    result = fileExists( "C:foo/bar/C:foo/bar/test.txt" );
		    """,
		    context );
		assertThat( variables.get( Key.of( "result" ) ) ).isEqualTo( false );
	}

}
