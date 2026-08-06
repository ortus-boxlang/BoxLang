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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.config.Configuration;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;

class ConfigSecretUtilTest {

	@BeforeAll
	static void setUp() {
		BoxRuntime.getInstance( true );
	}

	@DisplayName( "It encrypts and decrypts values with the runtime seed" )
	@Test
	void testRuntimeSeedEncryptionRoundTrip() {
		assertThat( BoxRuntime.getInstance().getConfiguration().security.getSecretSeed() ).isEqualTo( ConfigSecretUtil.getRuntimeSeed() );

		String encrypted = ConfigSecretUtil.encrypt( "runtime-secret" );

		assertThat( encrypted ).isNotEqualTo( "runtime-secret" );
		assertThat( encrypted ).matches( "[A-Za-z0-9+/]+={0,2}" );
		assertThat( ConfigSecretUtil.decrypt( encrypted ) ).isEqualTo( "runtime-secret" );
	}

	@DisplayName( "It uses a case-insensitive bxsecret prefix for mixed plaintext and encrypted values" )
	@Test
	void testPrefixedEncryption() {
		String encrypted = ConfigSecretUtil.encryptWithPrefix( "runtime-secret" );

		assertThat( ConfigSecretUtil.isEncrypted( encrypted ) ).isTrue();
		assertThat( ConfigSecretUtil.decryptIfEncrypted( encrypted ) ).isEqualTo( "runtime-secret" );
		assertThat( ConfigSecretUtil.decryptIfEncrypted( "BxSeCrEt:" + encrypted.substring( ConfigSecretUtil.SECURE_VALUE_PREFIX.length() ) ) )
		    .isEqualTo( "runtime-secret" );
		assertThat( ConfigSecretUtil.decryptIfEncrypted( "plaintext-secret" ) ).isEqualTo( "plaintext-secret" );
	}

	/**
	 * Verifies nested BoxLang configuration containers decrypt their prefixed values in place.
	 */
	@DisplayName( "It recursively decrypts structured values" )
	@Test
	void testDecryptValues() {
		IStruct values = Struct.of(
		    "secret", ConfigSecretUtil.encryptWithPrefix( "top-level" ),
		    "nested", Struct.of( "secret", ConfigSecretUtil.encryptWithPrefix( "nested" ) ),
		    "array", Array.of( ConfigSecretUtil.encryptWithPrefix( "array" ) )
		);

		ConfigSecretUtil.decryptValues( values );

		assertThat( values.getAsString( Key.of( "secret" ) ) ).isEqualTo( "top-level" );
		assertThat( values.getAsStruct( Key.of( "nested" ) ).getAsString( Key.of( "secret" ) ) ).isEqualTo( "nested" );
		assertThat( values.getAsArray( Key.of( "array" ) ).get( 0 ) ).isEqualTo( "array" );
	}

	/**
	 * Verifies the documented BoxLang example imports {@link ConfigSecretUtil} and encrypts a value with the runtime seed.
	 *
	 * @throws Throwable If the BoxLang source cannot be executed.
	 */
	@DisplayName( "It encrypts a secret from BoxLang without passing the runtime" )
	@Test
	void testBoxLangEncryptionExample() throws Throwable {
		IBoxContext context = new ScriptingRequestBoxContext( BoxRuntime.getInstance().getRuntimeContext() );

		// @formatter:off
		BoxRuntime.getInstance().executeSource( """
			import ortus.boxlang.runtime.util.ConfigSecretUtil;

			secret = ConfigSecretUtil.encryptWithPrefix( "my-sensitive-value" );
			""", context );
		// @formatter:on

		String secret = context.getScopeNearby( VariablesScope.name ).getAsString( Key.of( "secret" ) );
		assertThat( ConfigSecretUtil.isEncrypted( secret ) ).isTrue();
		assertThat( ConfigSecretUtil.decryptIfEncrypted( secret ) ).isEqualTo( "my-sensitive-value" );
	}

	@DisplayName( "It uses the configured encryption algorithm" )
	@Test
	void testConfiguredEncryptionAlgorithm() {
		Configuration	configuration	= new Configuration().process( Struct.of(
		    "security", Struct.of( "secretAlgorithm", "AES/ECB/PKCS5Padding" )
		) );
		String			seed			= ConfigSecretUtil.getRuntimeSeed();
		String			encrypted		= ConfigSecretUtil.encrypt( "runtime-secret", seed, configuration.security.secretAlgorithm );

		assertThat( ConfigSecretUtil.decrypt( encrypted, seed, configuration.security.secretAlgorithm ) ).isEqualTo( "runtime-secret" );
	}

}