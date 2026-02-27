package ortus.boxlang.compiler.prettyprint;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import ortus.boxlang.compiler.ast.BoxClass;
import ortus.boxlang.compiler.ast.BoxInterface;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.BoxScript;
import ortus.boxlang.compiler.ast.BoxTemplate;
import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.compiler.parser.Parser;
import ortus.boxlang.compiler.prettyprint.config.CFFormatConfigLoader;
import ortus.boxlang.compiler.prettyprint.config.Config;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

public final class PrettyPrint {

	// @formatter:on
	private static final String DEFAULT_CONFIG = """
	                                             	{
	                                             		"indentSize" : 4,
	                                             		"tabIndent" : true,
	                                             		"maxLineLength" : 120,
	                                             		"newLine" : "os",
	                                             		"singleQuote" : false,
	                                             		"bracketPadding" : true,
	                                             		"parensPadding" : true,
	                                             		"binaryOperatorsPadding" : true,
	                                             		"semicolons" : true,
	                                             		"struct" : {
	                                             		"padding" : true,
	                                             		"empty_padding" : false,
	                                             		"quote_keys" : false,
	                                             		"separator" : ": ",
	                                             		"multiline" : {
	                                             			"element_count" : 2,
	                                             			"comma_dangle" : false,
	                                             			"leading_comma" : {
	                                             			"enabled" : false,
	                                             			"padding" : true
	                                             			},
	                                             			"min_length" : 40
	                                             		}
	                                             		},
	                                             		"property" : {
	                                             		"multiline" : {
	                                             			"element_count" : 4,
	                                             			"comma_dangle" : false,
	                                             			"leading_comma" : {
	                                             			"enabled" : false,
	                                             			"padding" : true
	                                             			},
	                                             			"min_length" : 40
	                                             		},
	                                             		"key_value" : {
	                                             			"padding" : false
	                                             		}
	                                             		},
	                                             		"array" : {
	                                             		"padding" : true,
	                                             		"empty_padding" : false,
	                                             		"multiline" : {
	                                             			"element_count" : 2,
	                                             			"comma_dangle" : false,
	                                             			"leading_comma" : {
	                                             			"enabled" : false,
	                                             			"padding" : true
	                                             			},
	                                             			"min_length" : 40
	                                             		}
	                                             		},
	                                             		"for_loop_semicolons" : {
	                                             		"padding" : true
	                                             		},
	                                             		"function" : {
	                                             		"style" : "preserve",
	                                             		"parameters" : {
	                                             			"comma_dangle" : false,
	                                             			"multiline_count" : 3,
	                                             			"multiline_length" : 40
	                                             		},
	                                             		"arrow" : {
	                                             			"parens" : "always"
	                                             		}
	                                             		},
	                                             		"arguments" : {
	                                             		"comma_dangle" : false,
	                                             		"multiline_count" : 3,
	                                             		"multiline_length" : 40
	                                             		},
	                                             		"braces" : {
	                                             		"style" : "same-line",
	                                             		"require_for_single_statement" : true,
	                                             		"else" : {
	                                             			"style" : "same-line"
	                                             		}
	                                             		},
	                                             		"operators" : {
	                                             		"position" : "end",
	                                             		"ternary" : {
	                                             			"style" : "flat",
	                                             			"question_position" : "start"
	                                             		}
	                                             		},
	                                             		"chain" : {
	                                             		"break_count" : 3,
	                                             		"break_length" : 60
	                                             		},
	                                             		"template" : {
	                                             		"component_prefix" : "bx",
	                                             		"indent_content" : true,
	                                             		"single_attribute_per_line" : false,
	                                             		"self_closing" : true
	                                             		},
	                                             		"import" : {
	                                             		"sort" : false,
	                                             		"group" : false
	                                             		},
	                                             		"comments" : {
	                                             		"preserve_blank_lines" : true,
	                                             		"wrap" : false
	                                             		},
	                                             		"class" : {
	                                             		"member_order" : "preserve",
	                                             		"member_spacing" : 1,
	                                             		"property_order" : "preserve",
	                                             		"method_order" : "preserve",
	                                             		"method_grouping" : false
	                                             		},
	                                             		"sql" : {
	                                             		"uppercase_keywords" : true,
	                                             		"indent_clauses" : true
	                                             		}
	                                             	}
	                                             """;
	// @formatter:off

