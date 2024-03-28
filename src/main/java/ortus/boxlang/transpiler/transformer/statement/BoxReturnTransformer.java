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

package ortus.boxlang.transpiler.transformer.statement;

import java.util.HashMap;
import java.util.Map;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.NullLiteralExpr;

import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.statement.BoxReturn;
import ortus.boxlang.transpiler.JavaTranspiler;
import ortus.boxlang.transpiler.transformer.AbstractTransformer;
import ortus.boxlang.transpiler.transformer.TransformerContext;

/**
 * Transform a Return Statement in the equivalent Java Parser AST nodes
 */
public class BoxReturnTransformer extends AbstractTransformer {

	public BoxReturnTransformer( JavaTranspiler transpiler ) {
		super( transpiler );
	}

	@Override
	public Node transform( BoxNode node, TransformerContext context ) throws IllegalStateException {
		BoxReturn			boxReturn	= ( BoxReturn ) node;

		Map<String, String>	values		= new HashMap<>() {

											{
												put( "contextName", transpiler.peekContextName() );
											}
										};
		if ( boxReturn.getExpression() == null ) {
			values.put( "expr", new NullLiteralExpr().toString() );
		} else {
			Expression expr = ( Expression ) transpiler.transform( boxReturn.getExpression(), TransformerContext.RIGHT );
			values.put( "expr", expr.toString() );
		}
		String template;
		if ( transpiler.isInsideComponent() ) {
			template = "return Component.BodyResult.ofReturn( ${expr} );";
		} else if ( transpiler.canReturn() ) {
			template = "return ${expr};";
		} else {
			template = "return;";
		}
		// Avoid unreachable statement error
		template = "if( true ) " + template;
		Node javaStmt = parseStatement( template, values );
		logger.atTrace().log( node.getSourceText() + " -> " + javaStmt );
		addIndex( javaStmt, node );
		return javaStmt;
	}

}
