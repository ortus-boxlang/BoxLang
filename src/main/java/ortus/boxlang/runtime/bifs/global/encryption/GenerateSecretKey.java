
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

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.CastAttempt;
import ortus.boxlang.runtime.dynamic.casters.IntegerCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.util.EncryptionUtil;

@BoxBIF( description = "Generate a secret key for encryption" )
public class GenerateSecretKey extends BIF {

	/**
	 * Constructor
	 */
	public GenerateSecretKey() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( false, Argument.STRING, Key.algorithm, EncryptionUtil.DEFAULT_ENCRYPTION_ALGORITHM ),
		    new Argument( false, Argument.NUMERIC, Key.keySize )
		};
	}

	/**
	 * Generates a random secret key for the given algorithm and returns it as a Base64 string, ready to be used as the `key` of `encrypt()` and
	 * `decrypt()`.
	 *
	 * @argument.algorithm Optional. The algorithm to generate the key for. Defaults to AES. Valid values are: AES, ARCFOUR, Blowfish, ChaCha20,
	 *                     DES, DESede, HmacMD5, HmacSHA1, HmacSHA224, HmacSHA256, HmacSHA384, HmacSHA512, HmacSHA3-224, HmacSHA3-256, HmacSHA3-384 and
	 *                     HmacSHA3-512.
	 *
	 * @argument.keySize Optional. The size of the key in bits. If not provided, the default key size of the algorithm is used.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @return The generated key as a Base64 string.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		CastAttempt<Integer> keySizeAttempt = IntegerCaster.attempt( arguments.get( Key.keySize ) );

		return EncryptionUtil.encodeKey(
		    EncryptionUtil.generateKey(
		        arguments.getAsString( Key.algorithm ), keySizeAttempt.getOrDefault( null ) ) );
	}

}
