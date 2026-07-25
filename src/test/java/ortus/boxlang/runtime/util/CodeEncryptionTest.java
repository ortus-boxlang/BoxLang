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
package ortus.boxlang.runtime.util;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

@DisplayName( "CodeEncryption Tests" )
public class CodeEncryptionTest {

	static BoxRuntime instance;

	@BeforeAll
	public static void setUp() {
		instance = BoxRuntime.getInstance( true );
	}

	@AfterAll
	public static void teardown() {
	}

	@Test
	@DisplayName( "Encrypt then decrypt round-trips the original bytes" )
	void testRoundTrip() {
		byte[]	plain		= "function foo() { var x = 42; return x; }".getBytes( StandardCharsets.UTF_8 );
		byte[]	encrypted	= CodeEncryption.encrypt( plain, "moduleA", "super-secret" );
		byte[]	decrypted	= CodeEncryption.decrypt( encrypted, "super-secret" );
		assertThat( new String( decrypted, StandardCharsets.UTF_8 ) ).isEqualTo( new String( plain, StandardCharsets.UTF_8 ) );
	}

	@Test
	@DisplayName( "Encrypted bytes are detected; plain bytes are not" )
	void testIsEncrypted() {
		byte[] encrypted = CodeEncryption.encrypt( "x = 1;".getBytes( StandardCharsets.UTF_8 ), "k", "secret" );
		assertThat( CodeEncryption.isEncrypted( encrypted ) ).isTrue();
		assertThat( CodeEncryption.isEncrypted( "x = 1;".getBytes( StandardCharsets.UTF_8 ) ) ).isFalse();
		assertThat( CodeEncryption.isEncrypted( new byte[] { 1, 2 } ) ).isFalse();
		assertThat( CodeEncryption.isEncrypted( null ) ).isFalse();
	}

	@Test
	@DisplayName( "The keyId is readable from the encrypted header" )
	void testReadKeyId() {
		byte[] encrypted = CodeEncryption.encrypt( "x = 1;".getBytes( StandardCharsets.UTF_8 ), "moduleB", "secret" );
		assertThat( CodeEncryption.readKeyId( encrypted ) ).isEqualTo( "moduleB" );
	}

	@Test
	@DisplayName( "Decrypting with the wrong key fails (authenticated)" )
	void testWrongKeyFails() {
		byte[] encrypted = CodeEncryption.encrypt( "x = 1;".getBytes( StandardCharsets.UTF_8 ), "k", "right-key" );
		assertThrows( BoxRuntimeException.class, () -> CodeEncryption.decrypt( encrypted, "wrong-key" ) );
	}

	@Test
	@DisplayName( "Tampering with the ciphertext is detected" )
	void testTamperFails() {
		byte[] encrypted = CodeEncryption.encrypt( "x = 1;".getBytes( StandardCharsets.UTF_8 ), "k", "secret" );
		// Flip a byte in the ciphertext region (end of buffer)
		encrypted[ encrypted.length - 1 ] ^= 0x01;
		assertThrows( BoxRuntimeException.class, () -> CodeEncryption.decrypt( encrypted, "secret" ) );
	}

	@Test
	@DisplayName( "Two key-ids with different keys are independent" )
	void testTwoKeysAreIndependent() {
		byte[]	encA	= CodeEncryption.encrypt( "a = 1;".getBytes( StandardCharsets.UTF_8 ), "moduleA", "keyA" );
		byte[]	encB	= CodeEncryption.encrypt( "b = 2;".getBytes( StandardCharsets.UTF_8 ), "moduleB", "keyB" );
		// Each decrypts only with its own key
		assertThat( new String( CodeEncryption.decrypt( encA, "keyA" ), StandardCharsets.UTF_8 ) ).isEqualTo( "a = 1;" );
		assertThat( new String( CodeEncryption.decrypt( encB, "keyB" ), StandardCharsets.UTF_8 ) ).isEqualTo( "b = 2;" );
		assertThrows( BoxRuntimeException.class, () -> CodeEncryption.decrypt( encA, "keyB" ) );
	}

