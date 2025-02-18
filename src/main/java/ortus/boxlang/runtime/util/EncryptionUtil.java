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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Random;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;

import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxIOException;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * A utility class for encryption and encoding
 */
public final class EncryptionUtil {

	/**
	 * The default secure random number instance
	 */
	private final static SecureRandom	secureRandom					= new SecureRandom();

	/**
	 * The default algorithm to use
	 */
	public static final String			DEFAULT_HASH_ALGORITHM			= "MD5";

	/**
	 * Default encryption algorithm
	 */
	public static final String			DEFAULT_ENCRYPTION_ALGORITHM	= "AES";

	/**
	 * Default encryption algorithm
	 */
	public static final String			DEFAULT_ENCRYPTION_ENCODING		= "UU";

	/**
	 * Default key size
	 */
	public static final int				DEFAULT_ENCRYPTION_KEY_SIZE		= 256;

	/**
	 * The default encoding to use
	 */
	public static final String			DEFAULT_CHARSET					= "UTF-8";

	/**
	 * Default iterations to perform during encryption - the minimum recomended by NIST
	 */
	public final static int				DEFAULT_ENCRYPTION_ITERATIONS	= 1000;


	/**
	 * The IV size required by FBMA algorithms
	 */
	public static final int FBMA_IV_SIZE = 16;

	/**
	 * Base64 validation methods
	 */
	private static final String			BASE_64_REGEX_PATTERN			= "^([A-Za-z0-9+/]{4})*([A-Za-z0-9+/]{3}=|[A-Za-z0-9+/]{2}==)?$";
	private static final Pattern		BASE_64_PATTERN					= Pattern.compile( BASE_64_REGEX_PATTERN );

	/**
	 * Threadsafe instances of Random and Secure random instances which are used by the getRandom method
	 */
	private static HashMap<Key, Random>	randomStore						= new HashMap<Key, Random>();

	/**
	 * Quick 64 bit hash properties
	 */
	private static final long[]			byteTable						= generateHashLookupTable();
	private static final long			HSTART							= 0xBB40E64DA205B064L;
	private static final long			HMULT							= 7664345821815920749L;

	/**
	 * Supported key algorithms
	 * <a href="https://docs.oracle.com/en/java/javase/17/docs/specs/security/standard-names.html#keyfactory-algorithms">key factory algorithms</a>
	 */
	public static final IStruct			KEY_ALGORITHMS					= Struct.of(
	    Key.of( "AES" ), "AES",
	    Key.of( "ARCFOUR" ), "ARCFOUR",
	    Key.of( "Blowfish" ), "Blowfish",
	    Key.of( "ChaCha20" ), "ChaCha20",
	    Key.of( "DES" ), "DES",
	    Key.of( "DESede" ), "DESede",
	    Key.of( "HmacMD5" ), "HmacMD5",
	    Key.of( "HmacSHA1" ), "HmacSHA1",
	    Key.of( "HmacSHA224" ), "HmacSHA224",
	    Key.of( "HmacSHA256" ), "HmacSHA256",
	    Key.of( "HmacSHA384" ), "HmacSHA384",
	    Key.of( "HmacSHA512" ), "HmacSHA512",
	    Key.of( "HmacSHA3-224" ), "HmacSHA3-224",
	    Key.of( "HmacSHA3-256" ), "HmacSHA3-256",
	    Key.of( "HmacSHA3-384" ), "HmacSHA3-384",
	    Key.of( "HmacSHA3-512" ), "HmacSHA3-512"
	);

	/**
	 * URL Encoding properties
	 */
	public static final String			URL_SPACE						= "%20";
	public static final String			URL_PLUS_REGEX					= "\\+";

	/**
	 * Performs a hash of an object using the default algorithm
	 *
	 * @param object The object to be hashed
	 *
	 * @return returns the hashed string
	 */
	public static String hash( Object object ) {
		return hash( object, DEFAULT_HASH_ALGORITHM );
	}

	/**
	 * Performs a hash of an object using a supported algorithm
	 *
	 * @param object    The object to be hashed
	 * @param algorithm The supported {@link java.security.MessageDigest } algorithm (case-insensitive)
	 *
	 * @return returns the hashed string
	 */
	public static String hash( Object object, String algorithm ) {
		if ( object instanceof byte[] ) {
			return hash( ( byte[] ) object, algorithm, 1 );
		} else {
			return hash( object.toString().getBytes(), algorithm, 1 );
		}
	}

