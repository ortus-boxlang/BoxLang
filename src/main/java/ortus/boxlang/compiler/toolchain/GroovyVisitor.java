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
import ortus.boxlang.compiler.ast.expression.BoxArgument;
import ortus.boxlang.compiler.ast.expression.BoxArrayAccess;
import ortus.boxlang.compiler.ast.expression.BoxArrayDestructuringBinding;
import ortus.boxlang.compiler.ast.expression.BoxArrayDestructuringPattern;
import ortus.boxlang.compiler.ast.expression.BoxArrayLiteral;
import ortus.boxlang.compiler.ast.expression.BoxAssignment;
import ortus.boxlang.compiler.ast.expression.BoxAssignmentOperator;
import ortus.boxlang.compiler.ast.expression.BoxBinaryOperation;
import ortus.boxlang.compiler.ast.expression.BoxBinaryOperator;
import ortus.boxlang.compiler.ast.expression.BoxBooleanLiteral;
import ortus.boxlang.compiler.ast.expression.BoxComparisonOperation;
import ortus.boxlang.compiler.ast.expression.BoxComparisonOperator;
import ortus.boxlang.compiler.ast.expression.BoxFQN;
import ortus.boxlang.compiler.ast.expression.BoxFunctionInvocation;
import ortus.boxlang.compiler.ast.expression.BoxIdentifier;
import ortus.boxlang.compiler.ast.expression.BoxIntegerLiteral;
import ortus.boxlang.compiler.ast.expression.BoxMethodInvocation;
import ortus.boxlang.compiler.ast.expression.BoxStringLiteral;
import ortus.boxlang.compiler.ast.expression.BoxStructLiteral;
import ortus.boxlang.compiler.ast.expression.BoxStructType;
import ortus.boxlang.compiler.ast.expression.BoxUnaryOperation;
import ortus.boxlang.compiler.ast.expression.BoxUnaryOperator;
import ortus.boxlang.compiler.ast.expression.BoxNull;
import ortus.boxlang.compiler.ast.statement.BoxAccessModifier;
import ortus.boxlang.compiler.ast.statement.BoxAnnotation;
import ortus.boxlang.compiler.ast.statement.BoxArgumentDeclaration;
import ortus.boxlang.compiler.ast.statement.BoxAssert;
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
import ortus.boxlang.compiler.ast.statement.BoxSwitch;
import ortus.boxlang.compiler.ast.statement.BoxSwitchCase;
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
import ortus.boxlang.parser.antlr.GroovyGrammar.EnumDeclarationContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.ExprStatementContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.FieldDeclarationContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.ForInControlContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.ForStatementContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.IfStatementContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.LabeledStatementContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.MethodDeclarationContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.ParameterContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.ParameterListContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.AssertStatementContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.CaseClauseContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.DefaultClauseContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.ReturnStatementContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.SwitchStatementContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.ThrowStatementContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.TryStatementContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.TupleDeclStatementContext;
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
		var					pos			= tools.getPosition( ctx );
		var					src			= tools.getSourceText( ctx );
		List<BoxAnnotation>	annotations	= buildInheritanceAnnotations( ctx, pos, src );
		List<BoxStatement>	body		= buildClassMemberBody( ctx );
		return new BoxClass( imports, body, annotations, List.of(), List.of(), pos, src, BoxSourceType.GROOVYSCRIPT );
	}

	// Groovy's anonymous inner class (new Runnable() { void run() {...} }) - see
	// GroovyExpressionVisitor#visitNewInstanceExpr for how the caller reaches this and why. Builds
	// a synthetically-named BoxLocalClass exactly like an explicit nested classDeclaration would.
	// <p>
	// Deliberately does NOT emit an "implements" annotation for the named type: confirmed
	// empirically that BoxClass's own "implements" resolution requires the resolved type to be a
	// real BoxInterface (a BoxLang-native interface), and throws a ClassCastException for an
	// arbitrary JDK interface like java.lang.Runnable - which is precisely the classic anonymous-
	// class idiom this feature exists for. This is still a plain BoxLocalClass with no declared
	// supertype at all - real Java-interface conformance (so the CONSTRUCTED INSTANCE is genuinely
	// passable to Java code expecting that exact type, not just duck-typed) is instead handled one
	// layer up, by wrapping the instance in a real JDK dynamic proxy via createDynamicProxy() when
	// the type name is confidently a known Java interface - see GroovyExpressionVisitor#
	// visitNewInstanceExpr/resolveJavaInterfaceFqn for the full reasoning and its own scope
	// boundary (a curated interface-name set, not general symbol resolution).
	BoxStatement buildAnonymousLocalClass( BoxIdentifier name,
	    ortus.boxlang.parser.antlr.GroovyGrammar.ClassBodyContext bodyCtx, Position pos, String src ) {
		// buildClassMemberBody pushes/pops its own isolated hoist-scope frame around bodyCtx, so
		// anonymous classes discovered while building THIS class's body land inside it, while any
		// still pending from an enclosing/sibling scope are left untouched - see
		// GroovyExpressionVisitor#pushHoistScope for the full reasoning.
		List<BoxStatement> body = bodyCtx == null ? new ArrayList<>() : buildClassMemberBody( bodyCtx );
		return new ortus.boxlang.compiler.ast.statement.BoxLocalClass( name, body, List.of(), List.of(), List.of(), pos, src,
		    BoxSourceType.GROOVYSCRIPT );
	}

	// A class/interface/trait declaration found INSIDE another class's body (as opposed to the
	// top-level, single-class-per-file case GroovyParser#toAst dispatches directly to buildClass())
	// reaches here via classMember's generic visitor dispatch. Builds a BoxLocalClass instead of a
	// top-level BoxClass - the exact same "named class defined inline, scoped to the enclosing
	// file/class" AST node native BoxLang's own nested-class syntax already produces (see
	// BoxVisitor#visitLocalClass) - so no new AST/runtime machinery is needed. This models a Java-
	// style STATIC nested class (a peer type instantiated via "new Name()", with no implicit
	// reference back to an outer instance); real Groovy's non-static inner classes (which capture
	// an enclosing instance) are NOT modeled - the same bounded scope BoxLocalClass itself has.
	@Override
	public BoxNode visitClassDeclaration( ClassDeclarationContext ctx ) {
		var					pos			= tools.getPosition( ctx );
		var					src			= tools.getSourceText( ctx );
		BoxIdentifier		name		= new BoxIdentifier( ctx.IDENTIFIER().getText(), tools.getPosition( ctx.IDENTIFIER().getSymbol() ),
		    ctx.IDENTIFIER().getText() );
		List<BoxAnnotation>	annotations	= buildInheritanceAnnotations( ctx, pos, src );
		List<BoxStatement>	body		= buildClassMemberBody( ctx );
		return new ortus.boxlang.compiler.ast.statement.BoxLocalClass( name, body, annotations, List.of(), List.of(), pos, src,
		    BoxSourceType.GROOVYSCRIPT );
	}

	// "static { ... }" class initializer block - maps directly onto BoxLang's own native
	// BoxStaticInitializer AST node, the exact same construct BoxGrammar's own "static { ... }"
	// class member already produces (see BoxVisitor#visitStaticInitializer).
	@Override
	public BoxNode visitStaticInitializer( ortus.boxlang.parser.antlr.GroovyGrammar.StaticInitializerContext ctx ) {
		var					pos		= tools.getPosition( ctx );
		var					src		= tools.getSourceText( ctx );
		List<BoxStatement>	body	= buildStatementList( ctx.block().blockStatements() );
		return new ortus.boxlang.compiler.ast.BoxStaticInitializer( body, pos, src );
	}

	private List<BoxStatement> buildClassMemberBody( ClassDeclarationContext ctx ) {
		return ctx.classBody() == null ? new ArrayList<>() : buildClassMemberBody( ctx.classBody() );
	}

	// Shared by every class-shaped body: a top-level class, an explicit nested classDeclaration,
	// and a synthesized anonymous class - see buildAnonymousLocalClass. Drains any anonymous
	// inner classes discovered anywhere within THIS body (including inside its own methods) and
	// appends them as peer members here, once the body itself is fully built - this is the
	// nearest enclosing scope that is never a function/closure/lambda, which is exactly what
	// BoxLocalClass's own hard compiler rule requires (see GroovyExpressionVisitor#
	// visitNewInstanceExpr for the full reasoning).
	private List<BoxStatement> buildClassMemberBody( ortus.boxlang.parser.antlr.GroovyGrammar.ClassBodyContext classBodyCtx ) {
		// Push a fresh hoist-scope frame before building any of this body's own members (including
		// nested class declarations, which build their own body - and therefore their own frame -
		// recursively): anonymous classes discovered while building THIS body must never be confused
		// with ones still pending from an enclosing or earlier-sibling scope. See
		// GroovyExpressionVisitor#pushHoistScope for the full reasoning.
		expressionVisitor.pushHoistScope();
		List<BoxStatement>		body					= new ArrayList<>();
		java.util.Set<String>	userDeclaredMethodNames	= classBodyCtx.classMember().stream()
		    .filter( m -> m.methodDeclaration() != null )
		    .map( m -> m.methodDeclaration().IDENTIFIER().getText().toLowerCase() )
		    .collect( java.util.stream.Collectors.toSet() );

		for ( var member : classBodyCtx.classMember() ) {
			body.add( ( BoxStatement ) member.accept( this ) );
			if ( member.fieldDeclaration() != null ) {
				body.addAll( buildFieldAccessors( member.fieldDeclaration().IDENTIFIER().getText(), userDeclaredMethodNames,
				    tools.getPosition( member.fieldDeclaration() ), tools.getSourceText( member.fieldDeclaration() ) ) );
			}
		}
		body.addAll( expressionVisitor.popHoistScope() );
		return body;
	}

	/**
	 * Wires up "extends"/"implements" as annotations on the class, using the exact key names
	 * ({@code BoxClassTransformer} looks for an annotation literally named "extends"/
	 * "implements" with a {@code BoxStringLiteral} value - the same convention CF's
	 * {@code component extends="Foo" implements="IBar,IBaz"} attribute syntax uses) rather than
	 * a dedicated AST field, since that's what the shared compiler pipeline actually reads.
	 */
	private List<BoxAnnotation> buildInheritanceAnnotations( ClassDeclarationContext ctx, Position pos, String src ) {
		List<BoxAnnotation> annotations = new ArrayList<>();
		if ( ctx.EXTENDS() != null ) {
			String superclassName = ctx.typeName().getText();
			annotations.add( new BoxAnnotation( new BoxFQN( "extends", pos, "extends" ),
			    new BoxStringLiteral( superclassName, pos, superclassName ), pos, src ) );
		}
		if ( ctx.IMPLEMENTS() != null ) {
			String interfaceList = ctx.typeList().typeName().stream().map( org.antlr.v4.runtime.RuleContext::getText )
			    .collect( java.util.stream.Collectors.joining( "," ) );
			annotations.add( new BoxAnnotation( new BoxFQN( "implements", pos, "implements" ),
			    new BoxStringLiteral( interfaceList, pos, interfaceList ), pos, src ) );
		}
		// abstract/final on the class itself (e.g. "abstract class Foo {}") - flag-style
		// annotations with a null value, mirroring how CFVisitor.visitBoxClass encodes the
		// same two modifiers for CF's `abstract component {}` / `final component {}`.
		for ( ClassModifierContext modCtx : ctx.classModifier() ) {
			if ( modCtx.ABSTRACT() != null ) {
				annotations.add( new BoxAnnotation( new BoxFQN( "abstract", pos, "abstract" ), null, pos, src ) );
			} else if ( modCtx.FINAL() != null ) {
				annotations.add( new BoxAnnotation( new BoxFQN( "final", pos, "final" ), null, pos, src ) );
			}
		}
		return annotations;
	}

	/**
	 * Synthesizes get&lt;Name&gt;/set&lt;Name&gt; accessor methods for a Groovy field, mirroring
	 * real Groovy's default behavior (an unmodified field gets an implicit public accessor
	 * pair) and giving the field a defined path for external access - since BoxLang's external
	 * {@code dereference} only sees declared members (methods/properties), not the private
	 * "variables" scope a bare field assignment writes into. Skipped when the class already
	 * declares a method with that exact name, so explicit user-written accessors always win.
	 */
	private List<BoxStatement> buildFieldAccessors( String fieldName, java.util.Set<String> userDeclaredMethodNames, Position pos, String src ) {
		List<BoxStatement>	accessors	= new ArrayList<>();
		String				capitalized	= Character.toUpperCase( fieldName.charAt( 0 ) ) + fieldName.substring( 1 );
		String				getterName	= "get" + capitalized;
		String				setterName	= "set" + capitalized;

		if ( !userDeclaredMethodNames.contains( getterName.toLowerCase() ) ) {
			BoxReturn returnStmt = new BoxReturn( new BoxIdentifier( fieldName, pos, fieldName ), pos, src );
			accessors.add( new BoxFunctionDeclaration( BoxAccessModifier.Public, List.of(), getterName, null, List.of(), List.of(), List.of(),
			    List.of( returnStmt ), pos, src ) );
		}
		if ( !userDeclaredMethodNames.contains( setterName.toLowerCase() ) ) {
			BoxArgumentDeclaration	param		= new BoxArgumentDeclaration( true, "Any", "value", null, List.of(), List.of(), pos, src );
			BoxAssignment			assign		= new BoxAssignment( new BoxIdentifier( fieldName, pos, fieldName ), BoxAssignmentOperator.Equal,
			    new BoxIdentifier( "value", pos, "value" ), List.of(), pos, src );
			BoxStatement			assignStmt	= new BoxExpressionStatement( assign, pos, src );
			accessors.add( new BoxFunctionDeclaration( BoxAccessModifier.Public, List.of(), setterName, null, List.of( param ), List.of(), List.of(),
			    List.of( assignStmt ), pos, src ) );
		}
		return accessors;
	}

	@Override
	public BoxNode visitFieldDeclaration( FieldDeclarationContext ctx ) {
		var				pos			= tools.getPosition( ctx );
		var				src			= tools.getSourceText( ctx );
		String			name		= ctx.IDENTIFIER().getText();
		// A Groovy field becomes a bare (variables-scoped) assignment executed in the class's
		// pseudo-constructor. Bare/unscoped is deliberate: it's what makes the field readable
		// as a plain identifier from inside the class's own methods (BoxLang's default
		// unscoped-identifier lookup chain covers "variables", not "this"). External
		// visibility is handled separately - buildClass() synthesizes get<Name>/set<Name>
		// accessors alongside this statement, mirroring both real Groovy's default
		// (unmodified fields get an implicit public accessor pair) and BoxLang's own
		// `property` mechanism, without needing to replicate BoxProperty's CF-annotation model.
		//
		// NOTE on "static": deliberately NOT modeled. Marking just this declaration statement
		// with BoxAssignmentModifier.STATIC only initializes BoxLang's static scope - it does
		// NOT make bare references to the field elsewhere in the class resolve there too.
		// Verified empirically: native BoxLang itself requires every read/write of a static
		// member to be explicitly scope-qualified ("static.total", never bare "total"), even
		// inside the declaring class's own methods. Real Groovy has no such requirement (a
		// static field reads/writes exactly like an instance one, bare, from anywhere in the
		// class). Doing this correctly needs a symbol-table pass that rewrites every bare
		// reference to a known static-field name throughout the whole class body into an
		// explicit static.<name> access - out of scope for now; a `static` modifier here is
		// silently treated the same as an unmodified field (instance-scoped, not shared).
		BoxIdentifier	target		= new BoxIdentifier( name, tools.getPosition( ctx.IDENTIFIER().getSymbol() ), name );
		BoxExpression	value		= ctx.expression() != null ? ctx.expression().accept( expressionVisitor ) : new BoxNull( pos, src );
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
		body = withVarargsPreamble( ctx.parameterList(), body, pos, src );

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
		// No implicit return here, unlike regular methods/closures: a constructor's job is to
		// initialize the instance, not produce a value, and invokeConstructor() expects "init"
		// to behave that way. Applying implicit-return to the last statement (e.g. a trailing
		// field assignment like "count = 10") would make init() return that value instead of
		// the constructed instance, breaking DynamicObject...invokeConstructor().
		List<BoxStatement>				body	= buildStatementList( ctx.block().blockStatements() );
		body = withVarargsPreamble( ctx.parameterList(), body, pos, src );

		// BoxLang convention (mirroring CFML): the constructor is the method named "init".
		return new BoxFunctionDeclaration( visibility, modifiers, "init", null, args, List.of(), List.of(), body, pos, src );
	}

	BoxArgumentDeclaration buildParameterDeclaration( ParameterContext ctx ) {
		var				pos				= tools.getPosition( ctx );
		var				src				= tools.getSourceText( ctx );
		// A variadic parameter ("int... nums") is still declared as one ordinary named argument
		// here - see buildVarargsPreamble for how it actually ends up bound to an array of every
		// extra positional argument the caller passed.
		String			type			= ctx.typeName() != null ? ctx.typeName().getText() : "Any";
		BoxExpression	defaultValue	= ctx.expression() != null ? ctx.expression().accept( expressionVisitor ) : null;
		// A variadic parameter must be callable with zero trailing arguments ("sum()" is valid
		// Groovy for "def sum(int... nums)"), so it can never be required, regardless of whether
		// an explicit default was also written.
		boolean			required		= defaultValue == null && ctx.ELLIPSIS() == null;
		return new BoxArgumentDeclaration( required, type, ctx.IDENTIFIER().getText(), defaultValue, List.of(), List.of(), pos, src );
	}

	// Groovy's "Type... name" variadic trailing parameter has no equivalent in BoxLang's own
	// function-declaration model (which has no first-class "collect the rest into an array"
	// parameter kind). Rather than add that to BoxLang's core function machinery - a much bigger
	// change than this feature needs - it's desugared entirely within the method body: the
	// parameter is declared as an ordinary single argument, and a synthesized preamble
	// overwrites it with every positional argument from its own declared position onward,
	// collected from the "arguments" scope (which, like CFML, holds every argument actually
	// passed regardless of how many were declared). Equivalent to:
	// __groovyVarargsCollected = []
	// if ( !( arguments.len() == <declaredPosition> && isNull( arguments[ <declaredPosition> ] ) ) ) {
	// __groovyVarargsIndex = <declaredPosition>
	// while ( __groovyVarargsIndex <= arguments.len() ) {
	// __groovyVarargsCollected.append( arguments[ __groovyVarargsIndex ] )
	// __groovyVarargsIndex = __groovyVarargsIndex + 1
	// }
	// }
	// nums = __groovyVarargsCollected
	// The "if" guard exists because arguments.len() counts a declared-but-not-passed parameter
	// as present (bound to null) rather than absent - confirmed empirically: calling a one-
	// parameter function with zero arguments still reports arguments.len() == 1. Without the
	// guard, "sum()" against "def sum(int... nums)" would collect a phantom [null] instead of
	// [] - the isNull check on that exact slot filters it back out. The one known edge case this
	// can't distinguish: explicitly passing a literal null as the SOLE variadic value
	// ("sum(null)") is indistinguishable from passing nothing, and is treated as the latter - an
	// accepted, documented limitation given how rare that call shape is.
	// Only positional calls are collected correctly this way - a named-argument call style isn't
	// handled - and only the LAST parameter being variadic makes sense, matching real Groovy.
	private List<BoxStatement> withVarargsPreamble( ParameterListContext parameterListCtx, List<BoxStatement> body, Position pos, String src ) {
		if ( body == null || parameterListCtx == null ) {
			return body;
		}
		List<ParameterContext> params = parameterListCtx.parameter();
		if ( params.isEmpty() ) {
			return body;
		}
		ParameterContext lastParam = params.get( params.size() - 1 );
		if ( lastParam.ELLIPSIS() == null ) {
			return body;
		}

		String				varargsName			= lastParam.IDENTIFIER().getText();
		int					declaredPosition	= params.size();
		String				counterName			= "__groovyVarargsIndex";
		// Collect into a SEPARATE temp variable, not directly into the parameter itself: a named
		// parameter and its "arguments[N]" slot are the same underlying storage in BoxLang, so
		// resetting the parameter to [] before the loop finishes reading "arguments" would make
		// arguments[declaredPosition] alias the very array being built - each append would then
		// read the array's own (still-growing) current state back into itself, corrupting it
		// into a self-referential array (confirmed via a StackOverflowError in Array.toString()).
		// Only overwriting the real parameter once, after every "arguments" read is done, avoids
		// that entirely.
		String				tempName			= "__groovyVarargsCollected";

		BoxExpression		argumentsLen		= new BoxMethodInvocation( new BoxIdentifier( "len", pos, "len" ),
		    new BoxIdentifier( "arguments", pos, "arguments" ),
		    List.of(), false, true, pos, src );
		BoxExpression		firstVarargSlot		= new BoxArrayAccess( new BoxIdentifier( "arguments", pos, "arguments" ), false,
		    new BoxIntegerLiteral( String.valueOf( declaredPosition ), pos, src ), pos, src );
		BoxExpression		nothingPassed		= new BoxBinaryOperation(
		    new BoxComparisonOperation( argumentsLen, BoxComparisonOperator.Equal, new BoxIntegerLiteral( String.valueOf( declaredPosition ), pos, src ), pos,
		        src ),
		    BoxBinaryOperator.And,
		    new BoxFunctionInvocation( "isNull", List.of( new BoxArgument( firstVarargSlot, pos, src ) ), pos, src ),
		    pos, src );

		BoxExpression		tempInit			= new BoxAssignment( new BoxIdentifier( tempName, pos, tempName ), BoxAssignmentOperator.Equal,
		    new BoxArrayLiteral( List.of(), pos, src ), List.of(), pos, src );
		BoxExpression		counterInit			= new BoxAssignment( new BoxIdentifier( counterName, pos, counterName ), BoxAssignmentOperator.Equal,
		    new BoxIntegerLiteral( String.valueOf( declaredPosition ), pos, src ), List.of(), pos, src );
		BoxExpression		condition			= new BoxComparisonOperation( new BoxIdentifier( counterName, pos, counterName ),
		    BoxComparisonOperator.LessThanEquals,
		    argumentsLen, pos, src );

		BoxExpression		currentArg			= new BoxArrayAccess( new BoxIdentifier( "arguments", pos, "arguments" ), false,
		    new BoxIdentifier( counterName, pos, counterName ), pos, src );
		BoxExpression		appendCall			= new BoxMethodInvocation( new BoxIdentifier( "append", pos, "append" ),
		    new BoxIdentifier( tempName, pos, tempName ),
		    List.of( new BoxArgument( currentArg, pos, src ) ), false, true, pos, src );
		BoxExpression		counterIncrement	= new BoxAssignment( new BoxIdentifier( counterName, pos, counterName ), BoxAssignmentOperator.Equal,
		    new BoxBinaryOperation( new BoxIdentifier( counterName, pos, counterName ), BoxBinaryOperator.Plus, new BoxIntegerLiteral( "1", pos, src ), pos,
		        src ),
		    List.of(), pos, src );

		BoxStatement		loopBody			= new BoxStatementBlock( List.of(
		    new BoxExpressionStatement( appendCall, pos, src ),
		    new BoxExpressionStatement( counterIncrement, pos, src ) ), pos, src );

		BoxStatement		collectBlock		= new BoxStatementBlock( List.of(
		    new BoxExpressionStatement( counterInit, pos, src ),
		    new BoxWhile( null, condition, loopBody, pos, src ) ), pos, src );

		BoxExpression		varargsAssign		= new BoxAssignment( new BoxIdentifier( varargsName, pos, varargsName ), BoxAssignmentOperator.Equal,
		    new BoxIdentifier( tempName, pos, tempName ), List.of(), pos, src );

		List<BoxStatement>	preamble			= List.of(
		    new BoxExpressionStatement( tempInit, pos, src ),
		    new BoxIfElse( new BoxUnaryOperation( nothingPassed, BoxUnaryOperator.Not, pos, src ), collectBlock, null, pos, src ),
		    new BoxExpressionStatement( varargsAssign, pos, src ) );

		List<BoxStatement>	newBody				= new ArrayList<>( preamble );
		newBody.addAll( body );
		return newBody;
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

	@Override
	public BoxNode visitLabeledStatement( LabeledStatementContext ctx ) {
		String	label	= ctx.IDENTIFIER().getText();
		BoxNode	inner	= ctx.statement().accept( this );
		// Groovy allows labeling any statement, but only loops actually use the label (for
		// labeled break/continue) - BoxWhile/BoxForIn/BoxForIndex/BoxDo all expose setLabel().
		// Anything else just has its label silently ignored, matching the fact that BoxBreak/
		// BoxContinue can only ever target a loop label in the first place.
		if ( inner instanceof BoxWhile w ) {
			w.setLabel( label );
		} else if ( inner instanceof BoxForIn f ) {
			f.setLabel( label );
		} else if ( inner instanceof BoxForIndex f ) {
			f.setLabel( label );
		} else if ( inner instanceof BoxDo d ) {
			d.setLabel( label );
		}
		return inner;
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

	// Groovy's "def (a, b) = [1, 2]" tuple declaration reuses BoxLang's own native array-
	// destructuring assignment AST (BoxArrayDestructuringPattern/BoxArrayDestructuringBinding) -
	// the exact same node shape BoxParser's own "[a, b] = expr" syntax produces - so the shared
	// BoxAssignmentTransformer (asm/java) already knows how to compile it via ArrayDestructurer,
	// with no Groovy-specific runtime handling needed. Only simple identifier targets are built
	// here (no nested patterns, defaults, or rest capture - Groovy's own tuple syntax doesn't
	// have those forms either).
	@Override
	public BoxNode visitTupleDeclStatement( TupleDeclStatementContext ctx ) {
		return buildDestructuringAssign( ctx.IDENTIFIER(), ctx.expression(), tools.getPosition( ctx ), tools.getSourceText( ctx ) );
	}

	// Groovy also allows re-assigning already-declared variables via the same tuple syntax, just
	// without the leading "def" - e.g. "(a, b) = [b, a]" to swap two existing variables. Shares the
	// exact same destructuring plumbing as the "def (...)" declaration form above; the only
	// difference is the absence of DEF in the grammar alternative that reaches here.
	@Override
	public BoxNode visitDestructuringAssignStatement( ortus.boxlang.parser.antlr.GroovyGrammar.DestructuringAssignStatementContext ctx ) {
		return buildDestructuringAssign( ctx.IDENTIFIER(), ctx.expression(), tools.getPosition( ctx ), tools.getSourceText( ctx ) );
	}

	private BoxNode buildDestructuringAssign( List<org.antlr.v4.runtime.tree.TerminalNode> identifiers,
	    ortus.boxlang.parser.antlr.GroovyGrammar.ExpressionContext valueCtx, Position pos, String src ) {
		List<BoxArrayDestructuringBinding>	bindings	= identifiers.stream()
		    .map( id -> new BoxArrayDestructuringBinding(
		        new BoxIdentifier( id.getText(), tools.getPosition( id.getSymbol() ), id.getText() ),
		        null, null, false, tools.getPosition( id.getSymbol() ), id.getText() ) )
		    .collect( Collectors.toList() );
		BoxArrayDestructuringPattern		pattern		= new BoxArrayDestructuringPattern( bindings, pos, src );
		BoxExpression						value		= valueCtx.accept( expressionVisitor );
		return new BoxExpressionStatement( new BoxAssignment( pattern, BoxAssignmentOperator.Equal, value, List.of(), pos, src ), pos, src );
	}

	// "enum Color { RED, GREEN, BLUE }" desugars to a struct-of-constants assignment:
	// Color = { RED: <constant>, GREEN: <constant>, BLUE: <constant>, values: [<constant>, ...] }
	// where each <constant> is a real ortus.boxlang.runtime.types.GroovyEnumValue instance (own
	// name + ordinal), not a plain string - giving real type identity and ordinal()/name() member
	// calls (dispatched generically via BoxLang's Java interop, since it's a plain Java object)
	// without needing BoxLang's own class system to support static constant fields, which prior
	// investigation in this codebase found disproportionately expensive (see
	// visitFieldDeclaration's "static" no-op notes). GroovyEnumValue is deliberately still
	// comparable/interchangeable with a plain string (see its own class header) so every pattern
	// the previous plain-string desugaring supported - Color.RED, "==" against a string literal,
	// string interpolation, and (since BoxSwitch is equality-only anyway) "switch (x) { case
	// Color.RED: ... }" - keeps working unchanged. What is still NOT modeled: nesting an enum
	// inside a class body (top-level only).
	private static final String GROOVY_ENUM_VALUE_FQN = "ortus.boxlang.runtime.types.GroovyEnumValue";

	@Override
	public BoxNode visitEnumDeclaration( EnumDeclarationContext ctx ) {
		var					pos				= tools.getPosition( ctx );
		var					src				= tools.getSourceText( ctx );
		String				enumTypeName	= ctx.IDENTIFIER( 0 ).getText();
		BoxIdentifier		enumName		= new BoxIdentifier( enumTypeName, tools.getPosition( ctx.IDENTIFIER( 0 ).getSymbol() ), enumTypeName );

		List<BoxExpression>	entries			= new ArrayList<>();
		List<BoxExpression>	constantValues	= new ArrayList<>();
		for ( int i = 1; i < ctx.IDENTIFIER().size(); i++ ) {
			String	constantName	= ctx.IDENTIFIER( i ).getText();
			int		ordinal			= i - 1;
			var		constantPos		= tools.getPosition( ctx.IDENTIFIER( i ).getSymbol() );
			entries.add( new BoxStringLiteral( constantName, constantPos, constantName ) );
			entries.add( buildEnumValueOf( enumTypeName, constantName, ordinal, constantPos, src ) );
			constantValues.add( buildEnumValueOf( enumTypeName, constantName, ordinal, constantPos, src ) );
		}
		entries.add( new BoxStringLiteral( "values", pos, "values" ) );
		entries.add( new BoxArrayLiteral( constantValues, pos, src ) );

		BoxExpression structLiteral = new BoxStructLiteral( BoxStructType.Ordered, entries, pos, src );
		return new BoxExpressionStatement( new BoxAssignment( enumName, BoxAssignmentOperator.Equal, structLiteral, List.of(), pos, src ), pos, src );
	}

	// Builds a fresh "GroovyEnumValue.of(enumTypeName, constantName, ordinal)" call expression -
	// called TWICE per constant (once for its direct struct entry, once for the "values" array),
	// deliberately never reusing one instance in two places in the tree, so each occurrence is an
	// independent node - same reasoning as GroovyExpressionVisitor#desugarCompoundAssign.
	private BoxExpression buildEnumValueOf( String enumTypeName, String constantName, int ordinal, Position pos, String src ) {
		BoxExpression		classRef	= new BoxFQN( GROOVY_ENUM_VALUE_FQN, pos, GROOVY_ENUM_VALUE_FQN );
		List<BoxArgument>	args		= List.of(
		    new BoxArgument( new BoxStringLiteral( enumTypeName, pos, enumTypeName ), pos, src ),
		    new BoxArgument( new BoxStringLiteral( constantName, pos, constantName ), pos, src ),
		    new BoxArgument( new BoxIntegerLiteral( String.valueOf( ordinal ), pos, src ), pos, src ) );
		return new ortus.boxlang.compiler.ast.expression.BoxStaticMethodInvocation( new BoxIdentifier( "of", pos, "of" ), classRef, args, pos, src );
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

	// Real Groovy "switch" uses subject.isCase(caseValue) semantics: a Class case value means an
	// instanceof check, a Range means containment, a List means containment - NOT plain equality.
	// BoxSwitch (shared with CFVisitor/BoxVisitor) only ever does plain equality, so a switch with
	// at least one such "smart" case is rewritten entirely as an if/else-if chain here rather than
	// built as a BoxSwitch - a switch with ONLY plain-equality cases (the common case, including
	// the existing "case Color.RED:" enum idiom, a MemberExpr rather than a bare identifier) is
	// completely unaffected, still compiled via the native, more efficient BoxSwitch.
	// <p>
	// The subject is evaluated exactly once into a synthetic temp variable (so a side-effecting
	// subject expression - "switch (computeThing()) {...}" - isn't re-evaluated once per case the
	// way a naive chain of independent "if" conditions would), and the whole if/else-if chain is
	// wrapped in a single-iteration "while (true) { ...; break }" purely so a matched case's own
	// explicit "break;" (or simply falling off the end of its body) has a legal, correctly-scoped
	// target to exit through - real switch fallthrough between cases is NOT preserved this way
	// (a documented, narrower gap versus a plain-equality BoxSwitch, which does support it), and
	// neither is a bare "continue;" written directly in a case body meaning to target a LOOP
	// enclosing the switch itself (it would instead be swallowed by this synthetic loop) - both
	// accepted, documented simplifications given how rarely either is combined with smart-switch
	// case values specifically.
	@Override
	public BoxNode visitSwitchStatement( SwitchStatementContext ctx ) {
		var		pos				= tools.getPosition( ctx );
		var		src				= tools.getSourceText( ctx );
		boolean	hasSmartCase	= ctx.switchCase().stream()
		    .anyMatch( c -> c instanceof CaseClauseContext caseCtx && isSmartCaseValue( caseCtx.expression() ) );
		if ( hasSmartCase ) {
			return buildSmartSwitch( ctx, pos, src );
		}
		BoxExpression		condition	= ctx.expression().accept( expressionVisitor );
		List<BoxSwitchCase>	cases		= ctx.switchCase().stream().map( c -> ( BoxSwitchCase ) c.accept( this ) ).collect( Collectors.toList() );
		return new BoxSwitch( condition, cases, pos, src );
	}

	private BoxNode buildSmartSwitch( SwitchStatementContext ctx, Position pos, String src ) {
		String			subjectName	= "__groovySwitchSubject";
		BoxStatement	subjectInit	= new BoxExpressionStatement(
		    new BoxAssignment( new BoxIdentifier( subjectName, pos, subjectName ), BoxAssignmentOperator.Equal,
		        ctx.expression().accept( expressionVisitor ), List.of(), pos, src ),
		    pos, src );

		// Built backward so each case's "else" is the chain already built from every case after
		// it - the trailing "default:" clause (if any) seeds the chain as its final "else". Each
		// case gets its OWN fresh "subjectName" identifier node (never the same node instance
		// reused across branches) - same reasoning as GroovyExpressionVisitor#
		// desugarCompoundAssign: every occurrence in the rebuilt tree must be independent.
		BoxStatement	chain		= null;
		for ( int i = ctx.switchCase().size() - 1; i >= 0; i-- ) {
			var sc = ctx.switchCase( i );
			if ( sc instanceof DefaultClauseContext defaultCtx ) {
				chain = new BoxStatementBlock( buildStatementList( defaultCtx.blockStatements() ), pos, src );
				continue;
			}
			CaseClauseContext	caseCtx		= ( CaseClauseContext ) sc;
			BoxExpression		matches		= buildCaseMatch( subjectName, caseCtx.expression(), pos, src );
			BoxStatement		thenBody	= new BoxStatementBlock( buildStatementList( caseCtx.blockStatements() ), pos, src );
			chain = new BoxIfElse( matches, thenBody, chain, pos, src );
		}

		List<BoxStatement> loopBody = new ArrayList<>();
		if ( chain != null ) {
			loopBody.add( chain );
		}
		// Always exit after the chain runs once - whether a case explicitly "break;"ed or simply
		// fell off the end of its body - so there is never any fallthrough into a case below it.
		loopBody.add( new BoxBreak( pos, src ) );
		BoxStatement syntheticLoop = new BoxWhile( null, new BoxBooleanLiteral( Boolean.TRUE, pos, src ),
		    new BoxStatementBlock( loopBody, pos, src ), pos, src );

		return new BoxStatementBlock( List.of( subjectInit, syntheticLoop ), pos, src );
	}

	// Builds the match condition for one "case" value against the (already-evaluated) subject,
	// dispatching on the case value's own syntactic shape - see visitSwitchStatement's header for
	// the full isCase() reasoning. Any shape not specifically recognized here falls back to plain
	// equality, exactly like BoxSwitch's own native matching - so a smart switch can freely mix
	// smart and ordinary equality cases in the same statement.
	private BoxExpression buildCaseMatch( String subjectName, ortus.boxlang.parser.antlr.GroovyGrammar.ExpressionContext caseExprCtx, Position pos,
	    String src ) {
		if ( caseExprCtx instanceof ortus.boxlang.parser.antlr.GroovyGrammar.RangeExprContext ) {
			// Range containment ("case 1..10:") - the range's own native ".contains()" member,
			// the exact same one visitInExpr already reuses for the "in" operator.
			BoxExpression rangeExpr = caseExprCtx.accept( expressionVisitor );
			return new BoxMethodInvocation( new BoxIdentifier( "contains", pos, "contains" ), rangeExpr,
			    List.of( new BoxArgument( new BoxIdentifier( subjectName, pos, subjectName ), pos, src ) ), false, true, pos, src );
		}
		if ( caseExprCtx instanceof ortus.boxlang.parser.antlr.GroovyGrammar.PrimaryExprContext primaryCtx
		    && primaryCtx.primary() instanceof ortus.boxlang.parser.antlr.GroovyGrammar.IdentifierExprContext idCtx
		    && isCapitalized( idCtx.IDENTIFIER().getText() ) ) {
			// Class case ("case String:") - an instanceof check against the bare (capitalized)
			// class name, the same heuristic (and same "no real symbol resolution" boundary)
			// GroovyParserControl#isCommandStyleCallStart's own bare-identifier recognition uses.
			String			className	= idCtx.IDENTIFIER().getText();
			BoxExpression	typeExpr	= new BoxFQN( className, pos, className );
			return new BoxBinaryOperation( new BoxIdentifier( subjectName, pos, subjectName ), BoxBinaryOperator.InstanceOf, typeExpr, pos, src );
		}
		if ( caseExprCtx instanceof ortus.boxlang.parser.antlr.GroovyGrammar.PrimaryExprContext collPrimaryCtx
		    && collPrimaryCtx.primary() instanceof ortus.boxlang.parser.antlr.GroovyGrammar.CollectionExprContext collCtx
		    && isListLiteral( collCtx.listOrMapLiteral() ) ) {
			// List containment ("case [1, 2, 3]:") - same ".contains()" member as the Range case.
			BoxExpression listExpr = caseExprCtx.accept( expressionVisitor );
			return new BoxMethodInvocation( new BoxIdentifier( "contains", pos, "contains" ), listExpr,
			    List.of( new BoxArgument( new BoxIdentifier( subjectName, pos, subjectName ), pos, src ) ), false, true, pos, src );
		}
		BoxExpression caseValue = caseExprCtx.accept( expressionVisitor );
		return new BoxComparisonOperation( new BoxIdentifier( subjectName, pos, subjectName ), BoxComparisonOperator.Equal, caseValue, pos, src );
	}

	private boolean isListLiteral( ortus.boxlang.parser.antlr.GroovyGrammar.ListOrMapLiteralContext ctx ) {
		return ctx instanceof ortus.boxlang.parser.antlr.GroovyGrammar.ListLiteralContext
		    || ctx instanceof ortus.boxlang.parser.antlr.GroovyGrammar.EmptyListLiteralContext;
	}

	private boolean isCapitalized( String name ) {
		return !name.isEmpty() && Character.isUpperCase( name.charAt( 0 ) );
	}

	@Override
	public BoxNode visitCaseClause( CaseClauseContext ctx ) {
		var					pos			= tools.getPosition( ctx );
		var					src			= tools.getSourceText( ctx );
		BoxExpression		condition	= ctx.expression().accept( expressionVisitor );
		List<BoxStatement>	body		= buildStatementList( ctx.blockStatements() );
		return new BoxSwitchCase( condition, null, body, pos, src );
	}

	private boolean isSmartCaseValue( ortus.boxlang.parser.antlr.GroovyGrammar.ExpressionContext exprCtx ) {
		if ( exprCtx instanceof ortus.boxlang.parser.antlr.GroovyGrammar.RangeExprContext ) {
			return true;
		}
		if ( exprCtx instanceof ortus.boxlang.parser.antlr.GroovyGrammar.PrimaryExprContext collPrimaryCtx
		    && collPrimaryCtx.primary() instanceof ortus.boxlang.parser.antlr.GroovyGrammar.CollectionExprContext collCtx
		    && isListLiteral( collCtx.listOrMapLiteral() ) ) {
			return true;
		}
		if ( exprCtx instanceof ortus.boxlang.parser.antlr.GroovyGrammar.PrimaryExprContext primaryCtx
		    && primaryCtx.primary() instanceof ortus.boxlang.parser.antlr.GroovyGrammar.IdentifierExprContext idCtx ) {
			return isCapitalized( idCtx.IDENTIFIER().getText() );
		}
		return false;
	}

	@Override
	public BoxNode visitDefaultClause( DefaultClauseContext ctx ) {
		var					pos		= tools.getPosition( ctx );
		var					src		= tools.getSourceText( ctx );
		List<BoxStatement>	body	= buildStatementList( ctx.blockStatements() );
		return new BoxSwitchCase( null, null, body, pos, src );
	}

	@Override
	public BoxNode visitAssertStatement( AssertStatementContext ctx ) {
		var				pos			= tools.getPosition( ctx );
		var				src			= tools.getSourceText( ctx );
		BoxExpression	condition	= ctx.expression( 0 ).accept( expressionVisitor );
		if ( ctx.expression().size() > 1 ) {
			BoxExpression message = ctx.expression( 1 ).accept( expressionVisitor );
			return new BoxAssert( condition, message, pos, src );
		}
		return new BoxAssert( condition, pos, src );
	}

	@Override
	public BoxNode visitExprStatement( ExprStatementContext ctx ) {
		var	pos	= tools.getPosition( ctx );
		var	src	= tools.getSourceText( ctx );
		return new BoxExpressionStatement( ctx.expression().accept( expressionVisitor ), pos, src );
	}

	// Groovy's paren-less "command-style" call (println "hi", apply plugin: 'groovy') - see
	// GroovyParserControl#isCommandStyleCallStart for exactly which shapes this is (deliberately
	// narrowly) gated to. Builds the exact same call node a parenthesized "name(args)" expression
	// statement would, via the shared bare-name-call helper (which also handles a call to a
	// statically-imported member correctly, same as the parenthesized form).
	@Override
	public BoxNode visitCommandCallStatement( ortus.boxlang.parser.antlr.GroovyGrammar.CommandCallStatementContext ctx ) {
		var					pos		= tools.getPosition( ctx );
		var					src		= tools.getSourceText( ctx );
		List<BoxArgument>	args	= expressionVisitor.buildArguments( ctx.argumentList() );
		BoxExpression		call	= expressionVisitor.buildBareNameCall( ctx.IDENTIFIER().getText(), args, pos, src );
		return new BoxExpressionStatement( call, pos, src );
	}

}
