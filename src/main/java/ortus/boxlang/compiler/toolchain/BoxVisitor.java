package ortus.boxlang.compiler.toolchain;

import org.antlr.v4.runtime.tree.ParseTree;
import ortus.boxlang.compiler.ast.*;
import ortus.boxlang.compiler.ast.expression.*;
import ortus.boxlang.compiler.ast.statement.BoxAnnotation;
import ortus.boxlang.compiler.ast.statement.BoxDocumentationAnnotation;
import ortus.boxlang.compiler.ast.statement.BoxImport;
import ortus.boxlang.compiler.ast.statement.BoxProperty;
import ortus.boxlang.compiler.ast.statement.component.BoxComponent;
import ortus.boxlang.parser.antlr.BoxScriptGrammar;
import ortus.boxlang.parser.antlr.BoxScriptGrammarBaseVisitor;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.services.ComponentService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * This class is responsible for creating the AST from the ANTLR generated
 * parse tree.
 * <p>
 * A parser tree is a great jumping off point for creating an AST, but it is
 * not the AST itself as its
 * structure is dictated by the grammar and not the structure of the language
 * itself.
 * <p>
 * We create a standardized AST here, from whence we can then perform further
 * analysis and transformations and
 * eventually code generation, should that be the end goal.
 * <p>
 * Note that by the time this visitor is called, it should have been
 * thoroughly checked
 * as this visitor makes no checks on parameters (and it should not), and
 * raises no Issues.
 */
public class BoxVisitor extends BoxScriptGrammarBaseVisitor<BoxNode> {

	private boolean			inOutputBlock		= false;
	public ComponentService	componentService	= BoxRuntime.getInstance().getComponentService();

	public void setInOutputBlock( boolean inOutputBlock ) {
		this.inOutputBlock = inOutputBlock;
	}

	public boolean getInOutputBlock() {
		return inOutputBlock;
	}

	private final Tools					tools				= new Tools();
	private final BoxExpressionVisitor	expressionVisitor	= new BoxExpressionVisitor();

	/**
	 * Visit the class or interface context to generate the AST node for the
	 * top level node
	 *
	 * @param ctx the parse tree
	 *
	 * @return the AST node representing the class or interface
	 */
	@Override
	public BoxNode visitClassOrInterface( BoxScriptGrammar.ClassOrInterfaceContext ctx ) {
		// NOTE: ANTLR renames rules that match Java keywords so interface_()
		// is used instead of interface
		return ctx.boxClass() != null ? ctx.boxClass().accept( this ) : ctx.interface_().accept( this );
	}

	@Override
	public BoxNode visitScript( BoxScriptGrammar.ScriptContext ctx ) {
		// Force the script to start at the top of the file so doc comments associate with functions correctly
		var					pos			= tools.getPositionStartingAt( ctx, tools.getFirstToken() );
		var					src			= tools.getSourceText( ctx );

		List<BoxStatement>	statements	= new ArrayList<>();
		ctx.functionOrStatement().forEach( stmt -> {
			statements.add( ( BoxStatement ) stmt.accept( this ) );
		} );
		return new BoxScript( statements, pos, src );
	}

	@Override
	public BoxNode visitImportStatement( BoxScriptGrammar.ImportStatementContext ctx ) {
		var				pos		= tools.getPosition( ctx );
		var				src		= tools.getSourceText( ctx );

		BoxExpression	expr	= Optional.ofNullable( ctx.importFQN() )
		    .map( fqn -> {
									    String prefix = Optional.ofNullable( ctx.PREFIX() ).map( ParseTree::getText ).orElse( "" );
									    return new BoxFQN( prefix + fqn.getText(), tools.getPosition( fqn ), tools.getSourceText( fqn ) );
								    } )
		    .orElse( null );

		BoxIdentifier	alias	= Optional.ofNullable( ctx.identifier() )
		    .map( id -> ( BoxIdentifier ) id.accept( expressionVisitor ) )
		    .orElse( null );

		return new BoxImport( expr, alias, pos, src );
	}

	@Override
	public BoxNode visitInclude( BoxScriptGrammar.IncludeContext ctx ) {
		var				pos		= tools.getPosition( ctx );
		var				src		= tools.getSourceText( ctx );
		BoxExpression	expr	= ctx.expression().accept( expressionVisitor );
		var				exprCtx	= ctx.expression();
		var				ePos	= tools.getPosition( exprCtx );
		var				eSrc	= tools.getSourceText( exprCtx );
		return new BoxComponent( "include", List.of( new BoxAnnotation( new BoxFQN( "template", ePos, eSrc ), expr, ePos, eSrc ) ), pos, src );
	}

