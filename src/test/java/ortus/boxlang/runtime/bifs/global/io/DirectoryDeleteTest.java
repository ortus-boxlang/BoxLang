
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

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.util.FileSystemUtil;

public class DirectoryDeleteTest {

	static BoxRuntime	instance;
	IBoxContext			context;
	IScope				variables;
	static Key			result			= new Key( "result" );
	static String		tmpDirectory	= "src/test/resources/tmp/directoryDeleteTest";
	static String		testDirectory	= tmpDirectory + "/foo";

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
	public void setupEach() throws IOException {
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
		if ( !FileSystemUtil.exists( tmpDirectory ) ) {
			FileSystemUtil.createDirectory( tmpDirectory );
		}
		if ( FileSystemUtil.exists( testDirectory ) ) {
			FileSystemUtil.deleteDirectory( testDirectory, true );
		}
	}

	@DisplayName( "It tests the BIF DirectoryDelete" )
	@Test
	public void testDirectoryDelete() throws IOException {
		String testFile = testDirectory + "/test.txt";
		variables.put( Key.of( "testDirectory" ), Path.of( testDirectory ).toAbsolutePath().toString() );
		FileSystemUtil.createDirectory( testDirectory );
		FileSystemUtil.write( testFile, "directory delete test!" );
		assertTrue( FileSystemUtil.exists( testDirectory ) );
		instance.executeSource(
		    """
		    directoryDelete( variables.testDirectory, true );
		    """,
		    context );
		assertFalse( FileSystemUtil.exists( testDirectory ) );
	}

	@DisplayName( "It tests the BIF DirectoryDelete without a recursive arg" )
	@Test
	public void testDirectoryDeleteError() throws IOException {
		String	testDirectory	= "src/test/resources/tmp/foo";
		String	testFile		= testDirectory + "/test.txt";
		variables.put( Key.of( "testDirectory" ), Path.of( testDirectory ).toAbsolutePath().toString() );
		FileSystemUtil.createDirectory( testDirectory );
		FileSystemUtil.write( testFile, "directory delete test!" );
		assertTrue( FileSystemUtil.exists( testDirectory ) );
		assertThrows(
		    BoxRuntimeException.class,
		    () -> instance.executeSource(
		        """
		        directoryDelete( variables.testDirectory );
		        """,
		        context )
		);
	}

}
