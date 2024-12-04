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

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.scopes.Key;

/**
 * This class can be used as a utility to handle regular expressions.
 * Add as many compiled patterns as needed and make sure you give them a meaningful name.
 */
public class RegexBuilder {

	/**
	 * Pattern Dictionary
	 * Add as many patterns as needed, but make sure they are in all caps and alphabetically ordered.
	 */
	public static final Pattern	ALPHA					= Pattern.compile( "^[a-zA-Z]*$" );
	public static final Pattern	CFC_OR_BX_FILE			= Pattern.compile( ".*\\.(cfc|bx)$" );
	public static final Pattern	BACKSLASH				= Pattern.compile( "\\\\" );
	public static final Pattern	CARRIAGE_RETURN			= Pattern.compile( "\\r" );
	public static final Pattern	CF_SQL					= Pattern.compile( "(?i)CF_SQL_" );
	public static final Pattern	COLON					= Pattern.compile( ":" );
	public static final Pattern	CREDIT_CARD_NUMBERS		= Pattern.compile( "[0-9 ,_-]+" );
	public static final Pattern	EMAIL					= Pattern.compile( "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$" );
	public static final Pattern	END_OF_LINE_COLONS		= Pattern.compile( ":+$" );
	public static final Pattern	JAVA_PACKAGE			= Pattern.compile( "(?i)(java|javax)\\\\..*" );
	public static final Pattern	LINE_ENDINGS			= Pattern.compile( "\\r?\\n" );
	public static final Pattern	MULTILINE_START_OF_LINE	= Pattern.compile( "(?m)^" );
	public static final Pattern	MULTIPLE_SPACES			= Pattern.compile( "\\s+" );
	public static final Pattern	NO_DIGITS				= Pattern.compile( "\\D" );
	public static final Pattern	NON_ALPHA				= Pattern.compile( "[^a-zA-Z]" );
	public static final Pattern	NON_ALPHANUMERIC		= Pattern.compile( "[^a-zA-Z0-9]" );
	public static final Pattern	NUMBERS					= Pattern.compile( "^-?\\d+(\\.\\d+)?$" );
	public static final Pattern	PACKAGE_NAMES			= Pattern.compile( "[^a-zA-Z0-9$\\.]" );
	public static final Pattern	PERIOD					= Pattern.compile( "\\." );
	public static final Pattern	REGEX_META				= Pattern.compile( "([\\\\$])" );
	public static final Pattern	REGEX_QUANTIFIER		= Pattern.compile( "\\{\\d*,?\\d*\\}" );
	public static final Pattern	REGEX_QUANTIFIER_END	= Pattern.compile( "(?<!\\\\)\\}" );
	public static final Pattern	REGEX_QUANTIFIER_START	= Pattern.compile( "(?<!\\\\)\\{" );
	public static final Pattern	SLASH					= Pattern.compile( "/" );
	public static final Pattern	SQL_COMMA_SPACING		= Pattern.compile( "\\s*(?![^()]*\\))(,)\\s*" );
	public static final Pattern	SQL_PARENTHESIS_END		= Pattern.compile( "(\\w|\\'|\"|\\`)\\)" );
	public static final Pattern	SQL_PARENTHESIS_START	= Pattern.compile( "\\((\\w|\\'|\"|\\`)" );
	public static final Pattern	STARTS_WITH_DIGIT		= Pattern.compile( "^\\d.*" );
	public static final Pattern	SSN						= Pattern.compile( "^(?!219099999|078051120)(?!666|000|9\\d{2})\\d{3}(?!00)\\d{2}(?!0{4})\\d{4}$" );
	public static final Pattern	TIMESTAMP				= Pattern.compile( "^\\{ts ([^\\}]*)\\}" );
	public static final Pattern	TELEPHONE				= Pattern.compile(
	    "^(?:(?:\\+?1\\s*(?:[.-]\\s*)?)?(?:\\(\\s*([2-9]1[02-9]|[2-9][02-8]1|[2-9][02-8][02-9])\\s*\\)|([2-9]1[02-9]|[2-9][02-8]1|[2-9][02-8][02-9]))\\s*(?:[.-]\\s*)?)?([2-9]1[02-9]|[2-9][02-9]1|[2-9][02-9]{2})\\s*(?:[.-]\\s*)?([0-9]{4})(?:\\s*(?:#|x\\.?|ext\\.?|extension)\\s*(\\d+))?$" );
	public static final Pattern	TWO_DOTS				= Pattern.compile( "\\.{2}" );
	public static final Pattern	TWENTY_FOUR_HOUR_TIME	= Pattern.compile( "^([0-1][\\d]|2[0-3]):[0-5][\\d]$" );
	public static final Pattern	URL						= Pattern.compile( "^(https?|ftp|file)://([A-Za-z0-90.]*)/?([-a-zA-Z0-9.+&@#/]+)?(\\??[^\\s]*)$" );
	public static final Pattern	UPPERCASE_GROUP			= Pattern.compile( "([A-Z])" );
	public static final Pattern	UUID_V4					= Pattern
	    .compile( "[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-4[a-fA-F0-9]{3}-[89abAB][a-fA-F0-9]{3}-[a-fA-F0-9]{12}" );
	public static final Pattern	UUID_PATTERN			= Pattern
	    .compile( "[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-4[a-fA-F0-9]{3}-[89abAB][a-fA-F0-9]{3}[a-fA-F0-9]{12}" );
	public static final Pattern	VOWELS					= Pattern.compile( "^[aeiou].*" );
	public static final Pattern	VALID_VARIABLENAME		= Pattern.compile( "^[a-zA-Z_][a-zA-Z0-9_]*$" );
	public static final Pattern	WHITESPACE				= Pattern.compile( "\\s" );
	public static final Pattern	ZIPCODE					= Pattern.compile( "\\d{5}([ -]?\\d{4})?" );

