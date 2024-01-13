
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
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;

import org.junit.jupiter.api.AfterAll;
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
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.util.FileSystemUtil;

public class FileGetMimeTypeTest {

	static BoxRuntime	instance;
	static IBoxContext	context;
	static IStruct		variables;
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
			FileSystemUtil.write( testTextFile, "file mimetype test!".getBytes( "UTF-8" ), true );
		}

		if ( !FileSystemUtil.exists( testBinaryFile ) ) {
			BufferedInputStream urlStream = new BufferedInputStream( new URL( "https://source.unsplash.com/random/200x200?sig=1" ).openStream() );
			FileSystemUtil.write( testBinaryFile, urlStream.readAllBytes(), true );
		}

	}

	@AfterAll
	public static void teardown() throws IOException {
		FileSystemUtil.deleteDirectory( "src/test/resources/tmp", true );
		instance.shutdown();
	}

	@BeforeEach
	public void setupEach() {
		variables.clear();
	}

	@DisplayName( "It can obtain the mimetype of a text file" )
	@Test
	public void testTextFileMimeType() {
		variables.put( Key.of( "testFile" ), testTextFile );
		instance.executeSource(
		    """
		    result = fileGetMimeType( variables.testFile );
		    """,
		    context );
		String result = variables.getAsString( Key.of( "result" ) );
		assertThat( result ).isInstanceOf( String.class );
		assertThat( result ).isEqualTo( "text/plain" );
	}

	@DisplayName( "It tests the ability to read a binary file" )
	@Test
	public void testBinaryFileMimeType() {
		variables.put( Key.of( "testFile" ), testBinaryFile );
		instance.executeSource(
		    """
		    result = fileGetMimeType( variables.testFile );
		    """,
		    context );
		Object result = variables.get( Key.of( "result" ) );
		assertThat( result ).isInstanceOf( String.class );
		assertThat( result ).isEqualTo( "image/jpeg" );

	}

	@DisplayName( "It will throw an error on a non-existent file when the strict arg is true" )
	@Test
	public void testThrowOnStrictNonExists() {
		variables.put( Key.of( "testFile" ), "src/test/resources/tmp/non-existent.blah" );

		assertThrows(
		    BoxRuntimeException.class,
		    () -> instance.executeSource(
		        """
		        result = fileGetMimeType( variables.testFile, true );
		        """,
		        context )
		);

	}

	@DisplayName( "It will throw an error on a zero-length file when the strict arg is true" )
	@Test
	public void testThrowOnStrictEmpty() throws FileNotFoundException, IOException {
		String emptyFile = "src/test/resources/tmp/empty.txt";
		variables.put( Key.of( "testFile" ), emptyFile );
		new FileOutputStream( new File( emptyFile ) ).close();
		assertThrows(
		    BoxRuntimeException.class,
		    () -> instance.executeSource(
		        """
		        result = fileGetMimeType( variables.testFile, true );
		        """,
		        context )
		);

	}

	@DisplayName( "It tests the ability to guess the mimetype of a non-existent file" )
	@Test
	public void testGuessedMimeType() {
		variables.put( Key.of( "testFile" ), "src/test/resources/tmp/test.png" );
		instance.executeSource(
		    """
		    result = fileGetMimeType( variables.testFile, false );
		    """,
		    context );
		Object result = variables.get( Key.of( "result" ) );
		assertThat( result ).isInstanceOf( String.class );
		assertThat( result ).isEqualTo( "image/png" );

	}

}
