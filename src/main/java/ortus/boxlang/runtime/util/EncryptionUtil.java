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

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.stream.IntStream;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxIOException;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

public final class EncryptionUtil {

	public final static String	DEFAULT_ALGORITHM	= "MD5";

	/**
	 * Supported key algorithms
	 * <a href="https://docs.oracle.com/en/java/javase/17/docs/specs/security/standard-names.html#keyfactory-algorithms">key factory algorithms</a>
	 */
	public final static Struct	KEY_ALGORITHMS		= new Struct(
	    new HashMap<String, String>() {

		    {
			    put( "AES", "AES" );
			    put( "ARCFOUR", "ARCFOUR" );
			    put( "Blowfish", "Blowfish" );
			    put( "ChaCha20", "ChaCha20" );
			    put( "DES", "DES" );
			    put( "DESede", "DESede" );
			    put( "HmacMD5", "HmacMD5" );
			    put( "HmacSHA1", "HmacSHA1" );
			    put( "HmacSHA224", "HmacSHA224" );
			    put( "HmacSHA256", "HmacSHA256" );
			    put( "HmacSHA384", "HmacSHA384" );
			    put( "HmacSHA512", "HmacSHA512" );
			    put( "HmacSHA3-224", "HmacSHA3-224" );
			    put( "HmacSHA3-256", "HmacSHA3-256" );
			    put( "HmacSHA3-384", "HmacSHA3-384" );
			    put( "HmacSHA3-512", "HmacSHA3-512" );
		    }
	    }
	);

	/**
	 * Performs a hash of a an object using the default algorithm
	 *
	 * @param object The object to be hashed
	 *
	 * @return returns the hashed string
	 */
	public static String hash( Object object ) {
		return hash( object, DEFAULT_ALGORITHM );
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
			return hash( ( byte[] ) object, algorithm );
		} else {
			return hash( object.toString().getBytes(), algorithm );
		}
	}

	/**
	 * Performs a hash of a byte array using a supported algorithm
	 *
	 * @param byteArray the byte array representing the object
	 * @param algorithm The supported {@link java.security.MessageDigest } algorithm (case-insensitive)
	 *
	 * @return returns the hashed string
	 */
	public static String hash( byte[] byteArray, String algorithm ) {
		try {
			StringBuilder	result	= new StringBuilder();
			MessageDigest	md		= MessageDigest.getInstance( algorithm.toUpperCase() );
			md.update( byteArray );
			byte[] digest = md.digest();
			IntStream
			    .range( 0, digest.length )
			    .forEach( idx -> result.append( String.format( "%02x", digest[ idx ] ) ) );
			return result.toString();
		} catch ( NoSuchAlgorithmException e ) {
			throw new BoxRuntimeException(
			    String.format(
			        "The algorithm [%s] provided is not a valid digest algorithm.",
			        algorithm.toUpperCase()
			    )
			);
		}
	}

	/**
	 * Peforms a checksum of a file path object using the MD5 algorithm
	 *
	 * @param filePath The {@link java.nio.file.Path} object
	 *
	 * @return returns the checksum string
	 */
	public static String checksum( Path filePath ) {
		return checksum( filePath, DEFAULT_ALGORITHM );
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

	public static String hmac( byte[] encryptItem, String key, String algorithm, String encoding ) {
		Charset charset = Charset.forName( encoding );
		// Attempt to keep the correct casing on the key
		algorithm = ( String ) KEY_ALGORITHMS.getOrDefault( algorithm, algorithm );
		try {
			Mac				mac			= Mac.getInstance( algorithm );
			SecretKeySpec	secretKey	= new SecretKeySpec( key.getBytes( charset ), algorithm );
			mac.init( secretKey );
			return hash( mac.doFinal( ( byte[] ) encryptItem ), algorithm.replaceFirst( "Hmac", "" ) );
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

	public static String base64Encode( Object item, Charset charset ) {
		byte[] encodeItem = null;
		if ( item instanceof byte[] ) {
			encodeItem = ( byte[] ) item;
		} else if ( item instanceof String ) {
			encodeItem = StringCaster.cast( item ).getBytes( charset );
		} else {
			encodeItem = item.toString().getBytes( charset );
		}
		return Base64.getEncoder().encode( encodeItem ).toString();
	}

}