	/**
	 * Iterative hash of a byte array
	 *
	 * @param byteArray
	 * @param algorithm
	 * @param iterations
	 *
	 * @return
	 */
	public static String hash( byte[] byteArray, String algorithm, int iterations ) {
		String result = null;
		try {
			MessageDigest md = MessageDigest.getInstance( algorithm.toUpperCase() );
			for ( int i = 0; i < iterations; i++ ) {
				try {
					md.reset();
					MessageDigest mdc = ( MessageDigest ) md.clone();
					mdc.update( byteArray );
					byte[] digest = mdc.digest();
					byteArray	= digest;
					result		= digestToString( digest );
				} catch ( CloneNotSupportedException e ) {
					throw new BoxRuntimeException(
					    String.format(
					        "The clone operation is not supported using the algorithm [%s].",
					        algorithm.toUpperCase()
					    )
					);
				}
			}
		} catch ( NoSuchAlgorithmException e ) {
			throw new BoxRuntimeException(
			    String.format(
			        "The algorithm [%s] provided is not a valid digest algorithm.",
			        algorithm.toUpperCase()
			    )
			);
		}
		return result;
	}

	/**
	 * Stringifies a digest
	 *
	 * @param digest The digest
	 *
	 * @return the strigified result
	 */
	public static String digestToString( byte[] digest ) {
		StringBuilder result = new StringBuilder();
		IntStream
		    .range( 0, digest.length )
		    .forEach( idx -> result.append( String.format( "%02x", digest[ idx ] ) ) );
		return result.toString();
	}

	/**
	 * Peforms a checksum of a file path object using the MD5 algorithm
	 *
	 * @param filePath The {@link java.nio.file.Path} object
	 *
	 * @return returns the checksum string
	 */
	public static String checksum( Path filePath ) {
		return checksum( filePath, DEFAULT_HASH_ALGORITHM );
	}

	/**
	 * Peforms a checksum of a file path object using a supported algorithm
	 *
	 * @param filePath  The {@link java.nio.file.Path} object
	 * @param algorithm The supported {@link java.security.MessageDigest } algorithm (case-insensitive)
	 *
	 * @return returns the checksum string
	 */
	public static String checksum( Path filePath, String algorithm ) {
		try {
			return hash( Files.readAllBytes( filePath ) );
		} catch ( IOException e ) {
			throw new BoxIOException( e );
		}
	}

	/**
	 * HMAC encodes a byte array using the default encoding
	 *
	 * @param encryptItem The byte array to encode
	 * @param key         The key to use
	 * @param algorithm   The algorithm to use
	 * @param encoding    The encoding to use
	 *
	 * @return returns the HMAC encoded string
	 */
	public static String hmac( byte[] encryptItem, String key, String algorithm, String encoding ) {
		Charset charset = Charset.forName( encoding );
		// Attempt to keep the correct casing on the key
		algorithm = ( String ) KEY_ALGORITHMS.getOrDefault( Key.of( algorithm ), algorithm );
		try {
			Mac				mac			= Mac.getInstance( algorithm );
			SecretKeySpec	secretKey	= new SecretKeySpec( key.getBytes( charset ), algorithm );
			mac.init( secretKey );
			mac.reset();
			return digestToString( mac.doFinal( encryptItem ) );
		} catch ( NoSuchAlgorithmException e ) {
			throw new BoxRuntimeException(
			    String.format(
			        "The algorithm [%s] provided is not a valid digest algorithm.",
			        algorithm
			    ),
			    e
			);
		} catch ( InvalidKeyException e ) {
			throw new BoxRuntimeException(
			    String.format(
			        "The the key provided, [%s], is not a valid encryption key.",
			        key
			    ),
			    e
			);
		} catch ( IllegalStateException e ) {
			throw new BoxRuntimeException(
			    "An illegal state exception occurred",
			    e
			);
		}
	}

