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
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;

import ortus.boxlang.compiler.ast.visitor.StringLiteralAuditVisitor;
import ortus.boxlang.compiler.parser.Parser;
import ortus.boxlang.compiler.parser.ParsingResult;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.exceptions.ExceptionUtil;

/**
 * 
 * THIS IS A TEST CLASS WHICH IS ONLY USED FOR DEBUGGING.
 * IT'S NOT INTEDED TO BE USER FACING AND MAY BE REMOVED AT ANY TIME.
 * 
 * I am a CLI tool that walks a directory of BoxLang/CFML source files,
 * parses each file, and collects every string literal (plain literals and
 * literal segments of interpolated strings). A top-N report of the most
 * frequently occurring strings is printed to the console.
 * <p>
 * Whitespace in the reported strings is replaced with readable placeholders
 * ({@code <space>}, {@code <tab>}, {@code <LF>}, {@code <CR>}, {@code <FF>})
 * so the output is unambiguous.
 */
public class StringLiteralAudit {

	public static void main( String[] args ) {
		BoxRuntime runtime = BoxRuntime.getInstance();

		try {
			String	source	= ".";
			int		topN	= 100;
			boolean	quiet	= false;

			for ( int i = 0; i < args.length; i++ ) {
				if ( args[ i ].equalsIgnoreCase( "--help" ) || args[ i ].equalsIgnoreCase( "-h" ) ) {
					printHelp();
					System.exit( 0 );
				}
				if ( args[ i ].equalsIgnoreCase( "--source" ) ) {
					if ( i + 1 >= args.length || args[ i + 1 ].startsWith( "--" ) ) {
						throw new BoxRuntimeException( "--source requires a path" );
					}
					source = args[ ++i ];
				}
				if ( args[ i ].equalsIgnoreCase( "--top" ) ) {
					if ( i + 1 >= args.length || args[ i + 1 ].startsWith( "--" ) ) {
						throw new BoxRuntimeException( "--top requires a number" );
					}
					topN = Integer.parseInt( args[ ++i ] );
				}
				if ( args[ i ].equalsIgnoreCase( "--quiet" ) ) {
					quiet = true;
				}
			}

			Path sourcePath = Paths.get( source ).normalize();
			if ( !sourcePath.isAbsolute() ) {
				sourcePath = Paths.get( "" ).resolve( sourcePath ).normalize().toAbsolutePath().normalize();
			}

			if ( !sourcePath.toFile().exists() ) {
				System.out.println( "Source path does not exist: " + sourcePath );
				System.exit( 1 );
			}

			// Thread-safe accumulator shared across parallel scans
			Map<String, LongAdder>	globalCounts		= new ConcurrentHashMap<>();
			long[]					totalFilesScanned	= { 0 };
			long[]					totalFilesParsed	= { 0 };
			long[]					totalStringLiterals	= { 0 };

			final boolean			finalQuiet			= quiet;
			final int				finalTopN			= topN;

			if ( sourcePath.toFile().isDirectory() ) {
				System.out.println( "Scanning all files in " + sourcePath );
				System.out.println();
				try {
					Files.walk( sourcePath, FileVisitOption.FOLLOW_LINKS )
					    .parallel()
					    .filter( Files::isRegularFile )
					    .forEach( path -> {
						    String ext = extensionOf( path );
						    if ( ext != null ) {
							    totalFilesScanned[ 0 ]++;
							    scanFile( path, globalCounts, totalFilesParsed, totalStringLiterals, finalQuiet );
						    }
					    } );
				} catch ( IOException e ) {
					throw new BoxRuntimeException( "Error walking source path", e );
				}
			} else {
				totalFilesScanned[ 0 ]++;
				scanFile( sourcePath, globalCounts, totalFilesParsed, totalStringLiterals, finalQuiet );
			}

			// --- Produce the top-N report ---
			System.out.println();
			System.out.println( "********************************************************" );
			System.out.println( "*        BoxLang String Literal Audit Report           *" );
			System.out.println( "********************************************************" );
			System.out.println();
			System.out.println( "  Files scanned:  " + totalFilesScanned[ 0 ] );
			System.out.println( "  Files parsed:   " + totalFilesParsed[ 0 ] );
			System.out.println( "  Strings found:  " + totalStringLiterals[ 0 ] );
			System.out.println( "  Unique strings: " + globalCounts.size() );
			System.out.println();
			System.out.println( "--- Top " + finalTopN + " most common strings ---" );
			System.out.println();

			String	headerFormat	= "%-5s %-12s %s";
			String	rowFormat		= "%-5d %-12d %s";
			System.out.println( String.format( headerFormat, "Rank", "Count", "String" ) );
			System.out.println( String.format( headerFormat, "----", "-----", "------" ) );

			// Sort by count descending (convert LongAdder to long for comparison)
			final int[] rank = { 1 };
			globalCounts.entrySet().stream()
			    .sorted( ( a, b ) -> Long.compare( b.getValue().longValue(), a.getValue().longValue() ) )
			    .limit( finalTopN )
			    .forEachOrdered( entry -> {
				    System.out.println( String.format( rowFormat, rank[ 0 ]++, entry.getValue().longValue(), entry.getKey() ) );
			    } );

			System.out.println();
			System.out.println( "Legend: <space> = SPACE, <tab> = TAB, <LF> = LF, <CR> = CR, <FF> = FORMFEED" );
			System.out.println();

		} finally {
			runtime.shutdown();
		}
	}

