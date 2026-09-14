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
package ortus.boxlang.compiler.toolchain;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import ortus.boxlang.compiler.ast.BoxClass;
import ortus.boxlang.compiler.ast.BoxExpression;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.BoxStatement;
import ortus.boxlang.compiler.ast.Position;
import ortus.boxlang.compiler.ast.expression.BoxAssignment;
import ortus.boxlang.compiler.ast.expression.BoxAssignmentOperator;
import ortus.boxlang.compiler.ast.expression.BoxBooleanLiteral;
import ortus.boxlang.compiler.ast.expression.BoxIdentifier;
import ortus.boxlang.compiler.ast.expression.BoxNull;
import ortus.boxlang.compiler.ast.statement.BoxAccessModifier;
import ortus.boxlang.compiler.ast.statement.BoxArgumentDeclaration;
import ortus.boxlang.compiler.ast.statement.BoxBreak;
import ortus.boxlang.compiler.ast.statement.BoxContinue;
import ortus.boxlang.compiler.ast.statement.BoxDo;
import ortus.boxlang.compiler.ast.statement.BoxExpressionStatement;
import ortus.boxlang.compiler.ast.statement.BoxForIn;
import ortus.boxlang.compiler.ast.statement.BoxForIndex;
import ortus.boxlang.compiler.ast.statement.BoxFunctionDeclaration;
import ortus.boxlang.compiler.ast.statement.BoxIfElse;
import ortus.boxlang.compiler.ast.statement.BoxImport;
import ortus.boxlang.compiler.ast.statement.BoxMethodDeclarationModifier;
import ortus.boxlang.compiler.ast.statement.BoxReturn;
import ortus.boxlang.compiler.ast.statement.BoxStatementBlock;
import ortus.boxlang.compiler.ast.statement.BoxThrow;
import ortus.boxlang.compiler.ast.statement.BoxTry;
import ortus.boxlang.compiler.ast.statement.BoxTryCatch;
import ortus.boxlang.compiler.ast.statement.BoxWhile;
import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.compiler.parser.GroovyParser;
import ortus.boxlang.parser.antlr.GroovyGrammar.BlockContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.BlockStatementContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.BlockStatementsContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.BreakStatementContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.CatchClauseContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.ClassDeclarationContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.ClassModifierContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.ClassicForControlContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.ConstructorDeclarationContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.ContinueStatementContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.DoWhileStatementContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.ExprStatementContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.FieldDeclarationContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.ForInControlContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.ForStatementContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.IfStatementContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.MethodDeclarationContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.ParameterContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.ReturnStatementContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.ThrowStatementContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.TryStatementContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.VarDeclStatementContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.WhileStatementContext;
import ortus.boxlang.parser.antlr.GroovyGrammarBaseVisitor;

/**
 * Walks GroovyGrammar's statement/declaration parse tree and builds the shared BoxLang AST
 * ({@code ortus.boxlang.compiler.ast.statement}), following the exact pattern {@code CFVisitor}
 * uses for CFML. Phase 2 of the Groovy parser/transpiler effort - see GroovyLexer.g4/
 * GroovyGrammar.g4 for the covered syntax subset.
 */
public class GroovyVisitor extends GroovyGrammarBaseVisitor<BoxNode> {

	private final GroovyParser				tools;
	private final GroovyExpressionVisitor	expressionVisitor;

	public GroovyVisitor( GroovyParser tools ) {
		this.tools				= tools;
		this.expressionVisitor	= new GroovyExpressionVisitor( tools, this );
	}

	public GroovyExpressionVisitor getExpressionVisitor() {
		return expressionVisitor;
	}

	// -----------------------------------------------------------------------------------------
	// Class / member declarations - called directly by GroovyParser, not via generic dispatch,
	// since imports (gathered at compilationUnit level) must be threaded in from outside.