	/**
	 * HMAC encodes an object using the default encoding
	 *
	 * @param input     The object to encode
	 * @param key       The key to use
	 * @param algorithm The algorithm to use
	 * @param encoding  The encoding to use
	 *
	 * @return returns the HMAC encoded string
	 */
	public static String hmac( Object input, String key, String algorithm, String encoding ) {
		Charset	charset		= Charset.forName( encoding );
		byte[]	encryptItem	= null;

		if ( input instanceof String ) {
			encryptItem = StringCaster.cast( input ).getBytes( charset );
		} else {
			encryptItem = input.toString().getBytes( charset );
		}

		return hmac( encryptItem, key, algorithm, encoding );

	}

	/**
	 * Base64 encodes an object using the default encoding
	 *
	 * @param item    The object to encode
	 * @param charset The charset to use
	 *
	 * @return returns the base64 encoded string
	 */
	public static String base64Encode( Object item, Charset charset ) {
		byte[] encodeItem = null;
		if ( item instanceof byte[] byteArray ) {
			encodeItem = byteArray;
		} else if ( item instanceof String strItem ) {
			encodeItem = strItem.getBytes( charset );
		} else {
			encodeItem = item.toString().getBytes( charset );
		}
		return Base64.getEncoder().encodeToString( encodeItem );
	}

	/**
	 * URL encodes a string. We use the default encoding
	 *
	 * @param target The string to encode
	 *
	 * @return returns the URL encoded string
	 */
	public static String urlEncode( String target ) {
		return urlEncode( target, DEFAULT_CHARSET );
	}

	/**
	 * URL encodes a string with the specified encoding string
	 *
	 * @param target   The string to encode
	 * @param encoding The encoding to use
	 *
	 * @return returns the URL encoded string
	 */
	public static String urlEncode( String target, String encoding ) {
		return urlEncode( target, Charset.forName( encoding ) );
	}

	/**
	 * URL encodes a string with the specified encoding Charset - replacing URLEncoder's '+' with '%20'
	 *
	 * @param target   The string to encode
	 * @param encoding The encoding to use
	 *
	 * @return returns the URL encoded string
	 */
	public static String urlEncode( String target, Charset encoding ) {
		return URLEncoder.encode( target, encoding ).replaceAll( URL_PLUS_REGEX, URL_SPACE );
	}

	/**
	 * URL decodes a string
	 * We use the default encoding
	 *
	 * @param target The string to decode
	 *
	 * @return returns the URL decoded string
	 */
	public static String urlDecode( String target ) {
		return urlDecode( target, DEFAULT_CHARSET );
	}

	/**
	 * URL decodes a string
	 *
	 * @param target   The string to decode
	 * @param encoding The encoding to use
	 *
	 * @return returns the URL decoded string
	 */
	public static String urlDecode( String target, String encoding ) {
		try {
			return URLDecoder.decode( target, encoding );
		} catch ( UnsupportedEncodingException e ) {
			throw new BoxRuntimeException( e.getMessage() );
		}
	}

	/**
	 * Generates a secret key
	 *
	 * @param algorithm The algorithm to use: AES, ARCFOUR, Blowfish, ChaCha20, DES, DESede, HmacMD5, HmacSHA1, HmacSHA224, HmacSHA256, HmacSHA384, HmacSHA512, HmacSHA3-224, HmacSHA3-256, HmacSHA3-384, HmacSHA3-512
	 * @param keySize   The key size
	 *
	 * @return returns the secret key
	 */
	public static SecretKey generateKey( String algorithm, Integer keySize ) {
		algorithm = ( String ) KEY_ALGORITHMS.getOrDefault( Key.of( algorithm ), algorithm );
		try {
			// 1. Obtain an instance of KeyGenerator for the specified algorithm.
			KeyGenerator keyGenerator = KeyGenerator.getInstance( algorithm );
			// 2. Initialize the key generator with a specific key size if provided - otherwise will use the default key size of the algorithm
			if ( keySize != null ) {
				keyGenerator.init( keySize );
			}
			// 3. Generate a secret key.
			return keyGenerator.generateKey();
		} catch ( NoSuchAlgorithmException e ) {
			throw new BoxRuntimeException(
			    String.format(
			        "The algorithm [%s] provided is not a valid key algorithm or it has not been loaded properly.",
			        algorithm
			    ),
			    e
			);
		}
	}

