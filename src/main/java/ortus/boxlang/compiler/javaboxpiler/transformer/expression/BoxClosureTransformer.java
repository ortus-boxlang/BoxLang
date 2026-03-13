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
package ortus.boxlang.compiler.javaboxpiler.transformer.expression;

import java.util.List;
import java.util.Map;

import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.ArrayCreationExpr;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;

import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.expression.BoxClosure;
import ortus.boxlang.compiler.ast.statement.BoxExpressionStatement;
import ortus.boxlang.compiler.javaboxpiler.JavaTranspiler;
import ortus.boxlang.compiler.javaboxpiler.transformer.AbstractTransformer;
import ortus.boxlang.compiler.javaboxpiler.transformer.TransformerContext;
import ortus.boxlang.runtime.config.util.PlaceholderHelper;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.util.Pair;

/**
 * Transform a Closure expression using inline static method pattern.
 * Closures are stored in a static List as ClosureDefinition instances and accessed by index.
 * Each time a closure is referenced, newInstance(context) is called to get a Closure instance.
 */
public class BoxClosureTransformer extends AbstractTransformer {

	// @formatter:off
	/**
	 * Template for instantiating a ClosureDefinition with a method reference to the static invoker
	 */
	private String instantiationTemplate = """
		new ClosureDefinition(
			Closure.defaultName,
			null,
			"any",
			Function.Access.PUBLIC,
			null,
			Struct.EMPTY,
			List.of(),
			true,
			imports,
			sourceType,
			path,
			${enclosingClassName}::${invokerMethodName}
		)
		""";
	
	/**
	 * Template for the static invoker method that executes the closure body
	 */
	private String invokerTemplate = """
		private static Object ${invokerMethodName}( FunctionBoxContext context ) {
			ClassLocator classLocator = ClassLocator.getInstance();
		}
		""";
	// @formatter:on

	public BoxClosureTransformer( JavaTranspiler transpiler ) {
		super( transpiler );
	}

	/**
	 * Transform a closure expression into a reference to a Closure instance.
	 * This creates:
	 * 1. A static invoker method that executes the closure body
	 * 2. An instantiation expression that creates the ClosureDefinition with a method reference
	 *
	 * @param node    The BoxClosure AST node
	 * @param context The transformer context
	 *
	 * @return An expression that gets a new Closure instance from the definition (e.g., closures.get(0).newInstance(context))
	 */
	@Override
	public Node transform( BoxNode node, TransformerContext context ) throws IllegalStateException {
		BoxClosure									boxClosure			= ( BoxClosure ) node;
		String										enclosingClassName	= transpiler.getProperty( "classname" );
		int											closureIndex		= transpiler.incrementAndGetClosureCounter();
		String										invokerMethodName	= "invokeClosure_" + closureIndex;

		// Reserve our slot in the closureInvokers list BEFORE transforming the body
		// This ensures nested closures get correct indices
		List<Pair<MethodDeclaration, Expression>>	closureInvokers		= ( ( JavaTranspiler ) transpiler ).getClosureInvokers();
		int											mySlot				= closureInvokers.size();
		closureInvokers.add( null ); // Placeholder

		// Transform arguments
		ArrayInitializerExpr argInitializer = new ArrayInitializerExpr();
		boxClosure.getArgs().forEach( arg -> {
			Expression argument = ( Expression ) transpiler.transform( arg );
			argInitializer.getValues().add( argument );
		} );

		// Create the arguments array expression
		ArrayCreationExpr argumentsArray = new ArrayCreationExpr();
		argumentsArray.setElementType( "Argument" );
		argumentsArray.setInitializer( argInitializer );

		// Transform annotations
		Expression						annotationStruct	= transformAnnotations( boxClosure.getAnnotations() );

		Map<String, String>				values				= Map.ofEntries(
		    Map.entry( "enclosingClassName", enclosingClassName ),
		    Map.entry( "invokerMethodName", invokerMethodName ),
		    Map.entry( "closureIndex", String.valueOf( mySlot ) ),
		    Map.entry( "contextName", transpiler.peekContextName() )
		);

		// Create the invoker method using javaParser.parseMethodDeclaration
		String							invokerCode			= PlaceholderHelper.resolve( invokerTemplate, values );
		ParseResult<MethodDeclaration>	result;
		try {
			result = javaParser.parseMethodDeclaration( invokerCode );
		} catch ( Exception e ) {
			throw new BoxRuntimeException( invokerCode, e );
		}
		if ( !result.isSuccessful() ) {
			throw new BoxRuntimeException( result + "\n" + invokerCode );
		}
		MethodDeclaration invokeMethod = result.getResult().orElseThrow();

		// Transform the closure body and add it to the invoker method
		transpiler.pushContextName( "context" );
		BlockStmt body = invokeMethod.getBody().get();
		transpiler.pushfunctionBodyCounter();
		int componentCounter = transpiler.getComponentCounter();
		transpiler.setComponentCounter( 0 );

		// If the body is a single expression, return it directly
		if ( boxClosure.getBody() instanceof BoxExpressionStatement boxExpr ) {
			body.addStatement( new ReturnStmt( new EnclosedExpr( ( Expression ) transpiler.transform( boxExpr.getExpression() ) ) ) );
		} else {
			// Otherwise, add the transformed body and return null at the end
			body.addStatement( ( Statement ) transpiler.transform( boxClosure.getBody() ) );
			body.addStatement( new ReturnStmt( new NullLiteralExpr() ) );
		}

		transpiler.setComponentCounter( componentCounter );
		transpiler.popfunctionBodyCounter();
		transpiler.popContextName();

		// Create the instantiation expression
		String				instantiationCode	= PlaceholderHelper.resolve( instantiationTemplate, values );
		ObjectCreationExpr	instantiationExpr	= ( ObjectCreationExpr ) parseExpression( instantiationCode, values );

		// Replace arguments (position 1) and annotations (position 4) using setArgument
		instantiationExpr.setArgument( 1, argumentsArray );
		instantiationExpr.setArgument( 4, annotationStruct );

		// Store the invoker method and instantiation expression
		closureInvokers.set( mySlot, Pair.of( invokeMethod, instantiationExpr ) );

		// Return an expression that gets a new Closure instance from the ClosureDefinition
		// closures.get(index).newInstance(context)
		return parseExpression( "closures.get(${closureIndex}).newInstance(${contextName})", values );
	}
}
