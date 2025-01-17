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

package ortus.boxlang.runtime.components.io;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.util.FileSystemUtil;

public class FileTest {

	static BoxRuntime	instance;
	IBoxContext			context;
	IScope				variables;
	static Key			result			= new Key( "result" );
	static String		tmpDirectory	= "src/test/resources/tmp/FileComponentTest";
	static String		testTextFile	= tmpDirectory + "/text.txt";
	static String		testTextFile2	= tmpDirectory + "/nested/path/text2.txt";
	static String		testNestedFile	= tmpDirectory + "/nested/path/text.txt";
	static String		testURLFile		= "https://raw.githubusercontent.com/ColdBox/coldbox-platform/development/license.txt";
	static String		testURLImage	= "https://ortus-public.s3.amazonaws.com/logos/ortus-medium.jpg";
	static String		testBinaryFile	= tmpDirectory + "/test.jpg";

	@BeforeAll
	public static void setUp() {
		instance = BoxRuntime.getInstance( true );
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
		if ( FileSystemUtil.exists( tmpDirectory ) ) {
			FileSystemUtil.deleteDirectory( tmpDirectory, true );
		}
		FileSystemUtil.createDirectory( tmpDirectory );
	}

	@Test
	public void testTextFileWriteCF() throws IOException {
		assertFalse( FileSystemUtil.exists( testTextFile ) );
		variables.put( Key.of( "testFile" ), Path.of( testTextFile ).toAbsolutePath().toString() );
		instance.executeSource(
		    """
		    <cffile action="write" file="#testFile#" output="I am writing!" >
		    """,
		    context, BoxSourceType.CFTEMPLATE );

		assertThat( FileSystemUtil.exists( testTextFile ) ).isTrue();
		assertThat( FileSystemUtil.read( testTextFile, ( String ) null, ( Integer ) null ) ).isEqualTo( "I am writing!" );
	}

	@Test
	public void testTextFileWriteBL() throws IOException {
		assertFalse( FileSystemUtil.exists( testTextFile ) );
		variables.put( Key.of( "testFile" ), Path.of( testTextFile ).toAbsolutePath().toString() );
		instance.executeSource(
		    """
		    <bx:file action="write" file="#testFile#" output="I am writing!" >
		    """,
		    context, BoxSourceType.BOXTEMPLATE );

		assertThat( FileSystemUtil.exists( testTextFile ) ).isTrue();
		assertThat( FileSystemUtil.read( testTextFile, ( String ) null, ( Integer ) null ) ).isEqualTo( "I am writing!" );
	}

	@Test
	public void testTextFileWriteScript() throws IOException {
		assertFalse( FileSystemUtil.exists( testTextFile ) );
		variables.put( Key.of( "testFile" ), Path.of( testTextFile ).toAbsolutePath().toString() );
		instance.executeSource(
		    """
		    bx:file action="write" file="#testFile#" output="I am writing!";
		    """,
		    context, BoxSourceType.BOXSCRIPT );

		assertThat( FileSystemUtil.exists( testTextFile ) ).isTrue();
		assertThat( FileSystemUtil.read( testTextFile, ( String ) null, ( Integer ) null ) ).isEqualTo( "I am writing!" );
	}

	@Test
	public void testTextFileReadCF() throws IOException {
		assertFalse( FileSystemUtil.exists( testTextFile ) );
		FileSystemUtil.write( testTextFile, "file read test!".getBytes( "UTF-8" ), true );

		variables.put( Key.of( "testFile" ), Path.of( testTextFile ).toAbsolutePath().toString() );
		instance.executeSource(
		    """
		    <cffile action="read" file="#testFile#" variable="readVariable">
		    """,
		    context, BoxSourceType.CFTEMPLATE );

		assertThat( FileSystemUtil.exists( testTextFile ) ).isTrue();
		assertThat( FileSystemUtil.read( testTextFile, ( String ) null, ( Integer ) null ) ).isEqualTo( "file read test!" );
	}