	/**
	 * Generate a SecretKey for the given algorithm and an optional key size
	 *
	 * @param algorithm The encryption algorithm
	 * @param keySize   The key size
	 *
	 * @return
	 */
	public static SecretKey generateKey( String algorithm ) {
		return generateKey( algorithm, null );
	}

	/**
	 * Encode the SecretKey to a string
	 *
	 * @param key The SecretKey to encode
	 *
	 * @return
	 */
	public static String encodeKey( SecretKey key ) {
		return base64Encode( key.getEncoded(), Charset.forName( EncryptionUtil.DEFAULT_CHARSET ) );
	}

	/**
	 * Decodes a secret key from a string representation
	 *
	 * @param key       The string representation of the key
	 * @param algorithm The algorithm used to generate the key
	 *
	 * @return
	 */
	public static SecretKey decodeKey( String key, String algorithm ) {
		return new SecretKeySpec( decodeKeyBytes( key ), algorithm );
	}

	/**
	 * Decode the SecretKey from a string and return the byte array
	 *
	 * @param key
	 *
	 * @return
	 */
	public static byte[] decodeKeyBytes( String key ) {
		return Base64.getDecoder().decode( key );
	}

	/**
	 * Generates a secret key using the default algorithm and key size
	 *
	 * @param algorithm The algorithm to use: AES, ARCFOUR, Blowfish, ChaCha20, DES, DESede, HmacMD5, HmacSHA1, HmacSHA224, HmacSHA256, HmacSHA384, HmacSHA512, HmacSHA3-224, HmacSHA3-256, HmacSHA3-384, HmacSHA3-512
	 * @param keySize   The key size
	 *
	 * @return returns the secret key
	 */
	public static String generateKeyAsString( String algorithm, int keySize ) {
		return convertSecretKeyToString( generateKey( algorithm, keySize ) );
	}

	/**
	 * Generates a secret key using the default algorithm and key size
	 */
	public static SecretKey generateKey() {
		return generateKey( DEFAULT_ENCRYPTION_ALGORITHM, DEFAULT_ENCRYPTION_KEY_SIZE );
	}

	/**
	 * Generates a secret key using the default algorithm and key size
	 */
	public static String generateKeyAsString() {
		return convertSecretKeyToString( generateKey() );
	}

	/**
	 * Converts a secret key to a string
	 *
	 * @param secretKey The secret key
	 *
	 * @return returns the secret key as a string
	 */
	public static String convertSecretKeyToString( SecretKey secretKey ) {
		// Get the secret key bytes
		byte[] rawData = secretKey.getEncoded();
		// Encode the bytes to a Base64 string
		return Base64.getEncoder().encodeToString( rawData );
	}

