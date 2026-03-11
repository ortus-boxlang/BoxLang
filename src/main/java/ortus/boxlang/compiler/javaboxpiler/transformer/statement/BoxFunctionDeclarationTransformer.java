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
package ortus.boxlang.compiler.javaboxpiler.transformer.statement;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.github.javaparser.ParseResult;

import ortus.boxlang.compiler.IBoxpiler;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.ArrayCreationExpr;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.EmptyStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;

import ortus.boxlang.compiler.ast.BoxExpression;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.BoxScript;
import ortus.boxlang.compiler.ast.BoxStatement;
import ortus.boxlang.compiler.ast.BoxTemplate;
import ortus.boxlang.compiler.ast.expression.BoxClosure;
import ortus.boxlang.compiler.ast.expression.BoxLambda;
import ortus.boxlang.compiler.ast.statement.BoxAccessModifier;
import ortus.boxlang.compiler.ast.statement.BoxFunctionDeclaration;
import ortus.boxlang.compiler.ast.statement.BoxMethodDeclarationModifier;
import ortus.boxlang.compiler.ast.statement.BoxReturnType;
import ortus.boxlang.compiler.ast.statement.BoxScriptIsland;
import ortus.boxlang.compiler.ast.statement.BoxType;
import ortus.boxlang.compiler.ast.statement.component.BoxTemplateIsland;
import ortus.boxlang.compiler.javaboxpiler.JavaTranspiler;
import ortus.boxlang.compiler.javaboxpiler.transformer.AbstractTransformer;
import ortus.boxlang.compiler.javaboxpiler.transformer.TransformerContext;
import ortus.boxlang.runtime.config.util.PlaceholderHelper;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.util.Pair;

/**
 * Transform a Function Declaration in the equivalent Java Parser AST nodes.
 * Supports both top-level UDF declarations (hoisted) and nested function declarations
 * inside other functions/closures (compiled as closures and assigned to the local scope).
 */
public class BoxFunctionDeclarationTransformer extends AbstractTransformer {

	// @formatter:off
	private String registrationTemplate = "context.registerUDF( udfs.get( ${functionName} ) );";

	private String instantiationTemplate = """
		new UDF(
			${functionName},
			null,
			"${returnType}",
			Function.Access.${access},
			null,
			null,
			List.of( ${modifiers} ),
			${defaultOutput},
			imports,
			sourceType,
			path,
			${className}::${invokerMethodName}
		)
			
 	""";

	private String nestedInstantiationTemplate = """
		new ClosureDefinition(
			${functionName},
			null,
			"${returnType}",
			Function.Access.${access},
			null,
			null,
			List.of( ${modifiers} ),
			${defaultOutput},
			imports,
			sourceType,
			path,
			${className}::${invokerMethodName}
		)
 	""";

	private String invokerTemplate = """
		private static Object ${invokerMethodName}( FunctionBoxContext context ) {
			ClassLocator classLocator = ClassLocator.getInstance();

		}
 	""";

	private String nestedRegistrationTemplate =
		"${contextName}.getScopeNearby( LocalScope.name, false ).put( ${functionName}, closures.get(${closureIndex}).newInstance(${contextName}) );";

	public BoxFunctionDeclarationTransformer(JavaTranspiler transpiler) {
    	super(transpiler);
    }
	// @formatter:on
	@Override
	public Node transform( BoxNode node, TransformerContext context ) throws IllegalStateException {
		BoxFunctionDeclaration function = ( BoxFunctionDeclaration ) node;

		if ( isNestedFunction( function ) ) {
			return transformNestedFunction( function, context );
		}

		return transformTopLevelFunction( function, context );
	}

	/**
	 * Detect if this function declaration is nested inside another function, closure, or lambda body.
	 */
	private boolean isNestedFunction( BoxFunctionDeclaration function ) {
		return function.getFirstAncestorOfType( BoxFunctionDeclaration.class ) != null
		    || function.getFirstAncestorOfType( BoxClosure.class ) != null
		    || function.getFirstAncestorOfType( BoxLambda.class ) != null;
	}

