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
package ortus.boxlang.runtime.config.util;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.ApplicationException;

/**
 * A helper class for resolving placeholders in configuration files
 * <p>
 * Placeholders are defined as {@code {placeholder-name}} and can be used in
 * configuration files to reference system properties or other values.
 * <p>
 */
public class PlaceholderHelper {

	/**
	 * The pattern to match placeholder patterns like "${...}"
	 */
	private static final Pattern	PLACEHOLDER_PATTERN	= Pattern.compile( "(?i)\\$\\{([^:}]+)(?::([^}]+))?\\}" );

	/**
	 * Core Replacements
	 */
	private static final Struct		PLACEHOLDER_MAP		= new Struct();
	static {
		// Add default core replacements
		PLACEHOLDER_MAP.put( "user-home", escapeReplacementMetaChars( System.getProperty( "user.home" ) ) );
		PLACEHOLDER_MAP.put( "java-temp", escapeReplacementMetaChars( System.getProperty( "java.io.tmpdir" ) ) );
		// Add additional replacements here
		// placeholderMap.put("your-placeholder", "replacement-value");
	}

	/**
	 * Resolve the input string and replace all placeholders with their values
	 * from the incoming placeholder map.
	 *
	 * <p>
	 * <strong>This method doesn't use the core replacement map, but the passed map</strong>
	 * </p>
	 *
	 * @param input The input string to Resolve
	 * @param map   The placeholder map to use for resolving the input string
	 *
	 * @return The Resolved string
	 */
	public static String resolve( String input, Struct map ) {
		// Create a pattern to match placeholder patterns like "${...}"
		Matcher matcher = PLACEHOLDER_PATTERN.matcher( input );

		// Replace all placeholders with their values
		return matcher.replaceAll( matchResult -> {
			String	placeholder		= matchResult.group( 1 );
			String	defaultValue	= matchResult.group( 2 );
			String	replacement		= ( String ) map.getOrDefault( placeholder, defaultValue != null ? defaultValue : matchResult.group() );

			return Matcher.quoteReplacement( replacement );
		} );
	}

	/**
	 * Resolve the input string and replace all placeholders with their values
	 * from the incoming placeholder map.
	 *
	 * <p>
	 * <strong>This method doesn't use the core replacement map, but the passed map</strong>
	 * </p>
	 *
	 * @param input The input string to Resolve
	 * @param map   The placeholder map to use for resolving the input string
	 *
	 * @return The Resolved string
	 */
	public static String resolve( String input, Map<String, String> map ) {
		return resolve( input, new Struct( map ) );
	}

	/**
	 * Resolve the input string and replace all placeholders with CORE values
	 * using the incoming placeholder map and an Object which will be cast to a
	 * String using the BoxLang rules
	 *
	 * @param input The Object to Resolve, which we will try to cast to a string
	 * @param map   The placeholder map to use for resolving the input string
	 *
	 * @return The Resolved string
	 */
	public static String resolve( Object input, Map<String, String> map ) {
		return resolve( StringCaster.cast( input ), map );
	}

	/**
	 * Resolve the input string and replace all placeholders with CORE values
	 * using the incoming placeholder map and an Object which will be cast to a
	 * String using the BoxLang rules
	 *
	 * @param input The Object to Resolve, which we will try to cast to a string
	 * @param map   The placeholder struct to use for resolving the input string
	 *
	 * @return The Resolved string
	 */
	public static String resolve( Object input, Struct map ) {
		return resolve( StringCaster.cast( input ), map );
	}

	/**
	 * Resolve the input string and replace all placeholders with CORE values
	 *
	 * @param input The input string to Resolve
	 *
	 * @return The Resolved string
	 */
	public static String resolve( String input ) {
		return resolve( input, PLACEHOLDER_MAP );
	}

	/**
	 * Resolve the input string and replace all placeholders with CORE values using
	 * an Object which will be cast to a String using the BoxLang rules
	 *
	 * @param input The Object to Resolve
	 *
	 * @return The Resolved string
	 *
	 * @throws ApplicationException - If the input object cannot be cast to a String
	 */
	public static String resolve( Object input ) {
		return resolve( StringCaster.cast( input ) );
	}

	/**
	 * Escape meta characters in the replacement string. In Java, the replacement
	 * string is treated as a regular expression and meta characters like "$" or
	 * "\" can cause problems. This method escapes these meta characters.
	 *
	 * A single "\" needs to be escaped two times: 1 by Java, 1 by Regex => \\\\
	 *
	 * @param input The input string to escape
	 *
	 * @return The escaped string
	 */
	private static String escapeReplacementMetaChars( String input ) {
		return input.replaceAll( "([\\\\$])", "\\\\$1" );
	}

}