	/**
	 * Build a matcher for the given pattern lookup
	 *
	 * @param input The input string to match against
	 *
	 * @return A new matcher instance
	 */
	public static RegexMatcher of( String input ) {
		return new RegexMatcher( input );
	}

	/**
	 * Build a matcher for the given input and string pattern
	 *
	 * @param input   The input string to match against
	 * @param pattern The pattern to match against
	 * @param noCase  Whether the pattern should be case insensitive or not
	 *
	 * @return A new matcher instance
	 */
	public static RegexMatcher of( String input, String pattern, Boolean noCase ) {
		return new RegexMatcher( input ).match( pattern, noCase );
	}

	/**
	 * Build a matcher for the given input and string pattern
	 *
	 * @param input   The input string to match against
	 * @param pattern The pattern to match against
	 *
	 * @return A new matcher instance
	 */
	public static RegexMatcher of( String input, String pattern ) {
		return new RegexMatcher( input ).match( pattern );
	}

	/**
	 * Build a matcher for the given input and string pattern
	 *
	 * @param input   The input string to match against
	 * @param pattern The pattern to match against
	 *
	 * @return A new matcher instance
	 */
	public static RegexMatcher of( String input, Pattern pattern ) {
		return new RegexMatcher( input ).match( pattern );
	}

	/**
	 * Conmmonly used utility to strip whitespace from a string
	 */
	public static String stripWhitespace( String input ) {
		return of( input, WHITESPACE ).replaceAllAndGet( "" );
	}

	public static String stripCarriageReturns( String input ) {
		return of( input, CARRIAGE_RETURN ).replaceAllAndGet( "" );
	}

	/**
	 * The RegexMatcher class is used to match a pattern against an input string
	 * by either using a pre-complied pattern or a string pattern that will be compiled on the fly.
	 */
	public static class RegexMatcher {

		private Pattern			pattern;
		private String			input;
		private final String	original;

		/**
		 * Create a new matcher instance
		 *
		 * @param input The input string to match against
		 */
		private RegexMatcher( String input ) {
			this.input		= input;
			this.original	= input;
		}