	/**
	 * Transform a nested function declaration as a named closure assigned to the parent's local scope.
	 * The function is compiled as a ClosureDefinition and at runtime, a new Closure instance is
	 * created (capturing the declaring context) and placed into the local scope under the function's name.
	 */
	private Node transformNestedFunction( BoxFunctionDeclaration function, TransformerContext context ) {
		BoxAccessModifier	access			= function.getAccessModifier();
		String				className		= transpiler.getProperty( "classname" );
		BoxReturnType		boxReturnType	= function.getType();
		BoxType				returnType		= BoxType.Any;
		String				fqn				= null;
		if ( boxReturnType != null ) {
			returnType = boxReturnType.getType();
			if ( returnType.equals( BoxType.Fqn ) ) {
				fqn = boxReturnType.getFqn();
			}
		}
		if ( access == null ) {
			access = BoxAccessModifier.Public;
		}

		@SuppressWarnings( "unchecked" )
		BoxNode				ancestor		= function.getFirstNodeOfTypes( BoxTemplate.class, BoxScript.class, BoxExpression.class, BoxScriptIsland.class,
		    BoxTemplateIsland.class );
		boolean				defaultOutput	= ancestor == null || !Set.of( BoxTemplate.class, BoxTemplateIsland.class ).contains( ancestor.getClass() );

		int											closureIndex		= transpiler.incrementAndGetClosureCounter();
		String										invokerMethodName	= "invokeClosure_" + closureIndex;

		List<Pair<MethodDeclaration, Expression>>	closureInvokers		= ( ( JavaTranspiler ) transpiler ).getClosureInvokers();
		int											mySlot				= closureInvokers.size();
		closureInvokers.add( null );

		Map<String, String>	values			= Map.ofEntries(
		    Map.entry( "className", className ),
		    Map.entry( "access", access.toString().toUpperCase() ),
		    Map.entry( "modifiers", transformModifiers( function.getModifiers() ) ),
		    Map.entry( "functionName", createKey( function.getName() ).toString() ),
		    Map.entry( "invokerMethodName", invokerMethodName ),
		    Map.entry( "returnType", returnType.equals( BoxType.Fqn ) ? fqn : returnType.name() ),
		    Map.entry( "defaultOutput", String.valueOf( defaultOutput ) ),
		    Map.entry( "closureIndex", String.valueOf( mySlot ) ),
		    Map.entry( "contextName", transpiler.peekContextName() )
		);

		// ********************** Invoker method (function body) **********************

		String							code	= PlaceholderHelper.resolve( invokerTemplate, values );
		ParseResult<MethodDeclaration>	result;
		try {
			result = javaParser.parseMethodDeclaration( code );
		} catch ( Exception e ) {
			throw new BoxRuntimeException( code, e );
		}
		if ( !result.isSuccessful() ) {
			throw new BoxRuntimeException( result + "\n" + code );
		}

		MethodDeclaration invokeMethod = result.getResult().orElseThrow();

		transpiler.pushContextName( "context" );
		transpiler.pushfunctionBodyCounter();
		int componentCounter = transpiler.getComponentCounter();
		transpiler.setComponentCounter( 0 );
		for ( BoxStatement statement : function.getBody() ) {
			Node javaStmt = transpiler.transform( statement );
			if ( javaStmt instanceof BlockStmt stmt ) {
				stmt.getStatements().forEach( it -> invokeMethod.getBody().get().addStatement( it ) );
			} else {
				invokeMethod.getBody().get().addStatement( ( Statement ) javaStmt );
			}
		}
		transpiler.setComponentCounter( componentCounter );
		transpiler.popfunctionBodyCounter();
		invokeMethod.getBody().get().addStatement( new ReturnStmt( new NullLiteralExpr() ) );
		transpiler.popContextName();

		// ********************** ClosureDefinition instantiation **********************

		ObjectCreationExpr		instantiationExpr	= ( ObjectCreationExpr ) parseExpression( nestedInstantiationTemplate, values );

		ArrayInitializerExpr	argInitializer		= new ArrayInitializerExpr();
		function.getArgs().forEach( arg -> {
			Expression argument = ( Expression ) transpiler.transform( arg );
			argInitializer.getValues().add( argument );
		} );
		ArrayCreationExpr argumentsArray = new ArrayCreationExpr()
		    .setElementType( "Argument" )
		    .setInitializer( argInitializer );
		instantiationExpr.setArgument( 1, argumentsArray );

		instantiationExpr.setArgument( 4, transformAnnotations( function.getAnnotations() ) );
		instantiationExpr.setArgument( 5, transformDocumentation( function.getDocumentation() ) );

		closureInvokers.set( mySlot, Pair.of( invokeMethod, instantiationExpr ) );

		// ********************** Inline registration to local scope **********************

		return parseStatement( nestedRegistrationTemplate, values );
	}

