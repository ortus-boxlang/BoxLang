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
package ortus.boxlang.transpiler;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.Statement;

import ortus.boxlang.parser.BoxParser;
import ortus.boxlang.parser.ParsingResult;

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
				        .desc( "output directory" ).build()

				);
				// Classpath
				add(
				    Option.builder( "cp" ).longOpt( "classpath" )
				        .argName( "classpath" )
				        .hasArg()
				        .required( true )
				        .desc( "file or directory" ).build()
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

		List<Path>	files	= new ArrayList<>();
		BoxParser	parser	= new BoxParser();

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
				        Set.of( "cfs", "cfc", "cfm", "cfml" )
				    )
				);
			}

			for ( Path file : files ) {
				System.out.println( file );
				ParsingResult result = parser.parse( file.toFile() );

				if ( result.isCorrect() ) {

					String	output		= cmd.getOptionValue( "output" );
					String	classpath	= cmd.getOptionValue( "classpath" );
					String	fqn			= "";
					try {

						Transpiler transpiler = Transpiler.getTranspiler( null /* Config ? */ );
						transpiler.setProperty( "baseclass", "BoxScript" );
						transpiler.setProperty( "returnType", "void" );
						TranspiledCode code = transpiler.transpile( result.getRoot() );

						fqn = transpiler.compileJava( code.getEntryPoint(), output, List.of( classpath ) );

						for ( CompilationUnit javaAST : code.getCallables() ) {
							fqn = transpiler.compileJava( javaAST, output, List.of( classpath ) );
						}

						transpiler.run( fqn, List.of( classpath, output ) );
					} catch ( Throwable e ) {
						e.printStackTrace();
						for ( StackTraceElement s : Arrays.stream( e.getStackTrace() ).toList() ) {
							if ( fqn.equalsIgnoreCase( s.getClassName() ) ) {
								// BoxNode node = transpiler.resloveReference( s.getLineNumber() );
								// if ( node != null ) {
								// String uri = node.getPosition().getSource().toString();
								// int line = node.getPosition().getStart().getLine();
								// System.err.println( uri + ":" + line + " " + node.getSourceText() );
								// }
							}
						}

					}
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

	private static void showLastStatement( CompilationUnit javaClass ) {
		MethodDeclaration	method	= javaClass.findCompilationUnit().orElseThrow()
		    .getType( 0 )
		    .getMethodsByName( "_invoke" ).get( 0 );

		NodeList<Statement>	body	= method.getBody().get().getStatements();
		Optional<Statement>	last	= body.getLast();

		System.out.println( "I am the last: " + last.get() );
		for ( Statement stmt : body ) {
			if ( body.indexOf( stmt ) == body.size() - 1 ) {
				System.out.println( "I am the last: " + stmt );
			}
		}

	}

}
