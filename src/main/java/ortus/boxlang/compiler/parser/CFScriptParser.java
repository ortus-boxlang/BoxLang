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
import java.util.List;
import java.util.Optional;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BOMInputStream;

import ortus.boxlang.compiler.ast.BoxExpression;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.BoxScript;
import ortus.boxlang.compiler.ast.BoxStatement;
import ortus.boxlang.compiler.ast.BoxTemplate;
import ortus.boxlang.compiler.ast.Point;
import ortus.boxlang.compiler.ast.Position;
import ortus.boxlang.compiler.ast.Source;
import ortus.boxlang.compiler.ast.SourceCode;
import ortus.boxlang.compiler.ast.SourceFile;
import ortus.boxlang.compiler.ast.comment.BoxDocComment;
import ortus.boxlang.compiler.ast.comment.BoxMultiLineComment;
import ortus.boxlang.compiler.ast.comment.BoxSingleLineComment;
import ortus.boxlang.compiler.ast.expression.BoxArrayAccess;
import ortus.boxlang.compiler.ast.expression.BoxArrayLiteral;
import ortus.boxlang.compiler.ast.expression.BoxBooleanLiteral;
import ortus.boxlang.compiler.ast.expression.BoxDecimalLiteral;
import ortus.boxlang.compiler.ast.expression.BoxDotAccess;
import ortus.boxlang.compiler.ast.expression.BoxExpressionInvocation;
import ortus.boxlang.compiler.ast.expression.BoxFunctionInvocation;
import ortus.boxlang.compiler.ast.expression.BoxIdentifier;
import ortus.boxlang.compiler.ast.expression.BoxIntegerLiteral;
import ortus.boxlang.compiler.ast.expression.BoxMethodInvocation;
import ortus.boxlang.compiler.ast.expression.BoxNew;
import ortus.boxlang.compiler.ast.expression.BoxNull;
import ortus.boxlang.compiler.ast.expression.BoxParenthesis;
import ortus.boxlang.compiler.ast.expression.BoxScope;
import ortus.boxlang.compiler.ast.expression.BoxStringLiteral;
import ortus.boxlang.compiler.ast.expression.BoxStructLiteral;
import ortus.boxlang.compiler.ast.visitor.CFTranspilerVisitor;
import ortus.boxlang.compiler.toolchain.CFExpressionVisitor;
import ortus.boxlang.compiler.toolchain.CFVisitor;
import ortus.boxlang.parser.antlr.CFScriptGrammar;
import ortus.boxlang.parser.antlr.CFScriptLexer;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.services.ComponentService;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * Parser for CF scripts
 */
@SuppressWarnings( "DuplicatedCode" )
public class CFScriptParser extends AbstractParser {

	private Token			firstToken			= null;
	public ComponentService	componentService	= BoxRuntime.getInstance().getComponentService();
	private boolean			inOutputBlock		= false;

	/**
	 * Constructor
	 */
	public CFScriptParser() {
		super();
	}

	public CFScriptParser( int startLine, int startColumn ) {
		super( startLine, startColumn );
	}

	public CFScriptParser( int startLine, int startColumn, boolean inOutputBlock ) {
		super( startLine, startColumn );
		this.inOutputBlock = inOutputBlock;
	}

	@SuppressWarnings( "unused" )
	public boolean getInOutputBlock() {
		return inOutputBlock;
	}

	@SuppressWarnings( "unused" )
	public void setInOutputBlock( boolean inOutputBlock ) {
		this.inOutputBlock = inOutputBlock;
	}

	/**
	 * Parse a cf script file
	 *
	 * @param file source file to parse
	 *
	 * @return a ParsingResult containing the AST with a BoxScript as root and the list of errors (if any)
	 *
	 * @throws IOException if the input stream is in error
	 *
	 * @see BoxScript
	 * @see ParsingResult
	 */
	public ParsingResult parse( File file ) throws IOException {
		this.file = file;
		setSource( new SourceFile( file ) );
		BOMInputStream		inputStream			= getInputStream( file );
		Optional<String>	ext					= Parser.getFileExtension( file.getAbsolutePath() );
		Boolean				classOrInterface	= ext.isPresent() && ext.get().equalsIgnoreCase( "cfc" );
		BoxNode				ast					= parserFirstStage( inputStream, classOrInterface );

		return new ParsingResult( ast, issues, comments );
	}

