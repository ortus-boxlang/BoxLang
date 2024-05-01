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
package ortus.boxlang.compiler.parser;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BOMInputStream;

import ortus.boxlang.compiler.ast.BoxClass;
import ortus.boxlang.compiler.ast.BoxExpression;
import ortus.boxlang.compiler.ast.BoxInterface;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.BoxScript;
import ortus.boxlang.compiler.ast.BoxStatement;
import ortus.boxlang.compiler.ast.BoxTemplate;
import ortus.boxlang.compiler.ast.Issue;
import ortus.boxlang.compiler.ast.Point;
import ortus.boxlang.compiler.ast.Position;
import ortus.boxlang.compiler.ast.Source;
import ortus.boxlang.compiler.ast.SourceCode;
import ortus.boxlang.compiler.ast.SourceFile;
import ortus.boxlang.compiler.ast.expression.BoxClosure;
import ortus.boxlang.compiler.ast.expression.BoxFQN;
import ortus.boxlang.compiler.ast.expression.BoxIdentifier;
import ortus.boxlang.compiler.ast.expression.BoxNull;
import ortus.boxlang.compiler.ast.expression.BoxStringInterpolation;
import ortus.boxlang.compiler.ast.expression.BoxStringLiteral;
import ortus.boxlang.compiler.ast.statement.BoxAccessModifier;
import ortus.boxlang.compiler.ast.statement.BoxAnnotation;
import ortus.boxlang.compiler.ast.statement.BoxArgumentDeclaration;
import ortus.boxlang.compiler.ast.statement.BoxBreak;
import ortus.boxlang.compiler.ast.statement.BoxBufferOutput;
import ortus.boxlang.compiler.ast.statement.BoxContinue;
import ortus.boxlang.compiler.ast.statement.BoxDocumentationAnnotation;
import ortus.boxlang.compiler.ast.statement.BoxExpressionStatement;
import ortus.boxlang.compiler.ast.statement.BoxFunctionDeclaration;
import ortus.boxlang.compiler.ast.statement.BoxIfElse;
import ortus.boxlang.compiler.ast.statement.BoxImport;
import ortus.boxlang.compiler.ast.statement.BoxMethodDeclarationModifier;
import ortus.boxlang.compiler.ast.statement.BoxProperty;
import ortus.boxlang.compiler.ast.statement.BoxRethrow;
import ortus.boxlang.compiler.ast.statement.BoxReturn;
import ortus.boxlang.compiler.ast.statement.BoxReturnType;
import ortus.boxlang.compiler.ast.statement.BoxScriptIsland;
import ortus.boxlang.compiler.ast.statement.BoxSwitch;
import ortus.boxlang.compiler.ast.statement.BoxSwitchCase;
import ortus.boxlang.compiler.ast.statement.BoxThrow;
import ortus.boxlang.compiler.ast.statement.BoxTry;
import ortus.boxlang.compiler.ast.statement.BoxTryCatch;
import ortus.boxlang.compiler.ast.statement.BoxType;
import ortus.boxlang.compiler.ast.statement.BoxWhile;
import ortus.boxlang.compiler.ast.statement.component.BoxComponent;
import ortus.boxlang.compiler.ast.visitor.CFTranspilerVisitor;
import ortus.boxlang.parser.antlr.CFTemplateGrammar;
import ortus.boxlang.parser.antlr.CFTemplateGrammar.ArgumentContext;
import ortus.boxlang.parser.antlr.CFTemplateGrammar.AttributeContext;
import ortus.boxlang.parser.antlr.CFTemplateGrammar.AttributeValueContext;
import ortus.boxlang.parser.antlr.CFTemplateGrammar.BoxImportContext;
import ortus.boxlang.parser.antlr.CFTemplateGrammar.BreakContext;
import ortus.boxlang.parser.antlr.CFTemplateGrammar.CaseContext;
import ortus.boxlang.parser.antlr.CFTemplateGrammar.CatchBlockContext;
import ortus.boxlang.parser.antlr.CFTemplateGrammar.ClassOrInterfaceContext;
import ortus.boxlang.parser.antlr.CFTemplateGrammar.ComponentContext;
import ortus.boxlang.parser.antlr.CFTemplateGrammar.ContinueContext;
import ortus.boxlang.parser.antlr.CFTemplateGrammar.FunctionContext;
import ortus.boxlang.parser.antlr.CFTemplateGrammar.GenericOpenCloseComponentContext;
import ortus.boxlang.parser.antlr.CFTemplateGrammar.GenericOpenComponentContext;
import ortus.boxlang.parser.antlr.CFTemplateGrammar.IncludeContext;
import ortus.boxlang.parser.antlr.CFTemplateGrammar.InterfaceContext;
import ortus.boxlang.parser.antlr.CFTemplateGrammar.InterpolatedExpressionContext;
import ortus.boxlang.parser.antlr.CFTemplateGrammar.OutputContext;
import ortus.boxlang.parser.antlr.CFTemplateGrammar.PropertyContext;
import ortus.boxlang.parser.antlr.CFTemplateGrammar.RethrowContext;
import ortus.boxlang.parser.antlr.CFTemplateGrammar.ReturnContext;
import ortus.boxlang.parser.antlr.CFTemplateGrammar.ScriptContext;
import ortus.boxlang.parser.antlr.CFTemplateGrammar.SetContext;
import ortus.boxlang.parser.antlr.CFTemplateGrammar.StatementContext;
import ortus.boxlang.parser.antlr.CFTemplateGrammar.StatementsContext;
import ortus.boxlang.parser.antlr.CFTemplateGrammar.SwitchContext;
import ortus.boxlang.parser.antlr.CFTemplateGrammar.TemplateContext;
import ortus.boxlang.parser.antlr.CFTemplateGrammar.TextContentContext;
import ortus.boxlang.parser.antlr.CFTemplateGrammar.ThrowContext;
import ortus.boxlang.parser.antlr.CFTemplateGrammar.TopLevelStatementsContext;
import ortus.boxlang.parser.antlr.CFTemplateGrammar.TryContext;
import ortus.boxlang.parser.antlr.CFTemplateGrammar.WhileContext;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.components.ComponentDescriptor;
import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.services.ComponentService;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

