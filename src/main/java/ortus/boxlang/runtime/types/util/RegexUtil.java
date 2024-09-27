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
package ortus.boxlang.runtime.types.util;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexUtil {

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
		// Regular expression to match valid quantifiers like {2,4}, {3}, {,5}, etc.
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
			// Append text between matches and escape curly braces in that portion
			String betweenMatches = input.substring( lastIndex, matcher.start() )
			    .replace( "{", "\\{" )
			    .replace( "}", "\\}" );

			escapedString.append( betweenMatches );

			// Append the valid quantifier without modification
			escapedString.append( matcher.group() );

			// Update lastIndex to the end of the current match
			lastIndex = matcher.end();
		}

		// Append and escape any remaining text after the last match
		escapedString.append( input.substring( lastIndex ).replace( "{", "\\{" ).replace( "}", "\\}" ) );

		return escapedString.toString();
	}

}
