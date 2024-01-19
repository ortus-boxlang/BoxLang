/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ortus.boxlang.transpiler.transformer.expression;

import java.util.HashMap;
import java.util.Map;

import com.github.javaparser.ast.Node;

import ortus.boxlang.ast.BoxNode;
import ortus.boxlang.ast.expression.BoxScope;
import ortus.boxlang.transpiler.JavaTranspiler;
import ortus.boxlang.transpiler.transformer.AbstractTransformer;
import ortus.boxlang.transpiler.transformer.TransformerContext;

public class BoxScopeTransformer extends AbstractTransformer {

	public BoxScopeTransformer( JavaTranspiler transpiler ) {
		super( transpiler );
	}

	@Override
	public Node transform( BoxNode node, TransformerContext context ) throws IllegalStateException {
		BoxScope			scope		= ( BoxScope ) node;
		String				side		= context == TransformerContext.NONE ? "" : "(" + context.toString() + ") ";
		Map<String, String>	values		= new HashMap<>() {

											{
												put( "scope", scope.getName() );
												put( "contextName", transpiler.peekContextName() );
											}
										};
		String				template	= "";
		if ( "local".equalsIgnoreCase( scope.getName() ) ) {
			template = "${contextName}.getScopeNearby( LocalScope.name )";
		} else if ( "variables".equalsIgnoreCase( scope.getName() ) ) {
			template = "${contextName}.getScopeNearby( VariablesScope.name )";
			// This is assuming all class templates' invoke method gets the varaiblesScope reference first
			// Nevermind-- this doens't work in a catch block where I need the variables scope to come from the CATCHBOXCONTEXT instead
			// template = "variablesScope";
		} else if ( "request".equalsIgnoreCase( scope.getName() ) ) {
			template = "${contextName}.getScopeNearby( RequestScope.name )";
		} else if ( "server".equalsIgnoreCase( scope.getName() ) ) {
			template = "${contextName}.getScopeNearby( ServerScope.name )";
		} else if ( "arguments".equalsIgnoreCase( scope.getName() ) ) {
			template = "${contextName}.getScopeNearby( ArgumentsScope.name )";
		} else if ( "application".equalsIgnoreCase( scope.getName() ) ) {
			template = "${contextName}.getScopeNearby( ApplicationScope.name )";
		} else if ( "session".equalsIgnoreCase( scope.getName() ) ) {
			template = "${contextName}.getScopeNearby( SessionScope.name )";
		} else if ( "url".equalsIgnoreCase( scope.getName() ) ) {
			template = "${contextName}.getScopeNearby( URLScope.name )";
		} else if ( "form".equalsIgnoreCase( scope.getName() ) ) {
			template = "${contextName}.getScopeNearby( FormScope.name )";
		} else if ( "cgi".equalsIgnoreCase( scope.getName() ) ) {
			template = "${contextName}.getScopeNearby( CGIScope.name )";
		} else if ( "cookie".equalsIgnoreCase( scope.getName() ) ) {
			template = "${contextName}.getScopeNearby( CookieScope.name )";
		} else if ( "this".equalsIgnoreCase( scope.getName() ) ) {

	// @formatter:off
			template = """
					// using a switch so I can wrap up logic that possibly thrown an exception as an expression.
					( switch ( 1 ) {
						case 1 -> {
							Object javaIsStupid = ${contextName};
							if( javaIsStupid instanceof ClassBoxContext ) {
								ClassBoxContext bc = (ClassBoxContext) javaIsStupid;
								yield bc.getThisClass();
							} else if( ${contextName} instanceof FunctionBoxContext ) {
								FunctionBoxContext fc = (FunctionBoxContext) ${contextName};
								if( fc.isInClass() ) {
									yield fc.getThisClass().getBottomClass();
								} else {
									throw new BoxRuntimeException( "Cannot get [this] from the current context because this function is not executing in a class." );
								}
							} else {
								throw new BoxRuntimeException( "Cannot get [this] from the current context" );
							}
						}
						default -> throw new BoxRuntimeException( "This code can never be run, but Java demands it" );
					} )
				""";
			// @formatter:on

		} else if ( "super".equalsIgnoreCase( scope.getName() ) ) {

	// @formatter:off
			template = """
				// using a switch so I can wrap up logic that possibly thrown an exception as an expression.
				( switch ( 1 ) {
					case 1 -> {
						Object javaIsStupid = ${contextName};
						if( javaIsStupid instanceof ClassBoxContext ) {
							ClassBoxContext bc = (ClassBoxContext) javaIsStupid;
							if( bc.getThisClass().getSuper() != null ) {
								yield bc.getThisClass().getSuper();
							} else {
								throw new BoxRuntimeException( "Cannot get [super] from the current context because this class does not extend another class." );
							}
						} else if( ${contextName} instanceof FunctionBoxContext ) {
							FunctionBoxContext fc = (FunctionBoxContext) ${contextName};
							if( fc.isInClass() ) {
								if( fc.getThisClass().getSuper() != null ) {
									yield fc.getThisClass().getSuper();
								} else {
									throw new BoxRuntimeException( "Cannot get [super] from the current context because this class does not extend another class." );
								}
							} else {
								throw new BoxRuntimeException( "Cannot get [super] from the current context because this function is not executing in a class." );
							}
						} else {
							throw new BoxRuntimeException( "Cannot get [super] from the current context" );
						}
					}
					default -> throw new BoxRuntimeException( "This code can never be run, but Java demands it" );

				} )
				""";
				// @formatter:on

		} else {
			throw new IllegalStateException( "Scope transformation not implemented: " + scope.getName() );
		}

		Node javaExpr = parseExpression( template, values );
		logger.debug( side + node.getSourceText() + " -> " + javaExpr );
		return javaExpr;
	}
}
