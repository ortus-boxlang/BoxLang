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
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

@DisplayName( "CFTranspiler CLI Tests" )
public class CFTranspilerCLITest {

	static BoxRuntime	instance;

	@TempDir
	Path				tempDir;

	@BeforeAll
	public static void setUp() {
		instance = BoxRuntime.getInstance( true );
	}

	@AfterAll
	public static void teardown() {
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
		int exitCode = CFTranspiler.run( new String[] { "--help" } );
		assertThat( exitCode ).isEqualTo( 0 );
	}

	@Test
	@DisplayName( "-h returns exit code 0" )
	void testHelpFlagShort() {
		int exitCode = CFTranspiler.run( new String[] { "-h" } );
		assertThat( exitCode ).isEqualTo( 0 );
	}

	// ---------- Missing target ----------

	@Test
	@DisplayName( "Missing --target throws exception" )
	void testMissingTarget() {
		assertThrows( BoxRuntimeException.class, () -> {
			CFTranspiler.run( new String[] { "--source", this.tempDir.toString() } );
		} );
	}

	// ---------- Non-existent source ----------

	@Test
	@DisplayName( "Non-existent source returns exit code 1" )
	void testNonExistentSource() {
		int exitCode = CFTranspiler.run( new String[] {
		    "--source", this.tempDir.resolve( "doesnotexist" ).toString(),
		    "--target", this.tempDir.resolve( "output" ).toString()
		} );
		assertThat( exitCode ).isEqualTo( 1 );
	}

	// ---------- Single file transpilation ----------

