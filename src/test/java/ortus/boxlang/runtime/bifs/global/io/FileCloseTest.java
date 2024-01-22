
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
import static org.junit.Assert.assertThrows;

import java.io.IOException;
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
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.util.FileSystemUtil;

public class FileCloseTest {

	static BoxRuntime		instance;
	static IBoxContext		context;
	static IScope			variables;
	static Key				result			= new Key( "result" );

	private static String	tmpDirectory	= "src/test/resources/tmp";
	private static String	testFile		= "src/test/resources/tmp/file-test.txt";
	private static String	emptyFile		= "src/test/resources/tmp/file-write-test.txt";
	static String			testBinaryFile	= "src/test/resources/tmp/test.jpg";
	private static File		readFile		= null;
	private static File		writeFile		= null;

	@BeforeAll
	public static void setUp() {
		instance	= BoxRuntime.getInstance( true );
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
	}

	@AfterAll
	public static void teardown() throws IOException {
		if ( readFile != null ) {
			readFile.close();
		}
		if ( writeFile != null ) {
			writeFile.close();
		}
		if ( FileSystemUtil.exists( tmpDirectory ) ) {
			FileSystemUtil.deleteDirectory( tmpDirectory, true );
		}
		instance.shutdown();
	}

	@BeforeEach
	public void setupEach() throws IOException {
		if ( !FileSystemUtil.exists( testFile ) ) {
			FileSystemUtil.write( testFile, "close file test!".getBytes( "UTF-8" ), true );
		}
		if ( FileSystemUtil.exists( emptyFile ) ) {
			FileSystemUtil.deleteFile( emptyFile );
		}
		variables.clear();
	}

	@DisplayName( "It tests the BIF FileOpen with a read stream" )
	@Test
	@Ignore
	public void testReadFileClose() {
		variables.put( Key.of( "testFile" ), Path.of( testFile ).toAbsolutePath().toString() );
		instance.executeSource(
		    """
		       result = fileOpen( testFile );
		    fileClose( result );
		       """,
		    context );
		readFile = ( File ) variables.get( Key.of( "result" ) );
		assertThat( readFile ).isInstanceOf( File.class );
		assertThrows(
		    BoxRuntimeException.class,
		    () -> readFile.readLine()
		);
	}

	@DisplayName( "It tests the BIF FileOpen with a write stream" )
	@Test
	@Ignore
	public void testWriteFileClose() {
		variables.put( Key.of( "testFile" ), Path.of( emptyFile ).toAbsolutePath().toString() );
		instance.executeSource(
		    """
		       result = fileOpen( testFile, "write" );
		    fileClose( result );
		       """,
		    context );
		readFile = ( File ) variables.get( Key.of( "result" ) );
		assertThat( readFile ).isInstanceOf( File.class );
		assertThrows(
		    BoxRuntimeException.class,
		    () -> readFile.writeLine( "blah!" )
		);
	}

	@DisplayName( "It the bif FileClose with an invalid file" )
	@Test
	@Ignore
	public void testBifWithInvalidFile() {
		variables.put( Key.of( "testFile" ), Path.of( testFile ).toAbsolutePath().toString() );
		assertThrows(
		    BoxRuntimeException.class,
		    () -> instance.executeSource(
		        """
		        result = fileClose( testFile );
		        """,
		        context )
		);

	}

}
