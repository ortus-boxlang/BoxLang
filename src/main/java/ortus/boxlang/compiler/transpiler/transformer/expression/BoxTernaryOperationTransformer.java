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
package ortus.boxlang.compiler.transpiler.transformer.expression;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.type.UnknownType;

import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.expression.BoxTernaryOperation;
import ortus.boxlang.compiler.transpiler.JavaTranspiler;
import ortus.boxlang.compiler.transpiler.transformer.AbstractTransformer;
import ortus.boxlang.compiler.transpiler.transformer.TransformerContext;

public class BoxTernaryOperationTransformer extends AbstractTransformer {

	public BoxTernaryOperationTransformer( JavaTranspiler transpiler ) {
		super( transpiler );
	}

	@Override
	public Node transform( BoxNode node, TransformerContext context ) throws IllegalStateException {
		BoxTernaryOperation	operation	= ( BoxTernaryOperation ) node;
		Expression			condition	= ( Expression ) transpiler.transform( operation.getCondition() );
		Expression			whenTrue	= wrapInLambda( ( Expression ) transpiler.transform( operation.getWhenTrue() ) );
		Expression			whenFalse	= wrapInLambda( ( Expression ) transpiler.transform( operation.getWhenFalse() ) );

		return new MethodCallExpr(
		    new NameExpr( "Ternary" ),
		    "invoke",
		    new NodeList<Expression>( new NameExpr( transpiler.peekContextName() ), condition, whenTrue, whenFalse )
		);
	}

	public Expression wrapInLambda( Expression body ) {
		String lambdaContextName = "lambdaContext" + transpiler.incrementAndGetLambdaContextCounter();

		transpiler.pushContextName( lambdaContextName );

		LambdaExpr lambda = new LambdaExpr();
		lambda.setParameters( new NodeList<>( new Parameter( new UnknownType(), lambdaContextName ) ) );
		lambda.setBody( new BlockStmt().addStatement( new ReturnStmt( body ) ) );

		transpiler.popContextName();

		return lambda;
	}
}