	/**
	 * Processes the encryption or decryption of an object
	 *
	 * @param cipherMode       The cipher mode to use
	 * @param obj              The object to encrypt
	 * @param algorithm        The string representation of the algorithm ( e.g. AES, DES, etc. )
	 * @param key              The secret key to use for encryption
	 * @param encoding         The encoding format to return the encrypted object in
	 * @param initVectorOrSalt The initialization vector or salt
	 * @param iterations       The number of iterations to use for the algorithm
	 *
	 * @return
	 */
	public static Object crypt( int cipherMode, Object obj, String algorithm, String key, String encoding, byte[] initVectorOrSalt, Integer iterations ) {
		byte[]	objectBytes	= cipherMode == Cipher.ENCRYPT_MODE
		    ? convertToByteArray( obj )
		    : decodeString( StringCaster.cast( obj ), encoding );

		int		ivsSize		= 0;

		try {
			Cipher cipher = Cipher.getInstance( algorithm );
			if ( initVectorOrSalt == null && ( isPBEAlgorithm( algorithm ) || isFBMAlgorithm( algorithm ) ) ) {
				ivsSize				= cipher.getBlockSize();
				initVectorOrSalt	= new byte[ ivsSize ];
				if ( cipherMode == Cipher.DECRYPT_MODE ) {
					System.arraycopy( objectBytes, 0, initVectorOrSalt, 0, ivsSize );
				} else {
					secureRandom.nextBytes( initVectorOrSalt );
				}
			}

			String					baseAlgorithm	= StringUtils.substringBefore( algorithm, "/" );
			AlgorithmParameterSpec	params			= null;
			SecretKey				cipherKey		= null;

			if ( isPBEAlgorithm( algorithm ) ) {
				params		= new PBEParameterSpec( initVectorOrSalt, iterations != null ? iterations : DEFAULT_ENCRYPTION_ITERATIONS );
				cipherKey	= SecretKeyFactory.getInstance( algorithm ).generateSecret( new PBEKeySpec( key.toCharArray() ) );
			} else if ( isFBMAlgorithm( algorithm ) ) {
				params = new IvParameterSpec( initVectorOrSalt );
			}

			if ( cipherKey == null ) {
				cipherKey = new SecretKeySpec( decodeKeyBytes( key ), baseAlgorithm );
			}

			cipher.init( cipherMode, cipherKey, params );

			if ( cipherMode == Cipher.DECRYPT_MODE ) {
				byte[] decryptedBytes = cipher.doFinal( objectBytes, ivsSize, objectBytes.length - ivsSize );
				try {
					return new String( decryptedBytes, DEFAULT_CHARSET );
				} catch ( UnsupportedEncodingException e ) {
					return SerializationUtils.deserialize( decryptedBytes );
				}
			} else {

				byte[] result = new byte[ ivsSize + cipher.getOutputSize( objectBytes.length ) ];

				if ( ivsSize > 0 ) {
					System.arraycopy( initVectorOrSalt, 0, result, 0, ivsSize );
				}

				cipher.doFinal( objectBytes, 0, objectBytes.length, result, ivsSize );

				return encodeObject( result, encoding );

			}

		} catch ( IllegalBlockSizeException e ) {
			throw new BoxRuntimeException( "An block size exception occurred while attempting to encrypt an object: " + e.getMessage(), e );
		} catch ( BadPaddingException e ) {
			if ( isECBMode( algorithm ) ) {
				throw new BoxRuntimeException(
				    "An padding exception occurred while attempting to encrypt an object. ECB modes require padding. The message received was:"
				        + e.getMessage(),
				    e );
			} else {
				throw new BoxRuntimeException( "An padding exception occurred while attempting to encrypt an object: " + e.getMessage(), e );
			}
		} catch ( InvalidKeySpecException | ShortBufferException | NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException
		    | InvalidAlgorithmParameterException e ) {
			throw new BoxRuntimeException( "An error occurred while attempting to encrypt an object: " + e.getMessage(), e );
		}

	}

	/**
	 * Encrypts an object using the specified algorithm and key, with optional vector or salt and iterations
	 *
	 * @param obj              The object to encrypt
	 * @param algorithm        The string representation of the algorithm ( e.g. AES, DES, etc. )
	 * @param key              The secret key to use for encryption
	 * @param encoding         The encoding format to return the encrypted object in
	 * @param initVectorOrSalt The initialization vector or salt
	 * @param iterations       The number of iterations to use for the algorithm
	 *
	 * @return
	 */
	public static String encrypt( Object obj, String algorithm, String key, String encoding, byte[] initVectorOrSalt, Integer iterations ) {
		return StringCaster.cast( crypt( Cipher.ENCRYPT_MODE, obj, algorithm, key, encoding, initVectorOrSalt, iterations ) );
	}

	/**
	 * Decrypts an object using the specified algorithm and key, with optional vector or salt and iterations
	 *
	 * @param encrypted        The encrypted object
	 * @param algorithm        The string representation of the algorithm ( e.g. AES, DES, etc. )
	 * @param key              The secret key to use for decryption
	 * @param encoding         The encoding format of the encrypted object
	 * @param initVectorOrSalt The initialization vector or salt
	 * @param iterations       The number of iterations to use for the algorithm
	 *
	 * @return The decrypted object
	 */
	public static Object decrypt( String encrypted, String algorithm, String key, String encoding, byte[] initVectorOrSalt, Integer iterations ) {
		return crypt( Cipher.DECRYPT_MODE, encrypted, algorithm, key, encoding, initVectorOrSalt, iterations );
	}

