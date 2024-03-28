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

import java.io.IOException;
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
	static Key			result				= new Key( "result" );
	static String		testTextFile		= "src/test/resources/tmp/FileComponentTest/text.txt";
	static String		testTextFileScript	= "src/test/resources/tmp/FileComponentTest/textScript.txt";
	static String		testTextFileCF		= "src/test/resources/tmp/FileComponentTest/textCF.txt";
	static String		testTextFileACF		= "src/test/resources/tmp/FileComponentTest/textACF.txt";
	static String		testTextFileBL		= "src/test/resources/tmp/FileComponentTest/textBL.txt";
	static String		testTextFile2		= "src/test/resources/tmp/FileComponentTest/nested/path/text2.txt";
	static String		testNestedFile		= "src/test/resources/tmp/FileComponentTest/nested/path/text.txt";
	static String		testBinaryFile		= "src/test/resources/tmp/FileComponentTest/test.jpg";
	static String		tmpDirectory		= "src/test/resources/tmp/FileComponentTest";

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
		if ( !FileSystemUtil.exists( tmpDirectory ) ) {
			FileSystemUtil.createDirectory( tmpDirectory );
		}
	}

	@Test
	public void testTextFileWrite() throws IOException {
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
		variables.put( Key.of( "testFile" ), Path.of( testTextFileBL ).toAbsolutePath().toString() );
		instance.executeSource(
		    """
		    <bx:file action="write" file="#testFile#" output="I am writing!" >
		    """,
		    context, BoxSourceType.BOXTEMPLATE );

		assertThat( FileSystemUtil.exists( testTextFileBL ) ).isTrue();
		assertThat( FileSystemUtil.read( testTextFileBL, ( String ) null, ( Integer ) null ) ).isEqualTo( "I am writing!" );
	}

	@Test
	public void testTextFileWriteScript() throws IOException {
		variables.put( Key.of( "testFile" ), Path.of( testTextFileScript ).toAbsolutePath().toString() );
		instance.executeSource(
		    """
		    file action="write" file="#testFile#" output="I am writing!";
		    """,
		    context, BoxSourceType.BOXSCRIPT );

		assertThat( FileSystemUtil.exists( testTextFileScript ) ).isTrue();
		assertThat( FileSystemUtil.read( testTextFileScript, ( String ) null, ( Integer ) null ) ).isEqualTo( "I am writing!" );
	}

	@Test
	public void testTextFileWriteCF() throws IOException {
		variables.put( Key.of( "testFile" ), Path.of( testTextFileCF ).toAbsolutePath().toString() );
		instance.executeSource(
		    """
		    file action="write" file="#testFile#" output="I am writing!";
		    """,
		    context, BoxSourceType.CFSCRIPT );

		assertThat( FileSystemUtil.exists( testTextFileCF ) ).isTrue();
		assertThat( FileSystemUtil.read( testTextFileCF, ( String ) null, ( Integer ) null ) ).isEqualTo( "I am writing!" );
	}

	@Test
	public void testTextFileWriteACF() throws IOException {
		variables.put( Key.of( "testFile" ), Path.of( testTextFileACF ).toAbsolutePath().toString() );
		instance.executeSource(
		    """
		    cffile( action="write", file="#testFile#", output="I am writing!" );
		    """,
		    context, BoxSourceType.CFSCRIPT );

		assertThat( FileSystemUtil.exists( testTextFileACF ) ).isTrue();
		assertThat( FileSystemUtil.read( testTextFileACF, ( String ) null, ( Integer ) null ) ).isEqualTo( "I am writing!" );
	}

}
