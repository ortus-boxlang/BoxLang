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
import java.io.PrintStream;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import ortus.boxlang.compiler.ast.Issue;
import ortus.boxlang.compiler.parser.Parser;
import ortus.boxlang.compiler.parser.ParsingResult;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.types.util.JSONUtil;

/**
 * I am a CLI tool that checks whether one or more BoxLang/CFML source files are syntactically
 * valid, without executing them or compiling them to bytecode - similar to {@code bash -n} or
 * {@code node --check}.
 */
public final class SyntaxCheck {

	private static final Set<String> SUPPORTED_EXTENSIONS = Set.of( "cfm", "cfc", "cfs", "bx", "bxs", "bxm" );

	/**
	 * The result of checking a single file
	 *
	 * @param file   the file that was checked
	 * @param valid  whether the file parsed without any syntax issues
	 * @param issues the list of syntax issues found (empty when valid)
	 */
	private record FileResult( Path file, boolean valid, List<Issue> issues ) {
	}

	/**
	 * Prevents instantiation of this utility class.
	 */
	private SyntaxCheck() {
	}

	/**
	 * Executes the syntax checker CLI and exits the JVM with the resulting status code.
	 *
	 * @param args command-line arguments
	 */
	public static void main( String[] args ) {
		BoxRuntime runtime = BoxRuntime.getInstance();
		try {
			System.exit( run( args, System.out, System.err ) );
		} finally {
			runtime.shutdown();
		}
	}

	/**
	 * Runs the syntax checker against the provided arguments and streams.
	 *
	 * @param args command-line arguments
	 * @param out  standard output stream for user-facing messages
	 * @param err  standard error stream for error messages
	 *
	 * @return {@code 0} if every checked file is syntactically valid, otherwise {@code 1}
	 */
	static int run( String[] args, PrintStream out, PrintStream err ) {
		String			source	= null;
		List<String>	files	= new ArrayList<>();
		boolean			quiet	= false;
		String			format	= "text";

		for ( int i = 0; i < args.length; i++ ) {
			String arg = args[ i ];
			if ( arg.equalsIgnoreCase( "--help" ) || arg.equalsIgnoreCase( "-h" ) ) {
				printHelp( out );
				return 0;
			} else if ( arg.equalsIgnoreCase( "--source" ) ) {
				if ( i + 1 >= args.length ) {
					err.println( "Error: --source requires a path" );
					return 1;
				}
				source = args[ ++i ];
			} else if ( arg.equalsIgnoreCase( "--quiet" ) || arg.equalsIgnoreCase( "-q" ) ) {
				quiet = true;
			} else if ( arg.equalsIgnoreCase( "--format" ) ) {
				if ( i + 1 >= args.length ) {
					err.println( "Error: --format requires a value (text|json)" );
					return 1;
				}
				format = args[ ++i ].toLowerCase();
				if ( !format.equals( "text" ) && !format.equals( "json" ) ) {
					err.println( "Error: --format must be 'text' or 'json'" );
					return 1;
				}
			} else if ( arg.startsWith( "--" ) ) {
				err.println( "Error: Unknown option: " + arg );
				return 1;
			} else {
				files.add( arg );
			}
		}

		Set<Path>				targets		= new LinkedHashSet<>();
		Map<Path, FileResult>	resultMap	= new ConcurrentHashMap<>();
		boolean					usageError	= false;

		if ( source != null ) {
			Path sourcePath = resolvePath( source );
			if ( !Files.exists( sourcePath ) ) {
				err.println( "Error: --source path does not exist: " + sourcePath );
				return 1;
			}
			if ( Files.isDirectory( sourcePath ) ) {
				try {
					Files.walk( sourcePath, FileVisitOption.FOLLOW_LINKS )
					    .filter( Files::isRegularFile )
					    .filter( path -> SUPPORTED_EXTENSIONS.contains( extensionOf( path ) ) )
					    .filter( path -> !DiskClassUtil.isJavaByteCode( path.toFile() ) )
					    .forEach( targets::add );
				} catch ( IOException e ) {
					err.println( "Error walking source path: " + e.getMessage() );
					return 1;
				}
			} else {
				targets.add( sourcePath );
			}
		}

		for ( String f : files ) {
			Path filePath = resolvePath( f );
			if ( !Files.exists( filePath ) ) {
				err.println( "❌ File does not exist: " + filePath );
				usageError = true;
				continue;
			}
			if ( !SUPPORTED_EXTENSIONS.contains( extensionOf( filePath ) ) ) {
				err.println( "⚠️  Skipping unsupported file type: " + filePath );
				continue;
			}
			targets.add( filePath );
		}

		if ( targets.isEmpty() ) {
			err.println( "Error: No files to check. Provide one or more file paths or --source <path>." );
			return 1;
		}

		targets.parallelStream().forEach( path -> resultMap.put( path, checkFile( path ) ) );

		int	validCount		= 0;
		int	invalidCount	= 0;
		for ( FileResult result : resultMap.values() ) {
			if ( result.valid() ) {
				validCount++;
			} else {
				invalidCount++;
			}
		}

		if ( format.equals( "json" ) ) {
			if ( !printJSON( out, err, targets, resultMap ) ) {
				return 1;
			}
		} else {
			printText( out, err, targets, resultMap, quiet, validCount, invalidCount );
		}

		return ( invalidCount > 0 || usageError ) ? 1 : 0;
	}

