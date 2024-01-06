
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

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
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.util.FileSystemUtil;

public class DirectoryMoveTest {

	static BoxRuntime	instance;
	static IBoxContext	context;
	static IScope		variables;
	static Key			result			= new Key( "result" );
	static String		tmpDirectory	= "src/test/resources/tmp";
	static String		source			= "src/test/resources/tmp/start";
	static String		destination		= "src/test/resources/tmp/end";
	static Object[]		sourceObjects	= new Object[] {
	    "test.txt",
	    "nested/test.txt",
	    "nested/further/test.txt",
	    "nested/further/test.md"
	};

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
		if ( FileSystemUtil.exists( destination ) ) {
			FileSystemUtil.deleteDirectory( destination, true );
		}
		if ( !FileSystemUtil.exists( source ) ) {
			FileSystemUtil.createDirectory( source );
			Stream.of( sourceObjects ).forEach( item -> {
				String	target		= source + "/" + item;
				Path	itemPath	= Path.of( target );
				if ( Files.isDirectory( itemPath ) ) {
					try {
						FileSystemUtil.createDirectory( target );
					} catch ( IOException e ) {
						throw new RuntimeException( e );
					}
				} else {
					try {
						FileSystemUtil.write( target, "move me", "utf-8", true );
					} catch ( IOException e ) {
						throw new RuntimeException( e );
					}
				}
			} );
		}
		variables.clear();
	}

	@AfterEach
	public void teardownEach() throws IOException {
		if ( FileSystemUtil.exists( destination ) ) {
			FileSystemUtil.deleteDirectory( destination, true );
		}
		variables.clear();
	}

	@DisplayName( "It tests the BIF DirectoryMove" )
	@Test
	public void testDirectoryMove() {
		variables.put( Key.of( "source" ), Path.of( source ).toAbsolutePath().toString() );
		variables.put( Key.of( "destination" ), Path.of( destination ).toAbsolutePath().toString() );
		assertTrue( FileSystemUtil.exists( source ) );
		assertFalse( FileSystemUtil.exists( destination ) );
		instance.executeSource(
		    """
		    DirectoryMove( source, destination );
		    """,
		    context );
		assertTrue( FileSystemUtil.exists( destination ) );
		assertFalse( FileSystemUtil.exists( source ) );
	}

	@DisplayName( "It tests the BIF DirectoryMove with false createPaths" )
	@Test
	public void testNoCreatePathsMove() {
		String nestedDestination = destination + "/even/more/nested/than/before";
		variables.put( Key.of( "source" ), Path.of( source ).toAbsolutePath().toString() );
		variables.put( Key.of( "destination" ), Path.of( nestedDestination ).toAbsolutePath().toString() );
		assertTrue( FileSystemUtil.exists( source ) );
		assertFalse( FileSystemUtil.exists( nestedDestination ) );
		assertThrows(
		    BoxRuntimeException.class,
		    () -> instance.executeSource(
		        """
		        DirectoryMove( source, destination, false );
		        """,
		        context )
		);

	}

	@DisplayName( "It tests the BIF DirectoryMove with an existing target directory" )
	@Test
	public void testExistingTargetMove() throws IOException {
		variables.put( Key.of( "source" ), Path.of( source ).toAbsolutePath().toString() );
		variables.put( Key.of( "destination" ), Path.of( destination ).toAbsolutePath().toString() );
		assertTrue( FileSystemUtil.exists( source ) );
		Files.createDirectories( Path.of( destination ) );
		assertTrue( FileSystemUtil.exists( destination ) );
		assertThrows(
		    BoxRuntimeException.class,
		    () -> instance.executeSource(
		        """
		        DirectoryMove( source, destination, false );
		        """,
		        context )
		);

	}

	@DisplayName( "It tests the BIF DirectoryRename" )
	@Test
	public void testDirectoryRename() {
		variables.put( Key.of( "source" ), Path.of( source ).toAbsolutePath().toString() );
		variables.put( Key.of( "destination" ), Path.of( destination ).toAbsolutePath().toString() );
		assertTrue( FileSystemUtil.exists( source ) );
		assertFalse( FileSystemUtil.exists( destination ) );
		instance.executeSource(
		    """
		    DirectoryRename( source, destination );
		    """,
		    context );
		assertTrue( FileSystemUtil.exists( destination ) );
		assertFalse( FileSystemUtil.exists( source ) );
	}

}
