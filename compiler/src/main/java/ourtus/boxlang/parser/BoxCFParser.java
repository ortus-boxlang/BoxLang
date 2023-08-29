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
import org.apache.commons.io.input.BOMInputStream;
import ortus.boxlang.parser.CFLexer;
import ortus.boxlang.parser.CFParser;
import ourtus.boxlang.ast.*;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class BoxCFParser extends BoxAbstractParser {

	public BoxCFParser() {
		super();
	}

	@Override
	protected ParserRuleContext parserFirstStage( File file ) throws IOException {
		BOMInputStream inputStream = getInputStream( file );

		CFLexer lexer = new CFLexer( CharStreams.fromStream( inputStream ) );
		CFParser parser = new CFParser( new CommonTokenStream( lexer ) );
		addErrorListeners( lexer, parser );

		return parser.script();
	}

	@Override
	protected BoxScript parseTreeToAst( File file, ParserRuleContext rule ) throws IOException {
		CFParser.ScriptContext parseTree = ( CFParser.ScriptContext ) rule;
		List<BoxStatement> statements = parseTree.functionOrStatement().stream().map( it -> toAst( file, it ) ).toList();

		return new BoxScript( getPosition( rule ), getSourceText( rule ), statements );
	}

	public ParsingResult parse( File file ) throws IOException {
		CFParser.ScriptContext parseTree = ( CFParser.ScriptContext ) parserFirstStage( file );
		BoxScript ast = parseTreeToAst( file, parseTree );
		return new ParsingResult( ast, issues );
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
		} else {
			throw new IllegalStateException( "not implemented: " + node.getClass().getSimpleName() );
		}
	}

	private BoxStatement toAst( File file, CFParser.SimpleStatementContext node ) {
		if ( node.assignment() != null ) {
			return toAst( file, node.assignment() );
		} else {
			throw new IllegalStateException( "not implemented: " + node.getClass().getSimpleName() );
		}
	}

	private BoxStatement toAst( File file, CFParser.AssignmentContext node ) {
		BoxExpr left = toAst( file, node.assignmentLeft() );
		BoxExpr right = toAst( file, node.assignmentRight() );
		return new BoxStmtAssignment( left, right, getPosition( node ), getSourceText( node ) );
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

		throw new IllegalStateException( "not implemented: " + expression.getClass().getSimpleName() );
	}

	private BoxExpr toAst( File file, CFParser.ArrayAccessContext node ) {
		BoxExpr index = toAst( file, node.arrayAccessIndex().expression() );
		BoxExpr context = toAst( file, node.identifier() );
		return new BoxExprArrayAccess( context, index, getPosition( node ), getSourceText( node ) );
	}

	private BoxExpr toAst( File file, CFParser.IdentifierContext identifier ) {
		return new BoxExprIdentifier( identifier.getText(), getPosition( identifier ), getSourceText( identifier ) );
	}

	private BoxExpr toAst( File file, CFParser.AssignmentRightContext right ) {
		return toAst( file, right.expression() );
	}

	private BoxExpr toAst( File file, CFParser.ExpressionContext expression ) {
		if ( expression.literalExpression() != null ) {
			if ( expression.literalExpression().stringLiteral() != null ) {
				return new BoxExprStringLiteral( expression.literalExpression().stringLiteral().getText(),
					getPosition( expression.literalExpression().stringLiteral() ),
					getSourceText( expression.literalExpression().stringLiteral() ) );
			}
			// TODO: add other cases
		} else if ( expression.identifier() != null ) {
			return toAst( file, expression.identifier() );
		} else if ( expression.objectExpression() != null ) {
			return toAst( file, expression.objectExpression() );
		}

		throw new IllegalStateException( "not implemented: " + expression.getClass().getSimpleName() );
	}

	private BoxExpr toAst( File file, CFParser.ObjectExpressionContext expression ) {
		if ( expression.arrayAccess() != null )
			return toAst( file, expression.arrayAccess() );
		else if ( expression.functionInvokation() != null )
			return toAst( file, expression.functionInvokation() );
		// TODO: add other cases
		throw new IllegalStateException( "not implemented: " + expression.getClass().getSimpleName() );
	}

	private BoxExpr toAst( File file, CFParser.FunctionInvokationContext invocation ) {
		BoxExprFunctionInvocation functionInvocation = new BoxExprFunctionInvocation( invocation.identifier().getText(),
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
