
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

import java.util.HashMap;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.CastAttempt;
import ortus.boxlang.runtime.dynamic.casters.IntegerCaster;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.util.EncryptionUtil;

@BoxBIF( description = "Generate a PBKDF2 (Password-Based Key Derivation Function) key" )
public class GeneratePBKDFKey extends BIF {

	/**
	 * Alt algorithms which are actually the same as the Hmac version
	 */
	private static final HashMap<Key, String> altAlgorithms = new HashMap<Key, String>() {

		{
			put( Key.of( "PBKDF2WithSHA1" ), "PBKDF2WithHmacSHA1" );
			put( Key.of( "PBKDF2WithSHA224" ), "PBKDF2WithHmacSHA224" );
			put( Key.of( "PBKDF2WithSHA256" ), "PBKDF2WithHmacSHA256" );
			put( Key.of( "PBKDF2WithSHA384" ), "PBKDF2WithHmacSHA384" );
			put( Key.of( "PBKDF2WithSHA512" ), "PBKDF2WithHmacSHA512" );
		}
	};

	/**
	 * Constructor
	 */
	public GeneratePBKDFKey() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, Argument.STRING, Key.algorithm, EncryptionUtil.DEFAULT_ENCRYPTION_ALGORITHM ),
		    new Argument( true, Argument.STRING, Key.passphrase ),
		    new Argument( true, Argument.STRING, Key.salt ),
		    new Argument( false, Argument.NUMERIC, Key.iterations ),
		    new Argument( false, Argument.NUMERIC, Key.keySize )
		};
	}

	/**
	 * Derives a secret key from a passphrase and salt using PBKDF2 and returns it as a Base64 string, which can be used as the `key` of
	 * `encrypt()` and `decrypt()`. The aliases `PBKDF2WithSHA1`, `PBKDF2WithSHA224`, `PBKDF2WithSHA256`, `PBKDF2WithSHA384` and
	 * `PBKDF2WithSHA512` are mapped to their `PBKDF2WithHmacSHA*` equivalents.
	 *
	 * @argument.algorithm The PBKDF2 algorithm: PBKDF2WithHmacSHA1, PBKDF2WithHmacSHA224, PBKDF2WithHmacSHA256, PBKDF2WithHmacSHA384 or
	 *                     PBKDF2WithHmacSHA512. Although declared with a default of AES, a PBKDF2 algorithm must be supplied.
	 *
	 * @argument.passphrase The passphrase to derive the key from.
	 *
	 * @argument.salt The salt, a random string, used when deriving the key. The same passphrase and salt always produce the same key.
	 *
	 * @argument.iterations Optional. The number of iterations. Defaults to 1000.
	 *
	 * @argument.keySize Optional. The size of the key in bits. Defaults to 256.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @return The derived key as a Base64 string.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		CastAttempt<Integer>	keySizeAttempt		= IntegerCaster.attempt( arguments.get( Key.keySize ) );
		CastAttempt<Integer>	iterationsAttempt	= IntegerCaster.attempt( arguments.get( Key.iterations ) );

		Key						algorithmKey		= Key.of( arguments.getAsString( Key.algorithm ) );
		if ( altAlgorithms.containsKey( algorithmKey ) ) {
			// If the algorithm is an alt, use the Hmac version
			arguments.put( Key.algorithm, altAlgorithms.get( algorithmKey ) );
		}

		return EncryptionUtil.encodeKey(
		    EncryptionUtil.generatePBKDFKey(
		        arguments.getAsString( Key.algorithm ),
		        StringCaster.cast( arguments.get( Key.passphrase ) ),
		        StringCaster.cast( arguments.get( Key.salt ) ),
		        iterationsAttempt.getOrDefault( null ),
		        keySizeAttempt.getOrDefault( null )
		    )
		);
	}

}
