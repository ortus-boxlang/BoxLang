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
package ortus.boxlang.compiler.parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Optional;

import ortus.boxlang.compiler.DiskClassUtil;
import ortus.boxlang.compiler.ast.BoxExpression;
import ortus.boxlang.compiler.ast.BoxScript;
import ortus.boxlang.compiler.ast.BoxStatement;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxIOException;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.exceptions.ParseException;

public class Parser {

	private static BoxRuntime runtime = BoxRuntime.getInstance();

	/**
	 * Parse a script file
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

	public ParsingResult parse( File file ) {
		BoxSourceType	fileType	= detectFile( file );
		AbstractParser	parser;
		boolean			isScript	= true;
		switch ( fileType ) {
			case CFSCRIPT -> {
				parser		= new CFParser();
				isScript	= true;
			}
			case CFTEMPLATE -> {
				parser		= new CFParser();
				isScript	= false;
			}
			case BOXSCRIPT -> {
				parser		= new BoxScriptParser();
				isScript	= true;
			}
			case BOXTEMPLATE -> {
				parser		= new BoxTemplateParser();
				isScript	= false;
			}
			default -> {
				throw new RuntimeException( "Unsupported file: " + file.getAbsolutePath() );
			}
		}
		ParsingResult result;
		try {
			result = parser.parse( file, isScript );
		} catch ( IOException e ) {
			throw new BoxIOException( e );
		}

		IStruct data = Struct.of(
		    "file", file,
		    "result", result
		);
		runtime.announce( "onParse", data );
		return ( ParsingResult ) data.get( "result" );
	}

	/**
	 * Parse a script string expression
	 *
	 * @param code source of the expression to parse
	 *
	 * @return a ParsingResult containing the AST with a BoxExpr as root and the list of errors (if any)
	 *
	 * @throws IOException
	 *
	 * @see ParsingResult
	 * @see BoxExpression
	 */
	public ParsingResult parse( String code, BoxSourceType sourceType ) throws IOException {
		return parse( code, sourceType, false );

	}

	/**
	 * Parse a script string expression
	 *
	 * @param code source of the expression to parse
	 *
	 * @return a ParsingResult containing the AST with a BoxExpr as root and the list of errors (if any)
	 *
	 * @throws IOException
	 *
	 * @see ParsingResult
	 * @see BoxExpression
	 */
	public ParsingResult parse( String code, BoxSourceType sourceType, Boolean classOrInterface ) throws IOException {
		AbstractParser	parser;
		boolean			isScript	= true;
		switch ( sourceType ) {
			case CFSCRIPT -> {
				parser		= new CFParser();
				isScript	= true;
			}
			case CFTEMPLATE -> {
				parser		= new CFParser();
				isScript	= false;
			}
			case BOXSCRIPT -> {
				parser		= new BoxScriptParser();
				isScript	= true;
			}
			case BOXTEMPLATE -> {
				parser		= new BoxTemplateParser();
				isScript	= false;
			}
			default -> {
				throw new RuntimeException( "Unsupported language" );
			}
		}
		ParsingResult	result	= parser.parse( code, classOrInterface, isScript );

		IStruct			data	= Struct.of(
		    "code", code,
		    "result", result
		);
		runtime.announce( "onParse", data );
		return ( ParsingResult ) data.get( "result" );

	}

	/**
	 * Parse a script string statement
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
			ParsingResult	result	= new BoxScriptParser().parseExpression( code );

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
		ParsingResult	result	= new BoxScriptParser().parseStatement( code );

		IStruct			data	= Struct.of(
		    "code", code,
		    "result", result
		);
		runtime.announce( "onParse", data );
		return ( ParsingResult ) data.get( "result" );
	}

	/**
	 * Attempt to detect the type of source code based on the contents
	 *
	 * @param file File to check
	 *
	 * @return a BoxFileType
	 *
	 * @see BoxSourceType
	 */
	public static BoxSourceType detectFile( File file ) {
		Optional<String> ext = getFileExtension( file.getAbsolutePath() );
		if ( !ext.isPresent() ) {
			throw new RuntimeException( "No file extension found for path : " + file.getAbsolutePath() );
		}

		switch ( ext.get() ) {
			case "cfs" -> {
				return BoxSourceType.CFSCRIPT;
			}
			case "cfm" -> {
				return BoxSourceType.CFTEMPLATE;
			}
			case "cfml" -> {
				return BoxSourceType.CFTEMPLATE;
			}
			case "cfc" -> {
				if ( new DiskClassUtil( null ).isJavaBytecode( file ) ) {
					return BoxSourceType.CFSCRIPT;
				}
				try {
					return guessClassType( file, StandardCharsets.UTF_8 );
				} catch ( IOException e ) {
					try {
						return guessClassType( file, StandardCharsets.ISO_8859_1 );
					} catch ( IOException e1 ) {
						throw new ParseException( "Could not read file [" + file.toString() + "] to detect syntax type.", e );
					}
				}
			}
			case "bxm" -> {
				return BoxSourceType.BOXTEMPLATE;
			}
			case "bxs" -> {
				return BoxSourceType.BOXSCRIPT;
			}
			case "bx" -> {
				return BoxSourceType.BOXSCRIPT;
			}
			default -> {
				// TODO: For CFCompat. Figure out how to contribute this from the compat module, and decide
				// whether BL proper should even have a behavior like this.
				// This is needed to handle cases where people use alternate file extensions for CFML files like .inc
				return BoxSourceType.CFTEMPLATE;
				// throw new RuntimeException( "Unsupported file: " + file.getAbsolutePath() );
			}
		}

	}

	private static BoxSourceType guessClassType( File file, Charset charset ) throws IOException {

		// This will only read the lines up until it finds a match to avoid loading the entire file
		boolean inComment = false;
		try ( BufferedReader reader = Files.newBufferedReader( file.toPath(), charset ) ) {
			String line;
			while ( ( line = reader.readLine() ) != null ) {
				// Remove any BOMs from the start of the file
				line = line.replaceFirst( "^\uFEFF", "" ).replaceFirst( "^\uFFFE", "" ).replaceFirst( "^\u0000FEFF", "" )
				    .replaceFirst( "^\uFFFE0000", "" ).toLowerCase().trim();
				// Rudimentary attempt to skip comments
				if ( line.startsWith( "//" ) ) {
					continue;
				}
				if ( line.contains( "<!---" ) || line.contains( "/*" ) ) {
					inComment = true;
				}
				if ( line.contains( "--->" ) || line.contains( "*/" ) ) {
					inComment = false;
				}
				if ( inComment ) {
					continue;
				}
				if ( line.startsWith( "component" ) || line.startsWith( "interface" ) ) {
					return BoxSourceType.CFSCRIPT;
				}
				if ( line.startsWith( "abstract" ) && line.contains( "component" ) ) {
					return BoxSourceType.CFSCRIPT;
				}
				if ( line.startsWith( "final" ) && line.contains( "component" ) ) {
					return BoxSourceType.CFSCRIPT;
				}
				if ( line.startsWith( "<cfcomponent" ) || line.startsWith( "<cfinterface" ) || line.startsWith( "<cfscript" ) ) {
					return BoxSourceType.CFTEMPLATE;
				}
			}
		}
		System.out.println( "Could not detect file type for file: " + file.getAbsolutePath() );
		return BoxSourceType.CFSCRIPT;
	}

	public static Optional<String> getFileExtension( String filename ) {
		return Optional.ofNullable( filename )
		    .filter( f -> f.contains( "." ) )
		    .map( f -> f.substring( filename.lastIndexOf( "." ) + 1 ).toLowerCase() );
	}

}
