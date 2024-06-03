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

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.fasterxml.jackson.jr.ob.JSON;

/**
 * A collection of string utility functions
 */
public class StringUtil {

	public static final String			NEW_LINE					= System.lineSeparator();
	public static final String			TAB							= "\t";
	public static final List<String>	SQL_KEYWORDS				= List.of(
	    "ALTER TABLE",
	    "CREATE TABLE",
	    "CASE",
	    "NULLIF",
	    "DELETE",
	    "DROP TABLE",
	    "FROM",
	    "GROUP BY",
	    "HAVING",
	    "INSERT INTO",
	    "LIMIT",
	    "ORDER BY",
	    "OFFSET",
	    "SELECT",
	    "UNION",
	    "UPDATE",
	    "WHERE"
	);
	public static final String			SQL_KEYWORDS_REGEX			= String.join( "|", SQL_KEYWORDS );
	public static final List<String>	SQL_INDENTED_KEYWORDS		= List.of(
	    "FULL JOIN",
	    "INNER JOIN",
	    "JOIN",
	    "LEFT JOIN",
	    "OUTER JOIN",
	    "LIKE",
	    "BETWEEN",
	    "IS NULL",
	    "IS NOT NULL",
	    "EXISTS",
	    "DISTINCT",
	    "UNION ALL",
	    "UNION",
	    "INTERSECT",
	    "MINUS",
	    "EXCEPT"
	);
	public static final String			SQL_INDENTED_KEYWORDS_REGEX	= String.join( "|", SQL_INDENTED_KEYWORDS );
	public static final List<String>	SQL_OPERATORS				= List.of(
	    "\\+", "\\-", "\\*", "\\/", "\\%", "\\=", "\\<", "\\>", "\\<\\=", "\\>\\=", "\\<\\>", "\\!\\="
	);
	public static final String			SQL_OPERATORS_REGEX			= String.join( "|", SQL_OPERATORS );
	public static final List<String>	SQL_LOGICAL_OPERATORS		= List.of(
	    "AND", "OR", "NOT"
	);
	public static final String			SQL_LOGICAL_OPERATORS_REGEX	= String.join( "|", SQL_LOGICAL_OPERATORS );
	public static final String			INDENT						= "  ";

	/**
	 * Slugify a string for URL Safety
	 *
	 * @param str       Target to slugify
	 * @param maxLength The maximum number of characters for the slug
	 * @param allow     a regex safe list of additional characters to allow
	 */
	public static String slugify( String str ) {
		return slugify( str, 0, "" );
	}

	/**
	 * Slugify a string for URL Safety
	 *
	 * @param str       Target to slugify
	 * @param maxLength The maximum number of characters for the slug
	 * @param allow     a regex safe list of additional characters to allow
	 */
	public static String slugify( String str, int maxLength, String allow ) {
		// Replace multiple spaces with a single space
		String slug = str.trim().toLowerCase()
		    .replace( "\\s+", "-" )
		    .replaceAll( "ä", "ae" )
		    .replaceAll( "ü", "ue" )
		    .replaceAll( "ö", "oe" )
		    .replaceAll( "ß", "ss" );

		// More cleanup
		slug = slug.toLowerCase().replaceAll( "[^a-z0-9" + allow + "]", "-" ).replaceAll( "-+", "-" );

		// is there a max length restriction
		if ( maxLength != 0 && slug.length() > maxLength ) {
			slug = slug.substring( 0, maxLength );
		}

		return slug;
	}

	/**
	 * Pretty print an incoming JSON string using the Jackson JR library
	 *
	 * @param target The target JSON string to prettify
	 *
	 * @return The prettified JSON string
	 */
	public static String prettyJson( String target ) {
		try {
			// Parse the JSON string into an Object
			Object jsonObject = JSON.std.anyFrom( target );
			// Serialize the Object back into a pretty-printed JSON string
			return JSON.std.with( JSON.Feature.PRETTY_PRINT_OUTPUT ).asString( jsonObject );
		} catch ( Exception e ) {
			return target;
		}
	}

	/**
	 * Format an incoming sql string to a pretty version
	 *
	 * @param target The target sql to prettify
	 *
	 * @return The prettified sql
	 */
	public static String prettySql( String target ) {
		return target.lines()
		    // trim it
		    .map( String::trim )
		    // comma spacing
		    .map( item -> item.replaceAll( "\\s*(?![^()]*\\))(,)\\s*", "," + NEW_LINE + INDENT ) )
		    // parenthesis spacing
		    .map( item -> item.replaceAll( "\\((\\w|\\'|\"|\\`)", "( $1" ) )
		    .map( item -> item.replaceAll( "(\\w|\\'|\"|\\`)\\)", "$1 )" ) )
		    // Keyword spacing
		    .map( item -> item.replaceAll( "(?i)(\s)*(" + SQL_KEYWORDS_REGEX + ")(\s)+", NEW_LINE + "$2" + NEW_LINE + INDENT ).toUpperCase() )
		    // Indented Keyword spacing
		    .map( item -> item.replaceAll( "(?i)(" + SQL_INDENTED_KEYWORDS_REGEX + ")", NEW_LINE + INDENT + "$1" ).toUpperCase() )
		    // Add a line break after a SQL_LOGICAL_OPERATORS and upper case the logical operator
		    .map( item -> item.replaceAll( "(?i)(" + SQL_LOGICAL_OPERATORS_REGEX + ")", "$1" + NEW_LINE ).toUpperCase() )

		    // Add spacing before an after a SQL_OPERATORS_REGEX
		    .map( item -> item.replaceAll( "(?i)(" + SQL_OPERATORS_REGEX + ")", " $1 " ) )
		    // Collect to a list of strings with a newline for each line
		    .collect( StringBuilder::new, ( sb, s ) -> sb.append( s ).append( NEW_LINE ), StringBuilder::append )
		    .toString();
	}

