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

		Position position = new Position( new Point( parseTree.start.getLine(), parseTree.start.getCharPositionInLine() ),
			new Point( parseTree.stop.getLine(), parseTree.stop.getCharPositionInLine() ), new SourceFile( file ) );
		String sourceText = ""; // TODO: extract from parse tree

		List<BoxStatement> statements = parseTree.functionOrStatement().stream().map( it -> toAst( file, it ) ).toList();

		return new BoxScript( position, sourceText, statements );
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
		throw new IllegalStateException( "not implemented: " + node.getClass().getSimpleName() );
	}

	private BoxStatement toAst( File file, CFParser.FunctionContext function ) {
		return null; // TODO
	}

	private BoxStatement toAst( File file, CFParser.ConstructorContext constructor ) {
		return null; // TODO
	}
}
