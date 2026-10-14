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

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

public class GenerateKeyPairTest {

	static BoxRuntime	instance;
	IBoxContext			context;
	IScope				variables;
	static Key			result	= new Key( "result" );

	@BeforeAll
	public static void setUp() {
		instance = BoxRuntime.getInstance( true );
	}

	@BeforeEach
	public void setupEach() {
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
	}

	private IStruct generate( String args ) {
		instance.executeSource( "result = generateKeyPair( " + args + " );", context );
		return variables.getAsStruct( result );
	}

	@DisplayName( "Ed25519 round trip with plain JDK code proves the formats" )
	@Test
	public void testEd25519RoundTrip() throws Exception {
		IStruct		kp			= generate( "'Ed25519'" );
		KeyFactory	factory		= KeyFactory.getInstance( "Ed25519" );
		PrivateKey	privateKey	= factory.generatePrivate( new PKCS8EncodedKeySpec( Base64.getDecoder().decode( kp.getAsString( Key.privateKey ) ) ) );
		PublicKey	publicKey	= factory.generatePublic( new X509EncodedKeySpec( Base64.getDecoder().decode( kp.getAsString( Key.publicKey ) ) ) );

		Signature	signer		= Signature.getInstance( "Ed25519" );
		signer.initSign( privateKey );
		signer.update( "hello".getBytes() );
		byte[]		signature	= signer.sign();

		Signature	verifier	= Signature.getInstance( "Ed25519" );
		verifier.initVerify( publicKey );
		verifier.update( "hello".getBytes() );
		assertThat( verifier.verify( signature ) ).isTrue();
		assertThat( kp.getAsString( Key.algorithm ) ).isEqualTo( "Ed25519" );
	}

	@DisplayName( "PEM fields wrap the same DER bytes" )
	@Test
	public void testPem() {
		IStruct kp = generate( "'ed25519'" );
		assertThat( kp.getAsString( Key.privatePem ) ).startsWith( "-----BEGIN PRIVATE KEY-----\n" );
		assertThat( kp.getAsString( Key.privatePem ) ).endsWith( "-----END PRIVATE KEY-----\n" );
		assertThat( kp.getAsString( Key.publicPem ) ).startsWith( "-----BEGIN PUBLIC KEY-----\n" );
		String body = kp.getAsString( Key.publicPem ).replace( "-----BEGIN PUBLIC KEY-----", "" ).replace( "-----END PUBLIC KEY-----", "" )
		    .replaceAll( "\\s", "" );
		assertThat( body ).isEqualTo( kp.getAsString( Key.publicKey ) );
	}

	@DisplayName( "Each supported algorithm generates a parseable pair" )
	@ParameterizedTest
	@ValueSource( strings = { "Ed25519", "Ed448", "X25519", "X448", "RSA", "EC" } )
	public void testEachAlgorithm( String algorithm ) throws Exception {
		assertParseable( generate( "algorithm=\"" + algorithm + "\"" ), algorithm );
	}

	@DisplayName( "RSA keySize and EC curve options work" )
	@Test
	public void testOptions() throws Exception {
		assertParseable( generate( "algorithm=\"RSA\", keySize=3072" ), "RSA" );
		assertParseable( generate( "algorithm=\"EC\", curve=\"secp384r1\"" ), "EC" );
	}

	private void assertParseable( IStruct kp, String algorithm ) throws Exception {
		assertThat( kp.getAsString( Key.algorithm ) ).isEqualTo( algorithm );
		KeyFactory factory = KeyFactory.getInstance( algorithm );
		assertThat( factory.generatePrivate( new PKCS8EncodedKeySpec( Base64.getDecoder().decode( kp.getAsString( Key.privateKey ) ) ) ) ).isNotNull();
		assertThat( factory.generatePublic( new X509EncodedKeySpec( Base64.getDecoder().decode( kp.getAsString( Key.publicKey ) ) ) ) ).isNotNull();
	}

	@DisplayName( "Algorithm and curve lookups are case insensitive" )
	@Test
	public void testCaseInsensitive() throws Exception {
		assertParseable( generate( "algorithm=\"ed25519\"" ), "Ed25519" );
		assertParseable( generate( "algorithm=\"  rsa \"" ), "RSA" );
		assertParseable( generate( "algorithm=\"ec\", curve=\"SECP256R1\"" ), "EC" );
	}

	@DisplayName( "Repeated and concurrent calls with the cached generator return distinct valid keys" )
	@Test
	public void testCachedGeneratorConcurrent() throws Exception {
		java.util.Set<String>	keys	= java.util.concurrent.ConcurrentHashMap.newKeySet();
		java.util.List<Thread>	threads	= new java.util.ArrayList<>();
		for ( int i = 0; i < 8; i++ ) {
			Thread t = new Thread( () -> {
				for ( int j = 0; j < 5; j++ ) {
					keys.add( ortus.boxlang.runtime.util.EncryptionUtil.generateKeyPair( "Ed25519", null, null ).getAsString( Key.privateKey ) );
				}
			} );
			threads.add( t );
			t.start();
		}
		for ( Thread t : threads ) {
			t.join();
		}
		assertThat( keys ).hasSize( 40 );
	}

	@DisplayName( "Two calls never return the same key" )
	@Test
	public void testUnique() {
		assertThat( generate( "'Ed25519'" ).getAsString( Key.privateKey ) ).isNotEqualTo( generate( "'Ed25519'" ).getAsString( Key.privateKey ) );
	}

	@DisplayName( "Bad algorithm throws and lists valid ones" )
	@Test
	public void testBadAlgorithm() {
		BoxRuntimeException e = assertThrows( BoxRuntimeException.class, () -> generate( "'nope'" ) );
		assertThat( e.getMessage() ).contains( "Ed25519" );
	}

	@DisplayName( "Bad RSA size, bad curve, and misplaced arguments throw" )
	@Test
	public void testBadSizeAndCurve() {
		assertThrows( BoxRuntimeException.class, () -> generate( "'RSA', 1024" ) );
		assertThrows( BoxRuntimeException.class, () -> generate( "algorithm='EC', curve='secp1'" ) );
		assertThrows( BoxRuntimeException.class, () -> generate( "algorithm='Ed25519', keySize=2048" ) );
		assertThrows( BoxRuntimeException.class, () -> generate( "algorithm='RSA', curve='secp256r1'" ) );
	}

}