	/**
	 * Convert a string to camel case using a functional approach.
	 *
	 * @param target The string to convert to camel case.
	 *
	 * @return The string in camel case.
	 */
	public static String camelCase( String target ) {
		// Replace underscores and hyphens with spaces
		String		replacedString	= target.replace( "_", " " ).replace( "-", " " );
		String[]	words			= replacedString.split( "\\s+" );

		// Process the array to form the camel case string
		return IntStream.range( 0, words.length )
		    .mapToObj( i -> i == 0 ? words[ i ].toLowerCase() : ucFirst( words[ i ].toLowerCase() ) )
		    .collect( Collectors.joining() );
	}

	/**
	 * Uppercase the first letter of a string
	 *
	 * @param target The target string to uppercase the first letter
	 *
	 * @return The string with the first letter uppercased
	 */
	public static String ucFirst( String target ) {
		return target.substring( 0, 1 ).toUpperCase() + target.substring( 1 );
	}

	/**
	 * Lowercase the first letter of a string
	 *
	 * @param target The target string to lowercase the first letter
	 *
	 * @return The string with the first letter lowercased
	 */
	public static String lcFirst( String target ) {
		return target.substring( 0, 1 ).toLowerCase() + target.substring( 1 );
	}

	/**
	 * Create kebab-case from a string
	 *
	 * @param target The target string to convert to kebab-case
	 *
	 * @return The string in kebab-case
	 */
	public static String kebabCase( String target ) {
		return target.toLowerCase().replaceAll( "\\s+", "-" );
	}

	/**
	 * Create snake_case from a string
	 *
	 * @param target The target string to convert to snake_case
	 *
	 * @return The string in snake_case
	 */
	public static String snakeCase( String target ) {
		return target.toLowerCase().replaceAll( "\\s+", "_" );
	}

	/**
	 * Create pascal case from a string
	 *
	 * @param target The target string to convert to pascal case
	 *
	 * @return The string in pascal case
	 */
	public static String pascalCase( String target ) {
		return ucFirst( camelCase( target ) );
	}

	/**
	 * Pluralize an English word based on standard rules.
	 *
	 * @param word The word to pluralize.
	 *
	 * @return The pluralized word.
	 */
	public static String pluralize( String word ) {
		String result = word;

		if ( result.endsWith( "s" ) ) {
			if ( result.endsWith( "ss" ) || result.endsWith( "us" ) ) {
				result += "es";
			} else {
				result += "s";
			}
		} else if ( result.endsWith( "y" ) ) {
			String			lastTwoChars	= result.length() > 1 ? result.substring( result.length() - 2 ).toLowerCase() : "";
			List<String>	suffixes		= Arrays.asList( "ay", "ey", "iy", "oy", "uy" );
			if ( suffixes.contains( lastTwoChars ) ) {
				result += "s";
			} else {
				result = result.substring( 0, result.length() - 1 ) + "ies";
			}
		} else if ( endsWithAny( result, "x", "s", "z", "ch", "sh" ) ) {
			result += "es";
		} else {
			result += "s";
		}

		return result;
	}

	/**
	 * Convert a plural word to a singular word.
	 *
	 * @param word The word to convert.
	 *
	 * @return The singular form of the word.
	 */
	public static String singularize( String word ) {
		String result = word;

		if ( result.endsWith( "s" ) ) {
			if ( result.endsWith( "sses" ) || result.endsWith( "uses" ) ) {
				result = result.substring( 0, result.length() - 2 );
			} else if ( result.endsWith( "ies" ) ) {
				result = result.substring( 0, result.length() - 3 ) + "y";
			} else if ( result.endsWith( "es" ) ) {
				if ( result.length() > 3 && endsWithAny( result, "shes", "ches" ) ) {
					result = result.substring( 0, result.length() - 2 );
				} else {
					result = result.substring( 0, result.length() - 1 );
				}
			} else {
				result = result.substring( 0, result.length() - 1 );
			}
		}

		return result;
	}

	/**
	 * Check if a string ends with any of the specified suffixes.
	 *
	 * @param word     The word to check.
	 * @param suffixes The suffixes to check for.
	 *
	 * @return True if the word ends with any of the suffixes, otherwise false.
	 */
	private static boolean endsWithAny( String word, String... suffixes ) {
		for ( String suffix : suffixes ) {
			if ( word.endsWith( suffix ) ) {
				return true;
			}
		}
		return false;
	}

}
