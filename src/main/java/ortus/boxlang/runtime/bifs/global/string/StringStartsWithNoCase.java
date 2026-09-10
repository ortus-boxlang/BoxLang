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

@BoxBIF( description = "Determines whether a string starts with a specified prefix, case-insensitive" )
@BoxMember( type = BoxLangType.STRING_STRICT )
public class StringStartsWithNoCase extends BIF {

	/**
	 * Constructor
	 */
	public StringStartsWithNoCase() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "string", Key.string ),
		    new Argument( true, "string", Key.substring )
		};
	}

	/**
	 * Determines whether a string starts with a specified prefix, case-insensitive.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.string The string to check.
	 *
	 * @argument.substring The prefix to check for.
	 *
	 * @return True if the string starts with the specified prefix (case-insensitive), false otherwise.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		String	input	= arguments.getAsString( Key.string );
		String	prefix	= arguments.getAsString( Key.substring );

		return input.toLowerCase().startsWith( prefix.toLowerCase() );
	}
}
