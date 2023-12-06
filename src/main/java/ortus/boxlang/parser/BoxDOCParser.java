package ortus.boxlang.parser;

import com.github.javaparser.ast.expr.StringLiteralExpr;
import org.antlr.v4.runtime.*;
import org.apache.commons.io.IOUtils;
import ortus.boxlang.ast.*;
import ortus.boxlang.ast.expression.BoxFQN;
import ortus.boxlang.ast.expression.BoxStringLiteral;
import ortus.boxlang.ast.statement.BoxAnnotation;
import ortus.boxlang.parser.antlr.CFParser;
import ortus.boxlang.parser.antlr.DOCLexer;
import ortus.boxlang.parser.antlr.DOCParser;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Parser a javadoc style documentation
 */
public class BoxDOCParser {

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
		this.issues = new ArrayList<>();
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
		return new BoxDocumentation( annotations, null, null );
	}

	private BoxNode toAst( File file, DOCParser.BlockTagContext node ) {
		BoxFQN				name	= new BoxFQN( node.blockTagName().NAME().getText(), null, null );
		BoxStringLiteral	value	= new BoxStringLiteral( node.blockTagContent( 0 ).getText(), null, null );
		return new BoxAnnotation( name, value, null, null );
	}

	protected ParserRuleContext parserFirstStage( File file, InputStream stream ) throws IOException {
		this.file = file;
		DOCLexer	lexer	= new DOCLexer( CharStreams.fromStream( stream ) );
		DOCParser	parser	= new DOCParser( new CommonTokenStream( lexer ) );
		addErrorListeners( lexer, parser );

		return parser.documentation();
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
