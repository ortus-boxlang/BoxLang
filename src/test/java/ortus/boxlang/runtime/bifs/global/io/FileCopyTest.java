
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

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.util.FileSystemUtil;

public class FileCopyTest {

	static BoxRuntime	instance;
	static IBoxContext	context;
	static IScope		variables;
	static Key			result					= new Key( "result" );
	static String		tmpDirectory			= "src/test/resources/tmp";
	static String		sourceFile				= "src/test/resources/tmp/source.txt";
	static String		destinationFile			= "src/test/resources/tmp/destination.txt";
	static String		nestedDestinationFile	= "src/test/resources/tmp/nested/further/destination.txt";

	@BeforeAll
	public static void setUp() {
		instance	= BoxRuntime.getInstance( true );
		context		= new ScriptingBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );

	}

	@AfterAll
	public static void teardown() throws IOException {
		if ( FileSystemUtil.exists( tmpDirectory ) ) {
			FileSystemUtil.deleteDirectory( tmpDirectory, true );
		}
		instance.shutdown();
	}

	@BeforeEach
	public void setupEach() throws IOException {
		if ( FileSystemUtil.exists( destinationFile ) ) {
			FileSystemUtil.deleteFile( destinationFile );
		}
		if ( !FileSystemUtil.exists( sourceFile ) ) {
			FileSystemUtil.write( sourceFile, "copy me", "utf-8", true );

		}
		variables.clear();
	}

	@AfterEach
	public void teardownEach() throws IOException {
		Files.deleteIfExists( Path.of( destinationFile ) );
		if ( FileSystemUtil.exists( "src/test/resources/tmp/nested" ) ) {
			FileSystemUtil.deleteDirectory( "src/test/resources/tmp/nested", true );
		}
		variables.clear();
	}

	@DisplayName( "It tests the BIF FileCopy" )
	@Test
	public void testDefaultFileCopy() throws IOException {

		variables.put( Key.of( "source" ), Path.of( sourceFile ).toAbsolutePath().toString() );
		variables.put( Key.of( "destination" ), Path.of( destinationFile ).toAbsolutePath().toString() );
		assertTrue( FileSystemUtil.exists( sourceFile ) );
		assertFalse( FileSystemUtil.exists( destinationFile ) );
		instance.executeSource(
		    """
		    fileCopy( source, destination );
		    """,
		    context );
		assertTrue( FileSystemUtil.exists( destinationFile ) );
		assertTrue( FileSystemUtil.read( destinationFile, "utf-8", null ).equals( "copy me" ) );
	}

	@DisplayName( "It tests the BIF FileCopy will create the nested destinatioin paths by default" )
	@Test
	public void testNestedFileCopy() throws IOException {

		variables.put( Key.of( "source" ), Path.of( sourceFile ).toAbsolutePath().toString() );
		variables.put( Key.of( "destination" ), Path.of( nestedDestinationFile ).toAbsolutePath().toString() );
		assertTrue( FileSystemUtil.exists( sourceFile ) );
		assertFalse( FileSystemUtil.exists( nestedDestinationFile ) );
		instance.executeSource(
		    """
		    fileCopy( source, destination );
		    """,
		    context );
		assertTrue( FileSystemUtil.exists( nestedDestinationFile ) );
		assertTrue( FileSystemUtil.read( nestedDestinationFile, "utf-8", null ).equals( "copy me" ) );
	}

	@DisplayName( "It tests that file copy error with createPath set to false" )
	@Test
	public void testFileCopy() throws IOException {
		variables.put( Key.of( "source" ), Path.of( sourceFile ).toAbsolutePath().toString() );
		variables.put( Key.of( "destination" ), Path.of( nestedDestinationFile ).toAbsolutePath().toString() );
		assertTrue( FileSystemUtil.exists( sourceFile ) );
		assertFalse( FileSystemUtil.exists( nestedDestinationFile ) );
		assertThrows(
		    RuntimeException.class,
		    () -> instance.executeSource(
		        """
		        fileCopy( source, destination, false );
		        """,
		        context )
		);
	}

}
