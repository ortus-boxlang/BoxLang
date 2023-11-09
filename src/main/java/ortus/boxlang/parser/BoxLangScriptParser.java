/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ortus.boxlang.parser;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;

import ortus.boxlang.ast.BoxScript;
import ortus.boxlang.ast.BoxStatement;
import ortus.boxlang.parser.antlr.BoxScriptLexer;
import ortus.boxlang.parser.antlr.BoxScriptParser;

/**
 * Parser for box scripts
 */
public class BoxLangScriptParser extends BoxAbstractParser {

	public BoxLangScriptParser() {
		super();
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
		BoxScriptLexer	lexer	= new BoxScriptLexer( CharStreams.fromStream( stream ) );
		BoxScriptParser	parser	= new BoxScriptParser( new CommonTokenStream( lexer ) );
		addErrorListeners( lexer, parser );

		return parser.script();
	}

	@Override
	protected BoxScript parseTreeToAst( File file, ParserRuleContext rule ) throws IOException {
		BoxScriptParser.ScriptContext	parseTree	= ( BoxScriptParser.ScriptContext ) rule;
		List<BoxStatement>				statements	= new ArrayList<>();

		if ( parseTree.tag() != null ) {
			parseTree = parseTree.tag().script();
		}

		parseTree.functionOrStatement().forEach( stmt -> {
			statements.add( toAst( file, stmt ) );
		} );
		return new BoxScript( statements, getPosition( rule ), getSourceText( rule ) );

	}

	private BoxStatement toAst( File file, BoxScriptParser.FunctionOrStatementContext stmt ) {
		return null;
	}

	public ParsingResult parse( File file ) {
		return null;
	}
}