	/**
	 * Scan a single source file, parse it, and collect string literals.
	 */
	private static void scanFile(
	    Path filePath,
	    Map<String, LongAdder> globalCounts,
	    long[] totalParsed,
	    long[] totalStrings,
	    boolean quiet ) {

		String logMessage = "Processing: " + filePath;
		try {
			if ( DiskClassUtil.isJavaByteCode( filePath.toFile() ) ) {
				return;
			}
			ParsingResult result = new Parser().parse( filePath.toFile() );
			if ( result.isCorrect() ) {
				StringLiteralAuditVisitor visitor = new StringLiteralAuditVisitor();
				result.getRoot().accept( visitor );
				// Merge per-file counts into global map
				visitor.getStringCounts().forEach( ( sanitized, adder ) -> {
					globalCounts.computeIfAbsent( sanitized, k -> new LongAdder() ).add( adder.longValue() );
				} );
				totalParsed[ 0 ]++;
				totalStrings[ 0 ] += visitor.getTotalStrings();
			} else {
				String errorMessage = "Parse error in " + filePath + "\n" +
				    result.getIssues().stream()
				        .map( Object::toString )
				        .collect( Collectors.joining( "\n" ) );
				System.out.println( errorMessage );
			}
		} catch ( Throwable e ) {
			logMessage += "\n" + "Processing failed: " + e.getMessage()
			    + "\n" + ExceptionUtil.getStackTraceAsString( e );
		} finally {
			if ( !quiet ) {
				System.out.println( logMessage );
			}
		}
	}

	/**
	 * Return a file extension we care about, or null if this file should be skipped.
	 */
	private static String extensionOf( Path path ) {
		String	name	= path.getFileName().toString();
		int		dot		= name.lastIndexOf( '.' );
		if ( dot < 0 ) {
			return null;
		}
		String ext = name.substring( dot + 1 ).toLowerCase();
		return switch ( ext ) {
			case "cfm", "cfc", "cfs", "cfml", "bxs", "bx", "bxm" -> ext;
			default -> null;
		};
	}

	private static void printHelp() {
		System.out.println( "🔤 BoxLang StringLiteralAudit - Find the most common string literals in a codebase" );
		System.out.println();
		System.out.println( "📋 USAGE:" );
		System.out.println( "  boxlang stringiteralaudit [OPTIONS]" );
		System.out.println( "  java -jar boxlang.jar ortus.boxlang.compiler.StringLiteralAudit [OPTIONS]" );
		System.out.println();
		System.out.println( "⚙️  OPTIONS:" );
		System.out.println( "  -h, --help              Show this help" );
		System.out.println( "      --source <PATH>     Source directory or file (default: .)" );
		System.out.println( "      --top <N>           Show top N strings (default: 100)" );
		System.out.println( "      --quiet             Suppress per-file processing messages" );
		System.out.println();
		System.out.println( "💡 EXAMPLES:" );
		System.out.println( "  boxlang stringiteralaudit" );
		System.out.println( "  boxlang stringiteralaudit --source /path/to/code --top 50" );
		System.out.println( "  boxlang stringiteralaudit --quiet" );
		System.out.println();
		System.out.println( "📖 More Information:" );
		System.out.println( "  https://boxlang.ortusbooks.com/" );
	}

}