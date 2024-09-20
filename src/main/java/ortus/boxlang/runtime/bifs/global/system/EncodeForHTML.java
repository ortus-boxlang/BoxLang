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
package ortus.boxlang.runtime.bifs.global.system;

import org.apache.commons.text.StringEscapeUtils;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;

@BoxBIF
@BoxBIF( alias = "htmlEditFormat" )
public class EncodeForHTML extends BIF {

	/**
	 * Constructor
	 */
	public EncodeForHTML() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "string", Key.string ),
		    new Argument( true, "boolean", Key.canonicalize, false )
		};
	}

	/**
	 * Encodes the input string for safe output in the body of a HTML tag. The encoding in meant to mitigate Cross Site Scripting (XSS) attacks. This
	 * function can provide more protection from XSS than the HTMLEditFormat or XMLFormat functions do.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.String The string to encode.
	 *
	 * @argument.canonicalize If set to true, canonicalization happens before encoding. If set to false, the given input string will just be encoded.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		String str = arguments.getAsString( Key.string );
		if ( str == null ) {
			return null;
		}

		if ( arguments.getAsBoolean( Key.canonicalize ) ) {
			str = str.intern();
		}
		return StringEscapeUtils.escapeHtml4( str );
	}
}