public class CFTemplateParser extends AbstractParser {

	private int				outputCounter		= 0;
	public ComponentService	componentService	= BoxRuntime.getInstance().getComponentService();

	public CFTemplateParser() {
		super();
	}

	public CFTemplateParser( int startLine, int startColumn ) {
		super( startLine, startColumn );
	}

	public ParsingResult parse( File file ) throws IOException {
		this.file = file;
		setSource( new SourceFile( file ) );
		BOMInputStream		inputStream			= getInputStream( file );

		Optional<String>	ext					= Parser.getFileExtension( file.getAbsolutePath() );
		Boolean				classOrInterface	= ext.isPresent() && ext.get().equalsIgnoreCase( "cfc" );
		BoxNode				ast					= parserFirstStage( inputStream, classOrInterface );
		return new ParsingResult( ast, issues );
	}

	public ParsingResult parse( String code ) throws IOException {
		return parse( code, false );
	}

	public ParsingResult parse( String code, Boolean classOrInterface ) throws IOException {
		this.sourceCode = code;
		setSource( new SourceCode( code ) );
		InputStream	inputStream	= IOUtils.toInputStream( code, StandardCharsets.UTF_8 );
		BoxNode		ast			= parserFirstStage( inputStream, classOrInterface );
		return new ParsingResult( ast, issues );
	}

	@Override
	protected BoxNode parserFirstStage( InputStream inputStream, Boolean classOrInterface ) throws IOException {
		BoxNode					rootNode;
		CFTemplateLexerCustom	lexer	= new CFTemplateLexerCustom( CharStreams.fromStream( inputStream ) );
		CFTemplateGrammar		parser	= new CFTemplateGrammar( new CommonTokenStream( lexer ) );
		addErrorListeners( lexer, parser );
		CFTemplateGrammar.ClassOrInterfaceContext	classOrInterfaceContext	= null;
		CFTemplateGrammar.TemplateContext			templateContext			= null;
		if ( classOrInterface ) {
			classOrInterfaceContext = parser.classOrInterface();
		} else {
			templateContext = parser.template();
		}

		if ( lexer.hasUnpoppedModes() ) {
			List<String>	modes		= lexer.getUnpoppedModes();
			// get position of end of last token from the lexer

			Position		position	= createOffsetPosition( lexer._token.getLine(),
			    lexer._token.getCharPositionInLine() + lexer._token.getText().length() - 1, lexer._token.getLine(),
			    lexer._token.getCharPositionInLine() + lexer._token.getText().length() - 1 );
			// Check for specific unpopped modes that we can throw a specific error for
			if ( lexer.lastModeWas( CFTemplateLexerCustom.OUTPUT_MODE ) ) {
				String	message				= "Unclosed output tag";
				Token	outputStartToken	= lexer.findPreviousToken( CFTemplateLexerCustom.OUTPUT_START );
				if ( outputStartToken != null ) {
					position = createOffsetPosition( outputStartToken.getLine(), outputStartToken.getCharPositionInLine(), outputStartToken.getLine(),
					    outputStartToken.getCharPositionInLine() + outputStartToken.getText().length() );
				}
				message += " on line " + position.getStart().getLine();
				issues.add( new Issue( message, position ) );
			} else if ( lexer.lastModeWas( CFTemplateLexerCustom.COMPONENT_MODE ) ) {
				String	message		= "Unclosed tag";
				Token	startToken	= lexer.findPreviousToken( CFTemplateLexerCustom.COMPONENT_OPEN );
				if ( startToken != null ) {
					position = createOffsetPosition( startToken.getLine(), startToken.getCharPositionInLine(), startToken.getLine(),
					    startToken.getCharPositionInLine() + startToken.getText().length() );
					List<Token> nameTokens = lexer.findPreviousTokenAndXSiblings( CFTemplateLexerCustom.COMPONENT_OPEN, 2 );
					if ( !nameTokens.isEmpty() ) {
						message += " [";
						for ( var t : nameTokens ) {
							message += t.getText();
						}
						message += "]";
					}
				}
				message += " starting on line " + position.getStart().getLine();
				issues.add( new Issue( message, position ) );
			} else {
				issues.add( new Issue( "Invalid Syntax. (Unpopped modes) [" + modes.stream().collect( Collectors.joining( ", " ) ) + "]", position ) );
			}
		}

		// Check if there are unconsumed tokens
		Token token = lexer.nextToken();
		while ( token.getType() != Token.EOF && ( token.getChannel() == CFTemplateLexerCustom.HIDDEN || token.getText().isBlank() ) ) {
			token = lexer.nextToken();
		}
		if ( token.getType() != Token.EOF ) {

			StringBuffer	extraText	= new StringBuffer();
			int				startLine	= token.getLine();
			int				startColumn	= token.getCharPositionInLine();
			int				endColumn	= startColumn + token.getText().length();
			Position		position	= createOffsetPosition( startLine, startColumn, startLine, endColumn );

			while ( token.getType() != Token.EOF && extraText.length() < 100 ) {
				extraText.append( token.getText() );
				token = lexer.nextToken();
			}
			issues.add( new Issue( "Extra char(s) [" + extraText.toString() + "] at the end of parsing.", position ) );
		}

		// Don't attempt to build AST if there are parsing issues
		if ( !issues.isEmpty() ) {
			return null;
		}
		if ( classOrInterface ) {
			rootNode = toAst( null, classOrInterfaceContext );
		} else {
			rootNode = toAst( null, templateContext );
		}
		// Transpile CF to BoxLang
		return rootNode.accept( new CFTranspilerVisitor() );
	}

