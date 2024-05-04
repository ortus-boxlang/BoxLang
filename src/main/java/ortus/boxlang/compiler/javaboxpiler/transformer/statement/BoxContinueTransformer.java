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

import com.github.javaparser.ast.Node;

import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.statement.BoxContinue;
import ortus.boxlang.compiler.javaboxpiler.JavaTranspiler;
import ortus.boxlang.compiler.javaboxpiler.transformer.AbstractTransformer;
import ortus.boxlang.compiler.javaboxpiler.transformer.TransformerContext;

public class BoxContinueTransformer extends AbstractTransformer {

	public BoxContinueTransformer( JavaTranspiler transpiler ) {
		super( transpiler );
	}

	@Override
	public Node transform( BoxNode node, TransformerContext context ) throws IllegalStateException {
		BoxContinue		continueNode	= ( BoxContinue ) node;
		String			template;
		ExitsAllowed	exitsAllowed	= getExitsAllowed( node );
		String			componentLabel	= "null";
		String			continueLabel	= "";
		if ( continueNode.getLabel() != null ) {
			continueLabel	= continueNode.getLabel().toLowerCase();
			componentLabel	= "\"" + continueNode.getLabel().toLowerCase() + "\"";
		}

		if ( exitsAllowed.equals( ExitsAllowed.COMPONENT ) ) {
			template = "if(true) return Component.BodyResult.ofContinue(" + componentLabel + ");";
		} else if ( exitsAllowed.equals( ExitsAllowed.LOOP ) ) {
			template = "if(true) continue " + continueLabel + ";";
		} else if ( exitsAllowed.equals( ExitsAllowed.FUNCTION ) ) {
			template = "if(true) return null;";
		} else {
			template = "if(true) return;";
		}

		Node javaStmt = parseStatement( template, new HashMap<>() );
		// logger.trace( node.getSourceText() + " -> " + javaStmt );
		addIndex( javaStmt, node );
		return javaStmt;
	}
}
