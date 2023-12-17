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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.WhileStmt;

import ortus.boxlang.ast.BoxNode;
import ortus.boxlang.ast.expression.BoxAssignmentModifier;
import ortus.boxlang.ast.statement.BoxAssignmentOperator;
import ortus.boxlang.ast.statement.BoxForIn;
import ortus.boxlang.transpiler.JavaTranspiler;
import ortus.boxlang.transpiler.transformer.AbstractTransformer;
import ortus.boxlang.transpiler.transformer.TransformerContext;
import ortus.boxlang.transpiler.transformer.expression.BoxAssignmentTransformer;

/**
 * Transform a BoxForIn Node the equivalent Java Parser AST nodes
 */
public class BoxForInTransformer extends AbstractTransformer {

	Logger logger = LoggerFactory.getLogger( BoxForInTransformer.class );

	public BoxForInTransformer( JavaTranspiler transpiler ) {
		super( transpiler );
	}

	/**
	 * Transform a collection for statement
	 *
	 * @param node    a BoxForIn instance
	 * @param context transformation context
	 *
	 * @return a Java Parser Block statement with an iterator and a while loop
	 *
	 * @throws IllegalStateException
	 */
	@Override
	public Node transform( BoxNode node, TransformerContext context ) throws IllegalStateException {
		BoxForIn					boxFor		= ( BoxForIn ) node;
		Node						collection	= transpiler.transform( boxFor.getExpression() );
		int							forInCount	= transpiler.incrementAndGetForInCounter();
		String						jVarName	= "forInIterator" + forInCount;

		BlockStmt					stmt		= new BlockStmt();

		Map<String, String>			values		= new HashMap<>() {

													{
														put( "variable", jVarName );
														put( "collection", collection.toString() );
														put( "contextName", transpiler.peekContextName() );
													}
												};

		List<BoxAssignmentModifier>	modifiers	= new ArrayList<BoxAssignmentModifier>();
		if ( boxFor.getHasVar() ) {
			modifiers.add( BoxAssignmentModifier.VAR );
		}
		Node loopAssignment = new BoxAssignmentTransformer( ( JavaTranspiler ) transpiler ).transformEquals(
		    boxFor.getVariable(),
		    ( Expression ) parseExpression( jVarName + ".next()", values ),
		    BoxAssignmentOperator.Equal,
		    modifiers,
		    ( boxFor.getHasVar() ? "var " : "" ) + boxFor.getVariable().getSourceText(),
		    context );

		values.put( "loopAssignment", loopAssignment.toString() );

		String		template1	= """
		                          	Iterator ${variable} = CollectionCaster.cast( ${collection} ).iterator();
		                          """;
		String		template2	= """
		                                                  	while( ${variable}.hasNext() ) {
		                          ${loopAssignment};
		                                                  	}
		                                                  """;
		WhileStmt	whileStmt	= ( WhileStmt ) parseStatement( template2, values );
		stmt.addStatement( ( Statement ) parseStatement( template1, values ) );
		boxFor.getBody().forEach( it -> {
			whileStmt.getBody().asBlockStmt().addStatement( ( Statement ) transpiler.transform( it ) );
		} );
		stmt.addStatement( whileStmt );
		logger.info( node.getSourceText() + " -> " + stmt );
		addIndex( stmt, node );
		return stmt;
	}
}
