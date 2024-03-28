/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ortus.boxlang.compiler.javaboxpiler.transformer.expression;

import java.util.HashMap;
import java.util.Map;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;

import ortus.boxlang.compiler.ast.BoxExpr;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.expression.BoxIdentifier;
import ortus.boxlang.compiler.ast.expression.BoxScope;
import ortus.boxlang.compiler.ast.expression.BoxStructLiteral;
import ortus.boxlang.compiler.ast.expression.BoxStructType;
import ortus.boxlang.compiler.javaboxpiler.JavaTranspiler;
import ortus.boxlang.compiler.javaboxpiler.transformer.AbstractTransformer;
import ortus.boxlang.compiler.javaboxpiler.transformer.TransformerContext;

/**
 * Transform a BoxStructUnorderedLiteral Node the equivalent Java Parser AST nodes
 */
public class BoxStructLiteralTransformer extends AbstractTransformer {

	public BoxStructLiteralTransformer( JavaTranspiler transpiler ) {
		super( transpiler );
	}

	/**
	 * Transform a BoxStructOrderedLiteral expression
	 *
	 * @param node    a BoxNewOperation instance
	 * @param context transformation context
	 *
	 * @return generates the corresponding of runtime representation
	 *
	 * @throws IllegalStateException
	 */
	@Override
	public Node transform( BoxNode node, TransformerContext context ) throws IllegalStateException {
		BoxStructLiteral	structLiteral	= ( BoxStructLiteral ) node;
		Map<String, String>	values			= new HashMap<>() {

												{
													put( "contextName", transpiler.peekContextName() );
												}
											};
		boolean				empty			= structLiteral.getValues().isEmpty();

		if ( structLiteral.getType() == BoxStructType.Unordered ) {
			if ( empty ) {
				Node javaExpr = parseExpression( "new Struct()", values );
				logger.atTrace().log( "{} -> {}", node.getSourceText(), javaExpr );
				addIndex( javaExpr, node );
				return javaExpr;
			}

			MethodCallExpr	javaExpr	= ( MethodCallExpr ) parseExpression( "Struct.of()", values );
			int				i			= 1;
			for ( BoxExpr expr : structLiteral.getValues() ) {
				Expression value;
				if ( expr instanceof BoxIdentifier && i % 2 != 0 ) {
					// { foo : "bar" }
					value = new StringLiteralExpr( expr.getSourceText() );
				} else if ( expr instanceof BoxScope && i % 2 != 0 ) {
					// { this : "bar" }
					value = new StringLiteralExpr( expr.getSourceText() );
				} else {
					// { "foo" : "bar" }
					value = ( Expression ) transpiler.transform( expr, context );
				}
				javaExpr.getArguments().add( value );
				i++;
			}
			logger.atTrace().log( "{} -> {}", node.getSourceText(), javaExpr );
			addIndex( javaExpr, node );
			return javaExpr;
		} else {
			if ( empty ) {
				Node javaExpr = parseExpression( "new Struct( Struct.TYPES.LINKED )", values );
				logger.atTrace().log( "{} -> {}", node.getSourceText(), javaExpr );
				addIndex( javaExpr, node );
				return javaExpr;
			}

			MethodCallExpr	javaExpr	= ( MethodCallExpr ) parseExpression( "Struct.linkedOf()", values );
			int				i			= 1;
			for ( BoxExpr expr : structLiteral.getValues() ) {
				Expression value;
				if ( expr instanceof BoxIdentifier && i % 2 != 0 ) {
					// { foo : "bar" }
					value = new StringLiteralExpr( expr.getSourceText() );
				} else if ( expr instanceof BoxScope && i % 2 != 0 ) {
					// { this : "bar" }
					value = new StringLiteralExpr( expr.getSourceText() );
				} else {
					// { "foo" : "bar" }
					value = ( Expression ) transpiler.transform( expr, context );
				}
				javaExpr.getArguments().add( value );
				i++;
			}
			logger.atTrace().log( "{} -> {}", node.getSourceText(), javaExpr );
			addIndex( javaExpr, node );
			return javaExpr;
		}

	}
}
