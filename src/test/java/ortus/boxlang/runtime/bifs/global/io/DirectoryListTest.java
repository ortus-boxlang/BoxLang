
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
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.DateTime;
import ortus.boxlang.runtime.types.Query;
import ortus.boxlang.runtime.util.FileSystemUtil;

public class DirectoryListTest {

	static BoxRuntime	instance;
	IBoxContext			context;
	IScope				variables;
	static Key			result			= new Key( "result" );
	static String		tmpDirectory	= "src/test/resources/tmp/directoryListTest";
	static String		testDirectory	= tmpDirectory + "/foo";
	static String		testFile1		= testDirectory + "/test.txt";
	static String		testDirectory2	= testDirectory + "/bar";
	static String		testFile2		= testDirectory2 + "/atest.txt";

	@BeforeAll
	public static void setUp() throws IOException {
		instance = BoxRuntime.getInstance( true );
		FileSystemUtil.createDirectory( testDirectory );
		FileSystemUtil.write( testFile1, "directory list test!" );
		FileSystemUtil.createDirectory( testDirectory2 );
		FileSystemUtil.write( testFile2, "test nested directory file" );
	}

	@AfterAll
	public static void teardown() throws IOException {
		FileSystemUtil.deleteDirectory( testDirectory, true );

	}

	@BeforeEach
	public void setupEach() {
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
	}

	@DisplayName( "It tests the BIF DirectoryList with the default arguments" )
	@Test
	public void testDirectoryListBif() {
		variables.put( Key.of( "testDirectory" ), Path.of( testDirectory ).toAbsolutePath().toString() );
		assertTrue( FileSystemUtil.exists( testDirectory ) );
		instance.executeSource(
		    """
		    result = directoryList( variables.testDirectory );
		    """,
		    context );
		var result = variables.get( Key.of( "result" ) );
		assertTrue( result instanceof Array );
		Array listing = ( Array ) result;
		assertTrue( listing.size() == 3 );
		for ( var i = 0; i < listing.size(); i++ ) {
			assertTrue( listing.get( i ) instanceof String );
		}
		assertThat( listing.get( 0 ) ).isEqualTo( Path.of( testDirectory ).toAbsolutePath().toString() );
		assertThat( listing.get( 1 ) ).isEqualTo( Path.of( testDirectory2 ).toAbsolutePath().toString() );
		assertThat( listing.get( 2 ) ).isEqualTo( Path.of( testFile1 ).toAbsolutePath().toString() );

	}

	@DisplayName( "It tests the BIF DirectoryList with the recursive option" )
	@Test
	public void testRecursiveDirectoryListBif() {
		variables.put( Key.of( "testDirectory" ), Path.of( testDirectory ).toAbsolutePath().toString() );
		assertTrue( FileSystemUtil.exists( testDirectory ) );
		instance.executeSource(
		    """
		    result = directoryList( variables.testDirectory, true );
		    """,
		    context );
		var result = variables.get( Key.of( "result" ) );
		assertTrue( result instanceof Array );
		Array listing = ( Array ) result;
		assertTrue( listing.size() == 4 );
		for ( var i = 0; i < listing.size(); i++ ) {
			assertTrue( listing.get( i ) instanceof String );
		}
		assertThat( listing.get( 0 ) ).isEqualTo( Path.of( testDirectory ).toAbsolutePath().toString() );
		assertThat( listing.get( 1 ) ).isEqualTo( Path.of( testDirectory2 ).toAbsolutePath().toString() );
		assertThat( listing.get( 2 ) ).isEqualTo( Path.of( testFile2 ).toAbsolutePath().toString() );
		assertThat( listing.get( 3 ) ).isEqualTo( Path.of( testFile1 ).toAbsolutePath().toString() );

	}

	@DisplayName( "It tests the BIF DirectoryList with a listInfo of names" )
	@Test
	public void testNamesDirectoryListBif() {
		variables.put( Key.of( "testDirectory" ), Path.of( testDirectory ).toAbsolutePath().toString() );
		assertTrue( FileSystemUtil.exists( testDirectory ) );
		instance.executeSource(
		    """
		    result = directoryList( variables.testDirectory, false, "name" );
		    """,
		    context );
		var result = variables.get( Key.of( "result" ) );
		assertTrue( result instanceof Array );
		Array listing = ( Array ) result;
		assertTrue( listing.size() == 3 );
		for ( var i = 0; i < listing.size(); i++ ) {
			assertTrue( listing.get( i ) instanceof String );
		}
		assertThat( listing.get( 0 ) ).isEqualTo( testDirectory.split( "/" )[ testDirectory.split( "/" ).length - 1 ] );
		assertThat( listing.get( 1 ) ).isEqualTo( testDirectory2.split( "/" )[ testDirectory2.split( "/" ).length - 1 ] );
		assertThat( listing.get( 2 ) ).isEqualTo( testFile1.split( "/" )[ testFile1.split( "/" ).length - 1 ] );

	}

