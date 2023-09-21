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

/**
 * A helper class for resolving placeholders in configuration files
 * <p>
 * Placeholders are defined as {@code {placeholder-name}} and can be used in
 * configuration files to reference system properties or other values.
 * <p>
 */
public class PlaceholderHelper {

	private static final Map<String, String> PLACEHOLDER_MAP = new HashMap<>();

	static {
		// Add default replacements
		PLACEHOLDER_MAP.put( "user-home", System.getProperty( "user.home" ) );
		PLACEHOLDER_MAP.put( "java-temp", System.getProperty( "java.io.tmpdir" ) );
		// Add additional replacements here
		// placeholderMap.put("your-placeholder", "replacement-value");
	}

	/**
	 * Resolve the input string and replace all placeholders with their values
	 *
	 * @param input The input string to Resolve
	 *
	 * @return The Resolved string
	 */
	public static String resolve( String input ) {
		// Create a pattern to match placeholder patterns like "{...}"
		Pattern	pattern	= Pattern.compile( "\\{([^}]+)\\}" );
		// Use Matcher and the stream API to replace placeholders
		Matcher	matcher	= pattern.matcher( input );
		return matcher.replaceAll( match -> PLACEHOLDER_MAP.getOrDefault( match.group( 1 ), match.group() ) );
	}

}