	/**
	 * Parse a cf script string
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
	public ParsingResult parse( String code ) throws IOException {
		return parse( code, false );
	}

	/**
	 * Parse a cf script string
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
	public ParsingResult parse( String code, Boolean classOrInterface ) throws IOException {
		this.sourceCode = code;
		setSource( new SourceCode( code ) );
		InputStream	inputStream	= IOUtils.toInputStream( code, StandardCharsets.UTF_8 );

		BoxNode		ast			= parserFirstStage( inputStream, classOrInterface );
		return new ParsingResult( ast, issues, comments );
	}

	/**
	 * Parse a cf script string expression
	 *
	 * @param code source of the expression to parse
	 *
	 * @return a ParsingResult containing the AST with a BoxExpr as root and the list of errors (if any)
	 *
	 * @throws IOException if the input stream is in error
	 *
	 * @see ParsingResult
	 * @see BoxExpression
	 */
	public ParsingResult parseExpression( String code ) throws IOException {
		setSource( new SourceCode( code ) );
		InputStream			inputStream	= IOUtils.toInputStream( code, StandardCharsets.UTF_8 );

		CFScriptLexerCustom	lexer		= new CFScriptLexerCustom( CharStreams.fromStream( inputStream, StandardCharsets.UTF_8 ) );
		CFScriptGrammar		parser		= new CFScriptGrammar( new CommonTokenStream( lexer ) );
		addErrorListeners( lexer, parser );

		CFScriptGrammar.ExpressionContext parseTree = parser.expression();

		// This must run FIRST before resetting the lexer
		validateParse( lexer );

		// This can add issues to an otherwise successful parse
		extractComments( lexer );

		var expressionVisitor = new CFExpressionVisitor( this, new CFVisitor( this ) );

		try {
			var ast = parseTree.accept( expressionVisitor );
			return new ParsingResult( ast, issues, comments );
		} catch ( Exception e ) {
			// Ignore issues creating AST if the parsing already had failures
			if ( issues.isEmpty() ) {
				throw e;
			}
			return new ParsingResult( null, issues, comments );
		}

	}

