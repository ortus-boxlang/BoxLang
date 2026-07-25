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

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.spec.KeySpec;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * Encrypts and decrypts BoxLang source at rest so it can be distributed in a non-readable form and
 * decrypted in memory, just before parsing. Encryption is AES-256-GCM (authenticated), with the key
 * derived from a caller-supplied secret via PBKDF2.
 *
 * <h2>File format</h2>
 *
 * <pre>
 * [4 bytes  magic 0xB0C5E1E1]      // BoxLang-encrypted marker (distinct from 0xCAFEBABE bytecode)
 * [1 byte   format version = 1]
 * [1 byte   keyId length K]        // unsigned, 0-255
 * [K bytes  keyId UTF-8]           // a label identifying WHICH key is needed (never the key itself)
 * [16 bytes salt]                  // PBKDF2 salt
 * [12 bytes IV/nonce]              // AES-GCM nonce
 * [N bytes  ciphertext + GCM tag]
 * </pre>
 *
 * <h2>Key resolution</h2>
 * At decrypt time the runtime reads the embedded keyId and resolves the actual key from the host, in
 * order: the environment variable {@code BOXLANG_CODE_KEY_<KEYID>} (keyId uppercased, non-alphanumerics
 * replaced with {@code _}), then {@code boxlang.json → security.codeKeys.<keyId>}. This lets a vendor
 * lock each module/artifact with its own key and hand each customer only the keys they bought.
 *
 * <h2>Enforcement (anti-webshell)</h2>
 * When {@code security.enforceEncryptedSource} is enabled, the runtime refuses to parse/execute any
 * file-based source that is not encrypted. In that lockdown posture a plaintext webshell dropped on the
 * server cannot run — only encrypted source is allowed. See {@link #isEnforceEncryptedSource()}.
 *
 * <h2>Compiled-class cache</h2>
 * Compiled bytecode for encrypted sources is never written to the on-disk class cache (it stays in memory
 * only and is recompiled from the encrypted source on each JVM start), so no decrypted artifact is
 * persisted. See the disk-write gate in {@code ASMBoxpiler}.
 *
 * <h2>Security note</h2>
 * Because the runtime must hold the key to decrypt, the key lives on the host. This reliably stops
 * casual reading/copying and locks out anyone without the key, but a determined party who holds the key
 * (or patches the runtime at the decrypt seam) can recover the plaintext. It is strong deterrence, not
 * unbreakable protection.
 */
public final class CodeEncryption {

	/**
	 * Magic number identifying a BoxLang-encrypted source file.
	 */
	public static final int				MAGIC				= 0xB0C5E1E1;

	/**
	 * Current on-disk format version.
	 */
	private static final byte			FORMAT_VERSION		= 1;

	/**
	 * PBKDF2 algorithm used to derive the AES key from the secret.
	 */
	private static final String			PBKDF2_ALGORITHM	= "PBKDF2WithHmacSHA256";

	/**
	 * AES transformation (authenticated GCM mode, no padding).
	 */
	private static final String			AES_TRANSFORMATION	= "AES/GCM/NoPadding";

	/**
	 * PBKDF2 iteration count.
	 */
	private static final int			ITERATIONS			= 210_000;

	/**
	 * Derived AES key size in bits.
	 */
	private static final int			KEY_SIZE_BITS		= 256;

	/**
	 * Salt length in bytes.
	 */
	private static final int			SALT_LEN			= 16;

	/**
	 * GCM nonce (IV) length in bytes.
	 */
	private static final int			IV_LEN				= 12;

	/**
	 * GCM authentication tag length in bits.
	 */
	private static final int			GCM_TAG_BITS		= 128;

	/**
	 * Environment variable prefix used for per-keyId key resolution.
	 */
	private static final String			ENV_PREFIX			= "BOXLANG_CODE_KEY_";

	/**
	 * Shared secure random.
	 */
	private static final SecureRandom	RANDOM				= new SecureRandom();

	/**
	 * Prevents instantiation of this utility class.
	 */
	private CodeEncryption() {
		// Prevent instantiation
	}

	/**
	 * Returns true if the given bytes carry the BoxLang-encrypted magic header.
	 *
	 * @param bytes the file bytes to inspect
	 *
	 * @return true when the bytes are a BoxLang-encrypted payload
	 */
	public static boolean isEncrypted( byte[] bytes ) {
		if ( bytes == null || bytes.length < 4 ) {
			return false;
		}
		int magic = ( ( bytes[ 0 ] & 0xFF ) << 24 )
		    | ( ( bytes[ 1 ] & 0xFF ) << 16 )
		    | ( ( bytes[ 2 ] & 0xFF ) << 8 )
		    | ( bytes[ 3 ] & 0xFF );
		return magic == MAGIC;
	}

	/**
	 * Returns true if the given file begins with the BoxLang-encrypted magic header. Reads only the
	 * first few bytes, so it is cheap to call on the compile/cache hot path.
	 *
	 * @param file the file to inspect (may be null or non-existent)
	 *
	 * @return true when the file is a BoxLang-encrypted payload
	 */
	public static boolean isEncryptedFile( java.io.File file ) {
		if ( file == null || !file.isFile() || !file.canRead() ) {
			return false;
		}
		try ( java.io.FileInputStream fis = new java.io.FileInputStream( file );
		    java.io.DataInputStream dis = new java.io.DataInputStream( fis ) ) {
			if ( dis.available() < 4 ) {
				return false;
			}
			return dis.readInt() == MAGIC;
		} catch ( java.io.IOException e ) {
			return false;
		}
	}

	/**
	 * Reads the keyId label embedded in an encrypted payload's header.
	 *
	 * @param bytes the encrypted payload
	 *
	 * @return the keyId label
	 *
	 * @throws BoxRuntimeException if the payload is not a valid encrypted file
	 */
	public static String readKeyId( byte[] bytes ) {
		if ( !isEncrypted( bytes ) ) {
			throw new BoxRuntimeException( "Not a BoxLang-encrypted payload." );
		}
		try {
			ByteBuffer buffer = ByteBuffer.wrap( bytes );
			buffer.getInt();		// magic
			buffer.get();			// version
			int		keyIdLen	= buffer.get() & 0xFF;
			byte[]	keyIdBytes	= new byte[ keyIdLen ];
			buffer.get( keyIdBytes );
			return new String( keyIdBytes, StandardCharsets.UTF_8 );
		} catch ( Exception e ) {
			throw new BoxRuntimeException( "Malformed BoxLang-encrypted header.", e );
		}
	}

	/**
	 * Encrypts the given plaintext with AES-256-GCM, stamping the given keyId into the header.
	 *
	 * @param plaintext the bytes to encrypt (typically UTF-8 source)
	 * @param keyId     a label identifying which key is required to decrypt (max 255 UTF-8 bytes)
	 * @param secret    the secret (passphrase or key material) used to derive the AES key
	 *
	 * @return the encrypted payload (header + ciphertext)
	 */
	public static byte[] encrypt( byte[] plaintext, String keyId, String secret ) {
		byte[] keyIdBytes = keyId.getBytes( StandardCharsets.UTF_8 );
		if ( keyIdBytes.length > 255 ) {
			throw new BoxRuntimeException( "keyId is too long (max 255 UTF-8 bytes): " + keyId );
		}
		try {
			byte[]	salt	= new byte[ SALT_LEN ];
			byte[]	iv		= new byte[ IV_LEN ];
			RANDOM.nextBytes( salt );
			RANDOM.nextBytes( iv );

			Cipher cipher = Cipher.getInstance( AES_TRANSFORMATION );
			cipher.init( Cipher.ENCRYPT_MODE, deriveKey( secret, salt ), new GCMParameterSpec( GCM_TAG_BITS, iv ) );
			byte[]		ciphertext	= cipher.doFinal( plaintext );

			ByteBuffer	buffer		= ByteBuffer.allocate( 4 + 1 + 1 + keyIdBytes.length + SALT_LEN + IV_LEN + ciphertext.length );
			buffer.putInt( MAGIC );
			buffer.put( FORMAT_VERSION );
			buffer.put( ( byte ) keyIdBytes.length );
			buffer.put( keyIdBytes );
			buffer.put( salt );
			buffer.put( iv );
			buffer.put( ciphertext );
			return buffer.array();
		} catch ( Exception e ) {
			throw new BoxRuntimeException( "Failed to encrypt source: " + e.getMessage(), e );
		}
	}

	/**
	 * Decrypts a BoxLang-encrypted payload with the given secret.
	 *
	 * @param bytes  the encrypted payload
	 * @param secret the secret used to derive the AES key (must match what was used to encrypt)
	 *
	 * @return the decrypted plaintext bytes
	 *
	 * @throws BoxRuntimeException if the payload is malformed or the secret is wrong (authentication fails)
	 */
	public static byte[] decrypt( byte[] bytes, String secret ) {
		if ( !isEncrypted( bytes ) ) {
			throw new BoxRuntimeException( "Not a BoxLang-encrypted payload." );
		}
		try {
			ByteBuffer buffer = ByteBuffer.wrap( bytes );
			buffer.getInt();		// magic
			buffer.get();			// version
			int keyIdLen = buffer.get() & 0xFF;
			buffer.position( buffer.position() + keyIdLen );	// skip keyId
			byte[]	salt	= new byte[ SALT_LEN ];
			byte[]	iv		= new byte[ IV_LEN ];
			buffer.get( salt );
			buffer.get( iv );
			byte[] ciphertext = new byte[ buffer.remaining() ];
			buffer.get( ciphertext );

			Cipher cipher = Cipher.getInstance( AES_TRANSFORMATION );
			cipher.init( Cipher.DECRYPT_MODE, deriveKey( secret, salt ), new GCMParameterSpec( GCM_TAG_BITS, iv ) );
			return cipher.doFinal( ciphertext );
		} catch ( javax.crypto.AEADBadTagException e ) {
			throw new BoxRuntimeException( "Failed to decrypt encrypted source: wrong key or corrupted/tampered file.", e );
		} catch ( Exception e ) {
			throw new BoxRuntimeException( "Failed to decrypt encrypted source: " + e.getMessage(), e );
		}
	}

	/**
	 * If the given bytes are a BoxLang-encrypted payload, decrypts them by resolving the key referenced
	 * by the embedded keyId from the host (env var or {@code security.codeKeys}). Otherwise returns the
	 * bytes unchanged, so plain (non-encrypted) source flows through untouched.
	 *
	 * <p>
	 * When {@code security.enforceEncryptedSource} is enabled, non-encrypted bytes are rejected instead of
	 * passed through — a lockdown mode that stops plaintext webshells from running (only encrypted source
	 * is allowed to execute).
	 *
	 * @param bytes the file bytes
	 *
	 * @return decrypted plaintext bytes, or the original bytes if not encrypted
	 *
	 * @throws BoxRuntimeException if encrypted but no key is available for the referenced keyId, or if
	 *                             enforcement is on and the bytes are not encrypted
	 */
	public static byte[] maybeDecrypt( byte[] bytes ) {
		if ( !isEncrypted( bytes ) ) {
			if ( isEnforceEncryptedSource() ) {
				throw new BoxRuntimeException(
				    "Execution blocked: this runtime is configured to only run encrypted source "
				        + "(security.enforceEncryptedSource=true), but the source is not encrypted." );
			}
			return bytes;
		}
		String	keyId	= readKeyId( bytes );
		String	secret	= resolveKey( keyId );
		if ( secret == null || secret.isEmpty() ) {
			throw new BoxRuntimeException(
			    "This source is encrypted and requires key '" + keyId + "', but no key was found. "
			        + "Set environment variable '" + ENV_PREFIX + normalizeEnv( keyId )
			        + "' or 'security.codeKeys." + keyId + "' in boxlang.json." );
		}
		return decrypt( bytes, secret );
	}

	/**
	 * Resolves the secret for a given keyId from the host: environment variable first, then the
	 * {@code security.codeKeys} map in the runtime configuration.
	 *
	 * @param keyId the key label to resolve
	 *
	 * @return the secret, or {@code null} if none is configured
	 */
	public static String resolveKey( String keyId ) {
		// 1) Environment variable BOXLANG_CODE_KEY_<KEYID>
		String envValue = System.getenv( ENV_PREFIX + normalizeEnv( keyId ) );
		if ( envValue != null && !envValue.isEmpty() ) {
			return envValue;
		}

		// 2) boxlang.json -> security.codeKeys.<keyId>
		try {
			IStruct codeKeys = BoxRuntime.getInstance().getConfiguration().security.codeKeys;
			if ( codeKeys != null ) {
				Object value = codeKeys.get( Key.of( keyId ) );
				if ( value != null ) {
					String stringValue = StringCaster.cast( value );
					if ( !stringValue.isEmpty() ) {
						return stringValue;
					}
				}
			}
		} catch ( Exception e ) {
			// Runtime or config not available; fall through to null
		}

		return null;
	}

	/**
	 * Returns true when the runtime is configured to only run encrypted source
	 * ({@code security.enforceEncryptedSource}). Fails safe to {@code false} when the runtime or its
	 * configuration is not available (so tooling that has no runtime is never accidentally blocked).
	 *
	 * @return true when non-encrypted source should be blocked
	 */
	public static boolean isEnforceEncryptedSource() {
		try {
			return BoxRuntime.getInstance().getConfiguration().security.enforceEncryptedSource;
		} catch ( Exception e ) {
			return false;
		}
	}

	/**
	 * Normalizes a keyId into an environment-variable-safe suffix: uppercased, with every character
	 * that is not A-Z or 0-9 replaced by an underscore.
	 *
	 * @param keyId the key label
	 *
	 * @return the normalized suffix
	 */
	public static String normalizeEnv( String keyId ) {
		return keyId.toUpperCase().replaceAll( "[^A-Z0-9]", "_" );
	}

	/**
	 * Derives a 256-bit AES key from the secret and salt using PBKDF2-HMAC-SHA256.
	 *
	 * @param secret the secret material
	 * @param salt   the PBKDF2 salt
	 *
	 * @return the derived AES key spec
	 *
	 * @throws Exception if key derivation fails
	 */
	private static SecretKeySpec deriveKey( String secret, byte[] salt ) throws Exception {
		SecretKeyFactory	factory		= SecretKeyFactory.getInstance( PBKDF2_ALGORITHM );
		KeySpec				spec		= new PBEKeySpec( secret.toCharArray(), salt, ITERATIONS, KEY_SIZE_BITS );
		byte[]				keyBytes	= factory.generateSecret( spec ).getEncoded();
		return new SecretKeySpec( keyBytes, "AES" );
	}
}
