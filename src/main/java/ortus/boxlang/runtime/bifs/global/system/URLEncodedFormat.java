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

@BoxBIF
@BoxMember( type = BoxLangType.STRING )
public class URLEncodedFormat extends BIF {

	/**
	 * Constructor
	 */
	public URLEncodedFormat() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "any", Key.string )
		};
	}

	/**
	 * Generates a URL-encoded string. For example, it replaces spaces with %20, and non-alphanumeric characters with equivalent hexadecimal escape
	 * sequences. Passes arbitrary strings within a URL. *
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.String
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		String str = StringCaster.cast( arguments.get( Key.string ) );
		try {
			// W3C says to use UTF-8 for all encoding: http://www.w3.org/TR/html40/appendix/notes.html#non-ascii-chars
			return java.net.URLEncoder.encode( str, "utf-8" );
		} catch ( UnsupportedEncodingException e ) {
			return str;
		}
	}
}
