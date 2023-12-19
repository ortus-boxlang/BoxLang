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
package ortus.boxlang.transpiler.transformer.expression;

import java.util.HashMap;
import java.util.Map;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;

import ortus.boxlang.ast.BoxExpr;
import ortus.boxlang.ast.BoxNode;
import ortus.boxlang.ast.expression.BoxIdentifier;
import ortus.boxlang.ast.expression.BoxStructLiteral;
import ortus.boxlang.ast.expression.BoxStructType;
import ortus.boxlang.transpiler.JavaTranspiler;
import ortus.boxlang.transpiler.transformer.AbstractTransformer;
import ortus.boxlang.transpiler.transformer.TransformerContext;

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
				logger.info( "{} -> {}", node.getSourceText(), javaExpr );
				addIndex( javaExpr, node );
				return javaExpr;
			}

			MethodCallExpr javaExpr = ( MethodCallExpr ) parseExpression( "Struct.of()", values );
			for ( BoxExpr expr : structLiteral.getValues() ) {
				Expression value;
				if ( expr instanceof BoxIdentifier ) {
					value = new StringLiteralExpr( expr.getSourceText() );
				} else {
					value = ( Expression ) transpiler.transform( expr, context );
				}
				javaExpr.getArguments().add( value );
			}
			logger.info( "{} -> {}", node.getSourceText(), javaExpr );
			addIndex( javaExpr, node );
			return javaExpr;
		} else {
			if ( empty ) {
				Node javaExpr = parseExpression( "new Struct( Struct.Type.LINKED )", values );
				logger.info( "{} -> {}", node.getSourceText(), javaExpr );
				addIndex( javaExpr, node );
				return javaExpr;
			}

			MethodCallExpr javaExpr = ( MethodCallExpr ) parseExpression( "Struct.linkedOf()", values );
			for ( BoxExpr expr : structLiteral.getValues() ) {
				Expression value;
				if ( expr instanceof BoxIdentifier ) {
					value = new StringLiteralExpr( expr.getSourceText() );
				} else {
					value = ( Expression ) transpiler.transform( expr, context );
				}
				javaExpr.getArguments().add( value );
			}
			logger.info( "{} -> {}", node.getSourceText(), javaExpr );
			addIndex( javaExpr, node );
			return javaExpr;
		}

	}
}
