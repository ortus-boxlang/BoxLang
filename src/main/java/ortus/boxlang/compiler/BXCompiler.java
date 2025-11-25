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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.runnables.RunnableLoader;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.util.ResolvedFilePath;

/**
 * I am a CLI tool for pre-compiling code to class files
 */
public class BXCompiler {

	private static final Set<String> SUPPORTED_EXTENSIONS = Set.of( "cfm", "cfc", "cfs", "bx", "bxs", "bxm" );

	/**
	 * Record to hold the parsed command-line options.
	 *
	 * @param source        Path to source directory or file to compile
	 * @param target        Path to target directory or file
	 * @param stopOnError   Whether to stop processing on first error
	 * @param includeStatic Whether to include static files in compilation
	 */
	public record CompilerOptions( String source, String target, Boolean stopOnError, Boolean includeStatic ) {
	}

	/**
	 * Record to hold compilation statistics.
	 *
	 * @param successCount Number of files successfully compiled
	 * @param failureCount Number of files that failed to compile
	 * @param errors       List of error messages for failed files
	 */
	public record CompilationStats( int successCount, int failureCount, java.util.List<String> errors ) {
	}

	/**
	 * Main entry point for the BXCompiler CLI tool.
	 *
	 * @param args Command-line arguments
	 */
	public static void main( String[] args ) {
		// Check for help first before initializing BoxRuntime
		for ( int i = 0; i < args.length; i++ ) {
			if ( args[ i ].equalsIgnoreCase( "--help" ) || args[ i ].equalsIgnoreCase( "-h" ) ) {
				printHelp();
				System.exit( 0 );
			}
		}

		BoxRuntime runtime = BoxRuntime.getInstance();
		try {
			CompilerOptions	options				= parseArguments( args );
			final Boolean	finalIncludeStatic	= options.includeStatic();
			Path			sourcePath			= Paths.get( options.source() ).normalize();

			// Track compilation statistics
			AtomicInteger	successCount		= new AtomicInteger( 0 );
			AtomicInteger	failureCount		= new AtomicInteger( 0 );
			List<String>	errors				= new ArrayList<>();
			long			startTime			= System.currentTimeMillis();

			// Resolve relative paths against current working directory
			if ( !sourcePath.isAbsolute() ) {
				sourcePath = Paths.get( "" ).resolve( sourcePath ).normalize().toAbsolutePath().normalize();
			}

			// Verify source path exists
			if ( !sourcePath.toFile().exists() ) {
				System.out.println( "Source Path does not exist: " + sourcePath.toString() );
				System.exit( 1 );
			}

			// Verify target path exists
			Path targetPath = Paths.get( options.target() ).normalize();
			if ( !targetPath.isAbsolute() ) {
				targetPath = Paths.get( "" ).resolve( targetPath ).normalize().toAbsolutePath().normalize();
			}

			// Compile source
			if ( sourcePath.toFile().isDirectory() ) {
				// Print compilation header
				System.out.println( "═══════════════════════════════════════════════════════════════" );
				System.out.println( "🥊 BOXLANG COMPILER" );
				System.out.println( "═══════════════════════════════════════════════════════════════" );
				System.out.println();
				System.out.println( "📂 Source: " + sourcePath.toString() );
				System.out.println( "🎯 Target: " + targetPath.toString() );
				System.out.println( "🔍 Include Static: " + finalIncludeStatic );
				System.out.println( "🛑 Stop On Error: " + options.stopOnError() );
				System.out.println();
				System.out.println( "───────────────────────────────────────────────────────────────" );
				System.out.println( "⚡ Starting compilation..." );
				System.out.println();

				// compile all .cfm, .cfs, and .cfc files in sourcePath to targetPath
				final Path finalTargetPath = targetPath;
				try {
					final Path		finalSourcePath		= sourcePath;
					final boolean	finalStopOnError	= options.stopOnError();
					Files.walk( finalSourcePath )
					    .parallel()
					    .filter( Files::isRegularFile )
					    .forEach( path -> {
						    String sourceExtension	= path.getFileName().toString().substring( path.getFileName().toString().lastIndexOf( "." ) + 1 )
						        .toLowerCase();
						    Path resolvedTargetPath	= finalTargetPath.resolve( finalSourcePath.relativize( path ).toString() );

						    if ( SUPPORTED_EXTENSIONS.contains( sourceExtension ) ) {
							    boolean success = compileFile( path, resolvedTargetPath, finalStopOnError, runtime, errors );
							    if ( success ) {
								    successCount.incrementAndGet();
							    } else {
								    failureCount.incrementAndGet();
							    }
						    } else if ( finalIncludeStatic && !Files.exists( resolvedTargetPath ) ) {
							    // If the file is not a supported source file, but we are including static files,
							    // we will copy it to the target directory
							    try {
								    ensureParentDirectoriesExist( resolvedTargetPath );
								    System.out.println( "Writing " + path.toString() + " to " + resolvedTargetPath.toString() );
								    Files.copy( path, resolvedTargetPath, StandardCopyOption.REPLACE_EXISTING );
							    } catch ( IOException e ) {
								    if ( finalStopOnError ) {
									    throw new BoxRuntimeException( "Error copying static file: " + path.toString(), e );
								    } else {
									    System.out.println( "Error copying static file: " + path.toString() + ": " + e.getMessage() );
								    }
							    }
						    }
					    } );
				} catch ( IOException e ) {
					throw new BoxRuntimeException( "Error walking source path", e );
				}
			}
			// Compile a FILE source
			else {
				String sourceFileName = sourcePath.getFileName().toString();
				if ( !SUPPORTED_EXTENSIONS.contains( sourceFileName.substring( sourceFileName.lastIndexOf( "." ) + 1 ).toLowerCase() ) ) {
					System.out.println( "Unsupported source file extension: " + sourcePath.getFileName().toString() );
					System.exit( 1 );
				}
				if ( targetPath.toFile().isDirectory() ) {
					// if target is a directory, use the source file name as the target file name
					targetPath = targetPath.resolve( sourceFileName );
				}
				boolean success = compileFile( sourcePath, targetPath, options.stopOnError(), runtime, errors );
				if ( success ) {
					successCount.incrementAndGet();
				} else {
					failureCount.incrementAndGet();
				}
			}

			// Calculate elapsed time
			long	elapsedTime	= System.currentTimeMillis() - startTime;
			double	seconds		= elapsedTime / 1000.0;

			// Print compilation summary
			printCompilationSummary( successCount.get(), failureCount.get(), errors, seconds );

			System.exit( failureCount.get() > 0 ? 1 : 0 );
		} finally {
			runtime.shutdown();
		}
	}

