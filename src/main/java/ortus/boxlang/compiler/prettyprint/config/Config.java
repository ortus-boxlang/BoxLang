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

import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.types.util.JSONUtil;

public final class Config {

	/**
	 * Number of spaces per indentation level. When {@code tabIndent} is true, this controls
	 * the display width of each tab character for alignment calculations.
	 *
	 * <pre>
	 * // indentSize: 2
	 * if ( true ) {
	 *     foo();
	 * }
	 *
	 * // indentSize: 4 (default)
	 * if ( true ) {
	 *     foo();
	 * }
	 * </pre>
	 */
	private int					indentSize					= 4;

	/**
	 * Use tab characters for indentation instead of spaces. When true, indentation is
	 * output as {@code \t} characters; when false, spaces are used based on {@code indentSize}.
	 *
	 * <pre>
	 * // tabIndent: true (default)
	 * →   if ( true ) {
	 * →   →   foo();
	 * →   }
	 *
	 * // tabIndent: false
	 *     if ( true ) {
	 *         foo();
	 *     }
	 * </pre>
	 */
	private boolean				tabIndent					= true;

	/**
	 * Maximum line length before the printer attempts to break lines. Used by the Doc layout
	 * algorithm to decide when to switch from flat to broken mode, and by the comment printer
	 * to wrap long comment text.
	 *
	 * <pre>
	 * // maxLineLength: 40
	 * foo(
	 *     arg1,
	 *     arg2,
	 *     arg3
	 * );
	 *
	 * // maxLineLength: 120
	 * foo( arg1, arg2, arg3 );
	 * </pre>
	 */
	private int					maxLineLength				= 115;

	/**
	 * Line separator to use in output. Set to {@code "os"} to use the operating system default
	 * ({@code \r\n} on Windows, {@code \n} on Unix/macOS), or specify an explicit string
	 * like {@code "\n"} or {@code "\r\n"}.
	 *
	 * <pre>
	 * // newLine: "os" (default) - uses System.lineSeparator()
	 * // newLine: "\n" - always Unix line endings
	 * // newLine: "\r\n" - always Windows line endings
	 * </pre>
	 */
	private String				newLine						= "\n";

	/**
	 * Use single quotes for string literals and struct keys instead of double quotes.
	 * Ignored for strings when {@code preserveStringQuotes} is true.
	 *
	 * <pre>
	 * // singleQuote: false (default)
	 * name = "Brad";
	 *
	 * // singleQuote: true
	 * name = 'Brad';
	 * </pre>
	 */
	private boolean				singleQuote					= false;

	/**
	 * Preserve the original quote style found in the source code rather than normalizing
	 * to the style specified by {@code singleQuote}. When true, strings keep whatever
	 * quote character they were written with.
	 *
	 * <pre>
	 * // Source: name = "Brad"; other = 'Wood';
	 *
	 * // preserveStringQuotes: true
	 * name = "Brad"; other = 'Wood';
	 *
	 * // preserveStringQuotes: false, singleQuote: true
	 * name = 'Brad'; other = 'Wood';
	 * </pre>
	 */
	private boolean				preserveStringQuotes		= false;

	/**
	 * Vertically align the separators ({@code =} or {@code :}) in consecutive assignment
	 * statements, struct literals, function arguments, and function parameters when they
	 * are formatted across multiple lines.
	 *
	 * <pre>
	 * // alignConsecutiveAssignments: true
	 * {
	 *     key         : "val1",
	 *     longerKey   : "val2",
	 *     x           : "val3"
	 * }
	 *
	 * // alignConsecutiveAssignments: false (default)
	 * {
	 *     key: "val1",
	 *     longerKey: "val2",
	 *     x: "val3"
	 * }
	 * </pre>
	 */
	private boolean				alignConsecutiveAssignments	= true;

	/**
	 * Vertically align attributes in consecutive {@code property} declarations within
	 * a class or component body.
	 *
	 * <pre>
	 * // alignConsecutiveProperties: true
	 * property name="id"     type="numeric";
	 * property name="name"   type="string";
	 *
	 * // alignConsecutiveProperties: false (default)
	 * property name="id" type="numeric";
	 * property name="name" type="string";
	 * </pre>
	 */
	private boolean				alignConsecutiveProperties	= true;

	/**
	 * Add spaces inside square brackets in array literals. Acts as the default for
	 * {@code array.padding} when not explicitly set.
	 *
	 * <pre>
	 * // bracketPadding: true
	 * arr = [ 1, 2, 3 ];
	 *
	 * // bracketPadding: false (default)
	 * arr = [1, 2, 3];
	 * </pre>
	 */
	private boolean				bracketPadding				= true;

