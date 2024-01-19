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

import ortus.boxlang.ast.BoxNode;
import ortus.boxlang.ast.statement.BoxAssert;
import ortus.boxlang.transpiler.JavaTranspiler;
import ortus.boxlang.transpiler.transformer.AbstractTransformer;
import ortus.boxlang.transpiler.transformer.TransformerContext;

/**
 * Transform a BoxAssert Node the equivalent Java Parser AST nodes
 */
public class BoxAssertTransformer extends AbstractTransformer {

	public BoxAssertTransformer( JavaTranspiler transpiler ) {
		super( transpiler );
	}

	/**
	 * Transform an assert statement
	 *
	 * @param node    a BoxAssert instance
	 * @param context transformation context
	 *
	 * @return Generates a Method invocation to the Runtime Assert
	 *
	 * @throws IllegalStateException
	 */
	@Override
	public Node transform( BoxNode node, TransformerContext context ) throws IllegalStateException {
		BoxAssert			boxAssert	= ( BoxAssert ) node;
		Expression			expr		= ( Expression ) transpiler.transform( boxAssert.getExpression(), TransformerContext.RIGHT );
		Map<String, String>	values		= new HashMap<>() {

											{
												put( "expr", expr.toString() );
												put( "contextName", transpiler.peekContextName() );

											}
										};
		String				template	= "Assert.invoke(context,${expr});";
		Node				javaStmt	= parseStatement( template, values );
		logger.debug( node.getSourceText() + " -> " + javaStmt );
		addIndex( javaStmt, node );
		return javaStmt;

	}
}
