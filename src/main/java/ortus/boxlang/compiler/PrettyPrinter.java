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

import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.parser.Parser;
import ortus.boxlang.compiler.parser.ParsingResult;
import ortus.boxlang.compiler.prettyprint.PrettyPrint;
import ortus.boxlang.compiler.prettyprint.config.Config;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.logging.BoxLangLogger;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

public class PrettyPrinter {

	private static final BoxLangLogger logger = BoxRuntime.getInstance().getLoggingService().getLogger( PrettyPrinter.class.getSimpleName() );

	public static void main( String[] args ) {
		BoxRuntime runtime = BoxRuntime.getInstance();
		try {
			if ( args.length != 1 ) {
				throw new BoxRuntimeException( "PrettyPrint expects a single source file path." );
			}

			String	source		= args[ 0 ];
			Path	sourcePath	= Paths.get( source ).normalize();

			if ( !sourcePath.isAbsolute() ) {
				sourcePath = Paths.get( "" ).resolve( sourcePath ).normalize().toAbsolutePath().normalize();
			}

			if ( !sourcePath.toFile().exists() ) {
				logger.error( "Source Path does not exist: " + sourcePath.toString() );
				System.exit( 1 );
			}

			prettyPrintFile( sourcePath );

			System.exit( 0 );
		} finally {
			runtime.shutdown();
		}
	}

	private static void prettyPrintFile( Path sourcePath ) {
		long	startTime	= System.currentTimeMillis();

		Config	config		= new Config()
		    .setIndentSize( 4 )
		    .setTabIndent( false )
		    .setMaxLineLength( 100 )
		    .setSingleQuote( true )
		    .setBracketPadding( true )
		    .setParensPadding( true );
		System.out.println( config.toJSON() );
		long			configTime	= System.currentTimeMillis();

		ParsingResult	result		= new Parser().parse( sourcePath.toFile(), false );
		long			parseTime	= System.currentTimeMillis();

		if ( !result.isCorrect() ) {
			logger.error( "Parsing failed for " + sourcePath.toString() );
			result.getIssues().forEach( issue -> logger.error( issue.toString() ) );
			return;
		}

		BoxNode	rootNode	= result.getRoot();
		var		doc			= PrettyPrint.generateDoc( rootNode, config );
		long	docTime		= System.currentTimeMillis();

		var		printed		= PrettyPrint.printDoc( doc, config );
		long	endTime		= System.currentTimeMillis();

		System.out.println( "---------------" );
		System.out.println( printed );
		System.out.println( "---------------" );
		System.out.println( "Config duration: " + ( configTime - startTime ) + " ms" );
		System.out.println( "Parse duration: " + ( parseTime - configTime ) + " ms" );
		System.out.println( "Doc duration: " + ( docTime - parseTime ) + " ms" );
		System.out.println( "Print duration: " + ( endTime - docTime ) + " ms" );
		System.out.println( "Total duration: " + ( endTime - startTime ) + " ms" );

		// write out the JSON AST, JSON Doc and pretty printed output to files alongside the source file
		Path	parentDir		= sourcePath.getParent();
		String	fileName		= sourcePath.getFileName().toString();
		String	baseName		= fileName.substring( 0, fileName.lastIndexOf( '.' ) );
		String	extension		= fileName.substring( fileName.lastIndexOf( '.' ) + 1 );

		Path	astPath			= parentDir.resolve( "ast.json" );
		Path	docPath			= parentDir.resolve( baseName + ".doc.json" );
		Path	docStringPath	= parentDir.resolve( baseName + ".doc.txt" );
		Path	fmtPath			= parentDir.resolve( baseName + ".fmt." + extension );

		try {
			writeFile( astPath, result.getRoot().toJSON() );
			writeFile( docPath, doc.toJSON() );
			writeFile( docStringPath, doc.toString() );
			writeFile( fmtPath, printed );
		} catch ( IOException e ) {
			throw new BoxRuntimeException( "Error writing output files", e );
		}

	}

	private static void writeFile( Path path, String content ) throws IOException {
		Files.write( path, content.getBytes( StandardCharsets.UTF_8 ) );
	}
}