	/**
	 * Parse a cf script string statement
	 *
	 * @param code source of the expression to parse
	 *
	 * @return a ParsingResult containing the AST with a BoxStatement as root and the list of errors (if any)
	 *
	 * @throws IOException if the input stream is in error
	 *
	 * @see ParsingResult
	 * @see BoxStatement
	 */
	public ParsingResult parseStatement( String code ) throws IOException {
		setSource( new SourceCode( code ) );
		InputStream			inputStream	= IOUtils.toInputStream( code, StandardCharsets.UTF_8 );

		CFScriptLexerCustom	lexer		= new CFScriptLexerCustom( CharStreams.fromStream( inputStream, StandardCharsets.UTF_8 ) );
		CFScriptGrammar		parser		= new CFScriptGrammar( new CommonTokenStream( lexer ) );
		addErrorListeners( lexer, parser );
		CFScriptGrammar.FunctionOrStatementContext parseTree = parser.functionOrStatement();

		// This must run FIRST before resetting the lexer
		validateParse( lexer );

		// This can add issues to an otherwise successful parse
		extractComments( lexer );

		var visitor = new CFVisitor( this );
		try {
			var ast = parseTree.accept( visitor );

			return new ParsingResult( ast, issues, comments );
		} catch ( Exception e ) {
			// Ignore issues creating AST if the parsing already had failures
			if ( issues.isEmpty() ) {
				throw e;
			}
			return new ParsingResult( null, issues, comments );
		}
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
	protected BoxNode parserFirstStage( InputStream stream, Boolean classOrInterface ) throws IOException {
		CFScriptLexerCustom	lexer	= new CFScriptLexerCustom( CharStreams.fromStream( stream, StandardCharsets.UTF_8 ) );
		CFScriptGrammar		parser	= new CFScriptGrammar( new CommonTokenStream( lexer ) );

		// DEBUG: Will print a trace of all parser rules visited:
		// boxParser.setTrace( true );
		addErrorListeners( lexer, parser );

		CFScriptGrammar.ClassOrInterfaceContext	classOrInterfaceContext	= null;
		CFScriptGrammar.ScriptContext			scriptContext			= null;
		if ( classOrInterface ) {
			classOrInterfaceContext = parser.classOrInterface();
		} else {
			scriptContext = parser.script();
		}

		// This must run FIRST before resetting the lexer
		validateParse( lexer );

		// This can add issues to an otherwise successful parse
		extractComments( lexer );

		lexer.reset();
		firstToken = lexer.nextToken();
		BoxNode	rootNode;

		// Create the visitor we will use to build the AST
		var		visitor	= new CFVisitor( this );

		try {
			if ( classOrInterface ) {
				rootNode = classOrInterfaceContext.accept( visitor );
			} else {
				rootNode = scriptContext.accept( visitor );
			}
		} catch ( Exception e ) {
			// Ignore issues creating AST if the parsing already had failures
			if ( issues.isEmpty() ) {
				throw e;
			}
			return null;
		}

		if ( isSubParser() ) {
			return rootNode;
		}
		if ( rootNode == null ) {
			return null;
		}

		// associate all comments in the source with the appropriate AST nodes
		rootNode.associateComments( this.comments );

		// Transpile CF to BoxLang
		return rootNode.accept( new CFTranspilerVisitor() );
	}

	private void validateParse( CFScriptLexerCustom lexer ) {

		if ( lexer.hasUnpoppedModes() ) {
			List<String> modes = lexer.getUnpoppedModes();

			if ( modes.contains( "hashMode" ) ) {
				Token lastHash = lexer.findPreviousToken( CFScriptLexer.ICHAR );
				errorListener.semanticError( "Unterminated hash expression inside of string literal.", getPosition( lastHash ) );
			} else if ( modes.contains( "quotesMode" ) ) {
				Token lastQuote = lexer.findPreviousToken( CFScriptLexer.OPEN_QUOTE );
				errorListener.semanticError( "Unterminated quote expression.", getPosition( lastQuote ) );
			} else if ( modes.contains( "squotesMode" ) ) {
				Token lastQuote = lexer.findPreviousToken( CFScriptLexer.OPEN_QUOTE );
				errorListener.semanticError( "Unterminated single quote expression.", getPosition( lastQuote ) );
			} else {
				// Catch-all. If this error is encountered, look at what modes were still on the stack, find what token was never ended, and
				// add logic like the above to handle it. Eventually, this catch-all should never be used.
				Position position = new Position( new Point( 0, 0 ), new Point( 0, 0 ), sourceToParse );
				errorListener.semanticError(
				    "Internal error(42): Un-popped Lexer modes. [" + String.join( ", ", modes ) + "] Please report this to the developers.", position );

			}
			// I'm only returning here because we have to reset the lexer above to get the position of the unmatched token, so we no longer have
			// the ability to check for unconsumed tokens.
			return;
		}

		// Check if there are unconsumed tokens
		Token token = lexer._token;
		while ( token.getType() != Token.EOF && ( token.getChannel() == CFScriptLexerCustom.HIDDEN ) ) {
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

			Token unclosedBrace = lexer.findUnclosedToken( CFScriptLexer.LBRACE, CFScriptLexer.RBRACE );
			if ( unclosedBrace != null ) {
				issues.clear();
				errorListener.reset();
				errorListener.semanticError( "Unclosed curly brace [{] on line " + ( unclosedBrace.getLine() + this.startLine ), createOffsetPosition(
				    unclosedBrace.getLine(), unclosedBrace.getCharPositionInLine(), unclosedBrace.getLine(), unclosedBrace.getCharPositionInLine() + 1 ) );
			}

			Token unclosedParen = lexer.findUnclosedToken( CFScriptLexer.LPAREN, CFScriptLexer.RPAREN );
			if ( unclosedParen != null ) {
				issues.clear();
				errorListener.reset();
				errorListener.semanticError( "Unclosed parenthesis [(] on line " + ( unclosedParen.getLine() + this.startLine ), createOffsetPosition(
				    unclosedParen.getLine(), unclosedParen.getCharPositionInLine(), unclosedParen.getLine(), unclosedParen.getCharPositionInLine() + 1 ) );
			}
		}
	}

	private void extractComments( CFScriptLexerCustom lexer ) throws IOException {
		lexer.reset();
		Token		token		= lexer.nextToken();
		DocParser	docParser	= new DocParser( token.getLine(), token.getCharPositionInLine() ).setSource( sourceToParse );
		while ( token.getType() != Token.EOF ) {
			if ( token.getType() == CFScriptLexer.JAVADOC_COMMENT ) {
				ParsingResult result = docParser.parse( null, token.getText() );
				if ( docParser.issues.isEmpty() ) {
					comments.add( ( BoxDocComment ) result.getRoot() );
				} else {
					// Add these issues to the main parser
					issues.addAll( docParser.issues );
				}
			} else if ( token.getType() == CFScriptLexer.LINE_COMMENT ) {
				String commentText = token.getText().trim().substring( 2 ).trim();
				comments.add( new BoxSingleLineComment( commentText, getPosition( token ), token.getText() ) );
			} else if ( token.getType() == CFScriptLexer.COMMENT ) {
				comments.add( new BoxMultiLineComment( extractMultiLineCommentText( token.getText(), false ), getPosition( token ), token.getText() ) );
				// Lucee allows <!--- tag comments ---> in script. Yuck.
			} else if ( token.getType() == CFScriptLexer.TAG_COMMENT_START ) {
				Token			startToken			= token;
				int				commentStartLine	= token.getLine() + this.startLine;
				int				commentStartColumn	= token.getCharPositionInLine() + this.startColumn;
				StringBuilder	tagComment			= new StringBuilder();
				token = lexer.nextToken();
				while ( token.getType() != CFScriptLexer.TAG_COMMENT_END && token.getType() != Token.EOF ) {
					// validate all tokens MUST be TAG_COMMENT_START, or TAG_COMMENT_TEXT
					if ( token.getType() != CFScriptLexer.TAG_COMMENT_START && token.getType() != CFScriptLexer.TAG_COMMENT_TEXT ) {
						errorListener.semanticError( "Invalid tag comment", getPosition( token ) );
						break;
					}
					tagComment.append( token.getText() );
					token = lexer.nextToken();
				}
				int		newLineCount		= tagComment.toString().length() - tagComment.toString().replace( "\n", "" ).length();
				int		commentEndLine		= token.getLine() + this.startLine + newLineCount;
				int		commentEndColumn	= token.getCharPositionInLine() + ( newLineCount > 0 ? this.startColumn : 0 );
				String	finalCommentText	= tagComment.toString();
				// Convert to a proper /* script comment */
				comments.add( new BoxMultiLineComment( finalCommentText.trim(),
				    createPosition( commentStartLine, commentStartColumn, commentEndLine, commentEndColumn ), getSourceText( startToken, token ) ) );
			}
			token = lexer.nextToken();
			docParser.setStartLine( token.getLine() );
			docParser.setStartColumn( token.getCharPositionInLine() );
		}
	}

	@SuppressWarnings( "unused" )
	public List<BoxStatement> parseCFMLStatements( String code, Position position ) {
		try {
			if ( inOutputBlock ) {
				code = "<cfoutput>" + code + "</cfoutput>";
			}
			ParsingResult result = new CFTemplateParser( position.getStart().getLine(), position.getStart().getColumn() ).setSource( sourceToParse )
			    .setSubParser( true ).parse( code );
			this.comments.addAll( result.getComments() );
			if ( result.getIssues().isEmpty() ) {
				BoxNode root = result.getRoot();
				if ( root instanceof BoxTemplate template ) {
					return template.getStatements();
				} else if ( root instanceof BoxStatement statement ) {
					return List.of( statement );
				} else {
					// Could be a BoxClass, which we may actually need to support
					errorListener.semanticError( "Unexpected root node type [" + root.getClass().getName() + "] in component island.", root.getPosition() );
					return null;
				}
			} else {
				// Add these issues to the main parser
				issues.addAll( result.getIssues() );
				return List.of();
			}
		} catch ( IOException e ) {
			throw new BoxRuntimeException( "Error parsing component island: " + code, e );
		}
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
		String escaped = string.replace( "##", "#" );
		return escaped.replace( quoteChar + quoteChar, quoteChar );
	}

	public BoxExpression parseCFExpression( String code, Position position ) {
		try {
			ParsingResult result = new CFScriptParser( position.getStart().getLine(), position.getStart().getColumn() ).setSource( sourceToParse )
			    .setSubParser( true ).parseExpression( code );
			this.comments.addAll( result.getComments() );
			if ( result.getIssues().isEmpty() ) {
				return ( BoxExpression ) result.getRoot();
			} else {
				// Add these issues to the main parser
				issues.addAll( result.getIssues() );
				return new BoxNull( null, null );
			}
		} catch ( IOException e ) {
			errorListener.semanticError( "Error parsing expression " + e.getMessage(), position );
			return new BoxNull( null, null );
		}
	}

	public Token getFirstToken() {
		return firstToken;
	}

	@Override
	CFScriptParser setSource( Source source ) {
		if ( this.sourceToParse != null ) {
			return this;
		}
		this.sourceToParse = source;
		this.errorListener.setSource( sourceToParse );
		return this;
	}

	@Override
	public CFScriptParser setSubParser( boolean subParser ) {
		this.subParser = subParser;
		return this;
	}

	/**
	 * Checks DOT access methods to ensure that nonsensical access methods are rejected at AST build time
	 * and not left to the runtime, when it is not so useful!
	 * <p>
	 * This is necessarily quite an involved check as there are many combinations of left and right
	 * </p>
	 *
	 * @param left  the left side of the dot access left.right
	 * @param right the right side of the dot access left.right
	 */
	public void checkDotAccess( BoxExpression left, BoxExpression right ) {

		// Check the right hand side to see if it is a valid access method
		checkRight( right );

		// Check to see if the left hand side is something that is valid to be accessed via a dot access
		checkLeft( left );

		// Now we know the LHS is valid for access by a dot method and the RHS is a valid access method, so
		// we can check the combinations here if needed.
		// TODO: @Brad - Add more checks here if needed
	}

	/**
	 * Check the right hand side of a dot access to ensure it is a valid access method
	 *
	 * @param right the right side of the dot access left.right
	 */
	private void checkRight( BoxExpression right ) {
		// Check the right hand side is a valid access method and fall through if it is not
		switch ( right ) {
			case BoxFunctionInvocation ignored -> {
			}
			case BoxIdentifier ignored -> {
			}
			case BoxDotAccess ignored -> {
			}
			case BoxIntegerLiteral ignored -> {
			}
			case BoxMethodInvocation ignored -> {
			}
			case BoxNull ignored -> {
			}
			case BoxBooleanLiteral ignored -> {
			}
			case BoxScope ignored -> {
			}
			case BoxExpressionInvocation ignored -> {
			}
			default -> errorListener.semanticError( "dot access via " + right.getDescription() + " is not a valid access method", right.getPosition() );
		}
	}

	/**
	 * Check the left hand side of a dot access to ensure it is a valid construct for dot access
	 *
	 * @param left the left side of the dot access left.right
	 */
	private void checkLeft( BoxExpression left ) {
		// Check the left hand side is a valid construct for dot access and fall through if it is not
		switch ( left ) {
			case BoxFunctionInvocation ignored -> {
			}
			case BoxArrayAccess ignored -> {
			}
			case BoxIdentifier ignored -> {
			}
			case BoxDotAccess ignored -> {
			}
			case BoxStringLiteral ignored -> {
			}
			case BoxBooleanLiteral ignored -> {
			}
			case BoxArrayLiteral ignored -> {
			}
			case BoxScope ignored -> {
			}
			case BoxMethodInvocation ignored -> {
			}
			case BoxStructLiteral ignored -> {
			}
			case BoxNew ignored -> {
			}
			case BoxDecimalLiteral ignored -> {
			}
			case BoxParenthesis ignored -> {
				// TODO: Brad - Should we allow this always, or check what is inside the parenthesis?
			}
			default -> errorListener.semanticError( left.getDescription() + " is not a valid construct for dot access", left.getPosition() );
		}
	}

	/**
	 * Check array access to ensure that nonsensical access methods are rejected at AST build time
	 *
	 * @param ctx    the Parsers ExprArrayAccessContext for source reference etc
	 * @param object the object node that is being accessed as if it were an array
	 * @param access the access node that is being used to access the object
	 */
	public void checkArrayAccess( CFScriptGrammar.ExprArrayAccessContext ctx, BoxExpression object, @SuppressWarnings( "unused" ) BoxExpression access ) {

		switch ( object ) {
			case BoxIdentifier ignored -> {
			}
			case BoxArrayAccess ignored -> {
			}
			case BoxDotAccess ignored -> {
			}
			case BoxStringLiteral ignored -> {
			}
			case BoxArrayLiteral ignored -> {
			}
			case BoxFunctionInvocation ignored -> {
			}
			case BoxNew ignored -> {
			}
			case BoxDecimalLiteral ignored -> {
			}
			case BoxBooleanLiteral ignored -> {
			}
			case BoxNull ignored -> {
			}
			case BoxStructLiteral ignored -> {
			}
			case BoxScope ignored -> {
			}
			case BoxIntegerLiteral ignored -> {
			}
			case BoxMethodInvocation ignored -> {
			}
			case BoxParenthesis ignored -> {
				// TODO: Brad - Should we allow this always, or check what is inside the parenthesis?
			}
			default -> errorListener.semanticError( object.getDescription() + " is not a valid construct for array access ", getPosition( ctx ) );
		}
	}

	public void reportExpressionError( BoxExpression expression ) {
		errorListener.semanticError( "Invalid expression error: " + expression.getSourceText(), expression.getPosition() );
	}

	public void reportStatementError( BoxStatement statement ) {
		errorListener.semanticError( "Invalid statement error: " + statement.getSourceText(), statement.getPosition() );
	}

	public void reportError( String message, Position position ) {
		errorListener.semanticError( message, position );
	}
}
