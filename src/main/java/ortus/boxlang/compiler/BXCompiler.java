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

import ortus.boxlang.compiler.javaboxpiler.JavaBoxpiler;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.exceptions.ParseException;
import ortus.boxlang.runtime.util.ResolvedFilePath;

/**
 * I am a CLI tool for pre-compiling code to class files
 * TODO: Not sure where this class should eventually live.
 */
public class BXCompiler {

	public static void main( String[] args ) {
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
				System.out.println( "Source Path does not exist: " + sourcePath.toString() );
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
				System.out.println( "Transpiling all .cfm files in " + sourcePath.toString() + " to " + targetPath.toString() );
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

	private static void compileFile( Path sourcePath, Path targetPath, Boolean stopOnError, BoxRuntime runtime, Path basePath, String mapping ) {
		try {
			Path directoryPath = targetPath.getParent();
			if ( directoryPath != null && !Files.exists( directoryPath ) ) {
				Files.createDirectories( directoryPath );
			}
		} catch ( IOException e ) {
			// folder already exists
		}
		System.out.println( "Writing " + targetPath.toString() );
		List<byte[]> bytesList = null;
		try {
			// calculate relative path by replacing the base path with an empty string
			Path relativePath = basePath.relativize( sourcePath );
			// remove file name
			bytesList = JavaBoxpiler.getInstance()
			    .compileTemplateBytes( ResolvedFilePath.of( mapping, basePath.toString(), relativePath.toString(), sourcePath ) );
		} catch ( ParseException e ) {
			if ( stopOnError ) {
				throw e;
			} else {
				System.err.println( "Error compiling " + sourcePath.toString() + ": " + e.getMessage() );
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