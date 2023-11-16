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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;

import ortus.boxlang.ast.BoxExpr;
import ortus.boxlang.ast.BoxNode;
import ortus.boxlang.ast.statement.BoxAssignment;
import ortus.boxlang.ast.statement.BoxAssigmentOperator;
import ortus.boxlang.runtime.types.exceptions.ApplicationException;
import ortus.boxlang.transpiler.JavaTranspiler;
import ortus.boxlang.transpiler.transformer.AbstractTransformer;
import ortus.boxlang.transpiler.transformer.TransformerContext;

public class BoxAssignmentTransformer extends AbstractTransformer {

	Logger logger = LoggerFactory.getLogger( BoxAssignmentTransformer.class );

	public BoxAssignmentTransformer() {
	}

	@Override
	public Node transform( BoxNode node, TransformerContext context ) throws IllegalStateException {
		logger.info( node.getSourceText() );
		BoxAssignment	assigment	= ( BoxAssignment ) node;
		Expression		right		= ( Expression ) JavaTranspiler.transform( assigment.getRight(), TransformerContext.RIGHT );
		BlockStmt		blockStmt	= new BlockStmt();
		for ( BoxExpr expr : assigment.getLeft() ) {

			Expression			left		= ( Expression ) JavaTranspiler.transform( expr, TransformerContext.LEFT );
			Map<String, String>	values		= new HashMap<>();
			ExpressionStmt		javaExpr;
			String template;

			if ( left instanceof MethodCallExpr method ) {
				if ( "assign".equalsIgnoreCase( method.getName().asString() ) ) {
					method.getArguments().add( right );
				}
				if ( "setDeep".equalsIgnoreCase( method.getName().asString() ) ) {
					method.getArguments().add( 1, right );
				}
				values.put("expr", method.getScope().orElseThrow().toString());
				values.put("key", method.getArguments().get(0).toString());
				values.put("right", right.toString());
				template = getMethodCallTemplate(assigment.getOp());

			} else if ( left instanceof NameExpr name ) {
				values.put("left", left.toString());
				values.put("right", right.toString());
				if (right instanceof NameExpr rname) {
					String tmp = "context.scopeFindNearby( Key.of( \"" + rname + "\" ), null).value()";
					values.put("right", tmp);
				}

				template = getNameExpressionTemplate(assigment.getOp());
			} else {
				throw new ApplicationException( "Unimplemented assignment operator type assignment operator type", expr.toString() );
			}
			javaExpr = new ExpressionStmt((Expression) parseExpression(template, values));
			blockStmt.addStatement( javaExpr );
			addIndex( javaExpr, node );
		}

		return blockStmt;
	}

	private String getNameExpressionTemplate(BoxAssigmentOperator operator ){
		return switch( operator ){
			case PlusEqual -> "Plus.invoke(context.scopeFindNearby(Key.of( \"${left}\" ),null).scope(),Key.of( \"${left}\"),${right})";
			case MinusEqual -> "Minus.invoke(context.scopeFindNearby(Key.of( \"${left}\" ),null).scope(),Key.of( \"${left}\"),${right})";
			case StarEqual -> "Multiply.invoke(context.scopeFindNearby(Key.of( \"${left}\" ),null).scope(),Key.of( \"${left}\"),${right})";
			case SlashEqual -> "Divide.invoke(context.scopeFindNearby(Key.of( \"${left}\" ),null).scope(),Key.of( \"${left}\"),${right})";
			case ConcatEqual -> "Concat.invoke(context.scopeFindNearby(Key.of( \"${left}\" ),null).scope(),Key.of( \"${left}\"),${right})";
			default -> """
						  context.scopeFindNearby(Key.of( "${left}" ),context.getDefaultAssignmentScope()).scope().assign( Key.of( "${left}" ), ${right} )
						  """;
		};
	}
	private String getMethodCallTemplate(BoxAssigmentOperator operator ){
		return switch( operator ) {
			case PlusEqual -> "Plus.invoke(${expr},${key},${right})";
			case MinusEqual -> "Minus.invoke(${expr},${key},${right})";
			case StarEqual -> "Multiply.invoke(${expr},${key},${right})";
			case SlashEqual -> "Divide.invoke(${expr},${key},${right})";
			case ModEqual -> "Modulus.invoke(${expr},${key},${right})";
			case ConcatEqual -> "Concat.invoke(${expr},${key},${right})";
			default -> throw new ApplicationException( "Unimplemented assignment operator method call", operator.toString());
		};
	}

}