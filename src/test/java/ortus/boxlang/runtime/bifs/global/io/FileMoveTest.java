
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;

import org.junit.Ignore;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.context.RequestBoxContext;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.util.FileSystemUtil;

public class FileMoveTest {

	static BoxRuntime	instance;
	IBoxContext			context;
	IScope				variables;
	static Key			result				= new Key( "result" );
	static String		source				= "src/test/resources/tmp/fileMoveTest/start.txt";
	static String		destination			= "src/test/resources/tmp/fileMoveTest/end.txt";
	static String		tmpDirectory		= "src/test/resources/tmp/fileMoveTest";
	static String		javaTempDirectory	= System.getProperty( "java.io.tmpdir" );

	@BeforeAll
	public static void setUp() {
		instance = BoxRuntime.getInstance( true );

		// We are testing this here to ensure the default configuration is set up correctly
		// assertFalse( instance.getConfiguration().security.isExtensionAllowed( "exe" ) );

	}

	@AfterAll
	public static void teardown() {

	}

	@BeforeEach
	public void setupEach() throws IOException {
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
		if ( !FileSystemUtil.exists( source ) ) {
			FileSystemUtil.write( source, "moving!".getBytes( "UTF-8" ), true );
		}
	}

	@AfterEach
	public void teardownEach() throws IOException {
		if ( FileSystemUtil.exists( tmpDirectory ) ) {
			FileSystemUtil.deleteDirectory( tmpDirectory, true );
		}
		if ( FileSystemUtil.exists( Path.of( javaTempDirectory, "start.txt" ).toAbsolutePath().toString() ) ) {
			FileSystemUtil.deleteFile( Path.of( javaTempDirectory, "start.txt" ).toAbsolutePath().toString() );
		}
	}

	@DisplayName( "It tests the BIF FileMove" )
	@Test
	@Ignore
	public void testBif() {
		variables.put( Key.of( "targetFile" ), Path.of( source ).toAbsolutePath().toString() );
		variables.put( Key.of( "destinationFile" ), Path.of( destination ).toAbsolutePath().toString() );
		assertTrue( FileSystemUtil.exists( source ) );
		assertFalse( FileSystemUtil.exists( destination ) );
		instance.executeSource(
		    """
		    fileMove( targetFile, destinationFile );
		    """,
		    context );
		assertTrue( FileSystemUtil.exists( destination ) );
	}

	@DisplayName( "It tests the BIF FileMove security" )
	@Test
	public void testBifSecurity() {
		variables.put( Key.of( "targetFile" ), Path.of( source ).toAbsolutePath().toString() );
		context.getParentOfType( RequestBoxContext.class ).getApplicationListener().updateSettings( Struct.of( "disallowedFileOperationExtensions", "exe" ) );

		try {
			assertThrows(
			    BoxRuntimeException.class,
			    () -> instance.executeSource(
			        """
			        fileMove( targetFile, "blah.exe" );
			             """,
			        context )
			);
		} finally {
			context.getParentOfType( RequestBoxContext.class ).getApplicationListener()
			    .updateSettings( Struct.of( "disallowedFileOperationExtensions", new Array() ) );
		}
	}

	@DisplayName( "It tests the BIF FileMove default overwrite argument will throw an error if the file exists" )
	@Test
	public void testBifDefaultOverwrite() {
		variables.put( Key.of( "targetFile" ), Path.of( source ).toAbsolutePath().toString() );
		variables.put( Key.of( "destinationFile" ), Path.of( destination ).toAbsolutePath().toString() );
		FileSystemUtil.write( variables.getAsString( Key.of( "destinationFile" ) ), "This file already exists" );
		assertTrue( FileSystemUtil.exists( source ) );
		assertTrue( FileSystemUtil.exists( destination ) );

		assertThrows(
		    BoxRuntimeException.class,
		    () -> instance.executeSource(
		        """
		        fileMove( targetFile, destinationFile );
		             """,
		        context )
		);
	}

	@DisplayName( "It can move a file to a destionation if it's a directory" )
	@Test
	public void testMoveToDirectory() throws IOException {
		variables.put( Key.of( "source" ), Path.of( source ).toAbsolutePath().toString() );
		variables.put( Key.of( "destination" ), Path.of( javaTempDirectory ).toAbsolutePath().toString() );
		assertThat( FileSystemUtil.exists( source ) ).isTrue();

		instance.executeSource(
		    """
		    fileMove( source, destination, true );
		      """,
		    context );

		assertThat( FileSystemUtil.exists( javaTempDirectory ) ).isTrue();
		assertThat( FileSystemUtil.exists( javaTempDirectory + "/start.txt" ) ).isTrue();
	}

}