	private BoxNode toAst( File file, ClassOrInterfaceContext classOrInterface ) {
		if ( classOrInterface.component() != null ) {
			return toAst( file, classOrInterface.component() );
		} else if ( classOrInterface.interface_() != null ) {
			return toAst( file, classOrInterface.interface_() );
		} else if ( classOrInterface.script() != null ) {
			return parseCFClassOrInterface( classOrInterface.script().scriptBody().getText(),
			    getPosition( classOrInterface.script().scriptBody() ) );
		} else {
			throw new IllegalStateException( "Unexpected classOrInterface type: " + classOrInterface.getText() );
		}
	}

	private BoxNode toAst( File file, InterfaceContext interface_ ) {
		List<BoxStatement>					body			= new ArrayList<>();
		List<BoxAnnotation>					annotations		= new ArrayList<>();
		List<BoxAnnotation>					postAnnotations	= new ArrayList<>();
		List<BoxDocumentationAnnotation>	documentation	= new ArrayList<>();
		List<BoxImport>						imports			= new ArrayList<>();

		interface_.boxImport().forEach( stmt -> {
			imports.add( toAst( file, stmt ) );
		} );

		for ( var attr : interface_.attribute() ) {
			annotations.add( toAst( file, attr ) );
		}
		interface_.function().forEach( stmt -> {
			BoxFunctionDeclaration funDec = toAst( file, stmt );
			// I don't think tags have a "default" modifier really and there's no docs on this.
			// So, if the body of the funciton is "empty", make it an abstract interface function
			if ( allStatementsAreWhitespace( funDec.getBody() ) ) {
				funDec.setBody( null );
			}
			body.add( funDec );
		} );

		return new BoxInterface( imports, body, annotations, postAnnotations, documentation, getPosition( interface_ ), getSourceText( interface_ ) );
	}

	protected BoxTemplate toAst( File file, TemplateContext rule ) throws IOException {
		List<BoxStatement> statements = new ArrayList<>();
		if ( rule.topLevelStatements() != null ) {
			statements = toAst( file, rule.topLevelStatements() );
		}
		return new BoxTemplate( statements, getPosition( rule ), getSourceText( rule ) );
	}

	private BoxNode toAst( File file, ComponentContext node ) {
		List<BoxImport>						imports			= new ArrayList<>();
		List<BoxStatement>					body			= new ArrayList<>();
		List<BoxAnnotation>					annotations		= new ArrayList<>();
		// This will be empty in components
		List<BoxDocumentationAnnotation>	documentation	= new ArrayList<>();
		List<BoxProperty>					properties		= new ArrayList<>();

		if ( node.boxImport() != null ) {
			imports.addAll( toAst( file, node.boxImport() ) );
		}
		for ( var attr : node.attribute() ) {
			annotations.add( toAst( file, attr ) );
		}

		if ( node.topLevelStatements() != null ) {
			body.addAll( toAst( file, node.topLevelStatements() ) );
		}
		// loop over body and move any BoxImport statements to the imports list
		for ( int i = body.size() - 1; i >= 0; i-- ) {
			BoxStatement statement = body.get( i );
			if ( statement instanceof BoxImport boxImport ) {
				imports.add( boxImport );
				body.remove( i );
			}
		}
		for ( CFTemplateGrammar.PropertyContext annotation : node.property() ) {
			properties.add( toAst( file, annotation ) );
		}

		return new BoxClass( imports, body, annotations, documentation, properties, getPosition( node ), getSourceText( node ) );
	}

	private BoxProperty toAst( File file, PropertyContext node ) {
		List<BoxAnnotation>					annotations		= new ArrayList<>();
		// This will be empty in components
		List<BoxDocumentationAnnotation>	documentation	= new ArrayList<>();

		for ( var attr : node.attribute() ) {
			annotations.add( toAst( file, attr ) );
		}

		return new BoxProperty( new ArrayList<BoxAnnotation>(), annotations, documentation, getPosition( node ), getSourceText( node ) );
	}

	private List<BoxImport> toAst( File file, List<BoxImportContext> imports ) {
		List<BoxImport> boxImports = new ArrayList<>();
		for ( var boxImport : imports ) {
			boxImports.add( toAst( file, boxImport ) );
		}
		return boxImports;
	}

	private BoxImport toAst( File file, BoxImportContext node ) {
		String				name		= null;
		String				prefix		= null;
		String				module		= null;
		BoxIdentifier		alias		= null;
		List<BoxAnnotation>	annotations	= new ArrayList<>();

		for ( var attr : node.attribute() ) {
			annotations.add( toAst( file, attr ) );
		}
		BoxFQN			nameFQN		= null;
		BoxExpression	nameSearch	= findExprInAnnotations( annotations, "name", false, null, "import", getPosition( node ) );
		if ( nameSearch != null ) {
			name	= getBoxExprAsString( nameSearch, "name", false );
			prefix	= getBoxExprAsString( findExprInAnnotations( annotations, "prefix", false, null, null, null ), "prefix", false );
			if ( prefix != null ) {
				name = prefix + ":" + name;
			}
			nameFQN = new BoxFQN( name, nameSearch.getPosition(), nameSearch.getSourceText() );
		}
		module = getBoxExprAsString( findExprInAnnotations( annotations, "module", false, null, null, null ), "module", false );

		BoxExpression aliasSearch = findExprInAnnotations( annotations, "alias", false, null, null, null );
		if ( aliasSearch != null ) {
			alias = new BoxIdentifier( getBoxExprAsString( aliasSearch, "alias", false ),
			    aliasSearch.getPosition(),
			    aliasSearch.getSourceText() );
		}

		return new BoxImport( nameFQN, alias, getPosition( node ), getSourceText( node ) );
	}

	private List<BoxStatement> toAst( File file, StatementsContext node ) {
		return statementsToAst( file, node );
	}

