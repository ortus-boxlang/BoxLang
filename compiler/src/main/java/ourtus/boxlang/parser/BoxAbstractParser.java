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

import org.antlr.v4.runtime.*;
import org.apache.commons.io.ByteOrderMark;
import org.apache.commons.io.input.BOMInputStream;
import org.jetbrains.annotations.NotNull;
import ourtus.boxlang.ast.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public abstract class BoxAbstractParser {

	protected File file;
	protected final List<Issue> issues;

	private final BaseErrorListener errorListener = new BaseErrorListener() {

		@Override
		public void syntaxError( Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine,
			String msg, RecognitionException e ) {
			String errorMessage = msg != null ? msg : "unspecified";
			Position position = new Position( new Point( line, charPositionInLine ), new Point( line, charPositionInLine ) );
			if ( file != null ) {
				position.setSource( new SourceFile( file ) );
			}
			issues.add( new Issue( errorMessage, position ) );
		}
	};

	public BoxAbstractParser() {
		this.issues = new ArrayList<>();
	}

	protected BOMInputStream getInputStream( File file ) throws IOException {
		this.file = file;
		return BOMInputStream.builder().setFile( file ).setByteOrderMarks( ByteOrderMark.UTF_8 ).setInclude( false ).get();

	}

	protected void addErrorListeners( @NotNull Lexer lexer, @NotNull Parser parser ) {
		lexer.removeErrorListeners();
		lexer.addErrorListener( errorListener );
		parser.removeErrorListeners();
		parser.addErrorListener( errorListener );
	}

	protected abstract ParserRuleContext parserFirstStage( InputStream stream ) throws IOException;

	protected abstract BoxScript parseTreeToAst( File file, ParserRuleContext parseTree ) throws IOException;
}
