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
import ortus.boxlang.compiler.ast.statement.BoxAnnotation;
import ortus.boxlang.compiler.ast.statement.BoxDocumentationAnnotation;
import ortus.boxlang.compiler.ast.statement.BoxImport;
import ortus.boxlang.compiler.ast.statement.BoxProperty;
import ortus.boxlang.compiler.toolchain.BoxExpressionVisitor;
import ortus.boxlang.parser.antlr.BoxScriptGrammar;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.services.ComponentService;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Parser for Box scripts
 */
public class BoxScriptParser extends AbstractParser {

	private boolean			inOutputBlock		= false;
	public ComponentService	componentService	= BoxRuntime.getInstance().getComponentService();

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
		BoxScriptGrammar		parser		= new BoxScriptGrammar( new CommonTokenStream( lexer ) );
		addErrorListeners( lexer, parser );

		BoxScriptGrammar.ExpressionContext parseTree = parser.expression();

		// This must run FIRST before resetting the lexer
		// validateParse( lexer );
		// This can add issues to an otherwise successful parse
		// extractComments( lexer );

		try {
			var				expressionVisitor	= new BoxExpressionVisitor();
			BoxExpression	ast					= parseTree.accept( expressionVisitor );
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
		BoxScriptGrammar		parser		= new BoxScriptGrammar( new CommonTokenStream( lexer ) );
		addErrorListeners( lexer, parser );
		BoxScriptGrammar.FunctionOrStatementContext parseTree = parser.functionOrStatement();

		// This must run FIRST before resetting the lexer
		validateParse( lexer );
		// This can add issues to an otherwise successful parse
		extractComments( lexer );
		try {
			BoxStatement ast = toAst( null, parseTree );
			return new ParsingResult( ast, issues, comments );
		} catch ( Exception e ) {
			// Ignore issues creating AST if the parsing already had failures
			if ( issues.isEmpty() ) {
				throw e;
			}
			return new ParsingResult( null, issues, comments );
		}
	}

