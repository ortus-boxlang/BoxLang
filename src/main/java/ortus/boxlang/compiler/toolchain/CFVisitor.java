package ortus.boxlang.compiler.toolchain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTree;

import ortus.boxlang.compiler.ast.BoxClass;
import ortus.boxlang.compiler.ast.BoxExpression;
import ortus.boxlang.compiler.ast.BoxInterface;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.BoxScript;
import ortus.boxlang.compiler.ast.BoxStatement;
import ortus.boxlang.compiler.ast.BoxStatementError;
import ortus.boxlang.compiler.ast.BoxStaticInitializer;
import ortus.boxlang.compiler.ast.expression.BoxArrayLiteral;
import ortus.boxlang.compiler.ast.expression.BoxAssignment;
import ortus.boxlang.compiler.ast.expression.BoxClosure;
import ortus.boxlang.compiler.ast.expression.BoxDotAccess;
import ortus.boxlang.compiler.ast.expression.BoxFQN;
import ortus.boxlang.compiler.ast.expression.BoxIdentifier;
import ortus.boxlang.compiler.ast.expression.BoxStringLiteral;
import ortus.boxlang.compiler.ast.expression.BoxUnaryOperation;
import ortus.boxlang.compiler.ast.expression.BoxUnaryOperator;
import ortus.boxlang.compiler.ast.statement.BoxAccessModifier;
import ortus.boxlang.compiler.ast.statement.BoxAnnotation;
import ortus.boxlang.compiler.ast.statement.BoxArgumentDeclaration;
import ortus.boxlang.compiler.ast.statement.BoxBreak;
import ortus.boxlang.compiler.ast.statement.BoxContinue;
import ortus.boxlang.compiler.ast.statement.BoxDo;
import ortus.boxlang.compiler.ast.statement.BoxDocumentationAnnotation;
import ortus.boxlang.compiler.ast.statement.BoxExpressionStatement;
import ortus.boxlang.compiler.ast.statement.BoxForIn;
import ortus.boxlang.compiler.ast.statement.BoxForIndex;
import ortus.boxlang.compiler.ast.statement.BoxFunctionDeclaration;
import ortus.boxlang.compiler.ast.statement.BoxIfElse;
import ortus.boxlang.compiler.ast.statement.BoxImport;
import ortus.boxlang.compiler.ast.statement.BoxMethodDeclarationModifier;
import ortus.boxlang.compiler.ast.statement.BoxParam;
import ortus.boxlang.compiler.ast.statement.BoxProperty;
import ortus.boxlang.compiler.ast.statement.BoxRethrow;
import ortus.boxlang.compiler.ast.statement.BoxReturn;
import ortus.boxlang.compiler.ast.statement.BoxReturnType;
import ortus.boxlang.compiler.ast.statement.BoxStatementBlock;
import ortus.boxlang.compiler.ast.statement.BoxSwitch;
import ortus.boxlang.compiler.ast.statement.BoxSwitchCase;
import ortus.boxlang.compiler.ast.statement.BoxThrow;
import ortus.boxlang.compiler.ast.statement.BoxTry;
import ortus.boxlang.compiler.ast.statement.BoxTryCatch;
import ortus.boxlang.compiler.ast.statement.BoxType;
import ortus.boxlang.compiler.ast.statement.BoxWhile;
import ortus.boxlang.compiler.ast.statement.component.BoxComponent;
import ortus.boxlang.compiler.ast.statement.component.BoxTemplateIsland;
import ortus.boxlang.compiler.parser.CFParser;
import ortus.boxlang.parser.antlr.CFGrammar.AtomsContext;
import ortus.boxlang.parser.antlr.CFGrammar.BoxClassContext;
import ortus.boxlang.parser.antlr.CFGrammar.BreakContext;
import ortus.boxlang.parser.antlr.CFGrammar.CaseContext;
import ortus.boxlang.parser.antlr.CFGrammar.CatchesContext;
import ortus.boxlang.parser.antlr.CFGrammar.ClassBodyContext;
import ortus.boxlang.parser.antlr.CFGrammar.ClassBodyStatementContext;
import ortus.boxlang.parser.antlr.CFGrammar.ClassOrInterfaceContext;
import ortus.boxlang.parser.antlr.CFGrammar.ComponentAttributeContext;
import ortus.boxlang.parser.antlr.CFGrammar.ComponentContext;
import ortus.boxlang.parser.antlr.CFGrammar.ComponentIslandContext;
import ortus.boxlang.parser.antlr.CFGrammar.ContinueContext;
import ortus.boxlang.parser.antlr.CFGrammar.DoContext;
import ortus.boxlang.parser.antlr.CFGrammar.EmptyStatementBlockContext;
import ortus.boxlang.parser.antlr.CFGrammar.ExprAddContext;
import ortus.boxlang.parser.antlr.CFGrammar.ExprAndContext;
import ortus.boxlang.parser.antlr.CFGrammar.ExprArrayAccessContext;
import ortus.boxlang.parser.antlr.CFGrammar.ExprArrayLiteralContext;
import ortus.boxlang.parser.antlr.CFGrammar.ExprAssignContext;
import ortus.boxlang.parser.antlr.CFGrammar.ExprBinaryContext;
import ortus.boxlang.parser.antlr.CFGrammar.ExprCatContext;
import ortus.boxlang.parser.antlr.CFGrammar.ExprDotFloatContext;
import ortus.boxlang.parser.antlr.CFGrammar.ExprDotOrColonAccessContext;
import ortus.boxlang.parser.antlr.CFGrammar.ExprElvisContext;
import ortus.boxlang.parser.antlr.CFGrammar.ExprEqualContext;
import ortus.boxlang.parser.antlr.CFGrammar.ExprFunctionCallContext;
import ortus.boxlang.parser.antlr.CFGrammar.ExprIdentifierContext;
import ortus.boxlang.parser.antlr.CFGrammar.ExprLiteralsContext;
import ortus.boxlang.parser.antlr.CFGrammar.ExprMultContext;
import ortus.boxlang.parser.antlr.CFGrammar.ExprNewContext;
import ortus.boxlang.parser.antlr.CFGrammar.ExprNotContainsContext;
import ortus.boxlang.parser.antlr.CFGrammar.ExprOrContext;
import ortus.boxlang.parser.antlr.CFGrammar.ExprOutStringContext;
import ortus.boxlang.parser.antlr.CFGrammar.ExprPostfixContext;
import ortus.boxlang.parser.antlr.CFGrammar.ExprPowerContext;
import ortus.boxlang.parser.antlr.CFGrammar.ExprPrecedenceContext;
import ortus.boxlang.parser.antlr.CFGrammar.ExprPrefixContext;
import ortus.boxlang.parser.antlr.CFGrammar.ExprRelationalContext;
import ortus.boxlang.parser.antlr.CFGrammar.ExprStatAnonymousFunctionContext;
import ortus.boxlang.parser.antlr.CFGrammar.ExprStatInvocableContext;
import ortus.boxlang.parser.antlr.CFGrammar.ExprTernaryContext;
import ortus.boxlang.parser.antlr.CFGrammar.ExprUnaryContext;
import ortus.boxlang.parser.antlr.CFGrammar.ExprXorContext;
import ortus.boxlang.parser.antlr.CFGrammar.ExpressionContext;
import ortus.boxlang.parser.antlr.CFGrammar.FinallyBlockContext;
import ortus.boxlang.parser.antlr.CFGrammar.ForContext;
import ortus.boxlang.parser.antlr.CFGrammar.FunctionContext;
import ortus.boxlang.parser.antlr.CFGrammar.FunctionOrStatementContext;
import ortus.boxlang.parser.antlr.CFGrammar.FunctionParamContext;
import ortus.boxlang.parser.antlr.CFGrammar.FunctionParamListContext;
import ortus.boxlang.parser.antlr.CFGrammar.FunctionSignatureContext;
import ortus.boxlang.parser.antlr.CFGrammar.IfContext;
import ortus.boxlang.parser.antlr.CFGrammar.ImportFQNContext;
import ortus.boxlang.parser.antlr.CFGrammar.ImportStatementContext;
import ortus.boxlang.parser.antlr.CFGrammar.IncludeContext;
import ortus.boxlang.parser.antlr.CFGrammar.InterfaceContext;
import ortus.boxlang.parser.antlr.CFGrammar.InvocableContext;
import ortus.boxlang.parser.antlr.CFGrammar.ModifierContext;
import ortus.boxlang.parser.antlr.CFGrammar.NormalStatementBlockContext;
import ortus.boxlang.parser.antlr.CFGrammar.NotContext;
import ortus.boxlang.parser.antlr.CFGrammar.ParamContext;
import ortus.boxlang.parser.antlr.CFGrammar.PostAnnotationContext;
import ortus.boxlang.parser.antlr.CFGrammar.PreAnnotationContext;
import ortus.boxlang.parser.antlr.CFGrammar.PropertyContext;
import ortus.boxlang.parser.antlr.CFGrammar.RethrowContext;
import ortus.boxlang.parser.antlr.CFGrammar.ReturnContext;
import ortus.boxlang.parser.antlr.CFGrammar.ScriptContext;
import ortus.boxlang.parser.antlr.CFGrammar.SimpleStatementContext;
import ortus.boxlang.parser.antlr.CFGrammar.StatementBlockContext;
import ortus.boxlang.parser.antlr.CFGrammar.StatementContext;
import ortus.boxlang.parser.antlr.CFGrammar.StatementOrBlockContext;
import ortus.boxlang.parser.antlr.CFGrammar.StaticInitializerContext;
import ortus.boxlang.parser.antlr.CFGrammar.StructExpressionContext;
import ortus.boxlang.parser.antlr.CFGrammar.SwitchContext;
import ortus.boxlang.parser.antlr.CFGrammar.ThrowContext;
import ortus.boxlang.parser.antlr.CFGrammar.TryContext;
import ortus.boxlang.parser.antlr.CFGrammar.WhileContext;
import ortus.boxlang.parser.antlr.CFGrammarBaseVisitor;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.services.ComponentService;

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
 * analysis and transformations and eventually code generation, should that be the end goal.
 */
