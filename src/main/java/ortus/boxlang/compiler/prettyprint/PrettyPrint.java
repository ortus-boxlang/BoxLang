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
package ortus.boxlang.compiler.prettyprint;

import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import ortus.boxlang.compiler.ast.BoxClass;
import ortus.boxlang.compiler.ast.BoxInterface;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.BoxScript;

import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.compiler.parser.Parser;
import ortus.boxlang.compiler.prettyprint.config.CFFormatConfigLoader;
import ortus.boxlang.compiler.prettyprint.config.Config;
import ortus.boxlang.runtime.BoxRuntime;


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
		System.exit( run( args, System.out, System.err ) );
	}

	static int run( String[] args, PrintStream out, PrintStream err ) {
		// initialize BoxRuntime if not already done
		BoxRuntime.getInstance();

		// process cli args
		// --check (make no changes, just check if the file is already formatted, return an error code if not)
		// -c, --config <config file> (defaults to .bxformat.json in the current working directory, falls back to .cfformat.json)
		// -i, --input <input file/folder> (defaults to current working directory)
		// -o, --output <output file/folder> (optional, if not provided, overwrite input files)
		// --overwrite <true|false> (optional, default true. when false, print to stdout)
		// --convertConfig (convert .cfformat.json to .bxformat.json)
		try {
			boolean	checkMode		= false;
			String	configPath		= null; // null means use fallback logic
			String	inputPath		= System.getProperty( "user.dir" );
			String	outputPath		= null;
			boolean	convertConfig	= false;
			boolean	overwrite		= true;

			for ( int i = 0; i < args.length; i++ ) {
				if ( args[ i ].equalsIgnoreCase( "--help" ) || args[ i ].equalsIgnoreCase( "-h" ) ) {
					printHelp( out );
					return 0;
				} else if ( args[ i ].equalsIgnoreCase( "--initConfig" ) ) {
					initConfig( out, err );
					return 0;
				} else if ( args[ i ].equalsIgnoreCase( "--convertConfig" ) ) {
					convertConfig = true;
				} else if ( args[ i ].equalsIgnoreCase( "--check" ) ) {
					checkMode = true;
				} else if ( args[ i ].equalsIgnoreCase( "--overwrite" ) && i + 1 < args.length ) {
					overwrite = Boolean.parseBoolean( args[ ++i ] );
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
				return convertCFFormatConfig( inputPath, out, err );
			}

			if ( !overwrite && outputPath != null ) {
				err.println( "Error: --output cannot be used when --overwrite=false" );
				return 1;
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

			List<Path> pathsToProcess = new ArrayList<>();
			try ( filesToProcess ) {
				filesToProcess.forEach( pathsToProcess::add );
			}

			boolean needsFormatting = false;

			for ( Path path : pathsToProcess ) {
				try {
					var		parser			= new Parser();
					var		parsingResult	= parser.parse( path.toFile(), false );
					String	formattedCode	= PrettyPrint.prettyPrint( parsingResult.getRoot(), config );

					if ( checkMode ) {
						String originalCode = Files.readString( path );
						if ( !originalCode.equals( formattedCode ) ) {
							out.println( "File needs formatting: " + path.toString() );
							needsFormatting = true;
						}
						continue;
					}

					if ( !overwrite ) {
						out.println( "=== " + path.toString() + " ===" );
						out.print( formattedCode );
						if ( !formattedCode.endsWith( config.lineSeparator() ) ) {
							out.println();
						}
						continue;
					}

					Path outputFilePath;
					if ( outputPath != null ) {
						outputFilePath = Paths.get( outputPath );
						if ( Files.isDirectory( outputFilePath ) ) {
							outputFilePath = outputFilePath.resolve( path.getFileName() );
						}
					} else {
						outputFilePath = path;
					}
					Files.writeString( outputFilePath, formattedCode );
					out.println( "Formatted file: " + outputFilePath.toString() );
				} catch ( Exception e ) {
					err.println( "Error processing file " + path.toString() + ": " + e.getMessage() );
					return 1;
				}
			}

			return needsFormatting ? 1 : 0;

		} catch ( Exception e ) {
			err.println( "Error: " + e.getMessage() );
			return 1;
		}
	}

	private static void initConfig( PrintStream out, PrintStream err ) {
		// check if file exists
		java.io.File configFile = new java.io.File( System.getProperty( "user.dir" ) + "/.bxformat.json" );
		if ( configFile.exists() ) {
			err.println( "Configuration file already exists at " + configFile.getAbsolutePath() );
			return;
		}

		try ( java.io.FileWriter writer = new java.io.FileWriter( configFile ) ) {
			writer.write( DEFAULT_CONFIG );
			out.println( "Default configuration file created at " + configFile.getAbsolutePath() );
		} catch ( java.io.IOException e ) {
			err.println( "Error creating configuration file: " + e.getMessage() );
		}
	}

	/**
	 * Convert a .cfformat.json file to .bxformat.json
	 *
	 * @param directory The directory containing the .cfformat.json file (or path to the file itself)
	 */
	private static int convertCFFormatConfig( String directory, PrintStream out, PrintStream err ) {
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
			err.println( "CFFormat configuration file not found: " + inputFile.getAbsolutePath() );
			return 1;
		}

		if ( outputFile.exists() ) {
			err.println( "BoxLang format configuration already exists: " + outputFile.getAbsolutePath() );
			err.println( "Please remove or rename it before converting." );
			return 1;
		}

		try {
			CFFormatConfigLoader.convertAndWriteBoxFormatFile( inputFile.getAbsolutePath(), outputFile.getAbsolutePath() );
			out.println( "Successfully converted CFFormat configuration!" );
			out.println( "  From: " + inputFile.getAbsolutePath() );
			out.println( "  To:   " + outputFile.getAbsolutePath() );
			return 0;
		} catch ( java.io.IOException e ) {
			err.println( "Error converting configuration: " + e.getMessage() );
			return 1;
		}
	}

	public static String prettyPrint( BoxNode node ) {
		return prettyPrint( node, new Config() );
	}

	public static String prettyPrint( BoxNode node, Config config ) {
		var doc = generateDoc( node, config );
		String output = printDoc( doc, config );
		output = output.replaceAll( "(?m)(^\\s*param\\s+[^=\\r\\n]*?)\\s+=\\s+", "$1 = " );
		// cfformat always produces a trailing newline; match this behaviour in CF compat mode
		if ( config.getCFFormatCompatibility() && !output.endsWith( "\n" ) ) {
			output = output + "\n";
		}
		return output;
	}

	public static Doc generateDoc( BoxNode node, Config config ) {
		Visitor visitor = new Visitor( resolveSourceType( node ), config );
		node.accept( visitor );
		var doc = visitor.getRoot();
		doc.condense();
		doc.propagateWillBreak();
		return doc;
	}

	/**
	 * Walk up the node's parent chain to find a root node that carries source type
	 * information. Falls back to BOXSCRIPT when called on a detached sub-node.
	 */
	private static BoxSourceType resolveSourceType( BoxNode node ) {
		BoxNode current = node;
		while ( current != null ) {
			if ( current instanceof BoxScript s ) return s.getBoxSourceType();
			if ( current instanceof BoxClass c ) return c.getBoxSourceType();
			if ( current instanceof BoxInterface i ) return i.getBoxSourceType();
			current = current.getParent();
		}
		return BoxSourceType.BOXSCRIPT;
	}

	public static String printDoc( Doc doc, Config config ) {
		var printer = new Printer( config );
		return printer.print( doc );
	}

	/**
	 * Prints the help message for the BoxLang Formatter tool.
	 */
	private static void printHelp( PrintStream out ) {
		out.println( "BoxLang Formatter - A CLI tool for formatting BoxLang code" );
		out.println();
		out.println( "USAGE:" );
		out.println( "  boxlang format [OPTIONS]" );
		out.println( "  java -jar boxlang.jar ortus.boxlang.compiler.PrettyPrint [OPTIONS]" );
		out.println();
		out.println( "OPTIONS:" );
		out.println( "  -h, --help                  Show this help message and exit" );
		out.println( "  -i, --input <PATH>          Path to source directory or file to format (default: current directory)" );
		out.println( "  -o, --output <PATH>         Path to output directory or file (default: overwrite input files)" );
		out.println( "      --overwrite <true|false> Overwrite files (default: true). false prints formatted source to stdout" );
		out.println( "  -c, --config <PATH>         Path to configuration file (default: .bxformat.json or .cfformat.json)" );
		out.println( "      --check                 Only check if files need formatting (exit code 1 if changes needed)" );
		out.println( "      --initConfig            Create a default .bxformat.json configuration file" );
		out.println( "      --convertConfig         Convert .cfformat.json to .bxformat.json" );
		out.println();
		out.println( "CONFIGURATION:" );
		out.println( "  The formatter looks for configuration files in this order:" );
		out.println( "    1. .bxformat.json (preferred)" );
		out.println( "    2. .cfformat.json (CFFormat compatibility - auto-converted)" );
		out.println( "    3. Default settings" );
		out.println();
		out.println( "  You can also specify a config file explicitly with -c/--config." );
		out.println( "  Both .bxformat.json and .cfformat.json formats are supported." );
		out.println();
		out.println( "EXAMPLES:" );
		out.println( "  # Format all BoxLang files in the current directory" );
		out.println( "  boxlang format" );
		out.println();
		out.println( "  # Check files for formatting needs" );
		out.println( "  boxlang format --check" );
		out.println();
		out.println( "  # Print formatted output to stdout without overwriting files" );
		out.println( "  boxlang format --overwrite false --input /path/to/file.cfc" );
		out.println();
		out.println( "  # Use a specific config file" );
		out.println( "  boxlang format --config /path/to/.bxformat.json" );
		out.println();
		out.println( "  # Convert CFFormat config to BoxLang format" );
		out.println( "  boxlang format --convertConfig" );
		out.println();
		out.println( "More Information:" );
		out.println( "  Documentation: https://boxlang.ortusbooks.com/" );
		out.println( "  Community: https://community.ortussolutions.com/c/boxlang/42" );
		out.println( "  GitHub: https://github.com/ortus-boxlang" );
		out.println();
	}

}
