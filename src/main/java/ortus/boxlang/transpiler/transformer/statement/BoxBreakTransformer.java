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

import com.github.javaparser.ast.Node;

import ortus.boxlang.ast.BoxNode;
import ortus.boxlang.ast.statement.BoxSwitchCase;
import ortus.boxlang.transpiler.JavaTranspiler;
import ortus.boxlang.transpiler.transformer.AbstractTransformer;
import ortus.boxlang.transpiler.transformer.TransformerContext;

public class BoxBreakTransformer extends AbstractTransformer {

	public BoxBreakTransformer( JavaTranspiler transpiler ) {
		super( transpiler );
	}

	@Override
	public Node transform( BoxNode node, TransformerContext context ) throws IllegalStateException {
		String	template;
		boolean	isInSwitch	= node.getParent() instanceof BoxSwitchCase;
		if ( transpiler.isInsideComponent() && !isInSwitch ) {
			template = "if(true) return Component.BodyResult.ofBreak();";
		} else {
			template = "if(true) break;";
		}
		Node javaStmt = parseStatement( template, new HashMap<>() );
		logger.debug( node.getSourceText() + " -> " + javaStmt );
		addIndex( javaStmt, node );
		return javaStmt;
	}
}
