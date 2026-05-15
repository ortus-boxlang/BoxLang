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
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.Statement;

import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.statement.BoxRefutableDestructuringDeclaration;
import ortus.boxlang.compiler.javaboxpiler.JavaTranspiler;
import ortus.boxlang.compiler.javaboxpiler.transformer.TransformerContext;
import ortus.boxlang.compiler.javaboxpiler.transformer.expression.BoxMatchExpressionTransformer;

public class BoxRefutableDestructuringDeclarationTransformer extends BoxMatchExpressionTransformer {

	public BoxRefutableDestructuringDeclarationTransformer( JavaTranspiler transpiler ) {
		super( transpiler );
	}

	@Override
	public Node transform( BoxNode node, TransformerContext context ) throws IllegalStateException {
		BoxRefutableDestructuringDeclaration	declaration	= ( BoxRefutableDestructuringDeclaration ) node;
		Map<String, String>						values		= new HashMap<>();

		declaration.validatePatternHasBindingTarget();

		values.put( "contextName", transpiler.peekContextName() );
		values.put( "subject", transpiler.transform( declaration.getSubject(), TransformerContext.RIGHT ).toString() );
		values.put( "pattern", buildPatternExpression( declaration.getPattern(), true ) );
		values.put( "isLocal", declaration.isLocalDeclaration() ? "true" : "false" );
		values.put( "isFinal", declaration.isFinalDeclaration() ? "true" : "false" );

		IfStmt javaIfStmt = ( IfStmt ) parseStatement(
		    "if( ortus.boxlang.runtime.dynamic.MatchExpression.declare(${contextName}, ${subject}, ${pattern}, ${isLocal}, ${isFinal}) ) {}",
		    values
		);

		javaIfStmt.setThenStmt( new BlockStmt() );
		javaIfStmt.setElseStmt( ensureBlockStatement( ( Statement ) transpiler.transform( declaration.getElseBody() ) ) );

		addIndex( javaIfStmt, node );
		return javaIfStmt;
	}

	private BlockStmt ensureBlockStatement( Statement statement ) {
		if ( statement instanceof BlockStmt blk ) {
			return blk;
		}

		BlockStmt blockStmt = new BlockStmt();
		blockStmt.addStatement( statement );
		return blockStmt;
	}
}