	/**
	 * Add spaces inside parentheses for function calls and function definitions. Acts as
	 * the global default; can be overridden by {@code arguments.padding} and
	 * {@code function.parameters.padding}.
	 *
	 * <pre>
	 * // parensPadding: true (default)
	 * foo( arg1, arg2 );
	 * function bar( required string name ) {}
	 *
	 * // parensPadding: false
	 * foo(arg1, arg2);
	 * function bar(required string name) {}
	 * </pre>
	 */
	private boolean				parensPadding				= true;

	/**
	 * Add spaces around binary operators such as {@code +}, {@code -}, {@code ==},
	 * {@code &&}, {@code ||}, etc.
	 *
	 * <pre>
	 * // binaryOperatorsPadding: true (default)
	 * result = a + b;
	 * if ( x == y &amp;&amp; z &gt; 0 ) {
	 * }
	 *
	 * // binaryOperatorsPadding: false
	 * result = a + b;
	 * if ( x == y &amp;&amp; z &gt; 0 ) {
	 * }
	 * </pre>
	 */
	private boolean				binaryOperatorsPadding		= true;

	/**
	 * Append semicolons after statements such as assignments, function calls,
	 * and import statements.
	 *
	 * <pre>
	 * // semicolons: true (default)
	 * import foo.Bar;
	 * name = "Brad";
	 * doSomething();
	 *
	 * // semicolons: false
	 * import foo.Bar
	 * name = "Brad"
	 * doSomething()
	 * </pre>
	 */
	private boolean				semicolons					= true;

	/**
	 * Enable compatibility mode with the legacy {@code cfformat} tool. Adjusts various
	 * formatting behaviors including annotation placement, property spacing, multiline
	 * decisions, and ensures a trailing newline in output.
	 */
	private boolean				cfFormatCompatibility		= false;

	/**
	 * Override the automatic source type detection. When {@code null} (default), the
	 * pretty printer resolves the source type from the AST root node. Set explicitly
	 * to force formatting as a specific language variant regardless of the source.
	 *
	 * <pre>
	 * // sourceType: null (default) - auto-detected from AST
	 * // sourceType: BOXSCRIPT - force BoxLang script formatting
	 * // sourceType: BOXTEMPLATE - force BoxLang template formatting
	 * // sourceType: CFSCRIPT - force CFML script formatting
	 * // sourceType: CFTEMPLATE - force CFML template formatting
	 * </pre>
	 */
	private BoxSourceType		sourceType					= null;

	/**
	 * Controls formatting of struct literals including padding, separators,
	 * key quoting, and multiline behavior.
	 */
	private StructConfig		struct						= new StructConfig();

	/**
	 * Controls formatting of property declarations including multiline
	 * behavior and key-value padding.
	 */
	private PropertyConfig		property					= new PropertyConfig();

	/**
	 * Controls formatting of array literals including padding and
	 * multiline behavior.
	 */
	private ArrayConfig			array						= new ArrayConfig();

	/**
	 * Controls spacing around semicolons in for-loop expressions.
	 */
	@JsonProperty( "for_loop_semicolons" )
	private ForLoopSemicolons	forLoopSemicolons			= new ForLoopSemicolons();

	/**
	 * Controls formatting of function declarations including style,
	 * parameter layout, and arrow function options.
	 */
	private FunctionConfig		function					= new FunctionConfig();

	/**
	 * Controls formatting of function call arguments including padding,
	 * trailing commas, and multiline behavior.
	 */
	private ArgumentsConfig		arguments					= new ArgumentsConfig();

	/**
	 * Controls brace placement style and whether braces are required
	 * for single-statement blocks.
	 */
	private BracesConfig		braces						= new BracesConfig();

	/**
	 * Controls operator formatting including line-break position,
	 * comparison style, and ternary layout.
	 */
	private OperatorsConfig		operators					= new OperatorsConfig();

	/**
	 * Controls when method chains break across multiple lines based
	 * on chain length and call count.
	 */
	private ChainConfig			chain						= new ChainConfig();

	/**
	 * Controls template/tag-based output including component prefix,
	 * content indentation, and self-closing tags.
	 */
	private TemplateConfig		template					= new TemplateConfig();

	/**
	 * Controls import statement formatting including sorting and grouping.
	 */
	@JsonProperty( "import" )
	private ImportConfig		importConfig				= new ImportConfig();

	/**
	 * Controls comment formatting including blank line preservation
	 * and line wrapping.
	 */
	private CommentsConfig		comments					= new CommentsConfig();

	/**
	 * Controls class and interface body formatting including member ordering,
	 * spacing, and method grouping.
	 */
	@JsonProperty( "class" )
	private ClassConfig			classConfig					= new ClassConfig();

	/**
	 * Controls SQL formatting inside query blocks including keyword casing
	 * and clause indentation.
	 */
	private SqlConfig			sql							= new SqlConfig();

	/** Default constructor. */
	public Config() {
	}

