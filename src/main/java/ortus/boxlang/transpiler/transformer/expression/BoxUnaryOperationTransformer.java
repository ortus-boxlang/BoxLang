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
package ortus.boxlang.transpiler.transformer.expression;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.ast.Node;

import ortus.boxlang.ast.BoxExpr;
import ortus.boxlang.ast.BoxNode;
import ortus.boxlang.ast.expression.BoxAccess;
import ortus.boxlang.ast.expression.BoxDotAccess;
import ortus.boxlang.ast.expression.BoxIdentifier;
import ortus.boxlang.ast.expression.BoxUnaryOperation;
import ortus.boxlang.ast.expression.BoxUnaryOperator;
import ortus.boxlang.runtime.config.util.PlaceholderHelper;
import ortus.boxlang.runtime.types.exceptions.ExpressionException;
import ortus.boxlang.transpiler.JavaTranspiler;
import ortus.boxlang.transpiler.transformer.AbstractTransformer;
import ortus.boxlang.transpiler.transformer.TransformerContext;

/**
 * Transform a BoxUnaryOperatorTransformer Node the equivalent Java Parser AST node
 */
public class BoxUnaryOperationTransformer extends AbstractTransformer {

	Logger logger = LoggerFactory.getLogger( BoxUnaryOperationTransformer.class );

	public BoxUnaryOperationTransformer( JavaTranspiler transpiler ) {
		super( transpiler );
	}

	/**
	 * Transform a unary operator
	 *
	 * @param node    a BoxUnaryOperator instance
	 * @param context transformation context
	 *
	 * @return Generates a Method invocation to the Runtime Increment/Increment
	 *
	 * @throws IllegalStateException
	 */
	@Override
	public Node transform( BoxNode node, TransformerContext context ) throws IllegalStateException {
		BoxUnaryOperation	operation	= ( BoxUnaryOperation ) node;
		BoxUnaryOperator	operator	= operation.getOperator();
		BoxExpr				expr		= operation.getExpr();

		String				template	= "";
		Map<String, String>	values		= new HashMap<>() {

											{
												put( "contextName", transpiler.peekContextName() );
											}
										};

		// +5, -6, or !true are "simple" use cases, same with ++5, --5, 5++, 5--
		if ( operator == BoxUnaryOperator.Plus || operator == BoxUnaryOperator.Minus || operator == BoxUnaryOperator.Not || operation.getExpr().isLiteral() ) {
			template = getMethodCallTemplateSimple( operator );
			values.put( "expr", resolveScope( transpiler.transform( expr ), context ).toString() );
		} else {
			Node accessKey;
			template = getMethodCallTemplateCompound( operator );
			// for non literals, we need to identify the key being incremented/decremented and the object it lives in (which may be a scope)
			if ( expr instanceof BoxIdentifier id ) {
				accessKey = createKey( id.getName() );
				values.put( "accessKey", accessKey.toString() );
				String obj = PlaceholderHelper.resolve(
				    "${contextName}.scopeFindNearby( ${accessKey}, null ).scope()",
				    values );
				values.put( "obj", obj );
			} else if ( expr instanceof BoxAccess objectAccess ) {
				values.put( "obj", transpiler.transform( objectAccess.getContext() ).toString() );
				// DotAccess just uses the string directly, array access allows any expression
				if ( objectAccess instanceof BoxDotAccess dotAccess ) {
					accessKey = createKey( ( ( BoxIdentifier ) dotAccess.getAccess() ).getName() );
				} else {
					accessKey = createKey( objectAccess.getAccess() );
				}
				values.put( "accessKey", accessKey.toString() );
			} else {
				throw new ExpressionException( "You cannot perform an increment/decrement operation on a " + expr.getClass().getSimpleName() + " expression." );
			}

		}

		Node javaExpr = parseExpression( template, values );
		logger.info( "{} -> {}", node.getSourceText(), javaExpr );
		addIndex( javaExpr, node );
		return javaExpr;
	}

	private String getMethodCallTemplateSimple( BoxUnaryOperator operator ) {
		return switch ( operator ) {
			// +5 or +tmp is the same as just 5 or tmp
			case Plus -> "${expr}";
			case Minus -> "Negate.invoke( ${expr} )";
			case Not -> "Not.invoke( ${expr} )";
			case PrePlusPlus -> "Increment.invoke( ${expr} )";
			// 5++ is the same as 5
			case PostPlusPlus -> "${expr}";
			case PreMinusMinus -> "Decrement.invoke( ${expr} )";
			// 5-- is the same as 5
			case PostMinusMinus -> "${expr}";
			default -> throw new ExpressionException( "Unknown unary operator " + operator.toString() );
		};
	}

	private String getMethodCallTemplateCompound( BoxUnaryOperator operator ) {
		// These all refernce variable names which need retrieved, modified, and then re-assigned
		return switch ( operator ) {
			case PrePlusPlus -> "Increment.invokePre( ${obj}, ${accessKey} )";
			case PostPlusPlus -> "Increment.invokePost( ${obj}, ${accessKey} )";
			case PreMinusMinus -> "Decrement.invokePre( ${obj}, ${accessKey} )";
			case PostMinusMinus -> "Decrement.invokePost( ${obj}, ${accessKey} )";
			default -> throw new ExpressionException( "Unknown unary compound operator " + operator.toString() );
		};
	}

}