@SuppressWarnings( "DuplicatedCode" )
public class CFVisitor extends CFGrammarBaseVisitor<BoxNode> {

	private final CFParser				tools;
	private final CFExpressionVisitor	expressionVisitor;
	public ComponentService				componentService	= BoxRuntime.getInstance().getComponentService();

	public CFVisitor( CFParser tools ) {
		this.tools				= tools;
		this.expressionVisitor	= new CFExpressionVisitor( tools, this );
	}

	/**
	 * Visit the class or interface context to generate the AST node for the
	 * top level node
	 *
	 * @param ctx the parse tree
	 *
	 * @return the AST node representing the class or interface
	 */
	@Override
	public BoxNode visitClassOrInterface( ClassOrInterfaceContext ctx ) {
		// NOTE: ANTLR renames rules that match Java keywords so interface_()
		// is used instead of interface
		return ctx.boxClass() != null ? ctx.boxClass().accept( this ) : ctx.interface_().accept( this );
	}

	@Override
	public BoxNode visitScript( ScriptContext ctx ) {
		// Force the script to start at the top of the file so doc comments associate with functions correctly
		var					pos			= tools.getPositionStartingAt( ctx, tools.getFirstToken() );
		var					src			= tools.getSourceText( ctx );

		List<BoxStatement>	statements	= ctx.functionOrStatement().stream()
		    .map( stmt -> tools.toStatementOrError( () -> ( BoxStatement ) stmt.accept( this ), stmt ) )
		    .collect( Collectors.toList() );
		return new BoxScript( statements, pos, src );
	}

