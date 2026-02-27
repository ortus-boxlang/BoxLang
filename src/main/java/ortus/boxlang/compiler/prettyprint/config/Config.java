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
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.jr.ob.JSON;
import com.fasterxml.jackson.jr.ob.JSON.Feature;
import com.fasterxml.jackson.jr.ob.JSONObjectException;

import ortus.boxlang.runtime.types.util.JSONUtil;

public final class Config {

	private int					indentSize				= 4;
	private boolean				tabIndent				= true;
	private int					maxLineLength			= 80;
	private String				newLine					= "os";
	private boolean				singleQuote				= false;
	private boolean				bracketPadding			= false;
	private boolean				parensPadding			= false;
	private boolean				binaryOperatorsPadding	= true;
	private boolean				semicolons				= true;
	private StructConfig		struct					= new StructConfig();
	private PropertyConfig		property				= new PropertyConfig();
	private ArrayConfig			array					= new ArrayConfig();

	@JsonProperty( "for_loop_semicolons" )
	private ForLoopSemicolons	forLoopSemicolons		= new ForLoopSemicolons();

	private FunctionConfig		function				= new FunctionConfig();
	private ArgumentsConfig		arguments				= new ArgumentsConfig();
	private BracesConfig		braces					= new BracesConfig();
	private OperatorsConfig		operators				= new OperatorsConfig();
	private ChainConfig			chain					= new ChainConfig();
	private TemplateConfig		template				= new TemplateConfig();

	@JsonProperty( "import" )
	private ImportConfig		importConfig			= new ImportConfig();
	private CommentsConfig		comments				= new CommentsConfig();

	@JsonProperty( "class" )
	private ClassConfig			classConfig				= new ClassConfig();
	private SqlConfig			sql						= new SqlConfig();

	public Config() {
	}

	public StructConfig getStruct() {
		return struct;
	}

	public Config setStruct( StructConfig struct ) {
		this.struct = struct;
		return this;
	}

	public Config setBinaryOperatorsPadding( boolean value ) {
		this.binaryOperatorsPadding = value;
		return this;
	}

	public boolean getBinaryOperatorsPadding() {
		return this.binaryOperatorsPadding;
	}

	public boolean getSemicolons() {
		return semicolons;
	}

	public Config setSemicolons( boolean semicolons ) {
		this.semicolons = semicolons;
		return this;
	}

	public int getIndentSize() {
		return indentSize;
	}

	public Config setIndentSize( int indentSize ) {
		this.indentSize = indentSize;
		return this;
	}

	public boolean getTabIndent() {
		return tabIndent;
	}

	public Config setTabIndent( boolean tabIndent ) {
		this.tabIndent = tabIndent;
		return this;
	}

	public int getMaxLineLength() {
		return maxLineLength;
	}

	public Config setMaxLineLength( int maxLineLength ) {
		this.maxLineLength = maxLineLength;
		return this;
	}

	public String getNewLine() {
		return newLine;
	}

	public Config setNewLine( String newLine ) {
		this.newLine = newLine;
		return this;
	}

	public boolean getSingleQuote() {
		return singleQuote;
	}

	public Config setSingleQuote( boolean singleQuote ) {
		this.singleQuote = singleQuote;
		return this;
	}

	public boolean getBracketPadding() {
		return bracketPadding;
	}

	public Config setBracketPadding( boolean bracketPadding ) {
		this.bracketPadding = bracketPadding;
		return this;
	}

	public boolean getParensPadding() {
		return parensPadding;
	}

	public Config setParensPadding( boolean parensPadding ) {
		this.parensPadding = parensPadding;
		return this;
	}

	public ForLoopSemicolons getForLoopSemicolons() {
		return forLoopSemicolons;
	}

	public Config setForLoopSemicolons( ForLoopSemicolons forLoopSemicolons ) {
		this.forLoopSemicolons = forLoopSemicolons;
		return this;
	}

