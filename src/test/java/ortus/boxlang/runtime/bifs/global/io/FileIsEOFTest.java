
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

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import ortus.boxlang.runtime.context.ScriptingBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.File;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.util.FileSystemUtil;

public class FileIsEOFTest {

	static BoxRuntime		instance;
	static IBoxContext		context;
	static IScope			variables;
	static Key				result			= new Key( "result" );

	private static String	tmpDirectory	= "src/test/resources/tmp";
	private static String	testFile		= "src/test/resources/tmp/file-test.txt";
	private static String	emptyFile		= "src/test/resources/tmp/file-write-test.txt";
	private static File		readFile		= null;
	private static File		writeFile		= null;

	@BeforeAll
	public static void setUp() throws IOException {
		instance	= BoxRuntime.getInstance( true );
		context		= new ScriptingBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
		if ( !FileSystemUtil.exists( tmpDirectory ) ) {
			FileSystemUtil.createDirectory( tmpDirectory );
		}
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
			File testFileObj = new File( testFile, "write", "utf-8", false );
			for ( var i = 1; i <= 100; i++ ) {
				testFileObj.writeLine( "Line number " + i + "!" );
			}
			testFileObj.close();
		}
		if ( FileSystemUtil.exists( emptyFile ) ) {
			FileSystemUtil.deleteFile( emptyFile );
		}
		variables.clear();
	}

	@DisplayName( "It tests the BIF FileISEOF with a read stream" )
	@Test
	@Ignore
	public void testReadFileEOF() {
		variables.put( Key.of( "testFile" ), Path.of( testFile ).toAbsolutePath().toString() );
		instance.executeSource(
		    """
		        fileObj = fileOpen( testFile, "read", "utf-8", true );
		       firstTest = fileIsEOF( fileObj );
		       for( i = 1; i <= 100; i++ ){
		     fileObj.readLine();
		       }
		       lastTest = fileIsEOF( fileObj );
		    fileObj.close();
		          """,
		    context );
		Boolean	firstTest	= ( Boolean ) variables.get( Key.of( "firstTest" ) );
		Boolean	lastTest	= ( Boolean ) variables.get( Key.of( "lastTest" ) );
		assertFalse( firstTest );
		assertTrue( lastTest );
	}

	@DisplayName( "It tests the BIF FileISEOF with a write stream" )
	@Test
	@Ignore
	public void testWriteFileEOF() {
		writeFile = new File( emptyFile, "write" );
		variables.put( Key.of( "testFile" ), writeFile );
		assertThrows(
		    BoxRuntimeException.class,
		    () -> instance.executeSource(
		        """
		        firstTest = fileIsEOF( testFile );
		           """,
		        context )
		);
	}

}
