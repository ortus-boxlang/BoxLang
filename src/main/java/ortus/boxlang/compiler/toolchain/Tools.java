package ortus.boxlang.compiler.toolchain;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import ortus.boxlang.compiler.ast.Issue;
import ortus.boxlang.compiler.ast.Point;
import ortus.boxlang.compiler.ast.Position;
import ortus.boxlang.compiler.ast.Source;
import ortus.boxlang.compiler.ast.comment.BoxComment;
import ortus.boxlang.compiler.parser.AbstractParser;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Tools {


	protected int						startLine;
	protected int						startColumn;
	protected File file;
	protected String					sourceCode;
	protected Source					sourceToParse;
	protected final List<Issue>			issues = new ArrayList<>();
	protected final List<BoxComment>	comments		= new ArrayList<>();

	/**
	 * Flag to indicate if the parser is parsing the outermost source
	 * or just being used to parse a portion of the code. When true, this skips
	 * comment assocation and final AST visitors, waiting for the entire AST to be
	 * assembled first.
	 */
	protected boolean					subParser		= false;

	/**
	 * Overrides the ANTLR4 default error listener collecting the errors
	 */
	public final BaseErrorListener errorListener	= new BaseErrorListener() {

		@Override
		public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line,
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
			new Point(startNode.start.getLine() + this.startLine, startNode.start.getCharPositionInLine() + startColumn ),
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
	 * Extracts the position from the ANTLR parse tree.
	 * ParseTree is a super interface, which can either be a
	 * TerminalNode or a ParserRuleContext
	 *
	 * @param parseTree any ANTLR parse tree
	 *
	 * @return a Position representing the region on the source code
	 *
	 * @see Position
	 */
	protected Position getPosition( ParseTree parseTree ) {
		if ( parseTree instanceof TerminalNode tm ) {
			Token token = tm.getSymbol();
			return getPosition( token, token );
		}
		return getPosition( ( ParserRuleContext ) parseTree );
	}

	/**
	 * Extracts the position from the ANTLR token
	 *
	 * @param startToken any ANTLR token
	 * @param endToken any ANTLR token
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
		return s.getText( new Interval(startIndex, stopIndex ) );
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

	public List<BoxComment> getComments() {
		return comments;
	}
}
