/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package ortus.boxlang.parser;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BOMInputStream;

import ortus.boxlang.ast.BoxClass;
import ortus.boxlang.ast.BoxDocumentation;
import ortus.boxlang.ast.BoxExpr;
import ortus.boxlang.ast.BoxNode;
import ortus.boxlang.ast.BoxScript;
import ortus.boxlang.ast.BoxStatement;
import ortus.boxlang.ast.BoxTemplate;
import ortus.boxlang.ast.Issue;
import ortus.boxlang.ast.Point;
import ortus.boxlang.ast.Position;
import ortus.boxlang.ast.expression.BoxAccess;
import ortus.boxlang.ast.expression.BoxArgument;
import ortus.boxlang.ast.expression.BoxArrayAccess;
import ortus.boxlang.ast.expression.BoxArrayLiteral;
import ortus.boxlang.ast.expression.BoxAssignment;
import ortus.boxlang.ast.expression.BoxAssignmentModifier;
import ortus.boxlang.ast.expression.BoxBinaryOperation;
import ortus.boxlang.ast.expression.BoxBinaryOperator;
import ortus.boxlang.ast.expression.BoxBooleanLiteral;
import ortus.boxlang.ast.expression.BoxClosure;
import ortus.boxlang.ast.expression.BoxComparisonOperation;
import ortus.boxlang.ast.expression.BoxComparisonOperator;
import ortus.boxlang.ast.expression.BoxDecimalLiteral;
import ortus.boxlang.ast.expression.BoxDotAccess;
import ortus.boxlang.ast.expression.BoxExpressionInvocation;
import ortus.boxlang.ast.expression.BoxFQN;
import ortus.boxlang.ast.expression.BoxFunctionInvocation;
import ortus.boxlang.ast.expression.BoxIdentifier;
import ortus.boxlang.ast.expression.BoxIntegerLiteral;
import ortus.boxlang.ast.expression.BoxLambda;
import ortus.boxlang.ast.expression.BoxMethodInvocation;
import ortus.boxlang.ast.expression.BoxNewOperation;
import ortus.boxlang.ast.expression.BoxNull;
import ortus.boxlang.ast.expression.BoxParenthesis;
import ortus.boxlang.ast.expression.BoxScope;
import ortus.boxlang.ast.expression.BoxStringConcat;
import ortus.boxlang.ast.expression.BoxStringInterpolation;
import ortus.boxlang.ast.expression.BoxStringLiteral;
import ortus.boxlang.ast.expression.BoxStructLiteral;
import ortus.boxlang.ast.expression.BoxStructType;
import ortus.boxlang.ast.expression.BoxTernaryOperation;
import ortus.boxlang.ast.expression.BoxUnaryOperation;
import ortus.boxlang.ast.expression.BoxUnaryOperator;
import ortus.boxlang.ast.statement.BoxAccessModifier;
import ortus.boxlang.ast.statement.BoxAnnotation;
import ortus.boxlang.ast.statement.BoxArgumentDeclaration;
import ortus.boxlang.ast.statement.BoxAssert;
import ortus.boxlang.ast.statement.BoxAssignmentOperator;
import ortus.boxlang.ast.statement.BoxBreak;
import ortus.boxlang.ast.statement.BoxContinue;
import ortus.boxlang.ast.statement.BoxDo;
import ortus.boxlang.ast.statement.BoxDocumentationAnnotation;
import ortus.boxlang.ast.statement.BoxExpression;
import ortus.boxlang.ast.statement.BoxForIn;
import ortus.boxlang.ast.statement.BoxForIndex;
import ortus.boxlang.ast.statement.BoxFunctionDeclaration;
import ortus.boxlang.ast.statement.BoxIfElse;
import ortus.boxlang.ast.statement.BoxImport;
import ortus.boxlang.ast.statement.BoxInclude;
import ortus.boxlang.ast.statement.BoxProperty;
import ortus.boxlang.ast.statement.BoxRethrow;
import ortus.boxlang.ast.statement.BoxReturn;
import ortus.boxlang.ast.statement.BoxReturnType;
import ortus.boxlang.ast.statement.BoxSwitch;
import ortus.boxlang.ast.statement.BoxSwitchCase;
import ortus.boxlang.ast.statement.BoxThrow;
import ortus.boxlang.ast.statement.BoxTry;
import ortus.boxlang.ast.statement.BoxTryCatch;
import ortus.boxlang.ast.statement.BoxType;
import ortus.boxlang.ast.statement.BoxWhile;
import ortus.boxlang.ast.statement.component.BoxComponent;
import ortus.boxlang.ast.statement.component.BoxTemplateIsland;
import ortus.boxlang.parser.antlr.CFLexer;
import ortus.boxlang.parser.antlr.CFParser;
import ortus.boxlang.parser.antlr.CFParser.BoxClassContext;
import ortus.boxlang.parser.antlr.CFParser.ComponentContext;
import ortus.boxlang.parser.antlr.CFParser.ComponentIslandContext;
import ortus.boxlang.parser.antlr.CFParser.NewContext;
import ortus.boxlang.parser.antlr.CFParser.PreannotationContext;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * Parser for CF scripts
 */
public class BoxCFParser extends BoxAbstractParser {

	private final List<BoxDocumentation>	javadocs		= new ArrayList<>();
	private boolean							inOutputBlock	= false;

	/**
	 * Constructor
	 */
	public BoxCFParser() {
		super();
	}

	public BoxCFParser( int startLine, int startColumn ) {
		super( startLine, startColumn );
	}

	public BoxCFParser( int startLine, int startColumn, boolean inOutputBlock ) {
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
	 * Parse a cf script file
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
		BOMInputStream			inputStream	= getInputStream( file );
		CFParser.ScriptContext	parseTree	= ( CFParser.ScriptContext ) parserFirstStage( inputStream );

		if ( issues.isEmpty() ) {
			BoxNode ast = parseTreeToAst( file, parseTree );
			return new ParsingResult( ast, issues );
		}
		return new ParsingResult( null, issues );
	}

	/**
	 * Parse a cf script string
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
		InputStream				inputStream	= IOUtils.toInputStream( code, StandardCharsets.UTF_8 );

		CFParser.ScriptContext	parseTree	= ( CFParser.ScriptContext ) parserFirstStage( inputStream );
		if ( issues.isEmpty() ) {
			BoxNode ast = parseTreeToAst( file, parseTree );
			return new ParsingResult( ast, issues );
		}
		return new ParsingResult( null, issues );
	}

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
	 * @see BoxExpr
	 */
	public ParsingResult parseExpression( String code ) throws IOException {
		InputStream	inputStream	= IOUtils.toInputStream( code, StandardCharsets.UTF_8 );

		CFLexer		lexer		= new CFLexer( CharStreams.fromStream( inputStream ) );
		CFParser	parser		= new CFParser( new CommonTokenStream( lexer ) );
		addErrorListeners( lexer, parser );
		// var t = lexer.nextToken();
		// while ( t.getType() != Token.EOF ) {
		//
		// System.out.println( t + " " + lexer.getVocabulary().getSymbolicName( t.getType() ) + " " + lexer.getModeNames()[ lexer._mode ] );
		// t = lexer.nextToken();
		// }
		CFParser.ExpressionContext parseTree = parser.expression();
		if ( issues.isEmpty() ) {
			BoxExpr ast = toAst( null, parseTree );
			return new ParsingResult( ast, issues );
		}
		return new ParsingResult( null, issues );
	}

