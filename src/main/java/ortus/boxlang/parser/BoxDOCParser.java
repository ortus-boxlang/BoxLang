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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.apache.commons.io.IOUtils;

import ortus.boxlang.ast.BoxDocumentation;
import ortus.boxlang.ast.BoxNode;
import ortus.boxlang.ast.Issue;
import ortus.boxlang.ast.Point;
import ortus.boxlang.ast.Position;
import ortus.boxlang.ast.SourceFile;
import ortus.boxlang.ast.expression.BoxFQN;
import ortus.boxlang.ast.expression.BoxStringLiteral;
import ortus.boxlang.ast.statement.BoxDocumentationAnnotation;
import ortus.boxlang.parser.antlr.DOCLexer;
import ortus.boxlang.parser.antlr.DOCParser;

/**
 * Parser a javadoc style documentation
 */
public class BoxDOCParser {

	protected int					startLine;
	protected int					startColumn;
	protected File					file;
	protected final List<Issue>		issues;

	/**
	 * Overrides the ANTL4 default error listener collecting the errors
	 */
	private final BaseErrorListener	errorListener	= new BaseErrorListener() {

														@Override
														public void syntaxError( Recognizer<?, ?> recognizer, Object offendingSymbol, int line,
														    int charPositionInLine,
														    String msg, RecognitionException e ) {
															String		errorMessage	= msg != null ? msg : "unspecified";
															Position	position		= new Position( new Point( line, charPositionInLine ),
															    new Point( line, charPositionInLine ) );
															if ( file != null ) {
																position.setSource( new SourceFile( file ) );
															}
															issues.add( new Issue( errorMessage, position ) );
														}
													};

	public BoxDOCParser() {
		this.startLine		= 0;
		this.startColumn	= 0;
		this.issues			= new ArrayList<>();
	}

	public BoxDOCParser( int startLine, int startColumn ) {
		this.startLine		= startLine;
		this.startColumn	= startColumn;
		this.issues			= new ArrayList<>();
	}

	/**
	 * Extracts the position from the ANTLR node
	 *
	 * @param node any ANTLR role
	 *
	 * @return a Position representing the region on the source code
	 *
	 * @see Position
	 */
	protected Position getPosition( ParserRuleContext node ) {
		return new Position( new Point( node.start.getLine() + this.startLine, node.start.getCharPositionInLine() + startColumn ),
		    new Point( node.stop.getLine() + startLine, node.stop.getCharPositionInLine() + startColumn ), new SourceFile( file ) );
	}

	public ParsingResult parse( File file, String code ) throws IOException {
		InputStream						inputStream	= IOUtils.toInputStream( code, StandardCharsets.UTF_8 );
		DOCParser.DocumentationContext	parseTree	= ( DOCParser.DocumentationContext ) parserFirstStage( file, inputStream );
		if ( issues.isEmpty() ) {

			BoxDocumentation ast = toAst( file, parseTree );
			return new ParsingResult( ast, issues );
		}
		return new ParsingResult( null, issues );
	}

	private BoxDocumentation toAst( File file, DOCParser.DocumentationContext parseTree ) {
		List<BoxNode> annotations = new ArrayList<>();
		parseTree.documentationContent().tagSection().blockTag().forEach( it -> {
			annotations.add( toAst( file, it ) );
		} );
		if ( parseTree.documentationContent().description() != null ) {
			annotations.add( toAst( file, parseTree.documentationContent().description() ) );
		}
		return new BoxDocumentation( annotations, getPosition( parseTree ), null );
	}

	private BoxNode toAst( File file, DOCParser.DescriptionContext node ) {
		BoxFQN			name	= new BoxFQN( "hint", null, null );
		// use string builder to get text from child nodes that are NOT descriptionNewLIne
		StringBuilder	valueSB	= new StringBuilder();
		node.children.forEach( it -> {
			if ( ! ( it instanceof DOCParser.DescriptionNewlineContext ) ) {
				valueSB.append( it.getText() );
			}
		} );
		BoxStringLiteral value = new BoxStringLiteral( valueSB.toString(), null, null );
		return new BoxDocumentationAnnotation( name, value, getPosition( node ), null );
	}

	private BoxNode toAst( File file, DOCParser.BlockTagContext node ) {
		BoxFQN			name	= new BoxFQN( node.blockTagName().NAME().getText(), null, null );
		// use string builder to get text from child nodes that are NOT a new line
		StringBuilder	valueSB	= new StringBuilder();
		node.blockTagContent().forEach( it -> {
			if ( it.NEWLINE() == null ) {
				valueSB.append( it.getText() );
			}
		} );
		BoxStringLiteral value = new BoxStringLiteral( valueSB.toString(), null, null );
		return new BoxDocumentationAnnotation( name, value, getPosition( node ), null );
	}

	protected ParserRuleContext parserFirstStage( File file, InputStream stream ) throws IOException {
		this.file = file;
		DOCLexer	lexer	= new DOCLexer( CharStreams.fromStream( stream ) );
		DOCParser	parser	= new DOCParser( new CommonTokenStream( lexer ) );
		addErrorListeners( lexer, parser );

		return parser.documentation();
	}

	public int getStartLine() {
		return startLine;
	}

	public void setStartLine( int startLine ) {
		this.startLine = startLine;
	}

	public int getStartColumn() {
		return startColumn;
	}

	public void setStartColumn( int startColumn ) {
		this.startColumn = startColumn;
	}

	/**
	 * Add the parser error listener to the ANTLR parser
	 *
	 * @param lexer  ANTLR lexer instance
	 * @param parser ANTLR parser instance
	 */
	protected void addErrorListeners( Lexer lexer, Parser parser ) {
		lexer.removeErrorListeners();
		lexer.addErrorListener( errorListener );
		parser.removeErrorListeners();
		parser.addErrorListener( errorListener );
	}

}