	/**
	 * Get the struct literal formatting configuration.
	 *
	 * @return the struct configuration
	 */
	public StructConfig getStruct() {
		return struct;
	}

	/**
	 * Set the struct literal formatting configuration.
	 *
	 * @param struct the struct configuration to set
	 *
	 * @return this config for chaining
	 */
	public Config setStruct( StructConfig struct ) {
		this.struct = struct;
		return this;
	}

	/**
	 * Set whether spaces are added around binary operators.
	 *
	 * @param value true to enable binary operator padding
	 *
	 * @return this config for chaining
	 */
	public Config setBinaryOperatorsPadding( boolean value ) {
		this.binaryOperatorsPadding = value;
		return this;
	}

	/**
	 * Get whether spaces are added around binary operators.
	 *
	 * @return true if binary operator padding is enabled
	 */
	public boolean getBinaryOperatorsPadding() {
		return this.binaryOperatorsPadding;
	}

	/**
	 * Get whether semicolons are appended after statements.
	 *
	 * @return true if semicolons are enabled
	 */
	public boolean getSemicolons() {
		return semicolons;
	}

	/**
	 * Set whether semicolons are appended after statements.
	 *
	 * @param semicolons true to enable semicolons
	 *
	 * @return this config for chaining
	 */
	public Config setSemicolons( boolean semicolons ) {
		this.semicolons = semicolons;
		return this;
	}

	/**
	 * Get whether cfformat compatibility mode is enabled.
	 *
	 * @return true if cfformat compatibility is enabled
	 */
	public boolean getCFFormatCompatibility() {
		return cfFormatCompatibility;
	}

	/**
	 * Set whether cfformat compatibility mode is enabled.
	 *
	 * @param cfFormatCompatibility true to enable cfformat compatibility
	 *
	 * @return this config for chaining
	 */
	public Config setCFFormatCompatibility( boolean cfFormatCompatibility ) {
		this.cfFormatCompatibility = cfFormatCompatibility;
		return this;
	}

	/**
	 * Get the source type override.
	 *
	 * @return the source type, or null if auto-detected
	 */
	public BoxSourceType getSourceType() {
		return sourceType;
	}

	/**
	 * Set the source type override.
	 *
	 * @param sourceType the source type to force, or null for auto-detection
	 *
	 * @return this config for chaining
	 */
	public Config setSourceType( BoxSourceType sourceType ) {
		this.sourceType = sourceType;
		return this;
	}

	/**
	 * Get the number of spaces per indentation level.
	 *
	 * @return the indent size
	 */
	public int getIndentSize() {
		return indentSize;
	}

	/**
	 * Set the number of spaces per indentation level.
	 *
	 * @param indentSize the indent size
	 *
	 * @return this config for chaining
	 */
	public Config setIndentSize( int indentSize ) {
		this.indentSize = indentSize;
		return this;
	}

	/**
	 * Get whether tab characters are used for indentation.
	 *
	 * @return true if tab indentation is enabled
	 */
	public boolean getTabIndent() {
		return tabIndent;
	}

	/**
	 * Set whether tab characters are used for indentation.
	 *
	 * @param tabIndent true to use tabs
	 *
	 * @return this config for chaining
	 */
	public Config setTabIndent( boolean tabIndent ) {
		this.tabIndent = tabIndent;
		return this;
	}

	/**
	 * Get the maximum line length before wrapping.
	 *
	 * @return the max line length
	 */
	public int getMaxLineLength() {
		return maxLineLength;
	}

	/**
	 * Set the maximum line length before wrapping.
	 *
	 * @param maxLineLength the max line length
	 *
	 * @return this config for chaining
	 */
	public Config setMaxLineLength( int maxLineLength ) {
		this.maxLineLength = maxLineLength;
		return this;
	}

	/**
	 * Get the line separator setting.
	 *
	 * @return the newLine setting ({@code "os"} or an explicit separator string)
	 */
	public String getNewLine() {
		return newLine;
	}

	/**
	 * Set the line separator.
	 *
	 * @param newLine the line separator ({@code "os"} for system default, or an explicit string)
	 *
	 * @return this config for chaining
	 */
	public Config setNewLine( String newLine ) {
		this.newLine = newLine;
		return this;
	}

	/**
	 * Get whether single quotes are used for strings.
	 *
	 * @return true if single quotes are used
	 */
	public boolean getSingleQuote() {
		return singleQuote;
	}

	/**
	 * Set whether single quotes are used for strings.
	 *
	 * @param singleQuote true to use single quotes
	 *
	 * @return this config for chaining
	 */
	public Config setSingleQuote( boolean singleQuote ) {
		this.singleQuote = singleQuote;
		return this;
	}

	/**
	 * Get whether original string quote style is preserved.
	 *
	 * @return true if string quotes are preserved
	 */
	public boolean getPreserveStringQuotes() {
		return preserveStringQuotes;
	}