	@DisplayName( "It tests the BIF DirectoryList with a listInfo of query" )
	@Test
	public void testQueryDirectoryListBif() {
		variables.put( Key.of( "testDirectory" ), Path.of( testDirectory ).toAbsolutePath().toString() );
		assertTrue( FileSystemUtil.exists( testDirectory ) );
		instance.executeSource(
		    """
		    result = directoryList( variables.testDirectory, false, "query" );
		    """,
		    context );
		var result = variables.get( Key.of( "result" ) );
		assertTrue( result instanceof Query );
		Query listing = ( Query ) result;
		assertTrue( listing.size() == 3 );
		for ( var i = 0; i < listing.size(); i++ ) {
			Object[] currentRow = listing.getRow( i );
			assertTrue( currentRow.length == 7 );
			assertThat( listing.getCell( Key.of( "name" ), i ) ).isInstanceOf( String.class );
			assertThat( listing.getCell( Key.size, i ) ).isInstanceOf( Long.class );
			assertThat( listing.getCell( Key.dateLastModified, i ) ).isInstanceOf( DateTime.class );
			assertThat( listing.getCell( Key.attributes, i ) ).isInstanceOf( String.class );
			assertThat( listing.getCell( Key.mode, i ) ).isInstanceOf( String.class );
			assertThat( listing.getCell( Key.directory, i ) ).isInstanceOf( String.class );
			if ( listing.getCell( Key.attributes, i ).equals( "File" ) ) {
				assertThat( listing.getCell( Key.type, i ) ).isInstanceOf( String.class );
			}

		}

	}

	@DisplayName( "It tests the BIF DirectoryList with a filter option" )
	@Test
	public void testFilterDirectoryListBif() {
		variables.put( Key.of( "testDirectory" ), Path.of( testDirectory ).toAbsolutePath().toString() );
		assertTrue( FileSystemUtil.exists( testDirectory ) );
		instance.executeSource(
		    """
		    result = directoryList( variables.testDirectory, true, "name", "*.txt" );
		    """,
		    context );
		var result = variables.get( Key.of( "result" ) );
		assertTrue( result instanceof Array );
		Array listing = ( Array ) result;
		assertTrue( listing.size() == 2 );
		for ( var i = 0; i < listing.size(); i++ ) {
			assertTrue( listing.get( i ) instanceof String );
			String fileName = ( String ) listing.get( i );
			assertTrue( fileName.split( "\\." ).length == 2 );
			assertThat( fileName.split( "\\." )[ fileName.split( "\\." ).length - 1 ] ).isEqualTo( "txt" );
		}
	}

	@DisplayName( "It tests the BIF DirectoryList with a sort option" )
	@Test
	public void testSortDirectoryListBif() {
		variables.put( Key.of( "testDirectory" ), Path.of( testDirectory ).toAbsolutePath().toString() );
		assertTrue( FileSystemUtil.exists( testDirectory ) );
		instance.executeSource(
		    """
		    result = directoryList( variables.testDirectory, true, "name", "*.txt", "name desc" );
		    """,
		    context );
		var result = variables.get( Key.of( "result" ) );
		assertTrue( result instanceof Array );
		Array listing = ( Array ) result;
		assertTrue( listing.size() == 2 );
		assertThat( listing.get( 0 ) ).isEqualTo( "test.txt" );
		assertThat( listing.get( 1 ) ).isEqualTo( "atest.txt" );
	}

	@DisplayName( "It tests the BIF DirectoryList with a type option" )
	@Test
	public void testTypeDirectoryListBif() {
		variables.put( Key.of( "testDirectory" ), Path.of( testDirectory ).toAbsolutePath().toString() );
		assertTrue( FileSystemUtil.exists( testDirectory ) );
		instance.executeSource(
		    """
		    result = directoryList( variables.testDirectory, true, "path", "*", "name", "directory" );
		    """,
		    context );
		var result = variables.get( Key.of( "result" ) );
		assertTrue( result instanceof Array );
		Array listing = ( Array ) result;
		assertTrue( listing.size() == 2 );
	}

}