	private List<BoxStatement> toAst( File file, TopLevelStatementsContext node ) {
		return statementsToAst( file, node );
	}

	private List<BoxStatement> statementsToAst( File file, ParserRuleContext node ) {
		List<BoxStatement> statements = new ArrayList<>();
		if ( node.children != null ) {
			for ( var child : node.children ) {
				if ( child instanceof StatementContext statement ) {
					if ( statement.genericCloseComponent() != null ) {
						String				componentName	= statement.genericCloseComponent().componentName().getText();
						// see if statements list has a BoxComponent with this name
						int					size			= statements.size();
						boolean				foundStart		= false;
						int					removeAfter		= -1;
						List<BoxStatement>	bodyStatements	= new ArrayList<>();
						// loop backwards checking for a BoxComponent with this name
						for ( int i = size - 1; i >= 0; i-- ) {
							BoxStatement boxStatement = statements.get( i );
							if ( boxStatement instanceof BoxComponent boxComponent ) {
								if ( boxComponent.getName().equalsIgnoreCase( componentName ) && boxComponent.getBody() == null ) {
									foundStart = true;
									// slice all statements from this position to the end and set them as the body of the start component
									boxComponent.setBody( new ArrayList<>( statements.subList( i + 1, size ) ) );
									bodyStatements = boxComponent.getBody();
									boxComponent.getPosition().setEnd( getPosition( statement.genericCloseComponent() ).getEnd() );
									boxComponent.setSourceText( getSourceText( boxComponent.getSourceStartIndex(), statement.genericCloseComponent() ) );
									removeAfter = i;
									break;
								} else if ( boxComponent.getBody() == null && boxComponent.getRequiresBody() ) {
									issues.add( new Issue( "Component [" + boxComponent.getName() + "] requires a body.", boxComponent.getPosition() ) );
								}
							}
						}
						// remove all items in list after removeAfter index
						if ( removeAfter >= 0 ) {
							statements.subList( removeAfter + 1, size ).clear();
						}
						if ( !foundStart ) {
							issues.add( new Issue( "Found end component [" + componentName + "] without matching start component",
							    getPosition( statement.genericCloseComponent() ) ) );
						}

						ComponentDescriptor descriptor = componentService.getComponent( componentName );
						if ( descriptor != null ) {
							if ( !descriptor.allowsBody() && ( !allStatementsAreWhitespace( bodyStatements ) ) ) {
								issues.add( new Issue( "The [" + componentName + "] component does not allow a body", getPosition( node ) ) );
							}
						}
					} else {
						statements.add( toAst( file, statement ) );
					}
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
				} else if ( child instanceof BoxImportContext importContext ) {
					statements.add( toAst( file, importContext ) );
				}
			}
		}
		// Loop over statements and look for any BoxComponets who require a body but it's null
		for ( BoxStatement statement : statements ) {
			if ( statement instanceof BoxComponent boxComponent ) {
				if ( boxComponent.getBody() == null && boxComponent.getRequiresBody() ) {
					issues.add( new Issue( "Component [" + boxComponent.getName() + "] requires a body.", boxComponent.getPosition() ) );
				}
			}
		}
		return statements;
	}

	private boolean allStatementsAreWhitespace( List<BoxStatement> bodyStatements ) {
		for ( BoxStatement statement : bodyStatements ) {
			if ( statement instanceof BoxBufferOutput bffr ) {
				if ( bffr.getExpression() instanceof BoxStringLiteral str && !str.getValue().isBlank() ) {
					return false;
				}
			} else {
				return false;
			}
		}
		return true;
	}

	private BoxStatement toAst( File file, StatementContext node ) {
		if ( node.output() != null ) {
			return toAst( file, node.output() );
		} else if ( node.set() != null ) {
			return toAst( file, node.set() );
		} else if ( node.if_() != null ) {
			return toAst( file, node.if_() );
		} else if ( node.try_() != null ) {
			return toAst( file, node.try_() );
		} else if ( node.function() != null ) {
			return toAst( file, node.function() );
		} else if ( node.return_() != null ) {
			return toAst( file, node.return_() );
		} else if ( node.while_() != null ) {
			return toAst( file, node.while_() );
		} else if ( node.break_() != null ) {
			return toAst( file, node.break_() );
		} else if ( node.continue_() != null ) {
			return toAst( file, node.continue_() );
		} else if ( node.include() != null ) {
			return toAst( file, node.include() );
		} else if ( node.rethrow() != null ) {
			return toAst( file, node.rethrow() );
		} else if ( node.throw_() != null ) {
			return toAst( file, node.throw_() );
		} else if ( node.switch_() != null ) {
			return toAst( file, node.switch_() );
		} else if ( node.genericOpenCloseComponent() != null ) {
			return toAst( file, node.genericOpenCloseComponent() );
		} else if ( node.genericOpenComponent() != null ) {
			return toAst( file, node.genericOpenComponent() );
		}
		throw new BoxRuntimeException( "Statement node parsing not implemented yet. File: " + file.toString() + "text: [" + node.getText() + "]" );

	}

	private BoxStatement toAst( File file, GenericOpenCloseComponentContext node ) {
		List<BoxAnnotation> attributes = new ArrayList<>();
		for ( var attr : node.attribute() ) {
			attributes.add( toAst( file, attr ) );
		}
		return new BoxComponent( node.componentName().getText(), attributes, List.of(), node.getStart().getStartIndex(), getPosition( node ),
		    getSourceText( node ) );
	}

