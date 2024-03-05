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

import java.util.regex.Matcher;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

@BoxBIF
@BoxMember( type = BoxLangType.STRING, name = "ReReplaceNoCase" )
public class ReReplaceNoCase extends BIF {

	/**
	 * Constructor
	 */
	public ReReplaceNoCase() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "string", Key.string ),
		    new Argument( true, "string", Key.regex ),
		    new Argument( true, "string", Key.substring ),
		    new Argument( true, "string", Key.scope, "once" )
		};
	}

	/**
	 * 
	 * Uses a regular expression (regex) to search a string for a string pattern and replace it with another. The search is case-sensitive.
	 * 
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 * 
	 * @argument.string The string to search
	 * 
	 * @argument.regex The regular expression to search for
	 * 
	 * @argument.substring The string to replace regex with
	 * 
	 * @argument.scope The scope to search in
	 * 
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		String	string		= arguments.getAsString( Key.string );
		String	regex		= arguments.getAsString( Key.regex );
		String	substring	= arguments.getAsString( Key.substring );
		String	scope		= arguments.getAsString( Key.scope ).toLowerCase();

		if ( scope.equals( "once" ) ) {
			return string.replaceFirst( "(?i)" + regex, Matcher.quoteReplacement( substring ) );
		} else if ( scope.equals( "all" ) ) {
			return string.replaceAll( "(?i)" + regex, Matcher.quoteReplacement( substring ) );
		} else {
			throw new BoxRuntimeException( "Invalid replacement scope: [" + scope + "]. Valid options are 'once' or 'all'." );
		}
	}

}
