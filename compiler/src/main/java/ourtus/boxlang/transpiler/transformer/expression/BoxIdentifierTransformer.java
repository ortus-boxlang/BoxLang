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
package ourtus.boxlang.transpiler.transformer.expression;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.NameExpr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ourtus.boxlang.ast.BoxNode;
import ourtus.boxlang.ast.expression.BoxIdentifier;
import ourtus.boxlang.transpiler.transformer.AbstractTransformer;
import ourtus.boxlang.transpiler.transformer.TransformerContext;

import java.util.HashMap;
import java.util.Map;

public class BoxIdentifierTransformer extends AbstractTransformer {

	Logger logger = LoggerFactory.getLogger( BoxScopeTransformer.class );

	@Override
	public Node transform( BoxNode node, TransformerContext context ) throws IllegalStateException {
		BoxIdentifier		identifier	= ( BoxIdentifier ) node;
		String				side		= context == TransformerContext.NONE ? "" : "(" + context.toString() + ") ";
		Map<String, String>	values		= new HashMap<>() {

											{
												put( "identifier", identifier.getName() );
											}
										};

		String				template	= switch ( context ) {
											case DEREFERENCING -> "Key.of( \"${identifier}\" )";
											default -> "${identifier}";
										};

		Node				javaExpr	= parseExpression( template, values );
		logger.info( side + node.getSourceText() + " -> " + javaExpr );

		return javaExpr;

	}

	private Node resolveScope( Node expr ) {
		if ( expr instanceof NameExpr ) {
			String				id			= expr.toString();
			String				template	= "context.scopeFindNearby(Key.of(\"${id}\"))";
			Map<String, String>	values		= new HashMap<>() {

												{
													put( "id", id.toString() );
												}
											};
			return parseExpression( template, values );

		}
		return expr;
	}
}
