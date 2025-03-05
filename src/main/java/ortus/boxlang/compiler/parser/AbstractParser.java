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
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.atn.AmbiguityInfo;
import org.antlr.v4.runtime.atn.DecisionInfo;
import org.antlr.v4.runtime.atn.DecisionState;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.apache.commons.io.ByteOrderMark;
import org.apache.commons.io.input.BOMInputStream;

import ortus.boxlang.compiler.ast.BoxExpression;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.BoxScript;
import ortus.boxlang.compiler.ast.Issue;
import ortus.boxlang.compiler.ast.Point;
import ortus.boxlang.compiler.ast.Position;
import ortus.boxlang.compiler.ast.Source;
import ortus.boxlang.compiler.ast.comment.BoxComment;
import ortus.boxlang.runtime.types.exceptions.BoxIOException;

/**
 * Parser abstract class
 */
public abstract class AbstractParser {

	protected int						startLine;
	protected int						startColumn;
	protected File						file;
	protected String					sourceCode;
	protected Source					sourceToParse;
	public final List<Issue>			issues;
	protected final List<BoxComment>	comments		= new ArrayList<>();

	/**
	 * Flag to indicate if the parser is parsing the outermost source
	 * or just being used to parse a portion of the code. When true, this skips
	 * comment association and final AST visitors, waiting for the entire AST to be
	 * assembled first.
	 */
	protected boolean					subParser		= false;

	/**
	 * Overrides the ANTLR4 default error listener collecting the errors
	 */
	final ErrorListener					errorListener	= new ErrorListener();;

	/**
	 * Constructor, initialize the error list
	 */
	public AbstractParser() {
		this.issues = new ArrayList<>();
		errorListener.setIssues( issues );
	}