	@Override
	public BoxNode visitImportStatement( ImportStatementContext ctx ) {
		var					pos		= tools.getPosition( ctx );
		var					src		= tools.getSourceText( ctx );

		ImportFQNContext	fqn		= ctx.importFQN();
		String				fqnText	= fqn.getText();
		if ( ( fqnText.startsWith( "\"" ) && fqnText.endsWith( "\"" ) ) || ( fqnText.startsWith( "'" ) && fqnText.endsWith( "'" ) ) ) {
			fqnText = fqnText.substring( 1, fqnText.length() - 1 );
		}
		BoxExpression expr = new BoxFQN( fqnText, tools.getPosition( fqn ), tools.getSourceText( fqn ) );

		return new BoxImport( expr, null, pos, src );
	}

	@Override
	public BoxNode visitInclude( IncludeContext ctx ) {
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
	public BoxNode visitBoxClass( BoxClassContext ctx ) {
		var									pos				= tools.getPositionStartingAt( ctx, ctx.COMPONENT().getSymbol() );
		var									src				= tools.getSourceText( ctx );

		List<BoxStatement>					body			= buildClassBody( ctx.classBody() );
		List<BoxImport>						imports			= new ArrayList<>();
		List<BoxAnnotation>					annotations		= new ArrayList<>();
		List<BoxDocumentationAnnotation>	documentation	= new ArrayList<>();
		List<BoxProperty>					property		= new ArrayList<>();

		processIfNotNull( ctx.importStatement(), stmt -> imports.add( ( BoxImport ) stmt.accept( this ) ) );
		processIfNotNull( ctx.postAnnotation(), a -> annotations.add( ( BoxAnnotation ) a.accept( this ) ) );
		// loop over body list and move any statments of type BoxProperty to the property list
		body.removeIf( stmt -> stmt instanceof BoxProperty prop && property.add( prop ) );

		// Convert abstract keyword to an annotation of null value.
		if ( ctx.ABSTRACT() != null ) {
			annotations.add( new BoxAnnotation( new BoxFQN( "abstract", tools.getPosition( ctx.ABSTRACT() ), ctx.ABSTRACT().getText() ), null,
			    tools.getPosition( ctx.ABSTRACT() ), ctx.ABSTRACT().getText() ) );
		}
		// Convert final keyword to an annotation of null value
		if ( ctx.FINAL() != null ) {
			annotations.add( new BoxAnnotation( new BoxFQN( "final", tools.getPosition( ctx.FINAL() ), ctx.FINAL().getText() ), null,
			    tools.getPosition( ctx.FINAL() ), ctx.FINAL().getText() ) );
		}

		return new BoxClass( imports, body, annotations, documentation, property, pos, src );
	}

	@Override
	public BoxNode visitClassBodyStatement( ClassBodyStatementContext ctx ) {
		return Optional.ofNullable( ctx.staticInitializer() )
		    .map( init -> init.accept( this ) )
		    .or( () -> Optional.ofNullable( ctx.property() ).map( prop -> prop.accept( this ) ) )
		    .orElseGet( () -> ctx.functionOrStatement().accept( this ) );
	}

	@Override
	public BoxNode visitStaticInitializer( StaticInitializerContext ctx ) {
		var					pos		= tools.getPosition( ctx );
		var					src		= tools.getSourceText( ctx );
		List<BoxStatement>	body	= buildStaticBody( ctx.normalStatementBlock() );
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
	public BoxInterface visitInterface( InterfaceContext ctx ) {

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
		processIfNotNull( ctx.postAnnotation(), annotation -> postAnnotations.add( ( BoxAnnotation ) annotation.accept( this ) ) );
		processIfNotNull( ctx.function(), stmt -> body.add( tools.toStatementOrError( () -> ( BoxStatement ) stmt.accept( this ), stmt ) ) );

		return new BoxInterface( imports, body, preAnnotations, postAnnotations, documentation, tools.getPosition( ctx ), tools.getSourceText( ctx ) );
	}

	@Override
	public BoxNode visitStatement( StatementContext ctx ) {
		List<Function<StatementContext, ParserRuleContext>> functions = Arrays.asList( StatementContext::importStatement, StatementContext::do_,
		    StatementContext::for_, StatementContext::if_, StatementContext::switch_, StatementContext::try_, StatementContext::while_,
		    StatementContext::expressionStatement, StatementContext::include, StatementContext::component, StatementContext::statementBlock,
		    StatementContext::simpleStatement, StatementContext::componentIsland, StatementContext::throw_,
		    StatementContext::emptyStatementBlock, StatementContext::function );

		// Iterate over the functions
		for ( Function<StatementContext, ParserRuleContext> function : functions ) {
			ParserRuleContext result = function.apply( ctx );
			if ( result != null ) {
				return result.accept( this );
			}
		}

		// If none of the functions return a non-null result, return null
		return null;
	}

	@Override
	public BoxNode visitNot( NotContext ctx ) {
		var				pos		= tools.getPosition( ctx );
		var				src		= tools.getSourceText( ctx );
		BoxExpression	expr	= ctx.expression().accept( expressionVisitor );
		return new BoxExpressionStatement( new BoxUnaryOperation( expr, BoxUnaryOperator.Not, pos, src ), pos, src );
	}

	@Override
	public BoxNode visitDo( DoContext ctx ) {
		var				pos			= tools.getPosition( ctx );
		var				src			= tools.getSourceText( ctx );
		BoxExpression	condition	= ctx.expression().accept( expressionVisitor );
		BoxStatement	body		= tools.toStatementOrError( () -> ( BoxStatement ) ctx.statementOrBlock().accept( this ), ctx.statementOrBlock() );
		String			label		= Optional.ofNullable( ctx.preFix() ).map( preFix -> preFix.identifier().getText() ).orElse( null );
		return new BoxDo( label, condition, body, pos, src );
	}

	@Override
	public BoxNode visitFor( ForContext ctx ) {
		var					pos			= tools.getPosition( ctx );
		var					src			= tools.getSourceText( ctx );
		BoxStatement		body		= tools.toStatementOrError( () -> ( BoxStatement ) ctx.statementOrBlock().accept( this ), ctx.statementOrBlock() );
		String				label		= Optional.ofNullable( ctx.preFix() ).map( preFix -> preFix.identifier().getText() ).orElse( null );
		List<BoxExpression>	expressions	= Optional.ofNullable( ctx.expression() ).orElse( Collections.emptyList() ).stream()
		    .map( expression -> expression.accept( expressionVisitor ) ).toList();

		// If this is the IN style, then we are guaranteed to have two expressions
		if ( ctx.IN() != null ) {
			return new BoxForIn( label, expressions.get( 0 ), expressions.get( 1 ), body, ctx.VAR() != null, pos, src );
		}

		// Otherwise we have an index with 0 <= n <= 3 expressions
		return new BoxForIndex(
		    label,
		    Optional.ofNullable( ctx.intializer ).map( init -> init.accept( expressionVisitor ) ).orElse( null ),
		    Optional.ofNullable( ctx.condition ).map( init -> init.accept( expressionVisitor ) ).orElse( null ),
		    Optional.ofNullable( ctx.increment ).map( init -> init.accept( expressionVisitor ) ).orElse( null ),
		    body,
		    pos,
		    src
		);
	}

	@Override
	public BoxNode visitIf( IfContext ctx ) {
		var				pos			= tools.getPosition( ctx );
		var				src			= tools.getSourceText( ctx );
		BoxExpression	condition	= ctx.expression().accept( expressionVisitor );
		BoxStatement	thenBody	= tools.toStatementOrError( () -> ( BoxStatement ) ctx.ifStmt.accept( this ), ctx.ifStmt );
		BoxStatement	elseBody	= Optional.ofNullable( ctx.elseStmt )
		    .map( stmt -> tools.toStatementOrError( () -> ( BoxStatement ) stmt.accept( this ), stmt ) ).orElse( null );
		return new BoxIfElse( condition, thenBody, elseBody, pos, src );
	}

	@Override
	public BoxNode visitSwitch( SwitchContext ctx ) {
		var					pos			= tools.getPosition( ctx );
		var					src			= tools.getSourceText( ctx );

		BoxExpression		condition	= ctx.expression().accept( expressionVisitor );
		List<BoxSwitchCase>	cases		= ctx.case_().stream().map( caseBlock -> ( BoxSwitchCase ) caseBlock.accept( this ) ).collect( Collectors.toList() );
		return new BoxSwitch( condition, cases, pos, src );
	}

	@Override
	public BoxNode visitCase( CaseContext ctx ) {
		var					pos			= tools.getPosition( ctx );
		var					src			= tools.getSourceText( ctx );

		// Assign null to condition if ctx.expression() is null, otherwise call accept(expressionVisitor)
		// if null, then this is the DEFAULT: clause
		BoxExpression		condition	= Optional.ofNullable( ctx.expression() ).map( expression -> expression.accept( expressionVisitor ) ).orElse( null );

		// Produce body from iterating ctx.statement() and calling accept(this) on each one,
		// or assign null to body if there are no ctx.statement()
		List<BoxStatement>	body		= Optional.ofNullable( ctx.statementOrBlock() )
		    .map( statements -> statements.stream().map( statement -> tools.toStatementOrError( () -> ( BoxStatement ) statement.accept( this ), statement ) )
		        .collect( Collectors.toList() ) )
		    .orElse( List.of() );

		return new BoxSwitchCase( condition, null, body, pos, src );
	}

	@Override
	public BoxStatement visitTry( TryContext ctx ) {
		var					pos				= tools.getPosition( ctx );
		var					src				= tools.getSourceText( ctx );

		List<BoxStatement>	body			= buildStatementBlock( ctx.normalStatementBlock() );
		List<BoxTryCatch>	catches			= ctx.catches().stream().map( catchBlock -> ( BoxTryCatch ) catchBlock.accept( this ) ).toList();
		List<BoxStatement>	finallyBlock	= Optional.ofNullable( ctx.finallyBlock() ).map( FinallyBlockContext::normalStatementBlock )
		    .map( this::buildStatementBlock ).orElse( List.of() );
		return new BoxTry( body, catches, finallyBlock, pos, src );
	}

	@Override
	public BoxStatement visitCatches( CatchesContext ctx ) {
		var					pos			= tools.getPosition( ctx );
		var					src			= tools.getSourceText( ctx );

		BoxExpression		exception	= ctx.ex.accept( expressionVisitor );
		List<BoxExpression>	catchTypes	= Optional.ofNullable( ctx.ct )
		    .map( ctList -> ctList.stream().map( this::buildCatchType ).collect( Collectors.toList() ) ).orElse( null );
		var					catchBody	= buildStatementBlock( ctx.normalStatementBlock() );

		return new BoxTryCatch( catchTypes, exception, catchBody, pos, src );
	}

	@Override
	public BoxStatement visitWhile( WhileContext ctx ) {
		var				pos			= tools.getPosition( ctx );
		var				src			= tools.getSourceText( ctx );

		BoxExpression	condition	= ctx.expression().accept( expressionVisitor );
		BoxStatement	body		= tools.toStatementOrError( () -> ( BoxStatement ) ctx.statementOrBlock().accept( this ), ctx.statementOrBlock() );
		String			label		= Optional.ofNullable( ctx.preFix() ).map( preFix -> preFix.identifier().getText() ).orElse( null );
		return new BoxWhile( label, condition, body, pos, src );
	}

	@Override
	public BoxNode visitComponent( ComponentContext ctx ) {
		var		pos				= tools.getPosition( ctx );
		var		src				= tools.getSourceText( ctx );
		String	componentName	= null;

		if ( ctx.componentName() != null ) {
			componentName = ctx.componentName().getText();
		} else {
			// strip prefix from name so "cfbrad" becomes "brad". ACF specific tag-in-script syntax
			componentName = ctx.prefixedComponentName().getText().substring( 2 );
		}

		List<BoxAnnotation> attributes = Optional.ofNullable( ctx.componentAttribute() )
		    .map( attributeList -> attributeList.stream().map( attribute -> ( BoxAnnotation ) attribute.accept( this ) ).collect( Collectors.toList() ) )
		    .orElse( Collections.emptyList() );

		attributes = buildComponentAttributes( componentName, attributes, ctx );

		List<BoxStatement> body = null;
		if ( ctx.normalStatementBlock() != null ) {
			body = buildStatementBlock( ctx.normalStatementBlock() );
		}

		// Special check for loop condition to avoid runtime eval
		if ( componentName.equalsIgnoreCase( "loop" ) ) {
			for ( var attr : attributes ) {
				if ( attr.getKey().getValue().equalsIgnoreCase( "condition" ) ) {
					BoxExpression condition = attr.getValue();
					if ( condition instanceof BoxStringLiteral str ) {
						// parse as script expression and update value
						condition = tools.parseCFExpression( str.getValue(), condition.getPosition() );
					}
					BoxExpression newCondition = new BoxClosure( List.of(), List.of(),
					    new BoxReturn( condition, condition.getPosition(), condition.getSourceText() ), condition.getPosition(), condition.getSourceText() );
					attr.setValue( newCondition );
				}
			}
		}

		return new BoxComponent( componentName, attributes, body, 0, pos, src );
	}

	@Override
	public BoxNode visitComponentAttribute( ComponentAttributeContext ctx ) {
		var				pos		= tools.getPosition( ctx );
		var				src		= tools.getSourceText( ctx );

		BoxFQN			name	= new BoxFQN( ctx.identifier().getText(), tools.getPosition( ctx.identifier() ), tools.getSourceText( ctx.identifier() ) );
		BoxExpression	value	= Optional.ofNullable( ctx.expression() ).map( expression -> expression.accept( expressionVisitor ) ).orElse( null );

		return new BoxAnnotation( name, value, pos, src );
	}

	@Override
	public BoxNode visitStatementOrBlock( StatementOrBlockContext ctx ) {
		if ( ctx.emptyStatementBlock() != null ) {
			return ctx.emptyStatementBlock().accept( this );
		}
		return ctx.statement().accept( this );
	}

	@Override
	public BoxNode visitStatementBlock( StatementBlockContext ctx ) {
		return new BoxStatementBlock( buildStatementBlock( ctx ), tools.getPosition( ctx ), tools.getSourceText( ctx ) );
	}

	@Override
	public BoxNode visitNormalStatementBlock( NormalStatementBlockContext ctx ) {
		return new BoxStatementBlock( buildStatementBlock( ctx ), tools.getPosition( ctx ), tools.getSourceText( ctx ) );
	}

	@Override
	public BoxNode visitEmptyStatementBlock( EmptyStatementBlockContext ctx ) {
		return new BoxStatementBlock( new ArrayList<>(), tools.getPosition( ctx ), tools.getSourceText( ctx ) );
	}

	@Override
	public BoxNode visitSimpleStatement( SimpleStatementContext ctx ) {

		List<Function<SimpleStatementContext, ParserRuleContext>> functions = Arrays.asList( SimpleStatementContext::break_, SimpleStatementContext::continue_,
		    SimpleStatementContext::rethrow, SimpleStatementContext::param, SimpleStatementContext::return_, SimpleStatementContext::not );

		// Iterate over the functions
		for ( Function<SimpleStatementContext, ParserRuleContext> function : functions ) {
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
	public BoxNode visitComponentIsland( ComponentIslandContext ctx ) {
		return new BoxTemplateIsland( tools.toAst( null, ctx.template().template_statements() ),
		    tools.getPosition( ctx.template() ), tools.getSourceText( ctx.template() ) );
	}

	@Override
	public BoxNode visitBreak( BreakContext ctx ) {
		var		pos		= tools.getPosition( ctx );
		var		src		= tools.getSourceText( ctx );
		String	label	= Optional.ofNullable( ctx.identifier() ).map( ParseTree::getText ).orElse( null );
		return new BoxBreak( label, pos, src );
	}

	@Override
	public BoxNode visitContinue( ContinueContext ctx ) {
		var		pos		= tools.getPosition( ctx );
		var		src		= tools.getSourceText( ctx );
		String	label	= Optional.ofNullable( ctx.identifier() ).map( ParseTree::getText ).orElse( null );
		return new BoxContinue( label, pos, src );
	}

	@Override
	public BoxNode visitRethrow( RethrowContext ctx ) {
		var	pos	= tools.getPosition( ctx );
		var	src	= tools.getSourceText( ctx );
		return new BoxRethrow( pos, src );
	}

	@Override
	public BoxNode visitParam( ParamContext ctx ) {
		var				pos				= tools.getPosition( ctx );
		var				src				= tools.getSourceText( ctx );

		BoxExpression	type			= null;
		BoxExpression	defaultValue	= null;
		String			accessText;
		BoxExpression	accessExpr;
		if ( ctx.type() != null ) {
			type = new BoxStringLiteral( ctx.type().getText(), tools.getPosition( ctx.type() ), tools.getSourceText( ctx.type() ) );
		}

		var expr = ctx.expressionStatement().accept( expressionVisitor );
		// We have one expression, but if it was an assignment, then we split it into two as the
		// access is the left side and the default value is the right side.
		if ( expr instanceof BoxAssignment assignment ) {

			accessExpr		= assignment.getLeft();
			// When we have an assignment, we want the text of the entire LHS, and not the text of the say dot access
			// expression as in variables.x = n that only yields .x for other reasons. So the easiest way to do
			// that is just to split out the left of the equals.
			accessText		= assignment.getSourceText().split( "=" )[ 0 ];
			defaultValue	= assignment.getRight();
		} else {
			accessExpr	= expr;
			accessText	= ctx.expressionStatement().getText();
		}
		return new BoxParam( new BoxStringLiteral( accessText, accessExpr.getPosition(), accessExpr.getSourceText() ), type, defaultValue, pos, src );
	}

	@Override
	public BoxNode visitReturn( ReturnContext ctx ) {
		var				pos		= tools.getPosition( ctx );
		var				src		= tools.getSourceText( ctx );

		BoxExpression	value	= Optional.ofNullable( ctx.expression() ).map( expression -> expression.accept( expressionVisitor ) ).orElse( null );
		return new BoxReturn( value, pos, src );
	}

	@Override
	public BoxNode visitThrow( ThrowContext ctx ) {
		var				pos		= tools.getPosition( ctx );
		var				src		= tools.getSourceText( ctx );

		BoxExpression	value	= null;
		if ( ctx.expression() != null ) {
			value = ctx.expression().accept( expressionVisitor );
		}
		return new BoxThrow( value, pos, src );
	}

	// ======================================================================
	// Expressions as statements
	//
	// It is often easier to allow expressions to be in the statement rules, then
	// our context tells us whether it is a statement or an expression, such as Assignment
	// for instance.
	public BoxNode visitInvocable( InvocableContext ctx ) {
		return buildExprStat( ctx );
	}

	public BoxNode visitExprStatInvocable( ExprStatInvocableContext ctx ) {
		return buildExprStat( ctx );
	}

	@Override
	public BoxNode visitExprPrecedence( ExprPrecedenceContext ctx ) {
		return buildExprStat( ctx );
	}

	@Override
	public BoxNode visitExprFunctionCall( ExprFunctionCallContext ctx ) {
		// Have the expression builder make the function call for us, then we have to
		// wrap it in a statement, which is what we are doing here.
		return buildExprStat( ctx );
	}

	@Override
	public BoxNode visitExprUnary( ExprUnaryContext ctx ) {
		return buildExprStat( ctx );
	}

	@Override
	public BoxNode visitExprPostfix( ExprPostfixContext ctx ) {
		return buildExprStat( ctx );
	}

	@Override
	public BoxNode visitExprPrefix( ExprPrefixContext ctx ) {
		return buildExprStat( ctx );
	}

	@Override
	public BoxNode visitExprDotFloat( ExprDotFloatContext ctx ) {
		return buildExprStat( ctx );
	}

	@Override
	public BoxNode visitExprDotOrColonAccess( ExprDotOrColonAccessContext ctx ) {
		return buildExprStat( ctx );
	}

	@Override
	public BoxNode visitExprPower( ExprPowerContext ctx ) {
		return buildExprStat( ctx );
	}

	@Override
	public BoxNode visitExprMult( ExprMultContext ctx ) {
		return buildExprStat( ctx );
	}

	@Override
	public BoxNode visitExprAdd( ExprAddContext ctx ) {
		return buildExprStat( ctx );
	}

	@Override
	public BoxNode visitExprBinary( ExprBinaryContext ctx ) {
		return buildExprStat( ctx );
	}

	@Override
	public BoxNode visitExprRelational( ExprRelationalContext ctx ) {
		return buildExprStat( ctx );
	}

	@Override
	public BoxNode visitExprEqual( ExprEqualContext ctx ) {
		return buildExprStat( ctx );
	}

	@Override
	public BoxNode visitExprXor( ExprXorContext ctx ) {
		return buildExprStat( ctx );
	}

	@Override
	public BoxNode visitExprCat( ExprCatContext ctx ) {
		return buildExprStat( ctx );
	}

	@Override
	public BoxNode visitExprNotContains( ExprNotContainsContext ctx ) {
		return buildExprStat( ctx );
	}

	@Override
	public BoxNode visitExprAnd( ExprAndContext ctx ) {
		return buildExprStat( ctx );
	}

	@Override
	public BoxNode visitExprOr( ExprOrContext ctx ) {
		return buildExprStat( ctx );
	}

	@Override
	public BoxNode visitExprElvis( ExprElvisContext ctx ) {
		return buildExprStat( ctx );
	}

	@Override
	public BoxNode visitExprTernary( ExprTernaryContext ctx ) {
		return buildExprStat( ctx );
	}

	/**
	 * Visit the Assign expressions that are actually statements, and treat them as so
	 */
	@Override
	public BoxNode visitExprAssign( ExprAssignContext ctx ) {
		return buildExprStat( ctx );
	}

	@Override
	public BoxNode visitExprOutString( ExprOutStringContext ctx ) {
		return buildExprStat( ctx );
	}

	@Override
	public BoxNode visitExprArrayAccess( ExprArrayAccessContext ctx ) {
		return buildExprStat( ctx );
	}

	@Override
	public BoxNode visitExprArrayLiteral( ExprArrayLiteralContext ctx ) {
		return buildExprStat( ctx );
	}

	@Override
	public BoxNode visitExprStatAnonymousFunction( ExprStatAnonymousFunctionContext ctx ) {
		return buildExprStat( ctx );
	}

	@Override
	public BoxNode visitExprNew( ExprNewContext ctx ) {
		return buildExprStat( ctx );
	}

	@Override
	public BoxNode visitExprIdentifier( ExprIdentifierContext ctx ) {
		return buildExprStat( ctx );
	}

	@Override
	public BoxNode visitExprLiterals( ExprLiteralsContext ctx ) {
		return buildExprStat( ctx );
	}

	@Override
	public BoxNode visitAtoms( AtomsContext ctx ) {
		return buildExprStat( ctx );
	}

	@Override
	public BoxNode visitStructExpression( StructExpressionContext ctx ) {
		return buildExprStat( ctx );
	}

	@Override
	public BoxProperty visitProperty( PropertyContext ctx ) {
		var									pos				= tools.getPosition( ctx );
		var									src				= tools.getSourceText( ctx );

		List<BoxAnnotation>					annotations		= new ArrayList<>();
		List<BoxAnnotation>					postAnnotations	= new ArrayList<>();
		List<BoxDocumentationAnnotation>	documentation	= new ArrayList<>();

		processIfNotNull( ctx.postAnnotation(), p -> postAnnotations.add( ( BoxAnnotation ) p.accept( this ) ) );

		return new BoxProperty( annotations, postAnnotations, documentation, pos, src );
	}

	public BoxAnnotation visitPostAnnotation( PostAnnotationContext ctx ) {
		var				pos		= tools.getPosition( ctx );
		var				src		= tools.getSourceText( ctx );

		// TODO: annotation names can actually have - chars which makes then not a valid FQN. Consider refactoring a dedicated BoxAnnotationName
		// node class since they don't fit as an identifier either, but I don't want them to just be a string literal either.
		BoxFQN			name	= new BoxFQN( ctx.postAnnotationName().getText(), tools.getPosition( ctx.postAnnotationName() ),
		    tools.getSourceText( ctx.postAnnotationName() ) );
		BoxExpression	value	= Optional.ofNullable( ctx.attributeSimple() ).map( attr -> attr.accept( expressionVisitor ) ).orElse( null );

		return new BoxAnnotation( name, value, pos, src );
	}

	public BoxAnnotation visitPreAnnotation( PreAnnotationContext ctx ) {
		var				pos		= tools.getPosition( ctx );
		var				src		= tools.getSourceText( ctx );

		BoxExpression	aName	= ctx.fqn().accept( expressionVisitor );

		BoxExpression	aValue	= null;
		if ( ctx.annotation() != null ) {
			List<BoxExpression> values = ctx.annotation().stream().map( expression -> expression.accept( expressionVisitor ) ).collect( Collectors.toList() );

			if ( values.size() == 1 ) {
				aValue = values.getFirst();
			} else if ( values.size() > 1 ) {
				aValue = new BoxArrayLiteral( values, pos, src );
			}
		}

		return new BoxAnnotation( ( BoxFQN ) aName, aValue, pos, src );
	}

	public BoxNode visitFunction( FunctionContext ctx ) {
		return buildFunction( ctx.postAnnotation(), ctx.functionSignature().identifier().getText(),
		    ctx.functionSignature(), ctx.normalStatementBlock(), ctx );
	}

	public BoxNode visitFunctionParam( FunctionParamContext ctx ) {
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
		for ( PostAnnotationContext annotation : ctx.postAnnotation() ) {
			annotations.add( ( BoxAnnotation ) annotation.accept( this ) );
		}

		return new BoxArgumentDeclaration( required, type, name, expr, annotations, documentation, pos, src );
	}

	@Override
	public BoxNode visitFunctionOrStatement( FunctionOrStatementContext ctx ) {
		return ctx.statement().accept( this );
	}

	public BoxNode visitErrorNode( ErrorNode node ) {
		var err = new BoxStatementError( tools.getPosition( node ), node.getText() );
		tools.reportStatementError( err );
		return err;
	}

	// ======================================================================
	// Builders
	//
	// Builders are used when they are more convenient than the visitor pattern, such as building a list of statements

	public BoxNode buildExprStat( ParserRuleContext ctx ) {
		var				pos		= tools.getPosition( ctx );
		var				src		= tools.getSourceText( ctx );
		BoxExpression	value	= ctx.accept( expressionVisitor );
		return new BoxExpressionStatement( value, pos, src );
	}

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
	private List<BoxAnnotation> buildComponentAttributes( String name, List<BoxAnnotation> attributes, ComponentContext ctx ) {

		if ( !name.equalsIgnoreCase( "param" ) ) {
			return attributes;
		}

		var					pos				= tools.getPosition( ctx );
		List<BoxAnnotation>	newAttributes	= new ArrayList<>();

		// If there is only one attribute, and it is not name= and has a value, then we need to convert it to a name/value pair
		// Ex: param foo="bar";
		// Becomes: param name="foo" default="bar";
		if ( attributes.size() == 1 && !attributes.get( 0 ).getKey().getValue().equalsIgnoreCase( "name" ) && attributes.get( 0 ).getValue() != null ) {
			newAttributes.add( new BoxAnnotation( new BoxFQN( "name", pos, "name" ),
			    new BoxStringLiteral( attributes.getFirst().getKey().getValue(), attributes.getFirst().getKey().getPosition(),
			        attributes.getFirst().getKey().getSourceText() ),
			    attributes.getFirst().getKey().getPosition(), "name=\"" + attributes.getFirst().getKey().getSourceText() + "\"" ) );
			newAttributes
			    .add( new BoxAnnotation( new BoxFQN( "default", attributes.getFirst().getValue().getPosition(), "default" ), attributes.getFirst().getValue(),
			        attributes.getFirst().getValue().getPosition(), "default=" + attributes.getFirst().getValue().getSourceText() ) );

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

	private List<BoxStatement> buildClassBody( ClassBodyContext ctx ) {
		List<BoxStatement> body = new ArrayList<>();
		processIfNotNull( ctx.classBodyStatement(), stmt -> body.add( tools.toStatementOrError( () -> ( BoxStatement ) stmt.accept( this ), stmt ) ) );
		return body;
	}

	private List<BoxStatement> buildStaticBody( NormalStatementBlockContext ctx ) {
		List<BoxStatement> body = new ArrayList<>();
		processIfNotNull( ctx.statement(), stmt -> body.add( tools.toStatementOrError( () -> ( BoxStatement ) stmt.accept( this ), stmt ) ) );
		return body;
	}

	private <T> void processIfNotNull( List<T> list, Consumer<T> consumer ) {
		Optional.ofNullable( list ).ifPresent( l -> l.forEach( consumer ) );
	}

	public <T> T getOrNull( List<T> list, int index ) {
		return ( index >= 0 && index < list.size() ) ? list.get( index ) : null;
	}

	private BoxFunctionDeclaration buildFunction( List<PostAnnotationContext> postAnnotations, String name,
	    FunctionSignatureContext functionSignature, NormalStatementBlockContext statementBlock, ParserRuleContext ctx ) {

		var									pos					= tools.getPosition( ctx );
		var									src					= tools.getSourceText( ctx );

		BoxReturnType						returnType;

		List<BoxArgumentDeclaration>		args				= new ArrayList<>();
		List<BoxAnnotation>					annotations			= new ArrayList<>();
		List<BoxDocumentationAnnotation>	documentation		= new ArrayList<>();
		List<BoxAnnotation>					annToRemove			= new ArrayList<>();
		List<BoxMethodDeclarationModifier>	modifiers			= new ArrayList<>();
		BoxAccessModifier					visibility			= null;
		List<BoxStatement>					body;

		List<ModifierContext>				modifierContexts	= Optional.ofNullable( functionSignature.modifier() ).orElse( Collections.emptyList() );

		// Resolve modifiers and access qualifiers, in any order
		for ( ModifierContext modifierContext : modifierContexts ) {
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

		// Accumulate post annotations
		Optional.ofNullable( postAnnotations ).orElse( Collections.emptyList() ).stream().map( annotation -> ( BoxAnnotation ) annotation.accept( this ) )
		    .forEach( annotations::add );

		// Resolve annotations with parameters
		Optional.ofNullable( functionSignature.functionParamList() ).map( FunctionParamListContext::functionParam ).orElse( Collections.emptyList() )
		    .forEach( arg -> {
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

		// If body is null, it indicates an abstract function otherwise the presence of
		// a body indicates a function declaration, even with no statements in it.
		body		= Optional.ofNullable( statementBlock ).map( this::buildStatementBlock ).orElse( null );

		annotations.removeAll( annToRemove );
		return new BoxFunctionDeclaration( visibility, modifiers, name, returnType, args, annotations, documentation, body, pos, src );
	}

	private void buildAnnotations( BoxArgumentDeclaration argDeclaration, List<BoxAnnotation> annotations, List<BoxAnnotation> annToRemove ) {
		annotations.stream().filter( pre -> pre.getKey().getValue().toLowerCase().startsWith( argDeclaration.getName().toLowerCase() + "." ) ).forEach( pre -> {
			String	preName	= pre.getKey().getValue();
			BoxFQN	key		= new BoxFQN( preName.substring( pre.getKey().getValue().indexOf( "." ) + 1 ), pre.getPosition(), pre.getSourceText() );
			argDeclaration.getAnnotations().add( new BoxAnnotation( key, pre.getValue(), pre.getPosition(), pre.getSourceText() ) );
			annToRemove.add( pre );
		} );
	}

	private List<BoxStatement> buildStatementBlock( StatementBlockContext statementBlock ) {
		return Optional.ofNullable( statementBlock ).map( StatementBlockContext::statement ).orElse( Collections.emptyList() ).stream()
		    .map( statement -> tools.toStatementOrError( () -> ( BoxStatement ) statement.accept( this ), statement ) )
		    // How would a BoxNull ever be at the top level of a statement block?
		    // .filter( boxNode -> ! ( boxNode instanceof BoxNull ) ).map( boxNode -> ( BoxStatement ) boxNode )
		    .collect( Collectors.toList() );
	}

	private List<BoxStatement> buildStatementBlock( NormalStatementBlockContext statementBlock ) {
		return Optional.ofNullable( statementBlock ).map( NormalStatementBlockContext::statement ).orElse( Collections.emptyList() ).stream()
		    .map( statement -> tools.toStatementOrError( () -> ( BoxStatement ) statement.accept( this ), statement ) )
		    // How would a BoxNull ever be at the top level of a statement block?
		    // .filter( boxNode -> ! ( boxNode instanceof BoxNull ) ).map( boxNode -> ( BoxStatement ) boxNode )
		    .collect( Collectors.toList() );
	}

	private BoxExpression buildCatchType( ExpressionContext ctx ) {
		var expr = ctx.accept( expressionVisitor );
		if ( expr instanceof BoxIdentifier ) {
			return new BoxFQN( ( ( BoxIdentifier ) expr ).getName(), expr.getPosition(), expr.getSourceText() );
		} else if ( expr instanceof BoxDotAccess ) {
			return new BoxFQN( ctx.getText(), expr.getPosition(), ctx.getText() );
		} else {
			// Can only be string here, but we assume the semantic verifier has checked this
			return expr;
		}
	}

}