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
		BoxExpr ast = toAst(null,parseTree);
		return new ParsingResult(ast,issues);
	}

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

	private BoxStatement toAst( File file, CFParser.StatementContext node ) {
		if ( node.simpleStatement() != null ) {
			return toAst( file, node.simpleStatement() );
		} else if ( node.if_() != null ) {
			return toAst(file,node.if_());
		} else {
			throw new IllegalStateException( "not implemented: " + node.getClass().getSimpleName() );
		}
	}

	private BoxIfElse toAst(File file, CFParser.IfContext node) {
		BoxExpr condition = toAst(file,node.expression());
		BoxIfElse ifStmt = new BoxIfElse(condition,getPosition(node),getSourceText(node));

		if(node.ifStmtBlock != null) {
			ifStmt.getBody().addAll(toAst(file,node.ifStmtBlock));
		}

		return  ifStmt;
	}

	private List<BoxStatement> toAst(File file, CFParser.StatementBlockContext block) {
		return block.statement().stream().map( stmt -> toAst(file,stmt) ).toList();
	}

	private BoxStatement toAst( File file, CFParser.SimpleStatementContext node ) {
		if ( node.assignment() != null ) {
			return toAst( file, node.assignment() );
		} else if(node.methodInvokation() != null) {
			BoxExpr expr = toAst(file,node.methodInvokation());
			return new BoxExpression(expr,getPosition(node),getSourceText(node));
		}
		throw new IllegalStateException( "not implemented: " + node.getClass().getSimpleName() );

	}

	private BoxStatement toAst( File file, CFParser.AssignmentContext node ) {
		BoxExpr left = toAst( file, node.assignmentLeft() );
		BoxExpr right = toAst( file, node.assignmentRight() );
		return new BoxAssignment( left, right, getPosition( node ), getSourceText( node ) );
	}

	private BoxExpr toAst( File file, CFParser.AssignmentLeftContext left ) {
		// TODO: case with assignmentLeft
		return toAst( file, left.accessExpression() );
	}

	private BoxExpr toAst( File file, CFParser.AccessExpressionContext expression ) {
		if ( expression.identifier() != null )
			return toAst( file, expression.identifier() );
		if ( expression.arrayAccess() != null )
			return toAst( file, expression.arrayAccess() );
		if ( expression.objectExpression() != null )
			return toAst( file, expression.objectExpression() );

		throw new IllegalStateException( "not implemented: " + expression.getClass().getSimpleName() );
	}

	private BoxExpr toAst( File file, CFParser.ArrayAccessContext node ) {
		BoxExpr index = toAst( file, node.arrayAccessIndex().expression() );
		BoxExpr context = toAst( file, node.identifier() );
		return new BoxArrayAccess( context, index, getPosition( node ), getSourceText( node ) );
	}

	private BoxExpr toAst( File file, CFParser.IdentifierContext identifier ) {
		return new BoxIdentifier( identifier.getText(), getPosition( identifier ), getSourceText( identifier ) );
	}

	private BoxExpr toAst( File file, CFParser.AssignmentRightContext right ) {
		return toAst( file, right.expression() );
	}

	private BoxExpr toAst( File file, CFParser.ExpressionContext expression ) {
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
			return toAst( file, expression.identifier() );
		} else if ( expression.accessExpression() != null ) {
			return toAst( file, expression.accessExpression() );
		} else if ( expression.objectExpression() != null ) {
			return toAst( file, expression.objectExpression() );
		} else if ( expression.methodInvokation() != null ) {
			return toAst( file, expression.methodInvokation() );
		} else if ( expression.PLUS() != null ) {
			BoxExpr left = toAst(file,expression.expression(0));
			BoxExpr right = toAst(file,expression.expression(1));
			return new BoxBinaryOperation(left,BoxBinaryOperator.Plus,right,getPosition(expression),getSourceText(expression));
		} else if ( expression.EQ() != null ) {
			BoxExpr left = toAst(file,expression.expression(0));
			BoxExpr right = toAst(file,expression.expression(1));
			return new BoxComparisonOperation(left, BoxComparisonOperator.Equal,right,getPosition(expression),getSourceText(expression));
		} else if ( expression.AMPERSAND() != null ) {
			BoxExpr left = toAst(file,expression.expression(0));
			BoxExpr right = toAst(file,expression.expression(1));
			return new BoxBinaryOperation(left,BoxBinaryOperator.Concat,right,getPosition(expression),getSourceText(expression));
		} else if ( expression.not() != null ) {
			BoxExpr expr = toAst(file,expression.not().expression());
			return new BoxNegateOperation(expr,BoxNegateOperator.Not,getPosition(expression),getSourceText(expression));
		} else if ( expression.CONTAINS() != null ) {
			BoxExpr left = toAst(file,expression.expression(0));
			BoxExpr right = toAst(file,expression.expression(1));
			return new BoxBinaryOperation(left,BoxBinaryOperator.Contains,right,getPosition(expression),getSourceText(expression));
		}

		throw new IllegalStateException( "not implemented: " + expression.getClass().getSimpleName() );
	}

	private BoxExpr toAst(File file, CFParser.MethodInvokationContext expression) {

		if(expression.accessExpression() != null) {
			BoxExpr obj = toAst(file, expression.accessExpression());
			String name = expression.functionInvokation().identifier().getText();
			return new BoxMethodInvocation(name, obj, getPosition(expression), getSourceText(expression));
		} else if(expression.objectExpression() != null) {
			return toAst(file,expression.objectExpression());
		}

		throw new IllegalStateException( "not implemented: " + expression.getClass().getSimpleName() );
	}

	private BoxExpr toAst( File file, CFParser.ObjectExpressionContext expression ) {
		if ( expression.arrayAccess() != null )
			return toAst( file, expression.arrayAccess() );
		else if ( expression.functionInvokation() != null )
			return toAst( file, expression.functionInvokation() );
		else if ( expression.identifier() != null )
			return toAst( file, expression.identifier() );
		// TODO: add other cases

		throw new IllegalStateException( "not implemented: " + expression.getClass().getSimpleName() );
	}

	private BoxExpr toAst( File file, CFParser.FunctionInvokationContext invocation ) {
		BoxFunctionInvocation functionInvocation = new BoxFunctionInvocation( invocation.identifier().getText(),
			getPosition( invocation ), getSourceText( invocation ) );

		if ( invocation.argumentList() != null ) {
			invocation.argumentList().argument().stream().map( arg -> toAst( file, arg ) )
				.forEach( arg -> functionInvocation.getArguments().add( arg ) );
		}

		return functionInvocation;
	}

	private BoxExpr toAst( File file, CFParser.ArgumentContext argument ) {
		return toAst( file, argument.expression().get( 0 ) );
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
