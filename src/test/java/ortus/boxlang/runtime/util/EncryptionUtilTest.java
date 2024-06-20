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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;

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

public class EncryptionUtilTest {

	static BoxRuntime	instance;
	IBoxContext			context;
	IScope				variables;
	static Key			result	= new Key( "result" );

	@BeforeAll
	public static void setUp() {
		instance = BoxRuntime.getInstance( true );
	}

	@AfterAll
	public static void teardown() {
	}

	@BeforeEach
	public void setupEach() {
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
	}

	@DisplayName( "Can generate secret keys" )
	@Test
	void testKeyGeneration() {
		assertTrue( EncryptionUtil.generateKey( "AES" ) instanceof SecretKey );
		assertTrue( EncryptionUtil.generateKey( "DES" ) instanceof SecretKey );
		assertTrue( EncryptionUtil.generateKey( "DESEDE" ) instanceof SecretKey );
		assertTrue( EncryptionUtil.generateKey( "BLOWFISH" ) instanceof SecretKey );
		assertTrue( EncryptionUtil.generateKey( "CHACHA20" ) instanceof SecretKey );
		assertTrue( EncryptionUtil.generateKey( "ARCFOUR" ) instanceof SecretKey );
	}

	@DisplayName( "Can generate secret keys with a keysize argument" )
	@Test
	void testKeyGenerationKeySize() {
		assertTrue( EncryptionUtil.generateKey( "AES", 128 ) instanceof SecretKey );
		assertTrue( EncryptionUtil.generateKey( "DES", 56 ) instanceof SecretKey );
		assertTrue( EncryptionUtil.generateKey( "DESEDE", 168 ) instanceof SecretKey );
		assertTrue( EncryptionUtil.generateKey( "BLOWFISH", 56 ) instanceof SecretKey );
		assertTrue( EncryptionUtil.generateKey( "CHACHA20", 256 ) instanceof SecretKey );
		assertTrue( EncryptionUtil.generateKey( "ARCFOUR", 56 ) instanceof SecretKey );
	}

	@DisplayName( "Can encode and decode secret keys" )
	@Test
	void testEncodeDecodeKey() {
		SecretKey	key			= EncryptionUtil.generateKey( "AES" );
		String		encodedKey	= EncryptionUtil.encodeKey( key );
		assertEquals( key, EncryptionUtil.decodeKey( encodedKey, "AES" ) );
	}

	@DisplayName( "Can create Ciphers" )
	@Test
	void testCreateCipher() {
		SecretKey key = EncryptionUtil.generateKey( "AES" );
		assertTrue( EncryptionUtil.createCipher( "AES", key, null, Cipher.ENCRYPT_MODE ) instanceof Cipher );
		assertTrue( EncryptionUtil.createCipher( "AES/CBC/PKCS5Padding", key, null, Cipher.ENCRYPT_MODE ) instanceof Cipher );
	}

	@DisplayName( "Can encrypt and decrypt objects" )
	@Test
	void testEncryptDecrypt() {
		SecretKey	key			= EncryptionUtil.generateKey( "AES" );
		String		encrypted	= EncryptionUtil.encrypt( "Hello, World!", "AES", key, "Base64", null, null );
		assertEquals( "Hello, World!", EncryptionUtil.decrypt( encrypted, "AES", key, "Base64", null, null ) );
	}

}