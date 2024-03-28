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
package ortus.boxlang.compiler.transpiler.transformer.expression;

import java.util.HashMap;
import java.util.Map;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;

import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.expression.BoxAccess;
import ortus.boxlang.compiler.ast.expression.BoxArgument;
import ortus.boxlang.compiler.ast.expression.BoxDotAccess;
import ortus.boxlang.compiler.ast.expression.BoxFunctionInvocation;
import ortus.boxlang.compiler.ast.expression.BoxIdentifier;
import ortus.boxlang.compiler.ast.expression.BoxIntegerLiteral;
import ortus.boxlang.compiler.ast.expression.BoxScope;
import ortus.boxlang.compiler.transpiler.JavaTranspiler;
import ortus.boxlang.compiler.transpiler.transformer.AbstractTransformer;
import ortus.boxlang.compiler.transpiler.transformer.TransformerContext;

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
			if ( dotAccess.getAccess() instanceof BoxIdentifier id ) {
				accessKey = createKey( ( id ).getName() );
			} else if ( dotAccess.getAccess() instanceof BoxIntegerLiteral il ) {
				accessKey = createKey( il );
			} else {
				throw new IllegalStateException( "Unsupported access type: " + dotAccess.getAccess().getClass().getName() );
			}
		} else {
			accessKey = createKey( objectAccess.getAccess() );
		}

		Map<String, String> values = new HashMap<>();
		values.put( "contextName", transpiler.peekContextName() );
		values.put( "safe", safe.toString() );
		values.put( "accessKey", accessKey.toString() );

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
			logger.atTrace().log( node.getSourceText() + " -> " + javaExpr );
			addIndex( javaExpr, node );
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
			BoxNode	parent		= ( BoxNode ) objectAccess.getParent();
			if ( ! ( parent instanceof BoxAccess )
			    // I don't know if this will work, but I'm trying to make an exception for query columns being passed to array BIFs
			    // This prolly won't work if a query column is passed as a second param that isn't the array
			    && ! ( parent instanceof BoxArgument barg && barg.getParent() instanceof BoxFunctionInvocation bfun
			        && bfun.getName().getName().toLowerCase().contains( "array" ) ) ) {
				template = "${contextName}.unwrapQueryColumn( " + template + " )";
			}
			Node javaExpr = parseExpression( template, values );
			logger.atTrace().log( node.getSourceText() + " -> " + javaExpr );
			addIndex( javaExpr, node );
			return javaExpr;
		}
	}

}
