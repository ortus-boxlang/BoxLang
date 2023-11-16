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
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.WhileStmt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ortus.boxlang.ast.BoxNode;
import ortus.boxlang.ast.statement.BoxForIndex;
import ortus.boxlang.transpiler.JavaTranspiler;
import ortus.boxlang.transpiler.transformer.AbstractTransformer;
import ortus.boxlang.transpiler.transformer.TransformerContext;

import java.util.HashMap;
import java.util.Map;

/**
 * Transform a BoxForIndex Node the equivalent Java Parser AST nodes
 */
public class BoxForIndexTransformer extends AbstractTransformer {

	Logger logger = LoggerFactory.getLogger( BoxForIndexTransformer.class );

	public BoxForIndexTransformer( JavaTranspiler transpiler ) {
		super( transpiler );
	}

	/**
	 * Transform an BoxForIndex for statement
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
		BoxForIndex			boxFor		= ( BoxForIndex ) node;
		Expression			variable	= ( Expression ) resolveScope( transpiler.transform( boxFor.getVariable(), TransformerContext.LEFT ),
		    TransformerContext.INIT );
		Expression			initial		= ( Expression ) transpiler.transform( boxFor.getInitial(), TransformerContext.RIGHT );
		Expression			condition	= ( Expression ) transpiler.transform( boxFor.getCondition() );
		Expression			step		= ( Expression ) transpiler.transform( boxFor.getStep() );
		Map<String, String>	values		= new HashMap<>() {

											{
												put( "condition", condition.toString() );
											}
										};

		if ( variable instanceof MethodCallExpr method ) {
			if ( "assign".equalsIgnoreCase( method.getName().asString() ) ) {
				method.getArguments().add( initial );
			}
		}

		String template2 = "while( ${condition} ) {}";
		if ( requiresBooleanCaster( boxFor.getCondition() ) ) {
			template2 = "while( BooleanCaster.cast( ${condition} ) ) {}";
		}
		BlockStmt		stmt	= new BlockStmt();
		ExpressionStmt	init	= new ExpressionStmt( variable );
		stmt.addStatement( init );
		WhileStmt whileStmt = ( WhileStmt ) parseStatement( template2, values );
		boxFor.getBody().forEach( it -> {
			whileStmt.getBody().asBlockStmt().addStatement( ( Statement ) transpiler.transform( it ) );
		} );
		ExpressionStmt stepStmt = new ExpressionStmt( step );
		whileStmt.getBody().asBlockStmt().addStatement( stepStmt );
		stmt.addStatement( whileStmt );
		logger.info( node.getSourceText() + " -> " + stmt );
		addIndex( stmt, node );
		return stmt;
	}
}
