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

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BOMInputStream;
import ortus.boxlang.compiler.ast.*;
import ortus.boxlang.compiler.ast.comment.BoxDocComment;
import ortus.boxlang.compiler.ast.comment.BoxMultiLineComment;
import ortus.boxlang.compiler.ast.comment.BoxSingleLineComment;
import ortus.boxlang.compiler.ast.expression.BoxNull;
import ortus.boxlang.compiler.toolchain.BoxExpressionVisitor;
import ortus.boxlang.compiler.toolchain.BoxVisitor;
import ortus.boxlang.compiler.toolchain.DotGen;
import ortus.boxlang.parser.antlr.BoxScriptGrammar;
import ortus.boxlang.parser.antlr.BoxScriptLexer;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.services.ComponentService;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Parser for Box scripts
 */
public class BoxScriptParser extends AbstractParser {

	private boolean			inOutputBlock		= false;
	public ComponentService	componentService	= BoxRuntime.getInstance().getComponentService();
	private Token			firstToken			= null;

	/**
	 * Constructor
	 */
	public BoxScriptParser() {
		super();
	}

	public BoxScriptParser( int startLine, int startColumn ) {
		super( startLine, startColumn );
	}

	public BoxScriptParser( int startLine, int startColumn, boolean inOutputBlock ) {
		super( startLine, startColumn );
		this.inOutputBlock = inOutputBlock;
	}

	public void setInOutputBlock( boolean inOutputBlock ) {
		this.inOutputBlock = inOutputBlock;
	}

	public boolean getInOutputBlock() {
		return inOutputBlock;
	}

	/**
	 * Parse a Box script file
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
	public ParsingResult parse( File file ) throws IOException {
		this.file = file;
		setSource( new SourceFile( file ) );
		BOMInputStream		inputStream			= getInputStream( file );
		Optional<String>	ext					= Parser.getFileExtension( file.getAbsolutePath() );
		Boolean				classOrInterface	= ext.isPresent() && ext.get().equalsIgnoreCase( "bx" );
		BoxNode				ast					= parserFirstStage( inputStream, classOrInterface );

		if ( issues.isEmpty() ) {
			return new ParsingResult( ast, issues, comments );
		}
		return new ParsingResult( null, issues, comments );
	}

	/**
	 * Parse a Box script string
	 *
	 * @param code source code to parse
	 *
	 * @return a ParsingResult containing the AST with a BoxScript as root and the list of errors (if any)
	 *
	 * @throws IOException
	 *
	 * @see BoxScript
	 * @see ParsingResult
	 */
	public ParsingResult parse( String code ) throws IOException {
		return parse( code, false );
	}

	/**
	 * Parse a Box script string
	 *
	 * @param code source code to parse
	 *
	 * @return a ParsingResult containing the AST with a BoxScript as root and the list of errors (if any)
	 *
	 * @throws IOException
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
	 * Parse a Box script string expression
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
	public ParsingResult parseExpression( String code ) throws IOException {
		setSource( new SourceCode( code ) );
		InputStream				inputStream	= IOUtils.toInputStream( code, StandardCharsets.UTF_8 );

		BoxScriptLexerCustom	lexer		= new BoxScriptLexerCustom( CharStreams.fromStream( inputStream, StandardCharsets.UTF_8 ) );
		BoxScriptGrammar		parser		= getParser( lexer );
		addErrorListeners( lexer, parser );

		BoxScriptGrammar.ExpressionContext parseTree = parser.expression();

		// This must run FIRST before resetting the lexer
		validateParse( lexer );

		// This can add issues to an otherwise successful parse
		extractComments( lexer );

		var				expressionVisitor	= new BoxExpressionVisitor( this, new BoxVisitor( this ) );
		BoxExpression	ast					= parseTree.accept( expressionVisitor );
		return new ParsingResult( ast, issues, comments );
	}

	/**
	 * Parse a Box script string statement
	 *
	 * @param code source of the expression to parse
	 *
	 * @return a ParsingResult containing the AST with a BoxStatement as root and the list of errors (if any)
	 *
	 * @throws IOException
	 *
	 * @see ParsingResult
	 * @see BoxStatement
	 */
	public ParsingResult parseStatement( String code ) throws IOException {
		setSource( new SourceCode( code ) );
		InputStream				inputStream	= IOUtils.toInputStream( code, StandardCharsets.UTF_8 );

		BoxScriptLexerCustom	lexer		= new BoxScriptLexerCustom( CharStreams.fromStream( inputStream, StandardCharsets.UTF_8 ) );
		BoxScriptGrammar		parser		= getParser( lexer );
		addErrorListeners( lexer, parser );
		BoxScriptGrammar.FunctionOrStatementContext parseTree = parser.functionOrStatement();

		// This must run FIRST before resetting the lexer
		validateParse( lexer );
		// This can add issues to an otherwise successful parse
		extractComments( lexer );

		var		visitor	= new BoxVisitor( this );
		BoxNode	ast		= parseTree.accept( visitor );
		return new ParsingResult( ast, issues, comments );
	}

