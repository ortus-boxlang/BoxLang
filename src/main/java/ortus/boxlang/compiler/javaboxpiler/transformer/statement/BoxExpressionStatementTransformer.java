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
package ortus.boxlang.compiler.javaboxpiler.transformer.statement;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.ExpressionStmt;

import ortus.boxlang.compiler.ast.BoxExpression;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.expression.BoxBooleanLiteral;
import ortus.boxlang.compiler.ast.expression.BoxDecimalLiteral;
import ortus.boxlang.compiler.ast.expression.BoxIntegerLiteral;
import ortus.boxlang.compiler.ast.expression.BoxNull;
import ortus.boxlang.compiler.ast.expression.BoxParenthesis;
import ortus.boxlang.compiler.ast.expression.BoxStringLiteral;
import ortus.boxlang.compiler.ast.statement.BoxExpressionStatement;
import ortus.boxlang.compiler.javaboxpiler.JavaTranspiler;
import ortus.boxlang.compiler.javaboxpiler.transformer.AbstractTransformer;
import ortus.boxlang.compiler.javaboxpiler.transformer.TransformerContext;

public class BoxExpressionStatementTransformer extends AbstractTransformer {

	public BoxExpressionStatementTransformer( JavaTranspiler transpiler ) {
		super( transpiler );
	}

	@Override
	public Node transform( BoxNode node, TransformerContext context ) throws IllegalStateException {
		BoxExpressionStatement	exprStmt	= ( BoxExpressionStatement ) node;
		BoxExpression			expr		= exprStmt.getExpression();
		Expression				javaExpr;

		// Java doesn't allow parenthetical statements in places that BoxLang would allow them
		// as such we need to unnest the parenthesis and just provide the expression itself
		while ( expr instanceof BoxParenthesis bpExpr ) {
			expr = bpExpr.getExpression();
		}

		if ( expr instanceof BoxIntegerLiteral || expr instanceof BoxStringLiteral || expr instanceof BoxBooleanLiteral || expr instanceof BoxDecimalLiteral
		    || expr instanceof BoxNull ) {
			// Java doesn't allow literal statements, so we will wrap them up in a form that Java will accept as an expression statement
			// ObjectRef.echoValue( expr )
			javaExpr = new MethodCallExpr( new NameExpr( "ObjectRef" ), "echoValue" ).addArgument( ( Expression ) transpiler.transform( expr ) );
		} else {
			javaExpr = ( Expression ) transpiler.transform( expr );
		}

		return addIndex( new ExpressionStmt( javaExpr ), node );
	}
}