	public AbstractParser( int startLine, int startColumn ) {
		this();
		this.startLine		= startLine - 1;
		this.startColumn	= startColumn;
		errorListener.setStartLine( this.startLine );
		errorListener.setStartColumn( this.startColumn );
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
	protected BOMInputStream getInputStream( File file ) {
		try {
			return BOMInputStream.builder().setFile( file ).setByteOrderMarks( ByteOrderMark.UTF_8 ).setInclude( false ).get();
		} catch ( IOException e ) {
			throw new BoxIOException( e );
		}
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
	public abstract ParsingResult parse( File file, boolean isScript ) throws IOException;

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
	public abstract ParsingResult parse( String code, boolean classOrInterface, boolean isScript ) throws IOException;

	/**
	 * Add the parser error listener to the ANTLR parser
	 *
	 * @param lexer  ANTLR lexer instance
	 * @param parser ANTLR parser instance
	 */
	protected void addErrorListeners( Lexer lexer, Parser parser ) {
		// JI: NOte that the lexer will never raise errors after recent upgrades
		lexer.removeErrorListeners();
		lexer.addErrorListener( errorListener );
		parser.removeErrorListeners();
		parser.addErrorListener( errorListener );
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
	protected abstract BoxNode parserFirstStage( InputStream stream, boolean classOrInterface, boolean isScript ) throws IOException;

	/**
	 * Extracts the position from the ANTLR node
	 *
	 * @param node any ANTLR role
	 *
	 * @return a Position representing the region on the source code
	 *
	 * @see Position
	 */
	public Position getPosition( ParserRuleContext node ) {
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
	public Position getPositionStartingAt( ParserRuleContext node, ParserRuleContext startNode ) {
		return getPosition( startNode, node );
	}

	/**
	 * Extracts the position from the ANTLR node, using a custom starting point.
	 *
	 * @param startNode any ANTLR role
	 *
	 * @return a Position representing the region on the source code
	 *
	 * @see Position
	 */
	public Position getPosition( ParserRuleContext startNode, ParserRuleContext endNode ) {
		int	stopLine	= 0;
		int	stopCol		= 0;
		if ( endNode.stop != null ) {
			stopLine	= endNode.stop.getLine() + startLine;
			stopCol		= endNode.stop.getCharPositionInLine() + endNode.stop.getText().length() + startColumn;
		}
		return new Position( new Point( startNode.start.getLine() + this.startLine, startNode.start.getCharPositionInLine() + startColumn ),
		    new Point( stopLine, stopCol ), sourceToParse );
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
	public Position getPositionStartingAt( ParserRuleContext node, Token startToken ) {
		int	stopLine	= 0;
		int	stopCol		= 0;
		if ( node.stop != null ) {
			stopLine	= node.stop.getLine() + startLine;
			stopCol		= node.stop.getCharPositionInLine() + node.stop.getText().length() + ( node.stop.getLine() > 1 ? 0 : startColumn );
		}
		return new Position(
		    new Point( startToken.getLine() + this.startLine, startToken.getCharPositionInLine() + ( startToken.getLine() > 1 ? 0 : startColumn ) ),
		    new Point( stopLine, stopCol ), sourceToParse );
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
	public Position getPosition( Token token ) {
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
	public Position getPosition( ParseTree parseTree ) {
		if ( parseTree instanceof TerminalNode tm ) {
			Token token = tm.getSymbol();
			return getPosition( token, token );
		}
		return getPosition( ( ParserRuleContext ) parseTree );
	}

	/**
	 * Extracts the position from the ANTLR tokens
	 *
	 * @param startToken any ANTLR token, from whence the start is derived
	 * @param endToken   any ANTLR token, from whence the stop is derived
	 *
	 * @return a Position representing the region on the source code
	 *
	 * @see Position
	 */
	public Position getPosition( Token startToken, Token endToken ) {
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
		return new Position( new Point( startRow, startCol ), new Point( endRow, endCol ), sourceToParse );
	}

	public Position createPosition( int startLine, int startColumn, int stopLine, int stopColumn ) {
		return new Position( new Point( startLine, startColumn ), new Point( stopLine, stopColumn ), sourceToParse );
	}

	public Position createOffsetPosition( int startLine, int startColumn, int stopLine, int stopColumn ) {
		return new Position( new Point( this.startLine + startLine, ( startLine == 1 ? this.startColumn : 0 ) + startColumn ),
		    new Point( this.startLine + stopLine, ( stopLine == 1 ? this.startColumn : 0 ) + stopColumn ), sourceToParse );
	}

	/**
	 * Extracts from the ANTLR node
	 *
	 * @param node any ANTLR role
	 *
	 * @return a string containing the source code
	 */
	public String getSourceText( ParserRuleContext node, int startIndex, int stopIndex ) {
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
	public String getSourceText( ParserRuleContext node ) {
		if ( node.getStop() == null ) {
			return "";
		}

		int	startIndex	= node.getStart().getStartIndex();
		int	stopIndex	= node.getStop().getStopIndex();

		// Ensure startIndex and stopIndex are within valid bounds
		if ( startIndex < 0 || stopIndex < 0 || startIndex > stopIndex ) {
			return "";
		}

		CharStream s = node.getStart().getTokenSource().getInputStream();

		// Ensure stopIndex is within the bounds of the input stream
		if ( stopIndex >= s.size() ) {
			stopIndex = s.size() - 1;
		}

		return s.getText( new Interval( startIndex, stopIndex ) );
	}

	/**
	 * Extracts text from a range of nodes
	 *
	 * @param startNode The start node
	 * @param stopNode  The stop node
	 *
	 * @return a string containing the source code
	 */
	public String getSourceText( ParserRuleContext startNode, ParserRuleContext stopNode ) {
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
	public String getSourceText( Token startToken, Token endToken ) {
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
	public String getSourceText( int startIndex, ParserRuleContext nodeStop ) {
		CharStream s = nodeStop.getStart().getTokenSource().getInputStream();
		return s.getText( new Interval( startIndex, nodeStop.getStop().getStopIndex() ) );
	}

	AbstractParser setSource( Source source ) {
		if ( this.sourceToParse != null ) {
			return this;
		}
		this.sourceToParse = source;
		this.errorListener.setSource( this.sourceToParse );
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

	/**
	 * Escape pounds in a string literal
	 *
	 * @param string the string to escape
	 *
	 * @return the escaped string
	 */
	private String escapeStringLiteral( String string ) {
		return string.replace( "##", "#" );
	}

	/**
	 * Escape double up quotes and pounds in a string literal
	 *
	 * @param quoteChar the quote character used to surround the string
	 * @param string    the string to escape
	 *
	 * @return the escaped string
	 */
	public String escapeStringLiteral( String quoteChar, String string ) {
		return string.replace( "##", "#" ).replace( quoteChar + quoteChar, quoteChar );
	}

	/**
	 * Test to see if the given token represents a scope
	 *
	 * @param scope the text to test
	 *
	 * @return true if the text represents a scope
	 */
	public boolean isScope( String scope ) {
		return switch ( scope.toUpperCase() ) {
			case "REQUEST" -> true;
			case "VARIABLES" -> true;
			case "SERVER" -> true;
			default -> false;
		};
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

	public void profileParser( org.antlr.v4.runtime.Parser parser ) {
		PrintStream out = System.out;

		out.printf( "%-35s", "rule" );
		out.printf( "%-15s", "time" );
		out.printf( "%-15s", "invocations" );
		out.printf( "%-15s", "lookahead" );
		out.printf( "%-15s", "lookahead(max)" );
		out.printf( "%-15s%n", "errors" );

		for ( DecisionInfo decisionInfo : parser.getParseInfo().getDecisionInfo() ) {
			DecisionState	ds		= parser.getATN().getDecisionState( decisionInfo.decision );
			String			rule	= parser.getRuleNames()[ ds.ruleIndex ];
			if ( decisionInfo.timeInPrediction > 0 ) {
				out.printf( "%-35s", rule );
				out.printf( "%-15s", decisionInfo.timeInPrediction / 1_000_000D + "ms" );
				out.printf( "%-15s", decisionInfo.invocations );
				out.printf( "%-15s", decisionInfo.SLL_TotalLook );
				out.printf( "%-15s", decisionInfo.SLL_MaxLook );
				out.printf( "%-15s%n", decisionInfo.errors );

				// out.printf( "%-15s", decisionInfo.ambiguities );
				for ( AmbiguityInfo ambiguity : decisionInfo.ambiguities ) {
					out.println();

					out.println( "		**** Ambiguity ****" );
					DecisionState	dsa					= parser.getATN().getDecisionState( ambiguity.decision );
					String			rulea				= parser.getRuleNames()[ dsa.ruleIndex ];
					// out.println( " rule:" + rulea );
					// out.println( " fullCtx:" + ambiguity.fullCtx );

					String			ambiguousSubstring	= ambiguity.input.getText( Interval.of( ambiguity.startIndex, ambiguity.stopIndex ) );
					out.println( "		ambiguous text: [" + ambiguousSubstring + "]" );

					out.println( "		ambigAlts:" + ambiguity.ambigAlts );

					// Iterate over the configurations and print only those that match the ambiguous alternatives
					/*
					 * out.println( "    Configurations:" );
					 * for ( ATNConfig config : ambiguity.configs ) {
					 * if ( ambiguity.ambigAlts.get( config.alt ) ) {
					 * out.println( "        State: " + config.state.stateNumber );
					 * out.println( "        Context: " + Arrays.toString( config.context.toStrings( parser, config.state.stateNumber ) ) );
					 * out.println( "        Alt: " + config.alt );
					 * }
					 * }
					 */
					out.println();
				}
			}
		}
	}

}
