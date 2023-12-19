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
import java.util.stream.Collectors;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;

import ortus.boxlang.ast.BoxNode;
import ortus.boxlang.ast.expression.BoxIdentifier;
import ortus.boxlang.ast.expression.BoxMethodInvocation;
import ortus.boxlang.transpiler.JavaTranspiler;
import ortus.boxlang.transpiler.transformer.AbstractTransformer;
import ortus.boxlang.transpiler.transformer.TransformerContext;

public class BoxMethodInvocationTransformer extends AbstractTransformer {

	public BoxMethodInvocationTransformer( JavaTranspiler transpiler ) {
		super( transpiler );
	}

	@Override
	public Node transform( BoxNode node, TransformerContext context ) throws IllegalStateException {
		BoxMethodInvocation	invocation	= ( BoxMethodInvocation ) node;
		Boolean				safe		= invocation.isSafe() || context == TransformerContext.SAFE;
		String				side		= context == TransformerContext.NONE ? "" : "(" + context.toString() + ") ";

		Expression			expr		= ( Expression ) transpiler.transform( invocation.getObj(),
		    context );

		String				args		= invocation.getArguments().stream()
		    .map( it -> transpiler.transform( it ).toString() )
		    .collect( Collectors.joining( ", " ) );

		Map<String, String>	values		= new HashMap<>() {

											{
												put( "contextName", transpiler.peekContextName() );
												put( "safe", safe.toString() );
											}
										};

		String				target		= null;
		if ( invocation.getName() instanceof BoxIdentifier id ) {
			target = BoxBuiltinRegistry.getInstance().getRegistry().get( id.getName() );
		}

		values.put( "expr", expr.toString() );
		values.put( "args", args );

		String template;

		if ( target != null ) {
			template = "${expr}." + target;
		} else {
			Node accessKey;
			// DotAccess just uses the string directly, array access allows any expression
			if ( invocation.getUsedDotAccess() ) {
				accessKey = createKey( ( ( BoxIdentifier ) invocation.getName() ).getName() );
			} else {
				accessKey = createKey( invocation.getName() );
			}
			values.put( "methodKey", accessKey.toString() );
			template = """
			           Referencer.getAndInvoke(
			             context,
			             ${expr},
			             ${methodKey},
			             new Object[] { ${args} },
			             ${safe}
			           )
			           """;
		}
		Node javaExpr = parseExpression( template, values );
		logger.info( side + node.getSourceText() + " -> " + javaExpr );
		addIndex( javaExpr, node );
		return javaExpr;
	}
}
