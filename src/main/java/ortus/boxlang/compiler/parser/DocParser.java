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
package ortus.boxlang.compiler.parser;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.apache.commons.io.IOUtils;

import ortus.boxlang.compiler.ast.BoxDocumentation;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.expression.BoxFQN;
import ortus.boxlang.compiler.ast.expression.BoxStringLiteral;
import ortus.boxlang.compiler.ast.statement.BoxDocumentationAnnotation;
import ortus.boxlang.parser.antlr.DocGrammar;
import ortus.boxlang.parser.antlr.DocLexer;

/**
 * Parser a javadoc style documentation
 */
public class DocParser extends AbstractParser {

	protected File file;

	public DocParser() {
		super();
	}

	public DocParser( int startLine, int startColumn ) {
		this();
		this.startLine		= startLine - 1;
		this.startColumn	= startColumn;
	}

	public ParsingResult parse( File file, String code ) throws IOException {
		InputStream						inputStream	= IOUtils.toInputStream( code, StandardCharsets.UTF_8 );
		DocGrammar.DocumentationContext	parseTree	= ( DocGrammar.DocumentationContext ) parserFirstStage( file, inputStream );
		if ( issues.isEmpty() ) {

			BoxDocumentation ast = toAst( file, parseTree );
			return new ParsingResult( ast, issues );
		}
		return new ParsingResult( null, issues );
	}

	private BoxDocumentation toAst( File file, DocGrammar.DocumentationContext parseTree ) {
		List<BoxNode> annotations = new ArrayList<>();
		if ( parseTree.documentationContent() != null ) {
			if ( parseTree.documentationContent().tagSection() != null ) {
				parseTree.documentationContent().tagSection().blockTag().forEach( it -> {
					annotations.add( toAst( file, it ) );
				} );
			}
			if ( parseTree.documentationContent().description() != null ) {
				annotations.add( toAst( file, parseTree.documentationContent().description() ) );
			}
		}
		return new BoxDocumentation( annotations, getPosition( parseTree ), getSourceText( parseTree ) );
	}

	private BoxNode toAst( File file, DocGrammar.DescriptionContext node ) {
		BoxFQN			name		= new BoxFQN( "hint", null, null );
		int				numLines	= 0;
		// use string builder to get text from child nodes that are NOT descriptionNewLIne
		StringBuilder	valueSB		= new StringBuilder();
		for ( var child : node.children ) {
			if ( child instanceof DocGrammar.DescriptionNewlineContext ) {
				numLines++;
				// only add new line if there are more than one
				if ( numLines > 1 ) {
					valueSB.append( "\n" );
				}
			} else if ( child instanceof DocGrammar.SpaceContext ) {
				if ( numLines <= 1 )
					valueSB.append( child.getText() );
			} else {
				valueSB.append( child.getText() );
				numLines = 0;
			}
		}
		BoxStringLiteral value = new BoxStringLiteral( valueSB.toString(), getPosition( node ), getSourceText( node ) );
		return new BoxDocumentationAnnotation( name, value, getPosition( node ), getSourceText( node ) );
	}

	private BoxNode toAst( File file, DocGrammar.BlockTagContext node ) {
		BoxFQN			name	= new BoxFQN( node.blockTagName().NAME().getText(), getPosition( node.blockTagName() ), getSourceText( node.blockTagName() ) );
		// use string builder to get text from child nodes that are NOT a new line
		StringBuilder	valueSB	= new StringBuilder();
		node.blockTagContent().forEach( it -> {
			if ( it.NEWLINE() == null ) {
				valueSB.append( it.getText() );
			}
		} );
		BoxStringLiteral value = new BoxStringLiteral( valueSB.toString(), getPosition( node ), getSourceText( node ) );
		return new BoxDocumentationAnnotation( name, value, getPosition( node ), getSourceText( node ) );
	}

	protected ParserRuleContext parserFirstStage( File file, InputStream stream ) throws IOException {
		this.file = file;
		DocLexer	lexer	= new DocLexer( CharStreams.fromStream( stream ) );
		DocGrammar	parser	= new DocGrammar( new CommonTokenStream( lexer ) );
		addErrorListeners( lexer, parser );

		return parser.documentation();
	}

	public int getStartLine() {
		return startLine;
	}

	public void setStartLine( int startLine ) {
		this.startLine = startLine - 1;
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

	@Override
	public ParsingResult parse( File file ) throws IOException {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException( "Unimplemented method 'parse'" );
	}

	@Override
	public ParsingResult parse( String code ) throws IOException {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException( "Unimplemented method 'parse'" );
	}

	@Override
	protected ParserRuleContext parserFirstStage( InputStream stream ) throws IOException {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException( "Unimplemented method 'parserFirstStage'" );
	}

	@Override
	protected BoxNode parseTreeToAst( File file, ParserRuleContext rule ) throws IOException {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException( "Unimplemented method 'parseTreeToAst'" );
	}

}
