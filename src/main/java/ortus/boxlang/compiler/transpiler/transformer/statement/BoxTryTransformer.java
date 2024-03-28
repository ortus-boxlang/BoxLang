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

package ortus.boxlang.compiler.transpiler.transformer.statement;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.BinaryExpr.Operator;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.TryStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

import ortus.boxlang.compiler.ast.BoxExpr;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.expression.BoxFQN;
import ortus.boxlang.compiler.ast.expression.BoxStringLiteral;
import ortus.boxlang.compiler.ast.statement.BoxTry;
import ortus.boxlang.compiler.ast.statement.BoxTryCatch;
import ortus.boxlang.compiler.transpiler.JavaTranspiler;
import ortus.boxlang.compiler.transpiler.transformer.AbstractTransformer;
import ortus.boxlang.compiler.transpiler.transformer.TransformerContext;

public class BoxTryTransformer extends AbstractTransformer {

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
			BlockStmt	abortBody		= new BlockStmt();
			abortBody.addStatement( parseStatement(
			    "throw e;",
			    Map.of()
			) );
			// This top-level if statment is used to implement the runtime checks for multiple catch clauses
			IfStmt	javaIfStmt	= new IfStmt();
			// This is used to keep track of the last if statement so we can add the else statement
			IfStmt	javaLastIf	= javaIfStmt;
			// Add our empty top-level if statement to the catch body, we'll flesh out the conditions and then/else blocks as we go
			catchBody.addStatement( javaIfStmt );
			String	catchContextName;
			String	catchName;
			int		typeCounter	= 0;
			for ( BoxTryCatch clause : boxTry.getCatches() ) {
				catchContextName	= "catchContext" + catchCounter + ++typeCounter;
				catchName			= clause.getException().getName();
				Map<String, String> values = new HashMap<>();
				values.put( "catchNameKey", createKey( catchName ).toString() );
				values.put( "catchContextName", catchContextName );
				values.put( "contextName", transpiler.peekContextName() );
				values.put( "throwableName", throwableName );

				if ( typeCounter > 1 ) {
					IfStmt tmp = new IfStmt();
					javaLastIf.setElseStmt( tmp );
					javaLastIf = tmp;
				}

				javaLastIf.setCondition( getIfCondition( clause.getCatchTypes(), context, values.get( "contextName" ), values.get( "throwableName" ) ) );

				BlockStmt	ifhBody	= new BlockStmt();
				Statement	handler	= ( Statement ) parseStatement(
				    "CatchBoxContext ${catchContextName} = new CatchBoxContext( ${contextName}, ${catchNameKey}, ${throwableName} );",
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

			final String		catchContextName2	= "catchContext" + catchCounter + ++typeCounter;
			Map<String, String>	values				= new HashMap<>() {

														{
															put( "catchNameKey", createKey( "e" ).toString() );
															put( "catchContextName", catchContextName2 );
															put( "contextName", transpiler.peekContextName() );
															put( "throwableName", throwableName );
														}
													};
			javaLastIf.setElseStmt( new BlockStmt( new NodeList<Statement>(
			    ( Statement ) parseStatement(
			        "CatchBoxContext ${catchContextName} = new CatchBoxContext( ${contextName}, ${catchNameKey}, ${throwableName} );",
			        values
			    ),
			    ( Statement ) parseStatement( "${catchContextName}.rethrow();", values ) ) )
			);

			NodeList<CatchClause> catchClauses = new NodeList<>();
			catchClauses
			    .add( new CatchClause( new Parameter( new ClassOrInterfaceType( "ortus.boxlang.runtime.types.exceptions.AbortException" ), "e" ), abortBody ) );
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

	private static boolean isAny( BoxExpr expr ) {
		if ( expr instanceof BoxStringLiteral str ) {
			return str.getValue().compareToIgnoreCase( "any" ) == 0;
		} else if ( expr instanceof BoxFQN fqn ) {
			return fqn.getValue().compareToIgnoreCase( "any" ) == 0;
		}

		return false;
	}

	private Expression getIfCondition( List<BoxExpr> boxCatchTypes, TransformerContext context, String contextName, String throwableName ) {

		if ( boxCatchTypes.size() == 0 || boxCatchTypes.stream().anyMatch( BoxTryTransformer::isAny ) ) {
			return new BooleanLiteralExpr( true );
		} else if ( boxCatchTypes.size() == 1 ) {
			return transformCatchType( boxCatchTypes.get( 0 ), context, contextName, throwableName );
		}

		int size = boxCatchTypes.size();
		return IntStream.range( 0, size )
		    .map( i -> size - i + 0 - 1 )
		    .mapToObj( i -> transformCatchType( boxCatchTypes.get( i ), context, contextName, throwableName ) )
		    .reduce( null, ( acc, ex ) -> {
			    BinaryExpr expr = new BinaryExpr();

			    expr.setOperator( Operator.OR );
			    expr.setLeft( ex );

			    if ( acc != null ) {
				    expr.setRight( acc );
			    }

			    return expr;
		    } );
	}

	private Expression transformCatchType( BoxExpr catchType, TransformerContext context, String contextName, String throwableName ) {
		NameExpr		nameExpr		= new NameExpr( "ExceptionUtil" );
		MethodCallExpr	methodCallExpr	= new MethodCallExpr( nameExpr, "exceptionIsOfType" );
		methodCallExpr.addArgument( contextName );
		methodCallExpr.addArgument( throwableName );

		if ( catchType instanceof BoxFQN fqn ) {
			methodCallExpr.addArgument( "\"" + fqn.getValue() + "\"" );
		} else {
			methodCallExpr.addArgument( ( Expression ) transpiler.transform( catchType, context ) );
		}

		return methodCallExpr;
	}

}
