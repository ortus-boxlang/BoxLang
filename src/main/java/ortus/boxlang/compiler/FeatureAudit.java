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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ortus.boxlang.compiler.ast.visitor.FeatureAuditVisitor;
import ortus.boxlang.compiler.parser.Parser;
import ortus.boxlang.compiler.parser.ParsingResult;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * I am a CLI tool for auditing code to determine BIFs and tags in use
 * which are not yet supported by BoxLang.
 */
public class FeatureAudit {

	public static void main( String[] args ) {
		BoxRuntime runtime = BoxRuntime.getInstance();
		// This must be run after the runtime is loaded, but before we parse any files.
		FeatureAuditVisitor.setupRuntimeStubs();
		Map<String, List<FeatureAuditVisitor.FeatureUsed>>			results				= new HashMap<>();
		Map<String, List<FeatureAuditVisitor.AggregateFeatureUsed>>	aggregateResults	= new HashMap<>();
		StringBuffer												reportText			= new StringBuffer();
		Map<String, Integer>										filesProcessed		= new HashMap<>();

		try {
			String	source		= ".";
			Boolean	missing		= false;
			Boolean	aggregate	= false;
			Boolean	summary		= false;
			String	reportFile	= null;
			Boolean	quiet		= false;

			for ( int i = 0; i < args.length; i++ ) {
				if ( args[ i ].equalsIgnoreCase( "--source" ) ) {
					if ( i + 1 >= args.length || args[ i + 1 ].startsWith( "--" ) ) {
						throw new BoxRuntimeException( "--source requires a path" );
					}
					source = args[ ++i ];
				}
				if ( args[ i ].equalsIgnoreCase( "--missing" ) ) {
					missing = true;
				}
				if ( args[ i ].equalsIgnoreCase( "--aggregate" ) ) {
					// See if next token is "summary"
					if ( i + 1 < args.length && args[ i + 1 ].equalsIgnoreCase( "summary" ) ) {
						summary = true;
						i++;
					}
					aggregate = true;
				}
				if ( args[ i ].equalsIgnoreCase( "--reportFile" ) ) {
					if ( i + 1 >= args.length || args[ i + 1 ].startsWith( "--" ) ) {
						throw new BoxRuntimeException( "--reportFile requires a path" );
					}
					reportFile = args[ ++i ];
				}
				if ( args[ i ].equalsIgnoreCase( "--quiet" ) ) {
					quiet = true;
				}
			}
			Path	sourcePath	= Paths.get( source ).normalize();
			Path	reportPath	= null;
			if ( !sourcePath.isAbsolute() ) {
				sourcePath = Paths.get( "" ).resolve( sourcePath ).normalize().toAbsolutePath().normalize();
			}

			if ( !sourcePath.toFile().exists() ) {
				System.out.println( "Source Path does not exist: " + sourcePath.toString() );
				System.exit( 1 );
			}

			if ( reportFile != null ) {
				// ensure path ends with .csv
				if ( !reportFile.endsWith( ".csv" ) ) {
					reportFile += ".csv";
				}
				reportPath = Paths.get( reportFile ).normalize();
				if ( !reportPath.isAbsolute() ) {
					reportPath = Paths.get( "" ).resolve( reportPath ).normalize().toAbsolutePath().normalize();
				}
				// create directories if they don't exist
				if ( !reportPath.getParent().toFile().exists() ) {
					try {
						Files.createDirectories( reportPath.getParent() );
					} catch ( IOException e ) {
						e.printStackTrace();
					}
				}
			}

			if ( aggregate ) {
				reportText.append( "File," ).append( FeatureAuditVisitor.AggregateFeatureUsed.csvHeader() ).append( "\n" );
			} else {
				reportText.append( "File," ).append( FeatureAuditVisitor.FeatureUsed.csvHeader() ).append( "\n" );
			}
			final Boolean	finalMissing	= missing;
			final Boolean	finalAggregate	= aggregate;
			final Boolean	finalQuiet		= quiet;
			final Boolean	finalSummary	= summary;
			final Path		finalReportPath	= reportPath;

			if ( sourcePath.toFile().isDirectory() ) {
				System.out.println( "Scanning all files in " + sourcePath.toString() );
				System.out.println();
				try {
					final Path finalSourcePath = sourcePath;
					Files.walk( finalSourcePath )
					    .parallel()
					    .filter( Files::isRegularFile )
					    .forEach( path -> {
						    String sourceExtension = path.getFileName().toString().substring( path.getFileName().toString().lastIndexOf( "." ) + 1 );
						    if ( extensionWeCareAbout( sourceExtension, filesProcessed ) ) {
							    scanFile( path, results, aggregateResults, finalMissing, finalAggregate, finalReportPath != null, reportText,
							        finalQuiet || finalSummary );
						    }
					    } );
				} catch ( IOException e ) {
					throw new BoxRuntimeException( "Error walking source path", e );
				}
			} else {
				String sourceExtension = sourcePath.getFileName().toString().substring( sourcePath.getFileName().toString().lastIndexOf( "." ) + 1 );
				if ( extensionWeCareAbout( sourceExtension, filesProcessed ) ) {
					scanFile( sourcePath, results, aggregateResults, finalMissing, finalAggregate, finalReportPath != null, reportText,
					    finalQuiet || finalSummary );
				}
			}
			if ( summary ) {
				if ( finalReportPath != null ) {
					reportText.setLength( 0 );
					reportText.append( "Name,Type,Module,Missing,Count\n" );
				}
				System.out.println();
				System.out.println( "*******************" );
				System.out.println( "*     Summary     *" );
				System.out.println( "*******************" );
				System.out.println();
				System.out.println( "Files processed: " + filesProcessed.values().stream().mapToInt( Integer::intValue ).sum() );
				filesProcessed.forEach( ( k, v ) -> System.out.println( "  * ." + k + ": " + v ) );
				System.out.println();
				System.out.println();

				Map<String, FeatureAuditVisitor.AggregateFeatureUsed> summaryData = new HashMap<>();
				results.forEach( ( k, v ) -> {
					v.forEach( f -> {
						String key = String.format( "%s%s (%s) - %s", ( f.missing() ? "[MISSING] " : "" ), f.name(), f.type(), f.module() );
						if ( summaryData.containsKey( key ) ) {
							summaryData.put( key, summaryData.get( key ).increment() );
						} else {
							summaryData.put( key, new FeatureAuditVisitor.AggregateFeatureUsed( f.name(), f.type(), f.module(), f.missing(), 1 ) );
						}
					} );
				} );
				summaryData.values().stream().sorted().forEach( data -> {
					if ( !finalQuiet ) {
						System.out.println( data.toString() );
					}
					if ( finalReportPath != null ) {
						// Name,Type,Module,Missing,Count
						reportText.append( data.name() ).append( "," ).append( data.type() ).append( "," ).append( data.module() ).append( "," )
						    .append( data.missing() ).append( "," ).append( data.count() ).append( "\n" );
					}
				} );

				if ( finalReportPath != null ) {
					try {
						Files.write( finalReportPath, reportText.toString().getBytes() );
						System.out.println();
						System.out.println( "Report written to " + finalReportPath.toString() );
					} catch ( IOException e ) {
						e.printStackTrace();
					}
				}

			} else {
				if ( finalReportPath != null ) {
					try {
						Files.write( finalReportPath, reportText.toString().getBytes() );
						System.out.println();
						System.out.println( "Report written to " + finalReportPath.toString() );
					} catch ( IOException e ) {
						e.printStackTrace();
					}
				}
			}

			System.exit( 0 );
		} finally {
			runtime.shutdown();
		}
	}

