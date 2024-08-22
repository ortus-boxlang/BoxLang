
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

}
