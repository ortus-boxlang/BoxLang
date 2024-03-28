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
package ortus.boxlang.compiler.transpiler.transformer.expression;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.StringLiteralExpr;

import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.expression.BoxStringLiteral;
import ortus.boxlang.compiler.transpiler.JavaTranspiler;
import ortus.boxlang.compiler.transpiler.transformer.AbstractTransformer;
import ortus.boxlang.compiler.transpiler.transformer.TransformerContext;

/**
 * Transform a BoxStringLiteral Node the equivalent Java Parser AST nodes
 */
public class BoxStringLiteralTransformer extends AbstractTransformer {

	public BoxStringLiteralTransformer( JavaTranspiler transpiler ) {
		super( transpiler );
	}

	/**
	 * Transform BoxStringLiteral argument
	 *
	 * @param node    a BoxStringLiteral instance
	 * @param context transformation context
	 *
	 * @return generates a Java Parser string Literal
	 */
	@Override
	public Node transform( BoxNode node, TransformerContext context ) throws IllegalStateException {
		BoxStringLiteral	literal	= ( BoxStringLiteral ) node;
		StringLiteralExpr	expr	= new StringLiteralExpr( escape( literal.getValue() ) );
		String				side	= context == TransformerContext.NONE ? "" : "(" + context.toString() + ") ";
		logger.atTrace().log( side + node.getSourceText() + " -> " + expr );
		return expr;
	}

	/**
	 * Escape a give String to make it safe to be printed or stored.
	 *
	 * @param s The input String.
	 *
	 * @return The output String.
	 **/
	private String escape( String s ) {
		return s.replace( "\\", "\\\\" )
		    .replace( "\t", "\\t" )
		    .replace( "\b", "\\b" )
		    .replace( "\n", "\\n" )
		    .replace( "\r", "\\r" )
		    .replace( "\f", "\\f" )
		    .replace( "\"", "\\\"" );
	}
}