	public BoxScriptParser setSource( Source source ) {
		if ( this.sourceToParse != null ) {
			return this;
		}
		this.sourceToParse = source;
		return this;
	}

	/**
	 * A private instance of the parser that will be reused by this BoxScriptParser instance
	 */
	private BoxScriptGrammar boxParser;

	/**
	 * Returns an instance of BoxParserGrammar parser, set up to parse the supplied
	 * InputStream. If the parser instance already exists, then we just set up the parser
	 * with the new lexer. If it does not yet exist, then we create it and give it the supplied lexer.
	 *
	 * @param lexer The new lexer instance to use
	 *
	 * @return An instance of the parser to use for the given input
	 */
	private BoxScriptGrammar getParser( BoxScriptLexerCustom lexer ) {
		if ( boxParser == null ) {
			boxParser = new BoxScriptGrammar( new CommonTokenStream( lexer ) );
			// boxParser.getInterpreter().setPredictionMode( PredictionMode.SLL );
		} else {
			boxParser.setInputStream( new CommonTokenStream( lexer ) );
		}
		addErrorListeners( lexer, boxParser );
		boxParser.setTrace( true );
		return boxParser;
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
		BoxScriptLexerCustom						lexer					= new BoxScriptLexerCustom(
		    CharStreams.fromStream( stream, StandardCharsets.UTF_8 ) );
		var											parser					= getParser( lexer );
		BoxScriptGrammar.ClassOrInterfaceContext	classOrInterfaceContext	= null;
		BoxScriptGrammar.ScriptContext				scriptContext			= null;
		File										f						= null;
		if ( classOrInterface ) {
			System.out.println( "\n\n============================\nCompiling class\n" );
			f						= new File( "./grapher/data/scratch_class.bx" );
			classOrInterfaceContext	= parser.classOrInterface();
		} else {
			System.out.println( "\n\n============================\nCompiling script\n" );
			f				= new File( "./grapher/data/scratch_script.bx" );
			scriptContext	= parser.script();
		}
		File last = new File( "./grapher/data/lastAST.json" );
		if ( last.exists() ) {
			var copy = new File( "./grapher/data/prevAST.json" );
			if ( copy.exists() ) {
				copy.delete();
			}
			Files.copy( last.toPath(), copy.toPath() );
		}
		// This must run FIRST before resetting the lexer
		validateParse( lexer );

		// This can add issues to an otherwise successful parse
		extractComments( lexer );

		lexer.reset();
		firstToken = lexer.nextToken();
		BoxNode	rootNode;

		// TODO: Before we call the AST builder, we call the semantic analyzer and
		// check that the source is semantically valid as well is syntactically so.

		// Create the visitor we will use to build the AST. Note that it
		// references this instance of BoxScriptParser, which is not ideal, but
		// we would have to rebuild the inheritance structure to avoid this. Ideally
		// teh tools in AbstractParser would be in their own class and injected into
		// the parsers, including this one.
		var		visitor	= new BoxVisitor( this );
		DotGen	dotGen;
		if ( f.exists() ) {
			f.delete();
			f.createNewFile();
		}
		lexer.reset();
		Files.write( f.toPath(), lexer.getInputStream().toString().getBytes() );

		if ( classOrInterface ) {
			dotGen		= new DotGen( classOrInterfaceContext, parser, f );
			rootNode	= classOrInterfaceContext.accept( visitor );

		} else {
			dotGen		= new DotGen( scriptContext, parser, f );
			rootNode	= scriptContext.accept( visitor );
		}
		dotGen.writeDotFor();
		dotGen.writeTreeFor();
		dotGen.writeSvgFor();

		if ( isSubParser() ) {
			return rootNode;
		}
		if ( rootNode == null ) {
			return null;
		}

		var json = rootNode.toJSON();

		// associate all comments in the source with the appropriate AST nodes
		return rootNode.associateComments( this.comments );
	}

