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
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import ourtus.boxlang.ast.BoxNode;
import ourtus.boxlang.ast.expression.BoxBooleanLiteral;
import ourtus.boxlang.ast.expression.BoxStringLiteral;

public class BoxBooleanLiteralTransformer implements ourtus.boxlang.transpiler.transformer.Transformer {
	@Override
	public Node transform(BoxNode node) throws IllegalStateException {
		BoxBooleanLiteral literal = (BoxBooleanLiteral) node;
		return new BooleanLiteralExpr("true".equalsIgnoreCase(literal.getValue()));
	}
}
