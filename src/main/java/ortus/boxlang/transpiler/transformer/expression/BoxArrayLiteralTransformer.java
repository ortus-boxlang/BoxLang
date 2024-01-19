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
import com.github.javaparser.ast.expr.MethodCallExpr;

import ortus.boxlang.ast.BoxExpr;
import ortus.boxlang.ast.BoxNode;
import ortus.boxlang.ast.expression.BoxArrayLiteral;
import ortus.boxlang.transpiler.JavaTranspiler;
import ortus.boxlang.transpiler.transformer.AbstractTransformer;
import ortus.boxlang.transpiler.transformer.TransformerContext;

/**
 * Transform a BoxArrayLiteral Node the equivalent Java Parser AST nodes
 * The array type in BoxLang is represented by the ortus.boxlang.runtime.types.Array
 * class, which implements the Java List interface,
 * and provides several methods of construction:
 *
 * <pre>
 * //empty array
 * new Array()
 * // From a native Java array
 * Array.fromArray(new Object[]{"foo","bar"})
 * // From another Java List
 * Array.fromList(List.of("foo","bar","baz"))
 * // Varargs
 * Array.of("foo","bar","baz")
 * </pre>
 */
public class BoxArrayLiteralTransformer extends AbstractTransformer {

	public BoxArrayLiteralTransformer( JavaTranspiler transpiler ) {
		super( transpiler );
	}

	/**
	 * Transform a BoxArrayLiteral expression
	 *
	 * @param node    a BoxNewOperation instance
	 * @param context transformation context
	 *
	 * @return generates the corresponding of runtime representation
	 *
	 * @throws IllegalStateException
	 */
	@Override
	public Node transform( BoxNode node, TransformerContext context ) throws IllegalStateException {
		BoxArrayLiteral		arrayLiteral	= ( BoxArrayLiteral ) node;
		Map<String, String>	values			= new HashMap<>() {

												{
													put( "contextName", transpiler.peekContextName() );
												}
											};

		if ( arrayLiteral.getValues().isEmpty() ) {
			Node javaExpr = parseExpression( "new Array()", values );
			logger.debug( "{} -> {}", node.getSourceText(), javaExpr );
			addIndex( javaExpr, node );
			return javaExpr;
		}
		MethodCallExpr javaExpr = ( MethodCallExpr ) parseExpression( "Array.of()", values );
		for ( BoxExpr expr : arrayLiteral.getValues() ) {
			Expression value = ( Expression ) transpiler.transform( expr, context );
			javaExpr.getArguments().add( value );
		}
		logger.debug( "{} -> {}", node.getSourceText(), javaExpr );
		addIndex( javaExpr, node );
		return javaExpr;

	}
}
