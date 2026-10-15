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

@BoxBIF( description = "Replace occurrences of a substring in a string (case-insensitive)" )
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
		String									string				= arguments.getAsString( Key.string );
		String									substring1			= arguments.getAsString( Key.substring1 );
		Object									obj					= arguments.get( Key.obj );
		String									scope				= arguments.getAsString( Key.scope ).toLowerCase();

		// The replacement is either a plain string or a callable; work that out once up front
		// instead of re-casting the same value on every match inside the loop.
		CastAttempt<String>						replacementString	= StringCaster.attempt( obj );
		ortus.boxlang.runtime.types.Function	replacementFunction	= replacementString.wasSuccessful() ? null : FunctionCaster.cast( obj );

		switch ( scope ) {
			case "one" -> {
				String	lowerCaseString	= string.toLowerCase();
				int		idx				= lowerCaseString.indexOf( substring1.toLowerCase() );
				if ( idx == -1 ) {
					return string;
				}
				return replacementString.wasSuccessful()
				    ? string.substring( 0, idx ) + replacementString.get() + string.substring( idx + substring1.length() )
				    : string.substring( 0, idx )
				        + context.invokeFunction( replacementFunction,
				            new Object[] { string.substring( idx, idx + substring1.length() ), idx + 1, string } )
				        + string.substring( idx + substring1.length() );
			}
			case "all" -> {
				String			lowerCaseString		= string.toLowerCase();
				String			lowerCaseSubstring	= substring1.toLowerCase();
				int				substringLength		= substring1.length();
				String			replacement			= replacementString.wasSuccessful() ? replacementString.get() : null;
				StringBuilder	result				= new StringBuilder();
				int				totalLength			= string.length();
				int				i					= 0;
				while ( i < totalLength ) {
					// If what's left of the target can't hold a full match, copy the tail in one shot and stop
					if ( i + substringLength > totalLength ) {
						result.append( string, i, totalLength );
						break;
					}
					// In-place compare at offset i (no substring() allocation); both sides are already lower-cased
					if ( lowerCaseString.startsWith( lowerCaseSubstring, i ) ) {
						result.append( replacement != null
						    ? replacement
						    : context.invokeFunction( replacementFunction,
						        new Object[] { string.substring( i, i + substringLength ), i + 1, string } ) );
						i += substringLength;
					} else {
						result.append( string.charAt( i ) );
						i++;
					}
				}
				return result.toString();
			}
			default -> throw new BoxRuntimeException( "Invalid replacement scope: [" + scope + "]. Valid options are 'one' or 'all'." );
		}
	}

}
