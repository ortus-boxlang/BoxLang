
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
import static org.junit.Assert.assertTrue;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URI;
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
import ortus.boxlang.runtime.util.FileSystemUtil;

public class FileReadTest {

	static BoxRuntime	instance;
	IBoxContext			context;
	IScope				variables;
	static Key			result			= new Key( "result" );
	static String		testTextFile	= "src/test/resources/tmp/fileReadTest/text.txt";
	static String		testURLFile		= "https://raw.githubusercontent.com/ColdBox/coldbox-platform/development/license.txt";
	static String		testURLImage	= "https://ortus-public.s3.amazonaws.com/logos/ortus-medium.jpg";
	static String		testBinaryFile	= "src/test/resources/tmp/fileReadTest/test.jpg";
	static String		testKeyFile		= "src/test/resources/tmp/fileReadTest/test.key";
	static String		testCertFile	= "src/test/resources/tmp/fileReadTest/test.pem";
	static String		tmpDirectory	= "src/test/resources/tmp/fileReadTest";

	@BeforeAll
	public static void setUp() throws IOException {
		instance = BoxRuntime.getInstance( true );

		if ( !FileSystemUtil.exists( testTextFile ) ) {
			FileSystemUtil.write( testTextFile, "file read test!".getBytes( "UTF-8" ), true );
		}

		if ( !FileSystemUtil.exists( testKeyFile ) ) {
			FileSystemUtil.write( testKeyFile, "-----BEGIN PRIVATE KEY-----".getBytes( "UTF-8" ), true );
		}

		if ( !FileSystemUtil.exists( testCertFile ) ) {
			FileSystemUtil.write( testCertFile, "-----BEGIN CERTIFICATE-----".getBytes( "UTF-8" ), true );
		}

		if ( !FileSystemUtil.exists( testBinaryFile ) ) {
			BufferedInputStream urlStream = new BufferedInputStream( URI.create( testURLImage ).toURL().openStream() );
			FileSystemUtil.write( testBinaryFile, urlStream.readAllBytes(), true );
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

	@DisplayName( "It tests the ability to read a text file" )
	@Test
	public void testTextFileRead() {
		variables.put( Key.of( "testFile" ), Path.of( testTextFile ).toAbsolutePath().toString() );
		instance.executeSource(
		    """
		    result = fileRead( variables.testFile );
		    """,
		    context );
		String result = variables.getAsString( Key.of( "result" ) );
		assertThat( result ).isInstanceOf( String.class );
		assertThat( result ).isEqualTo( "file read test!" );
	}

	@DisplayName( "It tests the ability to read a URL text file" )
	@Test
	public void testURLFileRead() {
		variables.put( Key.of( "testFile" ), testURLFile );
		instance.executeSource(
		    """
		    result = fileRead( variables.testFile );
		    """,
		    context );
		String result = variables.getAsString( Key.of( "result" ) );
		assertThat( result ).isInstanceOf( String.class );
		assertThat( result ).contains( "ColdBox Framework" );
		assertThat( result ).contains( System.getProperty( "line.separator" ) );
	}

	@DisplayName( "It tests the ability to read a URL binary file" )
	@Test
	public void testURLBinaryFileRead() {
		variables.put( Key.of( "testFile" ), testURLImage );
		instance.executeSource(
		    """
		    result = fileRead( variables.testFile );
		    """,
		    context );
		Object result = variables.get( Key.of( "result" ) );
		assertTrue( result instanceof byte[] );
	}

	@DisplayName( "It tests the ability to read a text file with a charset arg" )
	@Test
	public void testTextFileCharsetRead() {
		variables.put( Key.of( "testFile" ), Path.of( testTextFile ).toAbsolutePath().toString() );
		instance.executeSource(
		    """
		    result = fileRead( variables.testFile, "utf-8" );
		    """,
		    context );
		String result = variables.getAsString( Key.of( "result" ) );
		assertThat( result ).isInstanceOf( String.class );
		assertThat( result ).isEqualTo( "file read test!" );
	}

	@DisplayName( "It tests the ability to read a text file with a buffersize arg" )
	@Test
	public void testTextFileBufferRead() {
		variables.put( Key.of( "testFile" ), Path.of( testTextFile ).toAbsolutePath().toString() );
		instance.executeSource(
		    """
		    result = fileRead( variables.testFile, 10 );
		    """,
		    context );
		String result = variables.getAsString( Key.of( "result" ) );
		assertThat( result ).isInstanceOf( String.class );
		assertThat( result ).isEqualTo( "file read test!" );
	}

	@DisplayName( "It tests the ability to read a text file with a charset and buffersize arg" )
	@Test
	public void testTextFileCharsetBufferRead() {
		variables.put( Key.of( "testFile" ), Path.of( testTextFile ).toAbsolutePath().toString() );
		instance.executeSource(
		    """
		    result = fileRead( variables.testFile, "utf-8", 10 );
		    """,
		    context );
		String result = variables.getAsString( Key.of( "result" ) );
		assertThat( result ).isInstanceOf( String.class );
		assertThat( result ).isEqualTo( "file read test!" );
	}

	@DisplayName( "It tests the ability to read a binary file" )
	@Test
	public void testBinaryFileRead() {
		variables.put( Key.of( "testFile" ), Path.of( testBinaryFile ).toAbsolutePath().toString() );
		instance.executeSource(
		    """
		    result = fileRead( variables.testFile );
		    """,
		    context );
		Object result = variables.get( Key.of( "result" ) );
		assertTrue( result instanceof byte[] );
	}

	@DisplayName( "Will correctly detect common cert extensions as text" )
	@Test
	public void testCertExtensions() {
		variables.put( Key.of( "testFile" ), Path.of( testKeyFile ).toAbsolutePath().toString() );
		instance.executeSource(
		    """
		    result = fileRead( variables.testFile );
		    """,
		    context );
		Object result = variables.get( Key.of( "result" ) );
		assertTrue( result instanceof String );
		assertThat( result ).isEqualTo( "-----BEGIN PRIVATE KEY-----" );

		variables.put( Key.of( "testFile" ), Path.of( testCertFile ).toAbsolutePath().toString() );
		instance.executeSource(
		    """
		    result = fileRead( variables.testFile );
		    """,
		    context );
		result = variables.get( Key.of( "result" ) );
		assertTrue( result instanceof String );
		assertThat( result ).isEqualTo( "-----BEGIN CERTIFICATE-----" );
	}

}
