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

import ortus.boxlang.ast.BoxBufferOutput;
import ortus.boxlang.ast.BoxExpr;
import ortus.boxlang.ast.BoxNode;
import ortus.boxlang.ast.BoxScript;
import ortus.boxlang.ast.BoxScriptIsland;
import ortus.boxlang.ast.BoxStatement;
import ortus.boxlang.ast.BoxTemplate;
import ortus.boxlang.ast.Point;
import ortus.boxlang.ast.Position;
import ortus.boxlang.ast.expression.BoxStringInterpolation;
import ortus.boxlang.ast.expression.BoxStringLiteral;
import ortus.boxlang.ast.statement.BoxExpression;
import ortus.boxlang.ast.statement.BoxIfElse;
import ortus.boxlang.ast.statement.tag.BoxOutput;
import ortus.boxlang.parser.antlr.CFMLLexer;
import ortus.boxlang.parser.antlr.CFMLParser;
import ortus.boxlang.parser.antlr.CFMLParser.OutputContext;
import ortus.boxlang.parser.antlr.CFMLParser.ScriptContext;
import ortus.boxlang.parser.antlr.CFMLParser.SetContext;
import ortus.boxlang.parser.antlr.CFMLParser.StatementContext;
import ortus.boxlang.parser.antlr.CFMLParser.StatementsContext;
import ortus.boxlang.parser.antlr.CFMLParser.TemplateContext;
import ortus.boxlang.parser.antlr.CFMLParser.TextContentContext;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

public class BoxCFMLParser extends BoxAbstractParser {

	public BoxCFMLParser() {
		super();
	}

	public BoxCFMLParser( int startLine, int startColumn ) {
		super( startLine, startColumn );
	}

	@Override
	protected ParserRuleContext parserFirstStage( InputStream inputStream ) throws IOException {
		CFMLLexer	lexer	= new CFMLLexer( CharStreams.fromStream( inputStream ) );
		CFMLParser	parser	= new CFMLParser( new CommonTokenStream( lexer ) );
		addErrorListeners( lexer, parser );
		return parser.template();
	}

	@Override
	protected BoxScript parseTreeToAst( File file, ParserRuleContext parseTree ) throws IOException {
		TemplateContext		template	= ( TemplateContext ) parseTree;

		List<BoxStatement>	statements	= new ArrayList<>();
		if ( template.statements() != null ) {
			statements.addAll( toAst( file, template.statements() ) );
		}
		if ( template.component() != null ) {
			throw new BoxRuntimeException( "tag component parsing not implemented yet" );
		}
		if ( template.interface_() != null ) {
			throw new BoxRuntimeException( "tag interface parsing not implemented yet" );
		}
		return new BoxTemplate( statements, getPosition( parseTree ), getSourceText( parseTree ) );
	}

	private List<BoxStatement> toAst( File file, StatementsContext node ) {
		List<BoxStatement> statements = new ArrayList<>();
		for ( var child : node.children ) {
			if ( child instanceof StatementContext statement ) {
				statements.add( toAst( file, statement ) );
			} else if ( child instanceof TextContentContext textContent ) {
				statements.add( toAst( file, textContent ) );
			} else if ( child instanceof ScriptContext script ) {
				if ( script.scriptBody() != null ) {
					statements.add(
					    new BoxScriptIsland(
					        parseCFStatements( script.scriptBody().getText(), getPosition( script.scriptBody() ) ),
					        getPosition( script.scriptBody() ),
					        getSourceText( script.scriptBody() )
					    )
					);
				}
			}
		}
		return statements;
	}

	private BoxStatement toAst( File file, StatementContext node ) {
		if ( node.output() != null ) {
			return toAst( file, node.output() );
		} else if ( node.set() != null ) {
			return toAst( file, node.set() );
		} else if ( node.if_() != null ) {
			return toAst( file, node.if_() );
		}
		throw new BoxRuntimeException( "Statement node " + node.getClass().getName() + " parsing not implemented yet. " + node.getText() );

	}

	private BoxIfElse toAst( File file, CFMLParser.IfContext node ) {
		// if condition will always exist
		BoxExpr				condition	= parseCFExpression( node.ifCondition.getText(), getPosition( node.ifCondition ) );
		List<BoxStatement>	thenBody	= new ArrayList<>();
		List<BoxStatement>	elseBody	= new ArrayList<>();

		// Then body will always exist
		thenBody.addAll( toAst( file, node.thenBody ) );

		if ( node.ELSE() != null ) {
			elseBody.addAll( toAst( file, node.elseBody ) );
		}

		// Loop backward over elseif conditions, each one becoming the elseBody of the next.
		for ( int i = node.elseIfCondition.size() - 1; i >= 0; i-- ) {
			int		stopIndex;
			Point	end	= new Point( node.elseIfTagClose.get( i ).getLine(),
			    node.elseIfTagClose.get( i ).getCharPositionInLine() );
			stopIndex = node.elseIfTagClose.get( i ).getStopIndex();
			if ( node.elseThenBody.get( i ).statement().size() > 0 ) {
				end			= new Point( node.elseThenBody.get( i ).statement( node.elseThenBody.get( i ).statement().size() - 1 ).getStop().getLine(),
				    node.elseThenBody.get( i ).statement( node.elseThenBody.get( i ).statement().size() - 1 ).getStop().getCharPositionInLine() );
				stopIndex	= node.elseThenBody.get( i ).statement( node.elseThenBody.get( i ).statement().size() - 1 ).getStop().getStopIndex();
			}
			Position	pos				= new Position(
			    new Point( node.ELSEIF( i ).getSymbol().getLine(), node.ELSEIF( i ).getSymbol().getCharPositionInLine() - 3 ),
			    end );
			BoxExpr		thisCondition	= parseCFExpression( node.elseIfCondition.get( i ).getText(), getPosition( node.elseIfCondition.get( i ) ) );
			elseBody = List.of( new BoxIfElse( thisCondition, toAst( file, node.elseThenBody.get( i ) ), elseBody, pos,
			    getSourceText( node, node.ELSEIF().get( i ).getSymbol().getStartIndex() - 3, stopIndex ) ) );
		}

		// If there were no elseif's, the elsebody here will be the <cfelse>. Otherwise, it will be the last elseif.
		return new BoxIfElse( condition, thenBody, elseBody, getPosition( node ), getSourceText( node ) );
	}