	public BoxClass buildClass( ClassDeclarationContext ctx, List<BoxImport> imports ) {
		var					pos		= tools.getPosition( ctx );
		var					src		= tools.getSourceText( ctx );

		List<BoxStatement>	body	= ctx.classBody() == null
		    ? new ArrayList<>()
		    : ctx.classBody().classMember().stream().map( m -> ( BoxStatement ) m.accept( this ) ).collect( Collectors.toList() );

		return new BoxClass( imports, body, List.of(), List.of(), List.of(), pos, src, BoxSourceType.GROOVYSCRIPT );
	}

	@Override
	public BoxNode visitFieldDeclaration( FieldDeclarationContext ctx ) {
		var				pos			= tools.getPosition( ctx );
		var				src			= tools.getSourceText( ctx );
		BoxIdentifier	target		= new BoxIdentifier( ctx.IDENTIFIER().getText(), tools.getPosition( ctx.IDENTIFIER().getSymbol() ),
		    ctx.IDENTIFIER().getText() );
		BoxExpression	value		= ctx.expression() != null ? ctx.expression().accept( expressionVisitor ) : new BoxNull( pos, src );
		// Simplification: a Groovy field becomes a plain assignment executed in the class's
		// pseudo-constructor, establishing the member dynamically. Real BoxLang `property`
		// semantics (getters/setters, accessor codegen) are not modeled here - deferred to a
		// later phase.
		BoxAssignment	assignment	= new BoxAssignment( target, BoxAssignmentOperator.Equal, value, List.of(), pos, src );
		return new BoxExpressionStatement( assignment, pos, src );
	}

	@Override
	public BoxNode visitMethodDeclaration( MethodDeclarationContext ctx ) {
		var									pos			= tools.getPosition( ctx );
		var									src			= tools.getSourceText( ctx );
		BoxAccessModifier					visibility	= null;
		List<BoxMethodDeclarationModifier>	modifiers	= new ArrayList<>();
		for ( ClassModifierContext modCtx : ctx.classModifier() ) {
			applyModifier( modCtx, modifiers, visibility == null ? null : visibility );
			BoxAccessModifier maybeVisibility = toAccessModifier( modCtx );
			if ( maybeVisibility != null ) {
				visibility = maybeVisibility;
			}
		}

		List<BoxArgumentDeclaration>	args	= ctx.parameterList() == null
		    ? new ArrayList<>()
		    : ctx.parameterList().parameter().stream().map( this::buildParameterDeclaration ).collect( Collectors.toList() );

		List<BoxStatement>				body	= ctx.block() == null ? null : applyImplicitReturn( buildStatementList( ctx.block().blockStatements() ) );

		return new BoxFunctionDeclaration( visibility, modifiers, ctx.IDENTIFIER().getText(), null, args, List.of(), List.of(), body, pos, src );
	}

	@Override
	public BoxNode visitConstructorDeclaration( ConstructorDeclarationContext ctx ) {
		var									pos			= tools.getPosition( ctx );
		var									src			= tools.getSourceText( ctx );
		BoxAccessModifier					visibility	= null;
		List<BoxMethodDeclarationModifier>	modifiers	= new ArrayList<>();
		for ( ClassModifierContext modCtx : ctx.classModifier() ) {
			applyModifier( modCtx, modifiers, visibility == null ? null : visibility );
			BoxAccessModifier maybeVisibility = toAccessModifier( modCtx );
			if ( maybeVisibility != null ) {
				visibility = maybeVisibility;
			}
		}
		List<BoxArgumentDeclaration>	args	= ctx.parameterList() == null
		    ? new ArrayList<>()
		    : ctx.parameterList().parameter().stream().map( this::buildParameterDeclaration ).collect( Collectors.toList() );
		List<BoxStatement>				body	= applyImplicitReturn( buildStatementList( ctx.block().blockStatements() ) );

		// BoxLang convention (mirroring CFML): the constructor is the method named "init".
		return new BoxFunctionDeclaration( visibility, modifiers, "init", null, args, List.of(), List.of(), body, pos, src );
	}

