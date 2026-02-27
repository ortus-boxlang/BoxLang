
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
package ortus.boxlang.runtime.bifs.global.encryption;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Base64;

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
import ortus.boxlang.runtime.util.EncryptionUtil;

public class EncryptTest {

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

	@DisplayName( "It tests the BIF Encrypt" )
	@Test
	public void testBif() {
		instance.executeSource(
		    """
		    key = generateSecretKey();
		    result = Encrypt( "foo", key );
		    """,
		    context );
		assertTrue( variables.get( result ) instanceof String );
		assertEquals(
		    "foo",
		    EncryptionUtil.decrypt(
		        variables.getAsString( result ),
		        "AES",
		        variables.getAsString( Key.key ),
		        EncryptionUtil.DEFAULT_ENCRYPTION_ENCODING,
		        null,
		        null
		    )
		);

		// Test with all arguments
		instance.executeSource(
		    """
		       key = generateSecretKey( "AES", 256 );
		    iv = "e+T6cmQfzqMpO2oUeboGYw==";
		       result = Encrypt( "foo", key, "AES/CBC/PKCS5Padding", "Base64", binaryDecode( iv, "base64" ), 2000 );
		       """,
		    context );
		assertTrue( variables.get( result ) instanceof String );
		assertEquals(
		    "foo",
		    EncryptionUtil.decrypt(
		        variables.getAsString( result ),
		        "AES/CBC/PKCS5Padding",
		        variables.getAsString( Key.key ),
		        "Base64",
		        Base64.getDecoder().decode( variables.getAsString( Key.of( "iv" ) ) ),
		        2000
		    )
		);

		instance.executeSource(
		    """
		       key = generateSecretKey( "AES", 256 );
		    iv = "e+T6cmQfzqMpO2oUeboGYw==";
		       result = Encrypt( "foo", key, "AES/ECB/PKCS5Padding", "UU", binaryDecode( iv, "base64" ), 2000 );
		       """,
		    context );
		assertTrue( variables.get( result ) instanceof String );
		assertEquals(
		    "foo",
		    EncryptionUtil.decrypt(
		        variables.getAsString( result ),
		        "AES/ECB/PKCS5Padding",
		        variables.getAsString( Key.key ),
		        "UU",
		        Base64.getDecoder().decode( variables.getAsString( Key.of( "iv" ) ) ),
		        2000
		    )
		);

		instance.executeSource(
		    """
		       key = generateSecretKey( "DESede" );
		    iv = generateSecretKey( "DESede" );
		       result = Encrypt( "foo", key, "DESede/ECB/PKCS5Padding", "UU", binaryDecode( iv, "base64" ), 2000 );
		       """,
		    context );
		assertTrue( variables.get( result ) instanceof String );
		assertEquals(
		    "foo",
		    EncryptionUtil.decrypt(
		        variables.getAsString( result ),
		        "DESede/ECB/PKCS5Padding",
		        variables.getAsString( Key.key ),
		        "UU",
		        Base64.getDecoder().decode( variables.getAsString( Key.of( "iv" ) ) ),
		        2000
		    )
		);

		instance.executeSource(
		    """
		    key = generateSecretKey( "Blowfish" );
		    result = Encrypt( "foo", key, "Blowfish", "UU", "foo", 2000 );
		    """,
		    context );
		assertTrue( variables.get( result ) instanceof String );
		assertEquals(
		    "foo",
		    EncryptionUtil.decrypt(
		        variables.getAsString( result ),
		        "Blowfish",
		        variables.getAsString( Key.key ),
		        "UU",
		        "foo".getBytes(),
		        2000
		    )
		);

	}

	@DisplayName( "It tests that backward compat is maintained" )
	@Test
	public void testCompat() {
		instance.executeSource(
		    """
		    key = "oeY9XnYhS4ERqBeMDkcmVw==";
		    iv = "e+T6cmQfzqMpO2oUeboGYw==";
		    result = encrypt( "foobar", key, "AES/CBC/PKCS5Padding", "base64", binaryDecode( iv, "base64" ) );
		    	""", context );

		assertTrue( variables.get( result ) instanceof String );
		assertEquals( "G4w/e9u0DUM+jQTOoxC/nA==", variables.getAsString( result ) );

		// Without an init vector the result will be different every time
		instance.executeSource(
		    """
		    key = "oeY9XnYhS4ERqBeMDkcmVw==";
		    iv = "e+T6cmQfzqMpO2oUeboGYw==";
		    result = encrypt( "foobar", key, "AES/CBC/PKCS5Padding", "base64" );
		    	""", context );
		assertEquals(
		    "foobar",
		    EncryptionUtil.decrypt(
		        variables.getAsString( result ),
		        "AES/CBC/PKCS5Padding",
		        variables.getAsString( Key.key ),
		        "base64",
		        null,
		        1000
		    )
		);
	}

