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
package ortus.boxlang.compiler;

import static com.google.common.truth.Truth.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ortus.boxlang.runtime.BoxRuntime;

@DisplayName( "SyntaxCheck CLI Tests" )
public class SyntaxCheckTest {

	static BoxRuntime				instance;

	@TempDir
	Path							tempDir;

	private ByteArrayOutputStream	outBuffer;
	private ByteArrayOutputStream	errBuffer;
	private PrintStream				out;
	private PrintStream				err;

	@BeforeAll
	public static void setUp() {
		instance = BoxRuntime.getInstance( true );
	}

	@BeforeEach
	public void resetStreams() {
		this.outBuffer	= new ByteArrayOutputStream();
		this.errBuffer	= new ByteArrayOutputStream();
		this.out		= new PrintStream( this.outBuffer, true, StandardCharsets.UTF_8 );
		this.err		= new PrintStream( this.errBuffer, true, StandardCharsets.UTF_8 );
	}

	private String out() {
		return this.outBuffer.toString( StandardCharsets.UTF_8 );
	}

	private String err() {
		return this.errBuffer.toString( StandardCharsets.UTF_8 );
	}

	private Path createSourceFile( String name, String content ) throws IOException {
		Path file = this.tempDir.resolve( name );
		Files.createDirectories( file.getParent() );
		Files.write( file, content.getBytes( StandardCharsets.UTF_8 ) );
		return file;
	}

	private Path createSourceDir( String dirName ) throws IOException {
		Path dir = this.tempDir.resolve( dirName );
		Files.createDirectories( dir );
		return dir;
	}

	// ---------- Help flag ----------

	@Test
	@DisplayName( "--help returns exit code 0" )
	void testHelpFlag() {
		int exitCode = SyntaxCheck.run( new String[] { "--help" }, this.out, this.err );
		assertThat( exitCode ).isEqualTo( 0 );
		assertThat( out() ).contains( "USAGE" );
	}

	@Test
	@DisplayName( "-h returns exit code 0" )
	void testHelpFlagShort() {
		int exitCode = SyntaxCheck.run( new String[] { "-h" }, this.out, this.err );
		assertThat( exitCode ).isEqualTo( 0 );
	}

	// ---------- No files ----------

	@Test
	@DisplayName( "No files or --source returns exit code 1" )
	void testNoFilesProvided() {
		int exitCode = SyntaxCheck.run( new String[] {}, this.out, this.err );
		assertThat( exitCode ).isEqualTo( 1 );
	}

	// ---------- Non-existent source ----------

	@Test
	@DisplayName( "Non-existent --source returns exit code 1" )
	void testNonExistentSource() {
		int exitCode = SyntaxCheck.run( new String[] {
		    "--source", this.tempDir.resolve( "doesnotexist" ).toString()
		}, this.out, this.err );
		assertThat( exitCode ).isEqualTo( 1 );
	}

	@Test
	@DisplayName( "Non-existent positional file returns exit code 1" )
	void testNonExistentFile() {
		int exitCode = SyntaxCheck.run( new String[] {
		    this.tempDir.resolve( "doesnotexist.bxs" ).toString()
		}, this.out, this.err );
		assertThat( exitCode ).isEqualTo( 1 );
		assertThat( err() ).contains( "does not exist" );
	}

	// ---------- Valid files ----------

	@Test
	@DisplayName( "Valid .bx class file returns exit code 0 with no error output" )
	void testValidBxFile() throws IOException {
		Path	source		= createSourceFile( "Good.bx", "class { function init() { return this; } }" );
		int		exitCode	= SyntaxCheck.run( new String[] { source.toString() }, this.out, this.err );
		assertThat( exitCode ).isEqualTo( 0 );
		assertThat( err() ).isEmpty();
	}

	@Test
	@DisplayName( "Valid .bxs script file returns exit code 0" )
	void testValidBxsFile() throws IOException {
		Path	source		= createSourceFile( "good.bxs", "x = 1 + 2;" );
		int		exitCode	= SyntaxCheck.run( new String[] { source.toString() }, this.out, this.err );
		assertThat( exitCode ).isEqualTo( 0 );
		assertThat( err() ).isEmpty();
	}

	@Test
	@DisplayName( "Valid .cfc file returns exit code 0" )
	void testValidCfcFile() throws IOException {
		Path	source		= createSourceFile( "MyComponent.cfc", "component { function init() { return this; } }" );
		int		exitCode	= SyntaxCheck.run( new String[] { source.toString() }, this.out, this.err );
		assertThat( exitCode ).isEqualTo( 0 );
	}

	@Test
	@DisplayName( "Valid .cfm file returns exit code 0" )
	void testValidCfmFile() throws IOException {
		Path	source		= createSourceFile( "page.cfm", "<cfoutput>Hello</cfoutput>" );
		int		exitCode	= SyntaxCheck.run( new String[] { source.toString() }, this.out, this.err );
		assertThat( exitCode ).isEqualTo( 0 );
	}

	@Test
	@DisplayName( "Valid .cfs file returns exit code 0" )
	void testValidCfsFile() throws IOException {
		Path	source		= createSourceFile( "script.cfs", "x = 1;" );
		int		exitCode	= SyntaxCheck.run( new String[] { source.toString() }, this.out, this.err );
		assertThat( exitCode ).isEqualTo( 0 );
	}

