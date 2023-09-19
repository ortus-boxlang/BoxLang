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

import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ortus.boxlang.ast.BoxNode;
import com.github.javaparser.ast.*;
import com.github.javaparser.ast.expr.Expression;
import ortus.boxlang.ast.statement.BoxAssigmentOperator;
import ortus.boxlang.ast.statement.BoxAssignment;
import ortus.boxlang.transpiler.BoxLangTranspiler;
import ortus.boxlang.transpiler.transformer.AbstractTransformer;
import ortus.boxlang.transpiler.transformer.TransformerContext;

import java.util.HashMap;
import java.util.Map;

public class BoxAssignmentTransformer extends AbstractTransformer {

	Logger logger = LoggerFactory.getLogger( BoxAssignmentTransformer.class );

	public BoxAssignmentTransformer() {
	}

	@Override
	public Node transform( BoxNode node, TransformerContext context ) throws IllegalStateException {
		logger.info( node.getSourceText() );
		BoxAssignment		assigment	= ( BoxAssignment ) node;
		Expression			left		= ( Expression ) BoxLangTranspiler.transform( ( ( BoxAssignment ) node ).getLeft(), TransformerContext.LEFT );
		Expression			right		= ( Expression ) BoxLangTranspiler.transform( ( ( BoxAssignment ) node ).getRight(), TransformerContext.RIGHT );
		Map<String, String>	values		= new HashMap<>();

		ExpressionStmt		javaExpr	= new ExpressionStmt( left );
		if ( left instanceof MethodCallExpr method ) {
			if ( "put".equalsIgnoreCase( method.getName().asString() ) ) {
				method.getArguments().add( right );
			}
		}

		if ( assigment.getOp() == BoxAssigmentOperator.PlusEqual ) {
			MethodCallExpr methodCall = ( MethodCallExpr ) left;
			values.put( "expr", methodCall.getScope().get().toString() );
			values.put( "key", methodCall.getArguments().get( 0 ).toString() );
			values.put( "right", right.toString() );
			String template = "Plus.invoke(${expr},${key},${right})";
			javaExpr = new ExpressionStmt( ( Expression ) parseExpression( template, values ) );
		}

		if ( assigment.getOp() == BoxAssigmentOperator.MinusEqual ) {
			MethodCallExpr methodCall = ( MethodCallExpr ) left;
			values.put( "expr", methodCall.getScope().get().toString() );
			values.put( "key", methodCall.getArguments().get( 0 ).toString() );
			values.put( "right", right.toString() );
			String template = "Minus.invoke(${expr},${key},${right})";
			javaExpr = new ExpressionStmt( ( Expression ) parseExpression( template, values ) );
		}

		if ( assigment.getOp() == BoxAssigmentOperator.StarEqual ) {
			MethodCallExpr methodCall = ( MethodCallExpr ) left;
			values.put( "expr", methodCall.getScope().get().toString() );
			values.put( "key", methodCall.getArguments().get( 0 ).toString() );
			values.put( "right", right.toString() );
			String template = "Multiply.invoke(${expr},${key},${right})";
			javaExpr = new ExpressionStmt( ( Expression ) parseExpression( template, values ) );
		}

		if ( assigment.getOp() == BoxAssigmentOperator.SlashEqual ) {
			MethodCallExpr methodCall = ( MethodCallExpr ) left;
			values.put( "expr", methodCall.getScope().get().toString() );
			values.put( "key", methodCall.getArguments().get( 0 ).toString() );
			values.put( "right", right.toString() );
			String template = "Divide.invoke(${expr},${key},${right})";
			javaExpr = new ExpressionStmt( ( Expression ) parseExpression( template, values ) );
		}

		if ( assigment.getOp() == BoxAssigmentOperator.ModEqual ) {
			MethodCallExpr methodCall = ( MethodCallExpr ) left;
			values.put( "expr", methodCall.getScope().get().toString() );
			values.put( "key", methodCall.getArguments().get( 0 ).toString() );
			values.put( "right", right.toString() );
			String template = "Module.invoke(${expr},${key},${right})";
			javaExpr = new ExpressionStmt( ( Expression ) parseExpression( template, values ) );
		}
		if ( assigment.getOp() == BoxAssigmentOperator.ConcatEqual ) {
			MethodCallExpr methodCall = ( MethodCallExpr ) left;
			values.put( "expr", methodCall.getScope().get().toString() );
			values.put( "key", methodCall.getArguments().get( 0 ).toString() );
			values.put( "right", right.toString() );
			String template = "Concat.invoke(${expr},${key},${right})";
			javaExpr = new ExpressionStmt( ( Expression ) parseExpression( template, values ) );
		}

		addIndex( javaExpr, node );
		return javaExpr;
	}

}
