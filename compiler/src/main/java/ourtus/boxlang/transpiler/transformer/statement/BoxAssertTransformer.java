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
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ourtus.boxlang.ast.BoxNode;
import ourtus.boxlang.transpiler.BoxLangTranspiler;
import ourtus.boxlang.transpiler.transformer.AbstractTransformer;
import ourtus.boxlang.transpiler.transformer.TransformerContext;
import ourtus.boxlang.ast.statement.BoxAssert;

import java.util.HashMap;
import java.util.Map;

/**
 * Transform a BoxAssert Node the equivalent Java Parser AST nodes
 */
public class BoxAssertTransformer extends AbstractTransformer {

	Logger logger = LoggerFactory.getLogger( BoxAssertTransformer.class );

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
		Expression			expr		= ( Expression ) BoxLangTranspiler.transform( boxAssert.getExpression(), TransformerContext.RIGHT );
		Map<String, String>	values		= new HashMap<>() {

											{
												put( "expr", expr.toString() );

											}
										};
		String				template	= "Assert.invoke(${expr});";
		Node				javaStmt	= parseStatement( template, values );
		logger.info( node.getSourceText() + " -> " + javaStmt );
		addIndex( javaStmt, node );
		return javaStmt;

	}
}