	// ---------- Invalid files ----------

	@Test
	@DisplayName( "Invalid .bxs file returns exit code 1 with issue details" )
	void testInvalidBxsFile() throws IOException {
		Path	source		= createSourceFile( "bad.bxs", "if ( true {" );
		int		exitCode	= SyntaxCheck.run( new String[] { source.toString() }, this.out, this.err );
		assertThat( exitCode ).isEqualTo( 1 );
		assertThat( err() ).contains( "bad.bxs" );
		assertThat( err() ).contains( "Line:" );
	}

	@Test
	@DisplayName( "Invalid .cfm file returns exit code 1" )
	void testInvalidCfmFile() throws IOException {
		Path	source		= createSourceFile( "bad.cfm", "<cfoutput><cfloop><cfif></cfoutput>" );
		int		exitCode	= SyntaxCheck.run( new String[] { source.toString() }, this.out, this.err );
		assertThat( exitCode ).isEqualTo( 1 );
	}

	// ---------- Directory scanning ----------

	@Test
	@DisplayName( "Directory with mixed valid/invalid files returns exit code 1 and reports only the invalid one" )
	void testDirectoryMixedFiles() throws IOException {
		Path dir = createSourceDir( "mixed" );
		createSourceFile( "mixed/good.bxs", "x = 1;" );
		createSourceFile( "mixed/bad.bxs", "if ( true {" );
		createSourceFile( "mixed/readme.txt", "not a source file" );

		int exitCode = SyntaxCheck.run( new String[] { "--source", dir.toString() }, this.out, this.err );
		assertThat( exitCode ).isEqualTo( 1 );
		assertThat( out() ).contains( "good.bxs" );
		assertThat( err() ).contains( "bad.bxs" );
		assertThat( out() ).doesNotContain( "readme.txt" );
	}

	@Test
	@DisplayName( "Directory with only valid files returns exit code 0" )
	void testDirectoryAllValid() throws IOException {
		Path dir = createSourceDir( "allgood" );
		createSourceFile( "allgood/one.bxs", "x = 1;" );
		createSourceFile( "allgood/two.bxs", "y = 2;" );

		int exitCode = SyntaxCheck.run( new String[] { "--source", dir.toString() }, this.out, this.err );
		assertThat( exitCode ).isEqualTo( 0 );
		assertThat( out() ).contains( "2 valid" );
	}

	// ---------- Multiple positional files ----------

	@Test
	@DisplayName( "Multiple positional files, one invalid, returns exit code 1" )
	void testMultipleFilesOneInvalid() throws IOException {
		Path	good		= createSourceFile( "multi-good.bxs", "x = 1;" );
		Path	bad			= createSourceFile( "multi-bad.bxs", "if ( true {" );

		int		exitCode	= SyntaxCheck.run( new String[] { good.toString(), bad.toString() }, this.out, this.err );
		assertThat( exitCode ).isEqualTo( 1 );
	}

	// ---------- --quiet flag ----------

	@Test
	@DisplayName( "--quiet suppresses success output but not failures" )
	void testQuietFlag() throws IOException {
		Path	good		= createSourceFile( "quiet-good.bxs", "x = 1;" );
		Path	bad			= createSourceFile( "quiet-bad.bxs", "if ( true {" );

		int		exitCode	= SyntaxCheck.run( new String[] { "--quiet", good.toString(), bad.toString() }, this.out, this.err );
		assertThat( exitCode ).isEqualTo( 1 );
		assertThat( out() ).isEmpty();
		assertThat( err() ).contains( "quiet-bad.bxs" );
	}

	// ---------- --format json ----------

	@Test
	@DisplayName( "--format json produces a JSON array for a valid file" )
	void testFormatJsonValid() throws IOException {
		Path	source		= createSourceFile( "json-good.bxs", "x = 1;" );
		int		exitCode	= SyntaxCheck.run( new String[] { "--format", "json", source.toString() }, this.out, this.err );
		assertThat( exitCode ).isEqualTo( 0 );
		assertThat( out() ).contains( "\"valid\" : true" );
	}

	@Test
	@DisplayName( "--format json produces a JSON array for an invalid file" )
	void testFormatJsonInvalid() throws IOException {
		Path	source		= createSourceFile( "json-bad.bxs", "if ( true {" );
		int		exitCode	= SyntaxCheck.run( new String[] { "--format", "json", source.toString() }, this.out, this.err );
		assertThat( exitCode ).isEqualTo( 1 );
		assertThat( out() ).contains( "\"valid\" : false" );
		assertThat( out() ).contains( "\"issues\"" );
	}

	@Test
	@DisplayName( "Invalid --format value returns exit code 1" )
	void testInvalidFormatValue() throws IOException {
		Path	source		= createSourceFile( "format-bad.bxs", "x = 1;" );
		int		exitCode	= SyntaxCheck.run( new String[] { "--format", "xml", source.toString() }, this.out, this.err );
		assertThat( exitCode ).isEqualTo( 1 );
	}

}
