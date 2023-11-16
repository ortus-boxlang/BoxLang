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
import ortus.boxlang.ast.statement.BoxAssigmentOperator;
import ortus.boxlang.ast.statement.BoxAssignment;
import ortus.boxlang.transpiler.JavaTranspiler;
import ortus.boxlang.transpiler.transformer.AbstractTransformer;
import ortus.boxlang.transpiler.transformer.TransformerContext;

public class BoxAssignmentTransformer extends AbstractTransformer {

	Logger logger = LoggerFactory.getLogger( BoxAssignmentTransformer.class );

	public BoxAssignmentTransformer( JavaTranspiler transpiler ) {
		super( transpiler );
	}

	@Override
	public Node transform( BoxNode node, TransformerContext context ) throws IllegalStateException {
		logger.info( node.getSourceText() );
		BoxAssignment	assigment	= ( BoxAssignment ) node;
		Expression		right		= ( Expression ) transpiler.transform( assigment.getRight(), TransformerContext.RIGHT );
		BlockStmt		blockStmt	= new BlockStmt();
		for ( BoxExpr expr : assigment.getLeft() ) {

			Expression			left		= ( Expression ) transpiler.transform( expr, TransformerContext.LEFT );
			Map<String, String>	values		= new HashMap<>();
			ExpressionStmt		javaExpr	= new ExpressionStmt( left );
			if ( left instanceof MethodCallExpr method ) {
				if ( "assign".equalsIgnoreCase( method.getName().asString() ) ) {
					method.getArguments().add( right );
				}
				if ( "setDeep".equalsIgnoreCase( method.getName().asString() ) ) {
					method.getArguments().add( 1, right );
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
					String template = "Modulus.invoke(${expr},${key},${right})";
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

			} else if ( left instanceof NameExpr name ) {
				values.put( "left", left.toString() );
				values.put( "right", right.toString() );
				if ( right instanceof NameExpr rname ) {
					String tmp = "context.scopeFindNearby( Key.of( \"" + rname + "\" ), null).value()";
					values.put( "right", tmp );
				}

				String template = """
				                  context.scopeFindNearby(Key.of( "${left}" ),context.getDefaultAssignmentScope()).scope().assign( Key.of( "${left}" ), ${right} )
				                  """;

				javaExpr = new ExpressionStmt( ( Expression ) parseExpression( template, values ) );

				if ( assigment.getOp() == BoxAssigmentOperator.PlusEqual ) {
					values.put( "left", left.toString() );
					values.put( "right", right.toString() );
					template	= "Plus.invoke(context.scopeFindNearby(Key.of( \"${left}\" ),null).scope(),Key.of( \"${left}\"),${right})";
					javaExpr	= new ExpressionStmt( ( Expression ) parseExpression( template, values ) );
				}
				if ( assigment.getOp() == BoxAssigmentOperator.MinusEqual ) {
					values.put( "left", left.toString() );
					values.put( "right", right.toString() );
					template	= "Minus.invoke(context.scopeFindNearby(Key.of( \"${left}\" ),null).scope(),Key.of( \"${left}\"),${right})";
					javaExpr	= new ExpressionStmt( ( Expression ) parseExpression( template, values ) );
				}
				if ( assigment.getOp() == BoxAssigmentOperator.StarEqual ) {
					values.put( "left", left.toString() );
					values.put( "right", right.toString() );
					template	= "Multiply.invoke(context.scopeFindNearby(Key.of( \"${left}\" ),null).scope(),Key.of( \"${left}\"),${right})";
					javaExpr	= new ExpressionStmt( ( Expression ) parseExpression( template, values ) );
				}
				if ( assigment.getOp() == BoxAssigmentOperator.SlashEqual ) {
					values.put( "left", left.toString() );
					values.put( "right", right.toString() );
					template	= "Divide.invoke(context.scopeFindNearby(Key.of( \"${left}\" ),null).scope(),Key.of( \"${left}\"),${right})";
					javaExpr	= new ExpressionStmt( ( Expression ) parseExpression( template, values ) );
				}
				if ( assigment.getOp() == BoxAssigmentOperator.ConcatEqual ) {
					values.put( "left", left.toString() );
					values.put( "right", right.toString() );
					template	= "Concat.invoke(context.scopeFindNearby(Key.of( \"${left}\" ),null).scope(),Key.of( \"${left}\"),${right})";
					javaExpr	= new ExpressionStmt( ( Expression ) parseExpression( template, values ) );
				}
			}
			blockStmt.addStatement( javaExpr );
			addIndex( javaExpr, node );
		}

		return blockStmt;
	}

}
