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
import ortus.boxlang.ast.expression.BoxExpressionInvocation;
import ortus.boxlang.transpiler.JavaTranspiler;
import ortus.boxlang.transpiler.transformer.AbstractTransformer;
import ortus.boxlang.transpiler.transformer.TransformerContext;

public class BoxExpressionInvocationTransformer extends AbstractTransformer {

	public BoxExpressionInvocationTransformer( JavaTranspiler transpiler ) {
		super( transpiler );
	}

	@Override
	public Node transform( BoxNode node, TransformerContext context ) throws IllegalStateException {
		BoxExpressionInvocation	invocation	= ( BoxExpressionInvocation ) node;

		Expression				expr		= ( Expression ) transpiler.transform( invocation.getExpr(), context );

		String					args		= invocation.getArguments().stream()
		    .map( it -> transpiler.transform( it ).toString() )
		    .collect( Collectors.joining( ", " ) );

		String					template	= """
		                                      ${contextName}.invokeFunction(
		                                        ${expr},
		                                        new Object[] { ${args} }
		                                      )
		                                      """;

		Map<String, String>		values		= new HashMap<>() {

												{
													put( "contextName", transpiler.peekContextName() );
													put( "expr", expr.toString() );
													put( "args", args );
												}
											};

		Node					javaExpr	= parseExpression( template, values );
		logger.debug( node.getSourceText() + " -> " + javaExpr );
		addIndex( javaExpr, node );
		return javaExpr;
	}
}