	private PrettyPrint() {
		// Prevent instantiation
	}

	public static void main( String[] args ) {
		// initialize BoxRuntime if not already done
		BoxRuntime.getInstance();

		// process cli args
		// --check (make no changes, just check if the file is already formatted, return an error code if not)
		// -c, --config <config file> (defaults to .bxformat.json in the current working directory, falls back to .cfformat.json)
		// -i, --input <input file/folder> (defaults to current working directory)
		// -o, --output <output file/folder> (optional, if not provided, overwrite input files)
		// --convertConfig (convert .cfformat.json to .bxformat.json)
		try {
			boolean	checkMode		= false;
			String	configPath		= null; // null means use fallback logic
			String	inputPath		= System.getProperty( "user.dir" );
			String	outputPath		= null;
			boolean	convertConfig	= false;

			for ( int i = 0; i < args.length; i++ ) {
				if ( args[ i ].equalsIgnoreCase( "--help" ) || args[ i ].equalsIgnoreCase( "-h" ) ) {
					printHelp();
					System.exit( 0 );
				} else if ( args[ i ].equalsIgnoreCase( "--initConfig" ) ) {
					initConfig();
					System.exit( 0 );
				} else if ( args[ i ].equalsIgnoreCase( "--convertConfig" ) ) {
					convertConfig = true;
				} else if ( args[ i ].equalsIgnoreCase( "--check" ) ) {
					checkMode = true;
				} else if ( ( args[ i ].equalsIgnoreCase( "--config" ) || args[ i ].equalsIgnoreCase( "-c" ) ) && i + 1 < args.length ) {
					configPath = args[ ++i ];
				} else if ( ( args[ i ].equalsIgnoreCase( "--input" ) || args[ i ].equalsIgnoreCase( "-i" ) ) && i + 1 < args.length ) {
					inputPath = args[ ++i ];
				} else if ( ( args[ i ].equalsIgnoreCase( "--output" ) || args[ i ].equalsIgnoreCase( "-o" ) ) && i + 1 < args.length ) {
					outputPath = args[ ++i ];
				}
			}

			// Handle --convertConfig option
			if ( convertConfig ) {
				convertCFFormatConfig( inputPath );
				System.exit( 0 );
			}

			// Load config with fallback logic if no explicit path provided
			Config config;
			if ( configPath != null ) {
				// Explicit config path provided - auto-detect format
				config = Config.loadConfigAutoDetect( configPath );
			} else {
				// Use fallback logic: .bxformat.json -> .cfformat.json -> default
				config = Config.loadConfigWithFallback( System.getProperty( "user.dir" ) );
			}
			// get a stream of files to process either from a single file or a directory
			Stream<Path> filesToProcess;

			if ( Files.isDirectory( Paths.get( inputPath ) ) ) {
				filesToProcess = Files.walk( Paths.get( inputPath ) )
				    .filter( p -> !Files.isDirectory( p ) )
				    .filter( p -> {
					    String fileName = p.getFileName().toString().toLowerCase();
					    return fileName.endsWith( ".bx" ) || fileName.endsWith( ".bxs" ) || fileName.endsWith( ".bxm" )
					        || fileName.endsWith( ".cfm" ) || fileName.endsWith( ".cfc" ) || fileName.endsWith( ".cfs" );
				    } );
			} else {
				filesToProcess = Stream.of( Paths.get( inputPath ) );
			}

			final boolean	finalCheckMode	= checkMode;
			final String	finalOutputPath	= outputPath;
			filesToProcess.forEach( path -> {
				try {
					var		parser			= new Parser();
					var		parsingResult	= parser.parse( path.toFile(), false );
					String	formattedCode	= PrettyPrint.prettyPrint( parsingResult.getRoot(), config );

					if ( finalCheckMode ) {
						String originalCode = Files.readString( path );
						if ( !originalCode.equals( formattedCode ) ) {
							System.out.println( "File needs formatting: " + path.toString() );
							System.exit( 1 );
						}
					} else {
						Path outputFilePath;
						if ( finalOutputPath != null ) {
							outputFilePath = Paths.get( finalOutputPath );
							if ( Files.isDirectory( outputFilePath ) ) {
								outputFilePath = outputFilePath.resolve( path.getFileName() );
							}
						} else {
							outputFilePath = path;
						}
						Files.writeString( outputFilePath, formattedCode );
						System.out.println( "Formatted file: " + outputFilePath.toString() );
					}
				} catch ( Exception e ) {
					System.err.println( "Error processing file " + path.toString() + ": " + e.getMessage() );
				}
			} );

		} catch ( Exception e ) {
			System.err.println( "Error: " + e.getMessage() );
			System.exit( 1 );
		}
	}

