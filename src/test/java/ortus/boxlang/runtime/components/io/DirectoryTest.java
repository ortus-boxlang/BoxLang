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

package ortus.boxlang.runtime.components.io;

import static com.google.common.truth.Truth.assertThat;
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

import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.DateTime;
import ortus.boxlang.runtime.types.Query;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.util.FileSystemUtil;

public class DirectoryTest {

	static BoxRuntime	instance;
	IBoxContext			context;
	IScope				variables;
	static Key			result			= new Key( "result" );
	static String		tmpDirectory	= "src/test/resources/tmp/directoryComponentCreateTest";
	static String		targetDirectory	= "src/test/resources/tmp/directoryComponentCreateTest/nested/path";
	static String		deleteDirectory	= tmpDirectory + "/foo";
	static String		listDirectory	= "src/test/resources/tmp/directoryComponentListTest";
	static String		testDirectory	= listDirectory + "/foo";
	static String		testFile1		= testDirectory + "/test.txt";
	static String		testDirectory2	= testDirectory + "/bar";
	static String		testFile2		= testDirectory2 + "/atest.txt";

	@BeforeAll
	public static void setUp() {
		instance = BoxRuntime.getInstance( true );
	}

	@AfterAll
	public static void teardown() {
		if ( FileSystemUtil.exists( tmpDirectory ) ) {
			FileSystemUtil.deleteDirectory( tmpDirectory, true );
		}
		if ( FileSystemUtil.exists( listDirectory ) ) {
			FileSystemUtil.deleteDirectory( listDirectory, true );
		}
	}

	@BeforeEach
	public void setupEach() throws IOException {
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
		if ( FileSystemUtil.exists( tmpDirectory ) ) {
			FileSystemUtil.deleteDirectory( tmpDirectory, true );
		}
		if ( FileSystemUtil.exists( listDirectory ) ) {
			FileSystemUtil.deleteDirectory( listDirectory, true );
		}

	}

	@DisplayName( "It tests the Directory action Create with the defaults" )
	@Test
	public void testDirectoryCreateDefault() {
		variables.put( Key.of( "destination" ), Path.of( targetDirectory ).toAbsolutePath().toString() );
		assertFalse( FileSystemUtil.exists( targetDirectory ) );
		instance.executeSource(
		    """
		    bx:directory action="Create" directory="#destination#";
		    """,
		    context );
		assertTrue( FileSystemUtil.exists( targetDirectory ) );
	}

	@DisplayName( "It tests the Directory action Create with the defaults Tag" )
	@Test
	public void testDirectoryCreateDefaultTag() {
		variables.put( Key.of( "destination" ), Path.of( targetDirectory ).toAbsolutePath().toString() );
		assertFalse( FileSystemUtil.exists( targetDirectory ) );
		instance.executeSource(
		    """
		    <cfdirectory action="Create" directory="#destination#">
		    """,
		    context, BoxSourceType.CFTEMPLATE );
		assertTrue( FileSystemUtil.exists( targetDirectory ) );
	}

	@DisplayName( "It tests the Directory action Copy with the defaults" )
	@Test
	public void testDirectoryCopyDefault() {
		assertFalse( FileSystemUtil.exists( targetDirectory ) );
		assertFalse( FileSystemUtil.exists( testDirectory2 ) );
		String testFile = targetDirectory + "/test.txt";
		variables.put( Key.of( "source" ), Path.of( targetDirectory ).toAbsolutePath().toString() );
		variables.put( Key.of( "destination" ), Path.of( testDirectory2 ).toAbsolutePath().toString() );
		FileSystemUtil.createDirectory( targetDirectory );
		FileSystemUtil.write( testFile, "copy directory test!" );
		instance.executeSource(
		    """
		    bx:directory action="copy" directory="#source#" destination="#destination#";
		      """,
		    context );
		assertTrue( FileSystemUtil.exists( targetDirectory ) );
		assertTrue( FileSystemUtil.exists( testFile ) );
		assertTrue( FileSystemUtil.exists( testDirectory2 ) );
		assertTrue( FileSystemUtil.exists( testDirectory2 + "/test.txt" ) );
	}

	@DisplayName( "It tests the Directory action Create will throw an error if createPath is false" )
	@Test
	public void testNoCreatePaths() {
		variables.put( Key.of( "destination" ), Path.of( targetDirectory ).toAbsolutePath().toString() );
		assertFalse( FileSystemUtil.exists( targetDirectory ) );
		assertThrows(
		    RuntimeException.class,
		    () -> instance.executeSource(
		        """
		        bx:directory action="Create" directory="#destination#" createPath=false;
		                  """,
		        context )
		);
	}

