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
package ortus.boxlang.runtime.bifs.global.string;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.util.RegexBuilder;

@BoxBIF
@BoxMember( type = BoxLangType.STRING, name = "Wrap" )
public class Wrap extends BIF {

	public Wrap() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "string", Key.string ),
		    new Argument( true, "integer", Key.limit ),
		    new Argument( false, "boolean", Key.strip, false )
		};
	}

	/**
	 * Wraps a string at the specified limit, breaking at the last space within the limit.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.string The string to wrap.
	 *
	 * @argument.limit The character limit at which to wrap the string.
	 *
	 * @argument.strip If true, replaces all line endings with spaces before wrapping. Default is false.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		String	input	= arguments.getAsString( Key.string );
		int		limit	= arguments.getAsInteger( Key.limit );
		boolean	strip	= arguments.getAsBoolean( Key.strip );

		if ( strip ) {
			input = RegexBuilder.of( input, RegexBuilder.LINE_ENDINGS ).replaceAllAndGet( " " );
		}

		return wrapText( input, limit );
	}

	/**
	 * Wraps the given text at the specified limit, breaking at the last space within the limit.
	 *
	 * @param text  The text to wrap.
	 * @param limit The character limit at which to wrap the text.
	 *
	 * @return The wrapped text.
	 */
	private String wrapText( String text, int limit ) {
		if ( text == null ) {
			return "";
		}

		StringBuilder	wrapped	= new StringBuilder();
		int				index	= 0;

		while ( index < text.length() ) {
			// If the remaining text is shorter than the limit, add it all and break
			if ( index + limit > text.length() ) {
				wrapped.append( text, index, text.length() );
				break;
			}

			int spaceToWrapAt = text.lastIndexOf( ' ', index + limit );

			// If there is no acceptable space, force wrap at the limit
			if ( spaceToWrapAt <= index ) {
				wrapped.append( text, index, index + limit ).append( System.lineSeparator() );
				index += limit;
			} else {
				// Else, wrap at the last space within limit
				wrapped.append( text, index, spaceToWrapAt ).append( System.lineSeparator() );
				index = spaceToWrapAt + 1; // skip the space
			}
		}

		return wrapped.toString();
	}
}
