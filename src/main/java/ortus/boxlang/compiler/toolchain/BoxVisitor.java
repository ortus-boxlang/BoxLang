package ortus.boxlang.compiler.toolchain;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import ortus.boxlang.compiler.ast.*;
import ortus.boxlang.compiler.ast.expression.*;
import ortus.boxlang.compiler.ast.statement.BoxType;
import ortus.boxlang.compiler.ast.statement.*;
import ortus.boxlang.compiler.ast.statement.component.BoxComponent;
import ortus.boxlang.compiler.ast.statement.component.BoxTemplateIsland;
import ortus.boxlang.compiler.parser.BoxScriptParser;
import ortus.boxlang.parser.antlr.BoxScriptGrammar;
import ortus.boxlang.parser.antlr.BoxScriptGrammarBaseVisitor;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.services.ComponentService;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
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

	public ComponentService componentService = BoxRuntime.getInstance().getComponentService();

	public BoxVisitor( BoxScriptParser tools ) {
		this.tools				= tools;
		this.expressionVisitor	= new BoxExpressionVisitor( tools, this );
	}

	private final BoxScriptParser		tools;
	private final BoxExpressionVisitor	expressionVisitor;

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

		List<BoxStatement>	statements	= ctx.functionOrStatement().stream().map( stmt -> ( BoxStatement ) stmt.accept( this ) ).collect( Collectors.toList() );

		return new BoxScript( statements, pos, src );
	}

	@Override
	public BoxNode visitImportStatement( BoxScriptGrammar.ImportStatementContext ctx ) {
		var				pos		= tools.getPosition( ctx );
		var				src		= tools.getSourceText( ctx );

		BoxExpression	expr	= Optional.ofNullable( ctx.importFQN() ).map( fqn -> {
									String prefix = Optional.ofNullable( ctx.PREFIX() ).map( ParseTree::getText ).orElse( "" );
									return new BoxFQN( prefix + fqn.getText(), tools.getPosition( fqn ), tools.getSourceText( fqn ) );
								} ).orElse( null );

		BoxIdentifier	alias	= Optional.ofNullable( ctx.identifier() ).map( id -> ( BoxIdentifier ) id.accept( expressionVisitor ) ).orElse( null );

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

		processIfNotNull( ctx.importStatement(), stmt -> imports.add( ( BoxImport ) stmt.accept( this ) ) );
		processIfNotNull( ctx.preAnnotation(), a -> annotations.add( ( BoxAnnotation ) a.accept( this ) ) );
		processIfNotNull( ctx.postAnnotation(), a -> annotations.add( ( BoxAnnotation ) a.accept( this ) ) );
		processIfNotNull( ctx.property(), p -> property.add( ( BoxProperty ) p.accept( this ) ) );

		return new BoxClass( imports, body, annotations, documentation, property, pos, src );
	}

	@Override
	public BoxNode visitClassBodyStatement( BoxScriptGrammar.ClassBodyStatementContext ctx ) {
		return Optional.ofNullable( ctx.staticInitializer() ).map( init -> init.accept( this ) ).orElseGet( () -> ctx.functionOrStatement().accept( this ) );
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
		return buildFunction( ctx.functionSignature().preAnnotation(), ctx.postAnnotation(), ctx.functionSignature().identifier().getText(),
		    ctx.functionSignature(), null, ctx );
	}

	@Override
	public BoxNode visitStatement( BoxScriptGrammar.StatementContext ctx ) {
		List<Function<BoxScriptGrammar.StatementContext, ParserRuleContext>> functions = Arrays.asList( BoxScriptGrammar.StatementContext::importStatement,
		    BoxScriptGrammar.StatementContext::do_, BoxScriptGrammar.StatementContext::for_, BoxScriptGrammar.StatementContext::if_,
		    BoxScriptGrammar.StatementContext::switch_, BoxScriptGrammar.StatementContext::try_, BoxScriptGrammar.StatementContext::while_,
		    BoxScriptGrammar.StatementContext::expression, BoxScriptGrammar.StatementContext::include, BoxScriptGrammar.StatementContext::component,
		    BoxScriptGrammar.StatementContext::statementBlock, BoxScriptGrammar.StatementContext::simpleStatement,
		    BoxScriptGrammar.StatementContext::componentIsland, BoxScriptGrammar.StatementContext::varDecl );

		// Iterate over the functions
		for ( Function<BoxScriptGrammar.StatementContext, ParserRuleContext> function : functions ) {
			ParserRuleContext result = function.apply( ctx );
			if ( result != null ) {
				return result.accept( this );
			}
		}

		// If none of the functions return a non-null result, return null
		return null;
	}

	@Override
	public BoxNode visitDo( BoxScriptGrammar.DoContext ctx ) {
		var				pos			= tools.getPosition( ctx );
		var				src			= tools.getSourceText( ctx );
		BoxExpression	condition	= ctx.expression().accept( expressionVisitor );
		BoxStatement	body		= ( BoxStatement ) ctx.statement().accept( this );
		String			label		= Optional.ofNullable( ctx.PREFIX() ).map( ParseTree::getText ).map( text -> text.substring( 0, text.length() - 1 ) )
		    .orElse( null );
		return new BoxDo( label, condition, body, pos, src );
	}

	@Override
	public BoxNode visitFor( BoxScriptGrammar.ForContext ctx ) {
		var					pos			= tools.getPosition( ctx );
		var					src			= tools.getSourceText( ctx );
		BoxStatement		body		= ( BoxStatement ) ctx.statement().accept( this );
		String				label		= Optional.ofNullable( ctx.PREFIX() ).map( ParseTree::getText ).map( text -> text.substring( 0, text.length() - 1 ) )
		    .orElse( null );
		List<BoxExpression>	expressions	= Optional.ofNullable( ctx.expression() ).orElse( Collections.emptyList() ).stream()
		    .map( expression -> expression.accept( expressionVisitor ) ).toList();

		// If this is the IN style, then we are guaranteed to have two expressions
		if ( ctx.IN() != null ) {
			return new BoxForIn( label, expressions.get( 0 ), expressions.get( 1 ), body, ctx.VAR() != null, pos, src );
		}

		// Otherwise we have an index with 0 <= n <= 3 expressions
		return new BoxForIndex( label, getOrNull( expressions, 0 ), getOrNull( expressions, 1 ), getOrNull( expressions, 2 ), body, pos, src );
	}

	@Override
	public BoxNode visitIf( BoxScriptGrammar.IfContext ctx ) {
		var				pos			= tools.getPosition( ctx );
		var				src			= tools.getSourceText( ctx );

		BoxExpression	condition	= ctx.expression().accept( expressionVisitor );
		BoxStatement	thenBody	= ( BoxStatement ) ctx.ifStmt.accept( this );
		BoxStatement	elseBody	= Optional.ofNullable( ctx.elseStmt ).map( stmt -> ( BoxStatement ) stmt.accept( this ) ).orElse( null );
		return new BoxIfElse( condition, thenBody, elseBody, pos, src );
	}

	@Override
	public BoxNode visitSwitch( BoxScriptGrammar.SwitchContext ctx ) {
		var					pos			= tools.getPosition( ctx );
		var					src			= tools.getSourceText( ctx );

		BoxExpression		condition	= ctx.expression().accept( expressionVisitor );
		List<BoxSwitchCase>	cases		= ctx.case_().stream().map( caseBlock -> ( BoxSwitchCase ) caseBlock.accept( this ) ).collect( Collectors.toList() );
		return new BoxSwitch( condition, cases, pos, src );
	}

	@Override
	public BoxNode visitCase( BoxScriptGrammar.CaseContext ctx ) {
		var					pos			= tools.getPosition( ctx );
		var					src			= tools.getSourceText( ctx );

		// Assign null to condition if ctx.expression() is null, otherwise call accept(expressionVisitor)
		// if null, then this is the default.
		BoxExpression		condition	= Optional.ofNullable( ctx.expression() ).map( expression -> expression.accept( expressionVisitor ) ).orElse( null );

		// Produce body from iterating ctx.statement() and calling accept(this) on each one,
		// or assign null to body if there are no ctx.statement()
		List<BoxStatement>	body		= Optional.ofNullable( ctx.statement() )
		    .map( statements -> statements.stream().map( statement -> ( BoxStatement ) statement.accept( this ) ).collect( Collectors.toList() ) )
		    .orElse( null );

		return new BoxSwitchCase( condition, null, body, pos, src );
	}

	@Override
	public BoxStatement visitTry( BoxScriptGrammar.TryContext ctx ) {
		var					pos				= tools.getPosition( ctx );
		var					src				= tools.getSourceText( ctx );

		List<BoxStatement>	body			= buildStatementBlock( ctx.statementBlock() );
		List<BoxTryCatch>	catches			= ctx.catches().stream().map( catchBlock -> ( BoxTryCatch ) catchBlock.accept( this ) ).toList();
		List<BoxStatement>	finallyBlock	= Optional.ofNullable( ctx.finallyBlock() ).map( BoxScriptGrammar.FinallyBlockContext::statementBlock )
		    .map( this::buildStatementBlock ).orElse( null );
		return new BoxTry( body, catches, finallyBlock, pos, src );
	}

	@Override
	public BoxStatement visitCatches( BoxScriptGrammar.CatchesContext ctx ) {
		var					pos			= tools.getPosition( ctx );
		var					src			= tools.getSourceText( ctx );

		BoxExpression		exception	= ctx.ex.accept( expressionVisitor );
		List<BoxExpression>	catchTypes	= Optional.ofNullable( ctx.ct )
		    .map( ctList -> ctList.stream().map( ct -> ct.accept( expressionVisitor ) ).collect( Collectors.toList() ) ).orElse( null );
		var					catchBody	= buildStatementBlock( ctx.statementBlock() );

		List<BoxStatement>	body		= buildStatementBlock( ctx.statementBlock() );
		return new BoxTryCatch( catchTypes, exception, catchBody, pos, src );
	}

	@Override
	public BoxStatement visitWhile( BoxScriptGrammar.WhileContext ctx ) {
		var				pos			= tools.getPosition( ctx );
		var				src			= tools.getSourceText( ctx );

		BoxExpression	condition	= ctx.expression().accept( expressionVisitor );
		BoxStatement	body		= ( BoxStatement ) ctx.statement().accept( this );
		String			label		= Optional.ofNullable( ctx.PREFIX() ).map( ParseTree::getText ).map( text -> text.substring( 0, text.length() - 1 ) )
		    .orElse( null );
		return new BoxWhile( label, condition, body, pos, src );
	}

	@Override
	public BoxNode visitComponent( BoxScriptGrammar.ComponentContext ctx ) {
		var					pos			= tools.getPosition( ctx );
		var					src			= tools.getSourceText( ctx );

		String				name		= ctx.componentName().getText();
		List<BoxAnnotation>	attributes	= Optional.ofNullable( ctx.componentAttribute() )
		    .map( attributeList -> attributeList.stream().map( attribute -> ( BoxAnnotation ) attribute.accept( this ) ).collect( Collectors.toList() ) )
		    .orElse( Collections.emptyList() );

		attributes = buildComponentAttributes( name, attributes, ctx );

		List<BoxStatement> body = null;
		if ( ctx.statementBlock() != null ) {
			body = buildStatementBlock( ctx.statementBlock() );
		}

		// Special check for loop condition to avoid runtime eval
		if ( name.equalsIgnoreCase( "loop" ) ) {
			for ( var attr : attributes ) {
				if ( attr.getKey().getValue().equalsIgnoreCase( "condition" ) ) {
					BoxExpression condition = attr.getValue();
					if ( condition instanceof BoxStringLiteral str ) {
						// parse as script expression and update value
						// TODO: AbstractParser inheritance and BoxParser as tools
						condition = tools.parseBoxExpression( str.getValue(), condition.getPosition() );
					}
					BoxExpression newCondition = new BoxClosure( List.of(), List.of(),
					    new BoxReturn( condition, condition.getPosition(), condition.getSourceText() ), condition.getPosition(), condition.getSourceText() );
					attr.setValue( newCondition );
				}
			}
		}

		return new BoxComponent( name, attributes, body, 0, pos, src );
	}

	@Override
	public BoxNode visitStatementBlock( BoxScriptGrammar.StatementBlockContext ctx ) {
		return new BoxStatementBlock( buildStatementBlock( ctx ), tools.getPosition( ctx ), tools.getSourceText( ctx ) );
	}

	@Override
	public BoxNode visitSimpleStatement( BoxScriptGrammar.SimpleStatementContext ctx ) {

		if ( ctx.SEMICOLON() != null ) {
			// TODO: Is it better to create a BoxEmptyStatement, or return null then filter null out?
			return new BoxNull( tools.getPosition( ctx ), tools.getSourceText( ctx ) );
		}

		List<Function<BoxScriptGrammar.SimpleStatementContext, ParserRuleContext>> functions = Arrays.asList( BoxScriptGrammar.SimpleStatementContext::break_,
		    BoxScriptGrammar.SimpleStatementContext::continue_, BoxScriptGrammar.SimpleStatementContext::rethrow,
		    BoxScriptGrammar.SimpleStatementContext::assert_, BoxScriptGrammar.SimpleStatementContext::param, BoxScriptGrammar.SimpleStatementContext::return_,
		    BoxScriptGrammar.SimpleStatementContext::throw_ );

		// Iterate over the functions
		for ( Function<BoxScriptGrammar.SimpleStatementContext, ParserRuleContext> function : functions ) {
			ParserRuleContext result = function.apply( ctx );
			if ( result != null ) {
				return result.accept( this );
			}
		}

		// If none of the functions return a non-null result, return null, though this cannot
		// happen unless the grammar changes without changing this visitor
		return null;
	}

	@Override
	public BoxNode visitComponentIsland( BoxScriptGrammar.ComponentIslandContext ctx ) {
		var	pos	= tools.getPosition( ctx );
		var	src	= tools.getSourceText( ctx );

		return new BoxTemplateIsland( tools.parseBoxTemplateStatements( ctx.componentIslandBody().getText(), tools.getPosition( ctx.componentIslandBody() ) ),
		    tools.getPosition( ctx.componentIslandBody() ), tools.getSourceText( ctx.componentIslandBody() ) );
	}

	@Override
	public BoxNode visitBreak( BoxScriptGrammar.BreakContext ctx ) {
		var		pos		= tools.getPosition( ctx );
		var		src		= tools.getSourceText( ctx );
		String	label	= Optional.ofNullable( ctx.identifier() ).map( ParseTree::getText ).orElse( null );
		return new BoxBreak( label, pos, src );
	}

	@Override
	public BoxNode visitContinue( BoxScriptGrammar.ContinueContext ctx ) {
		var		pos		= tools.getPosition( ctx );
		var		src		= tools.getSourceText( ctx );
		String	label	= Optional.ofNullable( ctx.identifier() ).map( ParseTree::getText ).orElse( null );
		return new BoxContinue( label, pos, src );
	}

	@Override
	public BoxNode visitRethrow( BoxScriptGrammar.RethrowContext ctx ) {
		var	pos	= tools.getPosition( ctx );
		var	src	= tools.getSourceText( ctx );
		return new BoxRethrow( pos, src );
	}

	@Override
	public BoxNode visitAssert( BoxScriptGrammar.AssertContext ctx ) {
		var				pos			= tools.getPosition( ctx );
		var				src			= tools.getSourceText( ctx );
		BoxExpression	condition	= ctx.expression().accept( expressionVisitor );
		return new BoxAssert( condition, pos, src );
	}

	@Override
	public BoxNode visitParam( BoxScriptGrammar.ParamContext ctx ) {
		var				pos				= tools.getPosition( ctx );
		var				src				= tools.getSourceText( ctx );

		BoxExpression	type			= null;
		BoxExpression	defaultValue	= null;
		BoxExpression	accessExpr;
		if ( ctx.type() != null ) {
			type = new BoxStringLiteral( ctx.type().getText(), tools.getPosition( ctx.type() ), tools.getSourceText( ctx.type() ) );
		}

		var expr = ctx.expression().accept( expressionVisitor );

		// We have one expression, but if it was an assignment, then we split it into two as the
		// access is the left side and the default value is the right side.
		if ( expr instanceof BoxAssignment assignment ) {
			accessExpr		= assignment.getLeft();
			defaultValue	= assignment.getRight();
		} else {
			accessExpr = expr;
		}
		return new BoxParam( new BoxStringLiteral( accessExpr.getSourceText(), accessExpr.getPosition(), accessExpr.getSourceText() ), type, defaultValue, pos,
		    src );
	}

	@Override
	public BoxNode visitReturn( BoxScriptGrammar.ReturnContext ctx ) {
		var				pos		= tools.getPosition( ctx );
		var				src		= tools.getSourceText( ctx );

		BoxExpression	value	= Optional.ofNullable( ctx.expression() ).map( expression -> expression.accept( expressionVisitor ) ).orElse( null );
		return new BoxReturn( value, pos, src );
	}

	@Override
	public BoxNode visitThrow( BoxScriptGrammar.ThrowContext ctx ) {
		var				pos		= tools.getPosition( ctx );
		var				src		= tools.getSourceText( ctx );

		BoxExpression	value	= ctx.expression().accept( expressionVisitor );
		return new BoxThrow( value, pos, src );
	}

	// ======================================================================
	// Expressions as statements
	//
	// It is often easier to allow expressions to be in the statement rules, then
	// our context tells us whether it is a statement or an expression, such as Assignment
	// for instance.

	public BoxNode visitExprFunctionCall( BoxScriptGrammar.ExprFunctionCallContext ctx ) {
		var	pos	= tools.getPosition( ctx );
		var	src	= tools.getSourceText( ctx );

		// Have teh expression builder make the function call for us, then we have to
		// wrap it in a statement, which is what we are doing here.
		return new BoxExpressionStatement( ctx.accept( expressionVisitor ), pos, src );
	}

	/**
	 * Visit the Assign expressions that are actually statements, and treat them as so
	 */
	@Override
	public BoxExpressionStatement visitExprAssign( BoxScriptGrammar.ExprAssignContext ctx ) {
		var						pos			= tools.getPosition( ctx );
		var						src			= tools.getSourceText( ctx );

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

		return new BoxExpressionStatement( new BoxAssignment( target, operator, value, new ArrayList<BoxAssignmentModifier>(), pos, src ), pos, src );
	}

	/**
	 * Visit variable declarations with or without assignments
	 */
	@Override
	public BoxNode visitVarDecl( BoxScriptGrammar.VarDeclContext ctx ) {
		var	pos			= tools.getPosition( ctx );
		var	src			= tools.getSourceText( ctx );

		// The variable declaration here comes form the statement var xyz

		var	modifiers	= new ArrayList<BoxAssignmentModifier>();
		var	expr		= ( BoxAssignment ) ctx.expression().accept( this );

		// Note that if more than one modifier is allowed, this will automatically
		// use them, and we will not have to change the code
		processIfNotNull( ctx.varModifier(), modifier -> modifiers.add( buildAssignmentModifier( modifier ) ) );
		expr.setModifiers( modifiers );
		return new BoxExpressionStatement( expr, pos, src );
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

	public BoxNode visitFunctionParam( BoxScriptGrammar.FunctionParamContext ctx ) {
		var									pos				= tools.getPosition( ctx );
		var									src				= tools.getSourceText( ctx );

		boolean								required		= false;
		String								type			= "Any";
		String								name;
		BoxExpression						expr			= null;
		List<BoxAnnotation>					annotations		= new ArrayList<>();
		List<BoxDocumentationAnnotation>	documentation	= new ArrayList<>();

		name = ctx.identifier().getText();
		if ( ctx.REQUIRED() != null ) {
			required = true;
		}

		if ( ctx.expression() != null ) {
			expr = ctx.expression().accept( expressionVisitor );
		}
		if ( ctx.type() != null ) {
			type = ctx.type().getText();
		}
		for ( BoxScriptGrammar.PostAnnotationContext annotation : ctx.postAnnotation() ) {
			annotations.add( ( BoxAnnotation ) annotation.accept( this ) );
		}

		return new BoxArgumentDeclaration( required, type, name, expr, annotations, documentation, pos, src );
	}

	@Override
	public BoxNode visitFunctionOrStatement( BoxScriptGrammar.FunctionOrStatementContext ctx ) {
		return Optional.ofNullable( ctx.statement() ).map( stmt -> stmt.accept( this ) )
		    .orElseGet( () -> Optional.ofNullable( ctx.abstractFunction() ).map( absFunc -> absFunc.accept( this ) )
		        .orElseGet( () -> Optional.ofNullable( ctx.function() ).map( func -> func.accept( this ) ).orElse( null ) ) );
	}

	// ======================================================================
	// Builders
	//
	// Builders are used when they are more convenient than the visitor pattern, such as building a list of statements

	/**
	 * Special check of component attributes to see if they are params script shortcut
	 * Factor out the duplicated code
	 *
	 * @param name       the name of the component
	 * @param attributes the attributes as we have constructed them so far
	 * @param ctx        the context of the component
	 * 
	 * @return Either the existing attributes or a new list of attributes
	 */
	private List<BoxAnnotation> buildComponentAttributes( String name, List<BoxAnnotation> attributes, BoxScriptGrammar.ComponentContext ctx ) {

		if ( !name.equalsIgnoreCase( "parm" ) ) {
			return attributes;
		}

		var					pos				= tools.getPosition( ctx );
		List<BoxAnnotation>	newAttributes	= new ArrayList<>();

		// If there is only one attribute, and it is not name= and has a value, then we need to convert it to a name/value pair
		// Ex: param foo="bar";
		// Becomes: param name="foo" default="bar";
		if ( attributes.size() == 1 && !attributes.get( 0 ).getKey().getValue().equalsIgnoreCase( "name" ) && attributes.get( 0 ).getValue() != null ) {
			newAttributes.add( new BoxAnnotation( new BoxFQN( "name", pos, "name" ),
			    new BoxStringLiteral( attributes.get( 0 ).getKey().getValue(), attributes.get( 0 ).getKey().getPosition(),
			        attributes.get( 0 ).getKey().getSourceText() ),
			    attributes.get( 0 ).getKey().getPosition(), "name=\"" + attributes.get( 0 ).getKey().getSourceText() + "\"" ) );
			newAttributes.add( new BoxAnnotation( new BoxFQN( "default", attributes.get( 0 ).getValue().getPosition(), "default" ),
			    attributes.get( 0 ).getValue(), attributes.get( 0 ).getValue().getPosition(), "default=" + attributes.get( 0 ).getValue().getSourceText() ) );

			return newAttributes;
		}
		// If there are two attributes, the first one has a null value, and none of them are named "name"
		// Ex: param String foo="bar";
		// Becomes: param type="String" name="foo" default="bar";
		// Ex: param String foo;
		// Becomes: param type="String" name="foo";
		if ( attributes.size() == 2 && attributes.get( 0 ).getValue() == null && !attributes.get( 0 ).getKey().getValue().equalsIgnoreCase( "name" )
		    && !attributes.get( 1 ).getKey().getValue().equalsIgnoreCase( "name" ) ) {
			newAttributes.add( new BoxAnnotation( new BoxFQN( "type", pos, "type" ),
			    new BoxStringLiteral( attributes.get( 0 ).getKey().getValue(), attributes.get( 0 ).getKey().getPosition(),
			        attributes.get( 0 ).getKey().getSourceText() ),
			    attributes.get( 0 ).getKey().getPosition(), "type=\"" + attributes.get( 0 ).getKey().getSourceText() + "\"" ) );
			newAttributes.add( new BoxAnnotation( new BoxFQN( "name", pos, "name" ),
			    new BoxStringLiteral( attributes.get( 1 ).getKey().getValue(), attributes.get( 1 ).getKey().getPosition(),
			        attributes.get( 1 ).getKey().getSourceText() ),
			    attributes.get( 1 ).getKey().getPosition(), "name=" + attributes.get( 1 ).getKey().getSourceText() ) );
			// Only if there is a default
			if ( attributes.get( 1 ).getValue() != null ) {
				newAttributes
				    .add( new BoxAnnotation( new BoxFQN( "default", attributes.get( 1 ).getValue().getPosition(), "default" ), attributes.get( 1 ).getValue(),
				        attributes.get( 1 ).getValue().getPosition(), "default=" + attributes.get( 1 ).getValue().getSourceText() ) );
			}
			return newAttributes;
		}
		return attributes;
	}

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
		Optional.ofNullable( list ).ifPresent( l -> l.forEach( consumer ) );
	}

	public <T> T getOrNull( List<T> list, int index ) {
		return ( index >= 0 && index < list.size() ) ? list.get( index ) : null;
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

	private BoxFunctionDeclaration buildFunction( List<BoxScriptGrammar.PreAnnotationContext> preannotations,
	    List<BoxScriptGrammar.PostAnnotationContext> postannotations, String name, BoxScriptGrammar.FunctionSignatureContext functionSignature,
	    BoxScriptGrammar.StatementBlockContext statementBlock, ParserRuleContext ctx ) {

		var										pos					= tools.getPosition( ctx );
		var										src					= tools.getSourceText( ctx );

		BoxReturnType							returnType;

		List<BoxStatement>						body;    // Is null for interface function.
		List<BoxArgumentDeclaration>			args				= new ArrayList<>();
		List<BoxAnnotation>						annotations			= new ArrayList<>();
		List<BoxDocumentationAnnotation>		documentation		= new ArrayList<>();
		List<BoxAnnotation>						annToRemove			= new ArrayList<>();
		List<BoxMethodDeclarationModifier>		modifiers			= new ArrayList<>();
		BoxAccessModifier						visibility			= null;

		List<BoxScriptGrammar.ModifierContext>	modifierContexts	= Optional.ofNullable( functionSignature.modifier() ).orElse( Collections.emptyList() );

		// Resolve modifiers and access qualifiers, in any order
		// TODO: convert to build helper method
		for ( BoxScriptGrammar.ModifierContext modifierContext : modifierContexts ) {
			String modifierText = modifierContext.getText().toUpperCase();

			switch ( modifierText ) {
				case "STATIC" :
					modifiers.add( BoxMethodDeclarationModifier.STATIC );
					break;
				case "FINAL" :
					modifiers.add( BoxMethodDeclarationModifier.FINAL );
					break;
				case "ABSTRACT" :
					modifiers.add( BoxMethodDeclarationModifier.ABSTRACT );
					break;
				case "DEFAULT" :
					modifiers.add( BoxMethodDeclarationModifier.DEFAULT );
					break;
				case "PUBLIC" :
					visibility = BoxAccessModifier.Public;
					break;
				case "PRIVATE" :
					visibility = BoxAccessModifier.Private;
					break;
				case "REMOTE" :
					visibility = BoxAccessModifier.Remote;
					break;
				case "PACKAGE" :
					visibility = BoxAccessModifier.Package;
					break;
				default :
					// Handle unknown modifier - cannot happen
					break;
			}
		}

		// Accumulate pre annotations
		Optional.ofNullable( preannotations ).orElse( Collections.emptyList() ).stream().map( annotation -> ( BoxAnnotation ) annotation.accept( this ) )
		    .forEach( annotations::add );

		// Accumulate post annotations
		Optional.ofNullable( postannotations ).orElse( Collections.emptyList() ).stream().map( annotation -> ( BoxAnnotation ) annotation.accept( this ) )
		    .forEach( annotations::add );

		// Resolve annotations with parameters
		Optional.ofNullable( functionSignature.functionParamList() ).map( BoxScriptGrammar.FunctionParamListContext::functionParam )
		    .orElse( Collections.emptyList() ).forEach( arg -> {
			    BoxArgumentDeclaration argDeclaration = ( BoxArgumentDeclaration ) arg.accept( this );
			    buildAnnotations( argDeclaration, annotations, annToRemove );
			    args.add( argDeclaration );
		    } );

		// Function return type
		returnType	= Optional.ofNullable( functionSignature.returnType() ).map( returnTypeContext -> {
						String	targetType	= returnTypeContext.getText();
						BoxType	boxType		= BoxType.fromString( targetType );
						String	fqn			= boxType.equals( BoxType.Fqn ) ? targetType : null;
						return new BoxReturnType( boxType, fqn, tools.getPosition( returnTypeContext ), tools.getSourceText( returnTypeContext ) );
					} ).orElse( null );

		body		= buildStatementBlock( statementBlock );
		annotations.removeAll( annToRemove );

		return new BoxFunctionDeclaration( visibility, modifiers, name, returnType, args, annotations, documentation, body, pos, src );
	}

	private void buildAnnotations( BoxArgumentDeclaration argDeclaration, List<BoxAnnotation> annotations, List<BoxAnnotation> annToRemove ) {
		annotations.stream().filter( pre -> pre.getKey().getValue().toLowerCase().startsWith( argDeclaration.getName().toLowerCase() ) ).forEach( pre -> {
			String	prename	= pre.getKey().getValue();
			BoxFQN	key		= new BoxFQN( prename.substring( pre.getKey().getValue().indexOf( "." ) + 1 ), pre.getPosition(), pre.getSourceText() );
			argDeclaration.getAnnotations().add( new BoxAnnotation( key, pre.getValue(), pre.getPosition(), pre.getSourceText() ) );
			annToRemove.add( pre );
		} );
	}

	private List<BoxStatement> buildStatementBlock( BoxScriptGrammar.StatementBlockContext statementBlock ) {
		return Optional.ofNullable( statementBlock ).map( BoxScriptGrammar.StatementBlockContext::statement ).orElse( null ).stream()
		    .map( statement -> ( BoxStatement ) statement.accept( this ) ).collect( Collectors.toList() );
	}
}