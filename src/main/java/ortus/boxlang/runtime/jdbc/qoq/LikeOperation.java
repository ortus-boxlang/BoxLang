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
package ortus.boxlang.runtime.jdbc.qoq;

import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import ortus.boxlang.runtime.types.exceptions.DatabaseException;

/**
 * Implements the LIKE operation for SQL QoQ
 */
public class LikeOperation {

	/**
	 * regex metachars
	 */
	private static final Set<Character>	specials	= Set.of( '{', '}', '[', ']', '(', ')', '.', '?', '+', '\\', '^', '$', '*', '|' );

	/**
	 * Cache of compiled patterns
	 */
	private static Map<String, Pattern>	patterns	= new WeakHashMap<String, Pattern>();

	/**
	 * Invoke a SQL Like operation.
	 * 
	 * @param stringToSearchIn   The string to search in
	 * @param patternToSearchFor The pattern to search for
	 * @param escape             The escape character
	 * 
	 * @return true if the pattern matches the string
	 */
	public static boolean invoke( String stringToSearchIn, String patternToSearchFor, String escape ) {
		if ( stringToSearchIn == null || patternToSearchFor == null ) {
			return false;
		}

		stringToSearchIn	= stringToSearchIn.toLowerCase();
		patternToSearchFor	= patternToSearchFor.toLowerCase();
		escape				= escape == null ? null : escape.toLowerCase();
		Pattern p = createPattern( patternToSearchFor, escape == null ? null : escape );
		return p.matcher( stringToSearchIn ).matches();
	}

	/**
	 * Escape any actual regex metachars in the pattern
	 * 
	 * @param sb The string builder to append to
	 * @param c  The char to append
	 */
	private static void escapeForRegex( StringBuilder sb, char c ) {
		// If we have a regex metachar, escape it
		if ( specials.contains( c ) ) {
			sb.append( '\\' ).append( c );
		} else {
			sb.append( c );
		}
	}

	/**
	 * Create a pattern from the patternToSearchFor
	 */
	private static Pattern createPattern( String patternToSearchFor, String escape ) {
		var	patternCacheKey	= patternToSearchFor + escape == null ? "" : escape;
		var	pattern			= patterns.get( patternCacheKey );
		if ( pattern != null )
			return pattern;
		// Thread-safe compilation so only one thread compiles a pattern
		synchronized ( LikeOperation.class ) {
			// Double check in the lock
			pattern = patterns.get( patternCacheKey );
			if ( pattern != null )
				return pattern;

			char esc = 0;
			if ( escape != null && !escape.isEmpty() ) {
				esc = escape.charAt( 0 );
				if ( escape.length() > 1 ) {
					throw new DatabaseException(
					    "Invalid escape character [" + escape + "] has been specified in a LIKE conditional.  Escape char must be a single character." );
				}
			}

			StringBuilder	sb	= new StringBuilder( patternToSearchFor.length() );
			int				len	= patternToSearchFor.length();
			char			c;
			for ( int i = 0; i < len; i++ ) {
				c = patternToSearchFor.charAt( i );
				if ( c == esc ) {
					// If we aren't at the end of the string grab the next char
					// An escape char at the end of the string gets used as a literal
					if ( i + 1 < len ) {
						c = patternToSearchFor.charAt( ++i );
					}
					escapeForRegex( sb, c );
				} else {
					if ( c == '%' )
						sb.append( ".*" );
					else if ( c == '_' )
						sb.append( '.' );
					else if ( c == '[' ) {
						sb.append( c );
						// If we just opened unescaped brackets, check for a ^
						// All other ^ chars are treated like normal
						if ( i + 1 < len && patternToSearchFor.charAt( i + 1 ) == '^' ) {
							i++;
							sb.append( '^' );
						}
					} else if ( c == ']' )
						sb.append( c );
					else
						escapeForRegex( sb, c );
				}

			}
			try {
				patterns.put( patternCacheKey, pattern = Pattern.compile( sb.toString(), Pattern.DOTALL ) );
			} catch ( PatternSyntaxException e ) {
				throw new DatabaseException( "Invalid LIKE pattern [" + patternToSearchFor + "] has been specified in a LIKE conditional", e );
			}
			return pattern;
		}
	}

}