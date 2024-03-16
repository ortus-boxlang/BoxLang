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
package ortus.boxlang.parser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;

import ortus.boxlang.ast.BoxExpr;
import ortus.boxlang.ast.BoxScript;
import ortus.boxlang.ast.BoxStatement;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

public class BoxParser {

	private static BoxRuntime runtime = BoxRuntime.getInstance();

	/**
	 * Attempt to detect the type of source code based on the contents
	 *
	 * @param file File to check
	 *
	 * @return a BoxFileType
	 *
	 * @see BoxScriptType
	 */
	public static BoxScriptType detectFile( File file ) {
		Optional<String> ext = getFileExtension( file.getAbsolutePath() );
		if ( !ext.isPresent() ) {
			throw new RuntimeException( "No file extension found for path : " + file.getAbsolutePath() );
		}

		switch ( ext.get() ) {
			case "cfs" -> {
				return BoxScriptType.CFSCRIPT;
			}
			case "cfm" -> {
				return BoxScriptType.CFMARKUP;
			}
			case "cfml" -> {
				return BoxScriptType.CFMARKUP;
			}
			case "cfc" -> {
				try {
					List<String> content = Files.readAllLines( file.toPath() );
					// TODO: This approach can be tricked by comments
					if ( content.stream()
					    .anyMatch( lines -> lines.toLowerCase().contains( "<cfcomponent" ) || lines.toLowerCase().contains( "<cfinterface" ) ) ) {
						return BoxScriptType.CFMARKUP;
					}
				} catch ( IOException e ) {
					throw new RuntimeException( e );
				}
				return BoxScriptType.CFSCRIPT;
			}
			case "bxm" -> {
				return BoxScriptType.BOXMARKUP;
			}
			case "bxs" -> {
				return BoxScriptType.BOXSCRIPT;
			}
			case "bx" -> {
				try {
					List<String> content = Files.readAllLines( file.toPath() );
					// TODO: This approach can be tricked by comments
					if ( content.stream()
					    .anyMatch( lines -> lines.toLowerCase().contains( "<bxclass" ) || lines.toLowerCase().contains( "<bxinterface" ) ) ) {
						return BoxScriptType.BOXMARKUP;
					}
				} catch ( IOException e ) {
					throw new RuntimeException( e );
				}
				return BoxScriptType.BOXSCRIPT;
			}
			default -> {
				throw new RuntimeException( "Unsupported file: " + file.getAbsolutePath() );
			}
		}

	}

	public static Optional<String> getFileExtension( String filename ) {
		return Optional.ofNullable( filename )
		    .filter( f -> f.contains( "." ) )
		    .map( f -> f.substring( filename.lastIndexOf( "." ) + 1 ).toLowerCase() );
	}

	/**
	 * Parse a cf script file
	 *
	 * @param file source file to parse
	 *
	 * @return a ParsingResult containing the AST with a BoxScript as root and the list of errors (if any)
	 *
	 * @throws IOException
	 *
	 * @see BoxScript
	 * @see ParsingResult
	 */

	public ParsingResult parse( File file ) throws IOException {
		BoxScriptType		fileType	= detectFile( file );
		BoxAbstractParser	parser;
		switch ( fileType ) {
			case CFSCRIPT -> {
				parser = new BoxCFParser();
			}
			case CFMARKUP -> {
				parser = new BoxCFMLParser();
			}
			case BOXSCRIPT -> {
				parser = new BoxCFParser();
				// TODO: Swith after splitting parsers
				// parser = new BoxLangScriptParser();
			}
			case BOXMARKUP -> {
				// TODO: Swith after splitting parsers
				parser = new BoxCFMLParser();
			}
			default -> {
				throw new RuntimeException( "Unsupported file: " + file.getAbsolutePath() );
			}
		}
		ParsingResult	result	= parser.parse( file );

		IStruct			data	= Struct.of(
		    "file", file,
		    "result", result
		);
		runtime.announce( "onParse", data );
		return ( ParsingResult ) data.get( "result" );
	}

	/**
	 * Parse a cf script string expression
	 *
	 * @param code source of the expression to parse
	 *
	 * @return a ParsingResult containing the AST with a BoxExpr as root and the list of errors (if any)
	 *
	 * @throws IOException
	 *
	 * @see ParsingResult
	 * @see BoxExpr
	 */
	public ParsingResult parse( String code, BoxScriptType fileType ) throws IOException {
		BoxAbstractParser parser;
		switch ( fileType ) {
			case CFSCRIPT -> {
				parser = new BoxCFParser();
			}
			case CFMARKUP -> {
				parser = new BoxCFMLParser();
			}
			case BOXSCRIPT -> {
				parser = new BoxCFParser();
			}
			case BOXMARKUP -> {
				parser = new BoxCFMLParser();
			}
			default -> {
				throw new RuntimeException( "Unsupported language" );
			}
		}
		ParsingResult	result	= parser.parse( code );

		IStruct			data	= Struct.of(
		    "code", code,
		    "result", result
		);
		runtime.announce( "onParse", data );
		return ( ParsingResult ) data.get( "result" );

	}

	/**
	 * Parse a cf script string statement
	 *
	 * @param code source of the expression to parse
	 *
	 * @return a ParsingResult containing the AST with a BoxStatement as root and the list of errors (if any)
	 *
	 * 
	 * @see ParsingResult
	 * @see BoxStatement
	 */
	public ParsingResult parseExpression( String code ) {
		try {
			ParsingResult	result	= new BoxCFParser().parseExpression( code );

			IStruct			data	= Struct.of(
			    "code", code,
			    "result", result
			);
			runtime.announce( "onParse", data );
			return ( ParsingResult ) data.get( "result" );
		} catch ( IOException e ) {
			throw new BoxRuntimeException( "Error parsing expression", e );
		}
	}

	public ParsingResult parseStatement( String code ) throws IOException {
		ParsingResult	result	= new BoxCFParser().parseStatement( code );

		IStruct			data	= Struct.of(
		    "code", code,
		    "result", result
		);
		runtime.announce( "onParse", data );
		return ( ParsingResult ) data.get( "result" );
	}

}
