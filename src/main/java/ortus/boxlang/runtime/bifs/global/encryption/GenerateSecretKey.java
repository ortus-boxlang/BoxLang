
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

@BoxBIF
@BoxBIF(alias = "GeneratePDBKDFKey")
public class GenerateSecretKey extends BIF {

	/**
	 * Constructor
	 */
	public GenerateSecretKey() {
		super();
		declaredArguments = new Argument[] {
				new Argument(false, Argument.STRING, Key.algorithm, EncryptionUtil.DEFAULT_ENCRYPTION_ALGORITHM),
				new Argument(false, Argument.NUMERIC, Key.keySize)
		};
	}

	/**
	 * Generates an encoded encryption key using the specified algorithm and key
	 * size
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.algorithm The algorithm to use for generating the key. The default
	 *                     is AES. Example values are: AES, DES, DESede, Blowfish,
	 *                     HmacSHA1, HmacSHA256, HmacSHA384, HmacSHA512
	 *
	 * @argument.keySize The optional size of the key to generate. If not provided
	 *                   the default key size for the algorithm will be used
	 */
	public Object _invoke(IBoxContext context, ArgumentsScope arguments) {
		CastAttempt<Integer> keySizeAttempt = IntegerCaster.attempt(arguments.get(Key.keySize));

		return EncryptionUtil.encodeKey(
				EncryptionUtil.generateKey(
						arguments.getAsString(Key.algorithm), keySizeAttempt.getOrDefault(null)));
	}

}