	private BoxStatement toAst( File file, GenericOpenComponentContext node ) {
		List<BoxAnnotation> attributes = new ArrayList<>();
		for ( var attr : node.attribute() ) {
			attributes.add( toAst( file, attr ) );
		}
		String name = node.componentName().getText();

		// Special check for cfloop condition to avoid runtime eval
		if ( name.equalsIgnoreCase( "loop" ) ) {
			for ( var attr : attributes ) {
				if ( attr.getKey().getValue().equalsIgnoreCase( "condition" ) ) {
					BoxExpression condition = attr.getValue();
					if ( condition instanceof BoxStringLiteral str ) {
						// parse as CF script expression and update value
						condition = parseCFExpression( str.getValue(), condition.getPosition() );
					}
					BoxExpression newCondition = new BoxClosure(
					    List.of(),
					    List.of(),
					    List.of(
					        new BoxReturn( condition, null, null )
					    ),
					    null,
					    null );
					attr.setValue( newCondition );
				}
			}
		}

		// Body may get set later, if we find an end component
		var					comp		= new BoxComponent( name, attributes, null, node.getStart().getStartIndex(), getPosition( node ),
		    getSourceText( node ) );

		ComponentDescriptor	descriptor	= componentService.getComponent( name );
		if ( descriptor != null && descriptor.requiresBody() ) {
			comp.setRequiresBody( true );
		}

		return comp;

	}

	private BoxStatement toAst( File file, SwitchContext node ) {
		BoxExpression		expression;
		List<BoxAnnotation>	annotations	= new ArrayList<>();
		List<BoxSwitchCase>	cases		= new ArrayList<>();

		for ( var attr : node.attribute() ) {
			annotations.add( toAst( file, attr ) );
		}

		expression = findExprInAnnotations( annotations, "expression", true, null, "switch", getPosition( node ) );

		if ( node.switchBody() != null && node.switchBody().children != null ) {
			for ( var c : node.switchBody().children ) {
				if ( c instanceof CFTemplateGrammar.CaseContext caseNode ) {
					cases.add( toAst( file, caseNode ) );
					// We're willing to overlook text, but not other CF components
				} else if ( ! ( c instanceof CFTemplateGrammar.TextContentContext ) ) {
					issues.add( new Issue( "Switch body can only contain case statements - ", getPosition( ( ParserRuleContext ) c ) ) );
				}
			}
		}
		return new BoxSwitch( expression, cases, getPosition( node ), getSourceText( node ) );
	}

	private BoxSwitchCase toAst( File file, CaseContext node ) {
		BoxExpression	value		= null;
		BoxExpression	delimiter	= null;

		// Only check for these on case nodes, not default case
		if ( !node.CASE().isEmpty() ) {
			List<BoxAnnotation> annotations = new ArrayList<>();

			for ( var attr : node.attribute() ) {
				annotations.add( toAst( file, attr ) );
			}

			value		= findExprInAnnotations( annotations, "value", true, null, "case", getPosition( node ) );
			delimiter	= findExprInAnnotations( annotations, "delimiter", false, new BoxStringLiteral( ",", null, null ), "case", getPosition( node ) );
		}

		List<BoxStatement> statements = new ArrayList<>();
		if ( node.statements() != null ) {
			statements.addAll( toAst( file, node.statements() ) );
		}

		// In component mode, the break is implied
		statements.add( new BoxBreak( null, null ) );

		return new BoxSwitchCase( value, delimiter, statements, getPosition( node ), getSourceText( node ) );
	}

	private BoxStatement toAst( File file, ThrowContext node ) {
		BoxExpression		object			= null;
		BoxExpression		type			= null;
		BoxExpression		message			= null;
		BoxExpression		detail			= null;
		BoxExpression		errorcode		= null;
		BoxExpression		extendedinfo	= null;

		List<BoxAnnotation>	annotations		= new ArrayList<>();

		for ( var attr : node.attribute() ) {
			annotations.add( toAst( file, attr ) );
		}

		for ( var annotation : annotations ) {
			if ( annotation.getKey().getValue().equalsIgnoreCase( "object" ) ) {
				object = annotation.getValue();
			}
			if ( annotation.getKey().getValue().equalsIgnoreCase( "type" ) ) {
				type = annotation.getValue();
			}
			if ( annotation.getKey().getValue().equalsIgnoreCase( "message" ) ) {
				message = annotation.getValue();
			}
			if ( annotation.getKey().getValue().equalsIgnoreCase( "detail" ) ) {
				detail = annotation.getValue();
			}
			if ( annotation.getKey().getValue().equalsIgnoreCase( "errorcode" ) ) {
				errorcode = annotation.getValue();
			}
			if ( annotation.getKey().getValue().equalsIgnoreCase( "extendedinfo" ) ) {
				extendedinfo = annotation.getValue();
			}
		}

		return new BoxThrow( object, type, message, detail, errorcode, extendedinfo, getPosition( node ), getSourceText( node ) );
	}

	private BoxStatement toAst( File file, RethrowContext node ) {
		return new BoxRethrow( getPosition( node ), getSourceText( node ) );
	}

	private BoxStatement toAst( File file, IncludeContext node ) {
		List<BoxAnnotation> annotations = new ArrayList<>();

		for ( var attr : node.attribute() ) {
			annotations.add( toAst( file, attr ) );
		}

		return new BoxComponent(
		    "include",
		    annotations,
		    getPosition( node ),
		    getSourceText( node )
		);
	}

	private BoxStatement toAst( File file, ContinueContext node ) {
		return new BoxContinue( getPosition( node ), getSourceText( node ) );
	}

	private BoxStatement toAst( File file, BreakContext node ) {
		return new BoxBreak( getPosition( node ), getSourceText( node ) );
	}

	private BoxStatement toAst( File file, WhileContext node ) {
		BoxExpression		condition;
		List<BoxStatement>	body		= new ArrayList<>();
		List<BoxAnnotation>	annotations	= new ArrayList<>();

		for ( var attr : node.attribute() ) {
			annotations.add( toAst( file, attr ) );
		}

		BoxExpression conditionSearch = findExprInAnnotations( annotations, "condition", true, null, "while", getPosition( node ) );
		condition = parseCFExpression(
		    getBoxExprAsString(
		        conditionSearch,
		        "condition",
		        false
		    ),
		    conditionSearch.getPosition()
		);

		if ( node.statements() != null ) {
			body.addAll( toAst( file, node.statements() ) );
		}

		return new BoxWhile( condition, body, getPosition( node ), getSourceText( node ) );
	}

