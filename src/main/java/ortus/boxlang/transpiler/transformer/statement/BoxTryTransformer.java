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

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.TryStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ortus.boxlang.ast.BoxNode;
import ortus.boxlang.ast.expression.BoxIdentifier;
import ortus.boxlang.ast.expression.BoxStringLiteral;
import ortus.boxlang.ast.statement.BoxTry;
import ortus.boxlang.ast.statement.BoxTryCatch;
import ortus.boxlang.transpiler.JavaTranspiler;
import ortus.boxlang.transpiler.transformer.AbstractTransformer;
import ortus.boxlang.transpiler.transformer.TransformerContext;
import ortus.boxlang.transpiler.transformer.expression.BoxParenthesisTransformer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

		NodeList<CatchClause> catchClauses = new NodeList<>();
		boxTry.getCatches().forEach( clause -> {
			BlockStmt			catchBody	= new BlockStmt();
			String				name		= computeName( clause );
			Map<String, String>	values		= new HashMap<>() {

												{
													put( "expr", name );
												}
											};

			Statement			handler		= ( Statement ) parseStatement( "catchContext = new CatchBoxContext( context, Key.of( \"${expr}\" ), ${expr} );",
			    values );

			catchBody.addStatement( handler );
			clause.getCatchBody().forEach( stmt -> catchBody.getStatements().add(
			    ( Statement ) transpiler.transform( stmt )
			) );

			catchClauses.add( new CatchClause( new Parameter( new ClassOrInterfaceType( "Throwable" ), name ), catchBody ) );
		}
		);

		BlockStmt finallyBody = new BlockStmt();
		boxTry.getFinallyBody().forEach( stmt -> finallyBody.getStatements().add(
		    ( Statement ) transpiler.transform( stmt )
		) );

		javaTry.setTryBlock( tryBody );
		javaTry.setCatchClauses( catchClauses );
		javaTry.setFinallyBlock( finallyBody );
		return javaTry;
	}

	private String computeName( BoxTryCatch clause ) throws IllegalStateException {
		List<ortus.boxlang.ast.Node> ancestors = clause.walkAncestors().stream().filter( it -> it instanceof BoxTry ).toList();

		if ( clause.getException() instanceof BoxStringLiteral literal ) {
			return literal.getValue();
		}
		if ( clause.getException() instanceof BoxIdentifier identifier ) {
			return identifier.getName(); // + ancestors.size();
		}
		throw new IllegalStateException( "invalid name" );
	}
}
