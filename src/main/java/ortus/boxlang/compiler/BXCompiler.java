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
import java.util.List;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.logging.BoxLangLogger;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.exceptions.ParseException;
import ortus.boxlang.runtime.util.ResolvedFilePath;

/**
 * I am a CLI tool for pre-compiling code to class files
 */
public class BXCompiler {

	private static final BoxLangLogger logger = BoxRuntime.getInstance().getLoggingService().getLogger( BXCompiler.class.getSimpleName() );

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
		System.out.println( "      --basePath <PATH>       📁 Base path for resolving relative paths (default: current directory)" );
		System.out.println( "      --source <PATH>         📂 Path to source directory or file to compile (default: current directory)" );
		System.out.println( "      --target <PATH>         🎯 Path to target directory or file (required)" );
		System.out.println( "      --mapping <NAME>        🗺️  Mapping name for the compiled file (e.g., modules.myModule)" );
		System.out.println( "      --stopOnError [BOOL]    🛑 Stop processing on first error (default: false)" );
		System.out.println();
		System.out.println( "📦 COMPILATION PROCESS:" );
		System.out.println( "  • Compiles BoxLang/ColdFusion source files to Java bytecode class files" );
		System.out.println( "  • Creates pre-compiled templates for faster runtime execution" );
		System.out.println( "  • Preserves directory structure in target location" );
		System.out.println( "  • Validates source path is within or equal to base path" );
		System.out.println();
		System.out.println( "🔧 SUPPORTED SOURCE FILES:" );
		System.out.println( "  .cfm  - ColdFusion markup pages" );
		System.out.println( "  .cfc  - ColdFusion components" );
		System.out.println( "  .cfs  - ColdFusion script files" );
		System.out.println();
		System.out.println( "💡 EXAMPLES:" );
		System.out.println( "  # ⚡ Compile current directory to target" );
		System.out.println( "  boxlang compile --target ./compiled" );
		System.out.println();
		System.out.println( "  # 📂 Compile specific source directory with mapping" );
		System.out.println( "  boxlang compile --source ./src --target ./build --mapping myapp" );
		System.out.println();
		System.out.println( "  # 📄 Compile single file with custom base path" );
		System.out.println( "  boxlang compile --basePath /app --source /app/modules/user.cfm --target ./compiled" );
		System.out.println();
		System.out.println( "  # 🛑 Stop on first compilation error" );
		System.out.println( "  boxlang compile --source ./src --target ./build --stopOnError" );
		System.out.println();
		System.out.println( "  # 🗺️  Compile with nested module mapping" );
		System.out.println( "  boxlang compile --source ./modules/auth --target ./compiled --mapping modules.auth" );
		System.out.println();
		System.out.println( "📂 PATH REQUIREMENTS:" );
		System.out.println( "  • Source path must be equal to or a subdirectory of the base path" );
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
			String	base		= ".";
			String	source		= ".";
			String	target		= null;
			String	mapping		= "";
			Boolean	stopOnError	= false;
			for ( int i = 0; i < args.length; i++ ) {
				if ( args[ i ].equalsIgnoreCase( "--mapping" ) ) {
					if ( i + 1 >= args.length ) {
						throw new BoxRuntimeException( "--mapping requires a name like [modules.myModule]" );
					}
					mapping = args[ i + 1 ];
				}
				if ( args[ i ].equalsIgnoreCase( "--basePath" ) ) {
					if ( i + 1 >= args.length ) {
						throw new BoxRuntimeException( "--basePath requires a path" );
					}
					base = args[ i + 1 ];
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
			mapping = mapping.replace( "/", "." ).replace( "\\", "." );
			// trim double ..
			while ( mapping.contains( ".." ) ) {
				mapping = mapping.replace( "..", "." );
			}
			// trim leading or trailing .
			if ( mapping.startsWith( "." ) ) {
				mapping = mapping.substring( 1 );
			}
			if ( mapping.endsWith( "." ) ) {
				mapping = mapping.substring( 0, mapping.length() - 1 );
			}
			final String	finalMapping	= mapping;
			Path			basePath		= Paths.get( base ).normalize();
			if ( !basePath.isAbsolute() ) {
				basePath = Paths.get( "" ).resolve( basePath ).normalize().toAbsolutePath().normalize();
			}
			final Path	finalBasePath	= basePath;
			Path		sourcePath		= Paths.get( source ).normalize();
			if ( !sourcePath.isAbsolute() ) {
				sourcePath = Paths.get( "" ).resolve( sourcePath ).normalize().toAbsolutePath().normalize();
			}

			if ( !sourcePath.toFile().exists() ) {
				logger.warn( "Source Path does not exist: " + sourcePath.toString() );
				System.exit( 1 );
			}
			// source path must be equal to or a subdirectory of the base path
			if ( !sourcePath.startsWith( basePath ) ) {
				throw new BoxRuntimeException( "Source path must be equal to or a subdirectory of the base path" );
			}

			if ( target == null ) {
				throw new BoxRuntimeException( "--target is required " );
			}
			Path targetPath = Paths.get( target ).normalize();
			if ( !targetPath.isAbsolute() ) {
				targetPath = Paths.get( "" ).resolve( targetPath ).normalize().toAbsolutePath().normalize();
			}

			if ( sourcePath.toFile().isDirectory() ) {
				logger.debug( "Transpiling all .cfm files in " + sourcePath.toString() + " to " + targetPath.toString() );
				// compile all .cfm, .cfs, and .cfc files in sourcePath to targetPath
				final Path finalTargetPath = targetPath;
				try {
					final Path		finalSourcePath		= sourcePath;
					final boolean	finalStopOnError	= stopOnError;
					Files.walk( finalSourcePath )
					    .parallel()
					    .filter( Files::isRegularFile )
					    .forEach( path -> {
						    String sourceExtension = path.getFileName().toString().substring( path.getFileName().toString().lastIndexOf( "." ) + 1 );
						    if ( sourceExtension.equals( "cfm" ) || sourceExtension.equals( "cfc" ) || sourceExtension.equals( "cfs" ) ) {
							    String targetExtension	= sourceExtension;
							    Path resolvedTargetPath	= finalTargetPath
							        .resolve(
							            finalSourcePath.relativize( path ).toString().substring( 0, finalSourcePath.relativize( path ).toString().length() - 3 )
							                + targetExtension );
							    compileFile( path, resolvedTargetPath, finalStopOnError, runtime, finalBasePath, finalMapping );
						    }
					    } );
				} catch ( IOException e ) {
					throw new BoxRuntimeException( "Error walking source path", e );
				}
			} else {
				String	sourceExtension	= sourcePath.getFileName().toString().substring( sourcePath.getFileName().toString().lastIndexOf( "." ) + 1 );
				String	targetExtension	= sourceExtension;
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
				compileFile( sourcePath, targetPath, stopOnError, runtime, finalBasePath, finalMapping );
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
	 * @param basePath    The base path used for resolving relative paths.
	 * @param mapping     The mapping name for the compiled file.
	 */
	public static void compileFile( Path sourcePath, Path targetPath, Boolean stopOnError, BoxRuntime runtime, Path basePath, String mapping ) {
		try {
			Path directoryPath = targetPath.getParent();
			if ( directoryPath != null && !Files.exists( directoryPath ) ) {
				Files.createDirectories( directoryPath );
			}
		} catch ( IOException e ) {
			// folder already exists
		}
		logger.debug( "Writing " + targetPath.toString() );
		List<byte[]> bytesList = null;
		try {
			// calculate relative path by replacing the base path with an empty string
			Path relativePath = basePath.relativize( sourcePath );
			// remove file name
			bytesList = runtime.getCompiler()
			    .compileTemplateBytes( ResolvedFilePath.of( mapping, basePath.toString(), relativePath.toString(), sourcePath ) );
		} catch ( ParseException e ) {
			if ( stopOnError ) {
				throw e;
			} else {
				logger.error( "Error compiling " + sourcePath.toString() + ": " + e.getMessage() );
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

}