	/**
	 * Visit the boxClass context to generate the AST node for the class
	 *
	 * @param ctx the parse tree
	 *
	 * @return the AST node representing the class
	 */
	@Override
	public BoxNode visitBoxClass( BoxScriptGrammar.BoxClassContext ctx ) {
		var									pos				= tools.getPosition( ctx );
		var									src				= tools.getSourceText( ctx );

		List<BoxStatement>					body			= buildClassBody( ctx.classBody() );
		List<BoxImport>						imports			= new ArrayList<>();
		List<BoxAnnotation>					annotations		= new ArrayList<>();
		List<BoxDocumentationAnnotation>	documentation	= new ArrayList<>();
		List<BoxProperty>					property		= new ArrayList<>();

		processIfNotNull( ctx.importStatement(), stmt -> {
			imports.add( ( BoxImport ) stmt.accept( this ) );
		} );

		processIfNotNull( ctx.preAnnotation(), stmt -> {
			annotations.add( ( BoxAnnotation ) stmt.accept( this ) );
		} );

		processIfNotNull( ctx.postAnnotation(), stmt -> {
			annotations.add( ( BoxAnnotation ) stmt.accept( this ) );
		} );

		processIfNotNull( ctx.property(), stmt -> {
			property.add( ( BoxProperty ) stmt.accept( this ) );
		} );

		return new BoxClass( imports, body, annotations, documentation, property, pos, src );
	}

	@Override
	public BoxNode visitClassBodyStatement( BoxScriptGrammar.ClassBodyStatementContext ctx ) {
		return Optional.ofNullable( ctx.staticInitializer() )
		    .map( init -> init.accept( this ) )
		    .orElseGet( () -> ctx.functionOrStatement().accept( this ) );
	}

	@Override
	public BoxNode visitStaticInitializer( BoxScriptGrammar.StaticInitializerContext ctx ) {
		var					pos		= tools.getPosition( ctx );
		var					src		= tools.getSourceText( ctx );
		List<BoxStatement>	body	= buildStaticBody( ctx.statementBlock() );
		return new BoxStaticInitializer( body, pos, src );
	}

	/**
	 * Visit the interface_ context to generate the AST node for the interface
	 *
	 * @param ctx the parse tree
	 *
	 * @return the AST node representing the interface
	 */
	@Override
	public BoxInterface visitInterface( BoxScriptGrammar.InterfaceContext ctx ) {

		// Again, the BOX AST should really just accept expressions or
		// statements,
		// but for now we will cast and explore AST inheritance later (it may
		// be too late).

		List<BoxStatement>					body			= new ArrayList<>();
		List<BoxAnnotation>					preAnnotations	= new ArrayList<>();
		List<BoxAnnotation>					postAnnotations	= new ArrayList<>();
		List<BoxDocumentationAnnotation>	documentation	= new ArrayList<>();
		List<BoxImport>						imports			= new ArrayList<>();

		processIfNotNull( ctx.importStatement(), stmt -> imports.add( ( BoxImport ) stmt.accept( this ) ) );
		processIfNotNull( ctx.preAnnotation(), stmt -> preAnnotations.add( ( BoxAnnotation ) stmt.accept( this ) ) );
		processIfNotNull( ctx.postAnnotation(), annotation -> postAnnotations.add( ( BoxAnnotation ) annotation.accept( this ) ) );
		processIfNotNull( ctx.abstractFunction(), stmt -> body.add( ( BoxStatement ) stmt.accept( this ) ) );
		processIfNotNull( ctx.function(), stmt -> body.add( ( BoxStatement ) stmt.accept( this ) ) );

		// TODO: Why is staticInitializer not processed in the original BoxsScriptParser code?

		return new BoxInterface( imports, body, preAnnotations, postAnnotations, documentation, tools.getPosition( ctx ), tools.getSourceText( ctx ) );
	}