	/**
	 * Set whether original string quote style is preserved.
	 *
	 * @param preserveStringQuotes true to preserve original quotes
	 *
	 * @return this config for chaining
	 */
	public Config setPreserveStringQuotes( boolean preserveStringQuotes ) {
		this.preserveStringQuotes = preserveStringQuotes;
		return this;
	}

	/**
	 * Get whether consecutive assignments are vertically aligned.
	 *
	 * @return true if alignment is enabled
	 */
	public boolean getAlignConsecutiveAssignments() {
		return alignConsecutiveAssignments;
	}

	/**
	 * Set whether consecutive assignments are vertically aligned.
	 *
	 * @param alignConsecutiveAssignments true to enable alignment
	 *
	 * @return this config for chaining
	 */
	public Config setAlignConsecutiveAssignments( boolean alignConsecutiveAssignments ) {
		this.alignConsecutiveAssignments = alignConsecutiveAssignments;
		return this;
	}

	/**
	 * Get whether consecutive properties are vertically aligned.
	 *
	 * @return true if alignment is enabled
	 */
	public boolean getAlignConsecutiveProperties() {
		return alignConsecutiveProperties;
	}

	/**
	 * Set whether consecutive properties are vertically aligned.
	 *
	 * @param alignConsecutiveProperties true to enable alignment
	 *
	 * @return this config for chaining
	 */
	public Config setAlignConsecutiveProperties( boolean alignConsecutiveProperties ) {
		this.alignConsecutiveProperties = alignConsecutiveProperties;
		return this;
	}

	/**
	 * Get whether spaces are added inside array brackets.
	 *
	 * @return true if bracket padding is enabled
	 */
	public boolean getBracketPadding() {
		return bracketPadding;
	}

	/**
	 * Set whether spaces are added inside array brackets.
	 *
	 * @param bracketPadding true to enable bracket padding
	 *
	 * @return this config for chaining
	 */
	public Config setBracketPadding( boolean bracketPadding ) {
		this.bracketPadding = bracketPadding;
		return this;
	}

	/**
	 * Get whether spaces are added inside parentheses.
	 *
	 * @return true if parentheses padding is enabled
	 */
	public boolean getParensPadding() {
		return parensPadding;
	}

	/**
	 * Set whether spaces are added inside parentheses.
	 *
	 * @param parensPadding true to enable parentheses padding
	 *
	 * @return this config for chaining
	 */
	public Config setParensPadding( boolean parensPadding ) {
		this.parensPadding = parensPadding;
		return this;
	}

	/**
	 * Get the for-loop semicolons configuration.
	 *
	 * @return the for-loop semicolons configuration
	 */
	public ForLoopSemicolons getForLoopSemicolons() {
		return forLoopSemicolons;
	}

	/**
	 * Set the for-loop semicolons configuration.
	 *
	 * @param forLoopSemicolons the for-loop semicolons configuration
	 *
	 * @return this config for chaining
	 */
	public Config setForLoopSemicolons( ForLoopSemicolons forLoopSemicolons ) {
		this.forLoopSemicolons = forLoopSemicolons;
		return this;
	}

	/**
	 * Get the property declaration formatting configuration.
	 *
	 * @return the property configuration
	 */
	public PropertyConfig getProperty() {
		return property;
	}

	/**
	 * Set the property declaration formatting configuration.
	 *
	 * @param property the property configuration
	 *
	 * @return this config for chaining
	 */
	public Config setProperty( PropertyConfig property ) {
		this.property = property;
		return this;
	}

	/**
	 * Get the array literal formatting configuration.
	 *
	 * @return the array configuration
	 */
	public ArrayConfig getArray() {
		return array;
	}

	/**
	 * Set the array literal formatting configuration.
	 *
	 * @param array the array configuration
	 *
	 * @return this config for chaining
	 */
	public Config setArray( ArrayConfig array ) {
		this.array = array;
		return this;
	}

	/**
	 * Get the function declaration formatting configuration.
	 *
	 * @return the function configuration
	 */
	public FunctionConfig getFunction() {
		return function;
	}

	/**
	 * Set the function declaration formatting configuration.
	 *
	 * @param function the function configuration
	 *
	 * @return this config for chaining
	 */
	public Config setFunction( FunctionConfig function ) {
		this.function = function;
		return this;
	}

	/**
	 * Get the function call argument formatting configuration.
	 *
	 * @return the arguments configuration
	 */
	public ArgumentsConfig getArguments() {
		return arguments;
	}

	/**
	 * Set the function call argument formatting configuration.
	 *
	 * @param arguments the arguments configuration
	 *
	 * @return this config for chaining
	 */
	public Config setArguments( ArgumentsConfig arguments ) {
		this.arguments = arguments;
		return this;
	}

