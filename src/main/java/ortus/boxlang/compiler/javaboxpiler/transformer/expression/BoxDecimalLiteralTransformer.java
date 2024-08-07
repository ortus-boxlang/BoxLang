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
import com.github.javaparser.ast.expr.ObjectCreationExpr;

import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.expression.BoxDecimalLiteral;
import ortus.boxlang.compiler.javaboxpiler.JavaTranspiler;
import ortus.boxlang.compiler.javaboxpiler.transformer.AbstractTransformer;
import ortus.boxlang.compiler.javaboxpiler.transformer.TransformerContext;

/**
 * Transform a BoxBooleanLiteral Node the equivalent Java Parser AST nodes
 */
public class BoxDecimalLiteralTransformer extends AbstractTransformer {

	public BoxDecimalLiteralTransformer( JavaTranspiler transpiler ) {
		super( transpiler );
	}

	/**
	 * Transform BoxDecimalLiteral argument
	 *
	 * @param node    a BoxDecimalLiteral instance
	 * @param context transformation context
	 *
	 * @return generates a Java Parser boolean Literal
	 */
	@Override
	public Node transform( BoxNode node, TransformerContext context ) throws IllegalStateException {
		BoxDecimalLiteral	literal		= ( BoxDecimalLiteral ) node;

		// The only guarantee to always retain the precision of the decimal is to use a BigDecimal, regardless of the "size" of the number
		// Do NOT enforce a match context precision here. Whatever the user typed is what they want.
		// Precision may be lost when they perform a math operation on this number, but don't throw away what they typed in the source.
		Expression			javaExpr	= new ObjectCreationExpr()
		    .setType( "java.math.BigDecimal" )
		    .addArgument( "\"" + literal.getValue() + "\"" );

		// logger.trace( node.getSourceText() + " -> " + javaExpr );
		addIndex( javaExpr, node );
		return javaExpr;
	}
}