	public PropertyConfig getProperty() {
		return property;
	}

	public Config setProperty( PropertyConfig property ) {
		this.property = property;
		return this;
	}

	public ArrayConfig getArray() {
		return array;
	}

	public Config setArray( ArrayConfig array ) {
		this.array = array;
		return this;
	}

	public FunctionConfig getFunction() {
		return function;
	}

	public Config setFunction( FunctionConfig function ) {
		this.function = function;
		return this;
	}

	public ArgumentsConfig getArguments() {
		return arguments;
	}

	public Config setArguments( ArgumentsConfig arguments ) {
		this.arguments = arguments;
		return this;
	}

	public BracesConfig getBraces() {
		return braces;
	}

	public Config setBraces( BracesConfig braces ) {
		this.braces = braces;
		return this;
	}

	public OperatorsConfig getOperators() {
		return operators;
	}

	public Config setOperators( OperatorsConfig operators ) {
		this.operators = operators;
		return this;
	}

	public ChainConfig getChain() {
		return chain;
	}

	public Config setChain( ChainConfig chain ) {
		this.chain = chain;
		return this;
	}

	public TemplateConfig getTemplate() {
		return template;
	}

	public Config setTemplate( TemplateConfig template ) {
		this.template = template;
		return this;
	}

	public ImportConfig getImportConfig() {
		return importConfig;
	}

	public Config setImportConfig( ImportConfig importConfig ) {
		this.importConfig = importConfig;
		return this;
	}

	public CommentsConfig getComments() {
		return comments;
	}

	public Config setComments( CommentsConfig comments ) {
		this.comments = comments;
		return this;
	}

	public ClassConfig getClassConfig() {
		return classConfig;
	}

	public Config setClassConfig( ClassConfig classConfig ) {
		this.classConfig = classConfig;
		return this;
	}

	public SqlConfig getSql() {
		return sql;
	}

	public Config setSql( SqlConfig sql ) {
		this.sql = sql;
		return this;
	}

	/**
	 * Load configuration from a specific file path.
	 *
	 * @param filePath Path to the configuration file
	 *
	 * @return Config object with the loaded settings
	 *
	 * @throws JSONObjectException if the JSON is malformed
	 * @throws IOException         if the file cannot be read
	 */
	public static Config loadConfig( String filePath ) throws JSONObjectException, IOException {
		return JSONUtil.getJSONBuilder().beanFrom( Config.class, new File( filePath ) );
	}

	/**
	 * Load configuration with fallback logic.
	 * First tries .bxformat.json in the specified directory, then falls back to .cfformat.json.
	 * If neither exists, returns a default Config.
	 *
	 * @param directory The directory to search for config files
	 *
	 * @return Config object with the loaded settings, or default Config if no file found
	 */
	public static Config loadConfigWithFallback( String directory ) {
		return loadConfigWithFallback( directory, false );
	}

	/**
	 * Load configuration with fallback logic.
	 * First tries .bxformat.json in the specified directory, then falls back to .cfformat.json.
	 * If neither exists, returns a default Config or throws an exception.
	 *
	 * @param directory     The directory to search for config files
	 * @param requireConfig If true, throws an exception if no config file is found
	 *
	 * @return Config object with the loaded settings
	 *
	 * @throws RuntimeException if requireConfig is true and no config file is found
	 */
	public static Config loadConfigWithFallback( String directory, boolean requireConfig ) {
		File	bxFormatFile	= new File( directory, ".bxformat.json" );
		File	cfFormatFile	= new File( directory, ".cfformat.json" );

		// First, try .bxformat.json
		if ( bxFormatFile.exists() ) {
			try {
				return loadConfig( bxFormatFile.getAbsolutePath() );
			} catch ( Exception e ) {
				System.err.println( "Warning: Failed to load .bxformat.json: " + e.getMessage() );
			}
		}

		// Fall back to .cfformat.json
		if ( cfFormatFile.exists() ) {
			try {
				Config config = CFFormatConfigLoader.loadCFFormatConfig( cfFormatFile );
				System.out.println( "Loaded configuration from .cfformat.json (consider converting to .bxformat.json)" );
				return config;
			} catch ( Exception e ) {
				System.err.println( "Warning: Failed to load .cfformat.json: " + e.getMessage() );
			}
		}

		// No config file found
		if ( requireConfig ) {
			throw new RuntimeException( "No configuration file found (.bxformat.json or .cfformat.json) in " + directory );
		}

		return new Config();
	}