	/**
	 * Get the brace formatting configuration.
	 *
	 * @return the braces configuration
	 */
	public BracesConfig getBraces() {
		return braces;
	}

	/**
	 * Set the brace formatting configuration.
	 *
	 * @param braces the braces configuration
	 *
	 * @return this config for chaining
	 */
	public Config setBraces( BracesConfig braces ) {
		this.braces = braces;
		return this;
	}

	/**
	 * Get the operator formatting configuration.
	 *
	 * @return the operators configuration
	 */
	public OperatorsConfig getOperators() {
		return operators;
	}

	/**
	 * Set the operator formatting configuration.
	 *
	 * @param operators the operators configuration
	 *
	 * @return this config for chaining
	 */
	public Config setOperators( OperatorsConfig operators ) {
		this.operators = operators;
		return this;
	}

	/**
	 * Get the method chain formatting configuration.
	 *
	 * @return the chain configuration
	 */
	public ChainConfig getChain() {
		return chain;
	}

	/**
	 * Set the method chain formatting configuration.
	 *
	 * @param chain the chain configuration
	 *
	 * @return this config for chaining
	 */
	public Config setChain( ChainConfig chain ) {
		this.chain = chain;
		return this;
	}

	/**
	 * Get the template formatting configuration.
	 *
	 * @return the template configuration
	 */
	public TemplateConfig getTemplate() {
		return template;
	}

	/**
	 * Set the template formatting configuration.
	 *
	 * @param template the template configuration
	 *
	 * @return this config for chaining
	 */
	public Config setTemplate( TemplateConfig template ) {
		this.template = template;
		return this;
	}

	/**
	 * Get the import statement formatting configuration.
	 *
	 * @return the import configuration
	 */
	public ImportConfig getImportConfig() {
		return importConfig;
	}

	/**
	 * Set the import statement formatting configuration.
	 *
	 * @param importConfig the import configuration
	 *
	 * @return this config for chaining
	 */
	public Config setImportConfig( ImportConfig importConfig ) {
		this.importConfig = importConfig;
		return this;
	}

	/**
	 * Get the comment formatting configuration.
	 *
	 * @return the comments configuration
	 */
	public CommentsConfig getComments() {
		return comments;
	}

	/**
	 * Set the comment formatting configuration.
	 *
	 * @param comments the comments configuration
	 *
	 * @return this config for chaining
	 */
	public Config setComments( CommentsConfig comments ) {
		this.comments = comments;
		return this;
	}

	/**
	 * Get the class/interface formatting configuration.
	 *
	 * @return the class configuration
	 */
	public ClassConfig getClassConfig() {
		return classConfig;
	}

	/**
	 * Set the class/interface formatting configuration.
	 *
	 * @param classConfig the class configuration
	 *
	 * @return this config for chaining
	 */
	public Config setClassConfig( ClassConfig classConfig ) {
		this.classConfig = classConfig;
		return this;
	}

	/**
	 * Get the SQL formatting configuration.
	 *
	 * @return the SQL configuration
	 */
	public SqlConfig getSql() {
		return sql;
	}

	/**
	 * Set the SQL formatting configuration.
	 *
	 * @param sql the SQL configuration
	 *
	 * @return this config for chaining
	 */
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
		return new Config().loadFromConfigFile( filePath );
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
		if ( filePath == null ) {
			return false;
		}
		String lowerPath = filePath.toLowerCase();
		return lowerPath.endsWith( ".cfformat.json" ) || lowerPath.endsWith( ".cfconfig.json" );
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

	/**
	 * Load configuration from a JSON file and apply it to this instance.
	 * Existing values are overridden by values present in the file.
	 *
	 * @param filePath path to the JSON configuration file
	 *
	 * @return this config for chaining
	 */
	@SuppressWarnings( "unchecked" )
	public Config loadFromConfigFile( String filePath ) {
		Map<String, Object> config = ( Map<String, Object> ) JSONUtil.fromJSON( new File( filePath ) );
		applyMapConfig( config );
		return this;
	}

	/**
	 * Apply configuration values from a map to this instance.
	 * Existing values are overridden by values present in the map.
	 *
	 * @param config a map of configuration key-value pairs
	 *
	 * @return this config for chaining
	 */
	public Config loadFromConfig( Map<String, Object> config ) {
		applyMapConfig( config );
		return this;
	}

	/**
	 * Calculate the column position for a given indentation level.
	 *
	 * @param indentLevel the indentation level (0-based)
	 *
	 * @return the column position in characters
	 */
	public int indentColumn( int indentLevel ) {
		return indentSize * indentLevel;
	}

