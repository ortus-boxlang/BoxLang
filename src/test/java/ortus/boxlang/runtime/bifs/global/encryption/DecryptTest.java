
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

public class DecryptTest {

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

	@DisplayName( "It tests the BIF Decrypt" )
	@Test
	public void testBif() {
		instance.executeSource(
		    """
		       key = generateSecretKey();
		    encrypted = Encrypt( "foo", key );
		       result = Decrypt( encrypted, key );
		       """,
		    context );
		assertTrue( variables.get( result ) instanceof String );
		assertEquals(
		    "foo",
		    variables.getAsString( result ) );

		instance.executeSource(
		    """
		    key = generateSecretKey( "Blowfish" );
		    encrypted = Encrypt( "foo", key, "Blowfish", "Hex" );
		       result = Decrypt( encrypted, key, "Blowfish", "Hex" );
		       """,
		    context );
		assertTrue( variables.get( result ) instanceof String );
		assertEquals(
		    "foo",
		    variables.getAsString( result ) );

		instance.executeSource(
		    """
		    key = generateSecretKey( "AES" );
		    encrypted = Encrypt( "foo", key, "AES/ECB/PKCS5Padding", "Hex" );
		       result = Decrypt( encrypted, key, "AES/ECB/PKCS5Padding", "Hex" );
		       """,
		    context );
		assertTrue( variables.get( result ) instanceof String );
		assertEquals(
		    "foo",
		    variables.getAsString( result ) );

		instance.executeSource(
		    """
		    key = generateSecretKey( "DESede" );
		    encrypted = Encrypt( "foo", key, "DESede/ECB/PKCS5Padding", "Hex" );
		       result = Decrypt( encrypted, key, "DESede/ECB/PKCS5Padding", "Hex" );
		       """,
		    context );
		assertTrue( variables.get( result ) instanceof String );
		assertEquals(
		    "foo",
		    variables.getAsString( result ) );

	}

	@DisplayName( "It tests textual salt values" )
	@Test
	public void testSaltValues() {
		instance.executeSource(
		    """
		    key = "oeY9XnYhS4ERqBeMDkcmVw==";
		    salt = "foo";
		    result = decrypt( "FE54Rn9W0YoZteRe1aV1qg==", key, "AES", "base64", salt );
		    	""", context );

		assertTrue( variables.get( result ) instanceof String );
		assertEquals( "foobar", variables.getAsString( result ) );

		instance.executeSource(
		    """
		    key = "oeY9XnYhS4ERqBeMDkcmVw==";
		    salt = "foo";
		    result = decrypt( "mE7BeU2ljKYNWkbKKwqkIA==", key, "AES/CBC/PKCS5Padding", "base64", salt );
		    	""", context );

		assertTrue( variables.get( result ) instanceof String );
		assertEquals( "foobar", variables.getAsString( result ) );

	}