	/**
	 * Determines which config file path to use, with fallback logic.
	 * Returns the path to .bxformat.json if it exists, otherwise .cfformat.json if it exists,
	 * otherwise returns the default .bxformat.json path.
	 *
	 * @param directory The directory to search for config files
	 *
	 * @return Path to the config file to use
	 */
	public static String getConfigFilePath( String directory ) {
		File bxFormatFile = new File( directory, ".bxformat.json" );
		if ( bxFormatFile.exists() ) {
			return bxFormatFile.getAbsolutePath();
		}

		File cfFormatFile = new File( directory, ".cfformat.json" );
		if ( cfFormatFile.exists() ) {
			return cfFormatFile.getAbsolutePath();
		}

		// Return default path
		return bxFormatFile.getAbsolutePath();
	}

	/**
	 * Check if a config file path is a CFFormat file.
	 *
	 * @param filePath The file path to check
	 *
	 * @return true if the file is a .cfformat.json file
	 */
	public static boolean isCFFormatConfig( String filePath ) {
		return filePath != null && filePath.toLowerCase().endsWith( ".cfformat.json" );
	}

	/**
	 * Load configuration from a file path, automatically detecting the format.
	 * If the file is a .cfformat.json, it will be converted to Config.
	 *
	 * @param filePath Path to the configuration file
	 *
	 * @return Config object with the loaded settings
	 *
	 * @throws IOException if the file cannot be read
	 */
	public static Config loadConfigAutoDetect( String filePath ) throws IOException {
		if ( isCFFormatConfig( filePath ) ) {
			return CFFormatConfigLoader.loadCFFormatConfig( filePath );
		}
		return loadConfig( filePath );
	}

	@SuppressWarnings( "unchecked" )
	public Config loadFromConfigFile( String filePath ) {
		Map<String, Object> config = ( Map<String, Object> ) JSONUtil.fromJSON( new File( filePath ) );
		applyMapConfig( config );
		return this;
	}

	public Config loadFromConfig( Map<String, Object> config ) {
		applyMapConfig( config );
		return this;
	}

	public int indentColumn( int indentLevel ) {
		return indentSize * indentLevel;
	}

	public String indentToColumn( int column ) {
		if ( tabIndent ) {
			int	tabs	= column / indentSize;
			int	spaces	= column % indentSize;
			return "\t".repeat( tabs ) + " ".repeat( spaces );
		}
		return " ".repeat( column );
	}

	public String indentToLevel( int indentLevel ) {
		return indentToColumn( indentColumn( indentLevel ) );
	}

	public String lineSeparator() {
		return newLine.equals( "os" ) ? System.lineSeparator() : newLine;
	}

	public Map<String, Object> toMap() {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put( "indentSize", indentSize );
		map.put( "tabIndent", tabIndent );
		map.put( "maxLineLength", maxLineLength );
		map.put( "newLine", newLine );
		map.put( "singleQuote", singleQuote );
		map.put( "bracketPadding", bracketPadding );
		map.put( "parensPadding", parensPadding );
		map.put( "binaryOperatorsPadding", binaryOperatorsPadding );
		map.put( "semicolons", semicolons );
		map.put( "struct", struct.toMap() );
		map.put( "property", property.toMap() );
		map.put( "array", array.toMap() );
		map.put( "for_loop_semicolons", forLoopSemicolons.toMap() );
		map.put( "function", function.toMap() );
		map.put( "arguments", arguments.toMap() );
		map.put( "braces", braces.toMap() );
		map.put( "operators", operators.toMap() );
		map.put( "chain", chain.toMap() );
		map.put( "template", template.toMap() );
		map.put( "import", importConfig.toMap() );
		map.put( "comments", comments.toMap() );
		map.put( "class", classConfig.toMap() );
		map.put( "sql", sql.toMap() );
		return map;
	}