	@Override
	public BoxNode visitAbstractFunction( BoxScriptGrammar.AbstractFunctionContext ctx ) {
		var					pos		= tools.getPosition( ctx );
		var					src		= tools.getSourceText( ctx );
		List<BoxArgument>	args	= new ArrayList<>();
		List<BoxAnnotation>	annotations	= new ArrayList<>();

		processIfNotNull( ctx.preAnnotation(), stmt -> annotations.add( ( BoxAnnotation ) stmt.accept( this ) );
		processIfNotNull( ctx.postAnnotation(), stmt -> annotations.add( ( BoxAnnotation ) stmt.accept( this ) );

		// TODO: Restart here tomorrow
		
		return null;
	}

	// ======================================================================
	// Expressions as statements
	//
	// It is often easier to allow expressions to be in the statement rules, then
	// our context tells us whether it is a statement or an expression, such as Assignment
	// for instance.

	/**
	 * Visit the Assign expressions that are actually statements, and treat them as so
	 */
	@Override
	public BoxAssignment visitExprAssign( BoxScriptGrammar.ExprAssignContext ctx ) {
		BoxExpression			target		= ctx.expression( 0 ).accept( expressionVisitor );
		BoxExpression			value		= ctx.expression( 1 ).accept( expressionVisitor );
		BoxAssignmentOperator	operator	= null;
		switch ( ctx.op.getType() ) {
			case BoxScriptGrammar.EQUALSIGN -> operator = BoxAssignmentOperator.Equal;
			case BoxScriptGrammar.PLUSEQUAL -> operator = BoxAssignmentOperator.PlusEqual;
			case BoxScriptGrammar.MINUSEQUAL -> operator = BoxAssignmentOperator.MinusEqual;
			case BoxScriptGrammar.STAREQUAL -> operator = BoxAssignmentOperator.StarEqual;
			case BoxScriptGrammar.SLASHEQUAL -> operator = BoxAssignmentOperator.SlashEqual;
			case BoxScriptGrammar.MODEQUAL -> operator = BoxAssignmentOperator.ModEqual;
			case BoxScriptGrammar.CONCATEQUAL -> operator = BoxAssignmentOperator.ConcatEqual;
		}
		// Note that modifiers are not seen in the expression version of assign

		return new BoxAssignment( target, operator, value, null, tools.getPosition( ctx ), tools.getSourceText( ctx ) );
	}

	/**
	 * Visit variable declarations with or without assignments
	 */
	@Override
	public BoxNode visitVarDecl( BoxScriptGrammar.VarDeclContext ctx ) {

		// The variable declaration here comes form the statement var xyz

		var	modifiers	= new ArrayList<BoxAssignmentModifier>();
		var	expr		= ( BoxAssignment ) ctx.expression().accept( this );

		// Note that if more than one modifier is allowed, this will automatically
		// use them, and we will not have to change the code
		processIfNotNull( ctx.varModifier(), modifier -> modifiers.add( buildAssignmentModifier( modifier ) ) );
		expr.setModifiers( modifiers );
		return expr;
	}

	public BoxAnnotation visitPostAnnotation( BoxScriptGrammar.PostAnnotationContext ctx ) {
		var				pos		= tools.getPosition( ctx );
		var				src		= tools.getSourceText( ctx );

		BoxFQN			name	= new BoxFQN( ctx.identifier().getText(), tools.getPosition( ctx.identifier() ), tools.getSourceText( ctx.identifier() ) );
		BoxExpression	value	= Optional.ofNullable( ctx.expression() ).map( expression -> expression.accept( expressionVisitor ) ).orElse( null );

		return new BoxAnnotation( name, value, pos, src );
	}

	public BoxAnnotation visitPreAnnotation( BoxScriptGrammar.PreAnnotationContext ctx ) {
		var				pos		= tools.getPosition( ctx );
		var				src		= tools.getSourceText( ctx );

		BoxExpression	aname	= ctx.fqn().accept( expressionVisitor );

		BoxExpression	avalue	= null;
		if ( ctx.expression() != null ) {
			List<BoxExpression> values = ctx.expression().stream().map( expression -> expression.accept( expressionVisitor ) ).collect( Collectors.toList() );

			if ( values.size() == 1 ) {
				avalue = values.getFirst();
			} else if ( values.size() > 1 ) {
				avalue = new BoxArrayLiteral( values, pos, src );
			}
		}

		return new BoxAnnotation( ( BoxFQN ) aname, avalue, pos, src );
	}

	// ======================================================================
	// Builders
	//
	// Builders are used when they are more convenient than the visitor pattern, such as building a list of statements

	private List<BoxStatement> buildClassBody( BoxScriptGrammar.ClassBodyContext ctx ) {
		List<BoxStatement> body = new ArrayList<>();
		processIfNotNull( ctx.classBodyStatement(), stmt -> body.add( ( BoxStatement ) stmt.accept( this ) ) );
		return body;
	}

	private List<BoxStatement> buildStaticBody( BoxScriptGrammar.StatementBlockContext ctx ) {
		List<BoxStatement> body = new ArrayList<>();
		processIfNotNull( ctx.statement(), stmt -> body.add( ( BoxStatement ) stmt.accept( this ) ) );
		return body;
	}

	private <T> void processIfNotNull( List<T> list, Consumer<T> consumer ) {
		Optional.ofNullable( list )
		    .ifPresent( l -> l.forEach( consumer ) );
	}

	public BoxAssignmentModifier buildAssignmentModifier( BoxScriptGrammar.VarModifierContext ctx ) {
		BoxAssignmentModifier modifier = null;
		// No error checks, we expect the parse tree to have been verified by this point
		// As we expect the modifiers to be expanded, we use a switch here
		switch ( ctx.op.getType() ) {
			case BoxScriptGrammar.VAR -> modifier = BoxAssignmentModifier.VAR;
		}
		return modifier;
	}

}