	public List<BoxStatement> parseBoxTemplateStatements( String code, Position position ) {
		try {
			if ( inOutputBlock ) {
				code = "<bx:output>" + code + "</bx:output>";
			}
			ParsingResult result = new BoxTemplateParser( position.getStart().getLine(), position.getStart().getColumn() )
			    .setSource( sourceToParse )
			    .setSubParser( true )
			    .parse( code );
			this.comments.addAll( result.getComments() );
			if ( result.getIssues().isEmpty() ) {
				BoxNode root = result.getRoot();
				if ( root instanceof BoxTemplate template ) {
					return template.getStatements();
				} else if ( root instanceof BoxStatement statement ) {
					return List.of( statement );
				} else {
					// Could be a BoxClass, which we may actually need to support
					issues.add( new Issue( "Unexpected root node type [" + root.getClass().getName() + "] in component island.", root.getPosition() ) );
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

	public BoxExpression parseBoxExpression( String code, Position position ) {
		try {
			ParsingResult result = new BoxScriptParser( position.getStart().getLine(), position.getStart().getColumn() )
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
			issues.add( new Issue( "Error parsing expression " + e.getMessage(), position ) );
			return new BoxNull( null, null );
		}
	}

	private void validateParse( BoxScriptLexerCustom lexer ) {

		if ( lexer.hasUnpoppedModes() ) {
			List<String> modes = lexer.getUnpoppedModes();

			if ( modes.contains( "hashMode" ) ) {
				Token lastHash = lexer.findPreviousToken( BoxScriptLexerCustom.ICHAR );
				issues.add( new Issue( "Untermimated hash expression inside of string literal.", getPosition( lastHash ) ) );
			} else if ( modes.contains( "quotesMode" ) ) {
				Token lastQuote = lexer.findPreviousToken( BoxScriptLexerCustom.OPEN_QUOTE );
				issues.add( new Issue( "Untermimated double quote expression.", getPosition( lastQuote ) ) );
			} else if ( modes.contains( "squotesMode" ) ) {
				Token lastQuote = lexer.findPreviousToken( BoxScriptLexerCustom.OPEN_QUOTE );
				issues.add( new Issue( "Untermimated single quote expression.", getPosition( lastQuote ) ) );
			} else {
				// Catch-all. If this error is encontered, look at what modes were still on the stack, find what token was never ended, and
				// add logic like the above to handle it. Eventually, this catch-all should never be used.
				Position position = new Position( new Point( 0, 0 ), new Point( 0, 0 ), sourceToParse );
				issues.add( new Issue(
				    "Unpopped Lexer modes. [" + modes.stream().collect( Collectors.joining( ", " ) ) + "] Please report this to get a better error message.",
				    position ) );
			}
			// I'm only returning here because we have to reset the lexer above to get the position of the unmatched token, so we no longer have
			// the ability to check for unconsumed tokens.
			return;
		}

		// Check if there are unconsumed tokens
		Token token = lexer.nextToken();
		while ( token.getType() != Token.EOF && ( token.getChannel() == BoxScriptLexerCustom.HIDDEN ) ) {
			token = lexer.nextToken();
		}
		if ( token.getType() != Token.EOF ) {
			StringBuffer	extraText	= new StringBuffer();
			int				startLine	= token.getLine();
			int				startColumn	= token.getCharPositionInLine();
			int				endColumn	= startColumn + token.getText().length();
			Position		position	= createOffsetPosition( startLine, startColumn, startLine, endColumn );
			while ( token.getType() != Token.EOF && extraText.length() < 100 ) {
				extraText.append( token.getText() );
				token = lexer.nextToken();
			}
			issues.add( new Issue( "Extra char(s) [" + extraText.toString() + "] at the end of parsing.", position ) );
		}

		// If there is already a parsing issue, try to get a more specific error
		if ( issues.isEmpty() ) {

			Token unclosedBrace = lexer.findUnclosedToken( BoxScriptLexerCustom.LBRACE, BoxScriptLexerCustom.RBRACE );
			if ( unclosedBrace != null ) {
				issues.clear();
				issues.add(
				    new Issue( "Unclosed curly brace [{] on line " + ( unclosedBrace.getLine() + this.startLine ),
				        createOffsetPosition( unclosedBrace.getLine(),
				            unclosedBrace.getCharPositionInLine(), unclosedBrace.getLine(), unclosedBrace.getCharPositionInLine() + 1 ) ) );
			}

			Token unclosedParen = lexer.findUnclosedToken( BoxScriptLexerCustom.LPAREN, BoxScriptLexerCustom.RPAREN );
			if ( unclosedParen != null ) {
				issues.clear();
				issues
				    .add( new Issue( "Unclosed parenthesis [(] on line " + ( unclosedParen.getLine() + this.startLine ),
				        createOffsetPosition( unclosedParen.getLine(),
				            unclosedParen.getCharPositionInLine(), unclosedParen.getLine(), unclosedParen.getCharPositionInLine() + 1 ) ) );
			}
		}
	}

	/**
	 * Extract comments from the lexer's hidden channel and parse the java doc comments
	 *
	 * @param lexer the lexer to extract comments from
	 */
	private void extractComments( BoxScriptLexerCustom lexer ) throws IOException {
		lexer.reset();
		Token		token		= lexer.nextToken();
		DocParser	docParser	= new DocParser( token.getLine(), token.getCharPositionInLine() ).setSource( sourceToParse );
		while ( token.getType() != Token.EOF ) {
			if ( token.getType() == BoxScriptLexer.JAVADOC_COMMENT ) {
				ParsingResult result = docParser.parse( null, token.getText() );
				if ( docParser.issues.isEmpty() ) {
					comments.add( ( BoxDocComment ) result.getRoot() );
				} else {
					// Add these issues to the main parser
					issues.addAll( docParser.issues );
				}
			} else if ( token.getType() == BoxScriptLexer.LINE_COMMENT ) {
				String commentText = token.getText().trim().substring( 2 ).trim();
				comments.add( new BoxSingleLineComment( commentText, getPosition( token ), token.getText() ) );
			} else if ( token.getType() == BoxScriptLexer.COMMENT ) {
				comments.add( new BoxMultiLineComment( extractMultiLineCommentText( token.getText(), false ), getPosition( token ), token.getText() ) );
			}
			token = lexer.nextToken();
			docParser.setStartLine( token.getLine() );
			docParser.setStartColumn( token.getCharPositionInLine() );
		}
	}

	public Token getFirstToken() {
		return firstToken;
	}

	@Override
	public BoxScriptParser setSubParser( boolean subParser ) {
		this.subParser = subParser;
		return this;
	}
}