	public String toJSON() {
		try {
			return JSON.std.with( Feature.PRETTY_PRINT_OUTPUT, Feature.WRITE_NULL_PROPERTIES )
			    .asString( toMap() );
		} catch ( IOException e ) {
			e.printStackTrace();
			throw new RuntimeException( "Failed to convert to JSON", e );
		}
	}

	@SuppressWarnings( "unchecked" )
	private void applyMapConfig( Map<String, Object> config ) {
		if ( config.containsKey( "indentSize" ) && config.get( "indentSize" ) instanceof Number indentSize ) {
			this.indentSize = indentSize.intValue();
		}
		if ( config.containsKey( "tabIndent" ) && config.get( "tabIndent" ) instanceof Boolean tabIndent ) {
			this.tabIndent = tabIndent;
		}
		if ( config.containsKey( "maxLineLength" ) && config.get( "maxLineLength" ) instanceof Number maxLineLength ) {
			this.maxLineLength = maxLineLength.intValue();
		}
		if ( config.containsKey( "newLine" ) && config.get( "newLine" ) instanceof String newLine ) {
			this.newLine = newLine;
		}
		if ( config.containsKey( "singleQuote" ) && config.get( "singleQuote" ) instanceof Boolean singleQuote ) {
			this.singleQuote = singleQuote;
		}
		if ( config.containsKey( "bracketPadding" ) && config.get( "bracketPadding" ) instanceof Boolean bracketPadding ) {
			this.bracketPadding = bracketPadding;
		}
		if ( config.containsKey( "parensPadding" ) && config.get( "parensPadding" ) instanceof Boolean parensPadding ) {
			this.parensPadding = parensPadding;
		}
		if ( config.containsKey( "binaryOperatorsPadding" ) && config.get( "binaryOperatorsPadding" ) instanceof Boolean binaryOperatorsPadding ) {
			this.binaryOperatorsPadding = binaryOperatorsPadding;
		}
		if ( config.containsKey( "semicolons" ) && config.get( "semicolons" ) instanceof Boolean semicolons ) {
			this.semicolons = semicolons;
		}
		if ( config.containsKey( "struct" ) && config.get( "struct" ) instanceof Map structMap ) {
			applyStructConfig( ( Map<String, Object> ) structMap );
		}
		if ( config.containsKey( "array" ) && config.get( "array" ) instanceof Map arrayMap ) {
			applyArrayConfig( ( Map<String, Object> ) arrayMap );
		}
		if ( config.containsKey( "property" ) && config.get( "property" ) instanceof Map propertyMap ) {
			applyPropertyConfig( ( Map<String, Object> ) propertyMap );
		}
		if ( config.containsKey( "for_loop_semicolons" ) && config.get( "for_loop_semicolons" ) instanceof Map forLoopMap ) {
			applyForLoopSemicolonsConfig( ( Map<String, Object> ) forLoopMap );
		}
		if ( config.containsKey( "function" ) && config.get( "function" ) instanceof Map functionMap ) {
			applyFunctionConfig( ( Map<String, Object> ) functionMap );
		}
		if ( config.containsKey( "arguments" ) && config.get( "arguments" ) instanceof Map argumentsMap ) {
			applyArgumentsConfig( ( Map<String, Object> ) argumentsMap );
		}
		if ( config.containsKey( "braces" ) && config.get( "braces" ) instanceof Map bracesMap ) {
			applyBracesConfig( ( Map<String, Object> ) bracesMap );
		}
		if ( config.containsKey( "operators" ) && config.get( "operators" ) instanceof Map operatorsMap ) {
			applyOperatorsConfig( ( Map<String, Object> ) operatorsMap );
		}
		if ( config.containsKey( "chain" ) && config.get( "chain" ) instanceof Map chainMap ) {
			applyChainConfig( ( Map<String, Object> ) chainMap );
		}
		if ( config.containsKey( "template" ) && config.get( "template" ) instanceof Map templateMap ) {
			applyTemplateConfig( ( Map<String, Object> ) templateMap );
		}
		if ( config.containsKey( "import" ) && config.get( "import" ) instanceof Map importMap ) {
			applyImportConfig( ( Map<String, Object> ) importMap );
		}
		if ( config.containsKey( "comments" ) && config.get( "comments" ) instanceof Map commentsMap ) {
			applyCommentsConfig( ( Map<String, Object> ) commentsMap );
		}
		if ( config.containsKey( "class" ) && config.get( "class" ) instanceof Map classMap ) {
			applyClassConfig( ( Map<String, Object> ) classMap );
		}
		if ( config.containsKey( "sql" ) && config.get( "sql" ) instanceof Map sqlMap ) {
			applySqlConfig( ( Map<String, Object> ) sqlMap );
		}
	}