	private static void initConfig() {
		// check if file exists
		java.io.File configFile = new java.io.File( System.getProperty( "user.dir" ) + "/.bxformat.json" );
		if ( configFile.exists() ) {
			System.err.println( "Configuration file already exists at " + configFile.getAbsolutePath() );
			return;
		}

		try ( java.io.FileWriter writer = new java.io.FileWriter( configFile ) ) {
			writer.write( DEFAULT_CONFIG );
			System.out.println( "Default configuration file created at " + configFile.getAbsolutePath() );
		} catch ( java.io.IOException e ) {
			System.err.println( "Error creating configuration file: " + e.getMessage() );
		}
	}

	/**
	 * Convert a .cfformat.json file to .bxformat.json
	 *
	 * @param directory The directory containing the .cfformat.json file (or path to the file itself)
	 */
	private static void convertCFFormatConfig( String directory ) {
		java.io.File	inputFile;
		java.io.File	outputFile;

		// Check if the input is a file or directory
		java.io.File	input	= new java.io.File( directory );
		if ( input.isFile() && directory.toLowerCase().endsWith( ".cfformat.json" ) ) {
			inputFile	= input;
			outputFile	= new java.io.File( input.getParentFile(), ".bxformat.json" );
		} else {
			// It's a directory, look for .cfformat.json
			inputFile	= new java.io.File( directory, ".cfformat.json" );
			outputFile	= new java.io.File( directory, ".bxformat.json" );
		}

		if ( !inputFile.exists() ) {
			System.err.println( "CFFormat configuration file not found: " + inputFile.getAbsolutePath() );
			System.exit( 1 );
		}

		if ( outputFile.exists() ) {
			System.err.println( "BoxLang format configuration already exists: " + outputFile.getAbsolutePath() );
			System.err.println( "Please remove or rename it before converting." );
			System.exit( 1 );
		}

		try {
			CFFormatConfigLoader.convertAndWriteBoxFormatFile( inputFile.getAbsolutePath(), outputFile.getAbsolutePath() );
			System.out.println( "Successfully converted CFFormat configuration!" );
			System.out.println( "  From: " + inputFile.getAbsolutePath() );
			System.out.println( "  To:   " + outputFile.getAbsolutePath() );
		} catch ( java.io.IOException e ) {
			System.err.println( "Error converting configuration: " + e.getMessage() );
			System.exit( 1 );
		}
	}

	public static String prettyPrint( BoxNode node ) {
		return prettyPrint( node, new Config() );
	}

	public static String prettyPrint( BoxNode node, Config config ) {
		var doc = generateDoc( node, config );
		return printDoc( doc, config );
	}

