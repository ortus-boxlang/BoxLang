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
package ortus.boxlang.compiler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import ortus.boxlang.compiler.parser.Parser;
import ortus.boxlang.compiler.parser.ParsingResult;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.logging.BoxLangLogger;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * I am a CLI tool for transpiling ColdFusion code to BoxLang
 */
public class CFTranspiler {

	private static final BoxLangLogger logger = BoxRuntime.getInstance().getLoggingService().getLogger( CFTranspiler.class.getSimpleName() );

	/**
	 * Prints the help message for the CFTranspiler tool.
	 */
	private static void printHelp() {
		System.out.println( "🔄 BoxLang CFTranspiler - A CLI tool for transpiling ColdFusion code to BoxLang" );
		System.out.println();
		System.out.println( "📋 USAGE:" );
		System.out.println( "  boxlang cftranspile [OPTIONS]  # 🔧 Using OS binary" );
		System.out.println( "  java -jar boxlang.jar ortus.boxlang.compiler.CFTranspiler [OPTIONS] # 🐍 Using Java JAR" );
		System.out.println();
		System.out.println( "⚙️  OPTIONS:" );
		System.out.println( "  -h, --help                  ❓ Show this help message and exit" );
		System.out.println( "  -v, --verbose               🔍 Enable verbose output with detailed progress information" );
		System.out.println( "      --source <PATH>         📁 Path to source directory or file to transpile (default: current directory)" );
		System.out.println( "      --target <PATH>         🎯 Path to target directory or file (required)" );
		System.out.println( "      --stopOnError [BOOL]    🛑 Stop processing on first error (default: false)" );
		System.out.println();
		System.out.println( "🔄 FILE EXTENSION MAPPING:" );
		System.out.println( "  .cfm  →  .bxm  (ColdFusion markup to BoxLang markup)" );
		System.out.println( "  .cfc  →  .bx   (ColdFusion component to BoxLang class)" );
		System.out.println( "  .cfs  →  .bxs  (ColdFusion script to BoxLang script)" );
		System.out.println();
		System.out.println( "💡 EXAMPLES:" );
		System.out.println( "  # 🔄 Transpile current directory to target directory" );
		System.out.println( "  boxlang cftranspile --target ./boxlang-output" );
		System.out.println();
		System.out.println( "  # 📁 Transpile specific source directory" );
		System.out.println( "  boxlang cftranspile --source ./coldfusion-code --target ./boxlang-code" );
		System.out.println();
		System.out.println( "  # 📄 Transpile single file" );
		System.out.println( "  boxlang cftranspile --source app.cfm --target app.bxm" );
		System.out.println();
		System.out.println( "  # 🛑 Stop on first error" );
		System.out.println( "  boxlang cftranspile --source ./cf-app --target ./bx-app --stopOnError" );
		System.out.println();
		System.out.println( "  # 💻 Verbose output with detailed progress" );
		System.out.println( "  boxlang cftranspile --source ./cf-app --target ./bx-app --verbose" );
		System.out.println();
		System.out.println( "  # Transpile with directory structure preservation" );
		System.out.println( "  boxlang cftranspile --source /path/to/cf/project --target /path/to/bx/project" );
		System.out.println();
		System.out.println( "📂 BEHAVIOR:" );
		System.out.println( "  • Directory transpilation preserves folder structure" );
		System.out.println( "  • Single file transpilation allows custom target naming" );
		System.out.println( "  • Missing target directories are created automatically" );
		System.out.println( "  • Parsing errors are logged, processing continues unless --stopOnError is used" );
		System.out.println();
		System.out.println( "🔧 SUPPORTED SOURCE FILES:" );
		System.out.println( "  .cfm  - ColdFusion markup pages" );
		System.out.println( "  .cfc  - ColdFusion components" );
		System.out.println( "  .cfs  - ColdFusion script files" );
		System.out.println();
		System.out.println( "📖 More Information:" );
		System.out.println( "  📖 Documentation: https://boxlang.ortusbooks.com/" );
		System.out.println( "  💬 Community: https://community.ortussolutions.com/c/boxlang/42" );
		System.out.println( "  💾 GitHub: https://github.com/ortus-boxlang" );
		System.out.println();
	}