	private void applyStructConfig( Map<String, Object> config ) {
		if ( config.containsKey( "padding" ) && config.get( "padding" ) instanceof Boolean padding ) {
			this.struct.setPadding( padding );
		}
		if ( config.containsKey( "empty_padding" ) && config.get( "empty_padding" ) instanceof Boolean emptyPadding ) {
			this.struct.setEmptyPadding( emptyPadding );
		}
		if ( config.containsKey( "quote_keys" ) && config.get( "quote_keys" ) instanceof Boolean quoteKeys ) {
			this.struct.setQuoteKeys( quoteKeys );
		}
		if ( config.containsKey( "separator" ) && config.get( "separator" ) instanceof String separator ) {
			this.struct.setSeparator( parseSeparator( separator ) );
		}
		if ( config.containsKey( "multiline" ) && config.get( "multiline" ) instanceof Map multilineMap ) {
			applyMultilineConfig( this.struct.getMultiline(), multilineMap );
		}
	}

	private void applyArrayConfig( Map<String, Object> config ) {
		if ( config.containsKey( "padding" ) && config.get( "padding" ) instanceof Boolean padding ) {
			this.array.setPadding( padding );
		}
		if ( config.containsKey( "empty_padding" ) && config.get( "empty_padding" ) instanceof Boolean emptyPadding ) {
			this.array.setEmptyPadding( emptyPadding );
		}
		if ( config.containsKey( "multiline" ) && config.get( "multiline" ) instanceof Map multilineMap ) {
			applyMultilineConfig( this.array.getMultiline(), multilineMap );
		}
	}

	private void applyPropertyConfig( Map<String, Object> config ) {
		if ( config.containsKey( "multiline" ) && config.get( "multiline" ) instanceof Map multilineMap ) {
			applyMultilineConfig( this.property.getMultiline(), multilineMap );
		}
		if ( config.containsKey( "key_value" ) && config.get( "key_value" ) instanceof Map keyValueMap ) {
			if ( keyValueMap.containsKey( "padding" ) && keyValueMap.get( "padding" ) instanceof Boolean padding ) {
				this.property.getKeyValue().setPadding( padding );
			}
		}
	}

	private void applyForLoopSemicolonsConfig( Map<String, Object> config ) {
		if ( config.containsKey( "padding" ) && config.get( "padding" ) instanceof Boolean padding ) {
			this.forLoopSemicolons.setPadding( padding );
		}
	}