	private BoxStatement toAst( File file, ReturnContext node ) {
		BoxExpression expr;
		if ( node.expression() != null ) {
			expr = parseCFExpression( node.expression().getText(), getPosition( node.expression() ) );
		} else {
			expr = new BoxNull( null, null );
		}
		return new BoxReturn( expr, getPosition( node ), getSourceText( node ) );
	}

	private BoxFunctionDeclaration toAst( File file, FunctionContext node ) {
		BoxReturnType						returnType		= null;
		String								name			= null;
		List<BoxStatement>					body			= new ArrayList<>();
		List<BoxArgumentDeclaration>		args			= new ArrayList<>();
		List<BoxAnnotation>					annotations		= new ArrayList<>();
		List<BoxDocumentationAnnotation>	documentation	= new ArrayList<>();
		BoxAccessModifier					accessModifier	= null;
		List<BoxMethodDeclarationModifier>	modifiers		= new ArrayList<>();

		for ( var attr : node.attribute() ) {
			annotations.add( toAst( file, attr ) );
		}

		name = getBoxExprAsString( findExprInAnnotations( annotations, "name", true, null, "function", getPosition( node ) ), "name", false );

		String accessText = getBoxExprAsString( findExprInAnnotations( annotations, "function", false, null, null, null ), "access", true );
		if ( accessText != null ) {
			accessText = accessText.toLowerCase();
			if ( accessText.equals( "public" ) ) {
				accessModifier = BoxAccessModifier.Public;
			} else if ( accessText.equals( "private" ) ) {
				accessModifier = BoxAccessModifier.Private;
			} else if ( accessText.equals( "remote" ) ) {
				accessModifier = BoxAccessModifier.Remote;
			} else if ( accessText.equals( "package" ) ) {
				accessModifier = BoxAccessModifier.Package;
			}
		}

		BoxExpression	returnTypeSearch	= findExprInAnnotations( annotations, "returnType", false, null, null, null );
		String			returnTypeText		= getBoxExprAsString( returnTypeSearch, "returnType", true );
		if ( returnTypeText != null ) {
			BoxType	boxType	= BoxType.fromString( returnTypeText );
			String	fqn		= boxType.equals( BoxType.Fqn ) ? returnTypeText : null;
			returnType = new BoxReturnType( boxType, fqn, returnTypeSearch.getPosition(), returnTypeSearch.getSourceText() );
		}

		for ( var arg : node.argument() ) {
			args.add( toAst( file, arg ) );
		}

		body.addAll( toAst( file, node.body ) );

		return new BoxFunctionDeclaration( accessModifier, modifiers, name, returnType, args, annotations, documentation, body, getPosition( node ),
		    getSourceText( node ) );
	}

	private BoxArgumentDeclaration toAst( File file, ArgumentContext node ) {
		Boolean								required		= false;
		String								type			= "Any";
		String								name			= "undefined";
		BoxExpression						expr			= null;
		List<BoxAnnotation>					annotations		= new ArrayList<>();
		List<BoxDocumentationAnnotation>	documentation	= new ArrayList<>();

		for ( var attr : node.attribute() ) {
			annotations.add( toAst( file, attr ) );
		}

		name		= getBoxExprAsString( findExprInAnnotations( annotations, "name", true, null, "function", getPosition( node ) ), "name", false );

		required	= BooleanCaster.cast(
		    getBoxExprAsString(
		        findExprInAnnotations( annotations, "required", false, null, null, null ),
		        "required",
		        false
		    )
		);

		expr		= findExprInAnnotations( annotations, "default", false, null, null, null );
		type		= getBoxExprAsString( findExprInAnnotations( annotations, "type", false, new BoxStringLiteral( "Any", null, null ), null, null ), "type",
		    false );

		return new BoxArgumentDeclaration( required, type, name, expr, annotations, documentation, getPosition( node ), getSourceText( node ) );
	}

	private BoxAnnotation toAst( File file, AttributeContext attribute ) {
		BoxFQN			name	= new BoxFQN( attribute.attributeName().getText(), getPosition( attribute.attributeName() ),
		    getSourceText( attribute.attributeName() ) );
		BoxExpression	value;
		if ( attribute.attributeValue() != null ) {
			value = toAst( file, attribute.attributeValue() );
		} else {
			value = new BoxStringLiteral( "", null, null );
		}
		return new BoxAnnotation( name, value, getPosition( attribute ), getSourceText( attribute ) );
	}

	private BoxExpression toAst( File file, AttributeValueContext node ) {
		if ( node.identifier() != null ) {
			return new BoxStringLiteral( node.identifier().getText(), getPosition( node ),
			    getSourceText( node ) );
		}
		if ( node.interpolatedExpression() != null ) {
			return toAst( file, node.interpolatedExpression() );
		} else {
			return toAst( file, node.quotedString() );
		}
	}

	private BoxStatement toAst( File file, TryContext node ) {
		List<BoxStatement> tryBody = new ArrayList<>();
		for ( var statements : node.statements() ) {
			tryBody.addAll( toAst( file, statements ) );
		}
		List<BoxTryCatch>	catches		= node.catchBlock().stream().map( it -> toAst( file, it ) ).toList();
		List<BoxStatement>	finallyBody	= new ArrayList<>();
		if ( node.finallyBlock() != null ) {
			finallyBody.addAll( toAst( file, node.finallyBlock().statements() ) );
		}
		return new BoxTry( tryBody, catches, finallyBody, getPosition( node ), getSourceText( node ) );
	}

