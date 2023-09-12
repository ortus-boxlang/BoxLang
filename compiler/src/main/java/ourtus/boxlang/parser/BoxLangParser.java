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
package ourtus.boxlang.parser;

import ourtus.boxlang.ast.BoxExpr;
import ourtus.boxlang.ast.BoxScript;
import ourtus.boxlang.ast.BoxStatement;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

public class BoxLangParser {

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
		try {
			List<String> content = Files.readAllLines( file.toPath() );
			if ( content.stream().anyMatch( lines -> lines.contains( "<cfcomponent" ) || lines.contains( "<cfset" ) || lines.contains( "<cfparam" )
			    || lines.contains( "<cfoutput" ) || lines.contains( "<cfinterface" ) ) ) {
				return BoxFileType.CFML;
			}

		} catch ( IOException e ) {
			throw new RuntimeException( e );
		}
		return BoxFileType.CF;

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
			case CF -> {
				return new BoxCFParser().parse( file );
			}
			case CFML -> {
				return new BoxCFMLParser().parse( file );
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
			case CF -> {
				return new BoxCFParser().parse( code );
			}
			case CFML -> {
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