	@Test
	@DisplayName( "normalizeEnv upcases and replaces non-alphanumerics" )
	void testNormalizeEnv() {
		assertThat( CodeEncryption.normalizeEnv( "moduleA" ) ).isEqualTo( "MODULEA" );
		assertThat( CodeEncryption.normalizeEnv( "bx-compat.foo" ) ).isEqualTo( "BX_COMPAT_FOO" );
	}

	@Test
	@DisplayName( "resolveKey reads from security.codeKeys config" )
	void testResolveKeyFromConfig() {
		instance.getConfiguration().security.codeKeys.put( Key.of( "moduleZ" ), "config-secret" );
		assertThat( CodeEncryption.resolveKey( "moduleZ" ) ).isEqualTo( "config-secret" );
		assertThat( CodeEncryption.resolveKey( "nope-not-configured" ) ).isNull();
	}

	@Test
	@DisplayName( "maybeDecrypt passes plain bytes through unchanged" )
	void testMaybeDecryptPassThrough() {
		byte[] plain = "just plain source".getBytes( StandardCharsets.UTF_8 );
		assertThat( CodeEncryption.maybeDecrypt( plain ) ).isEqualTo( plain );
	}

	@Test
	@DisplayName( "maybeDecrypt decrypts using a configured key" )
	void testMaybeDecryptWithConfiguredKey() {
		instance.getConfiguration().security.codeKeys.put( Key.of( "moduleY" ), "y-secret" );
		byte[]	encrypted	= CodeEncryption.encrypt( "hello".getBytes( StandardCharsets.UTF_8 ), "moduleY", "y-secret" );
		byte[]	decrypted	= CodeEncryption.maybeDecrypt( encrypted );
		assertThat( new String( decrypted, StandardCharsets.UTF_8 ) ).isEqualTo( "hello" );
	}

	@Test
	@DisplayName( "maybeDecrypt fails clearly when no key is configured" )
	void testMaybeDecryptMissingKey() {
		byte[]				encrypted	= CodeEncryption.encrypt( "hidden".getBytes( StandardCharsets.UTF_8 ), "unconfiguredModule", "some-secret" );
		BoxRuntimeException	ex			= assertThrows( BoxRuntimeException.class, () -> CodeEncryption.maybeDecrypt( encrypted ) );
		assertThat( ex.getMessage() ).contains( "unconfiguredModule" );
	}

	// ---------- enforceEncryptedSource (anti-webshell) ----------

	@Test
	@DisplayName( "enforceEncryptedSource blocks plaintext but still runs encrypted" )
	void testEnforceEncryptedSource() {
		boolean previous = instance.getConfiguration().security.enforceEncryptedSource;
		try {
			instance.getConfiguration().security.enforceEncryptedSource = true;
			instance.getConfiguration().security.codeKeys.put( Key.of( "enforceMod" ), "enf-secret" );

			// Plaintext is blocked
			byte[]				plain	= "x = 1;".getBytes( StandardCharsets.UTF_8 );
			BoxRuntimeException	ex		= assertThrows( BoxRuntimeException.class, () -> CodeEncryption.maybeDecrypt( plain ) );
			assertThat( ex.getMessage() ).contains( "enforceEncryptedSource" );

			// Encrypted source still decrypts normally
			byte[] encrypted = CodeEncryption.encrypt( "y = 2;".getBytes( StandardCharsets.UTF_8 ), "enforceMod", "enf-secret" );
			assertThat( new String( CodeEncryption.maybeDecrypt( encrypted ), StandardCharsets.UTF_8 ) ).isEqualTo( "y = 2;" );
		} finally {
			instance.getConfiguration().security.enforceEncryptedSource = previous;
		}
	}

	@Test
	@DisplayName( "isEnforceEncryptedSource reflects the security config" )
	void testIsEnforceEncryptedSource() {
		boolean previous = instance.getConfiguration().security.enforceEncryptedSource;
		try {
			instance.getConfiguration().security.enforceEncryptedSource = true;
			assertThat( CodeEncryption.isEnforceEncryptedSource() ).isTrue();
			instance.getConfiguration().security.enforceEncryptedSource = false;
			assertThat( CodeEncryption.isEnforceEncryptedSource() ).isFalse();
		} finally {
			instance.getConfiguration().security.enforceEncryptedSource = previous;
		}
	}
}