	public static void main( String[] args ) {
		BoxRuntime runtime = BoxRuntime.getInstance();
		try {
			String	source		= ".";
			String	target		= null;
			Boolean	stopOnError	= false;
			Boolean	verbose		= false;
			for ( int i = 0; i < args.length; i++ ) {
				if ( args[ i ].equalsIgnoreCase( "--help" ) || args[ i ].equalsIgnoreCase( "-h" ) ) {
					printHelp();
					System.exit( 0 );
				}
				if ( args[ i ].equalsIgnoreCase( "--verbose" ) || args[ i ].equalsIgnoreCase( "-v" ) ) {
					verbose = true;
				}
				if ( args[ i ].equalsIgnoreCase( "--source" ) ) {
					if ( i + 1 >= args.length ) {
						throw new BoxRuntimeException( "--source requires a path" );
					}
					source = args[ i + 1 ];
				}
				if ( args[ i ].equalsIgnoreCase( "--target" ) ) {
					if ( i + 1 >= args.length ) {
						throw new BoxRuntimeException( "--target requires a path" );
					}
					target = args[ i + 1 ];
				}
				if ( args[ i ].equalsIgnoreCase( "--stopOnError" ) ) {
					if ( i + 1 >= args.length || args[ i + 1 ].startsWith( "--" ) ) {
						stopOnError = true;
					} else {
						stopOnError = Boolean.parseBoolean( args[ i + 1 ] );
					}

				}
			}
			Path sourcePath = Paths.get( source ).normalize();
			if ( !sourcePath.isAbsolute() ) {
				sourcePath = Paths.get( "" ).resolve( sourcePath ).normalize().toAbsolutePath().normalize();
			}

			if ( !sourcePath.toFile().exists() ) {
				logger.error( "Source Path does not exist: " + sourcePath.toString() );
				System.exit( 1 );
			}
			if ( target == null ) {
				throw new BoxRuntimeException( "--target is required " );
			}

			if ( verbose ) {
				System.out.println( "🚀 BoxLang CFTranspiler Starting..." );
				System.out.println( "📁 Source: " + source );
				System.out.println( "🎯 Target: " + target );
				System.out.println( "🛑 Stop on Error: " + stopOnError );
				System.out.println();
			}
			Path targetPath = Paths.get( target ).normalize();
			if ( !targetPath.isAbsolute() ) {
				targetPath = Paths.get( "" ).resolve( targetPath ).normalize().toAbsolutePath().normalize();
			}

			if ( sourcePath.toFile().isDirectory() ) {
				if ( verbose ) {
					System.out.println( "📂 Directory Mode: Transpiling all ColdFusion files" );
					System.out.println( "📍 Scanning: " + sourcePath.toString() );
					System.out.println( "🎯 Output to: " + targetPath.toString() );
					System.out.println( "🔍 Looking for: .cfm, .cfc, .cfs files" );
					System.out.println();
				}
				logger.debug( "Transpiling all .cfm/.cfc/.cfs files in " + sourcePath.toString() + " to " + targetPath.toString() );
				// Transpile all .cfm, .cfs, and .cfc files in sourcePath to targetPath
				final Path		finalTargetPath	= targetPath;
				final Boolean	finalVerbose	= verbose;
				try {
					final Path		finalSourcePath		= sourcePath;
					final boolean	finalStopOnError	= stopOnError;
					Files.walk( finalSourcePath )
					    .parallel()
					    .filter( Files::isRegularFile )
					    .forEach( path -> {
						    String sourceExtension = path.getFileName().toString().substring( path.getFileName().toString().lastIndexOf( "." ) + 1 );
						    if ( sourceExtension.equals( "cfm" ) || sourceExtension.equals( "cfc" ) || sourceExtension.equals( "cfs" ) ) {
							    String targetExtension	= mapExtension( sourceExtension );
							    Path resolvedTargetPath	= finalTargetPath
							        .resolve(
							            finalSourcePath.relativize( path ).toString().substring( 0, finalSourcePath.relativize( path ).toString().length() - 3 )
							                + targetExtension );
							    transpileFile( path, resolvedTargetPath, finalStopOnError, finalVerbose );
						    }
					    } );
				} catch ( IOException e ) {
					throw new BoxRuntimeException( "Error walking source path", e );
				}
			} else {
				if ( verbose ) {
					System.out.println( "📄 Single File Mode: Transpiling individual file" );
					System.out.println( "📍 Source: " + sourcePath.toString() );
				}
				String	sourceExtension	= sourcePath.getFileName().toString().substring( sourcePath.getFileName().toString().lastIndexOf( "." ) + 1 );
				String	targetExtension	= mapExtension( sourceExtension );
				String	trgName			= targetPath.getFileName().toString();
				if ( targetPath.toFile().isDirectory() && !trgName.endsWith( ".bx" )
				    && !trgName.endsWith( ".bxs" ) && !trgName.endsWith( ".bxm" ) ) {
					targetPath = targetPath.resolve( sourcePath.getFileName().toString().replace( sourceExtension, targetExtension ) );
				} else {
					if ( !trgName.endsWith( targetExtension ) ) {
						// append correct extension
						targetPath = targetPath.resolveSibling( trgName + "." + targetExtension );
					}
				}
				if ( verbose ) {
					System.out.println( "🎯 Target: " + targetPath.toString() );
					System.out.println( "🔄 Extension: ." + sourceExtension + " → ." + targetExtension );
					System.out.println();
				}
				transpileFile( sourcePath, targetPath, stopOnError, verbose );
			}

			if ( verbose ) {
				System.out.println( "✅ CFTranspiler completed successfully!" );
			}
			System.exit( 0 );
		} finally {
			runtime.shutdown();
		}
	}

