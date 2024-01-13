
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
import ortus.boxlang.runtime.context.ScriptingBoxContext;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.util.FileSystemUtil;

public class FileExistsTest {

	static BoxRuntime	instance;
	static IBoxContext	context;
	static IStruct		variables;
	static Key			result	= new Key( "result" );

	@BeforeAll
	public static void setUp() {
		instance	= BoxRuntime.getInstance( true );
		context		= new ScriptingBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
	}

	@AfterAll
	public static void teardown() throws IOException {
		FileSystemUtil.deleteDirectory( "src/test/resources/tmp", true );
		instance.shutdown();
	}

	@BeforeEach
	public void setupEach() {
		variables.clear();
	}

	@DisplayName( "It tests the BIF FileExists on an existing file" )
	@Test
	public void testFileExists() throws IOException {
		String testFile = "src/test/resources/tmp/exists.txt";
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
		String testFile = "src/test/resources/tmp/not-exists.txt";
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
		String testFile = "src/test/resources/tmp";
		variables.put( Key.of( "testFile" ), Path.of( testFile ).toAbsolutePath().toString() );
		instance.executeSource(
		    """
		    result = fileExists( variables.testFile );
		    """,
		    context );
		Boolean result = ( Boolean ) variables.get( Key.of( "result" ) );
		assertFalse( result );
	}

}
