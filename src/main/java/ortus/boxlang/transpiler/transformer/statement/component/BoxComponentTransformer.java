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
package ortus.boxlang.transpiler.transformer.statement.component;

import java.util.ArrayList;
import java.util.List;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.UnknownType;

import ortus.boxlang.ast.BoxNode;
import ortus.boxlang.ast.expression.BoxFQN;
import ortus.boxlang.ast.expression.BoxStringLiteral;
import ortus.boxlang.ast.statement.BoxAnnotation;
import ortus.boxlang.ast.statement.component.BoxComponent;
import ortus.boxlang.transpiler.JavaTranspiler;
import ortus.boxlang.transpiler.transformer.AbstractTransformer;
import ortus.boxlang.transpiler.transformer.TransformerContext;

public class BoxComponentTransformer extends AbstractTransformer {

	public BoxComponentTransformer( JavaTranspiler transpiler ) {
		super( transpiler );
	}

	@Override
	public Node transform( BoxNode node, TransformerContext context ) throws IllegalStateException {
		BoxComponent		boxComponent	= ( BoxComponent ) node;
		Expression			jComponentBody;
		String				componentName	= boxComponent.getName();
		List<BoxAnnotation>	attributes		= new ArrayList<BoxAnnotation>();
		attributes.addAll( boxComponent.getAttributes() );

		// Check for custom tag shortcut like <cf_brad>
		if ( componentName.startsWith( "_" ) ) {
			attributes.add(
			    new BoxAnnotation(
			        new BoxFQN( "name", null, componentName ),
			        new BoxStringLiteral( componentName.substring( 1 ), null, componentName ),
			        null,
			        null )
			);
			componentName = "module";
		}

		if ( boxComponent.getBody() != null ) {

			BlockStmt	jBody				= new BlockStmt();
			String		lambdaContextName	= "lambdaContext" + transpiler.incrementAndGetLambdaContextCounter();
			transpiler.pushContextName( lambdaContextName );
			for ( BoxNode statement : boxComponent.getBody() ) {
				jBody.getStatements().add( ( Statement ) transpiler.transform( statement ) );
			}
			transpiler.popContextName();
			LambdaExpr lambda = new LambdaExpr();
			lambda.setParameters( new NodeList<>(
			    new Parameter( new UnknownType(), lambdaContextName ) ) );
			lambda.setBody( jBody );
			jComponentBody = lambda;
		} else {
			jComponentBody = new NullLiteralExpr();
		}

		Statement jStatement = new ExpressionStmt(
		    new MethodCallExpr(
		        new NameExpr( transpiler.peekContextName() ),
		        "invokeComponent",
		        new NodeList<Expression>(
		            createKey( componentName ),
		            transformAnnotations( attributes, true ),
		            jComponentBody )
		    )
		);
		logger.debug( node.getSourceText() + " -> " + jStatement );
		addIndex( jStatement, node );
		return jStatement;
	}
}
