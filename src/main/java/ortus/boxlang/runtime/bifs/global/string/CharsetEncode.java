
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

package ortus.boxlang.runtime.bifs.global.string;

import java.nio.charset.Charset;

import org.apache.commons.lang3.StringUtils;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;

@BoxBIF( description = "Encode a string to the specified character set" )

public class CharsetEncode extends BIF {

	/**
	 * Constructor
	 */
	public CharsetEncode() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "byte[]", Key.binary ),
		    new Argument( false, "string", Key.encoding, "utf-8" )
		};
	}

	/**
	 * Convert binary data (a byte array) into a string by decoding it with the given character set.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.binary The binary data to convert to a string.
	 *
	 * @argument.encoding The name of the character set to use, for example "utf-8" or "iso-8859-1". Defaults to "utf-8".
	 *
	 * @throws java.nio.charset.UnsupportedCharsetException if the character set is not supported.
	 *
	 * @return The string decoded from the bytes.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		byte[] binary = ( byte[] ) arguments.get( Key.binary );

		return StringUtils.toEncodedString(
		    binary,
		    Charset.forName( arguments.getAsString( Key.encoding ) )
		);

	}

}
