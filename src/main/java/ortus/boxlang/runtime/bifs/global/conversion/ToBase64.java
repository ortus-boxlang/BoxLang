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
package ortus.boxlang.runtime.bifs.global.conversion;

import java.io.UnsupportedEncodingException;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

@BoxBIF
@BoxMember( type = BoxLangType.STRING )
public class ToBase64 extends BIF {

	/**
	 * Constructor
	 */
	public ToBase64() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "any", Key.string_or_object ),
		    new Argument( false, "string", Key.encoding, "UTF-8" )
		};
	}

	/**
	 * Calculates the Base64 representation of a string or binary object. The Base64 format uses printable characters, allowing binary data to be sent in
	 * forms and e-mail, and stored in a database or file.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.string_or_object A string or a binary object.
	 * 
	 * @argument.encoding The character encoding (character set) of the string, used with binary data.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Object string_or_object = arguments.get( Key.string_or_object );

		if ( string_or_object instanceof byte[] b ) {
			return java.util.Base64.getEncoder().encodeToString( b );
		}

		String	string		= StringCaster.cast( string_or_object );
		String	encoding	= arguments.getAsString( Key.encoding );
		try {
			return java.util.Base64.getEncoder().encodeToString( string.getBytes( encoding ) );
		} catch ( UnsupportedEncodingException e ) {
			throw new BoxRuntimeException( "Bad encoding option: " + encoding, e );
		}

	}
}