	/**
	 * Parses a single file and reports whether it is syntactically valid.
	 *
	 * @param path the file to check
	 *
	 * @return the result of the check
	 */
	private static FileResult checkFile( Path path ) {
		try {
			if ( DiskClassUtil.isJavaByteCode( path.toFile() ) ) {
				return new FileResult( path, false, List.of( new Issue( "Skipped: precompiled bytecode file", null ) ) );
			}
			ParsingResult result = new Parser().parse( path.toFile() );
			return new FileResult( path, result.isCorrect(), result.getIssues() );
		} catch ( Throwable t ) {
			String message = t.getMessage() != null ? t.getMessage() : t.toString();
			return new FileResult( path, false, List.of( new Issue( message, null ) ) );
		}
	}

	/**
	 * Prints human-readable check results: silent per-file success, an error block per invalid
	 * file, and a final summary line.
	 */
	private static void printText( PrintStream out, PrintStream err, Set<Path> targets, Map<Path, FileResult> resultMap, boolean quiet, int validCount,
	    int invalidCount ) {
		for ( Path path : targets ) {
			FileResult result = resultMap.get( path );
			if ( result.valid() ) {
				if ( !quiet ) {
					out.println( "✅ " + path );
				}
			} else {
				err.println( "❌ " + path );
				for ( Issue issue : result.issues() ) {
					err.println( "   " + issue.toString() );
				}
			}
		}
		if ( !quiet ) {
			out.println();
			out.println( "───────────────────────────────" );
			out.println( String.format( "✅ %d valid   ❌ %d invalid   (%d files checked)", validCount, invalidCount, targets.size() ) );
		}
	}

	/**
	 * Prints check results as a JSON array of {@code {file, valid, issues}} records, for
	 * CI/editor tooling integration.
	 *
	 * @return {@code true} if the JSON was written successfully, {@code false} on a serialization error
	 */
	private static boolean printJSON( PrintStream out, PrintStream err, Set<Path> targets, Map<Path, FileResult> resultMap ) {
		List<Map<String, Object>> records = new ArrayList<>();
		for ( Path path : targets ) {
			FileResult			result	= resultMap.get( path );
			Map<String, Object>	record	= new LinkedHashMap<>();
			record.put( "file", path.toString() );
			record.put( "valid", result.valid() );
			List<Map<String, Object>> issues = new ArrayList<>();
			for ( Issue issue : result.issues() ) {
				Map<String, Object> issueMap = new LinkedHashMap<>();
				issueMap.put( "message", issue.getMessage() );
				if ( issue.getPosition() != null ) {
					issueMap.put( "line", issue.getPosition().getStart().getLine() );
					issueMap.put( "column", issue.getPosition().getStart().getColumn() );
				} else {
					issueMap.put( "line", null );
					issueMap.put( "column", null );
				}
				issues.add( issueMap );
			}
			record.put( "issues", issues );
			records.add( record );
		}
		try {
			out.println( JSONUtil.getJSONBuilder( true ).asString( records ) );
			return true;
		} catch ( IOException e ) {
			err.println( "Error: Failed to serialize results to JSON: " + e.getMessage() );
			return false;
		}
	}

	/**
	 * Resolves a raw path argument to an absolute, normalized path against the current working
	 * directory.
	 */
	private static Path resolvePath( String raw ) {
		Path path = Paths.get( raw ).normalize();
		if ( !path.isAbsolute() ) {
			path = Paths.get( "" ).resolve( path ).normalize().toAbsolutePath().normalize();
		}
		return path;
	}

	/**
	 * Returns the lower-cased file extension of a path, or an empty string if it has none.
	 */
	private static String extensionOf( Path path ) {
		String	name	= path.getFileName().toString();
		int		dot		= name.lastIndexOf( '.' );
		return dot >= 0 ? name.substring( dot + 1 ).toLowerCase() : "";
	}

	/**
	 * Prints the help message for the SyntaxCheck tool.
	 */
	private static void printHelp( PrintStream out ) {
		out.println( "✅ BoxLang Check - A CLI tool for validating source file syntax without executing it" );
		out.println();
		out.println( "📋 USAGE:" );
		out.println( "  boxlang check [OPTIONS] [FILE...]  # 🔧 Using OS binary" );
		out.println( "  java -jar boxlang.jar ortus.boxlang.compiler.SyntaxCheck [OPTIONS] [FILE...] # 🐍 Using Java JAR" );
		out.println();
		out.println( "⚙️  OPTIONS:" );
		out.println( "  -h, --help                  ❓ Show this help message and exit" );
		out.println( "      --source <PATH>         📁 Path to a source directory or file to check" );
		out.println( "      --format <text|json>    📄 Output format (default: text)" );
		out.println( "  -q, --quiet                  🔇 Suppress success output; failures are still reported" );
		out.println();
		out.println( "🔧 SUPPORTED SOURCE FILES:" );
		out.println( "  .cfm  .cfc  .cfs  .bx  .bxs  .bxm" );
		out.println();
		out.println( "💡 EXAMPLES:" );
		out.println( "  # ✅ Check one or more files" );
		out.println( "  boxlang check myapp.bx myComponent.cfc" );
		out.println();
		out.println( "  # ✅ Check an entire directory (e.g. in CI or a git hook)" );
		out.println( "  boxlang check --source ./src" );
		out.println();
		out.println( "  # 📄 Machine-readable output for editor/CI tooling" );
		out.println( "  boxlang check --source ./src --format json" );
		out.println();
		out.println( "🔚 EXIT CODES:" );
		out.println( "  0  All checked files are syntactically valid" );
		out.println( "  1  One or more files have syntax errors, or a usage error occurred" );
		out.println();
		out.println( "📖 More Information:" );
		out.println( "  📖 Documentation: https://boxlang.ortusbooks.com/" );
		out.println( "  💬 Community: https://community.ortussolutions.com/c/boxlang/42" );
		out.println( "  💾 GitHub: https://github.com/ortus-boxlang" );
		out.println();
	}

}
