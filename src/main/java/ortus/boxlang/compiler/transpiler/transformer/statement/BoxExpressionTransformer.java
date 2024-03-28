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
package ortus.boxlang.compiler.transpiler.transformer.statement;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.stmt.ExpressionStmt;

import ortus.boxlang.compiler.ast.BoxExpr;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.expression.BoxParenthesis;
import ortus.boxlang.compiler.ast.statement.BoxExpression;
import ortus.boxlang.compiler.transpiler.JavaTranspiler;
import ortus.boxlang.compiler.transpiler.transformer.AbstractTransformer;
import ortus.boxlang.compiler.transpiler.transformer.TransformerContext;

public class BoxExpressionTransformer extends AbstractTransformer {

	public BoxExpressionTransformer( JavaTranspiler transpiler ) {
		super( transpiler );
	}

	@Override
	public Node transform( BoxNode node, TransformerContext context ) throws IllegalStateException {
		BoxExpression	exprStmt	= ( BoxExpression ) node;
		BoxExpr			expr		= exprStmt.getExpression();

		// Java doesn't allow parenthetical statements in places that BoxLang would allow them
		// as such we need to unnest the parenthesis and just provide the expression itself
		while ( expr instanceof BoxParenthesis bpExpr ) {
			expr = bpExpr.getExpression();
		}

		Expression javaExpr = ( Expression ) transpiler.transform( expr );
		return addIndex( new ExpressionStmt( javaExpr ), node );
	}
}
