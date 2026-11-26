
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

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Objects;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.dynamic.casters.CastAttempt;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.util.EncryptionUtil;

@BoxBIF( description = "Encrypt a string using a specified algorithm" )
@BoxBIF( alias = "EncryptBinary" )

public class Encrypt extends BIF {

	/**
	 * Constructor
	 */
	public Encrypt() {
		super();
		// Uncomment and define declare argument to this BIF
		declaredArguments = new Argument[] {
		    new Argument( false, "any", Key.object ),
		    new Argument( true, "string", Key.key ),
		    new Argument( false, "string", Key.algorithm ),
		    new Argument( false, "string", Key.encoding, EncryptionUtil.DEFAULT_ENCRYPTION_ENCODING ),
		    new Argument( false, "any", Key.IVorSalt ),
		    new Argument( false, "integer", Key.iterations, EncryptionUtil.DEFAULT_ENCRYPTION_ITERATIONS ),
		    new Argument( false, "boolean", Key.precise, true )
		};
	}

	/**
	 * Encrypts an object using the specified algorithm and key and returns the encrypted data as an encoded string. Strings (and values castable
	 * to strings) are encrypted as UTF-8 bytes, binary values as raw bytes, and any other object must be serializable. A null object throws an
	 * exception.
	 *
	 * This BIF is also registered as `EncryptBinary`, which currently runs exactly the same code and so also returns an encoded string.
	 *
	 * @argument.object The object to encrypt. If the object is not a string or binary data, it must implement java.io.Serializable.
	 *
	 * @argument.key The Base64 string representation of the secret key to use for encryption (see `generateSecretKey()` and
	 *               `generatePBKDFKey()`). RSA algorithms instead take a Base64 RSA key (see `generateKeyPair()`).
	 *
	 * @argument.algorithm Optional. The algorithm to use, optionally with mode and padding, e.g. `AES/CBC/PKCS5Padding`. Defaults to `AES`.
	 *
	 * @argument.encoding Optional. The encoding of the returned encrypted text: `UU`, `Hex`, `Base64` or `Base64Url`. Defaults to `UU`.
	 *
	 * @argument.IVorSalt Optional. The initialization vector or salt (a string or binary). Required for feedback modes such as CBC and for PBE
	 *                    algorithms. For feedback-mode algorithms, a string is padded or truncated to 16 bytes. If omitted for these algorithms, a random one
	 *                    is generated and prepended to the encrypted data, so that `decrypt()` can also be called without it.
	 *
	 * @argument.iterations Optional. The number of iterations used by PBE algorithms. Defaults to 1000.
	 *
	 * @argument.precise Optional. Declared for compatibility but currently not used by the implementation. Defaults to true.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @return The encrypted data as a string, in the requested encoding.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		// Backwards compat argument named "string" is now "object"
		if ( arguments.get( Key.string ) != null ) {
			arguments.put( Key.object, arguments.get( Key.string ) );
		}
		Object content = arguments.get( Key.object );
		// Make sure content is not null or throw an error
		Objects.requireNonNull( content, "Content to encrypt cannot be null" );

		CastAttempt<String> stringContentAttempt = StringCaster.attempt( content );
		if ( stringContentAttempt.wasSuccessful() ) {
			content = stringContentAttempt.get();
		}

		String algorithm = arguments.getAsString( Key.algorithm );
		if ( algorithm == null ) {
			algorithm = EncryptionUtil.DEFAULT_ENCRYPTION_ALGORITHM;
		}
		String	encoding		= arguments.getAsString( Key.encoding );
		Charset	encodingCharset	= Charset.forName( EncryptionUtil.DEFAULT_CHARSET );
		byte[]	IVorSalt		= null;

		if ( arguments.get( Key.IVorSalt ) instanceof String ivString
		    && EncryptionUtil.isFBMAlgorithm( algorithm )
		    && ivString.getBytes().length != EncryptionUtil.FBMA_IV_SIZE ) {
			byte[]	bytes			= ivString.getBytes( encodingCharset );
			byte[]	fixedSizeBytes	= new byte[ EncryptionUtil.FBMA_IV_SIZE ];
			Arrays.fill( fixedSizeBytes, ( byte ) 0 );
			System.arraycopy( bytes, 0, fixedSizeBytes, 0, Math.min( bytes.length, EncryptionUtil.FBMA_IV_SIZE ) );
			arguments.put( Key.IVorSalt, fixedSizeBytes );
		}

		if ( arguments.get( Key.IVorSalt ) != null ) {
			IVorSalt = arguments.get( Key.IVorSalt ) instanceof byte[] ? ( byte[] ) arguments.get( Key.IVorSalt )
			    : arguments.getAsString( Key.IVorSalt ).getBytes( encodingCharset );
		}

		// SecretKey key = EncryptionUtil.decodeKey( arguments.getAsString( Key.key ), algorithm );
		return EncryptionUtil.encrypt( content, algorithm, arguments.getAsString( Key.key ), encoding, IVorSalt, arguments.getAsInteger( Key.iterations ) );
	}

}
