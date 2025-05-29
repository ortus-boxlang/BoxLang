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

import java.util.Set;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.CastAttempt;
import ortus.boxlang.runtime.dynamic.casters.FunctionCaster;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.validation.Validator;

@BoxBIF
@BoxMember( type = BoxLangType.STRING_STRICT, name = "ReplaceNoCase" )
public class ReplaceNoCase extends BIF {

	/**
	 * Constructor
	 */
	public ReplaceNoCase() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "string", Key.string, "", Set.of(
		        Validator.REQUIRED,
		        Validator.DEFAULT_VALUE
		    ) ),
		    new Argument( true, "string", Key.substring1, Set.of( Validator.REQUIRED ) ),
		    new Argument( true, "any", Key.obj ),
		    new Argument( true, "string", Key.scope, "one", Set.of( Validator.valueOneOf( "one", "all" ) ) ),
		    new Argument( false, "string", Key.start, "1" )
		};
	}

	/**
	 *
	 * Replaces occurrences of substring1 in a string with obj, in a specified scope. The search is case-sensitive. Function returns original string with
	 * replacements made
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.string The string to search
	 *
	 * @argument.substring1 The substring to search for
	 *
	 * @argument.obj The string to replace substring1 with
	 *
	 * @argument.scope The scope to search in
	 *
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		String	string		= arguments.getAsString( Key.string );
		String	substring1	= arguments.getAsString( Key.substring1 );
		Object	obj			= arguments.get( Key.obj );
		String	scope		= arguments.getAsString( Key.scope ).toLowerCase();
		if ( scope.equalsIgnoreCase( "one" ) ) {
			String	lowerCaseString		= string.toLowerCase();
			String	lowerCaseSubstring	= substring1.toLowerCase();
			int		idx					= lowerCaseString.indexOf( lowerCaseSubstring );
			if ( idx != -1 ) {
				CastAttempt<String> stringCastAttempt = StringCaster.attempt( obj );
				return stringCastAttempt.wasSuccessful()
				    ? string.substring( 0, idx ) + stringCastAttempt.get() + string.substring( idx + substring1.length() )
				    : string.substring( 0, idx )
				        + context.invokeFunction( FunctionCaster.cast( obj ),
				            new Object[] { string.substring( idx, idx + substring1.length() ), idx + 1, string } )
				        + string.substring( idx + substring1.length() );
			} else {
				return string;
			}
		} else if ( scope.equalsIgnoreCase( "all" ) ) {
			String			lowerCaseString		= string.toLowerCase();
			String			lowerCaseSubstring	= substring1.toLowerCase();
			StringBuilder	result				= new StringBuilder();
			int				i					= 0;
			while ( i < string.length() ) {
				if ( lowerCaseString.substring( i ).startsWith( lowerCaseSubstring ) ) {
					CastAttempt<String> stringCastAttempt = StringCaster.attempt( obj );
					if ( stringCastAttempt.wasSuccessful() ) {
						result.append( stringCastAttempt.get() );
					} else {
						result.append( context.invokeFunction( FunctionCaster.cast( obj ),
						    new Object[] { string.substring( i, i + substring1.length() ), i + 1, string } ) );
					}
					i += substring1.length();
				} else {
					result.append( string.charAt( i ) );
					i++;
				}
			}
			return result.toString();
		} else {
			throw new BoxRuntimeException( "Invalid replacement scope: [" + scope + "]. Valid options are 'one' or 'all'." );
		}
	}

}
