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

import ortus.boxlang.compiler.ast.BoxClass;
import ortus.boxlang.compiler.parser.Parser;
import ortus.boxlang.compiler.parser.ParsingResult;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.util.CodeEncryption;

@DisplayName( "Encrypted Source (decrypt-before-parse) Tests" )
public class EncryptedSourceTest {

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

	private void configureKey( String keyId, String secret ) {
		instance.getConfiguration().security.codeKeys.put( Key.of( keyId ), secret );
	}

	private Path writeFile( String name, byte[] bytes ) throws IOException {
		Path file = this.tempDir.resolve( name );
		Files.createDirectories( file.getParent() );
		Files.write( file, bytes );
		return file;
	}

	// ---------- Decrypt-before-parse for each requested extension ----------

	@Test
	@DisplayName( "Encrypted .bx class decrypts before parsing" )
	void testEncryptedBxClassParses() throws IOException {
		configureKey( "modBx", "kbx" );
		String			src			= "class { function greet() { return \"hi\"; } }";
		byte[]			encrypted	= CodeEncryption.encrypt( src.getBytes( StandardCharsets.UTF_8 ), "modBx", "kbx" );
		Path			file		= writeFile( "Thing.bx", encrypted );

		ParsingResult	result		= new Parser().parse( file.toFile() );
		assertThat( result.isCorrect() ).isTrue();
		assertThat( result.getRoot() ).isInstanceOf( BoxClass.class );
	}

	@Test
	@DisplayName( "Encrypted .cfc component decrypts before type detection and parsing" )
	void testEncryptedCfcParses() throws IOException {
		configureKey( "modCfc", "kcfc" );
		String			src			= "component { function init() { return this; } }";
		byte[]			encrypted	= CodeEncryption.encrypt( src.getBytes( StandardCharsets.UTF_8 ), "modCfc", "kcfc" );
		Path			file		= writeFile( "Widget.cfc", encrypted );

		ParsingResult	result		= new Parser().parse( file.toFile() );
		assertThat( result.isCorrect() ).isTrue();
		assertThat( result.getRoot() ).isInstanceOf( BoxClass.class );
	}

	@Test
	@DisplayName( "Encrypted .bxm template decrypts before parsing" )
	void testEncryptedBxmParses() throws IOException {
		configureKey( "modBxm", "kbxm" );
		String			src			= "<bx:set greeting = \"hello\">";
		byte[]			encrypted	= CodeEncryption.encrypt( src.getBytes( StandardCharsets.UTF_8 ), "modBxm", "kbxm" );
		Path			file		= writeFile( "view.bxm", encrypted );

		ParsingResult	result		= new Parser().parse( file.toFile() );
		assertThat( result.isCorrect() ).isTrue();
	}

	// ---------- Failure modes ----------

	@Test
	@DisplayName( "Wrong configured key fails to decrypt at parse time" )
	void testWrongKeyFailsAtParse() throws IOException {
		configureKey( "modWrong", "the-wrong-key" );
		byte[]	encrypted	= CodeEncryption.encrypt(
		    "class { function f() { return 1; } }".getBytes( StandardCharsets.UTF_8 ), "modWrong", "the-right-key" );
		Path	file		= writeFile( "Bad.bx", encrypted );

		assertThrows( Exception.class, () -> new Parser().parse( file.toFile() ) );
	}

	@Test
	@DisplayName( "Missing key fails with the keyId named in the message" )
	void testMissingKeyFailsWithKeyId() throws IOException {
		byte[]		encrypted	= CodeEncryption.encrypt(
		    "class { function f() { return 1; } }".getBytes( StandardCharsets.UTF_8 ), "neverConfigured", "s" );
		Path		file		= writeFile( "NoKey.bx", encrypted );

		Exception	ex			= assertThrows( Exception.class, () -> new Parser().parse( file.toFile() ) );
		assertThat( ex.getMessage() ).contains( "neverConfigured" );
	}
}