	@SuppressWarnings( "unchecked" )
	private void applyFunctionConfig( Map<String, Object> config ) {
		if ( config.containsKey( "style" ) && config.get( "style" ) instanceof String style ) {
			this.function.setStyle( style );
		}
		if ( config.containsKey( "parameters" ) && config.get( "parameters" ) instanceof Map parametersMap ) {
			applyParametersConfig( ( Map<String, Object> ) parametersMap );
		}
		if ( config.containsKey( "arrow" ) && config.get( "arrow" ) instanceof Map arrowMap ) {
			applyArrowConfig( ( Map<String, Object> ) arrowMap );
		}
	}

	private void applyParametersConfig( Map<String, Object> config ) {
		if ( config.containsKey( "comma_dangle" ) && config.get( "comma_dangle" ) instanceof Boolean commaDangle ) {
			this.function.getParameters().setCommaDangle( commaDangle );
		}
		if ( config.containsKey( "multiline_count" ) && config.get( "multiline_count" ) instanceof Number multilineCount ) {
			this.function.getParameters().setMultilineCount( multilineCount.intValue() );
		}
		if ( config.containsKey( "multiline_length" ) && config.get( "multiline_length" ) instanceof Number multilineLength ) {
			this.function.getParameters().setMultilineLength( multilineLength.intValue() );
		}
	}

	private void applyArrowConfig( Map<String, Object> config ) {
		if ( config.containsKey( "parens" ) && config.get( "parens" ) instanceof String parens ) {
			this.function.getArrow().setParens( parens );
		}
	}

	@SuppressWarnings( "rawtypes" )
	private void applyMultilineConfig( MultilineConfig multiline, Map config ) {
		if ( config.containsKey( "element_count" ) && config.get( "element_count" ) instanceof Number elementCount ) {
			multiline.setElementCount( elementCount.intValue() );
		}
		if ( config.containsKey( "comma_dangle" ) && config.get( "comma_dangle" ) instanceof Boolean commaDangle ) {
			multiline.setCommaDangle( commaDangle );
		}
		if ( config.containsKey( "min_length" ) && config.get( "min_length" ) instanceof Number minLength ) {
			multiline.setMinLength( minLength.intValue() );
		}
		if ( config.containsKey( "leading_comma" ) ) {
			multiline.setLeadingComma( config.get( "leading_comma" ) );
		}
	}

	private void applyArgumentsConfig( Map<String, Object> config ) {
		if ( config.containsKey( "comma_dangle" ) && config.get( "comma_dangle" ) instanceof Boolean commaDangle ) {
			this.arguments.setCommaDangle( commaDangle );
		}
		if ( config.containsKey( "multiline_count" ) && config.get( "multiline_count" ) instanceof Number multilineCount ) {
			this.arguments.setMultilineCount( multilineCount.intValue() );
		}
		if ( config.containsKey( "multiline_length" ) && config.get( "multiline_length" ) instanceof Number multilineLength ) {
			this.arguments.setMultilineLength( multilineLength.intValue() );
		}
	}

	@SuppressWarnings( "unchecked" )
	private void applyBracesConfig( Map<String, Object> config ) {
		if ( config.containsKey( "style" ) && config.get( "style" ) instanceof String style ) {
			this.braces.setStyle( style );
		}
		if ( config.containsKey( "require_for_single_statement" ) && config.get( "require_for_single_statement" ) instanceof Boolean require ) {
			this.braces.setRequireForSingleStatement( require );
		}
		if ( config.containsKey( "else" ) && config.get( "else" ) instanceof Map elseMap ) {
			applyElseConfig( ( Map<String, Object> ) elseMap );
		}
	}

	private void applyElseConfig( Map<String, Object> config ) {
		if ( config.containsKey( "style" ) && config.get( "style" ) instanceof String style ) {
			this.braces.getElseConfig().setStyle( style );
		}
	}

	@SuppressWarnings( "unchecked" )
	private void applyOperatorsConfig( Map<String, Object> config ) {
		if ( config.containsKey( "position" ) && config.get( "position" ) instanceof String position ) {
			this.operators.setPosition( position );
		}
		if ( config.containsKey( "ternary" ) && config.get( "ternary" ) instanceof Map ternaryMap ) {
			applyTernaryConfig( ( Map<String, Object> ) ternaryMap );
		}
	}

