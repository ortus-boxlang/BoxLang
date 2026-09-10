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
import ortus.boxlang.compiler.ast.expression.BoxIdentifier;
import ortus.boxlang.compiler.ast.expression.BoxScope;
import ortus.boxlang.compiler.ast.expression.BoxSpreadExpression;
import ortus.boxlang.compiler.ast.expression.BoxStructLiteral;
import ortus.boxlang.compiler.ast.expression.BoxStructType;
import ortus.boxlang.compiler.javaboxpiler.JavaTranspiler;
import ortus.boxlang.compiler.javaboxpiler.transformer.AbstractTransformer;
import ortus.boxlang.compiler.javaboxpiler.transformer.TransformerContext;

/**
 * Transform a BoxStructUnorderedLiteral Node the equivalent Java Parser AST nodes
 */
public class BoxStructLiteralTransformer extends AbstractTransformer {

	public BoxStructLiteralTransformer( JavaTranspiler transpiler ) {
		super( transpiler );
	}

	/**
	 * Transform a BoxStructOrderedLiteral expression
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
		BoxStructLiteral	structLiteral	= ( BoxStructLiteral ) node;
		String				structType		= structLiteral.getType() == BoxStructType.Ordered ? "LINKED" : "DEFAULT";

		MethodCallExpr		javaExpr		= ( MethodCallExpr ) parseExpression(
		    "ortus.boxlang.runtime.dynamic.LiteralSpreadUtil.struct( IStruct.TYPES." + structType + " )",
		    java.util.Map.of() );

		for ( int i = 0; i < structLiteral.getValues().size(); ) {
			BoxExpression current = structLiteral.getValues().get( i );
			if ( current instanceof BoxSpreadExpression spread ) {
				MethodCallExpr spreadExpr = ( MethodCallExpr ) parseExpression( "ortus.boxlang.runtime.dynamic.LiteralSpreadUtil.spread()",
				    java.util.Map.of() );
				spreadExpr.getArguments().add( ( Expression ) transpiler.transform( spread.getExpression(), context ) );
				javaExpr.getArguments().add( spreadExpr );
				i++;
				continue;
			}

			if ( i + 1 >= structLiteral.getValues().size() ) {
				throw new IllegalStateException( "Invalid struct literal data while transforming spread values." );
			}

			if ( current instanceof BoxIdentifier id ) {
				javaExpr.getArguments().add( createKey( id.getName() ) );
			} else if ( current instanceof BoxScope scope ) {
				javaExpr.getArguments().add( createKey( scope.getName() ) );
			} else {
				javaExpr.getArguments().add( ( Expression ) transpiler.transform( current, context ) );
			}
			javaExpr.getArguments().add( ( Expression ) transpiler.transform( structLiteral.getValues().get( i + 1 ), context ) );
			i += 2;
		}

		addIndex( javaExpr, node );
		return javaExpr;
	}
}
