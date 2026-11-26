/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package ortus.boxlang.runtime.bifs.global.encryption;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.util.EncryptionUtil;

@BoxBIF( description = "Generate an HMAC (Hash-based Message Authentication Code)" )
@BoxMember( type = BoxLangType.STRING_STRICT, name = "Hmac" )

public class Hmac extends BIF {

	private static final String		DEFAULT_ALGORITHM	= "HmacMD5";
	private static final String		DEFAULT_ENCODING	= "utf-8";
	private static final Integer	DEFAULT_ITERATIONS	= 1;

	/**
	 * Constructor
	 */
	public Hmac() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "any", Key.input ),
		    new Argument( true, "any", Key.key ),
		    new Argument( false, "string", Key.algorithm, DEFAULT_ALGORITHM ),
		    new Argument( false, "string", Key.encoding, DEFAULT_ENCODING ),
		    new Argument( false, "integer", Key.numIterations, DEFAULT_ITERATIONS )
		};
	}

	/**
	 * Generates an HMAC (Hash-based Message Authentication Code) of a value using a secret key, returned as a lowercase hexadecimal string. The
	 * input is converted to bytes with the given encoding. This BIF is also a member function on strings, e.g. `"message".hmac( "secret",
	 * "HmacSHA256" )`.
	 *
	 * @argument.input The value to generate the HMAC for.
	 *
	 * @argument.key The secret key used to generate the HMAC, as a string or binary value. Any other type throws an exception.
	 *
	 * @argument.algorithm Optional. The HMAC algorithm, e.g. HmacMD5, HmacSHA1, HmacSHA256, HmacSHA384 or HmacSHA512. Defaults to HmacMD5.
	 *
	 * @argument.encoding Optional. The charset used to convert strings to bytes. Defaults to utf-8.
	 *
	 * @argument.numIterations Optional. Declared for compatibility but currently not used by the implementation. Defaults to 1.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @return The HMAC as a lowercase hexadecimal string.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Object	hashItem	= arguments.get( Key.input );
		Object	key			= arguments.get( Key.key );
		String	algorithm	= arguments.getAsString( Key.algorithm );
		String	encoding	= arguments.getAsString( Key.encoding );

		if ( key instanceof byte[] byteKey ) {
			return EncryptionUtil.hmac( hashItem, byteKey, algorithm, encoding );
		} else if ( key instanceof String stringKey ) {
			return EncryptionUtil.hmac( hashItem, stringKey, algorithm, encoding );
		} else {
			throw new BoxRuntimeException( "Invalid key supplied to function [hmac]. Key must be a binary object or string" );
		}
	}
}