	private void applyTernaryConfig( Map<String, Object> config ) {
		if ( config.containsKey( "style" ) && config.get( "style" ) instanceof String style ) {
			this.operators.getTernary().setStyle( style );
		}
		if ( config.containsKey( "question_position" ) && config.get( "question_position" ) instanceof String questionPosition ) {
			this.operators.getTernary().setQuestionPosition( questionPosition );
		}
	}

	private void applyChainConfig( Map<String, Object> config ) {
		if ( config.containsKey( "break_count" ) && config.get( "break_count" ) instanceof Number breakCount ) {
			this.chain.setBreakCount( breakCount.intValue() );
		}
		if ( config.containsKey( "break_length" ) && config.get( "break_length" ) instanceof Number breakLength ) {
			this.chain.setBreakLength( breakLength.intValue() );
		}
	}

	private void applyTemplateConfig( Map<String, Object> config ) {
		if ( config.containsKey( "component_prefix" ) && config.get( "component_prefix" ) instanceof String componentPrefix ) {
			this.template.setComponentPrefix( componentPrefix );
		}
		if ( config.containsKey( "indent_content" ) && config.get( "indent_content" ) instanceof Boolean indentContent ) {
			this.template.setIndentContent( indentContent );
		}
		if ( config.containsKey( "single_attribute_per_line" ) && config.get( "single_attribute_per_line" ) instanceof Boolean singleAttr ) {
			this.template.setSingleAttributePerLine( singleAttr );
		}
		if ( config.containsKey( "self_closing" ) && config.get( "self_closing" ) instanceof Boolean selfClosing ) {
			this.template.setSelfClosing( selfClosing );
		}
	}

	private void applyImportConfig( Map<String, Object> config ) {
		if ( config.containsKey( "sort" ) && config.get( "sort" ) instanceof Boolean sort ) {
			this.importConfig.setSort( sort );
		}
		if ( config.containsKey( "group" ) && config.get( "group" ) instanceof Boolean group ) {
			this.importConfig.setGroup( group );
		}
	}

	private void applyCommentsConfig( Map<String, Object> config ) {
		if ( config.containsKey( "preserve_blank_lines" ) && config.get( "preserve_blank_lines" ) instanceof Boolean preserveBlankLines ) {
			this.comments.setPreserveBlankLines( preserveBlankLines );
		}
		if ( config.containsKey( "wrap" ) && config.get( "wrap" ) instanceof Boolean wrap ) {
			this.comments.setWrap( wrap );
		}
	}

	private void applyClassConfig( Map<String, Object> config ) {
		if ( config.containsKey( "member_order" ) && config.get( "member_order" ) instanceof String memberOrder ) {
			this.classConfig.setMemberOrder( memberOrder );
		}
		if ( config.containsKey( "member_spacing" ) && config.get( "member_spacing" ) instanceof Number memberSpacing ) {
			this.classConfig.setMemberSpacing( memberSpacing.intValue() );
		}
	}

	private void applySqlConfig( Map<String, Object> config ) {
		if ( config.containsKey( "uppercase_keywords" ) && config.get( "uppercase_keywords" ) instanceof Boolean uppercaseKeywords ) {
			this.sql.setUppercaseKeywords( uppercaseKeywords );
		}
		if ( config.containsKey( "indent_clauses" ) && config.get( "indent_clauses" ) instanceof Boolean indentClauses ) {
			this.sql.setIndentClauses( indentClauses );
		}
	}

	private Separator parseSeparator( String separator ) {
		return switch ( separator ) {
			case ":" -> Separator.COLON;
			case "=" -> Separator.EQUALS;
			case ": " -> Separator.COLON_SPACE;
			case "= " -> Separator.EQUALS_SPACE;
			default -> Separator.COLON_SPACE;
		};
	}
}