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
package ourtus.boxlang.transpiler.transformer.statement;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.stmt.BreakStmt;
import com.github.javaparser.ast.stmt.ContinueStmt;
import ourtus.boxlang.ast.BoxNode;
import ourtus.boxlang.transpiler.transformer.AbstractTransformer;
import ourtus.boxlang.transpiler.transformer.TransformerContext;

public class BoxContinueTransformer extends AbstractTransformer {
	@Override
	public Node transform(BoxNode node, TransformerContext context) throws IllegalStateException {
		return addIndex(new ContinueStmt(),node);
	}
}
