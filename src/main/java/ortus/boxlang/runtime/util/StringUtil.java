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
package ortus.boxlang.runtime.util;

import java.util.List;

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

}
