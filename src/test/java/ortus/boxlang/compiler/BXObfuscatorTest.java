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

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ortus.boxlang.runtime.BoxRuntime;

@DisplayName( "BXObfuscator CLI Tests" )
public class BXObfuscatorTest {

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

	/**
	 * Obfuscate a single source file and return the obfuscated output as a string.
	 */
	private String obfuscate( String fileName, String content, String... extraArgs ) throws IOException {
		Path	source	= createSourceFile( fileName, content );
		Path	target	= this.tempDir.resolve( "out" );
		Files.createDirectories( target );

		String[]	baseArgs	= new String[] { "--source", source.toString(), "--target", target.toString() };
		String[]	args		= new String[ baseArgs.length + extraArgs.length ];
		System.arraycopy( baseArgs, 0, args, 0, baseArgs.length );
		System.arraycopy( extraArgs, 0, args, baseArgs.length, extraArgs.length );

		int exitCode = runCli( args );
		assertThat( exitCode ).isEqualTo( 0 );

		Path output = target.resolve( fileName );
		assertThat( Files.exists( output ) ).isTrue();
		return Files.readString( output );
	}

	/**
	 * Run the obfuscator CLI capturing streams so tests stay quiet.
	 */
	private int runCli( String[] args ) {
		ByteArrayOutputStream	outBuf	= new ByteArrayOutputStream();
		ByteArrayOutputStream	errBuf	= new ByteArrayOutputStream();
		return BXObfuscator.run( args, new PrintStream( outBuf ), new PrintStream( errBuf ) );
	}

	// ---------- Help / argument validation ----------

	@Test
	@DisplayName( "--help returns exit code 0" )
	void testHelpFlag() {
		assertThat( runCli( new String[] { "--help" } ) ).isEqualTo( 0 );
	}

	@Test
	@DisplayName( "Missing --target returns exit code 1" )
	void testMissingTarget() {
		assertThat( runCli( new String[] { "--source", this.tempDir.toString() } ) ).isEqualTo( 1 );
	}

	@Test
	@DisplayName( "Non-existent source returns exit code 1" )
	void testNonExistentSource() {
		int exitCode = runCli( new String[] {
		    "--source", this.tempDir.resolve( "doesnotexist" ).toString(),
		    "--target", this.tempDir.resolve( "out" ).toString()
		} );
		assertThat( exitCode ).isEqualTo( 1 );
	}

	// ---------- Comment stripping ----------

	@Test
	@DisplayName( "Single-line comments are stripped" )
	void testStripSingleLineComment() throws IOException {
		String result = obfuscate( "a.bxs", """
		                                    // this is a secret comment
		                                    x = 1;
		                                    """ );
		assertThat( result ).doesNotContain( "secret comment" );
	}

	@Test
	@DisplayName( "Multi-line comments are stripped" )
	void testStripMultiLineComment() throws IOException {
		String result = obfuscate( "b.bxs", """
		                                    /* multi
		                                       line secret */
		                                    x = 1;
		                                    """ );
		assertThat( result ).doesNotContain( "secret" );
	}

	@Test
	@DisplayName( "Doc comments on functions are stripped" )
	void testStripDocComment() throws IOException {
		String result = obfuscate( "c.bxs", """
		                                    /**
		                                     * @secretDocTag hidden
		                                     */
		                                    function foo() { return 1; }
		                                    """ );
		assertThat( result ).doesNotContain( "secretDocTag" );
	}

	// ---------- Variable renaming ----------

	@Test
	@DisplayName( "var-declared locals are renamed" )
	void testRenameVarLocal() throws IOException {
		String result = obfuscate( "d.bxs", """
		                                    function foo() {
		                                      var mySecretLocal = 42;
		                                      return mySecretLocal + 1;
		                                    }
		                                    """ );
		assertThat( result ).doesNotContain( "mySecretLocal" );
		// The renamed identifier should appear in the declaration and the usage
		assertThat( result ).contains( "_a" );
	}

	@Test
	@DisplayName( "Non-var globals are NOT renamed" )
	void testGlobalNotRenamed() throws IOException {
		String result = obfuscate( "e.bxs", """
		                                    function foo() {
		                                      globalThing = 42;
		                                      return globalThing;
		                                    }
		                                    """ );
		assertThat( result ).contains( "globalThing" );
	}

	@Test
	@DisplayName( "local.-qualified access stays consistent with renamed var" )
	void testScopeQualifiedAccessRenamed() throws IOException {
		String result = obfuscate( "f.bxs", """
		                                    function foo() {
		                                      var thing = 1;
		                                      return local.thing;
		                                    }
		                                    """ );
		// The original name should be gone from BOTH the declaration and the local. access
		assertThat( result ).doesNotContain( "thing" );
	}

	@Test
	@DisplayName( "--no-rename-vars disables variable renaming" )
	void testNoRenameVars() throws IOException {
		String result = obfuscate( "g.bxs", """
		                                    function foo() {
		                                      var mySecretLocal = 42;
		                                      return mySecretLocal;
		                                    }
		                                    """, "--no-rename-vars" );
		assertThat( result ).contains( "mySecretLocal" );
	}