	@Test
	public void testTextFileReadBX() throws IOException {
		assertFalse( FileSystemUtil.exists( testTextFile ) );
		FileSystemUtil.write( testTextFile, "file read test!".getBytes( "UTF-8" ), true );

		variables.put( Key.of( "testFile" ), Path.of( testTextFile ).toAbsolutePath().toString() );
		instance.executeSource(
		    """
		    <bx:file action="read" file="#testFile#" variable="readVariable">
		    """,
		    context, BoxSourceType.BOXTEMPLATE );

		assertThat( FileSystemUtil.exists( testTextFile ) ).isTrue();
		assertThat( FileSystemUtil.read( testTextFile, ( String ) null, ( Integer ) null ) ).isEqualTo( "file read test!" );
	}

	@Test
	public void testTextFileReadScript() throws IOException {
		assertFalse( FileSystemUtil.exists( testTextFile ) );
		FileSystemUtil.write( testTextFile, "file read test!".getBytes( "UTF-8" ), true );

		variables.put( Key.of( "testFile" ), Path.of( testTextFile ).toAbsolutePath().toString() );
		instance.executeSource(
		    """
		    bx:file action="read" file="#testFile#" variable="readVariable"; variable="readVariable"
		    """,
		    context, BoxSourceType.BOXSCRIPT );

		assertThat( FileSystemUtil.exists( testTextFile ) ).isTrue();
		assertThat( FileSystemUtil.read( testTextFile, ( String ) null, ( Integer ) null ) ).isEqualTo( "file read test!" );
	}

	@Test
	public void testFileReadBinary() throws IOException {
		assertFalse( FileSystemUtil.exists( testBinaryFile ) );

		BufferedInputStream urlStream = new BufferedInputStream( URI.create( testURLImage ).toURL().openStream() );
		FileSystemUtil.write( testBinaryFile, urlStream.readAllBytes(), true );

		variables.put( Key.of( "testFile" ), Path.of( testBinaryFile ).toAbsolutePath().toString() );
		instance.executeSource(
		    """
		    bx:file action="readBinary" file="#testFile#" variable="readVariable";
		    """,
		    context, BoxSourceType.BOXSCRIPT );

		Object result = variables.get( Key.of( "readVariable" ) );
		assertTrue( result instanceof byte[] );
	}

	@Test
	public void testFileDelete() throws IOException {
		assertFalse( FileSystemUtil.exists( testTextFile ) );
		FileSystemUtil.write( testTextFile, "file read test!".getBytes( "UTF-8" ), true );

		assertTrue( FileSystemUtil.exists( testTextFile ) );

		variables.put( Key.of( "testFile" ), Path.of( testTextFile ).toAbsolutePath().toString() );
		instance.executeSource(
		    """
		    bx:file action="delete" file="#testFile#";
		    """,
		    context, BoxSourceType.BOXSCRIPT );

		assertFalse( FileSystemUtil.exists( testTextFile ) );
	}

	@Test
	public void testFileDeleteCF() throws IOException {
		assertFalse( FileSystemUtil.exists( testTextFile ) );
		FileSystemUtil.write( testTextFile, "file read test!".getBytes( "UTF-8" ), true );

		assertTrue( FileSystemUtil.exists( testTextFile ) );

		variables.put( Key.of( "testFile" ), Path.of( testTextFile ).toAbsolutePath().toString() );
		instance.executeSource(
		    """
		    <cffile action="delete" file="#testFile#">
		    """,
		    context, BoxSourceType.CFTEMPLATE );

		assertFalse( FileSystemUtil.exists( testTextFile ) );
	}

	@Test
	public void testFileDeleteBX() throws IOException {
		assertFalse( FileSystemUtil.exists( testTextFile ) );
		FileSystemUtil.write( testTextFile, "file read test!".getBytes( "UTF-8" ), true );

		assertTrue( FileSystemUtil.exists( testTextFile ) );

		variables.put( Key.of( "testFile" ), Path.of( testTextFile ).toAbsolutePath().toString() );
		instance.executeSource(
		    """
		    <bx:file action="delete" file="#testFile#">
		    """,
		    context, BoxSourceType.BOXTEMPLATE );

		assertFalse( FileSystemUtil.exists( testTextFile ) );
	}

