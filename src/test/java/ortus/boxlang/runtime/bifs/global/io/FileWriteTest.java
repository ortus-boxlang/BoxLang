
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
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.util.FileSystemUtil;

public class FileWriteTest {

	static BoxRuntime	instance;
	static IBoxContext	context;
	static IScope		variables;
	static Key			result			= new Key( "result" );
	static String		testTextFile	= "src/test/resources/tmp/text.txt";
	static String		testTextFile2	= "src/test/resources/tmp/nested/path/text2.txt";
	static String		testNestedFile	= "src/test/resources/tmp/nested/path/text.txt";
	static String		testBinaryFile	= "src/test/resources/tmp/test.jpg";
	static String		tmpDirectory	= "src/test/resources/tmp";

	@BeforeAll
	public static void setUp() throws IOException {
		instance	= BoxRuntime.getInstance( true );
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );

	}

	@AfterAll
	public static void teardown() throws IOException {

		if ( FileSystemUtil.exists( tmpDirectory ) ) {
			FileSystemUtil.deleteDirectory( tmpDirectory, true );
		}

		instance.shutdown();
	}

	@BeforeEach
	public void setupEach() throws IOException {
		if ( !FileSystemUtil.exists( tmpDirectory ) ) {
			FileSystemUtil.createDirectory( tmpDirectory );
		}
		variables.clear();
	}

	@DisplayName( "It tests the ability to write a text file with the default charset and ensure options" )
	@Test
	public void testTextFileWrite() throws IOException {
		variables.put( Key.of( "testFile" ), Path.of( testTextFile ).toAbsolutePath().toString() );
		instance.executeSource(
		    """
		    result = FileWrite( testFile, "I am writing!" );
		    """,
		    context );

		assertTrue( FileSystemUtil.exists( testTextFile ) );
		assertThat( FileSystemUtil.read( testTextFile, ( String ) null, ( Integer ) null ) ).isEqualTo( "I am writing!" );
	}

	@DisplayName( "It tests the ability to write a text file with the a specified charset" )
	@Test
	public void testCharsetTextFileWrite() throws IOException {
		variables.put( Key.of( "testFile" ), Path.of( testTextFile2 ).toAbsolutePath().toString() );
		instance.executeSource(
		    """
		    result = FileWrite( testFile, "I am writing 2!", "utf-16" );
		    """,
		    context );

		assertTrue( FileSystemUtil.exists( testTextFile2 ) );
		assertThat( FileSystemUtil.read( testTextFile2, "utf-16", ( Integer ) null ) ).isEqualTo( "I am writing 2!" );
	}

	@DisplayName( "It tests the ability to create the nested directories to a file" )
	@Test
	public void testNestedFileWrite() throws IOException {
		variables.put( Key.of( "testFile" ), Path.of( testNestedFile ).toAbsolutePath().toString() );
		instance.executeSource(
		    """
		    result = FileWrite( testFile, "I am nested!", "utf-8", true );
		    """,
		    context );

		assertTrue( FileSystemUtil.exists( testNestedFile ) );
		assertThat( FileSystemUtil.read( testNestedFile, ( String ) null, ( Integer ) null ) ).isEqualTo( "I am nested!" );
	}

	@DisplayName( "It tests the ability to write a binary file" )
	@Test
	public void testBinaryFileWrite() throws IOException {
		variables.put( Key.of( "testFile" ), Path.of( testBinaryFile ).toAbsolutePath().toString() );
		BufferedInputStream	urlStream		= new BufferedInputStream( new URL( "https://ortus-public.s3.amazonaws.com/logos/ortus-medium.jpg" ).openStream() );
		byte[]				binaryContent	= urlStream.readAllBytes();
		variables.put( Key.of( "binaryContent" ), binaryContent );
		instance.executeSource(
		    """
		    result = FileWrite( variables.testFile, binaryContent );
		    """,
		    context );
		assertTrue( FileSystemUtil.exists( testBinaryFile ) );
		assertThat( FileSystemUtil.read( testBinaryFile, ( String ) null, ( Integer ) null ) ).isEqualTo( binaryContent );
	}

}
