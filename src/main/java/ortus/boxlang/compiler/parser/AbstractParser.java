/**
 * [BoxLang]
 * <p>
 * Copyright [2023] [Ortus Solutions, Corp]
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package ortus.boxlang.compiler.parser;

import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.Interval;
import org.apache.commons.io.ByteOrderMark;
import org.apache.commons.io.input.BOMInputStream;
import ortus.boxlang.compiler.ast.*;
import ortus.boxlang.compiler.ast.comment.BoxComment;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Parser abstract class
 */
public abstract class AbstractParser {

	protected int						startLine;
	protected int						startColumn;
	protected File						file;
	protected String					sourceCode;
	protected Source					sourceToParse;
	protected final List<Issue>			issues;
	protected final List<BoxComment>	comments		= new ArrayList<>();

	/**
	 * Flag to indicate if the parser is parsing the outermost source
	 * or just being used to parse a portion of the code. When true, this skips
	 * comment assocation and final AST visitors, waiting for the entire AST to be
	 * assembled first.
	 */
	protected boolean					subParser		= false;

	/**
	 * Overrides the ANTL4 default error listener collecting the errors
	 */
	protected final BaseErrorListener	errorListener	= new BaseErrorListener() {

															@Override
															public void syntaxError( Recognizer<?, ?> recognizer, Object offendingSymbol, int line,
															    int charPositionInLine,
															    String msg, RecognitionException e ) {
																String		errorMessage	= msg != null ? msg : "unspecified";
																Position	position		= new Position(
																    new Point( line + startLine, charPositionInLine + startColumn ),
																    new Point( line + startLine, charPositionInLine + startColumn ),
																    sourceToParse
																);
																issues.add( new Issue( errorMessage, position ) );
															}
														};

	/**
	 * Constructor, initialize the error list
	 */
	public AbstractParser() {
		this.issues = new ArrayList<>();
	}

	public AbstractParser( int startLine, int startColumn ) {
		this();
		this.startLine		= startLine - 1;
		this.startColumn	= startColumn;
	}

	/**
	 * Convert the input file in a UTF-8 Input stream with BOM
	 *
	 * @param file input file
	 *
	 * @return a BOMInputStream
	 *
	 * @throws IOException
	 */
	protected BOMInputStream getInputStream( File file ) throws IOException {
		return BOMInputStream.builder().setFile( file ).setByteOrderMarks( ByteOrderMark.UTF_8 ).setInclude( false ).get();

	}

	/**
	 * Parse a file
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
	public abstract ParsingResult parse( File file ) throws IOException;

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
	 * @see BoxExpression
	 */
	public abstract ParsingResult parse( String code, Boolean classOrInterface ) throws IOException;

	/**
	 * Add the parser error listener to the ANTLR parser
	 *
	 * @param lexer  ANTLR lexer instance
	 * @param parser ANTLR parser instance
	 */
	protected void addErrorListeners( Lexer lexer, Parser parser ) {
		lexer.removeErrorListeners();
		lexer.addErrorListener( errorListener );
		parser.removeErrorListeners();
		parser.addErrorListener( errorListener );
		// parser.setErrorHandler( new ParserErrorStrategy() );
	}

	/**
	 * Fist stage parser
	 *
	 * @param stream           input stream (file or string) of the source code
	 * @param classOrInterface true if the code is a class or interface as opposed to just a list of statements
	 *
	 * @return the ANTLR ParserRule representing the parse tree of the code
	 *
	 * @throws IOException io error
	 */
	protected abstract BoxNode parserFirstStage( InputStream stream, Boolean classOrInterface ) throws IOException;

	/**
	 * Extracts from the ANTLR node
	 *
	 * @param node any ANTLR role
	 *
	 * @return a string containing the source code
	 */
	protected String getSourceText( ParserRuleContext node, int startIndex, int stopIndex ) {
		CharStream s = node.getStart().getTokenSource().getInputStream();
		return s.getText( new Interval( startIndex, stopIndex ) );
	}

	/**
	 * Extracts from the ANTLR node
	 *
	 * @param node any ANTLR role
	 *
	 * @return a string containing the source code
	 */
	protected String getSourceText( ParserRuleContext node ) {
		if ( node.getStop() == null ) {
			return "";
		}
		CharStream s = node.getStart().getTokenSource().getInputStream();
		return s.getText( new Interval( node.getStart().getStartIndex(), node.getStop().getStopIndex() ) );
	}

	/**
	 * Extracts text from a range of nodes
	 *
	 * @param startNode The start node
	 *
	 * @param stopNode  The stop node
	 *
	 * @return a string containing the source code
	 */
	protected String getSourceText( ParserRuleContext startNode, ParserRuleContext stopNode ) {
		if ( stopNode.getStop() == null ) {
			return "";
		}
		CharStream s = startNode.getStart().getTokenSource().getInputStream();
		return s.getText( new Interval( startNode.getStart().getStartIndex(), stopNode.getStop().getStopIndex() ) );
	}

	/**
	 * Extracts from the ANTLR node
	 *
	 * @param startToken The start token
	 * @param endToken   The end token
	 *
	 * @return a string containing the source code
	 */
	protected String getSourceText( Token startToken, Token endToken ) {
		CharStream s = startToken.getTokenSource().getInputStream();
		return s.getText( new Interval( startToken.getStartIndex(), endToken.getStopIndex() ) );
	}

	/**
	 * Extracts from the ANTLR node where one node is the start, and another node is the end
	 *
	 * @param startIndex The start index
	 * @param nodeStop   The stop node
	 *
	 * @return a string containing the source code
	 */
	protected String getSourceText( int startIndex, ParserRuleContext nodeStop ) {
		CharStream s = nodeStop.getStart().getTokenSource().getInputStream();
		return s.getText( new Interval( startIndex, nodeStop.getStop().getStopIndex() ) );
	}

	AbstractParser setSource( Source source ) {
		if ( this.sourceToParse != null ) {
			return this;
		}
		this.sourceToParse = source;
		return this;
	}

	public String extractMultiLineCommentText( String rawText, Boolean doc ) {
		rawText	= rawText.trim();
		rawText	= rawText.substring( ( doc ? 3 : 2 ), rawText.length() - 2 );
		String[]		lines	= rawText.split( "\\r?\\n", -1 );
		StringBuilder	sb		= new StringBuilder();
		for ( int i = 0; i < lines.length; i++ ) {
			String line = lines[ i ];
			// Remove leading * unless the first two chars are **
			line = line.trim();
			if ( i != 0 && i != lines.length - 1 && line.startsWith( "*" ) && !line.startsWith( "**" ) ) {
				line = line.substring( 1 );
			}
			sb.append( line.trim() );
			if ( i < lines.length - 1 ) {
				sb.append( "\n" );
			}
		}
		return sb.toString();
	}

	public AbstractParser setSubParser( boolean subParser ) {
		this.subParser = subParser;
		return this;
	}

	public boolean isSubParser() {
		return subParser;
	}

	public List<BoxComment> getComments() {
		return comments;
	}

}
