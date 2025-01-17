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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BOMInputStream;

import ortus.boxlang.compiler.ast.BoxClass;
import ortus.boxlang.compiler.ast.BoxExpression;
import ortus.boxlang.compiler.ast.BoxInterface;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.BoxScript;
import ortus.boxlang.compiler.ast.BoxStatement;
import ortus.boxlang.compiler.ast.BoxTemplate;
import ortus.boxlang.compiler.ast.Issue;
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
import ortus.boxlang.compiler.ast.expression.BoxClosure;
import ortus.boxlang.compiler.ast.expression.BoxDecimalLiteral;
import ortus.boxlang.compiler.ast.expression.BoxDotAccess;
import ortus.boxlang.compiler.ast.expression.BoxExpressionInvocation;
import ortus.boxlang.compiler.ast.expression.BoxFQN;
import ortus.boxlang.compiler.ast.expression.BoxFunctionInvocation;
import ortus.boxlang.compiler.ast.expression.BoxIdentifier;
import ortus.boxlang.compiler.ast.expression.BoxIntegerLiteral;
import ortus.boxlang.compiler.ast.expression.BoxMethodInvocation;
import ortus.boxlang.compiler.ast.expression.BoxNew;
import ortus.boxlang.compiler.ast.expression.BoxNull;
import ortus.boxlang.compiler.ast.expression.BoxParenthesis;
import ortus.boxlang.compiler.ast.expression.BoxScope;
import ortus.boxlang.compiler.ast.expression.BoxStaticAccess;
import ortus.boxlang.compiler.ast.expression.BoxStaticMethodInvocation;
import ortus.boxlang.compiler.ast.expression.BoxStringInterpolation;
import ortus.boxlang.compiler.ast.expression.BoxStringLiteral;
import ortus.boxlang.compiler.ast.expression.BoxStructLiteral;
import ortus.boxlang.compiler.ast.statement.BoxAccessModifier;
import ortus.boxlang.compiler.ast.statement.BoxAnnotation;
import ortus.boxlang.compiler.ast.statement.BoxArgumentDeclaration;
import ortus.boxlang.compiler.ast.statement.BoxBreak;
import ortus.boxlang.compiler.ast.statement.BoxBufferOutput;
import ortus.boxlang.compiler.ast.statement.BoxContinue;
import ortus.boxlang.compiler.ast.statement.BoxDocumentationAnnotation;
import ortus.boxlang.compiler.ast.statement.BoxExpressionStatement;
import ortus.boxlang.compiler.ast.statement.BoxFunctionDeclaration;
import ortus.boxlang.compiler.ast.statement.BoxIfElse;
import ortus.boxlang.compiler.ast.statement.BoxImport;
import ortus.boxlang.compiler.ast.statement.BoxMethodDeclarationModifier;
import ortus.boxlang.compiler.ast.statement.BoxProperty;
import ortus.boxlang.compiler.ast.statement.BoxRethrow;
import ortus.boxlang.compiler.ast.statement.BoxReturn;
import ortus.boxlang.compiler.ast.statement.BoxReturnType;
import ortus.boxlang.compiler.ast.statement.BoxScriptIsland;
import ortus.boxlang.compiler.ast.statement.BoxStatementBlock;
import ortus.boxlang.compiler.ast.statement.BoxSwitch;
import ortus.boxlang.compiler.ast.statement.BoxSwitchCase;
import ortus.boxlang.compiler.ast.statement.BoxTry;
import ortus.boxlang.compiler.ast.statement.BoxTryCatch;
import ortus.boxlang.compiler.ast.statement.BoxType;
import ortus.boxlang.compiler.ast.statement.BoxWhile;
import ortus.boxlang.compiler.ast.statement.component.BoxComponent;
import ortus.boxlang.compiler.ast.visitor.CFTranspilerVisitor;
import ortus.boxlang.compiler.toolchain.CFExpressionVisitor;
import ortus.boxlang.compiler.toolchain.CFVisitor;
import ortus.boxlang.parser.antlr.CFGrammar;
import ortus.boxlang.parser.antlr.CFGrammar.TemplateContext;
import ortus.boxlang.parser.antlr.CFGrammar.Template_argumentContext;
import ortus.boxlang.parser.antlr.CFGrammar.Template_attributeContext;
import ortus.boxlang.parser.antlr.CFGrammar.Template_attributeValueContext;
import ortus.boxlang.parser.antlr.CFGrammar.Template_boxImportContext;
import ortus.boxlang.parser.antlr.CFGrammar.Template_breakContext;
import ortus.boxlang.parser.antlr.CFGrammar.Template_caseContext;
import ortus.boxlang.parser.antlr.CFGrammar.Template_catchBlockContext;
import ortus.boxlang.parser.antlr.CFGrammar.Template_classOrInterfaceContext;
import ortus.boxlang.parser.antlr.CFGrammar.Template_componentContext;
import ortus.boxlang.parser.antlr.CFGrammar.Template_continueContext;
import ortus.boxlang.parser.antlr.CFGrammar.Template_functionContext;
import ortus.boxlang.parser.antlr.CFGrammar.Template_genericOpenCloseComponentContext;
import ortus.boxlang.parser.antlr.CFGrammar.Template_genericOpenComponentContext;
import ortus.boxlang.parser.antlr.CFGrammar.Template_includeContext;
import ortus.boxlang.parser.antlr.CFGrammar.Template_interfaceContext;
import ortus.boxlang.parser.antlr.CFGrammar.Template_outputContext;
import ortus.boxlang.parser.antlr.CFGrammar.Template_propertyContext;
import ortus.boxlang.parser.antlr.CFGrammar.Template_rethrowContext;
import ortus.boxlang.parser.antlr.CFGrammar.Template_returnContext;
import ortus.boxlang.parser.antlr.CFGrammar.Template_scriptContext;
import ortus.boxlang.parser.antlr.CFGrammar.Template_setContext;
import ortus.boxlang.parser.antlr.CFGrammar.Template_statementContext;
import ortus.boxlang.parser.antlr.CFGrammar.Template_statementsContext;
import ortus.boxlang.parser.antlr.CFGrammar.Template_switchContext;
import ortus.boxlang.parser.antlr.CFGrammar.Template_textContentContext;
import ortus.boxlang.parser.antlr.CFGrammar.Template_throwContext;
import ortus.boxlang.parser.antlr.CFGrammar.Template_tryContext;
import ortus.boxlang.parser.antlr.CFGrammar.Template_whileContext;
import ortus.boxlang.parser.antlr.CFLexer;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.components.ComponentDescriptor;
import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.services.ComponentService;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * Parser for CF scripts
 */
@SuppressWarnings( "DuplicatedCode" )
public class CFParser extends AbstractParser {

	private Token				firstToken			= null;
	private boolean				inOutputBlock		= false;
	private int					outputCounter		= 0;
	public ComponentService		componentService	= BoxRuntime.getInstance().getComponentService();
	private CFExpressionVisitor	expressionVisitor	= new CFExpressionVisitor( this, new CFVisitor( this ) );
	private boolean				classOrInterface	= false;

	/**
	 * Constructor
	 */
	public CFParser() {
		super();
	}

	public CFParser( int startLine, int startColumn ) {
		super( startLine, startColumn );
	}

	public CFParser( int startLine, int startColumn, boolean inOutputBlock ) {
		super( startLine, startColumn );
		this.inOutputBlock = inOutputBlock;
	}

