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
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * I am a CLI tool for transpiling ColdFusion code to BoxLang
 * TODO: Not sure where this class should eventually live.
 */
public class CFTranspiler {

	public static void main( String[] args ) {
		BoxRuntime runtime = BoxRuntime.getInstance();
		try {
			String	source		= ".";
			String	target		= null;
			Boolean	stopOnError	= false;

			for ( int i = 0; i < args.length; i++ ) {
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
				System.out.println( "Transpiling all .cfm files in " + sourcePath.toString() + " to " + targetPath.toString() );
				// Transpile all .cfm, .cfs, and .cfc files in sourcePath to targetPath
				final Path finalTargetPath = targetPath;
				try {
					final Path		finalSourcePath		= sourcePath;
					final boolean	finalStopOnError	= stopOnError;
					Files.walk( finalSourcePath ).filter( Files::isRegularFile ).forEach( path -> {
						String sourceExtension = path.getFileName().toString().substring( path.getFileName().toString().lastIndexOf( "." ) + 1 );
						if ( sourceExtension.equals( "cfm" ) || sourceExtension.equals( "cfc" ) || sourceExtension.equals( "cfs" ) ) {
							String	targetExtension		= mapExtension( sourceExtension );
							Path	resolvedTargetPath	= finalTargetPath
							    .resolve(
							        finalSourcePath.relativize( path ).toString().substring( 0, finalSourcePath.relativize( path ).toString().length() - 3 )
							            + targetExtension );
							transpileFile( path, resolvedTargetPath, finalStopOnError );
						}
					} );
				} catch ( IOException e ) {
					throw new BoxRuntimeException( "Error walking source path", e );
				}
			} else {
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
				transpileFile( sourcePath, targetPath, stopOnError );
			}

			System.exit( 0 );
		} finally {
			runtime.shutdown();
		}
	}

	private static void transpileFile( Path sourcePath, Path targetPath, Boolean stopOnError ) {
		try {
			Path directoryPath = targetPath.getParent();
			if ( directoryPath != null && !Files.exists( directoryPath ) ) {
				Files.createDirectories( directoryPath );
			}
		} catch ( IOException e ) {
			// folder already exists
		}
		System.out.println( "Writing " + targetPath.toString() );
		ParsingResult result = new Parser().parse( sourcePath.toFile() );
		if ( result.isCorrect() ) {
			// System.out.println( result.getRoot().toString() );
			try {
				Files.write( targetPath, result.getRoot().toString().getBytes( StandardCharsets.UTF_8 ) );
			} catch ( IOException e ) {
				throw new BoxRuntimeException( "Error writing transpiled file", e );
			}

		} else {
			System.out.println( "Parsing failed for " + sourcePath.toString() );
			result.getIssues().forEach( System.out::println );
			if ( stopOnError ) {
				System.exit( 1 );
			}
		}
	}

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