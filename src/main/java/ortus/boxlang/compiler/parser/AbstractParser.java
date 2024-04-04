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
import org.antlr.v4.runtime.misc.Interval;
import org.apache.commons.io.ByteOrderMark;
import org.apache.commons.io.input.BOMInputStream;

import ortus.boxlang.compiler.ast.BoxExpression;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.BoxScript;
import ortus.boxlang.compiler.ast.Issue;
import ortus.boxlang.compiler.ast.Point;
import ortus.boxlang.compiler.ast.Position;
import ortus.boxlang.compiler.ast.SourceFile;

/**
 * Parser abstract class
 */
public abstract class AbstractParser {

	protected int						startLine;
	protected int						startColumn;
	protected File						file;
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
																    new Point( line + startLine, charPositionInLine + startColumn ) );
																if ( file != null ) {
																	position.setSource( new SourceFile( file ) );
																}
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
		this.file = file;
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
	public abstract ParsingResult parse( String code ) throws IOException;

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
	 * @param stream input stream (file or string) of the source code
	 *
	 * @return the ANTLR ParserRule representing the parse tree of the code
	 *
	 * @throws IOException io error
	 */
	protected abstract ParserRuleContext parserFirstStage( InputStream stream ) throws IOException;

	/**
	 * Second stage parser, performs the transformation from ANTLR parse tree
	 * to the AST
	 *
	 * @param file source file, if any
	 * @param rule ANTLR parser rule to transform
	 *
	 * @return a BoxNode
	 *
	 * @see BoxNode
	 */
	protected abstract BoxNode parseTreeToAst( File file, ParserRuleContext rule ) throws IOException;

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
		int	stopLine	= 0;
		int	stopCol		= 0;
		if ( node.stop != null ) {
			stopLine	= node.stop.getLine() + startLine;
			stopCol		= node.stop.getCharPositionInLine() + startColumn;
		}
		return new Position( new Point( node.start.getLine() + this.startLine, node.start.getCharPositionInLine() + startColumn ),
		    new Point( stopLine, stopCol ), new SourceFile( file ) );
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

}
