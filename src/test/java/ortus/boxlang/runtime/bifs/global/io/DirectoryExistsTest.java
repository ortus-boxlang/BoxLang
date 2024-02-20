
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

public class DirectoryExistsTest {

	static BoxRuntime	instance;
	IBoxContext			context;
	IScope				variables;
	static Key			result			= new Key( "result" );
	static String		tmpDirectory	= "src/test/resources/tmp/directoryExistsTest";

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

	@DisplayName( "It tests the BIF DirectoryExists on an existing directory" )
	@Test
	public void testDirectoryExists() throws IOException {
		String testDirectory = tmpDirectory + "/foo";
		variables.put( Key.of( "testDirectory" ), Path.of( testDirectory ).toAbsolutePath().toString() );
		FileSystemUtil.createDirectory( testDirectory );
		assertTrue( FileSystemUtil.exists( testDirectory ) );
		instance.executeSource(
		    """
		    result = DirectoryExists( variables.testDirectory );
		    """,
		    context );
		Boolean result = ( Boolean ) variables.get( Key.of( "result" ) );
		assertTrue( result );
	}

	@DisplayName( "It tests the BIF DirectoryExists on a non-existent directory" )
	@Test
	public void testDirectoryNotExists() throws IOException {
		String testDirectory = tmpDirectory + "/blah";
		if ( FileSystemUtil.exists( testDirectory ) ) {
			FileSystemUtil.deleteDirectory( testDirectory, true );
		}
		variables.put( Key.of( "testDirectory" ), Path.of( testDirectory ).toAbsolutePath().toString() );
		instance.executeSource(
		    """
		    result = DirectoryExists( variables.testDirectory );
		    """,
		    context );
		Boolean result = ( Boolean ) variables.get( Key.of( "result" ) );
		assertFalse( result );
	}

	@DisplayName( "It tests the BIF DirectoryExists on a file" )
	@Test
	public void testDirectoryExistsFile() throws IOException {
		String testFile = tmpDirectory + "/a-file.txt";
		variables.put( Key.of( "testFile" ), Path.of( testFile ).toAbsolutePath().toString() );
		FileSystemUtil.write( testFile, "test directory!".getBytes( "UTF-8" ), true );
		assertTrue( FileSystemUtil.exists( testFile ) );
		instance.executeSource(
		    """
		    result = DirectoryExists( variables.testFile );
		    """,
		    context );
		Boolean result = ( Boolean ) variables.get( Key.of( "result" ) );
		assertFalse( result );
	}

}
