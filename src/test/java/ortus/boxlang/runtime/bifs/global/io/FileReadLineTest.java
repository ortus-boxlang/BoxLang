
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

public class FileReadLineTest {

	static BoxRuntime		instance;
	static IBoxContext		context;
	static IScope			variables;
	static Key				result			= new Key( "result" );

	private static String	tmpDirectory	= "src/test/resources/tmp";
	private static String	testFile		= "src/test/resources/tmp/file-read-line-test.txt";
	private static String	emptyFile		= "src/test/resources/tmp/empty.txt";
	private static File		writeFile		= null;
	private static File		readFile		= null;

	@BeforeAll
	public static void setUp() throws IOException {
		instance	= BoxRuntime.getInstance( true );
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
		if ( !FileSystemUtil.exists( tmpDirectory ) ) {
			FileSystemUtil.createDirectory( tmpDirectory );
		}
	}

	@AfterAll
	public static void teardown() throws IOException {
		if ( writeFile != null ) {
			writeFile.close();
		}
		if ( readFile != null ) {
			readFile.close();
		}
		if ( FileSystemUtil.exists( tmpDirectory ) ) {
			FileSystemUtil.deleteDirectory( tmpDirectory, true );
		}
		instance.shutdown();
	}

	@BeforeEach
	public void setupEach() throws IOException {
		if ( readFile != null ) {
			readFile.close();
			readFile = null;
		}
		if ( !FileSystemUtil.exists( testFile ) ) {
			writeFile = new File( testFile, "write" );
			writeFile.writeLine( "box" ).writeLine( "lang" ).writeLine( "rocks!" );
			writeFile.close();
			writeFile = null;
		}
		variables.clear();
	}

	@DisplayName( "It tests the BIF FileReadLine" )
	@Test
	@Ignore
	public void testFileReadLine() {
		readFile = new File( testFile, "read" );
		variables.put( Key.of( "testFileObj" ), readFile );
		instance.executeSource(
		    """
		    line1 = fileReadLine( testFileObj );
		    line2 = fileReadLine( testFileObj );
		    line3 = fileReadLine( testFileObj );
		    testFileObj.close();
		               """,
		    context );
		String	line1	= variables.getAsString( Key.of( "line1" ) );
		String	line2	= variables.getAsString( Key.of( "line2" ) );
		String	line3	= variables.getAsString( Key.of( "line3" ) );
		assertThat( line1 ).isEqualTo( "box" );
		assertThat( line2 ).isEqualTo( "lang" );
		assertThat( line3 ).isEqualTo( "rocks!" );
	}

	@DisplayName( "It tests the Member function File.readLine" )
	@Test
	@Ignore
	public void testReadLineMember() {
		readFile = new File( testFile, "read" );
		variables.put( Key.of( "testFileObj" ), readFile );
		instance.executeSource(
		    """
		    line1 = testFileObj.readLine();
		    line2 = testFileObj.readLine();
		    line3 = testFileObj.readLine();
		    testFileObj.close();
		               """,
		    context );
		String	line1	= variables.getAsString( Key.of( "line1" ) );
		String	line2	= variables.getAsString( Key.of( "line2" ) );
		String	line3	= variables.getAsString( Key.of( "line3" ) );
		assertThat( line1 ).isEqualTo( "box" );
		assertThat( line2 ).isEqualTo( "lang" );
		assertThat( line3 ).isEqualTo( "rocks!" );
	}

	@DisplayName( "It tests FileReadLine will throw an error on a write file object" )
	@Test
	@Ignore
	public void testWriteFileError() {
		readFile = new File( emptyFile, "write" );
		variables.put( Key.of( "testFileObj" ), readFile );
		assertThrows( BoxRuntimeException.class, () -> instance.executeSource(
		    """
		    fileReadLine( testFileObj );
		               """,
		    context )
		);
	}

}
