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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

	// POSIX Pattern
	private static final Pattern				POSIX_PATTERN			= Pattern.compile( "\\[(.*?)\\]" );
	private static final Pattern				POSIX_PATTERN_NOCASE	= Pattern.compile( "\\[(.*?)\\]", Pattern.CASE_INSENSITIVE );

	// Define POSIX character classes and their Java regex equivalents
	private static final Map<String, String>	POSIX_MAP				= new HashMap<>();
	static {
		POSIX_MAP.put( "[:alnum:]", "a-zA-Z0-9" );
		POSIX_MAP.put( "[:alpha:]", "a-zA-Z" );
		POSIX_MAP.put( "[:blank:]", " \\t" );
		POSIX_MAP.put( "[:cntrl:]", "\\x00-\\x1F\\x7F" );
		POSIX_MAP.put( "[:digit:]", "0-9" );
		POSIX_MAP.put( "[:graph:]", "\\x21-\\x7E" );
		POSIX_MAP.put( "[:lower:]", "a-z" );
		POSIX_MAP.put( "[:print:]", "\\x20-\\x7E" );
		POSIX_MAP.put( "[:punct:]", "!\"#$%&'()*+,-./:;<=>?@\\[\\]^_`{|}~" );
		POSIX_MAP.put( "[:space:]", "\\s" );
		POSIX_MAP.put( "[:upper:]", "A-Z" );
		POSIX_MAP.put( "[:xdigit:]", "0-9a-fA-F" );
	}

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
		boolean	noCase					= arguments.get( BIF.__functionName ).equals( Key.reFindNoCase );

		// Posix replacement for character classes
		reg_expression	= posixReplace( reg_expression, noCase );
		// Ignore non-quantifier curly braces like PERL
		reg_expression	= replaceNonQuantiferCurlyBraces( reg_expression );

		// Check if the start position is within valid bounds
		if ( start < 1 ) {
			// CF turns negative start into 1. Ugh, but ok.
			start = 1;
		}
		// Find the first occurrence of the substring from the specified start position
		Matcher matcher = java.util.regex.Pattern.compile( reg_expression, noCase ? Pattern.CASE_INSENSITIVE : 0 ).matcher( string );
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
				String targetGroup = matcher.group( i );

				// The group can be null on expressions where the group is not mandatory
				// Example: reFindNoCase( "^(f|x)?test$", methodName )
				if ( targetGroup == null ) {
					lenArray.add( 0 );
					matchArray.add( "" );
					posArray.add( 0 );
					continue;
				}

				// Add the length, match, and position of the group to the arrays
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
			if ( result.isEmpty() ) {
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
			if ( result.isEmpty() ) {
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

	/**
	 * Replace POSIX character classes with Java regex equivalents
	 *
	 * @param expression The regular expression to modify
	 * @param noCase     Whether to ignore case
	 *
	 * @return The modified regular expression
	 */
	public static String posixReplace( String expression, Boolean noCase ) {
		// Replace POSIX character classes with Java regex equivalents
		// Use a regex to find POSIX character classes in the regex
		Matcher posixMatcher = noCase ? POSIX_PATTERN_NOCASE.matcher( expression ) : POSIX_PATTERN.matcher( expression );

		// Return if no match
		if ( !posixMatcher.find() ) {
			return expression;
		}

		// Reset matcher to start from the beginning
		posixMatcher.reset();
		StringBuilder sb = new StringBuilder();
		// Replace each POSIX character class with its Java regex equivalent
		while ( posixMatcher.find() ) {
			String insideBrackets = posixMatcher.group( 1 ); // get the content inside the square brackets
			for ( Map.Entry<String, String> entry : POSIX_MAP.entrySet() ) {
				insideBrackets = insideBrackets.replace( entry.getKey(), entry.getValue() );
			}
			posixMatcher.appendReplacement( sb, Matcher.quoteReplacement( "[" + insideBrackets + "]" ) );
		}
		posixMatcher.appendTail( sb );

		// Replace POSIX character classes that are not inside square brackets
		String returnExpression = sb.toString();
		for ( Map.Entry<String, String> entry : POSIX_MAP.entrySet() ) {
			returnExpression = returnExpression.replace( entry.getKey(), "[" + entry.getValue() + "]" );
		}

		return returnExpression;
	}

	/**
	 * Perl regex allows abitrary curly braces in the regex. This function escapes the curly braces that are not part of valid quantifiers.
	 * Ex: {{foobar}}
	 * which is not a valid quantifier, will be escaped to \{\{foobar\}\}
	 * 
	 * @param input The regular expression string
	 * 
	 * @return The escaped regular expression string
	 */
	public static String replaceNonQuantiferCurlyBraces( String input ) {
		// String input = "Example regex with {{invalid}} and {valid{quantifiers}} like {2,4}";

		// Regular expression to match valid quantifiers
		String			quantifierRegex		= "\\{\\d*,?\\d*\\}";

		// Pattern to match valid quantifiers
		Pattern			quantifierPattern	= Pattern.compile( quantifierRegex );

		// Matcher for the input string
		Matcher			matcher				= quantifierPattern.matcher( input );

		// Create a StringBuilder to build the final output
		StringBuilder	escapedString		= new StringBuilder();

		// Index to keep track of the position in the input string
		int				lastIndex			= 0;

		while ( matcher.find() ) {
			// Append text between matches and the match itself
			escapedString.append( input, lastIndex, matcher.start() );
			escapedString.append( matcher.group() );

			// Update lastIndex to the end of the current match
			lastIndex = matcher.end();
		}

		// Append remaining text after the last match
		escapedString.append( input.substring( lastIndex ) );

		// Escape the remaining curly braces that are not part of valid quantifiers
		String finalResult = escapedString.toString().replaceAll( "\\{", "\\\\{" ).replaceAll( "\\}", "\\\\}" );

		return finalResult;
	}

}
