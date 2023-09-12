package ourtus.boxlang.transpiler;

import com.github.javaparser.ast.CompilationUnit;
import org.apache.commons.cli.*;
import ourtus.boxlang.parser.BoxLangParser;
import ourtus.boxlang.parser.ParsingResult;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Main {

	/**
	 * Compiler options
	 * 
	 * @return list of options
	 */
	private static List<Option> options() {
		return new ArrayList<>() {

			{
				// Input file/directory
				add(
				    Option.builder( "i" ).longOpt( "input" )
				        .argName( "path" )
				        .hasArg()
				        .required( true )
				        .desc( "input file or directory" ).build()
				);
				// Output file/directory
				add(
				    Option.builder( "o" ).longOpt( "output" )
				        .argName( "path" )
				        .hasArg()
				        .required( true )
				        .desc( "input file or directory" ).build()

				);

			}
		};
	}

	private static List<Path> scanForFiles( String path, Set<String> extensions ) {
		List<Path> fileList = new ArrayList<Path>();

		try {
			Files.walkFileTree( Paths.get( path ), new HashSet<>(), Integer.MAX_VALUE, new FileVisitor<>() {

				private boolean match( String fileName ) {
					int index = fileName.lastIndexOf( '.' );
					if ( index > 0 ) {
						String ext = fileName.substring( index + 1 );
						return extensions.contains( ext );
					}
					return false;
				}

				@Override
				public FileVisitResult preVisitDirectory( Path dir, BasicFileAttributes attrs ) throws IOException {
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFile( Path file, BasicFileAttributes attrs ) throws IOException {
					if ( match( file.getFileName().toString() ) )
						fileList.add( file );
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFileFailed( Path file, IOException exc ) throws IOException {
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult postVisitDirectory( Path dir, IOException exc ) throws IOException {
					return FileVisitResult.CONTINUE;
				}
			} );
		} catch ( Exception e ) {

		}
		return fileList;
	}

	public static void main( String[] args ) throws IOException {
		Options options = new Options();

		options().forEach( options::addOption );

		List<Path>			files		= new ArrayList<>();
		BoxLangParser		parser		= new BoxLangParser();
		BoxLangTranspiler	transpiler	= new BoxLangTranspiler();
		try {
			CommandLine cmd = new DefaultParser().parse( options, args );
			if ( cmd.hasOption( "input" ) ) {
				String inputFile = cmd.getOptionValue( "input" );
				if ( !Files.exists( Paths.get( inputFile ) ) ) {
					System.out.printf( "File not found: %s ", inputFile );
					System.exit( 0 );
				}
				files.addAll(
				    scanForFiles(
				        inputFile,
				        Set.of( "cfc", "cfm", "cfml" )
				    )
				);
			}
			if ( cmd.hasOption( "output" ) ) {

			}
			for ( Path file : files ) {
				System.out.println( file );
				ParsingResult result = parser.parse( file.toFile() );
				if ( result.isCorrect() ) {
					CompilationUnit javaAST = transpiler.transpile( result.getRoot() );
					System.out.println( javaAST.toString() );
				} else {
					result.getIssues().forEach( error -> System.err.println( error ) );
				}
			}

		} catch ( ParseException e ) {
			System.out.println( e.getMessage() );
			new HelpFormatter().printHelp( "Usage:", options );
			System.exit( 0 );
		}
	}

}
