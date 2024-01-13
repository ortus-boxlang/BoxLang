
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Instant;
import java.util.Set;

import org.apache.commons.lang3.SystemUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
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
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.util.FileSystemUtil;

public class FileSetAttributeTest {

	static BoxRuntime	instance;
	static IBoxContext	context;
	static IScope		variables;
	static Key			result			= new Key( "result" );
	static String		testTextFile	= "src/test/resources/tmp/time.txt";
	static String		tmpDirectory	= "src/test/resources/tmp";
	static Instant		creationTime	= null;

	@BeforeAll
	public static void setUp() throws IOException {
		instance	= BoxRuntime.getInstance( true );
		context		= new ScriptingBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );

		Assumptions.assumeTrue( FileSystems.getDefault().supportedFileAttributeViews().contains( "posix" ),
		    "The underlying file system is not posix compliant." );

		if ( !FileSystemUtil.exists( testTextFile ) ) {
			FileSystemUtil.write( testTextFile, "file modified time test!".getBytes( "UTF-8" ), true );
		}

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
		variables.clear();
		Files.setPosixFilePermissions( Path.of( testTextFile ), PosixFilePermissions.fromString( "rw-r--r--" ) );
	}

	@DisplayName( "It tests the ability to set the attribute on a file object" )
	@Test
	public void testFileSetAttribute() throws IOException {
		variables.put( Key.of( "testFile" ), Path.of( testTextFile ).toAbsolutePath().toString() );
		Set<PosixFilePermission> initialPermissions = FileSystemUtil.getPosixPermissions( testTextFile );
		assertEquals( initialPermissions, PosixFilePermissions.fromString( "rw-r--r--" ) );
		instance.executeSource(
		    """
		    file = FileOpen( variables.testFile );
		    result = fileSetAttribute( file, "readonly" );
		    file.close();
		       """,
		    context );
		Set<PosixFilePermission> finalPermissions = FileSystemUtil.getPosixPermissions( testTextFile );
		assertEquals( finalPermissions, PosixFilePermissions.fromString( "r--r--r--" ) );

	}

	@DisplayName( "It tests the File.setAttribute member function" )
	@Test
	public void testFileSetAttributeMember() throws IOException {
		variables.put( Key.of( "testFile" ), Path.of( testTextFile ).toAbsolutePath().toString() );
		Set<PosixFilePermission> initialPermissions = FileSystemUtil.getPosixPermissions( testTextFile );
		assertEquals( initialPermissions, PosixFilePermissions.fromString( "rw-r--r--" ) );
		instance.executeSource(
		    """
		    file = FileOpen( variables.testFile );
		    result = file.setAttribute( "readonly" );
		    file.close();
		       """,
		    context );
		Set<PosixFilePermission> finalPermissions = FileSystemUtil.getPosixPermissions( testTextFile );
		assertEquals( finalPermissions, PosixFilePermissions.fromString( "r--r--r--" ) );

	}

	@DisplayName( "It tests the ability to set the access mode on a file with a path" )
	@Test
	public void testFilePathSetAttribute() throws IOException {
		variables.put( Key.of( "testFile" ), Path.of( testTextFile ).toAbsolutePath().toString() );
		Set<PosixFilePermission> initialPermissions = FileSystemUtil.getPosixPermissions( testTextFile );
		assertEquals( initialPermissions, PosixFilePermissions.fromString( "rw-r--r--" ) );
		instance.executeSource(
		    """
		    result = fileSetAttribute( variables.testFile, "readonly" );
		       """,
		    context );
		Set<PosixFilePermission> finalPermissions = FileSystemUtil.getPosixPermissions( testTextFile );
		assertEquals( finalPermissions, PosixFilePermissions.fromString( "r--r--r--" ) );
	}

	@DisplayName( "It tests that an invalid attribute will throw an error" )
	@Test
	public void testModeError() throws IOException {
		variables.put( Key.of( "testFile" ), Path.of( testTextFile ).toAbsolutePath().toString() );
		assertThrows(
		    BoxRuntimeException.class,
		    () -> instance.executeSource(
		        """
		        result = fileSetAttribute( testFile, "blah" );
		           """,
		        context )
		);
	}

	@DisplayName( "It tests the ability to restore the default permissions" )
	@Test
	public void testSetAttributeNormal() throws IOException {
		variables.put( Key.of( "testFile" ), Path.of( testTextFile ).toAbsolutePath().toString() );
		Files.setPosixFilePermissions( Path.of( testTextFile ), PosixFilePermissions.fromString( "r--r--r--" ) );
		Set<PosixFilePermission> initialPermissions = FileSystemUtil.getPosixPermissions( testTextFile );
		assertEquals( initialPermissions, PosixFilePermissions.fromString( "r--r--r--" ) );
		instance.executeSource(
		    """
		    result = fileSetAttribute( variables.testFile, "normal" );
		       """,
		    context );
		Set<PosixFilePermission> finalPermissions = FileSystemUtil.getPosixPermissions( testTextFile );
		assertEquals( finalPermissions, PosixFilePermissions.fromString( "rw-rw-r--" ) );
	}

	@DisplayName( "It tests ability to set or ignore the 'hidden' attribute" )
	@Test
	public void testSetAttributeHidden() throws IOException {
		System.out.println( SystemUtils.IS_OS_WINDOWS );
		assertFalse( FileSystemUtil.isWindows );
		variables.put( Key.of( "testFile" ), Path.of( testTextFile ).toAbsolutePath().toString() );
		Set<PosixFilePermission> initialPermissions = FileSystemUtil.getPosixPermissions( testTextFile );
		assertEquals( initialPermissions, PosixFilePermissions.fromString( "rw-r--r--" ) );
		instance.executeSource(
		    """
		    result = fileSetAttribute( variables.testFile, "hidden" );
		       """,
		    context );
		if ( !FileSystemUtil.isWindows ) {
			Set<PosixFilePermission> finalPermissions = FileSystemUtil.getPosixPermissions( testTextFile );
			assertEquals( finalPermissions, initialPermissions );
		} else {
			assertTrue( ( Boolean ) Files.getAttribute( Path.of( testTextFile ), "dos:hidden" ) );
		}
	}

	@DisplayName( "It tests ability to set or ignore the 'system' attribute" )
	@Test
	public void testSetAttributeSystem() throws IOException {
		variables.put( Key.of( "testFile" ), Path.of( testTextFile ).toAbsolutePath().toString() );
		Set<PosixFilePermission> initialPermissions = FileSystemUtil.getPosixPermissions( testTextFile );
		assertEquals( initialPermissions, PosixFilePermissions.fromString( "rw-r--r--" ) );
		instance.executeSource(
		    """
		    result = fileSetAttribute( variables.testFile, "system" );
		       """,
		    context );
		if ( !FileSystemUtil.isWindows ) {
			Set<PosixFilePermission> finalPermissions = FileSystemUtil.getPosixPermissions( testTextFile );
			assertEquals( finalPermissions, initialPermissions );
		} else {
			assertTrue( ( Boolean ) Files.getAttribute( Path.of( testTextFile ), "dos:system" ) );
		}
	}

	@DisplayName( "It tests ability to set or ignore the 'archive' attribute" )
	@Test
	public void testSetAttributeArchive() throws IOException {
		variables.put( Key.of( "testFile" ), Path.of( testTextFile ).toAbsolutePath().toString() );
		Set<PosixFilePermission> initialPermissions = FileSystemUtil.getPosixPermissions( testTextFile );
		assertEquals( initialPermissions, PosixFilePermissions.fromString( "rw-r--r--" ) );
		instance.executeSource(
		    """
		    result = fileSetAttribute( variables.testFile, "archive" );
		       """,
		    context );
		if ( !FileSystemUtil.isWindows ) {
			Set<PosixFilePermission> finalPermissions = FileSystemUtil.getPosixPermissions( testTextFile );
			assertEquals( finalPermissions, initialPermissions );
		} else {
			assertTrue( ( Boolean ) Files.getAttribute( Path.of( testTextFile ), "dos:archive" ) );
		}
	}

}
