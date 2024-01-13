
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
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.File;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.util.FileSystemUtil;

public class FileWriteLineTest {

	static BoxRuntime		instance;
	static IBoxContext		context;
	static IStruct			variables;
	static Key				result			= new Key( "result" );

	private static String	tmpDirectory	= "src/test/resources/tmp";
	private static String	emptyFile		= "src/test/resources/tmp/file-write-line-test.txt";
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
		if ( FileSystemUtil.exists( emptyFile ) ) {
			FileSystemUtil.deleteFile( emptyFile );
		}
		variables.clear();
	}

	@DisplayName( "It tests the BIF FileWriteLine on a file object" )
	@Test
	@Ignore
	public void testWriteLineFile() throws IOException {
		variables.put( Key.of( "testFile" ), Path.of( emptyFile ).toAbsolutePath().toString() );
		instance.executeSource(
		    """
		          fileObj = fileOpen( testFile, "write" );
		    FileWriteLine( fileObj, "box" );
		    FileWriteLine( fileObj, "lang" );
		    FileWriteLine( fileObj, "is" );
		    FileWriteLine( fileObj, "awesome" );
		      fileObj.close();
		            """,
		    context );
		String		fileContents	= ( String ) FileSystemUtil.read( emptyFile );
		Object[]	lines			= fileContents.split( System.getProperty( "line.separator" ) );
		assertThat( lines.length ).isEqualTo( 4 );
	}

	@DisplayName( "It tests the BIF FileWriteLine on a file object with an offset" )
	@Test
	@Ignore
	public void testWriteLineOffset() throws IOException {
		variables.put( Key.of( "testFile" ), Path.of( emptyFile ).toAbsolutePath().toString() );
		FileSystemUtil.write( emptyFile, "this to the max!" );
		instance.executeSource(
		    """
		             fileObj = fileOpen( testFile, "write", "utf-8", true );
		       // FileSeek( fileObj, 2 );
		    fileObj.seek( 2 );
		       FileWriteLine( fileObj, "box" );
		       FileWriteLine( fileObj, "lang" );
		       FileWriteLine( fileObj, "is" );
		       FileWriteLine( fileObj, "awesome" );
		         fileObj.close();
		               """,
		    context );
		String		fileContents	= ( String ) FileSystemUtil.read( emptyFile );
		Object[]	lines			= fileContents.split( System.getProperty( "line.separator" ) );
		assertThat( lines.length ).isEqualTo( 5 );
	}

	@DisplayName( "It tests the BIF FileWriteLine on an existing file object opened in append mode" )
	@Test
	@Ignore
	public void testWriteLineAppendMode() throws IOException {
		variables.put( Key.of( "testFile" ), Path.of( emptyFile ).toAbsolutePath().toString() );
		instance.executeSource(
		    """
		          fileObj = fileOpen( testFile, "append" );
		    FileWriteLine( fileObj, "box" );
		    FileWriteLine( fileObj, "lang" );
		    FileWriteLine( fileObj, "is" );
		    FileWriteLine( fileObj, "awesome" );
		      fileObj.close();
		            """,
		    context );
		String		fileContents	= ( String ) FileSystemUtil.read( emptyFile );
		Object[]	lines			= fileContents.split( System.getProperty( "line.separator" ) );
		assertThat( lines.length ).isEqualTo( 4 );
	}

	@DisplayName( "It tests the BIF FileWriteLine with a string path" )
	@Test
	@Ignore
	public void testWriteLineString() throws IOException {
		variables.put( Key.of( "testFile" ), emptyFile );
		instance.executeSource(
		    """
		    FileWriteLine( testFile, "box" );
		    	    FileWriteLine( testFile, "lang" );
		    	    FileWriteLine( testFile, "is" );
		    	    FileWriteLine( testFile, "awesome" );
		    """,
		    context );
		String		fileContents	= ( String ) FileSystemUtil.read( emptyFile );
		Object[]	lines			= fileContents.split( System.getProperty( "line.separator" ) );
		assertThat( lines.length ).isEqualTo( 4 );
	}

}
