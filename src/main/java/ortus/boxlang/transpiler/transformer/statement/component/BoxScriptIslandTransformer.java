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
package ortus.boxlang.transpiler.transformer.statement.component;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.Statement;

import ortus.boxlang.ast.BoxNode;
import ortus.boxlang.ast.BoxScriptIsland;
import ortus.boxlang.ast.BoxStatement;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.transpiler.JavaTranspiler;
import ortus.boxlang.transpiler.transformer.AbstractTransformer;
import ortus.boxlang.transpiler.transformer.TransformerContext;

public class BoxScriptIslandTransformer extends AbstractTransformer {

	public BoxScriptIslandTransformer( JavaTranspiler transpiler ) {
		super( transpiler );
	}

	@Override
	public Node transform( BoxNode node, TransformerContext context ) throws IllegalStateException {
		BoxScriptIsland	scriptIsland	= ( BoxScriptIsland ) node;

		BlockStmt		body			= new BlockStmt();
		for ( BoxStatement statement : scriptIsland.getStatements() ) {
			var javaNode = transpiler.transform( statement );
			if ( javaNode instanceof Statement stmt ) {
				body.getStatements().add( stmt );
			} else {
				throw new BoxRuntimeException(
				    "Unexpected node type: " + javaNode.getClass().getSimpleName() + " for BL Node " + statement.getClass().getSimpleName() );
			}
		}
		return body;
	}
}
