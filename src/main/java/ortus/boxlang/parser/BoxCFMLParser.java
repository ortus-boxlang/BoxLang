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

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BOMInputStream;

import ortus.boxlang.ast.BoxScript;
import ortus.boxlang.ast.BoxStatement;
import ortus.boxlang.ast.Point;
import ortus.boxlang.ast.Position;
import ortus.boxlang.ast.SourceFile;
import ortus.boxlang.parser.antlr.CFMLLexer;
import ortus.boxlang.parser.antlr.CFMLParser;

public class BoxCFMLParser extends BoxAbstractParser {

	public BoxCFMLParser() {
		super();
	}

	@Override
	protected ParserRuleContext parserFirstStage( InputStream inputStream ) throws IOException {
		CFMLLexer	lexer	= new CFMLLexer( CharStreams.fromStream( inputStream ) );
		CFMLParser	parser	= new CFMLParser( new CommonTokenStream( lexer ) );
		addErrorListeners( lexer, parser );
		return parser.htmlDocument();
	}

	@Override
	protected BoxScript parseTreeToAst( File file, ParserRuleContext parseTree ) throws IOException {
		Position			position	= new Position( new Point( parseTree.start.getLine(), parseTree.start.getCharPositionInLine() ),
		    new Point( parseTree.stop.getLine(), parseTree.stop.getCharPositionInLine() ), new SourceFile( file ) );
		String				sourceText	= ""; // TODO: extract from parse tree
		List<BoxStatement>	statements	= new ArrayList<>();
		return new BoxScript( statements, position, sourceText );
	}

	public ParsingResult parse( File file ) throws IOException {
		BOMInputStream					inputStream	= getInputStream( file );

		CFMLParser.HtmlDocumentContext	parseTree	= ( CFMLParser.HtmlDocumentContext ) parserFirstStage( inputStream );
		BoxScript						ast			= parseTreeToAst( file, parseTree );
		return new ParsingResult( ast, issues );
	}

	public ParsingResult parse( String code ) throws IOException {
		InputStream						inputStream	= IOUtils.toInputStream( code, StandardCharsets.UTF_8 );
		CFMLParser.HtmlDocumentContext	parseTree	= ( CFMLParser.HtmlDocumentContext ) parserFirstStage( inputStream );
		BoxScript						ast			= parseTreeToAst( file, parseTree );
		return new ParsingResult( ast, issues );
	}

}