	/**
	 * Encodes an object byte array to the specified string output
	 *
	 * @param obj      The object byte array
	 * @param encoding The encoding format to use
	 *
	 * @return The string representation of the encrypted object
	 */
	public static String encodeObject( byte[] obj, String encoding ) {
		Key encodingKey = Key.of( encoding );
		// HEX encoding
		if ( encodingKey.equals( Key.encodingHex ) ) {
			StringBuilder sb = new StringBuilder( obj.length * 2 );
			for ( byte b : obj )
				sb.append( String.format( "%02x", b ) );
			return sb.toString();
		}
		// UU encoding
		else if ( encodingKey.equals( Key.encodingUU ) ) {
			return Base64.getMimeEncoder().encodeToString( obj );
		}
		// Base64 encoding
		else if ( encodingKey.equals( Key.encodingBase64 ) ) {
			return base64Encode( obj, Charset.forName( DEFAULT_CHARSET ) );
		}
		// Base64 URL encoding
		else if ( encodingKey.equals( Key.encodingBase64Url ) ) {
			return Base64.getUrlEncoder().encodeToString( obj );
		} else {
			throw new BoxRuntimeException(
			    String.format(
			        "The encoding argument [%s] is not a valid encoding type",
			        encodingKey.getName()
			    )
			);
		}
	}

	/**
	 * Decodes an encoded object string to a byte array
	 *
	 * @param encoded  The encoded object string
	 * @param encoding The encoding format to use
	 *
	 * @return The byte array representation of the encrypted object
	 */
	public static byte[] decodeString( String encoded, String encoding ) {
		Key encodingKey = Key.of( encoding );
		// HEX encoding
		if ( encodingKey.equals( Key.encodingHex ) ) {
			return HexFormat.of().parseHex( encoded );
		}
		// UU encoding
		else if ( encodingKey.equals( Key.encodingUU ) ) {
			return Base64.getMimeDecoder().decode( encoded );
		}
		// Base64 encoding
		else if ( encodingKey.equals( Key.encodingBase64 ) ) {
			return Base64.getDecoder().decode( encoded );
		}
		// Base64 URL encoding
		else if ( encodingKey.equals( Key.encodingBase64Url ) ) {
			return Base64.getUrlDecoder().decode( encoded );
		} else {
			throw new BoxRuntimeException(
			    String.format(
			        "The encoding argument [%s] is not a valid encoding type.",
			        encoding
			    )
			);
		}
	}

	/**
	 * Converts a generic object to a byte array
	 *
	 * @param obj
	 *
	 * @return
	 */
	public static byte[] convertToByteArray( Object obj ) {
		if ( obj instanceof String ) {
			try {
				return StringCaster.cast( obj ).getBytes( EncryptionUtil.DEFAULT_CHARSET );
			} catch ( UnsupportedEncodingException e ) {
				throw new BoxRuntimeException( "Provided object could not be encoded", e );
			}
		} else {
			try ( ByteArrayOutputStream b = new ByteArrayOutputStream() ) {
				try ( ObjectOutputStream o = new ObjectOutputStream( b ) ) {
					o.writeObject( obj );
				} catch ( IOException e ) {
					throw new BoxRuntimeException( "Error serializing object: " + e.getMessage(), e );
				}
				return b.toByteArray();
			} catch ( IOException e ) {
				throw new BoxRuntimeException( "Error serializing object: " + e.getMessage(), e );
			}
		}
	}

	/**
	 * Retrieves an instance of the specified random generator. If an agorithm is provided the method will return a SecureRandom instance
	 * 
	 * @param algorithm
	 * 
	 * @return
	 */
	public static Random getRandom( String algorithm ) {
		Key		algorithmKey	= algorithm == null ? Key._DEFAULT : Key.of( algorithm );

		Random	random			= randomStore.get( algorithmKey );
		if ( random == null ) {
			try {
				random = algorithmKey.equals( Key._DEFAULT ) ? new Random() : SecureRandom.getInstance( algorithm );
			} catch ( NoSuchAlgorithmException e ) {
				throw new BoxRuntimeException( "The algorithm: " + algorithm + " is not implemented in the current Java runtime", e );
			}
			randomStore.put( algorithmKey, random );
		}
		return random;
	}

