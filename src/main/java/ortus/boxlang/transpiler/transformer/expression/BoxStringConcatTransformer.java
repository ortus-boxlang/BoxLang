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

import java.util.List;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;

import ortus.boxlang.ast.BoxNode;
import ortus.boxlang.ast.expression.BoxStringConcat;
import ortus.boxlang.transpiler.Transpiler;
import ortus.boxlang.transpiler.transformer.AbstractTransformer;
import ortus.boxlang.transpiler.transformer.TransformerContext;

/**
 * Transform a String Interpolatiion the equivalent Java Parser AST nodes
 */
public class BoxStringConcatTransformer extends AbstractTransformer {

	public BoxStringConcatTransformer( Transpiler transpiler ) {
		super( transpiler );
	}

	/**
	 * Transform a String interpolation expression
	 *
	 * @param node    a BoxStringInterpolation
	 * @param context transformation context
	 *
	 * @return a Java Parser Expression
	 *
	 * @throws IllegalStateException
	 */
	@Override
	public Node transform( BoxNode node, TransformerContext context ) throws IllegalStateException {
		BoxStringConcat	interpolation	= ( BoxStringConcat ) node;
		Node			javaExpr;
		if ( interpolation.getValues().size() == 1 ) {
			javaExpr = transpiler.transform( interpolation.getValues().get( 0 ), TransformerContext.RIGHT );
		} else {

			List<Expression>	operands		= interpolation.getValues()
			    .stream()
			    .map( it -> ( Expression ) transpiler.transform( it, TransformerContext.RIGHT ) )
			    .toList();

			NameExpr			nameExpr		= new NameExpr( "Concat" );
			MethodCallExpr		methodCallExpr	= new MethodCallExpr( nameExpr, "invoke" );
			operands.forEach( methodCallExpr::addArgument );
			javaExpr = methodCallExpr;
		}
		logger.atTrace().log( "{} -> {}", node.getSourceText(), javaExpr );
		addIndex( javaExpr, node );
		return javaExpr;
	}
}
