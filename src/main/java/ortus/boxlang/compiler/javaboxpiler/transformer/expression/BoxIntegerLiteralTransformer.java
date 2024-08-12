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
package ortus.boxlang.compiler.javaboxpiler.transformer.expression;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.LongLiteralExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;

import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.expression.BoxIntegerLiteral;
import ortus.boxlang.compiler.javaboxpiler.JavaTranspiler;
import ortus.boxlang.compiler.javaboxpiler.transformer.AbstractTransformer;
import ortus.boxlang.compiler.javaboxpiler.transformer.TransformerContext;

/**
 * Transform a BoxIntegerLiteral Node the equivalent Java Parser AST nodes
 */
public class BoxIntegerLiteralTransformer extends AbstractTransformer {

	public BoxIntegerLiteralTransformer( JavaTranspiler transpiler ) {
		super( transpiler );
	}

	/**
	 * Transform BoxIntegerLiteral argument
	 *
	 * @param node    a BoxIntegerLiteral instance
	 * @param context transformation context
	 *
	 * @return generates a Java Parser integer Literal
	 */
	@Override
	public Node transform( BoxNode node, TransformerContext context ) throws IllegalStateException {
		BoxIntegerLiteral	literal	= ( BoxIntegerLiteral ) node;
		int					len		= literal.getValue().length();
		Expression			javaExpr;
		// 10 or fewer chars can use an int literal
		if ( len <= 10 ) {
			javaExpr = new IntegerLiteralExpr( literal.getValue() );
		} else if ( len <= 19 ) {
			// 11-19 chars needs a long literal
			javaExpr = new LongLiteralExpr( literal.getValue() + "L" );
		} else {
			// 20 or more chars needs a BigDecimal
			// Do NOT enforce a match context precision here. Whatever the user typed is what they want.
			// Precision may be lost when they perform a math operation on this number, but don't throw away what they typed in the source.
			javaExpr = new ObjectCreationExpr()
			    .setType( "java.math.BigDecimal" )
			    .addArgument( "\"" + literal.getValue() + "\"" );
		}
		// logger.trace( node.getSourceText() + " -> " + javaExpr );
		addIndex( javaExpr, node );
		return javaExpr;
	}
}