	@Test
	public void testFileCopy() throws IOException {
		assertFalse( FileSystemUtil.exists( testTextFile ) );
		FileSystemUtil.write( testTextFile, "file read test!".getBytes( "UTF-8" ), true );
		String copiedFile = tmpDirectory + "/copied-file.txt";

		assertTrue( FileSystemUtil.exists( testTextFile ) );
		assertFalse( FileSystemUtil.exists( copiedFile ) );

		variables.put( Key.of( "testFile" ), Path.of( testTextFile ).toAbsolutePath().toString() );
		variables.put( Key.of( "newFile" ), Path.of( copiedFile ).toAbsolutePath().toString() );
		instance.executeSource(
		    """
		    bx:file action="copy" source="#testFile#" destination="#newFile#";
		    """,
		    context, BoxSourceType.BOXSCRIPT );

		assertTrue( FileSystemUtil.exists( testTextFile ) );
		assertTrue( FileSystemUtil.exists( copiedFile ) );
	}

	@Test
	public void testFileCopyCF() throws IOException {
		assertFalse( FileSystemUtil.exists( testTextFile ) );
		FileSystemUtil.write( testTextFile, "file read test!".getBytes( "UTF-8" ), true );
		String copiedFile = tmpDirectory + "/copied-file.txt";

		assertTrue( FileSystemUtil.exists( testTextFile ) );
		assertFalse( FileSystemUtil.exists( copiedFile ) );

		variables.put( Key.of( "testFile" ), Path.of( testTextFile ).toAbsolutePath().toString() );
		variables.put( Key.of( "newFile" ), Path.of( copiedFile ).toAbsolutePath().toString() );
		instance.executeSource(
		    """
		    <cffile action="copy" source="#testFile#" destination="#newFile#">
		    """,
		    context, BoxSourceType.CFTEMPLATE );

		assertTrue( FileSystemUtil.exists( testTextFile ) );
		assertTrue( FileSystemUtil.exists( copiedFile ) );
	}

	@Test
	public void testFileCopyBX() throws IOException {
		assertFalse( FileSystemUtil.exists( testTextFile ) );
		FileSystemUtil.write( testTextFile, "file read test!".getBytes( "UTF-8" ), true );
		String copiedFile = tmpDirectory + "/copied-file.txt";

		assertTrue( FileSystemUtil.exists( testTextFile ) );
		assertFalse( FileSystemUtil.exists( copiedFile ) );

		variables.put( Key.of( "testFile" ), Path.of( testTextFile ).toAbsolutePath().toString() );
		variables.put( Key.of( "newFile" ), Path.of( copiedFile ).toAbsolutePath().toString() );
		instance.executeSource(
		    """
		    <bx:file action="copy" source="#testFile#" destination="#newFile#">
		    """,
		    context, BoxSourceType.BOXTEMPLATE );

		assertTrue( FileSystemUtil.exists( testTextFile ) );
		assertTrue( FileSystemUtil.exists( copiedFile ) );
	}

	@Test
	public void testFileMove() throws IOException {
		assertFalse( FileSystemUtil.exists( testTextFile ) );
		FileSystemUtil.write( testTextFile, "file read test!".getBytes( "UTF-8" ), true );
		String movedFile = tmpDirectory + "/moved-file.txt";

		assertTrue( FileSystemUtil.exists( testTextFile ) );
		assertFalse( FileSystemUtil.exists( movedFile ) );

		variables.put( Key.of( "testFile" ), Path.of( testTextFile ).toAbsolutePath().toString() );
		variables.put( Key.of( "newFile" ), Path.of( movedFile ).toAbsolutePath().toString() );
		instance.executeSource(
		    """
		    bx:file action="move" source="#testFile#" destination="#newFile#";
		    """,
		    context, BoxSourceType.BOXSCRIPT );

		assertFalse( FileSystemUtil.exists( testTextFile ) );
		assertTrue( FileSystemUtil.exists( movedFile ) );
	}

