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
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.TryStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;

import ortus.boxlang.ast.BoxNode;
import ortus.boxlang.ast.statement.BoxTry;
import ortus.boxlang.ast.statement.BoxTryCatch;
import ortus.boxlang.ast.statement.BoxTryCatchType;
import ortus.boxlang.transpiler.JavaTranspiler;
import ortus.boxlang.transpiler.transformer.AbstractTransformer;
import ortus.boxlang.transpiler.transformer.TransformerContext;
import ortus.boxlang.transpiler.transformer.expression.BoxParenthesisTransformer;

/***
 * catchContext = new CatchBoxContext(context, Key.of("e"), e1); //
 * {
 * context.scopeFindNearby(Key.of("one"), context.getDefaultAssignmentScope()).scope().assign(Key.of("one"),
 * Referencer.getAndInvoke(context, e /*** Do not use , Key.of("getMessage"), new Object[] {}, false));
 * }
 */
public class BoxTryTransformer extends AbstractTransformer {

	Logger logger = LoggerFactory.getLogger( BoxParenthesisTransformer.class );

	public BoxTryTransformer( JavaTranspiler transpiler ) {
		super( transpiler );
	}

	@Override
	public Node transform( BoxNode node, TransformerContext context ) throws IllegalStateException {
		BoxTry		boxTry	= ( BoxTry ) node;
		TryStmt		javaTry	= new TryStmt();

		BlockStmt	tryBody	= new BlockStmt();
		boxTry.getTryBody().forEach( stmt -> tryBody.getStatements().add(
		    ( Statement ) transpiler.transform( stmt )
		) );

		// Skip all catch logic if there are no catch clauses
		if ( boxTry.getCatches().size() > 0 ) {
			int			catchCounter	= transpiler.incrementAndGetTryCatchCounter();
			String		throwableName	= "e" + catchCounter;
			BlockStmt	catchBody		= new BlockStmt();
			// This top-level if statment is used to implement the runtime checks for multiple catch clauses
			IfStmt		javaIfStmt		= new IfStmt();
			// This is used to keep track of the last if statement so we can add the else statement
			IfStmt		javaLastIf		= javaIfStmt;
			// Add our empty top-level if statement to the catch body, we'll flesh out the conditions and then/else blocks as we go
			catchBody.addStatement( javaIfStmt );
			int typeCounter = 0;
			for ( BoxTryCatch clause : boxTry.getCatches() ) {
				String				catchContextName	= "catchContext" + catchCounter + ++typeCounter;
				String				catchName			= clause.getException().getName();
				Map<String, String>	values				= new HashMap<>() {

															{
																put( "catchName", catchName );
																put( "catchContextName", catchContextName );
																put( "contextName", transpiler.peekContextName() );
																put( "throwableName", throwableName );
																put( "type", clause.getName() );
															}
														};
				if ( typeCounter > 1 ) {
					IfStmt tmp = new IfStmt();
					javaLastIf.setElseStmt( tmp );
					javaLastIf = tmp;
				}
				if ( clause.getType() == BoxTryCatchType.Any ) {
					javaLastIf.setCondition( new BooleanLiteralExpr( true ) );
				} else {
					javaLastIf.setCondition( ( Expression ) parseExpression(
					    "ExceptionUtil.exceptionIsOfType( ${contextName}, ${throwableName}, ${type} )",
					    values
					) );
				}

				BlockStmt	ifhBody	= new BlockStmt();
				Statement	handler	= ( Statement ) parseStatement(
				    "CatchBoxContext ${catchContextName} = new CatchBoxContext( ${contextName}, Key.of( \"${catchName}\" ), ${throwableName} );",
				    values
				);

				ifhBody.addStatement( handler );

				transpiler.pushContextName( catchContextName );
				clause.getCatchBody().forEach( stmt -> ifhBody.getStatements().add(
				    ( Statement ) transpiler.transform( stmt )
				) );
				transpiler.popContextName();
				javaLastIf.setThenStmt( ifhBody );
			} // end for loop

			Map<String, String> values = new HashMap<>() {

				{
					put( "contextName", transpiler.peekContextName() );
				}
			};
			javaLastIf.setElseStmt( new BlockStmt( new NodeList<Statement>( ( Statement ) parseStatement( "${contextName}.rethrow();", values ) ) ) );

			NodeList<CatchClause> catchClauses = new NodeList<>();
			catchClauses.add( new CatchClause( new Parameter( new ClassOrInterfaceType( "Throwable" ), throwableName ), catchBody ) );
			javaTry.setCatchClauses( catchClauses );
		}

		BlockStmt finallyBody = new BlockStmt();
		boxTry.getFinallyBody().forEach( stmt -> finallyBody.getStatements().add(
		    ( Statement ) transpiler.transform( stmt )
		) );

		javaTry.setTryBlock( tryBody );
		javaTry.setFinallyBlock( finallyBody );
		return javaTry;
	}

}
