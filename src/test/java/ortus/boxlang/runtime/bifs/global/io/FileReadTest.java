
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
import java.net.URL;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterAll;
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

public class FileReadTest {

	static BoxRuntime	instance;
	static IBoxContext	context;
	static IScope		variables;
	static Key			result			= new Key( "result" );
	static String		testTextFile	= "src/test/resources/tmp/text.txt";
	static String		testBinaryFile	= "src/test/resources/tmp/test.jpg";
	static String		tmpDirectory	= "src/test/resources/tmp";

	@BeforeAll
	public static void setUp() throws IOException {
		instance	= BoxRuntime.getInstance( true );
		context		= new ScriptingBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );

		if ( !FileSystemUtil.exists( testTextFile ) ) {
			FileSystemUtil.write( testTextFile, "file read test!".getBytes( "UTF-8" ), true );
		}

		if ( !FileSystemUtil.exists( testBinaryFile ) ) {
			BufferedInputStream urlStream = new BufferedInputStream( new URL( "https://source.unsplash.com/random/200x200?sig=1" ).openStream() );
			FileSystemUtil.write( testBinaryFile, urlStream.readAllBytes(), true );
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
	public void setupEach() {
		variables.clear();
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
		String result = ( String ) variables.dereference( Key.of( "result" ), false );
		assertThat( result ).isInstanceOf( String.class );
		assertThat( result ).isEqualTo( "file read test!" );
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
		String result = ( String ) variables.dereference( Key.of( "result" ), false );
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
		String result = ( String ) variables.dereference( Key.of( "result" ), false );
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
		String result = ( String ) variables.dereference( Key.of( "result" ), false );
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
		Object result = variables.dereference( Key.of( "result" ), false );
		assertTrue( result instanceof byte[] );
	}

}