	/**
	 * Compiles a single file to the target path.
	 *
	 * @param sourcePath  The path to the source file to compile.
	 * @param targetPath  The path where the compiled file should be written.
	 * @param stopOnError If true, throws an exception on compilation errors; otherwise logs the error and continues.
	 * @param runtime     The BoxRuntime instance used for compilation.
	 *
	 * @return true if compilation was successful, false otherwise
	 */
	public static boolean compileFile( Path sourcePath, Path targetPath, Boolean stopOnError, BoxRuntime runtime ) {
		return compileFile( sourcePath, targetPath, stopOnError, runtime, new ArrayList<>() );
	}

	/**
	 * Compiles a single file to the target path.
	 *
	 * @param sourcePath  The path to the source file to compile.
	 * @param targetPath  The path where the compiled file should be written.
	 * @param stopOnError If true, throws an exception on compilation errors; otherwise logs the error and continues.
	 * @param runtime     The BoxRuntime instance used for compilation.
	 * @param errors      List to collect file paths for failed compilations.
	 *
	 * @return true if compilation was successful, false otherwise
	 */
	public static boolean compileFile( Path sourcePath, Path targetPath, Boolean stopOnError, BoxRuntime runtime, List<String> errors ) {
		ensureParentDirectoriesExist( targetPath );
		System.out.println( "⚡ Writing -> " + targetPath.toString() );
		List<byte[]> bytesList = null;
		try {
			bytesList = RunnableLoader.getInstance()
			    .getBoxpiler()
			    .compileTemplateBytes( ResolvedFilePath.of( "", "", sourcePath.toString(), sourcePath ) );
		} catch ( Exception e ) {
			synchronized ( errors ) {
				errors.add( sourcePath.toString() );
			}
			if ( stopOnError ) {
				throw e;
			} else {
				String errorMsg = sourcePath.toString() + ": " + e.getMessage();
				System.out.println( "═══════════════════════════════════════════════════════════════" );
				System.out.println( "❌ Error compiling " + errorMsg );
				System.out.println( "═══════════════════════════════════════════════════════════════" );
				return false;
			}
		}		// Concatenate the byte arrays with a delimiter of four null bytes
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		try {
			baos.write( ByteBuffer.allocate( 4 ).putInt( 0xCAFEBABE ).array() );
			for ( byte[] bytes : bytesList ) {
				// Write the length of the class file
				baos.write( ByteBuffer.allocate( 4 ).putInt( bytes.length ).array() );
				// Write the class bytes
				baos.write( bytes );
			}

			// Write the concatenated byte array to the target file
			Files.write( targetPath, baos.toByteArray() );
			return true;
		} catch ( IOException e ) {
			String errorMsg = targetPath.toString() + ": Unable to write to target file - " + e.getMessage();
			synchronized ( errors ) {
				errors.add( errorMsg );
			}
			System.out.println( "❌ Error: " + errorMsg );
			if ( stopOnError ) {
				throw new RuntimeException( "Unable to write to target file", e );
			}
			return false;
		}
	}

