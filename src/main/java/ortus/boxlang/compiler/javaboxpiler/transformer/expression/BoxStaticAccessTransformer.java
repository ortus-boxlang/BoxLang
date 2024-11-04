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
package ortus.boxlang.compiler.javaboxpiler.transformer.expression;

import java.util.HashMap;
import java.util.Map;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.StringLiteralExpr;

import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.expression.BoxFQN;
import ortus.boxlang.compiler.ast.expression.BoxIdentifier;
import ortus.boxlang.compiler.ast.expression.BoxIntegerLiteral;
import ortus.boxlang.compiler.ast.expression.BoxStaticAccess;
import ortus.boxlang.compiler.javaboxpiler.JavaTranspiler;
import ortus.boxlang.compiler.javaboxpiler.transformer.AbstractTransformer;
import ortus.boxlang.compiler.javaboxpiler.transformer.TransformerContext;
import ortus.boxlang.runtime.types.exceptions.ExpressionException;

public class BoxStaticAccessTransformer extends AbstractTransformer {

	public BoxStaticAccessTransformer( JavaTranspiler transpiler ) {
		super( transpiler );
	}

	@Override
	public Node transform( BoxNode node, TransformerContext context ) throws IllegalStateException {
		BoxStaticAccess	objectAccess	= ( BoxStaticAccess ) node;
		Boolean			safe			= objectAccess.isSafe() || context == TransformerContext.SAFE;

		Node			accessKey;
		// objectAccess just uses the string directly, array access allows any expression
		if ( objectAccess.getAccess() instanceof BoxIdentifier id ) {
			accessKey = createKey( ( id ).getName() );
		} else if ( objectAccess.getAccess() instanceof BoxIntegerLiteral il ) {
			accessKey = createKey( il );
		} else {
			throw new ExpressionException( "Unsupported access type: " + objectAccess.getAccess().getClass().getName(), objectAccess.getAccess() );
		}

		Map<String, String> values = new HashMap<>();
		values.put( "contextName", transpiler.peekContextName() );
		values.put( "safe", safe.toString() );
		values.put( "accessKey", accessKey.toString() );

		Expression jContext;

		if ( objectAccess.getContext() instanceof BoxFQN fqn ) {
			jContext = new StringLiteralExpr( fqn.getValue() );
		} else if ( objectAccess.getContext() instanceof BoxIdentifier id ) {
			// In BL code, this could be an import, but in CF it's just a string
			if ( transpiler.matchesImport( id.getName() ) && transpiler.getProperty( "sourceType" ).toLowerCase().startsWith( "box" ) ) {
				jContext = ( Expression ) transpiler.transform( id, context );
			} else {
				jContext = new StringLiteralExpr( id.getName() );
			}
		} else {
			// foo.bar()::baz
			// This is not valid in Java, but CF/BL perform all static access at runtime and use :: for field as well so it seems reasonable
			// that if foo.bar() returns a static class reference, then we should be able to statically dereference it
			jContext = ( Expression ) transpiler.transform( objectAccess.getContext(), context );
			// throw new ExpressionException( "Unexpected base token in static access.", objectAccess.getContext() );
		}

		// "scope" here isn't a BoxLang proper scope, it's just whatever Java source represents the context of the access expression
		values.put( "scopeReference", jContext.toString() );

		// TODO: I think this should use an explicit getStatic() method because the method we're using would also allow intance access, but if the dev used
		// :: then we REALLY should only be allowing static access here.
		String	template	= """
		                      Referencer.get(
		                      	${contextName},
		                      	BoxClassSupport.ensureClass(${contextName},${scopeReference},imports),
		                      	${accessKey},
		                      	${safe}
		                      			)
		                      	""";

		Node	javaExpr	= parseExpression( template, values );
		// logger.trace( node.getSourceText() + " -> " + javaExpr );
		addIndex( javaExpr, node );
		return javaExpr;
	}

}
