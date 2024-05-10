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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.Interval;
import org.apache.commons.io.ByteOrderMark;
import org.apache.commons.io.input.BOMInputStream;

import ortus.boxlang.compiler.ast.BoxExpression;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.BoxScript;
import ortus.boxlang.compiler.ast.Issue;
import ortus.boxlang.compiler.ast.Point;
import ortus.boxlang.compiler.ast.Position;
import ortus.boxlang.compiler.ast.Source;

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
	 * Extracts the position from the ANTLR node
	 *
	 * @param node any ANTLR role
	 *
	 * @return a Position representing the region on the source code
	 *
	 * @see Position
	 */
	protected Position getPosition( ParserRuleContext node ) {
		return getPositionStartingAt( node, node );
	}

	/**
	 * Extracts the position from the ANTLR node, using a custom starting point.
	 *
	 * @param node any ANTLR role
	 *
	 * @return a Position representing the region on the source code
	 *
	 * @see Position
	 */
	protected Position getPositionStartingAt( ParserRuleContext node, ParserRuleContext startNode ) {
		return getPosition( startNode, node );
	}

	/**
	 * Extracts the position from the ANTLR node, using a custom starting point.
	 *
	 * @param node any ANTLR role
	 *
	 * @return a Position representing the region on the source code
	 *
	 * @see Position
	 */
	protected Position getPosition( ParserRuleContext startNode, ParserRuleContext endNode ) {
		int	stopLine	= 0;
		int	stopCol		= 0;
		if ( endNode.stop != null ) {
			stopLine	= endNode.stop.getLine() + startLine;
			stopCol		= endNode.stop.getCharPositionInLine() + endNode.stop.getText().length() + startColumn;
		}
		return new Position(
		    new Point( startNode.start.getLine() + this.startLine, startNode.start.getCharPositionInLine() + startColumn ),
		    new Point( stopLine, stopCol ),
		    sourceToParse );
	}

	/**
	 * Extracts the position from the ANTLR node, using a custom starting point.
	 *
	 * @param node any ANTLR role
	 *
	 * @return a Position representing the region on the source code
	 *
	 * @see Position
	 */
	protected Position getPositionStartingAt( ParserRuleContext node, Token startToken ) {
		int	stopLine	= 0;
		int	stopCol		= 0;
		if ( node.stop != null ) {
			stopLine	= node.stop.getLine() + startLine;
			stopCol		= node.stop.getCharPositionInLine() + node.stop.getText().length() + ( node.stop.getLine() > 1 ? 0 : startColumn );
		}
		return new Position(
		    new Point( startToken.getLine() + this.startLine, startToken.getCharPositionInLine() + ( startToken.getLine() > 1 ? 0 : startColumn ) ),
		    new Point( stopLine, stopCol ),
		    sourceToParse );
	}

	/**
	 * Extracts the position from the ANTLR token
	 *
	 * @param token any ANTLR token
	 *
	 * @return a Position representing the region on the source code
	 *
	 * @see Position
	 */
	protected Position getPosition( Token token ) {
		return getPosition( token, token );
	}

	/**
	 * Extracts the position from the ANTLR token
	 *
	 * @param token any ANTLR token
	 *
	 * @return a Position representing the region on the source code
	 *
	 * @see Position
	 */
	protected Position getPosition( Token startToken, Token endToken ) {
		// Adjust the start row and start column by adding the offsets stored in the parser
		int		startRow		= startToken.getLine() + this.startLine;
		int		startCol		= startToken.getCharPositionInLine() + ( startToken.getLine() > 1 ? 0 : startColumn );

		// Get the text of the token
		String	text			= endToken.getText();
		// Count the number of line breaks in the token's text
		int		newLineCount	= text.length() - text.replace( "\n", "" ).length();
		// Calculate the end row by adding the number of line breaks to the start row
		int		endRow			= endToken.getLine() + this.startLine + newLineCount;

		int		endCol;
		if ( newLineCount > 0 ) {
			// If there are line breaks, set the end column to the length of the text after the last line break
			endCol = text.length() - text.lastIndexOf( '\n' ) - 1;
		} else {
			// If there are no line breaks, set the end column to the start column plus the length of the text
			endCol = endToken.getCharPositionInLine() + text.length() + ( endRow > 1 ? 0 : startColumn );
		}

		// Return a new Position object that represents the region of the source code that the token covers
		return new Position(
		    new Point( startRow, startCol ),
		    new Point( endRow, endCol ),
		    sourceToParse
		);
	}

	protected Position createPosition( int startLine, int startColumn, int stopLine, int stopColumn ) {
		return new Position(
		    new Point( startLine, startColumn ),
		    new Point( stopLine, stopColumn ),
		    sourceToParse
		);
	}

	protected Position createOffsetPosition( int startLine, int startColumn, int stopLine, int stopColumn ) {
		return new Position(
		    new Point( this.startLine + startLine, ( startLine == 1 ? this.startColumn : 0 ) + startColumn ),
		    new Point( this.startLine + stopLine, ( stopLine == 1 ? this.startColumn : 0 ) + stopColumn ),
		    sourceToParse
		);
	}

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

}
