
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

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.util.FileSystemUtil;

public class FileDeleteTest {

	static BoxRuntime	instance;
	IBoxContext			context;
	IScope				variables;
	static Key			result	= new Key( "result" );

	@BeforeAll
	public static void setUp() {
		instance = BoxRuntime.getInstance( true );
	}

	@AfterAll
	public static void teardown() {

	}

	@BeforeEach
	public void setupEach() {
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
	}

	@DisplayName( "It tests the BIF FileDelete" )
	@Test
	public void testFileDelete() throws IOException {
		String testFile = "src/test/resources/tmp/deletable.txt";
		variables.put( Key.of( "testFile" ), Path.of( testFile ).toAbsolutePath().toString() );
		FileSystemUtil.write( testFile, "file delete test!".getBytes( "UTF-8" ), true );
		assertTrue( FileSystemUtil.exists( testFile ) );
		instance.executeSource(
		    """
		    fileDelete( variables.testFile );
		    """,
		    context );
		assertFalse( FileSystemUtil.exists( testFile ) );
	}

	@DisplayName( "It can delete a file using a Java Path object" )
	@Test
	public void testFileDeleteWithJavaPath() throws IOException {
		String testFile = "src/test/resources/tmp/deletable-path.txt";
		FileSystemUtil.write( testFile, "file delete path test!".getBytes( "UTF-8" ), true );
		assertTrue( FileSystemUtil.exists( testFile ) );
		variables.put( Key.of( "testPath" ), Path.of( testFile ).toAbsolutePath() );
		instance.executeSource(
		    """
		    fileDelete( variables.testPath );
		    """,
		    context );
		assertFalse( FileSystemUtil.exists( testFile ) );
	}

	@DisplayName( "It can delete a file using a Java File object" )
	@Test
	public void testFileDeleteWithJavaFile() throws IOException {
		String testFile = "src/test/resources/tmp/deletable-file.txt";
		FileSystemUtil.write( testFile, "file delete file test!".getBytes( "UTF-8" ), true );
		assertTrue( FileSystemUtil.exists( testFile ) );
		variables.put( Key.of( "testFile" ), Path.of( testFile ).toAbsolutePath().toFile() );
		instance.executeSource(
		    """
		    fileDelete( variables.testFile );
		    """,
		    context );
		assertFalse( FileSystemUtil.exists( testFile ) );
	}

	@DisplayName( "It rejects an explicit BoxFile object" )
	@Test
	public void testFileDeleteRejectsBoxFile() throws IOException {
		String testFile = "src/test/resources/tmp/deletable-reject.txt";
		FileSystemUtil.write( testFile, "reject test!".getBytes( "UTF-8" ), true );
		variables.put( Key.of( "testFile" ), Path.of( testFile ).toAbsolutePath().toString() );
		assertThrows(
		    BoxRuntimeException.class,
		    () -> instance.executeSource(
		        """
		        fileObj = fileOpen( testFile, "read" );
		        try {
		            fileDelete( fileObj );
		        } finally {
		            fileClose( fileObj );
		        }
		        """,
		        context )
		);
	}

}
