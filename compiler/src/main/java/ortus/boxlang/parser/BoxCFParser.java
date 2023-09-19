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

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BOMInputStream;
import ortus.boxlang.parser.CFLexer;
import ortus.boxlang.parser.CFParser;
import ortus.boxlang.ast.*;
import ortus.boxlang.ast.expression.*;
import ortus.boxlang.ast.BoxStatement;
import ortus.boxlang.ast.statement.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Parser for CF scripts
 */
public class BoxCFParser extends BoxAbstractParser {

	/**
	 * Constructor
	 */
	public BoxCFParser() {
		super();
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
			BoxScript ast = parseTreeToAst( file, parseTree );
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
		InputStream				inputStream	= IOUtils.toInputStream( code );

		CFParser.ScriptContext	parseTree	= ( CFParser.ScriptContext ) parserFirstStage( inputStream );
		if ( issues.isEmpty() ) {
			BoxScript ast = parseTreeToAst( file, parseTree );
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
		InputStream	inputStream	= IOUtils.toInputStream( code );

		CFLexer		lexer		= new CFLexer( CharStreams.fromStream( inputStream ) );
		CFParser	parser		= new CFParser( new CommonTokenStream( lexer ) );
		addErrorListeners( lexer, parser );
		CFParser.ExpressionContext	parseTree	= parser.expression();
		BoxExpr						ast			= toAst( null, parseTree );
		return new ParsingResult( ast, issues );
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
		InputStream	inputStream	= IOUtils.toInputStream( code );

		CFLexer		lexer		= new CFLexer( CharStreams.fromStream( inputStream ) );
		CFParser	parser		= new CFParser( new CommonTokenStream( lexer ) );
		addErrorListeners( lexer, parser );
		CFParser.StatementContext	parseTree	= parser.statement();
		BoxStatement				ast			= toAst( null, parseTree );
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
		CFLexer		lexer	= new CFLexer( CharStreams.fromStream( stream ) );
		CFParser	parser	= new CFParser( new CommonTokenStream( lexer ) );
		addErrorListeners( lexer, parser );

		return parser.script();
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
	protected BoxScript parseTreeToAst( File file, ParserRuleContext rule ) {
		CFParser.ScriptContext	parseTree	= ( CFParser.ScriptContext ) rule;
		List<BoxStatement>		statements	= new ArrayList<>();
		parseTree.functionOrStatement().forEach( stmt -> {
			statements.add( toAst( file, stmt ) );
		} );
		return new BoxScript( statements, getPosition( rule ), getSourceText( rule ) );
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
		if ( node.constructor() != null ) {
			return toAst( file, node.constructor() );
		} else if ( node.function() != null ) {
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
		} else if ( node.break_() != null ) {
			return toAst( file, node.break_() );
		} else if ( node.continue_() != null ) {
			return toAst( file, node.continue_() );
		} else if ( node.switch_() != null ) {
			return toAst( file, node.switch_() );
		} else if ( node.for_() != null ) {
			return toAst( file, node.for_() );
		} else if ( node.assert_() != null ) {
			return toAst( file, node.assert_() );
		} else {
			throw new IllegalStateException( "not implemented: " + node.getClass().getSimpleName() );
		}
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
			BoxExpr	variable	= toAst( file, node.identifier() );
			BoxExpr	collection	= toAst( file, node.expression() );

			return new BoxForIn( variable, collection, body, getPosition( node ), getSourceText( node ) );
		}
		BoxExpr	variable	= toAst( file, node.forAssignment().expression( 0 ) );
		BoxExpr	initial		= toAst( file, node.forAssignment().expression( 1 ) );
		BoxExpr	condition	= toAst( file, node.forCondition().expression() );
		BoxExpr	step		= toAst( file, node.forIncrement().expression() );

		return new BoxForIndex( variable, initial, condition, step, body, getPosition( node ), getSourceText( node ) );
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
			cases.add( toAst( file, c, condition ) );
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
	private BoxSwitchCase toAst( File file, CFParser.CaseContext node, BoxExpr condition ) {
		BoxExpr expr = null;
		if ( node.expression() != null ) {
			BoxExpr temp = toAst( file, node.expression() );
			if ( !temp.isLiteral() ) {
				expr = temp;
			} else {
				expr = new BoxComparisonOperation( condition, BoxComparisonOperator.Equal, temp, getPosition( node.expression() ),
				    getSourceText( node.expression() ) );
			}

		}

		List<BoxStatement> statements = new ArrayList<>();
		if ( node.statement() != null ) {
			statements.add( toAst( file, node.statement() ) );
		}
		if ( node.statementBlock() != null ) {
			statements.addAll( toAst( file, node.statementBlock() ) );
		}

		return new BoxSwitchCase( expr, statements, getPosition( node ), getSourceText( node ) );
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
		BoxExpr				condition	= toAst( file, node.expression() );
		List<BoxStatement>	body		= new ArrayList<>();

		if ( node.statementBlock() != null ) {
			body.addAll( toAst( file, node.statementBlock() ) );
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
		if ( node.assignment() != null ) {
			return toAst( file, node.assignment() );
		} else if ( node.methodInvokation() != null ) {
			BoxExpr expr = toAst( file, node.methodInvokation() );
			return new BoxExpression( expr, getPosition( node ), getSourceText( node ) );
		} else if ( node.localDeclaration() != null ) {
			List<BoxExpr>	identifiers	= node.localDeclaration().identifier().stream().map( it -> toAst( file, it ) ).toList();
			BoxExpr			expr		= null;
			if ( node.localDeclaration().expression() != null ) {
				expr = toAst( file, node.localDeclaration().expression() );
			}
			return new BoxLocalDeclaration( identifiers, expr, getPosition( node ), getSourceText( node ) );
		} else if ( node.incrementDecrementStatement() != null ) {
			return toAst( file, node.incrementDecrementStatement() );
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
	 * Converts the Assignment parser rule to the corresponding AST node.
	 *
	 * @param file source file, if any
	 * @param node ANTLR AssignmentContext rule
	 *
	 * @return the corresponding AST BoxStatement subclass
	 *
	 * @see BoxAssignment
	 */
	private BoxStatement toAst( File file, CFParser.AssignmentContext node ) {
		BoxExpr					left	= toAst( file, node.assignmentLeft() );
		BoxExpr					right	= toAst( file, node.assignmentRight() );
		BoxAssigmentOperator	op		= BoxAssigmentOperator.Equal;
		if ( node.PLUSEQUAL() != null ) {
			op		= BoxAssigmentOperator.PlusEqual;
		} else if ( node.MINUSEQUAL() != null ) {
			op = BoxAssigmentOperator.MinusEqual;
		} else if ( node.STAREQUAL() != null ) {
			op = BoxAssigmentOperator.StarEqual;
		} else if ( node.SLASHEQUAL() != null ) {
			op = BoxAssigmentOperator.SlashEqual;
		} else if ( node.MODEQUAL() != null ) {
			op = BoxAssigmentOperator.ModEqual;
		} else if ( node.CONCATEQUAL() != null ) {
			op = BoxAssigmentOperator.ConcatEqual;

		}
		return new BoxAssignment( left, op, right, getPosition( node ), getSourceText( node ) );
	}

	/**
	 * Converts the AssignmentLeft parser rule to the corresponding AST node.
	 *
	 * @param file source file, if any
	 * @param node ANTLR AssignmentLeftContext rule
	 *
	 * @return the corresponding AST BoxExpression subclass
	 *
	 * @see BoxExpr
	 */
	private BoxExpr toAst( File file, CFParser.AssignmentLeftContext node ) {
		// TODO: review case with assignmentLeft
		return toAst( file, node.accessExpression() );
	}

	/**
	 * Converts the AssignmentRightContext parser rule to the corresponding AST node.
	 *
	 * @param file source file, if any
	 * @param node ANTLR AssignmentLeftContext rule
	 *
	 * @return the corresponding AST BoxExpression subclass
	 *
	 * @see BoxExpr
	 */
	private BoxExpr toAst( File file, CFParser.AssignmentRightContext node ) {
		return toAst( file, node.expression() );
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
	 * @see BoxObjectAccess
	 */
	private BoxExpr toAst( File file, CFParser.AccessExpressionContext node ) {
		if ( node.identifier() != null )
			return toAst( file, node.identifier() );
		if ( node.arrayAccess() != null )
			return toAst( file, node.arrayAccess() );
		if ( node.objectExpression() != null ) {
			BoxExpr	context	= toAst( file, node.objectExpression() );
			BoxExpr	target	= toAst( file, node.accessExpression() );
			return new BoxObjectAccess( context, target, getPosition( node ), getSourceText( node ) );
		}

		throw new IllegalStateException( "not implemented: " + node.getClass().getSimpleName() );
	}

	/**
	 * Converts the AccessExpression parser rule to the corresponding AST node.
	 *
	 * @param file source file, if any
	 * @param node ANTLR ArrayAccessContext rule
	 *
	 * @return the corresponding AST BoxArrayAccess
	 *
	 * @see BoxArrayAccess
	 */
	private BoxExpr toAst( File file, CFParser.ArrayAccessContext node ) {
		BoxExpr	index	= toAst( file, node.arrayAccessIndex().expression() );
		BoxExpr	context	= toAst( file, node.identifier() );
		return new BoxArrayAccess( context, index, getPosition( node ), getSourceText( node ) );
	}

	/**
	 * Converts the IdentifierContext parser rule to the corresponding AST node.
	 *
	 * @param file source file, if any
	 * @param node ANTLR ArrayAccessContext rule
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
		return new BoxIdentifier( node.getText(), getPosition( node ), getSourceText( node ) );
	}

	/**
	 * Converts the Scope parser rule to the corresponding AST node.
	 *
	 * @param file source file, if any
	 * @param node ANTLR ArrayAccessContext rule
	 *
	 * @return corresponding AST BoxScope
	 *
	 * @see BoxScope for the reserved keywords used to identify a scope
	 */
	private BoxExpr toAst( File file, CFParser.ScopeContext node ) {
		if ( node.VARIABLES() != null ) {
			return new BoxScope( node.VARIABLES().getText(), getPosition( node ), getSourceText( node ) );
		}
		throw new IllegalStateException( "not implemented: " + node.getClass().getSimpleName() );
	}

	/**
	 * Converts the Expression parser rule to the corresponding AST node.
	 * The operator precedence resolved in the ANTLR grammar
	 *
	 * @param file       source file, if any
	 * @param expression ANTLR ArrayAccessContext rule
	 *
	 * @return corresponding AST BoxExpr subclass
	 *
	 * @see BoxExpr subclasses
	 * @see BoxBinaryOperator
	 */
	private BoxExpr toAst( File file, CFParser.ExpressionContext expression ) {
		if ( expression.literalExpression() != null ) {
			if ( expression.literalExpression().stringLiteral() != null ) {
				CFParser.StringLiteralContext node = expression.literalExpression().stringLiteral();
				return new BoxStringLiteral(
				    node.getText(),
				    getPosition( node ),
				    getSourceText( node )
				);
			}
			if ( expression.literalExpression().integerLiteral() != null ) {
				CFParser.IntegerLiteralContext node = expression.literalExpression().integerLiteral();
				return new BoxIntegerLiteral(
				    node.getText(),
				    getPosition( node ),
				    getSourceText( node )
				);
			}
			if ( expression.literalExpression().floatLiteral() != null ) {
				CFParser.FloatLiteralContext node = expression.literalExpression().floatLiteral();
				return new BoxDecimalLiteral(
				    node.getText(),
				    getPosition( node ),
				    getSourceText( node )
				);
			}
			if ( expression.literalExpression().booleanLiteral() != null ) {
				CFParser.BooleanLiteralContext node = expression.literalExpression().booleanLiteral();
				return new BoxBooleanLiteral(
				    node.getText(),
				    getPosition( node ),
				    getSourceText( node ) );
			}
		} else if ( expression.identifier() != null ) {
			return toAst( file, expression.identifier() );
		} else if ( expression.accessExpression() != null ) {
			return toAst( file, expression.accessExpression() );
		} else if ( expression.objectExpression() != null ) {
			return toAst( file, expression.objectExpression() );
		} else if ( expression.methodInvokation() != null ) {
			return toAst( file, expression.methodInvokation() );
		} else if ( expression.unary() != null ) {
			return toAst( file, expression.unary() );
		} else if ( expression.AND() != null ) {
			BoxExpr	left	= toAst( file, expression.expression( 0 ) );
			BoxExpr	right	= toAst( file, expression.expression( 1 ) );
			return new BoxBinaryOperation( left, BoxBinaryOperator.And, right, getPosition( expression ), getSourceText( expression ) );
		} else if ( expression.OR() != null ) {
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
		} else if ( expression.POWER() != null ) {
			BoxExpr	left	= toAst( file, expression.expression( 0 ) );
			BoxExpr	right	= toAst( file, expression.expression( 1 ) );
			return new BoxBinaryOperation( left, BoxBinaryOperator.Power, right, getPosition( expression ), getSourceText( expression ) );
		} else if ( expression.XOR() != null ) {
			BoxExpr	left	= toAst( file, expression.expression( 0 ) );
			BoxExpr	right	= toAst( file, expression.expression( 1 ) );
			return new BoxBinaryOperation( left, BoxBinaryOperator.Xor, right, getPosition( expression ), getSourceText( expression ) );
		} else if ( expression.MOD() != null ) {
			BoxExpr	left	= toAst( file, expression.expression( 0 ) );
			BoxExpr	right	= toAst( file, expression.expression( 1 ) );
			return new BoxBinaryOperation( left, BoxBinaryOperator.Mod, right, getPosition( expression ), getSourceText( expression ) );
		} else if ( expression.INSTANCEOF() != null ) {
			BoxExpr	left	= toAst( file, expression.expression( 0 ) );
			BoxExpr	right	= toAst( file, expression.expression( 1 ) );
			return new BoxBinaryOperation( left, BoxBinaryOperator.InstanceOf, right, getPosition( expression ), getSourceText( expression ) );
		} else if ( expression.EQ() != null || expression.IS() != null ) {
			BoxExpr	left	= toAst( file, expression.expression( 0 ) );
			BoxExpr	right	= toAst( file, expression.expression( 1 ) );
			return new BoxComparisonOperation( left, BoxComparisonOperator.Equal, right, getPosition( expression ), getSourceText( expression ) );
		} else if ( expression.TEQ() != null ) {
			BoxExpr	left	= toAst( file, expression.expression( 0 ) );
			BoxExpr	right	= toAst( file, expression.expression( 1 ) );
			return new BoxComparisonOperation( left, BoxComparisonOperator.TEqual, right, getPosition( expression ), getSourceText( expression ) );
		} else if ( expression.NEQ() != null ) {
			BoxExpr	left	= toAst( file, expression.expression( 0 ) );
			BoxExpr	right	= toAst( file, expression.expression( 1 ) );
			return new BoxComparisonOperation( left, BoxComparisonOperator.NotEqual, right, getPosition( expression ), getSourceText( expression ) );
		} else if ( expression.GT() != null ) {
			BoxExpr	left	= toAst( file, expression.expression( 0 ) );
			BoxExpr	right	= toAst( file, expression.expression( 1 ) );
			return new BoxComparisonOperation( left, BoxComparisonOperator.GreaterThan, right, getPosition( expression ), getSourceText( expression ) );
		} else if ( expression.GTE() != null ) {
			BoxExpr	left	= toAst( file, expression.expression( 0 ) );
			BoxExpr	right	= toAst( file, expression.expression( 1 ) );
			return new BoxComparisonOperation( left, BoxComparisonOperator.GreaterThanEquals, right, getPosition( expression ), getSourceText( expression ) );
		} else if ( expression.LT() != null ) {
			BoxExpr	left	= toAst( file, expression.expression( 0 ) );
			BoxExpr	right	= toAst( file, expression.expression( 1 ) );
			return new BoxComparisonOperation( left, BoxComparisonOperator.LessThan, right, getPosition( expression ), getSourceText( expression ) );
		} else if ( expression.LTE() != null ) {
			BoxExpr	left	= toAst( file, expression.expression( 0 ) );
			BoxExpr	right	= toAst( file, expression.expression( 1 ) );
			return new BoxComparisonOperation( left, BoxComparisonOperator.LesslThanEqual, right, getPosition( expression ), getSourceText( expression ) );
		} else if ( expression.AMPERSAND() != null ) {
			BoxExpr	left	= toAst( file, expression.expression( 0 ) );
			BoxExpr	right	= toAst( file, expression.expression( 1 ) );
			return new BoxBinaryOperation( left, BoxBinaryOperator.Concat, right, getPosition( expression ), getSourceText( expression ) );
		} else if ( expression.ELVIS() != null ) {
			BoxExpr	left	= toAst( file, expression.expression( 0 ) );
			BoxExpr	right	= toAst( file, expression.expression( 1 ) );
			return new BoxBinaryOperation( left, BoxBinaryOperator.Elvis, right, getPosition( expression ), getSourceText( expression ) );
		} else if ( expression.QM() != null ) {
			BoxExpr	condition	= toAst( file, expression.expression( 0 ) );
			BoxExpr	whenTrue	= toAst( file, expression.expression( 1 ) );
			BoxExpr	whenFalse	= toAst( file, expression.expression( 2 ) );
			return new BoxTernaryOperation( condition, whenTrue, whenFalse, getPosition( expression ), getSourceText( expression ) );
		} else if ( expression.not() != null && expression.CONTAIN() == null ) {
			BoxExpr expr = toAst( file, expression.not().expression() );
			return new BoxNegateOperation( expr, BoxNegateOperator.Not, getPosition( expression ), getSourceText( expression ) );
		} else if ( expression.CONTAINS() != null ) {
			BoxExpr	left	= toAst( file, expression.expression( 0 ) );
			BoxExpr	right	= toAst( file, expression.expression( 1 ) );
			return new BoxBinaryOperation( left, BoxBinaryOperator.Contains, right, getPosition( expression ), getSourceText( expression ) );
		} else if ( expression.CONTAIN() != null && expression.DOES() != null && expression.NOT() != null ) {
			BoxExpr	left	= toAst( file, expression.expression( 0 ) );
			BoxExpr	right	= toAst( file, expression.expression( 1 ) );
			return new BoxBinaryOperation( left, BoxBinaryOperator.NotContains, right, getPosition( expression ), getSourceText( expression ) );
		} else if ( expression.LPAREN() != null ) {
			BoxExpr expr = toAst( file, expression.expression( 0 ) );
			return new BoxParenthesis( expr, getPosition( expression ), getSourceText( expression ) );
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
		}
		// TODO: add other cases
		throw new IllegalStateException( "not implemented: " + expression.getClass().getSimpleName() );
	}

	/**
	 * Converts the UnaryContext parser rule to the corresponding AST node.
	 *
	 * @param file source file, if any
	 * @param node ANTLR ArrayAccessContext rule
	 *
	 * @return corresponding AST BoxUnaryOperation
	 *
	 * @see BoxUnaryOperation
	 * @see BoxUnaryOperator
	 */
	private BoxExpr toAst( File file, CFParser.UnaryContext node ) {
		BoxExpr				expr	= toAst( file, node.expression() );
		BoxUnaryOperator	op		= node.MINUS() != null ? BoxUnaryOperator.Plus : BoxUnaryOperator.Minus;
		return new BoxUnaryOperation( expr, op, getPosition( node ), getSourceText( node ) );
	}

	/**
	 * Converts the MethodInvokation parser rule to the corresponding AST node.
	 *
	 * @param file source file, if any
	 * @param node ANTLR ArrayAccessContext rule
	 *
	 * @return corresponding AST BoxMethodInvocation
	 *
	 * @see BoxMethodInvocation
	 */
	private BoxExpr toAst( File file, CFParser.MethodInvokationContext node ) {

		List<BoxArgument>	args	= new ArrayList<>();
		String				name	= node.functionInvokation().identifier().getText();

		if ( node.accessExpression() != null ) {
			BoxExpr obj = toAst( file, node.accessExpression() );
			if ( node.functionInvokation().argumentList() != null ) {
				for ( CFParser.ArgumentContext arg : node.functionInvokation().argumentList().argument() ) {
					args.add( toAst( file, arg ) );
					// args.add( toAst(file,arg));
				}
			}
			return new BoxMethodInvocation( name, obj, args, getPosition( node ), getSourceText( node ) );
		} else if ( node.objectExpression() != null ) {
			BoxExpr obj = toAst( file, node.objectExpression() );
			if ( node.functionInvokation() != null ) {
				if ( node.functionInvokation().argumentList() != null ) {
					for ( CFParser.ArgumentContext arg : node.functionInvokation().argumentList().argument() ) {
						args.add( toAst( file, arg ) );
					}
				}
				return new BoxMethodInvocation( name, obj, args, getPosition( node ), getSourceText( node ) );
			}
		}

		throw new IllegalStateException( "not implemented: " + node.getClass().getSimpleName() );
	}

	/**
	 * Converts the ObjectExpression parser rule to the corresponding AST node. * @param file
	 *
	 * @param node ANTLR ObjectExpressionContext rule
	 *
	 * @return corresponding AST BoxAccess or an BoxIdentifier
	 *
	 * @see BoxAccess subclasses
	 * @see BoxIdentifier subclasses
	 */
	private BoxExpr toAst( File file, CFParser.ObjectExpressionContext node ) {
		if ( node.arrayAccess() != null )
			return toAst( file, node.arrayAccess() );
		else if ( node.functionInvokation() != null )
			return toAst( file, node.functionInvokation() );
		else if ( node.identifier() != null )
			return toAst( file, node.identifier() );
		// TODO: add other cases

		throw new IllegalStateException( "not implemented: " + node.getClass().getSimpleName() );
	}

	/**
	 * Converts the Function Invocation parser rule to the corresponding AST node. * @param file
	 *
	 * @param node ANTLR FunctionInvokationContext rule
	 *
	 * @return corresponding AST BoxFunctionInvocation
	 *
	 * @see BoxFunctionInvocation subclasses
	 * @see BoxArgument subclasses
	 */
	private BoxExpr toAst( File file, CFParser.FunctionInvokationContext node ) {
		List<BoxArgument> args = new ArrayList<>();
		if ( node.argumentList() != null ) {
			for ( CFParser.ArgumentContext arg : node.argumentList().argument() ) {
				args.add( toAst( file, arg ) );
			}
		}
		return new BoxFunctionInvocation( node.identifier().getText(),
		    args,
		    getPosition( node ), getSourceText( node ) );
	}

	/**
	 * Converts the Argument parser rule to the corresponding AST node.
	 *
	 * @param node ANTLR FunctionInvokationContext rule
	 *
	 * @return corresponding AST BoxArgument
	 *
	 * @see BoxArgument
	 */
	private BoxArgument toAst( File file, CFParser.ArgumentContext node ) {

		if ( node.EQUAL() != null || node.COLON() != null ) {
			BoxExpr	value	= toAst( file, node.expression().get( 1 ) );
			BoxExpr	name	= toAst( file, node.expression().get( 0 ) );
			return new BoxArgument( name, value, getPosition( node ), getSourceText( node ) );
		} else {
			BoxExpr value = toAst( file, node.expression().get( 0 ) );
			return new BoxArgument( value, getPosition( node ), getSourceText( node ) );
		}
	}

	private BoxStatement toAst( File file, CFParser.FunctionContext node ) {
		// TODO
		throw new IllegalStateException( "not implemented: " + node.getClass().getSimpleName() );
	}

	private BoxStatement toAst( File file, CFParser.ConstructorContext node ) {
		// TODO
		throw new IllegalStateException( "not implemented: " + node.getClass().getSimpleName() );
	}

}
