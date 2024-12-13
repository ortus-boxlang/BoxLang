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

import java.util.HashMap;
import java.util.Map;

import com.github.javaparser.ast.Node;

import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.expression.BoxIdentifier;
import ortus.boxlang.compiler.javaboxpiler.JavaTranspiler;
import ortus.boxlang.compiler.javaboxpiler.transformer.AbstractTransformer;
import ortus.boxlang.compiler.javaboxpiler.transformer.TransformerContext;

/**
 * Transform a BoxIdentifier Node the equivalent Java Parser AST nodes
 */
public class BoxIdentifierTransformer extends AbstractTransformer {

	public BoxIdentifierTransformer( JavaTranspiler transpiler ) {
		super( transpiler );
	}

	@Override
	public Node transform( BoxNode node, TransformerContext context ) throws IllegalStateException {
		BoxIdentifier		identifier	= ( BoxIdentifier ) node;
		String				side		= context == TransformerContext.NONE ? "" : "(" + context.toString() + ") ";
		Node				accessKey	= createKey( identifier.getName() );
		String				template;
		Map<String, String>	values		= new HashMap<>() {

											{
												put( "accessKey", accessKey.toString() );
												put( "id", identifier.getName() );
												put( "contextName", transpiler.peekContextName() );
											}
										};
		if ( transpiler.matchesImport( identifier.getName() ) && transpiler.getProperty( "sourceType" ).toLowerCase().startsWith( "box" ) ) {
			template = "classLocator.load( ${contextName}, \"${id}\", imports )";
		} else {
			template = switch ( context ) {
				case SAFE -> "${contextName}.scopeFindNearby( ${accessKey}, ${contextName}.getDefaultAssignmentScope(), false ).value()";
				default -> "${contextName}.scopeFindNearby( ${accessKey}, null, false ).value()";
			};
		}

		Node javaExpr;
		javaExpr = parseExpression( template, values );
		// logger.trace( side + node.getSourceText() + " -> " + javaExpr );
		return javaExpr;

	}
}
