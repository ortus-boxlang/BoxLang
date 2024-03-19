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
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.validation.Validator;

@BoxBIF
@BoxBIF( alias = "reFindNoCase" )
@BoxMember( type = BoxLangType.STRING, name = "reFind", objectArgument = "string" )
@BoxMember( type = BoxLangType.STRING, name = "reFindNoCase", objectArgument = "string" )
public class ReFind extends BIF {

	private static final Key reFindNoCase = Key.of( "reFindNoCase" );

	/**
	 * Constructor
	 */
	public ReFind() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "string", Key.reg_expression ),
		    new Argument( true, "string", Key.string ),
		    new Argument( false, "integer", Key.start, 1 ),
		    new Argument( false, "boolean", Key.returnSubExpressions, false ),
		    new Argument( false, "string", Key.scope, "one", Set.of( Validator.valueOneOf( "one", "all" ) ) )
		};
	}

	/**
	 * 
	 * Uses a regular expression (RE) to search a string for a pattern, starting from a specified position. The search is case-sensitive.
	 * It will return numeric if returnsubexpressions is false and a struct of arrays named "len", "match" and "pos" when returnsubexpressions is true.
	 * 
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 * 
	 * @argument.reg_expression The regular expression to search for
	 * 
	 * @argument.string The string to serach in
	 * 
	 * @argument.start The position from which to start searching in the string. Default is 1.
	 * 
	 * @argument.returnSubExpressions True: if the regular expression is found, the first array element contains the length and position, respectively, of
	 *                                the first match. If the regular expression contains parentheses that group subexpressions, each subsequent array
	 *                                element contains the length and position, respectively, of the first occurrence of each group. If the regular
	 *                                expression is not found, the arrays each contain one element with the value 0. False: the function returns the
	 *                                position in the string where the match begins. Default.
	 * 
	 * @argument.scope "one": returns the first value that matches the regex. "all": returns all values that match the regex.
	 * 
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		String	reg_expression			= arguments.getAsString( Key.reg_expression );
		String	string					= arguments.getAsString( Key.string );
		Integer	start					= arguments.getAsInteger( Key.start );
		Boolean	returnSubExpressions	= arguments.getAsBoolean( Key.returnSubExpressions );
		String	scope					= arguments.getAsString( Key.scope ).toLowerCase();
		boolean	noCase					= arguments.get( BIF.__functionName ).equals( reFindNoCase );

		if ( noCase ) {
			reg_expression = "(?i)" + reg_expression;
		}

		// Check if the start position is within valid bounds
		if ( start < 1 ) {
			// CF turns negative start into 1. Ugh, but ok.
			start = 1;
		}

		// Find the first occurrence of the substring from the specified start position
		Matcher matcher = java.util.regex.Pattern.compile( reg_expression ).matcher( string );
		if ( start > 1 ) {
			matcher.region( start - 1, string.length() );
		}
		Array result = new Array();

		while ( matcher.find() ) {

			int		groupCount	= matcher.groupCount();
			Array	lenArray	= Array.of( matcher.group().length() );
			Array	matchArray	= Array.of( matcher.group() );
			Array	posArray	= Array.of( matcher.start() + 1 );
			// Add remaining groups
			for ( int i = 1; i <= groupCount; i++ ) {
				lenArray.add( matcher.group( i ).length() );
				matchArray.add( matcher.group( i ) );
				posArray.add( matcher.start( i ) + 1 );
			}
			result.add( Struct.of(
			    Key.len, lenArray,
			    Key.match, matchArray,
			    Key.pos, posArray
			) );
			if ( scope.equals( "one" ) ) {
				break;
			}
		}

		if ( returnSubExpressions ) {
			if ( result.size() == 0 ) {
				if ( scope.equals( "all" ) ) {
					return Array.of(
					    Struct.of(
					        Key.len, Array.of( 0 ),
					        Key.match, Array.of( "" ),
					        Key.pos, Array.of( 0 )
					    ) );
				} else {
					return Struct.of(
					    Key.len, Array.of( 0 ),
					    Key.match, Array.of( "" ),
					    Key.pos, Array.of( 0 )
					);
				}
			} else if ( scope.equals( "one" ) ) {
				return result.get( 0 );
			} else {
				return result;
			}
		} else {
			if ( result.size() == 0 ) {
				return 0;
			} else if ( scope.equals( "one" ) ) {
				return ( ( Struct ) result.get( 0 ) ).getAsArray( Key.pos ).get( 0 );
			} else {
				// return array of just the positions
				Array positions = new Array();
				for ( int i = 0; i < result.size(); i++ ) {
					positions.add( ( ( Struct ) result.get( i ) ).getAsArray( Key.pos ).get( 0 ) );
				}
				return positions;
			}
		}

	}

}
