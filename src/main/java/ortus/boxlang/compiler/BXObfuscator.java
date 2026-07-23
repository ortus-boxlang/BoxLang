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

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.visitor.ObfuscationVisitor;
import ortus.boxlang.compiler.parser.Parser;
import ortus.boxlang.compiler.parser.ParsingResult;
import ortus.boxlang.compiler.prettyprint.PrettyPrint;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.util.CodeEncryption;

/**
 * I am a CLI tool for obfuscating BoxLang / ColdFusion source code so that it can be
 * deployed in a non-revealing form. I parse each source file into an AST, strip comments,
 * rename local variables (and, optionally, private functions and arguments), then re-emit
 * compact source using the pretty-printer.
 *
 * The output is still valid BoxLang source that runs identically to the original — only its
 * readability is reduced. If bytecode-only distribution is desired, the obfuscated output can
 * be piped through {@code boxlang compile}.
 *
 * With {@code --encrypt}, the obfuscated source is additionally encrypted at rest (AES-256-GCM) and
 * stamped with a key-id label. The runtime transparently decrypts it in memory just before parsing
 * (see {@code ortus.boxlang.runtime.util.CodeEncryption}), so the distributed files are unreadable
 * yet still run on any BoxLang version (no bytecode, no version binding). The decryption key is
 * resolved on the host by key-id from the {@code BOXLANG_CODE_KEY_<KEYID>} env var or the
 * {@code security.codeKeys} map in {@code boxlang.json}.
 */
public final class BXObfuscator {

	/**
	 * Source file extensions this tool will obfuscate.
	 */
	private static final Set<String> SUPPORTED_EXTENSIONS = Set.of( "bx", "bxs", "bxm", "cfm", "cfc", "cfs" );

	/**
	 * Parsed command-line options for the obfuscator.
	 *
	 * @param sourcePaths     one or more source files/directories to obfuscate
	 * @param targetPath      target directory to write obfuscated files into
	 * @param excludePaths    files/directories to skip
	 * @param renameVars      rename {@code var}-declared local variables
	 * @param renameFunctions rename private / script-level function names and their call sites
	 * @param renameArgs      rename function argument names
	 * @param stopOnError     stop processing on the first error
	 * @param encrypt         encrypt the obfuscated output at rest (decrypted in memory at parse time)
	 * @param key             the secret used to derive the encryption key (required when encrypt is true)
	 * @param keyId           a label identifying which key decrypts the output (baked into each file header)
	 */
	record ObfuscatorOptions(
	    List<String> sourcePaths,
	    String targetPath,
	    List<String> excludePaths,
	    boolean renameVars,
	    boolean renameFunctions,
	    boolean renameArgs,
	    boolean stopOnError,
	    boolean encrypt,
	    String key,
	    String keyId ) {
	}

	/**
	 * Prevents instantiation of this utility class.
	 */
	private BXObfuscator() {
		// Prevent instantiation
	}

	/**
	 * Main entry point for the obfuscator CLI tool. Exits the JVM with the resulting status code.
	 *
	 * @param args command-line arguments
	 */
	public static void main( String[] args ) {
		System.exit( run( args, System.out, System.err ) );
	}

