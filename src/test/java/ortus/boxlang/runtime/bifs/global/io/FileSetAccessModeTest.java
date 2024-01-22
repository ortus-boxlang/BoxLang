
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
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Instant;
import java.util.Set;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
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
import ortus.boxlang.runtime.types.File;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.util.FileSystemUtil;

public class FileSetAccessModeTest {

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
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
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
		Assumptions.assumeTrue( FileSystems.getDefault().supportedFileAttributeViews().contains( "posix" ),
		    "The underlying file system for path [src/test/resources/tmp/time.txt] is not posix compliant." );

		FileSystemUtil.setPosixPermissions( testTextFile, "555" );
	}

	@DisplayName( "It tests the ability to set the access mode on a file object" )
	@Test
	public void testFileSetAccessMode() throws IOException {
		variables.put( Key.of( "testFile" ), Path.of( testTextFile ).toAbsolutePath().toString() );
		Set<PosixFilePermission> initialPermissions = FileSystemUtil.getPosixPermissions( testTextFile );

		assertFalse( initialPermissions.contains( PosixFilePermission.OWNER_WRITE ) );
		assertFalse( initialPermissions.contains( PosixFilePermission.GROUP_WRITE ) );
		assertFalse( initialPermissions.contains( PosixFilePermission.OTHERS_WRITE ) );
		assertTrue( initialPermissions.contains( PosixFilePermission.OWNER_EXECUTE ) );
		assertTrue( initialPermissions.contains( PosixFilePermission.OTHERS_EXECUTE ) );
		assertTrue( initialPermissions.contains( PosixFilePermission.GROUP_EXECUTE ) );
		assertEquals( initialPermissions.size(), 6 );
		instance.executeSource(
		    """
		    file = FileOpen( variables.testFile );
		    result = fileSetAccessMode( file, 777 );
		    file.close();
		       """,
		    context );
		Set<PosixFilePermission> finalPermissions = FileSystemUtil.getPosixPermissions( testTextFile );
		assertTrue( variables.get( Key.of( "result" ) ) instanceof File );
		assertEquals( finalPermissions.size(), 9 );
		assertTrue( finalPermissions.contains( PosixFilePermission.OTHERS_WRITE ) );
		assertTrue( finalPermissions.contains( PosixFilePermission.GROUP_WRITE ) );
		assertTrue( finalPermissions.contains( PosixFilePermission.OTHERS_EXECUTE ) );
		assertTrue( finalPermissions.contains( PosixFilePermission.GROUP_EXECUTE ) );

	}

	@DisplayName( "It tests the File.setAccessMode member function" )
	@Test
	public void testFileSetAccessModeMember() throws IOException {
		variables.put( Key.of( "testFile" ), Path.of( testTextFile ).toAbsolutePath().toString() );
		Set<PosixFilePermission> initialPermissions = FileSystemUtil.getPosixPermissions( testTextFile );

		assertFalse( initialPermissions.contains( PosixFilePermission.OWNER_WRITE ) );
		assertFalse( initialPermissions.contains( PosixFilePermission.GROUP_WRITE ) );
		assertFalse( initialPermissions.contains( PosixFilePermission.OTHERS_WRITE ) );
		assertTrue( initialPermissions.contains( PosixFilePermission.OWNER_EXECUTE ) );
		assertTrue( initialPermissions.contains( PosixFilePermission.OTHERS_EXECUTE ) );
		assertTrue( initialPermissions.contains( PosixFilePermission.GROUP_EXECUTE ) );
		assertEquals( initialPermissions.size(), 6 );
		instance.executeSource(
		    """
		    file = FileOpen( variables.testFile );
		    file.setAccessMode( 777 );
		    file.close();
		       """,
		    context );
		Set<PosixFilePermission> finalPermissions = FileSystemUtil.getPosixPermissions( testTextFile );
		assertEquals( finalPermissions.size(), 9 );
		assertTrue( finalPermissions.contains( PosixFilePermission.OTHERS_WRITE ) );
		assertTrue( finalPermissions.contains( PosixFilePermission.GROUP_WRITE ) );
		assertTrue( finalPermissions.contains( PosixFilePermission.OTHERS_EXECUTE ) );
		assertTrue( finalPermissions.contains( PosixFilePermission.GROUP_EXECUTE ) );

	}

	@DisplayName( "It tests the ability to set the access mode on a file with a path" )
	@Test
	public void testFilePathSetAccessMode() throws IOException {

		variables.put( Key.of( "testFile" ), Path.of( testTextFile ).toAbsolutePath().toString() );
		Set<PosixFilePermission> initialPermissions = FileSystemUtil.getPosixPermissions( testTextFile );

		assertFalse( initialPermissions.contains( PosixFilePermission.OWNER_WRITE ) );
		assertFalse( initialPermissions.contains( PosixFilePermission.GROUP_WRITE ) );
		assertFalse( initialPermissions.contains( PosixFilePermission.OTHERS_WRITE ) );
		assertTrue( initialPermissions.contains( PosixFilePermission.OWNER_EXECUTE ) );
		assertTrue( initialPermissions.contains( PosixFilePermission.OTHERS_EXECUTE ) );
		assertTrue( initialPermissions.contains( PosixFilePermission.GROUP_EXECUTE ) );
		assertEquals( initialPermissions.size(), 6 );
		instance.executeSource(
		    """
		    result = fileSetAccessMode( variables.testFile, 777 );
		       """,
		    context );
		Set<PosixFilePermission> finalPermissions = FileSystemUtil.getPosixPermissions( testTextFile );
		assertEquals( finalPermissions.size(), 9 );
		assertTrue( finalPermissions.contains( PosixFilePermission.OTHERS_WRITE ) );
		assertTrue( finalPermissions.contains( PosixFilePermission.GROUP_WRITE ) );
		assertTrue( finalPermissions.contains( PosixFilePermission.OTHERS_EXECUTE ) );
		assertTrue( finalPermissions.contains( PosixFilePermission.GROUP_EXECUTE ) );
	}

	@DisplayName( "It tests that an invalid mode will throw an error" )
	@Test
	public void testModeError() throws IOException {
		variables.put( Key.of( "testFile" ), Path.of( testTextFile ).toAbsolutePath().toString() );
		assertThrows(
		    BoxRuntimeException.class,
		    () -> instance.executeSource(
		        """
		        result = fileSetAccessMode( testFile, "blah" );
		           """,
		        context )
		);
	}

}