	/**
	 * Ensures that the parent directories of the target path exist, creating them if necessary.
	 *
	 * @param targetPath The path for which to ensure parent directories exist.
	 */
	private static void ensureParentDirectoriesExist( Path targetPath ) {
		try {
			Path directoryPath = targetPath.getParent();
			if ( directoryPath != null && !Files.exists( directoryPath ) ) {
				Files.createDirectories( directoryPath );
			}
		} catch ( IOException e ) {
			// folder already exists
		}
	}

	/**
	 * Parses command-line arguments and returns a CompilerOptions record.
	 *
	 * @param args The command-line arguments to parse
	 *
	 * @return A CompilerOptions record containing the parsed options
	 *
	 * @throws BoxRuntimeException if required arguments are missing or invalid
	 */
	private static CompilerOptions parseArguments( String[] args ) {
		String	source			= ".";
		String	target			= null;
		Boolean	stopOnError		= false;
		Boolean	includeStatic	= false;

		for ( int i = 0; i < args.length; i++ ) {
			if ( args[ i ].equalsIgnoreCase( "--includeStatic" ) ) {
				includeStatic = true;
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

		if ( target == null ) {
			throw new BoxRuntimeException( "--target is required" );
		}

		return new CompilerOptions( source, target, stopOnError, includeStatic );
	}

	/**
	 * Prints a compilation summary with statistics and failed file paths.
	 *
	 * @param successCount Number of files successfully compiled
	 * @param failureCount Number of files that failed to compile
	 * @param errors       List of file paths that failed to compile
	 * @param seconds      Total compilation time in seconds
	 */
	private static void printCompilationSummary( int successCount, int failureCount, List<String> errors, double seconds ) {
		int totalFiles = successCount + failureCount;

		System.out.println();
		System.out.println( "═══════════════════════════════════════════════════════════════" );
		System.out.println( "📊 COMPILATION SUMMARY" );
		System.out.println( "═══════════════════════════════════════════════════════════════" );
		System.out.println();
		System.out.printf( "⏱️  Time Elapsed: %.2f seconds%n", seconds );
		System.out.printf( "📁 Total Files:   %d%n", totalFiles );
		System.out.printf( "✅ Successful:    %d%n", successCount );
		System.out.printf( "❌ Failed:        %d%n", failureCount );
		System.out.println();

		if ( failureCount > 0 ) {
			System.out.println( "═══════════════════════════════════════════════════════════════" );
			System.out.println( "❌ FAILED FILES" );
			System.out.println( "═══════════════════════════════════════════════════════════════" );
			System.out.println();

			int errorNumber = 1;
			for ( String filePath : errors ) {
				System.out.printf( "%d. %s%n", errorNumber++, filePath );
			}
			System.out.println();
			System.out.println( "═══════════════════════════════════════════════════════════════" );
		} else {
			System.out.println( "🎉 All files compiled successfully!" );
			System.out.println( "═══════════════════════════════════════════════════════════════" );
		}
	}

	/**
	 * Prints the help message for the BXCompiler tool.
	 */
	private static void printHelp() {
		System.out.println( "⚡ BoxLang Compiler - A CLI tool for pre-compiling BoxLang code to class files" );
		System.out.println();
		System.out.println( "📋 USAGE:" );
		System.out.println( "  boxlang compile [OPTIONS]  # 🔧 Using OS binary" );
		System.out.println( "  java -jar boxlang.jar ortus.boxlang.compiler.BXCompiler [OPTIONS] # 🐍 Using Java JAR" );
		System.out.println();
		System.out.println( "⚙️  OPTIONS:" );
		System.out.println( "  -h, --help                  ❓ Show this help message and exit" );
		System.out.println( "      --source <PATH>         📂 Path to source directory or file to compile (default: current directory)" );
		System.out.println( "      --target <PATH>         🎯 Path to target directory or file (required)" );
		System.out.println( "      --includeStatic         🔍 Include static files in compilation (default: false)" );
		System.out.println( "      --stopOnError [BOOL]    🛑 Stop processing on first error (default: false)" );
		System.out.println();
		System.out.println( "📦 COMPILATION PROCESS:" );
		System.out.println( "  • Compiles BoxLang/ColdFusion source files to Java bytecode class files" );
		System.out.println( "  • Creates pre-compiled templates for faster runtime execution" );
		System.out.println( "  • Preserves directory structure in target location" );
		System.out.println();
		System.out.println( "🔧 SUPPORTED SOURCE FILES:" );
		System.out.println( "  .cfm  - ColdFusion markup pages" );
		System.out.println( "  .cfc  - ColdFusion components" );
		System.out.println( "  .cfs  - ColdFusion script files" );
		System.out.println( "  .bx   - BoxLang class files" );
		System.out.println( "  .bxs  - BoxLang script files" );
		System.out.println( "  .bxm  - BoxLang module files" );
		System.out.println();
		System.out.println( "💡 EXAMPLES:" );
		System.out.println( "  # ⚡ Compile current directory to target" );
		System.out.println( "  boxlang compile --target ./compiled" );
		System.out.println();
		System.out.println( "  # 🛑 Stop on first compilation error" );
		System.out.println( "  boxlang compile --source ./src --target ./build --stopOnError" );
		System.out.println();
		System.out.println( "📂 PATH REQUIREMENTS:" );
		System.out.println( "  • Target directories are created automatically if they don't exist" );
		System.out.println( "  • Relative paths are resolved against the current working directory" );
		System.out.println();
		System.out.println( "🔄 OUTPUT FORMAT:" );
		System.out.println( "  • Compiled files maintain original directory structure" );
		System.out.println( "  • Binary format with CAFEBABE header for BoxLang runtime" );
		System.out.println( "  • Multiple class files concatenated with length prefixes" );
		System.out.println();
		System.out.println( "📖 More Information:" );
		System.out.println( "  📖 Documentation: https://boxlang.ortusbooks.com/" );
		System.out.println( "  💬 Community: https://community.ortussolutions.com/c/boxlang/42" );
		System.out.println( "  💾 GitHub: https://github.com/ortus-boxlang" );
		System.out.println();
	}

}