	/**
	 * Runs the obfuscator against the provided arguments and streams. Testable entry point that
	 * returns an exit code instead of terminating the JVM.
	 *
	 * @param args command-line arguments
	 * @param out  standard output stream for user-facing messages
	 * @param err  standard error stream for error messages
	 *
	 * @return {@code 0} on success, otherwise a non-zero exit code
	 */
	public static int run( String[] args, PrintStream out, PrintStream err ) {
		// Help short-circuits before doing any work
		for ( String arg : args ) {
			if ( arg.equalsIgnoreCase( "--help" ) || arg.equalsIgnoreCase( "-h" ) ) {
				printHelp( out );
				return 0;
			}
		}

		// Ensure the runtime (and therefore the parser) is initialized
		BoxRuntime.getInstance();

		ObfuscatorOptions options;
		try {
			options = parseArguments( args );
		} catch ( IllegalArgumentException e ) {
			err.println( "Error: " + e.getMessage() );
			return 1;
		}

		Path		targetRoot	= Paths.get( options.targetPath() ).toAbsolutePath().normalize();

		// Resolve exclude paths once, normalized to absolute
		List<Path>	excludes	= new ArrayList<>();
		for ( String exclude : options.excludePaths() ) {
			excludes.add( Paths.get( exclude ).toAbsolutePath().normalize() );
		}

		int	successCount	= 0;
		int	failureCount	= 0;

		for ( String sourceArg : options.sourcePaths() ) {
			Path sourcePath = Paths.get( sourceArg ).toAbsolutePath().normalize();

			if ( !Files.exists( sourcePath ) ) {
				err.println( "Error: source path does not exist: " + sourcePath );
				return 1;
			}

			// Build the list of files to process and, for each, the source root used to
			// compute its relative output location.
			List<Path>	filesToProcess	= new ArrayList<>();
			Path		sourceRoot;
			if ( Files.isDirectory( sourcePath ) ) {
				sourceRoot = sourcePath;
				try ( Stream<Path> walk = Files.walk( sourcePath ) ) {
					walk.filter( Files::isRegularFile )
					    .filter( BXObfuscator::isSupported )
					    .forEach( filesToProcess::add );
				} catch ( Exception e ) {
					err.println( "Error walking source path " + sourcePath + ": " + e.getMessage() );
					return 1;
				}
			} else {
				sourceRoot = sourcePath.getParent();
				if ( isSupported( sourcePath ) ) {
					filesToProcess.add( sourcePath );
				} else {
					err.println( "Error: unsupported source file extension: " + sourcePath.getFileName() );
					return 1;
				}
			}

			for ( Path file : filesToProcess ) {
				if ( isExcluded( file, excludes ) ) {
					continue;
				}
				Path	relative	= sourceRoot.relativize( file );
				Path	outputPath	= targetRoot.resolve( relative.toString() );
				try {
					obfuscateFile( file, outputPath, options );
					out.println( "🔒 Obfuscated -> " + outputPath );
					successCount++;
				} catch ( Exception e ) {
					failureCount++;
					err.println( "❌ Error obfuscating " + file + ": " + e.getMessage() );
					if ( options.stopOnError() ) {
						return 1;
					}
				}
			}
		}

		out.println();
		out.println( "📊 Obfuscation complete: " + successCount + " succeeded, " + failureCount + " failed." );
		return failureCount > 0 ? 1 : 0;
	}

	/**
	 * Obfuscates a single source file and writes the result to the target path.
	 *
	 * @param sourcePath the source file to obfuscate
	 * @param targetPath the destination file for the obfuscated output
	 * @param options    the obfuscation options
	 *
	 * @throws Exception if parsing fails or the output cannot be written
	 */
	private static void obfuscateFile( Path sourcePath, Path targetPath, ObfuscatorOptions options ) throws Exception {
		ParsingResult result = new Parser().parse( sourcePath.toFile(), false );
		if ( !result.isCorrect() || result.getRoot() == null ) {
			throw new IllegalStateException( "parse error: " + result.getIssues() );
		}

		BoxNode root = result.getRoot();

		// Comments are stripped up-front so the pretty-printer emits none of them
		ObfuscationVisitor.stripComments( root );

		ObfuscationVisitor visitor = new ObfuscationVisitor( options.renameVars(), options.renameFunctions(), options.renameArgs() );
		visitor.collectFunctionNames( root );
		root.accept( visitor );

		String	obfuscated	= PrettyPrint.prettyPrint( root );
		byte[]	outputBytes	= obfuscated.getBytes( StandardCharsets.UTF_8 );

		// Optionally encrypt the obfuscated source at rest. The runtime decrypts it in memory,
		// just before parsing, so the distributed file is unreadable but still runs on any version.
		if ( options.encrypt() ) {
			outputBytes = CodeEncryption.encrypt( outputBytes, options.keyId(), options.key() );
		}

		Path parent = targetPath.getParent();
		if ( parent != null ) {
			Files.createDirectories( parent );
		}
		Files.write( targetPath, outputBytes );
	}

