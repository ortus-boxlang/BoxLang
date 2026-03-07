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
package ortus.boxlang.compiler.prettyprint.config;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import ortus.boxlang.runtime.types.util.JSONUtil;

/**
 * Utility class for loading and converting CFFormat configuration files (.cfformat.json)
 * to BoxLang format configuration (Config).
 *
 * CFFormat uses a flat key structure with dot notation (e.g., "array.padding": true)
 * while BXFormat uses nested objects (e.g., { "array": { "padding": true } }).
 *
 * This class provides methods to:
 * - Load a CFFormat configuration file and convert it to a Config object
 * - Convert a CFFormat configuration to a BXFormat JSON string
 *
 * @see <a href="https://github.com/jcberquist/commandbox-cfformat/blob/master/reference.md">CFFormat Reference</a>
 */
public final class CFFormatConfigLoader {

	private CFFormatConfigLoader() {
		// Prevent instantiation
	}

	/**
	 * Load a CFFormat configuration file and convert it to a Config object.
	 *
	 * @param filePath Path to the .cfformat.json file
	 *
	 * @return Config object with the converted settings
	 *
	 * @throws IOException if the file cannot be read
	 */
	@SuppressWarnings( "unchecked" )
	public static Config loadCFFormatConfig( String filePath ) throws IOException {
		File				file		= new File( filePath );
		Map<String, Object>	cfConfig	= ( Map<String, Object> ) JSONUtil.fromJSON( file );
		return convertCFFormatToConfig( cfConfig );
	}

	/**
	 * Load a CFFormat configuration file and convert it to a Config object.
	 *
	 * @param file The .cfformat.json file
	 *
	 * @return Config object with the converted settings
	 *
	 * @throws IOException if the file cannot be read
	 */
	@SuppressWarnings( "unchecked" )
	public static Config loadCFFormatConfig( File file ) throws IOException {
		Map<String, Object> cfConfig = ( Map<String, Object> ) JSONUtil.fromJSON( file );
		return convertCFFormatToConfig( cfConfig );
	}

	/**
	 * Convert a CFFormat configuration map to a Config object.
	 *
	 * @param cfConfig Map of CFFormat settings (flat key structure with dot notation)
	 *
	 * @return Config object with the converted settings
	 */
	public static Config convertCFFormatToConfig( Map<String, Object> cfConfig ) {
		Config config = new Config();

		// Top-level settings
		applyIfPresent( cfConfig, "indent_size", value -> config.setIndentSize( toInt( value ) ) );
		applyIfPresent( cfConfig, "tab_indent", value -> config.setTabIndent( toBool( value ) ) );
		applyIfPresent( cfConfig, "max_columns", value -> config.setMaxLineLength( toInt( value ) ) );
		applyIfPresent( cfConfig, "newline", value -> config.setNewLine( toString( value ) ) );

		// Quote style - cfformat uses "single" or "double", bxformat uses boolean singleQuote
		applyIfPresent( cfConfig, "strings.quote", value -> {
			String quote = toString( value );
			config.setSingleQuote( "single".equalsIgnoreCase( quote ) );
		} );

		// Padding settings
		applyIfPresent( cfConfig, "brackets.padding", value -> config.setBracketPadding( toBool( value ) ) );
		applyIfPresent( cfConfig, "parentheses.padding", value -> config.setParensPadding( toBool( value ) ) );
		applyIfPresent( cfConfig, "binary_operators.padding", value -> config.setBinaryOperatorsPadding( toBool( value ) ) );

		// For loop semicolons
		applyIfPresent( cfConfig, "for_loop_semicolons.padding", value -> config.getForLoopSemicolons().setPadding( toBool( value ) ) );

		// Array settings
		applyArraySettings( cfConfig, config );

		// Struct settings
		applyStructSettings( cfConfig, config );

		// Function declaration settings (maps to function.parameters)
		applyFunctionDeclarationSettings( cfConfig, config );

		// Function call settings (maps to arguments)
		applyFunctionCallSettings( cfConfig, config );

		// Property settings
		applyPropertySettings( cfConfig, config );

		// Comment settings
		applyCommentSettings( cfConfig, config );

		return config;
	}

	/**
	 * Convert a CFFormat configuration file to BXFormat JSON string.
	 *
	 * @param cfFormatFilePath Path to the .cfformat.json file
	 *
	 * @return JSON string in BXFormat structure
	 *
	 * @throws IOException if the file cannot be read
	 */
	public static String convertToBoxFormatJSON( String cfFormatFilePath ) throws IOException {
		Config config = loadCFFormatConfig( cfFormatFilePath );
		return config.toJSON();
	}

	/**
	 * Convert a CFFormat configuration file and write it to a BXFormat file.
	 *
	 * @param cfFormatFilePath Path to the .cfformat.json file
	 * @param bxFormatFilePath Path to write the .bxformat.json file
	 *
	 * @throws IOException if the file cannot be read or written
	 */
	public static void convertAndWriteBoxFormatFile( String cfFormatFilePath, String bxFormatFilePath ) throws IOException {
		String bxFormatJSON = convertToBoxFormatJSON( cfFormatFilePath );
		Files.writeString( Path.of( bxFormatFilePath ), bxFormatJSON );
	}

	// ========== Private helper methods ==========

