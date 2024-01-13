
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
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.util.FileSystemUtil;

public class DirectoryCopyTest {

	static BoxRuntime	instance;
	static IBoxContext	context;
	static IStruct		variables;
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

	@DisplayName( "It tests the BIF DirectoryCopy without Recursion" )
	@Test
	public void testShallowDirectoryCopy() {

		variables.put( Key.of( "source" ), Path.of( source ).toAbsolutePath().toString() );
		variables.put( Key.of( "destination" ), Path.of( destination ).toAbsolutePath().toString() );
		assertTrue( FileSystemUtil.exists( source ) );
		assertFalse( FileSystemUtil.exists( destination ) );
		instance.executeSource(
		    """
		    directoryCopy( source, destination );
		    """,
		    context );
		assertTrue( FileSystemUtil.exists( destination ) );
		assertTrue( FileSystemUtil.exists( destination + "/test.txt" ) );
		assertFalse( FileSystemUtil.exists( destination + "/nested" ) );
	}

	@DisplayName( "It tests the BIF DirectoryCopy with Filter" )
	@Test
	public void testFilteredDirectoryCopy() {

		variables.put( Key.of( "source" ), Path.of( source ).toAbsolutePath().toString() );
		variables.put( Key.of( "destination" ), Path.of( destination ).toAbsolutePath().toString() );
		assertTrue( FileSystemUtil.exists( source ) );
		assertFalse( FileSystemUtil.exists( destination ) );
		instance.executeSource(
		    """
		    directoryCopy( source, destination, true, "*.md" );
		    """,
		    context );
		assertTrue( FileSystemUtil.exists( destination ) );
		assertFalse( FileSystemUtil.exists( destination + "/test.txt" ) );
		assertTrue( FileSystemUtil.exists( destination + "/nested" ) );
		assertFalse( FileSystemUtil.exists( destination + "/nested/test.txt" ) );
		assertTrue( FileSystemUtil.exists( destination + "/nested/further" ) );
		assertTrue( FileSystemUtil.exists( destination + "/nested/further/test.md" ) );
	}

	@DisplayName( "It tests the BIF DirectoryCopy with Recursion" )
	@Test
	public void testRecursiveDirectoryCopy() {
		variables.put( Key.of( "source" ), Path.of( source ).toAbsolutePath().toString() );
		variables.put( Key.of( "destination" ), Path.of( destination ).toAbsolutePath().toString() );
		assertTrue( FileSystemUtil.exists( source ) );
		assertFalse( FileSystemUtil.exists( destination ) );
		instance.executeSource(
		    """
		    directoryCopy( source, destination, true );
		    """,
		    context );
		assertTrue( FileSystemUtil.exists( destination ) );
		Stream.of( sourceObjects ).forEach( item -> {
			String target = destination + "/" + item;
			assertTrue( FileSystemUtil.exists( target ) );
		} );
	}

}