	/**
	 * Returns true if the file has a supported source extension.
	 *
	 * @param path the file path to test
	 *
	 * @return true when the extension is supported
	 */
	private static boolean isSupported( Path path ) {
		String	name	= path.getFileName().toString();
		int		dot		= name.lastIndexOf( '.' );
		if ( dot < 0 ) {
			return false;
		}
		return SUPPORTED_EXTENSIONS.contains( name.substring( dot + 1 ).toLowerCase() );
	}

	/**
	 * Returns true if the given file lives under any of the excluded paths.
	 *
	 * @param file     the candidate file (absolute, normalized)
	 * @param excludes the list of excluded paths (absolute, normalized)
	 *
	 * @return true when the file should be skipped
	 */
	private static boolean isExcluded( Path file, List<Path> excludes ) {
		for ( Path exclude : excludes ) {
			if ( file.startsWith( exclude ) ) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Parses command-line arguments into an {@link ObfuscatorOptions} record.
	 *
	 * @param args the command-line arguments
	 *
	 * @return the parsed options
	 *
	 * @throws IllegalArgumentException if required arguments are missing or invalid
	 */
	private static ObfuscatorOptions parseArguments( String[] args ) {
		List<String>	sourcePaths		= new ArrayList<>();
		String			targetPath		= null;
		List<String>	excludePaths	= new ArrayList<>();
		boolean			renameVars		= true;
		boolean			renameFunctions	= false;
		boolean			renameArgs		= false;
		boolean			stopOnError		= false;
		boolean			encrypt			= false;
		String			key				= null;
		String			keyId			= null;

		for ( int i = 0; i < args.length; i++ ) {
			String arg = args[ i ];
			if ( arg.equalsIgnoreCase( "--source" ) && i + 1 < args.length ) {
				for ( String part : args[ ++i ].split( "," ) ) {
					String trimmed = part.trim();
					if ( !trimmed.isEmpty() ) {
						sourcePaths.add( trimmed );
					}
				}
			} else if ( arg.equalsIgnoreCase( "--target" ) && i + 1 < args.length ) {
				targetPath = args[ ++i ];
			} else if ( arg.equalsIgnoreCase( "--excludes" ) && i + 1 < args.length ) {
				for ( String part : args[ ++i ].split( "," ) ) {
					String trimmed = part.trim();
					if ( !trimmed.isEmpty() ) {
						excludePaths.add( trimmed );
					}
				}
			} else if ( arg.equalsIgnoreCase( "--no-rename-vars" ) ) {
				renameVars = false;
			} else if ( arg.equalsIgnoreCase( "--rename-functions" ) ) {
				renameFunctions = true;
			} else if ( arg.equalsIgnoreCase( "--rename-args" ) ) {
				renameArgs = true;
			} else if ( arg.equalsIgnoreCase( "--stopOnError" ) ) {
				stopOnError = true;
			} else if ( arg.equalsIgnoreCase( "--encrypt" ) ) {
				encrypt = true;
			} else if ( arg.equalsIgnoreCase( "--key" ) && i + 1 < args.length ) {
				key = args[ ++i ];
			} else if ( arg.equalsIgnoreCase( "--key-id" ) && i + 1 < args.length ) {
				keyId = args[ ++i ];
			}
		}

		if ( sourcePaths.isEmpty() ) {
			sourcePaths.add( System.getProperty( "user.dir" ) );
		}

		if ( targetPath == null ) {
			throw new IllegalArgumentException( "--target is required" );
		}

		if ( encrypt ) {
			if ( key == null || key.isEmpty() ) {
				throw new IllegalArgumentException( "--key is required when --encrypt is used" );
			}
			// Default the key-id label when omitted so a single-artifact encrypt still works
			if ( keyId == null || keyId.isEmpty() ) {
				keyId = "default";
			}
		}

		return new ObfuscatorOptions( sourcePaths, targetPath, excludePaths, renameVars, renameFunctions, renameArgs, stopOnError, encrypt, key,
		    keyId );
	}

	/**
	 * Prints the help message for the obfuscator tool.
	 *
	 * @param out the stream to print help to
	 */
	private static void printHelp( PrintStream out ) {
		out.println( "🔒 BoxLang Obfuscator - Obfuscate BoxLang/ColdFusion source for non-revealing deployment" );
		out.println();
		out.println( "📋 USAGE:" );
		out.println( "  boxlang obfuscate --source <PATH[,PATH,...]> --target <DIR> [OPTIONS]" );
		out.println( "  java -jar boxlang.jar ortus.boxlang.compiler.BXObfuscator [OPTIONS]" );
		out.println();
		out.println( "⚙️  OPTIONS:" );
		out.println( "  -h, --help                  ❓ Show this help message and exit" );
		out.println( "      --source <PATH[,...]>   📂 Source file(s) or directory (default: current directory)" );
		out.println( "      --target <DIR>          🎯 Target directory for obfuscated output (required)" );
		out.println( "      --excludes <PATH[,...]> 🚫 Files/directories to skip" );
		out.println( "      --no-rename-vars        🔤 Disable local variable renaming (default: enabled)" );
		out.println( "      --rename-functions      🔧 Rename private/script functions and call sites (default: off)" );
		out.println( "      --rename-args           📥 Rename function argument names (default: off)" );
		out.println( "      --stopOnError           🛑 Stop processing on the first error (default: off)" );
		out.println( "      --encrypt               🔐 Encrypt output at rest (decrypted in memory at parse time)" );
		out.println( "      --key <SECRET>          🔑 Secret used to derive the encryption key (required with --encrypt)" );
		out.println( "      --key-id <LABEL>        🏷️  Key label baked into each file; the runtime resolves the key by" );
		out.println( "                                 this label (default: \"default\"). Give each module its own key-id." );
		out.println();
		out.println( "🔒 WHAT IT DOES:" );
		out.println( "  • Strips all comments and documentation" );
		out.println( "  • Renames var-declared local variables to short opaque names" );
		out.println( "  • Emits compact source that runs identically to the original" );
		out.println( "  • With --encrypt: writes unreadable ciphertext; the runtime decrypts in memory before" );
		out.println( "    parsing, using a key resolved from env BOXLANG_CODE_KEY_<KEYID> or boxlang.json" );
		out.println( "    security.codeKeys.<keyId>. Not bytecode, so no BoxLang-version binding." );
		out.println();
		out.println( "🔧 SUPPORTED SOURCE FILES:" );
		out.println( "  .bx .bxs .bxm  - BoxLang class/script/template files" );
		out.println( "  .cfm .cfc .cfs - ColdFusion markup/component/script files" );
		out.println();
		out.println( "💡 EXAMPLES:" );
		out.println( "  # Obfuscate a directory tree into ./dist" );
		out.println( "  boxlang obfuscate --source ./src --target ./dist" );
		out.println();
		out.println( "  # Aggressively obfuscate a single file (rename private functions too)" );
		out.println( "  boxlang obfuscate --source app.bx --target out/ --rename-functions" );
		out.println();
		out.println( "  # Obfuscate AND encrypt two modules, each with its own key-id" );
		out.println( "  boxlang obfuscate --source ./modA --target ./distA --encrypt --key secretA --key-id moduleA" );
		out.println( "  boxlang obfuscate --source ./modB --target ./distB --encrypt --key secretB --key-id moduleB" );
		out.println( "  # Clients run them by configuring security.codeKeys.moduleA / .moduleB (or env vars)" );
		out.println();
		out.println( "📖 More Information:" );
		out.println( "  📖 Documentation: https://boxlang.ortusbooks.com/" );
		out.println( "  💬 Community: https://community.ortussolutions.com/c/boxlang/42" );
		out.println( "  💾 GitHub: https://github.com/ortus-boxlang" );
		out.println();
	}
}
