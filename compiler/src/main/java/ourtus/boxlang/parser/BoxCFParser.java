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
package ourtus.boxlang.parser;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.misc.Interval;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BOMInputStream;
import ortus.boxlang.parser.CFLexer;
import ortus.boxlang.parser.CFParser;
import ourtus.boxlang.ast.*;
import ourtus.boxlang.ast.expression.*;
import ourtus.boxlang.ast.BoxStatement;
import ourtus.boxlang.ast.statement.*;

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
	 * @param file source file to parse
	 * @return a ParsingResult containing the AST with a BoxScript as root and the list of errors (if any)
	 * @throws IOException
	 * @see BoxScript
	 * @see ParsingResult
	 */
	public ParsingResult parse( File file ) throws IOException {
		BOMInputStream inputStream = getInputStream( file );
		CFParser.ScriptContext parseTree = ( CFParser.ScriptContext ) parserFirstStage( inputStream );
		BoxScript ast = parseTreeToAst( file, parseTree );
		return new ParsingResult( ast, issues );
	}

	/**
	 * Parse a cf script string
	 * @param code source code to parse
	 * @return a ParsingResult containing the AST with a BoxScript as root and the list of errors (if any)
	 * @throws IOException
	 * @see BoxScript
	 * @see ParsingResult
	 */
	public ParsingResult parse( String code ) throws IOException {
		InputStream inputStream = IOUtils.toInputStream(code);

		CFParser.ScriptContext parseTree = ( CFParser.ScriptContext ) parserFirstStage( inputStream );
		BoxScript ast = parseTreeToAst( file, parseTree );
		return new ParsingResult( ast, issues );
	}

	/**
	 * Parse a cf script string expression
	 * @param code source of the expression to parse
	 * @return a ParsingResult containing the AST with a BoxExpr as root and the list of errors (if any)
	 * @throws IOException
	 * @see ParsingResult
	 * @see BoxExpr
	 */
	public ParsingResult parseExpression( String code ) throws IOException {
		InputStream inputStream = IOUtils.toInputStream(code);

		CFLexer lexer = new CFLexer( CharStreams.fromStream( inputStream ) );
		CFParser parser = new CFParser( new CommonTokenStream( lexer ) );
		addErrorListeners( lexer, parser );
		CFParser.ExpressionContext parseTree = parser.expression();
		BoxExpr ast = toAst(null,parseTree);
		return new ParsingResult(ast,issues);
	}

	/**
	 * Parse a cf script string statement
	 * @param code source of the expression to parse
	 * @return a ParsingResult containing the AST with a BoxStatement as root and the list of errors (if any)
	 * @throws IOException
	 * @see ParsingResult
	 * @see BoxStatement
	 */
	public ParsingResult parseStatement(String code) throws IOException {
		InputStream inputStream = IOUtils.toInputStream(code);

		CFLexer lexer = new CFLexer( CharStreams.fromStream( inputStream ) );
		CFParser parser = new CFParser( new CommonTokenStream( lexer ) );
		addErrorListeners( lexer, parser );
		CFParser.StatementContext parseTree = parser.statement();
		BoxStatement ast = toAst(null,parseTree);
		return new ParsingResult(ast,issues);
	}

	/**
	 * Fist stage parser
	 * @param stream input stream (file or string) of the source code
	 * @return the ANTLR ParserRule representing the parse tree of the code
	 * @throws IOException io error
	 */
	@Override
	protected ParserRuleContext parserFirstStage(InputStream stream) throws IOException {
		CFLexer lexer = new CFLexer( CharStreams.fromStream( stream ) );
		CFParser parser = new CFParser( new CommonTokenStream( lexer ) );
		addErrorListeners( lexer, parser );

		return parser.script();
	}

	/**
	 * Second stage parser, performs the transformation from ANTLR parse tree
	 * to the AST
	 * @param file source file, if any
	 * @param rule ANTLR parser rule to transform
	 * @return a BoxScript Node
	 * @see BoxScript
	 */
	@Override
	protected BoxScript parseTreeToAst( File file, ParserRuleContext rule )  {
		CFParser.ScriptContext parseTree = ( CFParser.ScriptContext ) rule;
		List<BoxStatement> statements = new ArrayList<>();
		parseTree.functionOrStatement().forEach( stmt -> {
			statements.add(toAst( file, stmt));
		});
		return new BoxScript( statements, getPosition( rule ), getSourceText( rule ));
	}
	private BoxStatement toAst(File file, CFParser.FunctionOrStatementContext node) {
		if ( node.constructor() != null ) {
			return toAst( file, node.constructor() );
		} else if ( node.function() != null ) {
			return toAst( file, node.function() );
		} else if ( node.statement() != null ) {
			return toAst( file, node.statement());
		} else {
			throw new IllegalStateException( "not implemented: " + node.getClass().getSimpleName() );
		}
	}

	private BoxStatement toAst( File file, CFParser.StatementContext node ) {
		if ( node.simpleStatement() != null ) {
			return toAst( file, node.simpleStatement() );
		} else if ( node.if_() != null ) {
			return toAst(file,node.if_());
		} else if ( node.while_() != null ) {
			return toAst(file,node.while_());
		} else if ( node.break_() != null ) {
			return toAst(file,node.break_());
		} else if ( node.continue_() != null ) {
			return toAst(file,node.continue_());
		} else if ( node.switch_() != null ) {
			return toAst(file,node.switch_());
		} else if ( node.for_() != null ) {
			return toAst(file,node.for_());
		} else {
			throw new IllegalStateException( "not implemented: " + node.getClass().getSimpleName() );
		}
	}

	private BoxStatement toAst(File file, CFParser.ForContext node) {
		BoxExpr variable = toAst(file,node.identifier());
		BoxExpr collection = toAst(file,node.expression());
		List<BoxStatement> body = toAst(file,node.statementBlock());

		return new BoxForIn(variable,collection,body,getPosition(node),getSourceText(node));
	}

	private BoxStatement toAst(File file, CFParser.SwitchContext node) {
		BoxExpr condition = toAst(file,node.expression());
		List<BoxSwitchCase> cases = new ArrayList<>();
		for(CFParser.CaseContext c : node.case_()) {
			cases.add(toAst(file,c,condition));
		}
		return new BoxSwitch(condition,cases,getPosition(node),getSourceText(node));
	}

	private BoxSwitchCase toAst(File file, CFParser.CaseContext node,BoxExpr condition) {
		BoxExpr expr = null;
		if( node.expression() != null) {
			BoxExpr temp =  toAst(file,node.expression());
            if (!temp.isLiteral()) {
				expr = temp;
            } else {
                expr = new BoxComparisonOperation(condition,BoxComparisonOperator.Equal,temp,getPosition(node.expression()),getSourceText(node.expression()));
            }

        }

		List<BoxStatement> statements = new ArrayList<>();
		if(node.statement() != null) {
			statements.add(toAst(file,node.statement()));
		}
		if(node.statementBlock() != null) {
			statements.addAll(toAst(file,node.statementBlock()));
		}

		return new BoxSwitchCase(expr,statements,getPosition(node),getSourceText(node));
	}

	private BoxStatement toAst(File file, CFParser.ContinueContext node) {
		return new BoxContinue(getPosition(node),getSourceText(node));
	}

	private BoxStatement toAst(File file, CFParser.BreakContext node)  {
		return new BoxBreak(getPosition(node),getSourceText(node));
	}

	private BoxStatement toAst(File file, CFParser.WhileContext node) {
		BoxExpr condition = toAst(file,node.expression());
		List<BoxStatement> body = new ArrayList<>();

		if(node.statementBlock() != null) {
			body.addAll(toAst(file,node.statementBlock()));
		}
		return  new BoxWhile(condition, body,getPosition(node),getSourceText(node));
	}


	private BoxIfElse toAst(File file, CFParser.IfContext node) {
		BoxExpr condition = toAst(file,node.expression());
		List<BoxStatement> thenBody = new ArrayList<>();
		List<BoxStatement> elseBody = new ArrayList<>();

		if(node.ifStmt != null) {
			thenBody.add(toAst(file,node.ifStmt));
		}
		if(node.ifStmtBlock != null) {
			thenBody.addAll(toAst(file,node.ifStmtBlock));
		}
		if(node.elseStmt != null) {
			elseBody.add(toAst(file,node.elseStmt));
		}
		if(node.elseStmtBlock != null) {
			elseBody.addAll(toAst(file,node.elseStmtBlock));
		}
		return  new BoxIfElse(condition, thenBody, elseBody,getPosition(node),getSourceText(node));
	}

	private List<BoxStatement> toAst(File file, CFParser.StatementBlockContext block) {
		return block.statement().stream().map( stmt -> toAst(file,stmt) ).toList();
	}

	private BoxStatement toAst( File file, CFParser.SimpleStatementContext node ) {
		if ( node.assignment() != null ) {
			return toAst( file, node.assignment());
		} else if(node.methodInvokation() != null) {
			BoxExpr expr = toAst(file,node.methodInvokation());
			return new BoxExpression(expr,getPosition(node),getSourceText(node));
		} else if(node.localDeclaration() != null) {
			List<BoxExpr> identifiers = node.localDeclaration().identifier().stream().map(it -> toAst(file,it) ).toList();
			BoxLocalDeclaration stmt = new BoxLocalDeclaration(identifiers,getPosition(node),getSourceText(node));
			if(node.localDeclaration().expression() != null) {
				BoxExpr expr = toAst(file,node.localDeclaration().expression());
				stmt.setExpression(expr);
			}
			return stmt;
		}
		throw new IllegalStateException( "not implemented: " + node.getClass().getSimpleName() );

	}

	private BoxStatement toAst( File file, CFParser.AssignmentContext node ) {

		BoxExpr left = toAst( file, node.assignmentLeft() );
		BoxExpr right = toAst( file, node.assignmentRight() );

		BoxAssignment stmt = new BoxAssignment( left, right, getPosition( node ), getSourceText( node ) );
		return stmt;
	}

	private BoxExpr toAst( File file, CFParser.AssignmentLeftContext left) {
		// TODO: case with assignmentLeft
		return toAst( file, left.accessExpression() );
	}

	private BoxExpr toAst( File file, CFParser.AccessExpressionContext expression ) {
		if ( expression.identifier() != null )
			return toAst( file, expression.identifier()  );
		if ( expression.arrayAccess() != null )
			return toAst( file, expression.arrayAccess() );
		if ( expression.objectExpression() != null ) {
			BoxExpr context = toAst( file, expression.objectExpression());
			BoxExpr target = toAst( file, expression.accessExpression() );
			return new  BoxObjectAccess(context,target,getPosition(expression),getSourceText(expression));
		}

		throw new IllegalStateException( "not implemented: " + expression.getClass().getSimpleName() );
	}

	private BoxExpr toAst( File file, CFParser.ArrayAccessContext node ) {
		BoxExpr index = toAst( file, node.arrayAccessIndex().expression() );
		BoxExpr context = toAst( file, node.identifier());
		return new BoxArrayAccess( context, index, getPosition( node ), getSourceText( node ) );
	}

	private BoxExpr toAst( File file, CFParser.IdentifierContext identifier ) {
		CFParser.ReservedKeywordContext keyword = identifier.reservedKeyword();
		if(keyword != null && keyword.scope() != null) {
			return toAst(file,keyword.scope());
		}
		return new BoxIdentifier( identifier.getText(), getPosition( identifier ), getSourceText( identifier ) );
	}

	private BoxExpr toAst(File file, CFParser.ScopeContext scope) {
		if(scope.VARIABLES() != null) {
			return new BoxScope( scope.VARIABLES().getText(), getPosition( scope ), getSourceText( scope ) );
		}
		throw new IllegalStateException( "not implemented: " + scope.getClass().getSimpleName() );
	}

	private BoxExpr toAst( File file, CFParser.AssignmentRightContext right) {
		return toAst( file, right.expression() );
	}

	private BoxExpr toAst( File file, CFParser.ExpressionContext expression ) {
		if ( expression.literalExpression() != null ) {
			if ( expression.literalExpression().stringLiteral() != null ) {
				CFParser.StringLiteralContext node = expression.literalExpression().stringLiteral();
				return  new BoxStringLiteral(
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
			BoxExpr left = toAst(file,expression.expression(0));
			BoxExpr right = toAst(file,expression.expression(1));
			return new BoxBinaryOperation(left,BoxBinaryOperator.And,right,getPosition(expression),getSourceText(expression));
		} else if ( expression.OR() != null ) {
			BoxExpr left = toAst(file,expression.expression(0));
			BoxExpr right = toAst(file,expression.expression(1));
			return new BoxBinaryOperation(left,BoxBinaryOperator.Or,right,getPosition(expression),getSourceText(expression));
		} else if ( expression.PLUS() != null ) {
			BoxExpr left = toAst(file,expression.expression(0));
			BoxExpr right = toAst(file,expression.expression(1));
			return new BoxBinaryOperation(left,BoxBinaryOperator.Plus,right,getPosition(expression),getSourceText(expression));
		} else if ( expression.MINUS() != null ) {
			BoxExpr left = toAst(file,expression.expression(0));
			BoxExpr right = toAst(file,expression.expression(1));
			return new BoxBinaryOperation(left,BoxBinaryOperator.Minus,right,getPosition(expression),getSourceText(expression));
		}  else if ( expression.STAR() != null ) {
			BoxExpr left = toAst(file,expression.expression(0));
			BoxExpr right = toAst(file,expression.expression(1));
			return new BoxBinaryOperation(left,BoxBinaryOperator.Star,right,getPosition(expression),getSourceText(expression));
		}  else if ( expression.SLASH() != null ) {
			BoxExpr left = toAst(file,expression.expression(0));
			BoxExpr right = toAst(file,expression.expression(1));
			return new BoxBinaryOperation(left,BoxBinaryOperator.Slash,right,getPosition(expression),getSourceText(expression));
		}  else if ( expression.BACKSLASH() != null ) {
			BoxExpr left = toAst(file,expression.expression(0));
			BoxExpr right = toAst(file,expression.expression(1));
			return new BoxBinaryOperation(left,BoxBinaryOperator.Backslash,right,getPosition(expression),getSourceText(expression));
		} else if ( expression.POWER() != null ) {
			BoxExpr left = toAst(file,expression.expression(0));
			BoxExpr right = toAst(file,expression.expression(1));
			return new BoxBinaryOperation(left,BoxBinaryOperator.Power,right,getPosition(expression),getSourceText(expression));
		} else if ( expression.XOR() != null ) {
			BoxExpr left = toAst(file,expression.expression(0));
			BoxExpr right = toAst(file,expression.expression(1));
			return new BoxBinaryOperation(left,BoxBinaryOperator.Xor,right,getPosition(expression),getSourceText(expression));
		} else if ( expression.MOD() != null ) {
			BoxExpr left = toAst(file,expression.expression(0));
			BoxExpr right = toAst(file,expression.expression(1));
			return new BoxBinaryOperation(left,BoxBinaryOperator.Mod,right,getPosition(expression),getSourceText(expression));
		} else if ( expression.INSTANCEOF() != null ) {
			BoxExpr left = toAst(file,expression.expression(0));
			BoxExpr right = toAst(file,expression.expression(1));
			return new BoxBinaryOperation(left,BoxBinaryOperator.InstanceOf,right,getPosition(expression),getSourceText(expression));
		} else if ( expression.EQ() != null || expression.IS() != null) {
			BoxExpr left = toAst(file,expression.expression(0));
			BoxExpr right = toAst(file,expression.expression(1));
			return  new BoxComparisonOperation(left, BoxComparisonOperator.Equal,right,getPosition(expression),getSourceText(expression));
		} else if ( expression.TEQ() != null) {
			BoxExpr left = toAst(file,expression.expression(0));
			BoxExpr right = toAst(file,expression.expression(1));
			return new BoxComparisonOperation(left, BoxComparisonOperator.TEqual,right,getPosition(expression),getSourceText(expression));
		} else if ( expression.NEQ() != null) {
			BoxExpr left = toAst(file,expression.expression(0));
			BoxExpr right = toAst(file,expression.expression(1));
			return new BoxComparisonOperation(left, BoxComparisonOperator.NotEqual,right,getPosition(expression),getSourceText(expression));
		} else if ( expression.GT() != null) {
			BoxExpr left = toAst(file,expression.expression(0));
			BoxExpr right = toAst(file,expression.expression(1));
			return new BoxComparisonOperation(left, BoxComparisonOperator.GreaterThan,right,getPosition(expression),getSourceText(expression));
		} else if ( expression.GTE() != null) {
			BoxExpr left = toAst(file,expression.expression(0));
			BoxExpr right = toAst(file,expression.expression(1));
			return new BoxComparisonOperation(left, BoxComparisonOperator.GreaterThanEquals,right,getPosition(expression),getSourceText(expression));
		} else if ( expression.LT() != null) {
			BoxExpr left = toAst(file,expression.expression(0));
			BoxExpr right = toAst(file,expression.expression(1));
			return new BoxComparisonOperation(left, BoxComparisonOperator.LessThan,right,getPosition(expression),getSourceText(expression));
		} else if ( expression.LTE() != null) {
			BoxExpr left = toAst(file,expression.expression(0));
			BoxExpr right = toAst(file,expression.expression(1));
			return new BoxComparisonOperation(left, BoxComparisonOperator.LesslThanEqual,right,getPosition(expression),getSourceText(expression));
		} else if ( expression.AMPERSAND() != null ) {
			BoxExpr left = toAst(file,expression.expression(0));
			BoxExpr right = toAst(file,expression.expression(1));
			return new BoxBinaryOperation(left,BoxBinaryOperator.Concat,right,getPosition(expression),getSourceText(expression));
		} else if ( expression.ELVIS() != null ) {
			BoxExpr left = toAst(file,expression.expression(0));
			BoxExpr right = toAst(file,expression.expression(1));
			return new BoxBinaryOperation(left,BoxBinaryOperator.Elvis,right,getPosition(expression),getSourceText(expression));
		} else if ( expression.QM() != null ) {
			BoxExpr condition = toAst(file,expression.expression(0));
			BoxExpr whenTrue = toAst(file,expression.expression(1));
			BoxExpr whenFalse = toAst(file,expression.expression(2));
			return new BoxTernaryOperation(condition,whenTrue,whenFalse,getPosition(expression),getSourceText(expression));
		} else if ( expression.not() != null && expression.CONTAIN() == null ) {
			BoxExpr expr = toAst(file,expression.not().expression());
			return new BoxNegateOperation(expr,BoxNegateOperator.Not,getPosition(expression),getSourceText(expression));
		} else if ( expression.CONTAINS() != null ) {
			BoxExpr left = toAst(file,expression.expression(0));
			BoxExpr right = toAst(file,expression.expression(1));
			return new BoxBinaryOperation(left,BoxBinaryOperator.Contains,right,getPosition(expression),getSourceText(expression));
		} else if( expression.CONTAIN() != null && expression.DOES() != null && expression.NOT() != null) {
			BoxExpr left = toAst(file,expression.expression(0));
			BoxExpr right = toAst(file,expression.expression(1));
			return new BoxBinaryOperation(left,BoxBinaryOperator.NotContains,right,getPosition(expression),getSourceText(expression));
		} else if ( expression.LPAREN() != null ) {
			BoxExpr expr  = toAst(file,expression.expression(0));
			return new BoxParenthesis(expr,getPosition(expression),getSourceText(expression));
		}
		// TODO: add other cases
		throw new IllegalStateException( "not implemented: " + expression.getClass().getSimpleName() );
	}

	private BoxExpr toAst(File file, CFParser.UnaryContext expression) {
		BoxExpr expr = toAst(file,expression.expression());
		BoxBinaryOperator op = expression.MINUS() != null ? BoxBinaryOperator.Plus : BoxBinaryOperator.Minus ;
		return new BoxUnaryOperation(expr,op,getPosition(expression),getSourceText(expression));
	}

	private BoxExpr toAst(File file, CFParser.MethodInvokationContext expression) {

		List<BoxArgument> args = new ArrayList<>();
		String name = expression.functionInvokation().identifier().getText();

		if(expression.accessExpression() != null) {
			BoxExpr obj = toAst(file, expression.accessExpression());
			if(expression.functionInvokation().argumentList() != null) {
				for(CFParser.ArgumentContext arg : expression.functionInvokation().argumentList().argument()) {
					args.add( toAst(file,arg));
					//args.add( toAst(file,arg));
				}
			}
			return new BoxMethodInvocation(name, obj, args, getPosition(expression), getSourceText(expression));
		} else if(expression.objectExpression() != null) {
			BoxExpr obj = toAst(file,expression.objectExpression());
			if(expression.functionInvokation() != null) {
				if(expression.functionInvokation().argumentList() != null) {
					for(CFParser.ArgumentContext arg : expression.functionInvokation().argumentList().argument()) {
						args.add( toAst(file,arg));
					}
				}
				return new BoxMethodInvocation(name, obj, args, getPosition(expression), getSourceText(expression));
			}
		}

		throw new IllegalStateException( "not implemented: " + expression.getClass().getSimpleName() );
	}

	private BoxExpr toAst( File file, CFParser.ObjectExpressionContext expression ) {
		if ( expression.arrayAccess() != null )
			return toAst( file, expression.arrayAccess() );
		else if ( expression.functionInvokation() != null )
			return toAst( file, expression.functionInvokation() );
		else if ( expression.identifier() != null )
			return toAst( file, expression.identifier()  );
		// TODO: add other cases

		throw new IllegalStateException( "not implemented: " + expression.getClass().getSimpleName() );
	}

	private BoxExpr toAst( File file, CFParser.FunctionInvokationContext invocation ) {
		List<BoxArgument> args = new ArrayList<>();
		if(invocation.argumentList() != null) {
			for(CFParser.ArgumentContext arg : invocation.argumentList().argument()) {
				args.add( toAst(file,arg));
			}
		}
		return  new BoxFunctionInvocation( invocation.identifier().getText(),
			args,
			getPosition( invocation ), getSourceText( invocation ) );
	}

	private BoxArgument toAst( File file, CFParser.ArgumentContext argument ) {

		if(argument.EQUAL() != null || argument.COLON() != null) {
			BoxExpr value = toAst( file, argument.expression().get( 1 ) );
			BoxExpr name = toAst( file, argument.expression().get( 0 ) );
			return new BoxArgument(name,value,getPosition(argument),getSourceText(argument));
		} else {
			BoxExpr value = toAst( file, argument.expression().get( 0 ) );
			return new BoxArgument(value,getPosition(argument),getSourceText(argument));
		}
	}

	private BoxStatement toAst( File file, CFParser.FunctionContext function ) {
		return null; // TODO
	}

	private BoxStatement toAst( File file, CFParser.ConstructorContext constructor ) {
		return null; // TODO
	}

	private Position getPosition( ParserRuleContext context ) {
		return new Position( new Point( context.start.getLine(), context.start.getCharPositionInLine() ),
			new Point( context.stop.getLine(), context.stop.getCharPositionInLine() ), new SourceFile( file ) );
	}

	private String getSourceText( ParserRuleContext rule ) {
		CharStream s = rule.getStart().getTokenSource().getInputStream();
		return s.getText(new Interval(rule.getStart().getStartIndex(),rule.getStop().getStopIndex()));
	}


}
