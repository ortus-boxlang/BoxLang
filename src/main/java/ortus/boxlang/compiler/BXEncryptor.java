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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.util.CodeEncryption;

/**
 * I am a CLI tool for encrypting BoxLang / ColdFusion source at rest so it can be distributed in a
 * non-readable form. Each source file is encrypted with AES-256-GCM and stamped with a key-id label.
 *
 * The distributed files are unreadable on disk, yet the runtime transparently decrypts them in memory
 * just before parsing (see {@code ortus.boxlang.runtime.util.CodeEncryption}). Because decryption yields
 * source (not bytecode), the files run on any BoxLang version — there is no bytecode/version binding.
 *
 * At load time the runtime resolves the decryption key by the file's key-id from the
 * {@code BOXLANG_CODE_KEY_<KEYID>} environment variable or the {@code security.codeKeys} map in
 * {@code boxlang.json}. Give each module/artifact its own key-id, and hand each customer only the keys
 * they purchased.
 */
public final class BXEncryptor {

	/**
	 * Source file extensions this tool will encrypt (the ones the runtime knows how to parse).
	 */
	private static final Set<String> SUPPORTED_EXTENSIONS = Set.of( "bx", "bxs", "bxm", "cfm", "cfc", "cfs" );

	/**
	 * Parsed command-line options for the encryptor.
	 *
	 * @param sourcePaths  one or more source files/directories to encrypt
	 * @param targetPath   target directory to write encrypted files into
	 * @param excludePaths files/directories to skip
	 * @param key          the secret used to derive the AES key (required)
	 * @param keyId        the key-id label baked into each file header so the runtime resolves the right key
	 * @param stopOnError  stop processing on the first error
	 */
	record EncryptorOptions(
	    List<String> sourcePaths,
	    String targetPath,
	    List<String> excludePaths,
	    String key,
	    String keyId,
	    boolean stopOnError ) {
	}

	/**
	 * Prevents instantiation of this utility class.
	 */
	private BXEncryptor() {
		// Prevent instantiation
	}

	/**
	 * Main entry point for the encryptor CLI tool. Exits the JVM with the resulting status code.
	 *
	 * @param args command-line arguments
	 */
	public static void main( String[] args ) {
		System.exit( run( args, System.out, System.err ) );
	}

