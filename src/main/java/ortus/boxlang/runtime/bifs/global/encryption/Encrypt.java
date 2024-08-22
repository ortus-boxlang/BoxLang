
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

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.util.EncryptionUtil;

@BoxBIF
@BoxBIF( alias = "EncryptBinary" )

public class Encrypt extends BIF {

	/**
	 * Constructor
	 */
	public Encrypt() {
		super();
		// Uncomment and define declare argument to this BIF
		declaredArguments = new Argument[] {
		    new Argument( true, "any", Key.object ),
		    new Argument( true, "string", Key.key ),
		    new Argument( false, "string", Key.algorithm ),
		    new Argument( false, "string", Key.encoding, EncryptionUtil.DEFAULT_ENCRYPTION_ENCODING ),
		    new Argument( false, "any", Key.IVorSalt ),
		    new Argument( false, "integer", Key.iterations, EncryptionUtil.DEFAULT_ENCRYPTION_ITERATIONS ),
		    new Argument( false, "boolean", Key.precise, true )
		};
	}

	/**
	 * Encrypts an object using the specified algorithm and key
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @function.EncryptBinary Encrypts a binary object using the specified algorithm and key
	 *
	 * @argument.object The object to encrypt. If the object is not a string or binary data, the object must implement the java.io.Serializable interface
	 *
	 * @argument.key The string representation of the secret key to use for encryption ( see generateSecretKey() )
	 *
	 * @argument.algorithm The algorithm to use for encryption. Default is AES
	 *
	 * @argument.encoding The encoding type to use for encoding the encrypted data. Default is Base64
	 *
	 * @argument.IVorSalt The initialization vector or salt to use for encryption.
	 *
	 * @argument.iterations The number of iterations to use for encryption. Default is 1000
	 *
	 * @argument.precise If set to true, the string and key will be validated before encryption to ensure conformity to the algorithm. Default is false
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		if ( arguments.get( Key.string ) != null ) {
			arguments.put( Key.object, arguments.get( Key.string ) );
		}
		Object	content		= arguments.get( Key.object );
		String	algorithm	= arguments.getAsString( Key.algorithm );
		if ( algorithm == null ) {
			algorithm = EncryptionUtil.DEFAULT_ENCRYPTION_ALGORITHM;
		}
		String	encoding	= arguments.getAsString( Key.encoding );
		byte[]	IVorSalt	= null;
		if ( arguments.get( Key.IVorSalt ) != null ) {
			IVorSalt = arguments.get( Key.IVorSalt ) instanceof byte[] ? ( byte[] ) arguments.get( Key.IVorSalt )
			    : arguments.getAsString( Key.IVorSalt ).getBytes( Charset.forName( EncryptionUtil.DEFAULT_CHARSET ) );
		}

		// SecretKey key = EncryptionUtil.decodeKey( arguments.getAsString( Key.key ), algorithm );
		return EncryptionUtil.encrypt( content, algorithm, arguments.getAsString( Key.key ), encoding, IVorSalt, arguments.getAsInteger( Key.iterations ) );
	}

}
