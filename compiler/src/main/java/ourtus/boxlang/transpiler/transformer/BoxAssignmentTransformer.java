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
package ourtus.boxlang.transpiler.transformer;

import ourtus.boxlang.ast.BoxNode;
import com.github.javaparser.ast.*;
import com.github.javaparser.ast.expr.Expression;
import ourtus.boxlang.ast.statement.BoxAssignment;
import ourtus.boxlang.transpiler.BoxLangTranspiler;

import java.util.HashMap;
import java.util.Map;

public class BoxAssignmentTransformer extends AbstractTransformer{

	public BoxAssignmentTransformer() { }
	@Override
	public Node transform(BoxNode node) throws IllegalStateException {
		Expression left = (Expression) BoxLangTranspiler.transform(((BoxAssignment)node).getLeft());
		Expression right = (Expression) BoxLangTranspiler.transform(((BoxAssignment)node).getRight());
		Map<String, String> values = new HashMap<>() {{
			put("left", left.toString());
			put("right", right.toString());

		}};
		String template = "variableScope.put(${left},${right});";
		return parseStatement(template,values);

	}


}
