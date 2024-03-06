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
		if ( "variables".equalsIgnoreCase( scope.getName() ) ) {
			template = "${contextName}.getScopeNearby( VariablesScope.name )";
		} else if ( "request".equalsIgnoreCase( scope.getName() ) ) {
			template = "${contextName}.getScopeNearby( RequestScope.name )";
		} else if ( "server".equalsIgnoreCase( scope.getName() ) ) {
			template = "${contextName}.getScopeNearby( ServerScope.name )";
		} else {
			throw new IllegalStateException( "Scope transformation not implemented: " + scope.getName() );
		}

		Node javaExpr = parseExpression( template, values );
		logger.atTrace().log( side + node.getSourceText() + " -> " + javaExpr );
		return javaExpr;
	}
}
