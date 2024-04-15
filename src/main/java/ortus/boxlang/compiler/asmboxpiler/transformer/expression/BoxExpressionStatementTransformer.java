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
package ortus.boxlang.compiler.asmboxpiler.transformer.expression;

import org.objectweb.asm.tree.AbstractInsnNode;
import ortus.boxlang.compiler.asmboxpiler.AsmTranspiler;
import ortus.boxlang.compiler.asmboxpiler.transformer.AbstractTransformer;
import ortus.boxlang.compiler.ast.BoxExpression;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.expression.BoxParenthesis;
import ortus.boxlang.compiler.ast.statement.BoxExpressionStatement;

import java.util.List;

public class BoxExpressionStatementTransformer extends AbstractTransformer {

	public BoxExpressionStatementTransformer( AsmTranspiler transpiler ) {
		super( transpiler );
	}

	@Override
	public List<AbstractInsnNode> transform(BoxNode node ) throws IllegalStateException {
		BoxExpressionStatement	exprStmt	= ( BoxExpressionStatement ) node;
		BoxExpression			expr		= exprStmt.getExpression();

		// Java doesn't allow parenthetical statements in places that BoxLang would allow them
		// as such we need to unnest the parenthesis and just provide the expression itself
		while ( expr instanceof BoxParenthesis bpExpr ) {
			expr = bpExpr.getExpression();
		}

		return transpiler.transform( expr );
	}
}
