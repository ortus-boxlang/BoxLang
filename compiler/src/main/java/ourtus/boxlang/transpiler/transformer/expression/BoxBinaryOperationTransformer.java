/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package ourtus.boxlang.transpiler.transformer.expression;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;
import ourtus.boxlang.ast.BoxNode;
import ourtus.boxlang.ast.expression.BoxBinaryOperation;
import ourtus.boxlang.ast.expression.BoxBinaryOperator;
import ourtus.boxlang.transpiler.BoxLangTranspiler;
import ourtus.boxlang.transpiler.transformer.AbstractTransformer;
import ourtus.boxlang.transpiler.transformer.TransformerContext;

import java.util.HashMap;
import java.util.Map;

public class BoxBinaryOperationTransformer extends AbstractTransformer {
	@Override
	public Node transform(BoxNode node, TransformerContext context) throws IllegalStateException {
		BoxBinaryOperation operation = (BoxBinaryOperation)node;
		Expression left = (Expression) BoxLangTranspiler.transform(operation.getLeft());
		Expression right = (Expression) BoxLangTranspiler.transform(operation.getRight());

		Map<String, String> values = new HashMap<>() {{
			put("left", left.toString());
			put("right", right.toString());

		}};

		String template = "";
		if (operation.getOperator() == BoxBinaryOperator.Concat) {
			template = "Concat.invoke(${left},${right})";
		} else if (operation.getOperator() == BoxBinaryOperator.Plus) {
			template = "Plus.invoke(${left},${right})";
		} else if (operation.getOperator() == BoxBinaryOperator.Minus) {
			template = "Minus.invoke(${left},${right})";
		} else if (operation.getOperator() == BoxBinaryOperator.Star) {
			template = "Multiply.invoke(${left},${right})";
		} else if (operation.getOperator() == BoxBinaryOperator.Slash) {
			template = "Divide.invoke(${left},${right})";
		} else if (operation.getOperator() == BoxBinaryOperator.Contains) {
			template = "Contains.contains(${left},${right})";
		}
		return parseExpression(template,values);
	}

}
