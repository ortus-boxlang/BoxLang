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
import ortus.boxlang.ast.expression.BoxTernaryOperation;
import ortus.boxlang.transpiler.JavaTranspiler;
import ortus.boxlang.transpiler.transformer.AbstractTransformer;
import ortus.boxlang.transpiler.transformer.TransformerContext;

public class BoxTernaryOperationTransformer extends AbstractTransformer {

	public BoxTernaryOperationTransformer( JavaTranspiler transpiler ) {
		super( transpiler );
	}

	@Override
	public Node transform( BoxNode node, TransformerContext context ) throws IllegalStateException {
		BoxTernaryOperation	operation	= ( BoxTernaryOperation ) node;
		Expression			condition	= ( Expression ) transpiler.transform( operation.getCondition() );
		Expression			whenTrue	= ( Expression ) transpiler.transform( operation.getWhenTrue() );
		Expression			whenFalse	= ( Expression ) transpiler.transform( operation.getWhenFalse() );
		Map<String, String>	values		= new HashMap<>() {

											{
												put( "condition", condition.toString() );
												put( "whenTrue", whenTrue.toString() );
												put( "whenFalse", whenFalse.toString() );
												put( "contextName", transpiler.peekContextName() );
											}
										};

		String				template	= "Ternary.invoke(${condition},${whenTrue},${whenFalse})";

		return parseExpression( template, values );
	}
}
