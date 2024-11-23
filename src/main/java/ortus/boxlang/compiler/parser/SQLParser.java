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
import java.nio.charset.StandardCharsets;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BOMInputStream;

import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.BoxScript;
import ortus.boxlang.compiler.ast.Position;
import ortus.boxlang.compiler.ast.Source;
import ortus.boxlang.compiler.ast.SourceCode;
import ortus.boxlang.compiler.ast.SourceFile;
import ortus.boxlang.compiler.ast.comment.BoxMultiLineComment;
import ortus.boxlang.compiler.ast.comment.BoxSingleLineComment;
import ortus.boxlang.compiler.toolchain.SQLVisitor;
import ortus.boxlang.parser.antlr.SQLGrammar;
import ortus.boxlang.parser.antlr.SQLLexer;

/**
 * Parser for QoQ scripts
 */
public class SQLParser extends AbstractParser {

	private SQLVisitor expressionVisitor = new SQLVisitor( this );

	/**
	 * Constructor
	 */
	public SQLParser() {
		super();
	}

	public SQLParser( int startLine, int startColumn ) {
		super( startLine, startColumn );
	}

	/**
	 * Parse a QoQ script file
	 *
	 * @param file source file to parse
	 *
	 * @return a ParsingResult containing the AST with a BoxScript as root and the list of errors (if any)
	 *
	 * @throws IOException if the input stream is in error
	 *
	 * @see ParsingResult
	 */
	public ParsingResult parse( File file, boolean isScript ) throws IOException {
		this.file = file;
		setSource( new SourceFile( file ) );
		BOMInputStream	inputStream			= getInputStream( file );
		Boolean			classOrInterface	= false;
		BoxNode			ast					= parserFirstStage( inputStream, classOrInterface, isScript );

		return new ParsingResult( ast, issues, comments );
	}

	/**
	 * Parse a QoQ string
	 *
	 * @param code source code to parse
	 *
	 * @return a ParsingResult containing the AST with a BoxScript as root and the list of errors (if any)
	 *
	 * @throws IOException if the input stream is in error
	 *
	 * @see BoxScript
	 * @see ParsingResult
	 */
	public ParsingResult parse( String code, boolean isScript ) throws IOException {
		return parse( code, false, isScript );
	}

	public ParsingResult parse( String code ) throws IOException {
		return parse( code, false, true );
	}

	/**
	 * Parse a QoQ script string
	 *
	 * @param code source code to parse
	 *
	 * @return a ParsingResult containing the AST with a BoxScript as root and the list of errors (if any)
	 *
	 * @throws IOException if the input stream is in error
	 *
	 * @see BoxScript
	 * @see ParsingResult
	 */
	@Override
	public ParsingResult parse( String code, boolean classOrInterface, boolean isScript ) throws IOException {
		this.sourceCode = code;
		setSource( new SourceCode( code ) );
		InputStream	inputStream	= IOUtils.toInputStream( code, StandardCharsets.UTF_8 );

		BoxNode		ast			= parserFirstStage( inputStream, classOrInterface, isScript );
		return new ParsingResult( ast, issues, comments );
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
	@Override
	protected BoxNode parserFirstStage( InputStream stream, boolean classOrInterface, boolean isScript ) throws IOException {

		SQLLexerCustom	lexer	= new SQLLexerCustom( CharStreams.fromStream( stream, StandardCharsets.UTF_8 ), errorListener, this );
		SQLGrammar		parser	= new SQLGrammar( new CommonTokenStream( lexer ) );

		// DEBUG: Will print a trace of all parser rules visited:
		// boxParser.setTrace( true );
		addErrorListeners( lexer, parser );
		parser.setErrorHandler( new BoxParserErrorStrategy() );

		ParserRuleContext parseTree = parser.parse();

		// This must run FIRST before resetting the lexer
		validateParse( lexer );

		// This can add issues to an otherwise successful parse
		extractComments( lexer );

		lexer.reset();
		BoxNode rootNode;

		try {
			// Create the visitor we will use to build the AST
			var visitor = new SQLVisitor( this );
			rootNode = parseTree.accept( visitor );
		} catch ( Exception e ) {
			// Ignore issues creating AST if the parsing already had failures
			if ( issues.isEmpty() ) {
				throw e;
			}
			return null;
		}

		if ( rootNode == null ) {
			return null;
		}

		// associate all comments in the source with the appropriate AST nodes
		rootNode.associateComments( this.comments );

		return rootNode;
	}

	private void validateParse( SQLLexerCustom lexer ) {

		// Check if there are unconsumed tokens
		Token token = lexer._token;
		while ( token.getType() != Token.EOF && ( token.getChannel() == SQLLexerCustom.HIDDEN ) ) {
			token = lexer.nextToken();
		}
		if ( token.getType() != Token.EOF ) {
			StringBuilder	extraText	= new StringBuilder();
			int				startLine	= token.getLine();
			int				startColumn	= token.getCharPositionInLine();
			int				endColumn	= startColumn + token.getText().length();
			Position		position	= createOffsetPosition( startLine, startColumn, startLine, endColumn );
			while ( token.getType() != Token.EOF && extraText.length() < 100 ) {
				extraText.append( token.getText() );
				token = lexer.nextToken();
			}
			errorListener.semanticError( "Extra char(s) [" + extraText + "] at the end of parsing.", position );
		}

		// If there is already a parsing issue, try to get a more specific error
		if ( issues.isEmpty() ) {

			Token unclosedParen = lexer.findUnclosedToken( SQLLexer.OPEN_PAR, SQLLexer.CLOSE_PAR );
			if ( unclosedParen != null ) {
				issues.clear();
				errorListener.reset();
				errorListener.semanticError( "Unclosed parenthesis [(] on line " + ( unclosedParen.getLine() + this.startLine ), createOffsetPosition(
				    unclosedParen.getLine(), unclosedParen.getCharPositionInLine(), unclosedParen.getLine(), unclosedParen.getCharPositionInLine() + 1 ) );
			}
		}
	}

	private void extractComments( SQLLexerCustom lexer ) throws IOException {
		lexer.reset();
		Token token = lexer.nextToken();
		while ( token.getType() != Token.EOF ) {
			if ( token.getType() == SQLLexer.SINGLE_LINE_COMMENT ) {
				String commentText = token.getText().trim().substring( 2 ).trim();
				comments.add( new BoxSingleLineComment( commentText, getPosition( token ), token.getText() ) );
			} else if ( token.getType() == SQLLexer.MULTILINE_COMMENT ) {
				comments.add( new BoxMultiLineComment( extractMultiLineCommentText( token.getText(), false ), getPosition( token ), token.getText() ) );
			}
			token = lexer.nextToken();
		}
	}

	@Override
	SQLParser setSource( Source source ) {
		if ( this.sourceToParse != null ) {
			return this;
		}
		this.sourceToParse = source;
		this.errorListener.setSource( sourceToParse );
		return this;
	}

	@Override
	public SQLParser setSubParser( boolean subParser ) {
		this.subParser = subParser;
		return this;
	}

	public void reportError( String message, Position position ) {
		errorListener.semanticError( message, position );
	}
}