	private static void applyArraySettings( Map<String, Object> cfConfig, Config config ) {
		ArrayConfig array = config.getArray();

		applyIfPresent( cfConfig, "array.padding", value -> array.setPadding( toBool( value ) ) );
		applyIfPresent( cfConfig, "array.empty_padding", value -> array.setEmptyPadding( toBool( value ) ) );

		MultilineConfig multiline = array.getMultiline();
		applyIfPresent( cfConfig, "array.multiline.element_count", value -> multiline.setElementCount( toInt( value ) ) );
		applyIfPresent( cfConfig, "array.multiline.min_length", value -> multiline.setMinLength( toInt( value ) ) );
		applyIfPresent( cfConfig, "array.multiline.comma_dangle", value -> multiline.setCommaDangle( toBool( value ) ) );

		// Leading comma - cfformat uses two separate keys
		LeadingComma leadingComma = multiline.getLeadingComma();
		applyIfPresent( cfConfig, "array.multiline.leading_comma", value -> leadingComma.setEnabled( toBool( value ) ) );
		applyIfPresent( cfConfig, "array.multiline.leading_comma.padding", value -> leadingComma.setPadding( toBool( value ) ) );
	}

	private static void applyStructSettings( Map<String, Object> cfConfig, Config config ) {
		StructConfig struct = config.getStruct();

		applyIfPresent( cfConfig, "struct.padding", value -> struct.setPadding( toBool( value ) ) );
		applyIfPresent( cfConfig, "struct.empty_padding", value -> struct.setEmptyPadding( toBool( value ) ) );
		applyIfPresent( cfConfig, "struct.quote_keys", value -> struct.setQuoteKeys( toBool( value ) ) );

		// Separator - cfformat uses string like " : " or "="
		applyIfPresent( cfConfig, "struct.separator", value -> {
			String sep = toString( value );
			struct.setSeparator( parseSeparator( sep ) );
		} );

		MultilineConfig multiline = struct.getMultiline();
		applyIfPresent( cfConfig, "struct.multiline.element_count", value -> multiline.setElementCount( toInt( value ) ) );
		applyIfPresent( cfConfig, "struct.multiline.min_length", value -> multiline.setMinLength( toInt( value ) ) );
		applyIfPresent( cfConfig, "struct.multiline.comma_dangle", value -> multiline.setCommaDangle( toBool( value ) ) );

		LeadingComma leadingComma = multiline.getLeadingComma();
		applyIfPresent( cfConfig, "struct.multiline.leading_comma", value -> leadingComma.setEnabled( toBool( value ) ) );
		applyIfPresent( cfConfig, "struct.multiline.leading_comma.padding", value -> leadingComma.setPadding( toBool( value ) ) );
	}

	private static void applyFunctionDeclarationSettings( Map<String, Object> cfConfig, Config config ) {
		FunctionConfig.ParametersConfig params = config.getFunction().getParameters();

		applyIfPresent( cfConfig, "function_declaration.multiline.element_count", value -> params.setMultilineCount( toInt( value ) ) );
		applyIfPresent( cfConfig, "function_declaration.multiline.min_length", value -> params.setMultilineLength( toInt( value ) ) );
	}

	private static void applyFunctionCallSettings( Map<String, Object> cfConfig, Config config ) {
		ArgumentsConfig args = config.getArguments();

		applyIfPresent( cfConfig, "function_call.multiline.element_count", value -> args.setMultilineCount( toInt( value ) ) );
		applyIfPresent( cfConfig, "function_call.multiline.min_length", value -> args.setMultilineLength( toInt( value ) ) );
	}

	private static void applyPropertySettings( Map<String, Object> cfConfig, Config config ) {
		PropertyConfig	property	= config.getProperty();
		MultilineConfig	multiline	= property.getMultiline();

		applyIfPresent( cfConfig, "property.multiline.element_count", value -> multiline.setElementCount( toInt( value ) ) );
		applyIfPresent( cfConfig, "property.multiline.min_length", value -> multiline.setMinLength( toInt( value ) ) );
	}

	private static void applyCommentSettings( Map<String, Object> cfConfig, Config config ) {
		CommentsConfig comments = config.getComments();

		// cfformat uses "comment.asterisks" with values like "align", "indent", "ignored"
		// We don't have a direct mapping for this, but we can try to interpret it
		applyIfPresent( cfConfig, "comment.asterisks", value -> {
			// For now, we just note this setting exists
			// The bxformat comments config uses different settings
		} );
	}

	/**
	 * Helper to apply a value if the key exists in the config map.
	 */
	private static void applyIfPresent( Map<String, Object> config, String key, java.util.function.Consumer<Object> setter ) {
		if ( config.containsKey( key ) ) {
			setter.accept( config.get( key ) );
		}
	}

	private static boolean toBool( Object value ) {
		if ( value instanceof Boolean b ) {
			return b;
		}
		if ( value instanceof String s ) {
			return "true".equalsIgnoreCase( s );
		}
		return false;
	}

	private static int toInt( Object value ) {
		if ( value instanceof Number n ) {
			return n.intValue();
		}
		if ( value instanceof String s ) {
			try {
				return Integer.parseInt( s );
			} catch ( NumberFormatException e ) {
				return 0;
			}
		}
		return 0;
	}

	private static String toString( Object value ) {
		return value != null ? value.toString() : "";
	}

	private static Separator parseSeparator( String separator ) {
		// Check if it contains spaces
		boolean hasSpace = separator.contains( " " );

		// Determine the base character (colon or equals)
		if ( separator.contains( ":" ) ) {
			return hasSpace ? Separator.COLON_SPACE : Separator.COLON;
		} else if ( separator.contains( "=" ) ) {
			return hasSpace ? Separator.EQUALS_SPACE : Separator.EQUALS;
		}

		// Default
		return Separator.COLON_SPACE;
	}
}