	private BoxStatement toAst( File file, SetContext set ) {
		// In tags, a <bx:set ...> tag is an Expression Statement.
		return new BoxExpression( parseCFExpression( set.expression().getText(), getPosition( set.expression() ) ), getPosition( set ), getSourceText( set ) );
	}

	private BoxStatement toAst( File file, OutputContext node ) {
		List<BoxStatement> statements = new ArrayList<>();
		if ( node.statements() != null ) {
			statements.addAll( toAst( file, node.statements() ) );
		}
		return new BoxOutput( statements, getPosition( node ), getSourceText( node ) );
	}

	private BoxStatement toAst( File file, TextContentContext node ) {
		BoxExpr expression = null;
		// No interpolated expressions, only string
		if ( node.interpolatedExpression().isEmpty() ) {
			expression = new BoxStringLiteral( escapeStringLiteral( node.getText() ), getPosition( node ), getSourceText( node ) );
		} else {
			List<BoxExpr> expressions = new ArrayList<>();
			for ( var child : node.children ) {
				if ( child instanceof CFMLParser.InterpolatedExpressionContext intrpexpr && intrpexpr.expression() != null ) {
					// parse the text between the hash signs as a CF expression
					expressions.add( parseCFExpression( intrpexpr.expression().getText(), getPosition( intrpexpr ) ) );
				} else if ( child instanceof CFMLParser.NonInterpolatedTextContext strlit ) {
					expressions.add( new BoxStringLiteral( escapeStringLiteral( strlit.getText() ), getPosition( strlit ), getSourceText( strlit ) ) );
				}
			}
			expression = new BoxStringInterpolation( expressions, getPosition( node ), getSourceText( node ) );
		}
		return new BoxBufferOutput( expression, getPosition( node ), getSourceText( node ) );
	}

	/**
	 * Escape pounds in a string literal
	 *
	 * @param string the string to escape
	 *
	 * @return the escaped string
	 */
	private String escapeStringLiteral( String string ) {
		return string.replace( "##", "#" );
	}

	public ParsingResult parse( File file ) throws IOException {
		BOMInputStream				inputStream	= getInputStream( file );

		CFMLParser.TemplateContext	parseTree	= ( CFMLParser.TemplateContext ) parserFirstStage( inputStream );
		BoxScript					ast			= parseTreeToAst( file, parseTree );
		return new ParsingResult( ast, issues );
	}

	public ParsingResult parse( String code ) throws IOException {
		InputStream					inputStream	= IOUtils.toInputStream( code, StandardCharsets.UTF_8 );
		CFMLParser.TemplateContext	parseTree	= ( CFMLParser.TemplateContext ) parserFirstStage( inputStream );
		BoxScript					ast			= parseTreeToAst( file, parseTree );
		return new ParsingResult( ast, issues );
	}

	public BoxExpr parseCFExpression( String code, Position position ) {
		try {
			ParsingResult result = new BoxCFParser( position.getStart().getLine(), position.getStart().getColumn() ).parseExpression( code );
			if ( result.getIssues().isEmpty() ) {
				return ( BoxExpr ) result.getRoot();
			} else {
				// Add these issues to the main parser
				issues.addAll( result.getIssues() );
				return null;
			}
		} catch ( IOException e ) {
			throw new BoxRuntimeException( "Error parsing interpolated expression: " + code, e );
		}
	}

	public List<BoxStatement> parseCFStatements( String code, Position position ) {
		try {
			ParsingResult result = new BoxCFParser( position.getStart().getLine(), position.getStart().getColumn() ).parse( code );
			if ( result.getIssues().isEmpty() ) {
				BoxNode root = result.getRoot();
				if ( root instanceof BoxScript script ) {
					return script.getStatements();
				} else if ( root instanceof BoxStatement statement ) {
					return List.of( statement );
				} else {
					// Could be a BoxClass, which we may actually need to support if there is a .cfc file with a top-level <cfscript> node containing a
					// component.
					throw new BoxRuntimeException( "Unexpected root node type [" + root.getClass().getName() + "] in script island." );
				}
			} else {
				// Add these issues to the main parser
				issues.addAll( result.getIssues() );
				return null;
			}
		} catch ( IOException e ) {
			throw new BoxRuntimeException( "Error parsing interpolated expression: " + code, e );
		}
	}
}