	/**
	 * Generate an indentation string to reach a specific column position.
	 * Uses tabs or spaces based on the {@code tabIndent} setting.
	 *
	 * @param column the target column position
	 *
	 * @return the indentation string
	 */
	public String indentToColumn( int column ) {
		if ( tabIndent ) {
			int	tabs	= column / indentSize;
			int	spaces	= column % indentSize;
			return "\t".repeat( tabs ) + " ".repeat( spaces );
		}
		return " ".repeat( column );
	}

	/**
	 * Generate an indentation string for a given indentation level.
	 *
	 * @param indentLevel the indentation level (0-based)
	 *
	 * @return the indentation string
	 */
	public String indentToLevel( int indentLevel ) {
		return indentToColumn( indentColumn( indentLevel ) );
	}

	/**
	 * Get the resolved line separator string. Returns the OS default when
	 * {@code newLine} is set to {@code "os"}, otherwise returns the literal value.
	 *
	 * @return the line separator string
	 */
	public String lineSeparator() {
		return newLine.equals( "os" ) ? System.lineSeparator() : newLine;
	}

	/**
	 * Convert this entire configuration to a map for JSON serialization.
	 *
	 * @return a map representation of this configuration
	 */
	public Map<String, Object> toMap() {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put( "indentSize", indentSize );
		map.put( "tabIndent", tabIndent );
		map.put( "maxLineLength", maxLineLength );
		map.put( "newLine", newLine );
		map.put( "singleQuote", singleQuote );
		map.put( "preserveStringQuotes", preserveStringQuotes );
		map.put( "alignConsecutiveAssignments", alignConsecutiveAssignments );
		map.put( "alignConsecutiveProperties", alignConsecutiveProperties );
		map.put( "bracketPadding", bracketPadding );
		map.put( "parensPadding", parensPadding );
		map.put( "binaryOperatorsPadding", binaryOperatorsPadding );
		map.put( "semicolons", semicolons );
		map.put( "cfFormatCompatibility", cfFormatCompatibility );
		map.put( "sourceType", sourceType != null ? sourceType.name() : null );
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

	/**
	 * Convert this configuration to a formatted JSON string.
	 *
	 * @return a pretty-printed JSON string
	 */
	public String toJSON() {
		try {
			return JSON.std.with( Feature.PRETTY_PRINT_OUTPUT, Feature.WRITE_NULL_PROPERTIES )
			    .asString( toMap() );
		} catch ( IOException e ) {
			e.printStackTrace();
			throw new RuntimeException( "Failed to convert to JSON", e );
		}
	}

	/**
	 * Create a deep copy of this configuration, recursively cloning all nested config objects.
	 *
	 * @return a new Config with the same settings
	 */
	public Config clone() {
		Config clone = new Config();
		clone.indentSize					= this.indentSize;
		clone.tabIndent						= this.tabIndent;
		clone.maxLineLength					= this.maxLineLength;
		clone.newLine						= this.newLine;
		clone.singleQuote					= this.singleQuote;
		clone.preserveStringQuotes			= this.preserveStringQuotes;
		clone.alignConsecutiveAssignments	= this.alignConsecutiveAssignments;
		clone.alignConsecutiveProperties	= this.alignConsecutiveProperties;
		clone.bracketPadding				= this.bracketPadding;
		clone.parensPadding					= this.parensPadding;
		clone.binaryOperatorsPadding		= this.binaryOperatorsPadding;
		clone.semicolons					= this.semicolons;
		clone.cfFormatCompatibility			= this.cfFormatCompatibility;
		clone.sourceType					= this.sourceType;
		clone.struct						= this.struct.clone();
		clone.property						= this.property.clone();
		clone.array							= this.array.clone();
		clone.forLoopSemicolons				= this.forLoopSemicolons.clone();
		clone.function						= this.function.clone();
		clone.arguments						= this.arguments.clone();
		clone.braces						= this.braces.clone();
		clone.operators						= this.operators.clone();
		clone.chain							= this.chain.clone();
		clone.template						= this.template.clone();
		clone.importConfig					= this.importConfig.clone();
		clone.comments						= this.comments.clone();
		clone.classConfig					= this.classConfig.clone();
		clone.sql							= this.sql.clone();
		return clone;
	}

	/**
	 * Apply configuration values from a map to this instance, handling type
	 * checking and nested config objects.
	 *
	 * @param config a map of configuration key-value pairs
	 */
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
		if ( config.containsKey( "preserveStringQuotes" ) && config.get( "preserveStringQuotes" ) instanceof Boolean preserveStringQuotes ) {
			this.preserveStringQuotes = preserveStringQuotes;
		}
		if ( config.containsKey( "alignConsecutiveAssignments" )
		    && config.get( "alignConsecutiveAssignments" ) instanceof Boolean alignConsecutiveAssignments ) {
			this.alignConsecutiveAssignments = alignConsecutiveAssignments;
		}
		if ( config.containsKey( "alignConsecutiveProperties" ) && config.get( "alignConsecutiveProperties" ) instanceof Boolean alignConsecutiveProperties ) {
			this.alignConsecutiveProperties = alignConsecutiveProperties;
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
		if ( config.containsKey( "sourceType" ) && config.get( "sourceType" ) instanceof String sourceType ) {
			try {
				this.sourceType = BoxSourceType.valueOf( sourceType.toUpperCase() );
			} catch ( IllegalArgumentException e ) {
				// ignore invalid source type
			}
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

	/**
	 * Apply struct configuration values from a map.
	 *
	 * @param config a map of struct configuration key-value pairs
	 */
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

	/**
	 * Apply array configuration values from a map.
	 *
	 * @param config a map of array configuration key-value pairs
	 */
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

	/**
	 * Apply property configuration values from a map.
	 *
	 * @param config a map of property configuration key-value pairs
	 */
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

	/**
	 * Apply for-loop semicolons configuration values from a map.
	 *
	 * @param config a map of for-loop semicolons configuration key-value pairs
	 */
	private void applyForLoopSemicolonsConfig( Map<String, Object> config ) {
		if ( config.containsKey( "padding" ) && config.get( "padding" ) instanceof Boolean padding ) {
			this.forLoopSemicolons.setPadding( padding );
		}
	}

	/**
	 * Apply function configuration values from a map.
	 *
	 * @param config a map of function configuration key-value pairs
	 */
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

	/**
	 * Apply parameters configuration values from a map.
	 *
	 * @param config a map of parameters configuration key-value pairs
	 */
	private void applyParametersConfig( Map<String, Object> config ) {
		if ( config.containsKey( "padding" ) && config.get( "padding" ) instanceof Boolean padding ) {
			this.function.getParameters().setPadding( padding );
		}
		if ( config.containsKey( "empty_padding" ) && config.get( "empty_padding" ) instanceof Boolean emptyPadding ) {
			this.function.getParameters().setEmptyPadding( emptyPadding );
		}
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

	/**
	 * Apply arrow function configuration values from a map.
	 *
	 * @param config a map of arrow configuration key-value pairs
	 */
	private void applyArrowConfig( Map<String, Object> config ) {
		if ( config.containsKey( "parens" ) && config.get( "parens" ) instanceof String parens ) {
			this.function.getArrow().setParens( parens );
		}
	}

	/**
	 * Apply multiline configuration values from a map to a {@link MultilineConfig} instance.
	 *
	 * @param multiline the multiline config instance to update
	 * @param config    a map of multiline configuration key-value pairs
	 */
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

	/**
	 * Apply arguments configuration values from a map.
	 *
	 * @param config a map of arguments configuration key-value pairs
	 */
	private void applyArgumentsConfig( Map<String, Object> config ) {
		if ( config.containsKey( "padding" ) && config.get( "padding" ) instanceof Boolean padding ) {
			this.arguments.setPadding( padding );
		}
		if ( config.containsKey( "empty_padding" ) && config.get( "empty_padding" ) instanceof Boolean emptyPadding ) {
			this.arguments.setEmptyPadding( emptyPadding );
		}
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

	/**
	 * Apply braces configuration values from a map.
	 *
	 * @param config a map of braces configuration key-value pairs
	 */
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

	/**
	 * Apply else configuration values from a map.
	 *
	 * @param config a map of else configuration key-value pairs
	 */
	private void applyElseConfig( Map<String, Object> config ) {
		if ( config.containsKey( "style" ) && config.get( "style" ) instanceof String style ) {
			this.braces.getElseConfig().setStyle( style );
		}
	}

	/**
	 * Apply operators configuration values from a map.
	 *
	 * @param config a map of operators configuration key-value pairs
	 */
	@SuppressWarnings( "unchecked" )
	private void applyOperatorsConfig( Map<String, Object> config ) {
		if ( config.containsKey( "position" ) && config.get( "position" ) instanceof String position ) {
			this.operators.setPosition( position );
		}
		if ( config.containsKey( "comparison_style" ) && config.get( "comparison_style" ) instanceof String comparisonStyle ) {
			this.operators.setComparisonStyle( comparisonStyle );
		}
		if ( config.containsKey( "ternary" ) && config.get( "ternary" ) instanceof Map ternaryMap ) {
			applyTernaryConfig( ( Map<String, Object> ) ternaryMap );
		}
	}

	/**
	 * Apply ternary configuration values from a map.
	 *
	 * @param config a map of ternary configuration key-value pairs
	 */
	private void applyTernaryConfig( Map<String, Object> config ) {
		if ( config.containsKey( "style" ) && config.get( "style" ) instanceof String style ) {
			this.operators.getTernary().setStyle( style );
		}
		if ( config.containsKey( "question_position" ) && config.get( "question_position" ) instanceof String questionPosition ) {
			this.operators.getTernary().setQuestionPosition( questionPosition );
		}
	}

	/**
	 * Apply chain configuration values from a map.
	 *
	 * @param config a map of chain configuration key-value pairs
	 */
	private void applyChainConfig( Map<String, Object> config ) {
		if ( config.containsKey( "break_count" ) && config.get( "break_count" ) instanceof Number breakCount ) {
			this.chain.setBreakCount( breakCount.intValue() );
		}
		if ( config.containsKey( "break_length" ) && config.get( "break_length" ) instanceof Number breakLength ) {
			this.chain.setBreakLength( breakLength.intValue() );
		}
	}

	/**
	 * Apply template configuration values from a map.
	 *
	 * @param config a map of template configuration key-value pairs
	 */
	private void applyTemplateConfig( Map<String, Object> config ) {
		if ( config.containsKey( "enabled" ) && config.get( "enabled" ) instanceof Boolean enabled ) {
			this.template.setEnabled( enabled );
		}
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

	/**
	 * Apply import configuration values from a map.
	 *
	 * @param config a map of import configuration key-value pairs
	 */
	private void applyImportConfig( Map<String, Object> config ) {
		if ( config.containsKey( "sort" ) && config.get( "sort" ) instanceof Boolean sort ) {
			this.importConfig.setSort( sort );
		}
		if ( config.containsKey( "group" ) && config.get( "group" ) instanceof Boolean group ) {
			this.importConfig.setGroup( group );
		}
	}

	/**
	 * Apply comments configuration values from a map.
	 *
	 * @param config a map of comments configuration key-value pairs
	 */
	private void applyCommentsConfig( Map<String, Object> config ) {
		if ( config.containsKey( "preserve_blank_lines" ) && config.get( "preserve_blank_lines" ) instanceof Boolean preserveBlankLines ) {
			this.comments.setPreserveBlankLines( preserveBlankLines );
		}
		if ( config.containsKey( "wrap" ) && config.get( "wrap" ) instanceof Boolean wrap ) {
			this.comments.setWrap( wrap );
		}
	}

	/**
	 * Apply class configuration values from a map.
	 *
	 * @param config a map of class configuration key-value pairs
	 */
	private void applyClassConfig( Map<String, Object> config ) {
		if ( config.containsKey( "member_order" ) && config.get( "member_order" ) instanceof String memberOrder ) {
			this.classConfig.setMemberOrder( memberOrder );
		}
		if ( config.containsKey( "member_spacing" ) && config.get( "member_spacing" ) instanceof Number memberSpacing ) {
			this.classConfig.setMemberSpacing( memberSpacing.intValue() );
		}
		if ( config.containsKey( "property_order" ) && config.get( "property_order" ) instanceof String propertyOrder ) {
			this.classConfig.setPropertyOrder( propertyOrder );
		}
		if ( config.containsKey( "method_order" ) && config.get( "method_order" ) instanceof String methodOrder ) {
			this.classConfig.setMethodOrder( methodOrder );
		}
		if ( config.containsKey( "method_grouping" ) && config.get( "method_grouping" ) instanceof Boolean methodGrouping ) {
			this.classConfig.setMethodGrouping( methodGrouping );
		}
		if ( config.containsKey( "property_spacing" ) && config.get( "property_spacing" ) instanceof Number propSpacing ) {
			this.classConfig.setPropertySpacing( propSpacing.intValue() );
		}
	}

	/**
	 * Apply SQL configuration values from a map.
	 *
	 * @param config a map of SQL configuration key-value pairs
	 */
	private void applySqlConfig( Map<String, Object> config ) {
		if ( config.containsKey( "uppercase_keywords" ) && config.get( "uppercase_keywords" ) instanceof Boolean uppercaseKeywords ) {
			this.sql.setUppercaseKeywords( uppercaseKeywords );
		}
		if ( config.containsKey( "indent_clauses" ) && config.get( "indent_clauses" ) instanceof Boolean indentClauses ) {
			this.sql.setIndentClauses( indentClauses );
		}
	}

	/**
	 * Parse a separator string into a {@link Separator} enum value.
	 *
	 * @param separator the separator string (e.g. {@code ":"}, {@code " = "})
	 *
	 * @return the matching {@link Separator} enum value, defaulting to {@link Separator#COLON_SPACE}
	 */
	private Separator parseSeparator( String separator ) {
		return switch ( separator ) {
			case ":" -> Separator.COLON;
			case "=" -> Separator.EQUALS;
			case ": " -> Separator.COLON_SPACE;
			case " : " -> Separator.COLON_BOTH_SPACE;
			case "= " -> Separator.EQUALS_SPACE;
			case " = " -> Separator.EQUALS_BOTH_SPACE;
			default -> Separator.COLON_SPACE;
		};
	}
}