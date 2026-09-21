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

import java.util.List;
import java.util.Map;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.types.util.StringUtil;

/**
 * Encrypts and decrypts configuration secrets using the runtime seed.
 */
public final class ConfigSecretUtil {

	/**
	 * The prefix that indicates a value is encrypted with the runtime seed.
	 */
	public static final String	SECURE_VALUE_PREFIX		= "bxsecret:";

	/**
	 * The string encoding used for {@value #SECURE_VALUE_PREFIX} values.
	 */
	private static final String	SECURE_VALUE_ENCODING	= "Base64";

	private ConfigSecretUtil() {
	}

	/**
	 * Gets the Base64-encoded runtime seed from the runtime configuration.
	 *
	 * @return The Base64-encoded AES key.
	 */
	public static String getRuntimeSeed() {
		return BoxRuntime.getInstance().getConfiguration().security.getSecretSeed();
	}

	/**
	 * Encrypts a value using the runtime seed.
	 *
	 * @param value The plaintext value.
	 *
	 * @return The encrypted value using the configured algorithm and Base64 encoding.
	 */
	public static String encrypt( String value ) {
		BoxRuntime runtime = BoxRuntime.getInstance();
		return encrypt( value, getRuntimeSeed(), runtime.getConfiguration().security.secretAlgorithm );
	}

	/**
	 * Encrypts a value using the runtime seed and prefixes it for storage in a mixed plaintext/encrypted setting.
	 *
	 * @param value The plaintext value.
	 *
	 * @return The encrypted value prefixed with {@value #SECURE_VALUE_PREFIX}.
	 */
	public static String encryptWithPrefix( String value ) {
		return SECURE_VALUE_PREFIX + encrypt( value );
	}

	/**
	 * Encrypts a value using a runtime seed that was retrieved previously.
	 *
	 * @param value The plaintext value.
	 * @param seed  The Base64-encoded runtime AES key.
	 *
	 * @return The encrypted value using the default AES and UU encoding settings.
	 */
	public static String encrypt( String value, String seed ) {
		return encrypt( value, seed, EncryptionUtil.DEFAULT_ENCRYPTION_ALGORITHM );
	}

	/**
	 * Encrypts a value using a runtime seed and encryption algorithm.
	 *
	 * @param value     The plaintext value.
	 * @param seed      The Base64-encoded runtime AES key.
	 * @param algorithm The encryption algorithm.
	 *
	 * @return The encrypted value using UU encoding.
	 */
	public static String encrypt( String value, String seed, String algorithm ) {
		return EncryptionUtil.encrypt( value, algorithm, seed, SECURE_VALUE_ENCODING, null, null );
	}

	/**
	 * Decrypts a value using the runtime seed.
	 *
	 * @param value The encrypted value.
	 *
	 * @return The plaintext value.
	 */
	public static String decrypt( String value ) {
		BoxRuntime runtime = BoxRuntime.getInstance();
		return decrypt( value, getRuntimeSeed(), runtime.getConfiguration().security.secretAlgorithm );
	}

	/**
	 * Determines whether a value uses the runtime-encryption prefix.
	 *
	 * @param value The value to inspect.
	 *
	 * @return True when the value starts with {@value #SECURE_VALUE_PREFIX}, ignoring case.
	 */
	public static boolean isEncrypted( String value ) {
		return value != null && StringUtil.startsWithIgnoreCase( value, SECURE_VALUE_PREFIX );
	}

	/**
	 * Decrypts a prefixed value using the runtime seed, or returns a plaintext value unchanged.
	 *
	 * @param value The plaintext or prefixed encrypted value.
	 *
	 * @return The plaintext value.
	 */
	public static String decryptIfEncrypted( String value ) {
		return isEncrypted( value ) ? decrypt( value.substring( SECURE_VALUE_PREFIX.length() ) ) : value;
	}

	/**
	 * Recursively decrypts all {@code bxsecret:} values in a structured value using the active runtime configuration.
	 * Supported containers are BoxLang structs and arrays, Java maps, and Java lists. Containers are updated in place.
	 *
	 * @param value The value or container tree to decrypt.
	 *
	 * @return The value tree with encrypted strings replaced by plaintext values.
	 */
	@SuppressWarnings( "unchecked" )
	public static Object decryptValues( Object value ) {
		if ( value instanceof Map<?, ?> rawMap ) {
			Map<Object, Object> map = ( Map<Object, Object> ) rawMap;
			for ( Object key : List.copyOf( map.keySet() ) ) {
				map.put( key, decryptValues( map.get( key ) ) );
			}
		} else if ( value instanceof List<?> rawList ) {
			List<Object> list = ( List<Object> ) rawList;
			for ( int i = 0; i < list.size(); i++ ) {
				list.set( i, decryptValues( list.get( i ) ) );
			}
		} else if ( value instanceof String stringValue ) {
			return decryptIfEncrypted( stringValue );
		}

		return value;
	}

	/**
	 * Decrypts a prefixed value using the supplied seed and algorithm, or returns a plaintext value unchanged.
	 *
	 * @param value     The plaintext or prefixed encrypted value.
	 * @param seed      The Base64-encoded runtime seed.
	 * @param algorithm The encryption algorithm.
	 *
	 * @return The plaintext value.
	 */
	public static String decryptIfEncrypted( String value, String seed, String algorithm ) {
		return isEncrypted( value ) ? decrypt( value.substring( SECURE_VALUE_PREFIX.length() ), seed, algorithm ) : value;
	}

	/**
	 * Decrypts a value using a runtime seed that was retrieved previously.
	 *
	 * @param value The encrypted value.
	 * @param seed  The Base64-encoded runtime AES key.
	 *
	 * @return The plaintext value.
	 */
	public static String decrypt( String value, String seed ) {
		return decrypt( value, seed, EncryptionUtil.DEFAULT_ENCRYPTION_ALGORITHM );
	}

	/**
	 * Decrypts a value using a runtime seed and encryption algorithm.
	 *
	 * @param value     The encrypted value.
	 * @param seed      The Base64-encoded runtime AES key.
	 * @param algorithm The encryption algorithm.
	 *
	 * @return The plaintext value.
	 */
	public static String decrypt( String value, String seed, String algorithm ) {
		return ( String ) EncryptionUtil.decrypt( value, algorithm, seed,
		    SECURE_VALUE_ENCODING, null, null );
	}

	/**
	 * Decrypts a historical bare value that was stored using BoxLang's former default UU encoding.
	 * LEGACY COMPATIBILITY: Only persisted scheduler credentials created before {@value #SECURE_VALUE_PREFIX} use this format.
	 *
	 * @param value The legacy UU-encoded encrypted value.
	 *
	 * @return The decrypted plaintext value.
	 */
	public static String decryptLegacy( String value ) {
		BoxRuntime runtime = BoxRuntime.getInstance();
		return ( String ) EncryptionUtil.decrypt( value, runtime.getConfiguration().security.secretAlgorithm, getRuntimeSeed(),
		    EncryptionUtil.DEFAULT_ENCRYPTION_ENCODING, null, null );
	}

}