	@Test
	@DisplayName( "Named argument keys in calls are NOT renamed" )
	void testNamedArgKeyNotRenamed() throws IOException {
		String result = obfuscate( "h.bxs", """
		                                    function foo() {
		                                      var greeting = "hi";
		                                      return doThing( message = greeting );
		                                    }
		                                    """ );
		// The named-argument KEY "message" must be preserved
		assertThat( result ).contains( "message" );
		// The local "greeting" used as the value must be renamed away
		assertThat( result ).doesNotContain( "greeting" );
	}

	@Test
	@DisplayName( "Struct keys are NOT renamed" )
	void testStructKeyNotRenamed() throws IOException {
		String result = obfuscate( "i.bxs", """
		                                    function foo() {
		                                      var data = { secretKey : 1 };
		                                      return data.secretKey;
		                                    }
		                                    """ );
		// Struct member key must survive; the local variable "data" must not
		assertThat( result ).contains( "secretKey" );
		assertThat( result ).doesNotContain( "data" );
	}

	// ---------- Function renaming ----------

	@Test
	@DisplayName( "Private functions renamed with --rename-functions" )
	void testRenamePrivateFunction() throws IOException {
		String result = obfuscate( "PrivComp.bx", """
		                                          class {
		                                            private function secretHelper() { return 1; }
		                                            public function callIt() { return secretHelper(); }
		                                          }
		                                          """, "--rename-functions" );
		assertThat( result ).doesNotContain( "secretHelper" );
		// The public API method must remain callable
		assertThat( result ).contains( "callIt" );
	}

	@Test
	@DisplayName( "Public methods are NOT renamed even with --rename-functions" )
	void testPublicMethodNotRenamed() throws IOException {
		String result = obfuscate( "PubComp.bx", """
		                                         class {
		                                           public function publicApi() { return 1; }
		                                         }
		                                         """, "--rename-functions" );
		assertThat( result ).contains( "publicApi" );
	}

	// ---------- Directory processing ----------

	@Test
	@DisplayName( "Directory mode preserves structure and processes supported files" )
	void testDirectoryMode() throws IOException {
		createSourceFile( "proj/main.bxs", "x = 1;" );
		createSourceFile( "proj/sub/helper.bxs", "y = 2;" );
		createSourceFile( "proj/notes.txt", "ignore me" );

		Path	sourceDir	= this.tempDir.resolve( "proj" );
		Path	targetDir	= this.tempDir.resolve( "dist" );

		int		exitCode	= runCli( new String[] {
		    "--source", sourceDir.toString(),
		    "--target", targetDir.toString()
		} );
		assertThat( exitCode ).isEqualTo( 0 );
		assertThat( Files.exists( targetDir.resolve( "main.bxs" ) ) ).isTrue();
		assertThat( Files.exists( targetDir.resolve( "sub/helper.bxs" ) ) ).isTrue();
		// Non-source files are not copied
		assertThat( Files.exists( targetDir.resolve( "notes.txt" ) ) ).isFalse();
	}

	@Test
	@DisplayName( "--excludes skips matching paths" )
	void testExcludes() throws IOException {
		createSourceFile( "app/keep.bxs", "x = 1;" );
		createSourceFile( "app/vendor/skip.bxs", "y = 2;" );

		Path	sourceDir	= this.tempDir.resolve( "app" );
		Path	targetDir	= this.tempDir.resolve( "app-dist" );
		Path	excludeDir	= sourceDir.resolve( "vendor" );

		int		exitCode	= runCli( new String[] {
		    "--source", sourceDir.toString(),
		    "--target", targetDir.toString(),
		    "--excludes", excludeDir.toString()
		} );
		assertThat( exitCode ).isEqualTo( 0 );
		assertThat( Files.exists( targetDir.resolve( "keep.bxs" ) ) ).isTrue();
		assertThat( Files.exists( targetDir.resolve( "vendor/skip.bxs" ) ) ).isFalse();
	}

	// ---------- Behavior preservation ----------

	@Test
	@DisplayName( "Obfuscated output executes with identical behavior" )
	void testObfuscatedOutputRuns() throws IOException {
		String	result	= obfuscate( "calc.bxs", """
		                                         function add( a, b ) {
		                                           var sum = a + b;
		                                           return sum;
		                                         }
		                                         result = add( 2, 3 );
		                                         """ );
		// Run the obfuscated source and confirm the computation still yields 5
		var		context	= new ortus.boxlang.runtime.context.ScriptingRequestBoxContext( instance.getRuntimeContext() );
		instance.executeSource( result, context );
		Object value = context.getScopeNearby( ortus.boxlang.runtime.scopes.VariablesScope.name )
		    .get( ortus.boxlang.runtime.scopes.Key.of( "result" ) );
		assertThat( value.toString() ).isEqualTo( "5" );
	}
}
