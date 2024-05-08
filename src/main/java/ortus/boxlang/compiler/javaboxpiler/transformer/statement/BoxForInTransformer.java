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
package ortus.boxlang.compiler.javaboxpiler.transformer.statement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.LabeledStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.WhileStmt;

import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.expression.BoxAssignmentModifier;
import ortus.boxlang.compiler.ast.expression.BoxAssignmentOperator;
import ortus.boxlang.compiler.ast.statement.BoxForIn;
import ortus.boxlang.compiler.javaboxpiler.JavaTranspiler;
import ortus.boxlang.compiler.javaboxpiler.transformer.AbstractTransformer;
import ortus.boxlang.compiler.javaboxpiler.transformer.TransformerContext;
import ortus.boxlang.compiler.javaboxpiler.transformer.expression.BoxAssignmentTransformer;

/**
 * Transform a BoxForIn Node the equivalent Java Parser AST nodes
 */
public class BoxForInTransformer extends AbstractTransformer {

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
		BoxForIn					boxFor			= ( BoxForIn ) node;
		Expression					collection		= ( Expression ) transpiler.transform( boxFor.getExpression() );
		int							forInCount		= transpiler.incrementAndGetForInCounter();
		String						jVarName		= "forInIterator" + forInCount;
		String						jisQueryName	= "isQuery" + forInCount;
		String						jisStructName	= "isStruct" + forInCount;
		String						jCollectionName	= "collection" + forInCount;

		BlockStmt					stmt			= new BlockStmt();

		Map<String, String>			values			= new HashMap<>() {

														{
															put( "variable", jVarName );
															put( "isQueryName", jisQueryName );
															put( "isStructName", jisStructName );
															put( "jVarName", jVarName );
															put( "collectionName", jCollectionName );
															// put( "collection", collection.toString() );
															put( "contextName", transpiler.peekContextName() );
														}
													};

		List<BoxAssignmentModifier>	modifiers		= new ArrayList<BoxAssignmentModifier>();
		if ( boxFor.getHasVar() ) {
			modifiers.add( BoxAssignmentModifier.VAR );
		}
		Node loopAssignment = new BoxAssignmentTransformer( ( JavaTranspiler ) transpiler ).transformEquals(
		    boxFor.getVariable(),
		    ( Expression ) parseExpression( "${isStructName} ? ${jVarName}.next() : ${jVarName}.next()", values ),
		    BoxAssignmentOperator.Equal,
		    modifiers,
		    ( boxFor.getHasVar() ? "var " : "" ) + boxFor.getVariable().getSourceText(),
		    context );

		values.put( "loopAssignment", loopAssignment.toString() );

		String		template1			= """
		                                  Object ${collectionName} = DynamicObject.unWrap(  );
		                                                     """;
		String		template1a			= """
		                                  Boolean ${isQueryName} = ${collectionName} instanceof Query;
		                                                     """;

		String		template1b			= """
		                                  Boolean ${isStructName} = ${collectionName} instanceof Struct;
		                                                     """;

		String		template1c			= """
		                                  if( ${isQueryName} ) {
		                                  	${contextName}.registerQueryLoop( (Query) ${collectionName}, 0 );
		                                  }
		                                                     """;
		String		template1d			= """
		                                  	Iterator ${variable} = CollectionCaster.cast( ${collectionName} ).iterator();
		                                  """;
		String		template2a			= """
		                                                                           	while( ${variable}.hasNext() ) {
		                                  ${loopAssignment};

		                                                                           	}

		                                                                           """;
		String		template2b			= """
		                                  if( ${isQueryName} ) {
		                                                    				${contextName}.incrementQueryLoop( (Query) ${collectionName} );
		                                                    			}
		                                  """;
		String		template3			= """
		                                  if( ${isQueryName} ) {
		                                  	${contextName}.unregisterQueryLoop( (Query) ${collectionName} );
		                                  }
		                                                                             """;
		WhileStmt	whileStmt			= ( WhileStmt ) parseStatement( template2a, values );
		IfStmt		incrementQueryStmt	= ( IfStmt ) parseStatement( template2b, values );
		Statement	tempStmt			= ( Statement ) parseStatement( template1, values );
		( ( MethodCallExpr ) ( ( VariableDeclarationExpr ) ( ( ExpressionStmt ) tempStmt ).getExpression() ).getVariable( 0 ).getInitializer().get() )
		    .addArgument( collection );
		stmt.addStatement( tempStmt );
		stmt.addStatement( ( Statement ) parseStatement( template1a, values ) );
		stmt.addStatement( ( Statement ) parseStatement( template1b, values ) );
		stmt.addStatement( ( Statement ) parseStatement( template1c, values ) );
		stmt.addStatement( ( Statement ) parseStatement( template1d, values ) );

		// May be a single statement or a block statement, which is still a single statement :)
		whileStmt.getBody().asBlockStmt().addStatement( ( Statement ) transpiler.transform( boxFor.getBody() ) );
		whileStmt.getBody().asBlockStmt().addStatement( incrementQueryStmt );

		if ( boxFor.getLabel() != null ) {
			LabeledStmt labeledWhile = new LabeledStmt( boxFor.getLabel().toLowerCase(), whileStmt );
			stmt.addStatement( labeledWhile );
		} else {
			stmt.addStatement( whileStmt );
		}
		stmt.addStatement( ( Statement ) parseStatement( template3, values ) );
		// logger.trace( node.getSourceText() + " -> " + stmt );
		addIndex( stmt, node );
		return stmt;
	}
}
