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
import ortus.boxlang.runtime.types.util.StringUtil;

@BoxBIF( description = "Convert a string to a URL-friendly slug" )
@BoxMember( type = BoxLangType.STRING_STRICT )
public class Slugify extends BIF {

	/**
	 * Constructor
	 */
	public Slugify() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, Argument.STRING, Key.string ),
		    new Argument( false, Argument.INTEGER, Key.maxLength, 0 ),
		    new Argument( false, Argument.STRING, Key.allow, "" )
		};
	}

	/**
	 * Slugify a string so it is safe to use in a URL. The string is trimmed and lowercased, runs of whitespace become a hyphen, accents are
	 * removed, and every character
	 * that is not a-z or 0-9 (or one of the allowed characters) is replaced with a hyphen. Consecutive hyphens are not collapsed.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.string The string to slugify.
	 *
	 * @argument.maxLength The maximum number of characters of the resulting slug. 0 means no limit. Defaults to 0.
	 *
	 * @argument.allow Additional characters to allow, written so they are safe inside a regular expression character class (for example "_" or
	 *                 "\\-"). Defaults to none.
	 *
	 * @return The slug.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		return StringUtil.slugify(
		    arguments.getAsString( Key.string ),
		    arguments.getAsInteger( Key.maxLength ),
		    arguments.getAsString( Key.allow )
		);
	}
}