	@Test
	public void testFileMoveCF() throws IOException {
		assertFalse( FileSystemUtil.exists( testTextFile ) );
		FileSystemUtil.write( testTextFile, "file read test!".getBytes( "UTF-8" ), true );
		String movedFile = tmpDirectory + "/moved-file.txt";

		assertTrue( FileSystemUtil.exists( testTextFile ) );
		assertFalse( FileSystemUtil.exists( movedFile ) );

		variables.put( Key.of( "testFile" ), Path.of( testTextFile ).toAbsolutePath().toString() );
		variables.put( Key.of( "newFile" ), Path.of( movedFile ).toAbsolutePath().toString() );
		instance.executeSource(
		    """
		    <cffile action="move" source="#testFile#" destination="#newFile#">
		    """,
		    context, BoxSourceType.CFTEMPLATE );

		assertFalse( FileSystemUtil.exists( testTextFile ) );
		assertTrue( FileSystemUtil.exists( movedFile ) );
	}

	@Test
	public void testFileMoveBX() throws IOException {
		assertFalse( FileSystemUtil.exists( testTextFile ) );
		FileSystemUtil.write( testTextFile, "file read test!".getBytes( "UTF-8" ), true );
		String movedFile = tmpDirectory + "/moved-file.txt";

		assertTrue( FileSystemUtil.exists( testTextFile ) );
		assertFalse( FileSystemUtil.exists( movedFile ) );

		variables.put( Key.of( "testFile" ), Path.of( testTextFile ).toAbsolutePath().toString() );
		variables.put( Key.of( "newFile" ), Path.of( movedFile ).toAbsolutePath().toString() );
		instance.executeSource(
		    """
		    <bx:file action="move" source="#testFile#" destination="#newFile#">
		    """,
		    context, BoxSourceType.BOXTEMPLATE );

		assertFalse( FileSystemUtil.exists( testTextFile ) );
		assertTrue( FileSystemUtil.exists( movedFile ) );
	}

	@Test
	public void testFileRename() throws IOException {
		assertFalse( FileSystemUtil.exists( testTextFile ) );
		FileSystemUtil.write( testTextFile, "file read test!".getBytes( "UTF-8" ), true );
		String movedFile = tmpDirectory + "/moved-file.txt";

		assertTrue( FileSystemUtil.exists( testTextFile ) );
		assertFalse( FileSystemUtil.exists( movedFile ) );

		variables.put( Key.of( "testFile" ), Path.of( testTextFile ).toAbsolutePath().toString() );
		variables.put( Key.of( "newFile" ), Path.of( movedFile ).toAbsolutePath().toString() );
		instance.executeSource(
		    """
		    bx:file action="rename" source="#testFile#" destination="#newFile#";
		    """,
		    context, BoxSourceType.BOXSCRIPT );

		assertFalse( FileSystemUtil.exists( testTextFile ) );
		assertTrue( FileSystemUtil.exists( movedFile ) );
	}

	@Test
	public void testFileAppend() throws IOException {
		assertFalse( FileSystemUtil.exists( testTextFile ) );
		FileSystemUtil.write( testTextFile, "file read test".getBytes( "UTF-8" ), true );

		assertTrue( FileSystemUtil.exists( testTextFile ) );

		variables.put( Key.of( "testFile" ), Path.of( testTextFile ).toAbsolutePath().toString() );
		instance.executeSource(
		    """
		    bx:file action="append" file="#testFile#" output="!";
		    """,
		    context, BoxSourceType.BOXSCRIPT );

		assertThat( FileSystemUtil.read( testTextFile, null, null ) ).isEqualTo( "file read test!" );
	}

}
