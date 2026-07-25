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
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.util.CodeEncryption;

@DisplayName( "BXEncryptor CLI Tests" )
public class BXEncryptorTest {

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

	private void configureKey( String keyId, String secret ) {
		instance.getConfiguration().security.codeKeys.put( Key.of( keyId ), secret );
	}

	private int runCli( String[] args ) {
		ByteArrayOutputStream	outBuf	= new ByteArrayOutputStream();
		ByteArrayOutputStream	errBuf	= new ByteArrayOutputStream();
		return BXEncryptor.run( args, new PrintStream( outBuf ), new PrintStream( errBuf ) );
	}

	// ---------- Argument validation ----------

	@Test
	@DisplayName( "--help returns exit code 0" )
	void testHelpFlag() {
		assertThat( runCli( new String[] { "--help" } ) ).isEqualTo( 0 );
	}

	@Test
	@DisplayName( "Missing --target returns exit code 1" )
	void testMissingTarget() {
		assertThat( runCli( new String[] { "--source", this.tempDir.toString(), "--key", "s" } ) ).isEqualTo( 1 );
	}

	@Test
	@DisplayName( "Missing --key returns exit code 1" )
	void testMissingKey() {
		assertThat( runCli( new String[] { "--source", this.tempDir.toString(), "--target", this.tempDir.resolve( "o" ).toString() } ) )
		    .isEqualTo( 1 );
	}

	@Test
	@DisplayName( "Non-existent source returns exit code 1" )
	void testNonExistentSource() {
		int exitCode = runCli( new String[] {
		    "--source", this.tempDir.resolve( "nope" ).toString(),
		    "--target", this.tempDir.resolve( "out" ).toString(),
		    "--key", "s"
		} );
		assertThat( exitCode ).isEqualTo( 1 );
	}

	// ---------- Encrypting ----------

	@Test
	@DisplayName( "Encrypts a file to ciphertext with the given key-id" )
	void testEncryptsFile() throws IOException {
		Path	source		= createSourceFile( "Secret.bxs", "secretValue = \"proprietary-algorithm\";" );
		Path	target		= this.tempDir.resolve( "dist" );

		int		exitCode	= runCli( new String[] {
		    "--source", source.toString(),
		    "--target", target.toString(),
		    "--key", "my-secret", "--key-id", "moduleA"
		} );
		assertThat( exitCode ).isEqualTo( 0 );

		Path output = target.resolve( "Secret.bxs" );
		assertThat( Files.exists( output ) ).isTrue();

		byte[] outBytes = Files.readAllBytes( output );
		assertThat( CodeEncryption.isEncrypted( outBytes ) ).isTrue();
		assertThat( CodeEncryption.readKeyId( outBytes ) ).isEqualTo( "moduleA" );
		// The plaintext must be gone from disk
		assertThat( new String( outBytes, StandardCharsets.UTF_8 ) ).doesNotContain( "proprietary-algorithm" );
	}

	@Test
	@DisplayName( "Already-encrypted files are skipped (no double-encryption)" )
	void testSkipsAlreadyEncrypted() throws IOException {
		byte[]	encrypted	= CodeEncryption.encrypt( "x = 1;".getBytes( StandardCharsets.UTF_8 ), "k", "s" );
		Path	source		= this.tempDir.resolve( "pre/Already.bxs" );
		Files.createDirectories( source.getParent() );
		Files.write( source, encrypted );
		Path	target		= this.tempDir.resolve( "pre-out" );

		int		exitCode	= runCli( new String[] {
		    "--source", source.toString(),
		    "--target", target.toString(),
		    "--key", "another-secret", "--key-id", "k2"
		} );
		assertThat( exitCode ).isEqualTo( 0 );
		// Skipped: nothing written to the target
		assertThat( Files.exists( target.resolve( "Already.bxs" ) ) ).isFalse();
	}

	@Test
	@DisplayName( "Directory mode encrypts supported files and preserves structure" )
	void testDirectoryMode() throws IOException {
		createSourceFile( "proj/main.bxs", "a = 1;" );
		createSourceFile( "proj/sub/helper.cfc", "component { function f(){ return 1; } }" );
		createSourceFile( "proj/notes.txt", "leave me" );

		Path	sourceDir	= this.tempDir.resolve( "proj" );
		Path	targetDir	= this.tempDir.resolve( "proj-dist" );

		int		exitCode	= runCli( new String[] {
		    "--source", sourceDir.toString(),
		    "--target", targetDir.toString(),
		    "--key", "k", "--key-id", "proj"
		} );
		assertThat( exitCode ).isEqualTo( 0 );
		assertThat( CodeEncryption.isEncrypted( Files.readAllBytes( targetDir.resolve( "main.bxs" ) ) ) ).isTrue();
		assertThat( CodeEncryption.isEncrypted( Files.readAllBytes( targetDir.resolve( "sub/helper.cfc" ) ) ) ).isTrue();
		// Non-source files are not encrypted/copied
		assertThat( Files.exists( targetDir.resolve( "notes.txt" ) ) ).isFalse();
	}

	// ---------- End-to-end: encrypt then execute ----------

	@Test
	@DisplayName( "Encrypted template decrypts and executes with the configured key" )
	void testEncryptAndExecute() throws IOException {
		Path	source		= createSourceFile( "calc.bxm", "<bx:set result = 6 * 7>" );
		Path	target		= this.tempDir.resolve( "run" );

		int		exitCode	= runCli( new String[] {
		    "--source", source.toString(),
		    "--target", target.toString(),
		    "--key", "run-secret", "--key-id", "runmod"
		} );
		assertThat( exitCode ).isEqualTo( 0 );

		Path output = target.resolve( "calc.bxm" );
		assertThat( CodeEncryption.isEncrypted( Files.readAllBytes( output ) ) ).isTrue();

		configureKey( "runmod", "run-secret" );
		IBoxContext context = new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		instance.executeTemplate( output.toString(), context );

		Object result = context.getScopeNearby( VariablesScope.name ).get( Key.of( "result" ) );
		assertThat( result.toString() ).isEqualTo( "42" );
	}
}
