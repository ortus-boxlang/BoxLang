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
package ortus.boxlang.runtime.cache.filters;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.exceptions.BoxValidationException;

/**
 * A filter that uses globbing style wildcards to filter cache keys case-sensitive or not.
 * Ex: hello*world, hello?world, hello[abc]world, hello[^abc]world, lui?, lui*, lui[abc]world, lui[^abc]world
 */
public class WildcardFilter implements ICacheKeyFilter {

	/**
	 * The pattern to use for the wildcard
	 */
	protected final Pattern		regexPattern;

	/**
	 * The special characters for the wildcard
	 */
	private static final String	SPECIALS	= "{}[]().+\\^$";

	/**
	 * Create a new widlcard filter with a case-insensitive widlcard
	 *
	 * @param wildcard The widlcard to use
	 */
	public WildcardFilter( String wildcard ) {
		this( wildcard, true );
	}

	/**
	 * Create a new wildcard filter
	 *
	 * @param wildcard   The wildcard to use
	 * @param ignoreCase Whether the wildcard should be case-sensitive
	 */
	public WildcardFilter( String wildcard, boolean ignoreCase ) {
		// Escape the wildcard
		String regex = wildcard;
		for ( char c : SPECIALS.toCharArray() ) {
			regex = regex.replace( String.valueOf( c ), "\\" + c );
		}
		// Replace the wildcards
		regex = regex.replace( "?", "." ).replace( "*", ".*" ).replace( "[!", "[^" );
		// Compile the regex
		try {
			regexPattern = Pattern.compile( regex, ignoreCase ? 0 : Pattern.CASE_INSENSITIVE );
		} catch ( PatternSyntaxException e ) {
			throw new BoxValidationException( "Invalid wildcard: " + wildcard, e );
		}
	}

	/**
	 * Apply the regex to the key
	 */
	@SuppressWarnings( "null" )
	@Override
	public boolean apply( Key input ) {
		return regexPattern.matcher( input.toString() ).matches();
	}
}
