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
import com.github.javaparser.ast.expr.Expression;

import ortus.boxlang.ast.BoxNode;
import ortus.boxlang.ast.expression.BoxAccess;
import ortus.boxlang.ast.expression.BoxDotAccess;
import ortus.boxlang.ast.expression.BoxIdentifier;
import ortus.boxlang.ast.expression.BoxScope;
import ortus.boxlang.transpiler.JavaTranspiler;
import ortus.boxlang.transpiler.transformer.AbstractTransformer;
import ortus.boxlang.transpiler.transformer.TransformerContext;

public class BoxAccessTransformer extends AbstractTransformer {

	public BoxAccessTransformer( JavaTranspiler transpiler ) {
		super( transpiler );
	}

	@Override
	public Node transform( BoxNode node, TransformerContext context ) throws IllegalStateException {
		BoxAccess	objectAccess	= ( BoxAccess ) node;
		Boolean		safe			= objectAccess.isSafe() || context == TransformerContext.SAFE;

		Node		accessKey;
		// DotAccess just uses the string directly, array access allows any expression
		if ( objectAccess instanceof BoxDotAccess dotAccess ) {
			accessKey = createKey( ( ( BoxIdentifier ) dotAccess.getAccess() ).getName() );
		} else {
			accessKey = createKey( objectAccess.getAccess() );
		}

		Map<String, String> values = new HashMap<>() {

			{
				put( "contextName", transpiler.peekContextName() );
				put( "safe", safe.toString() );
				put( "accessKey", accessKey.toString() );
			}
		};

		// An access expression starting a scope can be optimized
		if ( objectAccess.getContext() instanceof BoxScope ) {
			Expression jContext = ( Expression ) transpiler.transform( objectAccess.getContext(), TransformerContext.NONE );
			values.put( "scopeReference", jContext.toString() );

			String	template	= """
			                                      ${scopeReference}.dereference(
			                      ${contextName},
			                                        ${accessKey},
			                                        ${safe}
			                                        )
			                                                      """;
			Node	javaExpr	= parseExpression( template, values );
			logger.info( node.getSourceText() + " -> " + javaExpr );
			return javaExpr;

		} else {
			// All other non-scoped vars we just lookup
			Expression jContext = ( Expression ) transpiler.transform( objectAccess.getContext(), context );
			// "scope" here isn't a BoxLang proper scope, it's just whatever Java source represents the context of the access expression
			values.put( "scopeReference", jContext.toString() );

			String	template	= """
			                                      Referencer.get(
			                      ${contextName},
			                                      	  ${scopeReference},
			                                            ${accessKey},
			                                            ${safe}
			                                                  )
			                                            """;

			Node	javaExpr	= parseExpression( template, values );
			logger.info( node.getSourceText() + " -> " + javaExpr );
			return javaExpr;
		}
	}

}