	@DisplayName( "It tests the Directory action Create defaults will throw an error if the directory exists" )
	@Test
	public void testIgnoreExistsFalse() {
		variables.put( Key.of( "destination" ), Path.of( targetDirectory ).toAbsolutePath().toString() );
		assertFalse( FileSystemUtil.exists( targetDirectory ) );
		instance.executeSource(
		    """
		    bx:directory action="Create" directory="#destination#";
		        """,
		    context );
		assertTrue( FileSystemUtil.exists( targetDirectory ) );
		assertThrows(
		    BoxRuntimeException.class,
		    () -> instance.executeSource(
		        """
		        directory action="Create directory="#destination#";
		             """,
		        context )
		);
	}

	@DisplayName( "It tests the Directory action Delete" )
	@Test
	public void testDirectoryDelete() throws IOException {
		String testFile = deleteDirectory + "/test.txt";
		variables.put( Key.of( "deleteDirectory" ), Path.of( deleteDirectory ).toAbsolutePath().toString() );
		FileSystemUtil.createDirectory( deleteDirectory );
		FileSystemUtil.write( testFile, "directory delete test!" );
		assertTrue( FileSystemUtil.exists( deleteDirectory ) );
		instance.executeSource(
		    """
		    directoryDelete( variables.deleteDirectory, true );
		    """,
		    context );
		assertFalse( FileSystemUtil.exists( deleteDirectory ) );
	}

	@DisplayName( "It tests the Directory action Delete without a recursive arg" )
	@Test
	public void testDirectoryDeleteError() throws IOException {
		String	deleteDirectory	= "src/test/resources/tmp/foo";
		String	testFile		= deleteDirectory + "/test.txt";
		variables.put( Key.of( "deleteDirectory" ), Path.of( deleteDirectory ).toAbsolutePath().toString() );
		FileSystemUtil.createDirectory( deleteDirectory );
		FileSystemUtil.write( testFile, "directory delete test!" );
		assertTrue( FileSystemUtil.exists( deleteDirectory ) );
		assertThrows(
		    BoxRuntimeException.class,
		    () -> instance.executeSource(
		        """
		        directoryDelete( variables.deleteDirectory );
		        """,
		        context )
		);
	}

	@DisplayName( "It tests the Directory action List with a listInfo of all" )
	@Test
	public void testAllDirectoryListBif() {
		FileSystemUtil.createDirectory( testDirectory );
		FileSystemUtil.write( testFile1, "directory list test!" );
		FileSystemUtil.createDirectory( testDirectory2 );
		FileSystemUtil.write( testFile2, "test nested directory file" );
		variables.put( Key.of( "testDirectory" ), Path.of( testDirectory ).toAbsolutePath().toString() );
		assertTrue( FileSystemUtil.exists( testDirectory ) );
		instance.executeSource(
		    """
		       println(variables.testDirectory)
		    bx:directory action="List" directory="#variables.testDirectory#" name="result" recurse=false listInfo="all";
		          """,
		    context );
		var result = variables.get( Key.of( "result" ) );
		assertTrue( result instanceof Query );
		Query listing = ( Query ) result;
		assertTrue( listing.size() == 2 );
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

		// Test for BL-202
		instance.executeSource(
		    """
		       println(variables.testDirectory)
		    bx:directory directory="#variables.testDirectory#" name="result" recurse=false;
		          """,
		    context );
		result = variables.get( Key.of( "result" ) );
		assertTrue( result instanceof Query );
		listing = ( Query ) result;
		assertTrue( listing.size() == 2 );
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

	@DisplayName( "It tests the Directory action List with a listInfo of name" )
	@Test
	public void testNameDirectoryListBif() {
		FileSystemUtil.createDirectory( testDirectory );
		FileSystemUtil.write( testFile1, "directory list test!" );
		FileSystemUtil.createDirectory( testDirectory2 );
		FileSystemUtil.write( testFile2, "test nested directory file" );
		variables.put( Key.of( "testDirectory" ), Path.of( testDirectory ).toAbsolutePath().toString() );
		assertTrue( FileSystemUtil.exists( testDirectory ) );
		instance.executeSource(
		    """
		       println(variables.testDirectory)
		    bx:directory action="List" directory="#variables.testDirectory#" name="result" recurse=true listInfo="name";
		          """,
		    context );
		var result = variables.get( Key.of( "result" ) );
		assertTrue( result instanceof Query );
		Query listing = ( Query ) result;
		assertThat( listing.size() ).isEqualTo( 3 );
		listing.sort( ( rowa, rowb ) -> rowa.getAsString( Key._NAME ).compareTo( rowb.getAsString( Key._NAME ) ) );
		for ( var i = 0; i < listing.size(); i++ ) {
			Object[] currentRow = listing.getRow( i );
			assertTrue( currentRow.length == 1 );
			assertThat( listing.getCell( Key.of( "name" ), i ) ).isInstanceOf( String.class );
		}
		assertThat( listing.getCell( Key.of( "name" ), 0 ) ).isEqualTo( "bar" );
		assertThat( listing.getCell( Key.of( "name" ), 1 ).toString().replace( '\\', '/' ) ).isEqualTo( "bar/atest.txt" );
		assertThat( listing.getCell( Key.of( "name" ), 2 ) ).isEqualTo( "test.txt" );

	}

}
