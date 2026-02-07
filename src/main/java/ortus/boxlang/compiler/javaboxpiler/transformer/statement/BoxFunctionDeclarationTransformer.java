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
 * Transform a Function Declaration in the equivalent Java Parser AST nodes
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

	private String invokerTemplate = """
		private static Object ${invokerMethodName}( FunctionBoxContext context ) {
			ClassLocator classLocator = ClassLocator.getInstance();

		}
 	""";

	public BoxFunctionDeclarationTransformer(JavaTranspiler transpiler) {
    	super(transpiler);
    }
	// @formatter:on
	@Override
	public Node transform( BoxNode node, TransformerContext context ) throws IllegalStateException {
		BoxFunctionDeclaration	function		= ( BoxFunctionDeclaration ) node;
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

		// If the closest ancestor is a script, then the default output is true
		@SuppressWarnings( "unchecked" )
		BoxNode				ancestor		= function.getFirstNodeOfTypes( BoxTemplate.class, BoxScript.class, BoxExpression.class, BoxScriptIsland.class,
		    BoxTemplateIsland.class );
		boolean				defaultOutput	= ancestor == null || !Set.of( BoxTemplate.class, BoxTemplateIsland.class ).contains( ancestor.getClass() );

		Map<String, String>	values			= Map.ofEntries(
		    Map.entry( "className", className ),
		    Map.entry( "access", access.toString().toUpperCase() ),
		    Map.entry( "modifiers", transformModifiers( function.getModifiers() ) ),
		    Map.entry( "functionName", createKey( function.getName() ).toString() ),
		    Map.entry( "invokerMethodName", "invokeFunction" + function.getName() ),
		    Map.entry( "returnType", returnType.equals( BoxType.Fqn ) ? fqn : returnType.name() ),
		    Map.entry( "defaultOutput", String.valueOf( defaultOutput ) )
		);
		transpiler.pushContextName( "context" );

		// ********************** Registration template **********************

		String							code	= PlaceholderHelper.resolve( invokerTemplate, values );
		ParseResult<MethodDeclaration>	result;
		try {
			result = javaParser.parseMethodDeclaration( code );
		} catch ( Exception e ) {
			// Temp debugging to see generated Java code
			throw new BoxRuntimeException( code, e );
		}
		if ( !result.isSuccessful() ) {
			// Temp debugging to see generated Java code
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
		// Ensure we have a return statement
		invokeMethod.getBody().get().addStatement( new ReturnStmt( new NullLiteralExpr() ) );
		transpiler.popContextName();

		// ********************** UDF Instantiation template **********************

		ObjectCreationExpr		instantiationExpr	= ( ObjectCreationExpr ) parseExpression( instantiationTemplate, values );

		/* Transform the arguments creating the initialization values */
		ArrayInitializerExpr	argInitializer		= new ArrayInitializerExpr();
		function.getArgs().forEach( arg -> {
			Expression argument = ( Expression ) transpiler.transform( arg );
			argInitializer.getValues().add( argument );
		} );
		ArrayCreationExpr argumentsArray = new ArrayCreationExpr()
		    .setElementType( "Argument" )
		    .setInitializer( argInitializer );
		instantiationExpr.setArgument( 1, argumentsArray );

		/* Transform the annotations creating the initialization value */
		instantiationExpr.setArgument( 4, transformAnnotations( function.getAnnotations() ) );

		/* Transform the documentation creating the initialization value */
		instantiationExpr.setArgument( 5, transformDocumentation( function.getDocumentation() ) );

		( ( JavaTranspiler ) transpiler ).getUDFInvokers().put( Key.of( function.getName() ), Pair.of( invokeMethod, instantiationExpr ) );

		// ********************** Registration template **********************

		Statement javaStmt = parseStatement( registrationTemplate, values );
		// logger.trace( node.getSourceText() + " -> " + javaStmt );
		// commenting this out to prevent BoxFunctionDeclarationTransformer nodes in the SourceMaps
		// This caused the debugger's stepping behavior to hit locations it shouldn't be stopping on
		// addIndex( javaStmt, node );
		if ( function.getModifiers().contains( BoxMethodDeclarationModifier.STATIC ) ) {
			( ( JavaTranspiler ) transpiler ).getStaticUDFDeclarations().add( javaStmt );
		} else {
			( ( JavaTranspiler ) transpiler ).getUDFDeclarations().add( javaStmt );
		}

		// The actual declaration is hoisted to the top, so I just need a dummy node to return here
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