	/**
	 * Transform a top-level function declaration as a hoisted UDF registration (original behavior).
	 */
	private Node transformTopLevelFunction( BoxFunctionDeclaration function, TransformerContext context ) {
		BoxAccessModifier		access			= function.getAccessModifier();
		String					className		= transpiler.getProperty( "classname" );
		BoxReturnType			boxReturnType	= function.getType();
		BoxType					returnType		= BoxType.Any;
		String					fqn				= null;
		if ( boxReturnType != null ) {
			returnType = boxReturnType.getType();
			if ( returnType.equals( BoxType.Fqn ) ) {
				fqn = boxReturnType.getFqn();
			}
		}
		if ( access == null ) {
			access = BoxAccessModifier.Public;
		}

		@SuppressWarnings( "unchecked" )
		BoxNode				ancestor		= function.getFirstNodeOfTypes( BoxTemplate.class, BoxScript.class, BoxExpression.class, BoxScriptIsland.class,
		    BoxTemplateIsland.class );
		boolean				defaultOutput	= ancestor == null || !Set.of( BoxTemplate.class, BoxTemplateIsland.class ).contains( ancestor.getClass() );

		Map<String, String>	values			= Map.ofEntries(
		    Map.entry( "className", className ),
		    Map.entry( "access", access.toString().toUpperCase() ),
		    Map.entry( "modifiers", transformModifiers( function.getModifiers() ) ),
		    Map.entry( "functionName", createKey( function.getName() ).toString() ),
		    Map.entry( "invokerMethodName", IBoxpiler.INVOKE_FUNCTION_PREFIX + IBoxpiler.sanitizeForJavaIdentifier( function.getName() ) ),
		    Map.entry( "returnType", returnType.equals( BoxType.Fqn ) ? fqn : returnType.name() ),
		    Map.entry( "defaultOutput", String.valueOf( defaultOutput ) )
		);
		transpiler.pushContextName( "context" );

		String							code	= PlaceholderHelper.resolve( invokerTemplate, values );
		ParseResult<MethodDeclaration>	result;
		try {
			result = javaParser.parseMethodDeclaration( code );
		} catch ( Exception e ) {
			throw new BoxRuntimeException( code, e );
		}
		if ( !result.isSuccessful() ) {
			throw new BoxRuntimeException( result + "\n" + code );
		}

		MethodDeclaration invokeMethod = result.getResult().orElseThrow();

		transpiler.pushfunctionBodyCounter();
		int componentCounter = transpiler.getComponentCounter();
		transpiler.setComponentCounter( 0 );
		for ( BoxStatement statement : function.getBody() ) {
			Node javaStmt = transpiler.transform( statement );
			if ( javaStmt instanceof BlockStmt stmt ) {
				stmt.getStatements().forEach( it -> invokeMethod.getBody().get().addStatement( it ) );
			} else {
				invokeMethod.getBody().get().addStatement( ( Statement ) javaStmt );
			}
		}
		transpiler.setComponentCounter( componentCounter );
		transpiler.popfunctionBodyCounter();
		invokeMethod.getBody().get().addStatement( new ReturnStmt( new NullLiteralExpr() ) );
		transpiler.popContextName();

		ObjectCreationExpr		instantiationExpr	= ( ObjectCreationExpr ) parseExpression( instantiationTemplate, values );

		ArrayInitializerExpr	argInitializer		= new ArrayInitializerExpr();
		function.getArgs().forEach( arg -> {
			Expression argument = ( Expression ) transpiler.transform( arg );
			argInitializer.getValues().add( argument );
		} );
		ArrayCreationExpr argumentsArray = new ArrayCreationExpr()
		    .setElementType( "Argument" )
		    .setInitializer( argInitializer );
		instantiationExpr.setArgument( 1, argumentsArray );

		instantiationExpr.setArgument( 4, transformAnnotations( function.getAnnotations() ) );
		instantiationExpr.setArgument( 5, transformDocumentation( function.getDocumentation() ) );

		( ( JavaTranspiler ) transpiler ).getUDFInvokers().put( Key.of( function.getName() ), Pair.of( invokeMethod, instantiationExpr ) );

		Statement javaStmt = parseStatement( registrationTemplate, values );
		if ( function.getModifiers().contains( BoxMethodDeclarationModifier.STATIC ) ) {
			( ( JavaTranspiler ) transpiler ).getStaticUDFDeclarations().add( javaStmt );
		} else {
			( ( JavaTranspiler ) transpiler ).getUDFDeclarations().add( javaStmt );
		}

		return new EmptyStmt();
	}

	/**
	 * Build a list of modifiers for this function
	 * 
	 * @param modifiers list of modifiers
	 * 
	 * @return a string with the list of modifiers
	 */
	private String transformModifiers( List<BoxMethodDeclarationModifier> modifiers ) {
		StringBuilder sb = new StringBuilder();
		for ( BoxMethodDeclarationModifier modifier : modifiers ) {
			sb
			    .append( "BoxMethodDeclarationModifier." )
			    .append( modifier.toString().toUpperCase() )
			    .append( ", " );
		}
		if ( sb.length() > 0 ) {
			sb.setLength( sb.length() - 2 );
		}
		return sb.toString();
	}
}
