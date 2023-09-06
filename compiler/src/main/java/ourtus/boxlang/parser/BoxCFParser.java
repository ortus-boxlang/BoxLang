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

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BOMInputStream;
import ortus.boxlang.parser.CFLexer;
import ortus.boxlang.parser.CFParser;
import ourtus.boxlang.ast.*;
import ourtus.boxlang.ast.expression.*;
import ourtus.boxlang.ast.BoxStatement;
import ourtus.boxlang.ast.statement.BoxAssignment;
import ourtus.boxlang.ast.statement.BoxExpression;
import ourtus.boxlang.ast.statement.BoxIfElse;
import ourtus.boxlang.ast.statement.BoxLocalDeclaration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class BoxCFParser extends BoxAbstractParser {

	public BoxCFParser() {
		super();
	}


	@Override
	protected ParserRuleContext parserFirstStage(InputStream stream) throws IOException {
		CFLexer lexer = new CFLexer( CharStreams.fromStream( stream ) );
		CFParser parser = new CFParser( new CommonTokenStream( lexer ) );
		addErrorListeners( lexer, parser );

		return parser.script();
	}

	@Override
	protected BoxScript parseTreeToAst( File file, ParserRuleContext rule ) throws IOException {
		CFParser.ScriptContext parseTree = ( CFParser.ScriptContext ) rule;
		BoxScript script = new BoxScript( getPosition( rule ), getSourceText( rule ));
		parseTree.functionOrStatement().stream().map(
			it -> {
                return toAst( file, it , script );
			}
		).forEach( stmt -> {
			script.getStatements().add(stmt);
		});
		return script;
	}

	public ParsingResult parse( File file ) throws IOException {
		BOMInputStream inputStream = getInputStream( file );
		CFParser.ScriptContext parseTree = ( CFParser.ScriptContext ) parserFirstStage( inputStream );
		BoxScript ast = parseTreeToAst( file, parseTree );
		return new ParsingResult( ast, issues );
	}
	public ParsingResult parse( String code ) throws IOException {
		InputStream inputStream = IOUtils.toInputStream(code);

		CFParser.ScriptContext parseTree = ( CFParser.ScriptContext ) parserFirstStage( inputStream );
		BoxScript ast = parseTreeToAst( file, parseTree );
		return new ParsingResult( ast, issues );
	}

	public ParsingResult parseExpression( String code ) throws IOException {
		InputStream inputStream = IOUtils.toInputStream(code);

		CFLexer lexer = new CFLexer( CharStreams.fromStream( inputStream ) );
		CFParser parser = new CFParser( new CommonTokenStream( lexer ) );
		addErrorListeners( lexer, parser );
		CFParser.ExpressionContext parseTree = parser.expression();
		BoxExpr ast = toAst(null,parseTree,null);
		return new ParsingResult(ast,issues);
	}

	public ParsingResult parseStatement(String code) throws IOException {
		InputStream inputStream = IOUtils.toInputStream(code);

		CFLexer lexer = new CFLexer( CharStreams.fromStream( inputStream ) );
		CFParser parser = new CFParser( new CommonTokenStream( lexer ) );
		addErrorListeners( lexer, parser );
		CFParser.StatementContext parseTree = parser.statement();
		BoxStatement ast = toAst(null,parseTree,null);
		return new ParsingResult(ast,issues);
	}

	private BoxStatement toAst(File file, CFParser.FunctionOrStatementContext node, Node parent) {
		if ( node.constructor() != null ) {
			return toAst( file, node.constructor() );
		} else if ( node.function() != null ) {
			return toAst( file, node.function() );
		} else if ( node.statement() != null ) {
			return toAst( file, node.statement() , parent );
		} else {
			throw new IllegalStateException( "not implemented: " + node.getClass().getSimpleName() );
		}
	}

	private BoxStatement toAst( File file, CFParser.StatementContext node, Node parent ) {
		if ( node.simpleStatement() != null ) {
			return toAst( file, node.simpleStatement(),parent );
		} else if ( node.if_() != null ) {
			return toAst(file,node.if_(),parent);
		} else {
			throw new IllegalStateException( "not implemented: " + node.getClass().getSimpleName() );
		}
	}


	private BoxIfElse toAst(File file, CFParser.IfContext node, Node parent) {
		BoxIfElse ifStmt = new BoxIfElse(getPosition(node),getSourceText(node));
		BoxExpr condition = toAst(file,node.expression(),ifStmt);
		ifStmt.setCondition(condition);

		if(node.ifStmt != null) {
			ifStmt.getThenBody().add(toAst(file,node.ifStmt,ifStmt));
		}
		if(node.ifStmtBlock != null) {
			ifStmt.getThenBody().addAll(toAst(file,node.ifStmtBlock,ifStmt));
		}
		if(node.elseStmtBlock != null) {
			ifStmt.getElseBody().addAll(toAst(file,node.elseStmtBlock,ifStmt));
		}
		if(node.elseStmt != null) {
			ifStmt.getElseBody().add(toAst(file,node.elseStmt,ifStmt));
		}

		ifStmt.setParent(parent);
		return  ifStmt;
	}

	private List<BoxStatement> toAst(File file, CFParser.StatementBlockContext block,Node parent) {
		return block.statement().stream().map( stmt -> toAst(file,stmt,parent) ).toList();
	}

	private BoxStatement toAst( File file, CFParser.SimpleStatementContext node, Node parent ) {
		if ( node.assignment() != null ) {
			BoxStatement stmt = toAst( file, node.assignment(), parent);
			stmt.setParent(parent);
			return stmt;
		} else if(node.methodInvokation() != null) {
			BoxExpr expr = toAst(file,node.methodInvokation(),parent);
			BoxStatement stmt = new BoxExpression(expr,getPosition(node),getSourceText(node));

			stmt.setParent(parent);
			expr.setParent(stmt);
			return stmt;
		} else if(node.localDeclaration() != null) {
			List<BoxExpr> identifiers = node.localDeclaration().identifier().stream().map(it -> toAst(file,it,parent) ).toList();
			BoxLocalDeclaration stmt = new BoxLocalDeclaration(identifiers,getPosition(node),getSourceText(node));
			if(node.localDeclaration().expression() != null) {
				BoxExpr expr = toAst(file,node.localDeclaration().expression(),parent);
				stmt.setExpression(expr);
			}
			return stmt;
		}
		throw new IllegalStateException( "not implemented: " + node.getClass().getSimpleName() );

	}

	private BoxStatement toAst( File file, CFParser.AssignmentContext node, Node parent ) {

		BoxExpr left = toAst( file, node.assignmentLeft(), parent );
		BoxExpr right = toAst( file, node.assignmentRight(), parent );

		BoxAssignment stmt = new BoxAssignment( left, right, getPosition( node ), getSourceText( node ) );
		stmt.setParent(parent);
		left.setParent(stmt);
		right.setParent(stmt);
		return stmt;
	}

	private BoxExpr toAst( File file, CFParser.AssignmentLeftContext left, Node parent) {
		// TODO: case with assignmentLeft
		return toAst( file, left.accessExpression(), parent );
	}

	private BoxExpr toAst( File file, CFParser.AccessExpressionContext expression, Node parent ) {
		if ( expression.identifier() != null )
			return toAst( file, expression.identifier() , parent );
		if ( expression.arrayAccess() != null )
			return toAst( file, expression.arrayAccess(), parent );
		if ( expression.objectExpression() != null ) {
			BoxExpr context = toAst( file, expression.objectExpression(), parent);
			BoxExpr target = toAst( file, expression.accessExpression(), parent );
			return new  BoxObjectAccess(context,target,getPosition(expression),getSourceText(expression));
		}


		throw new IllegalStateException( "not implemented: " + expression.getClass().getSimpleName() );
	}

	private BoxExpr toAst( File file, CFParser.ArrayAccessContext node , Node parent) {
		BoxExpr index = toAst( file, node.arrayAccessIndex().expression(),parent );
		BoxExpr context = toAst( file, node.identifier(), parent);
		return new BoxArrayAccess( context, index, parent, getPosition( node ), getSourceText( node ) );
	}

	private BoxExpr toAst( File file, CFParser.IdentifierContext identifier , Node parent) {
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

	private BoxExpr toAst( File file, CFParser.AssignmentRightContext right,Node parent ) {
		return toAst( file, right.expression(), parent );
	}

	private BoxExpr toAst( File file, CFParser.ExpressionContext expression, Node parent ) {
		if ( expression.literalExpression() != null ) {
			if ( expression.literalExpression().stringLiteral() != null ) {
				CFParser.StringLiteralContext node = expression.literalExpression().stringLiteral();
				return new BoxStringLiteral( node.getText(),
					getPosition( node ),
					getSourceText( node ) );
			}
			if ( expression.literalExpression().integerLiteral() != null ) {
				CFParser.IntegerLiteralContext node = expression.literalExpression().integerLiteral();
				return new BoxIntegerLiteral( node.getText(),
					getPosition( node ),
					getSourceText( node ) );
			}
			if ( expression.literalExpression().booleanLiteral() != null ) {
				CFParser.BooleanLiteralContext node = expression.literalExpression().booleanLiteral();
				return new BoxBooleanLiteral( node.getText(),
					getPosition( node ),
					getSourceText( node ) );
			}
			// TODO: add other cases
		} else if ( expression.identifier() != null ) {
			return toAst( file, expression.identifier(),parent );
		} else if ( expression.accessExpression() != null ) {
			return toAst( file, expression.accessExpression(),parent );
		} else if ( expression.objectExpression() != null ) {
			return toAst( file, expression.objectExpression(), parent );
		} else if ( expression.methodInvokation() != null ) {
			return toAst( file, expression.methodInvokation(), parent );
		} else if ( expression.AND() != null ) {
			BoxExpr left = toAst(file,expression.expression(0),parent);
			BoxExpr right = toAst(file,expression.expression(1),parent);
			return new BoxBinaryOperation(left,BoxBinaryOperator.And,right,getPosition(expression),getSourceText(expression));
		} else if ( expression.OR() != null ) {
			BoxExpr left = toAst(file,expression.expression(0),parent);
			BoxExpr right = toAst(file,expression.expression(1),parent);
			return new BoxBinaryOperation(left,BoxBinaryOperator.Or,right,getPosition(expression),getSourceText(expression));
		} else if ( expression.PLUS() != null ) {
			BoxExpr left = toAst(file,expression.expression(0), parent);
			BoxExpr right = toAst(file,expression.expression(1),parent);
			return new BoxBinaryOperation(left,BoxBinaryOperator.Plus,right,getPosition(expression),getSourceText(expression));
		} else if ( expression.MINUS() != null ) {
			BoxExpr left = toAst(file,expression.expression(0),parent);
			BoxExpr right = toAst(file,expression.expression(1),parent);
			return new BoxBinaryOperation(left,BoxBinaryOperator.Minus,right,getPosition(expression),getSourceText(expression));
		}  else if ( expression.STAR() != null ) {
			BoxExpr left = toAst(file,expression.expression(0),parent);
			BoxExpr right = toAst(file,expression.expression(1),parent);
			return new BoxBinaryOperation(left,BoxBinaryOperator.Star,right,getPosition(expression),getSourceText(expression));
		}  else if ( expression.SLASH() != null ) {
			BoxExpr left = toAst(file,expression.expression(0),parent);
			BoxExpr right = toAst(file,expression.expression(1),parent);
			return new BoxBinaryOperation(left,BoxBinaryOperator.Slash,right,getPosition(expression),getSourceText(expression));
		} else if ( expression.XOR() != null ) {
			BoxExpr left = toAst(file,expression.expression(0),parent);
			BoxExpr right = toAst(file,expression.expression(1),parent);
			return new BoxBinaryOperation(left,BoxBinaryOperator.Xor,right,getPosition(expression),getSourceText(expression));
		} else if ( expression.MOD() != null ) {
			BoxExpr left = toAst(file,expression.expression(0),parent);
			BoxExpr right = toAst(file,expression.expression(1),parent);
			return new BoxBinaryOperation(left,BoxBinaryOperator.Mod,right,getPosition(expression),getSourceText(expression));
		} else if ( expression.INSTANCEOF() != null ) {
			BoxExpr left = toAst(file,expression.expression(0),parent);
			BoxExpr right = toAst(file,expression.expression(1),parent);
			return new BoxBinaryOperation(left,BoxBinaryOperator.InstanceOf,right,getPosition(expression),getSourceText(expression));
		} else if ( expression.EQ() != null || expression.IS() != null) {
			BoxExpr left = toAst(file,expression.expression(0),parent);
			BoxExpr right = toAst(file,expression.expression(1),parent);
			BoxComparisonOperation stmt = new BoxComparisonOperation(left, BoxComparisonOperator.Equal,right,getPosition(expression),getSourceText(expression));
			stmt.setParent(parent);
			return stmt;
		} else if ( expression.TEQ() != null) {
			BoxExpr left = toAst(file,expression.expression(0),parent);
			BoxExpr right = toAst(file,expression.expression(1),parent);
			BoxComparisonOperation stmt = new BoxComparisonOperation(left, BoxComparisonOperator.TEqual,right,getPosition(expression),getSourceText(expression));
			stmt.setParent(parent);
			return stmt;

		} else if ( expression.NEQ() != null) {
			BoxExpr left = toAst(file,expression.expression(0),parent);
			BoxExpr right = toAst(file,expression.expression(1),parent);
			BoxComparisonOperation stmt = new BoxComparisonOperation(left, BoxComparisonOperator.NotEqual,right,getPosition(expression),getSourceText(expression));
			stmt.setParent(parent);
			return stmt;
		} else if ( expression.GT() != null) {
			BoxExpr left = toAst(file,expression.expression(0),parent);
			BoxExpr right = toAst(file,expression.expression(1),parent);
			BoxComparisonOperation stmt = new BoxComparisonOperation(left, BoxComparisonOperator.GreaterThan,right,getPosition(expression),getSourceText(expression));
			stmt.setParent(parent);
			return stmt;
		} else if ( expression.GTE() != null) {
			BoxExpr left = toAst(file,expression.expression(0),parent);
			BoxExpr right = toAst(file,expression.expression(1),parent);
			BoxComparisonOperation stmt = new BoxComparisonOperation(left, BoxComparisonOperator.GreaterThanEquals,right,getPosition(expression),getSourceText(expression));
			stmt.setParent(parent);
			return stmt;
		} else if ( expression.LT() != null) {
			BoxExpr left = toAst(file,expression.expression(0),parent);
			BoxExpr right = toAst(file,expression.expression(1),parent);
			BoxComparisonOperation stmt = new BoxComparisonOperation(left, BoxComparisonOperator.LessThan,right,getPosition(expression),getSourceText(expression));
			stmt.setParent(parent);
			return stmt;
		} else if ( expression.LTE() != null) {
			BoxExpr left = toAst(file,expression.expression(0),parent);
			BoxExpr right = toAst(file,expression.expression(1),parent);
			BoxComparisonOperation stmt = new BoxComparisonOperation(left, BoxComparisonOperator.LesslThanEqual,right,getPosition(expression),getSourceText(expression));
			stmt.setParent(parent);
			return stmt;
		} else if ( expression.AMPERSAND() != null ) {
			BoxExpr left = toAst(file,expression.expression(0),parent);
			BoxExpr right = toAst(file,expression.expression(1),parent);
			return new BoxBinaryOperation(left,BoxBinaryOperator.Concat,right,getPosition(expression),getSourceText(expression));
		} else if ( expression.ELVIS() != null ) {
			BoxExpr left = toAst(file,expression.expression(0),parent);
			BoxExpr right = toAst(file,expression.expression(1),parent);
			return new BoxBinaryOperation(left,BoxBinaryOperator.Elvis,right,getPosition(expression),getSourceText(expression));
		} else if ( expression.QM() != null ) {
			BoxExpr condition = toAst(file,expression.expression(0),parent);
			BoxExpr whenTrue = toAst(file,expression.expression(1),parent);
			BoxExpr whenFalse = toAst(file,expression.expression(2),parent);
			return new BoxTernaryOperation(condition,whenTrue,whenFalse,getPosition(expression),getSourceText(expression));
		} else if ( expression.not() != null && expression.CONTAIN() == null ) {
			BoxExpr expr = toAst(file,expression.not().expression(),parent);
			return new BoxNegateOperation(expr,BoxNegateOperator.Not,getPosition(expression),getSourceText(expression));
		} else if ( expression.CONTAINS() != null ) {
			BoxExpr left = toAst(file,expression.expression(0),parent);
			BoxExpr right = toAst(file,expression.expression(1),parent);
			return new BoxBinaryOperation(left,BoxBinaryOperator.Contains,right,getPosition(expression),getSourceText(expression));
		} else if( expression.CONTAIN() != null && expression.DOES() != null && expression.NOT() != null) {
			BoxExpr left = toAst(file,expression.expression(0),parent);
			BoxExpr right = toAst(file,expression.expression(1),parent);

			return new BoxBinaryOperation(left,BoxBinaryOperator.NotContains,right,getPosition(expression),getSourceText(expression));
		} else if ( expression.LPAREN() != null ) {
			BoxExpr expr  = toAst(file,expression.expression(0),parent);
			return new BoxParenthesis(expr,getPosition(expression),getSourceText(expression));
		}

		throw new IllegalStateException( "not implemented: " + expression.getClass().getSimpleName() );
	}

	private BoxExpr toAst(File file, CFParser.MethodInvokationContext expression, Node parent) {

		String name = expression.functionInvokation().identifier().getText();

		if(expression.accessExpression() != null) {
			BoxExpr obj = toAst(file, expression.accessExpression(),parent);
			BoxMethodInvocation stmt = new BoxMethodInvocation(name, obj, getPosition(expression), getSourceText(expression));
			if(expression.functionInvokation().argumentList() != null) {
				for(CFParser.ArgumentContext arg : expression.functionInvokation().argumentList().argument()) {
					stmt.getArguments().add( toAst(file,arg, stmt));
				}
			}
			return stmt;
		} else if(expression.objectExpression() != null) {
			BoxExpr obj = toAst(file,expression.objectExpression(),parent);
			if(expression.functionInvokation() != null) {
				BoxMethodInvocation stmt = new BoxMethodInvocation(name, obj, getPosition(expression), getSourceText(expression));
				if(expression.functionInvokation().argumentList() != null) {
					for(CFParser.ArgumentContext arg : expression.functionInvokation().argumentList().argument()) {
						stmt.getArguments().add( toAst(file,arg, stmt));
					}
				}
				return stmt;
			}
		}

		throw new IllegalStateException( "not implemented: " + expression.getClass().getSimpleName() );
	}

	private BoxExpr toAst( File file, CFParser.ObjectExpressionContext expression, Node parent ) {
		if ( expression.arrayAccess() != null )
			return toAst( file, expression.arrayAccess() , parent);
		else if ( expression.functionInvokation() != null )
			return toAst( file, expression.functionInvokation(),parent );
		else if ( expression.identifier() != null )
			return toAst( file, expression.identifier() , parent );
		// TODO: add other cases

		throw new IllegalStateException( "not implemented: " + expression.getClass().getSimpleName() );
	}

	private BoxExpr toAst( File file, CFParser.FunctionInvokationContext invocation, Node parent ) {
		BoxFunctionInvocation stmt = new BoxFunctionInvocation( invocation.identifier().getText(),
			getPosition( invocation ), getSourceText( invocation ) );

		if ( invocation.argumentList() != null ) {
			invocation.argumentList().argument().stream().map( arg -> toAst( file, arg ,stmt ) )
				.forEach( arg -> stmt.getArguments().add( arg ) );
		}

		return stmt;
	}

	private BoxExpr toAst( File file, CFParser.ArgumentContext argument, Node parent ) {
		return toAst( file, argument.expression().get( 0 ),parent );
		// TODO: handle default value (when expression().size() == 2
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
		return rule.getText(); // TODO
	}


}