	@DisplayName( "It tests textual salt values" )
	@Test
	public void testSaltValues() {
		instance.executeSource(
		    """
		    key = "oeY9XnYhS4ERqBeMDkcmVw==";
		    salt = "foo";
		    result = encrypt( "foobar", key, "AES", "base64", salt );
		    	""", context );

		assertTrue( variables.get( result ) instanceof String );
		assertEquals( "FE54Rn9W0YoZteRe1aV1qg==", variables.getAsString( result ) );

		instance.executeSource(
		    """
		    key = "oeY9XnYhS4ERqBeMDkcmVw==";
		    salt = "foo";
		    result = encrypt( "foobar", key, "AES/CBC/PKCS5Padding", "base64", salt );
		    	""", context );

		assertTrue( variables.get( result ) instanceof String );
		assertEquals( "mE7BeU2ljKYNWkbKKwqkIA==", variables.getAsString( result ) );

	}

	@DisplayName( "It tests RSA encryption with Base64 encoded private key" )
	@Test
	public void testRSAEncryptionWithPrivateKey() {
		instance.executeSource(
		    """
		    // PKCS#8 format RSA private key Base64 encoded (no PEM headers, single line)
		    privateKey = "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQDi0qOO58vXXa+7xUQHirsGvzdOcLnUF+enI1yzBZLNUL3CaLP7bJnTJ5pAjgZvRymNsOKDSelIV74WQ7y177csS9rOKKG5rjflSCTso6CrQA97m5Jzl/AsL/VOYJZoHYAd5goyT5sfeJ1NeizlgDkvRQxbR/tMGeUJG0i6fdL8su2vf3An0GSDH1QPfh5k/qT6sxSq30XVsZqmXRhgNfamIW29Eyag/GQ8gn55lgOrhl7/EaRWdzRFTxH1G2D9p4rUhG2VL1IHlYgw5V2I5G9/oUhJDl9yoCVRAR4H3DmqEHGmIYhPV3CoeWGgeAAReNoXpkrxp/ZRm9+48LuttqcxAgMBAAECggEAeIL5q3+0aeS47cbNckRfQiJuPBrgYLOivMapBeS8LqHrcFM47SiyQeIUrp/HA3CHv2RvtNmcPHeB40pyBSgr5jkXM9mas5DA2e0O0TvOra4Hi+EIWqorTQ7eApqGzyJ9Y2VJnZl2Da6DxRpYqEcMGvVQr177QV+wtLnuVkdrxh464KBcmGDqcbHUW3TKmDpuJ9ghUff+oO7dnPUx7F6R4gXjJ4N0dgGmYDtDxm2HBYjn2aTjYk2frYnxnxriU0Re3k937TWIhtvA86M/hvr9pGJ/UDDTElvHmjXJPRI/BhpJreIS2Q5YS0060MuKvTu+5kvE1tF50Duh8pMeE9KRkQKBgQDzJyUF490Mov8TvfzTDOThV0O+nGo+X6Bd/D130StaQI2pobaECOu8VhZ980viCbnS2xYELpFzhz/CWBZqV+iTqTZ80zoOn/3Et+gGQYLx34FhtA7FnkACM1Y5N4KD4F0LwyeuFLzVTu/MBq2BwaH9EPfCanDdSfvz9joSoEoQ3wKBgQDuzp2Lo3U4J3B2VQaO27mcVQPtnKMUCooOq87/Y6kTfxNy11m5UOGZgWviZd84ouM9km91Kq8RcffoeG5/6PQmSoTb32oelR1tE0O4qbu9kFxWUGrFPgxeH7dHXqlRYpXYG4e2kB1gQZZ6pdTCprkK1FCjgY1Ce6jYVbfcftj57wKBgDlmYUANtY4ZIFwZuohb/+AOSKjDpfUJgAMP27bgQvqwSIDl8v8iV/wC2pZrC9vVbe+P1pewIpgCMpP/VXNPQ1EwXfODra3sKOz6eSSY7H+KwrE830vZesTKN62UJBRbr7tqG4Dl1loIo2UnomgCPOpPyh00IWar43WJB9aDzlhDAoGAG8qp5RVZz/YvDWZpw/hoSnxOX7nJ9MwhMwHlri0gASfZ0JSlWX7DMoUwVAG9D69NON4w4HbeNu6HhmN1oKcwusATZC9E/1glO4txZy1Brxb82AK12kyVTeLtBn5KwDDz9VmG2sU81fXsGEvyTdDvWgZJeC3cja8sgDjBlASjJbsCgYEAnxZbxLmozoGa9jCWzWat8JXLOFoLx1zRhQUpyZ87t1pTjGCmdfy0NbZptAZr1ermWFUiqytmYwj987v51rN9H/osDqJUDZCWtrSZHfGje6AZ0eIXZexViLe5fqGx/AJB6bkDTqjllEG4RCla5EQXsRN7QCy8AMvbuaMkHR/SIBs=";

		    result = encrypt( "foo", privateKey, "RSA" );
		    """,
		    context );

		assertTrue( variables.get( result ) instanceof String );
		assertTrue( variables.getAsString( result ).length() > 0 );
	}

}
