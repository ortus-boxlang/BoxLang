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
package ourtus.boxlang.parser;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.Interval;
import org.apache.commons.io.ByteOrderMark;
import org.apache.commons.io.input.BOMInputStream;
import org.jetbrains.annotations.NotNull;
import ourtus.boxlang.ast.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Parser abstract class
 */
public abstract class BoxAbstractParser {

	protected File					file;
	protected final List<Issue>		issues;

	/**
	 * Overrides the ANTL4 default error listener collecting the errors
	 */
	private final BaseErrorListener	errorListener	= new BaseErrorListener() {

														@Override
														public void syntaxError( Recognizer<?, ?> recognizer, Object offendingSymbol, int line,
														    int charPositionInLine,
														    String msg, RecognitionException e ) {
															String		errorMessage	= msg != null ? msg : "unspecified";
															Position	position		= new Position( new Point( line, charPositionInLine ),
															    new Point( line, charPositionInLine ) );
															if ( file != null ) {
																position.setSource( new SourceFile( file ) );
															}
															issues.add( new Issue( errorMessage, position ) );
														}
													};

	/**
	 * Constructor, initialize the error list
	 */
	public BoxAbstractParser() {
		this.issues = new ArrayList<>();
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
	 * Add the parser error listener to the ANTLR parser
	 * 
	 * @param lexer  ANTLR lexer instance
	 * @param parser ANTLR parser instance
	 */
	protected void addErrorListeners( @NotNull Lexer lexer, @NotNull Parser parser ) {
		lexer.removeErrorListeners();
		lexer.addErrorListener( errorListener );
		parser.removeErrorListeners();
		parser.addErrorListener( errorListener );
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
	 * @return a BoxScript Node
	 * 
	 * @see BoxScript
	 */
	protected abstract BoxScript parseTreeToAst( File file, ParserRuleContext rule ) throws IOException;

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
		return new Position( new Point( node.start.getLine(), node.start.getCharPositionInLine() ),
		    new Point( node.stop.getLine(), node.stop.getCharPositionInLine() ), new SourceFile( file ) );
	}

	/**
	 * Extracts from the ANTLR node
	 * 
	 * @param node any ANTLR role
	 * 
	 * @return a string containing the source code
	 */
	protected String getSourceText( ParserRuleContext node ) {
		CharStream s = node.getStart().getTokenSource().getInputStream();
		return s.getText( new Interval( node.getStart().getStartIndex(), node.getStop().getStopIndex() ) );
	}
}