	// /**
	// * Fist stage parser
	// *
	// * @param stream input stream (file or string) of the source code
	// *
	// * @return the ANTLR ParserRule representing the parse tree of the code
	// *
	// * @throws IOException io error
	// */
	// @Override
	// protected BoxNode parserFirstStage( InputStream stream, Boolean classOrInterface ) throws IOException {
	// BoxScriptLexerCustom lexer = new BoxScriptLexerCustom( CharStreams.fromStream( stream, StandardCharsets.UTF_8 ) );
	// BoxScriptGrammar parser = new BoxScriptGrammar( new CommonTokenStream( lexer ) );
	// addErrorListeners( lexer, parser );
	// BoxScriptGrammar.ClassOrInterfaceContext classOrInterfaceContext = null;
	// BoxScriptGrammar.ScriptContext scriptContext = null;
	// if ( classOrInterface ) {
	// classOrInterfaceContext = parser.classOrInterface();
	// } else {
	// scriptContext = parser.script();
	// }
	//
	// // This must run FIRST before resetting the lexer
	// validateParse( lexer );
	// // This can add issues to an otherwise successful parse
	// extractComments( lexer );
	//
	// lexer.reset();
	// Token firstToken = lexer.nextToken();
	// BoxNode rootNode;
	// try {
	// if ( classOrInterface ) {
	// rootNode = toAst( null, classOrInterfaceContext );
	// } else {
	// rootNode = toAst( null, scriptContext, firstToken );
	// }
	// } catch ( Exception e ) {
	// // Ignore issues creating AST if the parsing already had failures
	// if ( issues.isEmpty() ) {
	// throw e;
	// }
	// return null;
	// }
	//
	// if ( isSubParser() ) {
	// return rootNode;
	// }
	// if ( rootNode == null ) {
	// return null;
	// }
	// // associate all comments in the source with the appropriate AST nodes
	// return rootNode.associateComments( this.comments );
	//
	// }
	//
	private void validateParse(BoxScriptLexerCustom lexer) {

		if (lexer.hasUnpoppedModes()) {
			List<String> modes = lexer.getUnpoppedModes();

			if (modes.contains("hashMode")) {
				Token lastHash = lexer.findPreviousToken(BoxScriptLexerCustom.ICHAR);
				issues.add(new Issue("Untermimated hash expression inside of string literal.", getPosition(lastHash)));
			} else if (modes.contains("quotesMode")) {
				Token lastQuote = lexer.findPreviousToken(BoxScriptLexerCustom.OPEN_QUOTE);
				issues.add(new Issue("Untermimated double quote expression.", getPosition(lastQuote)));
			} else if (modes.contains("squotesMode")) {
				Token lastQuote = lexer.findPreviousToken(BoxScriptLexerCustom.OPEN_QUOTE);
				issues.add(new Issue("Untermimated single quote expression.", getPosition(lastQuote)));
			} else {
				// Catch-all. If this error is encontered, look at what modes were still on the stack, find what token was never ended, and
				// add logic like the above to handle it. Eventually, this catch-all should never be used.
				Position position = new Position(new Point(0, 0), new Point(0, 0), sourceToParse);
				issues.add(new Issue("Unpopped Lexer modes. [" + modes.stream().collect(Collectors.joining(", ")) + "] Please report this to get a better error message.", position));
			}
			// I'm only returning here because we have to reset the lexer above to get the position of the unmatched token, so we no longer have
			// the ability to check for unconsumed tokens.
			return;
		}

	//
	// // Check if there are unconsumed tokens
	// Token token = lexer.nextToken();
	// while ( token.getType() != Token.EOF && ( token.getChannel() == BoxScriptLexerCustom.HIDDEN ) ) {
	// token = lexer.nextToken();
	// }
	// if ( token.getType() != Token.EOF ) {
	// StringBuffer extraText = new StringBuffer();
	// int startLine = token.getLine();
	// int startColumn = token.getCharPositionInLine();
	// int endColumn = startColumn + token.getText().length();
	// Position position = createOffsetPosition( startLine, startColumn, startLine, endColumn );
	// while ( token.getType() != Token.EOF && extraText.length() < 100 ) {
	// extraText.append( token.getText() );
	// token = lexer.nextToken();
	// }
	// issues.add( new Issue( "Extra char(s) [" + extraText.toString() + "] at the end of parsing.", position ) );
	// }
	//
	// // If there is already a parsing issue, try to get a more specific error
	// if ( issues.isEmpty() ) {
	//
	// Token unclosedBrace = lexer.findUnclosedToken( BoxScriptLexerCustom.LBRACE, BoxScriptLexerCustom.RBRACE );
	// if ( unclosedBrace != null ) {
	// issues.clear();
	// issues.add(
	// new Issue( "Unclosed curly brace [{] on line " + ( unclosedBrace.getLine() + this.startLine ),
	// createOffsetPosition( unclosedBrace.getLine(),
	// unclosedBrace.getCharPositionInLine(), unclosedBrace.getLine(), unclosedBrace.getCharPositionInLine() + 1 ) ) );
	// }
	//
	// Token unclosedParen = lexer.findUnclosedToken( BoxScriptLexerCustom.LPAREN, BoxScriptLexerCustom.RPAREN );
	// if ( unclosedParen != null ) {
	// issues.clear();
	// issues
	// .add( new Issue( "Unclosed parenthesis [(] on line " + ( unclosedParen.getLine() + this.startLine ),
	// createOffsetPosition( unclosedParen.getLine(),
	// unclosedParen.getCharPositionInLine(), unclosedParen.getLine(), unclosedParen.getCharPositionInLine() + 1 ) ) );
	// }
	// }
	// }
	//
	// private void extractComments( BoxScriptLexerCustom lexer ) throws IOException {
	// lexer.reset();
	// Token token = lexer.nextToken();
	// DocParser docParser = new DocParser( token.getLine(), token.getCharPositionInLine() ).setSource( sourceToParse );
	// while ( token.getType() != Token.EOF ) {
	// if ( token.getType() == BoxScriptLexer.JAVADOC_COMMENT ) {
	// ParsingResult result = docParser.parse( null, token.getText() );
	// if ( docParser.issues.isEmpty() ) {
	// comments.add( ( BoxDocComment ) result.getRoot() );
	// } else {
	// // Add these issues to the main parser
	// issues.addAll( docParser.issues );
	// }
	// } else if ( token.getType() == BoxScriptLexer.LINE_COMMENT ) {
	// String commentText = token.getText().trim().substring( 2 ).trim();
	// comments.add( new BoxSingleLineComment( commentText, getPosition( token ), token.getText() ) );
	// } else if ( token.getType() == BoxScriptLexer.COMMENT ) {
	// comments.add( new BoxMultiLineComment( extractMultiLineCommentText( token.getText(), false ), getPosition( token ), token.getText() ) );
	// }
	// token = lexer.nextToken();
	// docParser.setStartLine( token.getLine() );
	// docParser.setStartColumn( token.getCharPositionInLine() );
	// }
	// }
	//
	//
	// private BoxNode toAst( File file, BoxScriptGrammar.InterfaceContext interface_ ) {
	// List<BoxStatement> body = new ArrayList<>();
	// List<BoxAnnotation> annotations = new ArrayList<>();
	// List<BoxAnnotation> postAnnotations = new ArrayList<>();
	// List<BoxDocumentationAnnotation> documentation = new ArrayList<>();
	// List<BoxImport> imports = new ArrayList<>();
	//
	// interface_.importStatement().forEach( stmt -> {
	// imports.add( toAst( file, stmt ) );
	// } );
	//
	// for ( BoxScriptGrammar.PostannotationContext annotation : interface_.postannotation() ) {
	// postAnnotations.add( toAst( file, annotation ) );
	// }
	// interface_.interfaceFunction().forEach( stmt -> {
	// body.add( toAst( file, stmt ) );
	// } );
	// interface_.function().forEach( stmt -> {
	// BoxFunctionDeclaration funDec = toAst( file, stmt );
	// body.add( funDec );
	// // ensure last function added has default modifier
	// if ( funDec.getModifiers().stream().noneMatch( m -> m.equals( BoxMethodDeclarationModifier.DEFAULT ) ) ) {
	// issues.add( new Issue( "Interface methods must have the default modifier", funDec.getPosition() ) );
	// }
	// } );
	//
	// return new BoxInterface( imports, body, annotations, postAnnotations, documentation, getPosition( interface_ ), getSourceText( interface_ ) );
	// }
	//
	// /**
	// * Converts the interface Function declaration parser rule to the corresponding AST node.
	// *
	// * @param file source file, if any
	// * @param node ANTLR FunctionContext rule
	// *
	// * @return corresponding AST InterfaceFunctionContext
	// *
	// * @see InterfaceFunctionContext
	// */
	// private BoxFunctionDeclaration toAst( File file, BoxScriptGrammar.InterfaceFunctionContext node ) {
	// return processFunction(
	// node.functionSignature().preannotation(),
	// node.postannotation(),
	// node.functionSignature().identifier().getText(),
	// node.functionSignature(),
	// null,
	// node );
	// }
	//
	// /**
	// * Converts the Function declaration parser rule to the corresponding AST node.
	// *
	// * @param file source file, if any
	// * @param node ANTLR FunctionContext rule
	// *
	// * @return corresponding AST BoxFunctionDeclaration
	// *
	// * @see BoxFunctionDeclaration
	// */
	// private BoxFunctionDeclaration toAst( File file, BoxScriptGrammar.FunctionContext node ) {
	// return processFunction(
	// node.functionSignature().preannotation(),
	// node.postannotation(),
	// node.functionSignature().identifier().getText(),
	// node.functionSignature(),
	// node.statementBlock(),
	// node );
	// }
	//
	// private BoxFunctionDeclaration processFunction(
	// List<BoxScriptGrammar.PreannotationContext> preannotations,
	// List<BoxScriptGrammar.PostannotationContext> postannotations,
	// String name,
	// BoxScriptGrammar.FunctionSignatureContext functionSignature,
	// BoxScriptGrammar.StatementBlockContext statementBlock,
	// ParserRuleContext node ) {
	//
	// BoxReturnType returnType = null;
	// // Is null for interface function
	// List<BoxStatement> body = null;
	// List<BoxArgumentDeclaration> args = new ArrayList<>();
	// List<BoxAnnotation> annotations = new ArrayList<>();
	// List<BoxDocumentationAnnotation> documentation = new ArrayList<>();
	// List<BoxAnnotation> annToRemove = new ArrayList<>();
	// BoxAccessModifier accessModifier = null;
	// List<BoxMethodDeclarationModifier> modifiers = new ArrayList<>();
	//
	// if ( functionSignature.modifiers() != null ) {
	// if ( !functionSignature.modifiers().STATIC().isEmpty() ) {
	// modifiers.add( BoxMethodDeclarationModifier.STATIC );
	// }
	// if ( !functionSignature.modifiers().FINAL().isEmpty() ) {
	// modifiers.add( BoxMethodDeclarationModifier.FINAL );
	// }
	// if ( !functionSignature.modifiers().ABSTRACT().isEmpty() ) {
	// modifiers.add( BoxMethodDeclarationModifier.ABSTRACT );
	// }
	// if ( !functionSignature.modifiers().DEFAULT().isEmpty() ) {
	// modifiers.add( BoxMethodDeclarationModifier.DEFAULT );
	// }
	// if ( functionSignature.modifiers().accessModifier() != null && !functionSignature.modifiers().accessModifier().isEmpty() ) {
	// var accessModifierNode = functionSignature.modifiers().accessModifier( 0 );
	// if ( accessModifierNode.PUBLIC() != null ) {
	// accessModifier = BoxAccessModifier.Public;
	// } else if ( accessModifierNode.PRIVATE() != null ) {
	// accessModifier = BoxAccessModifier.Private;
	// } else if ( accessModifierNode.REMOTE() != null ) {
	// accessModifier = BoxAccessModifier.Remote;
	// } else if ( accessModifierNode.PACKAGE() != null ) {
	// accessModifier = BoxAccessModifier.Package;
	// }
	// }
	// }
	//
	// for ( BoxScriptGrammar.PreannotationContext annotation : preannotations ) {
	// annotations.add( toAst( file, annotation ) );
	// }
	// for ( BoxScriptGrammar.PostannotationContext annotation : postannotations ) {
	// annotations.add( toAst( file, annotation ) );
	// }
	//
	// if ( functionSignature.functionParamList() != null ) {
	// for ( BoxScriptGrammar.FunctionParamContext arg : functionSignature.functionParamList().functionParam() ) {
	// BoxArgumentDeclaration argDeclaration = toAst( file, arg );
	// /* Resolve annotations @name.key "value" */
	// for ( BoxAnnotation pre : annotations ) {
	// String prename = pre.getKey().getValue();
	// if ( prename.toLowerCase().startsWith( argDeclaration.getName().toLowerCase() ) ) {
	// if ( prename.indexOf( '.' ) > -1 ) {
	// prename = pre.getKey().getValue().substring( 0, pre.getKey().getValue().indexOf( "." ) );
	//
	// } else {
	// prename = "hint";
	// }
	// BoxFQN key = new BoxFQN(
	// pre.getKey().getValue().substring( pre.getKey().getValue().indexOf( "." ) + 1 ), pre.getPosition(),
	// pre.getSourceText()
	// );
	// argDeclaration.getAnnotations().add(
	// new BoxAnnotation( key, pre.getValue(), pre.getPosition(), pre.getSourceText() )
	// );
	// annToRemove.add( pre );
	// }
	// }
	// args.add( argDeclaration );
	// }
	// }
	//
	// if ( functionSignature.returnType() != null ) {
	// String targetType = functionSignature.returnType().getText();
	// BoxType boxType = BoxType.fromString( targetType );
	// String fqn = boxType.equals( BoxType.Fqn ) ? targetType : null;
	// returnType = new BoxReturnType( boxType, fqn, getPosition( functionSignature.returnType() ), getSourceText( functionSignature.returnType() ) );
	// }
	// if ( statementBlock != null ) {
	// body = new ArrayList<>();
	// body.addAll( toAstStatementBlockAsList( file, statementBlock ) );
	// }
	// annotations.removeAll( annToRemove );
	//
	// return new BoxFunctionDeclaration( accessModifier, modifiers, name, returnType, args, annotations, documentation, body, getPosition( node ),
	// getSourceText( node ) );
	// }
	//
	// protected BoxScript toAst( File file, BoxScriptGrammar.ScriptContext rule, Token firstToken ) {
	// List<BoxStatement> statements = new ArrayList<>();
	//
	// rule.functionOrStatement().forEach( stmt -> {
	// statements.add( toAst( file, stmt ) );
	// } );
	// // Force the script to start at the top of the file so doc comments associate with functions correctly
	// return new BoxScript( statements, getPositionStartingAt( rule, firstToken ), getSourceText( rule ) );
	// }
	//


	/**
	 * Converts the FunctionOrStatement parser rule to the corresponding AST node
	 *
	 * @param file source file, if any
	 * @param node ANTLR FunctionOrStatementContext rule
	 *
	 * @return the corresponding AST BoxStatement
	 *
	 * @see BoxStatement
	 */
	private BoxStatement toAst( File file, BoxScriptGrammar.FunctionOrStatementContext node ) {
		if ( node.function() != null ) {
			return toAst( file, node.function() );
		} else if ( node.statement() != null ) {
			return toAst( file, node.statement() );
		} else {
			issues.add( new Issue( "Function or statement not implemented", getPosition( node ) ) );
			return null;
		}
	}

	//
	// /**
	// * Converts the Statement parser rule to the corresponding AST node
	// *
	// * @param file source file, if any
	// * @param node ANTLR StatementContext rule
	// *
	// * @return the corresponding AST BoxStatement
	// *
	// * @see BoxStatement
	// */
	// private BoxStatement toAst( File file, BoxScriptGrammar.StatementContext node ) {
	// if ( node.simpleStatement() != null ) {
	// return toAst( file, node.simpleStatement() );
	// } else if ( node.if_() != null ) {
	// return toAst( file, node.if_() );
	// } else if ( node.while_() != null ) {
	// return toAst( file, node.while_() );
	// } else if ( node.do_() != null ) {
	// return toAst( file, node.do_() );
	// } else if ( node.switch_() != null ) {
	// return toAst( file, node.switch_() );
	// } else if ( node.for_() != null ) {
	// return toAst( file, node.for_() );
	// } else if ( node.try_() != null ) {
	// return toAst( file, node.try_() );
	// } else if ( node.componentIsland() != null ) {
	// return toAst( file, node.componentIsland() );
	// } else if ( node.component() != null ) {
	// return toAst( file, node.component() );
	// } else if ( node.include() != null ) {
	// return toAst( file, node.include() );
	// } else if ( node.statementBlock() != null ) {
	// return toAst( file, node.statementBlock() );
	// } else if ( node.importStatement() != null ) {
	// return toAst( file, node.importStatement() );
	// } else {
	// issues.add( new Issue( "Statement not implemented", getPosition( node ) ) );
	// return null;
	// }
	// }
	//
	// private BoxStatement toAst( File file, ComponentContext node ) {
	// List<BoxStatement> body = null;
	// String componentName = node.componentName().getText();
	// List<BoxAnnotation> attributes = new ArrayList<>();
	//
	// if ( node.componentAttributes() != null && node.componentAttributes().componentAttribute() != null ) {
	// for ( var attr : node.componentAttributes().componentAttribute() ) {
	// attributes.add( toAstAnnotation( file, attr ) );
	// }
	// }
	//
	// // Special check for param's script shortcut
	// if ( componentName.equalsIgnoreCase( "param" ) ) {
	// // If there is only one attribute and it is not name= and has a value, then we need to convert it to a name/value pair
	// // Ex: param foo="bar";
	// // Becomes: param name="foo" default="bar";
	// if ( attributes.size() == 1 && !attributes.get( 0 ).getKey().getValue().equalsIgnoreCase( "name" ) && attributes.get( 0 ).getValue() != null ) {
	// List<BoxAnnotation> newAttributes = new ArrayList<>();
	// newAttributes.add(
	// new BoxAnnotation(
	// new BoxFQN( "name", getPosition( node ), "name" ),
	// new BoxStringLiteral( attributes.get( 0 ).getKey().getValue(), attributes.get( 0 ).getKey().getPosition(),
	// attributes.get( 0 ).getKey().getSourceText() ),
	// attributes.get( 0 ).getKey().getPosition(),
	// "name=\"" + attributes.get( 0 ).getKey().getSourceText() + "\""
	// )
	// );
	// newAttributes.add(
	// new BoxAnnotation(
	// new BoxFQN( "default", attributes.get( 0 ).getValue().getPosition(), "default" ),
	// attributes.get( 0 ).getValue(),
	// attributes.get( 0 ).getValue().getPosition(),
	// "default=" + attributes.get( 0 ).getValue().getSourceText()
	// )
	// );
	// attributes = newAttributes;
	// // If there are two attributes, the first one has a null value, and none of them are named "name"
	// // Ex: param String foo="bar";
	// // Becomes: param type="String" name="foo" default="bar";
	// // Ex: param String foo;
	// // Becomes: param type="String" name="foo";
	// } else if ( attributes.size() == 2 && attributes.get( 0 ).getValue() == null && !attributes.get( 0 ).getKey().getValue().equalsIgnoreCase( "name" )
	// && !attributes.get( 1 ).getKey().getValue().equalsIgnoreCase( "name" ) ) {
	// List<BoxAnnotation> newAttributes = new ArrayList<>();
	// newAttributes.add(
	// new BoxAnnotation(
	// new BoxFQN( "type", getPosition( node ), "type" ),
	// new BoxStringLiteral( attributes.get( 0 ).getKey().getValue(), attributes.get( 0 ).getKey().getPosition(),
	// attributes.get( 0 ).getKey().getSourceText() ),
	// attributes.get( 0 ).getKey().getPosition(),
	// "type=\"" + attributes.get( 0 ).getKey().getSourceText() + "\""
	// )
	// );
	// newAttributes.add(
	// new BoxAnnotation(
	// new BoxFQN( "name", getPosition( node ), "name" ),
	// new BoxStringLiteral( attributes.get( 1 ).getKey().getValue(), attributes.get( 1 ).getKey().getPosition(),
	// attributes.get( 1 ).getKey().getSourceText() ),
	// attributes.get( 1 ).getKey().getPosition(),
	// "name=" + attributes.get( 1 ).getKey().getSourceText()
	// )
	// );
	// // Only if there is a default
	// if ( attributes.get( 1 ).getValue() != null ) {
	// newAttributes.add(
	// new BoxAnnotation(
	// new BoxFQN( "default", attributes.get( 1 ).getValue().getPosition(), "default" ),
	// attributes.get( 1 ).getValue(),
	// attributes.get( 1 ).getValue().getPosition(),
	// "default=" + attributes.get( 1 ).getValue().getSourceText()
	// )
	// );
	// }
	// attributes = newAttributes;
	// }
	// }
	//
	// ComponentDescriptor descriptor = componentService.getComponent( componentName );
	// if ( node.statementBlock() != null ) {
	// if ( descriptor != null && !descriptor.allowsBody() ) {
	// issues.add( new Issue( "The [" + componentName + "] component does not allow a body", getPosition( node ) ) );
	// }
	// body = new ArrayList<>();
	// body.addAll( toAstStatementBlockAsList( file, node.statementBlock() ) );
	// } else if ( descriptor != null && descriptor.requiresBody() ) {
	// issues.add( new Issue( "The [" + componentName + "] component requires a body", getPosition( node ) ) );
	// }
	//
	// // Special check for loop condition to avoid runtime eval
	// if ( componentName.equalsIgnoreCase( "loop" ) ) {
	// for ( var attr : attributes ) {
	// if ( attr.getKey().getValue().equalsIgnoreCase( "condition" ) ) {
	// BoxExpression condition = attr.getValue();
	// if ( condition instanceof BoxStringLiteral str ) {
	// // parse as script expression and update value
	// condition = parseBoxExpression( str.getValue(), condition.getPosition() );
	// }
	// BoxExpression newCondition = new BoxClosure(
	// List.of(),
	// List.of(),
	// new BoxReturn( condition, condition.getPosition(), condition.getSourceText() ),
	// condition.getPosition(),
	// condition.getSourceText() );
	// attr.setValue( newCondition );
	// }
	// }
	// }
	// return new BoxComponent( componentName, attributes, body, 0, getPosition( node ), getSourceText( node ) );
	// }
	//
	// private BoxAnnotation toAstAnnotation( File file, BoxScriptGrammar.ComponentAttributeContext node ) {
	// BoxFQN name = new BoxFQN( node.identifier().getText(), getPosition( node.identifier() ), getSourceText( node.identifier() ) );
	// BoxExpression value = null;
	// if ( node.expression() != null ) {
	// value = toAst( file, node.expression() );
	// }
	// return new BoxAnnotation( name, value, getPosition( node ), getSourceText( node ) );
	// }
	//
	// /**
	// * Converts the ComponentIslandContext parser rule to the corresponding AST node
	// *
	// * @param file source file, if any
	// * @param componentIsland ANTLR ComponentIslandContext rule
	// *
	// * @return the corresponding AST BoxComponentIsland
	// *
	// * @see BoxThrow
	// */
	// private BoxTemplateIsland toAst( File file, ComponentIslandContext componentIsland ) {
	// return new BoxTemplateIsland(
	// parseBoxTemplateStatements(
	// componentIsland.componentIslandBody().getText(),
	// getPosition( componentIsland.componentIslandBody() )
	// ),
	// getPosition( componentIsland.componentIslandBody() ),
	// getSourceText( componentIsland.componentIslandBody() )
	// );
	//
	// }
	//
	// public List<BoxStatement> parseBoxTemplateStatements( String code, Position position ) {
	// try {
	// if ( inOutputBlock ) {
	// code = "<bx:output>" + code + "</bx:output>";
	// }
	// ParsingResult result = new BoxTemplateParser( position.getStart().getLine(), position.getStart().getColumn() )
	// .setSource( sourceToParse )
	// .setSubParser( true )
	// .parse( code );
	// this.comments.addAll( result.getComments() );
	// if ( result.getIssues().isEmpty() ) {
	// BoxNode root = result.getRoot();
	// if ( root instanceof BoxTemplate template ) {
	// return template.getStatements();
	// } else if ( root instanceof BoxStatement statement ) {
	// return List.of( statement );
	// } else {
	// // Could be a BoxClass, which we may actually need to support
	// issues.add( new Issue( "Unexpected root node type [" + root.getClass().getName() + "] in component island.", root.getPosition() ) );
	// return null;
	// }
	// } else {
	// // Add these issues to the main parser
	// issues.addAll( result.getIssues() );
	// return List.of();
	// }
	// } catch ( IOException e ) {
	// throw new BoxRuntimeException( "Error parsing component island: " + code, e );
	// }
	// }
	//

	//
	// /**
	// * Converts the rethrow parser rule to the corresponding AST node
	// *
	// * @param file source file, if any
	// * @param node ANTLR RethrowContext rule
	// *
	// * @return the corresponding AST BoxStatement
	// *
	// * @see BoxThrow
	// */
	// private BoxStatement toAst( File file, BoxScriptGrammar.RethrowContext node ) {
	// return new BoxRethrow( getPosition( node ), getSourceText( node ) );
	// }
	//
	// /**
	// * Converts the throw parser rule to the corresponding AST node
	// *
	// * @param file source file, if any
	// * @param node ANTLR ThrowContext rule
	// *
	// * @return the corresponding AST BoxStatement
	// *
	// * @see BoxThrow
	// */
	// private BoxStatement toAst( File file, BoxScriptGrammar.ThrowContext node ) {
	// BoxExpression expression = toAst( file, node.expression() );
	// return new BoxThrow( expression, getPosition( node ), getSourceText( node ) );
	// }
	//
	// /**
	// * Converts the do parser rule to the corresponding AST node
	// *
	// * @param file source file, if any
	// * @param node ANTLR TryContext rule
	// *
	// * @return the corresponding AST BoxStatement
	// *
	// * @see BoxDo
	// */
	// private BoxStatement toAst( File file, BoxScriptGrammar.DoContext node ) {
	// BoxExpression condition = toAst( file, node.expression() );
	// BoxStatement body = null;
	// String label = null;
	// if ( node.label != null ) {
	// label = node.label.getText();
	// }
	//
	// body = toAst( file, node.statement() );
	// return new BoxDo( label, condition, body, getPosition( node ), getSourceText( node ) );
	// }
	//
	// /**
	// * Converts the try parser rule to the corresponding AST node
	// *
	// * @param file source file, if any
	// * @param node ANTLR TryContext rule
	// *
	// * @return the corresponding AST BoxStatement
	// *
	// * @see BoxTry
	// */
	// private BoxStatement toAst( File file, BoxScriptGrammar.TryContext node ) {
	// List<BoxStatement> tryBody = toAstStatementBlockAsList( file, node.statementBlock() );
	// List<BoxTryCatch> catches = node.catch_().stream().map( it -> toAst( file, it ) ).collect( Collectors.toList() );
	// List<BoxStatement> finallyBody = new ArrayList<>();
	// if ( node.finally_() != null ) {
	// finallyBody.addAll( toAstStatementBlockAsList( file, node.finally_().statementBlock() ) );
	// }
	// return new BoxTry( tryBody, catches, finallyBody, getPosition( node ), getSourceText( node ) );
	// }
	//
	// /**
	// * Converts the catch parser rule to the corresponding AST node
	// *
	// * @param file source file, if any
	// * @param node ANTLR TryContext rule
	// *
	// * @return the corresponding AST BoxStatement
	// *
	// * @see BoxTryCatch
	// */
	// private BoxTryCatch toAst( File file, BoxScriptGrammar.Catch_Context node ) {
	// BoxExpression exception = toAst( file, node.expression() );
	// List<BoxStatement> catchBody = toAstStatementBlockAsList( file, node.statementBlock() );
	//
	// List<BoxExpression> catchTypes = node.catchType().stream().map( ctNode -> {
	// if ( ctNode.fqn() != null ) {
	// return new BoxFQN( ctNode.fqn().getText(), getPosition( ctNode ),
	// getSourceText( ctNode ) );
	// }
	//
	// return toAst( file, ctNode.stringLiteral() );
	// } )
	// .collect( Collectors.toList() );
	//
	// return new BoxTryCatch( catchTypes, exception, catchBody, getPosition( node ), getSourceText( node ) );
	// }
	//
	// /**
	// * Converts the assert parser rule to the corresponding AST node
	// *
	// * @param file source file, if any
	// * @param node ANTLR AssertContext rule
	// *
	// * @return the corresponding AST BoxStatement
	// *
	// * @see BoxAssert
	// */
	// private BoxStatement toAst( File file, BoxScriptGrammar.AssertContext node ) {
	// BoxExpression expression = toAst( file, node.expression() );
	// return new BoxAssert( expression, getPosition( node ), getSourceText( node ) );
	// }
	//
	// /**
	// * Converts the For parser rule to the corresponding AST node
	// *
	// * @param file source file, if any
	// * @param node ANTLR ForContext rule
	// *
	// * @return the corresponding AST BoxStatement
	// *
	// * @see BoxForIn
	// * @see BoxForIndex
	// */
	// private BoxStatement toAst( File file, BoxScriptGrammar.ForContext node ) {
	// BoxStatement body;
	// String label = null;
	// if ( node.label != null ) {
	// label = node.label.getText();
	// }
	//
	// body = toAst( file, node.statement() );
	//
	// if ( node.IN() != null ) {
	// BoxExpression variable = toAst( file, node.accessExpression() );
	// Boolean hasVar = node.VAR() != null;
	// BoxExpression collection = toAst( file, node.expression() );
	//
	// return new BoxForIn( label, variable, collection, body, hasVar, getPosition( node ), getSourceText( node ) );
	// }
	// BoxExpression initializer = null;
	// BoxExpression condition = null;
	// BoxExpression step = null;
	// if ( node.forAssignment() != null ) {
	// initializer = toAst( file, node.forAssignment().expression() );
	// }
	// if ( node.forCondition() != null ) {
	// condition = toAst( file, node.forCondition().expression() );
	// }
	// if ( node.forIncrement() != null ) {
	// step = toAst( file, node.forIncrement().expression() );
	// }
	//
	// return new BoxForIndex( label, initializer, condition, step, body, getPosition( node ), getSourceText( node ) );
	// }
	//
	// /**
	// * Converts the Switch parser rule to the corresponding AST node
	// *
	// * @param file source file, if any
	// * @param node ANTLR SwitchContext rule
	// *
	// * @return the corresponding AST BoxStatement
	// *
	// * @see BoxSwitch
	// */
	// private BoxStatement toAst( File file, BoxScriptGrammar.SwitchContext node ) {
	// BoxExpression condition = toAst( file, node.expression() );
	// List<BoxSwitchCase> cases = new ArrayList<>();
	// for ( BoxScriptGrammar.CaseContext c : node.case_() ) {
	// cases.add( toAst( file, c ) );
	// }
	// return new BoxSwitch( condition, cases, getPosition( node ), getSourceText( node ) );
	// }
	//
	// /**
	// * Converts the Case parser rule to the corresponding AST node
	// *
	// * @param file source file, if any
	// * @param node ANTLR CaseContext rule
	// *
	// * @return the corresponding AST BoxStatement
	// *
	// * @see BoxSwitchCase
	// */
	// private BoxSwitchCase toAst( File file, BoxScriptGrammar.CaseContext node ) {
	// BoxExpression expr = null;
	// if ( node.expression() != null ) {
	// expr = toAst( file, node.expression() );
	// }
	//
	// List<BoxStatement> statements = new ArrayList<>();
	// if ( node.statement() != null ) {
	// for ( var statement : node.statement() ) {
	// statements.add( toAst( file, statement ) );
	// }
	// }
	// return new BoxSwitchCase( expr, null, statements, getPosition( node ), getSourceText( node ) );
	// }
	//
	// /**
	// * Converts the Continue parser rule to the corresponding AST node
	// *
	// * @param file source file, if any
	// * @param node ANTLR ContinueContext rule
	// *
	// * @return the corresponding AST BoxStatement
	// *
	// * @see BoxContinue
	// */
	// private BoxStatement toAst( File file, BoxScriptGrammar.ContinueContext node ) {
	// String label = null;
	// if ( node.identifier() != null ) {
	// label = node.identifier().getText();
	// }
	// return new BoxContinue( label, getPosition( node ), getSourceText( node ) );
	// }
	//
	// /**
	// * Converts the Break parser rule to the corresponding AST node
	// *
	// * @param file source file, if any
	// * @param node ANTLR BreakContext rule
	// *
	// * @return the corresponding AST BoxStatement
	// *
	// * @see BoxBreak
	// */
	// private BoxStatement toAst( File file, BoxScriptGrammar.BreakContext node ) {
	// String label = null;
	// if ( node.identifier() != null ) {
	// label = node.identifier().getText();
	// }
	// return new BoxBreak( label, getPosition( node ), getSourceText( node ) );
	// }
	//
	// /**
	// * Converts the While parser rule to the corresponding AST node
	// *
	// * @param file source file, if any
	// * @param node ANTLR WhileContext rule
	// *
	// * @return the corresponding AST BoxStatement
	// *
	// * @see BoxWhile
	// */
	// private BoxStatement toAst( File file, BoxScriptGrammar.WhileContext node ) {
	// BoxExpression condition = toAst( file, node.condition );
	// BoxStatement body;
	//
	// String label = null;
	// if ( node.label != null ) {
	// label = node.label.getText();
	// }
	//
	// body = toAst( file, node.statement() );
	// return new BoxWhile( label, condition, body, getPosition( node ), getSourceText( node ) );
	// }
	//
	// /**
	// * Converts the IfContext parser rule to the corresponding AST node
	// *
	// * @param file source file, if any
	// *
	// * @return the corresponding AST BoxIfElse
	// *
	// * @see BoxIfElse
	// */
	// private BoxIfElse toAst( File file, BoxScriptGrammar.IfContext node ) {
	// BoxExpression condition = toAst( file, node.expression() );
	// BoxStatement thenBody;
	// BoxStatement elseBody = null;
	//
	// thenBody = toAst( file, node.ifStmt );
	// if ( node.elseStmt != null ) {
	// elseBody = toAst( file, node.elseStmt );
	// }
	// return new BoxIfElse( condition, thenBody, elseBody, getPosition( node ), getSourceText( node ) );
	// }
	//
	// /**
	// * Converts the StatementBlock parser rule to the corresponding AST node
	// *
	// * @param file source file, if any
	// * @param node ANTLR BreakContext rule
	// *
	// * @return the list of the corresponding AST BoxStatement subclasses in the block
	// *
	// * @see BoxStatement
	// */
	// private List<BoxStatement> toAstStatementBlockAsList( File file, BoxScriptGrammar.StatementBlockContext node ) {
	// return node.statement().stream().map( stmt -> toAst( file, stmt ) ).collect( Collectors.toList() );
	// }
	//
	// private BoxStatement toAst( File file, BoxScriptGrammar.StatementBlockContext node ) {
	// return new BoxStatementBlock( toAstStatementBlockAsList( file, node ), getPosition( node ), getSourceText( node ) );
	// }
	//
	// /**
	// * Converts the SimpleStatement parser rule to the corresponding AST node.
	// * The SimpleStatement contains rules of an Expression statement
	// *
	// * @param file source file, if any
	// * @param node ANTLR SimpleStatementContext rule
	// *
	// * @return the corresponding AST BoxStatement subclass
	// *
	// * @see BoxStatement
	// */
	// private BoxStatement toAst( File file, BoxScriptGrammar.SimpleStatementContext node ) {
	//
	// if ( node.assert_() != null ) {
	// return toAst( file, node.assert_() );
	// } else if ( node.return_() != null ) {
	// BoxExpression expr = null;
	// if ( node.return_().expression() != null ) {
	// expr = toAst( file, node.return_().expression() );
	// }
	// return new BoxReturn( expr, getPosition( node ), getSourceText( node ) );
	// } else if ( node.incrementDecrementStatement() != null ) {
	// return toAst( file, node.incrementDecrementStatement() );
	// } else if ( node.expression() != null ) {
	// BoxExpression expr = toAst( file, node.expression() );
	// return new BoxExpressionStatement( expr, getPosition( node ), getSourceText( node ) );
	// } else if ( node.break_() != null ) {
	// return toAst( file, node.break_() );
	// } else if ( node.continue_() != null ) {
	// return toAst( file, node.continue_() );
	// } else if ( node.rethrow() != null ) {
	// return toAst( file, node.rethrow() );
	// } else if ( node.throw_() != null ) {
	// return toAst( file, node.throw_() );
	// } else if ( node.param() != null ) {
	// return toAst( file, node.param() );
	// }
	//
	// issues.add( new Issue( "Simple statement not implemented", getPosition( node ) ) );
	// return null;
	//
	// }
	//
	// private BoxStatement toAst( File file, ParamContext node ) {
	// BoxExpression type = null;
	// BoxExpression defaultValue = null;
	// if ( node.type() != null ) {
	// type = new BoxStringLiteral( node.type().getText(), getPosition( node.type() ), getSourceText( node.type() ) );
	// }
	// if ( node.expression() != null ) {
	// defaultValue = toAst( file, node.expression() );
	// }
	// return new BoxParam(
	// new BoxStringLiteral( node.accessExpression().getText(), getPosition( node.accessExpression() ), getSourceText( node.accessExpression() ) ),
	// type,
	// defaultValue,
	// getPosition( node ),
	// getSourceText( node )
	// );
	// }
	//
	// /**
	// * Converts the IncrementDecrementStatement parser rule to the corresponding AST node.
	// *
	// * @param file source file, if any
	// * @param node ANTLR IncrementDecrementStatementContext rule
	// *
	// * @return the corresponding AST BoxStatement subclass
	// *
	// * @see
	// */
	// private BoxStatement toAst( File file, BoxScriptGrammar.IncrementDecrementStatementContext node ) {
	// if ( node instanceof BoxScriptGrammar.PostIncrementContext ) {
	// BoxScriptGrammar.PostIncrementContext ctx = ( BoxScriptGrammar.PostIncrementContext ) node;
	// BoxExpression expr = toAst( file, ctx.accessExpression() );
	// BoxUnaryOperation post = new BoxUnaryOperation( expr, BoxUnaryOperator.PostPlusPlus, getPosition( node ),
	// getSourceText( node ) );
	// return new BoxExpressionStatement( post, getPosition( node ), getSourceText( node ) );
	// }
	// if ( node instanceof BoxScriptGrammar.PostDecrementContext ) {
	// BoxScriptGrammar.PostDecrementContext ctx = ( BoxScriptGrammar.PostDecrementContext ) node;
	// BoxExpression expr = toAst( file, ctx.accessExpression() );
	// BoxUnaryOperation post = new BoxUnaryOperation( expr, BoxUnaryOperator.PostMinusMinus, getPosition( node ),
	// getSourceText( node ) );
	// return new BoxExpressionStatement( post, getPosition( node ), getSourceText( node ) );
	// }
	// if ( node instanceof BoxScriptGrammar.PreIncrementContext ) {
	// BoxScriptGrammar.PreIncrementContext ctx = ( BoxScriptGrammar.PreIncrementContext ) node;
	// BoxExpression expr = toAst( file, ctx.accessExpression() );
	// BoxUnaryOperation post = new BoxUnaryOperation( expr, BoxUnaryOperator.PrePlusPlus, getPosition( node ),
	// getSourceText( node ) );
	// return new BoxExpressionStatement( post, getPosition( node ), getSourceText( node ) );
	// }
	// if ( node instanceof BoxScriptGrammar.PreDecremenentContext ) {
	// BoxScriptGrammar.PreDecremenentContext ctx = ( BoxScriptGrammar.PreDecremenentContext ) node;
	// BoxExpression expr = toAst( file, ctx.accessExpression() );
	// BoxUnaryOperation post = new BoxUnaryOperation( expr, BoxUnaryOperator.PreMinusMinus, getPosition( node ),
	// getSourceText( node ) );
	// return new BoxExpressionStatement( post, getPosition( node ), getSourceText( node ) );
	// }
	// issues.add( new Issue( "Increment/Decrement not implemented", getPosition( node ) ) );
	// return null;
	// }
	//
	// /**
	// * Converts the AccessExpression parser rule to the corresponding AST node.
	// *
	// * @param file source file, if any
	// * @param node ANTLR AccessExpressionContext rule
	// *
	// * @return the corresponding AST BoxExpression subclass
	// *
	// * @see BoxIdentifier
	// * @see BoxArrayAccess
	// * @see BoxDotAccess
	// */
	// private BoxExpression toAst (File file, BoxScriptGrammar.AccessExpressionContext node ){
	// BoxExpression expr = toAst(file, node.objectExpression());
	// // loop over children
	// for (int i = 0; i < node.getChildCount(); i++) {
	// ParseTree child = node.getChild(i);
	// if (child instanceof BoxScriptGrammar.DotAccessContext dotAccess) {
	// BoxExpression access;
	// // Any reserved keywords like scopes on the accessed after a dot is just a keyword.
	// if (dotAccess.identifier() != null && dotAccess.identifier().reservedKeyword() != null) {
	// BoxScriptGrammar.ReservedKeywordContext keyword = dotAccess.identifier().reservedKeyword();
	// access = new BoxIdentifier(keyword.getText(), getPosition(keyword), getSourceText(keyword));
	// } else if (dotAccess.identifier() != null) {
	// access = toAst(file, dotAccess.identifier());
	// } else {
	// // turn .123 into 123 as an integer literal
	// access = new BoxIntegerLiteral(dotAccess.floatLiteralDecimalOnly().getText().substring(1), getPosition(dotAccess.floatLiteralDecimalOnly()), getSourceText(dotAccess.floatLiteralDecimalOnly()));
	// }
	// expr = new BoxDotAccess(expr, dotAccess.QM() != null, access, getPosition(dotAccess), getSourceText(dotAccess));
	// } else if (child instanceof BoxScriptGrammar.ArrayAccessContext arrayAccess) {
	// expr = new BoxArrayAccess(expr, false, toAst(file, arrayAccess.expression()), getPosition(arrayAccess), getSourceText(arrayAccess));
	// } else if (child instanceof BoxScriptGrammar.MethodInvokationContext methodInvokation) {
	// if (methodInvokation.functionInvokation() != null) {
	// List<BoxArgument> args = toAst(file, methodInvokation.functionInvokation().invokationExpression().argumentList());
	// BoxScriptGrammar.IdentifierContext id = methodInvokation.functionInvokation().identifier();
	// BoxExpression name = new BoxIdentifier(id.getText(), getPosition(id), getSourceText(id));
	//
	// expr = new BoxMethodInvocation(name, expr, args, methodInvokation.QM() != null, true, getPosition(methodInvokation), getSourceText(methodInvokation));
	// } else if (methodInvokation.arrayAccess() != null) {
	// List<BoxArgument> args = toAst(file, methodInvokation.invokationExpression().argumentList());
	// BoxExpression name = toAst(file, methodInvokation.arrayAccess().expression());
	// expr = new BoxMethodInvocation(name, expr, args, false, false, getPosition(methodInvokation), getSourceText(methodInvokation));
	// } else {
	// issues.add(new Issue("Unimplemented method invocation does not use function invocation or array access rules", getPosition(node)));
	// return null;
	// }
	// } else if (child instanceof BoxScriptGrammar.InvokationExpressionContext invokationExpression) {
	// expr = new BoxExpressionInvocation(expr, toAst(file, invokationExpression.argumentList()), getPosition(invokationExpression), getSourceText(invokationExpression));
	// }
	// }
	// return expr;
	// }
	//
	// /**
	// * Converts the IdentifierContext parser rule to the corresponding AST node.
	// *
	// * @param file source file, if any
	// * @param node ANTLR IdentifierContext rule
	// *
	// * @return the corresponding AST BoxIdentifier or a BoxScope if it is a reserved keyword
	// *
	// * @see BoxScope
	// * @see BoxIdentifier
	// */
	// private BoxExpression toAst( File file, BoxScriptGrammar.IdentifierContext node ) {
	// BoxScriptGrammar.ReservedKeywordContext keyword = node.reservedKeyword();
	// if ( keyword != null && keyword.scope() != null ) {
	// return toAst( file, keyword.scope() );
	// }
	// String name = "";
	// if ( node.IDENTIFIER() != null ) {
	// name = node.IDENTIFIER().getText();
	// } else if ( node.reservedKeyword() != null ) {
	// name = node.reservedKeyword().getText();
	// }
	// return new BoxIdentifier( name, getPosition( node ), getSourceText( node ) );
	// }
	//
	// /**
	// * Converts the Scope parser rule to the corresponding AST node.
	// *
	// * @param file source file, if any
	// * @param node ANTLR ScopeContext rule
	// *
	// * @return corresponding AST BoxScope
	// *
	// * @see BoxScope for the reserved keywords used to identify a scope
	// */
	// private BoxExpression toAst( File file, BoxScriptGrammar.ScopeContext node ) {
	// return new BoxScope( node.getText(), getPosition( node ), getSourceText( node ) );
	// }
	//
	// /**
	// * Converts the string literal into a BoxStringLiteral or BoxStringInterpolation
	// *
	// * @param file source file, if any
	// * @param expression ANTLR StringLiteralContext rule
	// *
	// * @return corresponding AST BoxExpr subclass
	// *
	// * @see BoxExpression subclasses
	// */
	// private BoxExpression toAst( File file, BoxScriptGrammar.StringLiteralContext expression ) {
	// String quoteChar = expression.getText().substring( 0, 1 );
	// if ( expression.expression().isEmpty() ) {
	// String s = expression.getText();
	// // trim leading and trailing quote
	// s = s.substring( 1, s.length() - 1 );
	// return new BoxStringLiteral(
	// escapeStringLiteral( quoteChar, s ),
	// getPosition( expression ),
	// getSourceText( expression )
	// );
	//
	// } else {
	// List<BoxExpression> parts = new ArrayList<>();
	// expression.children.forEach( it -> {
	// if ( it != null && it instanceof BoxScriptGrammar.StringLiteralPartContext ) {
	// parts.add( new BoxStringLiteral( escapeStringLiteral( quoteChar, getSourceText( ( ParserRuleContext ) it ) ),
	// getPosition( ( ParserRuleContext ) it ),
	// getSourceText( ( ParserRuleContext ) it ) ) );
	// }
	// if ( it != null && it instanceof BoxScriptGrammar.ExpressionContext ) {
	// parts.add( toAst( file, ( BoxScriptGrammar.ExpressionContext ) it ) );
	// }
	// } );
	// return new BoxStringInterpolation( parts, getPosition( expression ), getSourceText( expression ) );
	// }
	// }
	//
	// /**
	// * Escape double up quotes and pounds in a string literal
	// *
	// * @param quoteChar the quote character used to surround the string
	// * @param string the string to escape
	// *
	// * @return the escaped string
	// */
	// private String escapeStringLiteral( String quoteChar, String string ) {
	// String escaped = string.replace( "##", "#" );
	// return escaped.replace( quoteChar + quoteChar, quoteChar );
	// }
	//
	// /**
	// * Converts the Expression parser rule to the corresponding AST node.
	// * The operator precedence resolved in the ANTLR grammar
	// *
	// * @param file source file, if any
	// * @param expression ANTLR ExpressionContext rule
	// *
	// * @return corresponding AST BoxExpr subclass
	// *
	// * @see BoxExpression subclasses
	// * @see BoxBinaryOperator
	// */
	// private BoxExpression toAst( File file, BoxScriptGrammar.ExpressionContext expression ) {
	// if ( expression.ternary() != null ) {
	// BoxExpression condition = toAst( file, expression.ternary().notTernaryExpression() );
	// BoxExpression whenTrue = toAst( file, expression.ternary().expression( 0 ) );
	// BoxExpression whenFalse = toAst( file, expression.ternary().expression( 1 ) );
	// return new BoxTernaryOperation( condition, whenTrue, whenFalse, getPosition( expression ), getSourceText( expression ) );
	// }
	// if ( expression.assignment() != null ) {
	// return toAst( file, expression.assignment() );
	// } else if ( expression.notTernaryExpression() != null ) {
	// return toAst( file, expression.notTernaryExpression() );
	// }
	//
	// issues.add( new Issue( "Expression not implemented", getPosition( expression ) ) );
	// return null;
	// }
	//
	// private BoxExpression toAst( File file, NotTernaryExpressionContext expression ) {
	// if ( expression.accessExpression() != null ) {
	// return toAst( file, expression.accessExpression() );
	// } else if ( expression.and() != null ) {
	// BoxExpression left = toAst( file, expression.notTernaryExpression( 0 ) );
	// BoxExpression right = toAst( file, expression.notTernaryExpression( 1 ) );
	// return new BoxBinaryOperation( left, BoxBinaryOperator.And, right, getPosition( expression ), getSourceText( expression ) );
	// } else if ( expression.or() != null ) {
	// BoxExpression left = toAst( file, expression.notTernaryExpression( 0 ) );
	// BoxExpression right = toAst( file, expression.notTernaryExpression( 1 ) );
	// return new BoxBinaryOperation( left, BoxBinaryOperator.Or, right, getPosition( expression ), getSourceText( expression ) );
	// } else if ( expression.PLUS() != null ) {
	// BoxExpression left = toAst( file, expression.notTernaryExpression( 0 ) );
	// BoxExpression right = toAst( file, expression.notTernaryExpression( 1 ) );
	// return new BoxBinaryOperation( left, BoxBinaryOperator.Plus, right, getPosition( expression ), getSourceText( expression ) );
	// } else if ( expression.MINUS() != null ) {
	// BoxExpression left = toAst( file, expression.notTernaryExpression( 0 ) );
	// BoxExpression right = toAst( file, expression.notTernaryExpression( 1 ) );
	// return new BoxBinaryOperation( left, BoxBinaryOperator.Minus, right, getPosition( expression ), getSourceText( expression ) );
	// } else if ( expression.STAR() != null ) {
	// BoxExpression left = toAst( file, expression.notTernaryExpression( 0 ) );
	// BoxExpression right = toAst( file, expression.notTernaryExpression( 1 ) );
	// return new BoxBinaryOperation( left, BoxBinaryOperator.Star, right, getPosition( expression ), getSourceText( expression ) );
	// } else if ( expression.SLASH() != null ) {
	// BoxExpression left = toAst( file, expression.notTernaryExpression( 0 ) );
	// BoxExpression right = toAst( file, expression.notTernaryExpression( 1 ) );
	// return new BoxBinaryOperation( left, BoxBinaryOperator.Slash, right, getPosition( expression ), getSourceText( expression ) );
	// } else if ( expression.BACKSLASH() != null ) {
	// BoxExpression left = toAst( file, expression.notTernaryExpression( 0 ) );
	// BoxExpression right = toAst( file, expression.notTernaryExpression( 1 ) );
	// return new BoxBinaryOperation( left, BoxBinaryOperator.Backslash, right, getPosition( expression ), getSourceText( expression ) );
	// } else if ( expression.unary() != null ) {
	// return toAst( file, expression.unary() );
	// } else if ( expression.POWER() != null ) {
	// BoxExpression left = toAst( file, expression.notTernaryExpression( 0 ) );
	// BoxExpression right = toAst( file, expression.notTernaryExpression( 1 ) );
	// return new BoxBinaryOperation( left, BoxBinaryOperator.Power, right, getPosition( expression ), getSourceText( expression ) );
	// } else if ( expression.XOR() != null ) {
	// BoxExpression left = toAst( file, expression.notTernaryExpression( 0 ) );
	// BoxExpression right = toAst( file, expression.notTernaryExpression( 1 ) );
	// return new BoxBinaryOperation( left, BoxBinaryOperator.Xor, right, getPosition( expression ), getSourceText( expression ) );
	// } else if ( expression.PERCENT() != null || expression.MOD() != null ) {
	// BoxExpression left = toAst( file, expression.notTernaryExpression( 0 ) );
	// BoxExpression right = toAst( file, expression.notTernaryExpression( 1 ) );
	// return new BoxBinaryOperation( left, BoxBinaryOperator.Mod, right, getPosition( expression ), getSourceText( expression ) );
	// } else if ( expression.instanceOf() != null ) {
	// BoxExpression left = toAst( file, expression.notTernaryExpression( 0 ) );
	// BoxExpression right = toAst( file, expression.notTernaryExpression( 1 ) );
	// return new BoxBinaryOperation( left, BoxBinaryOperator.InstanceOf, right, getPosition( expression ), getSourceText( expression ) );
	// } else if ( expression.TEQ() != null ) {
	// BoxExpression left = toAst( file, expression.notTernaryExpression( 0 ) );
	// BoxExpression right = toAst( file, expression.notTernaryExpression( 1 ) );
	// return new BoxComparisonOperation( left, BoxComparisonOperator.TEqual, right, getPosition( expression ), getSourceText( expression ) );
	// } else if ( expression.neq() != null ) {
	// BoxExpression left = toAst( file, expression.notTernaryExpression( 0 ) );
	// BoxExpression right = toAst( file, expression.notTernaryExpression( 1 ) );
	// return new BoxComparisonOperation( left, BoxComparisonOperator.NotEqual, right, getPosition( expression ), getSourceText( expression ) );
	// } else if ( expression.gt() != null ) {
	// BoxExpression left = toAst( file, expression.notTernaryExpression( 0 ) );
	// BoxExpression right = toAst( file, expression.notTernaryExpression( 1 ) );
	// return new BoxComparisonOperation( left, BoxComparisonOperator.GreaterThan, right, getPosition( expression ), getSourceText( expression ) );
	// } else if ( expression.gte() != null ) {
	// BoxExpression left = toAst( file, expression.notTernaryExpression( 0 ) );
	// BoxExpression right = toAst( file, expression.notTernaryExpression( 1 ) );
	// return new BoxComparisonOperation( left, BoxComparisonOperator.GreaterThanEquals, right, getPosition( expression ), getSourceText( expression ) );
	// } else if ( expression.lt() != null ) {
	// BoxExpression left = toAst( file, expression.notTernaryExpression( 0 ) );
	// BoxExpression right = toAst( file, expression.notTernaryExpression( 1 ) );
	// return new BoxComparisonOperation( left, BoxComparisonOperator.LessThan, right, getPosition( expression ), getSourceText( expression ) );
	// } else if ( expression.lte() != null ) {
	// BoxExpression left = toAst( file, expression.notTernaryExpression( 0 ) );
	// BoxExpression right = toAst( file, expression.notTernaryExpression( 1 ) );
	// return new BoxComparisonOperation( left, BoxComparisonOperator.LesslThanEqual, right, getPosition( expression ), getSourceText( expression ) );
	// } else if ( expression.eq() != null || expression.IS() != null ) {
	// BoxExpression left = toAst( file, expression.notTernaryExpression( 0 ) );
	// BoxExpression right = toAst( file, expression.notTernaryExpression( 1 ) );
	// return new BoxComparisonOperation( left, BoxComparisonOperator.Equal, right, getPosition( expression ), getSourceText( expression ) );
	// } else if ( expression.AMPERSAND() != null ) {
	// List<BoxExpression> parts = new ArrayList<>();
	// BoxScriptGrammar.NotTernaryExpressionContext current = expression;
	// // unwrap nested foo & bar & baz & bum into a single concat node
	// do {
	// parts.add( 0, toAst( file, current.right ) );
	// current = current.left;
	// } while ( current.AMPERSAND() != null );
	// parts.add( 0, toAst( file, current ) );
	//
	// return new BoxStringConcat( parts, getPosition( expression ), getSourceText( expression ) );
	//
	// } else if ( expression.EQV() != null ) {
	// BoxExpression left = toAst( file, expression.notTernaryExpression( 0 ) );
	//
	// BoxExpression right = toAst( file, expression.notTernaryExpression( 1 ) );
	// return new BoxBinaryOperation( left, BoxBinaryOperator.Equivalence, right, getPosition( expression ), getSourceText( expression ) );
	// } else if ( expression.IMP() != null ) {
	// BoxExpression left = toAst( file, expression.notTernaryExpression( 0 ) );
	//
	// BoxExpression right = toAst( file, expression.notTernaryExpression( 1 ) );
	// return new BoxBinaryOperation( left, BoxBinaryOperator.Implies, right, getPosition( expression ), getSourceText( expression ) );
	// } else if ( expression.ELVIS() != null ) {
	// BoxExpression left = toAst( file, expression.notTernaryExpression( 0 ) );
	//
	// BoxExpression right = toAst( file, expression.notTernaryExpression( 1 ) );
	// return new BoxBinaryOperation( left, BoxBinaryOperator.Elvis, right, getPosition( expression ), getSourceText( expression ) );
	// } else if ( expression.notOrBang() != null && expression.CONTAIN() == null ) {
	// BoxExpression expr = toAst( file, expression.notTernaryExpression( 0 ) );
	// return new BoxUnaryOperation( expr, BoxUnaryOperator.Not, getPosition( expression ), getSourceText( expression ) );
	// // return new BoxNegateOperation( expr, BoxNegateOperator.Not, getPosition( expression ), getSourceText( expression ) );
	// } else if ( expression.CONTAINS() != null ) {
	// BoxExpression left = toAst( file, expression.notTernaryExpression( 0 ) );
	// BoxExpression right = toAst( file, expression.notTernaryExpression( 1 ) );
	// return new BoxBinaryOperation( left, BoxBinaryOperator.Contains, right, getPosition( expression ), getSourceText( expression ) );
	// } else if ( expression.CONTAIN() != null && expression.DOES() != null && expression.NOT() != null ) {
	// BoxExpression left = toAst( file, expression.notTernaryExpression( 0 ) );
	// BoxExpression right = toAst( file, expression.notTernaryExpression( 1 ) );
	// return new BoxBinaryOperation( left, BoxBinaryOperator.NotContains, right, getPosition( expression ), getSourceText( expression ) );
	// } else if ( expression.pre != null ) {
	// BoxExpression expr = toAst( file, expression.notTernaryExpression( 0 ) );
	// if ( expression.PLUSPLUS() != null ) {
	// return new BoxUnaryOperation( expr, BoxUnaryOperator.PrePlusPlus, getPosition( expression ), getSourceText( expression ) );
	// }
	// if ( expression.MINUSMINUS() != null ) {
	// return new BoxUnaryOperation( expr, BoxUnaryOperator.PreMinusMinus, getPosition( expression ), getSourceText( expression ) );
	// }
	// } else if ( expression.post != null ) {
	// BoxExpression expr = toAst( file, expression.notTernaryExpression( 0 ) );
	// if ( expression.PLUSPLUS() != null ) {
	// return new BoxUnaryOperation( expr, BoxUnaryOperator.PostPlusPlus, getPosition( expression ), getSourceText( expression ) );
	// }
	// if ( expression.MINUSMINUS() != null ) {
	// return new BoxUnaryOperation( expr, BoxUnaryOperator.PostMinusMinus, getPosition( expression ), getSourceText( expression ) );
	// }
	// } else if ( expression.castAs() != null ) {
	// BoxExpression left = toAst( file, expression.notTernaryExpression( 0 ) );
	// BoxExpression right = toAst( file, expression.notTernaryExpression( 1 ) );
	// return new BoxBinaryOperation( left, BoxBinaryOperator.CastAs, right, getPosition( expression ), getSourceText( expression ) );
	// } else if ( !expression.ICHAR().isEmpty() ) {
	// return toAst( file, expression.notTernaryExpression( 0 ) );
	// } else if ( expression.assignment() != null ) {
	// return toAst( file, expression.assignment() );
	// } else if ( expression.NULL() != null ) {
	// return new BoxNull( getPosition( expression ), getSourceText( expression ) );
	// } else if ( expression.bitwiseSignedLeftShift() != null ) {
	// BoxExpression left = toAst( file, expression.notTernaryExpression( 0 ) );
	// BoxExpression right = toAst( file, expression.notTernaryExpression( 1 ) );
	// return new BoxBinaryOperation( left, BoxBinaryOperator.BitwiseSignedLeftShift, right, getPosition( expression ), getSourceText( expression ) );
	// } else if ( expression.bitwiseSignedRightShift() != null ) {
	// BoxExpression left = toAst( file, expression.notTernaryExpression( 0 ) );
	// BoxExpression right = toAst( file, expression.notTernaryExpression( 1 ) );
	// return new BoxBinaryOperation( left, BoxBinaryOperator.BitwiseSignedRightShift, right, getPosition( expression ), getSourceText( expression ) );
	// } else if ( expression.bitwiseUnsignedRightShift() != null ) {
	// BoxExpression left = toAst( file, expression.notTernaryExpression( 0 ) );
	// BoxExpression right = toAst( file, expression.notTernaryExpression( 1 ) );
	// return new BoxBinaryOperation( left, BoxBinaryOperator.BitwiseUnsignedRightShift, right, getPosition( expression ),
	// getSourceText( expression ) );
	// } else if ( expression.bitwiseAnd() != null ) {
	// BoxExpression left = toAst( file, expression.notTernaryExpression( 0 ) );
	// BoxExpression right = toAst( file, expression.notTernaryExpression( 1 ) );
	// return new BoxBinaryOperation( left, BoxBinaryOperator.BitwiseAnd, right, getPosition( expression ), getSourceText( expression ) );
	// } else if ( expression.bitwiseOr() != null ) {
	// BoxExpression left = toAst( file, expression.notTernaryExpression( 0 ) );
	// BoxExpression right = toAst( file, expression.notTernaryExpression( 1 ) );
	// return new BoxBinaryOperation( left, BoxBinaryOperator.BitwiseOr, right, getPosition( expression ), getSourceText( expression ) );
	// } else if ( expression.bitwiseXOR() != null ) {
	// BoxExpression left = toAst( file, expression.notTernaryExpression( 0 ) );
	// BoxExpression right = toAst( file, expression.notTernaryExpression( 1 ) );
	// return new BoxBinaryOperation( left, BoxBinaryOperator.BitwiseXor, right, getPosition( expression ), getSourceText( expression ) );
	// } else

	// } else if ( expression.staticAccessExpression() != null ) {
	// return toAst( file, expression.staticAccessExpression() );
	// }
	// issues.add( new Issue( "Expression not implemented", getPosition( expression ) ) );
	// return null;
	// }

	//
	// private BoxExpression toAst( File file, BoxScriptGrammar.StaticAccessExpressionContext staticAccessExpression ) {
	// BoxExpression expr = toAst( file, staticAccessExpression.staticObjectExpression() );
	//
	// if ( staticAccessExpression.staticMethodInvokation() != null ) {
	// List<BoxArgument> args = toAst( file,
	// staticAccessExpression.staticMethodInvokation().functionInvokation().invokationExpression().argumentList() );
	// BoxScriptGrammar.IdentifierContext id = staticAccessExpression.staticMethodInvokation().functionInvokation().identifier();
	// BoxIdentifier name = new BoxIdentifier( id.getText(), getPosition( id ), getSourceText( id ) );
	//
	// return new BoxStaticMethodInvocation( name, expr, args, getPosition( staticAccessExpression.staticMethodInvokation() ),
	// getSourceText( staticAccessExpression.staticMethodInvokation() ) );
	//
	// } else if ( staticAccessExpression.staticAccess() != null ) {
	// BoxExpression access;
	// // Any reserved keywords like scopes on the accessed after a dot is just a keyword.
	// if ( staticAccessExpression.staticAccess().identifier() != null ) {
	// access = new BoxIdentifier( staticAccessExpression.staticAccess().identifier().getText(),
	// getPosition( staticAccessExpression.staticAccess().identifier() ), getSourceText( staticAccessExpression.staticAccess().identifier() ) );
	// } else {
	// // turn .123 into 123 as an integer literal
	// access = new BoxIntegerLiteral( staticAccessExpression.staticAccess().floatLiteralDecimalOnly().getText().substring( 1 ),
	// getPosition( staticAccessExpression.staticAccess().floatLiteralDecimalOnly() ),
	// getSourceText( staticAccessExpression.staticAccess().floatLiteralDecimalOnly() ) );
	// }
	// return new BoxStaticAccess( expr, false, access, getPosition( staticAccessExpression.staticAccess() ),
	// getSourceText( staticAccessExpression.staticAccess() ) );
	// } else {
	// issues.add( new Issue( "Unimplemented static method invocation does not use function invocation or array access rules",
	// getPosition( staticAccessExpression ) ) );
	// return null;
	// }
	// }
	//
	// private BoxExpression toAst( File file, BoxScriptGrammar.StaticObjectExpressionContext staticObjectExpression ) {
	// if ( staticObjectExpression.fqn() != null ) {
	// return toAst( file, staticObjectExpression.fqn() );
	// } else if ( staticObjectExpression.identifier() != null ) {
	// return new BoxIdentifier( staticObjectExpression.identifier().getText(), getPosition( staticObjectExpression.identifier() ),
	// getSourceText( staticObjectExpression.identifier() ) );
	// } else {
	// issues.add( new Issue( "Unimplemented static object expression", getPosition( staticObjectExpression ) ) );
	// return null;
	// }
	// }
	//
	// private BoxExpression toAst( File file, AssignmentContext node ) {
	// BoxExpression left = toAst( file, node.assignmentLeft().accessExpression() );
	// BoxExpression right = toAst( file, node.assignmentRight().expression() );
	// BoxAssignmentOperator op = BoxAssignmentOperator.Equal;
	// if ( node.PLUSEQUAL() != null ) {
	// op = BoxAssignmentOperator.PlusEqual;
	// } else if ( node.MINUSEQUAL() != null ) {
	// op = BoxAssignmentOperator.MinusEqual;
	// } else if ( node.STAREQUAL() != null ) {
	// op = BoxAssignmentOperator.StarEqual;
	// } else if ( node.SLASHEQUAL() != null ) {
	// op = BoxAssignmentOperator.SlashEqual;
	// } else if ( node.MODEQUAL() != null ) {
	// op = BoxAssignmentOperator.ModEqual;
	// } else if ( node.CONCATEQUAL() != null ) {
	// op = BoxAssignmentOperator.ConcatEqual;
	// }
	// // In the future, we expect there to be more than just var here, thus the list.
	// List<BoxAssignmentModifier> modifiers = new ArrayList<BoxAssignmentModifier>();
	// if ( node.VAR() != null ) {
	// modifiers.add( BoxAssignmentModifier.VAR );
	// }
	//
	// return new BoxAssignment( left, op, right, modifiers, getPosition( node ), getSourceText( node ) );
	// }
	//
	// /**
	// * Converts the UnaryContext parser rule to the corresponding AST node.
	// *
	// * @param file source file, if any
	// * @param node ANTLR FqnContext rule
	// */
	// private BoxExpression toAst( File file, BoxScriptGrammar.FqnContext node ) {
	// return new BoxFQN( node.getText(), getPosition( node ), getSourceText( node ) );
	// }
	//
	// /**
	// * Converts the UnaryContext parser rule to the corresponding AST node.
	// *
	// * @param file source file, if any
	// * @param node ANTLR UnaryContext rule
	// *
	// * @return corresponding AST BoxUnaryOperation
	// *
	// * @see BoxUnaryOperation
	// * @see BoxUnaryOperator
	// */
	// private BoxExpression toAst( File file, BoxScriptGrammar.UnaryContext node ) {
	//
	// BoxExpression expr = toAst( file, node.expression() );
	// BoxUnaryOperator op = node.MINUS() != null ? BoxUnaryOperator.Minus
	// : ( node.PLUS() != null ? BoxUnaryOperator.Plus : BoxUnaryOperator.BitwiseComplement );
	// if ( expr instanceof BoxBinaryOperation bop ) {
	// return new BoxBinaryOperation(
	// new BoxUnaryOperation( bop.getLeft(), op, getPosition( node ), getSourceText( node ) ),
	// bop.getOperator(),
	// bop.getRight(),
	// bop.getPosition(),
	// bop.getSourceText()
	// );
	// }
	// return new BoxUnaryOperation( expr, op, getPosition( node ), getSourceText( node ) );
	// }
	//
	// /**
	// * Converts the ObjectExpression parser rule to the corresponding AST node. *
	// *
	// * @param file source file, if any
	// * @param node ANTLR ObjectExpressionContext rule
	// *
	// * @return corresponding AST
	// *
	// * @see BoxAccess subclasses
	// * @see BoxIdentifier subclasses
	// */
	// private BoxExpression toAst( File file, BoxScriptGrammar.ObjectExpressionContext node ) {
	// if ( node.LPAREN() != null ) {
	// BoxExpression expr = toAst( file, node.expression() );
	// return new BoxParenthesis( expr, getPosition( node ), getSourceText( node ) );
	// } else if ( node.functionInvokation() != null )
	// return toAst( file, node.functionInvokation() );
	// else if ( node.identifier() != null )
	// return toAst( file, node.identifier() );
	// else if ( node.literalExpression() != null ) {
	// return toAst( file, node.literalExpression() );
	// } else if ( node.new_() != null ) {
	// return toAst( file, node.new_() );
	// }
	//
	// issues.add( new Issue( "Object expression not implemented", getPosition( node ) ) );
	// return null;
	//
	// }
	//
	// /**
	// * Converts the NewContext parser rule to the corresponding AST node. *
	// *
	// * @param file source file, if any
	// * @param node ANTLR NewContext rule
	// *
	// * @return corresponding AST
	// */
	// private BoxExpression toAst( File file, NewContext node ) {
	// BoxExpression expr = null;
	// BoxIdentifier prefix = null;
	// List<BoxArgument> args = toAst( file, node.argumentList() );
	// if ( node.fqn() != null ) {
	// expr = toAst( file, node.fqn() );
	// }
	// if ( node.stringLiteral() != null ) {
	// expr = toAst( file, node.stringLiteral() );
	// }
	// if ( node.identifier() != null ) {
	// var tmp = toAst( file, node.identifier() );
	// if ( tmp instanceof BoxIdentifier bi ) {
	// prefix = bi;
	// } else {
	// prefix = new BoxIdentifier( ( ( BoxScope ) tmp ).getName(), getPosition( node.identifier() ), getSourceText( node.identifier() ) );
	// }
	//
	// }
	// return new BoxNew( prefix, expr, args, getPosition( node ), getSourceText( node ) );
	// }
	//
	// /**
	// * Converts the ObjectExpression parser rule to the corresponding AST node. * @param file
	// *
	// * @param file source file, if any
	// * @param node ANTLR LiteralExpressionContext rule
	// *
	// * @return corresponding AST BoxAccess or an LiteralExpressionContext
	// *
	// * @see BoxAccess subclasses
	// * @see BoxIdentifier subclasses
	// */
	// private BoxExpression toAst( File file, BoxScriptGrammar.LiteralExpressionContext node ) {
	// if ( node.stringLiteral() != null ) {
	// return toAst( file, node.stringLiteral() );
	// }
	// if ( node.integerLiteral() != null ) {
	// return toAst( file, node.integerLiteral() );
	// }
	// if ( node.floatLiteral() != null ) {
	// BoxScriptGrammar.FloatLiteralContext fnode = node.floatLiteral();
	// return new BoxDecimalLiteral(
	// fnode.getText(),
	// getPosition( fnode ),
	// getSourceText( fnode )
	// );
	// }
	// if ( node.booleanLiteral() != null ) {
	// BoxScriptGrammar.BooleanLiteralContext bnode = node.booleanLiteral();
	// return new BoxBooleanLiteral(
	// bnode.getText(),
	// getPosition( bnode ),
	// getSourceText( bnode ) );
	// }
	// if ( node.arrayExpression() != null ) {
	// return toAst( file, node.arrayExpression() );
	// }
	//
	// if ( node.structExpression() != null ) {
	// return toAst( file, node.structExpression() );
	// }
	//
	// issues.add( new Issue( "Literal expression not implemented", getPosition( node ) ) );
	// return null;
	//
	// }
	//
	// /**
	// * Converts the IntegerLiteral parser rule to the corresponding AST node. *
	// *
	// * @param file source file, if any
	// *
	// * @return corresponding AST BoxAccess or an IntegerLiteralContext
	// *
	// */
	// private BoxExpression toAst( File file, BoxScriptGrammar.IntegerLiteralContext integerLiteral ) {
	// return new BoxIntegerLiteral(
	// integerLiteral.getText(),
	// getPosition( integerLiteral ),
	// getSourceText( integerLiteral )
	// );
	// }
	//
	// /**
	// * Converts the Struct Expression parser rule to the corresponding AST node.
	// *
	// * @param file source file, if any
	// * @param node ANTLR ArrayExpressionContext rule
	// *
	// * @return corresponding AST BoxArray
	// *
	// * @see BoxArrayLiteral subclasses
	// */
	// private BoxExpression toAst( File file, BoxScriptGrammar.StructExpressionContext node ) {
	// List<BoxExpression> values = new ArrayList<>();
	// BoxStructType type = node.RBRACKET() != null ? BoxStructType.Ordered : BoxStructType.Unordered;
	// if ( node.structMembers() != null ) {
	// for ( BoxScriptGrammar.StructMemberContext pair : node.structMembers().structMember() ) {
	// if ( pair.stringLiteral() != null ) {
	// values.add( toAst( file, pair.stringLiteral() ) );
	// } else if ( pair.identifier() != null ) {
	// values.add( toAst( file, pair.identifier() ) );
	// } else if ( pair.integerLiteral() != null ) {
	// values.add( toAst( file, pair.integerLiteral() ) );
	// }
	// values.add( toAst( file, pair.expression() ) );
	// }
	// }
	// return new BoxStructLiteral( type, values, getPosition( node ), getSourceText( node ) );
	// }
	//
	// /**
	// * Converts the Array Expression parser rule to the corresponding AST node.
	// *
	// * @param file source file, if any
	// * @param node ANTLR ArrayExpressionContext rule
	// *
	// * @return corresponding AST BoxArray
	// *
	// * @see BoxArrayLiteral subclasses
	// */
	// private BoxExpression toAst( File file, BoxScriptGrammar.ArrayExpressionContext node ) {
	// List<BoxExpression> values = new ArrayList<>();
	// if ( node.arrayValues() != null ) {
	// for ( BoxScriptGrammar.ExpressionContext value : node.arrayValues().expression() ) {
	// values.add( toAst( file, value ) );
	// }
	// }
	// return new BoxArrayLiteral( values, getPosition( node ), getSourceText( node ) );
	// }
	//
	// /**
	// * Converts the Function Invocation parser rule to the corresponding AST node
	// *
	// * @param file source file, if any
	// * @param node ANTLR FunctionInvokationContext rule
	// *
	// * @return corresponding AST BoxFunctionInvocation
	// *
	// * @see BoxFunctionInvocation subclasses
	// * @see BoxArgument subclasses
	// */
	// private BoxExpression toAst( File file, BoxScriptGrammar.FunctionInvokationContext node ) {
	// List<BoxArgument> args = toAst( file, node.invokationExpression().argumentList() );
	// return new BoxFunctionInvocation( node.identifier().getText(),
	// args,
	// getPosition( node ), getSourceText( node ) );
	// }
	//
	// /**
	// * Converts the argument list to the corresponding List of AST nodes
	// *
	// * @param file source file, if any
	// * @param node ANTLR ArgumentListContext rule
	// *
	// * @return corresponding List of AST nodes
	// *
	// */
	// private List<BoxArgument> toAst( File file, BoxScriptGrammar.ArgumentListContext node ) {
	// List<BoxArgument> args = new ArrayList<>();
	// Boolean isNamed = false;
	// if ( node != null ) {
	// for ( BoxScriptGrammar.NamedArgumentContext arg : node.namedArgument() ) {
	// isNamed = true;
	// args.add( toAst( file, arg ) );
	// }
	// for ( BoxScriptGrammar.PositionalArgumentContext arg : node.positionalArgument() ) {
	// if ( isNamed ) {
	// issues.add( new Issue( "You cannot mix named and positional arguments", getPosition( arg ) ) );
	// }
	// args.add( toAst( file, arg ) );
	// }
	// }
	// return args;
	// }
	//
	// /**
	// * Converts the PositionalArgumentContext parser rule to the corresponding AST node.
	// *
	// * @param file source file, if any
	// * @param node ANTLR PositionalArgumentContext rule
	// *
	// * @return corresponding AST PositionalArgumentContext
	// *
	// * @see BoxArgument
	// */
	// private BoxArgument toAst( File file, BoxScriptGrammar.PositionalArgumentContext node ) {
	// BoxExpression value = toAst( file, node.expression() );
	// return new BoxArgument( null, value, getPosition( node ), getSourceText( node ) );
	// }
	//
	// /**
	// * Converts the NamedArgumentContext parser rule to the corresponding AST node.
	// *
	// * @param file source file, if any
	// * @param node ANTLR NamedArgumentContext rule
	// *
	// * @return corresponding AST NamedArgumentContext
	// *
	// * @see BoxArgument
	// */
	// private BoxArgument toAst( File file, BoxScriptGrammar.NamedArgumentContext node ) {
	// BoxExpression name;
	// if ( node.identifier() != null ) {
	// name = new BoxStringLiteral( node.identifier().getText(), getPosition( node ), getSourceText( node ) );
	// } else {
	// name = toAst( file, node.stringLiteral() );
	// }
	// BoxExpression value = toAst( file, node.expression() );
	// return new BoxArgument( name, value, getPosition( node ), getSourceText( node ) );
	// }
	//
	// /**
	// * Converts the AttributeSimple parser rule to the corresponding AST node.
	// *
	// * @param file source file, if any
	// * @param node ANTLR AttributeSimpleContext rule
	// *
	// * @return corresponding AST AttributeSimpleContext
	// *
	// */
	// private BoxExpression toAst( File file, BoxScriptGrammar.AttributeSimpleContext node ) {
	// if ( node.literalExpression() != null ) {
	// return toAst( file, node.literalExpression() );
	// } else if ( node.identifier() != null ) {
	// // Converting an identifer to a string literal here in the AST removes ambiguity, but also loses the
	// // lexical context of the original source code.
	// return new BoxStringLiteral( node.identifier().getText(), getPosition( node ), getSourceText( node ) );
	// } else if ( node.fqn() != null ) {
	// // Converting an fqn to a string literal here in the AST removes ambiguity, but also loses the
	// // lexical context of the original source code.
	// return new BoxStringLiteral( node.fqn().getText(), getPosition( node ), getSourceText( node ) );
	// }
	// issues.add( new Issue( "Attribute simple not implemented", getPosition( node ) ) );
	// return null;
	// }
	//
	// /**
	// * Converts the pre annotation parser rule to the corresponding AST node.
	// *
	// * @param file source file, if any
	// * @param annotation ANTLR PreannotationContext rule
	// *
	// * @return corresponding AST PreannotationContext
	// *
	// * @see PreannotationContext
	// */
	// private BoxAnnotation toAst( File file, BoxScriptGrammar.PreannotationContext annotation ) {
	// BoxExpression avalue = null;
	// BoxExpression aname = toAst( file, annotation.fqn() );
	// if ( annotation.literalExpression().size() == 1 ) {
	// avalue = toAst( file, annotation.literalExpression( 0 ) );
	// } else if ( annotation.literalExpression().size() > 1 ) {
	// List<BoxExpression> values = new ArrayList<>();
	// for ( BoxScriptGrammar.LiteralExpressionContext value : annotation.literalExpression() ) {
	// values.add( toAst( file, value ) );
	// }
	// avalue = new BoxArrayLiteral( values, getPosition( annotation ), getSourceText( annotation ) );
	// } else {
	// avalue = null;
	// }
	// return new BoxAnnotation( ( BoxFQN ) aname, avalue, getPosition( annotation ), getSourceText( annotation ) );
	// }
	//
	// /**
	// * Converts the Function argument parser rule to the corresponding AST node.
	// *
	// * @param file source file, if any
	// * @param node ANTLR ParamContext rule
	// *
	// * @return corresponding AST BoxArgumentDeclaration
	// *
	// * @see BoxArgumentDeclaration
	// */
	// private BoxArgumentDeclaration toAst( File file, BoxScriptGrammar.FunctionParamContext node ) {
	// Boolean required = false;
	// String type = "Any";
	// String name = "undefined";
	// BoxExpression expr = null;
	// List<BoxAnnotation> annotations = new ArrayList<>();
	// List<BoxDocumentationAnnotation> documentation = new ArrayList<>();
	//
	// name = node.identifier().getText();
	// if ( node.REQUIRED() != null ) {
	// required = true;
	// }
	//
	// if ( node.expression() != null ) {
	// expr = toAst( file, node.expression() );
	// }
	// if ( node.type() != null ) {
	// type = node.type().getText();
	// }
	// for ( BoxScriptGrammar.PostannotationContext annotation : node.postannotation() ) {
	// annotations.add( toAst( file, annotation ) );
	// }
	//
	// return new BoxArgumentDeclaration( required, type, name, expr, annotations, documentation, getPosition( node ), getSourceText( node ) );
	// }
	//
	// /**
	// * Converts a post annotation into the corresponding AST node
	// *
	// * @param file source file, if any
	// * @param node ANTLR PostannotationContext rule
	// *
	// * @return corresponding AST PostannotationContext
	// *
	// * @see BoxAnnotation
	// */
	// private BoxAnnotation toAst( File file, BoxScriptGrammar.PostannotationContext node ) {
	//
	// BoxFQN name = new BoxFQN( node.key.getText(), getPosition( node.key ), getSourceText( node.key ) );
	// BoxExpression value;
	// if ( node.value != null ) {
	// value = toAst( file, node.value );
	// } else {
	// value = null;
	// }
	// return new BoxAnnotation( name, value, getPosition( node ), getSourceText( node ) );
	// }
	//
	// /**
	// * Converts a Property into the corresponding AST node
	// *
	// * @param file source file, if any
	// * @param node ANTLR PropertyContext rule
	// *
	// * @return corresponding AST PropertyContext
	// *
	// */
	// private BoxProperty toAst( File file, BoxScriptGrammar.PropertyContext node ) {
	// List<BoxAnnotation> annotations = new ArrayList<>();
	// List<BoxAnnotation> postAnnotations = new ArrayList<>();
	// List<BoxDocumentationAnnotation> documentation = new ArrayList<>();
	//
	// for ( BoxScriptGrammar.PreannotationContext annotation : node.preannotation() ) {
	// annotations.add( toAst( file, annotation ) );
	// }
	// for ( BoxScriptGrammar.PostannotationContext annotation : node.postannotation() ) {
	// postAnnotations.add( toAst( file, annotation ) );
	// }
	//
	// return new BoxProperty( annotations, postAnnotations, documentation, getPosition( node ), getSourceText( node ) );
	// }
	//
	// public BoxExpression parseBoxExpression( String code, Position position ) {
	// try {
	// ParsingResult result = new BoxScriptParser( position.getStart().getLine(), position.getStart().getColumn() )
	// .setSource( sourceToParse )
	// .setSubParser( true )
	// .parseExpression( code );
	// this.comments.addAll( result.getComments() );
	// if ( result.getIssues().isEmpty() ) {
	// return ( BoxExpression ) result.getRoot();
	// } else {
	// // Add these issues to the main parser
	// issues.addAll( result.getIssues() );
	// return new BoxNull( null, null );
	// }
	// } catch ( IOException e ) {
	// issues.add( new Issue( "Error parsing expression " + e.getMessage(), position ) );
	// return new BoxNull( null, null );
	// }
	// }
	//
	// @Override
	// public BoxScriptParser setSource( Source source ) {
	// if ( this.sourceToParse != null ) {
	// return this;
	// }
	// this.sourceToParse = source;
	// return this;
	// }
	//
	// @Override
	// public BoxScriptParser setSubParser( boolean subParser ) {
	// this.subParser = subParser;
	// return this;
	// }

}
