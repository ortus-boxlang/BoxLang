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
 * A filter that uses a regex pattern to filter cache keys case-sensitive
 */
public class RegexFilter implements ICacheKeyFilter {

	/**
	 * The pattern to use
	 */
	protected final Pattern regexPattern;

	/**
	 * Create a new regex filter with a case-insensitive regex
	 *
	 * @param regex The regex to use
	 */
	public RegexFilter( String regex ) {
		this( regex, true );
	}

	/**
	 * Create a new regex filter
	 *
	 * @param regex      The regex to use
	 * @param ignoreCase Whether the regex should be case-insensitive. Default is case-insensitive.
	 */
	public RegexFilter( String regex, boolean ignoreCase ) {
		try {
			regexPattern = Pattern.compile( regex, ignoreCase ? 0 : Pattern.CASE_INSENSITIVE );
		} catch ( PatternSyntaxException e ) {
			throw new BoxValidationException( "Invalid regex: " + regex, e );
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