	@DisplayName( "It tests RSA decryption with Base64 encoded private key" )
	@Test
	public void testRSADecryptionWithPrivateKey() {
		instance.executeSource(
		    """
		    // PKCS#8 format RSA private key Base64 encoded (no PEM headers, single line)
		    privateKey = "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQDi0qOO58vXXa+7xUQHirsGvzdOcLnUF+enI1yzBZLNUL3CaLP7bJnTJ5pAjgZvRymNsOKDSelIV74WQ7y177csS9rOKKG5rjflSCTso6CrQA97m5Jzl/AsL/VOYJZoHYAd5goyT5sfeJ1NeizlgDkvRQxbR/tMGeUJG0i6fdL8su2vf3An0GSDH1QPfh5k/qT6sxSq30XVsZqmXRhgNfamIW29Eyag/GQ8gn55lgOrhl7/EaRWdzRFTxH1G2D9p4rUhG2VL1IHlYgw5V2I5G9/oUhJDl9yoCVRAR4H3DmqEHGmIYhPV3CoeWGgeAAReNoXpkrxp/ZRm9+48LuttqcxAgMBAAECggEAeIL5q3+0aeS47cbNckRfQiJuPBrgYLOivMapBeS8LqHrcFM47SiyQeIUrp/HA3CHv2RvtNmcPHeB40pyBSgr5jkXM9mas5DA2e0O0TvOra4Hi+EIWqorTQ7eApqGzyJ9Y2VJnZl2Da6DxRpYqEcMGvVQr177QV+wtLnuVkdrxh464KBcmGDqcbHUW3TKmDpuJ9ghUff+oO7dnPUx7F6R4gXjJ4N0dgGmYDtDxm2HBYjn2aTjYk2frYnxnxriU0Re3k937TWIhtvA86M/hvr9pGJ/UDDTElvHmjXJPRI/BhpJreIS2Q5YS0060MuKvTu+5kvE1tF50Duh8pMeE9KRkQKBgQDzJyUF490Mov8TvfzTDOThV0O+nGo+X6Bd/D130StaQI2pobaECOu8VhZ980viCbnS2xYELpFzhz/CWBZqV+iTqTZ80zoOn/3Et+gGQYLx34FhtA7FnkACM1Y5N4KD4F0LwyeuFLzVTu/MBq2BwaH9EPfCanDdSfvz9joSoEoQ3wKBgQDuzp2Lo3U4J3B2VQaO27mcVQPtnKMUCooOq87/Y6kTfxNy11m5UOGZgWviZd84ouM9km91Kq8RcffoeG5/6PQmSoTb32oelR1tE0O4qbu9kFxWUGrFPgxeH7dHXqlRYpXYG4e2kB1gQZZ6pdTCprkK1FCjgY1Ce6jYVbfcftj57wKBgDlmYUANtY4ZIFwZuohb/+AOSKjDpfUJgAMP27bgQvqwSIDl8v8iV/wC2pZrC9vVbe+P1pewIpgCMpP/VXNPQ1EwXfODra3sKOz6eSSY7H+KwrE830vZesTKN62UJBRbr7tqG4Dl1loIo2UnomgCPOpPyh00IWar43WJB9aDzlhDAoGAG8qp5RVZz/YvDWZpw/hoSnxOX7nJ9MwhMwHlri0gASfZ0JSlWX7DMoUwVAG9D69NON4w4HbeNu6HhmN1oKcwusATZC9E/1glO4txZy1Brxb82AK12kyVTeLtBn5KwDDz9VmG2sU81fXsGEvyTdDvWgZJeC3cja8sgDjBlASjJbsCgYEAnxZbxLmozoGa9jCWzWat8JXLOFoLx1zRhQUpyZ87t1pTjGCmdfy0NbZptAZr1ermWFUiqytmYwj987v51rN9H/osDqJUDZCWtrSZHfGje6AZ0eIXZexViLe5fqGx/AJB6bkDTqjllEG4RCla5EQXsRN7QCy8AMvbuaMkHR/SIBs=";
		    // X509 format RSA public key Base64 encoded (corresponding to the private key above)
		    publicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA4tKjjufL112vu8VEB4q7Br83TnC51BfnpyNcswWSzVC9wmiz+2yZ0yeaQI4Gb0cpjbDig0npSFe+FkO8te+3LEvaziihua435Ugk7KOgq0APe5uSc5fwLC/1TmCWaB2AHeYKMk+bH3idTXos5YA5L0UMW0f7TBnlCRtIun3S/LLtr39wJ9Bkgx9UD34eZP6k+rMUqt9F1bGapl0YYDX2piFtvRMmoPxkPIJ+eZYDq4Ze/xGkVnc0RU8R9Rtg/aeK1IRtlS9SB5WIMOVdiORvf6FISQ5fcqAlUQEeB9w5qhBxpiGIT1dwqHlhoHgAEXjaF6ZK8af2UZvfuPC7rbanMQIDAQAB";

		    // Encrypt with private key, decrypt with public key
		    encrypted = encrypt( "foo", privateKey, "RSA" );
		    result = decrypt( encrypted, publicKey, "RSA" );
		    """,
		    context );

		assertTrue( variables.get( result ) instanceof String );
		assertEquals( "foo", variables.getAsString( result ) );
	}

}
