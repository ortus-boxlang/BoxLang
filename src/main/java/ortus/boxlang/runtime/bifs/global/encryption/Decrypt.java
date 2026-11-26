
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

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.util.EncryptionUtil;

@BoxBIF( description = "Decrypt an encrypted string" )
@BoxBIF( alias = "DecryptBinary" )

public class Decrypt extends BIF {

	/**
	 * Constructor
	 */
	public Decrypt() {
		super();
		// Uncomment and define declare argument to this BIF
		declaredArguments = new Argument[] {
		    new Argument( true, "string", Key.string ),
		    new Argument( true, "string", Key.key ),
		    new Argument( false, "string", Key.algorithm ),
		    new Argument( false, "string", Key.encoding, EncryptionUtil.DEFAULT_ENCRYPTION_ENCODING ),
		    new Argument( false, "any", Key.IVorSalt ),
		    new Argument( false, "integer", Key.iterations, EncryptionUtil.DEFAULT_ENCRYPTION_ITERATIONS ),
		    new Argument( false, "boolean", Key.precise, false )
		};
	}

	/**
	 * Decrypts a string that was encrypted and encoded by `encrypt()`, using the given key and algorithm. The encrypted text is first decoded
	 * using the `encoding`, then decrypted. The result is returned as a string, unless the decrypted bytes are a Java-serialized object, in
	 * which case the deserialized object is returned.
	 *
	 * This BIF is also registered as `DecryptBinary`, which runs exactly the same code.
	 *
	 * @argument.string The encrypted (and encoded) text to decrypt.
	 *
	 * @argument.key The Base64 string representation of the secret key used for encryption (see `generateSecretKey()` and `generatePBKDFKey()`).
	 *               RSA algorithms instead take a Base64 RSA key (see `generateKeyPair()`).
	 *
	 * @argument.algorithm Optional. The algorithm (optionally with mode and padding, e.g. `AES/CBC/PKCS5Padding`) used for encryption. Defaults
	 *                     to `AES`.
	 *
	 * @argument.encoding Optional. The encoding of the encrypted text: `UU`, `Hex`, `Base64` or `Base64Url`. Defaults to `UU`.
	 *
	 * @argument.IVorSalt Optional. The initialization vector or salt used for encryption (a string or binary). Required for feedback modes such
	 *                    as CBC and for PBE algorithms. For feedback-mode algorithms, a string is padded or truncated to 16 bytes. If it was omitted when
	 *                    encrypting, it must be omitted here too.
	 *
	 * @argument.iterations Optional. The number of iterations used by PBE algorithms. Defaults to 1000.
	 *
	 * @argument.precise Optional. Declared for compatibility but currently not used by the implementation. Defaults to false.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @return The decrypted string, or the deserialized object if the decrypted data was a serialized Java object.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		String	content		= arguments.getAsString( Key.string );
		String	algorithm	= arguments.getAsString( Key.algorithm );
		if ( algorithm == null ) {
			algorithm = EncryptionUtil.DEFAULT_ENCRYPTION_ALGORITHM;
		}
		String	encoding		= arguments.getAsString( Key.encoding );
		Charset	encodingCharset	= Charset.forName( EncryptionUtil.DEFAULT_CHARSET );
		byte[]	IVorSalt		= null;
		if ( arguments.get( Key.IVorSalt ) != null ) {

			if ( arguments.get( Key.IVorSalt ) instanceof String ivString
			    && EncryptionUtil.isFBMAlgorithm( algorithm )
			    && ivString.getBytes().length != EncryptionUtil.FBMA_IV_SIZE ) {
				byte[]	bytes			= ivString.getBytes( encodingCharset );
				byte[]	fixedSizeBytes	= new byte[ EncryptionUtil.FBMA_IV_SIZE ];
				Arrays.fill( fixedSizeBytes, ( byte ) 0 );
				System.arraycopy( bytes, 0, fixedSizeBytes, 0, Math.min( bytes.length, EncryptionUtil.FBMA_IV_SIZE ) );
				arguments.put( Key.IVorSalt, fixedSizeBytes );
			}

			IVorSalt = arguments.get( Key.IVorSalt ) instanceof byte[] ? ( byte[] ) arguments.get( Key.IVorSalt )
			    : arguments.getAsString( Key.IVorSalt ).getBytes( Charset.forName( EncryptionUtil.DEFAULT_CHARSET ) );
		}

		return EncryptionUtil.decrypt( content, algorithm, arguments.getAsString( Key.key ), encoding, IVorSalt, arguments.getAsInteger( Key.iterations ) );
	}

}