	public boolean getInOutputBlock() {
		return inOutputBlock;
	}

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
	public ParsingResult parse( File file, boolean isScript ) throws IOException {
		this.file = file;
		setSource( new SourceFile( file ) );
		BOMInputStream		inputStream			= getInputStream( file );
		Optional<String>	ext					= Parser.getFileExtension( file.getAbsolutePath() );
		Boolean				classOrInterface	= ext.isPresent() && ext.get().equalsIgnoreCase( "cfc" );
		BoxNode				ast					= parserFirstStage( inputStream, classOrInterface, isScript );

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
	public ParsingResult parse( String code, boolean isScript ) throws IOException {
		return parse( code, false, isScript );
	}

	public ParsingResult parse( String code ) throws IOException {
		return parse( code, false, true );
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
	@Override
	public ParsingResult parse( String code, boolean classOrInterface, boolean isScript ) throws IOException {
		this.classOrInterface	= classOrInterface;
		this.sourceCode			= code;
		setSource( new SourceCode( code ) );
		InputStream	inputStream	= IOUtils.toInputStream( code, StandardCharsets.UTF_8 );

		BoxNode		ast			= parserFirstStage( inputStream, classOrInterface, isScript );
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
		InputStream		inputStream	= IOUtils.toInputStream( code, StandardCharsets.UTF_8 );

		CFLexerCustom	lexer		= new CFLexerCustom( CharStreams.fromStream( inputStream, StandardCharsets.UTF_8 ),
		    CFLexerCustom.DEFAULT_SCRIPT_MODE, errorListener, this );
		CFGrammar		parser		= new CFGrammar( new CommonTokenStream( lexer ) );
		addErrorListeners( lexer, parser );
		parser.setErrorHandler( new BoxParserErrorStrategy() );

		CFGrammar.ExpressionContext parseTree = parser.expression();

		// This must run FIRST before resetting the lexer
		validateParse( lexer );

		// This can add issues to an otherwise successful parse
		extractComments( lexer );

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
		InputStream		inputStream	= IOUtils.toInputStream( code, StandardCharsets.UTF_8 );

		CFLexerCustom	lexer		= new CFLexerCustom( CharStreams.fromStream( inputStream, StandardCharsets.UTF_8 ),
		    CFLexerCustom.DEFAULT_SCRIPT_MODE, errorListener, this );
		CFGrammar		parser		= new CFGrammar( new CommonTokenStream( lexer ) );
		addErrorListeners( lexer, parser );
		parser.setErrorHandler( new BoxParserErrorStrategy() );
		CFGrammar.FunctionOrStatementContext parseTree = parser.functionOrStatement();

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
	protected BoxNode parserFirstStage( InputStream stream, boolean classOrInterface, boolean isScript ) throws IOException {
		this.classOrInterface = classOrInterface;
		CFLexerCustom	lexer	= new CFLexerCustom( CharStreams.fromStream( stream, StandardCharsets.UTF_8 ),
		    isScript ? CFLexerCustom.DEFAULT_SCRIPT_MODE : CFLexerCustom.DEFAULT_TEMPLATE_MODE, errorListener, this );
		CFGrammar		parser	= new CFGrammar( new CommonTokenStream( lexer ) );

		// DEBUG: Will print a trace of all parser rules visited:
		// boxParser.setTrace( true );
		addErrorListeners( lexer, parser );
		parser.setErrorHandler( new BoxParserErrorStrategy() );

		ParserRuleContext parseTree = null;
		if ( classOrInterface ) {
			if ( isScript ) {
				parseTree = parser.classOrInterface();
			} else {
				parseTree = parser.template_classOrInterface();
			}
		} else {
			if ( isScript ) {
				parseTree = parser.script();
			} else {
				parseTree = parser.template();
			}
		}

		// This must run FIRST before resetting the lexer
		validateParse( lexer );

		// This can add issues to an otherwise successful parse
		extractComments( lexer );

		lexer.reset();
		firstToken = lexer.nextToken();
		BoxNode rootNode;

		try {

			if ( isScript ) {
				// Create the visitor we will use to build the AST
				var visitor = new CFVisitor( this );
				rootNode = parseTree.accept( visitor );
			} else {
				if ( classOrInterface ) {
					rootNode = toAst( null, ( CFGrammar.Template_classOrInterfaceContext ) parseTree );
				} else {
					rootNode = toAst( null, ( CFGrammar.TemplateContext ) parseTree );
				}
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

	private void validateParse( CFLexerCustom lexer ) {

		if ( lexer.hasUnpoppedModes() ) {
			List<String>	modes		= lexer.getUnpoppedModes();

			Position		position	= createOffsetPosition( lexer._token.getLine(),
			    lexer._token.getCharPositionInLine() + lexer._token.getText().length() - 1, lexer._token.getLine(),
			    lexer._token.getCharPositionInLine() + lexer._token.getText().length() - 1 );

			// Check for specific unpopped modes that we can throw a specific error for
			if ( lexer.hasMode( CFLexerCustom.TEMPLATE_EXPRESSION_MODE_COMPONENT ) ) {
				String	message		= "Unclosed expression inside an opening tag";
				Token	startToken	= lexer.findPreviousToken( CFLexerCustom.COMPONENT_OPEN );
				if ( startToken == null ) {
					startToken = lexer.findPreviousToken( CFLexerCustom.OUTPUT_START );
				}
				if ( startToken != null ) {
					position = createOffsetPosition( startToken.getLine(), startToken.getCharPositionInLine(), startToken.getLine(),
					    startToken.getCharPositionInLine() + startToken.getText().length() );
				}
				message += " on line " + position.getStart().getLine();
				errorListener.semanticError( message, position );
			} else if ( lexer.hasMode( CFLexerCustom.TEMPLATE_OUTPUT_MODE ) ) {
				String	message				= "Unclosed output tag";
				Token	outputStartToken	= lexer.findPreviousToken( CFLexerCustom.OUTPUT_START );
				if ( outputStartToken != null ) {
					position = createOffsetPosition( outputStartToken.getLine(), outputStartToken.getCharPositionInLine(), outputStartToken.getLine(),
					    outputStartToken.getCharPositionInLine() + outputStartToken.getText().length() );
				}
				message += " on line " + position.getStart().getLine();
				errorListener.semanticError( message, position );
			} else if ( lexer.hasMode( CFLexerCustom.TEMPLATE_COMMENT_MODE ) ) {
				String	message				= "Unclosed tag comment";
				Token	outputStartToken	= lexer.findPreviousToken( CFLexerCustom.COMMENT_START );
				if ( outputStartToken != null ) {
					position = createOffsetPosition( outputStartToken.getLine(), outputStartToken.getCharPositionInLine(), outputStartToken.getLine(),
					    outputStartToken.getCharPositionInLine() + outputStartToken.getText().length() );
				}
				message += " on line " + position.getStart().getLine();
				errorListener.semanticError( message, position );
			} else if ( lexer.hasMode( CFLexerCustom.TEMPLATE_COMPONENT_MODE ) ) {
				String	message		= "Unclosed tag";
				Token	startToken	= lexer.findPreviousToken( CFLexerCustom.PREFIX );
				if ( startToken == null ) {
					startToken = lexer.findPreviousToken( CFLexerCustom.SLASH_PREFIX );
				}
				if ( startToken != null ) {
					position = createOffsetPosition( startToken.getLine(), startToken.getCharPositionInLine(), startToken.getLine(),
					    startToken.getCharPositionInLine() + startToken.getText().length() );
					List<Token> nameTokens = lexer.findPreviousTokenAndXSiblings( startToken.getType(), 1 );
					if ( !nameTokens.isEmpty() ) {
						message += " [";
						for ( var t : nameTokens ) {
							message += t.getText();
						}
						message += "]";
					}
				}
				message += " starting on line " + position.getStart().getLine();
				errorListener.semanticError( message, position );
			} else if ( modes.contains( "hashMode" ) ) {
				Token lastHash = lexer.findPreviousToken( CFLexer.ICHAR );
				errorListener.semanticError( "Unterminated hash expression inside of string literal.", getPosition( lastHash ) );
			} else if ( modes.contains( "quotesMode" ) ) {
				Token lastQuote = lexer.findPreviousToken( CFLexer.OPEN_QUOTE );
				errorListener.semanticError( "Unterminated quote expression.", getPosition( lastQuote ) );
			} else if ( modes.contains( "squotesMode" ) ) {
				Token lastQuote = lexer.findPreviousToken( CFLexer.OPEN_QUOTE );
				errorListener.semanticError( "Unterminated single quote expression.", getPosition( lastQuote ) );
			} else {
				// Catch-all. If this error is encountered, look at what modes were still on the stack, find what token was never ended, and
				// add logic like the above to handle it. Eventually, this catch-all should never be used.
				position = new Position( new Point( 0, 0 ), new Point( 0, 0 ), sourceToParse );
				errorListener.semanticError(
				    "Internal error(42): Un-popped Lexer modes. [" + String.join( ", ", modes ) + "] Please report this to the developers.", position );

			}
			// I'm only returning here because we have to reset the lexer above to get the position of the unmatched token, so we no longer have
			// the ability to check for unconsumed tokens.
			return;
		} else {

			// Check if there are unconsumed tokens
			Token token = lexer._token;
			while ( token.getType() != Token.EOF && ( token.getChannel() == CFLexerCustom.HIDDEN ) ) {
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
		}

		// If there is already a parsing issue, try to get a more specific error
		if ( issues.isEmpty() ) {

			Token unclosedBrace = lexer.findUnclosedToken( CFLexer.LBRACE, CFLexer.RBRACE );
			if ( unclosedBrace != null ) {
				issues.clear();
				errorListener.reset();
				errorListener.semanticError( "Unclosed curly brace [{] on line " + ( unclosedBrace.getLine() + this.startLine ), createOffsetPosition(
				    unclosedBrace.getLine(), unclosedBrace.getCharPositionInLine(), unclosedBrace.getLine(), unclosedBrace.getCharPositionInLine() + 1 ) );
			}

			Token unclosedParen = lexer.findUnclosedToken( CFLexer.LPAREN, CFLexer.RPAREN );
			if ( unclosedParen != null ) {
				issues.clear();
				errorListener.reset();
				errorListener.semanticError( "Unclosed parenthesis [(] on line " + ( unclosedParen.getLine() + this.startLine ), createOffsetPosition(
				    unclosedParen.getLine(), unclosedParen.getCharPositionInLine(), unclosedParen.getLine(), unclosedParen.getCharPositionInLine() + 1 ) );
			}
		}
	}

	private void extractComments( CFLexerCustom lexer ) throws IOException {
		lexer.reset();
		Token		token		= lexer.nextToken();
		DocParser	docParser	= new DocParser( token.getLine(), token.getCharPositionInLine() ).setSource( sourceToParse );

		while ( token.getType() != Token.EOF ) {

			// uncomment this to see all tokens as they were sent from out custom lexer
			// System.out.println( token.toString() + " " + CFLexer.VOCABULARY.getSymbolicName( token.getType() ) );

			if ( token.getType() == CFLexer.JAVADOC_COMMENT ) {
				ParsingResult result = docParser.parse( null, token.getText() );
				if ( docParser.issues.isEmpty() ) {
					comments.add( ( BoxDocComment ) result.getRoot() );
				} else {
					// Add these issues to the main parser
					issues.addAll( docParser.issues );
				}
			} else if ( token.getType() == CFLexer.LINE_COMMENT ) {
				String commentText = token.getText().trim().substring( 2 ).trim();
				comments.add( new BoxSingleLineComment( commentText, getPosition( token ), token.getText() ) );
			} else if ( token.getType() == CFLexer.COMMENT ) {
				comments.add( new BoxMultiLineComment( extractMultiLineCommentText( token.getText(), false ), getPosition( token ), token.getText() ) );
				// Lucee allows <!--- tag comments ---> in script. Yuck.
			} else if ( token.getType() == CFLexer.TAG_COMMENT_START ) {
				Token			startToken			= token;
				int				commentStartLine	= token.getLine() + this.startLine;
				int				commentStartColumn	= token.getCharPositionInLine() + this.startColumn;
				StringBuilder	tagComment			= new StringBuilder();
				token = lexer.nextToken();
				while ( token.getType() != CFLexer.TAG_COMMENT_END && token.getType() != Token.EOF ) {
					// validate all tokens MUST be TAG_COMMENT_START, or TAG_COMMENT_TEXT
					if ( token.getType() != CFLexer.TAG_COMMENT_START && token.getType() != CFLexer.TAG_COMMENT_TEXT ) {
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
			} else if ( token.getType() == CFLexer.COMMENT_START ) {
				Token			startToken	= token;
				StringBuffer	tagComment	= new StringBuffer();
				token = lexer.nextToken();
				while ( token.getType() != CFLexer.COMMENT_END && token.getType() != Token.EOF ) {
					// validate all tokens MUST be COMMENT_START, or COMMENT_TEXT
					if ( token.getType() != CFLexer.COMMENT_START && token.getType() != CFLexer.COMMENT_TEXT ) {
						issues.add( new Issue( "Invalid tag comment", getPosition( token ) ) );
						break;
					}
					tagComment.append( token.getText() );
					token = lexer.nextToken();
				}
				String finalCommentText = tagComment.toString();
				// Convert to a proper /* script comment */
				comments.add(
				    new BoxMultiLineComment(
				        finalCommentText.trim(),
				        getPosition( startToken, token ),
				        getSourceText( startToken, token )
				    )
				);
			}
			token = lexer.nextToken();
			docParser.setStartLine( token.getLine() );
			docParser.setStartColumn( token.getCharPositionInLine() );
		}
	}

	private BoxNode toAst( File file, Template_classOrInterfaceContext classOrInterface ) {
		if ( classOrInterface.template_component() != null ) {
			return toAst( file, classOrInterface.template_component() );
		} else if ( classOrInterface.template_interface() != null ) {
			return toAst( file, classOrInterface.template_interface() );
		} else if ( classOrInterface.template_script() != null ) {
			if ( classOrInterface.template_script().classOrInterface() != null ) {
				return expressionVisitor.getStatementVisitor().visit( classOrInterface.template_script().classOrInterface() );
			} else {
				errorListener.semanticError(
				    "Script block in your CFC must contain a component or interface. (( " + classOrInterface.template_script().script().getText() + "))",
				    getPosition( classOrInterface ) );
				return null;
			}
		} else {
			issues.add( new Issue( "Unexpected classOrInterface type", getPosition( classOrInterface ) ) );
			return null;
		}
	}

	private BoxNode toAst( File file, Template_interfaceContext interface_ ) {
		List<BoxStatement>					body			= new ArrayList<>();
		List<BoxAnnotation>					annotations		= new ArrayList<>();
		List<BoxAnnotation>					postAnnotations	= new ArrayList<>();
		List<BoxDocumentationAnnotation>	documentation	= new ArrayList<>();
		List<BoxImport>						imports			= new ArrayList<>();

		interface_.template_boxImport().forEach( stmt -> {
			imports.add( toAst( file, stmt ) );
		} );

		for ( var attr : interface_.template_attribute() ) {
			annotations.add( toAst( file, attr ) );
		}
		interface_.template_function().forEach( stmt -> {
			BoxFunctionDeclaration funDec = toAst( file, stmt );
			// I don't think tags have a "default" modifier really and there's no docs on this.
			// So, if the body of the funciton is "empty", make it an abstract interface function
			if ( allStatementsAreWhitespace( funDec.getBody() ) ) {
				funDec.setBody( null );
			}
			body.add( funDec );
		} );

		return new BoxInterface( imports, body, annotations, postAnnotations, documentation, getPosition( interface_ ), getSourceText( interface_ ) );
	}

	private BoxTemplate toAst( File file, TemplateContext rule ) throws IOException {
		List<BoxStatement> statements = new ArrayList<>();
		if ( rule.template_statements() != null ) {
			statements = toAst( file, rule.template_statements() );
		}
		return new BoxTemplate( statements, getPosition( rule ), getSourceText( rule ) );
	}

	private BoxNode toAst( File file, Template_componentContext node ) {
		List<BoxImport>						imports			= new ArrayList<>();
		List<BoxStatement>					body			= new ArrayList<>();
		List<BoxAnnotation>					annotations		= new ArrayList<>();
		// This will be empty in components
		List<BoxDocumentationAnnotation>	documentation	= new ArrayList<>();
		List<BoxProperty>					properties		= new ArrayList<>();

		if ( node.template_boxImport() != null ) {
			imports.addAll( toAst( file, node.template_boxImport() ) );
		}
		for ( var attr : node.template_attribute() ) {
			annotations.add( toAst( file, attr ) );
		}

		if ( node.template_statements() != null ) {
			body.addAll( toAst( file, node.template_statements() ) );
		}
		// loop over body and move any BoxImport statements to the imports list
		for ( int i = body.size() - 1; i >= 0; i-- ) {
			BoxStatement statement = body.get( i );
			if ( statement instanceof BoxImport boxImport ) {
				imports.add( boxImport );
				body.remove( i );
			}
		}
		for ( CFGrammar.Template_propertyContext annotation : node.template_property() ) {
			properties.add( toAst( file, annotation ) );
		}

		return new BoxClass( imports, body, annotations, documentation, properties, getPosition( node ), getSourceText( node ) );
	}

	private BoxProperty toAst( File file, Template_propertyContext node ) {
		List<BoxAnnotation>					annotations		= new ArrayList<>();
		// This will be empty in components
		List<BoxDocumentationAnnotation>	documentation	= new ArrayList<>();

		for ( var attr : node.template_attribute() ) {
			annotations.add( toAst( file, attr ) );
		}

		return new BoxProperty( new ArrayList<BoxAnnotation>(), annotations, documentation, getPosition( node ), getSourceText( node ) );
	}

	private List<BoxImport> toAst( File file, List<Template_boxImportContext> imports ) {
		List<BoxImport> boxImports = new ArrayList<>();
		for ( var boxImport : imports ) {
			boxImports.add( toAst( file, boxImport ) );
		}
		return boxImports;
	}

	private BoxImport toAst( File file, Template_boxImportContext node ) {
		String				name		= null;
		String				prefix		= null;
		String				module		= null;
		BoxIdentifier		alias		= null;
		List<BoxAnnotation>	annotations	= new ArrayList<>();

		for ( var attr : node.template_attribute() ) {
			annotations.add( toAst( file, attr ) );
		}
		BoxFQN			nameFQN		= null;
		BoxExpression	nameSearch	= findExprInAnnotations( annotations, "name", false, null, "import", getPosition( node ) );
		if ( nameSearch != null ) {
			name	= getBoxExprAsString( nameSearch, "name", false );
			prefix	= getBoxExprAsString( findExprInAnnotations( annotations, "prefix", false, null, null, null ), "prefix", false );
			if ( prefix != null ) {
				name = prefix + ":" + name;
			}
			nameFQN = new BoxFQN( name, nameSearch.getPosition(), nameSearch.getSourceText() );
		}
		module = getBoxExprAsString( findExprInAnnotations( annotations, "module", false, null, null, null ), "module", false );

		BoxExpression aliasSearch = findExprInAnnotations( annotations, "alias", false, null, null, null );
		if ( aliasSearch != null ) {
			alias = new BoxIdentifier( getBoxExprAsString( aliasSearch, "alias", false ),
			    aliasSearch.getPosition(),
			    aliasSearch.getSourceText() );
		}

		return new BoxImport( nameFQN, alias, getPosition( node ), getSourceText( node ) );
	}

	public List<BoxStatement> toAst( File file, Template_statementsContext node ) {
		return statementsToAst( file, node );
	}

	private List<BoxStatement> statementsToAst( File file, ParserRuleContext node ) {
		List<BoxStatement> statements = new ArrayList<>();
		if ( node.children != null ) {
			for ( var child : node.children ) {
				if ( child instanceof Template_statementContext statement ) {
					if ( statement.template_genericCloseComponent() != null ) {
						String				componentName	= statement.template_genericCloseComponent().template_componentName().getText();
						// see if statements list has a BoxComponent with this name
						int					size			= statements.size();
						boolean				foundStart		= false;
						int					removeAfter		= -1;
						List<BoxStatement>	bodyStatements	= new ArrayList<>();
						// loop backwards checking for a BoxComponent with this name
						for ( int i = size - 1; i >= 0; i-- ) {
							BoxStatement boxStatement = statements.get( i );
							if ( boxStatement instanceof BoxComponent boxComponent ) {
								if ( boxComponent.getName().equalsIgnoreCase( componentName ) && boxComponent.getBody() == null ) {
									foundStart = true;
									// slice all statements from this position to the end and set them as the body of the start component
									boxComponent.setBody( new ArrayList<>( statements.subList( i + 1, size ) ) );
									bodyStatements = boxComponent.getBody();
									boxComponent.getPosition().setEnd( getPosition( statement.template_genericCloseComponent() ).getEnd() );
									boxComponent
									    .setSourceText( getSourceText( boxComponent.getSourceStartIndex(), statement.template_genericCloseComponent() ) );
									removeAfter = i;
									break;
								} else if ( boxComponent.getBody() == null && boxComponent.getRequiresBody() ) {
									issues.add( new Issue( "Component [" + boxComponent.getName() + "] requires a body.", boxComponent.getPosition() ) );
								}
							}
						}
						// remove all items in list after removeAfter index
						if ( removeAfter >= 0 ) {
							statements.subList( removeAfter + 1, size ).clear();
						}
						if ( !foundStart ) {
							issues.add( new Issue( "Found end component [" + componentName + "] without matching start component",
							    getPosition( statement.template_genericCloseComponent() ) ) );
						}

						ComponentDescriptor descriptor = componentService.getComponent( componentName );
						if ( descriptor != null ) {
							if ( !descriptor.allowsBody() && ( !allStatementsAreWhitespace( bodyStatements ) ) ) {
								issues.add( new Issue( "The [" + componentName + "] component does not allow a body", getPosition( node ) ) );
							}
						}
					} else {
						statements.add( toAst( file, statement ) );
					}
				} else if ( child instanceof Template_textContentContext textContent ) {
					statements.addAll( toAst( file, textContent ) );
				} else if ( child instanceof Template_scriptContext script ) {
					if ( script.script() != null ) {
						BoxScript scriptNode = ( BoxScript ) expressionVisitor.getStatementVisitor().visit( script.script() );
						statements.add(
						    new BoxScriptIsland(
						        scriptNode.getStatements(),
						        getPosition( script.script() ),
						        getSourceText( script.script() )
						    )
						);
					} else if ( script.classOrInterface() != null ) {
						errorListener.semanticError( "Class or Interface definitions are not allowed in script blocks", getPosition( script ) );
					}
				} else if ( child instanceof Template_boxImportContext importContext ) {
					statements.add( toAst( file, importContext ) );
				}
			}
		}
		// Loop over statements and look for any BoxComponets who require a body but it's null
		for ( BoxStatement statement : statements ) {
			if ( statement instanceof BoxComponent boxComponent ) {
				if ( boxComponent.getBody() == null && boxComponent.getRequiresBody() ) {
					issues.add( new Issue( "Component [" + boxComponent.getName() + "] requires a body.", boxComponent.getPosition() ) );
				}
			}
		}
		return statements;
	}

	private boolean allStatementsAreWhitespace( List<BoxStatement> bodyStatements ) {
		for ( BoxStatement statement : bodyStatements ) {
			if ( statement instanceof BoxBufferOutput bffr ) {
				if ( bffr.getExpression() instanceof BoxStringLiteral str && !str.getValue().isBlank() ) {
					return false;
				}
			} else {
				return false;
			}
		}
		return true;
	}

	private BoxStatement toAst( File file, Template_statementContext node ) {
		if ( node.template_output() != null ) {
			return toAst( file, node.template_output() );
		} else if ( node.template_set() != null ) {
			return toAst( file, node.template_set() );
		} else if ( node.template_if() != null ) {
			return toAst( file, node.template_if() );
		} else if ( node.template_try() != null ) {
			return toAst( file, node.template_try() );
		} else if ( node.template_function() != null ) {
			return toAst( file, node.template_function() );
		} else if ( node.template_return() != null ) {
			return toAst( file, node.template_return() );
		} else if ( node.template_while() != null ) {
			return toAst( file, node.template_while() );
		} else if ( node.template_break() != null ) {
			return toAst( file, node.template_break() );
		} else if ( node.template_continue() != null ) {
			return toAst( file, node.template_continue() );
		} else if ( node.template_include() != null ) {
			return toAst( file, node.template_include() );
		} else if ( node.template_rethrow() != null ) {
			return toAst( file, node.template_rethrow() );
		} else if ( node.template_throw() != null ) {
			return toAst( file, node.template_throw() );
		} else if ( node.template_switch() != null ) {
			return toAst( file, node.template_switch() );
		} else if ( node.template_genericOpenCloseComponent() != null ) {
			return toAst( file, node.template_genericOpenCloseComponent() );
		} else if ( node.template_genericOpenComponent() != null ) {
			return toAst( file, node.template_genericOpenComponent() );
		} else if ( node.template_boxImport() != null ) {
			return toAst( file, node.template_boxImport() );
		}
		issues.add( new Issue( "Statement node parsing not implemented yet", getPosition( node ) ) );
		return null;

	}

	private BoxStatement toAst( File file, Template_genericOpenCloseComponentContext node ) {
		List<BoxAnnotation> attributes = new ArrayList<>();
		for ( var attr : node.template_attribute() ) {
			attributes.add( toAst( file, attr ) );
		}
		return new BoxComponent( node.template_componentName().getText(), attributes, List.of(), node.getStart().getStartIndex(), getPosition( node ),
		    getSourceText( node ) );
	}

	private BoxStatement toAst( File file, Template_genericOpenComponentContext node ) {
		List<BoxAnnotation> attributes = new ArrayList<>();
		for ( var attr : node.template_attribute() ) {
			attributes.add( toAst( file, attr ) );
		}
		String name = node.template_componentName().getText();

		// Special check for cfloop condition to avoid runtime eval
		if ( name.equalsIgnoreCase( "loop" ) ) {
			for ( var attr : attributes ) {
				if ( attr.getKey().getValue().equalsIgnoreCase( "condition" ) ) {
					BoxExpression condition = attr.getValue();
					// parse as CF script expression and update value
					// In reality, we could just re-parse the source text for all expression types, but there's really no need unless it was a string or interpolated string.
					if ( condition instanceof BoxStringLiteral str ) {
						condition = parseCFExpression( str.getValue(), condition.getPosition() );
					} else if ( condition instanceof BoxStringInterpolation stri ) {
						// Strip "" from around the string
						condition = parseCFExpression( stri.getSourceText().substring( 1, stri.getSourceText().length() - 1 ), condition.getPosition() );
					}
					BoxExpression newCondition = new BoxClosure(
					    List.of(),
					    List.of(),
					    new BoxReturn( condition, null, null ),
					    null,
					    null );
					attr.setValue( newCondition );
				}
			}
		}

		// Body may get set later, if we find an end component
		var					comp		= new BoxComponent( name, attributes, null, node.getStart().getStartIndex(), getPosition( node ),
		    getSourceText( node ) );

		ComponentDescriptor	descriptor	= componentService.getComponent( name );
		if ( descriptor != null && descriptor.requiresBody() ) {
			comp.setRequiresBody( true );
		}

		return comp;

	}

	private BoxStatement toAst( File file, Template_switchContext node ) {
		BoxExpression		expression;
		List<BoxAnnotation>	annotations	= new ArrayList<>();
		List<BoxSwitchCase>	cases		= new ArrayList<>();

		for ( var attr : node.template_attribute() ) {
			annotations.add( toAst( file, attr ) );
		}

		expression = findExprInAnnotations( annotations, "expression", true, null, "switch", getPosition( node ) );

		if ( node.template_switchBody() != null && node.template_switchBody().children != null ) {
			for ( var c : node.template_switchBody().children ) {
				if ( c instanceof CFGrammar.Template_caseContext caseNode ) {
					cases.add( toAst( file, caseNode ) );
					// We're willing to overlook text, but not other CF components
				} else if ( ! ( c instanceof CFGrammar.Template_textContentContext ) ) {
					issues.add( new Issue( "Switch body can only contain case statements - ", getPosition( ( ParserRuleContext ) c ) ) );
				}
			}
		}
		return new BoxSwitch( expression, cases, getPosition( node ), getSourceText( node ) );
	}

	private BoxSwitchCase toAst( File file, Template_caseContext node ) {
		BoxExpression	value		= null;
		BoxExpression	delimiter	= null;

		// Only check for these on case nodes, not default case
		if ( !node.TEMPLATE_CASE().isEmpty() ) {
			List<BoxAnnotation> annotations = new ArrayList<>();

			for ( var attr : node.template_attribute() ) {
				annotations.add( toAst( file, attr ) );
			}

			value		= findExprInAnnotations( annotations, "value", true, null, "case", getPosition( node ) );
			delimiter	= findExprInAnnotations( annotations, "delimiter", false, new BoxStringLiteral( ",", null, null ), "case", getPosition( node ) );
		}

		List<BoxStatement> statements = new ArrayList<>();
		if ( node.template_statements() != null ) {
			statements.addAll( toAst( file, node.template_statements() ) );
		}

		// In component mode, the break is implied
		statements.add( new BoxBreak( null, null ) );

		return new BoxSwitchCase( value, delimiter, statements, getPosition( node ), getSourceText( node ) );
	}

	private BoxStatement toAst( File file, Template_throwContext node ) {
		BoxExpression		object			= null;
		BoxExpression		type			= null;
		BoxExpression		message			= null;
		BoxExpression		detail			= null;
		BoxExpression		errorcode		= null;
		BoxExpression		extendedinfo	= null;

		List<BoxAnnotation>	annotations		= new ArrayList<>();

		for ( var attr : node.template_attribute() ) {
			annotations.add( toAst( file, attr ) );
		}

		// Using generic component so attributeCollection can work
		return new BoxComponent(
		    "throw",
		    annotations,
		    getPosition( node ),
		    getSourceText( node )
		);
	}

	private BoxStatement toAst( File file, Template_rethrowContext node ) {
		return new BoxRethrow( getPosition( node ), getSourceText( node ) );
	}

	private BoxStatement toAst( File file, Template_includeContext node ) {
		List<BoxAnnotation> annotations = new ArrayList<>();

		for ( var attr : node.template_attribute() ) {
			annotations.add( toAst( file, attr ) );
		}

		return new BoxComponent(
		    "include",
		    annotations,
		    getPosition( node ),
		    getSourceText( node )
		);
	}

	private BoxStatement toAst( File file, Template_continueContext node ) {
		String label = null;
		if ( node.label != null ) {
			label = node.label.getText();
		}
		return new BoxContinue( label, getPosition( node ), getSourceText( node ) );
	}

	private BoxStatement toAst( File file, Template_breakContext node ) {
		String label = null;
		if ( node.label != null ) {
			label = node.label.getText();
		}
		return new BoxBreak( label, getPosition( node ), getSourceText( node ) );
	}

	private BoxStatement toAst( File file, Template_whileContext node ) {
		BoxExpression		condition;
		List<BoxStatement>	bodyStatements	= new ArrayList<>();
		List<BoxAnnotation>	annotations		= new ArrayList<>();

		for ( var attr : node.template_attribute() ) {
			annotations.add( toAst( file, attr ) );
		}

		BoxExpression conditionSearch = findExprInAnnotations( annotations, "condition", true, null, "while", getPosition( node ) );
		condition = parseCFExpression(
		    getBoxExprAsString(
		        conditionSearch,
		        "condition",
		        false
		    ),
		    conditionSearch.getPosition()
		);

		if ( node.template_statements() != null ) {
			bodyStatements.addAll( toAst( file, node.template_statements() ) );
		}
		BoxStatement	body		= new BoxStatementBlock( bodyStatements, getPosition( node.template_statements() ),
		    getSourceText( node.template_statements() ) );
		BoxExpression	labelSearch	= findExprInAnnotations( annotations, "label", false, null, "while", getPosition( node ) );
		String			label		= getBoxExprAsString( labelSearch, "label", false );

		return new BoxWhile( label, condition, body, getPosition( node ), getSourceText( node ) );
	}

	private BoxStatement toAst( File file, Template_returnContext node ) {
		BoxExpression expr;
		if ( node.expression() != null ) {
			expr = expressionVisitor.visit( node.expression() );
		} else {
			expr = new BoxNull( null, null );
		}
		return new BoxReturn( expr, getPosition( node ), getSourceText( node ) );
	}

	private BoxFunctionDeclaration toAst( File file, Template_functionContext node ) {
		BoxReturnType						returnType		= null;
		String								name			= null;
		List<BoxStatement>					body			= new ArrayList<>();
		List<BoxArgumentDeclaration>		args			= new ArrayList<>();
		List<BoxAnnotation>					annotations		= new ArrayList<>();
		List<BoxDocumentationAnnotation>	documentation	= new ArrayList<>();
		BoxAccessModifier					accessModifier	= null;
		List<BoxMethodDeclarationModifier>	modifiers		= new ArrayList<>();

		for ( var attr : node.template_attribute() ) {
			annotations.add( toAst( file, attr ) );
		}

		name = getBoxExprAsString( findExprInAnnotations( annotations, "name", true, null, "function", getPosition( node ) ), "name", false );

		String accessText = getBoxExprAsString( findExprInAnnotations( annotations, "function", false, null, null, null ), "access", true );
		if ( accessText != null ) {
			accessText = accessText.toLowerCase();
			if ( accessText.equals( "public" ) ) {
				accessModifier = BoxAccessModifier.Public;
			} else if ( accessText.equals( "private" ) ) {
				accessModifier = BoxAccessModifier.Private;
			} else if ( accessText.equals( "remote" ) ) {
				accessModifier = BoxAccessModifier.Remote;
			} else if ( accessText.equals( "package" ) ) {
				accessModifier = BoxAccessModifier.Package;
			}
		}

		BoxExpression	returnTypeSearch	= findExprInAnnotations( annotations, "returnType", false, null, null, null );
		String			returnTypeText		= getBoxExprAsString( returnTypeSearch, "returnType", true );
		if ( returnTypeText != null ) {
			BoxType	boxType	= BoxType.fromString( returnTypeText );
			String	fqn		= boxType.equals( BoxType.Fqn ) ? returnTypeText : null;
			returnType = new BoxReturnType( boxType, fqn, returnTypeSearch.getPosition(), returnTypeSearch.getSourceText() );
		}

		for ( var arg : node.template_argument() ) {
			args.add( toAst( file, arg ) );
		}

		body.addAll( toAst( file, node.body ) );

		return new BoxFunctionDeclaration( accessModifier, modifiers, name, returnType, args, annotations, documentation, body, getPosition( node ),
		    getSourceText( node ) );
	}

	private BoxArgumentDeclaration toAst( File file, Template_argumentContext node ) {
		Boolean								required		= false;
		String								type			= "Any";
		String								name			= "undefined";
		BoxExpression						expr			= null;
		List<BoxAnnotation>					annotations		= new ArrayList<>();
		List<BoxDocumentationAnnotation>	documentation	= new ArrayList<>();

		for ( var attr : node.template_attribute() ) {
			annotations.add( toAst( file, attr ) );
		}

		name		= getBoxExprAsString( findExprInAnnotations( annotations, "name", true, null, "function", getPosition( node ) ), "name", false );

		required	= BooleanCaster.cast(
		    getBoxExprAsString(
		        findExprInAnnotations( annotations, "required", false, null, null, null ),
		        "required",
		        false
		    )
		);

		expr		= findExprInAnnotations( annotations, "default", false, null, null, null );
		type		= getBoxExprAsString( findExprInAnnotations( annotations, "type", false, new BoxStringLiteral( "Any", null, null ), null, null ), "type",
		    false );

		return new BoxArgumentDeclaration( required, type, name, expr, annotations, documentation, getPosition( node ), getSourceText( node ) );
	}

	private BoxAnnotation toAst( File file, Template_attributeContext attribute ) {
		BoxFQN			name	= new BoxFQN( attribute.template_attributeName().getText(), getPosition( attribute.template_attributeName() ),
		    getSourceText( attribute.template_attributeName() ) );
		BoxExpression	value;
		if ( attribute.template_attributeValue() != null ) {
			value = toAst( file, attribute.template_attributeValue() );
		} else {
			value = new BoxStringLiteral( "", null, null );
		}
		return new BoxAnnotation( name, value, getPosition( attribute ), getSourceText( attribute ) );
	}

	private BoxExpression toAst( File file, Template_attributeValueContext node ) {
		if ( node.template_unquotedValue() != null ) {
			return new BoxStringLiteral( node.template_unquotedValue().getText(), getPosition( node ),
			    getSourceText( node ) );
		}
		if ( node.stringLiteral() != null ) {
			return expressionVisitor.visit( node.stringLiteral() );
		} else if ( node.el2() != null ) {
			return expressionVisitor.visit( node.el2() );
		} else {
			throw new BoxRuntimeException( "Unexpected attribute value type " + node.getText() );
		}
	}

	private BoxStatement toAst( File file, Template_tryContext node ) {
		List<BoxStatement> tryBody = new ArrayList<>();
		for ( var statements : node.template_statements() ) {
			tryBody.addAll( toAst( file, statements ) );
		}
		List<BoxTryCatch>	catches		= node.template_catchBlock().stream().map( it -> toAst( file, it ) ).toList();
		List<BoxStatement>	finallyBody	= new ArrayList<>();
		if ( node.template_finallyBlock() != null ) {
			finallyBody.addAll( toAst( file, node.template_finallyBlock().template_statements() ) );
		}
		return new BoxTry( tryBody, catches, finallyBody, getPosition( node ), getSourceText( node ) );
	}

	private BoxTryCatch toAst( File file, Template_catchBlockContext node ) {
		BoxExpression		exception	= new BoxIdentifier( "bxcatch", null, null );
		List<BoxExpression>	catchTypes;
		List<BoxStatement>	catchBody	= new ArrayList<>();

		List<BoxAnnotation>	annotations	= new ArrayList<>();

		if ( node.template_attribute() != null ) {
			for ( var attr : node.template_attribute() ) {
				annotations.add( toAst( file, attr ) );
			}
			var typeSearch = annotations.stream()
			    .filter( ( it ) -> it.getKey().getValue().equalsIgnoreCase( "type" ) && it.getValue() != null )
			    .findFirst();
			if ( typeSearch.isPresent() ) {
				catchTypes = List.of( typeSearch.get().getValue() );
			} else {
				catchTypes = List.of( new BoxFQN( "any", null, null ) );
			}
		} else {
			catchTypes = List.of( new BoxFQN( "any", null, null ) );
		}
		if ( node.template_statements() != null ) {
			catchBody = toAst( file, node.template_statements() );
		}
		return new BoxTryCatch( catchTypes, exception, catchBody, getPosition( node ), getSourceText( node ) );
	}

	private BoxIfElse toAst( File file, CFGrammar.Template_ifContext node ) {
		// if condition will always exist
		BoxExpression		condition			= expressionVisitor.visit( node.ifCondition );
		List<BoxStatement>	thenBodyStatements	= new ArrayList<>();
		List<BoxStatement>	elseBodyStatements	= new ArrayList<>();
		BoxStatement		elseBody			= null;

		// Then body will always exist
		thenBodyStatements.addAll( toAst( file, node.thenBody ) );

		if ( node.TEMPLATE_ELSE() != null ) {
			elseBodyStatements.addAll( toAst( file, node.elseBody ) );
			elseBody = new BoxStatementBlock( elseBodyStatements, getPosition( node.elseBody ), getSourceText( node.elseBody ) );
		}

		// Loop backward over elseif conditions, each one becoming the elseBody of the next.
		for ( int i = node.elseIfCondition.size() - 1; i >= 0; i-- ) {
			int		stopIndex;
			Point	end	= new Point( node.elseIfComponentClose.get( i ).getLine(),
			    node.elseIfComponentClose.get( i ).getCharPositionInLine() );
			stopIndex = node.elseIfComponentClose.get( i ).getStopIndex();
			if ( node.elseThenBody.get( i ).template_statement().size() > 0 ) {
				end			= new Point(
				    node.elseThenBody.get( i ).template_statement( node.elseThenBody.get( i ).template_statement().size() - 1 ).getStop().getLine(),
				    node.elseThenBody.get( i ).template_statement( node.elseThenBody.get( i ).template_statement().size() - 1 ).getStop()
				        .getCharPositionInLine() );
				stopIndex	= node.elseThenBody.get( i ).template_statement( node.elseThenBody.get( i ).template_statement().size() - 1 ).getStop()
				    .getStopIndex();
			}
			Position		pos				= new Position(
			    new Point( node.TEMPLATE_ELSEIF( i ).getSymbol().getLine(), node.TEMPLATE_ELSEIF( i ).getSymbol().getCharPositionInLine() - 3 ),
			    end, sourceToParse );
			BoxExpression	thisCondition	= expressionVisitor.visit( node.elseIfCondition.get( i ) );
			elseBodyStatements	= List.of(
			    new BoxIfElse(
			        thisCondition,
			        // TODO: I don't think this pos var is correct
			        new BoxStatementBlock( toAst( file, node.elseThenBody.get( i ) ), pos, getSourceText( node.elseThenBody.get( i ) ) ),
			        elseBody,
			        pos,
			        getSourceText( node, node.TEMPLATE_ELSEIF().get( i ).getSymbol().getStartIndex() - 3, stopIndex )
			    )
			);
			elseBody			= new BoxStatementBlock( elseBodyStatements, pos,
			    getSourceText( node, node.TEMPLATE_ELSEIF().get( i ).getSymbol().getStartIndex() - 3, stopIndex ) );
		}

		BoxStatement thenBody = new BoxStatementBlock( thenBodyStatements, getPosition( node.thenBody ), getSourceText( node.thenBody ) );
		// If there were no elseif's, the elsebody here will be the <cfelse>. Otherwise, it will be the last elseif.
		return new BoxIfElse( condition, thenBody, elseBody, getPosition( node ), getSourceText( node ) );
	}

	private BoxStatement toAst( File file, Template_setContext set ) {
		// In components, a <bx:set ...> component is an Expression Statement.
		return new BoxExpressionStatement( set.expression().accept( expressionVisitor ),
		    getPosition( set ),
		    getSourceText( set ) );
	}

	private BoxStatement toAst( File file, Template_outputContext node ) {
		List<BoxStatement>	statements	= new ArrayList<>();
		List<BoxAnnotation>	annotations	= new ArrayList<>();

		for ( var attr : node.template_attribute() ) {
			annotations.add( toAst( file, attr ) );
		}
		if ( node.template_statements() != null ) {
			outputCounter++;
			statements.addAll( toAst( file, node.template_statements() ) );
			outputCounter--;
		}

		return new BoxComponent( "output", annotations, statements, getPosition( node ), getSourceText( node ) );
	}

	/**
	 * A helper function to find a specific annotation by name and return the value expression
	 *
	 * @param annotations             the list of annotations to search
	 * @param name                    the name of the annotation to find
	 * @param required                whether the annotation is required. If required, and not present a parsing Issue is created.
	 * @param defaultValue            the default value to return if the annotation is not found. Ignored if requried is false.
	 * @param containingComponentName the name of the component that contains the annotation, used in error handling
	 * @param position                the position of the component, used in error handling
	 *
	 * @return the value expression of the annotation, or the default value if the annotation is not found
	 *
	 */
	private BoxExpression findExprInAnnotations( List<BoxAnnotation> annotations, String name, boolean required, BoxExpression defaultValue,
	    String containingComponentName,
	    Position position ) {
		var search = annotations.stream().filter( ( it ) -> it.getKey().getValue().equalsIgnoreCase( name ) ).findFirst();
		if ( search.isPresent() ) {
			return search.get().getValue();
		} else if ( !required ) {
			return defaultValue;
		} else {
			issues.add( new Issue( "Missing " + name + " attribute on " + containingComponentName + " component", position ) );
			return new BoxNull( null, null );
		}

	}

	/**
	 * A helper function to take a BoxExpr and return the value expression as a string.
	 * If the expression is not a string literal, an Issue is created.
	 *
	 * @param expr       the expression to get the value from
	 * @param name       the name of the attribute, used in error handling
	 * @param allowEmpty whether an empty string is allowed. If not allowed, an Issue is created.
	 *
	 * @return the value of the expression as a string, or null if the expression is null
	 */
	private String getBoxExprAsString( BoxExpression expr, String name, boolean allowEmpty ) {
		if ( expr == null ) {
			return null;
		}
		if ( expr instanceof BoxStringLiteral str ) {
			if ( !allowEmpty && str.getValue().trim().isEmpty() ) {
				issues.add( new Issue( "Attribute [" + name + "] cannot be empty", expr.getPosition() ) );
			}
			return str.getValue();
		} else {
			issues.add( new Issue( "Attribute [" + name + "] attribute must be a string literal", expr.getPosition() ) );
			return "";
		}
	}

	private List<BoxStatement> toAst( File file, Template_textContentContext node ) {
		List<BoxStatement>		statements	= new ArrayList<>();
		List<ParserRuleContext>	nodes		= new ArrayList<>();
		boolean					allLiterals	= true;

		for ( var child : node.children ) {
			if ( child instanceof CFGrammar.Template_interpolatedExpressionContext intrpexpr ) {
				allLiterals = false;
				nodes.add( intrpexpr );
			} else if ( child instanceof CFGrammar.Template_nonInterpolatedTextContext strlit ) {
				nodes.add( strlit );
			} else if ( child instanceof CFGrammar.Template_commentContext ) {
				if ( !nodes.isEmpty() ) {
					statements.add( processTextContent( file, nodes, allLiterals ) );
					allLiterals = true;
					nodes.clear();
				}
			}
		}
		if ( !nodes.isEmpty() ) {
			statements.add( processTextContent( file, nodes, allLiterals ) );
		}
		return statements;
	}

	private BoxStatement processTextContent( File file, List<ParserRuleContext> nodes, boolean allLiterals ) {
		BoxExpression	expr;
		Position		pos			= getPosition( nodes.get( 0 ), nodes.get( nodes.size() - 1 ) );
		String			sourceText	= getSourceText( nodes.get( 0 ), nodes.get( nodes.size() - 1 ) );
		// No interpolated nodes, only string
		if ( allLiterals ) {
			expr = new BoxStringLiteral(
			    // combine all the literal strings down into one
			    escapeStringLiteral( nodes.stream().map( n -> n.getText() ).collect( Collectors.joining( "" ) ) ),
			    pos,
			    sourceText
			);
		} else {
			List<BoxExpression> expressions = new ArrayList<>();
			for ( var child : nodes ) {
				if ( child instanceof CFGrammar.Template_interpolatedExpressionContext intrpexpr && intrpexpr.expression() != null ) {
					// parse the text between the hash signs as an expression
					expressions.add( intrpexpr.expression().accept( expressionVisitor ) );
				} else if ( child instanceof CFGrammar.Template_interpolatedExpressionContext intrpexpr && intrpexpr.expression() != null ) {
					// parse the text between the hash signs as an expression
					expressions.add( expressionVisitor.visit( intrpexpr.expression() ) );
				} else if ( child instanceof CFGrammar.Template_nonInterpolatedTextContext strlit ) {
					expressions.add( new BoxStringLiteral( escapeStringLiteral( strlit.getText() ), getPosition( strlit ), getSourceText( strlit ) ) );
				}
			}
			expr = new BoxStringInterpolation( expressions, pos, sourceText );
		}
		return new BoxBufferOutput( expr, pos, sourceText );
	}

	/**
	 * Escape pounds in a string literal
	 *
	 * @param string the string to escape
	 *
	 * @return the escaped string
	 */
	private String escapeStringLiteral( String string ) {
		if ( outputCounter == 0 ) {
			return string;
		}
		// We only need to do this if we're inside an output
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
		String escaped = string.replace( "##", "#" );
		return escaped.replace( quoteChar + quoteChar, quoteChar );
	}

	/**
	 * This is only in use now for loop and while, which allow conditions to be specified as a string.
	 * I could parse them as expressions, but that would be a lot more annoying work in the Lexer to detect that
	 * exact scenario and I don't think it's worth it.
	 * 
	 * @param code     the code to parse
	 * @param position the position of the code
	 * 
	 * @return the parsed expression
	 */
	public BoxExpression parseCFExpression( String code, Position position ) {
		try {
			ParsingResult result = new CFParser( position.getStart().getLine(), position.getStart().getColumn() )
			    .setSource( sourceToParse )
			    .setSubParser( true )
			    .parseExpression( code );
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
	CFParser setSource( Source source ) {
		if ( this.sourceToParse != null ) {
			return this;
		}
		this.sourceToParse = source;
		this.errorListener.setSource( sourceToParse );
		return this;
	}

	@Override
	public CFParser setSubParser( boolean subParser ) {
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
	public void checkDotAccess( BoxExpression left, BoxExpression right, boolean isStatic ) {

		// Check the right hand side to see if it is a valid access method
		checkRight( right, isStatic );

		// Check to see if the left hand side is something that is valid to be accessed via a dot access
		checkLeft( left, isStatic );

		// Now we know the LHS is valid for access by a dot method and the RHS is a valid access method, so
		// we can check the combinations here if needed.
		// TODO: @Brad - Add more checks here if needed
	}

	/**
	 * Check the right hand side of a dot access to ensure it is a valid access method
	 *
	 * @param right the right side of the dot access left.right
	 */
	private void checkRight( BoxExpression right, boolean isStatic ) {
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
			default -> errorListener.semanticError( ( isStatic ? "static" : "dot" ) + " access via " + right.getDescription() + " is not a valid access method",
			    right.getPosition() );
		}
	}

	/**
	 * Check the left hand side of a dot access to ensure it is a valid construct for dot access
	 *
	 * @param left the left side of the dot access left.right
	 */
	private void checkLeft( BoxExpression left, boolean isStatic ) {
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
			}
			case BoxStaticMethodInvocation ignored -> {
			}
			case BoxStaticAccess ignored -> {
			}
			case BoxFQN ignored -> {
			}
			default -> errorListener.semanticError( left.getDescription() + " is not a valid construct for " + ( isStatic ? "static" : "dot" ) + " access",
			    left.getPosition() );
		}
	}

	/**
	 * Check array access to ensure that nonsensical access methods are rejected at AST build time
	 *
	 * @param ctx    the Parsers ExprArrayAccessContext for source reference etc
	 * @param object the object node that is being accessed as if it were an array
	 * @param access the access node that is being used to access the object
	 */
	public void checkArrayAccess( CFGrammar.ExprArrayAccessContext ctx, BoxExpression object, @SuppressWarnings( "unused" ) BoxExpression access ) {

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
