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
import java.util.List;
import java.util.Set;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.runnables.RunnableLoader;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.exceptions.ParseException;
import ortus.boxlang.runtime.util.ResolvedFilePath;

/**
 * I am a CLI tool for pre-compiling code to class files
 */
public class BXCompiler {

	private static final Set<String> SUPPORTED_EXTENSIONS = Set.of( "cfm", "cfc", "cfs", "bx", "bxs", "bxm" );

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
			final Boolean	finalIncludeStatic	= includeStatic;
			Path			sourcePath			= Paths.get( source ).normalize();
			if ( !sourcePath.isAbsolute() ) {
				sourcePath = Paths.get( "" ).resolve( sourcePath ).normalize().toAbsolutePath().normalize();
			}

			if ( !sourcePath.toFile().exists() ) {
				System.out.println( "Source Path does not exist: " + sourcePath.toString() );
				System.exit( 1 );
			}

			if ( target == null ) {
				throw new BoxRuntimeException( "--target is required " );
			}
			Path targetPath = Paths.get( target ).normalize();
			if ( !targetPath.isAbsolute() ) {
				targetPath = Paths.get( "" ).resolve( targetPath ).normalize().toAbsolutePath().normalize();
			}

			if ( sourcePath.toFile().isDirectory() ) {
				System.out.println( "Compiling all source files in " + sourcePath.toString() + " to " + targetPath.toString() );
				// compile all .cfm, .cfs, and .cfc files in sourcePath to targetPath
				final Path finalTargetPath = targetPath;
				try {
					final Path		finalSourcePath		= sourcePath;
					final boolean	finalStopOnError	= stopOnError;
					Files.walk( finalSourcePath )
					    .parallel()
					    .filter( Files::isRegularFile )
					    .forEach( path -> {
						    String sourceExtension	= path.getFileName().toString().substring( path.getFileName().toString().lastIndexOf( "." ) + 1 )
						        .toLowerCase();
						    Path resolvedTargetPath	= finalTargetPath.resolve( finalSourcePath.relativize( path ).toString() );

						    if ( SUPPORTED_EXTENSIONS.contains( sourceExtension ) ) {
							    compileFile( path, resolvedTargetPath, finalStopOnError, runtime );
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
			} else {
				String sourceFileName = sourcePath.getFileName().toString();
				if ( !SUPPORTED_EXTENSIONS.contains( sourceFileName.substring( sourceFileName.lastIndexOf( "." ) + 1 ).toLowerCase() ) ) {
					System.out.println( "Unsupported source file extension: " + sourcePath.getFileName().toString() );
					System.exit( 1 );
				}
				if ( targetPath.toFile().isDirectory() ) {
					// if target is a directory, use the source file name as the target file name
					targetPath = targetPath.resolve( sourceFileName );
				}
				compileFile( sourcePath, targetPath, stopOnError, runtime );
			}

			System.exit( 0 );
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
	 */
	public static void compileFile( Path sourcePath, Path targetPath, Boolean stopOnError, BoxRuntime runtime ) {
		ensureParentDirectoriesExist( targetPath );
		System.out.println( "Writing " + targetPath.toString() );
		List<byte[]> bytesList = null;
		try {
			bytesList = RunnableLoader.getInstance().getBoxpiler()
			    .compileTemplateBytes( ResolvedFilePath.of( "", "", sourcePath.toString(), sourcePath ) );
		} catch ( ParseException e ) {
			if ( stopOnError ) {
				throw e;
			} else {
				System.out.println( "Error compiling " + sourcePath.toString() + ": " + e.getMessage() );
				return;
			}
		}
		// Concatenate the byte arrays with a delimiter of four null bytes
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
		} catch ( IOException e ) {
			throw new RuntimeException( "Unable to write to target file", e );
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

}