	@Test
	@DisplayName( "Transpile single .cfm file to .bxm" )
	void testTranspileSingleCfm() throws IOException {
		Path	source	= createSourceFile( "test.cfm", "<cfoutput>Hello</cfoutput>" );
		Path	target	= this.tempDir.resolve( "output" );
		Files.createDirectories( target );

		int exitCode = CFTranspiler.run( new String[] {
		    "--source", source.toString(),
		    "--target", target.toString()
		} );
		assertThat( exitCode ).isEqualTo( 0 );
		// Should create a .bxm file in the target directory
		assertThat( Files.list( target ).anyMatch( p -> p.getFileName().toString().endsWith( ".bxm" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "Transpile single .cfc file to .bx" )
	void testTranspileSingleCfc() throws IOException {
		Path	source	= createSourceFile( "MyComponent.cfc", "component { function init() { return this; } }" );
		Path	target	= this.tempDir.resolve( "output" );
		Files.createDirectories( target );

		int exitCode = CFTranspiler.run( new String[] {
		    "--source", source.toString(),
		    "--target", target.toString()
		} );
		assertThat( exitCode ).isEqualTo( 0 );
		assertThat( Files.list( target ).anyMatch( p -> p.getFileName().toString().endsWith( ".bx" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "Transpile single .cfs file to .bxs" )
	void testTranspileSingleCfs() throws IOException {
		Path	source	= createSourceFile( "script.cfs", "x = 1;" );
		Path	target	= this.tempDir.resolve( "output" );
		Files.createDirectories( target );

		int exitCode = CFTranspiler.run( new String[] {
		    "--source", source.toString(),
		    "--target", target.toString()
		} );
		assertThat( exitCode ).isEqualTo( 0 );
		assertThat( Files.list( target ).anyMatch( p -> p.getFileName().toString().endsWith( ".bxs" ) ) ).isTrue();
	}

	// ---------- Directory transpilation ----------

	@Test
	@DisplayName( "Transpile directory of CFML files" )
	void testTranspileDirectory() throws IOException {
		Path sourceDir = createSourceDir( "cfml-src" );
		createSourceFile( "cfml-src/page.cfm", "<cfoutput>Hello</cfoutput>" );
		createSourceFile( "cfml-src/comp.cfc", "component { function init() { return this; } }" );
		createSourceFile( "cfml-src/readme.txt", "This is a readme" );

		Path	targetDir	= this.tempDir.resolve( "bx-output" );

		int		exitCode	= CFTranspiler.run( new String[] {
		    "--source", sourceDir.toString(),
		    "--target", targetDir.toString()
		} );
		assertThat( exitCode ).isEqualTo( 0 );
		assertThat( Files.exists( targetDir.resolve( "page.bxm" ) ) ).isTrue();
		assertThat( Files.exists( targetDir.resolve( "comp.bx" ) ) ).isTrue();
		// Without --copy-others, non-CFML files should NOT be copied
		assertThat( Files.exists( targetDir.resolve( "readme.txt" ) ) ).isFalse();
	}

	// ---------- --copy-others flag ----------

	@Test
	@DisplayName( "--copy-others copies non-CFML files to target (different dirs)" )
	void testCopyOthersFlag() throws IOException {
		Path sourceDir = createSourceDir( "cfml-copy" );
		createSourceFile( "cfml-copy/page.cfm", "<cfoutput>Hello</cfoutput>" );
		createSourceFile( "cfml-copy/styles.css", "body { color: red; }" );
		createSourceFile( "cfml-copy/logo.png", "fakepng" );

		Path	targetDir	= this.tempDir.resolve( "bx-copy-output" );

		int		exitCode	= CFTranspiler.run( new String[] {
		    "--source", sourceDir.toString(),
		    "--target", targetDir.toString(),
		    "--copy-others"
		} );
		assertThat( exitCode ).isEqualTo( 0 );
		assertThat( Files.exists( targetDir.resolve( "page.bxm" ) ) ).isTrue();
		assertThat( Files.exists( targetDir.resolve( "styles.css" ) ) ).isTrue();
		assertThat( Files.exists( targetDir.resolve( "logo.png" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "--copy-others is ignored when source and target are the same" )
	void testCopyOthersIgnoredWhenSameDir() throws IOException {
		Path sameDir = createSourceDir( "cfml-same" );
		createSourceFile( "cfml-same/page.cfm", "<cfoutput>Hello</cfoutput>" );
		createSourceFile( "cfml-same/readme.txt", "readme content" );

		int exitCode = CFTranspiler.run( new String[] {
		    "--source", sameDir.toString(),
		    "--target", sameDir.toString(),
		    "--copy-others"
		} );
		assertThat( exitCode ).isEqualTo( 0 );
		// The .bxm file should be created
		assertThat( Files.exists( sameDir.resolve( "page.bxm" ) ) ).isTrue();
		// Original cfm still exists (no --delete-old)
		assertThat( Files.exists( sameDir.resolve( "page.cfm" ) ) ).isTrue();
	}

	// ---------- --delete-old flag ----------

	@Test
	@DisplayName( "--delete-old deletes CFML source files when source and target are the same" )
	void testDeleteOldSameDir() throws IOException {
		Path sameDir = createSourceDir( "cfml-delete" );
		createSourceFile( "cfml-delete/page.cfm", "<cfoutput>Hello</cfoutput>" );
		createSourceFile( "cfml-delete/comp.cfc", "component { function init() { return this; } }" );

		int exitCode = CFTranspiler.run( new String[] {
		    "--source", sameDir.toString(),
		    "--target", sameDir.toString(),
		    "--delete-old"
		} );
		assertThat( exitCode ).isEqualTo( 0 );
		// Transpiled files should exist
		assertThat( Files.exists( sameDir.resolve( "page.bxm" ) ) ).isTrue();
		assertThat( Files.exists( sameDir.resolve( "comp.bx" ) ) ).isTrue();
		// Old CFML files should be deleted
		assertThat( Files.exists( sameDir.resolve( "page.cfm" ) ) ).isFalse();
		assertThat( Files.exists( sameDir.resolve( "comp.cfc" ) ) ).isFalse();
	}

	@Test
	@DisplayName( "--delete-old is ignored when source and target differ" )
	void testDeleteOldIgnoredWhenDifferentDirs() throws IOException {
		Path sourceDir = createSourceDir( "cfml-nodelete" );
		createSourceFile( "cfml-nodelete/page.cfm", "<cfoutput>Hello</cfoutput>" );

		Path	targetDir	= this.tempDir.resolve( "bx-nodelete-output" );

		int		exitCode	= CFTranspiler.run( new String[] {
		    "--source", sourceDir.toString(),
		    "--target", targetDir.toString(),
		    "--delete-old"
		} );
		assertThat( exitCode ).isEqualTo( 0 );
		assertThat( Files.exists( targetDir.resolve( "page.bxm" ) ) ).isTrue();
		// Source file should NOT be deleted since dirs are different
		assertThat( Files.exists( sourceDir.resolve( "page.cfm" ) ) ).isTrue();
	}

	// ---------- --verbose flag ----------

	@Test
	@DisplayName( "--verbose flag does not affect exit code" )
	void testVerboseFlag() throws IOException {
		Path	source	= createSourceFile( "verbose.cfm", "<cfoutput>Hello</cfoutput>" );
		Path	target	= this.tempDir.resolve( "verbose-output" );
		Files.createDirectories( target );

		int exitCode = CFTranspiler.run( new String[] {
		    "--source", source.toString(),
		    "--target", target.toString(),
		    "--verbose"
		} );
		assertThat( exitCode ).isEqualTo( 0 );
	}

	@Test
	@DisplayName( "-v short flag works" )
	void testVerboseFlagShort() throws IOException {
		Path	source	= createSourceFile( "verboseshort.cfm", "<cfoutput>Hello</cfoutput>" );
		Path	target	= this.tempDir.resolve( "verbose-short-output" );
		Files.createDirectories( target );

		int exitCode = CFTranspiler.run( new String[] {
		    "--source", source.toString(),
		    "--target", target.toString(),
		    "-v"
		} );
		assertThat( exitCode ).isEqualTo( 0 );
	}

	// ---------- --stopOnError flag ----------

	@Test
	@DisplayName( "--stopOnError throws on parse failure" )
	void testStopOnError() throws IOException {
		Path sourceDir = createSourceDir( "cfml-stop" );
		createSourceFile( "cfml-stop/bad.cfm", "<cfoutput><cfloop><cfif></cfoutput>" );

		Path targetDir = this.tempDir.resolve( "bx-stop-output" );

		assertThrows( BoxRuntimeException.class, () -> {
			CFTranspiler.run( new String[] {
			    "--source", sourceDir.toString(),
			    "--target", targetDir.toString(),
			    "--stopOnError"
			} );
		} );
	}

	@Test
	@DisplayName( "Without --stopOnError, parse failures are skipped" )
	void testContinueOnError() throws IOException {
		Path sourceDir = createSourceDir( "cfml-continue" );
		createSourceFile( "cfml-continue/bad.cfm", "<cfoutput><cfloop><cfif></cfoutput>" );
		createSourceFile( "cfml-continue/good.cfm", "<cfoutput>Hello</cfoutput>" );

		Path	targetDir	= this.tempDir.resolve( "bx-continue-output" );

		int		exitCode	= assertDoesNotThrow( () -> CFTranspiler.run( new String[] {
		    "--source", sourceDir.toString(),
		    "--target", targetDir.toString()
		} ) );
		assertThat( exitCode ).isEqualTo( 0 );
		// Good file should still be transpiled
		assertThat( Files.exists( targetDir.resolve( "good.bxm" ) ) ).isTrue();
	}

	// ---------- Directory structure preservation ----------

	@Test
	@DisplayName( "Nested directory structure is preserved in target" )
	void testNestedDirectoryStructure() throws IOException {
		Path sourceDir = createSourceDir( "cfml-nested" );
		createSourceFile( "cfml-nested/sub/deep/page.cfm", "<cfoutput>Nested</cfoutput>" );
		createSourceFile( "cfml-nested/sub/comp.cfc", "component { function init() { return this; } }" );

		Path	targetDir	= this.tempDir.resolve( "bx-nested-output" );

		int		exitCode	= CFTranspiler.run( new String[] {
		    "--source", sourceDir.toString(),
		    "--target", targetDir.toString()
		} );
		assertThat( exitCode ).isEqualTo( 0 );
		assertThat( Files.exists( targetDir.resolve( "sub/deep/page.bxm" ) ) ).isTrue();
		assertThat( Files.exists( targetDir.resolve( "sub/comp.bx" ) ) ).isTrue();
	}

	// ---------- --copy-others with nested dirs ----------

	@Test
	@DisplayName( "--copy-others preserves nested non-CFML files" )
	void testCopyOthersNested() throws IOException {
		Path sourceDir = createSourceDir( "cfml-copy-nested" );
		createSourceFile( "cfml-copy-nested/page.cfm", "<cfoutput>Hello</cfoutput>" );
		createSourceFile( "cfml-copy-nested/assets/style.css", "body{}" );
		createSourceFile( "cfml-copy-nested/assets/img/logo.png", "fakepng" );

		Path	targetDir	= this.tempDir.resolve( "bx-copy-nested-output" );

		int		exitCode	= CFTranspiler.run( new String[] {
		    "--source", sourceDir.toString(),
		    "--target", targetDir.toString(),
		    "--copy-others"
		} );
		assertThat( exitCode ).isEqualTo( 0 );
		assertThat( Files.exists( targetDir.resolve( "page.bxm" ) ) ).isTrue();
		assertThat( Files.exists( targetDir.resolve( "assets/style.css" ) ) ).isTrue();
		assertThat( Files.exists( targetDir.resolve( "assets/img/logo.png" ) ) ).isTrue();
	}

	// ---------- Target directory auto-creation ----------

	@Test
	@DisplayName( "Target directory is created if it does not exist" )
	void testTargetDirectoryAutoCreation() throws IOException {
		Path sourceDir = createSourceDir( "cfml-autocreate" );
		createSourceFile( "cfml-autocreate/page.cfm", "<cfoutput>Hello</cfoutput>" );

		Path	targetDir	= this.tempDir.resolve( "nonexistent/deeply/nested/output" );

		int		exitCode	= CFTranspiler.run( new String[] {
		    "--source", sourceDir.toString(),
		    "--target", targetDir.toString()
		} );
		assertThat( exitCode ).isEqualTo( 0 );
		assertThat( Files.exists( targetDir.resolve( "page.bxm" ) ) ).isTrue();
	}
}
