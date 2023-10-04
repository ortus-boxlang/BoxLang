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

public class BoxParser {

	/**
	 * Attempt to detect the type of source code based on the contents
	 *
	 * @param file File to check
	 *
	 * @return a BoxFileType
	 *
	 * @see BoxFileType
	 */
	public static BoxFileType detectFile( File file ) {
		Optional<String> ext = getFileExtension( file.getAbsolutePath() );
		if ( !ext.isPresent() ) {
			throw new RuntimeException( "No file extension found for path : " + file.getAbsolutePath() );
		}

		switch ( ext.get() ) {
			case "cfs" -> {
				return BoxFileType.CFSCRIPT;
			}
			case "cfm" -> {
				return BoxFileType.CFMARKUP;
			}
			case "cfml" -> {
				return BoxFileType.CFMARKUP;
			}
			case "cfc" -> {
				try {
					List<String> content = Files.readAllLines( file.toPath() );
					// TODO: This approach can be tricked by comments
					if ( content.stream()
					    .anyMatch( lines -> lines.toLowerCase().contains( "<cfcomponent" ) || lines.toLowerCase().contains( "<cfinterface" ) ) ) {
						return BoxFileType.CFMARKUP;
					}
				} catch ( IOException e ) {
					throw new RuntimeException( e );
				}
				return BoxFileType.CFSCRIPT;
			}
			case "bxm" -> {
				return BoxFileType.BOXMARKUP;
			}
			case "bxs" -> {
				return BoxFileType.BOXSCRIPT;
			}
			case "bx" -> {
				try {
					List<String> content = Files.readAllLines( file.toPath() );
					// TODO: This approach can be tricked by comments
					if ( content.stream()
					    .anyMatch( lines -> lines.toLowerCase().contains( "<bxclass" ) || lines.toLowerCase().contains( "<bxinterface" ) ) ) {
						return BoxFileType.BOXMARKUP;
					}
				} catch ( IOException e ) {
					throw new RuntimeException( e );
				}
				return BoxFileType.BOXSCRIPT;
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
		BoxFileType fileType = detectFile( file );
		switch ( fileType ) {
			case CFSCRIPT -> {
				return new BoxCFParser().parse( file );
			}
			case CFMARKUP -> {
				return new BoxCFMLParser().parse( file );
			}
			case BOXSCRIPT -> {
				return new BoxLangScriptParser().parse( file );
			}
			default -> {
				throw new RuntimeException( "Unsupported file: " + file.getAbsolutePath() );
			}
		}

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
	public ParsingResult parse( String code, BoxFileType fileType ) throws IOException {
		switch ( fileType ) {
			case CFSCRIPT -> {
				return new BoxCFParser().parse( code );
			}
			case CFMARKUP -> {
				return new BoxCFMLParser().parse( code );
			}
			default -> {
				throw new RuntimeException( "Unsupported language" );
			}
		}

	}

	/**
	 * Parse a cf script string statement
	 *
	 * @param code source of the expression to parse
	 *
	 * @return a ParsingResult containing the AST with a BoxStatement as root and the list of errors (if any)
	 *
	 * @throws IOException
	 *
	 * @see ParsingResult
	 * @see BoxStatement
	 */
	public ParsingResult parseExpression( String code ) throws IOException {
		return new BoxCFParser().parseExpression( code );
	}

	public ParsingResult parseStatement( String code ) throws IOException {
		return new BoxCFParser().parseStatement( code );
	}

}