	/**
	 * Transpiles a single ColdFusion file to BoxLang and writes the result to the target path.
	 *
	 * @param sourcePath  The path to the source ColdFusion file.
	 * @param targetPath  The path where the transpiled BoxLang file should be written.
	 * @param stopOnError If true, the program will exit with an error code if parsing fails.
	 * @param verbose     If true, enables detailed output during transpilation.
	 *
	 * @throws BoxRuntimeException If there is an error writing the transpiled file or if parsing fails and stopOnError is true.
	 */
	private static void transpileFile( Path sourcePath, Path targetPath, Boolean stopOnError, Boolean verbose ) {
		if ( verbose ) {
			System.out.println( "🔄 Transpiling: " + sourcePath.getFileName().toString() );
		}
		try {
			Path directoryPath = targetPath.getParent();
			if ( directoryPath != null && !Files.exists( directoryPath ) ) {
				if ( verbose ) {
					System.out.println( "📁 Creating directory: " + directoryPath.toString() );
				}
				Files.createDirectories( directoryPath );
			}
		} catch ( IOException e ) {
			// folder already exists
		}
		if ( verbose ) {
			System.out.println( "⚙️  Parsing: " + sourcePath.getFileName().toString() );
		}
		logger.debug( "Writing " + targetPath.toString() );
		ParsingResult result = new Parser().parse( sourcePath.toFile() );
		if ( result.isCorrect() ) {
			if ( verbose ) {
				System.out.println( "📝 Writing: " + targetPath.getFileName().toString() );
			}
			// logger.debug( result.getRoot().toString() );
			try {
				Files.write( targetPath, result.getRoot().toString().getBytes( StandardCharsets.UTF_8 ) );
				if ( verbose ) {
					System.out.println( "✅ Success: " + sourcePath.getFileName().toString() + " → " + targetPath.getFileName().toString() );
				}
			} catch ( IOException e ) {
				throw new BoxRuntimeException( "Error writing transpiled file", e );
			}

		} else {
			if ( verbose ) {
				System.out.println( "❌ Failed: " + sourcePath.getFileName().toString() );
				System.out.println( "   Parsing errors found:" );
				result.getIssues().forEach( issue -> System.out.println( "   • " + issue.toString() ) );
			}
			logger.error( "Parsing failed for " + sourcePath.toString() );
			result.getIssues().forEach( issue -> logger.error( issue.toString() ) );
			if ( stopOnError ) {
				System.exit( 1 );
			}
		}
	}

	/**
	 * Maps ColdFusion file extensions to BoxLang file extensions.
	 *
	 * @param extension The ColdFusion file extension (e.g., "cfm", "cfc", "cfs").
	 *
	 * @return The corresponding BoxLang file extension (e.g., "bxm", "bx", "bxs").
	 *
	 * @throws BoxRuntimeException If the provided extension is not supported.
	 */
	private static String mapExtension( String extension ) {
		switch ( extension ) {
			case "cfm" :
				return "bxm";
			case "cfc" :
				return "bx";
			case "cfs" :
				return "bxs";
			default :
				throw new BoxRuntimeException( "Unsupported extension: " + extension );
		}
	}

}