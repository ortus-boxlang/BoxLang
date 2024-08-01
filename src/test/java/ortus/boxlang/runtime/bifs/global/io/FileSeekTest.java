
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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;

import org.junit.Ignore;
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
import ortus.boxlang.runtime.types.File;
import ortus.boxlang.runtime.util.FileSystemUtil;

public class FileSeekTest {

	static BoxRuntime		instance;
	IBoxContext				context;
	IScope					variables;
	static Key				result			= new Key( "result" );

	private static String	tmpDirectory	= "src/test/resources/tmp/fileSeekTest";
	private static String	testFile		= "src/test/resources/tmp/fileSeekTest/file-seek-test.txt";
	static String			testBinaryFile	= "src/test/resources/tmp/fileSeekTest/test.jpg";
	private static File		writeFile		= null;

	@BeforeAll
	public static void setUp() throws IOException {
		instance = BoxRuntime.getInstance( true );
		if ( !FileSystemUtil.exists( tmpDirectory ) ) {
			FileSystemUtil.createDirectory( tmpDirectory );
		}
	}

	@AfterAll
	public static void teardown() throws IOException {
		if ( writeFile != null ) {
			writeFile.close();
		}
		if ( FileSystemUtil.exists( tmpDirectory ) ) {
			FileSystemUtil.deleteDirectory( tmpDirectory, true );
		}

	}

	@BeforeEach
	public void setupEach() throws IOException {
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
		if ( FileSystemUtil.exists( testFile ) ) {
			FileSystemUtil.deleteFile( testFile );
		}

		if ( !FileSystemUtil.exists( testBinaryFile ) ) {
			BufferedInputStream urlStream = new BufferedInputStream(
			    URI.create( "https://ortus-public.s3.amazonaws.com/logos/ortus-medium.jpg" ).toURL().openStream() );
			FileSystemUtil.write( testBinaryFile, urlStream.readAllBytes(), true );
		}

	}

	@DisplayName( "It tests the BIF FileSeek" )
	@Test
	@Ignore
	public void testSeekFile() throws IOException {
		FileSystemUtil.write( testFile, "abcdefg".getBytes( "UTF-8" ), true );
		variables.put( Key.of( "testFile" ), Path.of( testFile ).toAbsolutePath().toString() );
		instance.executeSource(
		    """
		          fileObj = fileOpen( testFile, "read" );
		    fileSeek( fileObj, 2 );
		    result = fileObj.readLine();
		    fileObj.close();
		            """,
		    context );
		String result = variables.getAsString( Key.of( "result" ) );
		assertThat( result ).isEqualTo( "cdefg" );
	}

	@DisplayName( "It tests the BIF FileSeek after two seeks" )
	@Test
	@Ignore
	public void testSeekFileTwice() throws IOException {
		FileSystemUtil.write( testFile, "abcdefg".getBytes( "UTF-8" ), true );
		variables.put( Key.of( "testFile" ), Path.of( testFile ).toAbsolutePath().toString() );
		instance.executeSource(
		    """
		             fileObj = fileOpen( testFile, "read" );
		       fileSeek( fileObj, 2 );
		    fileSeek( fileObj, 2 );
		       result = fileObj.readLine();
		       fileObj.close();
		               """,
		    context );
		String result = variables.getAsString( Key.of( "result" ) );
		assertThat( result ).isEqualTo( "efg" );
	}

	@DisplayName( "It tests the File.seek member function" )
	@Test
	@Ignore
	public void testSeekMember() throws IOException {
		FileSystemUtil.write( testFile, "abcdefg".getBytes( "UTF-8" ), true );
		variables.put( Key.of( "testFile" ), Path.of( testFile ).toAbsolutePath().toString() );
		instance.executeSource(
		    """
		          fileObj = fileOpen( testFile, "read" );
		    fileObj.seek( 2 );
		    result = fileObj.readLine();
		    fileObj.close();
		            """,
		    context );
		String result = variables.getAsString( Key.of( "result" ) );
		assertThat( result ).isEqualTo( "cdefg" );
	}

	@DisplayName( "It tests the BIF FileSkipBytes" )
	@Test
	@Ignore
	public void testSkipBytesFile() throws IOException {
		variables.put( Key.of( "testFile" ), Path.of( testBinaryFile ).toAbsolutePath().toString() );
		instance.executeSource(
		    """
		          fileObj = fileOpen( testFile, "readbinary" );
		    fileSkipBytes( fileObj, 2 );
		    result = fileObj.read( 10 );
		    fileObj.close();
		            """,
		    context );
		assertTrue( variables.get( Key.of( "result" ) ) instanceof byte[] );
		byte[] result = ( byte[] ) variables.get( Key.of( "result" ) );
		assertThat( result.length ).isEqualTo( 10 );
	}

	@DisplayName( "It tests the BIF File.skipBytes member function" )
	@Test
	@Ignore
	public void testSkipBytesMember() throws IOException {
		variables.put( Key.of( "testFile" ), Path.of( testBinaryFile ).toAbsolutePath().toString() );
		instance.executeSource(
		    """
		          fileObj = fileOpen( testFile, "readbinary" );
		    fileObj.skipBytes( 2 );
		    result = fileObj.read( 10 );
		    fileObj.close();
		            """,
		    context );
		assertTrue( variables.get( Key.of( "result" ) ) instanceof byte[] );
		byte[] result = ( byte[] ) variables.get( Key.of( "result" ) );
		assertThat( result.length ).isEqualTo( 10 );
	}

}