	public static Doc generateDoc( BoxNode node, Config config ) {
		BoxSourceType sourceType;
		if ( node instanceof BoxScript boxScriptNode ) {
			sourceType = boxScriptNode.getBoxSourceType();
		} else if ( node instanceof BoxTemplate boxTemplateNode ) {
			sourceType = boxTemplateNode.getBoxSourceType();
		} else if ( node instanceof BoxClass boxClassNode ) {
			sourceType = boxClassNode.getBoxSourceType();
		} else if ( node instanceof BoxInterface boxInterfaceNode ) {
			sourceType = boxInterfaceNode.getBoxSourceType();
		} else {
			throw new BoxRuntimeException( "Unexpected BoxNode type: " + node.getClass().getName() );
		}
		Visitor visitor = new Visitor( sourceType, config );
		node.accept( visitor );
		var doc = visitor.getRoot();
		doc.condense();
		doc.propagateWillBreak();
		return doc;
	}

	public static String printDoc( Doc doc, Config config ) {
		var printer = new Printer( config );
		return printer.print( doc );
	}

	/**
	 * Prints the help message for the BoxLang Formatter tool.
	 */
	private static void printHelp() {
		System.out.println( "BoxLang Formatter - A CLI tool for formatting BoxLang code" );
		System.out.println();
		System.out.println( "USAGE:" );
		System.out.println( "  boxlang format [OPTIONS]" );
		System.out.println( "  java -jar boxlang.jar ortus.boxlang.compiler.PrettyPrint [OPTIONS]" );
		System.out.println();
		System.out.println( "OPTIONS:" );
		System.out.println( "  -h, --help                  Show this help message and exit" );
		System.out.println( "  -i, --input <PATH>          Path to source directory or file to format (default: current directory)" );
		System.out.println( "  -o, --output <PATH>         Path to output directory or file (default: overwrite input files)" );
		System.out.println( "  -c, --config <PATH>         Path to configuration file (default: .bxformat.json or .cfformat.json)" );
		System.out.println( "      --check                 Only check if files need formatting (exit code 1 if changes needed)" );
		System.out.println( "      --initConfig            Create a default .bxformat.json configuration file" );
		System.out.println( "      --convertConfig         Convert .cfformat.json to .bxformat.json" );
		System.out.println();
		System.out.println( "CONFIGURATION:" );
		System.out.println( "  The formatter looks for configuration files in this order:" );
		System.out.println( "    1. .bxformat.json (preferred)" );
		System.out.println( "    2. .cfformat.json (CFFormat compatibility - auto-converted)" );
		System.out.println( "    3. Default settings" );
		System.out.println();
		System.out.println( "  You can also specify a config file explicitly with -c/--config." );
		System.out.println( "  Both .bxformat.json and .cfformat.json formats are supported." );
		System.out.println();
		System.out.println( "EXAMPLES:" );
		System.out.println( "  # Format all BoxLang files in the current directory" );
		System.out.println( "  boxlang format" );
		System.out.println();
		System.out.println( "  # Check files for formatting needs" );
		System.out.println( "  boxlang format --check" );
		System.out.println();
		System.out.println( "  # Format a single file" );
		System.out.println( "  boxlang format --input /path/to/file.cfm" );
		System.out.println();
		System.out.println( "  # Use a specific config file" );
		System.out.println( "  boxlang format --config /path/to/.bxformat.json" );
		System.out.println();
		System.out.println( "  # Convert CFFormat config to BoxLang format" );
		System.out.println( "  boxlang format --convertConfig" );
		System.out.println();
		System.out.println( "More Information:" );
		System.out.println( "  Documentation: https://boxlang.ortusbooks.com/" );
		System.out.println( "  Community: https://community.ortussolutions.com/c/boxlang/42" );
		System.out.println( "  GitHub: https://github.com/ortus-boxlang" );
		System.out.println();
	}

}
