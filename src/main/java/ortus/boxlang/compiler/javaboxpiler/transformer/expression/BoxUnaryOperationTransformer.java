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

import java.util.HashMap;
import java.util.Map;

import com.github.javaparser.ast.Node;

import ortus.boxlang.compiler.ast.BoxExpression;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.expression.BoxAccess;
import ortus.boxlang.compiler.ast.expression.BoxDotAccess;
import ortus.boxlang.compiler.ast.expression.BoxIdentifier;
import ortus.boxlang.compiler.ast.expression.BoxParenthesis;
import ortus.boxlang.compiler.ast.expression.BoxScope;
import ortus.boxlang.compiler.ast.expression.BoxUnaryOperation;
import ortus.boxlang.compiler.ast.expression.BoxUnaryOperator;
import ortus.boxlang.compiler.javaboxpiler.JavaTranspiler;
import ortus.boxlang.compiler.javaboxpiler.transformer.AbstractTransformer;
import ortus.boxlang.compiler.javaboxpiler.transformer.TransformerContext;
import ortus.boxlang.runtime.config.util.PlaceholderHelper;
import ortus.boxlang.runtime.types.exceptions.ExpressionException;

/**
 * Transform a BoxUnaryOperatorTransformer Node the equivalent Java Parser AST node
 */
public class BoxUnaryOperationTransformer extends AbstractTransformer {

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
		BoxExpression		expr		= operation.getExpr();

		String				template	= "";
		Map<String, String>	values		= new HashMap<>() {

											{
												put( "contextName", transpiler.peekContextName() );
											}
										};

		// Outer parenthesis are useless at this point, so unwrap them until we get to something other than BoxParenthesis
		while ( expr instanceof BoxParenthesis boxparen ) {
			expr = boxparen.getExpression();
		}

		// for non literals, we need to identify the key being incremented/decremented and the object it lives in (which may be a scope)
		if ( expr instanceof BoxIdentifier id && operator != BoxUnaryOperator.Not && operator != BoxUnaryOperator.Minus && operator != BoxUnaryOperator.Plus ) {
			Node accessKey;
			template	= getMethodCallTemplateCompound( operation );
			accessKey	= createKey( id.getName() );
			values.put( "accessKey", accessKey.toString() );
			String obj = PlaceholderHelper.resolve(
			    "${contextName}.scopeFindNearby( ${accessKey}, null, true ).scope()",
			    values );
			values.put( "obj", obj );
		} else if ( expr instanceof BoxAccess objectAccess && operator != BoxUnaryOperator.Not && operator != BoxUnaryOperator.Minus
		    && operator != BoxUnaryOperator.Plus ) {
			Node accessKey;
			template = getMethodCallTemplateCompound( operation );
			values.put( "obj", transpiler.transform( objectAccess.getContext() ).toString() );
			// DotAccess just uses the string directly, array access allows any expression
			if ( objectAccess instanceof BoxDotAccess dotAccess ) {
				accessKey = createKey( ( ( BoxIdentifier ) dotAccess.getAccess() ).getName() );
			} else {
				accessKey = createKey( objectAccess.getAccess() );
			}
			values.put( "accessKey", accessKey.toString() );
		} else if ( expr instanceof BoxScope ) {
			throw new ExpressionException( "You cannot perform an increment/decrement operation on a " + expr.getClass().getSimpleName() + " expression.",
			    expr.getPosition(), expr.getSourceText() );
		} else {
			// +5, -6, or !true are "simple" use cases, same with ++5, --5, 5++, 5--, (something)++ (-5)-- ++foo() foo.bar()--
			template = getMethodCallTemplateSimple( operation );
			values.put( "expr", transpiler.transform( expr ).toString() );
		}

		Node javaExpr = parseExpression( template, values );
		// logger.trace( "{} -> {}", node.getSourceText(), javaExpr );
		addIndex( javaExpr, node );
		return javaExpr;
	}

	private String getMethodCallTemplateSimple( BoxUnaryOperation operation ) {
		BoxUnaryOperator operator = operation.getOperator();
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
			case BitwiseComplement -> "BitwiseComplement.invoke( ${expr} )";
			default -> throw new ExpressionException( "Unknown unary operator " + operator.toString(), operation.getPosition(), operation.getSourceText() );
		};
	}

	private String getMethodCallTemplateCompound( BoxUnaryOperation operation ) {
		BoxUnaryOperator operator = operation.getOperator();
		// These all refernce variable names which need retrieved, modified, and then re-assigned
		return switch ( operator ) {
			case PrePlusPlus -> "Increment.invokePre( ${contextName}, ${obj}, ${accessKey} )";
			case PostPlusPlus -> "Increment.invokePost( ${contextName}, ${obj}, ${accessKey} )";
			case PreMinusMinus -> "Decrement.invokePre( ${contextName}, ${obj}, ${accessKey} )";
			case PostMinusMinus -> "Decrement.invokePost( ${contextName}, ${obj}, ${accessKey} )";
			default -> throw new ExpressionException( "Unknown unary compound operator " + operator.toString(), operation.getPosition(),
			    operation.getSourceText() );
		};
	}

}