		/**
		 * Compile the pattern from the given pattern
		 *
		 * @param pattern The pattern to compile
		 *
		 * @return The matcher instance
		 */
		public RegexMatcher match( Pattern pattern ) {
			this.pattern = pattern;
			return this;
		}

		/**
		 * Compile the pattern from the given string with case sensitivity
		 *
		 * @param pattern The pattern to compile
		 *
		 * @return The matcher instance
		 */
		public RegexMatcher match( String pattern ) {
			return this.match( pattern, false );
		}

		/**
		 * Compile the pattern from the given string
		 *
		 * @param pattern The pattern to compile
		 * @param noCase  Whether the pattern should be case insensitive or not
		 *
		 * @return The matcher instance
		 */
		public RegexMatcher match( String pattern, Boolean noCase ) {
			Objects.requireNonNull( pattern, "Pattern cannot be null" );
			if ( pattern.isEmpty() ) {
				throw new IllegalArgumentException( "Pattern cannot be empty" );
			}

			// Lookup or compile the pattern into the regex cache
			String cacheKey = EncryptionUtil.hash( pattern );
			this.pattern = ( Pattern ) BoxRuntime.getInstance()
			    .getCacheService()
			    .getCache( Key.bxRegex )
			    .getOrSet( cacheKey, () -> Pattern.compile( pattern, noCase ? Pattern.CASE_INSENSITIVE : 0 ) );

			return this;
		}

		/**
		 * Check if the input string matches the pattern
		 *
		 * @return True if the input string matches the pattern, false otherwise
		 */
		public Boolean matches() {
			return this.pattern.matcher( this.input ).matches();
		}

		/**
		 * Get the matcher instance for the input string and pattern
		 *
		 * @return The matcher instance
		 */
		public Matcher matcher() {
			return this.pattern.matcher( this.input );
		}

		/**
		 * Replace all occurrences of the pattern in the input string with the replacement string
		 *
		 * @param replacement The replacement string
		 *
		 * @return The input string with all occurrences of the pattern replaced with the replacement string
		 */
		public RegexMatcher replaceAll( String replacement ) {
			Objects.requireNonNull( replacement, "Replacement cannot be null" );
			this.input = this.pattern
			    .matcher( this.input )
			    .replaceAll( replacement );

			return this;
		}

		/**
		 * Replace all occurrences of the pattern in the input string with the replacement string
		 *
		 * @param pattern     The pattern to match against
		 * @param replacement The replacement string
		 *
		 * @return The input string with all occurrences of the pattern replaced with the replacement string
		 */
		public RegexMatcher replaceAll( Pattern pattern, String replacement ) {
			Objects.requireNonNull( pattern, "Pattern cannot be null" );
			Objects.requireNonNull( replacement, "Replacement cannot be null" );
			this.input = pattern
			    .matcher( this.input )
			    .replaceAll( replacement );
			return this;
		}

		/**
		 * Replace all occurrences of the pattern in the input string with the replacement string
		 *
		 * @param replacement The replacement string
		 *
		 * @return The input string with all occurrences of the pattern replaced with the replacement string
		 */
		public String replaceAllAndGet( String replacement ) {
			return this.replaceAll( replacement ).get();
		}

		/**
		 * Replace all occurrences of the pattern in the input string with the replacement string
		 *
		 * @param replacement The replacement string
		 *
		 * @return The input string with all occurrences of the pattern replaced with the replacement string
		 */
		public String replaceAllAndGet( Pattern pattern, String replacement ) {
			return this.replaceAll( pattern, replacement ).get();
		}

		/**
		 * Get's the input string as modified by the matcher
		 *
		 * @return The input string
		 */
		public String get() {
			return this.input;
		}

		/**
		 * Reset the input string to the original value
		 *
		 * @return The matcher instance
		 */
		public RegexMatcher reset() {
			this.input = this.original;
			return this;
		}

	}

}
