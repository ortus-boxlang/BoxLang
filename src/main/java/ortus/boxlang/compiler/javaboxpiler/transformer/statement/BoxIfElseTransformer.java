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
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.Statement;

import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.statement.BoxIfElse;
import ortus.boxlang.compiler.javaboxpiler.JavaTranspiler;
import ortus.boxlang.compiler.javaboxpiler.transformer.AbstractTransformer;
import ortus.boxlang.compiler.javaboxpiler.transformer.TransformerContext;

public class BoxIfElseTransformer extends AbstractTransformer {

	public BoxIfElseTransformer( JavaTranspiler transpiler ) {
		super( transpiler );
	}

	@Override
	public Node transform( BoxNode node, TransformerContext context ) throws IllegalStateException {
		BoxIfElse	ifElse		= ( BoxIfElse ) node;
		Expression	condition	= ( Expression ) transpiler.transform( ifElse.getCondition(), TransformerContext.RIGHT );

		String		template	= "if(  ${condition}  ) {}";
		if ( requiresBooleanCaster( ifElse.getCondition() ) ) {
			template = "if( BooleanCaster.cast( ${condition} ) ) {}";
		}
		Map<String, String>	values		= new HashMap<>() {

											{
												put( "condition", condition.toString() );
												put( "contextName", transpiler.peekContextName() );
											}
										};

		IfStmt				javaIfStmt	= ( IfStmt ) parseStatement( template, values );

		// May be a single statement or a block statement
		javaIfStmt.setThenStmt( ensureBlockStatement( ( Statement ) transpiler.transform( ifElse.getThenBody() ) ) );

		if ( ifElse.getElseBody() != null ) {
			// May be a single statement or a block statement
			javaIfStmt.setElseStmt( ensureBlockStatement( ( Statement ) transpiler.transform( ifElse.getElseBody() ) ) );
		}

		addIndex( javaIfStmt, node );
		return javaIfStmt;

	}

	/**
	 * We need to wrap up single statements into a block statement to avoid ambiguity in the generated java
	 * 
	 * @param statement The statement to wrap
	 * 
	 * @return The block statement
	 */
	private BlockStmt ensureBlockStatement( Statement statement ) {
		if ( statement instanceof BlockStmt blk ) {
			return blk;
		}

		BlockStmt blockStmt = new BlockStmt();
		blockStmt.addStatement( statement );
		return blockStmt;
	}

}