	private BoxTryCatch toAst( File file, CatchBlockContext node ) {
		BoxExpression		exception	= new BoxIdentifier( "cfcatch", null, null );
		List<BoxExpression>	catchTypes;
		List<BoxStatement>	catchBody	= new ArrayList<>();

		if ( node.attribute() != null ) {
			var typeSearch = node.attribute().stream()
			    .filter( ( it ) -> it.attributeName().COMPONENT_NAME().getText().equalsIgnoreCase( "type" ) && it.attributeValue() != null ).findFirst();
			if ( typeSearch.isPresent() ) {
				BoxExpression type;
				if ( typeSearch.get().attributeValue().identifier() != null ) {
					type = new BoxStringLiteral( typeSearch.get().attributeValue().identifier().getText(), getPosition( typeSearch.get().attributeValue() ),
					    getSourceText( typeSearch.get().attributeValue() ) );
				} else {
					type = toAst( file, typeSearch.get().attributeValue().quotedString() );
				}
				catchTypes = List.of( type );
			} else {
				catchTypes = List.of( new BoxFQN( "any", null, null ) );
			}
		} else {
			catchTypes = List.of( new BoxFQN( "any", null, null ) );
		}
		if ( node.statements() != null ) {
			catchBody = toAst( file, node.statements() );
		}
		return new BoxTryCatch( catchTypes, exception, catchBody, getPosition( node ), getSourceText( node ) );
	}

	private BoxExpression toAst( File file, CFTemplateGrammar.QuotedStringContext node ) {
		String quoteChar = node.getText().substring( 0, 1 );
		if ( node.interpolatedExpression().isEmpty() ) {
			String s = node.getText();
			// trim leading and trailing quote
			s = s.substring( 1, s.length() - 1 );
			return new BoxStringLiteral(
			    escapeStringLiteral( quoteChar, s ),
			    getPosition( node ),
			    getSourceText( node )
			);

		} else {
			List<BoxExpression> parts = new ArrayList<>();
			node.children.forEach( it -> {
				if ( it != null && it instanceof CFTemplateGrammar.QuotedStringPartContext str ) {
					parts.add( new BoxStringLiteral( escapeStringLiteral( quoteChar, getSourceText( str ) ),
					    getPosition( str ),
					    getSourceText( str ) ) );
				}
				if ( it != null && it instanceof CFTemplateGrammar.InterpolatedExpressionContext interp ) {
					parts.add( toAst( file, interp ) );
				}
			} );
			return new BoxStringInterpolation( parts, getPosition( node ), getSourceText( node ) );
		}
	}

	private BoxExpression toAst( File file, InterpolatedExpressionContext interp ) {
		return parseCFExpression( interp.expression().getText(), getPosition( interp.expression() ) );
	}

	/**
	 * Escape double up quotes and pounds in a string literal
	 * 
	 * @param quoteChar the quote character used to surround the string
	 * @param string    the string to escape
	 * 
	 * @return the escaped string
	 */
	private String escapeStringLiteral( String quoteChar, String string ) {
		String escaped = string.replace( "##", "#" );
		return escaped.replace( quoteChar + quoteChar, quoteChar );
	}

	private BoxIfElse toAst( File file, CFTemplateGrammar.IfContext node ) {
		// if condition will always exist
		BoxExpression		condition	= parseCFExpression( node.ifCondition.getText(), getPosition( node.ifCondition ) );
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
			Point	end	= new Point( node.elseIfComponentClose.get( i ).getLine(),
			    node.elseIfComponentClose.get( i ).getCharPositionInLine() );
			stopIndex = node.elseIfComponentClose.get( i ).getStopIndex();
			if ( node.elseThenBody.get( i ).statement().size() > 0 ) {
				end			= new Point( node.elseThenBody.get( i ).statement( node.elseThenBody.get( i ).statement().size() - 1 ).getStop().getLine(),
				    node.elseThenBody.get( i ).statement( node.elseThenBody.get( i ).statement().size() - 1 ).getStop().getCharPositionInLine() );
				stopIndex	= node.elseThenBody.get( i ).statement( node.elseThenBody.get( i ).statement().size() - 1 ).getStop().getStopIndex();
			}
			Position		pos				= new Position(
			    new Point( node.ELSEIF( i ).getSymbol().getLine(), node.ELSEIF( i ).getSymbol().getCharPositionInLine() - 3 ),
			    end, sourceToParse );
			BoxExpression	thisCondition	= parseCFExpression( node.elseIfCondition.get( i ).getText(), getPosition( node.elseIfCondition.get( i ) ) );
			elseBody = List.of( new BoxIfElse( thisCondition, toAst( file, node.elseThenBody.get( i ) ), elseBody, pos,
			    getSourceText( node, node.ELSEIF().get( i ).getSymbol().getStartIndex() - 3, stopIndex ) ) );
		}

