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
import ortus.boxlang.runtime.dynamic.casters.IntegerCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.util.EncryptionUtil;

@BoxBIF( description = "Generate an asymmetric key pair (Ed25519, Ed448, X25519, X448, RSA, EC)" )
public class GenerateKeyPair extends BIF {

	/**
	 * Constructor
	 */
	public GenerateKeyPair() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, Argument.STRING, Key.algorithm ),
		    new Argument( false, Argument.NUMERIC, Key.keySize ),
		    new Argument( false, Argument.STRING, Key.curve )
		};
	}

	/**
	 * Generates an asymmetric key pair and returns it as a struct with
	 * algorithm, privateKey (base64 PKCS8 DER), publicKey (base64 X.509 SPKI DER),
	 * privatePem and publicPem.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.algorithm The algorithm: Ed25519, Ed448, X25519, X448, RSA or EC.
	 *
	 * @argument.keySize RSA only. 2048 (default), 3072 or 4096.
	 *
	 * @argument.curve EC only. secp256r1 (default), secp384r1 or secp521r1.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Object	size	= arguments.get( Key.keySize );
		Integer	keySize	= null;
		if ( size != null ) {
			keySize = IntegerCaster.attempt( size ).getOrFail();
		}
		return EncryptionUtil.generateKeyPair( arguments.getAsString( Key.algorithm ), keySize, arguments.getAsString( Key.curve ) );
	}

}
