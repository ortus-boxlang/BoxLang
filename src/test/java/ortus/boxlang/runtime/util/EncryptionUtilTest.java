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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;

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

	@DisplayName( "Can encrypt and decrypt objects" )
	@Test
	void testEncryptDecrypt() {
		String	key			= EncryptionUtil.encodeKey( EncryptionUtil.generateKey( "AES" ) );
		String	encrypted	= EncryptionUtil.encrypt( "Hello, World!", "AES", key, "Base64", null, null );
		assertEquals( "Hello, World!", EncryptionUtil.decrypt( encrypted, "AES", key, "Base64", null, null ) );

		encrypted = EncryptionUtil.encrypt( "Hello, World!", "AES/CBC/PKCS5Padding", key, "Base64", null, null );
		assertEquals( "Hello, World!", EncryptionUtil.decrypt( encrypted, "AES/CBC/PKCS5Padding", key, "Base64", null, null ) );
	}

	@DisplayName( "Can UUencode and UUdecode byte arrays" )
	@Test
	void testUUEncodeDecode() {
		// Empty array
		byte[]	empty	= new byte[ 0 ];
		String	encoded	= EncryptionUtil.uuEncode( empty );
		assertEquals( "`", encoded );
		assertArrayEquals( empty, EncryptionUtil.uuDecode( encoded ) );

		// Single byte
		byte[] single = new byte[] { 65 }; // 'A'
		encoded = EncryptionUtil.uuEncode( single );
		assertArrayEquals( single, EncryptionUtil.uuDecode( encoded ) );

		// Two bytes
		byte[] twoBytes = new byte[] { 65, 66 }; // 'A', 'B'
		encoded = EncryptionUtil.uuEncode( twoBytes );
		assertArrayEquals( twoBytes, EncryptionUtil.uuDecode( encoded ) );

		// Three bytes
		byte[] threeBytes = new byte[] { 65, 66, 67 }; // 'A', 'B', 'C'
		encoded = EncryptionUtil.uuEncode( threeBytes );
		assertArrayEquals( threeBytes, EncryptionUtil.uuDecode( encoded ) );

		// Full line (45 bytes)
		byte[] fullLine = "123456789012345678901234567890123456789012345".getBytes( StandardCharsets.UTF_8 );
		assertEquals( 45, fullLine.length );
		encoded = EncryptionUtil.uuEncode( fullLine );
		assertArrayEquals( fullLine, EncryptionUtil.uuDecode( encoded ) );

		// 46 bytes (should produce two lines)
		byte[] twoLines = "1234567890123456789012345678901234567890123456".getBytes( StandardCharsets.UTF_8 );
		assertEquals( 46, twoLines.length );
		encoded = EncryptionUtil.uuEncode( twoLines );
		assertArrayEquals( twoLines, EncryptionUtil.uuDecode( encoded ) );

		// Longer message
		String	message		= "BoxLang is Great!";
		byte[]	msgBytes	= message.getBytes( StandardCharsets.UTF_8 );
		encoded = EncryptionUtil.uuEncode( msgBytes );
		assertArrayEquals( msgBytes, EncryptionUtil.uuDecode( encoded ) );

		// Binary data with all byte values
		byte[] allBytes = new byte[ 256 ];
		for ( int i = 0; i < 256; i++ ) {
			allBytes[ i ] = ( byte ) i;
		}
		encoded = EncryptionUtil.uuEncode( allBytes );
		assertArrayEquals( allBytes, EncryptionUtil.uuDecode( encoded ) );

		// Null input
		assertEquals( "`", EncryptionUtil.uuEncode( null ) );
		assertArrayEquals( new byte[ 0 ], EncryptionUtil.uuDecode( null ) );

		// Empty string decode
		assertArrayEquals( new byte[ 0 ], EncryptionUtil.uuDecode( "" ) );
	}

	@DisplayName( "Can encrypt with UU encoding and produce expected output" )
	@Test
	void testEncryptWithUUEncoding() {
		String encrypted = EncryptionUtil.encrypt( "password", "AES", "GA+KUbtz0NmF8goZ2Z4MFQ==", "UU", null, null );
		assertEquals( "0:]LFXG.'VS9^=_V&L\\R41P", encrypted );
	}

	@DisplayName( "Can encrypt and decrypt with UU encoding round-trip" )
	@Test
	void testEncryptDecryptUU() {
		String	key			= EncryptionUtil.encodeKey( EncryptionUtil.generateKey( "AES" ) );
		String	encrypted	= EncryptionUtil.encrypt( "Hello, World!", "AES", key, "UU", null, null );
		assertEquals( "Hello, World!", EncryptionUtil.decrypt( encrypted, "AES", key, "UU", null, null ) );
	}

}
