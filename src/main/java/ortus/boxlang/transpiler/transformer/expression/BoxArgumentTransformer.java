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
import ortus.boxlang.ast.expression.BoxArgument;
import ortus.boxlang.transpiler.JavaTranspiler;
import ortus.boxlang.transpiler.transformer.AbstractTransformer;
import ortus.boxlang.transpiler.transformer.TransformerContext;

/**
 * Transform a BoxArgument Node the equivalent Java Parser AST nodes
 */
public class BoxArgumentTransformer extends AbstractTransformer {

	public BoxArgumentTransformer( JavaTranspiler transpiler ) {
		super( transpiler );
	}

	/**
	 * Transform a function/method argument
	 *
	 * @param node    a BoxArgument instance
	 * @param context transformation context
	 *
	 * @return Generates a Java Parser Expression
	 *
	 * @throws IllegalStateException
	 *
	 * @see BoxArgument
	 */
	@Override
	public Node transform( BoxNode node, TransformerContext context ) throws IllegalStateException {
		BoxArgument			arg			= ( BoxArgument ) node;
		String				side		= context == TransformerContext.NONE ? "" : "(" + context.toString() + ") ";
		Expression			expr		= ( Expression ) transpiler.transform( arg.getValue() );
		// TODO handle named parameters
		Map<String, String>	values		= new HashMap<>() {

											{
												put( "expr", expr.toString() );
												put( "contextName", transpiler.peekContextName() );
											}
										};

		String				template	= "${expr}";

		Node				javaExpr	= parseExpression( template, values );
		logger.info( side + node.getSourceText() + " -> " + javaExpr );

		return javaExpr;
	}
}