	/**
	 * Returns the algorithm parameters for the specified algorithm
	 *
	 * @param algorithm        The string representation of the algorithm ( e.g. AES, DES, etc. )
	 * @param initVectorOrSalt The initialization vector or salt
	 * @param iterations       The number of iterations to use for the algorithm
	 *
	 * @return The algorithm parameters
	 */
	private static AlgorithmParameterSpec getAlgorithmParams( String algorithm, byte[] initVectorOrSalt, Integer iterations ) {
		if ( isPBEAlgorithm( algorithm ) ) {
			return new PBEParameterSpec( initVectorOrSalt, iterations != null ? iterations : DEFAULT_ENCRYPTION_ITERATIONS );
		} else if ( isFBMAlgorithm( algorithm ) && initVectorOrSalt != null ) {
			return new IvParameterSpec( initVectorOrSalt );
		} else if ( isCBCMode( algorithm ) ) {
			return new IvParameterSpec( new byte[ 16 ] );
		} else {
			return null;
		}
	}

	/**
	 * Returns true if the algorithm is a Password Based Encryption algorithm
	 *
	 * @param algorithm The string representation of the algorithm
	 *
	 * @return
	 */
	public static boolean isPBEAlgorithm( String algorithm ) {
		return StringUtils.startsWithIgnoreCase( algorithm, "PBE" );
	}

	/**
	 * Returns true if the algorithm is a Feedback Mode algorithm
	 *
	 * @param algorithm The string representation of the algorithm
	 *
	 * @return
	 */
	public static boolean isFBMAlgorithm( String algorithm ) {
		String[] algorithmParts = StringUtils.split( algorithm, "/" );
		return algorithm.indexOf( "/" ) > -1 && !StringUtils.startsWithIgnoreCase( algorithmParts[ 1 ], "ECB" );
	}

	/**
	 * Returns true if the algorithm is a Feedback Mode algorithm
	 *
	 * @param algorithm The string representation of the algorithm
	 *
	 * @return
	 */
	private static boolean isCBCMode( String algorithm ) {
		String[] algorithmParts = StringUtils.split( algorithm, "/" );
		return algorithmParts.length > 1 && StringUtils.startsWithIgnoreCase( algorithmParts[ 1 ], "CBC" );
	}

	/**
	 * Returns true if the algorithm is a Feedback Mode algorithm
	 *
	 * @param algorithm The string representation of the algorithm
	 *
	 * @return
	 */
	private static boolean isECBMode( String algorithm ) {
		String[] algorithmParts = StringUtils.split( algorithm, "/" );
		return algorithmParts.length > 1 && algorithmParts[ 1 ].equals( "ECB" );
	}

	/**
	 * Creates an insecure but very fast 64 bit hash of a string
	 * 
	 * @param hashItem the string to hash
	 */
	public static String generate64BitHash( CharSequence hashItem ) {
		return generate64BitHash( hashItem, Character.MAX_RADIX );
	}

	/**
	 * Tests whether a string is already Base64 encoded
	 * 
	 * @param input
	 * 
	 * @return
	 */
	public static boolean isBase64( String input ) {
		return BASE_64_PATTERN.matcher( input ).matches();
	}

	/**
	 * Creates an insecure but very fast 64 bit hash of a string
	 * 
	 * @param hashItem the string to hash
	 * @param size     the radix to use for the final length of the hash
	 */
	public static String generate64BitHash( CharSequence hashItem, int size ) {
		long			h		= HSTART;
		final long		hmult	= HMULT;
		final long[]	ht		= byteTable;
		final int		len		= hashItem.length();
		for ( int i = 0; i < len; i++ ) {
			char ch = hashItem.charAt( i );
			h	= ( h * hmult ) ^ ht[ ch & 0xff ];
			h	= ( h * hmult ) ^ ht[ ( ch >>> 8 ) & 0xff ];
		}
		return Long.toString( h < 0 ? 0 - h : h, size );
	}

	/**
	 * Creates a lookup table for 64 bit hashes
	 */
	private static final long[] generateHashLookupTable() {
		long[]	_byteTable	= new long[ 256 ];
		long	h			= 0x544B2FBACAAF1684L;
		for ( int i = 0; i < 256; i++ ) {
			for ( int j = 0; j < 31; j++ ) {
				h	= ( h >>> 7 ) ^ h;
				h	= ( h << 11 ) ^ h;
				h	= ( h >>> 10 ) ^ h;
			}
			_byteTable[ i ] = h;
		}
		return _byteTable;
	}

}
