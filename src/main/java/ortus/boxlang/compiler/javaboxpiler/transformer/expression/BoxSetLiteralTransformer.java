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

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;

import ortus.boxlang.compiler.ast.BoxExpression;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.expression.BoxSetLiteral;
import ortus.boxlang.compiler.ast.expression.BoxSpreadExpression;
import ortus.boxlang.compiler.javaboxpiler.JavaTranspiler;
import ortus.boxlang.compiler.javaboxpiler.transformer.AbstractTransformer;
import ortus.boxlang.compiler.javaboxpiler.transformer.TransformerContext;

/**
 * Transpiles {@code set{...}} literals into a
 * {@code LiteralSpreadUtil.set(value1, value2, ...)} call which builds a
 * {@link ortus.boxlang.runtime.types.BoxSet} at runtime.
 */
public class BoxSetLiteralTransformer extends AbstractTransformer {

	public BoxSetLiteralTransformer( JavaTranspiler transpiler ) {
		super( transpiler );
	}

	@Override
	public Node transform( BoxNode node, TransformerContext context ) throws IllegalStateException {
		BoxSetLiteral	setLiteral	= ( BoxSetLiteral ) node;
		MethodCallExpr	javaExpr	= ( MethodCallExpr ) parseExpression(
		    "ortus.boxlang.runtime.dynamic.LiteralSpreadUtil.set()", java.util.Map.of() );

		for ( BoxExpression expr : setLiteral.getValues() ) {
			if ( expr instanceof BoxSpreadExpression spread ) {
				MethodCallExpr spreadExpr = ( MethodCallExpr ) parseExpression(
				    "ortus.boxlang.runtime.dynamic.LiteralSpreadUtil.spread()", java.util.Map.of() );
				spreadExpr.getArguments().add( ( Expression ) transpiler.transform( spread.getExpression(), context ) );
				javaExpr.getArguments().add( spreadExpr );
			} else {
				javaExpr.getArguments().add( ( Expression ) transpiler.transform( expr, context ) );
			}
		}

		addIndex( javaExpr, node );
		return javaExpr;
	}
}
