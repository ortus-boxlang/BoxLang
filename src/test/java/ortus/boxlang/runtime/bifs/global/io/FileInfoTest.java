
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.DateTime;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.util.FileSystemUtil;

public class FileInfoTest {

	static BoxRuntime	instance;
	IBoxContext			context;
	IScope				variables;
	static Key			result			= new Key( "result" );
	static String		testTextFile	= "src/test/resources/tmp/fileInfoTest/time.txt";
	static String		tmpDirectory	= "src/test/resources/tmp/fileInfoTest";

	@BeforeAll
	public static void setUp() throws IOException {
		instance = BoxRuntime.getInstance( true );

		if ( !FileSystemUtil.exists( testTextFile ) ) {
			FileSystemUtil.write( testTextFile, "file modified time test!".getBytes( "UTF-8" ), true );
		}
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

	@DisplayName( "It tests the BIF FileInfo" )
	@Test
	public void testFileInfo() throws IOException {
		variables.put( Key.of( "testFile" ), Path.of( testTextFile ).toAbsolutePath().toString() );
		instance.executeSource(
		    """
		    result = fileInfo( variables.testFile );
		       """,
		    context );
		assertTrue( variables.get( Key.of( "result" ) ) instanceof Struct );
		IStruct result = variables.getAsStruct( Key.of( "result" ) );
		assertTrue( result.containsKey( Key.of( "attributes" ) ) );
		assertTrue( result.get( "attributes" ) instanceof String );
		assertTrue( result.containsKey( Key.of( "checksum" ) ) );
		assertTrue( result.get( "checksum" ) instanceof String );
		System.out.println( result.getAsString( Key.of( "checksum" ) ) );
		assertThat( result.getAsString( Key.of( "checksum" ) ).length() ).isEqualTo( 32 );
		assertTrue( result.containsKey( Key.of( "dateLastModified" ) ) );
		assertTrue( result.get( "dateLastModified" ) instanceof DateTime );
		assertTrue( result.containsKey( Key.of( "execute" ) ) );
		assertTrue( result.get( "execute" ) instanceof Boolean );
		assertTrue( result.containsKey( Key.of( "mode" ) ) );
		assertTrue( result.get( "mode" ) instanceof String );
		if ( FileSystems.getDefault().supportedFileAttributeViews().contains( "posix" ) ) {
			assertEquals( StringCaster.cast( result.get( "mode" ) ).length(), 3 );
		}
		assertTrue( result.containsKey( Key.of( "name" ) ) );
		assertTrue( result.get( "name" ) instanceof String );
		assertTrue( result.containsKey( Key.of( "path" ) ) );
		assertTrue( result.get( "path" ) instanceof String );
		assertTrue( result.containsKey( Key.of( "read" ) ) );
		assertTrue( result.get( "read" ) instanceof Boolean );
		assertTrue( result.containsKey( Key.of( "size" ) ) );
		assertTrue( result.get( "size" ) instanceof Long );
		assertTrue( result.containsKey( Key.of( "type" ) ) );
		assertTrue( result.get( "type" ) instanceof String );
		assertEquals( ( String ) result.get( "type" ), "file" );
		assertTrue( result.containsKey( Key.of( "write" ) ) );
		assertTrue( result.get( "write" ) instanceof Boolean );
	}

	@DisplayName( "It tests the BIF FileInfo on a directory" )
	@Test
	public void testFileInfoDir() throws IOException {
		variables.put( Key.of( "testFile" ), Path.of( tmpDirectory ).toAbsolutePath().toString() );
		instance.executeSource(
		    """
		    result = fileInfo( variables.testFile );
		       """,
		    context );
		assertTrue( variables.get( Key.of( "result" ) ) instanceof Struct );
		IStruct result = variables.getAsStruct( Key.of( "result" ) );
		assertTrue( result.containsKey( Key.of( "attributes" ) ) );
		assertTrue( result.get( "attributes" ) instanceof String );
		assertTrue( result.containsKey( Key.of( "checksum" ) ) );
		assertTrue( result.get( "checksum" ) instanceof String );
		assertTrue( result.containsKey( Key.of( "dateLastModified" ) ) );
		assertTrue( result.get( "dateLastModified" ) instanceof DateTime );
		assertTrue( result.containsKey( Key.of( "execute" ) ) );
		assertTrue( result.get( "execute" ) instanceof Boolean );
		assertTrue( result.containsKey( Key.of( "mode" ) ) );
		assertTrue( result.get( "mode" ) instanceof String );
		if ( FileSystems.getDefault().supportedFileAttributeViews().contains( "posix" ) ) {
			assertEquals( StringCaster.cast( result.get( "mode" ) ).length(), 3 );
		}
		assertTrue( result.containsKey( Key.of( "name" ) ) );
		assertTrue( result.get( "name" ) instanceof String );
		assertTrue( result.containsKey( Key.of( "path" ) ) );
		assertTrue( result.get( "path" ) instanceof String );
		assertTrue( result.containsKey( Key.of( "read" ) ) );
		assertTrue( result.get( "read" ) instanceof Boolean );
		assertTrue( result.containsKey( Key.of( "size" ) ) );
		assertTrue( result.get( "size" ) instanceof Long );
		assertTrue( result.containsKey( Key.of( "type" ) ) );
		assertTrue( result.get( "type" ) instanceof String );
		assertEquals( ( String ) result.get( "type" ), "dir" );
		assertTrue( result.containsKey( Key.of( "write" ) ) );
		assertTrue( result.get( "write" ) instanceof Boolean );
	}

	@DisplayName( "It tests the File.info member function" )
	@Test
	public void testFileInfoMember() throws IOException {
		variables.put( Key.of( "testFile" ), Path.of( testTextFile ).toAbsolutePath().toString() );
		instance.executeSource(
		    """
		    file = fileOpen( variables.testFile );
		       result = file.info( variables.testFile );
		          """,
		    context );
		assertTrue( variables.get( Key.of( "result" ) ) instanceof Struct );
		IStruct result = variables.getAsStruct( Key.of( "result" ) );
		assertTrue( result.containsKey( Key.of( "attributes" ) ) );
		assertTrue( result.get( "attributes" ) instanceof String );
		assertTrue( result.containsKey( Key.of( "checksum" ) ) );
		assertTrue( result.get( "checksum" ) instanceof String );
		assertTrue( result.containsKey( Key.of( "dateLastModified" ) ) );
		assertTrue( result.get( "dateLastModified" ) instanceof DateTime );
		assertTrue( result.containsKey( Key.of( "execute" ) ) );
		assertTrue( result.get( "execute" ) instanceof Boolean );
		assertTrue( result.containsKey( Key.of( "mode" ) ) );
		assertTrue( result.get( "mode" ) instanceof String );
		if ( FileSystems.getDefault().supportedFileAttributeViews().contains( "posix" ) ) {
			assertEquals( StringCaster.cast( result.get( "mode" ) ).length(), 3 );
		}
		assertTrue( result.containsKey( Key.of( "name" ) ) );
		assertTrue( result.get( "name" ) instanceof String );
		assertTrue( result.containsKey( Key.of( "path" ) ) );
		assertTrue( result.get( "path" ) instanceof String );
		assertTrue( result.containsKey( Key.of( "read" ) ) );
		assertTrue( result.get( "read" ) instanceof Boolean );
		assertTrue( result.containsKey( Key.of( "size" ) ) );
		assertTrue( result.get( "size" ) instanceof Long );
		assertTrue( result.containsKey( Key.of( "type" ) ) );
		assertTrue( result.get( "type" ) instanceof String );
		assertEquals( ( String ) result.get( "type" ), "file" );
		assertTrue( result.containsKey( Key.of( "write" ) ) );
		assertTrue( result.get( "write" ) instanceof Boolean );
	}

	@DisplayName( "It tests the BIF GetFileInfo" )
	@Test
	public void testGetFileInfo() throws IOException {
		variables.put( Key.of( "testFile" ), Path.of( testTextFile ).toAbsolutePath().toString() );
		instance.executeSource(
		    """
		    result = getFileInfo( variables.testFile );
		       """,
		    context );
		assertTrue( variables.get( Key.of( "result" ) ) instanceof Struct );
		IStruct result = variables.getAsStruct( Key.of( "result" ) );
		assertTrue( result.containsKey( Key.of( "isAttributesSupported" ) ) );
		assertTrue( result.get( "isAttributesSupported" ) instanceof Boolean );
		assertTrue( result.containsKey( Key.of( "dateLastModified" ) ) );
		assertTrue( result.get( "dateLastModified" ) instanceof DateTime );
		assertTrue( result.containsKey( Key.of( "name" ) ) );
		assertTrue( result.get( "name" ) instanceof String );
		assertTrue( result.containsKey( Key.of( "path" ) ) );
		assertTrue( result.get( "path" ) instanceof String );
		assertTrue( result.containsKey( Key.of( "canRead" ) ) );
		assertTrue( result.get( "canRead" ) instanceof Boolean );
		assertTrue( result.containsKey( Key.of( "canWrite" ) ) );
		assertTrue( result.get( "canWrite" ) instanceof Boolean );
		assertTrue( result.containsKey( Key.of( "isArchive" ) ) );
		assertTrue( result.get( "isArchive" ) instanceof Boolean );
		assertTrue( result.containsKey( Key.of( "isCaseSensitive" ) ) );
		assertTrue( result.get( "isCaseSensitive" ) instanceof Boolean );
		assertTrue( result.containsKey( Key.of( "isHidden" ) ) );
		assertTrue( result.get( "isHidden" ) instanceof Boolean );
		assertTrue( result.containsKey( Key.of( "isModeSupported" ) ) );
		assertTrue( result.get( "isModeSupported" ) instanceof Boolean );
		assertTrue( result.containsKey( Key.of( "isSystem" ) ) );
		assertTrue( result.get( "isSystem" ) instanceof Boolean );
		assertTrue( result.containsKey( Key.of( "size" ) ) );
		assertTrue( result.get( "size" ) instanceof Long );
		assertTrue( result.containsKey( Key.of( "type" ) ) );
		assertTrue( result.get( "type" ) instanceof String );
		assertEquals( ( String ) result.get( "type" ), "file" );
	}

}