	BoxArgumentDeclaration buildParameterDeclaration( ParameterContext ctx ) {
		var				pos				= tools.getPosition( ctx );
		var				src				= tools.getSourceText( ctx );
		String			type			= ctx.typeName() != null ? ctx.typeName().getText() : "Any";
		BoxExpression	defaultValue	= ctx.expression() != null ? ctx.expression().accept( expressionVisitor ) : null;
		boolean			required		= defaultValue == null;
		return new BoxArgumentDeclaration( required, type, ctx.IDENTIFIER().getText(), defaultValue, List.of(), List.of(), pos, src );
	}

	private void applyModifier( ClassModifierContext modCtx, List<BoxMethodDeclarationModifier> modifiers, BoxAccessModifier currentVisibility ) {
		if ( modCtx.STATIC() != null ) {
			modifiers.add( BoxMethodDeclarationModifier.STATIC );
		} else if ( modCtx.FINAL() != null ) {
			modifiers.add( BoxMethodDeclarationModifier.FINAL );
		} else if ( modCtx.ABSTRACT() != null ) {
			modifiers.add( BoxMethodDeclarationModifier.ABSTRACT );
		}
	}

	private BoxAccessModifier toAccessModifier( ClassModifierContext modCtx ) {
		if ( modCtx.PUBLIC() != null ) {
			return BoxAccessModifier.Public;
		}
		if ( modCtx.PRIVATE() != null ) {
			return BoxAccessModifier.Private;
		}
		if ( modCtx.PROTECTED() != null ) {
			// BoxAccessModifier has no "protected" tier; Private is the closer, safer
			// approximation (more restrictive rather than less).
			return BoxAccessModifier.Private;
		}
		return null;
	}

	// -----------------------------------------------------------------------------------------
	// Blocks / statement lists

	List<BoxStatement> buildStatementList( BlockStatementsContext ctx ) {
		if ( ctx == null ) {
			return new ArrayList<>();
		}
		return ctx.statement().stream().map( s -> ( BoxStatement ) s.accept( this ) ).collect( Collectors.toList() );
	}

	BoxStatement buildStatementBlockFromBlockStatements( BlockStatementsContext ctx, Position pos, String src ) {
		return new BoxStatementBlock( applyImplicitReturn( buildStatementList( ctx ) ), pos, src );
	}

	/**
	 * Groovy closures and methods implicitly return the value of their last statement when it
	 * isn't already a {@code return}. This applies that rule at the top level of a body only
	 * (not recursively into if/else branches, loops, etc.) - a deliberately bounded Phase 2
	 * approximation of Groovy's real (more sophisticated) implicit-return semantics.
	 */
	private List<BoxStatement> applyImplicitReturn( List<BoxStatement> statements ) {
		if ( statements.isEmpty() ) {
			return statements;
		}
		int last = statements.size() - 1;
		if ( statements.get( last ) instanceof BoxExpressionStatement exprStmt ) {
			statements.set( last, new BoxReturn( exprStmt.getExpression(), exprStmt.getPosition(), exprStmt.getSourceText() ) );
		}
		return statements;
	}

	private BoxStatement buildBlock( BlockContext ctx ) {
		return new BoxStatementBlock( buildStatementList( ctx.blockStatements() ), tools.getPosition( ctx ), tools.getSourceText( ctx ) );
	}

	@Override
	public BoxNode visitBlockStatement( BlockStatementContext ctx ) {
		return buildBlock( ctx.block() );
	}

	// -----------------------------------------------------------------------------------------
	// Statements