	/**
	 * Runs the encryptor against the provided arguments and streams. Testable entry point that returns an
	 * exit code instead of terminating the JVM.
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

		// The runtime is not strictly required for encryption, but initialize it for consistency
		BoxRuntime.getInstance();

		EncryptorOptions options;
		try {
			options = parseArguments( args );
		} catch ( IllegalArgumentException e ) {
			err.println( "Error: " + e.getMessage() );
			return 1;
		}

		Path		targetRoot	= Paths.get( options.targetPath() ).toAbsolutePath().normalize();

		List<Path>	excludes	= new ArrayList<>();
		for ( String exclude : options.excludePaths() ) {
			excludes.add( Paths.get( exclude ).toAbsolutePath().normalize() );
		}

		int	successCount	= 0;
		int	failureCount	= 0;
		int	skippedCount	= 0;

		for ( String sourceArg : options.sourcePaths() ) {
			Path sourcePath = Paths.get( sourceArg ).toAbsolutePath().normalize();

			if ( !Files.exists( sourcePath ) ) {
				err.println( "Error: source path does not exist: " + sourcePath );
				return 1;
			}

			List<Path>	filesToProcess	= new ArrayList<>();
			Path		sourceRoot;
			if ( Files.isDirectory( sourcePath ) ) {
				sourceRoot = sourcePath;
				try ( Stream<Path> walk = Files.walk( sourcePath ) ) {
					walk.filter( Files::isRegularFile )
					    .filter( BXEncryptor::isSupported )
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
					byte[] bytes = Files.readAllBytes( file );
					// Do not double-encrypt an already-encrypted file
					if ( CodeEncryption.isEncrypted( bytes ) ) {
						out.println( "↔️  Already encrypted, skipping -> " + file );
						skippedCount++;
						continue;
					}
					byte[]	encrypted	= CodeEncryption.encrypt( bytes, options.keyId(), options.key() );
					Path	parent		= outputPath.getParent();
					if ( parent != null ) {
						Files.createDirectories( parent );
					}
					Files.write( outputPath, encrypted );
					out.println( "🔐 Encrypted -> " + outputPath );
					successCount++;
				} catch ( Exception e ) {
					failureCount++;
					err.println( "❌ Error encrypting " + file + ": " + e.getMessage() );
					if ( options.stopOnError() ) {
						return 1;
					}
				}
			}
		}

		out.println();
		out.println( "📊 Encryption complete: " + successCount + " encrypted, " + skippedCount + " skipped, " + failureCount + " failed." );
		return failureCount > 0 ? 1 : 0;
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
	 * Parses command-line arguments into an {@link EncryptorOptions} record.
	 *
	 * @param args the command-line arguments
	 *
	 * @return the parsed options
	 *
	 * @throws IllegalArgumentException if required arguments are missing or invalid
	 */
	private static EncryptorOptions parseArguments( String[] args ) {
		List<String>	sourcePaths		= new ArrayList<>();
		String			targetPath		= null;
		List<String>	excludePaths	= new ArrayList<>();
		String			key				= null;
		String			keyId			= null;
		boolean			stopOnError		= false;

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
			} else if ( arg.equalsIgnoreCase( "--key" ) && i + 1 < args.length ) {
				key = args[ ++i ];
			} else if ( arg.equalsIgnoreCase( "--key-id" ) && i + 1 < args.length ) {
				keyId = args[ ++i ];
			} else if ( arg.equalsIgnoreCase( "--stopOnError" ) ) {
				stopOnError = true;
			}
		}

		if ( sourcePaths.isEmpty() ) {
			sourcePaths.add( System.getProperty( "user.dir" ) );
		}

		if ( targetPath == null ) {
			throw new IllegalArgumentException( "--target is required" );
		}

		if ( key == null || key.isEmpty() ) {
			throw new IllegalArgumentException( "--key is required" );
		}

		// Default the key-id label when omitted so a single-artifact encrypt still works
		if ( keyId == null || keyId.isEmpty() ) {
			keyId = "default";
		}

		return new EncryptorOptions( sourcePaths, targetPath, excludePaths, key, keyId, stopOnError );
	}

	/**
	 * Prints the help message for the encryptor tool.
	 *
	 * @param out the stream to print help to
	 */
	private static void printHelp( PrintStream out ) {
		out.println( "🔐 BoxLang Encryptor - Encrypt BoxLang/ColdFusion source for non-revealing deployment" );
		out.println();
		out.println( "📋 USAGE:" );
		out.println( "  boxlang encrypt --source <PATH[,PATH,...]> --target <DIR> --key <SECRET> [OPTIONS]" );
		out.println( "  java -jar boxlang.jar ortus.boxlang.compiler.BXEncryptor [OPTIONS]" );
		out.println();
		out.println( "⚙️  OPTIONS:" );
		out.println( "  -h, --help                  ❓ Show this help message and exit" );
		out.println( "      --source <PATH[,...]>   📂 Source file(s) or directory (default: current directory)" );
		out.println( "      --target <DIR>          🎯 Target directory for encrypted output (required)" );
		out.println( "      --key <SECRET>          🔑 Secret used to derive the encryption key (required)" );
		out.println( "      --key-id <LABEL>        🏷️  Key label baked into each file; the runtime resolves the key by" );
		out.println( "                                 this label (default: \"default\"). Give each module its own key-id." );
		out.println( "      --excludes <PATH[,...]> 🚫 Files/directories to skip" );
		out.println( "      --stopOnError           🛑 Stop processing on the first error (default: off)" );
		out.println();
		out.println( "🔐 HOW IT WORKS:" );
		out.println( "  • Each file is encrypted with AES-256-GCM and stamped with the key-id" );
		out.println( "  • On disk the files are unreadable ciphertext" );
		out.println( "  • The runtime decrypts them in memory just before parsing — no bytecode, no version binding" );
		out.println( "  • Already-encrypted files are skipped (no double-encryption)" );
		out.println();
		out.println( "🔓 RUNNING ENCRYPTED CODE (on the consumer side):" );
		out.println( "  Provide the key by key-id, via environment variable or boxlang.json:" );
		out.println( "    export BOXLANG_CODE_KEY_MODULEA=\"the-secret\"        # env var (keyId uppercased, non-alnum -> _)" );
		out.println( "    // boxlang.json: { \"security\": { \"codeKeys\": { \"moduleA\": \"the-secret\" } } }" );
		out.println();
		out.println( "🔧 SUPPORTED SOURCE FILES:" );
		out.println( "  .bx .bxs .bxm  - BoxLang class/script/template files" );
		out.println( "  .cfm .cfc .cfs - ColdFusion markup/component/script files" );
		out.println();
		out.println( "💡 EXAMPLES:" );
		out.println( "  # Encrypt a project into ./dist with one key" );
		out.println( "  boxlang encrypt --source ./src --target ./dist --key \"my-secret\" --key-id myApp" );
		out.println();
		out.println( "  # Encrypt two modules, each with its own key-id and key" );
		out.println( "  boxlang encrypt --source ./modA --target ./distA --key SECRET-A --key-id moduleA" );
		out.println( "  boxlang encrypt --source ./modB --target ./distB --key SECRET-B --key-id moduleB" );
		out.println();
		out.println( "📖 More Information:" );
		out.println( "  📖 Documentation: https://boxlang.ortusbooks.com/" );
		out.println( "  💬 Community: https://community.ortussolutions.com/c/boxlang/42" );
		out.println( "  💾 GitHub: https://github.com/ortus-boxlang" );
		out.println();
	}
}
