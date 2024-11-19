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
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.util.RegexUtil;

@BoxBIF
@BoxBIF( alias = "reMatchNoCase" )
@BoxMember( type = BoxLangType.STRING, name = "reMatch", objectArgument = "string" )
@BoxMember( type = BoxLangType.STRING, name = "reMatchNoCase", objectArgument = "string" )
public class ReMatch extends BIF {

	private static final Key reMatchNoCase = Key.of( "reMatchNoCase" );

	/**
	 * Constructor
	 */
	public ReMatch() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "string", Key.reg_expression ),
		    new Argument( true, "string", Key.string )
		};
	}

	/**
	 * 
	 * Uses a regular expression (RE) to search a string for a pattern, starting from a specified position.
	 * 
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 * 
	 * @argument.reg_expression The regular expression to search for
	 * 
	 * @argument.string The string to serach in
	 * 
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		String	reg_expression	= arguments.getAsString( Key.reg_expression );
		String	string			= arguments.getAsString( Key.string );
		boolean	noCase			= arguments.get( BIF.__functionName ).equals( reMatchNoCase );

		if ( string == null ) {
			return new Array();
		}

		if ( noCase ) {
			reg_expression = "(?i)" + reg_expression;
		}

		// Posix replacement for character classes
		reg_expression	= RegexUtil.posixReplace( reg_expression, noCase );
		// Ignore non-quantifier curly braces like PERL
		reg_expression	= RegexUtil.replaceNonQuantiferCurlyBraces( reg_expression );

		Matcher	matcher	= java.util.regex.Pattern.compile( reg_expression ).matcher( string );
		Array	result	= new Array();

		while ( matcher.find() ) {
			result.add( matcher.group() );
		}

		return result;
	}

}