	@Override
	public BoxNode visitVarDeclStatement( VarDeclStatementContext ctx ) {
		var					pos			= tools.getPosition( ctx );
		var					src			= tools.getSourceText( ctx );
		List<BoxStatement>	declarators	= new ArrayList<>();

		// First declarator carries the (optional) type/def prefix, the rest are bare
		// "IDENTIFIER (= expr)?" per Groovy's comma-separated declaration syntax.
		BoxIdentifier		firstTarget	= new BoxIdentifier( ctx.IDENTIFIER( 0 ).getText(), tools.getPosition( ctx.IDENTIFIER( 0 ).getSymbol() ),
		    ctx.IDENTIFIER( 0 ).getText() );
		BoxExpression		firstValue	= ctx.expression().isEmpty() ? new BoxNull( pos, src ) : ctx.expression( 0 ).accept( expressionVisitor );
		declarators.add( new BoxExpressionStatement( new BoxAssignment( firstTarget, BoxAssignmentOperator.Equal, firstValue, List.of(), pos, src ), pos,
		    src ) );

		for ( int i = 1; i < ctx.IDENTIFIER().size(); i++ ) {
			BoxIdentifier	target	= new BoxIdentifier( ctx.IDENTIFIER( i ).getText(), tools.getPosition( ctx.IDENTIFIER( i ).getSymbol() ),
			    ctx.IDENTIFIER( i ).getText() );
			BoxExpression	value	= i < ctx.expression().size() ? ctx.expression( i ).accept( expressionVisitor ) : new BoxNull( pos, src );
			declarators.add( new BoxExpressionStatement( new BoxAssignment( target, BoxAssignmentOperator.Equal, value, List.of(), pos, src ), pos, src ) );
		}

		return declarators.size() == 1 ? declarators.get( 0 ) : new BoxStatementBlock( declarators, pos, src );
	}

	@Override
	public BoxNode visitIfStatement( IfStatementContext ctx ) {
		var				pos			= tools.getPosition( ctx );
		var				src			= tools.getSourceText( ctx );
		BoxExpression	condition	= ctx.expression().accept( expressionVisitor );
		BoxStatement	thenBody	= ( BoxStatement ) ctx.statement( 0 ).accept( this );
		BoxStatement	elseBody	= ctx.statement().size() > 1 ? ( BoxStatement ) ctx.statement( 1 ).accept( this ) : null;
		return new BoxIfElse( condition, thenBody, elseBody, pos, src );
	}

	@Override
	public BoxNode visitWhileStatement( WhileStatementContext ctx ) {
		var				pos			= tools.getPosition( ctx );
		var				src			= tools.getSourceText( ctx );
		BoxExpression	condition	= ctx.expression().accept( expressionVisitor );
		BoxStatement	body		= ( BoxStatement ) ctx.statement().accept( this );
		return new BoxWhile( null, condition, body, pos, src );
	}

	@Override
	public BoxNode visitDoWhileStatement( DoWhileStatementContext ctx ) {
		var				pos			= tools.getPosition( ctx );
		var				src			= tools.getSourceText( ctx );
		BoxExpression	condition	= ctx.expression().accept( expressionVisitor );
		BoxStatement	body		= buildBlock( ctx.block() );
		return new BoxDo( null, condition, body, pos, src );
	}

	@Override
	public BoxNode visitForStatement( ForStatementContext ctx ) {
		var				pos		= tools.getPosition( ctx );
		var				src		= tools.getSourceText( ctx );
		BoxStatement	body	= ( BoxStatement ) ctx.statement().accept( this );

		if ( ctx.forControl() instanceof ForInControlContext forIn ) {
			BoxIdentifier	variable	= new BoxIdentifier( forIn.IDENTIFIER().getText(), tools.getPosition( forIn.IDENTIFIER().getSymbol() ),
			    forIn.IDENTIFIER().getText() );
			BoxExpression	iterable	= forIn.expression().accept( expressionVisitor );
			boolean			hasVar		= forIn.typeName() != null || forIn.DEF() != null;
			return new BoxForIn( null, variable, null, iterable, body, hasVar, pos, src );
		}

		ClassicForControlContext	classic		= ( ClassicForControlContext ) ctx.forControl();
		BoxExpression				initializer	= buildForInit( classic );
		BoxExpression				condition	= classic.expression() != null ? classic.expression().accept( expressionVisitor )
		    : new BoxBooleanLiteral( Boolean.TRUE, pos, src );
		BoxExpression				step		= classic.forUpdate() != null && !classic.forUpdate().expression().isEmpty()
		    ? classic.forUpdate().expression( 0 ).accept( expressionVisitor )
		    : null;
		return new BoxForIndex( null, initializer, condition, step, body, pos, src );
	}

