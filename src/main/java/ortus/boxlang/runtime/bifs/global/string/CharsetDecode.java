
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

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;

@BoxBIF
@BoxMember( type = BoxLangType.STRING_STRICT, name = "CharsetDecode" )

public class CharsetDecode extends BIF {

	/**
	 * Constructor
	 */
	public CharsetDecode() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "string", Key.encoded_binary ),
		    new Argument( false, "string", Key.encoding, "utf-8" )
		};
	}

	/**
	 * Encodes a string to a binary representation
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.encoded_binary The string to encode to binary
	 * 
	 * @argument.encoding The charset encoding to use (default: utf-8 )
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		return arguments.getAsString( Key.encoded_binary )
		    .getBytes(
		        Charset.forName( arguments.getAsString( Key.encoding ) )
		    );
	}

}
