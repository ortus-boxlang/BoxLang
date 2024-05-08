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

import java.util.HashMap;
import java.util.Map;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.LabeledStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.TryStmt;
import com.github.javaparser.ast.stmt.WhileStmt;

import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.statement.BoxForIndex;
import ortus.boxlang.compiler.javaboxpiler.JavaTranspiler;
import ortus.boxlang.compiler.javaboxpiler.transformer.AbstractTransformer;
import ortus.boxlang.compiler.javaboxpiler.transformer.TransformerContext;

/**
 * Transform a BoxForIndex Node the equivalent Java Parser AST nodes
 */
public class BoxForIndexTransformer extends AbstractTransformer {

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
		BoxForIndex	boxFor		= ( BoxForIndex ) node;

		Expression	initializer	= null;
		Expression	condition	= null;
		Expression	step		= null;

		if ( boxFor.getInitializer() != null ) {
			initializer = ( Expression ) transpiler.transform( boxFor.getInitializer(), TransformerContext.LEFT );
		}
		if ( boxFor.getCondition() != null ) {
			condition = ( Expression ) transpiler.transform( boxFor.getCondition(), TransformerContext.RIGHT );
		} else {
			condition = new BooleanLiteralExpr( true );
		}
		if ( boxFor.getStep() != null ) {
			step = ( Expression ) transpiler.transform( boxFor.getStep(), TransformerContext.RIGHT );
		}
		Map<String, String> values = new HashMap<>();
		values.put( "condition", condition.toString() );
		values.put( "contextName", transpiler.peekContextName() );

		String template2 = "while( ${condition} ) {}";
		if ( requiresBooleanCaster( boxFor.getCondition() ) ) {
			template2 = "while( BooleanCaster.cast( ${condition} ) ) {}";
		}
		BlockStmt stmt = new BlockStmt();
		if ( initializer != null ) {
			ExpressionStmt init = new ExpressionStmt( initializer );
			stmt.addStatement( init );
		}
		WhileStmt	whileStmt	= ( WhileStmt ) parseStatement( template2, values );
		BlockStmt	body		= new BlockStmt();

		// May be a single statement or a block statement, which is still a single statement :)
		body.asBlockStmt().addStatement( ( Statement ) transpiler.transform( boxFor.getBody() ) );
		// for body is in the try body
		TryStmt tryStmt = new TryStmt();
		tryStmt.setTryBlock( body );
		if ( step != null ) {
			// And we run the step in the finally block
			ExpressionStmt stepStmt = new ExpressionStmt( step );
			tryStmt.setFinallyBlock( new BlockStmt( new NodeList<Statement>( stepStmt ) ) );
		} else {
			// We need to trick the Java compiler which requires a try resource, catch block, or finally block
			tryStmt.setFinallyBlock( new BlockStmt() );
		}

		whileStmt.setBody( tryStmt );
		if ( boxFor.getLabel() != null ) {
			LabeledStmt labeledWhile = new LabeledStmt( boxFor.getLabel().toLowerCase(), whileStmt );
			stmt.addStatement( labeledWhile );
		} else {
			stmt.addStatement( whileStmt );
		}

		// logger.trace( node.getSourceText() + " -> " + stmt );
		addIndex( stmt, node );
		return stmt;
	}
}
