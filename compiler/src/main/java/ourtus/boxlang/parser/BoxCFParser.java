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
package ourtus.boxlang.parser;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.ParserRuleContext;
import org.apache.commons.io.ByteOrderMark;
import org.apache.commons.io.input.BOMInputStream;
import ortus.boxlang.parser.CFLexer;
import ortus.boxlang.parser.CFParser;
import ourtus.boxlang.ast.ParsingResult;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class BoxCFParser extends BoxAbstractParser {
	public BoxCFParser() {
		super();
	}
	@Override
	protected ParserRuleContext parserFirstStage(File file) throws IOException {
		BOMInputStream inputStream = getInputStream(file);

		CFLexer lexer = new CFLexer(CharStreams.fromStream(inputStream));
		CFParser parser = new CFParser(new CommonTokenStream(lexer));
		addErrorListeners(lexer,parser);

		return parser.script();
	}

	public ParsingResult parse(File file) throws IOException {
		CFParser.ScriptContext parseTree = (CFParser.ScriptContext) parserFirstStage(file);

		return new ParsingResult(null,issues);
	}
}
