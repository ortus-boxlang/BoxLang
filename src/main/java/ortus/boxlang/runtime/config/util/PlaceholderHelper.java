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

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.util.RegexBuilder;

/**
 * A helper class for resolving placeholders in configuration files
 * <p>
 * Placeholders are defined as {@code {placeholder-name}} and can be used in
 * configuration files to reference system properties or other values.
 * </p>
 */
public class PlaceholderHelper {

	/**
	 * The pattern to match placeholder patterns like "${...}"
	 */
	private static final Pattern	PLACEHOLDER_PATTERN	= Pattern.compile( "(?i)\\$\\{([^:}]+)(?::([^}]+))?\\}" );

	/**
	 * Core Replacements
	 */
	private static final IStruct	PLACEHOLDER_MAP		= new Struct();
	static {
		// Add default core replacements
		PLACEHOLDER_MAP.put( "user-home", System.getProperty( "user.home" ) );
		PLACEHOLDER_MAP.put( "java-temp", System.getProperty( "java.io.tmpdir" ) );
		PLACEHOLDER_MAP.put( "user-dir", System.getProperty( "user.dir" ) );
		PLACEHOLDER_MAP.put( "boxlang-home", BoxRuntime.getInstance().getRuntimeHome().toString() );

		// Add all the environment variables as replacements, both with the legacy
		// "env." prefix and bare so `${env.FOO}` keeps working while `${FOO}` is also supported.
		Map<String, String> env = System.getenv();
		for ( Map.Entry<String, String> entry : env.entrySet() ) {
			PLACEHOLDER_MAP.put( "env." + entry.getKey(), entry.getValue() );
			PLACEHOLDER_MAP.put( entry.getKey(), entry.getValue() );
		}

		// Add all JVM system properties as replacements. These are added last with no prefix
		// so, for bare names, a system property always wins over an environment variable.
		System.getProperties().forEach( ( key, value ) -> PLACEHOLDER_MAP.put( key.toString(), value.toString() ) );
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
	public static String resolve( String input, IStruct map ) {
		return resolve( input, map, null );
	}

	/**
	 * Resolve the input string and replace all placeholders with their values
	 * from the incoming placeholder map, decrypting each replacement value with the
	 * supplied decryptor at the point of replacement. When the decryptor is null, no
	 * decryption occurs. This is used only by configuration-file processing so other
	 * placeholder call sites in the runtime remain unaffected.
	 *
	 * @param input     The input string to Resolve
	 * @param map       The placeholder map to use for resolving the input string
	 * @param decryptor The optional function applied to each replacement value, or null.
	 *
	 * @return The Resolved string
	 */
	public static String resolve( String input, IStruct map, java.util.function.Function<String, String> decryptor ) {
		// Create a pattern to match placeholder patterns like "${...}"
		Matcher matcher = PLACEHOLDER_PATTERN.matcher( input );

		// Replace all placeholders with their values
		return matcher.replaceAll( matchResult -> {
			String	placeholder		= matchResult.group( 1 );
			String	defaultValue	= matchResult.group( 2 );
			String	replacement		= StringCaster.cast( map.getOrDefault( placeholder, defaultValue != null ? defaultValue : matchResult.group() ) );

			if ( replacement == null ) {
				throw new BoxRuntimeException(
				    "Placeholder '" + placeholder + "' has no replacement value. Value values are " + map.asString() + ". Replacement code was: " + input );
			}

			// Optionally decrypt the replacement value at the point of replacement.
			if ( decryptor != null ) {
				replacement = decryptor.apply( replacement );
			}

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
	public static String resolve( Object input, IStruct map ) {
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
	 * @throws BoxRuntimeException - If the input object cannot be cast to a String
	 */
	public static String resolve( Object input ) {
		return resolve( StringCaster.cast( input ) );
	}

	/**
	 * Recursively replace all placeholders throughout a tree made up of Maps and Lists.
	 * Uses the default placeholder map
	 *
	 * @param object Object to populate into tree placeholders.
	 *
	 * @return The Resolved tree
	 *
	 */
	public static <T> T resolveAll( T object ) {
		return resolveAll( object, PLACEHOLDER_MAP );
	}

	/**
	 * Get the default placeholder map used for resolution. This is a shared instance and
	 * should not be mutated by callers; copy it before modification.
	 *
	 * @return The default placeholder map.
	 */
	public static IStruct getPlaceholderMap() {
		return PLACEHOLDER_MAP;
	}

	/**
	 * Recursively replace all placeholders throughout a tree made up of Maps and Lists.
	 * You can provide a custom placeholder map
	 *
	 * @param object Object to populate into tree placeholders.
	 * @param map    The placeholder struct to use for resolving the input string
	 *
	 * @return The Resolved tree
	 *
	 */
	@SuppressWarnings( "unchecked" )
	public static <T> T resolveAll( T object, IStruct map ) {
		return resolveAll( object, map, null );
	}

	/**
	 * Recursively replace all placeholders throughout a tree made up of Maps and Lists,
	 * optionally decrypting each replacement value with the supplied decryptor.
	 *
	 * @param object    Object to populate into tree placeholders.
	 * @param map       The placeholder struct to use for resolving the input string
	 * @param decryptor The optional function applied to each replacement value, or null.
	 *
	 * @return The Resolved tree
	 *
	 */
	@SuppressWarnings( "unchecked" )
	public static <T> T resolveAll( T object, IStruct map, java.util.function.Function<String, String> decryptor ) {
		if ( object instanceof Map<?, ?> rawMap ) {
			Map<Object, Object> configMap = ( Map<Object, Object> ) rawMap;
			for ( Object key : List.copyOf( configMap.keySet() ) ) {
				Object	value		= configMap.remove( key );
				String	newKey		= resolve( key.toString(), map, decryptor );
				Object	resolvedKey	= key instanceof Key ? Key.of( newKey ) : newKey;
				configMap.put( resolvedKey, PlaceholderHelper.resolveAll( value, map, decryptor ) );
			}
			return object;
		} else if ( object instanceof List<?> rawList ) {
			List<Object> configList = ( List<Object> ) rawList;
			for ( int i = 0; i < configList.size(); i++ ) {
				configList.set( i, PlaceholderHelper.resolveAll( configList.get( i ), map, decryptor ) );
			}
			return object;
		} else if ( object instanceof String strObj ) {
			return ( T ) resolve( strObj, map, decryptor );
		}

		// boolean, null, number
		return ( T ) object;
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
	@SuppressWarnings( "unused" )
	private static String escapeReplacementMetaChars( String input ) {
		return RegexBuilder.of( input, RegexBuilder.REGEX_META ).replaceAllAndGet( "\\\\$1" );
	}

}
