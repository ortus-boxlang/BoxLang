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
import ortus.boxlang.compiler.prettyprint.config.Config;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

public final class PrettyPrint {

	private PrettyPrint() {
		// Prevent instantiation
	}

	public static void main( String[] args ) {
		BoxRuntime instance = BoxRuntime.getInstance();

		// process cli args
		// --check (make no changes, just check if the file is already formatted, return an error code if not)
		// -c, --config <config file> (defaults to .bxformat.json in the current working directory)
		// -i, --input <input file/folder> (defaults to current working directory)
		// -o, --output <output file/folder> (optional, if not provided, overwrite input files)
		try {
			boolean	checkMode	= false;
			String	configPath	= System.getProperty( "user.dir" ) + "/.bxformat.json";
			String	inputPath	= System.getProperty( "user.dir" );
			String	outputPath	= null;
			boolean	initConfig	= false;

			for ( int i = 0; i < args.length; i++ ) {
				if ( args[ i ].equalsIgnoreCase( "--help" ) || args[ i ].equalsIgnoreCase( "-h" ) ) {
					printHelp();
					System.exit( 0 );
				} else if ( args[ i ].equalsIgnoreCase( "--initConfig" ) ) {
					initConfig();
					System.exit( 0 );
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

			Config			config	= Config.loadConfig( configPath );
			// get a stream of files to process either from a single file or a directory
			Stream<Path>	filesToProcess;

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
		// write a default config file to the current working directory
		String defaultConfig = """
		                       {
		                       "maxLineWidth": 80,
		                       "indentSize": 4,
		                       "useTabs": false,
		                       "trailingComma": "none",
		                       "bracketSpacing": true}
		                       		""";

		try ( java.io.FileWriter writer = new java.io.FileWriter( configFile ) ) {
			writer.write( defaultConfig );
			System.out.println( "Default configuration file created at " + configFile.getAbsolutePath() );
		} catch ( java.io.IOException e ) {
			System.err.println( "Error creating configuration file: " + e.getMessage() );
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
	 * Prints the help message for the FeatureAudit tool.
	 */
	private static void printHelp() {
		System.out.println( "🔍 BoxLang Formatter - A CLI tool for formatting BoxLang code" );
		System.out.println();
		System.out.println( "📋 USAGE:" );
		System.out.println( "  boxlang format [OPTIONS]  # 🔧 Using OS binary" );
		System.out.println( "  java -jar boxlang.jar ortus.boxlang.compiler.PrettyPrint [OPTIONS] # 🐍 Using Java JAR" );
		System.out.println();
		System.out.println( "⚙️  OPTIONS:" );
		System.out.println( "  -h, --help                  ❓ Show this help message and exit" );
		System.out.println( "      --input <PATH>          📁 Path to source directory or file to format (default: current directory)" );
		System.out.println( "      --check                 🚫 Only show files that need formatting and exit with non-zero status if changes are needed" );
		System.out.println();
		System.out.println( "💡 EXAMPLES:" );
		System.out.println( "  # 🔍 Format all BoxLang files in the current directory and pulling configuration from .bxformat.json" );
		System.out.println( "  boxlang format" );
		System.out.println();
		System.out.println( "  # 🚫 Check files for any formatting needs, exit with non-zero status if changes are needed" );
		System.out.println( "  boxlang format --check" );
		System.out.println();
		System.out.println( "  # 🎯 Format single file" );
		System.out.println( "  boxlang format --input /path/to/file.cfm" );
		System.out.println();
		System.out.println( "📖 More Information:" );
		System.out.println( "  📖 Documentation: https://boxlang.ortusbooks.com/" );
		System.out.println( "  💬 Community: https://community.ortussolutions.com/c/boxlang/42" );
		System.out.println( "  💾 GitHub: https://github.com/ortus-boxlang" );
		System.out.println();
	}

}