	/**
	 * Parse a cf script string statement
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
		InputStream	inputStream	= IOUtils.toInputStream( code, StandardCharsets.UTF_8 );

		CFLexer		lexer		= new CFLexer( CharStreams.fromStream( inputStream ) );
		CFParser	parser		= new CFParser( new CommonTokenStream( lexer ) );
		addErrorListeners( lexer, parser );
		CFParser.FunctionOrStatementContext	parseTree	= parser.functionOrStatement();

		BoxStatement						ast			= toAst( null, parseTree );
		return new ParsingResult( ast, issues );
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
	protected ParserRuleContext parserFirstStage( InputStream stream ) throws IOException {
		CFLexerCustom	lexer	= new CFLexerCustom( CharStreams.fromStream( stream ) );
		CFParser		parser	= new CFParser( new CommonTokenStream( lexer ) );
		addErrorListeners( lexer, parser );
		ParserRuleContext parseTree = parser.script();

		lexer.reset();
		Token			token		= lexer.nextToken();
		BoxDOCParser	docParser	= new BoxDOCParser( token.getLine(), token.getCharPositionInLine() );
		while ( token.getType() != Token.EOF ) {
			if ( token.getType() == CFLexer.JAVADOC_COMMENT ) {
				ParsingResult result = docParser.parse( null, token.getText() );
				if ( docParser.issues.isEmpty() ) {
					javadocs.add( ( BoxDocumentation ) result.getRoot() );
				} else {
					// Add these issues to the main parser
					issues.addAll( docParser.issues );
				}
			}
			token = lexer.nextToken();
		}

		if ( lexer.hasUnpoppedModes() ) {
			List<String>	modes		= lexer.getUnpoppedModes();

			/*
			 * modes.forEach( mode -> {
			 * System.out.println( "Unpopped mode: " + mode );
			 * } );
			 */
			// TODO: get position
			Position		position	= new Position( new Point( 0, 0 ),
			    new Point( 0, 0 ) );
			if ( modes.contains( "hashMode" ) ) {
				issues.add( new Issue( "Untermimated hash expression inside of string literal.", position ) );
			} else {
				// Not sure this is always the case
				issues.add( new Issue( "Untermimated string literal.", position ) );
			}
		}

		return parseTree;
	}

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
	@Override
	protected BoxNode parseTreeToAst( File file, ParserRuleContext rule ) {
		CFParser.ScriptContext	parseTree	= ( CFParser.ScriptContext ) rule;
		List<BoxStatement>		statements	= new ArrayList<>();
		List<BoxImport>			imports		= new ArrayList<>();

		parseTree.importStatement().forEach( stmt -> {
			imports.add( toAst( file, stmt ) );
		} );

		if ( parseTree.boxClass() != null ) {
			return toAst( file, parseTree.boxClass(), imports );
		} else {
			statements.addAll( imports );
			parseTree.functionOrStatement().forEach( stmt -> {
				statements.add( toAst( file, stmt ) );
			} );
			return new BoxScript( statements, getPosition( rule ), getSourceText( rule ) );
		}
	}

	private BoxNode toAst( File file, BoxClassContext component, List<BoxImport> imports ) {
		List<BoxStatement>					body			= new ArrayList<>();
		List<BoxAnnotation>					annotations		= new ArrayList<>();
		List<BoxDocumentationAnnotation>	documentation	= new ArrayList<>();
		List<BoxProperty>					property		= new ArrayList<>();

		BoxDocumentation					doc				= getDocIndex( component );
		if ( doc != null ) {
			for ( BoxNode n : doc.getAnnotations() ) {
				documentation.add( ( BoxDocumentationAnnotation ) n );
			}
		}
		for ( CFParser.PreannotationContext annotation : component.preannotation() ) {
			annotations.add( toAst( file, annotation ) );
		}
		for ( CFParser.PostannotationContext annotation : component.postannotation() ) {
			annotations.add( toAst( file, annotation ) );
		}
		for ( CFParser.PropertyContext annotation : component.property() ) {
			property.add( toAst( file, annotation ) );
		}
		component.functionOrStatement().forEach( stmt -> {
			body.add( toAst( file, stmt ) );
		} );

		return new BoxClass( imports, body, annotations, documentation, property, getPosition( component ), getSourceText( component ) );
	}

	/**
	 * Converts the ImportStatementContext parser rule to the corresponding AST node
	 *
	 * @param file source file, if any
	 * @param rule ANTLR ImportStatementContext rule
	 *
	 * @return the corresponding AST BoxStatement
	 *
	 * @see BoxImport
	 */
	private BoxImport toAst( File file, CFParser.ImportStatementContext rule ) {
		BoxExpr			expr	= null;
		BoxIdentifier	alias	= null;
		if ( rule.fqn() != null ) {
			String prefix = "";
			if ( rule.prefix != null ) {
				prefix = rule.prefix.getText() + ":";
			}
			expr = new BoxFQN( prefix + rule.fqn().getText(), getPosition( rule.fqn() ), getSourceText( rule.fqn() ) );
		}
		if ( rule.alias != null ) {
			BoxExpr tmp = toAst( file, rule.alias );
			if ( tmp instanceof BoxScope ) {
				throw new IllegalStateException( "Cannot use a scope as an alias" );
			} else {
				alias = ( BoxIdentifier ) tmp;
			}

		}
		return new BoxImport( expr, alias, getPosition( rule ), getSourceText( rule ) );
	}

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
	private BoxStatement toAst( File file, CFParser.FunctionOrStatementContext node ) {
		if ( node.function() != null ) {
			return toAst( file, node.function() );
		} else if ( node.statement() != null ) {
			return toAst( file, node.statement() );
		} else {
			throw new IllegalStateException( "not implemented: " + node.getClass().getSimpleName() );
		}
	}

	/**
	 * Converts the Statement parser rule to the corresponding AST node
	 *
	 * @param file source file, if any
	 * @param node ANTLR StatementContext rule
	 *
	 * @return the corresponding AST BoxStatement
	 *
	 * @see BoxStatement
	 */
	private BoxStatement toAst( File file, CFParser.StatementContext node ) {
		if ( node.simpleStatement() != null ) {
			return toAst( file, node.simpleStatement() );
		} else if ( node.if_() != null ) {
			return toAst( file, node.if_() );
		} else if ( node.while_() != null ) {
			return toAst( file, node.while_() );
		} else if ( node.do_() != null ) {
			return toAst( file, node.do_() );
		} else if ( node.switch_() != null ) {
			return toAst( file, node.switch_() );
		} else if ( node.for_() != null ) {
			return toAst( file, node.for_() );
		} else if ( node.try_() != null ) {
			return toAst( file, node.try_() );
		} else if ( node.componentIsland() != null ) {
			return toAst( file, node.componentIsland() );
		} else if ( node.component() != null ) {
			return toAst( file, node.component() );
		} else {
			throw new IllegalStateException( "not implemented: " + getSourceText( node ) );
		}
	}

	private BoxStatement toAst( File file, ComponentContext node ) {
		List<BoxStatement>	body			= null;
		String				componentName	= null;
		List<BoxAnnotation>	attributes		= new ArrayList<>();

		if ( node.componentAttributes() != null && node.componentAttributes().namedArgument() != null ) {
			for ( var attr : node.componentAttributes().namedArgument() ) {
				attributes.add( toAstAnnotation( file, attr ) );
			}
		} else if ( node.delimitedComponentAttributes() != null && node.delimitedComponentAttributes().namedArgument() != null ) {
			for ( var attr : node.delimitedComponentAttributes().namedArgument() ) {
				attributes.add( toAstAnnotation( file, attr ) );
			}
		}

		if ( node.componentName() != null ) {
			componentName = node.componentName().getText();
		} else {
			// strip prefix from name so "cfbrad" becomes "brad
			componentName = node.prefixedIdentifier().getText().substring( 2 );
		}

		if ( node.statementBlock() != null ) {
			body = new ArrayList<>();
			body.addAll( toAst( file, node.statementBlock() ) );
		}
		return new BoxComponent( componentName, attributes, body, 0, getPosition( node ), getSourceText( node ) );
	}

	private BoxAnnotation toAstAnnotation( File file, CFParser.NamedArgumentContext node ) {
		BoxFQN name;
		if ( node.identifier() != null ) {
			name = new BoxFQN( node.identifier().getText(), getPosition( node.identifier() ), getSourceText( node.identifier() ) );
		} else {
			// Raw text
			String	stringLit	= node.stringLiteral().getText();
			// Find quote char
			String	s			= stringLit.substring( 1, stringLit.length() - 1 );
			name = new BoxFQN( escapeStringLiteral( stringLit, s ), getPosition( node.identifier() ), getSourceText( node.identifier() ) );
		}
		BoxExpr value = toAst( file, node.expression() );
		return new BoxAnnotation( name, value, getPosition( node ), getSourceText( node ) );
	}

	/**
	 * Converts the ComponentIslandContext parser rule to the corresponding AST node
	 *
	 * @param file source file, if any
	 * @param node ANTLR ComponentIslandContext rule
	 *
	 * @return the corresponding AST BoxComponentIsland
	 *
	 * @see BoxThrow
	 */
	private BoxTemplateIsland toAst( File file, ComponentIslandContext componentIsland ) {
		return new BoxTemplateIsland(
		    parseCFMLStatements(
		        componentIsland.componentIslandBody().getText(),
		        getPosition( componentIsland.componentIslandBody() )
		    ),
		    getPosition( componentIsland.componentIslandBody() ),
		    getSourceText( componentIsland.componentIslandBody() )
		);

	}

	public List<BoxStatement> parseCFMLStatements( String code, Position position ) {
		try {
			if ( inOutputBlock ) {
				code = "<cfoutput>" + code + "</cfoutput>";
			}
			ParsingResult result = new BoxCFMLParser( position.getStart().getLine(), position.getStart().getColumn() ).parse( code );
			if ( result.getIssues().isEmpty() ) {
				BoxNode root = result.getRoot();
				if ( root instanceof BoxTemplate template ) {
					return template.getStatements();
				} else if ( root instanceof BoxStatement statement ) {
					return List.of( statement );
				} else {
					// Could be a BoxClass, which we may actually need to support
					throw new BoxRuntimeException( "Unexpected root node type [" + root.getClass().getName() + "] in component island." );
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
	 * Converts the include parser rule to the corresponding AST node
	 *
	 * @param file source file, if any
	 * @param node ANTLR IncludeContext rule
	 *
	 * @return the corresponding AST BoxStatement
	 *
	 * @see BoxThrow
	 */
	private BoxStatement toAst( File file, CFParser.IncludeContext node ) {
		return new BoxInclude( toAst( file, node.expression() ), getPosition( node ), getSourceText( node ) );
	}

	/**
	 * Converts the rethrow parser rule to the corresponding AST node
	 *
	 * @param file source file, if any
	 * @param node ANTLR RethrowContext rule
	 *
	 * @return the corresponding AST BoxStatement
	 *
	 * @see BoxThrow
	 */
	private BoxStatement toAst( File file, CFParser.RethrowContext node ) {
		return new BoxRethrow( getPosition( node ), getSourceText( node ) );
	}

	/**
	 * Converts the throw parser rule to the corresponding AST node
	 *
	 * @param file source file, if any
	 * @param node ANTLR ThrowContext rule
	 *
	 * @return the corresponding AST BoxStatement
	 *
	 * @see BoxThrow
	 */
	private BoxStatement toAst( File file, CFParser.ThrowContext node ) {
		BoxExpr expression = toAst( file, node.expression() );
		return new BoxThrow( expression, getPosition( node ), getSourceText( node ) );
	}

	/**
	 * Converts the do parser rule to the corresponding AST node
	 *
	 * @param file source file, if any
	 * @param node ANTLR TryContext rule
	 *
	 * @return the corresponding AST BoxStatement
	 *
	 * @see BoxDo
	 */
	private BoxStatement toAst( File file, CFParser.DoContext node ) {
		BoxExpr				condition	= toAst( file, node.expression() );
		List<BoxStatement>	body		= new ArrayList<>();

		if ( node.statementBlock() != null ) {
			body.addAll( toAst( file, node.statementBlock() ) );
		}
		return new BoxDo( condition, body, getPosition( node ), getSourceText( node ) );
	}

	/**
	 * Converts the try parser rule to the corresponding AST node
	 *
	 * @param file source file, if any
	 * @param node ANTLR TryContext rule
	 *
	 * @return the corresponding AST BoxStatement
	 *
	 * @see BoxTry
	 */
	private BoxStatement toAst( File file, CFParser.TryContext node ) {
		List<BoxStatement>	tryBody		= toAst( file, node.statementBlock() );
		List<BoxTryCatch>	catches		= node.catch_().stream().map( it -> toAst( file, it ) ).toList();
		List<BoxStatement>	finallyBody	= new ArrayList<>();
		if ( node.finally_() != null ) {
			finallyBody.addAll( toAst( file, node.finally_().statementBlock() ) );
		}
		return new BoxTry( tryBody, catches, finallyBody, getPosition( node ), getSourceText( node ) );
	}

	/**
	 * Converts the catch parser rule to the corresponding AST node
	 *
	 * @param file source file, if any
	 * @param node ANTLR TryContext rule
	 *
	 * @return the corresponding AST BoxStatement
	 *
	 * @see BoxTryCatch
	 */
	private BoxTryCatch toAst( File file, CFParser.Catch_Context node ) {
		BoxExpr				exception	= toAst( file, node.expression() );
		List<BoxStatement>	catchBody	= toAst( file, node.statementBlock() );

		List<BoxExpr>		catchTypes	= node.catchType().stream().map( ctNode -> {
											if ( ctNode.fqn() != null ) {
												return new BoxFQN( ctNode.fqn().getText(), getPosition( ctNode ),
												    getSourceText( ctNode ) );
											}

											return toAst( file, ctNode.stringLiteral() );
										} )
		    .collect( Collectors.toList() );

		return new BoxTryCatch( catchTypes, exception, catchBody, getPosition( node ), getSourceText( node ) );
	}

	/**
	 * Converts the assert parser rule to the corresponding AST node
	 *
	 * @param file source file, if any
	 * @param node ANTLR AssertContext rule
	 *
	 * @return the corresponding AST BoxStatement
	 *
	 * @see BoxAssert
	 */
	private BoxStatement toAst( File file, CFParser.AssertContext node ) {
		BoxExpr expression = toAst( file, node.expression() );
		return new BoxAssert( expression, getPosition( node ), getSourceText( node ) );
	}

	/**
	 * Converts the For parser rule to the corresponding AST node
	 *
	 * @param file source file, if any
	 * @param node ANTLR ForContext rule
	 *
	 * @return the corresponding AST BoxStatement
	 *
	 * @see BoxForIn
	 * @see BoxForIndex
	 */
	private BoxStatement toAst( File file, CFParser.ForContext node ) {
		List<BoxStatement> body = toAst( file, node.statementBlock() );
		if ( node.IN() != null ) {
			BoxExpr	variable	= toAst( file, node.accessExpression() );
			Boolean	hasVar		= node.VAR() != null;
			BoxExpr	collection	= toAst( file, node.expression() );

			return new BoxForIn( variable, collection, body, hasVar, getPosition( node ), getSourceText( node ) );
		}
		BoxExpr	initializer	= toAst( file, node.forAssignment().expression() );
		BoxExpr	condition	= toAst( file, node.forCondition().expression() );
		BoxExpr	step		= toAst( file, node.forIncrement().expression() );

		return new BoxForIndex( initializer, condition, step, body, getPosition( node ), getSourceText( node ) );
	}

	/**
	 * Converts the Switch parser rule to the corresponding AST node
	 *
	 * @param file source file, if any
	 * @param node ANTLR SwitchContext rule
	 *
	 * @return the corresponding AST BoxStatement
	 *
	 * @see BoxSwitch
	 */
	private BoxStatement toAst( File file, CFParser.SwitchContext node ) {
		BoxExpr				condition	= toAst( file, node.expression() );
		List<BoxSwitchCase>	cases		= new ArrayList<>();
		for ( CFParser.CaseContext c : node.case_() ) {
			cases.add( toAst( file, c ) );
		}
		return new BoxSwitch( condition, cases, getPosition( node ), getSourceText( node ) );
	}

	/**
	 * Converts the Case parser rule to the corresponding AST node
	 *
	 * @param file source file, if any
	 * @param node ANTLR CaseContext rule
	 *
	 * @return the corresponding AST BoxStatement
	 *
	 * @see BoxSwitchCase
	 */
	private BoxSwitchCase toAst( File file, CFParser.CaseContext node ) {
		BoxExpr expr = null;
		if ( node.expression() != null ) {
			expr = toAst( file, node.expression() );
		}

		List<BoxStatement> statements = new ArrayList<>();
		if ( node.statement() != null ) {
			for ( var statement : node.statement() ) {
				statements.add( toAst( file, statement ) );
			}
		}
		if ( node.statementBlock() != null ) {
			statements.addAll( toAst( file, node.statementBlock() ) );
		}
		return new BoxSwitchCase( expr, null, statements, getPosition( node ), getSourceText( node ) );
	}

	/**
	 * Converts the Continue parser rule to the corresponding AST node
	 *
	 * @param file source file, if any
	 * @param node ANTLR ContinueContext rule
	 *
	 * @return the corresponding AST BoxStatement
	 *
	 * @see BoxContinue
	 */
	private BoxStatement toAst( File file, CFParser.ContinueContext node ) {
		return new BoxContinue( getPosition( node ), getSourceText( node ) );
	}

	/**
	 * Converts the Break parser rule to the corresponding AST node
	 *
	 * @param file source file, if any
	 * @param node ANTLR BreakContext rule
	 *
	 * @return the corresponding AST BoxStatement
	 *
	 * @see BoxBreak
	 */
	private BoxStatement toAst( File file, CFParser.BreakContext node ) {
		return new BoxBreak( getPosition( node ), getSourceText( node ) );
	}

	/**
	 * Converts the While parser rule to the corresponding AST node
	 *
	 * @param file source file, if any
	 * @param node ANTLR WhileContext rule
	 *
	 * @return the corresponding AST BoxStatement
	 *
	 * @see BoxWhile
	 */
	private BoxStatement toAst( File file, CFParser.WhileContext node ) {
		BoxExpr				condition	= toAst( file, node.condition );
		List<BoxStatement>	body		= new ArrayList<>();

		if ( node.statementBlock() != null ) {
			body.addAll( toAst( file, node.statementBlock() ) );
		} else if ( node.statement() != null ) {
			body.add( toAst( file, node.statement() ) );
		}
		return new BoxWhile( condition, body, getPosition( node ), getSourceText( node ) );
	}

	/**
	 * Converts the IfContext parser rule to the corresponding AST node
	 *
	 * @param file source file, if any
	 *
	 * @return the corresponding AST BoxIfElse
	 *
	 * @see BoxIfElse
	 */
	private BoxIfElse toAst( File file, CFParser.IfContext node ) {
		BoxExpr				condition	= toAst( file, node.expression() );
		List<BoxStatement>	thenBody	= new ArrayList<>();
		List<BoxStatement>	elseBody	= new ArrayList<>();

		if ( node.ifStmt != null ) {
			thenBody.add( toAst( file, node.ifStmt ) );
		}
		if ( node.ifStmtBlock != null ) {
			thenBody.addAll( toAst( file, node.ifStmtBlock ) );
		}
		if ( node.elseStmt != null ) {
			elseBody.add( toAst( file, node.elseStmt ) );
		}
		if ( node.elseStmtBlock != null ) {
			elseBody.addAll( toAst( file, node.elseStmtBlock ) );
		}
		return new BoxIfElse( condition, thenBody, elseBody, getPosition( node ), getSourceText( node ) );
	}

	/**
	 * Converts the StatementBlock parser rule to the corresponding AST node
	 *
	 * @param file source file, if any
	 * @param node ANTLR BreakContext rule
	 *
	 * @return the list of the corresponding AST BoxStatement subclasses in the block
	 *
	 * @see BoxStatement
	 */
	private List<BoxStatement> toAst( File file, CFParser.StatementBlockContext node ) {
		return node.statement().stream().map( stmt -> toAst( file, stmt ) ).toList();
	}

	/**
	 * Converts the SimpleStatement parser rule to the corresponding AST node.
	 * The SimpleStatement contains rules of an Expression statement
	 *
	 * @param file source file, if any
	 * @param node ANTLR SimpleStatementContext rule
	 *
	 * @return the corresponding AST BoxStatement subclass
	 *
	 * @see BoxStatement
	 */
	private BoxStatement toAst( File file, CFParser.SimpleStatementContext node ) {

		if ( node.assert_() != null ) {
			return toAst( file, node.assert_() );
		} else if ( node.return_() != null ) {
			BoxExpr expr = null;
			if ( node.return_().expression() != null ) {
				expr = toAst( file, node.return_().expression() );
			}
			return new BoxReturn( expr, getPosition( node ), getSourceText( node ) );
		} else if ( node.incrementDecrementStatement() != null ) {
			return toAst( file, node.incrementDecrementStatement() );
		} else if ( node.expression() != null ) {
			BoxExpr expr = toAst( file, node.expression() );
			return new BoxExpression( expr, getPosition( node ), getSourceText( node ) );
		} else if ( node.break_() != null ) {
			return toAst( file, node.break_() );
		} else if ( node.continue_() != null ) {
			return toAst( file, node.continue_() );
		} else if ( node.rethrow() != null ) {
			return toAst( file, node.rethrow() );
		} else if ( node.include() != null ) {
			return toAst( file, node.include() );
		} else if ( node.throw_() != null ) {
			return toAst( file, node.throw_() );
		}

		throw new IllegalStateException( "not implemented: " + node.getClass().getSimpleName() );

	}

	/**
	 * Converts the IncrementDecrementStatement parser rule to the corresponding AST node.
	 *
	 * @param file source file, if any
	 * @param node ANTLR IncrementDecrementStatementContext rule
	 *
	 * @return the corresponding AST BoxStatement subclass
	 *
	 * @see
	 */
	private BoxStatement toAst( File file, CFParser.IncrementDecrementStatementContext node ) {
		if ( node instanceof CFParser.PostIncrementContext ) {
			CFParser.PostIncrementContext	ctx		= ( CFParser.PostIncrementContext ) node;
			BoxExpr							expr	= toAst( file, ctx.accessExpression() );
			BoxUnaryOperation				post	= new BoxUnaryOperation( expr, BoxUnaryOperator.PostPlusPlus, getPosition( node ), getSourceText( node ) );
			return new BoxExpression( post, getPosition( node ), getSourceText( node ) );
		}
		if ( node instanceof CFParser.PostDecrementContext ) {
			CFParser.PostDecrementContext	ctx		= ( CFParser.PostDecrementContext ) node;
			BoxExpr							expr	= toAst( file, ctx.accessExpression() );
			BoxUnaryOperation				post	= new BoxUnaryOperation( expr, BoxUnaryOperator.PostMinusMinus, getPosition( node ),
			    getSourceText( node ) );
			return new BoxExpression( post, getPosition( node ), getSourceText( node ) );
		}
		if ( node instanceof CFParser.PreIncrementContext ) {
			CFParser.PreIncrementContext	ctx		= ( CFParser.PreIncrementContext ) node;
			BoxExpr							expr	= toAst( file, ctx.accessExpression() );
			BoxUnaryOperation				post	= new BoxUnaryOperation( expr, BoxUnaryOperator.PrePlusPlus, getPosition( node ),
			    getSourceText( node ) );
			return new BoxExpression( post, getPosition( node ), getSourceText( node ) );
		}
		if ( node instanceof CFParser.PreDecremenentContext ) {
			CFParser.PreDecremenentContext	ctx		= ( CFParser.PreDecremenentContext ) node;
			BoxExpr							expr	= toAst( file, ctx.accessExpression() );
			BoxUnaryOperation				post	= new BoxUnaryOperation( expr, BoxUnaryOperator.PreMinusMinus, getPosition( node ),
			    getSourceText( node ) );
			return new BoxExpression( post, getPosition( node ), getSourceText( node ) );
		}
		throw new IllegalStateException( "not implemented: " + node.getClass().getSimpleName() );
	}

	/**
	 * Converts the AccessExpression parser rule to the corresponding AST node.
	 *
	 * @param file source file, if any
	 * @param node ANTLR AccessExpressionContext rule
	 *
	 * @return the corresponding AST BoxExpression subclass
	 *
	 * @see BoxIdentifier
	 * @see BoxArrayAccess
	 * @see BoxDotAccess
	 */
	private BoxExpr toAst( File file, CFParser.AccessExpressionContext node ) {
		BoxExpr expr = toAst( file, node.objectExpression() );
		// loop over children
		for ( int i = 0; i < node.getChildCount(); i++ ) {
			ParseTree child = node.getChild( i );
			if ( child instanceof CFParser.DotAccessContext dotAccess ) {
				BoxExpr access;
				// Any reserved keywords like scopes on the accessed after a dot is just a keyword.
				if ( dotAccess.identifier().reservedKeyword() != null ) {
					CFParser.ReservedKeywordContext keyword = dotAccess.identifier().reservedKeyword();
					access = new BoxIdentifier( keyword.getText(), getPosition( keyword ), getSourceText( keyword ) );
				} else {
					access = toAst( file, dotAccess.identifier() );
				}
				expr = new BoxDotAccess( expr, dotAccess.QM() != null, access, getPosition( dotAccess ),
				    getSourceText( dotAccess ) );
			}
			if ( child instanceof CFParser.ArrayAccessContext arrayAccess ) {
				expr = new BoxArrayAccess( expr, false, toAst( file, arrayAccess.expression() ), getPosition( arrayAccess ), getSourceText( arrayAccess ) );
			}
			if ( child instanceof CFParser.MethodInvokationContext methodInvokation ) {
				if ( methodInvokation.functionInvokation() != null ) {
					List<BoxArgument>			args	= toAst( file, methodInvokation.functionInvokation().invokationExpression().argumentList() );
					CFParser.IdentifierContext	id		= methodInvokation.functionInvokation().identifier();
					BoxExpr						name	= new BoxIdentifier( id.getText(), getPosition( id ), getSourceText( id ) );

					expr = new BoxMethodInvocation( name, expr, args, methodInvokation.QM() != null, true, getPosition( methodInvokation ),
					    getSourceText( methodInvokation ) );
				} else if ( methodInvokation.arrayAccess() != null ) {
					List<BoxArgument>	args	= toAst( file, methodInvokation.invokationExpression().argumentList() );
					BoxExpr				name	= toAst( file, methodInvokation.arrayAccess().expression() );
					expr = new BoxMethodInvocation( name, expr, args, false, false, getPosition( methodInvokation ), getSourceText( methodInvokation ) );
				} else {
					throw new IllegalStateException(
					    "unimplemented method invocation does not use function invocation or array access rules: " + getSourceText( methodInvokation ) );
				}
			}
			if ( child instanceof CFParser.InvokationExpressionContext invokationExpression ) {
				expr = new BoxExpressionInvocation( expr, toAst( file, invokationExpression.argumentList() ), getPosition( invokationExpression ),
				    getSourceText( invokationExpression ) );
			}
		}
		return expr;
	}

	/**
	 * Converts the IdentifierContext parser rule to the corresponding AST node.
	 *
	 * @param file source file, if any
	 * @param node ANTLR IdentifierContext rule
	 *
	 * @return the corresponding AST BoxIdentifier or a BoxScope if it is a reserved keyword
	 *
	 * @see BoxScope
	 * @see BoxIdentifier
	 */
	private BoxExpr toAst( File file, CFParser.IdentifierContext node ) {
		CFParser.ReservedKeywordContext keyword = node.reservedKeyword();
		if ( keyword != null && keyword.scope() != null ) {
			return toAst( file, keyword.scope() );
		}
		String name = "";
		if ( node.IDENTIFIER() != null ) {
			name = node.IDENTIFIER().getText();
		} else if ( node.reservedKeyword() != null ) {
			name = node.reservedKeyword().getText();
		}
		return new BoxIdentifier( name, getPosition( node ), getSourceText( node ) );
	}

	/**
	 * Converts the Scope parser rule to the corresponding AST node.
	 *
	 * @param file source file, if any
	 * @param node ANTLR ScopeContext rule
	 *
	 * @return corresponding AST BoxScope
	 *
	 * @see BoxScope for the reserved keywords used to identify a scope
	 */
	private BoxExpr toAst( File file, CFParser.ScopeContext node ) {
		return new BoxScope( node.getText(), getPosition( node ), getSourceText( node ) );
	}

	/**
	 * Converts the string literal into a BoxStringLiteral or BoxStringInterpolation
	 *
	 * @param file       source file, if any
	 * @param expression ANTLR StringLiteralContext rule
	 *
	 * @return corresponding AST BoxExpr subclass
	 *
	 * @see BoxExpr subclasses
	 */
	private BoxExpr toAst( File file, CFParser.StringLiteralContext expression ) {
		String quoteChar = expression.getText().substring( 0, 1 );
		if ( expression.expression().isEmpty() ) {
			String s = expression.getText();
			// trim leading and trailing quote
			s = s.substring( 1, s.length() - 1 );
			return new BoxStringLiteral(
			    escapeStringLiteral( quoteChar, s ),
			    getPosition( expression ),
			    getSourceText( expression )
			);

		} else {
			List<BoxExpr> parts = new ArrayList<>();
			expression.children.forEach( it -> {
				if ( it != null && it instanceof CFParser.StringLiteralPartContext ) {
					parts.add( new BoxStringLiteral( escapeStringLiteral( quoteChar, getSourceText( ( ParserRuleContext ) it ) ),
					    getPosition( ( ParserRuleContext ) it ),
					    getSourceText( ( ParserRuleContext ) it ) ) );
				}
				if ( it != null && it instanceof CFParser.ExpressionContext ) {
					parts.add( toAst( file, ( CFParser.ExpressionContext ) it ) );
				}
			} );
			return new BoxStringInterpolation( parts, getPosition( expression ), getSourceText( expression ) );
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
	private String escapeStringLiteral( String quoteChar, String string ) {
		String escaped = string.replace( "##", "#" );
		return escaped.replace( quoteChar + quoteChar, quoteChar );
	}

	/**
	 * Converts the Expression parser rule to the corresponding AST node.
	 * The operator precedence resolved in the ANTLR grammar
	 *
	 * @param file       source file, if any
	 * @param expression ANTLR ExpressionContext rule
	 *
	 * @return corresponding AST BoxExpr subclass
	 *
	 * @see BoxExpr subclasses
	 * @see BoxBinaryOperator
	 */
	private BoxExpr toAst( File file, CFParser.ExpressionContext expression ) {
		if ( expression.accessExpression() != null ) {
			return toAst( file, expression.accessExpression() );
		} else if ( expression.and() != null ) {
			BoxExpr	left	= toAst( file, expression.expression( 0 ) );
			BoxExpr	right	= toAst( file, expression.expression( 1 ) );
			return new BoxBinaryOperation( left, BoxBinaryOperator.And, right, getPosition( expression ), getSourceText( expression ) );
		} else if ( expression.or() != null && ( expression.THAN() == null ) ) {
			BoxExpr	left	= toAst( file, expression.expression( 0 ) );
			BoxExpr	right	= toAst( file, expression.expression( 1 ) );
			return new BoxBinaryOperation( left, BoxBinaryOperator.Or, right, getPosition( expression ), getSourceText( expression ) );
		} else if ( expression.PLUS() != null ) {
			BoxExpr	left	= toAst( file, expression.expression( 0 ) );
			BoxExpr	right	= toAst( file, expression.expression( 1 ) );
			return new BoxBinaryOperation( left, BoxBinaryOperator.Plus, right, getPosition( expression ), getSourceText( expression ) );
		} else if ( expression.MINUS() != null ) {
			BoxExpr	left	= toAst( file, expression.expression( 0 ) );
			BoxExpr	right	= toAst( file, expression.expression( 1 ) );
			return new BoxBinaryOperation( left, BoxBinaryOperator.Minus, right, getPosition( expression ), getSourceText( expression ) );
		} else if ( expression.STAR() != null ) {
			BoxExpr	left	= toAst( file, expression.expression( 0 ) );
			BoxExpr	right	= toAst( file, expression.expression( 1 ) );
			return new BoxBinaryOperation( left, BoxBinaryOperator.Star, right, getPosition( expression ), getSourceText( expression ) );
		} else if ( expression.SLASH() != null ) {
			BoxExpr	left	= toAst( file, expression.expression( 0 ) );
			BoxExpr	right	= toAst( file, expression.expression( 1 ) );
			return new BoxBinaryOperation( left, BoxBinaryOperator.Slash, right, getPosition( expression ), getSourceText( expression ) );
		} else if ( expression.BACKSLASH() != null ) {
			BoxExpr	left	= toAst( file, expression.expression( 0 ) );
			BoxExpr	right	= toAst( file, expression.expression( 1 ) );
			return new BoxBinaryOperation( left, BoxBinaryOperator.Backslash, right, getPosition( expression ), getSourceText( expression ) );
		} else if ( expression.unary() != null ) {
			return toAst( file, expression.unary() );
		} else if ( expression.POWER() != null ) {
			BoxExpr	left	= toAst( file, expression.expression( 0 ) );
			BoxExpr	right	= toAst( file, expression.expression( 1 ) );
			return new BoxBinaryOperation( left, BoxBinaryOperator.Power, right, getPosition( expression ), getSourceText( expression ) );
		} else if ( expression.XOR() != null ) {
			BoxExpr	left	= toAst( file, expression.expression( 0 ) );
			BoxExpr	right	= toAst( file, expression.expression( 1 ) );
			return new BoxBinaryOperation( left, BoxBinaryOperator.Xor, right, getPosition( expression ), getSourceText( expression ) );
		} else if ( expression.PERCENT() != null || expression.MOD() != null ) {
			BoxExpr	left	= toAst( file, expression.expression( 0 ) );
			BoxExpr	right	= toAst( file, expression.expression( 1 ) );
			return new BoxBinaryOperation( left, BoxBinaryOperator.Mod, right, getPosition( expression ), getSourceText( expression ) );
		} else if ( expression.INSTANCEOF() != null ) {
			BoxExpr	left	= toAst( file, expression.expression( 0 ) );
			BoxExpr	right	= toAst( file, expression.expression( 1 ) );
			return new BoxBinaryOperation( left, BoxBinaryOperator.InstanceOf, right, getPosition( expression ), getSourceText( expression ) );
		} else if ( expression.TEQ() != null ) {
			BoxExpr	left	= toAst( file, expression.expression( 0 ) );
			BoxExpr	right	= toAst( file, expression.expression( 1 ) );
			return new BoxComparisonOperation( left, BoxComparisonOperator.TEqual, right, getPosition( expression ), getSourceText( expression ) );
		} else if ( expression.neq() != null ) {
			BoxExpr	left	= toAst( file, expression.expression( 0 ) );
			BoxExpr	right	= toAst( file, expression.expression( 1 ) );
			return new BoxComparisonOperation( left, BoxComparisonOperator.NotEqual, right, getPosition( expression ), getSourceText( expression ) );
		} else if ( expression.gt() != null || ( expression.GREATER() != null && expression.THAN() != null ) && expression.OR() == null ) {
			BoxExpr	left	= toAst( file, expression.expression( 0 ) );
			BoxExpr	right	= toAst( file, expression.expression( 1 ) );
			return new BoxComparisonOperation( left, BoxComparisonOperator.GreaterThan, right, getPosition( expression ), getSourceText( expression ) );
		} else if ( expression.gte() != null || ( expression.GREATER() != null && expression.THAN() != null ) && expression.OR() != null ) {
			BoxExpr	left	= toAst( file, expression.expression( 0 ) );
			BoxExpr	right	= toAst( file, expression.expression( 1 ) );
			return new BoxComparisonOperation( left, BoxComparisonOperator.GreaterThanEquals, right, getPosition( expression ), getSourceText( expression ) );
		} else if ( expression.lt() != null || ( expression.LESS() != null && expression.THAN() != null && expression.OR() == null ) ) {
			BoxExpr	left	= toAst( file, expression.expression( 0 ) );
			BoxExpr	right	= toAst( file, expression.expression( 1 ) );
			return new BoxComparisonOperation( left, BoxComparisonOperator.LessThan, right, getPosition( expression ), getSourceText( expression ) );
		} else if ( expression.lte() != null || ( expression.LESS() != null && expression.THAN() != null && expression.OR() != null ) ) {
			BoxExpr	left	= toAst( file, expression.expression( 0 ) );
			BoxExpr	right	= toAst( file, expression.expression( 1 ) );
			return new BoxComparisonOperation( left, BoxComparisonOperator.LesslThanEqual, right, getPosition( expression ), getSourceText( expression ) );
		} else if ( expression.eq() != null || expression.IS() != null ) {
			BoxExpr	left	= toAst( file, expression.expression( 0 ) );
			BoxExpr	right	= toAst( file, expression.expression( 1 ) );
			return new BoxComparisonOperation( left, BoxComparisonOperator.Equal, right, getPosition( expression ), getSourceText( expression ) );
		} else if ( expression.AMPERSAND().size() > 0 ) {
			List<BoxExpr>				parts	= new ArrayList<>();
			CFParser.ExpressionContext	current	= expression;
			do {
				parts.add( toAst( file, ( CFParser.ExpressionContext ) current.expression().get( 0 ) ) );
				current = current.expression().get( 1 );
			} while ( current.AMPERSAND() != null && current.AMPERSAND().size() > 0 );
			parts.add( toAst( file, ( CFParser.ExpressionContext ) current ) );

			return new BoxStringConcat( parts, getPosition( expression ), getSourceText( expression ) );

		} else if ( expression.EQV() != null ) {
			BoxExpr	left	= toAst( file, expression.expression( 0 ) );

			BoxExpr	right	= toAst( file, expression.expression( 1 ) );
			return new BoxBinaryOperation( left, BoxBinaryOperator.Equivalence, right, getPosition( expression ), getSourceText( expression ) );
		} else if ( expression.IMP() != null ) {
			BoxExpr	left	= toAst( file, expression.expression( 0 ) );

			BoxExpr	right	= toAst( file, expression.expression( 1 ) );
			return new BoxBinaryOperation( left, BoxBinaryOperator.Implies, right, getPosition( expression ), getSourceText( expression ) );
		} else if ( expression.ELVIS() != null ) {
			BoxExpr	left	= toAst( file, expression.expression( 0 ) );

			BoxExpr	right	= toAst( file, expression.expression( 1 ) );
			return new BoxBinaryOperation( left, BoxBinaryOperator.Elvis, right, getPosition( expression ), getSourceText( expression ) );
		} else if ( expression.QM() != null ) {
			BoxExpr	condition	= toAst( file, expression.expression( 0 ) );
			BoxExpr	whenTrue	= toAst( file, expression.expression( 1 ) );
			BoxExpr	whenFalse	= toAst( file, expression.expression( 2 ) );
			return new BoxTernaryOperation( condition, whenTrue, whenFalse, getPosition( expression ), getSourceText( expression ) );
		} else if ( expression.notOrBang() != null && expression.CONTAIN() == null ) {
			BoxExpr expr = toAst( file, expression.expression( 0 ) );
			return new BoxUnaryOperation( expr, BoxUnaryOperator.Not, getPosition( expression ), getSourceText( expression ) );
			// return new BoxNegateOperation( expr, BoxNegateOperator.Not, getPosition( expression ), getSourceText( expression ) );
		} else if ( expression.CONTAINS() != null ) {
			BoxExpr	left	= toAst( file, expression.expression( 0 ) );
			BoxExpr	right	= toAst( file, expression.expression( 1 ) );
			return new BoxBinaryOperation( left, BoxBinaryOperator.Contains, right, getPosition( expression ), getSourceText( expression ) );
		} else if ( expression.CONTAIN() != null && expression.DOES() != null && expression.NOT() != null ) {
			BoxExpr	left	= toAst( file, expression.expression( 0 ) );
			BoxExpr	right	= toAst( file, expression.expression( 1 ) );
			return new BoxBinaryOperation( left, BoxBinaryOperator.NotContains, right, getPosition( expression ), getSourceText( expression ) );
		} else if ( expression.pre != null ) {
			BoxExpr expr = toAst( file, expression.expression( 0 ) );
			if ( expression.PLUSPLUS() != null ) {
				return new BoxUnaryOperation( expr, BoxUnaryOperator.PrePlusPlus, getPosition( expression ), getSourceText( expression ) );
			}
			if ( expression.MINUSMINUS() != null ) {
				return new BoxUnaryOperation( expr, BoxUnaryOperator.PreMinusMinus, getPosition( expression ), getSourceText( expression ) );
			}
		} else if ( expression.post != null ) {
			BoxExpr expr = toAst( file, expression.expression( 0 ) );
			if ( expression.PLUSPLUS() != null ) {
				return new BoxUnaryOperation( expr, BoxUnaryOperator.PostPlusPlus, getPosition( expression ), getSourceText( expression ) );
			}
			if ( expression.MINUSMINUS() != null ) {
				return new BoxUnaryOperation( expr, BoxUnaryOperator.PostMinusMinus, getPosition( expression ), getSourceText( expression ) );
			}
		} else if ( expression.CASTAS() != null ) {
			BoxExpr	left	= toAst( file, expression.expression( 0 ) );
			BoxExpr	right	= toAst( file, expression.expression( 1 ) );
			return new BoxBinaryOperation( left, BoxBinaryOperator.CastAs, right, getPosition( expression ), getSourceText( expression ) );
		} else if ( !expression.ICHAR().isEmpty() ) {
			return toAst( file, expression.expression( 0 ) );
		} else if ( expression.assignment() != null ) {
			CFParser.AssignmentContext	node	= expression.assignment();
			BoxExpr						left	= toAst( file, expression.assignment().assignmentLeft().accessExpression() );
			BoxExpr						right	= toAst( file, expression.assignment().assignmentRight().expression() );
			BoxAssignmentOperator		op		= BoxAssignmentOperator.Equal;
			if ( node.PLUSEQUAL() != null ) {
				op = BoxAssignmentOperator.PlusEqual;
			} else if ( node.MINUSEQUAL() != null ) {
				op = BoxAssignmentOperator.MinusEqual;
			} else if ( node.STAREQUAL() != null ) {
				op = BoxAssignmentOperator.StarEqual;
			} else if ( node.SLASHEQUAL() != null ) {
				op = BoxAssignmentOperator.SlashEqual;
			} else if ( node.MODEQUAL() != null ) {
				op = BoxAssignmentOperator.ModEqual;
			} else if ( node.CONCATEQUAL() != null ) {
				op = BoxAssignmentOperator.ConcatEqual;
			}
			// In the future, we expect there to be more than just var here, thus the list.
			List<BoxAssignmentModifier> modifiers = new ArrayList<BoxAssignmentModifier>();
			if ( node.VAR() != null ) {
				modifiers.add( BoxAssignmentModifier.VAR );
			}

			return new BoxAssignment( left, op, right, modifiers, getPosition( expression ), getSourceText( expression ) );
		} else if ( expression.NULL() != null ) {
			return new BoxNull( getPosition( expression ), getSourceText( expression ) );
		} else if ( expression.anonymousFunction() != null ) {
			/* Lambda declaration */
			if ( expression.anonymousFunction().lambda() != null ) {
				CFParser.LambdaContext			lambda		= expression.anonymousFunction().lambda();
				List<BoxArgumentDeclaration>	args		= new ArrayList<>();
				List<BoxAnnotation>				annotations	= new ArrayList<>();
				List<BoxStatement>				body		= new ArrayList<>();
				/* Process the arguments */
				if ( lambda.functionParamList() != null ) {
					for ( CFParser.FunctionParamContext arg : lambda.functionParamList().functionParam() ) {
						BoxArgumentDeclaration argDeclaration = toAst( file, arg );
						args.add( argDeclaration );
					}
				}
				if ( lambda.identifier() != null ) {
					BoxArgumentDeclaration argDeclaration = new BoxArgumentDeclaration( false, "Any", lambda.identifier().getText(), null, new ArrayList<>(),
					    new ArrayList<>(), getPosition( lambda.identifier() ), getSourceText( lambda.identifier() ) );
					args.add( argDeclaration );
				}
				/* Process the annotations */
				for ( CFParser.PostannotationContext annotation : lambda.postannotation() ) {
					annotations.add( toAst( file, annotation ) );
				}
				/* Process the body */
				if ( lambda.anonymousFunctionBody().statementBlock() != null ) {
					body.addAll( toAst( file, lambda.anonymousFunctionBody().statementBlock() ) );
				} else if ( lambda.anonymousFunctionBody().simpleStatement() != null ) {
					body.add( toAst( file, lambda.anonymousFunctionBody().simpleStatement() ) );
				}
				return new BoxLambda( args, annotations, body, getPosition( expression ), getSourceText( expression ) );
			} else if ( expression.anonymousFunction().closure() != null ) {
				/* Closure declaration */
				CFParser.ClosureContext			closure		= expression.anonymousFunction().closure();
				List<BoxArgumentDeclaration>	args		= new ArrayList<>();
				List<BoxAnnotation>				annotations	= new ArrayList<>();
				List<BoxStatement>				body		= new ArrayList<>();

				if ( closure.functionParamList() != null ) {
					for ( CFParser.FunctionParamContext arg : closure.functionParamList().functionParam() ) {
						BoxArgumentDeclaration argDeclaration = toAst( file, arg );
						args.add( argDeclaration );
					}
				}
				if ( closure.identifier() != null ) {
					BoxArgumentDeclaration argDeclaration = new BoxArgumentDeclaration( false, "Any", closure.identifier().getText(), null, new ArrayList<>(),
					    new ArrayList<>(), getPosition( closure.identifier() ), getSourceText( closure.identifier() ) );
					args.add( argDeclaration );
				}
				/* Process the annotations */
				for ( CFParser.PostannotationContext annotation : closure.postannotation() ) {
					annotations.add( toAst( file, annotation ) );
				}
				/* Process the body */
				// ()=> and ()->{} funnel through anonymousFunctionBody and have statementblock or simplestatement
				if ( closure.anonymousFunctionBody() != null ) {
					if ( closure.anonymousFunctionBody().statementBlock() != null ) {
						body.addAll( toAst( file, closure.anonymousFunctionBody().statementBlock() ) );
					} else if ( closure.anonymousFunctionBody().simpleStatement() != null ) {
						body.add( toAst( file, closure.anonymousFunctionBody().simpleStatement() ) );
					}
					// function() {} syntax always uses statement block
				} else if ( closure.statementBlock() != null ) {
					body.addAll( toAst( file, closure.statementBlock() ) );
				}

				return new BoxClosure( args, annotations, body, getPosition( expression ), getSourceText( expression ) );
			}
		}
		// TODO: add other cases
		throw new IllegalStateException( "expression not implemented: " + getSourceText( expression ) );
	}

	/**
	 * Converts the UnaryContext parser rule to the corresponding AST node.
	 *
	 * @param file source file, if any
	 * @param node ANTLR FqnContext rule
	 */
	private BoxExpr toAst( File file, CFParser.FqnContext node ) {
		return new BoxFQN( node.getText(), getPosition( node ), getSourceText( node ) );
	}

	/**
	 * Converts the UnaryContext parser rule to the corresponding AST node.
	 *
	 * @param file source file, if any
	 * @param node ANTLR UnaryContext rule
	 *
	 * @return corresponding AST BoxUnaryOperation
	 *
	 * @see BoxUnaryOperation
	 * @see BoxUnaryOperator
	 */
	private BoxExpr toAst( File file, CFParser.UnaryContext node ) {

		BoxExpr				expr	= toAst( file, node.expression() );
		BoxUnaryOperator	op		= node.MINUS() != null ? BoxUnaryOperator.Minus : BoxUnaryOperator.Plus;
		if ( expr instanceof BoxBinaryOperation bop ) {
			return new BoxBinaryOperation(
			    new BoxUnaryOperation( bop.getLeft(), op, getPosition( node ), getSourceText( node ) ),
			    bop.getOperator(),
			    bop.getRight(),
			    bop.getPosition(),
			    bop.getSourceText()
			);
		}
		return new BoxUnaryOperation( expr, op, getPosition( node ), getSourceText( node ) );
	}

	/**
	 * Converts the ObjectExpression parser rule to the corresponding AST node. *
	 *
	 * @param file source file, if any
	 * @param node ANTLR ObjectExpressionContext rule
	 *
	 * @return corresponding AST
	 *
	 * @see BoxAccess subclasses
	 * @see BoxIdentifier subclasses
	 */
	private BoxExpr toAst( File file, CFParser.ObjectExpressionContext node ) {
		if ( node.LPAREN() != null ) {
			BoxExpr expr = toAst( file, node.expression() );
			return new BoxParenthesis( expr, getPosition( node ), getSourceText( node ) );
		} else if ( node.functionInvokation() != null )
			return toAst( file, node.functionInvokation() );
		else if ( node.identifier() != null )
			return toAst( file, node.identifier() );
		else if ( node.literalExpression() != null ) {
			return toAst( file, node.literalExpression() );
		} else if ( node.new_() != null ) {
			return toAst( file, node.new_() );
		}

		throw new IllegalStateException( "ObjectExpression not implemented: " + getSourceText( node ) );

	}

	/**
	 * Converts the NewContext parser rule to the corresponding AST node. *
	 *
	 * @param file source file, if any
	 * @param node ANTLR NewContext rule
	 *
	 * @return corresponding AST
	 */
	private BoxExpr toAst( File file, NewContext node ) {
		BoxExpr				expr	= null;
		BoxIdentifier		prefix	= null;
		List<BoxArgument>	args	= toAst( file, node.argumentList() );
		if ( node.fqn() != null ) {
			expr = toAst( file, node.fqn() );
		}
		if ( node.stringLiteral() != null ) {
			expr = toAst( file, node.stringLiteral() );
		}
		if ( node.identifier() != null ) {
			var tmp = toAst( file, node.identifier() );
			if ( tmp instanceof BoxIdentifier bi ) {
				prefix = bi;
			} else {
				prefix = new BoxIdentifier( ( ( BoxScope ) tmp ).getName(), getPosition( node.identifier() ), getSourceText( node.identifier() ) );
			}

		}
		return new BoxNewOperation( prefix, expr, args, getPosition( node ), getSourceText( node ) );
	}

	/**
	 * Converts the ObjectExpression parser rule to the corresponding AST node. * @param file
	 *
	 * @param file source file, if any
	 * @param node ANTLR LiteralExpressionContext rule
	 *
	 * @return corresponding AST BoxAccess or an LiteralExpressionContext
	 *
	 * @see BoxAccess subclasses
	 * @see BoxIdentifier subclasses
	 */
	private BoxExpr toAst( File file, CFParser.LiteralExpressionContext node ) {
		if ( node.stringLiteral() != null ) {
			return toAst( file, node.stringLiteral() );
		}
		if ( node.integerLiteral() != null ) {
			CFParser.IntegerLiteralContext inode = node.integerLiteral();
			return new BoxIntegerLiteral(
			    inode.getText(),
			    getPosition( inode ),
			    getSourceText( inode )
			);
		}
		if ( node.floatLiteral() != null ) {
			CFParser.FloatLiteralContext fnode = node.floatLiteral();
			return new BoxDecimalLiteral(
			    fnode.getText(),
			    getPosition( fnode ),
			    getSourceText( fnode )
			);
		}
		if ( node.booleanLiteral() != null ) {
			CFParser.BooleanLiteralContext bnode = node.booleanLiteral();
			return new BoxBooleanLiteral(
			    bnode.getText(),
			    getPosition( bnode ),
			    getSourceText( bnode ) );
		}
		if ( node.arrayExpression() != null ) {
			return toAst( file, node.arrayExpression() );
		}

		if ( node.structExpression() != null ) {
			return toAst( file, node.structExpression() );
		}

		throw new IllegalStateException( "LiteralExpression not implemented: " + getSourceText( node ) );

	}

	/**
	 * Converts the Struct Expression parser rule to the corresponding AST node.
	 *
	 * @param file source file, if any
	 * @param node ANTLR ArrayExpressionContext rule
	 *
	 * @return corresponding AST BoxArray
	 *
	 * @see BoxArrayLiteral subclasses
	 */
	private BoxExpr toAst( File file, CFParser.StructExpressionContext node ) {
		List<BoxExpr>	values	= new ArrayList<>();
		BoxStructType	type	= node.RBRACKET() != null ? BoxStructType.Ordered : BoxStructType.Unordered;
		if ( node.structMembers() != null ) {
			for ( CFParser.StructMemberContext pair : node.structMembers().structMember() ) {
				if ( pair.stringLiteral() != null ) {
					values.add( toAst( file, pair.stringLiteral() ) );
				} else if ( pair.identifier() != null ) {
					values.add( toAst( file, pair.identifier() ) );
				}
				values.add( toAst( file, pair.expression() ) );
			}
		}
		return new BoxStructLiteral( type, values, getPosition( node ), getSourceText( node ) );
	}

	/**
	 * Converts the Array Expression parser rule to the corresponding AST node.
	 *
	 * @param file source file, if any
	 * @param node ANTLR ArrayExpressionContext rule
	 *
	 * @return corresponding AST BoxArray
	 *
	 * @see BoxArrayLiteral subclasses
	 */
	private BoxExpr toAst( File file, CFParser.ArrayExpressionContext node ) {
		List<BoxExpr> values = new ArrayList<>();
		if ( node.arrayValues() != null ) {
			for ( CFParser.ExpressionContext value : node.arrayValues().expression() ) {
				values.add( toAst( file, value ) );
			}
		}
		return new BoxArrayLiteral( values, getPosition( node ), getSourceText( node ) );
	}

	/**
	 * Converts the Function Invocation parser rule to the corresponding AST node
	 *
	 * @param file source file, if any
	 * @param node ANTLR FunctionInvokationContext rule
	 *
	 * @return corresponding AST BoxFunctionInvocation
	 *
	 * @see BoxFunctionInvocation subclasses
	 * @see BoxArgument subclasses
	 */
	private BoxExpr toAst( File file, CFParser.FunctionInvokationContext node ) {
		List<BoxArgument> args = toAst( file, node.invokationExpression().argumentList() );
		return new BoxFunctionInvocation( node.identifier().getText(),
		    args,
		    getPosition( node ), getSourceText( node ) );
	}

	/**
	 * Converts the argument list to the corresponding List of AST nodes
	 *
	 * @param file source file, if any
	 * @param node ANTLR ArgumentListContext rule
	 *
	 * @return corresponding List of AST nodes
	 *
	 */
	private List<BoxArgument> toAst( File file, CFParser.ArgumentListContext node ) {
		List<BoxArgument>	args	= new ArrayList<>();
		Boolean				isNamed	= false;
		if ( node != null ) {
			for ( CFParser.NamedArgumentContext arg : node.namedArgument() ) {
				isNamed = true;
				args.add( toAst( file, arg ) );
			}
			for ( CFParser.PositionalArgumentContext arg : node.positionalArgument() ) {
				if ( isNamed ) {
					issues.add( new Issue( "You cannot mix named and positional arguments", getPosition( arg ) ) );
				}
				args.add( toAst( file, arg ) );
			}
		}
		return args;
	}

	/**
	 * Converts the PositionalArgumentContext parser rule to the corresponding AST node.
	 *
	 * @param file source file, if any
	 * @param node ANTLR PositionalArgumentContext rule
	 *
	 * @return corresponding AST PositionalArgumentContext
	 *
	 * @see BoxArgument
	 */
	private BoxArgument toAst( File file, CFParser.PositionalArgumentContext node ) {
		BoxExpr value = toAst( file, node.expression() );
		return new BoxArgument( null, value, getPosition( node ), getSourceText( node ) );
	}

	/**
	 * Converts the NamedArgumentContext parser rule to the corresponding AST node.
	 *
	 * @param file source file, if any
	 * @param node ANTLR NamedArgumentContext rule
	 *
	 * @return corresponding AST NamedArgumentContext
	 *
	 * @see BoxArgument
	 */
	private BoxArgument toAst( File file, CFParser.NamedArgumentContext node ) {
		BoxExpr name;
		if ( node.identifier() != null ) {
			name = new BoxStringLiteral( node.identifier().getText(), getPosition( node ), getSourceText( node ) );
		} else {
			name = toAst( file, node.stringLiteral() );
		}
		BoxExpr value = toAst( file, node.expression() );
		return new BoxArgument( name, value, getPosition( node ), getSourceText( node ) );
	}

	/**
	 * Converts the Function declaration parser rule to the corresponding AST node.
	 *
	 * @param file source file, if any
	 * @param node ANTLR FunctionContext rule
	 *
	 * @return corresponding AST BoxFunctionDeclaration
	 *
	 * @see BoxFunctionDeclaration
	 */
	private BoxStatement toAst( File file, CFParser.FunctionContext node ) {
		BoxReturnType						returnType		= null;
		String								name			= "undefined";
		List<BoxStatement>					body			= new ArrayList<>();
		List<BoxArgumentDeclaration>		args			= new ArrayList<>();
		List<BoxAnnotation>					annotations		= new ArrayList<>();
		List<BoxAnnotation>					annToRemove		= new ArrayList<>();
		List<BoxDocumentationAnnotation>	documentation	= new ArrayList<>();
		List<BoxDocumentationAnnotation>	docToRemove		= new ArrayList<>();
		BoxAccessModifier					modifier		= null;

		BoxDocumentation					docs			= getDocIndex( node );
		if ( docs != null ) {
			for ( BoxNode n : docs.getAnnotations() ) {
				documentation.add( ( BoxDocumentationAnnotation ) n );
			}
		}

		for ( CFParser.PreannotationContext annotation : node.functionSignature().preannotation() ) {
			annotations.add( toAst( file, annotation ) );
		}
		for ( CFParser.PostannotationContext annotation : node.postannotation() ) {
			annotations.add( toAst( file, annotation ) );
		}
		if ( node.functionSignature().identifier() != null ) {
			name = node.functionSignature().identifier().getText();
		}

		if ( node.functionSignature().functionParamList() != null ) {
			for ( CFParser.FunctionParamContext arg : node.functionSignature().functionParamList().functionParam() ) {
				BoxArgumentDeclaration argDeclaration = toAst( file, arg );
				/* Resolve annotations @name.key "value" */
				for ( BoxAnnotation pre : annotations ) {
					String prename = pre.getKey().getValue();
					if ( prename.toLowerCase().startsWith( argDeclaration.getName().toLowerCase() ) ) {
						if ( prename.indexOf( '.' ) > -1 ) {
							prename = pre.getKey().getValue().substring( 0, pre.getKey().getValue().indexOf( "." ) );

						} else {
							prename = "hint";
						}
						BoxFQN key = new BoxFQN(
						    pre.getKey().getValue().substring( pre.getKey().getValue().indexOf( "." ) + 1 ), pre.getPosition(),
						    pre.getSourceText()
						);
						argDeclaration.getAnnotations().add(
						    new BoxAnnotation( key, pre.getValue(), pre.getPosition(), pre.getSourceText() )
						);
						annToRemove.add( pre );
					}
				}
				/* Resolve documentation @name.key "value" */
				for ( BoxDocumentationAnnotation doc : documentation ) {
					String docname = doc.getKey().getValue();
					if ( docname.indexOf( '.' ) > -1 ) {
						docname = doc.getKey().getValue().substring( 0, doc.getKey().getValue().indexOf( "." ) );
						if ( argDeclaration.getName().equalsIgnoreCase( docname ) ) {
							BoxFQN key = new BoxFQN(
							    doc.getKey().getValue().substring( doc.getKey().getValue().indexOf( "." ) + 1 ), doc.getPosition(),
							    doc.getSourceText()
							);
							argDeclaration.getDocumentation().add(
							    new BoxDocumentationAnnotation( key, doc.getValue(), doc.getPosition(), doc.getSourceText() )
							);
							docToRemove.add( doc );
						}
					} else {
						/* Case @name add hint */
						if ( argDeclaration.getName().equalsIgnoreCase( docname ) ) {
							BoxFQN hint = new BoxFQN(
							    "hint",
							    doc.getPosition(),
							    doc.getSourceText()
							);
							argDeclaration.getDocumentation().add(
							    new BoxDocumentationAnnotation( hint,
							        doc.getValue(), doc.getPosition(), doc.getSourceText() )
							);
							docToRemove.add( doc );
						}
					}
				}
				args.add( argDeclaration );
			}
		}

		if ( node.functionSignature().accessModifier() != null ) {
			if ( node.functionSignature().accessModifier().PUBLIC() != null ) {
				modifier = BoxAccessModifier.Public;
			} else if ( node.functionSignature().accessModifier().PRIVATE() != null ) {
				modifier = BoxAccessModifier.Private;
			} else if ( node.functionSignature().accessModifier().REMOTE() != null ) {
				modifier = BoxAccessModifier.Remote;
			} else if ( node.functionSignature().accessModifier().PACKAGE() != null ) {
				modifier = BoxAccessModifier.Package;
			}
		}
		if ( node.functionSignature().returnType() != null ) {
			var targetType = node.functionSignature().returnType().type();
			if ( targetType != null ) {
				if ( targetType.BOOLEAN() != null ) {
					returnType = new BoxReturnType( BoxType.Boolean, null, getPosition( targetType ), getSourceText( targetType ) );
				}
				if ( targetType.NUMERIC() != null ) {
					returnType = new BoxReturnType( BoxType.Numeric, null, getPosition( targetType ), getSourceText( targetType ) );
				}
				if ( targetType.STRING() != null ) {
					returnType = new BoxReturnType( BoxType.String, null, getPosition( targetType ), getSourceText( targetType ) );
				}
			}
			// TODO
			// Specification required to map the types
		}
		if ( node.statementBlock() != null ) {
			body.addAll( toAst( file, node.statementBlock() ) );
		}
		annotations.removeAll( annToRemove );
		documentation.removeAll( docToRemove );

		return new BoxFunctionDeclaration( modifier, name, returnType, args, annotations, documentation, body, getPosition( node ), getSourceText( node ) );
	}

	private BoxDocumentation getDocIndex( ParserRuleContext node ) {
		int	min		= Integer.MAX_VALUE;
		int	index	= -1;
		for ( BoxNode doc : this.javadocs ) {
			int distance = getPosition( node ).getStart().getLine() - doc.getPosition().getEnd().getLine();
			if ( distance >= 0 && distance < min ) {
				min		= distance;
				index	= this.javadocs.indexOf( doc );
			}
		}
		// remove element from the list and return it
		if ( index > -1 ) {
			return this.javadocs.remove( index );
		}
		return null;
	}

	/**
	 * Converts the AttributeSimple parser rule to the corresponding AST node.
	 *
	 * @param file source file, if any
	 * @param node ANTLR AttributeSimpleContext rule
	 *
	 * @return corresponding AST AttributeSimpleContext
	 *
	 */
	private BoxExpr toAst( File file, CFParser.AttributeSimpleContext node ) {
		if ( node.literalExpression() != null ) {
			return toAst( file, node.literalExpression() );
		} else if ( node.identifier() != null ) {
			// Converting an identifer to a string literal here in the AST removes ambiguity, but also loses the
			// lexical context of the original source code.
			return new BoxStringLiteral( node.identifier().getText(), getPosition( node ), getSourceText( node ) );
		}
		throw new IllegalStateException( "AttributeSimple not implemented: " + getSourceText( node ) );
	}

	/**
	 * Converts the pre annotation parser rule to the corresponding AST node.
	 *
	 * @param file source file, if any
	 * @param node ANTLR PreannotationContext rule
	 *
	 * @return corresponding AST PreannotationContext
	 *
	 * @see PreannotationContext
	 */
	private BoxAnnotation toAst( File file, CFParser.PreannotationContext annotation ) {
		BoxExpr	avalue	= null;
		BoxExpr	aname	= toAst( file, annotation.fqn() );
		if ( annotation.literalExpression().size() == 1 ) {
			avalue = toAst( file, annotation.literalExpression( 0 ) );
		} else if ( annotation.literalExpression().size() > 1 ) {
			List<BoxExpr> values = new ArrayList<>();
			for ( CFParser.LiteralExpressionContext value : annotation.literalExpression() ) {
				values.add( toAst( file, value ) );
			}
			avalue = new BoxArrayLiteral( values, getPosition( annotation ), getSourceText( annotation ) );
		} else {
			avalue = null;
		}
		return new BoxAnnotation( ( BoxFQN ) aname, avalue, getPosition( annotation ), getSourceText( annotation ) );
	}

	/**
	 * Converts the Function argument parser rule to the corresponding AST node.
	 *
	 * @param file source file, if any
	 * @param node ANTLR ParamContext rule
	 *
	 * @return corresponding AST BoxArgumentDeclaration
	 *
	 * @see BoxArgumentDeclaration
	 */
	private BoxArgumentDeclaration toAst( File file, CFParser.FunctionParamContext node ) {
		Boolean								required		= false;
		String								type			= "Any";
		String								name			= "undefined";
		BoxExpr								expr			= null;
		List<BoxAnnotation>					annotations		= new ArrayList<>();
		List<BoxDocumentationAnnotation>	documentation	= new ArrayList<>();

		name = node.identifier().getText();
		if ( node.REQUIRED() != null ) {
			required = true;
		}

		if ( node.expression() != null ) {
			expr = toAst( file, node.expression() );
		}
		if ( node.type() != null ) {
			type = node.type().getText();
		}
		for ( CFParser.PostannotationContext annotation : node.postannotation() ) {
			annotations.add( toAst( file, annotation ) );
		}

		return new BoxArgumentDeclaration( required, type, name, expr, annotations, documentation, getPosition( node ), getSourceText( node ) );
	}

	/**
	 * Converts a post annotation into the corresponding AST node
	 *
	 * @param file source file, if any
	 * @param node ANTLR PostannotationContext rule
	 *
	 * @return corresponding AST PostannotationContext
	 *
	 * @see BoxAnnotation
	 */
	private BoxAnnotation toAst( File file, CFParser.PostannotationContext node ) {

		BoxFQN	name	= new BoxFQN( node.key.getText(), getPosition( node.key ), getSourceText( node.key ) );
		BoxExpr	value;
		if ( node.value != null ) {
			value = toAst( file, node.value );
		} else {
			value = null;
		}
		return new BoxAnnotation( name, value, getPosition( node ), getSourceText( node ) );
	}

	/**
	 * Converts a Property into the corresponding AST node
	 *
	 * @param file source file, if any
	 * @param node ANTLR PropertyContext rule
	 *
	 * @return corresponding AST PropertyContext
	 *
	 */
	private BoxProperty toAst( File file, CFParser.PropertyContext node ) {
		List<BoxAnnotation>					annotations		= new ArrayList<>();
		List<BoxDocumentationAnnotation>	documentation	= new ArrayList<>();

		for ( CFParser.PreannotationContext annotation : node.preannotation() ) {
			annotations.add( toAst( file, annotation ) );
		}
		for ( CFParser.PostannotationContext annotation : node.postannotation() ) {
			annotations.add( toAst( file, annotation ) );
		}

		BoxDocumentation doc = getDocIndex( node );
		if ( doc != null ) {
			for ( BoxNode n : doc.getAnnotations() ) {
				documentation.add( ( BoxDocumentationAnnotation ) n );
			}
		}

		return new BoxProperty( annotations, documentation, getPosition( node ), getSourceText( node ) );
	}

}