	private BoxExpression buildForInit( ClassicForControlContext classic ) {
		var	pos	= tools.getPosition( classic );
		var	src	= tools.getSourceText( classic );
		if ( classic.forInit() == null ) {
			return null;
		}
		if ( classic.forInit().IDENTIFIER() != null && !classic.forInit().IDENTIFIER().isEmpty() ) {
			// (typeName|DEF) IDENTIFIER = expr (, IDENTIFIER = expr)* - only the first
			// declarator is represented (classic for's initializer is a single BoxExpression
			// slot); additional comma-separated declarators are a documented Phase 2 gap.
			BoxIdentifier	target	= new BoxIdentifier( classic.forInit().IDENTIFIER( 0 ).getText(),
			    tools.getPosition( classic.forInit().IDENTIFIER( 0 ).getSymbol() ), classic.forInit().IDENTIFIER( 0 ).getText() );
			BoxExpression	value	= classic.forInit().expression( 0 ).accept( expressionVisitor );
			return new BoxAssignment( target, BoxAssignmentOperator.Equal, value, List.of(), pos, src );
		}
		return classic.forInit().expression( 0 ).accept( expressionVisitor );
	}

	@Override
	public BoxNode visitTryStatement( TryStatementContext ctx ) {
		var					pos			= tools.getPosition( ctx );
		var					src			= tools.getSourceText( ctx );
		List<BoxStatement>	tryBody		= buildStatementList( ctx.block().blockStatements() );
		List<BoxTryCatch>	catches		= ctx.catchClause().stream().map( this::buildCatchClause ).collect( Collectors.toList() );
		List<BoxStatement>	finallyBody	= ctx.finallyClause() != null ? buildStatementList( ctx.finallyClause().block().blockStatements() )
		    : new ArrayList<>();
		return new BoxTry( tryBody, catches, finallyBody, pos, src );
	}

	private BoxTryCatch buildCatchClause( CatchClauseContext ctx ) {
		var					pos			= tools.getPosition( ctx );
		var					src			= tools.getSourceText( ctx );
		List<BoxExpression>	catchTypes	= ctx.typeName().stream().map( expressionVisitor::toTypeExpression ).collect( Collectors.toList() );
		BoxIdentifier		exception	= new BoxIdentifier( ctx.IDENTIFIER().getText(), tools.getPosition( ctx.IDENTIFIER().getSymbol() ),
		    ctx.IDENTIFIER().getText() );
		List<BoxStatement>	catchBody	= buildStatementList( ctx.block().blockStatements() );
		return new BoxTryCatch( catchTypes, exception, catchBody, pos, src );
	}

	@Override
	public BoxNode visitThrowStatement( ThrowStatementContext ctx ) {
		var	pos	= tools.getPosition( ctx );
		var	src	= tools.getSourceText( ctx );
		return new BoxThrow( ctx.expression().accept( expressionVisitor ), pos, src );
	}

	@Override
	public BoxNode visitReturnStatement( ReturnStatementContext ctx ) {
		var				pos			= tools.getPosition( ctx );
		var				src			= tools.getSourceText( ctx );
		BoxExpression	expression	= ctx.expression() != null ? ctx.expression().accept( expressionVisitor ) : null;
		return new BoxReturn( expression, pos, src );
	}

	@Override
	public BoxNode visitBreakStatement( BreakStatementContext ctx ) {
		var		pos		= tools.getPosition( ctx );
		var		src		= tools.getSourceText( ctx );
		String	label	= ctx.IDENTIFIER() != null ? ctx.IDENTIFIER().getText() : null;
		return label != null ? new BoxBreak( label, pos, src ) : new BoxBreak( pos, src );
	}

	@Override
	public BoxNode visitContinueStatement( ContinueStatementContext ctx ) {
		var		pos		= tools.getPosition( ctx );
		var		src		= tools.getSourceText( ctx );
		String	label	= ctx.IDENTIFIER() != null ? ctx.IDENTIFIER().getText() : null;
		return label != null ? new BoxContinue( label, pos, src ) : new BoxContinue( pos, src );
	}

	@Override
	public BoxNode visitExprStatement( ExprStatementContext ctx ) {
		var	pos	= tools.getPosition( ctx );
		var	src	= tools.getSourceText( ctx );
		return new BoxExpressionStatement( ctx.expression().accept( expressionVisitor ), pos, src );
	}

}