	private static void scanFile( Path sourcePath, Map<String, List<FeatureAuditVisitor.FeatureUsed>> results,
	    Map<String, List<FeatureAuditVisitor.AggregateFeatureUsed>> aggregateResults, Boolean missing, Boolean aggregate, boolean doReport,
	    StringBuffer reportText, boolean quiet ) {
		System.out.println( "Processing: " + sourcePath.toString() );
		ParsingResult result;
		try {
			result = new Parser().parse( sourcePath.toFile() );
		} catch ( Throwable e ) {
			System.out.println( "Parsing failed: " + e.getMessage() );
			e.printStackTrace();
			return;
		}
		if ( result.isCorrect() ) {
			FeatureAuditVisitor visitor = new FeatureAuditVisitor();
			result.getRoot().accept( visitor );
			if ( missing ) {
				results.put( sourcePath.toString(), visitor.getFeaturesUsed().stream().filter( f -> f.missing() ).toList() );
				aggregateResults.put( sourcePath.toString(), visitor.getAggregateFeaturesUsed().stream().filter( f -> f.missing() ).toList() );
			} else {
				results.put( sourcePath.toString(), visitor.getFeaturesUsed() );
				aggregateResults.put( sourcePath.toString(), visitor.getAggregateFeaturesUsed() );
			}
			if ( aggregate ) {
				if ( aggregateResults.get( sourcePath.toString() ).size() > 0 ) {
					aggregateResults.get( sourcePath.toString() ).forEach( data -> {
						if ( !quiet )
							System.out.println( data );
						if ( doReport )
							reportText.append( sourcePath.toString() ).append( "," ).append( data.toCSV() ).append( "\n" );
					} );
				}
			} else {
				if ( results.get( sourcePath.toString() ).size() > 0 ) {
					results.get( sourcePath.toString() ).forEach( data -> {
						if ( !quiet )
							System.out.println( data );
						if ( doReport )
							reportText.append( sourcePath.toString() ).append( "," ).append( data.toCSV() ).append( "\n" );
					} );
				}
			}
		} else {
			System.out.println( "Parsing failed for " + sourcePath.toString() );
			result.getIssues().forEach( System.out::println );
		}
	}

	private static boolean extensionWeCareAbout( String extension, Map<String, Integer> filesProcessed ) {
		extension = extension.toLowerCase();
		if ( extension.equals( "cfm" ) || extension.equals( "cfc" ) || extension.equals( "cfs" ) || extension.equals( "cfml" ) || extension.equals( "bxs" )
		    || extension.equals( "bx" ) || extension.equals( "bxm" ) ) {
			if ( filesProcessed.containsKey( extension ) ) {
				filesProcessed.put( extension, filesProcessed.get( extension ) + 1 );
			} else {
				filesProcessed.put( extension, 1 );
			}
			return true;
		}
		return false;
	}

}