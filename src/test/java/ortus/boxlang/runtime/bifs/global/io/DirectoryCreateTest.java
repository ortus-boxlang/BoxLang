
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
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.util.FileSystemUtil;

public class DirectoryCreateTest {

	static BoxRuntime	instance;
	IBoxContext			context;
	IScope				variables;
	static Key			result			= new Key( "result" );
	static String		tmpDirectory	= "src/test/resources/tmp/directoryCreateTest";
	static String		targetDirectory	= "src/test/resources/tmp/directoryCreateTest/nested/path";

	@BeforeAll
	public static void setUp() {
		instance = BoxRuntime.getInstance( true );
	}

	@AfterAll
	public static void teardown() {

	}

	@BeforeEach
	public void setupEach() throws IOException {
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
		if ( FileSystemUtil.exists( tmpDirectory + "/nested" ) ) {
			FileSystemUtil.deleteDirectory( tmpDirectory + "/nested", true );
		}

	}

	@DisplayName( "It tests the BIF DirectoryCreate with the defaults" )
	@Test
	public void testDirectoryCreateDefault() {
		variables.put( Key.of( "destination" ), Path.of( targetDirectory ).toAbsolutePath().toString() );
		assertFalse( FileSystemUtil.exists( targetDirectory ) );
		instance.executeSource(
		    """
		    directoryCreate( destination );
		    """,
		    context );
		assertTrue( FileSystemUtil.exists( targetDirectory ) );
	}

	@DisplayName( "It tests the BIF DirectoryCreate will throw an error if createPath is false" )
	@Test
	public void testNoCreatePaths() {
		variables.put( Key.of( "destination" ), Path.of( targetDirectory ).toAbsolutePath().toString() );
		assertFalse( FileSystemUtil.exists( targetDirectory ) );
		assertThrows(
		    RuntimeException.class,
		    () -> instance.executeSource(
		        """
		        directoryCreate( destination, false );
		        """,
		        context )
		);
	}

	@DisplayName( "It tests the BIF DirectoryCreate defaults will throw an error if the directory exists" )
	@Test
	public void testIgnoreExistsFalse() {
		variables.put( Key.of( "destination" ), Path.of( targetDirectory ).toAbsolutePath().toString() );
		assertFalse( FileSystemUtil.exists( targetDirectory ) );
		instance.executeSource(
		    """
		    directoryCreate( destination );
		    """,
		    context );
		assertTrue( FileSystemUtil.exists( targetDirectory ) );
		assertThrows(
		    BoxRuntimeException.class,
		    () -> instance.executeSource(
		        """
		        directoryCreate( destination );
		        """,
		        context )
		);
	}

	@DisplayName( "It tests the BIF DirectoryCreate defaults will not throw an error if ignoreExists is true" )
	@Test
	public void testIgnoreExists() {
		variables.put( Key.of( "destination" ), Path.of( targetDirectory ).toAbsolutePath().toString() );
		assertFalse( FileSystemUtil.exists( targetDirectory ) );
		instance.executeSource(
		    """
		    directoryCreate( destination );
		    """,
		    context );
		assertTrue( FileSystemUtil.exists( targetDirectory ) );
		instance.executeSource(
		    """
		    directoryCreate( destination, true, true );
		    """,
		    context );
		assertTrue( FileSystemUtil.exists( targetDirectory ) );
	}

}