		// If there were no elseif's, the elsebody here will be the <cfelse>. Otherwise, it will be the last elseif.
		return new BoxIfElse( condition, thenBody, elseBody, getPosition( node ), getSourceText( node ) );
	}

	private BoxStatement toAst( File file, SetContext set ) {
		// In components, a <bx:set ...> component is an Expression Statement.
		return new BoxExpressionStatement( parseCFExpression( set.expression().getText(), getPosition( set.expression() ) ), getPosition( set ),
		    getSourceText( set ) );
	}

	private BoxStatement toAst( File file, OutputContext node ) {
		List<BoxStatement>	statements	= new ArrayList<>();
		List<BoxAnnotation>	annotations	= new ArrayList<>();

		for ( var attr : node.attribute() ) {
			annotations.add( toAst( file, attr ) );
		}
		if ( node.statements() != null ) {
			outputCounter++;
			statements.addAll( toAst( file, node.statements() ) );
			outputCounter--;
		}

		return new BoxComponent( "output", annotations, statements, getPosition( node ), getSourceText( node ) );
	}

	/**
	 * A helper function to find a specific annotation by name and return the value expression
	 * 
	 * @param annotations             the list of annotations to search
	 * @param name                    the name of the annotation to find
	 * @param required                whether the annotation is required. If required, and not present a parsing Issue is created.
	 * @param defaultValue            the default value to return if the annotation is not found. Ignored if requried is false.
	 * @param containingComponentName the name of the component that contains the annotation, used in error handling
	 * @param position                the position of the component, used in error handling
	 * 
	 * @return the value expression of the annotation, or the default value if the annotation is not found
	 * 
	 */
	private BoxExpression findExprInAnnotations( List<BoxAnnotation> annotations, String name, boolean required, BoxExpression defaultValue,
	    String containingComponentName,
	    Position position ) {
		var search = annotations.stream().filter( ( it ) -> it.getKey().getValue().equalsIgnoreCase( name ) ).findFirst();
		if ( search.isPresent() ) {
			return search.get().getValue();
		} else if ( !required ) {
			return defaultValue;
		} else {
			issues.add( new Issue( "Missing " + name + " attribute on " + containingComponentName + " component", position ) );
			return new BoxNull( null, null );
		}

	}

	/**
	 * A helper function to take a BoxExpr and return the value expression as a string.
	 * If the expression is not a string literal, an Issue is created.
	 * 
	 * @param expr       the expression to get the value from
	 * @param name       the name of the attribute, used in error handling
	 * @param allowEmpty whether an empty string is allowed. If not allowed, an Issue is created.
	 * 
	 * @return the value of the expression as a string, or null if the expression is null
	 */
	private String getBoxExprAsString( BoxExpression expr, String name, boolean allowEmpty ) {
		if ( expr == null ) {
			return null;
		}
		if ( expr instanceof BoxStringLiteral str ) {
			if ( !allowEmpty && str.getValue().trim().isEmpty() ) {
				issues.add( new Issue( "Attribute [" + name + "] cannot be empty", expr.getPosition() ) );
			}
			return str.getValue();
		} else {
			issues.add( new Issue( "Attribute [" + name + "] attribute must be a string literal", expr.getPosition() ) );
			return "";
		}
	}

	private BoxStatement toAst( File file, TextContentContext node ) {
		BoxExpression expression = null;
		// No interpolated expressions, only string
		if ( node.interpolatedExpression().isEmpty() ) {
			expression = new BoxStringLiteral( escapeStringLiteral( node.getText() ), getPosition( node ), getSourceText( node ) );
		} else {
			List<BoxExpression> expressions = new ArrayList<>();
			for ( var child : node.children ) {
				if ( child instanceof CFTemplateGrammar.InterpolatedExpressionContext intrpexpr && intrpexpr.expression() != null ) {
					// parse the text between the hash signs as a CF expression
					expressions.add( toAst( file, intrpexpr ) );
				} else if ( child instanceof CFTemplateGrammar.NonInterpolatedTextContext strlit ) {
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

	public BoxExpression parseCFExpression( String code, Position position ) {
		try {
			ParsingResult result = new CFScriptParser( position.getStart().getLine(), position.getStart().getColumn() ).setSource( sourceToParse )
			    .parseExpression( code );
			if ( result.getIssues().isEmpty() ) {
				return ( BoxExpression ) result.getRoot();
			} else {
				// Add these issues to the main parser
				issues.addAll( result.getIssues() );
				return new BoxNull( null, null );
			}
		} catch ( IOException e ) {
			issues.add( new Issue( "Error parsing interpolated expression " + e.getMessage(), position ) );
			return new BoxNull( null, null );
		}
	}

	public BoxNode parseCFClassOrInterface( String code, Position position ) {
		try {
			ParsingResult result = new CFScriptParser( position.getStart().getLine(), position.getStart().getColumn(), ( outputCounter > 0 ) )
			    .setSource( sourceToParse ).parse( code,
			        true );
			if ( result.getIssues().isEmpty() ) {
				BoxNode root = result.getRoot();
				if ( root instanceof BoxClass bc ) {
					return bc;
				} else if ( root instanceof BoxInterface bi ) {
					return bi;
				} else {
					issues.add( new Issue( "Expected a class or interface, but found  [" + root.getClass().getName() + "] in script island.", position ) );
					return new BoxNull( null, null );
				}
			} else {
				// Add these issues to the main parser
				issues.addAll( result.getIssues() );
				return new BoxNull( null, null );
			}
		} catch ( IOException e ) {
			issues.add( new Issue( "Error parsing script island " + e.getMessage(), position ) );
			return new BoxNull( null, null );
		}
	}

	public List<BoxStatement> parseCFStatements( String code, Position position ) {
		try {
			ParsingResult result = new CFScriptParser( position.getStart().getLine(), position.getStart().getColumn(), ( outputCounter > 0 ) )
			    .setSource( sourceToParse ).parse( code );
			if ( result.getIssues().isEmpty() ) {
				BoxNode root = result.getRoot();
				if ( root instanceof BoxScript script ) {
					return script.getStatements();
				} else if ( root instanceof BoxStatement statement ) {
					return List.of( statement );
				} else {
					// Could be a BoxClass, which we may actually need to support if there is a .cfc file with a top-level <cfscript> node containing a
					// component.
					issues.add( new Issue( "Unexpected root node type [" + root.getClass().getName() + "] in script island.", position ) );
					return List.of();
				}
			} else {
				// Add these issues to the main parser
				issues.addAll( result.getIssues() );
				return List.of( new BoxExpressionStatement( new BoxNull( null, null ), null, null ) );
			}
		} catch ( IOException e ) {
			issues.add( new Issue( "Error parsing interpolated expression " + e.getMessage(), position ) );
			return List.of();
		}
	}

	@Override
	CFTemplateParser setSource( Source source ) {
		if ( this.sourceToParse != null ) {
			return this;
		}
		this.sourceToParse = source;
		return this;
	}
}
